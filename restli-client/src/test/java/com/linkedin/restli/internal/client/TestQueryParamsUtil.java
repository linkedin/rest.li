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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema.Type;
import com.linkedin.data.schema.MaskMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Joel Hare
 */
public class TestQueryParamsUtil
{
  @Test
  public void testConvertToDataMap()
  {
    Map<String, Object> queryParams = new HashMap<>();

    Map<String, Object> hashMapParam = new HashMap<>();
    hashMapParam.put("someField", "someValue");
    hashMapParam.put("foo", "bar");
    hashMapParam.put("notifications", ImmutableMap.of("a", "b"));
    hashMapParam.put("type", Type.BOOLEAN);

    List<Object> subList = new ArrayList<>();
    subList.add("first");
    subList.add(ImmutableMap.of("x", "1", "y", 2));
    subList.add(ImmutableList.of(Type.ARRAY, Type.BYTES, Type.MAP));
    hashMapParam.put("subList", subList);

    List<Object> arrayListParam = new ArrayList<>();
    arrayListParam.add("x");
    arrayListParam.add("y");
    arrayListParam.add(hashMapParam);
    arrayListParam.add(Type.DOUBLE);

    queryParams.put("hashMapParam", hashMapParam);
    queryParams.put("arrayListParam", arrayListParam);

    DataMap dataMapQueryParams = QueryParamsUtil.convertToDataMap(queryParams);

    UriBuilder uriBuilder = new UriBuilder();
    URIParamUtils.addSortedParams(uriBuilder, dataMapQueryParams);
    String query = uriBuilder.build().getQuery();
    Assert.assertEquals(query,
        "arrayListParam=List(x,y,(foo:bar,notifications:(a:b),someField:someValue,subList:List(first,(x:1,y:2),List(ARRAY,BYTES,MAP)),type:BOOLEAN),DOUBLE)"
        + "&hashMapParam=(foo:bar,notifications:(a:b),someField:someValue,subList:List(first,(x:1,y:2),List(ARRAY,BYTES,MAP)),type:BOOLEAN)");
  }

  @Test (expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Map key '1' is not of type String")
  public void testNonStringKeyToDataMap()
  {
    Map<String, Object> queryParams = new HashMap<>();

    Map<Object, Object> hashMapParam = new HashMap<>();
    hashMapParam.put(1, "numeric key");

    queryParams.put("hashMapParam", hashMapParam);

    QueryParamsUtil.convertToDataMap(queryParams);
  }

  @Test
  public void testCustomProjectionDataMapSerializer()
  {
    Map<String, Object> queryParams = new HashMap<>();
    Set<PathSpec> specSet = new HashSet<>();
    specSet.add(new PathSpec("random"));
    queryParams.put(RestConstants.FIELDS_PARAM, specSet);
    queryParams.put(RestConstants.PAGING_FIELDS_PARAM, specSet);
    queryParams.put(RestConstants.METADATA_FIELDS_PARAM, specSet);

    DataMap dataMap =
        QueryParamsUtil.convertToDataMap(queryParams, Collections.emptyMap(),
            AllProtocolVersions.LATEST_PROTOCOL_VERSION, (paramName, pathSpecs) -> {
              DataMap dataMap1 = new DataMap();
              dataMap1.put("random", 2);
              return dataMap1;
            });

    DataMap expectedMap = new DataMap();
    expectedMap.put("random", 2);
    Assert.assertEquals(dataMap.getDataMap(RestConstants.FIELDS_PARAM), expectedMap);
    Assert.assertEquals(dataMap.getDataMap(RestConstants.PAGING_FIELDS_PARAM), expectedMap);
    Assert.assertEquals(dataMap.getDataMap(RestConstants.METADATA_FIELDS_PARAM), expectedMap);
  }

  @Test
  public void testCustomProjectionDataMapSerializerReturningNull()
  {
    Map<String, Object> queryParams = new HashMap<>();
    Set<PathSpec> specSet = new HashSet<>();
    specSet.add(new PathSpec("random"));
    queryParams.put(RestConstants.FIELDS_PARAM, specSet);
    queryParams.put(RestConstants.PAGING_FIELDS_PARAM, specSet);
    queryParams.put(RestConstants.METADATA_FIELDS_PARAM, specSet);

    DataMap dataMap =
        QueryParamsUtil.convertToDataMap(queryParams, Collections.emptyMap(),
            AllProtocolVersions.LATEST_PROTOCOL_VERSION, (paramName, pathSpecs) -> null);
    Assert.assertNull(dataMap.getDataMap(RestConstants.FIELDS_PARAM));
    Assert.assertNull(dataMap.getDataMap(RestConstants.PAGING_FIELDS_PARAM));
    Assert.assertNull(dataMap.getDataMap(RestConstants.METADATA_FIELDS_PARAM));
  }

  @Test
  public void testPreSerializedProjectionParams()
  {
    Map<String, Object> queryParams = new HashMap<>();
    queryParams.put(RestConstants.FIELDS_PARAM, "fields");
    queryParams.put(RestConstants.PAGING_FIELDS_PARAM, "paging");
    queryParams.put(RestConstants.METADATA_FIELDS_PARAM, "metadata");

    DataMap dataMap =
        QueryParamsUtil.convertToDataMap(queryParams, Collections.emptyMap(),
            AllProtocolVersions.LATEST_PROTOCOL_VERSION, (paramName, pathSpecs) -> null);

    Assert.assertEquals("fields", dataMap.getString(RestConstants.FIELDS_PARAM));
    Assert.assertEquals("paging", dataMap.getString(RestConstants.PAGING_FIELDS_PARAM));
    Assert.assertEquals("metadata", dataMap.getString(RestConstants.METADATA_FIELDS_PARAM));
  }

  @Test
  public void testMaskTreeProjectionParams()
  {
    Map<String, Object> queryParams = new HashMap<>();
    MaskMap fieldsMask = TestRecord.createMask().withId().withMessage();
    queryParams.put(RestConstants.FIELDS_PARAM, fieldsMask.getDataMap());
    DataMap pagingMask = new DataMap();
    pagingMask.put("paging", MaskMap.POSITIVE_MASK);
    queryParams.put(RestConstants.PAGING_FIELDS_PARAM, pagingMask);
    DataMap metaDataMask = new DataMap();
    metaDataMask.put("metadata", MaskMap.POSITIVE_MASK);
    queryParams.put(RestConstants.METADATA_FIELDS_PARAM, metaDataMask);

    DataMap dataMap =
        QueryParamsUtil.convertToDataMap(queryParams, Collections.emptyMap(),
            AllProtocolVersions.LATEST_PROTOCOL_VERSION, (paramName, pathSpecs) -> null);

    Assert.assertSame(dataMap.get(RestConstants.FIELDS_PARAM), fieldsMask.getDataMap());
    Assert.assertSame(dataMap.get(RestConstants.PAGING_FIELDS_PARAM), pagingMask);
    Assert.assertSame(dataMap.get(RestConstants.METADATA_FIELDS_PARAM), metaDataMask);

    UriBuilder uriBuilder = new UriBuilder();
    URIParamUtils.addSortedParams(uriBuilder, dataMap);
    String uri = uriBuilder.build().getQuery();
    Assert.assertEquals(uri, "fields=message,id&metadataFields=metadata&pagingFields=paging");
  }
}
