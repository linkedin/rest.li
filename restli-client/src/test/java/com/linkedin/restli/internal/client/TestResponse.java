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

package com.linkedin.restli.internal.client;


import com.linkedin.restli.client.Response;

import java.net.HttpCookie;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 */
public class TestResponse
{
  @Test
  public void testHeadersCaseInsensitiveGet()
  {
    int status = 200;
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "value");
    Response<Long> response = new ResponseImpl<Long>(status, headers, Collections.<HttpCookie>emptyList());
    Assert.assertEquals(response.getHeader("HEADER"), "value");
  }

  @Test
  public void testHeadersCaseInsensitiveSet()
  {
    int status = 200;
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "value");
    headers.put("HEADER", "value");
    Response<Long> response = new ResponseImpl<Long>(status, headers, Collections.<HttpCookie>emptyList());
    Assert.assertEquals(response.getHeaders().size(), 1);
    Assert.assertEquals(response.getHeader("HEADER"), "value");
  }
}
