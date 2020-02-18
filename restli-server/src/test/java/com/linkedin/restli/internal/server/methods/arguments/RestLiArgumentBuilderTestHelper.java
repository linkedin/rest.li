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
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.MutablePathKeys;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.config.ResourceMethodConfig;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.easymock.EasyMock;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.mockito.Matchers.*;


/**
 * @author Soojung Ha
 */
// TODO : Use builder pattern for getMock* methods
public class RestLiArgumentBuilderTestHelper
{
  public static RestRequest getMockRequest(boolean returnHeaders, String entity)
  {
    RestRequest mockRequest = createMock(RestRequest.class);

    if (entity != null)
    {
      expect(mockRequest.getHeaders()).andReturn(
          Collections.singletonMap(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON));
      expect(mockRequest.getEntity()).andReturn(ByteString.copy(entity.getBytes())).anyTimes();
    }
    else if (returnHeaders)
    {
      expect(mockRequest.getHeaders()).andReturn(Collections.emptyMap());
    }

    replay(mockRequest);
    return mockRequest;
  }

  public static RestRequest getMockRequest(String entity, ProtocolVersion version)
  {
    RestRequest mockRequest = createMock(RestRequest.class);
    Map<String, String> headers = new HashMap<>();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());
    if (entity != null)
    {
      headers.put(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON);
      expect(mockRequest.getEntity()).andReturn(ByteString.copy(entity.getBytes()));
    }

