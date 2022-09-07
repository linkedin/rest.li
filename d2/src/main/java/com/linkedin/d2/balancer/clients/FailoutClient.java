/*
   Copyright (c) 2022 LinkedIn Corp.
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
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientDelegator;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfig;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.net.URI;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A D2 delegator which rewrites URIs to redirect requests to another cluster if the target service has been failed out
 * of service.
 */
public class FailoutClient extends D2ClientDelegator
{
  private static final Logger LOG = LoggerFactory.getLogger(FailoutClient.class);

  private final FailoutRedirectStrategy _redirectStrategy;
  private final LoadBalancerWithFacilities _balancer;

  public FailoutClient(D2Client d2Client, LoadBalancerWithFacilities balancer, FailoutRedirectStrategy redirectStrategy)
  {
    super(d2Client);
    _balancer = balancer;
    _redirectStrategy = redirectStrategy;
  }

  @Override
  public Future<RestResponse> restRequest(final RestRequest request)
  {
    return restRequest(request, new RequestContext());
  }

  @Override
  public Future<RestResponse> restRequest(final RestRequest request, final RequestContext requestContext)
  {
    final FutureCallback<RestResponse> future = new FutureCallback<>();
    restRequest(request, requestContext, future);
    return future;
  }

  @Override
  public void restRequest(final RestRequest request, final Callback<RestResponse> callback)
  {
    restRequest(request, new RequestContext(), callback);
  }

  @Override
  public void restRequest(final RestRequest request, final RequestContext requestContext, final Callback<RestResponse> callback)
  {
    determineRequestUri(request, new Callback<URI>()
    {
      @Override
      public void onError(Throwable e)
      {
        LOG.error("Failed to build request URI. Original request URI will be used.", e);
        FailoutClient.super.restRequest(request, requestContext, callback);
      }

      @Override
      public void onSuccess(URI result)
      {
        final RestRequest redirectedRequest = request.builder().setURI(result).build();
        FailoutClient.super.restRequest(redirectedRequest, requestContext, callback);
      }
    });
  }

  @Override
  public void streamRequest(final StreamRequest request, final Callback<StreamResponse> callback)
  {
    streamRequest(request, new RequestContext(), callback);
  }

  @Override
  public void streamRequest(final StreamRequest request, final RequestContext requestContext, final Callback<StreamResponse> callback)
  {
    determineRequestUri(request, new Callback<URI>()
    {
      @Override
      public void onError(Throwable e)
      {
        LOG.error("Failed to build request URI. Original request URI will be used.", e);
        FailoutClient.super.streamRequest(request, requestContext, callback);
      }

      @Override
      public void onSuccess(URI result)
      {
        final StreamRequest redirectedRequest = request.builder().setURI(result).build(
          request.getEntityStream());
        FailoutClient.super.streamRequest(redirectedRequest, requestContext, callback);
      }
    });
  }

  /**
   * Attempts to determine correct request Uri. The original request URI will be used if there is no active failout.
   *
   * @param request the D2 request from which the service name can be found.
   * @param callback callback to be invoked once the request URI has been determined
   */
  private void determineRequestUri(final Request request, Callback<URI> callback)
  {
    String currentService = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
    _balancer.getLoadBalancedServiceProperties(currentService, new Callback<ServiceProperties>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(ServiceProperties result)
      {
        String cluster = result.getClusterName();
        FailoutConfig config = _balancer.getClusterInfoProvider().getFailoutConfig(cluster);

        if (config != null && config.isFailedOut())
        {
          // Rewrites the URI based on failout config
          callback.onSuccess(_redirectStrategy.redirect(config, request.getURI()));
        }
        else
        {
          // Keep URI unchanged if there is no active failout
          callback.onSuccess(request.getURI());
        }
      }
    });
  }
}
