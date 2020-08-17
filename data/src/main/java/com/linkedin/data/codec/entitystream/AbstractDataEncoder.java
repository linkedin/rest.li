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
import com.linkedin.entitystream.WriteHandle;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract data type encoder for a {@link com.linkedin.data.DataComplex} object implemented as a
 * {@link com.linkedin.entitystream.Writer} writing to an {@link com.linkedin.entitystream.EntityStream} of
 * {@link ByteString}. The implementation writes to an internal non-blocking <code>OutputStream</code>
 * implementation that has a fixed-size primary buffer and an unbounded overflow buffer. Because the bytes are pulled
 * from the encoder asynchronously, it needs to keep the state in a stack.
 *
 * @author kramgopa, xma
 */
public abstract class AbstractDataEncoder implements DataEncoder
{
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataEncoder.class);

  private static final Object MAP = new Object();
  private static final Object LIST = new Object();

  private Data.TraverseCallback _traverseCallback;
  private QueueBufferedOutputStream _out;
  private Deque<DataComplex> _stack;
  private Deque<Iterator<?>> _iteratorStack;
  private Deque<Object> _typeStack;
  private WriteHandle<? super ByteString> _writeHandle;
  private boolean _done;

  private AbstractDataEncoder(int bufferSize)
  {
    _out = new QueueBufferedOutputStream(bufferSize);
    _stack = new ArrayDeque<>();
    _iteratorStack = new ArrayDeque<>();
    _typeStack = new ArrayDeque<>();
    _done = false;
  }

  protected AbstractDataEncoder(DataMap dataMap, int bufferSize)
  {
    this(bufferSize);

    _stack.push(dataMap);
    _typeStack.push(MAP);
  }

  protected AbstractDataEncoder(DataList dataList, int bufferSize)
  {
    this(bufferSize);

    _stack.push(dataList);
    _typeStack.push(LIST);
  }

  @Override
  public void onInit(WriteHandle<? super ByteString> wh)
  {
    _writeHandle = wh;

    try
    {
      _traverseCallback = createTraverseCallback(_out);
    }
    catch (IOException e)
    {
      _writeHandle.error(e);
    }
  }

  /**
   * This interface to create data object traverseCallback that process different kind of traversal event.
   *
   * Callback methods can throw IOException
   * @param out writes data object bytes to this output stream
   * @throws IOException as a checked exception to indicate traversal error.
   */
  abstract protected Data.TraverseCallback createTraverseCallback(OutputStream out) throws IOException;

  /**
   * Pre-process this {@link DataMap} before serializing it.
   *
   * <p>This can be overridden by implementations to modify the map before serializing. Implementations may also
   * choose to directly serialize the map in whatever form they prefer, and return null to indicate that they have
   * internally handled serialization.</p>
   */
  protected DataMap preProcessMap(DataMap dataMap) throws IOException
  {
    return dataMap;
  }

  /**
   * Pre-process this {@link DataList} before serializing it.
   *
   * <p>This can be overridden by implementations to modify the map before serializing. Implementations may also
   * choose to directly serialize the list in whatever form they prefer, and return null to indicate that they have
   * internally handled serialization.</p>
   */
  protected DataList preProcessList(DataList dataList) throws IOException
  {
    return dataList;
  }

  /**
   * Create an iterator that will be used by the encoder to iterate over entries of this {@link DataMap} when
   * serializing.
   *
   * <p>This can be overridden by implementations to control the order in which entries are serialized. It is
   * highly recommended to not modify the iterator or the backing map after this method has been called. Doing so
   * may result in a {@link java.util.ConcurrentModificationException}</p>
   */
  protected Iterator<Map.Entry<String, Object>> createIterator(DataMap dataMap) throws IOException
  {
    return dataMap.entrySet().iterator();
  }

  /**
   * Create an iterator that will be used by the encoder to iterate over elements of this {@link DataList} when
   * serializing.
   *
   * <p>This can be overridden by implementations to control the order in which elements are serialized. It is
   * highly recommended to not modify the iterator or the backing list after this method has been called. Doing so
   * may result in a {@link java.util.ConcurrentModificationException}</p>
   */
  protected Iterator<Object> createIterator(DataList dataList) throws IOException
  {
    return dataList.iterator();
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
      DataComplex current = _stack.peek();
      if (_iteratorStack.size() != _stack.size())
      {
        if (_typeStack.peek() == MAP)
        {
          DataMap dataMap = preProcessMap((DataMap) current);
          if (dataMap != null)
          {
            _iteratorStack.push(createIterator(dataMap));
            _traverseCallback.startMap(dataMap);
          }
          else
          {
            removeProcessedEntity();
            if (_done)
            {
              _traverseCallback.close();
              break;
            }
          }
        }
        else
        {
          DataList dataList = preProcessList((DataList) current);
          if (dataList != null)
          {
            _iteratorStack.push(createIterator(dataList));
            _traverseCallback.startList(dataList);
          }
          else
          {
            removeProcessedEntity();
            if (_done)
            {
              _traverseCallback.close();
              break;
            }
          }
        }

        continue;
      }

      Iterator<?> curr = _iteratorStack.peek();
      if (curr.hasNext())
      {
        Object currItem = curr.next();
        if (_typeStack.peek() == MAP)
        {
          Map.Entry<String, ?> entry = (Map.Entry<String, ?>) currItem;
          _traverseCallback.key(entry.getKey());
          writeValue(entry.getValue());
        }
        else
        {
          writeValue(currItem);
        }
      }
      else
      {
        _iteratorStack.pop();
        Object type = removeProcessedEntity();

        if (type == MAP)
        {
          _traverseCallback.endMap();
        }
        else
        {
          _traverseCallback.endList();
        }

        if (_done)
        {
          _traverseCallback.close();
          break;
        }
      }
    }
  }

  private void writeValue(Object value) throws Exception
  {
    if (value == null || value == Data.NULL)
    {
      _traverseCallback.nullValue();
      return;
    }

    switch (Data.TYPE_MAP.get(value.getClass()))
    {
      case 1:
        _traverseCallback.stringValue((String) value);
        break;
      case 2:
        _traverseCallback.integerValue((int) value);
        break;
      case 3:
        _stack.push((DataMap) value);
        _typeStack.push(MAP);
        break;
      case 4:
        _stack.push((DataList) value);
        _typeStack.push(LIST);
        break;
      case 5:
        _traverseCallback.booleanValue((boolean) value);
        break;
      case 6:
        _traverseCallback.longValue((long) value);
        break;
      case 7:
        _traverseCallback.floatValue((float) value);
        break;
      case 8:
        _traverseCallback.doubleValue((double) value);
        break;
      case 9:
        _traverseCallback.byteStringValue((ByteString) value);
        break;
      default:
        _traverseCallback.illegalValue(value);
    }
  }

  private Object removeProcessedEntity()
  {
    _stack.pop();
    _done = _stack.isEmpty();
    return _typeStack.pop();
  }

  @Override
  public void onAbort(Throwable e)
  {
    try
    {
      _traverseCallback.close();
    }
    catch (IOException ioe)
    {
      LOGGER.warn("Error closing output stream on abort due to " + e.getMessage(), ioe);
    }
  }
}
