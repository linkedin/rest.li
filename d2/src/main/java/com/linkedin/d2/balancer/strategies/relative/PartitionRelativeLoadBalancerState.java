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

package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.PartitionLoadBalancerStateListener;
import com.linkedin.d2.balancer.strategies.degrader.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.strategies.degrader.RingFactory;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.util.degrader.CallTracker;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


/**
 * Each partitionLoadBalancerState corresponds to a partition for a particular service
 * It keeps the tracker clients and the ring for the partition
 */
public class PartitionRelativeLoadBalancerState
{
  private final int _partitionId;
  private final Lock _lock;
  private final int _pointsPerWeight;
  private final RingFactory<URI> _ringFactory;
  private final List<PartitionLoadBalancerStateListener<PartitionRelativeLoadBalancerState>> _listeners;
  private Map<TrackerClient, Double> _recoveryMap;
  private long _clusterGenerationId;
  private Map<TrackerClient, LoadBalancerQuarantine> _quarantineMap;
  private Map<TrackerClient, LoadBalancerQuarantine> _quarantineHistory;
  private Map<URI, Integer> _pointsMap;
  private Ring<URI> _ring;
  private Map<TrackerClient, TrackerClientState> _trackerClientStateMap;
  private PartitionStats _partitionStats;

  public PartitionRelativeLoadBalancerState(int partitionId, RingFactory<URI> ringFactory, int pointsPerWeight,
      Lock lock, List<PartitionLoadBalancerStateListener<PartitionRelativeLoadBalancerState>> listeners)
  {
    _partitionId = partitionId;
    _lock = lock;
    _clusterGenerationId = -1;
    _ringFactory = ringFactory;
    _pointsPerWeight = pointsPerWeight;

    _recoveryMap = new HashMap<>();
    _quarantineMap = new HashMap<>();
    _quarantineHistory = new HashMap<>();
    _trackerClientStateMap = new HashMap<>();
    _listeners = listeners;
    resetRing();
  }

  private PartitionRelativeLoadBalancerState(int partitionId, Lock lock, RingFactory<URI> ringFactory, int pointsPerWeight,
      Map<TrackerClient, Double> recoveryMap, long clusterGenerationId,
      Map<TrackerClient, LoadBalancerQuarantine> quarantineMap,
      Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory,
      Map<TrackerClient, TrackerClientState> trackerClientStateMap,
      List<PartitionLoadBalancerStateListener<PartitionRelativeLoadBalancerState>> listeners)
  {
    _partitionId = partitionId;
    _lock = lock;
    _ringFactory = ringFactory;
    _pointsPerWeight = pointsPerWeight;
    _recoveryMap = recoveryMap;
    _clusterGenerationId = clusterGenerationId;
    _quarantineMap = quarantineMap;
    _quarantineHistory = quarantineHistory;
    _trackerClientStateMap = trackerClientStateMap;
    _listeners = listeners;
    resetRing();
  }

  public PartitionRelativeLoadBalancerState copy()
  {
    return new PartitionRelativeLoadBalancerState(this.getPartitionId(),
        this.getLock(),
        this.getRingFactory(),
        this.getPointsPerWeight(),
        new HashMap<>(this.getRecoveryMap()),
        this.getClusterGenerationId(),
        new HashMap<>(this.getQuarantineMap()),
        new HashMap<>(this.getQuarantineHistory()),
        new HashMap<>(this.getTrackerClientStateMap()),
        this.getListeners());
  }

  public Lock getLock()
  {
    return _lock;
  }

  public int getPartitionId()
  {
    return _partitionId;
  }

  public long getClusterGenerationId() {
    return _clusterGenerationId;
  }

  public Map<TrackerClient, TrackerClientState> getTrackerClientStateMap()
  {
    return _trackerClientStateMap;
  }

  public Set<TrackerClient> getTrackerClients()
  {
    return _trackerClientStateMap.keySet();
  }

  public Map<TrackerClient, LoadBalancerQuarantine> getQuarantineMap()
  {
    return _quarantineMap;
  }

  public Map<TrackerClient, LoadBalancerQuarantine> getQuarantineHistory()
  {
    return _quarantineHistory;
  }

  public Map<TrackerClient, Double> getRecoveryMap()
  {
    return _recoveryMap;
  }

  public RingFactory<URI> getRingFactory()
  {
    return _ringFactory;
  }

  public Ring<URI> getRing() {
    return _ring;
  }

  public void setClusterGenerationId(long clusterGenerationId)
  {
    _clusterGenerationId = clusterGenerationId;
  }

  public Map<URI, Integer> getPointsMap()
  {
    return _pointsMap;
  }

  public void resetRing()
  {
    Set<TrackerClient> trackerClients = _trackerClientStateMap.keySet();
    Map<URI, CallTracker> callTrackerMap = Collections.unmodifiableMap(trackerClients.stream()
        .collect(Collectors.toMap(TrackerClient::getUri, TrackerClient::getCallTracker)));
    _pointsMap = _trackerClientStateMap.entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().getUri(),
            entry -> (int) Math.round(entry.getValue().getHealthScore() * entry.getKey().getPartitionWeight(_partitionId) * _pointsPerWeight)));
    _ring = _ringFactory.createRing(_pointsMap, callTrackerMap);
  }

  public void setPartitionStats(double avgClusterLatency, long clusterCallCount, long clusterErrorCount)
  {
    _partitionStats = new PartitionStats(avgClusterLatency, clusterCallCount, clusterErrorCount);
  }

  public PartitionStats getPartitionStats()
  {
    return _partitionStats;
  }

  public List<PartitionLoadBalancerStateListener<PartitionRelativeLoadBalancerState>> getListeners()
  {
    return Collections.unmodifiableList(_listeners);
  }

  public void removeTrackerClient(TrackerClient trackerClient)
  {
    _trackerClientStateMap.remove(trackerClient);
    _quarantineMap.remove(trackerClient);
    _quarantineHistory.remove(trackerClient);
    _recoveryMap.remove(trackerClient);
  }

  int getPointsPerWeight()
  {
    return _pointsPerWeight;
  }

  @Override
  public String toString() {
    return "PartitionRelativeLoadBalancerState{" + "_partitionId=" + _partitionId
        + ", _clusterGenerationId=" + _clusterGenerationId
        + ", _recoveryMap=" + _recoveryMap + ", _quarantineMap=" + _quarantineMap
        + ", _pointsMap=" + _pointsMap + ", _ring=" + _ring + ", _trackerClientStateMap=" + _trackerClientStateMap
        + ", _partitionStats=" + _partitionStats + '}';
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

    double getAvgClusterLatency() {
      return _avgClusterLatency;
    }

    long getClusterCallCount() {
      return _clusterCallCount;
    }

    long getClusterErrorCount() {
      return _clusterErrorCount;
    }
  }
}
