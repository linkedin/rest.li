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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.DarkClusterStrategyNameArray;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.zkfs.ZKFSLoadBalancer;
import com.linkedin.darkcluster.api.BaseDarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.darkcluster.api.NoOpDarkClusterStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DarkClusterStrategyFactoryImpl creates and maintains the strategies needed for dark clusters. This involves refreshing
 * when darkClusterConfig changes are detected, by way of a {@link LoadBalancerClusterListener}
 * start() must be called in order to register the ClusterListener. For instance, the same mechanism that starts the d2 load balancer
 * {@link ZKFSLoadBalancer} should also start this class.
 */
public class DarkClusterStrategyFactoryImpl implements DarkClusterStrategyFactory
{
  private static final Logger LOG = LoggerFactory.getLogger(DarkClusterStrategyFactoryImpl.class);
  public static final DarkClusterStrategy NO_OP_DARK_CLUSTER_STRATEGY = new NoOpDarkClusterStrategy();

  // ClusterInfoProvider isn't available until the D2 client is started, so it can't be
  // populated during construction time.
  private final Facilities _facilities;
  private final String _sourceClusterName;
  private final DarkClusterDispatcher _darkClusterDispatcher;
  private final Notifier _notifier;

  private final Map<String, DarkClusterStrategy> _darkStrategyMap;
  private final Random _random;
  private final LoadBalancerClusterListener _clusterListener;
  private final DarkClusterVerifierManager _verifierManager;

  public DarkClusterStrategyFactoryImpl(@Nonnull Facilities facilities,
                                        @Nonnull String sourceClusterName,
                                        @Nonnull DarkClusterDispatcher darkClusterDispatcher,
                                        @Nonnull Notifier notifier,
                                        @Nonnull Random random,
                                        @Nonnull DarkClusterVerifierManager verifierManager)
  {
    _facilities = facilities;
    _sourceClusterName = sourceClusterName;
    _notifier = notifier;
    _darkStrategyMap = new ConcurrentHashMap<>();
    _random = random;
    _darkClusterDispatcher = darkClusterDispatcher;
    _verifierManager = verifierManager;
    _clusterListener = new DarkClusterListener();
  }

  @Override
  public void start()
  {
    _facilities.getClusterInfoProvider().registerClusterListener(_clusterListener);
    // make sure we're listening to the source cluster and have strategies for any
    // associated dark clusters.
    _clusterListener.onClusterAdded(_sourceClusterName);
    LOG.info("listening to dark clusters on " + _sourceClusterName);
  }

  @Override
  public void shutdown()
  {
    _facilities.getClusterInfoProvider().unregisterClusterListener(_clusterListener);
  }

  /**
   * If we don't have a strategy for the darkClusterName, return the NO_OP strategy, and rely on the listener to
   * populate the darkStrategyMap. We don't want to create a race condition by trying to add what the listener is trying
   * to remove.
   * @param darkClusterName darkClusterName to look up
   * @return darkClusterStrategy to use.
   */
  @Override
  public DarkClusterStrategy get(@Nonnull String darkClusterName)
  {
    return _darkStrategyMap.getOrDefault(darkClusterName, NO_OP_DARK_CLUSTER_STRATEGY);
  }

  /**
   * In the future, additional strategies can be added, and the logic here can choose the appropriate one based on the config values.
   */
  private DarkClusterStrategy createStrategy(String darkClusterName, DarkClusterConfig darkClusterConfig)
  {
    if (darkClusterConfig.hasDarkClusterStrategyPrioritizedList())
    {
      DarkClusterStrategyNameArray strategyList = darkClusterConfig.getDarkClusterStrategyPrioritizedList();
      for (com.linkedin.d2.DarkClusterStrategyName darkClusterStrategyName : strategyList)
      {
        switch(darkClusterStrategyName)
        {
          case RELATIVE_TRAFFIC:
            if (RelativeTrafficMultiplierDarkClusterStrategy.isValidConfig(darkClusterConfig))
            {
              BaseDarkClusterDispatcher baseDarkClusterDispatcher =
                new BaseDarkClusterDispatcherImpl(darkClusterName, _darkClusterDispatcher, _notifier, _verifierManager);
              return new RelativeTrafficMultiplierDarkClusterStrategy(_sourceClusterName, darkClusterName, darkClusterConfig.getMultiplier(),
                                                                      baseDarkClusterDispatcher, _notifier, _facilities.getClusterInfoProvider(),
                                                                      _random);
            }
            break;
          case CONSTANT_QPS:
            // the constant qps strategy is not yet implemented, continue to the next strategy if it exists
            break;
          default:
            break;
        }
      }
    }
    return new NoOpDarkClusterStrategy();
  }

  /**
   * DarkClusterListener will only take action on dark clusters that exist in the strategy map.
   */
  private class DarkClusterListener implements LoadBalancerClusterListener
  {

    @Override
    public void onClusterAdded(String updatedClusterName)
    {
      // It is sufficient to listen just to source cluster updates, because all
      // pertinent dark cluster strategy properties are contained there.
      if (_sourceClusterName.equals(updatedClusterName))
      {
        try
        {
          DarkClusterConfigMap updatedDarkConfigMap = _facilities.getClusterInfoProvider().getDarkClusterConfigMap(_sourceClusterName);

          Set<String> oldDarkStrategySet = _darkStrategyMap.keySet();
          Set<String> updatedDarkClusterConfigKeySet = updatedDarkConfigMap.keySet();
          // Any old strategy entry that isn't in the "updated" set should be removed from the strategyMap.
          oldDarkStrategySet.removeAll(updatedDarkClusterConfigKeySet);
          for (String darkClusterToRemove : oldDarkStrategySet)
          {
            _darkStrategyMap.remove(darkClusterToRemove);
            LOG.info("Removed dark cluster strategy for dark cluster: " + darkClusterToRemove + ", source cluster: " + _sourceClusterName);
          }

          // Now update/add the dark clusters.
          for (Map.Entry<String, DarkClusterConfig> entry : updatedDarkConfigMap.entrySet())
          {
            String darkClusterToAdd = entry.getKey();
            // For simplicity, we refresh all strategies since we expect cluster updates to be rare and refresh to be cheap.
            _darkStrategyMap.put(darkClusterToAdd, createStrategy(darkClusterToAdd, entry.getValue()));
            LOG.info("Created new strategy for dark cluster: " + darkClusterToAdd + ", source cluster: " + _sourceClusterName);
          }
        }
        catch (ServiceUnavailableException e)
        {
          _notifier.notify(() -> new RuntimeException("PEGA_0019 unable to refresh DarkClusterConfigMap for source cluster: "
                                                        + _sourceClusterName));
        }
      }
    }

    /**
     * If the source cluster is removed, the only thing we can do is to make sure the darkStrategyMap is cleared.
     */
    @Override
    public void onClusterRemoved(String clusterName)
    {
      if (_sourceClusterName.equals(clusterName))
      {
        _darkStrategyMap.clear();
      }
    }
  }
}
