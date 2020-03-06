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

import java.util.Map;
import java.util.Random;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterUtils;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractDarkClusterStrategyImpl provides the basic logic to rewrite the original request into dark cluster request(s). It uses
 * the ClusterInfoProvider to pull what darkClusters the requests should be sent to. Non-abstract implementations of DarkClusterStrategy that
 * implement getNumDuplicateRequests() are provided with the DarkClusterConfigMap to calculate the appropriate number, depending on the Strategy.
 */
public abstract class AbstractDarkClusterStrategyImpl implements DarkClusterStrategy
{
  private static final Logger LOG = LoggerFactory.getLogger(AbstractDarkClusterStrategyImpl.class);

  private final Random _random = new Random();
  private final ClusterInfoProvider _clusterInfoProvider;
  private final String _clusterName;
  private final BaseDarkClusterDispatcher _baseDarkClusterDispatcher;

  public AbstractDarkClusterStrategyImpl(ClusterInfoProvider clusterInfoProvider, String clusterName,
                                         BaseDarkClusterDispatcher baseDarkClusterDispatcher)
  {
    _clusterInfoProvider = clusterInfoProvider;
    _clusterName = clusterName;
    _baseDarkClusterDispatcher = baseDarkClusterDispatcher;
  }

  @Override
  public boolean handleRequest(RestRequest request, RequestContext requestContext)
  {
    boolean darkRequestSent = false;
    float randomNum = _random.nextFloat();
    try
    {
      DarkClusterConfigMap configMap = _clusterInfoProvider.getDarkClusterConfigMap(_clusterName);
      for (Map.Entry<String, DarkClusterConfig> darkClusterConfigEntry : configMap.entrySet())
      {
        String darkClusterName = darkClusterConfigEntry.getKey();
        final RestRequest darkRequest = DarkClusterUtils.updateRequestInfo(darkClusterName, request);

        int numRequestDuplicates = getNumDuplicateRequests(darkClusterName, _clusterName, darkClusterConfigEntry.getValue(), randomNum);

        _baseDarkClusterDispatcher.sendRequest(request, darkRequest, requestContext, numRequestDuplicates);
        darkRequestSent = true;
      }
    }
    catch (ServiceUnavailableException e)
    {
      e.printStackTrace();
      return false;
    }
    return darkRequestSent;
  }
}
