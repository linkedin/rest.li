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

package com.linkedin.restli.internal.common;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Gary Lin
 */
public class TestHeaderUtil
{
  @Test
  public void testMergeHeader()
  {
    Map<String, String> headers1 = new HashMap<>();
    Map<String, String> headers2 = new HashMap<>();
    headers1.put("X-header1", "header1Value");
    headers1.put("X-commonheader", "commonHeader1Value");
    headers2.put("X-header2", "header2Value");
    headers2.put("X-CommonHeader", "commonHeader2Value");

    Map<String, String> combineHeaders = HeaderUtil.mergeHeaders(headers1, headers2);

    Assert.assertEquals(combineHeaders.size(), 3);
    Assert.assertEquals(combineHeaders.get("x-header1"), "header1Value");
    Assert.assertEquals(combineHeaders.get("x-header2"), "header2Value");
    Assert.assertEquals(combineHeaders.get("x-commonheader"), "commonHeader2Value");
  }

  @Test
  public void testRemoveHeaders()
  {
    Map<String, String> headers = new HashMap<>();
    headers.put("X-header1", "header1Value");
    headers.put("X-header2", "header2Value");
    headers.put("X-header3", "header3Value");
    List<String> headersToRemove = Arrays.asList("x-header3", "x-header4");

    Map<String, String> newHeader = HeaderUtil.removeHeaders(headers, headersToRemove);

    Assert.assertEquals(newHeader.size(), 2);
    Assert.assertEquals(newHeader.get("x-header1"), "header1Value");
    Assert.assertEquals(newHeader.get("x-header2"), "header2Value");
  }
}
