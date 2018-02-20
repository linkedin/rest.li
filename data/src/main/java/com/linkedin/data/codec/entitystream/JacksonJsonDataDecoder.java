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

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.entitystream.ReadHandle;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.linkedin.data.codec.entitystream.JacksonJsonDataDecoder.Token.END_ARRAY;
import static com.linkedin.data.codec.entitystream.JacksonJsonDataDecoder.Token.END_OBJECT;
import static com.linkedin.data.codec.entitystream.JacksonJsonDataDecoder.Token.FIELD_NAME;
import static com.linkedin.data.codec.entitystream.JacksonJsonDataDecoder.Token.SIMPLE_VALUE;
import static com.linkedin.data.codec.entitystream.JacksonJsonDataDecoder.Token.START_ARRAY;
import static com.linkedin.data.codec.entitystream.JacksonJsonDataDecoder.Token.START_OBJECT;


public class JacksonJsonDataDecoder<T extends DataComplex> implements JsonDataDecoder<T>
{
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

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private CompletableFuture<T> _completable;
  private T _result;
  private ReadHandle _readHandle;

  private final NonBlockingJsonParser _parser;

  private Deque<DataComplex> _stack;
  private String _currField;
  private byte _expectedTokens;
  private boolean _isCurrList;

  protected JacksonJsonDataDecoder(byte expectedFirstToken)
      throws IOException
  {
    _completable = new CompletableFuture<>();
    _result = null;
    _parser = (NonBlockingJsonParser) JSON_FACTORY.createNonBlockingByteArrayParser();
    _stack = new ArrayDeque<>();
    _expectedTokens = expectedFirstToken;
  }

  public JacksonJsonDataDecoder()
      throws IOException
  {
    this((byte) (START_OBJECT.bitPattern | START_ARRAY.bitPattern));
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _readHandle = rh;
    _readHandle.request(1);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    byte[] bytes = data.copyBytes(); // TODO: Avoid copying bytes?
    try
    {
      _parser.feedInput(bytes, 0, bytes.length);
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
      while ((token = _parser.nextToken()) != null)
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
            _currField = _parser.getCurrentName();
            _expectedTokens = VALUE;
            break;
          case VALUE_STRING:
            validate(SIMPLE_VALUE);
            addValue(_parser.getText());
            break;
          case VALUE_NUMBER_INT:
          case VALUE_NUMBER_FLOAT:
            validate(SIMPLE_VALUE);
            JsonParser.NumberType numberType = _parser.getNumberType();
            switch (numberType)
            {
              case INT:
                addValue(_parser.getIntValue());
                break;
              case LONG:
                addValue(_parser.getLongValue());
                break;
              case FLOAT:
                addValue(_parser.getFloatValue());
                break;
              case DOUBLE:
                addValue(_parser.getDoubleValue());
                break;
              case BIG_INTEGER:
              case BIG_DECIMAL:
              default:
                handleException(new Exception("Unexpected number value type " + numberType + " at " + _parser.getTokenLocation()));
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
            handleException(new Exception("Unexpected token " + token + " at " + _parser.getTokenLocation()));
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
      handleException(new Exception("Expecting " + joinTokens(_expectedTokens) + " but get " + token + " at " + _parser.getTokenLocation()));
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
    assert !_stack.isEmpty() : "Trying to pop empty stack at " + _parser.getTokenLocation();

    DataComplex tmp = _stack.pop();
    if (_stack.isEmpty())
    {
      _result = (T) tmp;
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
    if (_result != null)
    {
      _completable.complete(_result);
    }
    else
    {
      handleException(new Exception("Unexpected end of source at " + _parser.getTokenLocation()));
    }
  }

  @Override
  public void onError(Throwable e)
  {
    _completable.completeExceptionally(e);
  }

  @Override
  public void getResult(Callback<T> callback)
  {
    _completable.whenComplete((result, e) ->
    {
      if (e != null)
      {
        callback.onError(e);
      }
      else
      {
        callback.onSuccess(result);
      }
    });
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
