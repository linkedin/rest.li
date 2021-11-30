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

package com.linkedin.d2.balancer.event;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerQuarantine;
import com.linkedin.util.clock.Clock;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * D2MonitorEventEmitter is responsible for building up the appropriate D2Monitor event, and sends it out through
 * EventEmitter interface. To keep the total data volume under control, the following measures are taken:
 *
 * 1. Choose the right hosts/URI: D2Monitor contains all unhealthy clients and limited number of healthy clients for
 *    reference.
 *
 * 2. Users can define custom data emission interval.
 *
 * This class keeps track of last emitting timeStamp and therefore is stateful.
 */
public class D2MonitorEventEmitter
{
  public static final int MAX_HEALTHY_HOSTS_TO_EMIT = 2;

  private final int _partitionId;
  private final String _clusterName;
  private final String _serviceName;
  private final Clock _clock;
  private final EventEmitter _eventEmitter;
  private final long _emittingInterval;
  private final int _pointsPerWeight;

  private long _lastEmittingTimeStamp;

  public D2MonitorEventEmitter(String clusterName,
                               String serviceName,
                               int partitionId,
                               Clock clock,
                               EventEmitter eventEmitter,
                               long emittingInterval,
                               int pointsPerWeight)
  {
    _partitionId = partitionId;
    _lastEmittingTimeStamp = clock.currentTimeMillis();
    _clusterName = clusterName;
    _serviceName = serviceName;
    _clock = clock;
    _eventEmitter = eventEmitter;
    _emittingInterval = emittingInterval;
    _pointsPerWeight = pointsPerWeight;
  }

  /**
   * Emits D2 event if allowed.
   *
   * @param clusterStatsProvider
   */
  public void emitEvent(ClusterStatsProvider clusterStatsProvider)
  {
    D2MonitorBuilder builder = new D2MonitorBuilder(_serviceName, _clusterName, _partitionId);
    D2MonitorBuilder.D2MonitorClusterStatsBuilder clusterStatsBuilder = builder.getClusterStatsBuilder();

    // 1. Set cluster metrics
    clusterStatsBuilder.setClusterNumHosts(clusterStatsProvider._trackerClients.size())
        .setClusterCurrentCallCount(clusterStatsProvider._clusterCallCount)
        .setClusterCurrentAverageLatencyMs(clusterStatsProvider._averageLatencyMs)
        .setClusterCurrentDroppedCalls(clusterStatsProvider._droppedCalls)
        .setClusterCurrentErrorCount(clusterStatsProvider._errorCount)
        .setClusterDropLevel(clusterStatsProvider._dropLevel);

    long currentTime = _clock.currentTimeMillis();
    long intervalMs =  currentTime - _lastEmittingTimeStamp;

    if (allowedToEmit(intervalMs))
    {
      // 2. build up D2MonitorEvent with appropriate uris from the trackerClients
      createD2MonitorEvent(clusterStatsProvider._trackerClients, builder, clusterStatsProvider._pointsMap, clusterStatsProvider._quarantineMap);

      // 3. emit the event
      _eventEmitter.emitEvent(builder.build(intervalMs));

      // 4. update the timeStamp
      _lastEmittingTimeStamp = currentTime;
    }
  }

  private boolean allowedToEmit(long intervalMs)
  {
    return intervalMs >= _emittingInterval;
  }

  private boolean isClientHealthy(TrackerClient trackerClient, final Map<URI, Integer> pointsMap)
  {
    int perfectHealth = (int) (trackerClient.getPartitionWeight(_partitionId) * trackerClient.getSubsetWeight(_partitionId) * _pointsPerWeight);
    return pointsMap.get(trackerClient.getUri()) >= perfectHealth;
  }

  /**
   * Add client info to {@link D2MonitorBuilder}.
   *
   * Unhealthy clients are always added, but only a certain number of
   * healthy clients are added
   */
  private void createD2MonitorEvent(Set<TrackerClient> trackerClients,
                                    D2MonitorBuilder d2MonitorBuilder,
                                    Map<URI, Integer> pointsMap,
                                    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap)
  {
    List<TrackerClient> healthyClients = new ArrayList<>();

    for (TrackerClient client : trackerClients)
    {
      if (!pointsMap.containsKey(client.getUri()))
      {
        continue;
      }
      if (isClientHealthy(client, pointsMap))
      {
        healthyClients.add(client);
      }
      else
      {
        d2MonitorBuilder.addUriInfoBuilder(client.getUri(), createUriInfoBuilder(client, pointsMap, quarantineMap));
      }
    }

    if (!healthyClients.isEmpty())
    {
      addRandomClientsToUriInfo(healthyClients, d2MonitorBuilder, pointsMap, quarantineMap);
    }
  }

  /**
   * Randomly pick healthy clients and add them to the event.
   */
  private void addRandomClientsToUriInfo(List<TrackerClient> healthyClients,
                                         D2MonitorBuilder builder,
                                         Map<URI, Integer> pointsMap,
                                         Map<TrackerClient, LoadBalancerQuarantine> quarantineMap)
  {
    // The operation is equivalent to shuffle + limit, but we do not have to shuffle the whole list since
    // the number of entries to add is generally much less than the size of health clients.
    Random random = new Random();
    for (int i = 0; i < Math.min(MAX_HEALTHY_HOSTS_TO_EMIT, healthyClients.size()); ++i)
    {
      Collections.swap(healthyClients, i, random.nextInt(healthyClients.size() - i) + i);
      TrackerClient nextClient = healthyClients.get(i);
      builder.addUriInfoBuilder(nextClient.getUri(), createUriInfoBuilder(nextClient, pointsMap, quarantineMap));
    }
  }

  // Create UriInfoBuilder from corresponding TrackerClient
  private D2MonitorBuilder.D2MonitorUriInfoBuilder createUriInfoBuilder(TrackerClient client,
                                                                        Map<URI, Integer> pointsMap,
                                                                        Map<TrackerClient, LoadBalancerQuarantine> quarantineMap)
  {
    D2MonitorBuilder.D2MonitorUriInfoBuilder uriInfoBuilder =
        new D2MonitorBuilder.D2MonitorUriInfoBuilder(client.getUri());
    uriInfoBuilder.copyStats(client.getLatestCallStats());
    uriInfoBuilder.setTransmissionPoints(pointsMap.get(client.getUri()));
    LoadBalancerQuarantine quarantine = quarantineMap.get(client);
    if (quarantine != null)
    {
      uriInfoBuilder.setQuarantineDuration(quarantine.getTimeTilNextCheck());
    }
    return uriInfoBuilder;
  }

  public static class ClusterStatsProvider
  {
    private final Map<URI, Integer> _pointsMap;
    private final Map<TrackerClient, LoadBalancerQuarantine> _quarantineMap;
    private final Set<TrackerClient> _trackerClients;
    private final long _clusterCallCount;
    private final double _averageLatencyMs;
    private final long _droppedCalls;
    private final long _errorCount;
    private final double _dropLevel;

    public ClusterStatsProvider(Map<URI, Integer> pointsMap, Map<TrackerClient, LoadBalancerQuarantine> quarantineMap,
                                Set<TrackerClient> trackerClients, long clusterCallCount, double averageLatencyMs, long droppedCalls, long errorCount,
                                double dropLevel)
    {
      _pointsMap = pointsMap;
      _quarantineMap = quarantineMap;
      _trackerClients = trackerClients;
      _clusterCallCount = clusterCallCount;
      _averageLatencyMs = averageLatencyMs;
      _droppedCalls = droppedCalls;
      _errorCount = errorCount;
      _dropLevel = dropLevel;
    }
  }
}
