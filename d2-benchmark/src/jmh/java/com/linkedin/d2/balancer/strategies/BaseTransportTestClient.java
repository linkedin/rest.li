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

package com.linkedin.d2.balancer.strategies;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import java.util.Map;


/**
 * Create a simple mock of transport client
 * This transport client will directly return without any latency or error.
 */
public class BaseTransportTestClient implements TransportClient {
  BaseTransportTestClient() {
  }

  @Override
  public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
      TransportCallback<RestResponse> callback) {
    RestResponseBuilder restResponseBuilder = new RestResponseBuilder().setEntity(request.getURI().getRawPath().getBytes());
    callback.onResponse(TransportResponseImpl.success(restResponseBuilder.build()));
  }

  @Override
  public void shutdown(Callback<None> callback) {
    callback.onSuccess(None.none());
  }
}
