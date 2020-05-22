/*
   Copyright (c) 2020 LinkedIn Corp.

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

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.parser.NonBlockingDataParser;
import com.linkedin.entitystream.ReadHandle;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.linkedin.data.parser.NonBlockingDataParser.Token.*;

/**
 * A decoder for a {@link DataComplex} object implemented as a
 * {@link com.linkedin.entitystream.Reader} reading from an {@link com.linkedin.entitystream.EntityStream} of
 * ByteString. The implementation is backed by a non blocking {@link NonBlockingDataParser}
 * because the raw bytes are pushed to the decoder, it keeps the partially built data structure in a stack.
 * It is not thread safe. Caller must ensure thread safety.
 *
 * @author kramgopa, xma, amgupta1
 */
public abstract class AbstractDataDecoder<T extends DataComplex> implements DataDecoder<T>
{
  private static final EnumSet<NonBlockingDataParser.Token> SIMPLE_VALUE =
      EnumSet.of(STRING, RAW_BYTES, INTEGER, LONG, FLOAT, DOUBLE, BOOL_TRUE, BOOL_FALSE, NULL);
  private static final EnumSet<NonBlockingDataParser.Token> FIELD_NAME = EnumSet.of(STRING);
  private static final EnumSet<NonBlockingDataParser.Token> VALUE = EnumSet.of(START_OBJECT, START_ARRAY);
  private static final EnumSet<NonBlockingDataParser.Token> NEXT_OBJECT_FIELD = EnumSet.of(END_OBJECT);
  private static final EnumSet<NonBlockingDataParser.Token> NEXT_ARRAY_ITEM = EnumSet.of(END_ARRAY);

  protected static final EnumSet<NonBlockingDataParser.Token> NONE = EnumSet.noneOf(NonBlockingDataParser.Token.class);
  protected static final EnumSet<NonBlockingDataParser.Token> START_TOKENS =
      EnumSet.of(NonBlockingDataParser.Token.START_OBJECT, NonBlockingDataParser.Token.START_ARRAY);
  public static final EnumSet<NonBlockingDataParser.Token> START_ARRAY_TOKEN = EnumSet.of(NonBlockingDataParser.Token.START_ARRAY);
  public static final EnumSet<NonBlockingDataParser.Token> START_OBJECT_TOKEN = EnumSet.of(NonBlockingDataParser.Token.START_OBJECT);

  static
  {
    VALUE.addAll(SIMPLE_VALUE);
    NEXT_OBJECT_FIELD.addAll(FIELD_NAME);
    NEXT_ARRAY_ITEM.addAll(VALUE);
  }

  private final CompletableFuture<T> _completable;
  private T _result;
  private ReadHandle _readHandle;
  private NonBlockingDataParser _parser;

  private final Deque<DataComplex> _stack;
  private final Deque<String> _currFieldStack;
  private String _currField;
  private boolean _isCurrList;
  private ByteString _currentChunk;
  private int _currentChunkIndex = -1;

  protected EnumSet<NonBlockingDataParser.Token> _expectedTokens;

  protected AbstractDataDecoder(EnumSet<NonBlockingDataParser.Token> expectedFirstTokens)
  {
    _completable = new CompletableFuture<>();
    _result = null;
    _stack = new ArrayDeque<>();
    _currFieldStack = new ArrayDeque<>();
    _expectedTokens = expectedFirstTokens;
  }

  protected AbstractDataDecoder()
  {
    this(START_TOKENS);
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _readHandle = rh;

    try
    {
      _parser = createDataParser();
    }
    catch (IOException e)
    {
      handleException(e);
    }

    _readHandle.request(1);
  }

  /**
   * Interface to create non blocking data object parser that process different kind of event/read operations.
   */
  protected abstract NonBlockingDataParser createDataParser() throws IOException;

