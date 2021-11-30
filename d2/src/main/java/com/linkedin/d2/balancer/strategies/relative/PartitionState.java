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

package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.common.callback.Callbacks;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.PartitionStateUpdateListener;
import com.linkedin.d2.balancer.strategies.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.strategies.RingFactory;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheck;
import com.linkedin.util.degrader.CallTracker;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Each {@link PartitionState} corresponds to a partition for a particular service in the relative load balancer
 * It keeps the tracker clients and the ring of the partition
 */
public class PartitionState
{
  private static final long INITIAL_CLUSTER_GENERATION_ID = -1;
  private static final int LOG_SIZE_LIMIT = 10;
  private int _partitionId;
  private int _pointsPerWeight;
  private RingFactory<URI> _ringFactory;
  private List<PartitionStateUpdateListener<PartitionState>> _listeners;
  private Set<TrackerClient> _recoveryTrackerClients;
  private long _clusterGenerationId;
  private Map<TrackerClient, LoadBalancerQuarantine> _quarantineMap;
  private Map<TrackerClient, LoadBalancerQuarantine> _quarantineHistory;
  private Map<TrackerClient, HealthCheck> _healthCheckMap;
  private Map<URI, Integer> _pointsMap;
  private Ring<URI> _ring;
  private Map<TrackerClient, TrackerClientState> _trackerClientStateMap;
  private PartitionStats _partitionStats;

