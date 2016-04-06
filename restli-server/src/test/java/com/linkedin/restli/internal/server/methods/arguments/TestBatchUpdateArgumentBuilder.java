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

package com.linkedin.restli.internal.server.methods.arguments;


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RoutingException;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * @author Soojung Ha
 */
public class TestBatchUpdateArgumentBuilder
{
  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
    Object[] compoundKeys = new Object[]{new CompoundKey().append("string1", "apples").append("string2", "oranges"),
        new CompoundKey().append("string1", "coffee").append("string2", "tea")};
    Object[] complexResourceKeys = new Object[]{
        new ComplexResourceKey<MyComplexKey, EmptyRecord>(new MyComplexKey().setA("A1").setB(111L), new EmptyRecord()),
        new ComplexResourceKey<MyComplexKey, EmptyRecord>(new MyComplexKey().setA("A2").setB(222L), new EmptyRecord())};

    return new Object[][]
        {
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"10001\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"10002\":{\"b\":456,\"a\":\"XY\"}}}",
                new Object[]{10001, 10002}
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"string1=apples&string2=oranges\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"string1=coffee&string2=tea\":{\"b\":456,\"a\":\"XY\"}}}",
                compoundKeys
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "{\"entities\":{\"(string1:coffee,string2:tea)\":{\"b\":456,\"a\":\"XY\"}," +
                    "\"(string1:apples,string2:oranges)\":{\"b\":123,\"a\":\"abc\"}}}",
                compoundKeys
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"a=A1&b=111\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"a=A2&b=222\":{\"b\":456,\"a\":\"XY\"}}}",
                complexResourceKeys
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "{\"entities\":{\"($params:(),a:A2,b:222)\":{\"b\":456,\"a\":\"XY\"}," +
                    "\"($params:(),a:A1,b:111)\":{\"b\":123,\"a\":\"abc\"}}}",
                complexResourceKeys
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilderSuccess(ProtocolVersion version, String requestEntity, Object[] keys)
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(requestEntity, version);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, null, false);
    @SuppressWarnings("rawtypes")
    Parameter<BatchUpdateRequest> param = new Parameter<BatchUpdateRequest>(
        "",
        BatchUpdateRequest.class,
        null,
        false,
        null,
        Parameter.ParamType.BATCH,
        false,
        new AnnotationSet(new Annotation[]{}));
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, param);
    Set<Object> batchKeys = new HashSet<Object>(Arrays.asList(keys));
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, batchKeys, true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 2);

    RestLiArgumentBuilder argumentBuilder = new BatchUpdateArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);

    assertEquals(args.length, 1);
    assertTrue(args[0] instanceof BatchUpdateRequest);
    Map<?, ?> data = ((BatchUpdateRequest)args[0]).getData();
    assertEquals(data.size(), 2);
    MyComplexKey entity1 = (MyComplexKey) data.get(keys[0]);
    MyComplexKey entity2 = (MyComplexKey) data.get(keys[1]);
    assertEquals(entity1.getA(), "abc");
    assertEquals((long) entity1.getB(), 123L);
    assertEquals(entity2.getA(), "XY");
    assertEquals((long) entity2.getB(), 456L);

    verify(request, model, descriptor, context, routingResult);
  }

  @DataProvider(name = "failureData")
  private Object[][] failureData()
  {
    Object[] compoundKeys = new Object[]{new CompoundKey().append("string1", "XXX").append("string2", "oranges"),
        new CompoundKey().append("string1", "coffee").append("string2", "tea")};
    Object[] complexResourceKeys = new Object[]{
        new ComplexResourceKey<MyComplexKey, EmptyRecord>(new MyComplexKey().setA("A1").setB(111L), new EmptyRecord()),
        new ComplexResourceKey<MyComplexKey, EmptyRecord>(new MyComplexKey().setA("A2").setB(222L), new EmptyRecord())};

    return new Object[][]
        {
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"10001\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"10002\":{\"b\":456,\"a\":\"XY\"}}}",
                new Object[]{10001, 99999}
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"10001\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"99999\":{\"b\":456,\"a\":\"XY\"}}}",
                new Object[]{10001, 10002}
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"10001\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"10002\":{\"b\":456,\"a\":\"XY\"}}}",
                new Object[]{10001, 10002, 10003}
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"string1=apples&string2=oranges\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"string1=coffee&string2=tea\":{\"b\":456,\"a\":\"XY\"}}}",
                compoundKeys
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "{\"entities\":{\"(string1:coffee,string2:tea)\":{\"b\":456,\"a\":\"XY\"}," +
                    "\"(string1:apples,string2:oranges)\":{\"b\":123,\"a\":\"abc\"}}}",
                compoundKeys
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"a=A1&b=999\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"a=A2&b=222\":{\"b\":456,\"a\":\"XY\"}}}",
                complexResourceKeys
            }
        };
  }

  @Test(dataProvider = "failureData")
  public void testFailure(ProtocolVersion version, String requestEntity, Object[] keys)
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(requestEntity, version);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, null, false);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 1, null);
    Set<Object> batchKeys = new HashSet<Object>(Arrays.asList(keys));
    ServerResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, batchKeys, false);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 1);

    RestLiArgumentBuilder argumentBuilder = new BatchUpdateArgumentBuilder();
    try
    {
      argumentBuilder.extractRequestData(routingResult, request);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertTrue(e.getMessage().contains("Batch request mismatch"));
    }

    verify(request, model, descriptor, context, routingResult);
  }
}