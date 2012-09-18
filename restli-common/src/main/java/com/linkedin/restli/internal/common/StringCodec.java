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

/**
 * This class is used to encode and decode a string. You specify an array
 * of characters that need to be encoded in the constructor. You can also
 * specify the encoding character. Here is an example.
 *
 * <p>The string "<code>This is a test string.</code>" will be encoded as
 * "<code>This is a test string*3</code>" if the encoding character is '*'
 * and the character you want to encode is '.'
 *
 * <p> This class will be useful if you want to use a character with a
 * special meaning and still be able to use the character. (for example,
 * you want to return a list of values comma separated, but you still want
 * to have the comma available inside a value..).
 *
 * <p> Due to the encoding there are some limitations:
 * <li> You cannot encode more than 7 characters
 * <li> You cannot use the numbers '0'-'9' as either encoding or encoded
 *
 * @version $Revision: 33147 $
 * @author Yan Pujante */
public class StringCodec
{
  /**
   * The character that will be used in the encoding */
  private final char _encodingChar;

  /**
   * The array of characters that needs to be encoded */
  private final char[] _charactersToEncode;

  /**
   * The string that represents the encoded char */
  private final String _encodedEncodingCharString;

  /**
   * The string that represents the null string */
  private final String _encodedNullString;

  /**
   * The string that represents the empty string */
  private final String _encodedEmptyString;

  /**
   * The array of encoded chars for fast lookup */
  private final String _encodedCharsString[];

  /**
   * Constructor. You provide the array of characters that you want to be
   * encoded. The encoding character is the one you provide. Due to the
   * encoding, the array you provide must contain less or equal to 7
   * characters.
   *
   * @param encodingChar the character used for encoding
   * @param charactersToEncode the array of characters to encode
   * @exception IllegalArgumentException if problem with arguments */
  public StringCodec(char encodingChar, char[] charactersToEncode)
    throws IllegalArgumentException
  {
    if(charactersToEncode == null ||
       charactersToEncode.length == 0 ||
       charactersToEncode.length > 7)
    {
      throw new IllegalArgumentException("The array you provide must be not null, not empty, and contain less than 7 characters");
    }

    for(int i = 0; i < charactersToEncode.length; i++)
    {
      if(charactersToEncode[i] == encodingChar)
        throw new IllegalArgumentException("The characters you can encode must be different from the encoding character!");

      if(charactersToEncode[i] >= '0' && charactersToEncode[i] <= '9')
        throw new IllegalArgumentException("The character you encode must not be one of '0'-'9'");
    }

    if(encodingChar >= '0' && encodingChar <= '9')
      throw new IllegalArgumentException("The encoding character must not be one of '0'-'9'");

    _charactersToEncode = charactersToEncode;
    _encodingChar = encodingChar;

    char c = '0';

    _encodedEncodingCharString =
      new StringBuilder().append(_encodingChar).append(c++).toString();
    _encodedNullString =
      new StringBuilder().append(_encodingChar).append(c++).toString();
    _encodedEmptyString =
      new StringBuilder().append(_encodingChar).append(c++).toString();

    _encodedCharsString = new String[charactersToEncode.length];
    for(int i = 0; i < charactersToEncode.length; i++)
    {
      _encodedCharsString[i] =
        new StringBuilder().append(_encodingChar).append(c++).toString();
    }
  }

  /**
   * Constructor. You provide the array of characters that you want to be
   * encoded. The encoding character will be a '*'. Due to the encoding,
   * the array you provide must contain less or equal to 7 characters.
   *
   * @param charactersToEncode the array of characters to encode
   * @exception IllegalArgumentException if problem with the argument */
  public StringCodec(char[] charactersToEncode) throws IllegalArgumentException
  {
    this('*', charactersToEncode);
  }

  /**
   * Encodes the incoming string. It will return a string that does not
   * contain anymore one of the characters to encode you specified in the
   * constructor. It is safe to give a <code>null</code> string... it will
   * return the encoded <code>null</code> string (idem for the empty
   * string)... thus the string returned will always be at least one
   * character in length!
   *
   * @param s the string to encode
   * @return the encoded string */
  public String encode(String s)
  {
    if(s == null)
      return _encodedNullString;

    if(s.length() == 0)
      return _encodedEmptyString;

    StringBuilder sb = new StringBuilder();
    char c;
    int len = s.length();

  mainloop:
    for(int i = 0; i < len; i++)
    {
      c = s.charAt(i);

      if(c == _encodingChar)
      {
        sb.append(_encodedEncodingCharString);
        continue;
      }

      for(int j = 0; j < _charactersToEncode.length; j++)
      {
        if(c == _charactersToEncode[j])
        {
          sb.append(_encodedCharsString[j]);
          continue mainloop;
        }
      }

      sb.append(c);
    }

    return sb.toString();
  }

  /**
   * Decodes the string that was previously encoded with this codec.
   *
   * @param s the string to decode
   * @return the decoded string
   * @throws CannotDecodeException if the string cannot be decoded */
  public String decode(String s) throws CannotDecodeException
  {
    if(s == null)
      return null;
    
    if(s.equals(_encodedNullString))
      return null;

    if(s.equals(_encodedEmptyString))
      return "";

    StringBuilder sb = new StringBuilder();
    char c;
    int len = s.length();
    int lastIndex = len - 1;

    for(int i = 0; i < len; i++)
    {
      c = s.charAt(i);

      if(c == _encodingChar)
      {
        if(i == lastIndex)
          throw new CannotDecodeException(s);

        c = s.charAt(++i);

        if(c == '0')
          sb.append(_encodingChar);
        else
        {
          int idx = c - '3';
          try
          {
            sb.append(_charactersToEncode[idx]);
          }
          catch(ArrayIndexOutOfBoundsException ex)
          {
            throw new CannotDecodeException(s);
          }
        }
      }
      else
        sb.append(c);
    }

    return sb.toString();
  }


  /**
   * @return the chars to encode */
  public char[] getCharactersToEncode()
  {
    char[] res = new char[_charactersToEncode.length];
    System.arraycopy(_charactersToEncode, 0, res, 0, _charactersToEncode.length);
    return res;
  }
  
  /**
   * computes the length of the encoded string
   * 
   * @param stringToEncode an unencoded string
   * @return length of encoded string
   */
  public int computeEncodedLength(String stringToEncode)
  {
    if(stringToEncode == null)
      return _encodedNullString.length();

    if(stringToEncode.length() == 0)
      return _encodedEmptyString.length();
    
    
    char chr;
    int totalLength = stringToEncode.length();
    int encodingLengh = 0;
    for (int i = 0; i < stringToEncode.length() ; i++)
    {
      chr = stringToEncode.charAt(i);

      if(chr == _encodingChar)
      {
        encodingLengh += _encodedEncodingCharString.length()-1;
      }

      for(int j = 0; j < _charactersToEncode.length; j++)
      {
        if(chr == _charactersToEncode[j])
        {
          encodingLengh += _encodedCharsString[j].length()-1;
        }
      }
    }

    return totalLength + encodingLengh;
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
