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


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RoutingException;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
public class TestBatchPatchArgumentBuilder
{
  private static final String ERROR_MESSAGE_BATCH_KEYS_MISMATCH = "Batch request mismatch";
  private static final String ERROR_MESSAGE_DUPLICATE_BATCH_KEYS = "Duplicate key in batch request";

  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
    Map<String, Object> aMap1 = new HashMap<String, Object>();
    aMap1.put("a", "someString");
    Map<String, Object> setMap1 = new HashMap<String, Object>();
    setMap1.put("$set", new DataMap(aMap1));
    Map<String, Object> patchMap1 = new HashMap<String, Object>();
    patchMap1.put("patch", new DataMap(setMap1));
    PatchRequest<MyComplexKey> patch1 = new PatchRequest<MyComplexKey>(new DataMap(patchMap1));

    Map<String, Object> aMap2 = new HashMap<String, Object>();
    aMap2.put("a", "someOtherString");
    Map<String, Object> setMap2 = new HashMap<String, Object>();
    setMap2.put("$set", new DataMap(aMap2));
    Map<String, Object> data2 = new HashMap<String, Object>();
    data2.put("patch", new DataMap(setMap2));
    PatchRequest<MyComplexKey> patch2 = new PatchRequest<MyComplexKey>(new DataMap(data2));

    @SuppressWarnings("rawtypes")
    PatchRequest[] patches = new PatchRequest[]{patch1, patch2};

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
                "{\"entities\":{\"10001\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"10002\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                new Object[]{10001, 10002},
                patches
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"string1=apples&string2=oranges\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"string1=coffee&string2=tea\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                compoundKeys,
                patches
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"(string1:apples,string2:oranges)\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"(string1:coffee,string2:tea)\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                compoundKeys,
                patches
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"a=A1&b=111\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"a=A2&b=222\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                complexResourceKeys,
                patches
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"($params:(),a:A2,b:222)\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}," +
                    "\"($params:(),a:A1,b:111)\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}}}",
                complexResourceKeys,
                patches
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilderSuccess(ProtocolVersion version, Key primaryKey, Key[] associationKeys,
      String requestEntity, Object[] keys, PatchRequest<MyComplexKey>[] patches)
  {
    Set<Object> batchKeys = new HashSet<Object>(Arrays.asList(keys));
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(requestEntity, version);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, primaryKey, associationKeys, batchKeys);

    @SuppressWarnings("rawtypes")
    Parameter<BatchPatchRequest> param = new Parameter<BatchPatchRequest>(
      "",
      BatchPatchRequest.class,
      null,
      false,
      null,
      Parameter.ParamType.BATCH,
      false,
      new AnnotationSet(new Annotation[]{}));
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(
        model, 2, Collections.singletonList(param));
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(batchKeys, true, false);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, context);

    RestLiArgumentBuilder argumentBuilder = new BatchPatchArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);

    assertEquals(args.length, 1);
    assertTrue(args[0] instanceof BatchPatchRequest);
    Map<?, ?> data = ((BatchPatchRequest)args[0]).getData();
    assertEquals(data.size(), keys.length);
    for (int i = 0; i < keys.length; i++)
    {
      assertEquals(data.get(keys[i]), patches[i]);
    }

    verify(request, descriptor, context, routingResult);
  }

  @DataProvider
  private Object[][] failureData()
  {
    Object[] compoundKeys = new Object[]{new CompoundKey().append("string1", "apples").append("string2", "oranges"),
        new CompoundKey().append("string1", "XYZ").append("string2", "tea")};

    Object[] complexResourceKeys = new Object[]{
        new ComplexResourceKey<MyComplexKey, EmptyRecord>(new MyComplexKey().setA("XYZ").setB(111L), new EmptyRecord()),
        new ComplexResourceKey<MyComplexKey, EmptyRecord>(new MyComplexKey().setA("A2").setB(222L), new EmptyRecord())};

    return new Object[][]
        {
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("integerKey", Integer.class, new IntegerDataSchema()),
                null,
                "{\"entities\":{\"10001\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"10002\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                new Object[]{10001, 99999},
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("integerKey", Integer.class, new IntegerDataSchema()),
                null,
                "{\"entities\":{\"10001\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"99999\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                new Object[]{10001, 10002},
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("integerKey", Integer.class, new IntegerDataSchema()),
                null,
                "{\"entities\":{\"10001\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"10002\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                new Object[]{10001, 10002, 10003},
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"string1=apples&string2=oranges\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"string1=coffee&string2=tea\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                compoundKeys,
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                // Duplicate key in the entities body
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"string1=apples&string2=oranges\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"string2=oranges&string1=apples\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                compoundKeys,
                ERROR_MESSAGE_DUPLICATE_BATCH_KEYS
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"(string1:apples,string2:oranges)\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"(string1:coffee,string2:tea)\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                compoundKeys,
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                // Duplicate key in the entities body
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("compoundKey", CompoundKey.class, null),
                new Key[] { new Key("string1", String.class), new Key("string2", String.class) },
                "{\"entities\":{\"(string1:apples,string2:oranges)\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"(string2:oranges,string1:apples)\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                compoundKeys,
                ERROR_MESSAGE_DUPLICATE_BATCH_KEYS
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"a=A1&b=111\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"a=A2&b=222\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                complexResourceKeys,
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                // Duplicate key in the entities body
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"a=A2&b=222\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"b=222&a=A2\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                complexResourceKeys,
                ERROR_MESSAGE_DUPLICATE_BATCH_KEYS
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"($params:(),a:A2,b:222)\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}," +
                    "\"($params:(),a:A1,b:111)\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}}}",
                complexResourceKeys,
                ERROR_MESSAGE_BATCH_KEYS_MISMATCH
            },
            {
                // Duplicate key in the entities body
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                new Key("complexKey", ComplexResourceKey.class, null),
                null,
                "{\"entities\":{\"($params:(),a:A2,b:222)\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}," +
                    "\"($params:(),b:222,a:A2)\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}}}",
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

    RestLiArgumentBuilder argumentBuilder = new BatchPatchArgumentBuilder();
    try
    {
      argumentBuilder.extractRequestData(routingResult, request);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertTrue(e.getMessage().contains(errorMessage));
    }

    verify(request, context, routingResult);
  }
}
