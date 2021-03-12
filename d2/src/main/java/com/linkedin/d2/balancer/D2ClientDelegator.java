/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.balancer;

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
 * Abstract class implementing the delegating methods for D2Client
 */
public abstract class D2ClientDelegator implements D2Client
{
  protected D2Client _d2Client;

  public D2ClientDelegator(D2Client d2Client)
  {
    _d2Client = d2Client;
  }

  @Override
  public Facilities getFacilities()
  {
    return _d2Client.getFacilities();
  }

  @Override
  public void start(Callback<None> callback)
  {
    _d2Client.start(callback);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _d2Client.shutdown(callback);
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request)
  {
    return _d2Client.restRequest(request);
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext)
  {
    return _d2Client.restRequest(request, requestContext);
  }

  @Override
  public void restRequest(RestRequest request, Callback<RestResponse> callback)
  {
    _d2Client.restRequest(request, callback);
  }

  @Override
  public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    _d2Client.restRequest(request, requestContext, callback);
  }

  @Override
  public void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
  {
    _d2Client.streamRequest(request, callback);
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    _d2Client.streamRequest(request, requestContext, callback);
  }

  @Override
  public void restRequestStreamResponse(RestRequest request, Callback<StreamResponse> callback) {
    _d2Client.restRequestStreamResponse(request, callback);
  }

  @Override
  public void restRequestStreamResponse(RestRequest request, RequestContext requestContext,
      Callback<StreamResponse> callback) {
    _d2Client.restRequestStreamResponse(request, requestContext, callback);
  }

  @Override
  public void getMetadata(URI uri, Callback<Map<String, Object>> callback)
  {
    _d2Client.getMetadata(uri, callback);
  }
}