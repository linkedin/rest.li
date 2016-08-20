/*
   Copyright (c) 2016 LinkedIn Corp.

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

/* $Id$ */
package com.linkedin.r2.transport.http.common;

/**
 * Enumerates supported HTTP protocols
 */
public enum HttpProtocolVersion
{
  /**
   * HTTP/1.1
   */
  HTTP_1_1,

  /**
   * HTTP/2
   */
  HTTP_2;

  private static final String HTTP_1_1_LITERALS = "HTTP/1.1";
  private static final String HTTP_2_LITERALS = "HTTP/2";
  private static final String HTTP_2_LITERALS_ALTERNATIVE = "HTTP/2.0";

  static
  {
    HTTP_1_1._literals = HTTP_1_1_LITERALS;
    HTTP_2._literals = HTTP_2_LITERALS;
  }

  private String _literals;

  public String literals()
   {
      return _literals;
   }

  /**
   * Parses a given string representation of HTTP protocol to an {@link HttpProtocolVersion} enumeration.
   * @param version a string representation of HTTP protocol version
   * @return the corresponding enumeration or {@code null} is nothing matches
   */
  public static HttpProtocolVersion parse(String version)
  {
    if (version.equalsIgnoreCase(HTTP_1_1_LITERALS))
    {
      return HTTP_1_1;
    }
    else if (version.equalsIgnoreCase(HTTP_2_LITERALS))
    {
      return HTTP_2;
    }
    else if (version.equalsIgnoreCase(HTTP_2_LITERALS_ALTERNATIVE))
    {
      return HTTP_2;
    }
    return null;
  }
}
