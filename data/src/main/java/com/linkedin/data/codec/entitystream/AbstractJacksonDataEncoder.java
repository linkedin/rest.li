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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.entitystream.WriteHandle;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract JSON and JSON-like data type encoder for a {@link com.linkedin.data.DataComplex} object implemented as a
 * {@link com.linkedin.entitystream.Writer} writing to an {@link com.linkedin.entitystream.EntityStream} of
 * {@link ByteString}. The implementation is backed by Jackson's {@link JsonGenerator} or it's subclasses for JSON like
 * formats. The <code>JsonGenerator</code> writes to an internal non-blocking <code>OutputStream</code>
 * implementation that has a fixed-size primary buffer and an unbounded overflow buffer. Because the bytes are pulled
 * from the encoder asynchronously, it needs to keep the state in a stack.
 *
 * @author kramgopa, xma
 */
abstract class AbstractJacksonDataEncoder implements DataEncoder
{
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJacksonDataEncoder.class);

  private static final Object MAP = new Object();
  private static final Object LIST = new Object();

  private JsonFactory _jsonFactory;
  private JsonGenerator _generator;
  private QueueBufferedOutputStream _out;
  private Deque<Iterator<?>> _stack;
  private Deque<Object> _typeStack;
  private WriteHandle<? super ByteString> _writeHandle;
  private boolean _done;

  private AbstractJacksonDataEncoder(JsonFactory jsonFactory, int bufferSize)
  {
    _jsonFactory = jsonFactory;
    _out = new QueueBufferedOutputStream(bufferSize);
    _stack = new ArrayDeque<>();
    _typeStack = new ArrayDeque<>();
    _done = false;
  }

  protected AbstractJacksonDataEncoder(JsonFactory jsonFactory, DataMap dataMap, int bufferSize)
  {
    this(jsonFactory, bufferSize);

    _stack.push(dataMap.entrySet().iterator());
    _typeStack.push(MAP);
  }

  protected AbstractJacksonDataEncoder(JsonFactory jsonFactory, DataList dataList, int bufferSize)
  {
    this(jsonFactory, bufferSize);

    _stack.push(dataList.iterator());
    _typeStack.push(LIST);
  }

  @Override
  public void onInit(WriteHandle<? super ByteString> wh)
  {
    _writeHandle = wh;

    try
    {
      _generator = _jsonFactory.createGenerator(_out);
      if (_typeStack.peek() == MAP)
      {
        _generator.writeStartObject();
      }
      else
      {
        _generator.writeStartArray();
      }
    }
    catch (IOException e)
    {
      _writeHandle.error(e);
    }
  }

  @Override
  public void onWritePossible()
  {
    while (_writeHandle.remaining() > 0)
    {
      if (_done)
      {
        if (_out.isEmpty())
        {
          _writeHandle.done();
          break;
        }
        else
        {
          _writeHandle.write(_out.getBytes());
        }
      }
      else if (_out.isFull())
      {
        _writeHandle.write(_out.getBytes());
      }
      else
      {
        try
        {
          generate();
        }
        catch (Exception e)
        {
          _writeHandle.error(e);
          break;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void generate()
      throws Exception
  {
    while (!_out.isFull())
    {
      Iterator<?> curr = _stack.peek();

      if (curr.hasNext())
      {
        Object currItem = curr.next();
        if (_typeStack.peek() == MAP)
        {
          Map.Entry<String, ?> entry = (Map.Entry<String, ?>) currItem;
          _generator.writeFieldName(entry.getKey());
          writeValue(entry.getValue());
        }
        else
        {
          writeValue(currItem);
        }
      }
      else
      {
        _stack.pop();
        Object type = _typeStack.pop();

        if (type == MAP)
        {
          _generator.writeEndObject();
        }
        else
        {
          _generator.writeEndArray();
        }

        _done = _stack.isEmpty();
        if (_done)
        {
          _generator.close();
          break;
        }
      }
    }
  }

  private void writeValue(Object value)
      throws Exception
  {
    switch (Data.TYPE_MAP.get(value.getClass()))
    {
      case 1:
        _generator.writeString((String) value);
        break;
      case 2:
        _generator.writeNumber((Integer) value);
        break;
      case 3:
        _stack.push(((DataMap) value).entrySet().iterator());
        _typeStack.push(MAP);
        _generator.writeStartObject();
        break;
      case 4:
        _stack.push(((DataList) value).iterator());
        _typeStack.push(LIST);
        _generator.writeStartArray();
        break;
      case 5:
        _generator.writeBoolean((Boolean) value);
        break;
      case 6:
        _generator.writeNumber((Long) value);
        break;
      case 7:
        _generator.writeNumber((Float) value);
        break;
      case 8:
        _generator.writeNumber((Double) value);
        break;
      case 9:
      {
        char[] charArray = ((ByteString) value).asAvroCharArray();
        _generator.writeString(charArray, 0, charArray.length);
        break;
      }
      default:
        throw new Exception("Unexpected value type " + value.getClass() + " for value " + value);
    }
  }

  @Override
  public void onAbort(Throwable e)
  {
    try
    {
      _generator.close();
    }
    catch (IOException ioe)
    {
      LOGGER.warn("Error closing JsonGenerator on abort due to " + e.getMessage(), ioe);
    }
  }
}
