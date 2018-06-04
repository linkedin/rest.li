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

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.entitystream.WriteHandle;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;


/**
 * An JSON encoder for a {@link com.linkedin.data.DataComplex} object implemented as a {@link com.linkedin.entitystream.Writer}
 * writing to an {@link com.linkedin.entitystream.EntityStream} of {@link ByteString}. The implementation is backed by
 * Jackson's {@link JsonGenerator}. The <code>JsonGenerator</code> writes to an internal non-blocking <code>OutputStream</code>
 * implementation that has a fixed-size primary buffer and an unbounded overflow buffer. Because the bytes are pulled
 * from the encoder asynchronously, it needs to keep the state in a stack.
 *
 * @author Xiao Ma
 */
public class JacksonJsonDataEncoder implements JsonDataEncoder
{
  private static final Logger LOGGER = LoggerFactory.getLogger(JacksonJsonDataEncoder.class);

  private static final JsonFactory JSON_FACTORY = new JsonFactory();
  private static final Object MAP = new Object();
  private static final Object LIST = new Object();

  private JsonGenerator _generator;
  private QueueBufferedOutputStream _out;
  private Deque<Iterator<?>> _stack;
  private Deque<Object> _typeStack;
  private WriteHandle<? super ByteString> _writeHandle;
  private boolean _done;

  private JacksonJsonDataEncoder(int bufferSize)
  {
    _out = new QueueBufferedOutputStream(bufferSize);
    _stack = new ArrayDeque<>();
    _typeStack = new ArrayDeque<>();
    _done = false;
  }

  public JacksonJsonDataEncoder(DataMap dataMap, int bufferSize)
  {
    this(bufferSize);

    _stack.push(dataMap.entrySet().iterator());
    _typeStack.push(MAP);
  }

  public JacksonJsonDataEncoder(DataList dataList, int bufferSize)
  {
    this(bufferSize);

    _stack.push(dataList.iterator());
    _typeStack.push(LIST);
  }

  @Override
  public void onInit(WriteHandle<? super ByteString> wh)
  {
    _writeHandle = wh;

    try
    {
      _generator = JSON_FACTORY.createGenerator(_out);
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
    /* Expecting string and integer to be most popular */
    Class<?> clas = value.getClass();
    if (clas == String.class)
    {
      _generator.writeString((String) value);
    }
    else if (clas == Integer.class)
    {
      _generator.writeNumber((Integer) value);
    }
    else if (clas == DataMap.class)
    {
      _stack.push(((DataMap) value).entrySet().iterator());
      _typeStack.push(MAP);
      _generator.writeStartObject();
    }
    else if (clas == DataList.class)
    {
      _stack.push(((DataList) value).iterator());
      _typeStack.push(LIST);
      _generator.writeStartArray();
    }
    else if (clas == Boolean.class)
    {
      _generator.writeBoolean((Boolean) value);
    }
    else if (clas == Long.class)
    {
      _generator.writeNumber((Long) value);
    }
    else if (clas == Float.class)
    {
      _generator.writeNumber((Float) value);
    }
    else if (clas == Double.class)
    {
      _generator.writeNumber((Double) value);
    }
    else if (clas == ByteString.class)
    {
      _generator.writeString(((ByteString) value).asAvroString());
    }
    else
    {
      throw new Exception("Unexpected value type " + clas + " for value " + value);
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

  /**
   * This OutputStream is non-blocking and has a fixed-size primary buffer and an unbounded overflow buffer. When the primary buffer is full, the
   * remaining bytes are written to the overflow buffer. It supports getting the bytes in the primary buffer as a
   * ByteString. Once the bytes from the primary buffer are retrieved, the bytes from the overflow buffer will fill in
   * the primary buffer.
   *
   * This class is not thread-safe.
   */
  private static class QueueBufferedOutputStream extends OutputStream
  {
    private int _bufferSize;
    /**
     * The primary buffer and the overflow buffer is implemented as a linked list of fixed-sized byte array, with the
     * head being the primary buffer and the rest being overflow buffer. When the head is retrieved, the first element
     * in the rest automatically becomes the head.
     *
     * This implementation observes the following constraints:
     *  - If the head is not full, there should be no more list element.
     *  - After each write, there should never be an empty byte array as tail.
     */
    private Deque<byte[]> _buffers = new ArrayDeque<>();
    private int _tailOffset;

    QueueBufferedOutputStream(int bufferSize)
    {
      _bufferSize = bufferSize;
    }

    @Override
    public void write(int b)
        throws IOException
    {
      byte[] tail = _buffers.peekLast();
      if (tail == null || _tailOffset == _bufferSize)
      {
        tail = new byte[_bufferSize];
        _tailOffset = 0;
        _buffers.addLast(tail);
      }

      tail[_tailOffset++] = (byte) b;
    }

    @Override
    public void write(byte[] data, int offset, int length)
    {
      if (length == 0)
      {
        return;
      }

      byte[] tail = _buffers.peekLast();
      if (tail == null)
      {
        tail = new byte[_bufferSize];
        _buffers.addLast(tail);
        _tailOffset = 0;
      }

      while (length > 0)
      {
        int remaining = _bufferSize - _tailOffset;
        if (length > remaining)
        {
          System.arraycopy(data, offset, tail, _tailOffset, remaining);

          tail = new byte[_bufferSize];
          _buffers.addLast(tail);
          _tailOffset = 0;

          length -= remaining;
          offset += remaining;
        }
        else
        {
          System.arraycopy(data, offset, tail, _tailOffset, length);

          _tailOffset += length;
          break;
        }
      }
    }

    /**
     * Tests whether or not the buffer is empty.
     */
    boolean isEmpty()
    {
      return _buffers.isEmpty();
    }

    /**
     * Gets whether or not the primary buffer is full.
     */
    boolean isFull()
    {
      int size = _buffers.size();
      return size > 1 || (size == 1 && _tailOffset == _bufferSize);
    }

    /**
     * Gets the bytes in the primary buffer. It should only be called when the primary buffer is full, or when reading
     * the last ByteString.
     *
     * It also makes the head of the overflow buffer the primary buffer so that those bytes are returned next time
     * this method is called.
     */
    ByteString getBytes()
    {
      byte[] bytes = _buffers.removeFirst();
      return _buffers.isEmpty()
          ? ByteString.unsafeWrap(bytes, 0, _tailOffset)
          : ByteString.unsafeWrap(bytes);
    }
  }
}
