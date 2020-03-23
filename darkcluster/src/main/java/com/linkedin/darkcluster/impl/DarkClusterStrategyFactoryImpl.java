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
                                        @Nonnull String clusterName,
                                        @Nonnull DarkClusterDispatcher darkClusterDispatcher,
                                        @Nonnull Notifier notifier,
                                        @Nonnull Random random,
                                        @Nonnull DarkClusterVerifierManager verifierManager)
  {
    _clusterInfoProvider = clusterInfoProvider;
    _sourceClusterName = clusterName;
    _notifier = notifier;
    _darkStrategyMap = new ConcurrentHashMap<>();
    _random = random;
    _darkClusterDispatcher = darkClusterDispatcher;
    _verifierManager = verifierManager;
    _clusterListener = new LoadBalancerClusterListener()
    {
      @Override
      public void onClusterAdded(String updatedClusterName)
      {
        if (_darkStrategyMap.containsKey(updatedClusterName))
        {
          // this is a dark cluster name. however, to refresh the strategies, we need to pull the
          // darkClusterConfigMap on the parent d2 cluster, because that has the properties needed
          // to recreate the dark cluster strategies, such as the multiplier.
          try
          {
            DarkClusterConfigMap darkConfigMap = _clusterInfoProvider.getDarkClusterConfigMap(_sourceClusterName);
            for (Map.Entry<String, DarkClusterConfig> entry : darkConfigMap.entrySet())
            {
              String darkClusterName = entry.getKey();
              // just update the cluster that's changed.
              if (darkClusterName.equals(updatedClusterName))
              {
                _darkStrategyMap.put(entry.getKey(), new AtomicReference<>(createStrategy(clusterName, entry.getValue())));
              }
            }

          }
          catch (ServiceUnavailableException e)
          {
            _notifier.notify(() -> new RuntimeException("unable to refresh DarkClusterConfigMap for source cluster: " + _sourceClusterName));
          }
        }
      }

      @Override
      public void onClusterRemoved(String clusterName)
      {
        _darkStrategyMap.remove(clusterName);
      }
    };

    _clusterInfoProvider.registerClusterListener(_clusterListener);
  }

  @Override
  public DarkClusterStrategy getOrCreate(@Nonnull String darkClusterName, @Nonnull DarkClusterConfig darkClusterConfig)
  {
    if (!_darkStrategyMap.containsKey(darkClusterName))
    {
      _darkStrategyMap.putIfAbsent(darkClusterName, new AtomicReference<>(createStrategy(darkClusterName, darkClusterConfig)));
    }
    return _darkStrategyMap.get(darkClusterName).get();
  }

  private DarkClusterStrategy createStrategy(String darkClusterName, DarkClusterConfig darkClusterConfig)
  {
    if (darkClusterConfig.hasMultiplier() && darkClusterConfig.getMultiplier() > 0)
    {
      // probably need to save a handle to this for metrics
      BaseDarkClusterDispatcher baseDarkClusterDispatcher = new BaseDarkClusterDispatcherImpl(darkClusterName, _darkClusterDispatcher,
                                                                                              _notifier, _verifierManager);
      return new RelativeTrafficDarkCanaryStrategyImpl(_sourceClusterName, darkClusterName, darkClusterConfig.getMultiplier(),
                                                       baseDarkClusterDispatcher, _notifier, _random);
    }
    else
    {
      return new NoOpDarkClusterStrategy();
    }
  }
}