  @Override
  public void onDataAvailable(ByteString data)
  {
    // Process chunk incrementally without copying the data in the interest of performance.
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
      _currentChunkIndex = _currentChunk.feed(_parser, _currentChunkIndex);
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
      NonBlockingDataParser.Token token;
      while ((token = _parser.nextToken()) != EOF_INPUT)
      {
        validate(token);
        switch (token)
        {
          case START_OBJECT:
            push(createDataObject(_parser), false);
            break;
          case START_ARRAY:
            push(createDataList(_parser), true);
            break;
          case END_OBJECT:
          case END_ARRAY:
            pop();
            break;
          case STRING:
            if (!_isCurrList && _currField == null)
            {
              _currField = _parser.getString();
              _expectedTokens = VALUE;
            }
            else
            {
              addValue(_parser.getString());
            }
            break;
          case RAW_BYTES:
            addValue(_parser.getRawBytes());
            break;
          case INTEGER:
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
          case BOOL_TRUE:
            addValue(Boolean.TRUE);
            break;
          case BOOL_FALSE:
            addValue(Boolean.FALSE);
            break;
          case NULL:
            addValue(Data.NULL);
            break;
          case NOT_AVAILABLE:
            readNextChunk();
            return;
          default:
            handleException(new Exception("Unexpected token " + token + " from data parser"));
        }
      }
    }
    catch (IOException e)
    {
      handleException(e);
    }
  }

  /**
   * Interface to new complex object, invoked at the start of parsing object.
   */
  protected abstract DataComplex createDataObject(NonBlockingDataParser parser);

  /**
   * Interface to new complex list, invoked at start of parsing array.
   */
  protected abstract DataComplex createDataList(NonBlockingDataParser parser);

  protected final boolean isCurrList()
  {
    return _isCurrList;
  }

  protected final void validate(NonBlockingDataParser.Token token)
  {
    if (!(token == NOT_AVAILABLE || _expectedTokens.contains(token)))
    {
      handleException(new Exception("Expecting " + _expectedTokens + " but got " + token));
    }
  }

  private void push(DataComplex dataComplex, boolean isList)
  {
    if (!(_isCurrList || _stack.isEmpty()))
    {
      _currFieldStack.push(_currField);
      _currField = null;
    }
    _stack.push(dataComplex);
    _isCurrList = isList;
    updateExpected();
  }

  @SuppressWarnings("unchecked")
  private void pop()
  {
    // The stack should never be empty because of token validation.
    assert !_stack.isEmpty() : "Trying to pop empty stack";

    DataComplex tmp = _stack.pop();
    tmp = postProcessDataComplex(tmp);
    if (_stack.isEmpty())
    {
      _result = (T) tmp;
      // No more tokens is expected.
      _expectedTokens = NONE;
    }
    else
    {
      _isCurrList = _stack.peek() instanceof DataList;
      if (!_isCurrList)
      {
        _currField = _currFieldStack.pop();
      }
      addValue(tmp);
      updateExpected();
    }
  }

  /**
   * Method invoked to do any post processing on complex object/list after its completely parsed and popped from stack
   */
  protected DataComplex postProcessDataComplex(DataComplex dataComplex)
  {
    return dataComplex;
  }

  /**
   * Method invoked to add element to currently pending complex object/list
   */
  protected void addValue(Object value)
  {
    if (!_stack.isEmpty())
    {
      DataComplex currItem = _stack.peek();
      if (_isCurrList)
      {
        CheckedUtil.addWithoutChecking((DataList) currItem, value);
      }
      else
      {
        addEntryToDataObject(currItem, _currField, value);
        _currField = null;
      }
      updateExpected();
    }
  }

  /**
   * Method invoked to add element to the provided data object
   */
  protected void addEntryToDataObject(DataComplex dataObject, String currField, Object currValue)
  {
    CheckedUtil.putWithoutChecking((DataMap) dataObject, currField, currValue);
  }

  /**
   * Util method to replace top of currently pending complex object stack. Warning: use with caution
   */
  protected final void replaceObjectStackTop(DataComplex dataComplex)
  {
    _stack.pop();
    _stack.push(dataComplex);
  }

  /**
   * Method to update next expected tokens after a value or start object/list tokens
   */
  protected void updateExpected()
  {
    _expectedTokens = _isCurrList ? NEXT_ARRAY_ITEM : NEXT_OBJECT_FIELD;
  }

  @Override
  public void onDone()
  {
    // We must signal to the parser the end of the input and pull any remaining token, even if it's unexpected.
    _parser.endOfInput();
    processTokens();

    if (_stack.isEmpty())
    {
      _completable.complete(_result);
    }
    else
    {
      handleException(new Exception("Unexpected end of source"));
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

  protected void handleException(Throwable e)
  {
    _readHandle.cancel();
    _completable.completeExceptionally(e);
  }
}
