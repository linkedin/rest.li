/*
   Copyright (c) 2019 LinkedIn Corp.

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

// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.linkedin.data.protobuf;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;


/**
 * Utility class for writing Protocol Buffers encoded binary data.
 */
public class ProtoWriter
{
  public static final int FIXED32_SIZE = 4;
  public static final int FIXED64_SIZE = 8;
  private static final int MAX_VARINT32_SIZE = 5;
  private static final int MAX_VARINT64_SIZE = 10;
  private static final int DEFAULT_BUFFER_SIZE = 4096;

  private final OutputStream _out;
  private final byte[] _buffer;
  private final int _limit;
  private int _position;

  /**
   * Create a new {@code ProtoWriter} wrapping the given {@code OutputStream}.
   */
  public ProtoWriter(OutputStream out)
  {
    _out = out;
    _buffer = new byte[DEFAULT_BUFFER_SIZE];
    _limit = DEFAULT_BUFFER_SIZE;
  }

  /**
   * Write a single byte.
   */
  public void writeByte(final byte value) throws IOException
  {
    if (_position == _limit)
    {
      flush();
    }

    buffer(value);
  }

  /**
   * Write a byte array.
   */
  public void writeBytes(final byte[] value) throws IOException
  {
    writeBytes(value, 0, value.length);
  }

  /**
   * Write a byte array slice.
   */
  public void writeBytes(byte[] value, int offset, int length) throws IOException
  {
    if (_limit - _position >= length)
    {
      // We have room in the current buffer.
      System.arraycopy(value, offset, _buffer, _position, length);
      _position += length;
    }
    else
    {
      // Write extends past current buffer.  Fill the rest of this buffer and
      // flush.
      final int bytesWritten = _limit - _position;
      System.arraycopy(value, offset, _buffer, _position, bytesWritten);
      offset += bytesWritten;
      length -= bytesWritten;
      _position = _limit;
      flush();

      // Now deal with the rest.
      // Since we have an output stream, this is our buffer
      // and buffer offset == 0
      if (length <= _limit)
      {
        // Fits in new buffer.
        System.arraycopy(value, offset, _buffer, 0, length);
        _position = length;
      }
      else
      {
        // Write is very big.  Let's do it all at once.
        _out.write(value, offset, length);
      }
    }
  }

  /**
   * Write a fixed length 32-bit signed integer.
   */
  public final void writeFixedInt32(final int value) throws IOException
  {
    flushIfNotAvailable(FIXED32_SIZE);
    _buffer[_position++] = (byte) (value & 0xFF);
    _buffer[_position++] = (byte) ((value >> 8) & 0xFF);
    _buffer[_position++] = (byte) ((value >> 16) & 0xFF);
    _buffer[_position++] = (byte) ((value >> 24) & 0xFF);
  }

  /**
   * Write a variable length 32-bit signed integer.
   */
  public final void writeInt32(final int value) throws IOException
  {
    if (value >= 0)
    {
      writeUInt32(value);
    }
    else
    {
      // Must sign-extend.
      writeUInt64(value);
    }
  }

  /**
   * Write a fixed length 64-bit signed integer.
   */
  public final void writeFixedInt64(final long value) throws IOException
  {
    flushIfNotAvailable(FIXED64_SIZE);
    _buffer[_position++] = (byte) ((int) (value) & 0xFF);
    _buffer[_position++] = (byte) ((int) (value >> 8) & 0xFF);
    _buffer[_position++] = (byte) ((int) (value >> 16) & 0xFF);
    _buffer[_position++] = (byte) ((int) (value >> 24) & 0xFF);
    _buffer[_position++] = (byte) ((int) (value >> 32) & 0xFF);
    _buffer[_position++] = (byte) ((int) (value >> 40) & 0xFF);
    _buffer[_position++] = (byte) ((int) (value >> 48) & 0xFF);
    _buffer[_position++] = (byte) ((int) (value >> 56) & 0xFF);
  }

  /**
   * Write a variable length 64-bit signed integer.
   */
  public final void writeInt64(final long value) throws IOException
  {
    writeUInt64(value);
  }

  /**
   * Compute the number of bytes that would be needed to encode an unsigned 32-bit integer.
   */
  private static int computeUInt32Size(final int value)
  {
    if ((value & (~0 << 7)) == 0)
    {
      return 1;
    }

    if ((value & (~0 << 14)) == 0)
    {
      return 2;
    }

    if ((value & (~0 << 21)) == 0)
    {
      return 3;
    }

    if ((value & (~0 << 28)) == 0)
    {
      return 4;
    }

    return 5;
  }

  private void buffer(byte value) throws IOException
  {
    _buffer[_position++] = value;
  }

