/*
   Copyright (c) 2018 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.linkedin.data.codec.entitystream;

import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.dataformat.smile.async.NonBlockingByteArrayParser;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataDecodingException;
import com.linkedin.data.codec.symbol.SymbolTable;
import com.linkedin.entitystream.ReadHandle;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.linkedin.data.codec.entitystream.JacksonLICORDataDecoder.Token.*;

/**
 * A LICOR (LinkedIn Compact Object Representation) decoder for a {@link DataComplex} object implemented as a
 * {@link com.linkedin.entitystream.Reader} reading from an {@link com.linkedin.entitystream.EntityStream} of
 * ByteString. The implementation is backed by Jackson's {@link NonBlockingByteArrayParser}. Because the raw bytes are
 * pushed to the decoder, it keeps the partially built data structure in a stack.
 *
 * <p>LICOR is a tweaked version of JSON that serializes maps as lists, and has support for serializing field IDs
 * in lieu of field names using an optional symbol table. The payload is serialized as JSON or SMILE depending on
 * whether the codec is configured to use binary or not.</p>
 *
 * @author kramgopa
 */
class JacksonLICORDataDecoder<T extends DataComplex> implements DataDecoder<T>
{
  /**
   * Internal tokens. Each token is presented by a bit in a byte.
   */
  enum Token
  {
    START_ARRAY  (0b00000001),
    END_ARRAY    (0b00000010),
    SIMPLE_VALUE (0b00000100),
    FIELD_NAME   (0b00001000),
    STRUCT_MARKER(0b00010000);

    final byte bitPattern;

    Token(int bp)
    {
      bitPattern = (byte) bp;
    }
  }

  private static final byte VALUE = (byte) (SIMPLE_VALUE.bitPattern | START_ARRAY.bitPattern);
  private static final byte NEXT_ARRAY_ITEM = (byte) (VALUE | END_ARRAY.bitPattern);
  private static final byte NEXT_MAP_KEY = (byte) (FIELD_NAME.bitPattern | END_ARRAY.bitPattern);

  private final JsonFactory _jsonFactory;
  private final SymbolTable _symbolTable;

  private CompletableFuture<T> _completable;
  private T _result;
  private ReadHandle _readHandle;

  private JsonParser _jsonParser;
  private ByteArrayFeeder _byteArrayFeeder;

  private Deque<DataComplex> _stack;
  private String _currField;
  private Byte _expectedStartMarker;
  // Expected tokens represented by a bit pattern. Every bit represents a token.
  private byte _expectedTokens;
  private boolean _isCurrList;
  private boolean _isFieldNameExpected;
  private boolean _isStructStart;
  private ByteString _currentChunk;
  private int _currentChunkIndex = -1;

  public JacksonLICORDataDecoder(boolean decodeBinary)
  {
    this(decodeBinary, false, null);
    _expectedStartMarker = null;
  }

  public JacksonLICORDataDecoder(boolean decodeBinary, boolean isDataList, SymbolTable symbolTable)
  {
    _jsonFactory = JacksonLICORStreamDataCodec.getFactory(decodeBinary);
    _completable = new CompletableFuture<>();
    _result = null;
    _stack = new ArrayDeque<>();
    _expectedTokens = START_ARRAY.bitPattern;
    _expectedStartMarker = isDataList ? JacksonLICORStreamDataCodec.LIST_ORDINAL : JacksonLICORStreamDataCodec.MAP_ORDINAL;
    _symbolTable = symbolTable;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _readHandle = rh;

    try
    {
      _jsonParser = _jsonFactory.createNonBlockingByteArrayParser();
      _byteArrayFeeder = (ByteArrayFeeder)_jsonParser;
    }
    catch (IOException e)
    {
      handleException(e);
    }

    readNextChunk();
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    _currentChunk = data;
    _currentChunkIndex = 0;

    processCurrentChunk();
  }

  private void readNextChunk()
  {
    if (_currentChunkIndex == -1)
    {
      _readHandle.request(1);
      return;
    }

    processCurrentChunk();
  }

  private void processCurrentChunk()
  {
    try
    {
      _currentChunkIndex = _currentChunk.feed(_byteArrayFeeder, _currentChunkIndex);
      processTokens();
    }
    catch (IOException e)
    {
      handleException(e);
    }
  }

