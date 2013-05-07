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


import com.linkedin.data.ByteString;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asMap;
import static com.linkedin.data.TestUtil.noCommonDataComplex;
import static com.linkedin.data.TestUtil.out;
import static org.testng.Assert.*;

/**
 * Unit tests for map {@link DataTemplate}'s.
 *
 * @author slim
 */
public class TestMapTemplate
{
  static <E> void assertCollectionEquals(Collection<E> c1, Collection<E> c2)
  {
    assertTrue(c1.containsAll(c2));
    assertTrue(c2.containsAll(c1));
  }

  public static <MapTemplate extends AbstractMapTemplate<E>, E>
  void testMap(Class<MapTemplate> templateClass,
               MapDataSchema schema,
               Map<String, E> input,
               Map<String, E> adds)
  {
    try
    {
      Exception exc = null;

      // constructor and putall
      MapTemplate map1 = templateClass.newInstance();
      map1.putAll(input);
      assertEquals(map1, input);

      /*
      Constructor[] constructors = templateClass.getConstructors();
      for (Constructor c : constructors)
      {
        out.println(c);
      }
      */
      try
      {
        int size = input.size();

        // constructor(int capacity)
        Constructor<MapTemplate> capacityConstructor = templateClass.getConstructor(int.class);
        MapTemplate map = capacityConstructor.newInstance(input.size());
        assertEquals(map, Collections.emptyMap());
        map.putAll(input);
        assertEquals(input, map);
        map.clear();
        assertEquals(size, input.size());

        // constructor(int capacity, float loadFactor)
        Constructor<MapTemplate> loadFactorConstructor = templateClass.getConstructor(int.class, float.class);
        map = loadFactorConstructor.newInstance(input.size(), input.size() * 1.4f);
        assertEquals(map, Collections.emptyMap());
        map.putAll(input);
        assertEquals(input, map);
        map.clear();
        assertEquals(size, input.size());

        // constructor(Map<String, E>)
        Constructor<MapTemplate> mapConstructor = templateClass.getConstructor(Map.class);
        map = mapConstructor.newInstance(input);
        assertEquals(input, map);
        map.clear();
        assertEquals(size, input.size());

        // constructor(DataMap)
        Constructor<MapTemplate> dataMapConstructor = templateClass.getConstructor(DataMap.class);
        map = dataMapConstructor.newInstance(map1.data());
        assertEquals(map1, map);
        assertEquals(input, map);
        map.clear();
        assertEquals(map1, map);
      }
      catch (Exception e)
      {
        assertSame(e, null);
      }

      // test wrapping
      map1.clear();
      map1.putAll(input);
      DataMap dataMap2 = new DataMap();
      MapTemplate map2 = DataTemplateUtil.wrap(dataMap2, schema, templateClass);
      for (Map.Entry<String, E> e : input.entrySet())
      {
        E v = e.getValue();
        Object o;
        if (v instanceof DataTemplate)
        {
          o = ((DataTemplate<?>) v).data();
        }
        else if (v instanceof Enum)
        {
          o = v.toString();
        }
        else
        {
          o = v;
        }
        dataMap2.put(e.getKey(), o);
        assertEquals(map2.hashCode(), dataMap2.hashCode());
      }
      assertEquals(map1, map2);
      MapTemplate map2a = DataTemplateUtil.wrap(dataMap2, templateClass);
      assertEquals(map1, map2a);
      assertSame(map2a.data(), map2.data());

      // valueClass()
      assertSame(map1.valueClass(), input.values().iterator().next().getClass());

      // equals(), hashCode()
      map1.clear();
      map1.putAll(input);
      assertTrue(map1.equals(map1));
      assertTrue(map1.equals(input));
      assertFalse(map1.equals(null));
      assertFalse(map1.equals(adds));
      assertFalse(map1.equals(new HashMap<String,E>()));
      map2.clear();
      Map<String,E> hashMap2 = new HashMap<String,E>();
      List<Map.Entry<String,E>> inputList = new ArrayList<Map.Entry<String, E>>(input.entrySet());
      int lastHash = 0;
      for (int i = 0; i < inputList.size(); ++i)
      {
        Map.Entry<String,E> entry = inputList.get(i);
        map2.put(entry.getKey(), entry.getValue());
        hashMap2.put(entry.getKey(), entry.getValue());

        if (i == (inputList.size() - 1))
        {
          assertTrue(map1.equals(map2));
          assertTrue(map1.equals(hashMap2));
        }
        else
        {
          assertFalse(map1.equals(map2));
          assertFalse(map1.equals(hashMap2));
        }

        int newHash = map2.hashCode();
        if (i > 0)
        {
          assertFalse(lastHash == newHash);
        }

        lastHash = newHash;
      }

      // schema()
      MapDataSchema schema1 = map1.schema();
      assertTrue(schema1 != null);
      assertEquals(schema1.getType(), DataSchema.Type.MAP);
      assertEquals(schema1, schema);

      // get(Object key), put(K key, V value, containsKey(Object key), containsValue(Object value), toString
      MapTemplate map3 = templateClass.newInstance();
      for (Map.Entry<String, E> e : input.entrySet())
      {
        String key = e.getKey();
        E value = e.getValue();
        E replaced = map3.put(key, value);
        assertTrue(replaced == null);
        E got = map3.get(key);
        assertTrue(got != null);
        assertEquals(value, got);
        assertSame(value, got);
        assertTrue(map3.containsKey(key));
        assertTrue(map3.containsValue(value));
        assertTrue(map3.toString().contains(key + "=" + value));
      }
      assertNull(map3.get(null));
      assertNull(map3.get(1));
      assertNull(map3.get(new Object()));
      assertNull(map3.get("not found"));

      assertEquals(map3, input);
      Iterator<Map.Entry<String, E>> e2 = adds.entrySet().iterator();
      for (Map.Entry<String, E> e : input.entrySet())
      {
        if (e2.hasNext() == false)
          break;
        E newValue = e2.next().getValue();
        String key = e.getKey();
        E value = e.getValue();
        assertTrue(map3.containsKey(key));
        assertTrue(map3.containsValue(value));
        E replaced = map3.put(key, newValue);
        assertTrue(replaced != null);
        assertEquals(replaced, value);
        assertSame(replaced, value);
        assertTrue(map3.containsKey(key));
        assertTrue(map3.containsValue(newValue));
        assertTrue(map3.toString().contains(key));
        assertTrue(map3.toString().contains(newValue.toString()));
      }

      // put(String key, E value) with replacement of existing value
      map3.clear();
      String testKey = "testKey";
      E lastValue = null;
      for (Map.Entry<String, E> e : input.entrySet())
      {
        E putResult = map3.put(testKey, e.getValue());
        if (lastValue != null)
        {
          assertEquals(putResult, lastValue);
        }
        lastValue = e.getValue();
      }

      // remove(Object key), containsKey(Object key), containsValue(Object value)
      MapTemplate map4 = templateClass.newInstance();
      map4.putAll(input);
      int map4Size = map4.size();
      for (Map.Entry<String, E> e : input.entrySet())
      {
        String key = e.getKey();
        E value = e.getValue();
        assertTrue(map4.containsKey(key));
        assertTrue(map4.containsValue(value));
        E removed = map4.remove(key);
        assertTrue(removed != null);
        assertEquals(value, removed);
        assertSame(value, removed);
        assertFalse(map4.containsKey(key));
        assertFalse(map4.containsValue(value));
        map4Size--;
        assertEquals(map4Size, map4.size());
      }
      assertTrue(map4.isEmpty());
      assertTrue(map4.size() == 0);
      assertFalse(map4.containsValue(null));
      assertFalse(map4.containsValue(new StringArray()));

      // clone and copy return types
      TestDataTemplateUtil.assertCloneAndCopyReturnType(templateClass);

      // clone
      exc = null;
      map4 = templateClass.newInstance();
      map4.putAll(input);
      try
      {
        exc = null;
        @SuppressWarnings("unchecked")
        MapTemplate map4Clone = (MapTemplate) map4.clone();
        assertTrue(map4Clone.getClass() == templateClass);
        assertEquals(map4Clone, map4);
        assertTrue(map4Clone != map4);
        for (Map.Entry<String, Object> entry : map4.data().entrySet())
        {
          if (entry.getValue() instanceof DataComplex)
          {
            assertSame(map4Clone.data().get(entry.getKey()), entry.getValue());
          }
        }
        String key = map4Clone.keySet().iterator().next();
        map4Clone.remove(key);
        assertEquals(map4Clone.size(), map4.size() - 1);
        assertFalse(map4Clone.equals(map4));
        assertTrue(map4.entrySet().containsAll(map4Clone.entrySet()));
        map4.remove(key);
        assertEquals(map4Clone, map4);
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assert(exc == null);

      //copy
      MapTemplate map4a = templateClass.newInstance();
      map4a.putAll(input);
      try
      {
        @SuppressWarnings("unchecked")
        MapTemplate map4aCopy = (MapTemplate) map4a.copy();
        assertTrue(map4aCopy.getClass() == templateClass);
        assertEquals(map4a, map4aCopy);
        boolean hasComplex = false;
        for (Map.Entry<String, Object> entry : map4a.data().entrySet())
        {
          if (entry.getValue() instanceof DataComplex)
          {
            assertNotSame(map4aCopy.data().get(entry.getKey()), entry.getValue());
            hasComplex = true;
          }
        }
        assertTrue(DataTemplate.class.isAssignableFrom(map4a._valueClass) == false || hasComplex);
        assertTrue(noCommonDataComplex(map4a.data(), map4aCopy.data()));
        boolean mutated = false;
        for (Object value : map4aCopy.data().values())
        {
          if (value instanceof DataComplex)
          {
            mutated |= TestUtil.mutateDataComplex((DataComplex) value);
          }
        }
        assertEquals(mutated, hasComplex);
        if (mutated)
        {
          assertNotEquals(map4aCopy, map4a);
        }
        else
        {
          assertEquals(map4aCopy, map4a);
          String key = map4aCopy.keySet().iterator().next();
          map4aCopy.remove(key);
          assertFalse(map4aCopy.containsKey(key));
          assertTrue(map4a.containsKey(key));
        }
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assert(exc == null);

      // entrySet, keySet, values, clear
      MapTemplate map5 = templateClass.newInstance();
      map5.putAll(input);
      assertEquals(map5.entrySet(), input.entrySet());
      assertCollectionEquals(map5.entrySet(), input.entrySet());
      assertEquals(map5.keySet(), input.keySet());
      assertCollectionEquals(map5.keySet(), input.keySet());
      assertCollectionEquals(map5.values(), input.values());

      assertEquals(map5.size(), input.size());
      assertFalse(map5.isEmpty());
      map5.clear();
      assertEquals(map5.size(), 0);
      assertTrue(map5.isEmpty());

      map5.putAll(adds);
      assertEquals(map5.entrySet(), adds.entrySet());
      assertEquals(map5.keySet(), adds.keySet());
      assertCollectionEquals(map5.values(), adds.values());

      assertEquals(map5.size(), adds.size());
      assertFalse(map5.isEmpty());
      map5.clear();
      assertEquals(map5.size(), 0);
      assertTrue(map5.isEmpty());

      // entrySet contains
      MapTemplate map6 = templateClass.newInstance();
      Set<Map.Entry<String, E>> entrySet6 = map6.entrySet();
      for (Map.Entry<String, E> e : input.entrySet())
      {
        assertFalse(entrySet6.contains(e));
        map6.put(e.getKey(), e.getValue());
        assertTrue(entrySet6.contains(e));
      }
      for (Map.Entry<String, E> e : adds.entrySet())
      {
        assertFalse(entrySet6.contains(e));
      }
      assertFalse(entrySet6.contains(null));
      assertFalse(entrySet6.contains(1));
      assertFalse(entrySet6.contains(new Object()));
      assertFalse(entrySet6.contains(new AbstractMap.SimpleEntry<String, Object>(null, null)));
      assertFalse(entrySet6.contains(new AbstractMap.SimpleEntry<String, Object>("xxxx", null)));
      assertFalse(entrySet6.contains(new AbstractMap.SimpleEntry<String, Object>("xxxx", "xxxx")));
      assertFalse(entrySet6.contains(new AbstractMap.SimpleEntry<String, Object>("xxxx", new Object())));

      // entrySet iterator
      for (Map.Entry<String, E> e : map6.entrySet())
      {
        assertTrue(map6.containsKey(e.getKey()));
        assertTrue(map6.containsValue(e.getValue()));
      }
      try
      {
        exc = null;
        map6.entrySet().iterator().remove();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof UnsupportedOperationException);

      // entrySet containsAll
      assertTrue(map6.entrySet().containsAll(input.entrySet()));
      assertTrue(input.entrySet().containsAll(map6.entrySet()));

      // entrySet add
      for (Map.Entry<String, E> e : input.entrySet())
      {
        try
        {
          exc = null;
          entrySet6.add(e);
        }
        catch (Exception ex)
        {
          exc = ex;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof UnsupportedOperationException);
      }

      // entrySet remove
      for (Map.Entry<String, E> e : input.entrySet())
      {
        try
        {
          exc = null;
          entrySet6.remove(e);
        }
        catch (Exception ex)
        {
          exc = ex;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof UnsupportedOperationException);
      }

      Object invalidEntries[] = { null, 1, new Object() };
      for (Object e : invalidEntries)
      {
        try
        {
          exc = null;
          entrySet6.remove(e);
        }
        catch (Exception ex)
        {
          exc = ex;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof UnsupportedOperationException);
      }

      // entrySet addAll
      try
      {
        exc = null;
        entrySet6.addAll(adds.entrySet());
      }
      catch (Exception ex)
      {
        exc = ex;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof UnsupportedOperationException);

      // entrySet clear
      try
      {
        exc = null;
        entrySet6.clear();
      }
      catch (Exception ex)
      {
        exc = ex;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof UnsupportedOperationException);

      // entrySet removeAll
      Collection<?> collectionsToRemove[] = { adds.entrySet(), Collections.emptySet() };
      for (Collection<?> c : collectionsToRemove)
      {
        try
        {
          exc = null;
          entrySet6.removeAll(c);
        }
        catch (Exception ex)
        {
          exc = ex;
        }
        assertTrue(exc != null);
        assertTrue(exc instanceof UnsupportedOperationException);
      }

      // entrySet retainAll
      try
      {
        exc = null;
        entrySet6.retainAll(adds.entrySet());
      }
      catch (Exception ex)
      {
        exc = ex;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof UnsupportedOperationException);

      // entrySet equals, isEmpty
      MapTemplate map7 = templateClass.newInstance();
      MapTemplate map8 = templateClass.newInstance();
      map8.putAll(input);
      Set<Map.Entry<String, E>> entrySet7 = map7.entrySet();
      assertTrue(entrySet7.isEmpty());
      Map<String, E> hashMap7 = new HashMap<String, E>();
      lastHash = 0;
      for (int i = 0; i < inputList.size(); ++i)
      {
        Map.Entry<String,E> entry = inputList.get(i);
        map7.put(entry.getKey(), entry.getValue());
        hashMap7.put(entry.getKey(), entry.getValue());

        if (i == (inputList.size() - 1))
        {
          assertTrue(entrySet7.equals(map8.entrySet()));
        }
        else
        {
          assertFalse(entrySet7.equals(map8.entrySet()));
          assertTrue(entrySet7.equals(hashMap7.entrySet()));
        }
        assertTrue(entrySet7.toString().contains(entry.getKey()));
        assertTrue(entrySet7.toString().contains(entry.getValue().toString()));

        int newHash = entrySet7.hashCode();
        if (i > 0)
        {
          assertFalse(lastHash == newHash);
        }

        lastHash = newHash;

        assertFalse(entrySet7.isEmpty());
      }
      assertTrue(entrySet7.equals(input.entrySet()));
      assertFalse(map7.entrySet().equals(null));
      assertFalse(map7.entrySet().equals(new Object()));

      // test Map.Entry.set()
      MapTemplate map9 = templateClass.newInstance();
      map9.putAll(input);
      lastValue = null;
      for (Map.Entry<String, E> e : map9.entrySet())
      {
        E value = e.getValue();
        if (lastValue != null)
        {
          exc = null;
          try
          {
            E ret = e.setValue(lastValue);
            /* CowMap entrySet() returns unmodifiable view.
               This may change if don't use CowMap to back DataMap.
            assertEquals(ret, value);
            assertEquals(e.getValue(), lastValue);
            assertEquals(map9.get(e.getKey()), lastValue);
            */
          }
          catch (Exception ex)
          {
            exc = ex;
          }
          assertTrue(exc instanceof UnsupportedOperationException);
        }
        lastValue = value;
      }
    }
    catch (IllegalAccessException exc)
    {
      fail("Unexpected exception", exc);
    }
    catch (InstantiationException exc)
    {
      fail("Unexpected exception", exc);
    }
  }

  public <MapTemplate extends AbstractMapTemplate<E>, E>
  void testMapBadInput(Class<MapTemplate> templateClass,
                       MapDataSchema schema,
                       Map<String, E> good,
                       Map<String, Object> badInput,
                       Map<String, Object> badOutput)
  {

    MapTemplate mapTemplateBad = null;
    try
    {
      mapTemplateBad = templateClass.newInstance();
    }
    catch (IllegalAccessException exc)
    {
      fail("Unexpected exception", exc);
    }
    catch (InstantiationException exc)
    {
      fail("Unexpected exception", exc);
    }

    Exception exc = null;
    DataMap badDataMap = new DataMap();

    @SuppressWarnings("unchecked")
    Map<String, E> badIn = (Map<String, E>) badInput;

    // put(String key, E element)
    for (Map.Entry<String, E> entry : badIn.entrySet())
    {
      exc = null;
      E value = entry.getValue();
      try
      {
        mapTemplateBad.put(entry.getKey(), value);
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(value == null || exc instanceof ClassCastException);
      assertTrue(value != null || exc instanceof NullPointerException);
    }

    // putAll(Collection<E> c)
    try
    {
      exc = null;
      mapTemplateBad.putAll(badIn);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof ClassCastException);

    badDataMap.clear();
    badDataMap.putAll(badOutput);
    MapTemplate badWrappedMapTemplate = DataTemplateUtil.wrap(badDataMap, schema, templateClass);

    // Get bad
    for (String key : badOutput.keySet())
    {
      try
      {
        exc = null;
        badWrappedMapTemplate.get(key);
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    // Set returns bad
    badDataMap.clear();
    badDataMap.putAll(badOutput);
    assertEquals(badWrappedMapTemplate.size(), badOutput.size());
    for (String key : badOutput.keySet())
    {
      try
      {
        exc = null;
        badWrappedMapTemplate.put(key, good.get(good.keySet().iterator().next()));
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    // Remove returns bad
    badDataMap.clear();
    badDataMap.putAll(badOutput);
    assertEquals(badWrappedMapTemplate.size(), badOutput.size());
    for (String key : badOutput.keySet())
    {
      try
      {
        exc = null;
        badWrappedMapTemplate.remove(key);
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    // entrySet returns bad
    badDataMap.clear();
    badDataMap.putAll(badOutput);
    for (Map.Entry<String, E> entry : badWrappedMapTemplate.entrySet())
    {
      try
      {
        exc = null;
        entry.getValue();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }
  }

  @SuppressWarnings("unchecked")
  public <MapTemplate extends AbstractMapTemplate<E>, E extends Number>
  void testNumberMap(Class<MapTemplate> templateClass,
                     MapDataSchema schema,
                     Map<String, E> castTo,
                     Map<String, ? extends Number> castFrom)
  {
    try
    {
      // test insert non-native, converted to element type on set
      MapTemplate map1 = templateClass.newInstance();
      map1.putAll((Map<String, E>) castFrom);
      for (String i : castFrom.keySet())
      {
        assertEquals(castTo.get(i), map1.get(i));
        assertEquals(map1.data().get(i), castTo.get(i));
      }

      // test underlying is non-native, convert on get to element type on get.
      DataMap dataList2 = new DataMap(castFrom);
      MapTemplate map2 = DataTemplateUtil.wrap(dataList2, schema, templateClass);
      for (String i : castTo.keySet())
      {
        assertSame(dataList2.get(i), castFrom.get(i));
        assertEquals(castTo.get(i), map2.get(i));
      }

      // test entrySet, convert on access to element type
      int lastHash = 0;
      boolean first = true;
      for (Map.Entry<String, E> e : map2.entrySet())
      {
        String key = e.getKey();
        E castToValue = castTo.get(key);
        assertEquals(e.getValue(), castToValue);
        assertTrue(e.equals(new AbstractMap.SimpleEntry(key, castToValue)));
        assertFalse(e.equals(null));
        assertFalse(e.equals(new Object()));

        String eToString = e.toString();
        assertEquals(eToString, key + "=" + castToValue.toString());

        // hashCode
        int newHash = e.hashCode();
        if (!first)
        {
          assertTrue(lastHash != newHash);
        }

        first = false;
        lastHash = newHash;
      }
    }
    catch (IllegalAccessException exc)
    {
      fail("Unexpected exception", exc);
    }
    catch (InstantiationException exc)
    {
      fail("Unexpected exception", exc);
    }
  }

  @Test
  public void testBooleanMap()
  {
    MapDataSchema schema = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"boolean\" }");

    Map<String, Boolean> input = asMap("true", true, "false", false);
    Map<String, Boolean> adds = asMap("thirteen", true, "seventeen", false, "nineteen", true);
    Map<String, Object> badInput = asMap("integer", 99, "long", 999L, "float", 88.0f, "double", 888.0, "string", "hello",
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("integer", 99, "long", 999L, "float", 88.0f, "double", 888.0, "string", "hello",
                                          "bytes", ByteString.empty(),
                                          "map", new DataMap(),
                                          "list", new DataList());

    testMap(BooleanMap.class, schema, input, adds);
    testMapBadInput(BooleanMap.class, schema, input, badInput, badOutput);
  }

  @Test
  public void testIntegerMap()
  {
    MapDataSchema schema = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"int\" }");

    Map<String, Integer> input = asMap("one", 1, "three", 3, "five", 5, "seven", 7, "eleven", 11);
    Map<String, Integer> adds = asMap("thirteen", 13, "seventeen", 17, "nineteen", 19);
    Map<String, Object> badInput = asMap("boolean", true, "string", "hello",
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("boolean", true, "string", "hello",
                                          "bytes", ByteString.empty(),
                                          "map", new DataMap(),
                                          "list", new DataList());

    testMap(IntegerMap.class, schema, input, adds);
    testMapBadInput(IntegerMap.class, schema, input, badInput, badOutput);

    Map<String, ? extends Number> castFrom = asMap("one", 1L, "three", 3.0f, "five", 5.0, "seven", 7, "eleven", 11);
    testNumberMap(IntegerMap.class, schema, input, castFrom);
  }

  @Test
  public void testLongMap()
  {
    MapDataSchema schema = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"long\" }");

    Map<String, Long> input = asMap("one", 1L, "three", 3L, "five", 5L, "seven", 7L, "eleven", 11L);
    Map<String, Long> adds = asMap("thirteen", 13L, "seventeen", 17L, "nineteen", 19L);
    Map<String, Object> badInput = asMap("boolean", true, "string", "hello",
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("boolean", true, "string", "hello",
                                          "bytes", ByteString.empty(),
                                          "map", new DataMap(),
                                          "list", new DataList());

    testMap(LongMap.class, schema, input, adds);
    testMapBadInput(LongMap.class, schema, input, badInput, badOutput);

    Map<String, ? extends Number> castFrom = asMap("one", 1, "three", 3.0f, "five", 5.0, "seven", 7, "eleven", 11);
    testNumberMap(LongMap.class, schema, input, castFrom);
  }

  @Test
  public void testFloatMap()
  {
    MapDataSchema schema = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"float\" }");

    Map<String, Float> input = asMap("one", 1.0f, "three", 3.0f, "five", 5.0f, "seven", 7.0f, "eleven", 11.0f);
    Map<String, Float> adds = asMap("thirteen", 13.0f, "seventeen", 17.0f, "nineteen", 19.0f);
    Map<String, Object> badInput = asMap("boolean", true, "string", "hello",
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("boolean", true, "string", "hello",
                                          "bytes", ByteString.empty(),
                                          "map", new DataMap(),
                                          "list", new DataList());

    testMap(FloatMap.class, schema, input, adds);
    testMapBadInput(FloatMap.class, schema, input, badInput, badOutput);

    Map<String, ? extends Number> castFrom = asMap("one", 1, "three", 3L, "five", 5.0, "seven", 7, "eleven", 11);
    testNumberMap(FloatMap.class, schema, input, castFrom);
  }

  @Test
  public void testDoubleMap()
  {
    MapDataSchema schema = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"double\" }");

    Map<String, Double> input = asMap("one", 1.0, "three", 3.0, "five", 5.0, "seven", 7.0, "eleven", 11.0);
    Map<String, Double> adds = asMap("thirteen", 13.0, "seventeen", 17.0, "nineteen", 19.0);
    Map<String, Object> badInput = asMap("boolean", true, "string", "hello",
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("boolean", true, "string", "hello",
                                          "bytes", ByteString.empty(),
                                          "map", new DataMap(),
                                          "list", new DataList());

    testMap(DoubleMap.class, schema, input, adds);
    testMapBadInput(DoubleMap.class, schema, input, badInput, badOutput);

    Map<String, ? extends Number> castFrom = asMap("one", 1, "three", 3L, "five", 5.0f, "seven", 7, "eleven", 11);
    testNumberMap(DoubleMap.class, schema, input, castFrom);
  }

  @Test
  public void testStringMap()
  {
    MapDataSchema schema = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"string\" }");

    Map<String, String> input = asMap("one", "1", "three", "3", "five", "5", "seven", "7", "eleven", "11");
    Map<String, String> adds = asMap("thirteen", "13", "seventeen", "17", "nineteen", "19");
    Map<String, Object> badInput = asMap("boolean", true, "integer", 99, "long", 999L, "float", 88.0f, "double", 888.0,
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("boolean", true, "integer", 99, "long", 999L, "float", 88.0f, "double", 888.0,
                                          "bytes", ByteString.empty(),
                                          "map", new DataMap(),
                                          "list", new DataList());

    testMap(StringMap.class, schema, input, adds);
    testMapBadInput(StringMap.class, schema, input, badInput, badOutput);
  }

  @Test
  public void testBytesMap()
  {
    MapDataSchema schema = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"bytes\" }");

    Map<String, ByteString> input = asMap("one", ByteString.copyAvroString("1", false), "three", ByteString.copyAvroString("3", false), "five", ByteString.copyAvroString("5", false), "seven", ByteString.copyAvroString("7", false), "eleven", ByteString.copyAvroString("11", false));
    Map<String, ByteString> adds = asMap("thirteen", ByteString.copyAvroString("13", false), "seventeen", ByteString.copyAvroString("17", false), "nineteen", ByteString.copyAvroString("19", false));
    Map<String, Object> badInput = asMap("boolean", true, "integer", 99, "long", 999L, "float", 88.0f, "double", 888.0,
                                         "data", "\u0100",
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("boolean", true, "integer", 99, "long", 999L, "float", 88.0f, "double", 888.0,
                                          "data", "\u0100",
                                          "map", new DataMap(),
                                          "list", new DataList());

    testMap(BytesMap.class, schema, input, adds);
    testMapBadInput(BytesMap.class, schema, input, badInput, badOutput);
  }

  public static enum Fruits
  {
    APPLE, ORANGE, BANANA, GRAPES, PINEAPPLE
  }

  public static class EnumMapTemplate extends com.linkedin.data.template.DirectMapTemplate<Fruits>
  {
    public static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : { \"type\" : \"enum\", \"name\" : \"Fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\", \"BANANA\", \"GRAPES\", \"PINEAPPLE\" ] } }");
    public EnumMapTemplate()
    {
      this(new DataMap());
    }
    public EnumMapTemplate(int capacity)
    {
      this(new DataMap(capacity));
    }
    public EnumMapTemplate(int capacity, float loadFactor)
    {
      this(new DataMap(capacity, loadFactor));
    }
    public EnumMapTemplate(Map<String, Fruits> map)
    {
      this(newDataMapOfSize(map.size()));
      putAll(map);
    }
    public EnumMapTemplate(DataMap map)
    {
      super(map, SCHEMA, Fruits.class, String.class);
    }
    @Override
    public EnumMapTemplate clone() throws CloneNotSupportedException
    {
      return (EnumMapTemplate) super.clone();
    }
    @Override
    public EnumMapTemplate copy() throws CloneNotSupportedException
    {
      return (EnumMapTemplate) super.copy();
    }
  }

  @Test
  public void testEnumMap()
  {
    Map<String, Fruits> input = asMap("apple", Fruits.APPLE, "orange", Fruits.ORANGE, "banana", Fruits.BANANA); // must be unique
    Map<String, Fruits> adds = asMap("grapes", Fruits.GRAPES, "pineapple", Fruits.PINEAPPLE);
    Map<String, Object> badInput = asMap("boolean", true, "integer", 1, "long", 2L, "float", 3.0f, "double", 4.0, "string", "hello",
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("boolean", true, "integer", 1, "long", 2L, "float", 3.0f, "double", 4.0, "string", "hello",
                                          "bytes", ByteString.empty(),
                                          "map", new DataMap(),
                                          "list", new DataList());

    testMap(EnumMapTemplate.class, EnumMapTemplate.SCHEMA, input, adds);
    testMapBadInput(EnumMapTemplate.class, EnumMapTemplate.SCHEMA, input, badInput, badOutput);
  }

  public static class MapOfStringMapTemplate extends WrappingMapTemplate<StringMap>
  {
    public static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : { \"type\" : \"map\", \"values\" : \"string\" } }");
    public MapOfStringMapTemplate()
    {
      this(new DataMap());
    }
    public MapOfStringMapTemplate(int capacity)
    {
      this(new DataMap(capacity));
    }
    public MapOfStringMapTemplate(int capacity, float loadFactor)
    {
      this(new DataMap(capacity, loadFactor));
    }
    public MapOfStringMapTemplate(Map<String, StringMap> map)
    {
      this(newDataMapOfSize(map.size()));
      putAll(map);
    }
    public MapOfStringMapTemplate(DataMap map)
    {
      super(map, SCHEMA, StringMap.class);
    }
    @Override
    public MapOfStringMapTemplate clone() throws CloneNotSupportedException
    {
      return (MapOfStringMapTemplate) super.clone();
    }
    @Override
    public MapOfStringMapTemplate copy() throws CloneNotSupportedException
    {
      return (MapOfStringMapTemplate) super.copy();
    }
  }

  @Test
  public void testMapOfStringMap()
  {
    Map<String, StringMap> input = new HashMap<String, StringMap>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "input" + i;
      input.put(key, new StringMap());
      input.get(key).put("subinput" + i, "subinputvalue" + i);
    }
    Map<String, StringMap> adds = new HashMap<String, StringMap>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "add" + i;
      adds.put(key, new StringMap());
      adds.get(key).put("subadd" + i, "subaddvalue" + i);
    }
    Map<String, Object> badInput = asMap("boolean", true, "integer", 1, "long", 2L, "float", 3.0f, "double", 4.0, "string", "hello",
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray(),
                                         "record", new FooRecord());
    Map<String, Object> badOutput = asMap("boolean", true, "integer", 1, "long", 2L, "float", 3.0f, "double", 4.0, "string", "hello",
                                          "bytes", ByteString.empty(),
                                          "list", new DataList());

    testMap(MapOfStringMapTemplate.class, MapOfStringMapTemplate.SCHEMA, input, adds);
    testMapBadInput(MapOfStringMapTemplate.class, MapOfStringMapTemplate.SCHEMA, input, badInput, badOutput);
  }

  public static class FooRecord extends RecordTemplate
  {
    public static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"string\" } ] }");
    public static final RecordDataSchema.Field FIELD_bar = SCHEMA.getField("bar");

    public FooRecord()
    {
      super(new DataMap(), SCHEMA);
    }
    public FooRecord(DataMap map)
    {
      super(map, SCHEMA);
    }

    public String getBar(GetMode mode)
    {
      return obtainDirect(FIELD_bar, String.class, mode);
    }

    public void removeBar()
    {
      remove(FIELD_bar);
    }

    public void setBar(String value)
    {
      putDirect(FIELD_bar, String.class, value);
    }

    @Override
    public FooRecord clone() throws CloneNotSupportedException
    {
      return (FooRecord) super.clone();
    }

    @Override
    public FooRecord copy() throws CloneNotSupportedException
    {
      return (FooRecord) super.copy();
    }
  }

  public static class FooRecordMap extends WrappingMapTemplate<FooRecord>
  {
    public static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : { \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [ { \"name\" : \"bar\", \"type\" : \"string\" } ] } }");
    public FooRecordMap()
    {
      this(new DataMap());
    }
    public FooRecordMap(int capacity)
    {
      this(new DataMap(capacity));
    }
    public FooRecordMap(int capacity, float loadFactor)
    {
      this(new DataMap(capacity, loadFactor));
    }
    public FooRecordMap(Map<String, FooRecord> map)
    {
      this(newDataMapOfSize(map.size()));
      putAll(map);
    }
    public FooRecordMap(DataMap map)
    {
      super(map, SCHEMA, FooRecord.class);
    }
    @Override
    public FooRecordMap clone() throws CloneNotSupportedException
    {
      return (FooRecordMap) super.clone();
    }
    @Override
    public FooRecordMap copy() throws CloneNotSupportedException
    {
      return (FooRecordMap) super.copy();
    }
  }

  @Test
  public void testFooRecordMap()
  {
    Map<String, FooRecord> input = new HashMap<String, FooRecord>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "input" + i;
      input.put(key, new FooRecord());
      input.get(key).setBar("subinputvalue" + i);
    }
    Map<String, FooRecord> adds = new HashMap<String, FooRecord>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "add" + i;
      adds.put(key, new FooRecord());
      adds.get(key).setBar("subaddvalue" + i);
    }
    Map<String, Object> badInput = asMap("boolean", true, "integer", 1, "long", 2L, "float", 3.0f, "double", 4.0, "string", "hello",
                                         "bytes", ByteString.empty(),
                                         "object", new Object(),
                                         "null", null,
                                         "array", new StringArray());
    Map<String, Object> badOutput = asMap("boolean", true, "integer", 1, "long", 2L, "float", 3.0f, "double", 4.0, "string", "hello",
                                          "bytes", ByteString.empty(),
                                          "list", new DataList());

    testMap(FooRecordMap.class, FooRecordMap.SCHEMA, input, adds);
    testMapBadInput(FooRecordMap.class, FooRecordMap.SCHEMA, input, badInput, badOutput);
  }

  public <E> void dump(Set<Map.Entry<String,E>> set)
  {
    for (Map.Entry<String,E> entry : set)
    {
      Object k = entry.getKey();
      Object v = entry.getValue();
      out.println(k + "(" + k.getClass() + ") = " + v + " (" + v.getClass() + ") " + entry.hashCode());
    }
  }

  protected static class PrimitiveLegacyMap<T> extends DirectMapTemplate<T>
  {
    public PrimitiveLegacyMap(DataMap map, MapDataSchema schema, Class<T> valuesClass)
    {
      super(map, schema, valuesClass);
      assertSame(_dataClass, valuesClass);
    }
  }

  protected static class EnumLegacyMap extends DirectMapTemplate<Fruits>
  {
    public static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : { \"type\" : \"enum\", \"name\" : \"Fruits\", \"symbols\" : [ \"APPLE\", \"ORANGE\", \"BANANA\", \"GRAPES\", \"PINEAPPLE\" ] } }");
    public EnumLegacyMap(DataMap map)
    {
      super(map, SCHEMA, Fruits.class);
      assertSame(_dataClass, String.class);
    }
  }

  @Test
  public void testLegacyConstructor()
  {
    Map<String, Class<?>> primitiveStringToClassMap = asMap(
      "int", Integer.class,
      "long", Long.class,
      "float", Float.class,
      "double", Double.class,
      "boolean", Boolean.class,
      "string", String.class);
    for (Map.Entry<String, Class<?>> e : primitiveStringToClassMap.entrySet())
    {
      MapDataSchema schema = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"" + e.getKey() + "\" }");
      @SuppressWarnings("unchecked")
      PrimitiveLegacyMap<?> map = new PrimitiveLegacyMap(new DataMap(), schema, e.getValue());
    }
    EnumLegacyMap enumMap = new EnumLegacyMap(new DataMap());
  }
}
