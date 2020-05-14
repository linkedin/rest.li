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

package com.linkedin.d2.balancer.clients;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class TestClient implements TransportClient
{
  public static final int DEFAULT_REQUEST_TIMEOUT = 500;
  public StreamRequest streamRequest;
  public RestRequest restRequest;
  public RequestContext restRequestContext;
  public Map<String, String> restWireAttrs;
  public TransportCallback<StreamResponse> streamCallback;
  public TransportCallback<RestResponse> restCallback;
  public ScheduledExecutorService _scheduler;

  public boolean shutdownCalled;
  private final boolean _emptyResponse;
  private boolean _deferCallback;
  private int _minRequestTimeout;

  public TestClient()
  {
    this(true);
  }

  public TestClient(boolean emptyResponse)
  {
    this(emptyResponse, false, DEFAULT_REQUEST_TIMEOUT);
  }

  public TestClient(boolean emptyResponse, boolean deferCallback, int minRequestTimeout)
  {
    this(emptyResponse, deferCallback, minRequestTimeout, Executors.newSingleThreadScheduledExecutor());
  }

  public TestClient(boolean emptyResponse, boolean deferCallback, int minRequestTimeout, ScheduledExecutorService scheduler)
  {
    _emptyResponse = emptyResponse;
    _deferCallback = deferCallback;

    // this parameter is important to respect the contract between R2 and D2 to never have a connection shorter than
    // the request timeout to not affect the D2 load balancing/degrading
    _minRequestTimeout = minRequestTimeout;
    _scheduler = scheduler;
  }

  @Override
  public void restRequest(RestRequest request,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      TransportCallback<RestResponse> callback)
  {
    restRequest = request;
    restRequestContext = requestContext;
    restWireAttrs = wireAttrs;
    restCallback = callback;
    RestResponseBuilder builder = new RestResponseBuilder();
    RestResponse response = _emptyResponse ? builder.build() :
        builder.setEntity("This is not empty".getBytes()).build();
    if (_deferCallback)
    {
      scheduleTimeout(requestContext, callback);
      return;
    }
    callback.onResponse(TransportResponseImpl.success(response));
  }

  @Override
  public void streamRequest(StreamRequest request,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      TransportCallback<StreamResponse> callback)
  {
    streamRequest = request;
    restRequestContext = requestContext;
    restWireAttrs = wireAttrs;
    streamCallback = callback;

    StreamResponseBuilder builder = new StreamResponseBuilder();
    StreamResponse response = _emptyResponse ? builder.build(EntityStreams.emptyStream())
        : builder.build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy("This is not empty".getBytes()))));
    if (_deferCallback)
    {
      scheduleTimeout(requestContext, callback);
      return;
    }
    callback.onResponse(TransportResponseImpl.success(response, wireAttrs));
  }

  private <T> void scheduleTimeout(RequestContext requestContext, TransportCallback<T> callback)
  {
    Integer requestTimeout = (Integer) requestContext.getLocalAttr(R2Constants.REQUEST_TIMEOUT);
    if (requestTimeout == null)
    {
      requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    }
    if (requestTimeout < _minRequestTimeout)
    {
      throw new RuntimeException(
          "The timeout is always supposed to be greater than the timeout defined by the service."
              + " This error is enforced in the tests");
    }
    Integer finalRequestTimeout = requestTimeout;
    _scheduler.schedule(() -> callback.onResponse(
        TransportResponseImpl.error(new TimeoutException("Timeout expired after " + finalRequestTimeout + "ms"))),
        requestTimeout, TimeUnit.MILLISECONDS);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    shutdownCalled = true;

    callback.onSuccess(None.none());
  }
}
