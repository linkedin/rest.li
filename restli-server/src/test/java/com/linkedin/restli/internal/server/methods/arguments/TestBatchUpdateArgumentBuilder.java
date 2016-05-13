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


import com.linkedin.data.schema.IntegerDataSchema;
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
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RoutingException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
  private static final String ERROR_MESSAGE_BATCH_KEYS_MISMATCH = "Batch request mismatch";
  private static final String ERROR_MESSAGE_DUPLICATE_BATCH_KEYS = "Duplicate key in batch request";

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
                new Key("integerKey", Integer.class, new IntegerDataSchema()),
                null,
                "{\"entities\":{\"10001\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"10002\":{\"b\":456,\"a\":\"XY\"}}}",
                new Object[]{10001, 10002}
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"string1=apples&string2=oranges\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"string1=coffee&string2=tea\":{\"b\":456,\"a\":\"XY\"}}}",
                compoundKeys
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"(string1:coffee,string2:tea)\":{\"b\":456,\"a\":\"XY\"}," +
                    "\"(string1:apples,string2:oranges)\":{\"b\":123,\"a\":\"abc\"}}}",
                compoundKeys
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"a=A1&b=111\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"a=A2&b=222\":{\"b\":456,\"a\":\"XY\"}}}",
                complexResourceKeys
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"($params:(),a:A2,b:222)\":{\"b\":456,\"a\":\"XY\"}," +
                    "\"($params:(),a:A1,b:111)\":{\"b\":123,\"a\":\"abc\"}}}",
                complexResourceKeys
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilderSuccess(ProtocolVersion version, Key primaryKey, Key[] associationKeys, String requestEntity, Object[] keys)
  {
    Set<Object> batchKeys = new HashSet<Object>(Arrays.asList(keys));
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(requestEntity, version);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, primaryKey, associationKeys, batchKeys);
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
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(
        model, 3, Collections.singletonList(param));
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(batchKeys, true, false);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, context);

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
                new Key("integerKey", Integer.class, new IntegerDataSchema()),
                null,
                "{\"entities\":{\"10001\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"10002\":{\"b\":456,\"a\":\"XY\"}}}",
                new Object[]{10001, 99999},
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("integerKey", Integer.class, new IntegerDataSchema()),
                null,
                "{\"entities\":{\"10001\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"99999\":{\"b\":456,\"a\":\"XY\"}}}",
                new Object[]{10001, 10002},
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("integerKey", Integer.class, new IntegerDataSchema()),
                null,
                "{\"entities\":{\"10001\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"10002\":{\"b\":456,\"a\":\"XY\"}}}",
                new Object[]{10001, 10002, 10003},
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"string1=apples&string2=oranges\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"string1=coffee&string2=tea\":{\"b\":456,\"a\":\"XY\"}}}",
                compoundKeys,
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                // Duplicate key in the entities body
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"string1=coffee&string2=tea\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"string2=tea&string1=coffee\":{\"b\":456,\"a\":\"XY\"}}}",
                compoundKeys,
                ERROR_MESSAGE_DUPLICATE_BATCH_KEYS
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"(string1:coffee,string2:tea)\":{\"b\":456,\"a\":\"XY\"}," +
                    "\"(string1:apples,string2:oranges)\":{\"b\":123,\"a\":\"abc\"}}}",
                compoundKeys,
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                // Duplicate key in the entities body
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"(string1:coffee,string2:tea)\":{\"b\":456,\"a\":\"XY\"}," +
                    "\"(string2:tea,string1:coffee)\":{\"b\":123,\"a\":\"abc\"}}}",
                compoundKeys,
                ERROR_MESSAGE_DUPLICATE_BATCH_KEYS
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"a=A1&b=999\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"a=A2&b=222\":{\"b\":456,\"a\":\"XY\"}}}",
                complexResourceKeys,
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                // Duplicate key in the entities body
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"a=A1&b=111\":{\"b\":123,\"a\":\"abc\"}," +
                    "\"b=111&a=A1\":{\"b\":456,\"a\":\"XY\"}}}",
                complexResourceKeys,
                ERROR_MESSAGE_DUPLICATE_BATCH_KEYS
            }
        };
  }

  @Test(dataProvider = "failureData")
  public void testFailure(ProtocolVersion version, Key primaryKey, Key[] associationKeys, String requestEntity, Object[] keys, String errorMessage)
  {
    Set<Object> batchKeys = new HashSet<Object>(Arrays.asList(keys));
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(requestEntity, version);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, primaryKey, associationKeys, batchKeys);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(batchKeys, false, false);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, context);

    RestLiArgumentBuilder argumentBuilder = new BatchUpdateArgumentBuilder();
    try
    {
      argumentBuilder.extractRequestData(routingResult, request);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertTrue(e.getMessage().contains(errorMessage));
    }

    verify(request, model, descriptor, context, routingResult);
  }
}