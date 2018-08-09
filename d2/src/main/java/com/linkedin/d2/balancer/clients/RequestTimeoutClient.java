/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientDelegator;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.client.TimeoutCallback;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for determining the timeout value for requests.
 * There are three sources of timeout at this level, arranged in increasing precedence
 *
 * 1. The default value in {@link HttpClientFactory}
 * 2. The value configured by user in transportClientProperties. When present, it will clobber item 1.
 * 3. Per request timeout passed for each request. When present, it will clobber item 1 or 2.
 *
 * This class will pick the correct timeout value and set it in {@link RequestContext} under {@code R2Constants.REQUEST_TIME}
 *
 * One peculiarity is when the per request timeout is set to be shorter than the standard timeout (item 1&2). In this case,
 * the standard timeout value will be passed down in RequestContext under {@code R2Constants.REQUEST_TIME} and the shorter per request timeout
 * will be passed down in RequestContext under {@code R2Constants.DEFAULT_REQUEST_TIMEOUT}. A {@link TimeoutCallback} will be used to simulate
 * the behavior of tighter timeout.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class RequestTimeoutClient extends D2ClientDelegator
{
  private static final Logger LOG = LoggerFactory.getLogger(RequestTimeoutClient.class);

  private final D2Client _d2Client;
  private final LoadBalancer _balancer;
  private final ScheduledExecutorService _scheduler;

  public RequestTimeoutClient(D2Client d2Client, LoadBalancer balancer, ScheduledExecutorService scheduler)
  {
    super(d2Client);
    _d2Client = d2Client;
    _balancer = balancer;
    _scheduler = scheduler;
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request)
  {
    return restRequest(request, new RequestContext());
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext)
  {
    final FutureCallback<RestResponse> future = new FutureCallback<>();
    restRequest(request, requestContext, future);
    return future;
  }

  @Override
  public void restRequest(RestRequest request, Callback<RestResponse> callback)
  {
    restRequest(request, new RequestContext(), callback);
  }

  @Override
  public void restRequest(final RestRequest request, final RequestContext requestContext,
      final Callback<RestResponse> callback)
  {
    final Callback<RestResponse> transportCallback =
        decorateCallbackWithRequestTimeout(callback, request, requestContext);
    _d2Client.restRequest(request, requestContext, transportCallback);
  }

  @Override
  public void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
  {
    streamRequest(request, new RequestContext(), callback);
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    final Callback<StreamResponse> transportCallback =
        decorateCallbackWithRequestTimeout(callback, request, requestContext);

    _d2Client.streamRequest(request, requestContext, transportCallback);
  }

  /**
   * Enforces the user timeout to the layer below if necessary.
   *
   * The layer below must have the guarantee that the request timeout is always greater or equal than the one set by
   * D2 to not impact the D2 load balancing policies. This avoids that the degrader/loadbalancer are never triggered.
   *
   * If the value is higher, instead, it will have an impact on the degrader/loadbalancer. If it skews too much the
   * latency and triggers too many times degrader and loadbalancer, those values should be adjusted. In this way
   * we give the guarantee that in the worst case the policies are triggered too much, instead of the opposite (never
   * triggering) which could cause a melt down.
   *
   * The callback has the guarantee to be called at most once, no matter if the call succeeds or times out
   */
  private <RES> Callback<RES> decorateCallbackWithRequestTimeout(Callback<RES> callback, Request request,
      RequestContext requestContext)
  {
    // First, find default timeout value. We get the service properties for this uri
    String serviceName = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
    Map<String, Object> transportClientProperties;
    try
    {
      transportClientProperties =
          _balancer.getLoadBalancedServiceProperties(serviceName).getTransportClientProperties();
    } catch (ServiceUnavailableException e)
    {
      return callback;
    }

    int defaultRequestTimeout = MapUtil.getWithDefault(transportClientProperties, PropertyKeys.HTTP_REQUEST_TIMEOUT,
        HttpClientFactory.DEFAULT_REQUEST_TIMEOUT, Integer.class);

    // Start handling per request timeout
    Number requestTimeout = ((Number) requestContext.getLocalAttr(R2Constants.REQUEST_TIMEOUT));

    if (requestTimeout == null)
    {
      // if no per request timeout is specified, update client experienced timeout with timeout to use and return
      requestContext.putLocalAttr(R2Constants.CLIENT_REQUEST_TIMEOUT_VIEW, defaultRequestTimeout);
      return callback;
    }

    if (requestTimeout.longValue() >= defaultRequestTimeout)
    {
      // if per request is longer than default, just return. The R2 client further down will pick up the longer timeout.
      return callback;
    }

    // if the request timeout is lower than the one set in d2, we will remove the timeout value to prevent R2 client from picking it up
    requestContext.removeLocalAttr(R2Constants.REQUEST_TIMEOUT);

    // we put the client experienced timeout in requestContext for bookkeeping
    requestContext.putLocalAttr(R2Constants.CLIENT_REQUEST_TIMEOUT_VIEW, requestTimeout);

    // we will create a timeout callback which will simulate its behavior
    String timeoutMessage = "Exceeded request timeout of " + requestTimeout + "ms";
    TimeoutCallback<RES> timeoutCallback =
        new TimeoutCallback<>(_scheduler, requestTimeout.longValue(), TimeUnit.MILLISECONDS, callback,
            timeoutMessage);

    return timeoutCallback;
  }
}