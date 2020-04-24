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

/**
 * DarkClusterStrategyFactoryImpl creates and maintains the strategies needed for dark clusters. This involves refreshing
 * when darkClusterConfig changes are detected, by way of a {@link LoadBalancerClusterListener}
 * start() must be called in order to register the ClusterListener. For instance, the same mechanism that starts the d2 load balancer
 * {@link ZKFSLoadBalancer} should also start this class.
 */
public class DarkClusterStrategyFactoryImpl implements DarkClusterStrategyFactory
{
  private static final DarkClusterStrategy NO_OP_DARK_CLUSTER_STRATEGY = new NoOpDarkClusterStrategy();

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
  }

  @Override
  public void shutdown()
  {
    _facilities.getClusterInfoProvider().unregisterClusterListener(_clusterListener);
  }



  @Override
  public DarkClusterStrategy getOrCreate(@Nonnull String darkClusterName, @Nonnull DarkClusterConfig darkClusterConfig)
  {
    // pre jdk9 computeIfAbsent has a performance issue. In the future, this contain/put could be rewritten to
    // _darkStrategyMap.computeIfAbsent(darkClusterName, k -> createStrategy(darkClusterName, darkClusterConfig));
    if (!_darkStrategyMap.containsKey(darkClusterName))
    {
      _darkStrategyMap.putIfAbsent(darkClusterName, createStrategy(darkClusterName, darkClusterConfig));
    }
    // it's theoretically possible for the Listener to remove the entry after the containsKey but before we retrieve it. Rather
    // than adding synchronization between accessors of _darkStrategyMap, we will make each put or get resilient
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
      // We will be listening to both the source cluster because we needed the DarkClusterConfig
      // from the source cluster, and that called listenToCluster.
      // We also will be listening to updates on the dark clusters, because we'll be sending d2 requests
      // to the dark clusters, and will be listening on the dark cluster znodes.
      // It is more precise to update on just dark cluster updates, because listening on the
      // source cluster updates might have unrelated changes, whereas when a dark cluster update happens
      // we know for sure we need to update that dark cluster.
      if (_darkStrategyMap.containsKey(updatedClusterName))
      {
        // this is a dark cluster name. however, to refresh the strategies, we need to pull the
        // darkClusterConfigMap on the parent d2 cluster, because that has the properties needed
        // to recreate the dark cluster strategies, such as the multiplier.
        String darkClusterName = updatedClusterName;
        try
        {
          DarkClusterConfigMap darkConfigMap = _facilities.getClusterInfoProvider().getDarkClusterConfigMap(_sourceClusterName);
          if (darkConfigMap.containsKey(darkClusterName))
          {
            // just update the dark cluster that changed
            _darkStrategyMap.put(darkClusterName, createStrategy(darkClusterName,
                                                                 darkConfigMap.get(darkClusterName)));
          }
        }
        catch (ServiceUnavailableException e)
        {
          _notifier.notify(() -> new RuntimeException("PEGA_0019 unable to refresh DarkClusterConfigMap for source cluster: "
                                                        + _sourceClusterName + ", darkClusterName: " + darkClusterName));
        }
      }
    }

    /**
     * The only thing we can do on onClusterRemoved is to remove the cluster that got triggered. Theoretically we should never see the source
     * cluster being removed if we still have strategies for it's dark clusters. If there is any case where the source cluster
     * could be removed while we still have strategies for the dark cluster, we should reevaluate this cluster listener. For now, there is no
     * need to take any additional action.
     */
    @Override
    public void onClusterRemoved(String clusterName)
    {
      _darkStrategyMap.remove(clusterName);
    }
  }
}
