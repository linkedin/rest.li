package com.linkedin.restli.internal.server.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TestDataMapUtils
{
  @Test
  public void dataMapCleanUp()
  {
    DataMap originalDataMap = new DataMap(ImmutableMap.<String, Object>builder()
        .put("float", 0F)
        .put("string", "str")
        .put("integer", 1)
        .put("long", 2L)
        .put("double", Data.NULL)
        .put("boolean", false)
        .put("array", new DataList(ImmutableList.of(100L, 110L, Data.NULL)))
        .put("map", new DataMap(ImmutableMap.of(
            "20", "200",
            "21", "210",
            "22", Data.NULL)))
        .put("arrayofarray", new DataList(ImmutableList.of(
            new DataList(ImmutableList.of(100L, 110L)),
            new DataList(ImmutableList.of(500, Data.NULL)))))
        .put("innerRecord", new DataMap(ImmutableMap.of(
            "float", 30.0F,
            "string", "str2",
            "innerInnerRecord", new DataMap(ImmutableMap.of(
                "float", 40.0F,
                "integer", Data.NULL)))))
        .build());

    DataMapUtils.removeNulls(originalDataMap);

    DataMap cleanedDataMap = new DataMap(ImmutableMap.<String, Object>builder()
        .put("float", 0F)
        .put("string", "str")
        .put("integer", 1)
        .put("long", 2L)
        .put("boolean", false)
        .put("array", new DataList(ImmutableList.of(100L, 110L)))
        .put("map", new DataMap(ImmutableMap.of(
            "20", "200",
            "21", "210")))
        .put("arrayofarray", new DataList(ImmutableList.of(
            new DataList(ImmutableList.of(100L, 110L)),
            new DataList(ImmutableList.of(500)))))
        .put("innerRecord", new DataMap(ImmutableMap.of(
            "float", 30.0F,
            "string", "str2",
            "innerInnerRecord", new DataMap(ImmutableMap.of(
                "float", 40.0F)))))
        .build());

    assertEquals(originalDataMap, cleanedDataMap, "DataMap not cleaned up as expected");
  }
}
