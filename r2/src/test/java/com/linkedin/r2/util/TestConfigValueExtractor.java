package com.linkedin.r2.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestConfigValueExtractor
{
  @Test
  public void testNullObject()
  {
    List<String> emptyList = ConfigValueExtractor.buildList(null, ",");
    Assert.assertTrue(emptyList.isEmpty());
  }

  @Test
  public void testStringObject()
  {
    List<String> actualList = ConfigValueExtractor.buildList("foo, bar, baz", ",");
    List<String> expectedList = Arrays.asList(new String[]{"foo", "bar", "baz"});
    Assert.assertEquals(expectedList, actualList);
  }

  @Test
  public void testListObject()
  {
    List<String> inputList = new ArrayList<String>();
    inputList.add("foo");
    inputList.add("bar");
    inputList.add("baz");

    List<String> actualList = ConfigValueExtractor.buildList(inputList, ",");
    Assert.assertEquals(inputList, actualList);
  }

  @Test
  public void testListAndString()
  {
    List<String> inputList = new ArrayList<String>();
    inputList.add("foo");
    inputList.add("bar");
    inputList.add("baz");

    List<String> configForList = ConfigValueExtractor.buildList(inputList, ",");
    List<String> configForString = ConfigValueExtractor.buildList("foo, bar, baz", ",");

    Assert.assertTrue(configForList.equals(configForString));
  }
}
