/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.restli.server;

import java.util.List;
import java.util.Map;

import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.rest.RestRequest;

/**
 * Captures nested/scoped resource context.
 *
 * @author dellamag
 */
public interface ResourceContext
{
  /**
   * get the RestRequest which caused the current context to be created.
   *
   * @return RestRequest for the current context
   */
  RestRequest getRawRequest();

  /**
   * get the HTTP request method for the current context.
   *
   * @return String representation of HTTP request method, per RFC 2616
   */
  String getRequestMethod();

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
   * check whether a given parameter was present in the request.
   *
   * @param key - the name of the parameter
   * @return true if the request contains the specified parameter
   */
  boolean hasParameter(String key);

  /**
   * get the value of a given parameter from the request. If multiple values were
   * specified in the request, only the first will be returned.
   *
   * @param key - the name of the parameter
   * @return value of the parameter from the request, or null if the parameter was not
   *         present in the request
   */
  String getParameter(String key);

  /**
   * Get the value a given parameter from the request as Object. Supports structured
   * DataTemplate-backed parameters.
   *
   * @param key - the name of the parameter
   * @return value of the parameter from the request, or null if the parameter was not
   *         present in the request
   */
  Object getStructuredParameter(String key);

  /**
   * get all values for a given parameter from the request.
   *
   * @param key - the name of the parameter
   * @return list of values for the parameter in the request, or null if the parameter was
   *         not present in the request
   */
  List<String> getParameterValues(String key);

  /**
   * get all headers from the request.
   *
   * @return a map of header name -> header value
   */
  Map<String, String> getRequestHeaders();

  /**
   * set a header to be sent in the response message.
   *
   * @param name - the name of the header
   * @param value - the value of the header
   */
  void setResponseHeader(String name, String value);
}