    expect(mockRequest.getHeaders()).andReturn(headers).anyTimes();
    replay(mockRequest);
    return mockRequest;
  }

  static ResourceModel getMockResourceModel(Class<? extends RecordTemplate> valueClass, Key key,
      Key[] associationKeys, Set<Object> batchKeys)
  {
    ResourceModel model = createMock(ResourceModel.class);
    if (valueClass != null)
    {
      expect((Class) model.getValueClass()).andReturn(valueClass);
    }

    // This conditional block to set the mock expectations doesn't explicitly take care of Alternate Key types yet.
    if (key != null)
    {
      expect(model.getPrimaryKey()).andReturn(key).anyTimes();

      if (CompoundKey.class.equals(key.getType()))
      {
        Set<Key> assocKeys = new HashSet<>();
        Collections.addAll(assocKeys, associationKeys);
        expect(model.getKeys()).andReturn(assocKeys).anyTimes();
      }
      else if (ComplexResourceKey.class.equals(key.getType()))
      {
        if (batchKeys != null && batchKeys.size() > 0)
        {
          ComplexResourceKey<? extends RecordTemplate, ? extends RecordTemplate> complexKey =
              (ComplexResourceKey<? extends RecordTemplate, ? extends RecordTemplate>) batchKeys.toArray()[0];
          expect((Class) model.getKeyKeyClass()).andReturn(complexKey.getKey().getClass()).anyTimes();
          expect((Class) model.getKeyParamsClass()).andReturn(complexKey.getParams().getClass()).anyTimes();
        }
      }
    }
    replay(model);
    return model;
  }

  public static ResourceModel getMockResourceModel(Class<? extends RecordTemplate> valueClass, Key key, boolean returnNullKey)
  {
    ResourceModel model = createMock(ResourceModel.class);
    if (valueClass != null)
    {
      expect((Class) model.getValueClass()).andReturn(valueClass);
    }
    if (key != null || returnNullKey)
    {
      expect(model.getPrimaryKey()).andReturn(key);
    }
    if (key != null)
    {
      expect(model.getKeyName()).andReturn(key.getName());
    }
    replay(model);
    return model;
  }

  public static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, Parameter<?> param)
  {
    return getMockResourceMethodDescriptor(model, param, null);
  }

  public static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, Parameter<?> param, Method method)
  {
    List<Parameter<?>> paramList = new ArrayList<>();
    if (param != null)
    {
      paramList.add(param);
    }
    return getMockResourceMethodDescriptor(model, 1, paramList, method);
  }

  static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model)
  {
    ResourceMethodDescriptor descriptor = createMock(ResourceMethodDescriptor.class);
    if (model != null)
    {
      expect(descriptor.getResourceModel()).andReturn(model).anyTimes();
    }
    replay(descriptor);
    return descriptor;
  }

  public static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, int getResourceModelCount, List<Parameter<?>> paramList)
  {
    return getMockResourceMethodDescriptor(model, getResourceModelCount, paramList, null);
  }

  public static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, Integer getResourceModelCount, List<Parameter<?>> paramList, Method method)
  {
    ResourceMethodDescriptor descriptor = createMock(ResourceMethodDescriptor.class);
    if (model != null)
    {
      if (getResourceModelCount != null)
      {
        expect(descriptor.getResourceModel()).andReturn(model).times(getResourceModelCount);
      }
      else
      {
        expect(descriptor.getResourceModel()).andReturn(model).anyTimes();
      }
    }
    if (paramList != null)
    {
      expect(descriptor.getParameters()).andReturn(paramList);
    }
    if (method != null)
    {
      expect(descriptor.getMethod()).andReturn(method);
    }
    replay(descriptor);
    return descriptor;
  }

  public static ResourceMethodDescriptor getMockResourceMethodDescriptor(ResourceModel model, List<Parameter<?>> paramList, String actionName, RecordDataSchema dataSchema)
  {
    ResourceMethodDescriptor descriptor = createMock(ResourceMethodDescriptor.class);
    if (model != null)
    {
      expect(descriptor.getResourceModel()).andReturn(model);
    }
    expect(descriptor.getRequestDataSchema()).andReturn(dataSchema);
    if (actionName != null)
    {
      expect(descriptor.getActionName()).andReturn(actionName);
    }
    if (paramList != null)
    {
      expect(descriptor.getParameters()).andReturn(paramList);
    }
    replay(descriptor);
    return descriptor;
  }

  public static ResourceContext getMockResourceContext()
  {
    ResourceContext context = createMock(ResourceContext.class);
    PathKeysImpl pathKeys = new PathKeysImpl();
    expect(context.getPathKeys()).andReturn(pathKeys);
    replay(context);
    return context;
  }

  static ServerResourceContext getMockResourceContext(Set<Object> batchKeys, ProtocolVersion version, boolean attachmentReaderGetExpected,
      boolean hasAlternateKeyParam)
  {
    ServerResourceContext context = createMock(ServerResourceContext.class);
    expect(context.getRestliProtocolVersion()).andReturn(version).anyTimes();

    if (batchKeys != null)
    {
      PathKeysImpl pathKeys = new PathKeysImpl();
      if (batchKeys != null)
      {
        pathKeys.setBatchKeys(batchKeys);
      }
      expect(context.getPathKeys()).andReturn(pathKeys).anyTimes();
    }

    if (attachmentReaderGetExpected)
    {
      expect(context.getRequestAttachmentReader()).andReturn(null);
    }

    expect(context.getParameter(RestConstants.ALT_KEY_PARAM)).andReturn(hasAlternateKeyParam ? "" : null).anyTimes();
    replay(context);
    return context;
  }

  public static ServerResourceContext getMockResourceContext(String keyName, Object keyValue, Set<Object> batchKeys,
                                                             boolean attachmentReaderGetExpected)
  {
    return getMockResourceContext(keyName, keyValue, batchKeys, null, attachmentReaderGetExpected);
  }

  public static ServerResourceContext getMockResourceContext(String keyName, Object keyValue, Set<Object> batchKeys,
                                                             Map<String, String> headers,
                                                             boolean attachmentReaderGetExpected)
  {
    ServerResourceContext context = createMock(ServerResourceContext.class);
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
      expect(context.getPathKeys()).andReturn(pathKeys);
    }
    if (headers != null)
    {
      expect(context.getRequestHeaders()).andReturn(headers);
    }
    if (attachmentReaderGetExpected)
    {
      expect(context.getRequestAttachmentReader()).andReturn(null);
    }
    replay(context);
    return context;
  }

  public static ServerResourceContext getMockResourceContext(MutablePathKeys pathKeys, boolean returnStructuredParameter,
                                                             boolean attachmentReaderGetExpected)
  {
    ServerResourceContext context = createMock(ServerResourceContext.class);
    if (pathKeys != null)
    {
      expect(context.getPathKeys()).andReturn(pathKeys);
    }
    if (returnStructuredParameter)
    {
      expect(context.getStructuredParameter("")).andReturn(null);
    }
    if (attachmentReaderGetExpected)
    {
      expect(context.getRequestAttachmentReader()).andReturn(null);
    }
    replay(context);
    return context;
  }

  public static ServerResourceContext getMockResourceContext(Map<String, String> parameters,
                                                             boolean attachmentReaderGetExpected)
  {
    ServerResourceContext context = createMock(ServerResourceContext.class);
    for (String key : parameters.keySet())
    {
      expect(context.hasParameter(key)).andReturn(true).anyTimes();
      expect(context.getParameter(key)).andReturn(parameters.get(key));
    }
    if (attachmentReaderGetExpected)
    {
      expect(context.getRequestAttachmentReader()).andReturn(null);
    }
    replay(context);
    return context;
  }

  public static ServerResourceContext getMockResourceContext(Map<String, String> parameters, MaskTree projectionMask,
                                                             MaskTree metadataMask, MaskTree pagingMask,
                                                             boolean attachmentReaderGetExpected)
  {
    ServerResourceContext context = createMock(ServerResourceContext.class);
    for (String key : parameters.keySet())
    {
      expect(context.hasParameter(key)).andReturn(true).anyTimes();
      expect(context.getParameter(key)).andReturn(parameters.get(key));
    }
    expect(context.getProjectionMask()).andReturn(projectionMask);
    expect(context.getMetadataProjectionMask()).andReturn(metadataMask);
    expect(context.getPagingProjectionMask()).andReturn(pagingMask);
    if (attachmentReaderGetExpected)
    {
      expect(context.getRequestAttachmentReader()).andReturn(null);
    }
    replay(context);
    return context;
  }

  public static ServerResourceContext getMockResourceContext(String parameterKey, List<String> parameterValues,
                                                             boolean attachmentReaderGetExpected)
  {
    ServerResourceContext context = createMock(ServerResourceContext.class);
    expect(context.hasParameter(parameterKey)).andReturn(true);
    expect(context.getParameterValues(parameterKey)).andReturn(parameterValues);
    if (attachmentReaderGetExpected)
    {
      expect(context.getRequestAttachmentReader()).andReturn(null);
    }
    replay(context);
    return context;
  }

  public static ServerResourceContext getMockResourceContextWithStructuredParameter(String parameterKey, String parameterValue,
                                                                                    Object structuredParameter,
                                                                                    boolean attachmentReaderGetExpected)
  {
    ServerResourceContext context = createMock(ServerResourceContext.class);
    expect(context.hasParameter(parameterKey)).andReturn(true);
    expect(context.getStructuredParameter(parameterKey)).andReturn(structuredParameter);
    if (attachmentReaderGetExpected)
    {
      expect(context.getRequestAttachmentReader()).andReturn(null);
    }
    replay(context);
    return context;
  }

  public static RoutingResult getMockRoutingResult()
  {
    RoutingResult mockRoutingResult = createMock(RoutingResult.class);
    ResourceMethodConfig mockResourceMethodConfig = createMock(ResourceMethodConfig.class);
    expect(mockRoutingResult.getResourceMethodConfig()).andReturn(mockResourceMethodConfig).anyTimes();
    replay(mockRoutingResult, mockResourceMethodConfig);

    return mockRoutingResult;
  }

  public static RoutingResult getMockRoutingResult(ResourceMethodDescriptor descriptor, int getResourceMethodCount,
                                                   ServerResourceContext context, int getContextCount)
  {
    RoutingResult mockRoutingResult = createMock(RoutingResult.class);
    if (descriptor != null)
    {
      expect(mockRoutingResult.getResourceMethod()).andReturn(descriptor).times(getResourceMethodCount);
    }
    if (context != null && getContextCount > 0)
    {
      expect(mockRoutingResult.getContext()).andReturn(context).times(getContextCount);
    }
    ResourceMethodConfig mockResourceMethodConfig = createMock(ResourceMethodConfig.class);
    expect(mockRoutingResult.getResourceMethodConfig()).andReturn(mockResourceMethodConfig).anyTimes();
    replay(mockRoutingResult, mockResourceMethodConfig);
    return mockRoutingResult;
  }

  static RoutingResult getMockRoutingResult(ResourceMethodDescriptor descriptor,
      ServerResourceContext context)
  {
    RoutingResult mockRoutingResult = createMock(RoutingResult.class);
    if (descriptor != null)
    {
      expect(mockRoutingResult.getResourceMethod()).andReturn(descriptor).anyTimes();
    }
    if (context != null)
    {
      expect(mockRoutingResult.getContext()).andReturn(context).anyTimes();
    }
    ResourceMethodConfig mockResourceMethodConfig = createMock(ResourceMethodConfig.class);
    expect(mockResourceMethodConfig.shouldValidateResourceKeyParams()).andReturn(false).anyTimes();
    expect(mockRoutingResult.getResourceMethodConfig()).andReturn(mockResourceMethodConfig).anyTimes();
    replay(mockRoutingResult, mockResourceMethodConfig);

    return mockRoutingResult;
  }
}