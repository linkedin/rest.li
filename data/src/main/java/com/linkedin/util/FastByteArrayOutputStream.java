/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2017 LinkedIn Corp.
 *
 */

package com.linkedin.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.IllegalArgumentException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This code is derived from org.springframework.util.FastByteArrayOutputStream.
 * It is an alternative of {@link java.io.ByteArrayOutputStream}. The internal
 * storage is implemented as a linked list of byte arrays. When its capacity is
 * full, a new array will be added to the end of the linked list. The advantage
 * over {@link java.io.ByteArrayOutputStream} is that FastByteArrayOutputStream
 * does not require to copy buffer or discard original array on resize.
 *
 * We removed some unnecessary functionalities from springframework, e.g.
 * getInputStream, resize. We also modified toByteArray to only copy contents
 * from the internal linked list to a new byte array. This reduces toByteArray's
 * overhead to the same level as the original toByteArrayUnsafe, and therefore
 * we no longer need toByteArrayUnsafe and discard it.
 */

public class FastByteArrayOutputStream extends OutputStream
{
  private static final int DEFAULT_BUFFER_SIZE = 256;

  // The internal buffer list to store contents.
  private final LinkedList<byte[]> _bufferList = new LinkedList<byte[]>();

  // The size of the next buffer to allocate in the list.
  private int _nextBufferSize = 0;

  // Total size of previous buffers (exclude the last buffer) in the list.
  private int _alreadyBufferedSize = 0;

  // The index of the last buffer to be written next.
  private int _index = 0;

  /**
   * Create a new <code>FastByteArrayOutputStream</code> with the default buffer size.
   */
  public FastByteArrayOutputStream() { this(DEFAULT_BUFFER_SIZE); }

  /**
   * Create a new <code>FastByteArrayOutputStream</code> with customized buffer size.
   */
  public FastByteArrayOutputStream(int initialBufferSize)
  {
    this._nextBufferSize = initialBufferSize;
  }

  /**
   * Write the specified byte into the output stream.
   *
   * @param datum  the byte to be written.
   */
  @Override
  public void write(int datum)
  {
    if (this._bufferList.peekLast() == null || this._bufferList.getLast().length == this._index)
    {
      // If there's no buffer or its capacity is full, add a new one to the list.
      addBuffer(1);
    }
    this._bufferList.getLast()[this._index++] = (byte) datum;
  }

  /**
   * Write <code>length</code> bytes from the specified byte array to the output stream
   * starting from <code>offset</code>.
   *
   * @param data    the source byte array.
   * @param offset  the offset to start from source array.
   * @param length  the number of bytes to write.
   */
  @Override
  public void write(byte[] data, int offset, int length)
  {
    if (data == null)
    {
      throw new NullPointerException();
    }
    else if (offset < 0 || offset + length > data.length || length < 0 || length > MAX_STREAM_SIZE - size())
    {
      throw new IndexOutOfBoundsException();
    }
    else
    {
      if (this._bufferList.peekLast() == null || this._bufferList.getLast().length == this._index)
      {
        // If there's no buffer or its capacity is full, add a new one to the list.
        addBuffer(length);
      }
      if (this._index + length > this._bufferList.getLast().length)
      {
        // If the data can not fit into the last buffer in the list, we need to
        // copy it chunk by chunk.
        int pos = offset;
        do {
          if (this._index == this._bufferList.getLast().length)
          {
            addBuffer(length);
          }
          int copyLength = this._bufferList.getLast().length - this._index;
          if (length < copyLength)
          {
            copyLength = length;
          }
          System.arraycopy(data, pos, this._bufferList.getLast(), this._index, copyLength);
          pos += copyLength;
          this._index += copyLength;
          length -= copyLength;
        }
        while (length > 0);
      }
      else
      {
        // If the data can fit into the last buffer in the list, copy it directly.
        System.arraycopy(data, offset, this._bufferList.getLast(), this._index, length);
        this._index += length;
      }
    }
  }

  /**
   * Convert the content into a string.
   */
  @Override
  public String toString() { return new String(toByteArray()); }

  /**
   * Return the number of bytes stored in the output stream. This is usually
   * less than the total number of buffer.length in the list.
   */
  public int size() { return this._alreadyBufferedSize + this._index; }

  /**
   * Return a single byte array which contains all contents from the internal buffers.
   * Modify the returned buffer will not affect the output stream's content.
   */
  public byte[] toByteArray()
  {
    if (this._bufferList.peekFirst() == null)
    {
      // Return an empty array if the output stream is empty
      return new byte[0];
    }
    else
    {
      int totalSize = size();
      byte[] targetBuffer = new byte[totalSize];
      int pos = 0;
      Iterator<byte[]> iter = this._bufferList.iterator();
      while (iter.hasNext())
      {
        byte[] buffer = iter.next();
        if (iter.hasNext())
        {
          // If it has next buffer, we know this buffer is full and
          // we copy the whole buffer to the new buffer.
          System.arraycopy(buffer, 0, targetBuffer, pos, buffer.length);
          pos += buffer.length;
        }
        else
        {
          // If this is the last buffer, we only copy valid content based on _index
          System.arraycopy(buffer, 0, targetBuffer, pos, this._index);
        }
      }
      return targetBuffer;
    }
  }

  /**
   * The maximum number of bytes the stream is allowed to store. Exceeding the limit will
   * result in OutOfMemoryError when invoking toByteArray().
   */
  private static final int MAX_STREAM_SIZE = Integer.MAX_VALUE - 8;

  /**
   * Allocate new buffer into the linked list.
   *
   * @param minCapacity  minimum capacity to satisfy.
   */
  private void addBuffer(int minCapacity)
  {
    if (this._bufferList.peekLast() != null)
    {
      this._alreadyBufferedSize += this._index;
      this._index = 0;
    }
    if (this._nextBufferSize < minCapacity)
    {
      this._nextBufferSize = nextPowerOf2(minCapacity);
    }

    // Make sure we always stay within maximum stream size.
    if (this._nextBufferSize > MAX_STREAM_SIZE - size())
    {
      this._nextBufferSize = MAX_STREAM_SIZE - size();
    }
    this._bufferList.add(new byte[this._nextBufferSize]);
    this._nextBufferSize *= 2;
  }

  /**
   * Get the next power of 2 of the given value.
   *
   * @param val  the value to determine the next power of 2.
   */
  private static int nextPowerOf2(int val)
  {
    val--;
    val = (val >> 1) | val;
    val = (val >> 2) | val;
    val = (val >> 4) | val;
    val = (val >> 8) | val;
    val = (val >> 16) | val;
    return ++val;
  }
}
