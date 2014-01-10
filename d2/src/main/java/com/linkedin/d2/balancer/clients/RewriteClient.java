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

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.d2.balancer.LoadBalancerClient;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.common.util.None;

import static com.linkedin.d2.discovery.util.LogUtil.debug;

public class RewriteClient implements LoadBalancerClient
{
  private static final Logger      _log = LoggerFactory.getLogger(TrackerClient.class);

  private final String          _serviceName;
  private final URI             _uri;
  private final TransportClient _wrappedClient;

  public RewriteClient(String serviceName, URI uri, TransportClient wrappedClient)
  {
    _serviceName = serviceName;
    _uri = uri;
    _wrappedClient = wrappedClient;

    debug(_log, "created rewrite client: ", this);
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<RestResponse> callback)
  {
    _wrappedClient.restRequest(rewriteRequest(request),
                               requestContext,
                               wireAttrs,
                               callback);
  }

  @Override
  public void rpcRequest(RpcRequest request,
                         RequestContext requestContext,
                         Map<String, String> wireAttrs,
                         TransportCallback<RpcResponse> callback)
  {
    _wrappedClient.rpcRequest(rewriteRequest(request),
                              requestContext,
                              wireAttrs,
                              callback);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _wrappedClient.shutdown(callback);
  }

  public TransportClient getWrappedClient()
  {
    return _wrappedClient;
  }

  @SuppressWarnings("unchecked")
  private <M extends Request> M rewriteRequest(M req)
  {
    return (M)req.requestBuilder().setURI(rewriteUri(req.getURI())).build();
  }

  private URI rewriteUri(URI uri)
  {
    assert _serviceName.equals(LoadBalancerUtil.getServiceNameFromUri(uri));

    String path = LoadBalancerUtil.getRawPathFromUri(uri);

    UriBuilder builder = UriBuilder.fromUri(_uri);
    if (path != null)
    {
      builder.path(path);
    }
    builder.replaceQuery(uri.getRawQuery());
    builder.fragment(uri.getRawFragment());
    URI rewrittenUri = builder.build();

    debug(_log, "rewrite uri ", uri, " -> ", rewrittenUri);

    return rewrittenUri;
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
    return "RewriteClient [_serviceName=" + _serviceName + ", _uri=" + _uri
        + ", _wrappedClient=" + _wrappedClient + "]";
  }
}