  private void processTokens()
  {
    try
    {
      JsonToken token;
      while ((token = _jsonParser.nextToken()) != null)
      {
        switch (token)
        {
          case START_ARRAY:
            validate(START_ARRAY);
            _isStructStart = true;
            _expectedTokens = STRUCT_MARKER.bitPattern;
            break;
          case END_ARRAY:
            validate(END_ARRAY);
            pop();
            break;
          case VALUE_STRING:
            if (_isFieldNameExpected)
            {
              validate(FIELD_NAME);
              _isFieldNameExpected = false;
              _currField = _jsonParser.getText();
              _expectedTokens = VALUE;
            }
            else
            {
              validate(SIMPLE_VALUE);
              addValue(_jsonParser.getText());
            }
            break;
          case VALUE_NUMBER_INT:
          case VALUE_NUMBER_FLOAT:
            JsonParser.NumberType numberType = _jsonParser.getNumberType();
            switch (numberType)
            {
              case INT:
                if (_isStructStart)
                {
                  _isStructStart = false;
                  validate(STRUCT_MARKER);
                  byte marker = _jsonParser.getByteValue();
                  if (_expectedStartMarker != null && marker != _expectedStartMarker)
                  {
                    marker = -1;
                  }
                  else
                  {
                    _expectedStartMarker = null;
                  }

                  switch (marker)
                  {
                    case JacksonLICORStreamDataCodec.LIST_ORDINAL:
                    {
                      push(new DataList(), true);
                      break;
                    }
                    case JacksonLICORStreamDataCodec.MAP_ORDINAL:
                    {
                      push(new DataMap(), false);
                      break;
                    }
                    default:
                    {
                      throw new DataDecodingException("Unexpected marker: " + marker + " " + _jsonParser.getText());
                    }
                  }
                }
                else if (_isFieldNameExpected)
                {
                  validate(FIELD_NAME);
                  _isFieldNameExpected = false;
                  int sid = _jsonParser.getIntValue();
                  if (_symbolTable == null || (_currField = _symbolTable.getSymbolName(sid)) == null)
                  {
                    throw new DataDecodingException("Did not find mapping for symbol: " + sid);
                  }
                  _expectedTokens = VALUE;
                }
                else
                {
                  validate(SIMPLE_VALUE);
                  addValue(_jsonParser.getIntValue());
                }
                break;
              case LONG:
                validate(SIMPLE_VALUE);
                addValue(_jsonParser.getLongValue());
                break;
              case FLOAT:
                validate(SIMPLE_VALUE);
                addValue(_jsonParser.getFloatValue());
                break;
              case DOUBLE:
                validate(SIMPLE_VALUE);
                addValue(_jsonParser.getDoubleValue());
                break;
              case BIG_INTEGER:
              case BIG_DECIMAL:
              default:
                handleException(new Exception("Unexpected number value type " + numberType + " at " + _jsonParser.getTokenLocation()));
                break;
            }
            break;
          case VALUE_TRUE:
            validate(SIMPLE_VALUE);
            addValue(Boolean.TRUE);
            break;
          case VALUE_FALSE:
            validate(SIMPLE_VALUE);
            addValue(Boolean.FALSE);
            break;
          case VALUE_NULL:
            validate(SIMPLE_VALUE);
            addValue(Data.NULL);
            break;
          case NOT_AVAILABLE:
            readNextChunk();
            return;
          default:
            handleException(new Exception("Unexpected token " + token + " at " + _jsonParser.getTokenLocation()));
        }
      }
    }
    catch (IOException e)
    {
      handleException(e);
    }
  }

  private void validate(Token token)
  {
    if ((_expectedTokens & token.bitPattern) == 0)
    {
      handleException(new Exception("Expecting " + joinTokens(_expectedTokens) + " but got " + token
          + " at " + _jsonParser.getTokenLocation()));
    }
  }

  private void push(DataComplex dataComplex, boolean isList)
  {
    addValue(dataComplex);
    _stack.push(dataComplex);
    _isCurrList = isList;
    _isFieldNameExpected = !_isCurrList;
    _expectedTokens = _isFieldNameExpected ? NEXT_MAP_KEY : NEXT_ARRAY_ITEM;
  }

  @SuppressWarnings("unchecked")
  private void pop()
  {
    // The stack should never be empty because of token validation.
    assert !_stack.isEmpty() : "Trying to pop empty stack at " + _jsonParser.getTokenLocation();

    DataComplex tmp = _stack.pop();
    if (_stack.isEmpty())
    {
      _result = (T) tmp;
      // No more tokens is expected.
      _expectedTokens = 0;
    }
    else
    {
      _isCurrList = _stack.peek() instanceof DataList;
      _isFieldNameExpected = !_isCurrList;
      _expectedTokens = _isFieldNameExpected ? NEXT_MAP_KEY : NEXT_ARRAY_ITEM;
    }
  }

  private void addValue(Object value)
  {
    if (!_stack.isEmpty())
    {
      DataComplex currItem = _stack.peek();
      if (_isCurrList)
      {
        ((DataList) currItem).add(value);
        _expectedTokens = NEXT_ARRAY_ITEM;
      }
      else
      {
        ((DataMap) currItem).put(_currField, value);
        _isFieldNameExpected = true;
        _expectedTokens = NEXT_MAP_KEY;
      }
    }
  }

  @Override
  public void onDone()
  {
    // We must signal to the parser the end of the input and pull any remaining token, even if it's unexpected.
    _byteArrayFeeder.endOfInput();
    processTokens();

    if (_stack.isEmpty())
    {
      _completable.complete(_result);
    }
    else
    {
      handleException(new Exception("Unexpected end of source at " + _jsonParser.getTokenLocation()));
    }
  }

  @Override
  public void onError(Throwable e)
  {
    _completable.completeExceptionally(e);
  }

  @Override
  public CompletionStage<T> getResult()
  {
    return _completable;
  }

  private void handleException(Throwable e)
  {
    _readHandle.cancel();
    _completable.completeExceptionally(e);
  }

  /**
   * Build a string for the tokens represented by the bit pattern.
   */
  private String joinTokens(byte tokens)
  {
    return tokens == 0
        ? "no tokens"
        : Arrays.stream(JacksonLICORDataDecoder.Token.values())
            .filter(token -> (tokens & token.bitPattern) > 0)
            .map(JacksonLICORDataDecoder.Token::name)
            .collect(Collectors.joining(", "));
  }
}
