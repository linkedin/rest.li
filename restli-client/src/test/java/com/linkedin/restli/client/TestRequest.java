/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceProperties;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.common.ResourcePropertiesImpl;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;


public class TestRequest
{
  @Test
  public void testToSecureString()
  {
    GetRequestBuilder<Long, TestRecord> builder = generateDummyRequestBuilder();
    Request<TestRecord> request = builder.id(5L).build();
    Assert.assertEquals(
        request.toSecureString(),
        "com.linkedin.restli.client.GetRequest{_method=get, _baseUriTemplate=abc, _methodName=null, "
            + "_requestOptions=RestliRequestOptions{_protocolVersionOption=USE_LATEST_IF_AVAILABLE, "
            + "_requestCompressionOverride=null, _responseCompressionOverride=null, _contentType=null, "
            + "_acceptTypes=null, _acceptResponseAttachments=false}}");
  }

  @Test
  public void testHeadersCaseInsensitiveGet()
  {
    final long id = 42l;
    GetRequestBuilder<Long, TestRecord> builder = generateDummyRequestBuilder();
    Request<TestRecord> request = builder.id(id).addHeader("header", "value").build();
    Assert.assertEquals(request.getHeaders().get("HEADER"), "value");
  }

  @Test
  public void testHeadersCaseInsensitiveAdd()
  {
    final long id = 42l;
    GetRequestBuilder<Long, TestRecord> builder = generateDummyRequestBuilder();
    Request<TestRecord> request = builder
        .id(id)
        .addHeader("header", "value1")
        .addHeader("HEADER", "value2")
        .build();
    Assert.assertEquals(request.getHeaders().get("HEADER"), "value1,value2");
  }

  @Test
  public void testHeadersCaseInsensitiveSet()
  {
    final long id = 42l;
    GetRequestBuilder<Long, TestRecord> builder = generateDummyRequestBuilder();
    Request<TestRecord> request = builder
        .id(id)
        .setHeader("header", "value1")
        .setHeader("HEADER", "value2")
        .build();
    Assert.assertEquals(request.getHeaders().get("header"), "value2");
  }

  @Test
  public void testResourceProperties()
  {
    Set<ResourceMethod> expectedSupportedMethods = new HashSet<ResourceMethod>();
    expectedSupportedMethods.add(ResourceMethod.GET);
    expectedSupportedMethods.add(ResourceMethod.BATCH_PARTIAL_UPDATE);

    ResourceSpec expectedResourceSpec = new ResourceSpecImpl(
                                          expectedSupportedMethods,
                                          null,
                                          null,
                                          ComplexResourceKey.class,
                                          TestRecord.class,
                                          TestRecord.class,
                                          TestRecord.class,
                                          Collections.<String, Object>emptyMap());

    Map<String, Object> pathKeys = new HashMap<String, Object>();
    pathKeys.put("id", new ComplexResourceKey<TestRecord, TestRecord>(new TestRecord(), new TestRecord()));


    Request<TestRecord> request = new Request<TestRecord>(ResourceMethod.GET, null,
                                                          Collections.<String, String>emptyMap(),
                                                          Collections.<HttpCookie>emptyList(),
                                                          new EntityResponseDecoder<TestRecord>(TestRecord.class),
                                                          expectedResourceSpec, Collections.<String, Object>emptyMap(),
                                                          Collections.<String, Class<?>>emptyMap(), null, "testRecord",
                                                          pathKeys, RestliRequestOptions.DEFAULT_OPTIONS, null);

    ResourceProperties expectedResourceProperties =
        new ResourcePropertiesImpl(expectedResourceSpec.getSupportedMethods(),
                                   expectedResourceSpec.getKeyType(),
                                   expectedResourceSpec.getComplexKeyType(),
                                   expectedResourceSpec.getValueType(),
                                   expectedResourceSpec.getKeyParts());

    Assert.assertEquals(request.getResourceProperties(), expectedResourceProperties);
  }

  @DataProvider
  public Object[][] toRequestFieldsData()
  {
    return new Object[][]
      {
          {
              Arrays.asList(new PathSpec("spec1"), new PathSpec("spec2"), new PathSpec("spec1")),
              Arrays.asList(new PathSpec("spec2"), new PathSpec("spec1"), new PathSpec("spec2")),
              asMap("dummyK", "dummyV", "dummyK2", "dummyV2"),
              asMap("dummyK", "dummyV", "dummyK2", "dummyV2"),
              true
          },
          {
              Arrays.asList(new PathSpec("spec1"), new PathSpec("spec2"), new PathSpec("spec1")),
              Arrays.asList(new PathSpec("spec1"), new PathSpec("spec2"), new PathSpec("spec3")),
              asMap("dummyK", "dummyV", "dummyK2", "dummyV2"),
              asMap("dummyK", "dummyV", "dummyK2", "dummyV2"),
              false
          },
          {
              Arrays.asList(new PathSpec("spec1"), new PathSpec("spec2"), new PathSpec("spec1")),
              Arrays.asList(new PathSpec("spec1"), new PathSpec("spec2"), new PathSpec("spec1")),
              asMap("dummyK", "dummyV", "dummyK2", "dummyV2"),
              asMap("dummyK", "dummyV", "dummyK3", "dummyV3"),
              false
          },
          {
              Arrays.asList(new PathSpec("spec1"), new PathSpec("spec2"), new PathSpec("spec1")),
              Arrays.asList(),
              asMap("dummyK", "dummyV", "dummyK2", "dummyV2"),
              asMap("dummyK", "dummyV", "dummyK3", "dummyV3"),
              false
          }
      };
  }

