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

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

/**
 * The role of the DarkClusterDispatcher is to rewrite the request, send it, and provide APIs to verify the response.
 */
public interface DarkClusterManager
{
  String HEADER_METHOD_OVERRIDE = "X-HTTP-Method-Override";

  /**
   * Send the request to the dark cluster. As part of the contract, sendDarkRequest will duplicate the oldRequest as a part of rewriting
   * the request to the dark cluster, and the old request context will also be duplicated to not affect the old request. All implementations
   * should obey this.
   * @param oldRequest real request that needs to be duplicated
   * @param oldRequestContext requestContext that needs to be duplicated
   * @return true if request is sent at least once.
   */
  boolean sendDarkRequest(final RestRequest oldRequest, final RequestContext oldRequestContext);

  boolean hasVerifier();

  void verifyResponse(RestRequest originalRequest, RestResponse originalResponse);

  void verifyError(RestRequest originalRequest, Throwable originalError);
}
