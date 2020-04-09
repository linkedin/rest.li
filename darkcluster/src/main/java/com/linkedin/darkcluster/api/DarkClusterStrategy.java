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

/**
 * DarkClusterStrategy controls if a request should be duplicated to the dark canary clusters, and how, if any, traffic shaping should take place.
 * Implementations should be threadsafe, as there will be concurrent access.
 */
public interface DarkClusterStrategy
{
  /**
   * Send request to dark canary according to strategy. This may include not sending the request, or sending it multiple times.
   * @param originalRequest incoming request
   * @param darkRequest dark request to send
   * @param requestContext requestContext for the dark request. The requestContext should be duplicated for each dark request sent.
   * @return  true if at least one request was sent.
   */
  boolean handleRequest(final RestRequest originalRequest, final RestRequest darkRequest, final RequestContext requestContext);
}
