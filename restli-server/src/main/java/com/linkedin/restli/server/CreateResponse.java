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

import com.linkedin.restli.common.HttpStatus;

/**
 * @author dellamag
 */
public class CreateResponse
{
  // _id should be set for success response. However, historically, we allow null value.
  private final Object _id;

  // _error must be set for an error response. Otherwise, it's consider a success response.
  private final RestLiServiceException _error;

  // _status should always be set from a success response or from an exception.
  private final HttpStatus _status;

  /**
   * Constructs a success response. The HTTP status defaults to 201.
   *
   * @param id the newly created resource id
   */
  public CreateResponse(final Object id)
  {
    _id = id;
    _status = HttpStatus.S_201_CREATED;
    _error = null;
  }

  /**
   * Constructs a success response.
   *
   * @param id the newly created resource id
   * @param status HTTP response status.
   *               Should not be an error status code; passing a status >= 400 will not appear as an exception
   *
   */
  public CreateResponse(final Object id, final HttpStatus status)
  {
    _id = id;
    _status = status;
    _error = null;
  }

  /**
   * Constructs a success response without an ID.
   *
   * @param status HTTP response status.
   *               Should not be an error status code; passing a status >= 400 will not appear as an exception
   */
  public CreateResponse(final HttpStatus status)
  {
    _id = null;
    _status = status;
    _error = null;
  }

  /**
   * Constructs a CreateResponse containing error details.
   *
   * @param error A rest.li exception containing the appropriate HTTP response status and error details.
   */
  public CreateResponse(RestLiServiceException error)
  {
    _id = null;
    _status = error.getStatus();
    _error = error;
  }

  /**
   * @return true if the response has id, false otherwise
   */
  public boolean hasId()
  {
    return _id != null;
  }

  public Object getId()
  {
    return _id;
  }

  public HttpStatus getStatus()
  {
    return _status;
  }

  /**
   * Checks if the response contains an error.
   *
   * @return true if the response contains an error.
   */
  public boolean hasError()
  {
    return _error != null;
  }

  /**
   * Gets the exception representing a create response error, if any.
   *
   * @return an exception containing the appropriate HTTP response status and error details,
   *         or null if response does not contain an error.
   */
  public RestLiServiceException getError()
  {
    return _error;
  }
}
