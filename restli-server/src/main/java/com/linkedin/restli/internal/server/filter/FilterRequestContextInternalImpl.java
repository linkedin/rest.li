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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.filter.FilterResourceModel;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * @author nshankar
 */
@SuppressWarnings("deprecation")
// TODO: Change to implementing FilterRequestContext and rename to FilterRequestContextImpl
public class FilterRequestContextInternalImpl implements FilterRequestContextInternal
{
  private RestLiRequestData _requestData;
  private final ServerResourceContext _context;
  private final ResourceMethodDescriptor _resourceMethod;
  private final Map<String, Object> _scratchPad;
  private final FilterResourceModel _resourceModel;

  // Collection specific
  private final RecordDataSchema _collectionCustomTypeSchema;

  /**
   * @deprecated Use {@link #FilterRequestContextInternalImpl(ServerResourceContext, ResourceMethodDescriptor, RestLiRequestData)}
   *             and pass in RestLiRequestData.
   */
  @Deprecated
  // TODO: Remove this constructor once external use are removed.
  public FilterRequestContextInternalImpl(final ServerResourceContext context,
      final ResourceMethodDescriptor resourceMethod)
  {
    this(context, resourceMethod, null);
  }

  public FilterRequestContextInternalImpl(final ServerResourceContext context,
      final ResourceMethodDescriptor resourceMethod,
      final RestLiRequestData requestData)
  {
    _context = context;
    _resourceMethod = resourceMethod;
    _requestData = requestData;
    _scratchPad = new HashMap<>();
    _resourceModel = new FilterResourceModelImpl(resourceMethod.getResourceModel());
    _collectionCustomTypeSchema = resourceMethod.getCollectionCustomMetadataType() == null ? null : (RecordDataSchema) DataTemplateUtil.getSchema(resourceMethod.getCollectionCustomMetadataType());
  }

  @Override
  public String getResourceName()
  {
    return getFilterResourceModel().getResourceName();
  }

  @Override
  public ProjectionMode getProjectionMode()
  {
    return _context.getProjectionMode();
  }

  @Override
  public MaskTree getProjectionMask()
  {
    return _context.getProjectionMask();
  }

  @Override
  public MaskTree getMetadataProjectionMask()
  {
    return _context.getMetadataProjectionMask();
  }

  @Override
  public MaskTree getPagingProjectionMask()
  {
    return _context.getPagingProjectionMask();
  }

  @Override
  public void setProjectionMask(MaskTree projectionMask)
  {
    _context.setProjectionMask(projectionMask);
  }

  @Override
  public void setMetadataProjectionMask(MaskTree metadataProjectionMask)
  {
    _context.setMetadataProjectionMask(metadataProjectionMask);
  }

  @Override
  public void setPagingProjectionMask(MaskTree pagingProjectionMask)
  {
    _context.setPagingProjectionMask(pagingProjectionMask);
  }

  @Override
  public PathKeys getPathKeys()
  {
    return _context.getPathKeys();
  }

  @Override
  public String getResourceNamespace()
  {
    return getFilterResourceModel().getResourceNamespace();
  }

  @Override
  public ResourceMethod getMethodType()
  {
    return _resourceMethod.getMethodType();
  }

  @Override
  public Map<String, String> getRequestHeaders()
  {
    return _context.getRequestHeaders();
  }

  @Override
  public DataMap getCustomAnnotations()
  {
    return _resourceMethod.getCustomAnnotationData();
  }

  @Override
  public URI getRequestURI()
  {
    return _context.getRequestURI();
  }

  @Override
  public ProtocolVersion getRestliProtocolVersion()
  {
    return _context.getRestliProtocolVersion();
  }

  @Override
  public DataMap getQueryParameters()
  {
    return _context.getParameters();
  }

  @Override
  public RestLiRequestData getRequestData()
  {
    return _requestData;
  }

  @Override
  @Deprecated
  public void setRequestData(RestLiRequestData data)
  {
    _requestData = data;
  }

  @Override
  public Map<String, Object> getFilterScratchpad()
  {
    return _scratchPad;
  }

  @Override
  public String getFinderName()
  {
    return _resourceMethod.getFinderName();
  }

  @Override
  public String getActionName()
  {
    return _resourceMethod.getActionName();
  }

  @Override
  public FilterResourceModel getFilterResourceModel()
  {
    return _resourceModel;
  }

  @Override
  public RecordDataSchema getCollectionCustomMetadataSchema()
  {
    return _collectionCustomTypeSchema;
  }

  @Override
  public RecordDataSchema getActionRequestSchema()
  {
    return _resourceMethod.getRequestDataSchema();
  }

  @Override
  public RecordDataSchema getActionResponseSchema()
  {
    return _resourceMethod.getActionReturnRecordDataSchema();
  }

  @Override
  public Method getMethod()
  {
    return _resourceMethod.getMethod();
  }

  @Override
  public Map<String, Object> getRequestContextLocalAttrs()
  {
    return Collections.unmodifiableMap(_context.getRawRequestContext().getLocalAttrs());
  }

  @Override
  public Optional<Object> getCustomContextData(String key)
  {
    return _context.getCustomContextData(key);
  }

  @Override
  public void putCustomContextData(String key, Object data)
  {
    _context.putCustomContextData(key, data);
  }

  @Override
  public Optional<Object> removeCustomContextData(String key)
  {
    return _context.removeCustomContextData(key);
  }
}