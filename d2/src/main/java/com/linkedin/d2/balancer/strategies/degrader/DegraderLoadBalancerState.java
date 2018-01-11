/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheck;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


/**
 * A collection of Partition objects, one for each partition, lazily initialized.
 */
public class DegraderLoadBalancerState
{
  private final ConcurrentMap<Integer, Partition> _partitions;
  private final String _serviceName;
  private final Map<String, String> _degraderProperties;
  private final DegraderLoadBalancerStrategyConfig _config;
  private final AtomicBoolean _quarantineEnabled;
  private final AtomicInteger _quarantineRetries;
  // _healthCheckMap keeps track of HealthCheck clients associated with TrackerClientUpdater
  // It should only be accessed under the update lock.
  // Note: after quarantine is enabled, there is no need to send health checking requests to all
  // trackerClients anymore and we do not have to hold the healthCheck objects in healthCheckMap.
  // When individual trackerClient is quarantined, the corresponding healthCheck will be
  // generated again.
  private final ConcurrentMap<TrackerClientUpdater, HealthCheck> _healthCheckMap;
  private final List<PartitionDegraderLoadBalancerStateListener.Factory> _degraderStateListenerFactories;

  DegraderLoadBalancerState(String serviceName, Map<String, String> degraderProperties,
      DegraderLoadBalancerStrategyConfig config,
      List<PartitionDegraderLoadBalancerStateListener.Factory> degraderStateListenerFactories)
  {
    _degraderProperties = degraderProperties != null ? degraderProperties : Collections.<String, String>emptyMap();
    _partitions = new ConcurrentHashMap<Integer, Partition>();
    _serviceName = serviceName;
    _config = config;
    _degraderStateListenerFactories = degraderStateListenerFactories;
    _quarantineEnabled = new AtomicBoolean(false);
    _quarantineRetries = new AtomicInteger(0);
    _healthCheckMap = new ConcurrentHashMap<>();
  }

  public Partition getPartition(int partitionId)
  {
    Partition partition = _partitions.get(partitionId);
    if (partition == null)
    {
      // this is mainly executed in bootstrap time
      // after the system is stabilized, i.e. after all partitionIds have been seen,
      // there will be no need to initialize the map
      // Note that we do this trick because partition count is not available in
      // service configuration (it's in cluster configuration) and we do not want to
      // intermingle the two configurations
      Partition newValue = new Partition(partitionId,
          new ReentrantLock(),
          new PartitionDegraderLoadBalancerState
              (-1, _config.getClock().currentTimeMillis(), false,
                  new DegraderRingFactory<>(_config),
                  new HashMap<URI, Integer>(),
                  PartitionDegraderLoadBalancerState.Strategy.
                      LOAD_BALANCE,
                  0, 0,
                  new HashMap<TrackerClient, Double>(),
                  _serviceName, _degraderProperties,
                  0, 0, 0,
                  new HashMap<>(), new HashMap<>(),
                  null),
          _degraderStateListenerFactories.stream()
              .map(factory -> factory.create(partitionId, _config)).collect(Collectors.toList()));

      Partition oldValue = _partitions.putIfAbsent(partitionId, newValue);
      if (oldValue == null)
        partition = newValue;
      else // another thread already initialized this partition
        partition = oldValue; // newValue is discarded
    }
    return partition;
  }

  Ring<URI> getRing(int partitionId)
  {
    if (_partitions.get(partitionId) != null)
    {
      PartitionDegraderLoadBalancerState state = _partitions.get(partitionId).getState();
      return state.getRing();
    }
    else
    {
      return null;
    }
  }

  // this method never returns null
  public PartitionDegraderLoadBalancerState getPartitionState(int partitionId)
  {
    return getPartition(partitionId).getState();
  }

  void setPartitionState(int partitionId, PartitionDegraderLoadBalancerState newState)
  {
    getPartition(partitionId).setState(newState);
  }

  void putHealthCheckClient(TrackerClientUpdater updater, HealthCheck client)
  {
    _healthCheckMap.put(updater, client);
  }

  Map<TrackerClientUpdater, HealthCheck> getHealthCheckMap()
  {
     return _healthCheckMap;
  }

  String getServiceName()
  {
    return _serviceName;
  }

  boolean isQuarantineEnabled()
  {
    return _quarantineEnabled.get();
  }

  /**
   * Attempts to enables quarantine. Quarantine is enabled only if quarantine is not already enabled. Otherwise,
   * no side-effect is taken place.
   *
   * @return {@code true} if quarantine is not already enabled and is enabled as the result of this call;
   * {@code false} otherwise.
   */
  boolean tryEnableQuarantine()
  {
    return _quarantineEnabled.compareAndSet(false, true);
  }

  int incrementAndGetQuarantineRetries()
  {
    return _quarantineRetries.incrementAndGet();
  }

  public void shutdown(DegraderLoadBalancerStrategyConfig config)
  {
    // Need to shutdown quarantine and release the related transport client
    if (config.getQuarantineMaxPercent() <= 0.0 || !_quarantineEnabled.get())
    {
      return;
    }

    for (Partition par : _partitions.values())
    {
      Lock lock = par.getLock();
      lock.lock();

      try
      {
        PartitionDegraderLoadBalancerState curState = par.getState();
        curState.getQuarantineMap().values().forEach(DegraderLoadBalancerQuarantine::shutdown);
      }
      finally
      {
        lock.unlock();
      }
    }
  }

  @Override
  public String toString()
  {
    return "PartitionStates: [" + _partitions + "]";
  }
}
