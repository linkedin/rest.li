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
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * @author Soojung Ha
 */
public class TestBatchCreateArgumentBuilder
{
  @Test
  public void testArgumentBuilderSuccess()
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, "{\"elements\":[{\"b\":123,\"a\":\"abc\"},{\"b\":5678,\"a\":\"xyzw\"}]}", 1);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, null, false);
    @SuppressWarnings("rawtypes")
    Parameter<BatchCreateRequest> param = new Parameter<BatchCreateRequest>("",
        BatchCreateRequest.class,
        null,
        false,
        null,
        Parameter.ParamType.BATCH,
        false,
        new AnnotationSet(new Annotation[]{}));
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, param);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, null);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);

    RestLiArgumentBuilder argumentBuilder = new BatchCreateArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);

    assertEquals(args.length, 1);
    assertTrue(args[0] instanceof BatchCreateRequest);
    @SuppressWarnings("unchecked")
    List<MyComplexKey> entities = ((BatchCreateRequest<Integer, MyComplexKey>)args[0]).getInput();
    assertEquals(entities.size(), 2);
    assertEquals(entities.get(0).getA(), "abc");
    assertEquals((long) entities.get(0).getB(), 123L);
    assertEquals(entities.get(1).getA(), "xyzw");
    assertEquals((long) entities.get(1).getB(), 5678L);

    verify(request, model, descriptor, context, routingResult);
  }

  @DataProvider
  private Object[][] failureData()
  {
    return new Object[][]
        {
            {"{\"elements\":{\"b\":123,\"a\":\"abc\"},{\"b\":5678,\"a\":\"xyzw\"}]}"},
            {"{\"elements\":1234}"},
            {"{\"elements\":"},
            {"{\"elements\":[{\"b\":123,\"a\":\"abc\"},{1234:5678,\"a\":\"xyzw\"}]}"}
        };
  }

  @Test(dataProvider = "failureData")
  public void testFailure(String entity)
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity, 1);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, null, false);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 1, null);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, null, 0);

    RestLiArgumentBuilder argumentBuilder = new BatchCreateArgumentBuilder();
    try
    {
      argumentBuilder.extractRequestData(routingResult, request);
      fail("Expected RestLiInternalException or ClassCastException");
    }
    catch (RestLiInternalException e)
    {
      assertTrue(e.getMessage().contains("JsonParseException"));
    }
    catch (ClassCastException e)
    {
      assertTrue(e.getMessage().contains("java.lang.Integer cannot be cast to com.linkedin.data.DataList"));
    }

    verify(request, model, descriptor, routingResult);
  }
}
