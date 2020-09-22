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

package com.linkedin.data;

import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.collections.CheckedMap;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.*;
import static org.testng.Assert.*;

public class TestData
{
  final static List<Object> referenceList1 = new ArrayList<Object>();
  final static int RL1_BOOLEAN_INDEX = 0;
  final static int RL1_INTEGER_INDEX = 1;
  final static int RL1_LONG_INDEX = 2;
  final static int RL1_FLOAT_INDEX = 3;
  final static int RL1_DOUBLE_INDEX = 4;
  final static int RL1_STRING_INDEX = 5;
  final static int RL1_BYTES_INDEX = 6;
  final static Boolean RL1_BOOLEAN_VALUE = true;
  final static Integer RL1_INTEGER_VALUE = 123;
  final static Long RL1_LONG_VALUE = 345L;
  final static Float RL1_FLOAT_VALUE = 567.5f;
  final static Double RL1_DOUBLE_VALUE = 9.99;
  final static String RL1_STRING_VALUE = "foobar";
  final static ByteString RL1_BYTES_VALUE = ByteString.copyAvroString("byte_string", false);
  static
  {
    referenceList1.add(RL1_BOOLEAN_INDEX, RL1_BOOLEAN_VALUE);
    referenceList1.add(RL1_INTEGER_INDEX, RL1_INTEGER_VALUE);
    referenceList1.add(RL1_LONG_INDEX, RL1_LONG_VALUE);
    referenceList1.add(RL1_FLOAT_INDEX, RL1_FLOAT_VALUE);
    referenceList1.add(RL1_DOUBLE_INDEX, RL1_DOUBLE_VALUE);
    referenceList1.add(RL1_STRING_INDEX, RL1_STRING_VALUE);
    referenceList1.add(RL1_BYTES_INDEX, RL1_BYTES_VALUE);
  }

  final static Map<String,Object> referenceMap1 = new HashMap<String,Object>();
  final static String RM1_BOOLEAN_KEY = "boolean_key";
  final static String RM1_INTEGER_KEY = "integer_key";
  final static String RM1_LONG_KEY = "long_key";
  final static String RM1_FLOAT_KEY = "float_key";
  final static String RM1_DOUBLE_KEY = "double_key";
  final static String RM1_STRING_KEY = "string_key";
  final static String RM1_BYTES_KEY = "bytes_key";
  final static Boolean RM1_BOOLEAN_VALUE = true;
  final static Integer RM1_INTEGER_VALUE = 12;
  final static Long RM1_LONG_VALUE = 34L;
  final static Float RM1_FLOAT_VALUE = 56.5f;
  final static Double RM1_DOUBLE_VALUE = 7.89;
  final static String RM1_STRING_VALUE = "baz";
  final static ByteString RM1_BYTES_VALUE = ByteString.copyAvroString("bytes", false);
  static
  {
    referenceMap1.put(RM1_BOOLEAN_KEY, RM1_BOOLEAN_VALUE);
    referenceMap1.put(RM1_INTEGER_KEY, RM1_INTEGER_VALUE);
    referenceMap1.put(RM1_LONG_KEY, RM1_LONG_VALUE);
    referenceMap1.put(RM1_FLOAT_KEY, RM1_FLOAT_VALUE);
    referenceMap1.put(RM1_DOUBLE_KEY, RM1_DOUBLE_VALUE);
    referenceMap1.put(RM1_STRING_KEY, RM1_STRING_VALUE);
    referenceMap1.put(RM1_BYTES_KEY, RM1_BYTES_VALUE);
  }

  final static List<Object> illegalObjects = new ArrayList<Object>();
  static {
    illegalObjects.add(new AtomicInteger(-13));
    illegalObjects.add(new AtomicLong(-13));
    illegalObjects.add(new BigDecimal(13));
    illegalObjects.add(new BigInteger("13"));
    illegalObjects.add(new Byte("13"));
    illegalObjects.add(new Short("13"));

    illegalObjects.add(new ArrayList<Object>());
    illegalObjects.add(new HashMap<String,String>());
    illegalObjects.add(new HashSet<String>());
  }

  final static Map<String,Object> illegalMap = new HashMap<String,Object>();
  static
  {
    for (Object o : illegalObjects)
    {
      illegalMap.put("Illegal-" + o.getClass().getName(), o);
    }
  }

  public final static DataMap referenceDataMap1 = new DataMap();
  static
  {
    referenceDataMap1.putAll(referenceMap1);
    DataMap map1_1 = new DataMap(referenceMap1);
    DataList list1_1 = new DataList(referenceList1);
    referenceDataMap1.put("map1_1", map1_1);
    referenceDataMap1.put("list1_1", list1_1);
    referenceDataMap1.put("map1_2", new DataMap());
    referenceDataMap1.put("list1_2", new DataList());
    referenceDataMap1.makeReadOnly();
  }
  final String referenceDump1 =
    "  map : {\n" +
    "    boolean_key : true\n" +
    "    bytes_key : bytes\n" +
    "    double_key : 7.89\n" +
    "    float_key : 56.5\n" +
    "    integer_key : 12\n" +
    "    list1_1 : [\n" +
    "      true\n" +
    "      123\n" +
    "      345\n" +
    "      567.5\n" +
    "      9.99\n" +
    "      foobar\n" +
    "      byte_string\n" +
    "    ]\n" +
    "    list1_2 : []\n" +
    "    long_key : 34\n" +
    "    map1_1 : {\n" +
    "      boolean_key : true\n" +
    "      bytes_key : bytes\n" +
    "      double_key : 7.89\n" +
    "      float_key : 56.5\n" +
    "      integer_key : 12\n" +
    "      long_key : 34\n" +
    "      string_key : baz\n" +
    "    }\n" +
    "    map1_2 : {}\n" +
    "    string_key : baz\n" +
    "  }\n";

  public final static DataList referenceDataList1 = new DataList();
  static
  {
    referenceDataList1.addAll(referenceList1);
    referenceDataList1.add(0, new DataList(referenceList1));
    referenceDataList1.add(1, new DataMap(referenceMap1));
    referenceDataList1.makeReadOnly();
  }

  public final static DataMap referenceDataMapOfMaps = new DataMap();
  static
  {
    DataMap mapLevel1 = new DataMap();
    DataMap mapLevel2 = new DataMap();
    mapLevel2.put("mapLevel3_1", referenceDataMap1);
    mapLevel1.put("mapLevel2_1", mapLevel2);
    mapLevel1.put("mapLevel2_2", referenceDataMap1);
    referenceDataMapOfMaps.put("mapLevel1_1", mapLevel1);
    referenceDataMapOfMaps.put("mapLevel1_2", referenceDataMap1);
  }

