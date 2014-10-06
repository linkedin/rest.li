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
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Soojung Ha
 */
public class TestBatchPatchArgumentBuilder
{
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

    return new Object[][]
        {
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"10001\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"10002\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                new Object[]{10001, 10002},
                patches
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "{\"entities\":{\"string1=apples&string2=oranges\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"string1=coffee&string2=tea\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                new Object[]{new CompoundKey().append("string1", "apples").append("string2", "oranges"),
                    new CompoundKey().append("string1", "coffee").append("string2", "tea")},
                patches
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "{\"entities\":{\"(string1:apples,string2:oranges)\":{\"patch\":{\"$set\":{\"a\":\"someString\"}}}," +
                    "\"(string1:coffee,string2:tea)\":{\"patch\":{\"$set\":{\"a\":\"someOtherString\"}}}}}",
                new Object[]{new CompoundKey().append("string1", "apples").append("string2", "oranges"),
                    new CompoundKey().append("string1", "coffee").append("string2", "tea")},
                patches
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilder(ProtocolVersion version, String requestEntity, Object[] keys, PatchRequest<MyComplexKey>[] patches) throws RestLiSyntaxException
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(requestEntity, version);
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
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, param);
    Set<Object> batchKeys = new HashSet<Object>(Arrays.asList(keys));
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, batchKeys);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 2);

    RestLiArgumentBuilder argumentBuilder = new BatchPatchArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);

    Assert.assertEquals(args.length, 1);
    Assert.assertTrue(args[0] instanceof BatchPatchRequest);
    Map<?, ?> data = ((BatchPatchRequest)args[0]).getData();
    Assert.assertEquals(data.size(), keys.length);
    for (int i = 0; i < keys.length; i++)
    {
      Assert.assertEquals(data.get(keys[i]), patches[i]);
    }

    EasyMock.verify(request, descriptor, context, routingResult);
  }
}
