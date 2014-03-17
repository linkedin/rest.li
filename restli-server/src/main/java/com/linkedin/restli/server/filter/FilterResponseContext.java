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


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;

import java.util.Map;


/**
 * @author nshankar
 *
 */
public interface FilterResponseContext
{
  /**
   * Get the entity returned by the resource.
   *
   * @return Response Entity.
   */
  RecordTemplate getResponseEntity();

  /**
   * Set the response entity.
   *
   * @param entity
   *          Response entity.
   */
  void setResponseEntity(final RecordTemplate entity);

  /**
   * Obtain the HTTP status.
   *
   * @return HTTP Status.
   */
  HttpStatus getHttpStatus();

  /**
   * Set the HTTP status.
   *
   * @param status
   *          HTTP status.
   */
  void setHttpStatus(HttpStatus status);

  /**
   * Get a mutable map of response headers.
   *
   * @return Response headers.
   */
  Map<String, String> getResponseHeaders();
}
