/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.internal.client;

import com.google.common.collect.ImmutableMap;
import com.linkedin.data.DataMap;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.internal.common.URIParamUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Joel Hare
 */
public class TestQueryParamsUtil
{
  @Test
  public void testConvertToDataMap() {
    Map<String, Object> queryParams = new HashMap<>();

    Map<String, Object> hashMapParam = new HashMap<>();
    hashMapParam.put("someField", "someValue");
    hashMapParam.put("foo", "bar");
    hashMapParam.put("notifications", ImmutableMap.of("a", "b"));

    List<Object> subList = new ArrayList<>();
    subList.add("first");
    subList.add(ImmutableMap.of("x", "1", "y", 2));
    hashMapParam.put("subList", subList);

    List<Object> arrayListParam = new ArrayList<>();
    arrayListParam.add("x");
    arrayListParam.add("y");
    arrayListParam.add(hashMapParam);

    queryParams.put("hashMapParam", hashMapParam);
    queryParams.put("arrayListParam", arrayListParam);

    DataMap dataMapQueryParams = QueryParamsUtil.convertToDataMap(queryParams);

    UriBuilder uriBuilder = new UriBuilder();
    URIParamUtils.addSortedParams(uriBuilder, dataMapQueryParams);
    String query = uriBuilder.build().getQuery();
    Assert.assertEquals(query,
        "arrayListParam=List(x,y,(foo:bar,notifications:(a:b),someField:someValue,subList:List(first,(x:1,y:2))))"
        + "&hashMapParam=(foo:bar,notifications:(a:b),someField:someValue,subList:List(first,(x:1,y:2)))");
  }

  @Test (expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Map key '1' is not of type String")
  public void testNonStringKeyToDataMap() {
    Map<String, Object> queryParams = new HashMap<>();

    Map<Object, Object> hashMapParam = new HashMap<>();
    hashMapParam.put(1, "numeric key");

    queryParams.put("hashMapParam", hashMapParam);

    QueryParamsUtil.convertToDataMap(queryParams);
  }

}
