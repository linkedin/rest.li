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
 * $Id: $
 */
package com.linkedin.restli.internal.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.linkedin.restli.common.RestConstants;

public class URLEscaper
{
  public enum Escaping
  {
    URL_ESCAPING,
    NO_ESCAPING
  }

  public static String escape(String str, Escaping escaping)
  {
    if(escaping == Escaping.NO_ESCAPING) return str;
    try
    {
      return URLEncoder.encode(str, RestConstants.DEFAULT_CHARSET_NAME);
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static String unescape(String str, Escaping escaping)
  {
    if(escaping == Escaping.NO_ESCAPING) return str;
    try
    {
      return URLDecoder.decode(str, RestConstants.DEFAULT_CHARSET_NAME);
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }
}

