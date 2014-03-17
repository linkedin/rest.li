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
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiRequestData;

import java.net.URI;
import java.util.Map;


public interface FilterRequestContext
{
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
   * get the projection mask parsed from the query.
   *
   * @return MaskTree parsed from query, or null if no projection mask was requested.
   */
  MaskTree getProjectionMask();

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
   */
  String getResourceName();

  /**
   * Get the namespace of the target resource.
   *
   * @return Namespace of the resource.
   */
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
}
