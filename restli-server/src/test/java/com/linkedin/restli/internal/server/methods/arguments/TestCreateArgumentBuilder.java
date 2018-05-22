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

import com.linkedin.common.callback.Callback;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.UnstructuredDataReactiveReader;
import com.linkedin.restli.server.resources.CollectionResourceAsyncTemplate;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataAssociationResourceReactive;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Soojung Ha
 */
public class TestCreateArgumentBuilder
{
  @Test
  public void testArgumentBuilderSuccess() throws IOException, NoSuchMethodException
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, "{\"a\":\"xyz\",\"b\":123}");
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, null, false);
    Parameter<MyComplexKey> param = new Parameter<>("",
        MyComplexKey.class,
        DataTemplateUtil.getSchema(MyComplexKey.class),
        false,
        null,
        Parameter.ParamType.POST,
        false,
        new AnnotationSet(new Annotation[]{}));
    ResourceMethodDescriptor descriptor = getMockResourceMethodDescriptor(model, 1, param, false);
    ServerResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, null, true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 3, context, 1);

    RestLiArgumentBuilder argumentBuilder = new CreateArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult,
        DataMapUtils.readMapWithExceptions(request));
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args.length, 1);
    assertTrue(args[0] instanceof MyComplexKey);
    assertEquals(((MyComplexKey)args[0]).getA(), "xyz");
    assertEquals((long) ((MyComplexKey)args[0]).getB(), 123L);

    verify(request, model, descriptor, context, routingResult);
  }

  @Test
  public void testUnstructuredDataArgumentBuilder() throws IOException, NoSuchMethodException
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, "{}");
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(MyComplexKey.class, null, false);

    ResourceMethodDescriptor descriptor = getMockResourceMethodDescriptor(model, 2, null, true);
    ServerResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, null, true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);
    RestLiArgumentBuilder argumentBuilder = new CreateArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult,
        DataMapUtils.readMapWithExceptions(request));
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args.length, 0);
    assertEquals(requestData.hasEntity(), false);
  }

  private ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, int getResourceModelCount, Parameter<?> param, boolean isUnstructuredDataTemplate)
      throws NoSuchMethodException
  {
    List<Parameter<?>> paramList = new ArrayList<>();
    if (param != null)
    {
      paramList.add(param);
    }

    ResourceMethodDescriptor descriptor = createMock(ResourceMethodDescriptor.class);
    if (model != null)
    {
      expect(descriptor.getResourceModel()).andReturn(model).times(getResourceModelCount);
    }
    if (paramList != null)
    {
      expect(descriptor.getParameters()).andReturn(paramList);
    }
    if (isUnstructuredDataTemplate)
    {
      expect(descriptor.getMethod()).andReturn(
          UnstructuredDataAssociationResourceReactive.class.getMethod("create", UnstructuredDataReactiveReader.class,
              Callback.class));
    }
    else
    {
      expect(descriptor.getMethod()).andReturn(
          CollectionResourceAsyncTemplate.class.getMethod("create", RecordTemplate.class, Callback.class));
    }
    replay(descriptor);
    return descriptor;
  }
}