  public void testDataMapChecker(Map<String,Object> map)
  {
    Exception exc = null;
    for (Map.Entry<String,Object> entry : illegalMap.entrySet())
    {
      String k = entry.getKey();
      Object v = entry.getValue();
      try
      {
        exc = null;
        map.put(k, v);
      }
      catch (IllegalArgumentException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      try
      {
        exc = null;
        map.entrySet().add(entry);
      }
      catch (UnsupportedOperationException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      try
      {
        exc = null;
        map.keySet().add(k);
      }
      catch (UnsupportedOperationException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      try
      {
        exc = null;
        map.values().add(v);
      }
      catch (UnsupportedOperationException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    try
    {
      exc = null;
      map.putAll(illegalMap);
    }
    catch (IllegalArgumentException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    try
    {
      exc = null;
      map.entrySet().addAll(illegalMap.entrySet());
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    try
    {
      exc = null;
      map.values().addAll(illegalObjects);
    }
    catch (UnsupportedOperationException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
  }

  public void testDataListChecker(List<Object> list)
  {
    boolean empty = false;
    if (list.isEmpty())
    {
      // avoid empty list
      list.add(-1);
      list.add(-2);
      empty = true;
    }

    Exception exc = null;
    for (Object o : illegalObjects)
    {
      try
      {
        exc = null;
        list.add(o);
      }
      catch (IllegalArgumentException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      try
      {
        exc = null;
        list.add(list.size() / 2, o);
      }
      catch (IllegalArgumentException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      try
      {
        exc = null;
        list.set(list.size() / 2, o);
      }
      catch (IllegalArgumentException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    try
    {
      exc = null;
      list.addAll(illegalObjects);
    }
    catch (IllegalArgumentException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    try
    {
      exc = null;
      list.addAll(list.size() / 2, illegalObjects);
    }
    catch (IllegalArgumentException e)
    {
      exc = e;
    }
    assertTrue(exc != null);

    /* Iterator */

    for (Object o : illegalObjects)
    {
      try
      {
        exc = null;
        ListIterator<Object> it = list.listIterator();
        it.next();
        it.add(o);
      }
      catch (IllegalArgumentException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      try
      {
        exc = null;
        ListIterator<Object> it = list.listIterator();
        it.next();
        it.set(o);
      }
      catch (IllegalArgumentException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }

    if (empty)
    {
      list.clear();
    }

    /* SubList */

    if (list instanceof DataList)
    {
      testDataListChecker(list.subList(list.size() / 2, list.size()));
    }
  }

  @Test
  public void testDataListConstructor()
  {
    DataList list1 = new DataList();
    assertTrue(list1.isEmpty());
    testDataListChecker(list1);
    list1.addAll(referenceList1);
    assertTrue(list1.equals(referenceList1));

    DataList list2 = new DataList(1000);
    assertTrue(list2.isEmpty());
    testDataListChecker(list2);
    list2.addAll(referenceList1);
    assertTrue(list2.equals(referenceList1));

    DataList list4 = new DataList(referenceList1);
    assertTrue(list4.containsAll(referenceList1));
    assertTrue(list4.equals(referenceList1));
    testDataListChecker(list4);
  }

  @Test
  public void testDataMapConstructor()
  {
    DataMap map1 = new DataMap();
    assertTrue(map1.isEmpty());
    testDataMapChecker(map1);
    map1.putAll(referenceMap1);
    assertTrue(map1.equals(referenceMap1));

    DataMap map2 = new DataMap(1000);
    assertTrue(map2.isEmpty());
    testDataMapChecker(map2);
    map2.putAll(referenceMap1);
    assertTrue(map2.equals(referenceMap1));

    DataMap map3 = new DataMap(3, 0.3f);
    assertTrue(map3.isEmpty());
    testDataMapChecker(map3);
    map3.putAll(referenceMap1);
    assertTrue(map3.equals(referenceMap1));

    DataMap map4 = new DataMap(referenceMap1);
    assertTrue(map4.entrySet().containsAll(referenceMap1.entrySet()));
    assertTrue(map4.equals(referenceMap1));
    testDataMapChecker(map4);
  }

  public void testDataMapAccessor(String key, Object value)
  {
    DataMap map = new DataMap();
    map.put(key, value);
    Class<?> clas = value.getClass();

    if (clas == Boolean.class)
    {
      assertTrue(map.getBoolean(key) == value);
    }
    else if (clas == Integer.class)
    {
      assertTrue(map.getInteger(key) == value);
    }
    else if (clas == Long.class)
    {
      assertTrue(map.getLong(key) == value);
    }
    else if (clas == Float.class)
    {
      assertTrue(map.getFloat(key) == value);
    }
    else if (clas == Double.class)
    {
      assertTrue(map.getDouble(key) == value);
    }
    else if (clas == String.class)
    {
      assertTrue(map.getString(key) == value);
    }
    else if (clas == ByteString.class)
    {
      assertTrue(map.getByteString(key) == value);
    }

    if (clas != Boolean.class)
    {
      Exception exc = null;
      try
      {
        map.getBoolean(key);
      }
      catch (ClassCastException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    if (clas != Integer.class)
    {
      Exception exc = null;
      try
      {
        map.getInteger(key);
      }
      catch (ClassCastException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    if (clas != Long.class)
    {
      Exception exc = null;
      try
      {
        map.getLong(key);
      }
      catch (ClassCastException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    if (clas != Float.class)
    {
      Exception exc = null;
      try
      {
        map.getFloat(key);
      }
      catch (ClassCastException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    if (clas != Double.class)
    {
      Exception exc = null;
      try
      {
        map.getDouble(key);
      }
      catch (ClassCastException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    if (clas != String.class)
    {
      Exception exc = null;
      try
      {
        map.getString(key);
      }
      catch (ClassCastException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    if (clas != ByteString.class)
    {
      Exception exc = null;
      try
      {
        map.getByteString(key);
      }
      catch (ClassCastException e)
      {
        exc = e;
      }
      assertTrue((clas == String.class && Data.validStringAsBytes((String) value)) || exc != null);
    }
  }

  @Test
  public void testDataMapAccessors()
  {
    Object[] objects = {
        new Boolean(true),
        new Integer(1),
        new Long(2),
        new Float(1.5),
        new Double(2.0),
        new String("foo"),
        ByteString.copyAvroString("bar", false)
    };
    for (Object o : objects)
    {
      testDataMapAccessor("key", o);
    }
  }

  @Test
  public void testCopy() throws CloneNotSupportedException
  {
    boolean copyOnWrite = ! CheckedMap.class.isAssignableFrom(DataMap.class);

    /* DataMap with only immutable types */

    DataMap map1 = new DataMap(referenceMap1);
    DataMap map2 = map1.copy();
    DataMap map3 = map2.copy();
    assertTrue(! copyOnWrite || map1.getUnderlying() == map2.getUnderlying());
    assertTrue(! copyOnWrite || map1.getUnderlying() == map3.getUnderlying());
    assertEquals(map1, map2);
    assertEquals(map1, map3);

    map2.put("2", "2");
    assertTrue(! copyOnWrite || map1.getUnderlying() != map2.getUnderlying());
    assertTrue(! copyOnWrite || map1.getUnderlying() == map3.getUnderlying());
    assertFalse(map1.equals(map2));
    assertEquals(map1, map3);

    /* DataMap containing DataMap */

    DataMap map2_1 = new DataMap(referenceMap1);
    map2_1.put("2_1", "2_1");
    map2.put("map2_1", map2_1);
    assertTrue(map2.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());

    DataMap map4 = map2.copy();
    assertTrue(! copyOnWrite || map4.getUnderlying() != map2.getUnderlying());
    assertTrue(! copyOnWrite || map4.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertTrue(! copyOnWrite || map2.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertEquals(map4, map2);

    DataMap map5 = map4.copy();
    assertTrue(! copyOnWrite || map5.getUnderlying() != map2.getUnderlying());
    assertTrue(! copyOnWrite || map5.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertTrue(! copyOnWrite || map2.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertEquals(map5, map4);
    assertEquals(map5, map2);

    map5.getDataMap("map2_1").put("x", "x");
    assertTrue(! copyOnWrite || map5.getDataMap("map2_1").getUnderlying() != map2_1.getUnderlying());
    assertTrue(! copyOnWrite || map4.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertFalse(map5.getDataMap("map2_1").equals(map2_1));
    assertEquals(map4.getDataMap("map2_1"), map2_1);

    /* DataList with only primitive types */

    DataList list1 = new DataList(referenceList1);
    DataList list2 = list1.copy();
    DataList list3 = list2.copy();
    assertTrue(! copyOnWrite || list1.getUnderlying() == list2.getUnderlying());
    assertTrue(! copyOnWrite || list1.getUnderlying() == list3.getUnderlying());
    assertEquals(list1, list2);
    assertEquals(list1, list3);

    list2.add("2");
    assertTrue(! copyOnWrite || list1.getUnderlying() != list2.getUnderlying());
    assertTrue(! copyOnWrite || list1.getUnderlying() == list3.getUnderlying());
    assertFalse(list1.equals(list2));
    assertEquals(list1, list3);

    /* DataList containing DataList */

    DataList list2_1 = new DataList(referenceList1);
    list2_1.add("2_1");
    list2.add(0, list2_1);
    assertTrue(! copyOnWrite || list2.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertFalse(list2.equals(list1));
    assertFalse(list2.equals(list3));
    assertEquals(list1, list3);

    DataList list4 = list2.copy();
    assertTrue(! copyOnWrite || list4.getUnderlying() != list2.getUnderlying());
    assertTrue(! copyOnWrite || list4.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertTrue(! copyOnWrite || list2.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertEquals(list4, list2);
    assertEquals(list4.getDataList(0), list2_1);
    assertEquals(list2.getDataList(0), list2_1);

    DataList list5 = list4.copy();
    assertTrue(! copyOnWrite || list5.getUnderlying() != list2.getUnderlying());
    assertTrue(! copyOnWrite || list5.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertTrue(! copyOnWrite || list2.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertEquals(list5, list4);
    assertEquals(list5.getDataList(0), list2_1);
    assertEquals(list2.getDataList(0), list2_1);

    /* DataMap containing DataList */

    DataMap map6 = map1.copy();
    assertTrue(! copyOnWrite || map6.getUnderlying() == map1.getUnderlying());
    assertEquals(map6, map1);
    map6.put("list2_1", list2_1);
    assertTrue(! copyOnWrite || map6.getUnderlying() != map1.getUnderlying());
    assertFalse(map6.equals(map1));

    DataMap map7 = map6.copy();
    assertTrue(! copyOnWrite || map7.getUnderlying() != map6.getUnderlying());
    assertTrue(! copyOnWrite || map7.getDataList("list2_1").getUnderlying() == list2_1.getUnderlying());
    assertEquals(map7, map6);
    assertEquals(map7.getDataList("list2_1"), list2_1);

    DataMap map8 = map6.copy();
    assertTrue(! copyOnWrite || map8.getUnderlying() != map6.getUnderlying());
    assertTrue(! copyOnWrite || map8.getDataList("list2_1").getUnderlying() == list2_1.getUnderlying());
    assertEquals(map8, map6);
    assertEquals(map8.getDataList("list2_1"), list2_1);

    map7.getDataList("list2_1").remove(0);
    assertTrue(! copyOnWrite || map7.getDataList("list2_1").getUnderlying() != list2_1.getUnderlying());
    assertTrue(! copyOnWrite || map8.getDataList("list2_1").getUnderlying() == list2_1.getUnderlying());
    assertFalse(map7.getDataList("list2_1").equals(list2_1));
    assertEquals(map8.getDataList("list2_1"), list2_1);

    /* DataList containing DataMap */

    DataList list6 = list1.copy();
    assertTrue(! copyOnWrite || list6.getUnderlying() == list1.getUnderlying());
    assertEquals(list6, list1);
    list6.add(0, map2_1);
    assertTrue(! copyOnWrite || list6.getUnderlying() != list1.getUnderlying());
    assertFalse(list6.equals(list1));

    DataList list7 = list6.copy();
    assertTrue(! copyOnWrite || list7.getUnderlying() != list6.getUnderlying());
    assertTrue(! copyOnWrite || list7.getDataMap(0).getUnderlying() == map2_1.getUnderlying());
    assertEquals(list7, list6);
    assertEquals(list7.getDataMap(0), map2_1);

    DataList list8 = list6.copy();
    assertTrue(! copyOnWrite || list8.getUnderlying() != list6.getUnderlying());
    assertTrue(! copyOnWrite || list8.getDataMap(0).getUnderlying() == map2_1.getUnderlying());
    assertEquals(list8, list6);
    assertEquals(list8.getDataMap(0), map2_1);

    list7.getDataMap(0).remove(RM1_BOOLEAN_KEY);
    assertTrue(! copyOnWrite || list7.getDataMap(0).getUnderlying() != map2_1.getUnderlying());
    assertTrue(! copyOnWrite || list8.getDataMap(0).getUnderlying() == map2_1.getUnderlying());
    assertFalse(list7.getDataMap(0).equals(map2_1));
    assertEquals(list8.getDataMap(0), map2_1);

    /* DataMap containing both DataList and DataMap */

    DataMap map10 = map1.copy();
    assertTrue(! copyOnWrite || map10.getUnderlying() == map1.getUnderlying());
    assertEquals(map10, map1);
    map10.put("map2_1", map2_1);
    map10.put("list2_1", list2_1);
    assertTrue(! copyOnWrite || map10.getUnderlying() != map1.getUnderlying());
    assertFalse(map10.equals(map1));

    DataMap map11 = map10.copy();
    assertTrue(! copyOnWrite || map11.getUnderlying() != map10.getUnderlying());
    assertTrue(! copyOnWrite || map11.getDataList("list2_1").getUnderlying() == list2_1.getUnderlying());
    assertTrue(! copyOnWrite || map11.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertEquals(map11, map10);
    assertEquals(map11.getDataList("list2_1"), list2_1);
    assertEquals(map11.getDataMap("map2_1"), map2_1);

    DataMap map12 = map11.copy();
    assertTrue(! copyOnWrite || map12.getUnderlying() != map11.getUnderlying());
    assertTrue(! copyOnWrite || map12.getDataList("list2_1").getUnderlying() == list2_1.getUnderlying());
    assertTrue(! copyOnWrite || map12.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertEquals(map12, map10);
    assertEquals(map12.getDataList("list2_1"), list2_1);
    assertEquals(map12.getDataMap("map2_1"), map2_1);

    map12.getDataList("list2_1").remove(1);
    assertTrue(! copyOnWrite || map12.getDataList("list2_1").getUnderlying() != list2_1.getUnderlying());
    assertTrue(! copyOnWrite || map11.getDataList("list2_1").getUnderlying() == list2_1.getUnderlying());
    assertFalse(map12.getDataList("list2_1").equals(list2_1));
    assertEquals(map11.getDataList("list2_1"), list2_1);

    DataMap map13 = map10.copy();
    assertTrue(! copyOnWrite || map13.getUnderlying() != map10.getUnderlying());
    assertTrue(! copyOnWrite || map13.getDataList("list2_1").getUnderlying() == list2_1.getUnderlying());
    assertTrue(! copyOnWrite || map13.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertEquals(map13, map10);
    assertEquals(map13.getDataList("list2_1"), list2_1);
    assertEquals(map13.getDataMap("map2_1"), map2_1);

    map13.getDataMap("map2_1").clear();
    assertTrue(! copyOnWrite || map13.getDataMap("map2_1").getUnderlying() != map2_1.getUnderlying());
    assertTrue(! copyOnWrite || map10.getDataMap("map2_1").getUnderlying() == map2_1.getUnderlying());
    assertFalse(map13.getDataMap("map2_1").equals(map2_1));
    assertEquals(map10.getDataMap("map2_1"), map2_1);

    /* DataList containing both DataList and DataMap */

    DataList list10 = list1.copy();
    assertTrue(! copyOnWrite || list10.getUnderlying() == list1.getUnderlying());
    assertEquals(list10, list1);
    list10.add(0, list2_1);
    list10.add(1, map2_1);
    assertTrue(! copyOnWrite || list10.getUnderlying() != list1.getUnderlying());
    assertFalse(list10.equals(list1));

    DataList list11 = list10.copy();
    assertTrue(! copyOnWrite || list11.getUnderlying() != list10.getUnderlying());
    assertTrue(! copyOnWrite || list11.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertTrue(! copyOnWrite || list11.getDataMap(1).getUnderlying() == map2_1.getUnderlying());
    assertEquals(list11, list10);
    assertEquals(list11.getDataList(0), list2_1);
    assertEquals(list11.getDataMap(1), map2_1);

    DataList list12 = list11.copy();
    assertTrue(! copyOnWrite || list12.getUnderlying() != list11.getUnderlying());
    assertTrue(! copyOnWrite || list12.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertTrue(! copyOnWrite || list12.getDataMap(1).getUnderlying() == map2_1.getUnderlying());
    assertEquals(list12, list11);
    assertEquals(list12.getDataList(0), list2_1);
    assertEquals(list12.getDataMap(1), map2_1);

    list12.getDataList(0).set(2, "xxx");
    assertTrue(! copyOnWrite || list12.getDataList(0).getUnderlying() != list2_1.getUnderlying());
    assertTrue(! copyOnWrite || list11.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertFalse(list12.getDataList(0).equals(list2_1));
    assertEquals(list11.getDataList(0), list2_1);

    DataList list13 = list10.copy();
    assertTrue(! copyOnWrite || list13.getUnderlying() != list10.getUnderlying());
    assertTrue(! copyOnWrite || list13.getDataList(0).getUnderlying() == list2_1.getUnderlying());
    assertTrue(! copyOnWrite || list13.getDataMap(1).getUnderlying() == map2_1.getUnderlying());
    assertEquals(list13, list10);
    assertEquals(list13.getDataList(0), list2_1);
    assertEquals(list13.getDataMap(1), map2_1);

    list13.getDataMap(1).put("x", "XX");
    assertTrue(! copyOnWrite || list13.getDataMap(1).getUnderlying() != map2_1.getUnderlying());
    assertTrue(! copyOnWrite || list10.getDataMap(1).getUnderlying() == map2_1.getUnderlying());
    assertFalse(list13.getDataMap(1).equals(map2_1));
    assertEquals(list10.getDataMap(1), map2_1);

    /* Diamond shaped object graph */

    {
      DataMap a = new DataMap();
      DataList b = new DataList();
      DataList c = new DataList();
      DataMap d = new DataMap();
      a.put("b", b);
      a.put("c", c);
      b.add(d);
      c.add(d);
      DataMap aCopy = a.copy();
      DataList bCopy = (DataList) aCopy.get("b");
      DataList cCopy = (DataList) aCopy.get("c");
      assertSame(bCopy.get(0), cCopy.get(0));
    }
    {
      DataList a = new DataList();
      DataMap b = new DataMap();
      DataMap c = new DataMap();
      DataList d = new DataList();
      a.add(b);
      a.add(c);
      b.put("d", d);
      c.put("d", d);
      DataList aCopy = a.copy();
      DataMap bCopy = (DataMap) aCopy.get(0);
      DataMap cCopy = (DataMap) aCopy.get(1);
      assertSame(bCopy.get("d"), cCopy.get("d"));
    }

    /* Circular object graph */
    {
      // DataList only
      DataList a = new DataList();
      a.disableChecker();
      a.add(a);
      DataList aCopy = a.copy();
      assertSame(aCopy.get(0), aCopy);

      DataList b = new DataList();
      b.disableChecker();
      a.add(b);
      b.add(a);

      aCopy = a.copy();
      DataList bCopy = (DataList) aCopy.get(1);
      assertSame(aCopy.get(0), aCopy);
      assertSame(bCopy.get(0), aCopy);
    }
    /* Circular object graph */
    {
      // DataMap only
      DataMap a = new DataMap();
      a.disableChecker();
      a.put("a", a);
      DataMap aCopy = a.copy();
      assertSame(aCopy.get("a"), aCopy);

      DataMap b = new DataMap();
      b.disableChecker();
      a.put("b", b);
      b.put("a", a);

      aCopy = a.copy();
      DataMap bCopy = (DataMap) aCopy.get("b");
      assertSame(aCopy.get("a"), aCopy);
      assertSame(bCopy.get("a"), aCopy);
    }
  }

  @Test
  public void mapClonesHaveDifferentHashValues() throws CloneNotSupportedException
  {
    DataMap originalMap = new DataMap();
    originalMap.put("key", "value");

    DataMap copyMap = originalMap.copy();

    // The objects should be "equal," but not identical.
    assertTrue(copyMap.equals(originalMap));
    assertFalse(copyMap.dataComplexHashCode() == originalMap.dataComplexHashCode());
  }

  @Test
  public void testListClonesHaveDifferentHashValues() throws CloneNotSupportedException
  {
    DataList originalList = new DataList();
    originalList.add("value");

    DataList copyList = originalList.copy();

    // The objects should be "equal," but not identical.
    assertTrue(copyList.equals(originalList));
    assertFalse(copyList.dataComplexHashCode() == originalList.dataComplexHashCode());
  }

  @Test
  public void testDeepCopy() throws CloneNotSupportedException
  {
    DataMap root = new DataMap();

    DataMap a = new DataMap();
    a.put("key", "a");

    DataMap b = a.copy();
    b.put("key", "b");

    root.put("a", a);
    root.put("b", b);

    DataMap copy = root.copy();
    assertEquals(root, copy);

    ((DataMap)copy.get("a")).put("key", "A");
    ((DataMap)copy.get("b")).put("key", "B");

    DataMap rootA = (DataMap)root.get("a");
    DataMap rootB = (DataMap)root.get("b");
    DataMap copyA = (DataMap)copy.get("a");
    DataMap copyB = (DataMap)copy.get("b");

    assertEquals(rootA.get("key"), ("a"));
    assertEquals(rootB.get("key"), ("b"));

    assertEquals(copyA.get("key"), ("A"));
    assertEquals(copyB.get("key"), ("B"));
  }

  @Test
  public void testNullValue()
  {
    DataMap map1 = new DataMap();
    {
      Exception exc = null;
      try
      {
        map1.put("a", null);
      }
      catch (NullPointerException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
    DataList list1 = new DataList();
    {
      Exception exc = null;
      try
      {
        list1.add(null);
      }
      catch (NullPointerException e)
      {
        exc = e;
      }
      assertTrue(exc != null);
    }
  }

  @Test
  public void testMakeReadOnly() throws CloneNotSupportedException
  {
    DataMap map1 = referenceDataMap1;
    assertTrue(map1.isReadOnly());
    assertTrue(map1.getDataMap("map1_1").isReadOnly());
    assertTrue(map1.getDataList("list1_1").isReadOnly());

    DataMap map2 = map1.copy();
    assertFalse(map2.isReadOnly());
    assertFalse(map2.getDataMap("map1_1").isReadOnly());
    assertFalse(map2.getDataList("list1_1").isReadOnly());
    assertFalse(map2.isMadeReadOnly());
    assertFalse(map2.getDataMap("map1_1").isMadeReadOnly());
    assertFalse(map2.getDataList("list1_1").isMadeReadOnly());

    map2.makeReadOnly();
    assertTrue(map2.isReadOnly());
    assertTrue(map2.getDataMap("map1_1").isReadOnly());
    assertTrue(map2.getDataList("list1_1").isReadOnly());
    assertTrue(map1.isReadOnly());
    assertTrue(map1.getDataMap("map1_1").isReadOnly());
    assertTrue(map1.getDataList("list1_1").isReadOnly());
    assertTrue(map2.isMadeReadOnly());
    assertTrue(map2.getDataMap("map1_1").isMadeReadOnly());
    assertTrue(map2.getDataList("list1_1").isMadeReadOnly());

    DataList list1 = referenceDataList1;
    assertTrue(list1.isReadOnly());
    assertTrue(list1.getDataList(0).isReadOnly());
    assertTrue(list1.getDataMap(1).isReadOnly());

    DataList list2 = list1.copy();
    assertFalse(list2.isReadOnly());
    assertFalse(list2.getDataList(0).isReadOnly());
    assertFalse(list2.getDataMap(1).isReadOnly());
    assertFalse(list2.isMadeReadOnly());
    assertFalse(list2.getDataList(0).isMadeReadOnly());
    assertFalse(list2.getDataMap(1).isMadeReadOnly());

    list2.makeReadOnly();
    assertTrue(list2.isReadOnly());
    assertTrue(list2.getDataList(0).isReadOnly());
    assertTrue(list2.getDataMap(1).isReadOnly());
    assertTrue(list1.isReadOnly());
    assertTrue(list1.getDataList(0).isReadOnly());
    assertTrue(list1.getDataMap(1).isReadOnly());
    assertTrue(list2.isMadeReadOnly());
    assertTrue(list2.getDataList(0).isMadeReadOnly());
    assertTrue(list2.getDataMap(1).isMadeReadOnly());
  }

  @Test
  public void testDump()
  {
    StringBuilder sb1 = new StringBuilder();
    Data.dump("map", referenceDataMap1, "  ", sb1);
    assertEquals(sb1.toString(), referenceDump1);
    assertEquals(Data.dump("map", referenceDataMap1, "  ").toString(), referenceDump1);

    StringBuilder sb2 = new StringBuilder();
    Data.dump("", new DataMap(), "", sb2);
    assertEquals(sb2.toString(), "{}\n");

    String s3 = Data.dump(null, new DataList(), "");
    assertEquals(s3, "[]\n");
  }

  @Test
  public void testTraverseCallback() throws Exception
  {
    Data.TraverseCallback noOp = new Data.TraverseCallback()
    {
    };

    Data.TraverseCallback removeNestedMap = new Data.TraverseCallback()
    {
      @Override
      public void startMap(DataMap map)
      {
        map.remove("map1_1");
        map.remove("map1_2");
      }
    };

    DataMap referenceDataMapCopy = referenceDataMap1.copy();
    Data.traverse(referenceDataMapCopy, noOp);
    assertEquals(referenceDataMapCopy, referenceDataMap1);
    Data.traverse(referenceDataMapCopy, removeNestedMap);
    assertFalse(referenceDataMapCopy.containsKey("map1_1"));
    assertFalse(referenceDataMapCopy.containsKey("map1_2"));
  }

  @Test
  public void testObjectIsAcyclic()
  {
    assertTrue(Data.objectIsAcyclic(true));
    assertTrue(Data.objectIsAcyclic(1));
    assertTrue(Data.objectIsAcyclic(1L));
    assertTrue(Data.objectIsAcyclic(1.0f));
    assertTrue(Data.objectIsAcyclic(1.0));
    assertTrue(Data.objectIsAcyclic("string"));
    assertTrue(Data.objectIsAcyclic(new DataMap()));
    assertTrue(Data.objectIsAcyclic(new DataList()));

    DataMap a = new DataMap();
    DataList b = new DataList();
    DataMap c = new DataMap();

    a.put("b", b);
    a.put("c", c);

    assertTrue(Data.objectIsAcyclic(a));
    assertTrue(Data.objectIsAcyclic(b));
    assertTrue(Data.objectIsAcyclic(c));

    DataMap dm = new DataMap();
    b.add(dm);
    c.put("d", dm);

    assertTrue(Data.objectIsAcyclic(a));
    assertTrue(Data.objectIsAcyclic(b));
    assertTrue(Data.objectIsAcyclic(c));
    assertTrue(Data.objectIsAcyclic(dm));

    DataList e = new DataList();
    DataMap f = new DataMap();

    dm.put("e", e);
    dm.put("f", f);

    assertTrue(Data.objectIsAcyclic(a));
    assertTrue(Data.objectIsAcyclic(b));
    assertTrue(Data.objectIsAcyclic(c));
    assertTrue(Data.objectIsAcyclic(dm));
    assertTrue(Data.objectIsAcyclic(e));
    assertTrue(Data.objectIsAcyclic(f));

    a.disableChecker();
    b.disableChecker();
    c.disableChecker();
    dm.disableChecker();
    e.disableChecker();
    f.disableChecker();

    // loop from e to e
    assertTrue(Data.objectIsAcyclic(a));
    e.add(e);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertFalse(Data.objectIsAcyclic(e));
    assertTrue(Data.objectIsAcyclic(f));
    e.remove(0);

    // loop from e to dm
    assertTrue(Data.objectIsAcyclic(a));
    e.add(dm);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertFalse(Data.objectIsAcyclic(e));
    assertTrue(Data.objectIsAcyclic(f));
    e.remove(0);

    // loop from e to c
    assertTrue(Data.objectIsAcyclic(a));
    e.add(c);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertFalse(Data.objectIsAcyclic(e));
    assertTrue(Data.objectIsAcyclic(f));
    e.remove(0);

    // loop from e to b
    assertTrue(Data.objectIsAcyclic(a));
    e.add(b);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertFalse(Data.objectIsAcyclic(e));
    assertTrue(Data.objectIsAcyclic(f));
    e.remove(0);

    // loop from e to a
    assertTrue(Data.objectIsAcyclic(a));
    e.add(a);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertFalse(Data.objectIsAcyclic(e));
    assertTrue(Data.objectIsAcyclic(f));
    e.remove(0);

    // loop from f to f
    assertTrue(Data.objectIsAcyclic(a));
    f.put("f", f);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertTrue(Data.objectIsAcyclic(e));
    assertFalse(Data.objectIsAcyclic(f));
    f.remove("f");

    // loop from f to dm
    assertTrue(Data.objectIsAcyclic(a));
    f.put("d", dm);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertTrue(Data.objectIsAcyclic(e));
    assertFalse(Data.objectIsAcyclic(f));
    f.remove("d");

    // loop from f to c
    assertTrue(Data.objectIsAcyclic(a));
    f.put("c", c);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertTrue(Data.objectIsAcyclic(e));
    assertFalse(Data.objectIsAcyclic(f));
    f.remove("c");

    // loop from f to b
    assertTrue(Data.objectIsAcyclic(a));
    f.put("b", b);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertTrue(Data.objectIsAcyclic(e));
    assertFalse(Data.objectIsAcyclic(f));
    f.remove("b");

    // loop from f to a
    assertTrue(Data.objectIsAcyclic(a));
    f.put("a", a);
    assertFalse(Data.objectIsAcyclic(a));
    assertFalse(Data.objectIsAcyclic(b));
    assertFalse(Data.objectIsAcyclic(c));
    assertFalse(Data.objectIsAcyclic(dm));
    assertTrue(Data.objectIsAcyclic(e));
    assertFalse(Data.objectIsAcyclic(f));
    f.remove("a");
  }

  private void putAndExpectIllegalArgumentException(DataMap map, String key, Object value)
  {
    Exception exc = null;
    try
    {
      map.put(key, value);
      assertTrue(map.get(key) == null);
    }
    catch (IllegalArgumentException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
  }


  @Test
  public void testDataMapNoCyclesOnPut()
  {
    // test with DataMap

    DataMap a = new DataMap();
    DataMap b = new DataMap();
    DataMap c = new DataMap();

    a.put("b", b);
    a.put("c", c);

    assertTrue(Data.objectIsAcyclic(a));

    DataMap d = new DataMap();
    b.put("d", d);
    c.put("d", d);

    assertTrue(Data.objectIsAcyclic(a));

    DataMap e = new DataMap();
    d.put("e", e);

    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to d.
    putAndExpectIllegalArgumentException(e, "e", e);
    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to d.
    putAndExpectIllegalArgumentException(e, "d", d);
    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to c
    putAndExpectIllegalArgumentException(e, "c", c);
    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to b
    putAndExpectIllegalArgumentException(e, "b", b);
    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to a
    putAndExpectIllegalArgumentException(e, "a", a);
    assertTrue(Data.objectIsAcyclic(a));
  }

  private void addAndExpectIllegalArgumentException(DataList list, Object value)
  {
    Exception exc = null;
    try
    {
      int size = list.size();
      list.add(value);
      assertTrue(list.get(size) == null);
    }
    catch (IllegalArgumentException e)
    {
      exc = e;
    }
    assertTrue(exc != null);
  }

  @Test
  public void testDataListNoCyclesOnAdd()
  {
    // test with DataList

    DataList a = new DataList();
    DataList b = new DataList();
    DataList c = new DataList();

    a.add(b);
    a.add(c);

    assertTrue(Data.objectIsAcyclic(a));

    DataList d = new DataList();
    b.add(d);
    c.add(d);

    assertTrue(Data.objectIsAcyclic(a));

    DataList e = new DataList();
    d.add(e);

    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to e.
    addAndExpectIllegalArgumentException(e, e);
    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to d.
    addAndExpectIllegalArgumentException(e, d);
    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to c
    addAndExpectIllegalArgumentException(e, c);
    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to b
    addAndExpectIllegalArgumentException(e, b);
    assertTrue(Data.objectIsAcyclic(a));

    // cyclic due to edge from e to a
    addAndExpectIllegalArgumentException(e, a);
    assertTrue(Data.objectIsAcyclic(a));
  }

  @Test
  public void testNoCyclesOnAddAndPut()
  {
    assertTrue(Data.objectIsAcyclic(true));
    assertTrue(Data.objectIsAcyclic(1));
    assertTrue(Data.objectIsAcyclic(1L));
    assertTrue(Data.objectIsAcyclic(1.0f));
    assertTrue(Data.objectIsAcyclic(1.0));
    assertTrue(Data.objectIsAcyclic("string"));
    assertTrue(Data.objectIsAcyclic(new DataMap()));
    assertTrue(Data.objectIsAcyclic(new DataList()));

    DataMap a = new DataMap();
    DataList b = new DataList();
    DataMap c = new DataMap();

    a.put("b", b);
    a.put("c", c);

    assertTrue(Data.objectIsAcyclic(a));
    assertTrue(Data.objectIsAcyclic(b));
    assertTrue(Data.objectIsAcyclic(c));

    DataMap dm = new DataMap();
    b.add(dm);
    c.put("d", dm);

    assertTrue(Data.objectIsAcyclic(a));
    assertTrue(Data.objectIsAcyclic(b));
    assertTrue(Data.objectIsAcyclic(c));
    assertTrue(Data.objectIsAcyclic(dm));

    DataList e = new DataList();
    DataMap f = new DataMap();

    dm.put("e", e);
    dm.put("f", f);

    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to e
    addAndExpectIllegalArgumentException(e, e);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to dm
    addAndExpectIllegalArgumentException(e, dm);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to c
    addAndExpectIllegalArgumentException(e, c);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to b
    addAndExpectIllegalArgumentException(e, b);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to a
    addAndExpectIllegalArgumentException(e, a);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from f to f
    putAndExpectIllegalArgumentException(f, "f", f);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from f to dm
    putAndExpectIllegalArgumentException(f, "d", dm);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to c
    putAndExpectIllegalArgumentException(f, "c", c);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to b
    putAndExpectIllegalArgumentException(f, "b", b);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to a
    putAndExpectIllegalArgumentException(f, "a", a);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from dm to dm
    putAndExpectIllegalArgumentException(dm, "d", dm);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from dm to c
    putAndExpectIllegalArgumentException(dm, "c", c);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from dm to b
    putAndExpectIllegalArgumentException(dm, "b", b);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from dm to a
    putAndExpectIllegalArgumentException(dm, "a", a);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from c to b
    putAndExpectIllegalArgumentException(c, "c", c);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from c to a
    putAndExpectIllegalArgumentException(c, "a", a);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from b to b
    addAndExpectIllegalArgumentException(b, b);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from b to a
    addAndExpectIllegalArgumentException(b, a);
    assertTrue(Data.objectIsAcyclic(a));

    DataList dl = new DataList();
    b.clear();
    b.add(dl);
    c.put("d", dl);

    assertTrue(Data.objectIsAcyclic(a));
    assertTrue(Data.objectIsAcyclic(b));
    assertTrue(Data.objectIsAcyclic(c));
    assertTrue(Data.objectIsAcyclic(dl));

    dl.add(e);
    dl.add(f);

    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to e
    addAndExpectIllegalArgumentException(e, e);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to dl
    addAndExpectIllegalArgumentException(e, dl);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to c
    addAndExpectIllegalArgumentException(e, c);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to b
    addAndExpectIllegalArgumentException(e, b);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to a
    addAndExpectIllegalArgumentException(e, a);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from f to f
    putAndExpectIllegalArgumentException(f, "f", f);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from f to dl
    putAndExpectIllegalArgumentException(f, "d", dl);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to c
    putAndExpectIllegalArgumentException(f, "c", c);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to b
    putAndExpectIllegalArgumentException(f, "b", b);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from e to a
    putAndExpectIllegalArgumentException(f, "a", a);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from dl to dl
    addAndExpectIllegalArgumentException(dl, dl);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from dl to c
    addAndExpectIllegalArgumentException(dl, c);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from dl to b
    addAndExpectIllegalArgumentException(dl, b);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from dl to a
    addAndExpectIllegalArgumentException(dl, a);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from c to b
    putAndExpectIllegalArgumentException(c, "c", c);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from c to a
    putAndExpectIllegalArgumentException(c, "a", a);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from b to b
    addAndExpectIllegalArgumentException(b, b);
    assertTrue(Data.objectIsAcyclic(a));

    // loop from b to a
    addAndExpectIllegalArgumentException(b, a);
    assertTrue(Data.objectIsAcyclic(a));
  }

  Collection<Object> asCollection(Object... objects)
  {
    ArrayList<Object> c = new ArrayList<Object>();
    for (Object o : objects)
    {
      c.add(o);
    }
    return c;
  }

  @Test
  public void testAppendNames()
  {
    Object inputs[][] =
    {
        {
          asCollection("a", 1, "b"),
          "a[1].b"
        },
        {
          asCollection(2, "b"),
          "[2].b"
        },
        {
          asCollection(3, 4, "b"),
          "[3][4].b"
        },
        {
          asCollection("a", "b", "c"),
          "a.b.c"
        },
        {
          asCollection("a", "b", 5, 6, "c", "d", 7, 8),
          "a.b[5][6].c.d[7][8]"
        },
    };
    for (Object[] input : inputs)
    {
      StringBuilder builder = new StringBuilder();
      @SuppressWarnings("unchecked")
      Collection<Object> names = (Collection<Object>) input[0];
      Data.appendNames(builder, names);
      String output = builder.toString();
      assertEquals(output, input[1]);
    }
  }

  @Test
  public void testDataMapInstrumentation()
  {
    DataMap map = new DataMap();
    map.put("a", "foo");
    map.put("b", "bamboo");

    map.startInstrumentingAccess();
    map.get("a");

    StringBuilder prefix = new StringBuilder("prefix");
    Map<String, Map<String, Object>> instrumentedData = new HashMap<String, Map<String, Object>>();

    map.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.size(), 1);
    assertTrue(instrumentedData.containsKey("prefix.a"));
    assertEquals(instrumentedData.get("prefix.a").get("value"), "foo");
    assertEquals(instrumentedData.get("prefix.a").get("timesAccessed"), 1);

    map.put("a", "bar");
    map.get("a");

    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.get("prefix.a").get("value"), "bar");
    assertEquals(instrumentedData.get("prefix.a").get("timesAccessed"), 2);

    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, true);

    assertEquals(instrumentedData.size(), 2);
    assertEquals(instrumentedData.get("prefix.b").get("value"), "bamboo");
    assertEquals(instrumentedData.get("prefix.b").get("timesAccessed"), 0);

    map.stopInstrumentingAccess();

    map.get("a");

    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, true);

    assertEquals(instrumentedData.get("prefix.a").get("timesAccessed"), 2);

    map.clearInstrumentedData();
    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.size(), 0);

    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, true);

    assertEquals(instrumentedData.size(), 2);
    assertEquals(instrumentedData.get("prefix.a").get("timesAccessed"), 0);
    assertEquals(instrumentedData.get("prefix.b").get("timesAccessed"), 0);
  }

  @Test
  public void testDataListInstrumentation()
  {
    DataList list = new DataList();
    list.add("a");
    list.add("b");
    list.add("c");

    list.startInstrumentingAccess();
    list.get(1);

    StringBuilder prefix = new StringBuilder("prefix");
    Map<String, Map<String, Object>> instrumentedData = new HashMap<String, Map<String, Object>>();

    list.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.size(), 1);
    assertTrue(instrumentedData.containsKey("prefix[1]"));
    assertEquals(instrumentedData.get("prefix[1]").get("value"), "b");
    assertEquals(instrumentedData.get("prefix[1]").get("timesAccessed"), 1);

    list.set(1, "bar");
    list.get(1);

    instrumentedData.clear();
    list.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.get("prefix[1]").get("value"), "bar");
    assertEquals(instrumentedData.get("prefix[1]").get("timesAccessed"), 2);

    instrumentedData.clear();
    list.collectInstrumentedData(prefix, instrumentedData, true);

    assertEquals(instrumentedData.size(), 3);
    assertEquals(instrumentedData.get("prefix[0]").get("value"), "a");
    assertEquals(instrumentedData.get("prefix[0]").get("timesAccessed"), 0);
    assertEquals(instrumentedData.get("prefix[2]").get("value"), "c");
    assertEquals(instrumentedData.get("prefix[2]").get("timesAccessed"), 0);

    list.stopInstrumentingAccess();

    list.get(1);

    instrumentedData.clear();
    list.collectInstrumentedData(prefix, instrumentedData, true);

    assertEquals(instrumentedData.get("prefix[1]").get("timesAccessed"), 2);

    list.clearInstrumentedData();
    instrumentedData.clear();
    list.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.size(), 0);

    instrumentedData.clear();
    list.collectInstrumentedData(prefix, instrumentedData, true);

    assertEquals(instrumentedData.size(), 3);
    assertEquals(instrumentedData.get("prefix[0]").get("timesAccessed"), 0);
    assertEquals(instrumentedData.get("prefix[1]").get("timesAccessed"), 0);
    assertEquals(instrumentedData.get("prefix[2]").get("timesAccessed"), 0);
  }

  @Test
  public void testNestedInstrumentation()
  {
    DataMap map = new DataMap();
    map.put("int", 123);

    DataMap containedMap = new DataMap();
    containedMap.put("a", "v_a");
    containedMap.put("b", 99);
    map.put("map", containedMap);

    DataList containedList = new DataList();
    containedList.add("foo");
    containedList.add(88);
    containedList.add(99.0);
    map.put("list", containedList);

    DataMap containedMap2 = new DataMap();
    containedList.add(containedMap2);
    containedMap2.put("bar", "bar2");

    map.startInstrumentingAccess();
    containedMap.get("b");

    StringBuilder prefix = new StringBuilder("prefix");
    Map<String, Map<String, Object>> instrumentedData = new HashMap<String, Map<String, Object>>();

    map.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.size(), 1);
    assertEquals(instrumentedData.get("prefix.map.b").get("value"), "99");
    assertEquals(instrumentedData.get("prefix.map.b").get("timesAccessed"), 1);

    containedList.get(1);

    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.size(), 2);
    assertEquals(instrumentedData.get("prefix.list[1]").get("value"), "88");
    assertEquals(instrumentedData.get("prefix.list[1]").get("timesAccessed"), 1);

    map.get("int");

    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.size(), 3);
    assertEquals(instrumentedData.get("prefix.int").get("value"), "123");
    assertEquals(instrumentedData.get("prefix.int").get("timesAccessed"), 1);

    containedMap2.get("bar");

    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, false);

    assertEquals(instrumentedData.size(), 4);
    assertEquals(instrumentedData.get("prefix.list[3].bar").get("value"), "bar2");
    assertEquals(instrumentedData.get("prefix.list[3].bar").get("timesAccessed"), 1);

    instrumentedData.clear();
    map.collectInstrumentedData(prefix, instrumentedData, true);

    assertEquals(instrumentedData.size(), 7);

    assertEquals(instrumentedData.get("prefix.list[0]").get("value"), "foo");
    assertEquals(instrumentedData.get("prefix.list[1]").get("value"), "88");
    assertEquals(instrumentedData.get("prefix.list[2]").get("value"), "99.0");
    assertEquals(instrumentedData.get("prefix.list[3].bar").get("value"), "bar2");
    assertEquals(instrumentedData.get("prefix.map.a").get("value"), "v_a");
    assertEquals(instrumentedData.get("prefix.map.b").get("value"), "99");
    assertEquals(instrumentedData.get("prefix.int").get("value"), "123");
  }

  @Test
  public void testNoCommonDataComplex()
  {
    DataMap map1 = new DataMap();
    map1.put("map1", "1");
    DataMap map2 = new DataMap();
    map2.put("map2", "2");
    DataList list1 = new DataList();
    list1.add("1");
    DataList list2 = new DataList();
    list2.add("2");

    Object inputs[][] =
    {
      {
        map1,
        map2,
        true
      },
      {
        map1,
        list1,
        true
      },
      {
        map2,
        list2,
        true
      },
      {
        map1,
        list2,
        true
      },
      {
        list1,
        list2,
        true,
      },
      {
        new DataMap(),
        map1,
        true
      },
      {
        new DataList(),
        list1,
        true
      },
      {
        map1,
        map1,
        false
      },
      {
        map2,
        map2,
        false
      },
      {
        list1,
        list1,
        false
      },
      {
        list2,
        list2,
        false
      },
      {
        new DataMap(asMap("m1", map1)),
        new DataMap(asMap("m1", map1)),
        false
      },
      {
        new DataMap(asMap("m0", 1, "m1", map1)),
        new DataMap(asMap("m1", map1, "m2", "2")),
        false
      },
      {
        new DataList(Arrays.asList(map1)),
        new DataList(Arrays.asList(map1)),
        false
      },
      {
        new DataList(Arrays.asList(0, map1)),
        new DataList(Arrays.asList(map1, "2")),
        false
      },
    };

    for (Object[] row : inputs)
    {
      Object o1 = row[0];
      Object o2 = row[1];
      boolean expected = (Boolean) row[2];
      assertEquals(noCommonDataComplex(o1, o2), expected);
      assertEquals(noCommonDataComplex(o2, o1), expected);
    }
  }

  @Test(expectedExceptions = IOException.class)
  public void testMapCycleDetection() throws Exception
  {
    DataMap root = new DataMap();
    root.getUnderlying().put("child", root);
    new JacksonDataCodec().mapToString(root);
  }

  @Test(expectedExceptions = IOException.class)
  public void testListCycleDetection() throws Exception
  {
    DataList root = new DataList();
    root.getUnderlying().add(root);
    new JacksonDataCodec().listToString(root);
  }

  @Test(expectedExceptions = IOException.class)
  public void testMixedCycleDetection() throws Exception
  {
    DataMap root = new DataMap();
    DataList list = new DataList();
    list.getUnderlying().add(root);
    root.getUnderlying().put("child", list);
    new JacksonDataCodec().mapToString(root);
  }

  @Test
  public void testNonCyclicDuplicates() throws Exception
  {
    DataMap root = new DataMap();
    DataMap sub = new DataMap();
    sub.put("a", "b");
    root.put("c", sub);
    root.put("d", sub);
    new JacksonDataCodec().mapToString(root);
  }
}
