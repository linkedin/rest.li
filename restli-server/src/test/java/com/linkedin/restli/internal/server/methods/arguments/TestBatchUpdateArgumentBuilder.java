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
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Soojung Ha
 */
public class TestBatchUpdateArgumentBuilder
{
  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
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
                new Object[]{new CompoundKey().append("string1", "apples").append("string2", "oranges"),
                    new CompoundKey().append("string1", "coffee").append("string2", "tea")}
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "{\"entities\":{\"(string1:coffee,string2:tea)\":{\"b\":456,\"a\":\"XY\"}," +
                    "\"(string1:apples,string2:oranges)\":{\"b\":123,\"a\":\"abc\"}}}",
                new Object[]{new CompoundKey().append("string1", "apples").append("string2", "oranges"),
                    new CompoundKey().append("string1", "coffee").append("string2", "tea")}
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilder(ProtocolVersion version, String requestEntity, Object[] keys) throws RestLiSyntaxException
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
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, batchKeys);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 2);

    RestLiArgumentBuilder argumentBuilder = new BatchUpdateArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);

    Assert.assertEquals(args.length, 1);
    Assert.assertTrue(args[0] instanceof BatchUpdateRequest);
    Map<?, ?> data = ((BatchUpdateRequest)args[0]).getData();
    Assert.assertEquals(data.size(), 2);
    MyComplexKey entity1 = (MyComplexKey) data.get(keys[0]);
    MyComplexKey entity2 = (MyComplexKey) data.get(keys[1]);
    Assert.assertEquals(entity1.getA(), "abc");
    Assert.assertEquals((long) entity1.getB(), 123L);
    Assert.assertEquals(entity2.getA(), "XY");
    Assert.assertEquals((long) entity2.getB(), 456L);

    EasyMock.verify(request, model, descriptor, context, routingResult);
  }
}
