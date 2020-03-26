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
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.BaseDarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.darkcluster.api.NoOpDarkClusterStrategy;

/**
 * DarkClusterStrategyFactoryImpl creates and maintains the strategies needed for dark clusters. This involves refreshing
 * when darkClusterConfig changes are detected, by way of a {@link LoadBalancerClusterListener}
 */
public class DarkClusterStrategyFactoryImpl implements DarkClusterStrategyFactory
{
  private final ClusterInfoProvider _clusterInfoProvider;
  private final String _sourceClusterName;
  private final DarkClusterDispatcher _darkClusterDispatcher;
  private final Notifier _notifier;
  private final Map<String, AtomicReference<DarkClusterStrategy>> _darkStrategyMap;
  private final Random _random;
  private final LoadBalancerClusterListener _clusterListener;
  private final DarkClusterVerifierManager _verifierManager;

  public DarkClusterStrategyFactoryImpl(@Nonnull ClusterInfoProvider clusterInfoProvider,
                                        @Nonnull String sourceClusterName,
                                        @Nonnull DarkClusterDispatcher darkClusterDispatcher,
                                        @Nonnull Notifier notifier,
                                        @Nonnull Random random,
                                        @Nonnull DarkClusterVerifierManager verifierManager)
  {
    _clusterInfoProvider = clusterInfoProvider;
    _sourceClusterName = sourceClusterName;
    _notifier = notifier;
    _darkStrategyMap = new ConcurrentHashMap<>();
    _random = random;
    _darkClusterDispatcher = darkClusterDispatcher;
    _verifierManager = verifierManager;
    _clusterListener = new DarkClusterListener();

    _clusterInfoProvider.registerClusterListener(_clusterListener);
  }

  @Override
  public DarkClusterStrategy getOrCreate(@Nonnull String darkClusterName, @Nonnull DarkClusterConfig darkClusterConfig)
  {
    if (!_darkStrategyMap.containsKey(darkClusterName))
    {
      _darkStrategyMap.putIfAbsent(darkClusterName, new AtomicReference<>(createStrategy(darkClusterName, darkClusterConfig)));
    }
    // it's theoretically possible for the Listener to remove the entry after the containsKey but before we retrieve it. Rather
    // than adding synchronization between accessors of _darkStrategyMap, we will make each put or get resilient
    //return _darkStrategyMap.get(darkClusterName).get();
    return _darkStrategyMap.getOrDefault(darkClusterName, new AtomicReference<DarkClusterStrategy>(new NoOpDarkClusterStrategy())).get();
  }

  /**
   * In the future, additional strategies can be added, and the logic here can choose the appropriate one based on the config values.
   */
  private DarkClusterStrategy createStrategy(String darkClusterName, DarkClusterConfig darkClusterConfig)
  {
    if (darkClusterConfig.hasMultiplier() && darkClusterConfig.getMultiplier() > 0)
    {
      BaseDarkClusterDispatcher baseDarkClusterDispatcher = new BaseDarkClusterDispatcherImpl(darkClusterName, _darkClusterDispatcher,
                                                                                              _notifier, _verifierManager);
      return new ConstantMultiplierDarkClusterStrategy(_sourceClusterName, darkClusterName, darkClusterConfig.getMultiplier(),
                                                       baseDarkClusterDispatcher, _notifier, _random);
    }
    else
    {
      return new NoOpDarkClusterStrategy();
    }
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
          DarkClusterConfigMap darkConfigMap = _clusterInfoProvider.getDarkClusterConfigMap(_sourceClusterName);
          if (darkConfigMap.containsKey(darkClusterName))
          {
            // just update the dark cluster that changed
            _darkStrategyMap.put(darkClusterName, new AtomicReference<>(createStrategy(darkClusterName,
                                                                                          darkConfigMap.get(darkClusterName))));
          }
        }
        catch (ServiceUnavailableException e)
        {
          _notifier.notify(() -> new RuntimeException("PEGA_0019 unable to refresh DarkClusterConfigMap for source cluster: " + _sourceClusterName + ", darkClusterName: " + darkClusterName));
        }
      }
    }

    @Override
    public void onClusterRemoved(String clusterName)
    {
      _darkStrategyMap.remove(clusterName);
    }
  }
}
