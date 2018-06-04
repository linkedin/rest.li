/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.response;


import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;


/**
 * @author Sean Sheng
 */
public class ResponseBuilderUtil
{
  private static final String KEY1 = "key1";
  private static final String KEY2_LOWER = "key";
  private static final String KEY2_UPPER = "KEY2";
  private static final String VALUE1 = "value1";
  private static final String VALUE2 = "value2";

  /**
   * Gets the prototype HTTP headers.
   *
   * @return Prototype HTTP headers
   */
  public static Map<String, String> getHeaders()
  {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put(KEY1, VALUE1);
    headers.put(KEY2_LOWER, VALUE2);
    headers.put(KEY2_UPPER, VALUE2);
    return headers;
  }

  /**
   * Validates {@code response} against {@code headers} ensuring that keys are case insensitive
   * and all entries are present.
   *
   * @param response Partial rest response to validate
   * @param headers Headers to validate against
   */
  public static void validateHeaders(RestLiResponse response, Map<String, String> headers)
  {
    Assert.assertEquals(response.getHeaders(), headers);
    for (String key : headers.keySet())
    {
      Assert.assertEquals(response.getHeader(key.toUpperCase()), headers.get(key));
      Assert.assertEquals(response.getHeader(key.toLowerCase()), headers.get(key));
    }
  }
}
