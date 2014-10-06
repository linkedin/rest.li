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

import com.linkedin.data.ByteString;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceContext;
import org.easymock.EasyMock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Soojung Ha
 */
public class RestLiArgumentBuilderTestHelper
{
  public static RestRequest getMockRequest(boolean returnHeaders, String entity, int getEntityCount)
  {
    RestRequest mockRequest = EasyMock.createMock(RestRequest.class);
    if (returnHeaders)
    {
      EasyMock.expect(mockRequest.getHeaders()).andReturn(Collections.<String, String>emptyMap());
    }
    if (entity != null)
    {
      EasyMock.expect(mockRequest.getHeader("Content-Type")).andReturn("application/json");
      EasyMock.expect(mockRequest.getEntity()).andReturn(ByteString.copy(entity.getBytes())).times(getEntityCount);
    }
    EasyMock.replay(mockRequest);
    return mockRequest;
  }

  public static RestRequest getMockRequest(String entity, ProtocolVersion version)
  {
    RestRequest mockRequest = EasyMock.createMock(RestRequest.class);
    Map<String, String> headers = new HashMap<String, String>();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());
    EasyMock.expect(mockRequest.getHeaders()).andReturn(headers);
    if (entity != null)
    {
      EasyMock.expect(mockRequest.getHeader("Content-Type")).andReturn("application/json");
      EasyMock.expect(mockRequest.getEntity()).andReturn(ByteString.copy(entity.getBytes()));
    }
    EasyMock.replay(mockRequest);
    return mockRequest;
  }

  public static ResourceModel getMockResourceModel(Class<? extends RecordTemplate> valueClass, Key key, boolean returnNullKey)
  {
    ResourceModel model = EasyMock.createMock(ResourceModel.class);
    if (valueClass != null)
    {
      EasyMock.expect((Class) model.getValueClass()).andReturn(valueClass);
    }
    if (key != null || returnNullKey)
    {
      EasyMock.expect(model.getPrimaryKey()).andReturn(key);
    }
    if (key != null)
    {
      EasyMock.expect(model.getKeyName()).andReturn(key.getName());
    }
    EasyMock.replay(model);
    return model;
  }

  public static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, Parameter<?> param)
  {
    List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
    if (param != null)
    {
      paramList.add(param);
    }
    return getMockResourceMethodDescriptor(model, 1, paramList);
  }

  public static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, int getResourceModelCount, List<Parameter<?>> paramList)
  {
    ResourceMethodDescriptor descriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    if (model != null)
    {
      EasyMock.expect(descriptor.getResourceModel()).andReturn(model).times(getResourceModelCount);
    }
    EasyMock.expect(descriptor.getParameters()).andReturn(paramList);
    EasyMock.replay(descriptor);
    return descriptor;
  }

  public static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, List<Parameter<?>> paramList, String actionName, RecordDataSchema dataSchema)
  {
    ResourceMethodDescriptor descriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    if (model != null)
    {
      EasyMock.expect(descriptor.getResourceModel()).andReturn(model);
    }
    EasyMock.expect(descriptor.getRequestDataSchema()).andReturn(dataSchema);
    if (actionName != null)
    {
      EasyMock.expect(descriptor.getActionName()).andReturn(actionName);
    }
    if (paramList != null)
    {
      EasyMock.expect(descriptor.getParameters()).andReturn(paramList);
    }
    EasyMock.replay(descriptor);
    return descriptor;
  }

  public static ResourceContext getMockResourceContext(String keyName, Object keyValue, Set<Object> batchKeys)
  {
    ResourceContext context = EasyMock.createMock(ResourceContext.class);
    if (keyName != null || batchKeys != null)
    {
      PathKeysImpl pathKeys = new PathKeysImpl();
      if (keyName != null)
      {
        pathKeys.append(keyName, keyValue);
      }
      if (batchKeys != null)
      {
        pathKeys.setBatchKeys(batchKeys);
      }
      EasyMock.expect(context.getPathKeys()).andReturn(pathKeys);
    }
    EasyMock.replay(context);
    return context;
  }

  public static ResourceContext getMockResourceContext(PathKeys pathKeys, boolean returnStructuredParameter)
  {
    ResourceContext context = EasyMock.createMock(ResourceContext.class);
    if (pathKeys != null)
    {
      EasyMock.expect(context.getPathKeys()).andReturn(pathKeys);
    }
    if (returnStructuredParameter)
    {
      EasyMock.expect(context.getStructuredParameter("")).andReturn(null);
    }
    EasyMock.replay(context);
    return context;
  }

  public static ResourceContext getMockResourceContext(Map<String, String> parameters)
  {
    ResourceContext context = EasyMock.createMock(ResourceContext.class);
    for (String key : parameters.keySet())
    {
      EasyMock.expect(context.getParameter(key)).andReturn(parameters.get(key));
    }
    EasyMock.replay(context);
    return context;
  }

  public static RoutingResult getMockRoutingResult(ResourceMethodDescriptor descriptor, int getResourceMethodCount,
                                                   ResourceContext context, int getContextCount)
  {
    RoutingResult mockRoutingResult = EasyMock.createMock(RoutingResult.class);
    EasyMock.expect(mockRoutingResult.getResourceMethod()).andReturn(descriptor).times(getResourceMethodCount);
    if (context != null)
    {
      EasyMock.expect(mockRoutingResult.getContext()).andReturn(context).times(getContextCount);
    }
    EasyMock.replay(mockRoutingResult);
    return mockRoutingResult;
  }
}