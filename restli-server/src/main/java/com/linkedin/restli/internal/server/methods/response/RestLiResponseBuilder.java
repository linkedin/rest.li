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

package com.linkedin.restli.internal.server.methods.response;


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RoutingResult;

import java.util.Map;


/**
 * {@link RestLiResponseBuilder} returns a {@link PartialRestResponse} so that rest.li can fill in
 * other response data and metadata (headers, links, field projection, etc).
 *
 * @author dellamag
 */
public interface RestLiResponseBuilder
{
  PartialRestResponse buildResponse(RoutingResult routingResult,
                                    RestLiResponseEnvelope responseData);

  RestLiResponseEnvelope buildRestLiResponseData(RestRequest request,
                                                 RoutingResult routingResult,
                                                 Object result,
                                                 Map<String, String> headers);
}
