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


package com.linkedin.restli.server;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class TestRestLiRequestDataImpl
{
  @Test
  public void testBatchRequest()
  {
    List<Foo> batchEntities = Arrays.asList(Foo.createFoo("foo", "bar"));
    List<String> batchKeys = Arrays.asList("key1", "key2", "key3");
    Map<String, Foo> batchKeyEntityMap = new HashMap<>();
    batchKeyEntityMap.put("key1", Foo.createFoo("foo1", "bar1"));
    batchKeyEntityMap.put("key2", Foo.createFoo("foo2", "bar2"));
    RestLiRequestData requestData1 = new RestLiRequestDataImpl.Builder().batchKeys(batchKeys).build();
    RestLiRequestData requestData2 = new RestLiRequestDataImpl.Builder().batchEntities(batchEntities).build();
    RestLiRequestData requestData3 =
        new RestLiRequestDataImpl.Builder().batchEntities(batchEntities).batchKeys(batchKeys).build();
    RestLiRequestData requestData4 = new RestLiRequestDataImpl.Builder().batchKeyEntityMap(batchKeyEntityMap).build();

    assertTrue(requestData1.isBatchRequest());
    assertFalse(requestData1.hasEntity());
    assertFalse(requestData1.hasKey());
    assertFalse(requestData1.hasBatchEntities());
    assertFalse(requestData1.hasBatchKeyEntityMap());
    assertTrue(requestData1.hasBatchKeys());
    assertEquals(requestData1.getBatchKeys(), batchKeys);

    assertTrue(requestData2.isBatchRequest());
    assertFalse(requestData2.hasEntity());
    assertFalse(requestData2.hasKey());
    assertTrue(requestData2.hasBatchEntities());
    assertFalse(requestData2.hasBatchKeys());
    assertFalse(requestData2.hasBatchKeyEntityMap());
    assertEquals(requestData2.getBatchEntities(), batchEntities);

    assertTrue(requestData3.isBatchRequest());
    assertFalse(requestData3.hasEntity());
    assertFalse(requestData3.hasKey());
    assertTrue(requestData3.hasBatchEntities());
    assertTrue(requestData3.hasBatchKeys());
    assertFalse(requestData3.hasBatchKeyEntityMap());
    assertEquals(requestData3.getBatchEntities(), batchEntities);
    assertEquals(requestData3.getBatchKeys(), batchKeys);

    assertTrue(requestData4.isBatchRequest());
    assertFalse(requestData4.hasEntity());
    assertFalse(requestData4.hasKey());
    assertFalse(requestData4.hasEntity());
    assertFalse(requestData4.hasKey());
    assertEquals(requestData4.getBatchKeyEntityMap(), batchKeyEntityMap);
  }

  @Test
  public void testNonBatchRequest()
  {
    Foo entity = Foo.createFoo("foo", "bar");
    String key = "key1";
    RestLiRequestData requestData = new RestLiRequestDataImpl.Builder().key(key).entity(entity).build();
    assertFalse(requestData.isBatchRequest());
    assertFalse(requestData.hasBatchEntities());
    assertFalse(requestData.hasBatchKeyEntityMap());
    assertFalse(requestData.hasBatchKeys());
    assertTrue(requestData.hasEntity());
    assertTrue(requestData.hasKey());
    assertEquals(requestData.getEntity(), entity);
    assertEquals(requestData.getKey(), key);
  }

  private static class Foo extends RecordTemplate
  {
    private Foo(DataMap map)
    {
      super(map, null);
    }

    public static Foo createFoo(String key, String value)
    {
      DataMap dataMap = new DataMap();
      dataMap.put(key, value);
      return new Foo(dataMap);
    }
  }
}
