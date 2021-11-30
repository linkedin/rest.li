/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: StringCodec.java 33147 2007-11-18 22:29:12Z dmccutch $ */
package com.linkedin.restli.internal.common;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to encode and decode a string using a given encoding char
 * and a set of reserved characters to encode.
 *
 * All reserved characters are encoded to their ASCII hex value and prefixed with the
 * encoding char.  The encoding char is also encoded to it's respective ASCII hex value and
 * prefixed with the encoding char.
 *
 * All encoded hex values are decoded to their ascii char value.
 *
 * Percent encoding could, for example, be implemented by using '%' as the escape char
 * and '!', '#', '$', '&' ... as the reserved chars.
 */
public class AsciiHexEncoding
{
  /**
   * The character that will be used in the encoding */
  private final char _encodingChar;

  /**
   * The set of characters that are reserved and must always be encoded */
  private final Set<Character> _reservedChars;

  public AsciiHexEncoding(char escapeChar, char[] reservedChars)
    throws IllegalArgumentException
  {
    this(escapeChar, toSet(reservedChars));
  }

  private static Set<Character> toSet(char[] chars)
  {
    HashSet<Character> reservedCharsSet = new HashSet<>();
    for(char c : chars)
    {
      reservedCharsSet.add(c);
    }
    return reservedCharsSet;
  }

  /**
   * Constructor. You provide the set of reserved characters that you want
   * to be encoded. The encoding character is the one you provide.
   *
   * @param encodingChar the character used for encoding
   * @param reservedChars the set of reserved characters to encode
   * @exception IllegalArgumentException if problem with arguments
   */
  public AsciiHexEncoding(char encodingChar, Set<Character> reservedChars)
    throws IllegalArgumentException
  {
    if (encodingChar > 127)
    {
      throw new IllegalArgumentException(reservedChars + " not allowed.  Only ascii chars may be used as encoding char.");
    }
    if (reservedChars == null)
    {
      throw new IllegalArgumentException("reservedChars must be non-null");
    }

    for (Character reservedChar : reservedChars)
    {
      if (reservedChar.charValue() == encodingChar)
      {
        throw new IllegalArgumentException("The characters you can encode must be different from the encoding character!");
      }
      if (reservedChar.charValue() > 127)
      {
        throw new IllegalArgumentException(reservedChar + " not allowed.  Only ascii chars may be used as reserved chars.");
      }
    }

    _reservedChars = reservedChars;
    _encodingChar = encodingChar;
  }

  /**
   * Encodes the incoming string according to the given escape character and reserved characters.
   *
   * @param s the string to encode
   * @return the encoded string
   */
  public String encode(String s)
  {
    if (s == null)
    {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    int len = s.length();
    for (int i = 0; i < len; i++)
    {
      char c = s.charAt(i);
      if (c == _encodingChar || _reservedChars.contains(c))
      {
        sb.append(_encodingChar);
        int asciiVal = (int) c;
        if (asciiVal < 16) // <= 'F'
        {
          sb.append('0');
        }
        sb.append(Integer.toHexString(asciiVal).toUpperCase());
      }
      else
      {
        sb.append(c);
      }
    }

    return sb.toString();
  }

  /**
   * Decodes the string,  .
   *
   * @param s the string to decode
   * @return the decoded string
   * @throws CannotDecodeException if the string cannot be decoded */
  public String decode(String s) throws CannotDecodeException
  {
    if (s == null)
    {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    int len = s.length();
    for (int i = 0; i < len; i++)
    {
      char c = s.charAt(i);

      if (c == _encodingChar)
      {
        if (i + 3 > len)
        {
          throw new CannotDecodeException(s + " - Failed to decode incomplete escaped char at offset " + i);
        }
        String asciiHex = s.substring(i + 1, i + 3);
        try
        {
          Integer asciiInt = Integer.parseInt(asciiHex, 16);
          sb.append((char) asciiInt.intValue());
        }
        catch (NumberFormatException ex)
        {
          throw new CannotDecodeException(s + " - Failed to decode escaped char at offset " + i);
        }
        i += 2;
      }
      else
      {
        sb.append(c);
      }
    }

    return sb.toString();
  }

  /**
   * Defines the exception thrown when the string cannot be decoded */
  public static class CannotDecodeException extends Exception
  {
    private static final long serialVersionUID = 1L;

    public CannotDecodeException(String msg)
    {
      super(msg);
    }

    public CannotDecodeException(String message, Throwable cause)
    {
      super(message, cause);
    }
  }
}
