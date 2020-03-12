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
 * The role of the DarkClusterDispatcher is to determine if the request is safe to send, rewrite the request, find the right sending strategy,
 * and send it to the dark clusters via the strategy.
 */
public interface DarkClusterManager
{
  String HEADER_METHOD_OVERRIDE = "X-HTTP-Method-Override";

  /**
   * Send the request to the dark cluster. sendDarkRequest should ensure that the original request and requestContext are not modified.
   *
   * @param oldRequest real request
   * @param oldRequestContext requestContext
   * @return true if request is sent at least once.
   */
  boolean sendDarkRequest(final RestRequest oldRequest, final RequestContext oldRequestContext);
}
