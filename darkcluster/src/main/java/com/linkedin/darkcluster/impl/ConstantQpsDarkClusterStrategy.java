/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.darkcluster.impl;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.ConstantQpsRateLimiter;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.BaseDarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;

/**
 * ConstantQpsDarkClusterStrategy figures out how many dark requests to send. The high level goal of this strategy is to
 * keep the incoming QPS per dark cluster host constant.
 *
 * It uses the {@link ClusterInfoProvider} to determine the number of instances in both the source and target cluster,
 * and uses that to calculate the number of requests to send in order to make the QPS per dark cluster host constant and equal
 * to a specified value, assuming all hosts in the source cluster send traffic.
 *
 * This strategy differs from the RELATIVE_TRAFFIC and IDENTICAL_TRAFFIC strategies in that requests are dispatched by a
 * rate-limited event loop after being stored in a circular buffer. This provides a steady stream of outbound traffic that
 * only duplicates requests when the inbound rate of traffic is less than the outbound rate. With the other strategies,
 * requests are randomly selected based on a multiplier. With this strategy, all requests are submitted to the rate-limiter,
 * which dispatches and evicts stored requests based on its configuration.
 */
public class ConstantQpsDarkClusterStrategy implements DarkClusterStrategy
{
  private final String _originalClusterName;
  private final String _darkClusterName;
  private final Float _darkClusterPerHostQps;
  private final BaseDarkClusterDispatcher _baseDarkClusterDispatcher;
  private final Notifier _notifier;
  private final ClusterInfoProvider _clusterInfoProvider;
  private final ConstantQpsRateLimiter _rateLimiter;

  private static final long ONE_SECOND_PERIOD = TimeUnit.SECONDS.toMillis(1);
  private static final int NUM_REQUESTS_TO_SEND_PER_RATE_LIMITER_CYCLE = 1;

  public ConstantQpsDarkClusterStrategy(@Nonnull String originalClusterName, @Nonnull String darkClusterName,
      @Nonnull Float darkClusterPerHostQps, @Nonnull BaseDarkClusterDispatcher baseDarkClusterDispatcher,
      @Nonnull Notifier notifier, @Nonnull ClusterInfoProvider clusterInfoProvider, @Nonnull ConstantQpsRateLimiter rateLimiter)
  {
    _originalClusterName = originalClusterName;
    _darkClusterName = darkClusterName;
    _darkClusterPerHostQps = darkClusterPerHostQps;
    _baseDarkClusterDispatcher = baseDarkClusterDispatcher;
    _notifier = notifier;
    _clusterInfoProvider = clusterInfoProvider;
    _rateLimiter = rateLimiter;
  }

  @Override
  public boolean handleRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext)
  {
    float sendRate = getSendRate();
    // set burst in such a way that requests are dispatched evenly across the ONE_SECOND_PERIOD
    int burst = (int) Math.max(1, Math.ceil(sendRate / ONE_SECOND_PERIOD));
    _rateLimiter.setRate(sendRate, ONE_SECOND_PERIOD, burst);
    return addRequest(originalRequest, darkRequest, requestContext);
  }

  /**
   * We won't create this strategy if this config isn't valid for this strategy. For instance, we don't want to create
   * the ConstantQpsDarkClusterStrategy if any of the configurables are zero, because we'd be doing pointless work on every getOrCreate.
   * Instead if will go to the next strategy (or NoOpDarkClusterStrategy).
   *
   * This is a static method defined here because we don't want to instantiate a strategy to check this. It cannot be a
   * method that is on the interface because static methods on an interface cannot be overridden by implementations.
   * @param darkClusterConfig
   * @return true if config is valid for this strategy
   */
  public static boolean isValidConfig(DarkClusterConfig darkClusterConfig)
  {
    return darkClusterConfig.hasDispatcherOutboundTargetRate() &&
        darkClusterConfig.getDispatcherOutboundTargetRate() > 0 &&
        darkClusterConfig.hasDispatcherMaxRequestsToBuffer() &&
        darkClusterConfig.getDispatcherMaxRequestsToBuffer() > 0 &&
        darkClusterConfig.hasDispatcherBufferedRequestExpiryInSeconds() &&
        darkClusterConfig.getDispatcherBufferedRequestExpiryInSeconds() > 0;
  }

  /**
   * Provides the rate of requests to send per second from this host to the dark cluster. Result of this method call should
   * be used to configure the ConstantQpsRateLimiter.
   *
   * It uses the {@link ClusterInfoProvider} to make the following calculation:
   *
   * RequestsPerSecond = ((# instances in dark cluster) * darkClusterPerHostQps) / (# instances in source cluster)
   *
   * For example, if there are 2 dark instances, and 10 instances in the source cluster, with a darkClusterPerHostQps of 50, we get:
   * RequestsPerSecond = (2 * 50)/10 = 10.
   *
   * another example:
   * 1 dark instance, 7 source instances, darkClusterPerHostQps = 75.
   * RequestsPerSecond = (1 * 75)/7 = 10.71429.
   *
   * An uncommon but possible configuration:
   * 10 dark instances, 1 source instance, darkClusterPerHostQps = 50.
   * RequestsPerSecond = (10 * 50)/1 = 500.
   *
   * @return requests per second this host should dispatch
   */
  private float getSendRate()
  {
    try
    {
      // Only support https for now. http support can be added later if truly needed, but would be non-ideal
      // because potentially both dark and source would have to be configured.
      int numDarkClusterInstances = _clusterInfoProvider.getHttpsClusterCount(_darkClusterName);
      int numSourceClusterInstances = _clusterInfoProvider.getHttpsClusterCount(_originalClusterName);
      if (numSourceClusterInstances != 0)
      {
        return (numDarkClusterInstances * _darkClusterPerHostQps) / numSourceClusterInstances;
      }

      return 0F;
    }
    catch (ServiceUnavailableException e)
    {
      _notifier.notify(() -> new RuntimeException(
          "PEGA_0020 unable to compute strategy for source cluster: " + _originalClusterName + ", darkClusterName: " + _darkClusterName, e));
      // safe thing is to return 0 so dark traffic isn't sent.
      return 0F;
    }
  }

  /**
   * Wraps the provided request in a Callback and adds it to the rate-limiter for storage in its buffer. Once stored,
   * the rate-limiter will begin including this request in the collection of requests it dispatches. Requests stored in
   * the {@link ConstantQpsRateLimiter} will continue to be dispatched until overwritten by newer requests, or until their TTLs expire.

   * @return always returns true since callbacks can always be added to {@link ConstantQpsRateLimiter};
   */
  private boolean addRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext)
  {
    _rateLimiter.submit(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        //
      }

      @Override
      public void onSuccess(None result)
      {
        _baseDarkClusterDispatcher.sendRequest(originalRequest, darkRequest, requestContext, NUM_REQUESTS_TO_SEND_PER_RATE_LIMITER_CYCLE);
      }
    });
    return true;
  }
}
