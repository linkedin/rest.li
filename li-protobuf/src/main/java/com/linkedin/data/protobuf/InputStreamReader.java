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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Implementation of {@link ProtoReader} that uses an {@link InputStream} as the data source.
 */
final class InputStreamReader extends ProtoReader
{
  private final InputStream _input;
  private final byte[] _buffer;

  /**
   * Represents how many bytes are currently filled in the _buffer
   */
  private int _bufferSize;

  private int _bufferSizeAfterLimit;
  private int _pos;

  /**
   * The total number of bytes read before the current _buffer. The total bytes read up to the
   * current position can be computed as {@code _totalBytesRetired + _pos}.
   */
  private int _totalBytesRetired;

  /**
   * The absolute position of the end of the current message.
   */
  private int _currentLimit = Integer.MAX_VALUE;

  InputStreamReader(final InputStream input, int bufferSize)
  {
    _input = input;
    _buffer = new byte[bufferSize];
    _bufferSize = 0;
    _pos = 0;
    _totalBytesRetired = 0;
  }

  @Override
  public String readASCIIString() throws IOException {
    final int size = readInt32();
    if (size > 0)
    {
      // If we can fit into a buffer, read directly off the buffer,
      if (size < _bufferSize)
      {
        // Slow path: We can fit into a buffer, but there aren't enough bytes available in the current buffer.
        // Refill!
        if (size > (_bufferSize - _pos))
        {
          refillBuffer(size);
        }

        String value = Utf8Utils.decodeASCII(_buffer, _pos, size, _textBuffer);
        _pos += size;
        return value;
      }
      else
      {
        Utf8Utils.LongDecoderState state = new InputStreamLongDecoderState(_buffer, _pos, _bufferSize, _input);
        String value = Utf8Utils.decodeLongASCII(state, size, _textBuffer);
        _pos = state.getPosition();
        _bufferSize = state.getBufferSize();
        return value;
      }
    }
    else if (size == 0)
    {
      return "";
    }
    else
    {
      throw new IOException("Read negative size: " + size + ". Invalid string");
    }
  }

  @Override
  public String readString() throws IOException
  {
    final int size = readInt32();
    if (size > 0)
    {
      // If we can fit into a buffer, read directly off the buffer,
      if (size < _bufferSize)
      {
        // Slow path: We can fit into a buffer, but there aren't enough bytes available in the current buffer.
        // Refill!
        if (size > (_bufferSize - _pos))
        {
          refillBuffer(size);
        }

        String value = Utf8Utils.decode(_buffer, _pos, size, _textBuffer);
        _pos += size;
        return value;
      }
      else
      {
        Utf8Utils.LongDecoderState state = new InputStreamLongDecoderState(_buffer, _pos, _bufferSize, _input);
        String value = Utf8Utils.decodeLong(state, size, _textBuffer);
        _pos = state.getPosition();
        _bufferSize = state.getBufferSize();
        return value;
      }
    }
    else if (size == 0)
    {
      return "";
    }
    else
    {
      throw new IOException("Read negative size: " + size + ". Invalid string");
    }
  }

  @Override
  public byte[] readByteArray() throws IOException
  {
    final int size = readInt32();
    if (size <= (_bufferSize - _pos) && size > 0)
    {
      // Fast path: We already have the bytes in a contiguous _buffer, so
      // just copy directly from it.
      final byte[] result = Arrays.copyOfRange(_buffer, _pos, _pos + size);
      _pos += size;
      return result;
    }
    else
    {
      // Slow path: Build a byte array first then copy it.
      return readRawBytesSlowPath(size);
    }
  }

