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

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.transport.http.client.ConstantQpsRateLimiter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfig;

import java.util.function.Supplier;
import javax.annotation.Nonnull;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.DarkClusterStrategyNameArray;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
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

  // Map of partition ID to dark cluster name to list of dark cluster strategies for that partition
  private final Map<Integer, Map<String, DarkClusterStrategy>> _partitionToDarkStrategyMap;
  private volatile boolean _sourceClusterPresent = false;
  private final Random _random;
  private final LoadBalancerClusterListener _clusterListener;
  private final DarkClusterVerifierManager _verifierManager;
  private final Supplier<ConstantQpsRateLimiter> _rateLimiterSupplier;

  public DarkClusterStrategyFactoryImpl(@Nonnull Facilities facilities,
                                        @Nonnull String sourceClusterName,
                                        @Nonnull DarkClusterDispatcher darkClusterDispatcher,
                                        @Nonnull Notifier notifier,
                                        @Nonnull Random random,
                                        @Nonnull DarkClusterVerifierManager verifierManager,
                                        Supplier<ConstantQpsRateLimiter> rateLimiterSupplier)
  {
    _facilities = facilities;
    _sourceClusterName = sourceClusterName;
    _notifier = notifier;
    _partitionToDarkStrategyMap = new ConcurrentHashMap<>();
    _random = random;
    _darkClusterDispatcher = darkClusterDispatcher;
    _verifierManager = verifierManager;
    _rateLimiterSupplier = rateLimiterSupplier;
    _clusterListener = new DarkClusterListener();
  }

  public DarkClusterStrategyFactoryImpl(@Nonnull Facilities facilities,
                                        @Nonnull String sourceClusterName,
                                        @Nonnull DarkClusterDispatcher darkClusterDispatcher,
                                        @Nonnull Notifier notifier,
                                        @Nonnull Random random,
                                        @Nonnull DarkClusterVerifierManager verifierManager)
  {
    this(facilities, sourceClusterName, darkClusterDispatcher, notifier, random, verifierManager, (Supplier<ConstantQpsRateLimiter>) null);
  }

  /**
   * Deprecated. Please pass a Supplier<ConstantQpsRateLimiter> instead of ConstantQpsRateLimiter
   */
  @Deprecated
  public DarkClusterStrategyFactoryImpl(@Nonnull Facilities facilities,
      @Nonnull String sourceClusterName,
      @Nonnull DarkClusterDispatcher darkClusterDispatcher,
      @Nonnull Notifier notifier,
      @Nonnull Random random,
      @Nonnull DarkClusterVerifierManager verifierManager,
      @Nonnull ConstantQpsRateLimiter rateLimiter)
  {
    this(facilities, sourceClusterName, darkClusterDispatcher, notifier, random, verifierManager, () -> rateLimiter);
  }

  @Override
  public void start()
  {
    // make sure we're listening to the source cluster and have strategies for any
    // associated dark clusters. While registering the cluster listener is enough,
    // we also "warm up" the strategies directly by triggering the clusterListener so that
    // we retrieve the dark clusters before any inbound request.
    _facilities.getClusterInfoProvider().registerClusterListener(_clusterListener);
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
  public DarkClusterStrategy get(@Nonnull String darkClusterName, int partitionId)
  {
    Map<String, DarkClusterStrategy> darkMap = _partitionToDarkStrategyMap.computeIfAbsent(partitionId, k -> new ConcurrentHashMap<>());
    DarkClusterStrategy existing = darkMap.get(darkClusterName);
    if (existing != null)
    {
      return existing;
    }

    if (!_sourceClusterPresent)
    {
      return NO_OP_DARK_CLUSTER_STRATEGY;
    }

    try
    {
      DarkClusterConfigMap darkClusterConfigMap = _facilities.getClusterInfoProvider().getDarkClusterConfigMap(_sourceClusterName);
      if (darkClusterConfigMap != null && darkClusterConfigMap.containsKey(darkClusterName))
      {
        DarkClusterConfig config = darkClusterConfigMap.get(darkClusterName);
        DarkClusterStrategy strategy = createStrategy(darkClusterName, config, partitionId);
        darkMap.put(darkClusterName, strategy);
        return strategy;
      }
    }
    catch (Throwable t)
    {
      // fall-through to NO_OP
    }
    return NO_OP_DARK_CLUSTER_STRATEGY;
  }

  /**
   * In the future, additional strategies can be added, and the logic here can choose the appropriate one based on the config values.
   */
  private DarkClusterStrategy createStrategy(String darkClusterName, DarkClusterConfig darkClusterConfig, int partitionId)
  {
    // Create partition-aware ClusterInfoProvider that filters cluster information for this specific partition
    com.linkedin.d2.balancer.util.ClusterInfoProvider partitionAwareProvider = 
        new PartitionAwareClusterInfoProvider(_facilities.getClusterInfoProvider(), partitionId);
        
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
              return new RelativeTrafficMultiplierDarkClusterStrategy(_sourceClusterName, darkClusterName,
                                                                      darkClusterConfig.getMultiplier(), baseDarkClusterDispatcher,
                                                                      _notifier, partitionAwareProvider,
                                                                      _random);
            }
            break;
          case IDENTICAL_TRAFFIC:
            if (IdenticalTrafficMultiplierDarkClusterStrategy.isValidConfig(darkClusterConfig))
            {
              BaseDarkClusterDispatcher baseDarkClusterDispatcher =
                  new BaseDarkClusterDispatcherImpl(darkClusterName, _darkClusterDispatcher, _notifier, _verifierManager);
              return new IdenticalTrafficMultiplierDarkClusterStrategy(_sourceClusterName, darkClusterName,
                                                                       darkClusterConfig.getMultiplier(), baseDarkClusterDispatcher,
                                                                       _notifier, partitionAwareProvider,
                                                                       _random);
            }
            break;
          case CONSTANT_QPS:
            if (_rateLimiterSupplier == null)
            {
              LOG.error("Dark Cluster {} configured to use CONSTANT_QPS strategy, but no rate limiter provided during instantiation. "
                  + "No Dark Cluster strategy will be used!", darkClusterName);
              break;
            }
            if (ConstantQpsDarkClusterStrategy.isValidConfig(darkClusterConfig))
            {
              BaseDarkClusterDispatcher baseDarkClusterDispatcher =
                  new BaseDarkClusterDispatcherImpl(darkClusterName, _darkClusterDispatcher, _notifier, _verifierManager);
              return new ConstantQpsDarkClusterStrategy(_sourceClusterName, darkClusterName,
                  darkClusterConfig.getDispatcherOutboundTargetRate(), baseDarkClusterDispatcher,
                  _notifier, partitionAwareProvider,
                  _rateLimiterSupplier, darkClusterConfig.getDispatcherMaxRequestsToBuffer(),
                  darkClusterConfig.getDispatcherBufferedRequestExpiryInSeconds());
            }
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
        _sourceClusterPresent = true;
        _facilities.getClusterInfoProvider().getDarkClusterConfigMap(_sourceClusterName, new Callback<DarkClusterConfigMap>()
        {
          @Override
          public void onError(Throwable e)
          {
            _notifier.notify(() -> new RuntimeException("PEGA_0019 unable to refresh DarkClusterConfigMap for source cluster: "
                    + _sourceClusterName, e));
          }

          @Override
          public void onSuccess(DarkClusterConfigMap updatedDarkConfigMap)
          {
            // Determine partitions to (re)build. If none exist yet, ensure default partition is initialized.
            java.util.Set<Integer> partitions = new java.util.HashSet<>(_partitionToDarkStrategyMap.keySet());
            if (partitions.isEmpty())
            {
              partitions.add(com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
            }

            for (int partitionId : partitions)
            {
              Map<String, DarkClusterStrategy> darkStrategyMap = new ConcurrentHashMap<>();
              for (Map.Entry<String, DarkClusterConfig> entry : updatedDarkConfigMap.entrySet())
              {
                String darkClusterToAdd = entry.getKey();
                darkStrategyMap.put(darkClusterToAdd, createStrategy(darkClusterToAdd, entry.getValue(), partitionId));
                LOG.info("Created new strategy for dark cluster: " + darkClusterToAdd + ", partition: " + partitionId + ", source cluster: " + _sourceClusterName);
              }
              _partitionToDarkStrategyMap.put(partitionId, darkStrategyMap);
            }
          }
        });
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
        _partitionToDarkStrategyMap.clear();
        _sourceClusterPresent = false;
      }
    }
  }

  /**
   * Partition-aware wrapper around a {@link ClusterInfoProvider} that filters cluster information
   * for a specific partition before forwarding to the strategies.
   */
  private static final class PartitionAwareClusterInfoProvider implements com.linkedin.d2.balancer.util.ClusterInfoProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PartitionAwareClusterInfoProvider.class);
    private final com.linkedin.d2.balancer.util.ClusterInfoProvider _delegate;
    private final int _partitionId;

    PartitionAwareClusterInfoProvider(com.linkedin.d2.balancer.util.ClusterInfoProvider delegate, int partitionId) {
      _delegate = delegate;
      _partitionId = partitionId;
    }

    @Override
    public int getHttpsClusterCount(String clusterName) throws ServiceUnavailableException {
      return getClusterCount(clusterName, PropertyKeys.HTTPS_SCHEME, _partitionId);
    }

    @Override
    public int getClusterCount(String clusterName, String scheme, int partitionId) throws ServiceUnavailableException {
      return _delegate.getClusterCount(clusterName, scheme, partitionId);
    }

    @Override
    public DarkClusterConfigMap getDarkClusterConfigMap(String clusterName) throws ServiceUnavailableException {
      return _delegate.getDarkClusterConfigMap(clusterName);
    }

    @Override
    public void getDarkClusterConfigMap(String clusterName, Callback<DarkClusterConfigMap> callback) {
      _delegate.getDarkClusterConfigMap(clusterName, callback);
    }

    @Override
    public void registerClusterListener(LoadBalancerClusterListener clusterListener) {
      _delegate.registerClusterListener(clusterListener);
    }

    @Override
    public void unregisterClusterListener(LoadBalancerClusterListener clusterListener) {
      _delegate.unregisterClusterListener(clusterListener);
    }

    @Override
    public FailoutConfig getFailoutConfig(String clusterName) {
      return _delegate.getFailoutConfig(clusterName);
    }
  }
}
