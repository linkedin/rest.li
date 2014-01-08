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
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.AbstractClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.error;
import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.trace;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

public class DynamicClient extends AbstractClient implements D2Client
{
  private static final Logger _log = LoggerFactory.getLogger(DynamicClient.class);

  private final LoadBalancer  _balancer;
  private final Facilities    _facilities;

  public DynamicClient(LoadBalancer balancer, Facilities facilities)
  {
    _balancer = balancer;
    _facilities = facilities;
    debug(_log, "created dynamic client: ", this);
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Callback<RestResponse> callback)
  {
    trace(_log, "rest request: ", request);

    try
    {
      TransportClient client = _balancer.getClient(request, requestContext);

      if (client != null)
      {
        new TransportClientAdapter(client).restRequest(request, requestContext, callback);
      }
      else
      {
        throw new ServiceUnavailableException("unknown: " + request.getURI(),
                                              "got null client from load balancer");
      }
    }
    catch (ServiceUnavailableException e)
    {
      callback.onError(e);

      warn(_log, "unable to find service for: ", request);
    }
  }

  @Override
  public void rpcRequest(RpcRequest request,
                         RequestContext requestContext,
                         Callback<RpcResponse> callback)
  {
    trace(_log, "rpc request: ", request);

    try
    {
      TransportClient client = _balancer.getClient(request, requestContext);

      if (client != null)
      {
        new TransportClientAdapter(client).rpcRequest(request, requestContext, callback);
      }
      else
      {
        throw new ServiceUnavailableException("unknown: " + request.getURI(),
                                              "got null client from load balancer");
      }
    }
    catch (ServiceUnavailableException e)
    {
      callback.onError(e);

      warn(_log, "unable to find service for: ", request);
    }
  }

  @Override
  public void start(Callback<None> callback)
  {
    _log.info("starting D2 client");
    _balancer.start(callback);
  }

  @Override
  public void shutdown(final Callback<None> callback)
  {
    info(_log, "shutting down dynamic client");

    _balancer.shutdown(new PropertyEventShutdownCallback()
    {
      @Override
      public void done()
      {
        info(_log, "dynamic client shutdown complete");

        callback.onSuccess(None.none());
      }
    });
  }

  @Override
  public Facilities getFacilities()
  {
    return _facilities;
  }

  @Override
  public Map<String, Object> getMetadata(URI uri)
  {
    if (_balancer != null)
    {
      try
      {
        String serviceName = LoadBalancerUtil.getServiceNameFromUri(uri);
        ServiceProperties serviceProperties = _balancer.getLoadBalancedServiceProperties(serviceName);
        if (serviceProperties != null)
        {
          return Collections.unmodifiableMap(serviceProperties.getServiceMetadataProperties());
        }
      }
      catch (ServiceUnavailableException e)
      {
        error(_log, e);
      }
    }
    return Collections.emptyMap();
  }
}
