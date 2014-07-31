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

/* $Id$ */
package com.linkedin.r2.transport.common.bridge.client;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;

import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface TransportClient
{
  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received.
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @param wireAttrs attributes that should be sent over the wire to the server
   * @param callback the callback to invoke with the response
   */
  void restRequest(RestRequest request,
                   RequestContext requestContext,
                   Map<String, String> wireAttrs,
                   TransportCallback<RestResponse> callback);

  /**
   * Starts asynchronous shutdown of the client. This method should block minimally, if at all.
   *
   * @param callback a callback that will be invoked once the shutdown is complete
   */
  void shutdown(Callback<None> callback);
}