  public PartitionState(int partitionId, RingFactory<URI> ringFactory, int pointsPerWeight,
      List<PartitionStateUpdateListener<PartitionState>> listeners)
  {
    this(partitionId, ringFactory, pointsPerWeight, new HashSet<>(), INITIAL_CLUSTER_GENERATION_ID,
        new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), listeners);
  }

  PartitionState(int partitionId, RingFactory<URI> ringFactory, int pointsPerWeight,
      Set<TrackerClient> recoveryTrackerClients, long clusterGenerationId,
      Map<TrackerClient, LoadBalancerQuarantine> quarantineMap,
      Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory,
      Map<TrackerClient, HealthCheck> healthCheckMap,
      Map<TrackerClient, TrackerClientState> trackerClientStateMap,
      List<PartitionStateUpdateListener<PartitionState>> listeners)
  {
    _partitionId = partitionId;
    _ringFactory = ringFactory;
    _pointsPerWeight = pointsPerWeight;
    _recoveryTrackerClients = recoveryTrackerClients;
    _clusterGenerationId = clusterGenerationId;
    _quarantineMap = quarantineMap;
    _quarantineHistory = quarantineHistory;
    _healthCheckMap = healthCheckMap;
    _trackerClientStateMap = trackerClientStateMap;
    _listeners = listeners;
    updateRing();
  }

  PartitionState (PartitionState oldPartitionState)
  {
    this(oldPartitionState.getPartitionId(),
        oldPartitionState.getRingFactory(),
        oldPartitionState.getPointsPerWeight(),
        new HashSet<>(oldPartitionState.getRecoveryTrackerClients()),
        oldPartitionState.getClusterGenerationId(),
        new HashMap<>(oldPartitionState.getQuarantineMap()),
        new HashMap<>(oldPartitionState.getQuarantineHistory()),
        new HashMap<>(oldPartitionState.getHealthCheckMap()),
        new HashMap<>(oldPartitionState.getTrackerClientStateMap()),
        oldPartitionState.getListeners());
  }

  int getPartitionId()
  {
    return _partitionId;
  }

  long getClusterGenerationId()
  {
    return _clusterGenerationId;
  }

  public Map<TrackerClient, TrackerClientState> getTrackerClientStateMap()
  {
    return _trackerClientStateMap;
  }

  Set<TrackerClient> getTrackerClients()
  {
    return _trackerClientStateMap.keySet();
  }

  public Map<TrackerClient, LoadBalancerQuarantine> getQuarantineMap()
  {
    return _quarantineMap;
  }

  Map<TrackerClient, LoadBalancerQuarantine> getQuarantineHistory()
  {
    return _quarantineHistory;
  }

  Map<TrackerClient, HealthCheck> getHealthCheckMap()
  {
    return _healthCheckMap;
  }

  Set<TrackerClient> getRecoveryTrackerClients()
  {
    return _recoveryTrackerClients;
  }

  RingFactory<URI> getRingFactory()
  {
    return _ringFactory;
  }

  Ring<URI> getRing()
  {
    return _ring;
  }

  void setClusterGenerationId(long clusterGenerationId)
  {
    _clusterGenerationId = clusterGenerationId;
  }

  public Map<URI, Integer> getPointsMap()
  {
    return _pointsMap;
  }

  /**
   * Update the hash ring using the latest tracker clients and points map
   */
  void updateRing()
  {
    Set<TrackerClient> trackerClients = _trackerClientStateMap.keySet();
    Map<URI, CallTracker> callTrackerMap = Collections.unmodifiableMap(trackerClients.stream()
        .collect(Collectors.toMap(TrackerClient::getUri, TrackerClient::getCallTracker)));
    _pointsMap = _trackerClientStateMap.entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().getUri(),
            entry -> (int) Math.round(entry.getValue().getHealthScore()
                * entry.getKey().getPartitionWeight(_partitionId)
                * entry.getKey().getSubsetWeight(_partitionId)
                * _pointsPerWeight)));
    _ring = _ringFactory.createRing(_pointsMap, callTrackerMap);
  }

  void setPartitionStats(double avgClusterLatency, long clusterCallCount, long clusterErrorCount)
  {
    _partitionStats = new PartitionStats(avgClusterLatency, clusterCallCount, clusterErrorCount);
  }

  PartitionStats getPartitionStats()
  {
    return _partitionStats;
  }

  List<PartitionStateUpdateListener<PartitionState>> getListeners()
  {
    return _listeners;
  }

  void removeTrackerClient(TrackerClient trackerClient)
  {
    _trackerClientStateMap.remove(trackerClient);
    _quarantineMap.remove(trackerClient);
    _quarantineHistory.remove(trackerClient);
    _healthCheckMap.remove(trackerClient);
    _recoveryTrackerClients.remove(trackerClient);
    trackerClient.shutdown(Callbacks.empty());
  }

  int getPointsPerWeight()
  {
    return _pointsPerWeight;
  }

  @Override
  public String toString()
  {
    return "PartitionRelativeLoadBalancerState={" + "_partitionId=" + _partitionId
        + ", _clusterGenerationId=" + _clusterGenerationId
        + ", _numHostsInCluster=" + (getTrackerClients().size())
        + ", _partitionStats={" + _partitionStats + "}"
        + ", _recoveryTrackerClients={" + _recoveryTrackerClients
            .stream().limit(LOG_SIZE_LIMIT).map(client -> client.getUri().toString()).collect(Collectors.joining(","))
        + (_recoveryTrackerClients.size() > LOG_SIZE_LIMIT ? "...(total " + _recoveryTrackerClients.size() + ")" : "") + "}"
        + ", _quarantineMap={" + _quarantineMap.keySet()
            .stream().limit(LOG_SIZE_LIMIT).map(client -> client.getUri().toString()).collect(Collectors.joining(","))
        + (_quarantineMap.size() > LOG_SIZE_LIMIT ? "...(total " + _quarantineMap.size() + ")" : "") + "}}";
  }

  class PartitionStats
  {
    private final double _avgClusterLatency;
    private final long _clusterCallCount;
    private final long _clusterErrorCount;

    PartitionStats(double avgClusterLatency, long clusterCallCount, long clusterErrorCount)
    {
      _avgClusterLatency = avgClusterLatency;
      _clusterCallCount = clusterCallCount;
      _clusterErrorCount = clusterErrorCount;
    }

    double getAvgClusterLatency()
    {
      return _avgClusterLatency;
    }

    long getClusterCallCount()
    {
      return _clusterCallCount;
    }

    long getClusterErrorCount()
    {
      return _clusterErrorCount;
    }

    public String toString()
    {
      return "_avgClusterLatency=" + _avgClusterLatency
          +", _clusterCallCount=" + _clusterCallCount
          +", _clusterErrorCount= " + _clusterErrorCount;
    }
  }
}
