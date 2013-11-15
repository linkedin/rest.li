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

package com.linkedin.data.template;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;

import java.util.IdentityHashMap;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestDataObjectToObjectCache
{
  @Test
  public void testPutAndGet()
  {
    IdentityHashMap<Object, DataTemplate<?>> controlCache = new IdentityHashMap<Object, DataTemplate<?>>();
    DataObjectToObjectCache<DataTemplate<?>> testCache = new DataObjectToObjectCache<DataTemplate<?>>();

    populateTestData(controlCache, testCache);
    crossCheckTestData(controlCache, testCache);
  }

  @Test
  public void testClone() throws CloneNotSupportedException
  {
    IdentityHashMap<Object, DataTemplate<?>> controlCache = new IdentityHashMap<Object, DataTemplate<?>>();
    DataObjectToObjectCache<DataTemplate<?>> testCache = new DataObjectToObjectCache<DataTemplate<?>>();

    populateTestData(controlCache, testCache);
    testCache = testCache.clone();
    crossCheckTestData(controlCache, testCache);
  }

  @Test
  public void testKeyNotFound()
  {
    DataObjectToObjectCache<DataTemplate<?>> testCache = new DataObjectToObjectCache<DataTemplate<?>>();

    Assert.assertNull(testCache.get(new Object()));
    Assert.assertNull(testCache.get(new DataMap()));
    Assert.assertNull(testCache.get(new DataList()));
  }

  @Test
  public void testValueOverwrite()
  {
    DataObjectToObjectCache<DataTemplate<?>> testCache = new DataObjectToObjectCache<DataTemplate<?>>();
    DataMap mapKey = new DataMap();
    DataList listKey = new DataList();
    Object objKey = new Object();

    testCache.put(mapKey, new Bar());
    testCache.put(listKey, new Bar());
    testCache.put(objKey, new Bar());

    Bar mapBar = new Bar();
    Bar listBar = new Bar();
    Bar objBar = new Bar();

    testCache.put(mapKey, mapBar);
    testCache.put(listKey, listBar);
    testCache.put(objKey, objBar);

    Assert.assertSame(testCache.get(mapKey), mapBar);
    Assert.assertSame(testCache.get(listKey), listBar);
    Assert.assertSame(testCache.get(objKey), objBar);
  }

  private void crossCheckTestData(IdentityHashMap<Object, DataTemplate<?>> controlCache,
                                  DataObjectToObjectCache<DataTemplate<?>> testCache)
  {
    for (IdentityHashMap.Entry<Object, DataTemplate<?>> controlEntry : controlCache.entrySet())
    {
      Assert.assertSame(testCache.get(controlEntry.getKey()), controlEntry.getValue());
    }
  }

  private void populateTestData(IdentityHashMap<Object, DataTemplate<?>> controlCache,
                                DataObjectToObjectCache<DataTemplate<?>> testCache)
  {
    for (int i=0; i<100; ++i)
    {
      DataMap map = new DataMap();
      DataList list = new DataList();
      Object obj = new Object();
      Bar mapBar = new Bar();
      Bar listBar = new Bar();
      Bar objBar = new Bar();

      controlCache.put(map, mapBar);
      testCache.put(map, mapBar);

      controlCache.put(list, listBar);
      testCache.put(list, listBar);

      controlCache.put(obj, objBar);
      testCache.put(obj, objBar);
    }
  }

  private static class Bar extends RecordTemplate
  {
    public static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema
        (
            "{ \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] }"
        );
    private static final RecordDataSchema.Field FIELD_int = SCHEMA.getField("int");

    public Bar()
    {
      super(new DataMap(), SCHEMA);
    }

    public Integer getInt()
    {
      return obtainDirect(FIELD_int, Integer.TYPE, GetMode.STRICT);
    }

    public Bar setInt(int value)
    {
      putDirect(FIELD_int, Integer.class, Integer.class, value, SetMode.DISALLOW_NULL);
      return this;
    }
  }
}
