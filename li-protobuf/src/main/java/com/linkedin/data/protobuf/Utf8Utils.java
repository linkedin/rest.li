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

import java.io.IOException;


/**
 * A set of low-level, high-performance static utility methods related to the UTF-8 character
 * encoding.
 */
public class Utf8Utils
{
  /**
   * UTF-8 lookup table.
   *
   * Bytes representing ASCII characters return 0.
   * Bytes representing multibyte characters return the number of bytes they represent.
   * Invalid UTF-8 bytes return -1.
   */
  private final static int[] UTF8_LOOKUP_TABLE;

  static
  {
    final int[] table = new int[256];

    for (int c = 128; c < 256; ++c)
    {
      int code;

      // Store bytes needed for decoding.
      if ((c & 0xE0) == 0xC0)
      { // 2 bytes (0x0080 - 0x07FF)
        code = 2;
      }
      else if ((c & 0xF0) == 0xE0)
      { // 3 bytes (0x0800 - 0xFFFF)
        code = 3;
      }
      else if ((c & 0xF8) == 0xF0)
      {
        // 4 bytes; double-char with surrogates.
        code = 4;
      }
      else
      {
        // -1 for error marker.
        code = -1;
      }
      table[c] = code;
    }
    UTF8_LOOKUP_TABLE = table;
  }

  private static final IllegalArgumentException INVALID_UTF8_EXCEPTION = new IllegalArgumentException("Invalid UTF-8");