  @Override
  public int readInt32() throws IOException
  {
    // See implementation notes for readInt64
    fastpath:
    {
      int tempPos = _pos;

      if (_bufferSize == tempPos)
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
      else if (_bufferSize - tempPos < 9)
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

      if (_bufferSize == tempPos)
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
      else if (_bufferSize - tempPos < 9)
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

  private void recomputeBufferSizeAfterLimit()
  {
    _bufferSize += _bufferSizeAfterLimit;
    final int bufferEnd = _totalBytesRetired + _bufferSize;
    if (bufferEnd > _currentLimit)
    {
      // Limit is in current _buffer.
      _bufferSizeAfterLimit = bufferEnd - _currentLimit;
      _bufferSize -= _bufferSizeAfterLimit;
    }
    else
    {
      _bufferSizeAfterLimit = 0;
    }
  }

  private interface RefillCallback
  {
    void onRefill();
  }

  private RefillCallback refillCallback = null;

  /**
   * Reads more bytes from the _input, making at least {@code n} bytes available in the _buffer.
   * Caller must ensure that the requested space is not yet available, and that the requested
   * space is less than BUFFER_SIZE.
   *
   * @throws EOFException The end of the stream or the current _limit was reached.
   */
  private void refillBuffer(int n) throws IOException
  {
    if (!tryRefillBuffer(n))
    {
      throw new EOFException();
    }
  }

  /**
   * Tries to read more bytes from the _input, making at least {@code n} bytes available in the
   * _buffer. Caller must ensure that the requested space is not yet available, and that the
   * requested space is less than BUFFER_SIZE.
   *
   * @return {@code true} If the bytes could be made available; {@code false} 1. Current at the
   * end of the stream 2. The current _limit was reached 3. The total size _limit was reached
   */
  private boolean tryRefillBuffer(int n) throws IOException
  {
    if (_pos + n <= _bufferSize)
    {
      throw new IllegalStateException(
          "refillBuffer() called when " + n + " bytes were already available in _buffer");
    }

    // Check whether the size of total message needs to read is bigger than the size _limit.
    // We shouldn't throw an exception here as isAtEnd() function needs to get this function's
    // return as the result.
    if (n > DEFAULT_SIZE_LIMIT - _totalBytesRetired - _pos)
    {
      return false;
    }

    // Shouldn't throw the exception here either.
    if (_totalBytesRetired + _pos + n > _currentLimit)
    {
      // Oops, we hit a _limit.
      return false;
    }

    if (refillCallback != null)
    {
      refillCallback.onRefill();
    }

    int tempPos = _pos;
    if (tempPos > 0)
    {
      if (_bufferSize > tempPos)
      {
        System.arraycopy(_buffer, tempPos, _buffer, 0, _bufferSize - tempPos);
      }
      _totalBytesRetired += tempPos;
      _bufferSize -= tempPos;
      _pos = 0;
    }

    // Here we should refill the _buffer as many bytes as possible.
    int bytesRead =
        _input.read(
            _buffer,
            _bufferSize,
            Math.min(
                //  the size of allocated but unused bytes in the _buffer
                _buffer.length - _bufferSize,
                //  do not exceed the total bytes _limit
                DEFAULT_SIZE_LIMIT - _totalBytesRetired - _bufferSize));
    if (bytesRead == 0 || bytesRead < -1 || bytesRead > _buffer.length)
    {
      throw new IllegalStateException(
          _input.getClass()
              + "#read(byte[]) returned invalid result: "
              + bytesRead
              + "\nThe InputStream implementation is buggy.");
    }
    if (bytesRead > 0)
    {
      _bufferSize += bytesRead;
      recomputeBufferSizeAfterLimit();
      return (_bufferSize >= n) || tryRefillBuffer(n);
    }

    return false;
  }

  @Override
  public byte readRawByte() throws IOException
  {
    if (_pos == _bufferSize)
    {
      refillBuffer(1);
    }
    return _buffer[_pos++];
  }

  /**
   * Exactly like readRawBytes, but caller must have already checked the fast path: (size <=
   * (_bufferSize - _pos) && size > 0)
   */
  private byte[] readRawBytesSlowPath(final int size) throws IOException
  {
    // Attempt to read the data in one byte array when it's safe to do.
    byte[] result = readRawBytesSlowPathOneChunk(size);
    if (result != null)
    {
      return result;
    }

    final int originalBufferPos = _pos;
    final int bufferedBytes = _bufferSize - _pos;

    // Mark the current _buffer consumed.
    _totalBytesRetired += _bufferSize;
    _pos = 0;
    _bufferSize = 0;

    // Determine the number of bytes we need to read from the _input stream.
    int sizeLeft = size - bufferedBytes;

    // The size is very large. For security reasons we read them in small
    // chunks.
    List<byte[]> chunks = readRawBytesSlowPathRemainingChunks(sizeLeft);

    // OK, got everything.  Now concatenate it all into one _buffer.
    final byte[] bytes = new byte[size];

    // Start by copying the leftover bytes from this._buffer.
    System.arraycopy(_buffer, originalBufferPos, bytes, 0, bufferedBytes);

    // And now all the chunks.
    int tempPos = bufferedBytes;
    for (final byte[] chunk : chunks)
    {
      System.arraycopy(chunk, 0, bytes, tempPos, chunk.length);
      tempPos += chunk.length;
    }

    // Done.
    return bytes;
  }

  /**
   * Attempts to read the data in one byte array when it's safe to do. Returns null if the size to
   * read is too large and needs to be allocated in smaller chunks for security reasons.
   * <p>
   * Returns a byte[] that may have escaped to user code via InputStream APIs.
   */
  private byte[] readRawBytesSlowPathOneChunk(final int size) throws IOException
  {
    if (size == 0)
    {
      return new byte[0];
    }
    if (size < 0)
    {
      throw new EOFException();
    }

    // Integer-overflow-conscious check that the message size so far has not exceeded sizeLimit.
    int currentMessageSize = _totalBytesRetired + _pos + size;
    if (currentMessageSize - DEFAULT_SIZE_LIMIT > 0)
    {
      throw new EOFException();
    }

    // Verify that the message size so far has not exceeded _currentLimit.
    if (currentMessageSize > _currentLimit)
    {
      throw new EOFException();
    }

    final int bufferedBytes = _bufferSize - _pos;
    // Determine the number of bytes we need to read from the _input stream.
    int sizeLeft = size - bufferedBytes;
    if (sizeLeft < DEFAULT_TEXT_BUFFER_SIZE || sizeLeft <= _input.available())
    {
      // Either the bytes we need are known to be available, or the required _buffer is
      // within an allowed threshold - go ahead and allocate the _buffer now.
      final byte[] bytes = new byte[size];

      // Copy all of the buffered bytes to the result _buffer.
      System.arraycopy(_buffer, _pos, bytes, 0, bufferedBytes);
      _totalBytesRetired += _bufferSize;
      _pos = 0;
      _bufferSize = 0;

      // Fill the remaining bytes from the _input stream.
      int tempPos = bufferedBytes;
      while (tempPos < bytes.length)
      {
        int n = _input.read(bytes, tempPos, size - tempPos);
        if (n == -1)
        {
          throw new EOFException();
        }
        _totalBytesRetired += n;
        tempPos += n;
      }

      return bytes;
    }

    return null;
  }

  /**
   * Reads the remaining data in small chunks from the _input stream.
   * <p>
   * Returns a byte[] that may have escaped to user code via InputStream APIs.
   */
  private List<byte[]> readRawBytesSlowPathRemainingChunks(int sizeLeft) throws IOException
  {
    // The size is very large.  For security reasons, we can't allocate the
    // entire byte array yet.  The size comes directly from the _input, so a
    // maliciously-crafted message could provide a bogus very large size in
    // order to trick the app into allocating a lot of memory.  We avoid this
    // by allocating and reading only a small chunk at a time, so that the
    // malicious message must actually *be* extremely large to cause
    // problems.  Meanwhile, we _limit the allowed size of a message elsewhere.
    final List<byte[]> chunks = new ArrayList<>();

    while (sizeLeft > 0)
    {
      final byte[] chunk = new byte[Math.min(sizeLeft, DEFAULT_TEXT_BUFFER_SIZE)];
      int tempPos = 0;
      while (tempPos < chunk.length)
      {
        final int n = _input.read(chunk, tempPos, chunk.length - tempPos);
        if (n == -1)
        {
          throw new EOFException();
        }
        _totalBytesRetired += n;
        tempPos += n;
      }
      sizeLeft -= chunk.length;
      chunks.add(chunk);
    }

    return chunks;
  }

  private static class InputStreamLongDecoderState extends Utf8Utils.LongDecoderState
  {
    private final InputStream _inputStream;

    InputStreamLongDecoderState(byte[] buffer, int initialPosition, int bufferSize, InputStream inputStream)
    {
      _buffer = buffer;
      _position = initialPosition;
      _bufferSize = bufferSize;
      _inputStream = inputStream;
    }

    @Override
    public void readNextChunk() throws IOException
    {
      int bytesRead = _inputStream.read(_buffer, 0, _buffer.length);
      if (bytesRead == -1)
      {
        throw new EOFException();
      }

      _position = 0;
      _bufferSize = bytesRead;
    }
  }
}