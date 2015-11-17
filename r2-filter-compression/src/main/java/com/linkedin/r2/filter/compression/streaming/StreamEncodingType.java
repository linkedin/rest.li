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


package com.linkedin.r2.filter.compression.streaming;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author Ang Xu
 */
public enum StreamEncodingType
{
  GZIP("gzip"),
  DEFLATE("deflate"),
  SNAPPY_FRAMED("x-snappy-framed"),
  BZIP2("bzip2"),
  IDENTITY("identity"),
  ANY("*");

  private static final Map<String,StreamEncodingType> REVERSE_MAP;

  static
  {
    Map<String, StreamEncodingType> reverseMap = new HashMap<String, StreamEncodingType>();
    for(StreamEncodingType t : StreamEncodingType.values())
    {
      reverseMap.put(t.getHttpName(), t);
    }
    REVERSE_MAP = Collections.unmodifiableMap(reverseMap);
  }

  private final String _httpName;

  StreamEncodingType(String httpName)
  {
    _httpName = httpName;
  }

  public String getHttpName()
  {
    return _httpName;
  }

  public StreamingCompressor getCompressor(Executor executor)
  {
    switch (this)
    {
      case GZIP:
        return new GzipCompressor(executor);
      case DEFLATE:
        return new DeflateCompressor(executor);
      case BZIP2:
        return new Bzip2Compressor(executor);
      case SNAPPY_FRAMED:
        return new SnappyCompressor(executor);
      case IDENTITY:
        return new NoopCompressor();
      default:
        return null;
    }
  }

  public static StreamEncodingType get(String httpName)
  {
    return REVERSE_MAP.get(httpName);
  }

  /**
   * Checks whether the encoding is supported.
   *
   * @param encodingName Http encoding name.
   * @return true if the encoding is supported
   */
  public static boolean isSupported(String encodingName)
  {
    return REVERSE_MAP.containsKey(encodingName);
  }

}
