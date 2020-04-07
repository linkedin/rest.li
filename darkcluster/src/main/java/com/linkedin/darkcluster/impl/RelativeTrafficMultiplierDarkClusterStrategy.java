/*
   Copyright (c) 2020 LinkedIn Corp.

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

import java.util.Random;

import javax.annotation.Nonnull;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.BaseDarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;

/**
 * RelativeTrafficMultiplierDarkClusterStrategy figures out how many dark requests to send. It uses the {@link ClusterInfoProvider} to determine
 * the number ofinstances in both the source and target cluster, and uses that to calculate the number of request to send in order to make the
 * level of traffic proportional to itself on any instance in the dark cluster (accounting for multiplier), assuming all hosts in the source cluster
 * send traffic.
 */
public class RelativeTrafficMultiplierDarkClusterStrategy implements DarkClusterStrategy
{
  private final String _originalClusterName;
  private final String _darkClusterName;
  private final Float _multiplier;
  private final BaseDarkClusterDispatcher _baseDarkClusterDispatcher;
  private final Notifier _notifier;
  private final Random _random;
  private final ClusterInfoProvider _clusterInfoProvider;

  public RelativeTrafficMultiplierDarkClusterStrategy(@Nonnull String originalClusterName, @Nonnull String darkClusterName, @Nonnull Float multiplier,
                                                      @Nonnull BaseDarkClusterDispatcher baseDarkClusterDispatcher,
                                                      @Nonnull Notifier notifier,
                                                      @Nonnull ClusterInfoProvider clusterInfoProvider,
                                                      @Nonnull Random random)
  {
    _originalClusterName = originalClusterName;
    _darkClusterName = darkClusterName;
    _multiplier = multiplier;
    _baseDarkClusterDispatcher = baseDarkClusterDispatcher;
    _notifier = notifier;
    _clusterInfoProvider = clusterInfoProvider;
    _random = random;
  }

  @Override
  public boolean handleRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext)
  {
    int numRequestDuplicates = getNumDuplicateRequests(_random.nextFloat());
    return _baseDarkClusterDispatcher.sendRequest(originalRequest, darkRequest, requestContext, numRequestDuplicates);
  }

  /**
   * The formula to keep traffic proportional to the sending cluster is
   * Avg#DarkRequests = ((# instances in dark cluster) * multiplier) / (# instances in source cluster)
   *
   * For example, if there are 2 dark instances, and 10 instances in the source cluster, with a multiplier of 1, we get:
   * Avg#DarkRequests = (2 * 1)/10 = 0.2, or a 20% chance of sending a request. Across 10 instances, 20% of traffic will
   * be duplicated, and roughly 10% will go to each dark instance. If multiplier of 1.2 was chosen, then there would be a
   * 24% of the source cluster traffic redirected.
   *
   * another example:
   * 1 dark instance, 7 source instances, multiplier = 0.5. the Avg#DarkRequests = (1 * 0.5)/7 = 0.0714. Thus, each
   * source instance should send 7.14% of it's traffic.
   * This make sense: 7.14% * 7 source instances = 50% source instance traffic.
   *
   * An uncommon but possible configuration:
   * 10 dark instances, 10 source instances, multiplier = 1.5. Avg#DarkRequests = (10 * 1.5)/10 = 1.5. In this case at least
   * 1 request will be sent, with a 50% probability another request will be sent as well.
   */
  private int getNumDuplicateRequests(float randomNum)
  {

    try
    {
      // Only support https for now. http support can be added later if truly needed, but would be non-ideal
      // because potentially both dark and source would have to be configured.
      int numDarkClusterInstances = _clusterInfoProvider.getHttpsClusterCount(_darkClusterName);
      int numSourceClusterInstances = _clusterInfoProvider.getHttpsClusterCount(_originalClusterName);
      if (numSourceClusterInstances != 0)
      {
        float avgNumDarkRequests = (numDarkClusterInstances * _multiplier) / numSourceClusterInstances;
        float avgDarkDecimalPart = avgNumDarkRequests % 1;
        return randomNum < avgDarkDecimalPart ? ((int)avgNumDarkRequests) + 1 : (int)avgNumDarkRequests;
      }

      return 0;
    }
    catch (ServiceUnavailableException e)
    {
      _notifier.notify(() -> new RuntimeException("PEGA_0020 unable to compute strategy for source cluster: "
                                                    + _originalClusterName + ", darkClusterName: " + _darkClusterName, e));
      // safe thing is to return 0 so dark traffic isn't sent.
      return 0;
    }
  }

  // for testing purposes, but ok to expose publicly on implementation.
  public Float getMultiplier()
  {
    return _multiplier;
  }
}
