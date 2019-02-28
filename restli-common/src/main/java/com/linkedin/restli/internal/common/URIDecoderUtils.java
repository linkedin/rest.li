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

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * LinkedIn elects to include this software in this distribution under the CDDL license.
 *
 * Modifications:
 * - The core of this class's low-level logic is heavily derived from the URI decoding logic found in
 *   Jersey's UriComponent class, written by Paul Sandoz. The code has been restructured, refactored,
 *   and revised to a great extent, though certain portions still resemble corresponding parts of the
 *   original code.
 */

package com.linkedin.restli.internal.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;


/**
 * <p>
 * Utils for decoding characters in URI elements. Used primarily by
 * {@link URIElementParser} when parsing Rest.li 2.0.0 URIs.
 * </p>
 *
 * <p>
 * This logic heavily borrows from the original URI decoding logic found in
 * {@link com.linkedin.jersey.api.uri.UriComponent}, and its behavior should be consistent.
 * The purpose of re-creating the decoding logic was to restructure it such that the
 * {@link URIElementParser} could be more efficient in tokenizing URI elements by decoding
 * characters as they are read to avoid using fully-buffered strings. Ideally,
 * {@link com.linkedin.jersey.api.uri.UriComponent} should eventually be retired so
 * that we can reduce our third-party dependencies and have more concise code.
 * </p>
 *
 * <p>
 * Note that this decoder does <i>not</i> decode plus signs (<code>'+'</code>) as spaces.
 * </p>
 *
 * @author Evan Williams
 */
public class URIDecoderUtils
{
  /**
   * Decodes a given string.
   *
   * @param s source string
   * @return decoded string
   */
  public static String decode(String s)
  {
    final int n = s.length();
    StringBuilder result = new StringBuilder(n);
    for (int i = 0; i < n; i++)
    {
      char c = s.charAt(i);
      if (c == '%')
      {
        int numCharsConsumed = decodeConsecutiveOctets(result, s, i);
        i += numCharsConsumed - 1;
      }
      else
      {
        result.append(c);
      }
    }
    return result.toString();
  }

  /**
   * Decodes the consecutive percent-escaped octets found in the source string into a string.
   * Starts decoding at the specified index and decodes until it reaches an octet that is
   * not encoded. Writes the resulting string to a given StringBuilder.
   *
   * @param dest StringBuilder to write to
   * @param s source string
   * @param start index indicating where to begin decoding
   * @return number of characters consumed in the source string
   */
  public static int decodeConsecutiveOctets(StringBuilder dest, String s, int start)
  {
    final int n = s.length();

    if (start >= n)
    {
      throw new IllegalArgumentException("Cannot decode from index " + start + " of a length-" + n + " string");
    }

    if (s.charAt(start) != '%')
    {
      throw new IllegalArgumentException("Must begin decoding from a percent-escaped octet, but found '" + s.charAt(start) + "'");
    }

    if (start + 3 < n && s.charAt(start + 3) == '%')
    {
      // If there are multiple consecutive encoded octets, decode all into bytes
      ByteBuffer bb = decodeConsecutiveOctets(s, start);
      int numCharsConsumed = bb.limit() * 3;
      // Decode the bytes into a string
      decodeBytes(dest, bb);
      return numCharsConsumed;
    }
    else if (start + 2 < n)
    {
      // Else, decode just one octet
      byte b = decodeOctet(s, start + 1);
      decodeByte(dest, b);
      return 3;
    }

    throw new IllegalArgumentException("Malformed percent-encoded octet at index " + start);
  }

