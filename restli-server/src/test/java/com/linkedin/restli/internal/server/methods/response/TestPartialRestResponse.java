/*
   Copyright (c) 2014 LinkedIn Corp.

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
package com.linkedin.restli.internal.server.methods.response;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestPartialRestResponse
{
  @Test
  public void testHeaders()
  {
    Map<String, String> inputHeaderMap = new HashMap<String, String>();
    inputHeaderMap.put("foo", "bar");
    inputHeaderMap.put("bar", "baz");
    PartialRestResponse response = new PartialRestResponse.Builder().headers(inputHeaderMap).build();
    assertEquals(response.getHeaders(), inputHeaderMap);
    assertEquals(response.getHeader("FOO"), "bar");
    assertEquals(response.getHeader("BAR"), "baz");
    // Check that the header map is mutable.
    response.getHeaders().put("foo1", "bar1");
    assertEquals(response.getHeader("foo1"), "bar1");
    assertEquals(response.getHeader("FOO1"), "bar1");
  }

  @Test
  public void testHttpStatus()
  {
    PartialRestResponse response = new PartialRestResponse.Builder().status(HttpStatus.S_200_OK).build();
    assertEquals(response.getStatus(), HttpStatus.S_200_OK);
  }

  @Test
  public void testEntity()
  {
    DataMap data = new DataMap();
    RecordTemplate record = new Foo(data);
    PartialRestResponse response = new PartialRestResponse.Builder().entity(record).build();
    assertEquals(response.getEntity(), record);
    assertTrue(response.hasData());
    assertEquals(response.getDataMap(), data);
  }

  private static class Foo extends RecordTemplate
  {
    public Foo(DataMap map)
    {
      super(map, null);
    }
  }
}
