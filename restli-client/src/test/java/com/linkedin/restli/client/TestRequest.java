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
<<<<<<< HEAD
 */

package com.linkedin.restli.client;


import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceProperties;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.common.ResourcePropertiesImpl;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;


public class TestRequest
{
  @Test
  public void testToSecureString()
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
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(
        "abc",
        TestRecord.class,
        spec,
        RestliRequestOptions.DEFAULT_OPTIONS);

    Request<TestRecord> request = builder.id(5L).build();

    Assert.assertEquals(
        request.toSecureString(),
        "com.linkedin.restli.client.GetRequest{_method=get, _baseUriTemplate=abc, _methodName=null, _requestOptions={_protocolVersionOption: USE_LATEST_IF_AVAILABLE, _requestCompressionOverride: null, _contentType: null, _acceptTypes: null}}");
  }

  @Test
  public void testHeadersCaseInsensitiveGet()
  {
    final long id = 42l;
    final ResourceSpec spec = new ResourceSpecImpl(
        EnumSet.allOf(ResourceMethod.class),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Long.class,
        null,
        null,
        TestRecord.class,
        Collections.<String, Class<?>> emptyMap());
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(
        "abc",
        TestRecord.class,
        spec,
        RestliRequestOptions.DEFAULT_OPTIONS);
    Request<TestRecord> request = builder.id(id).addHeader("header", "value").build();
    Assert.assertEquals(request.getHeaders().get("HEADER"), "value");
  }

  @Test
  public void testHeadersCaseInsensitiveAdd()
  {
    final long id = 42l;
    final ResourceSpec spec = new ResourceSpecImpl(
        EnumSet.allOf(ResourceMethod.class),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Long.class,
        null,
        null,
        TestRecord.class,
        Collections.<String, Class<?>> emptyMap());
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(
        "abc",
        TestRecord.class,
        spec,
        RestliRequestOptions.DEFAULT_OPTIONS);
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
    final ResourceSpec spec = new ResourceSpecImpl(
        EnumSet.allOf(ResourceMethod.class),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Collections.<String, DynamicRecordMetadata> emptyMap(),
        Long.class,
        null,
        null,
        TestRecord.class,
        Collections.<String, Class<?>> emptyMap());
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(
        "abc",
        TestRecord.class,
        spec,
        RestliRequestOptions.DEFAULT_OPTIONS);
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


    Request<TestRecord> request = new Request<TestRecord>(ResourceMethod.GET,
                                                          null,
                                                          Collections.<String, String>emptyMap(),
                                                          new EntityResponseDecoder<TestRecord>(TestRecord.class),
                                                          expectedResourceSpec,
                                                          Collections.<String, Object>emptyMap(),
                                                          Collections.<String, Class<?>>emptyMap(),
                                                          null,
                                                          "testRecord",
                                                          pathKeys,
                                                          RestliRequestOptions.DEFAULT_OPTIONS);

    ResourceProperties expectedResourceProperties =
        new ResourcePropertiesImpl(expectedResourceSpec.getSupportedMethods(),
                                   expectedResourceSpec.getKeyType(),
                                   expectedResourceSpec.getComplexKeyType(),
                                   expectedResourceSpec.getValueType(),
                                   expectedResourceSpec.getKeyParts());

    Assert.assertEquals(request.getResourceProperties(), expectedResourceProperties);
  }
}
