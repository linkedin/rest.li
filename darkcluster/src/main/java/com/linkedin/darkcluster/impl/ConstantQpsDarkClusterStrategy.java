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
import com.linkedin.darkcluster.util.GuaranteedRateLimiter;
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
 * keep the incoming QPS per dark host constant.
 *
 * It uses the {@link ClusterInfoProvider} to determine the number of instances in both the source and target cluster,
 * and uses that to calculate the number of requests to send in order to make the QPS per dark host constant and equal
 * to a specified value, assuming all hosts in the source cluster send traffic.
 */
public class ConstantQpsDarkClusterStrategy implements DarkClusterStrategy {
  private final String _originalClusterName;
  private final String _darkClusterName;
  private final Integer _darkClusterPerHostInboundTargetRate;
  private final BaseDarkClusterDispatcher _baseDarkClusterDispatcher;
  private final Notifier _notifier;
  private final ClusterInfoProvider _clusterInfoProvider;
  private final GuaranteedRateLimiter _rateLimiter;

  public ConstantQpsDarkClusterStrategy(@Nonnull String originalClusterName, @Nonnull String darkClusterName,
      @Nonnull Integer darkClusterPerHostInboundTargetRate, @Nonnull BaseDarkClusterDispatcher baseDarkClusterDispatcher,
      @Nonnull Notifier notifier, @Nonnull ClusterInfoProvider clusterInfoProvider, @Nonnull GuaranteedRateLimiter rateLimiter) {
    _originalClusterName = originalClusterName;
    _darkClusterName = darkClusterName;
    _darkClusterPerHostInboundTargetRate = darkClusterPerHostInboundTargetRate;
    _baseDarkClusterDispatcher = baseDarkClusterDispatcher;
    _notifier = notifier;
    _clusterInfoProvider = clusterInfoProvider;
    _rateLimiter = rateLimiter;
  }

  @Override
  public boolean handleRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext) {
    float sendRate = getPerHostSendRate();
    _rateLimiter.setRate(sendRate, 1000, 1);
    return addRequestToQueue(originalRequest, darkRequest, requestContext);
  }

  /**
   * We won't create this strategy if this config isn't valid for this strategy. For instance, we don't want to create
   * the ConstantQpsDarkClusterStrategy if there's no darkClusterPerHostInboundTargetRate or if the
   * darkClusterPerHostInboundTargetRate is zero, because we'd be doing pointless work on every getOrCreate.
   * Instead if will go to the next strategy (or NoOpDarkClusterStrategy).
   *
   * This is a static method defined here because we don't want to instantiate a strategy to check this. It cannot be a
   * method that is on the interface because static methods on an interface cannot be overridden by implementations.
   * @param darkClusterConfig
   * @return true if config is valid for this strategy
   */
  public static boolean isValidConfig(DarkClusterConfig darkClusterConfig) {
    // TODO: Commented out for testing in ei. Still finalizing what config values we'll use. Uncomment when design is finalized.
    return true;
    // return darkClusterConfig.hasDispatcherOutboundTargetRate() &&
    //    darkClusterConfig.getDispatcherOutboundTargetRate() > 0;
  }

  /**
   * TODO: javadocs
   * @return
   */
  private float getPerHostSendRate() {
    try {
      // Only support https for now. http support can be added later if truly needed, but would be non-ideal
      // because potentially both dark and source would have to be configured.
      int numDarkClusterInstances = _clusterInfoProvider.getHttpsClusterCount(_darkClusterName);
      int numSourceClusterInstances = _clusterInfoProvider.getHttpsClusterCount(_originalClusterName);
      if (numSourceClusterInstances != 0) {
        return (float) (_darkClusterPerHostInboundTargetRate * numDarkClusterInstances) / numSourceClusterInstances;
      }

      return 0F;
    } catch (ServiceUnavailableException e) {
      _notifier.notify(() -> new RuntimeException(
          "PEGA_0020 unable to compute strategy for source cluster: " + _originalClusterName + ", darkClusterName: " + _darkClusterName, e));
      // safe thing is to return 0 so dark traffic isn't sent.
      return 0F;
    }
  }

  /**
   * TODO: javadocs
   * @param originalRequest
   * @param darkRequest
   * @param requestContext
   * @return
   */
  private boolean addRequestToQueue(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext)
  {
    _rateLimiter.submit(new Callback<None>() {
      @Override
      public void onError(Throwable e) {
        //
      }

      @Override
      public void onSuccess(None result) {
        _baseDarkClusterDispatcher.sendRequest(originalRequest, darkRequest, requestContext, 1);
      }
    });
    return true;
  }
}
