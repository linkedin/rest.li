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
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

import java.net.ConnectException;
import java.net.URI;
import java.util.Map;

public class RetryTrackerClient extends TrackerClient
{
  private final URI _uri;

  public RetryTrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient)
  {
    super(uri, partitionDataMap, wrappedClient);
    _uri = uri;
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<RestResponse> callback)
  {
    TransportResponse<RestResponse> response;
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
      response = TransportResponseImpl.success(new RestResponseBuilder().build(), wireAttrs);
    }
    callback.onResponse(response);
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
}
