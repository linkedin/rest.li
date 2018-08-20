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
import com.linkedin.d2.balancer.LoadBalancerClient;
import com.linkedin.d2.balancer.util.D2URIRewriter;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;

public class RewriteLoadBalancerClient implements LoadBalancerClient
{
  private static final Logger      _log = LoggerFactory.getLogger(TrackerClient.class);

  private final String          _serviceName;
  private final URI             _uri;
  private final RewriteClient _client;

  public RewriteLoadBalancerClient(String serviceName, URI uri, TransportClient client)
  {
    _serviceName = serviceName;
    _uri = uri;
    _client = new RewriteClient(client, new D2URIRewriter(uri));
    debug(_log, "created rewrite client: ", this);
  }

  @Override
  public void restRequest(RestRequest request,
                   RequestContext requestContext,
                   Map<String, String> wireAttrs,
                   TransportCallback<RestResponse> callback)
  {
    assert _serviceName.equals(LoadBalancerUtil.getServiceNameFromUri(request.getURI()));
    _client.restRequest(request, requestContext, wireAttrs, callback);
  }

  @Override
  public void streamRequest(StreamRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<StreamResponse> callback)
  {
    assert _serviceName.equals(LoadBalancerUtil.getServiceNameFromUri(request.getURI()));
    _client.streamRequest(request, requestContext, wireAttrs, callback);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _client.shutdown(callback);
  }

  @Deprecated
  public TransportClient getWrappedClient()
  {
    return _client;
  }

  public TransportClient getDecoratedClient()
  {
    return _client;
  }

  @Override
  public URI getUri()
  {
    return _uri;
  }

  public String getServiceName()
  {
    return _serviceName;
  }

  @Override
  public String toString()
  {
    return "RewriteLoadBalancerClient [_serviceName=" + _serviceName + ", _uri=" + _uri
        + ", _wrappedClient=" + _client + "]";
  }
}
