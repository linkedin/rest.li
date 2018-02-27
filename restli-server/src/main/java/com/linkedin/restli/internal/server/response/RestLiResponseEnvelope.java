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

package com.linkedin.restli.internal.server.response;


import com.linkedin.data.DataMap;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * Abstract envelope for storing response data.
 *
 * @author nshankar
 * @author erli
 * @author gye
 *
 */
public abstract class RestLiResponseEnvelope
{
  // Only one of _status and _exception is non-null. Setting value to one of them sets the other to null.
  private HttpStatus _status;
  private RestLiServiceException _exception;

  // Custom metadata for the response.
  private final DataMap _responseMetadata = new DataMap();

  RestLiResponseEnvelope(HttpStatus status)
  {
    _status = status;
  }

  RestLiResponseEnvelope(RestLiServiceException exception)
  {
    _exception = exception;
  }

  /**
   * Gets the status of the response.
   *
   * @return the http status.
   */
  public HttpStatus getStatus()
  {
    return _exception != null ? _exception.getStatus() : _status;
  }

  void setStatus(HttpStatus status)
  {
    assert status != null;
    _status = status;
    _exception = null;
  }

  /**
   * Gets the RestLiServiceException associated with the response data when the data is an error response.
   *
   * @return the RestLiServiceException if one exists; else null.
   */
  public RestLiServiceException getException()
  {
    return _exception;
  }

  /**
   * Determines if the data corresponds to an error response.
   *
   * @return true if the response is an error response; else false.
   */
  public boolean isErrorResponse()
  {
    return _exception != null;
  }

  /**
   * Sets the RestLiServiceException to the envelope. This is intended for internal use only by {@link com.linkedin.restli.internal.server.filter.RestLiFilterChainIterator}
   * when handling exception from the filter implementation.
   * <p/>
   * DO NOT USE in filter implementation. {@link com.linkedin.restli.server.filter.Filter} should throw exception or
   * return a future that completes exceptionally in case of errorr.
   */
  public void setExceptionInternal(RestLiServiceException exception)
  {
    assert exception != null;
    _exception = exception;
    _status = null;
    clearData();
    _responseMetadata.clear();
  }

  /**
   * Map that will become the $metadata field of the response.
   *
   * @return DataMap containing the metadata values.
   */
  public final DataMap getResponseMetadata() {
    return _responseMetadata;
  }

  /**
   * Returns the {@link ResponseType}.
   *
   * @return {@link ResponseType}.
   */
  public abstract ResponseType getResponseType();

  /**
   * Returns the {@link ResourceMethod}.
   *
   * @return {@link ResourceMethod}.
   */
  public abstract ResourceMethod getResourceMethod();

  /**
   * Sets the data stored by this response envelope to null.
   */
  protected abstract void clearData();
}