  /**
   * Decodes the consecutive percent-escaped octets found in the source string into bytes.
   * Starts decoding at the specified index and decodes until it reaches an octet that is
   * not encoded.
   *
   * @param s source string
   * @param start index indicating where to begin decoding
   * @return ByteBuffer containing the decoded octets found in the source string
   */
  private static ByteBuffer decodeConsecutiveOctets(String s, int start)
  {
    // Find end of consecutive encoded octet sequence
    int end = start;
    while (end < s.length() && s.charAt(end) == '%')
    {
      end += 3;
    }

    if (end > s.length())
    {
      throw new IllegalArgumentException("Malformed percent-encoded octet at index " + (end - 3));
    }

    // Allocate just enough memory for byte buffer
    ByteBuffer bb = ByteBuffer.allocate((end - start) / 3);

    // Decode the consecutive octets
    for (int i = start; i < end; i += 3)
    {
      byte b = decodeOctet(s, i + 1);
      bb.put(b);
    }
    bb.flip();
    return bb;
  }

  /**
   * Decodes a given sequence of bytes into a string.
   *
   * @param dest StringBuilder to write to
   * @param source ByteBuffer to read from
   */
  private static void decodeBytes(StringBuilder dest, ByteBuffer source)
  {
    // If there is only one byte and it's in the ASCII range
    if (source.limit() == 1 && isAscii(source.get(0)))
    {
      // Character can be appended directly
      dest.append((char) source.get(0));
    }
    else
    {
      // Decode multiple bytes
      decodeNonAsciiBytes(dest, source);
    }
  }

  /**
   * Decodes an encoded octet represented as a byte.
   *
   * @param dest StringBuilder to write to
   * @param b byte to decode
   */
  private static void decodeByte(StringBuilder dest, byte b)
  {
    if (isAscii(b))
    {
      // Octet can be appended directly
      dest.append((char) b);
    }
    else
    {
      // Decode non-ascii character
      decodeNonAsciiBytes(dest, ByteBuffer.wrap(new byte[]{ b }));
    }
  }

  /**
   * Indicates whether the given byte can be decoded into ASCII.
   *
   * @param b the byte in question
   * @return true if the byte can be decoded into ASCII
   */
  private static boolean isAscii(byte b)
  {
    return (b & 0xFF) < 0x80;
  }

  /**
   * Decodes bytes that cannot be decoded into ASCII by decoding them into UTF-8.
   *
   * @param dest StringBuilder to write to
   * @param source ByteBuffer to read from
   */
  private static void decodeNonAsciiBytes(StringBuilder dest, ByteBuffer source)
  {
    CharBuffer cb = Charset.forName("UTF-8").decode(source);
    dest.append(cb);
  }

  /**
   * Decodes an octet represented as a sequence of two hexadecimal characters into a single byte.
   * This sequence is defined as the two characters found in the source string starting at the
   * specified index.
   *
   * @param s source string
   * @param start index from which to start decoding
   * @return the octet in single-byte form
   */
  private static byte decodeOctet(String s, int start)
  {
    return (byte) (decodeHex(s, start) << 4 | decodeHex(s, start + 1));
  }

  /**
   * Decodes a single hex character into a byte. Uses the character found in the source string
   * at the specified index.
   *
   * @param s source string
   * @param i index of the hex character
   * @return decoded hex character
   */
  private static byte decodeHex(String s, int i)
  {
    final byte value = decodeHex(s.charAt(i));
    if (value == -1)
    {
      throw new IllegalArgumentException("Malformed percent-encoded octet at index " + i +
          ", invalid hexadecimal digit '" + s.charAt(i) + "'");
    }
    return value;
  }

  private static final byte[] HEX_TABLE = createHexTable();

  @SuppressWarnings("Duplicates")
  private static byte[] createHexTable()
  {
    byte[] table = new byte[0x80];
    Arrays.fill(table, (byte) -1);

    for (char c = '0'; c <= '9'; c++)
    {
      table[c] = (byte) (c - '0');
    }
    for (char c = 'A'; c <= 'F'; c++)
    {
      table[c] = (byte) (c - 'A' + 10);
    }
    for (char c = 'a'; c <= 'f'; c++)
    {
      table[c] = (byte) (c - 'a' + 10);
    }
    return table;
  }

  /**
   * Decodes a single hex character into a byte. Returns -1 if the character is invalid.
   *
   * @param c hex character
   * @return decoded hex character
   */
  private static byte decodeHex(char c)
  {
    return (c < 128) ? HEX_TABLE[c] : -1;
  }
}
