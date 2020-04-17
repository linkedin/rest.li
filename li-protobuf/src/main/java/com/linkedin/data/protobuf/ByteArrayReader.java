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
import java.util.Arrays;


/**
 * A {@link ProtoReader} implementation that uses a backing array as the input.
 */
final class ByteArrayReader extends ProtoReader
{
  private final byte[] _buffer;
  private int _limit;
  private int _pos;

  ByteArrayReader(final byte[] buffer, final int offset, final int len)
  {
    _buffer = buffer;
    _limit = offset + len;
    _pos = offset;
  }

  @Override
  public String readASCIIString() throws IOException {
    final int size = readInt32();
    if (size > 0 && size <= (_limit - _pos))
    {
      String result = Utf8Utils.decodeASCII(_buffer, _pos, size, _textBuffer);
      _pos += size;
      return result;
    }

    if (size == 0)
    {
      return "";
    }
    throw new EOFException();
  }

  @Override
  public String readString() throws IOException
  {
    final int size = readInt32();
    if (size > 0 && size <= (_limit - _pos))
    {
      String result = Utf8Utils.decode(_buffer, _pos, size, _textBuffer);
      _pos += size;
      return result;
    }

    if (size == 0)
    {
      return "";
    }
    throw new EOFException();
  }

  @Override
  public byte[] readByteArray() throws IOException
  {
    final int length = readInt32();
    if (length > 0 && length <= (_limit - _pos))
    {
      final int tempPos = _pos;
      _pos += length;
      return Arrays.copyOfRange(_buffer, tempPos, _pos);
    }

    if (length == 0)
    {
      return new byte[0];
    }

    throw new EOFException();
  }

  @Override
  public int readInt32() throws IOException
  {
    // See implementation notes for readInt64
    fastpath:
    {
      int tempPos = _pos;

      if (_limit == tempPos)
      {
        break fastpath;
      }

      final byte[] buffer = this._buffer;
      int x;
      if ((x = buffer[tempPos++]) >= 0)
      {
        _pos = tempPos;
        return x;
      }
      else if (_limit - tempPos < 9)
      {
        break fastpath;
      }
      else if ((x ^= (buffer[tempPos++] << 7)) < 0)
      {
        x ^= (~0 << 7);
      }
      else if ((x ^= (buffer[tempPos++] << 14)) >= 0)
      {
        x ^= (~0 << 7) ^ (~0 << 14);
      }
      else if ((x ^= (buffer[tempPos++] << 21)) < 0)
      {
        x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
      }
      else
      {
        int y = buffer[tempPos++];
        x ^= y << 28;
        x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
        if (y < 0
            && buffer[tempPos++] < 0
            && buffer[tempPos++] < 0
            && buffer[tempPos++] < 0
            && buffer[tempPos++] < 0
            && buffer[tempPos++] < 0)
        {
          break fastpath; // Will throw malformedVarint()
        }
      }
      _pos = tempPos;
      return x;
    }
    return (int) readRawVarint64SlowPath();
  }

  @Override
  public long readInt64() throws IOException
  {
    // Implementation notes:
    //
    // Optimized for one-byte values, expected to be common.
    // The particular code below was selected from various candidates
    // empirically, by winning VarintBenchmark.
    //
    // Sign extension of (signed) Java bytes is usually a nuisance, but
    // we exploit it here to more easily obtain the sign of bytes read.
    // Instead of cleaning up the sign extension bits by masking eagerly,
    // we delay until we find the final (positive) byte, when we clear all
    // accumulated bits with one xor.  We depend on javac to constant fold.
    fastpath:
    {
      int tempPos = _pos;

      if (_limit == tempPos)
      {
        break fastpath;
      }

      final byte[] buffer = this._buffer;
      long x;
      int y;
      if ((y = buffer[tempPos++]) >= 0)
      {
        _pos = tempPos;
        return y;
      }
      else if (_limit - tempPos < 9)
      {
        break fastpath;
      }
      else if ((y ^= (buffer[tempPos++] << 7)) < 0)
      {
        x = y ^ (~0 << 7);
      }
      else if ((y ^= (buffer[tempPos++] << 14)) >= 0)
      {
        x = y ^ ((~0 << 7) ^ (~0 << 14));
      }
      else if ((y ^= (buffer[tempPos++] << 21)) < 0)
      {
        x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
      }
      else if ((x = y ^ ((long) buffer[tempPos++] << 28)) >= 0L)
      {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
      }
      else if ((x ^= ((long) buffer[tempPos++] << 35)) < 0L)
      {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
      }
      else if ((x ^= ((long) buffer[tempPos++] << 42)) >= 0L)
      {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
      }
      else if ((x ^= ((long) buffer[tempPos++] << 49)) < 0L)
      {
        x ^=
            (~0L << 7)
                ^ (~0L << 14)
                ^ (~0L << 21)
                ^ (~0L << 28)
                ^ (~0L << 35)
                ^ (~0L << 42)
                ^ (~0L << 49);
      }
      else
      {
        x ^= ((long) buffer[tempPos++] << 56);
        x ^=
            (~0L << 7)
                ^ (~0L << 14)
                ^ (~0L << 21)
                ^ (~0L << 28)
                ^ (~0L << 35)
                ^ (~0L << 42)
                ^ (~0L << 49)
                ^ (~0L << 56);
        if (x < 0L)
        {
          if (buffer[tempPos++] < 0L)
          {
            break fastpath; // Will throw malformedVarint()
          }
        }
      }
      _pos = tempPos;
      return x;
    }
    return readRawVarint64SlowPath();
  }

  long readRawVarint64SlowPath() throws IOException
  {
    long result = 0;
    for (int shift = 0; shift < 64; shift += 7)
    {
      final byte b = readRawByte();
      result |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0)
      {
        return result;
      }
    }
    throw new IOException("Malformed VarInt");
  }

  @Override
  public byte readRawByte() throws IOException
  {
    if (_pos == _limit)
    {
      throw new EOFException();
    }
    return _buffer[_pos++];
  }
}
