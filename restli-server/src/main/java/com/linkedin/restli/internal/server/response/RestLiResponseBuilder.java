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

package com.linkedin.restli.internal.server.response;


import com.linkedin.r2.message.Request;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.RestLiResponseData;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * A Rest.li response is built in three steps.
 * <ol>
 *   <li>Build a {@link RestLiResponseData} from the result object returned by the server application resource
 *   implementation. The <code>RestLiResponseData</code> object is then sent through the response filter chain.</li>
 *   <li>Build a {@link RestLiResponse} from the <code>RestLiResponseData</code></li> after it has been processed
 *   by the Rest.li filters.
 *   <li>Build a {@link com.linkedin.r2.message.rest.RestResponse} or {@link com.linkedin.r2.message.stream.StreamResponse}
 *   from the <code>RestLiResponse</code>.</li>
 * </ol>
 *
 * <code>RestLiResponseBuilder</code> is responsible for the first two steps and contains methods for each of them.
 *
 * @author dellamag
 */
public interface RestLiResponseBuilder<D extends RestLiResponseData<?>>
{
  /**
   * Executes {@linkplain RestLiResponseBuilder the second step} of building the response.
   */
  RestLiResponse buildResponse(RoutingResult routingResult,
                                    D responseData);

  /**
   * Executes {@linkplain RestLiResponseBuilder the first step} of building the response.
   *
   * @param result The result object returned from the respective Rest.li method implementation. See concrete implementation
   *               classes for the expect result object types.
   */
  D buildRestLiResponseData(Request request,
                            RoutingResult routingResult,
                            Object result,
                            Map<String, String> headers,
                            List<HttpCookie> cookies);
}
