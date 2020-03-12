package com.linkedin.darkcluster.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.NoOpDarkClusterStrategyImpl;

/**
 * DarkClusterStrategyFactoryImpl creates and maintains the strategies needed for dark clusters. This involves refreshing
 * when darkClusterConfig changes are detected. The complexity of listening for event changes is hidden by ClusterInfoProvider, so a simple
 * time and change based mechanism is used here to periodically refresh the list.
 */
public class DarkClusterStrategyFactoryImpl implements DarkClusterStrategyFactory
{
  private final ClusterInfoProvider _clusterInfoProvider;
  private final String _clusterName;
  private final BaseDarkClusterDispatcher _baseDarkClusterDispatcher;
  private final Notifier _notifier;
  Map<String, AtomicReference<DarkClusterStrategy>> _darkStrategyMap;
  private final ExecutorService _executorService;
  private final Random _random;

  public DarkClusterStrategyFactoryImpl(@Nonnull ClusterInfoProvider clusterInfoProvider, @Nonnull String clusterName,
                                        @Nonnull BaseDarkClusterDispatcher baseDarkClusterDispatcher, @Nonnull Notifier notifier,
                                        @Nonnull ExecutorService executorService, @Nonnull Random random)
  {
    _clusterInfoProvider = clusterInfoProvider;
    _clusterName = clusterName;
    _baseDarkClusterDispatcher = baseDarkClusterDispatcher;
    _notifier = notifier;
    _darkStrategyMap = new HashMap<>();
    _executorService = executorService;
    _random = random;
  }

  public DarkClusterStrategy getOrCreate(@Nonnull String darkClusterName, @Nonnull DarkClusterConfig darkClusterConfig)
  {
    if (!_darkStrategyMap.containsKey(darkClusterName))
    {
      _darkStrategyMap.putIfAbsent(darkClusterName, new AtomicReference<>(createStrategy(darkClusterName, darkClusterConfig)));
    }
    if (strategyNeedsRefresh(darkClusterName, darkClusterConfig))
    {
      kickoffRefresh(darkClusterName, darkClusterConfig);
    }

    return _darkStrategyMap.get(darkClusterName).get();
  }

  private boolean strategyNeedsRefresh(String darkClusterName, DarkClusterConfig darkClusterConfig)
  {
    // compare timestamp of last refresh, and if time, if any values changed in multiplier related configs.
    // instance count changes don't count here as a change.
    return false;
  }

  private void kickoffRefresh(String darkClusterName, DarkClusterConfig darkClusterConfig)
  {
    // recreate the strategy, put onto executor.
  }

  private DarkClusterStrategy createStrategy(String darkClusterName, DarkClusterConfig darkClusterConfig)
  {
    if (darkClusterConfig.hasMultiplier() && darkClusterConfig.getMultiplier() > 0)
    {
      return new RelativeTrafficDarkCanaryStrategyImpl(_clusterName, darkClusterName, darkClusterConfig.getMultiplier(), _clusterInfoProvider,
                                                       _baseDarkClusterDispatcher, _notifier, _random);
    }
    else
    {
      return new NoOpDarkClusterStrategyImpl();
    }
  }
}
