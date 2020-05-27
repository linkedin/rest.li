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


package com.linkedin.data.protobuf;

/**
 * A container for holding text data in the form of a char[] to minimize allocations when parsing strings.
 */
public final class TextBuffer
{
  private char[] _buffer;

  /**
   * Constructor
   *
   * @param initialSize  The initial size of the buffer instantiated from the pool.
   */
  public TextBuffer(int initialSize)
  {
    _buffer = new char[initialSize];
  }

  /**
   * Get a buffer of the given size from this instance. If the underlying instance size greater than or equal
   * to the requested size, the underlying buffer is returned as is. Else, the existing buffer is set to null, and
   * a new buffer of the given size is allocated afresh and returned.
   */
  public char[] getBuf(int size)
  {
    if (_buffer == null)
    {
      throw new IllegalStateException("Buffer already in use or closed.");
    }

    if (_buffer.length >= size)
    {
      char[] buffer = _buffer;
      _buffer = null;
      return buffer;
    }

    _buffer = null;
    return new char[size];
  }

  /**
   * Get a underlying buffer from this instance.
   */
  public char[] getBuf()
  {
    if (_buffer == null)
    {
      throw new IllegalStateException("Buffer already in use or closed.");
    }

    char[] buffer = _buffer;
    _buffer = null;
    return buffer;
  }

  /**
   * Return the buffer back to this instance.
   */
  public void returnBuf(char[] buffer)
  {
    if (_buffer != null)
    {
      throw new IllegalStateException("Buffer return attempted when buffer not in use.");
    }

    _buffer = buffer;
  }
}

