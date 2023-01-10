package com.linkedin.util;

import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestDataComplexUtil {

  @Test
  void testConvertDroppingNulls()
  {
    DataMap parent = DataComplexUtil.convertMap(inputMap());

    Assert.assertNotNull(parent);
    Assert.assertEquals(parent.size(), 2);

    Assert.assertFalse(parent.containsKey("child1"));

    Assert.assertTrue(parent.containsKey("child2"));
    DataMap child2 = parent.getDataMap("child2");

    Assert.assertNotNull(child2);
    Assert.assertEquals(child2.size(), 1);

    Assert.assertTrue(child2.containsKey("gchild1"));
    Assert.assertEquals(child2.get("gchild1"), 123);

    Assert.assertFalse(child2.containsKey("gchild2"));

    Assert.assertTrue(parent.containsKey("child3"));
    DataList child3 = parent.getDataList("child3");

    Assert.assertNotNull(child3);
    Assert.assertEquals(child3.size(), 1);

    Assert.assertEquals(child3.get(0), "gchild3");
  }

  @Test
  void testConvertRetainingNulls()
  {
    DataMap parent = DataComplexUtil.convertMapRetainingNulls(inputMap());

    Assert.assertNotNull(parent);
    Assert.assertEquals(parent.size(), 3);

    Assert.assertTrue(parent.containsKey("child1"));
    Assert.assertEquals(parent.get("child1"), Data.NULL);

    Assert.assertTrue(parent.containsKey("child2"));
    DataMap child2 = parent.getDataMap("child2");

    Assert.assertNotNull(child2);
    Assert.assertEquals(child2.size(), 2);

    Assert.assertTrue(child2.containsKey("gchild1"));
    Assert.assertEquals(child2.get("gchild1"), 123);

    Assert.assertTrue(child2.containsKey("gchild2"));
    Assert.assertEquals(child2.get("gchild2"), Data.NULL);

    Assert.assertTrue(parent.containsKey("child3"));
    DataList child3 = parent.getDataList("child3");

    Assert.assertNotNull(child3);
    Assert.assertEquals(child3.size(), 2);

    Assert.assertEquals(child3.get(0), "gchild3");
    Assert.assertEquals(child3.get(1), Data.NULL);
  }

  @Test
  void testConvertMapWithDataComplex()
  {
    DataMap parent = DataComplexUtil.convertMap(inputMapWithDataComplex());

    Assert.assertNotNull(parent);
    Assert.assertEquals(parent.size(), 2);

    Assert.assertTrue(parent.containsKey("child1"));
    DataMap child1 = parent.getDataMap("child1");

    Assert.assertNotNull(child1);
    Assert.assertEquals(child1.size(), 1);

    Assert.assertTrue(child1.containsKey("gchild1"));
    Assert.assertEquals(child1.get("gchild1"), 123);

    Assert.assertTrue(parent.containsKey("child2"));
    DataList child2 = parent.getDataList("child2");

    Assert.assertNotNull(child2);
    Assert.assertEquals(child2.size(), 1);

    DataList gchild2 = child2.getDataList(0);
    Assert.assertNotNull(gchild2);
    Assert.assertEquals(gchild2.size(), 1);
    Assert.assertEquals(gchild2.get(0), "ggchild2");
  }

  @Test
  void testConvertListWithoutRetainingNull()
  {
    List<String> listWithNull = Arrays.asList("element1", null, "element2");
    DataList dataList = DataComplexUtil.convertList(listWithNull);

    Assert.assertNotNull(dataList);
    Assert.assertEquals(dataList.size(), 2);
    Assert.assertEquals(dataList.get(0), "element1");
    Assert.assertEquals(dataList.get(1), "element2");
  }

  @Test
  void testConvertListWithRetainingNull()
  {
    List<String> listWithNull = Arrays.asList("element1", null, "element2");
    DataList dataList = DataComplexUtil.convertListRetainingNulls(listWithNull);

    Assert.assertNotNull(dataList);
    Assert.assertEquals(dataList.size(), 3);
    Assert.assertEquals(dataList.get(0), "element1");
    Assert.assertEquals(dataList.get(1), Data.NULL);
    Assert.assertEquals(dataList.get(2), "element2");
  }

  private Map<String, ?> inputMap()
  {
    Map<String, Object> parent = new HashMap<>();

    parent.put("child1", null);

    Map<String, Integer> child2 = new HashMap<>();
    child2.put("gchild1", 123);
    child2.put("gchild2", null);
    parent.put("child2", child2);

    List<String> child3 = new ArrayList<>();
    child3.add("gchild3");
    child3.add(null);
    parent.put("child3", child3);

    return parent;
  }

  private Map<String, ?> inputMapWithDataComplex()
  {
    Map<String, Object> parent = new HashMap<>();

    DataMap child1 = new DataMap();
    child1.put("gchild1", 123);
    parent.put("child1", child1);

    DataList child2 = new DataList();
    DataList gchild2 = new DataList();
    gchild2.add("ggchild2");
    child2.add(gchild2);
    parent.put("child2", child2);

    return parent;
  }
}
