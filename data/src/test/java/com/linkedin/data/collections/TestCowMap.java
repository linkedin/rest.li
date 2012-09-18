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

package com.linkedin.data.collections;


import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.Test;

import static com.linkedin.data.collections.TestCommonMap.containsReferenceMap2;
import static com.linkedin.data.collections.TestCommonMap.referenceMap1;
import static com.linkedin.data.collections.TestCommonMap.referenceMap2;
import static com.linkedin.data.collections.TestCommonMap.testAgainstReferenceMap1;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class TestCowMap
{
  @Test
  public void testCopyOnWrite() throws CloneNotSupportedException
  {
    CowMap<String,String> map1 = new CowMap<String,String>(referenceMap1);
    testAgainstReferenceMap1(map1);
    assertEquals(map1.getRefCounted().getRefCount(), 0);

    CowMap<String,String> map2 = map1.clone();
    assertEquals(map1.getRefCounted().getRefCount(), 1);
    assertTrue(map2.getRefCounted() == map1.getRefCounted());
    testAgainstReferenceMap1(map2);
    assertEquals(map1.getRefCounted().getRefCount(), 1);
    assertTrue(map2.getRefCounted() == map1.getRefCounted());

    CowMap<String,String> map3 = map1.clone();
    assertEquals(map1.getRefCounted().getRefCount(), 2);
    assertTrue(map3.getRefCounted() == map1.getRefCounted());
    testAgainstReferenceMap1(map3);
    assertEquals(map1.getRefCounted().getRefCount(), 2);
    assertTrue(map3.getRefCounted() == map1.getRefCounted());

    map3.containsKey("a");
    map3.containsKey("k1");
    map3.containsValue("");
    map3.containsValue("1");
    map3.entrySet();
    map3.get("a");
    map3.get("k1");
    map3.isEmpty();
    map3.keySet();
    map3.size();
    map3.values();
    map3.equals("a");
    map3.equals(map1);
    map3.hashCode();
    map3.toString();
    assertTrue(map3.getRefCounted() == map1.getRefCounted());

    map2.put("k4", "4");
    assertEquals(map2.get("k4"), "4");
    assertTrue(map2.containsKey("k4"));
    assertFalse(map1.containsKey("k4"));
    assertFalse(map3.containsKey("k4"));
    assertTrue(map3.getRefCounted() == map1.getRefCounted());
    assertTrue(map2.getRefCounted() != map1.getRefCounted());
    assertEquals(map1.getRefCounted().getRefCount(), 1);
    assertEquals(map2.getRefCounted().getRefCount(), 0);

    CowMap<String,String> map4 = map3.clone();
    assertTrue(map4.getRefCounted() == map3.getRefCounted());
    assertEquals(map3.getRefCounted().getRefCount(), 2);
    map4.clear();
    assertEquals(map4.size(), 0);
    assertTrue(map4.isEmpty());
    assertEquals(map3.getRefCounted().getRefCount(), 1);
    assertEquals(map4.getRefCounted().getRefCount(), 0);
    assertTrue(map4.getRefCounted() != map3.getRefCounted());

    CowMap<String,String> map5 = map3.clone();
    assertTrue(map5.getRefCounted() == map3.getRefCounted());
    assertEquals(map3.getRefCounted().getRefCount(), 2);
    map5.putAll(referenceMap2);
    containsReferenceMap2(map5);
    assertEquals(map5.size(), referenceMap1.size() + referenceMap2.size());
    assertEquals(map3.getRefCounted().getRefCount(), 1);
    assertEquals(map5.getRefCounted().getRefCount(), 0);
    assertTrue(map5.getRefCounted() != map3.getRefCounted());

    CowMap<String,String> map6 = map3.clone();
    assertTrue(map6.getRefCounted() == map3.getRefCounted());
    assertEquals(map6.getRefCounted().getRefCount(), 2);
    map6.remove("k1");
    assertFalse(map6.containsKey("k1"));
    assertEquals(map6.size(), referenceMap1.size() - 1);
    assertEquals(map3.getRefCounted().getRefCount(), 1);
    assertEquals(map6.getRefCounted().getRefCount(), 0);
    assertTrue(map6.getRefCounted() != map3.getRefCounted());

    CowMap<String,String> map7 = map3.clone();
    assertTrue(map7.getRefCounted() == map3.getRefCounted());
    assertEquals(map7.getRefCounted().getRefCount(), 2);
    map7.containsKey("k1");
    map7.containsValue("1");
    map7.get("k1");
    map7.isEmpty();
    map7.size();
    map7.hashCode();
    map7.equals(map6);
    map7.toString();
    assertTrue(map7.getRefCounted() == map3.getRefCounted());
    map7.setReadOnly();
    Set<Map.Entry<String,String>> set7 = map7.entrySet();
    assertEquals(set7, map7.getRefCounted().getObject().entrySet());
    Set<String> set7a = map7.keySet();
    assertEquals(set7a, map7.getRefCounted().getObject().keySet());
    Collection<String> c7 = map7.values();
    assertEquals(c7, map7.getRefCounted().getObject().values());
    assertTrue(map7.getRefCounted() == map3.getRefCounted());
    map7.invalidate();
    assertEquals(map3.getRefCounted().getRefCount(), 1);
    assertTrue(map7.getRefCounted() == null);

    Exception exc = null;

    CowMap<String,String> map8 = map3.clone();
    assertTrue(map8.getRefCounted() == map3.getRefCounted());
    assertEquals(map8.getRefCounted().getRefCount(), 2);
    Set<Map.Entry<String,String>> set8 = map8.entrySet();
    assertEquals(set8, map8.getRefCounted().getObject().entrySet());
    assertTrue(map8.getRefCounted() == map3.getRefCounted());
    try
    {
      exc = null;
      set8.clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    map8.invalidate();

    CowMap<String,String> map9 = map3.clone();
    assertTrue(map9.getRefCounted() == map3.getRefCounted());
    assertEquals(map9.getRefCounted().getRefCount(), 2);
    Set<String> set9 = map9.keySet();
    assertEquals(set9, map9.getRefCounted().getObject().keySet());
    assertTrue(map9.getRefCounted() == map3.getRefCounted());
    try
    {
      exc = null;
      set9.clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    map9.invalidate();

    CowMap<String,String> map10 = map3.clone();
    assertTrue(map10.getRefCounted() == map3.getRefCounted());
    assertEquals(map10.getRefCounted().getRefCount(), 2);
    Collection<String> c10 = map10.values();
    assertEquals(c10, map10.getRefCounted().getObject().values());
    assertTrue(map10.getRefCounted() == map3.getRefCounted());
    try
    {
      exc = null;
      c10.clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    map10.invalidate();
  }
}
