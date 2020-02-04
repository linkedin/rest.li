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
   * Decodes the given UTF-8 encoded byte array slice into a {@link String}.
   *
   * @throws IllegalArgumentException if the input is not valid UTF-8.
   */
  public static String decode(byte[] bytes, int index, int size)
  {
    // Bitwise OR combines the sign bits so any negative value fails the check.
    if ((index | size | bytes.length - index - size) < 0)
    {
      throw new ArrayIndexOutOfBoundsException(
          String.format("buffer length=%d, index=%d, size=%d", bytes.length, index, size));
    }

    int offset = index;
    final int limit = offset + size;

    // The longest possible resulting String is the same as the number of input bytes, when it is
    // all ASCII. For other cases, this over-allocates and we will truncate in the end.
    char[] resultArr = new char[size];
    int resultPos = 0;

    while (offset < limit)
    {
      byte byte1 = bytes[offset++];
      int value = byte1 & 0xff;
      switch (UTF8_LOOKUP_TABLE[value])
      {
        case 0:
          DecodeUtil.handleOneByte(byte1, resultArr, resultPos++);
          break;
        case 2:
          DecodeUtil.handleTwoBytes(byte1, bytes[offset++], resultArr, resultPos++);
          break;
        case 3:
          DecodeUtil.handleThreeBytes(byte1, bytes[offset++], bytes[offset++], resultArr, resultPos++);
          break;
        case 4:
          DecodeUtil.handleFourBytes(byte1, bytes[offset++], bytes[offset++], bytes[offset++], resultArr, resultPos++);
          // 4-byte case requires two chars.
          resultPos++;
          break;
        default:
          throw INVALID_UTF8_EXCEPTION;
      }
    }

    return new String(resultArr, 0, resultPos);
  }

  /**
   * Decodes the given UTF-8 encoded section with the given size using the given {@link ProtoReader} into a
   * {@link String}.
   *
   * @throws IllegalArgumentException if the input is not valid UTF-8.
   */
  public static String decode(ProtoReader protoReader, int size) throws IOException
  {
    // The longest possible resulting String is the same as the number of input bytes, when it is
    // all ASCII. For other cases, this over-allocates and we will truncate in the end.
    char[] resultArr = new char[size];
    int resultPos = 0;

    while (size > 0)
    {
      byte byte1 = protoReader.readRawByte();
      int value = byte1 & 0xff;
      switch (UTF8_LOOKUP_TABLE[value])
      {
        case 0:
          DecodeUtil.handleOneByte(byte1, resultArr, resultPos++);
          size--;
          break;
        case 2:
          DecodeUtil.handleTwoBytes(byte1, protoReader.readRawByte(), resultArr, resultPos++);
          size -= 2;
          break;
        case 3:
          DecodeUtil.handleThreeBytes(byte1, protoReader.readRawByte(), protoReader.readRawByte(), resultArr, resultPos++);
          size -= 3;
          break;
        case 4:
          DecodeUtil.handleFourBytes(byte1, protoReader.readRawByte(), protoReader.readRawByte(), protoReader.readRawByte(), resultArr, resultPos++);
          // 4-byte case requires two chars.
          resultPos++;
          size -= 4;
          break;
        default:
          throw INVALID_UTF8_EXCEPTION;
      }
    }

    return new String(resultArr, 0, resultPos);
  }

  /**
   * Utility methods for decoding bytes into {@link String}. Callers are responsible for extracting
   * bytes (possibly using Unsafe methods), and checking remaining bytes.
   */
  private static class DecodeUtil
  {
    private static void handleOneByte(byte byte1, char[] resultArr, int resultPos)
    {
      resultArr[resultPos] = (char) byte1;
    }

    private static void handleTwoBytes(byte byte1, byte byte2, char[] resultArr, int resultPos)
    {
      // Simultaneously checks for illegal trailing-byte in leading position (<= '11000000') and
      // overlong 2-byte, '11000001'.
      if (byte1 < (byte) 0xC2 || isNotTrailingByte(byte2))
      {
        throw INVALID_UTF8_EXCEPTION;
      }

      resultArr[resultPos] = (char) (((byte1 & 0x1F) << 6) | trailingByteValue(byte2));
    }

    private static void handleThreeBytes(byte byte1, byte byte2, byte byte3, char[] resultArr, int resultPos)
    {
      if (isNotTrailingByte(byte2)
          // overlong? 5 most significant bits must not all be zero
          || (byte1 == (byte) 0xE0 && byte2 < (byte) 0xA0)
          // check for illegal surrogate codepoints
          || (byte1 == (byte) 0xED && byte2 >= (byte) 0xA0)
          || isNotTrailingByte(byte3))
      {
        throw INVALID_UTF8_EXCEPTION;
      }

      resultArr[resultPos] =
          (char)
              (((byte1 & 0x0F) << 12) | (trailingByteValue(byte2) << 6) | trailingByteValue(byte3));
    }

    private static void handleFourBytes(byte byte1, byte byte2, byte byte3, byte byte4, char[] resultArr, int resultPos)
    {
      if (isNotTrailingByte(byte2)
          // Check that 1 <= plane <= 16.  Tricky optimized form of:
          //   valid 4-byte leading byte?
          // if (byte1 > (byte) 0xF4 ||
          //   overlong? 4 most significant bits must not all be zero
          //     byte1 == (byte) 0xF0 && byte2 < (byte) 0x90 ||
          //   codepoint larger than the highest code point (U+10FFFF)?
          //     byte1 == (byte) 0xF4 && byte2 > (byte) 0x8F)
          || (((byte1 << 28) + (byte2 - (byte) 0x90)) >> 30) != 0
          || isNotTrailingByte(byte3)
          || isNotTrailingByte(byte4))
      {
        throw INVALID_UTF8_EXCEPTION;
      }

      int codepoint =
          ((byte1 & 0x07) << 18)
              | (trailingByteValue(byte2) << 12)
              | (trailingByteValue(byte3) << 6)
              | trailingByteValue(byte4);
      resultArr[resultPos] = DecodeUtil.highSurrogate(codepoint);
      resultArr[resultPos + 1] = DecodeUtil.lowSurrogate(codepoint);
    }

    /**
     * Returns whether the byte is not a valid continuation of the form '10XXXXXX'.
     */
    private static boolean isNotTrailingByte(byte b)
    {
      return b > (byte) 0xBF;
    }

    /**
     * Returns the actual value of the trailing byte (removes the prefix '10') for composition.
     */
    private static int trailingByteValue(byte b)
    {
      return b & 0x3F;
    }

    private static char highSurrogate(int codePoint)
    {
      return (char)
          ((Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10)) + (codePoint >>> 10));
    }

    private static char lowSurrogate(int codePoint)
    {
      return (char) (Character.MIN_LOW_SURROGATE + (codePoint & 0x3ff));
    }
  }
}
