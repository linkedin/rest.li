/*
   Copyright (c) 2016 LinkedIn Corp.

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
package com.linkedin.d2.balancer.clients;

import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.DrainReader;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.util.clock.SystemClock;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class RetryTrackerClient extends DegraderTrackerClient
{
  private final URI _uri;

  public RetryTrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient)
  {
    super(uri, partitionDataMap, wrappedClient, SystemClock.instance(), null,
          TrackerClientImpl.DEFAULT_CALL_TRACKER_INTERVAL, TrackerClientImpl.DEFAULT_ERROR_STATUS_PATTERN);
    _uri = uri;
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<RestResponse> callback)
  {
    handleRequest(request, wireAttrs, callback, r -> {}, () -> new RestResponseBuilder().build());
  }

  @Override
  public void streamRequest(StreamRequest request,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      TransportCallback<StreamResponse> callback)
  {
    handleRequest(request, wireAttrs, callback,
        r -> r.getEntityStream().setReader(new DrainReader()),
        () -> new StreamResponseBuilder().build(EntityStreams.emptyStream()));
  }

  @Override
  public URI getUri()
  {
    return _uri;
  }

  @Override
  public String toString()
  {
    return "";
  }

  private <REQ extends Request, RESP extends Response> void handleRequest(
      REQ request,
      Map<String, String> wireAttrs,
      TransportCallback<RESP> callback,
      Consumer<REQ> requestConsumer,
      Supplier<RESP> responseSupplier)
  {
    // Process request
    requestConsumer.accept(request);

    // Prepare response
    TransportResponse<RESP> response;
    if (_uri.toString().startsWith("http://test.linkedin.com/retry"))
    {
      RetriableRequestException ex = new RetriableRequestException("Data not available");
      response = TransportResponseImpl.error(ex);
    }
    else if (_uri.toString().equals("http://test.linkedin.com/bad"))
    {
      response = TransportResponseImpl.error(RestException.forError(404, "exception happens"), wireAttrs);
    }
    else
    {
      response = TransportResponseImpl.success(responseSupplier.get(), wireAttrs);
    }
    callback.onResponse(response);
  }
}
