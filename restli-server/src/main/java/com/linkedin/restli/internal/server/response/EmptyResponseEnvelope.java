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


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * Response for {@link com.linkedin.restli.internal.server.ResponseType#STATUS_ONLY}.
 *
 * @author erli
 */
public abstract class EmptyResponseEnvelope extends RestLiResponseEnvelope
{
  EmptyResponseEnvelope(HttpStatus status)
  {
    super(status);
  }

  EmptyResponseEnvelope(RestLiServiceException exception)
  {
    super(exception);
  }

  /**
   * Since there is no data, the {@link RestLiResponseEnvelope} invariant is maintained by default
   * Users can simply change the status using this method without need to set data.
   *
   * @param httpStatus
   */
  public void setStatus(HttpStatus httpStatus)
  {
    super.setStatus(httpStatus);
  }

  @Override
  protected void clearData()
  {
    // no data to clear, need to override due to extending abstract class.
  }

  /**
   * Returns the {@link ResponseType}.
   *
   * @return {@link ResponseType}.
   */
  @Override
  public final ResponseType getResponseType()
  {
    return ResponseType.STATUS_ONLY;
  }
}