  /**
   * Encodes an input character sequence ({@code in}) to UTF-8 in the target array ({@code out}).
   * For a string, this method is similar to
   *
   * <pre>{@code
   * byte[] a = string.getBytes(UTF_8);
   * System.arraycopy(a, 0, bytes, offset, a.length);
   * return offset + a.length;
   * }</pre>
   * <p>
   * but is more efficient in both time and space. One key difference is that this method requires
   * paired surrogates, and therefore does not support chunking. While {@code
   * String.getBytes(UTF_8)} replaces unpaired surrogates with the default replacement character,
   * this method throws {@link IllegalArgumentException}.
   *
   * <p>To ensure sufficient space in the output buffer, either call {@link #encodedLength} to
   * compute the exact amount needed, or leave room for {@code Utf8.MAX_BYTES_PER_CHAR *
   * sequence.length()}, which is the largest possible number of bytes that any input can be
   * encoded to.
   *
   * @param in     the input character sequence to be encoded
   * @param out    the target array
   * @param offset the starting offset in {@code bytes} to start writing at
   * @param length the length of the {@code bytes}, starting from {@code offset}
   * @return the new offset, equivalent to {@code offset + Utf8.encodedLength(sequence)}
   * @throws IllegalArgumentException       if {@code sequence} contains ill-formed UTF-16 (unpaired
   *                                        surrogates)
   * @throws ArrayIndexOutOfBoundsException if {@code sequence} encoded in UTF-8 is longer than
   *                                        {@code bytes.length - offset}
   */
  public static int encode(CharSequence in, byte[] out, int offset, int length)
  {
    int utf16Length = in.length();
    int j = offset;
    int i = 0;
    int limit = offset + length;

    // Designed to take advantage of
    // https://wiki.openjdk.java.net/display/HotSpotInternals/RangeCheckElimination
    for (char c; i < utf16Length && i + j < limit && (c = in.charAt(i)) < 0x80; i++)
    {
      out[j + i] = (byte) c;
    }

    if (i == utf16Length)
    {
      return j + utf16Length;
    }

    j += i;
    for (char c; i < utf16Length; i++)
    {
      c = in.charAt(i);
      if (c < 0x80 && j < limit)
      {
        out[j++] = (byte) c;
      }
      else if (c < 0x800 && j <= limit - 2)
      {
        // 11 bits, two UTF-8 bytes
        out[j++] = (byte) ((0xF << 6) | (c >>> 6));
        out[j++] = (byte) (0x80 | (0x3F & c));
      }
      else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c) && j <= limit - 3)
      {
        // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
        out[j++] = (byte) ((0xF << 5) | (c >>> 12));
        out[j++] = (byte) (0x80 | (0x3F & (c >>> 6)));
        out[j++] = (byte) (0x80 | (0x3F & c));
      }
      else if (j <= limit - 4)
      {
        // Minimum code point represented by a surrogate pair is 0x10000, 17 bits,
        // four UTF-8 bytes
        final char low;
        if (i + 1 == in.length() || !Character.isSurrogatePair(c, (low = in.charAt(++i))))
        {
          throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1) + " of " + utf16Length);
        }
        int codePoint = Character.toCodePoint(c, low);
        out[j++] = (byte) ((0xF << 4) | (codePoint >>> 18));
        out[j++] = (byte) (0x80 | (0x3F & (codePoint >>> 12)));
        out[j++] = (byte) (0x80 | (0x3F & (codePoint >>> 6)));
        out[j++] = (byte) (0x80 | (0x3F & codePoint));
      }
      else
      {
        // If we are surrogates and we're not a surrogate pair, always throw an
        // UnpairedSurrogateException instead of an ArrayOutOfBoundsException.
        if ((Character.MIN_SURROGATE <= c && c <= Character.MAX_SURROGATE)
            && (i + 1 == in.length() || !Character.isSurrogatePair(c, in.charAt(i + 1))))
        {
          throw new IllegalArgumentException("Unpaired surrogate at index " + i + " of " + utf16Length);
        }
        throw new ArrayIndexOutOfBoundsException("Failed writing " + c + " at index " + j);
      }
    }
    return j;
  }

  /**
   * Returns the number of bytes in the UTF-8-encoded form of {@code sequence}. For a string, this
   * method is equivalent to {@code string.getBytes(UTF_8).length}, but is more efficient in both
   * time and space.
   *
   * @throws IllegalArgumentException if {@code sequence} contains ill-formed UTF-16 (unpaired
   *                                  surrogates)
   */
  public static int encodedLength(CharSequence sequence)
  {
    // Warning to maintainers: this implementation is highly optimized.
    int utf16Length = sequence.length();
    int utf8Length = utf16Length;
    int i = 0;

    // This loop optimizes for pure ASCII.
    while (i < utf16Length && sequence.charAt(i) < 0x80)
    {
      i++;
    }

    // This loop optimizes for chars less than 0x800.
    for (; i < utf16Length; i++)
    {
      char c = sequence.charAt(i);
      if (c < 0x800)
      {
        utf8Length += ((0x7f - c) >>> 31); // branch free!
      }
      else
      {
        utf8Length += encodedLengthGeneral(sequence, i);
        break;
      }
    }

    if (utf8Length < utf16Length)
    {
      // Necessary and sufficient condition for overflow because of maximum 3x expansion
      throw new IllegalArgumentException(
          "UTF-8 length does not fit in int: " + (utf8Length + (1L << 32)));
    }
    return utf8Length;
  }

  private static int encodedLengthGeneral(CharSequence sequence, int start)
  {
    int utf16Length = sequence.length();
    int utf8Length = 0;
    for (int i = start; i < utf16Length; i++)
    {
      char c = sequence.charAt(i);
      if (c < 0x800)
      {
        utf8Length += (0x7f - c) >>> 31; // branch free!
      }
      else
      {
        utf8Length += 2;
        // jdk7+: if (Character.isSurrogate(c)) {
        if (Character.MIN_SURROGATE <= c && c <= Character.MAX_SURROGATE)
        {
          // Check that we have a well-formed surrogate pair.
          int cp = Character.codePointAt(sequence, i);
          if (cp < Character.MIN_SUPPLEMENTARY_CODE_POINT)
          {
            throw new IllegalArgumentException("Unpaired surrogate at index " + i + " of " + utf16Length);
          }
          i++;
        }
      }
    }
    return utf8Length;
  }

  /**
   * Decodes the given ASCII encoded byte array slice into a {@link String}.
   */
  public static String decodeASCII(byte[] bytes, int index, int size, TextBuffer textBuffer)
  {
    int offset = index;
    final int limit = offset + size;

    // Reuse buffers to avoid thrashing due to transient allocs.
    char[] resultArr = null;
    try
    {
      resultArr = textBuffer.getBuf(size);
      int resultPos = 0;
      while (offset < limit) {
        resultArr[resultPos++] = (char) bytes[offset++];
      }
      return new String(resultArr, 0, size);
    }
    finally
    {
      textBuffer.returnBuf(resultArr);
    }
  }

  /**
   * Decodes a long ASCII encoded byte source that spans multiple byte array chunks into a {@link String}.
   */
  public static String decodeLongASCII(LongDecoderState state, int size, TextBuffer textBuffer) throws IOException
  {
    // Reuse buffers to avoid thrashing due to transient allocs.
    char[] resultArr = null;
    try
    {
      resultArr = textBuffer.getBuf(size);
      int resultPos = 0;

      byte[] buffer = state._buffer;
      int position = state._position;
      int limit = state._offset + state._bufferSize;

      while (resultPos < size)
      {
        if (position >= limit)
        {
          state.readNextChunk();
          buffer = state._buffer;
          position = state._position;
          limit = state._offset + state._bufferSize;
        }

        while (position < limit && resultPos < size)
        {
          resultArr[resultPos++] = (char) buffer[position++];
        }
      }

      state._position = position;
      return new String(resultArr, 0, resultPos);
    }
    finally
    {
      textBuffer.returnBuf(resultArr);
    }
  }

  /**
   * Decodes the given UTF-8 encoded byte array slice into a {@link String}.
   *
   * @throws IllegalArgumentException if the input is not valid UTF-8.
   *
   * @deprecated Use {@link #decode(byte[], int, int, TextBuffer)} instead, re-using the same TextBuffer between
   * invocations, as much as possible.
   */
  @Deprecated
  public static String decode(byte[] bytes, int index, int size)
  {
    return decode(bytes, index, size, new TextBuffer(ProtoReader.DEFAULT_TEXT_BUFFER_SIZE));
  }

  /**
   * Decodes the given UTF-8 encoded byte array slice into a {@link String}.
   *
   * @throws IllegalArgumentException if the input is not valid UTF-8.
   */
  public static String decode(byte[] bytes, int index, int size, TextBuffer textBuffer)
  {
    int offset = index;
    final int limit = offset + size;

    // The longest possible resulting String is the same as the number of input bytes, when it is
    // all ASCII. For other cases, this over-allocates and we will truncate in the end. Use a pooled
    // buffer here to avoid thrashing due to transient allocs.
    char[] resultArr = null;

    try
    {
      resultArr = textBuffer.getBuf(size);
      int resultPos = 0;

      while (offset < limit)
      {
        int i = bytes[offset++] & 0xff;
        switch (UTF8_LOOKUP_TABLE[i])
        {
          case 0:
            // ASCII. Nothing to do, since byte is same as char.
            break;
          case 2:
            // 2 byte unicode
            i = ((i & 0x1F) << 6) | (bytes[offset++] & 0x3F);
            break;
          case 3:
            // 3 byte unicode
            i = ((i & 0x0F) << 12) | ((bytes[offset++] & 0x3F) << 6) | (bytes[offset++] & 0x3F);
            break;
          case 4:
            // 4 byte unicode
            i = ((i & 0x07) << 18) | ((bytes[offset++] & 0x3F) << 12) | ((bytes[offset++] & 0x3F) << 6) | (bytes[offset++] & 0x3F);
            // Split the codepoint
            i -= 0x10000;
            resultArr[resultPos++] = (char) (0xD800 | (i >> 10));
            i = 0xDC00 | (i & 0x3FF);
            break;
          default:
            throw new IllegalArgumentException("Invalid UTF-8. UTF-8 character cannot be " + UTF8_LOOKUP_TABLE[i] + "bytes");
        }
        resultArr[resultPos++] = (char) i;
      }

      return new String(resultArr, 0, resultPos);
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      throw new IllegalArgumentException("Invalid UTF-8. Unterminated multi-byte sequence", e);
    }
    finally
    {
      textBuffer.returnBuf(resultArr);
    }
  }

  /**
   * Decodes the given long UTF-8 encoded byte source that spans across multiple byte array chunks into a
   * {@link String}.
   *
   * <p>The loops in the multi-byte sections are intentionally hand unrolled here for performance reasons.</p>
   *
   * @throws IllegalArgumentException if the input is not valid UTF-8.
   */
  public static String decodeLong(LongDecoderState state, int size, TextBuffer textBuffer) throws IOException
  {
    // The longest possible resulting String is the same as the number of input bytes, when it is
    // all ASCII. For other cases, this over-allocates and we will truncate in the end. Use a pooled
    // buffer here to avoid thrashing due to transient allocs.
    char[] resultArr = null;

    try
    {
      resultArr = textBuffer.getBuf(size);
      int resultPos = 0;

      byte[] buffer = state._buffer;
      int position = state._position;
      int limit = state._offset + state._bufferSize;
      int totalBytesRead = 0;

      while (totalBytesRead < size)
      {
        if (position >= limit)
        {
          state.readNextChunk();
          buffer = state._buffer;
          position = state._position;
          limit = state._offset + state._bufferSize;
        }

        int i = buffer[position++] & 0xff;
        switch (UTF8_LOOKUP_TABLE[i])
        {
          case 0:
            // ASCII. Nothing to do, since byte is same as char.
            totalBytesRead++;
            break;
          case 2:
            // 2 byte unicode
            if (position >= limit)
            {
              state.readNextChunk();
              buffer = state._buffer;
              position = state._position;
              limit = state._offset + state._bufferSize;
            }
            i = ((i & 0x1F) << 6) | (buffer[position++] & 0x3F);
            totalBytesRead += 2;
            break;
          case 3:
            // 3 byte unicode
            if (position < limit -1)
            {
              i = ((i & 0x0F) << 12) | ((buffer[position++] & 0x3F) << 6) | (buffer[position++] & 0x3F);
            }
            else
            {
              byte byte2, byte3;
              if (position >= limit)
              {
                state.readNextChunk();
                buffer = state._buffer;
                position = state._position;
                limit = state._offset + state._bufferSize;
              }
              byte2 = buffer[position++];

              if (position >= limit)
              {
                state.readNextChunk();
                buffer = state._buffer;
                position = state._position;
                limit = state._offset + state._bufferSize;
              }
              byte3 = buffer[position++];
              i = ((i & 0x0F) << 12) | ((byte2 & 0x3F) << 6) | (byte3 & 0x3F);
            }
            totalBytesRead += 3;
            break;
          case 4:
            // 4 byte unicode
            if (position < limit - 2)
            {
              i = ((i & 0x07) << 18) | ((buffer[position++] & 0x3F) << 12) | ((buffer[position++] & 0x3F) << 6) | (buffer[position++] & 0x3F);
            }
            else
            {
              byte byte2, byte3, byte4;
              if (position >= limit)
              {
                state.readNextChunk();
                buffer = state._buffer;
                position = state._position;
                limit = state._offset + state._bufferSize;
              }
              byte2 = buffer[position++];

              if (position >= limit)
              {
                state.readNextChunk();
                buffer = state._buffer;
                position = state._position;
                limit = state._offset + state._bufferSize;
              }
              byte3 = buffer[position++];

              if (position >= limit)
              {
                state.readNextChunk();
                buffer = state._buffer;
                position = state._position;
                limit = state._offset + state._bufferSize;
              }
              byte4 = buffer[position++];

              i = ((i & 0x07) << 18) | ((byte2 & 0x3F) << 12) | ((byte3 & 0x3F) << 6) | (byte4 & 0x3F);
            }
            // Split the codepoint
            i -= 0x10000;
            resultArr[resultPos++] = (char) (0xD800 | (i >> 10));
            i = 0xDC00 | (i & 0x3FF);
            totalBytesRead += 4;
            break;
          default:
            throw new IllegalArgumentException("Invalid UTF-8. UTF-8 character cannot be " + UTF8_LOOKUP_TABLE[i] + "bytes");
        }
        resultArr[resultPos++] = (char) i;
      }

      state._position = position;
      return new String(resultArr, 0, resultPos);
    }
    finally
    {
      textBuffer.returnBuf(resultArr);
    }
  }

  /**
   * Class to maintain state when decoding a {@link String} from a byte source spanning multiple byte array chunks.
   */
  public static abstract class LongDecoderState
  {
    protected byte[] _buffer;
    protected int _offset;
    protected int _position;
    protected int _bufferSize;

    public abstract void readNextChunk() throws IOException;

    public byte[] getBuffer()
    {
      return _buffer;
    }

    public int getOffset()
    {
      return _offset;
    }

    public int getPosition()
    {
      return _position;
    }

    public int getBufferSize()
    {
      return _bufferSize;
    }
  }
}
