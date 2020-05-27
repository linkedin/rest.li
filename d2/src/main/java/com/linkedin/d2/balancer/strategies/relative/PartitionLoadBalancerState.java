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
import com.linkedin.d2.balancer.strategies.degrader.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.util.hashing.Ring;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Each partitionLoadBalancerState corresponds to a partition for a particular service
 * It keeps the tracker clients and the ring for the partition
 */
public class PartitionLoadBalancerState
{
  private final int _partitionId;
  private final Lock _lock;
  private Map<URI, Integer> _pointsMap;
  private Map<TrackerClient, Double> _recoveryMap;
  private long _clusterGenerationId;
  // TODO: copied from old files, need to re-evaluate
  private Map<TrackerClient, LoadBalancerQuarantine> _quarantineMap;
  private Map<TrackerClient, LoadBalancerQuarantine> _quarantineHistory;
  private Ring<URI> _ring;
  private ConcurrentMap<TrackerClient, TrackerClientState> _trackerClientStateMap;

  public PartitionLoadBalancerState(int partitionId)
  {
    _partitionId = partitionId;
    _lock = new ReentrantLock();
    _clusterGenerationId = -1;

    _pointsMap = new HashMap<>();
    _recoveryMap = new HashMap<>();

  }

  public Lock getLock()
  {
    return _lock;
  }

  public long getClusterGenerationId() {
    return _clusterGenerationId;
  }

  public ConcurrentMap<TrackerClient, TrackerClientState> getTrackerClientStateMap()
  {
    return _trackerClientStateMap;
  }

  public Set<TrackerClient> getTrackerClients()
  {
    return _trackerClientStateMap.keySet();
  }

  public void setClusterGenerationId(long clusterGenerationId)
  {
    _clusterGenerationId = clusterGenerationId;
  }
}
