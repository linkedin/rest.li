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
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;


/**
 * This OutputStream is non-blocking and has a fixed-size primary buffer and an unbounded overflow buffer. When the
 * primary buffer is full, the remaining bytes are written to the overflow buffer. It supports getting the bytes in the
 * primary buffer as a ByteString. Once the bytes from the primary buffer are retrieved, the bytes from the overflow
 * buffer will fill in the primary buffer.
 *
 * This class is not thread-safe.
 */
class QueueBufferedOutputStream extends OutputStream
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
