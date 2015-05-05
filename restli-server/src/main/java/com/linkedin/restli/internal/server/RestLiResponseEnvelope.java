/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.internal.server;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.Map;
import java.util.TreeMap;


/**
 * Concrete implementation of {@link RestLiResponseData}.
 *
 * @author nshankar
 * @author erli
 *
 */
public abstract class RestLiResponseEnvelope implements RestLiResponseData
{
  /* Overview of variable invariant:
   *
   * _status is reserved for the status of a response without a thrown exception.
   * _exception contains its own status that should be used whenever an exception
   * is thrown.
   *
   * Because we only need one, only one of {status/exception} may be nonnull.
   *
   * Furthermore, subclasses extending this class should maintain the invariant
   * that there are generally two sets of variables, one for exception response
   * and another for regular response. If one group is set, another must be
   * set to null.
   */
  private HttpStatus _status;
  private RestLiServiceException _exception;
  private Map<String, String> _headers;

  // Private constructor used to instantiate all shared common objects used.
  private RestLiResponseEnvelope(Map<String, String> headers)
  {
    _headers = new TreeMap<String, String>(headers);
  }

  /**
   * Instantiates a top level response with no exceptions.
   *
   * @param httpStatus Status of the response.
   * @param headers of the response.
   */
  protected RestLiResponseEnvelope(HttpStatus httpStatus, Map<String, String> headers)
  {
    this(headers);
    setStatus(httpStatus);
  }

  /**
   * Instantiates a top level failed response with an exception.
   *
   * @param exception exception thrown.
   * @param headers of the response.
   */
  protected RestLiResponseEnvelope(RestLiServiceException exception, Map<String, String> headers)
  {
    this(headers);
    setException(exception);
  }

  @Override
  public boolean isErrorResponse()
  {
    return _exception != null;
  }

  @Override
  public RestLiServiceException getServiceException()
  {
    return _exception;
  }

  @Override
  public abstract ResponseType getResponseType();

  /**
   * Sets the top level exception of this response.
   * Each inheriting class must maintain invariant unique to its type.
   *
   * @param exception to set this response to.
   */
  protected void setException(RestLiServiceException exception)
  {
    if (exception == null)
    {
      throw new UnsupportedOperationException("Null is not permitted in setting an exception.");
    }
    _exception = exception;
    _status = null;
  }

  /**
   * Returns the top level status either from the response or from the exception.
   *
   * @return Top level status of the request.
   */
  @Override
  public HttpStatus getStatus()
  {
    return _exception != null ? _exception.getStatus() : _status;
  }

  /**
   * Sets the status of a response for when there are no exceptions.
   * Does not check if exception is already null, but will instead
   * null the exception to maintain the invariant that there
   * is only one source for getting the status.
   *
   * @param status status to set for this response.
   */
  protected void setStatus(HttpStatus status)
  {
    if (status == null)
    {
      throw new UnsupportedOperationException("Setting status to null is not permitted for when there are no exceptions.");
    }
    _status = status;
    _exception = null;

    _headers.remove(HeaderUtil.getErrorResponseHeaderName(_headers));
  }

  /**
   * Gets a mutable map of the headers of this response.
   *
   * @return a mutable map of string values that indicates the headers of this response.
   */
  public Map<String, String> getHeaders()
  {
    return _headers;
  }
}
