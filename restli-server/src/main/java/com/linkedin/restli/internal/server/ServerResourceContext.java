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

/**
 * $Id: $
 */

package com.linkedin.restli.internal.server;


import com.linkedin.data.DataMap;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.URI;
import java.util.Map;

/**
 * Richer resource context used inside server framework
 *
 * @author Josh Walker
 * @version $Revision: $
 */

public interface ServerResourceContext extends ResourceContext
{
  /**
   * @return {@link DataMap} of request parameters.
   */
  DataMap getParameters();

  /**
   * @return request {@link URI}
   */
  URI getRequestURI();

  /**
   * @return rest.li request action name
   */
  String getRequestActionName();

  /**
   * @return rest.li request finder name
   */
  String getRequestFinderName();

  @Override
  MutablePathKeys getPathKeys();

  /**
   * @return response headers
   */
  Map<String, String> getResponseHeaders();

  /**
   * @return map of {@link RestLiServiceException}s keyed by batch key values
   */
  Map<Object, RestLiServiceException> getBatchKeyErrors();

  /**
   * @return rest.li request method
   */
  String getRestLiRequestMethod();

  /**
   * @return Rest.li protocol version used by the client sending the request
   */
  ProtocolVersion getRestliProtocolVersion();

  /**
   * Set the MIME type that that has been chosen as the response MIME type.
   * @param type Selected MIME type.
   */
  void setResponseMimeType(String type);

  /**
   * @return response MIME type.
   */
  String getResponseMimeType();
}
