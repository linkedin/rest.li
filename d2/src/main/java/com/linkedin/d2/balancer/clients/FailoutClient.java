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
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfig;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A D2 delegator which rewrites URIs to redirect requests to another cluster if the target service has been failed out
 * of service.
 */
public class FailoutClient extends D2ClientDelegator {
  private static final Logger LOG = LoggerFactory.getLogger(FailoutClient.class);

  private final FailoutRedirectStrategy _redirectStrategy;
  private final LoadBalancerWithFacilities _balancer;

  public FailoutClient(D2Client d2Client, LoadBalancerWithFacilities balancer,
      FailoutRedirectStrategy redirectStrategy)
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
  public void restRequest(RestRequest request, final RequestContext requestContext,
      final Callback<RestResponse> callback)
  {
    final FailoutConfig config = getClusterFailoutConfig(request);
    if (config != null && config.isFailedOut()) {
      request = request.builder().setURI(
          _redirectStrategy.redirect(config, request.getURI())).build();
    }

    super.restRequest(request, requestContext, callback);
  }

  @Override
  public void streamRequest(final StreamRequest request, final Callback<StreamResponse> callback)
  {
    streamRequest(request, new RequestContext(), callback);
  }

  @Override
  public void streamRequest(StreamRequest request, final RequestContext requestContext,
      final Callback<StreamResponse> callback)
  {
    final FailoutConfig config = getClusterFailoutConfig(request);
    if (config != null && config.isFailedOut()) {
      request = request.builder().setURI(
          _redirectStrategy.redirect(config, request.getURI())).build(
            request.getEntityStream());
    }

    super.streamRequest(request, requestContext, callback);
  }

  /**
   * Attempt to find the cluster failout config.
   *
   * <p>This should succeed provided the current D2 service is available.</p>
   *
   * @param request the D2 request from which the service name can be found.
   */
  private FailoutConfig getClusterFailoutConfig(Request request)
  {
    String currentService = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
    String currentCluster;
    try {
      currentCluster = _balancer.getLoadBalancedServiceProperties(currentService).getClusterName();
    } catch (ServiceUnavailableException e) {
      LOG.error("Unable to determine the current cluster name; current service is unavailable.", e);
      return null;
    }
    return _balancer.getClusterInfoProvider().getFailoutConfig(currentCluster);
  }
}
