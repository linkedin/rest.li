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
package com.linkedin.d2.balancer.clients;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.Degrader;
import com.linkedin.util.degrader.DegraderControl;
import com.linkedin.util.degrader.DegraderImpl;

/**
 * Used by {@link com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3}
 * with added {@link Degrader} logic.
 */
public class DegraderTrackerClientImpl extends TrackerClientImpl implements DegraderTrackerClient
{

  private final Map<Integer, PartitionState> _partitionStates;

  public DegraderTrackerClientImpl(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient)
  {
    this(uri, partitionDataMap, wrappedClient, SystemClock.instance(), null,
         TrackerClientImpl.DEFAULT_CALL_TRACKER_INTERVAL, DEFAULT_ERROR_STATUS_PATTERN);
  }

  public DegraderTrackerClientImpl(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                               Clock clock, DegraderImpl.Config config)
  {
    this(uri, partitionDataMap, wrappedClient, clock, config, TrackerClientImpl.DEFAULT_CALL_TRACKER_INTERVAL,
         TrackerClientImpl.DEFAULT_ERROR_STATUS_PATTERN, Collections.emptyMap());
  }

  public DegraderTrackerClientImpl(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                               Clock clock, DegraderImpl.Config config, long interval, Pattern errorStatusPattern)
  {
    this(uri, partitionDataMap, wrappedClient, clock, config, interval, errorStatusPattern, null);
  }

  public DegraderTrackerClientImpl(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                               Clock clock, DegraderImpl.Config config, long interval, Pattern errorStatusPattern,
                               Map<String, Object> uriSpecificProperties)
  {
    super(uri, partitionDataMap, wrappedClient, clock, interval, errorStatusPattern);

    if (config == null)
    {
      config = new DegraderImpl.Config();
    }

    config.setCallTracker(_callTracker);
    config.setClock(clock);
    // The overrideDropRate will be globally determined by the DegraderLoadBalancerStrategy.
    config.setOverrideDropRate(0.0);

    if (uriSpecificProperties == null)
    {
      uriSpecificProperties = new HashMap<>();
    }
    if (uriSpecificProperties.containsKey(PropertyKeys.DO_NOT_SLOW_START)
      && Boolean.parseBoolean(uriSpecificProperties.get(PropertyKeys.DO_NOT_SLOW_START).toString()))
    {
      config.setInitialDropRate(DegraderImpl.DEFAULT_DO_NOT_SLOW_START_INITIAL_DROP_RATE);
    }

    /* TrackerClient contains state for each partition, but they actually share the same DegraderImpl
     *
     * There used to be a deadlock if each partition has its own DegraderImpl:
     * getStats() and rolloverStats() in DegraderImpl are both synchronized. getstats() will check whether
     * the state is stale, and if yes a rollover event will be delivered which will call rolloverStats() in all
     * DegraderImpl within this CallTracker. Therefore, when multiple threads are calling getStats() simultaneously,
     * one thread may try to grab a lock which is already acquired by another.
     *
     * An example:
     * Suppose we have two threads, and here is the execution sequence:
     * 1. Thread 1 (DegraderImpl 1): grab its lock, enter getStats()
     * 2. Thread 2 (DegraderImpl 2): grab its lock, enter getStats()
     * 3. Thread 1: PendingEvent is delivered to all registered StatsRolloverEventListener, so it will call rolloverStats()
     *    in both DegraderImpl 1 and DegraderImpl 2. But the lock of DegraderImpl 2 has already been acquired by thread 2
     * 4. Same happens for thread 2. Deadlock.
     *
     * Solution:
     * Currently all DegraderImpl within the same CallTracker actually share exactly the same information,
     * so we just use create one instance of DegraderImpl, and use it for all partitions.
     *
     * Pros and Cons:
     * Deadlocks will be gone since there will be only one DegraderImpl.
     * However, now it becomes harder to have different configurations for different partitions.
     */
    int mapSize = partitionDataMap.size();
    Map<Integer, PartitionState>partitionStates = new HashMap<Integer, PartitionState>(mapSize * 2);
    config.setName("TrackerClient Degrader: " + uri);
    DegraderImpl degrader = new DegraderImpl(config);
    DegraderControl degraderControl = new DegraderControl(degrader);
    for (Map.Entry<Integer, PartitionData> entry : partitionDataMap.entrySet())
    {
      int partitionId = entry.getKey();
      PartitionState partitionState = new PartitionState(entry.getValue(), degrader, degraderControl);
      partitionStates.put(partitionId, partitionState);
    }
    _partitionStates = Collections.unmodifiableMap(partitionStates);
  }

  @Override
  public Degrader getDegrader(int partitionId)
  {
    return getPartitionState(partitionId).getDegrader();
  }

  @Override
  public DegraderControl getDegraderControl(int partitionId)
  {

    return getPartitionState(partitionId).getDegraderControl();
  }

  private PartitionState getPartitionState(int partitionId)
  {
    PartitionState partitionState = _partitionStates.get(partitionId);
    if (partitionState == null)
    {
      String msg = "PartitionState does not exist for partitionId: " + partitionId + ". The current states are " + _partitionStates;
      throw new IllegalStateException(msg);
    }
    return partitionState;
  }

  private class PartitionState
  {
    private final Degrader _degrader;
    private final DegraderControl _degraderControl;
    private final PartitionData _partitionData;

    PartitionState(PartitionData partitionData, Degrader degrader, DegraderControl degraderControl)
    {
      _partitionData = partitionData;
      _degrader = degrader;
      _degraderControl = degraderControl;
    }

    Degrader getDegrader()
    {
      return _degrader;
    }

    DegraderControl getDegraderControl()
    {
      return _degraderControl;
    }

    PartitionData getPartitionData()
    {
      return _partitionData;
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      sb.append("{_partitionData = ");
      sb.append(_partitionData);
      sb.append(", _degrader = " + _degrader);
      sb.append(", degraderMinCallCount = " + _degraderControl.getMinCallCount());
      sb.append("}");
      return sb.toString();
    }
  }
}
