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


import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiResponseData;


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
  /**
   * Overview of response envelope invariant:
   *
   * In the {@link RestLiResponseData} that stores this envelope - if there is an exception, RestLiResponseEnvelope must
   * have its data set to null.
   * When RestLiResponseEnvelope's data is not null, there must be no error - thus, in the parent
   * {@link RestLiResponseData} exception must be null and status must be not null.
   *
   * As such, in any subclass envelope, setting data into the envelope must call the parent response data's setStatus()
   * method to ensure the above invariant is maintained.
   */

  protected RestLiResponseDataImpl _restLiResponseData;

  /**
   * Constructor.
   *
   * @param restLiResponseData the response data that stores this response envelope. We need this response data object
   *                           so we can change its Http status when data is set within the envelope. This ensures we
   *                           can maintain the above mentioned invariant.
   */
  protected RestLiResponseEnvelope(RestLiResponseDataImpl restLiResponseData)
  {
    _restLiResponseData = restLiResponseData;
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
