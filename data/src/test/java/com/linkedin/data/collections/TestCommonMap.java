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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestCommonMap
{
  public static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));

  @SuppressWarnings("serial")
  public static final Map<String,String> referenceMap1 = new HashMap<String,String>()
  {
    {
      put("k1", "1");
      put("k2", "2");
      put("k3", "3");
    }
  };

  @SuppressWarnings("serial")
  public static final Map<String,String> referenceMap2 = new HashMap<String,String>()
  {
    {
      put("kA", "A");
      put("kB", "B");
      put("kC", "C");
    }
  };

  public static void testAgainstReferenceMap1(Map<String,String> map)
  {
    assertEquals(map.get("k1"), "1");
    assertEquals(map.get("k2"), "2");
    assertEquals(map.get("k3"), "3");
    assertTrue(map.containsKey("k1"));
    assertTrue(map.containsKey("k2"));
    assertTrue(map.containsKey("k3"));
    assertTrue(map.containsValue("1"));
    assertTrue(map.containsValue("2"));
    assertTrue(map.containsValue("3"));
    assertTrue(map.toString().indexOf("k1=1") != -1);
    assertTrue(map.toString().indexOf("k2=2") != -1);
    assertTrue(map.toString().indexOf("k3=3") != -1);
    assertEquals(map.size(), 3);
    assertTrue(map.equals(referenceMap1));
    assertEquals(map.hashCode(), referenceMap1.hashCode());
    assertFalse(map.isEmpty());
    assert(map.entrySet().equals(referenceMap1.entrySet()));
    assertEquals(map.keySet(), referenceMap1.keySet());
    assertEquals(new HashSet<>(map.values()), new HashSet<>(referenceMap1.values()));
  }

  public static void containsReferenceMap2(Map<String,String> map)
  {
    assertEquals(map.get("kA"), "A");
    assertEquals(map.get("kB"), "B");
    assertEquals(map.get("kC"), "C");
  }

  public static void verifyReadOnly(CommonMap<String,String> map)
  {
    Exception exc = null;
    assertTrue(map.isReadOnly());
    try
    {
      exc = null;
      map.clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    try
    {
      exc = null;
      map.put("kX", "X");
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    try
    {
      exc = null;
      map.remove("k1");
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    try
    {
      exc = null;
      map.entrySet().clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    try
    {
      exc = null;
      for (Map.Entry<String,String> e : map.entrySet())
      {
        e.setValue("x");
      }
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    try
    {
      exc = null;
      map.keySet().clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    try
    {
      exc = null;
      map.values().clear();
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
  }

  @Test(dataProvider = "factories")
  public void testConstructor(CommonMapFactory factory)
  {
    CommonMap<String,String> map1 = factory.create();
    map1.put("k1", "1");
    map1.put("k2", "2");
    map1.put("k3", "3");
    testAgainstReferenceMap1(map1);

    CommonMap<String,String> map2 = factory.create(referenceMap1);
    testAgainstReferenceMap1(map2);

    CommonMap<String,String> map3 = factory.create(10);
    map3.putAll(referenceMap1);
    testAgainstReferenceMap1(map3);

    CommonMap<String,String> map4 = factory.create(2, 0.5f);
    map4.putAll(referenceMap1);
    testAgainstReferenceMap1(map4);
  }

  @Test(dataProvider = "factories")
  public void testReadOnly(CommonMapFactory factory)
  {
    CommonMap<String,String> map1 = factory.create(referenceMap1);
    map1.setReadOnly();
    testAgainstReferenceMap1(map1);
    verifyReadOnly(map1);
  }

  @Test(dataProvider = "factories")
  public void testClone(CommonMapFactory factory) throws CloneNotSupportedException
  {
    CommonMap<String,String> map1 = factory.create(referenceMap1);
    assertFalse(map1.isReadOnly());
    CommonMap<String,String> map2 = map1.clone();
    assertFalse(map2.isReadOnly());
    map2.setReadOnly();
    assertTrue(map2.isReadOnly());
    CommonMap<String,String> map3 = map2.clone();
    assertFalse(map3.isReadOnly());
    assertTrue(map2.isReadOnly());
    assertFalse(map1.isReadOnly());
  }

  static class Checker<K,V> implements MapChecker<K,V>
  {
    int checkCount = 0;
    CommonMap<K,V> lastMap;
    K lastKey;
    V lastValue;

    @Override
    public void checkKeyValue(CommonMap<K, V> map, K key, V value)
    {
      checkCount++;
      lastMap = map;
      lastKey = key;
      lastValue = value;
    }
  }

  @Test(dataProvider = "factories")
  public void testCheckKeyValue(CommonMapFactory factory) throws CloneNotSupportedException
  {
    Checker<String, String> checker1 = new Checker<>();

    CommonMap<String,String> map1 = factory.create(checker1);
    assertEquals(checker1.checkCount, 0);

    Checker<String, String> checker2 = new Checker<>();
    CommonMap<String,String> map2 = factory.create(referenceMap1, checker2);
    assertEquals(checker2.checkCount, referenceMap1.size());

    CommonMap<String,String> map3 = map1.clone();
    String key = "a";
    String value = "v";
    map3.put(key, value);
    assertEquals(checker1.checkCount, 1);
    assertSame(checker1.lastMap, map3);
    assertSame(checker1.lastKey, key);
    assertSame(checker1.lastValue, value);

    CommonMap<String,String> map4 = map2.clone();
    map4.put("a", "v");
    assertEquals(checker2.checkCount, referenceMap1.size() + 1);

    map3.putAll(referenceMap1);
    assertEquals(checker1.checkCount, referenceMap1.size() + 1);
  }

  @Test(dataProvider = "factories")
  public void testEntrySetKeySetValues(CommonMapFactory factory)
  {
    int count = 10;
    Map<String, String> map = factory.create();
    for (Integer i = 0; i < count; i++)
    {
      String v = i.toString();
      map.put(v, v);
    }

    // entrySet
    for (Map.Entry<String, String> e : map.entrySet())
    {
      map.put(e.getKey(), "X" + e.getKey() + "X");
    }
    for (Integer i = 0; i < count; i++)
    {
      String k = i.toString();
      assertEquals(map.get(k), "X" + k + "X");
    }

    // keySet
    Set<String> expectedKeys = new HashSet<>();
    for (Integer i = 0; i < count; i++)
    {
      String v = i.toString();
      expectedKeys.add(v);
    }
    assertEquals(map.keySet(), expectedKeys);

    // values
    Collection<String> expectedValues = new ArrayList<>();
    for (Integer i = 0; i < count; i++)
    {
      expectedValues.add("X" + i + "X");
    }
    assert(map.values().containsAll(expectedValues));
    assert(expectedValues.containsAll(map.values()));
  }

  @DataProvider(name = "factories")
  public Object[][] mapFactories()
  {
    return new Object[][] {
      { new CowMapFactory() },
      { new CheckedMapFactory() }
    };
  }

  public interface CommonMapFactory
  {
    <K,V> CommonMap<K,V> create();
    <K,V> CommonMap<K,V> create(int initialCapacity);
    <K,V> CommonMap<K,V> create(int initialCapacity, float factor);
    <K,V> CommonMap<K,V> create(Map<K,V> map);
    <K,V> CommonMap<K,V> create(MapChecker<K,V> checker);
    <K,V> CommonMap<K,V> create(Map<K,V> map, MapChecker<K,V> checker);
  }

  public static class CowMapFactory implements CommonMapFactory
  {
    public <K,V> CommonMap<K,V> create()
    {
      return new CowMap<>();
    }
    public <K,V> CommonMap<K,V> create(int initialCapacity)
    {
      return new CowMap<>(initialCapacity);
    }
    public <K,V> CommonMap<K,V> create(int initialCapacity, float factor)
    {
      return new CowMap<>(initialCapacity, factor);
    }
    public <K,V> CommonMap<K,V> create(Map<K,V> map)
    {
      return new CowMap<>(map);
    }
    public <K,V> CommonMap<K,V> create(MapChecker<K,V> checker)
    {
      return new CowMap<>(checker);
    }
    public <K,V> CommonMap<K,V> create(Map<K,V> map, MapChecker<K,V> checker)
    {
      return new CowMap<>(map, checker);
    }
  }

  public static class CheckedMapFactory implements CommonMapFactory
  {
    public <K,V> CommonMap<K,V> create()
    {
      return new CheckedMap<>();
    }
    public <K,V> CommonMap<K,V> create(int initialCapacity)
    {
      return new CheckedMap<>(initialCapacity);
    }
    public <K,V> CommonMap<K,V> create(int initialCapacity, float factor)
    {
      return new CheckedMap<>(initialCapacity, factor);
    }
    public <K,V> CommonMap<K,V> create(Map<K,V> map)
    {
      return new CheckedMap<>(map);
    }
    public <K,V> CommonMap<K,V> create(MapChecker<K,V> checker)
    {
      return new CheckedMap<>(checker);
    }
    public <K,V> CommonMap<K,V> create(Map<K,V> map, MapChecker<K,V> checker)
    {
      return new CheckedMap<>(map, checker);
    }
  }
}
