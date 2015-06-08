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
package com.linkedin.r2.transport.common.bridge.server;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;

import java.util.Map;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 */
public interface TransportDispatcher
{
  /**
   * Dispatch a {@link com.linkedin.r2.message.rest.RestRequest}.
   *
   * @param req the {@link com.linkedin.r2.message.rest.RestRequest} to be dispatched.
   * @param wireAttrs the wire attributes of the request.
   * @param requestContext {@link com.linkedin.r2.message.RequestContext} context for the request
   * @param callback a {@link com.linkedin.r2.transport.common.bridge.common.TransportCallback} to be called with the {@link com.linkedin.r2.message.rest.RestResponse}.
   */
  void handleRestRequest(RestRequest req, Map<String, String> wireAttrs,
                           RequestContext requestContext, TransportCallback<RestResponse> callback);

  /**
   * Dispatch a {@link com.linkedin.r2.message.stream.StreamRequest}.
   *
   * Any implementation that wants to support streaming MUST override this method.
   *
   * @param req the {@link com.linkedin.r2.message.stream.StreamRequest} to be dispatched.
   * @param wireAttrs the wire attributes of the request.
   * @param requestContext {@link com.linkedin.r2.message.RequestContext} context for the request
   * @param callback a {@link com.linkedin.r2.transport.common.bridge.common.TransportCallback} to be called with the {@link com.linkedin.r2.message.stream.StreamResponse}.
   */
   default void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs,
                         RequestContext requestContext, TransportCallback<StreamResponse> callback)
   {
     throw new UnsupportedOperationException("Please use an implementation that supports streaming.");
   }
}
