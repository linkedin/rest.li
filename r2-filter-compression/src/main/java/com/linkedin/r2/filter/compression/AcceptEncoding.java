/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.r2.filter.compression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class implements the parser and information about accept-encoding entries for an
 * Http request.
 */
public class AcceptEncoding implements Comparable<AcceptEncoding>
{
  private final EncodingType _type;
  private final float _quality;

  /**
   * Instantiates a particular Accept-Encoding entry.
   * @param type Encoding of this encoding entry
   * @param quality Quality value of this encoding entry
   */
  public AcceptEncoding(EncodingType type, float quality)
  {
    _type = type;
    _quality = quality;
  }

  /**
   * @return Quality value of this entry
   */
  public float getQuality()
  {
    return _quality;
  }

  /**
   * @return Encoding of this entry
   */
  public EncodingType getType()
  {
    return _type;
  }

  /**
   * Takes a comma delimited string of content-encoding values and parses them,
   * returning an array of parsed EncodingType in the order of
   * the parsed string. Throws IllegalArgumentException
   * if something not supported or not recognized shows up.
   * @param acceptCompression
   */
  public static EncodingType[] parseAcceptEncoding(String acceptCompression)
  {
    if(acceptCompression.trim().isEmpty())
    {
      return new EncodingType[0];
    }

    String[] entries = acceptCompression.toLowerCase().split(CompressionConstants.ENCODING_DELIMITER);
    EncodingType[] types = new EncodingType[entries.length];
    for(int i = 0; i < entries.length; i++)
    {
      types[i] = EncodingType.get(entries[i].trim());
    }

    return types;
  }

  /**
   * Takes the value of Accept-Encoding HTTP header field and returns a list of supported types in
   * their order of appearance in the HTTP header value (unsupported types are filtered out).
   * @param headerValue Http header value of Accept-Encoding field
   * @return ArrayList of accepted-encoding entries
   * @throws CompressionException
   */
  public static List<AcceptEncoding> parseAcceptEncodingHeader(String headerValue, Set<EncodingType> supportedEncodings) throws CompressionException
  {
    headerValue = headerValue.toLowerCase();
    String[] entries = headerValue.split(CompressionConstants.ENCODING_DELIMITER);
    List<AcceptEncoding> parsedEncodings = new ArrayList<AcceptEncoding>();

    for(String entry : entries)
    {
      String[] content = entry.trim().split(CompressionConstants.QUALITY_DELIMITER);

      if(content.length < 1 || content.length > 2)
      {
        throw new IllegalArgumentException(CompressionConstants.ILLEGAL_FORMAT + entry);
      }

      EncodingType type = null;
      String encodingName = content[0].trim();
      if (EncodingType.isSupported(encodingName))
      {
        type = EncodingType.get(encodingName);
      }
      Float quality = 1.0f;

      if (type != null && supportedEncodings.contains(type))
      {
        if (content.length > 1)
        {
          String acceptEncodingPart = content[1].trim();
          if (acceptEncodingPart.startsWith(CompressionConstants.QUALITY_PREFIX))
          {
            try
            {
              quality = Float.parseFloat(acceptEncodingPart.substring(CompressionConstants.QUALITY_PREFIX.length()));
            }
            catch (NumberFormatException e)
            {
              throw new CompressionException(CompressionConstants.ILLEGAL_FORMAT + entry, e);
            }
          }
          else
          {
            throw new CompressionException(CompressionConstants.ILLEGAL_FORMAT + entry);
          }
        }

        parsedEncodings.add(new AcceptEncoding(type, quality));
      }
    }

    return parsedEncodings;
  }


  /** Chooses the best acceptable encoding based on
   * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">RFC2616</a>.
   * If no possible encoding can be used, null is returned indicating that
   * HTTP 406 (NOT ACCEPTABLE) should be used.
   * @param entries List of accepted-encoding entries
   * @return Encoding type of choice, null if not possible; must contain compressor
   */
  public static EncodingType chooseBest(List<AcceptEncoding> entries)
  {
    Collections.sort(entries);
    HashSet<EncodingType> bannedEncoding = new HashSet<EncodingType>();

    //Add the banned entries to the disallow list
    int lastEntry = entries.size()-1;
    while(lastEntry >= 0 && entries.get(lastEntry).getQuality() <= 0.0)
    {
      AcceptEncoding removed = entries.remove(lastEntry);
      bannedEncoding.add(removed.getType());
      lastEntry--;
    }

    //Return the first acceptable entry
    for(AcceptEncoding type : entries)
    {
      if (type.getType() == EncodingType.ANY)
      {
        //NOTE: this is very conservative by returning IDENTITY
        //for all ANYs unless explicitly stated, and in fact
        //breaks the official HTTP spec for some esoteric cases
        //such as "identity;q=0.0, *,gzip=0.5"
        //The current code is to ensure that server doesn't
        //return something unintelligible to the client.
        if (!bannedEncoding.contains(EncodingType.IDENTITY))
        {
          return EncodingType.IDENTITY;
        }
      }
      else
      {
        return type.getType();
      }
    }

    //If we're at the end of the list, if either ANY or IDENTITY is
    //in the ban list, then identity is banned by default; otherwise,
    //No encoding will be used.
    return bannedEncoding.contains(EncodingType.ANY) ||
        bannedEncoding.contains(EncodingType.IDENTITY) ? null : EncodingType.IDENTITY;
  }

  @Override
  public int compareTo(AcceptEncoding target)
  {
    return new Float(target.getQuality()).compareTo(getQuality());
  }
}
