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

package com.linkedin.restli.server.filter;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.CustomRequestContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiRequestData;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public interface FilterRequestContext extends CustomRequestContext
{
  Logger LOG = LoggerFactory.getLogger(FilterRequestContext.class);

  /**
   * Get the URI of the request.
   *
   * @return request {@link URI}
   */
  URI getRequestURI();

  /**
   * Get the RestLi Protocol version of the request.
   *
   * @return Rest.li protocol version used by the client sending the request
   */
  ProtocolVersion getRestliProtocolVersion();

  /**
   * get the PathKeys parsed from the URI path.
   *
   * @return PathKeys for this context.
   */
  PathKeys getPathKeys();

  /**
   * get the projection mask parsed from the query for root object entities.
   *
   * @return MaskTree parsed from query, or null if no projection mask was requested.
   */
  MaskTree getProjectionMask();

  /**
   * get the projection mask parsed from the query for CollectionResult metadata
   *
   * @return MaskTree parsed from query, or null if no projection mask was requested.
   */
  MaskTree getMetadataProjectionMask();

  /**
   * Sets the specified projection mask for root entity in the response. Setting the projection mask to {@code null}
   * implies all fields should be projected.
   *
   * @param projectionMask Projection mask to use for root entity
   */
  default void setProjectionMask(MaskTree projectionMask)
  {
    LOG.warn("This is a default no-op implementation. The specified projectionMask is ignored.");
  }

  /**
   * Get all query parameters from the request.
   *
   * @return {@link DataMap} of request query parameters.
   */
  DataMap getQueryParameters();

  /**
   * Get a mutable representation of all headers from the request.
   *
   * @return a mutable map of header name -> header value
   */
  Map<String, String> getRequestHeaders();

  /**
   * Get the projection mode to be applied to the response body.
   *
   * @return Projection mode for the response body.
   */
  ProjectionMode getProjectionMode();

  /**
   * Get the name of the target resource.
   *
   * @return Name of the resource.
   * @deprecated Use getResourceModel().getResourceName() instead.
   */
  @Deprecated
  String getResourceName();

  /**
   * Get the namespace of the target resource.
   *
   * @return Namespace of the resource.
   * @deprecated Use getResourceModel().getResourceNamespace() instead.
   */
  @Deprecated
  String getResourceNamespace();

  /**
   * Obtain the finder name associate with the resource.
   *
   * @return Method name if the method is a finder; else, null.
   */
  String getFinderName();

  /**
   * Obtain the name of the action associate with the resource.
   *
   * @return Method name if the method is an action; else, null.
   */
  String getActionName();

  /**
   * Get the type of operation, GET, CREATE, UPDATE, etc.
   *
   * @return Type of operation that's being invoked on the resource.
   */
  ResourceMethod getMethodType();

  /**
   * Get custom annotations defined on the resource.
   *
   * @return customer annotations defined on the resource.
   */
  DataMap getCustomAnnotations();

  /**
   * Get request data.
   *
   * @return Request data.
   */
  RestLiRequestData getRequestData();

  /**
   * Get a Map that can be used as a scratch-pad between filters.
   *
   * @return a scratch pad in the form of a Map.
   */
  Map<String, Object> getFilterScratchpad();

  /**
   * Obtain the {@link FilterResourceModel} corresponding to the resource.
   *
   * @return ResourceModel corresponding to the resource.
   */
  FilterResourceModel getFilterResourceModel();

  /**
   * Obtain the collection metadata schema of finders corresponding to the resource.
   *
   * @return Metadata schema for the resource.
   */
  RecordDataSchema getCollectionCustomMetadataSchema();

  /**
   * Obtain the schema of the action request object.
   *
   * @return record template of the request object.
   */
  RecordDataSchema getActionRequestSchema();

  /**
   * Obtain the schema of the action response object.
   *
   * @return record template of the response object.
   */
  RecordDataSchema getActionResponseSchema();

  /**
   * Get {@link Method} that's being invoked on the resource.
   *
   * @return {@link Method}
   */
  Method getMethod();

  /**
   * Return the attributes from R2 RequestContext.
   *
   * @see RequestContext#getLocalAttrs()
   * @return the attributes contained by RequestContext.
   */
  Map<String, Object> getRequestContextLocalAttrs();
}