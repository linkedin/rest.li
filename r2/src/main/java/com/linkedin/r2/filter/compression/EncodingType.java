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

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for parsing Accept-encoding
 * */
public enum EncodingType
{
  //NOTE: declaration order implicitly defines preference order
  GZIP(new GzipCompressor()),
  DEFLATE(new DeflateCompressor()),
  BZIP2(new Bzip2Compressor()),
  SNAPPY(new SnappyCompressor()),
  IDENTITY("identity"),
  ANY("*");

  private String httpName;
  private Compressor compressor;
  private static Map<String,EncodingType> _reverseMap;

  //Initialize the reverse map for lookups
  static
  {
    _reverseMap = new HashMap<String,EncodingType>();
    for(EncodingType t : EncodingType.values())
    {
      _reverseMap.put(t.getHttpName(), t);
    }
  }

  /**
   * @param HttpName Http value for this encoding
   */
  EncodingType(String httpName)
  {
    this.httpName = httpName;
    compressor = null;
  }

  /**
   * @param compressor Compressor associated with this encoding enum
   */
  EncodingType(Compressor compressor)
  {
    this.compressor = compressor;
    httpName = compressor.getContentEncodingName();
  }

  /**
   * @return Http value for this enum
   */
  public String getHttpName()
  {
    return httpName;
  }

  /**
   * Returns the compressor of this compression method
   */
  public Compressor getCompressor()
  {
    return compressor;
  }

  /**
   * @param compressionHeader Http encoding value as string
   * @return Associated enum value, null if no mapping exists
   */
  public static EncodingType get(String compressionHeader)
  {
    return _reverseMap.get(compressionHeader);
  }

  /**
   * @return if this encoding has a compressor. Generally, speaking, this is false
   * for ANY (*).
   */
  public boolean hasCompressor()
  {
    return getCompressor() != null;
  }
}
