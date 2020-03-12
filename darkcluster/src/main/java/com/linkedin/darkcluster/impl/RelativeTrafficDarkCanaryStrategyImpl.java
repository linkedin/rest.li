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
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;

/**
 * RelativeTrafficDarkCanaryStrategyImpl figures out how many dark requests to send. It uses the ClusterInfoProvider to determine the number of
 * instances in both the source and target cluster, and uses that to calculate the number of request to send in order to make the level of traffic
 * proportional to itself on any instance in the dark cluster (accounting for multiplier), assuming all hosts in the source cluster send traffic.
 */
public class RelativeTrafficDarkCanaryStrategyImpl implements DarkClusterStrategy
{
  private final String _originalClusterName;
  private final String _darkClusterName;
  private final Float _multiplier;
  private final ClusterInfoProvider _clusterInfoProvider;
  private final BaseDarkClusterDispatcher _baseDarkClusterDispatcher;
  private final Notifier _notifier;
  private final Random _random;

  public RelativeTrafficDarkCanaryStrategyImpl(@Nonnull String originalClusterName, @Nonnull String darkClusterName, @Nonnull Float multiplier,
                                               @Nonnull ClusterInfoProvider clusterInfoProvider,
                                               @Nonnull BaseDarkClusterDispatcher baseDarkClusterDispatcher,
                                               @Nonnull Notifier notifier, @Nonnull Random random)
  {
    _originalClusterName = originalClusterName;
    _darkClusterName = darkClusterName;
    _multiplier = multiplier;
    _clusterInfoProvider = clusterInfoProvider;
    _baseDarkClusterDispatcher = baseDarkClusterDispatcher;
    _notifier = notifier;
    _random = random;
  }

  @Override
  public boolean handleRequest(RestRequest originalRequest,RestRequest darkRequest, RequestContext requestContext)
  {
    boolean darkRequestSent = false;
    int numRequestDuplicates = getNumDuplicateRequests(_darkClusterName, _originalClusterName, _multiplier, _random.nextFloat());

    _baseDarkClusterDispatcher.sendRequest(originalRequest, darkRequest, requestContext, numRequestDuplicates);
    darkRequestSent = true;
    return darkRequestSent;
  }

  private int getNumDuplicateRequests(String darkClusterName, String originalClusterName, Float multiplier, float randomNum)
  {
    //        float multiplier = darkClusterConfigEntry.getValue().getMultiplier();
    //        float multiplierDecimalPart = multiplier % 1;
    //        int numRequestDuplicates = randomNum < multiplierDecimalPart ? (int) multiplier + 1 : (int) multiplier;
    // Not yet implemented
    return 1;
  }
}
