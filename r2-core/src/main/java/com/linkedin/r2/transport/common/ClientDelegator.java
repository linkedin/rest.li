/*
   Copyright (c) 2018 LinkedIn Corp.

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
package com.linkedin.r2.transport.common;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;


/**
 * This class offers delegating ability to {@link Client}
 */
public class ClientDelegator implements Client
{
  private Client _client;

  public ClientDelegator(Client client)
  {
    _client = client;
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request)
  {
    return _client.restRequest(request);
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext)
  {
    return _client.restRequest(request, requestContext);
  }

  @Override
  public void restRequest(RestRequest request, Callback<RestResponse> callback)
  {
    _client.restRequest(request, callback);
  }

  @Override
  public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    _client.restRequest(request, requestContext, callback);
  }

  @Override
  public void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
  {
    _client.streamRequest(request, callback);
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    _client.streamRequest(request, requestContext, callback);
  }

  @Override
  public void restRequestStreamResponse(RestRequest request, Callback<StreamResponse> callback) {
    _client.restRequestStreamResponse(request, callback);
  }

  @Override
  public void restRequestStreamResponse(RestRequest request, RequestContext requestContext,
      Callback<StreamResponse> callback) {
    _client.restRequestStreamResponse(request, requestContext, callback);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _client.shutdown(callback);
  }

  @Override
  public void getMetadata(URI uri, Callback<Map<String, Object>> callback)
  {
    _client.getMetadata(uri, callback);
  }
}
