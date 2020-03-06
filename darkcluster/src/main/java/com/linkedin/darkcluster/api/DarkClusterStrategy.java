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

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;

/**
 * DarkClusterStrategy controls if a request should be duplicated to the dark canary clusters, and how, if any, traffic shaping should take place.
 */
public interface DarkClusterStrategy
{
  /**
   * Send request to dark canary according to strategy. This may include not sending the request, or sending it multiple times.
   * @param request incoming request
   * @param requestContext requestContext for the request
   * @return  true if handled successfully else false
   */
  boolean handleRequest(final RestRequest request, final RequestContext requestContext);

  int getNumDuplicateRequests(String darkClusterName, String originalClusterName, final DarkClusterConfig darkClusterConfig, final float randomNum);
}
