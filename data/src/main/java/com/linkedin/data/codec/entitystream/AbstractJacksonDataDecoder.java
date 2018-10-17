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
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
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

import static com.linkedin.data.codec.entitystream.AbstractJacksonDataDecoder.Token.END_ARRAY;
import static com.linkedin.data.codec.entitystream.AbstractJacksonDataDecoder.Token.END_OBJECT;
import static com.linkedin.data.codec.entitystream.AbstractJacksonDataDecoder.Token.FIELD_NAME;
import static com.linkedin.data.codec.entitystream.AbstractJacksonDataDecoder.Token.SIMPLE_VALUE;
import static com.linkedin.data.codec.entitystream.AbstractJacksonDataDecoder.Token.START_ARRAY;
import static com.linkedin.data.codec.entitystream.AbstractJacksonDataDecoder.Token.START_OBJECT;


/**
 * A JSON or JSON-like format decoder for a {@link DataComplex} object implemented as a
 * {@link com.linkedin.entitystream.Reader} reading from an {@link com.linkedin.entitystream.EntityStream} of
 * ByteString. The implementation is backed by a non blocking {@link JsonParser}. Because the raw bytes are
 * pushed to the decoder, it keeps the partially built data structure in a stack.
 *
 * @author kramgopa, xma
 */
class AbstractJacksonDataDecoder<T extends DataComplex> implements DataDecoder<T>
{
  /**
   * Internal tokens. Each token is presented by a bit in a byte.
   */
  enum Token
  {
    START_OBJECT(0b00000001),
    END_OBJECT  (0b00000010),
    START_ARRAY (0b00000100),
    END_ARRAY   (0b00001000),
    FIELD_NAME  (0b00010000),
    SIMPLE_VALUE(0b00100000);

    final byte bitPattern;

    Token(int bp)
    {
      bitPattern = (byte) bp;
    }
  }

  private static final byte VALUE = (byte) (SIMPLE_VALUE.bitPattern | START_OBJECT.bitPattern | START_ARRAY.bitPattern);
  private static final byte NEXT_OBJECT_FIELD = (byte) (FIELD_NAME.bitPattern | END_OBJECT.bitPattern);
  private static final byte NEXT_ARRAY_ITEM = (byte) (VALUE | END_ARRAY.bitPattern);

  private final JsonFactory _jsonFactory;

  private CompletableFuture<T> _completable;
  private T _result;
  private ReadHandle _readHandle;

  private JsonParser _jsonParser;
  private ByteArrayFeeder _byteArrayFeeder;

  private Deque<DataComplex> _stack;
  private String _currField;
  // Expected tokens represented by a bit pattern. Every bit represents a token.
  private byte _expectedTokens;
  private boolean _isCurrList;

  protected AbstractJacksonDataDecoder(JsonFactory jsonFactory, byte expectedFirstToken)
  {
    _jsonFactory = jsonFactory;
    _completable = new CompletableFuture<>();
    _result = null;
    _stack = new ArrayDeque<>();
    _expectedTokens = expectedFirstToken;
  }

  protected AbstractJacksonDataDecoder(JsonFactory jsonFactory)
  {
    this(jsonFactory, (byte) (START_OBJECT.bitPattern | START_ARRAY.bitPattern));
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

    _readHandle.request(1);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    byte[] bytes = data.copyBytes(); // TODO: Avoid copying bytes?
    try
    {
      /**
       * Method that can be called to feed more data, if (and only if)
       * {@link #needMoreInput} returns true.
       * --Copied from ByteArrayFeeder--
       **/
      if (_byteArrayFeeder.needMoreInput())
      {
        _byteArrayFeeder.feedInput(bytes, 0, bytes.length);
      }
    }
    catch (IOException e)
    {
      handleException(e);
    }

    processTokens();
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
          case START_OBJECT:
            validate(START_OBJECT);
            push(new DataMap(), false);
            break;
          case END_OBJECT:
            validate(END_OBJECT);
            pop();
            break;
          case START_ARRAY:
            validate(START_ARRAY);
            push(new DataList(), true);
            break;
          case END_ARRAY:
            validate(END_ARRAY);
            pop();
            break;
          case FIELD_NAME:
            validate(FIELD_NAME);
            _currField = _jsonParser.getCurrentName();
            _expectedTokens = VALUE;
            break;
          case VALUE_STRING:
            validate(SIMPLE_VALUE);
            addValue(_jsonParser.getText());
            break;
          case VALUE_NUMBER_INT:
          case VALUE_NUMBER_FLOAT:
            validate(SIMPLE_VALUE);
            JsonParser.NumberType numberType = _jsonParser.getNumberType();
            switch (numberType)
            {
              case INT:
                addValue(_jsonParser.getIntValue());
                break;
              case LONG:
                addValue(_jsonParser.getLongValue());
                break;
              case FLOAT:
                addValue(_jsonParser.getFloatValue());
                break;
              case DOUBLE:
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
            _readHandle.request(1);
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
      handleException(new Exception("Expecting " + joinTokens(_expectedTokens) + " but get " + token
          + " at " + _jsonParser.getTokenLocation()));
    }
  }

  private void push(DataComplex dataComplex, boolean isList)
  {
    addValue(dataComplex);
    _stack.push(dataComplex);
    _isCurrList = isList;
    updateExpected();
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
      updateExpected();
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
      }
      else
      {
        ((DataMap) currItem).put(_currField, value);
      }

      updateExpected();
    }
  }

  private void updateExpected()
  {
    _expectedTokens = _isCurrList ? NEXT_ARRAY_ITEM : NEXT_OBJECT_FIELD;
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
        : Arrays.stream(Token.values())
            .filter(token -> (tokens & token.bitPattern) > 0)
            .map(Token::name)
            .collect(Collectors.joining(", "));
  }
}
