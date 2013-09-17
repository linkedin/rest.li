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
package com.linkedin.restli.examples;

import java.net.URLEncoder;

public class StringTestKeys
{
  public static String URL = "http://foo.biz:1234/path/path/path?k1=v1&k2=v2&k3=string+with+pluses&k4=string%20with%20pcts";
  public static String SINGLE_ENCODED_URL;
  public static String DOUBLE_ENCODED_URL;

  public static String URL2 = "http://foo.biz:1234/url2?k1=v1&k2=v2&k3=a+b&k4=1%202&q=1,2,3";
  public static String URL3 = "http://foo.biz:1234/url3?k1=v1&k2=v2&k3=a+b&k4=1%202";

  public static String SIMPLEKEY = "KEY 1";
  public static String SIMPLEKEY2 = "KEY 2";
  public static String SIMPLEKEY3 = "KEY 3";

  static {
    try
    {
      SINGLE_ENCODED_URL = URLEncoder.encode(URL, "UTF-8");
      DOUBLE_ENCODED_URL = URLEncoder.encode(SINGLE_ENCODED_URL, "UTF-8");
    } catch (Exception e)
    {
    }
  }
}
