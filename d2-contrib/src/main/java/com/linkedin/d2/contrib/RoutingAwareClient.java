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

package com.linkedin.d2.contrib;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.util.concurrent.Future;

/**
 * RoutingAwareClient will currently only support RestRequests with an additional key.
 *
 * @author David Hoa
 * @version $Revision: $
 */

public interface RoutingAwareClient
{
  /**
   * This function can be used to send a restRequest with an additional routeKey to determine
   * the appropriate d2 service to use for routing the request. An example routeKey might be
   * a memberID. It is up to the implementation to convert the routeKey into the correct
   * d2 service name. 
   *
   * @param request original rest request
   * @param callback used to return the new service name.
   * @param routeKey key used to determine a new service name.
   */
  void restRequest(RestRequest request, Callback<RestResponse> callback, String routeKey);

  /**
   * This function can be used to send a restRequest with an additional routeKey to determine
   * the appropriate d2 service to use for routing the request. An example routeKey might be
   * a memberID. It is up to the implementation to convert the routeKey into the correct
   * d2 service name. 
   *
   * @param request original rest request
   * @param requestContext request context
   * @param callback used to return the new service name.
   * @param routeKey key used to determine a new service name.
   */
  void restRequest(RestRequest request, RequestContext requestContext,
                   Callback<RestResponse> callback, String routeKey);

  /**
   * This function can be used to send a restRequest with an additional routeKey to determine
   * the appropriate d2 service to use for routing the request. An example routeKey might be
   * a memberID. It is up to the implementation to convert the routeKey into the correct
   * d2 service name. 
   *
   * The returned Future can be used to retrieve the RestResponse.
   *
   * @param request original rest request
   * @param routeKey key used to determine a new service name.
   * @return a future to wait for the response
   */
  Future<RestResponse> restRequest(RestRequest request, String routeKey);

  /**
   * This function can be used to send a restRequest with an additional routeKey to determine
   * the appropriate d2 service to use for routing the request. An example routeKey might be
   * a memberID. It is up to the implementation to convert the routeKey into the correct
   * d2 service name. 
   *
   * The returned Future can be used to retrieve the RestResponse.
   *
   * @param request original rest request
   * @param requestContext request context
   * @param routeKey key used to determine a new service name.
   * @return a future to wait for the response
   */
  Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext, String routeKey);
}