  @Test(dataProvider = "toRequestFieldsData")
  public void testRequestFieldsEqual(List<PathSpec> pathSpecs1, List<PathSpec> pathSpecs2, Map<String,String> param1,  Map<String,String> param2, boolean expect) {
    GetRequestBuilder<Long, TestRecord> builder1 = generateDummyRequestBuilder();
    GetRequestBuilder<Long, TestRecord> builder2 = generateDummyRequestBuilder();

    for (Map.Entry<String, String> entry : param1.entrySet())
    {
      builder1.setParam(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, String> entry : param2.entrySet())
    {
      builder2.setParam(entry.getKey(), entry.getValue());
    }

    builder1.addFields(pathSpecs1.toArray(new PathSpec[pathSpecs1.size()]));
    builder2.addFields(pathSpecs2.toArray(new PathSpec[pathSpecs2.size()]));

    assertEquals(builder1.build().equals(builder2.build()), expect);
  }

  @Test(dataProvider = "toRequestFieldsData")
  public void testRequestMetadataFieldsEqual(List<PathSpec> pathSpecs1, List<PathSpec> pathSpecs2, Map<String,String> param1,  Map<String,String> param2, boolean expect)
  {
    GetRequestBuilder<Long, TestRecord> builder1 = generateDummyRequestBuilder();
    GetRequestBuilder<Long, TestRecord> builder2 = generateDummyRequestBuilder();

    for (Map.Entry<String, String> entry : param1.entrySet())
    {
      builder1.setParam(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, String> entry : param2.entrySet())
    {
      builder2.setParam(entry.getKey(), entry.getValue());
    }

    builder1.addMetadataFields(pathSpecs1.toArray(new PathSpec[pathSpecs1.size()]));
    builder2.addMetadataFields(pathSpecs2.toArray(new PathSpec[pathSpecs2.size()]));

    assertEquals(builder1.build().equals(builder2.build()), expect);
  }

  @Test(dataProvider = "toRequestFieldsData")
  public void testRequestPagingFieldsEqual(List<PathSpec> pathSpecs1, List<PathSpec> pathSpecs2, Map<String,String> param1,  Map<String,String> param2, boolean expect)
  {
    GetRequestBuilder<Long, TestRecord> builder1 = generateDummyRequestBuilder();
    GetRequestBuilder<Long, TestRecord> builder2 = generateDummyRequestBuilder();

    for (Map.Entry<String, String> entry : param1.entrySet())
    {
      builder1.setParam(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, String> entry : param2.entrySet())
    {
      builder2.setParam(entry.getKey(), entry.getValue());
    }

    builder1.addPagingFields(pathSpecs1.toArray(new PathSpec[pathSpecs1.size()]));
    builder2.addPagingFields(pathSpecs2.toArray(new PathSpec[pathSpecs2.size()]));

    assertEquals(builder1.build().equals(builder2.build()), expect);
  }

  @Test
  public void testSetProjectionDataMapSerializer()
  {
    ProjectionDataMapSerializer customSerializer = (paramName, pathSpecs) -> new DataMap();
    GetRequest<TestRecord> getRequest = generateDummyRequestBuilder().build();
    getRequest.setProjectionDataMapSerializer(customSerializer);
    assertEquals(getRequest.getRequestOptions().getProjectionDataMapSerializer(), customSerializer);
  }

  private GetRequestBuilder<Long, TestRecord> generateDummyRequestBuilder ()
  {
    final ResourceSpec spec = new ResourceSpecImpl(
        EnumSet.allOf(ResourceMethod.class),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Long.class,
        null,
        null,
        TestRecord.class,
        Collections.<String, Class<?>> emptyMap());
    return new GetRequestBuilder<Long, TestRecord>(
        "abc",
        TestRecord.class,
        spec,
        RestliRequestOptions.DEFAULT_OPTIONS).id(0L);
  }

  private Map<String, String> asMap(String... strs)
  {
    int index = 0;
    String key = null;
    HashMap<String,String> map = new HashMap<>();
    for (String str : strs)
    {
      if (index % 2 == 0)
      {
        key = str;
      }
      else
      {
        map.put(key, str);
      }
      index++;
    }
    return map;
  }
}
