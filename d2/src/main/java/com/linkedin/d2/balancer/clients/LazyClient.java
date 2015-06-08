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
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;

public class LazyClient implements TransportClient
{
  private static final Logger             _log = LoggerFactory.getLogger(TransportClient.class);

  private final TransportClientFactory _clientFactory;
  private final Map<String, String>    _properties;
  private volatile TransportClient     _wrappedClient;

  public LazyClient(Map<String, String> properties, TransportClientFactory clientFactory)
  {
    _properties = properties;
    _clientFactory = clientFactory;

    debug(_log, "created lazy client: ", this);
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<RestResponse> callback)
  {
    getWrappedClient().restRequest(request, requestContext, wireAttrs, callback);
  }

  @Override
  public void streamRequest(StreamRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<StreamResponse> callback)
  {
    getWrappedClient().streamRequest(request, requestContext, wireAttrs, callback);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    getWrappedClient().shutdown(callback);
  }

  public synchronized TransportClient getWrappedClient()
  {
    if (_wrappedClient == null)
    {
      debug(_log, "initializing wrapped client with properties: ", _properties);

      _wrappedClient = _clientFactory.getClient(_properties);
    }

    return _wrappedClient;
  }

  @Override
  public String toString()
  {
    return "LazyClient [_clientFactory=" + _clientFactory + ", _properties="
        + _properties + ", _wrappedClient=" + _wrappedClient + "]";
  }
}