  /**
   * Flush any buffered data to the underlying outputstream.
   */
  public void flush() throws IOException
  {
    _out.write(_buffer, 0, _position);
    _position = 0;
  }

  private void flushIfNotAvailable(int requiredSize) throws IOException
  {
    if (_limit - _position < requiredSize)
    {
      flush();
    }
  }

  /**
   * Write a variable length 32-bit unsigned integer.
   */
  public void writeUInt32(int value) throws IOException
  {
    flushIfNotAvailable(MAX_VARINT32_SIZE);
    bufferUInt32(value);
  }

  private void bufferUInt32(int value) throws IOException
  {
    while (true)
    {
      if ((value & ~0x7F) == 0)
      {
        _buffer[_position++] = (byte) value;
        return;
      }
      else
      {
        _buffer[_position++] = (byte) ((value & 0x7F) | 0x80);
        value >>>= 7;
      }
    }
  }

  /**
   * Write a variable length 64-bit unsigned integer.
   */
  public void writeUInt64(long value) throws IOException
  {
    flushIfNotAvailable(MAX_VARINT64_SIZE);
    bufferUInt64(value);
  }

  private void bufferUInt64(long value) throws IOException
  {
    while (true)
    {
      if ((value & ~0x7FL) == 0)
      {
        _buffer[_position++] = (byte) value;
        return;
      }
      else
      {
        _buffer[_position++] = (byte) (((int) value & 0x7F) | 0x80);
        value >>>= 7;
      }
    }
  }

  /**
   * Write a String without any leading ordinal.
   */
  public void writeString(String value) throws IOException
  {
    writeString(value, null);
  }

  /**
   * Write a String.
   */
  public void writeString(String value, Function<Integer, Byte> leadingOrdinalGenerator) throws IOException
  {
    // Based on whether a leading ordinal generator is provided or not, we need to budget 0 or 1 byte.
    final int leadingOrdinalLength = (leadingOrdinalGenerator == null) ? 0 : 1;

    // UTF-8 byte length of the string is at least its UTF-16 code unit length (value.length()),
    // and at most 3 times of it. We take advantage of this in both branches below.
    final int maxLength = value.length() * 3;
    final int maxLengthVarIntSize = computeUInt32Size(maxLength);

    // If we are streaming and the potential length is too big to fit in our buffer, we take the
    // slower path.
    if (maxLengthVarIntSize + maxLength + leadingOrdinalLength > _limit)
    {
      // Allocate a byte[] that we know can fit the string and encode into it. String.getBytes()
      // does the same internally and then does *another copy* to return a byte[] of exactly the
      // right size. We can skip that copy and just writeRawBytes up to the actualLength of the
      // UTF-8 encoded bytes.
      final byte[] encodedBytes = new byte[maxLength];
      int actualLength = Utf8Utils.encode(value, encodedBytes, 0, maxLength);

      if (leadingOrdinalGenerator != null)
      {
        writeByte(leadingOrdinalGenerator.apply(actualLength));
      }

      writeUInt32(actualLength);
      writeBytes(encodedBytes, 0, actualLength);
      return;
    }

    // Fast path: we have enough space available in our buffer for the string...
    if (maxLengthVarIntSize + maxLength + leadingOrdinalLength > _limit - _position)
    {
      // Flush to free up space.
      flush();
    }

    final int oldPosition = _position;
    try
    {
      // Optimize for the case where we know this length results in a constant varint length as
      // this saves a pass for measuring the length of the string.
      final int minLengthVarIntSize = computeUInt32Size(value.length());

      if (minLengthVarIntSize == maxLengthVarIntSize)
      {
        _position = oldPosition + leadingOrdinalLength + minLengthVarIntSize;
        int newPosition = Utf8Utils.encode(value, _buffer, _position, _limit - _position);
        // Since this class is stateful and tracks the position, we rewind and store the state,
        // prepend the length, then reset it back to the end of the string.
        _position = oldPosition;
        int length = newPosition - oldPosition - leadingOrdinalLength - minLengthVarIntSize;

        if (leadingOrdinalGenerator != null)
        {
          buffer(leadingOrdinalGenerator.apply(length));
        }

        bufferUInt32(length);
        _position = newPosition;
      }
      else
      {
        int length = Utf8Utils.encodedLength(value);

        if (leadingOrdinalGenerator != null)
        {
          buffer(leadingOrdinalGenerator.apply(length));
        }

        bufferUInt32(length);
        _position = Utf8Utils.encode(value, _buffer, _position, length);
      }
    }
    catch (IllegalArgumentException e)
    {
      throw new IOException(e);
    }
    catch (IndexOutOfBoundsException e)
    {
      throw new EOFException(String.format("Pos: %d, limit: %d, len: %d", _position, _limit, 1));
    }
  }
}
