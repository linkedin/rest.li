/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster.api;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

public interface DarkClusterVerifier
{
  /**
   * Invoked when request has been forwarded to dark canary server(s) and response or error from real server arrives
   * @param request original request
   * @param response the response or error from real server
   */
  void onResponse(RestRequest request, Response response);

  /**
   * Invoked when response or error from dark canary server arrives.
   * This could be invoked multiple times for the same request if the request is forwarded
   * to multiple dark canary servers.
   * @param request original request
   * @param darkResponse dark canary response
   */
  void onDarkResponse(RestRequest request, DarkResponse darkResponse);

  /**
   * An object that represents the union of response or error.
   */
  interface Response
  {
    /**
     * Returns {@code true} if this response has an error. Use {@link #getError()} to get the error.
     *
     * @return {@code true} if this response has an error.
     */
    boolean hasError();

    /**
     * Returns the underlying value for this response. If this response has an error then this method
     * will return {@code null}.
     *
     * @return the value for this response or {@code null} if this response has an error.
     */
    RestResponse getResponse();

    /**
     * If this response has an error, this method returns the error. Otherwise {@code null} is
     * returned.
     *
     * @return the response for this error or {@code null} if there is no error.
     */
    Throwable getError();
  }

  interface DarkResponse extends Response
  {
    /**
     * Returns the property of the dark canary that generated the response/error
     * @return the property of the dark canary that generated the response/error
     */
    //DarkCanaryProperty getDarkCanaryProperty();
  }
}
