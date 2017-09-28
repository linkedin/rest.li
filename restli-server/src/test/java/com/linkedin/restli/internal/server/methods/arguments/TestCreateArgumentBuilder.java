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


import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RoutingException;

import java.lang.annotation.Annotation;

import org.testng.annotations.Test;

import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Soojung Ha
 */
public class TestCreateArgumentBuilder
{
  @Test
  public void testArgumentBuilderSuccess()
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, "{\"a\":\"xyz\",\"b\":123}", 1);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, null, false);
    Parameter<MyComplexKey> param = new Parameter<>("",
        MyComplexKey.class,
        DataTemplateUtil.getSchema(MyComplexKey.class),
        false,
        null,
        Parameter.ParamType.POST,
        false,
        new AnnotationSet(new Annotation[]{}));
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, param);
    ServerResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, null, true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);

    RestLiArgumentBuilder argumentBuilder = new CreateArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args.length, 1);
    assertTrue(args[0] instanceof MyComplexKey);
    assertEquals(((MyComplexKey)args[0]).getA(), "xyz");
    assertEquals((long) ((MyComplexKey)args[0]).getB(), 123L);

    verify(request, model, descriptor, context, routingResult);
  }

  @Test(dataProvider = "failureEntityData", dataProviderClass = RestLiArgumentBuilderTestHelper.class)
  public void testFailure(String entity)
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity, 1);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, null, false);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 1, null);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, null, 0);

    RestLiArgumentBuilder argumentBuilder = new CreateArgumentBuilder();
    try
    {
      argumentBuilder.extractRequestData(routingResult, request);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertTrue(e.getMessage().contains("Error parsing entity body"));
    }

    verify(request, model, descriptor, routingResult);
  }
}
