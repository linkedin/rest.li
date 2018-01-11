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
import com.linkedin.d2.balancer.event.D2MonitorBuilder;
import com.linkedin.d2.balancer.event.EventEmitter;
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
 * 2. Use different intervals to emit data: when all hosts are in health state, highEventEmittingInterval is used. If
 *    there're hosts in unhealthy state, we use lowEventEmittingInterval.
 *
 * This class keeps track of last emitting timeStamp therefore stateful.
 */
class D2MonitorEventEmitter implements PartitionDegraderLoadBalancerStateListener
{
  private final DegraderLoadBalancerStrategyConfig _config;
  private final int _partitionId;
  private final String _serviceName;
  private long _lastEmittingTimeStamp;


  public static final int MAX_HEALTHY_HOSTS_TO_EMIT = 2;

  D2MonitorEventEmitter(String serviceName, final DegraderLoadBalancerStrategyConfig config, int partitionId)
  {
    _config = config;
    _partitionId = partitionId;
    _lastEmittingTimeStamp = config.getClock().currentTimeMillis();
    _serviceName = serviceName;
  }

  @Override
  public void onUpdate(PartitionDegraderLoadBalancerState state)
  {
    D2MonitorBuilder builder = new D2MonitorBuilder(_serviceName, _config.getClusterName(), _partitionId);
    D2MonitorBuilder.D2MonitorClusterStatsBuilder clusterStatsBuilder = builder.getClusterStatsBuilder();
    Set<TrackerClient> trackerClients = state.getTrackerClients();

    // 1. Set cluster metrics
    clusterStatsBuilder.setClusterNumHosts(trackerClients.size())
        .setClusterCurrentCallCount(state.getCurrentClusterCallCount())
        .setClusterCurrentAverageLatencyMs(state.getCurrentAvgClusterLatency())
        .setClusterCurrentDroppedCalls(state.getCurrentClusterDropCount())
        .setClusterCurrentErrorCount(state.getCurrentClusterErrorCount())
        .setClusterDropLevel(state.getCurrentOverrideDropRate());

    // 2. build up D2MonitorEvent with appropriate uris from the trackerClients
    boolean isHealthy = createD2MonitorEvent(trackerClients, builder, state);
    long clk = _config.getClock().currentTimeMillis();
    long intervalMs =  clk - _lastEmittingTimeStamp;

    if (allowedToEmit(isHealthy, intervalMs))
    {
      // 3. emit the event
      EventEmitter emitter = _config.getEventEmitter();
      emitter.emitEvent(builder.build(intervalMs));

      // 4. update the timeStamp
      _lastEmittingTimeStamp = clk;
    }
  }

  // To emit D2Monitor events, the following conditions need to meet:
  // 1. Either lowEventEmittingInterval or highEventEmittingInterval is greater than 0.
  // 2. The interval since last emitting meets one of the following requirements:
  // 2.1. The interval is greater than lowEventEmittingInterval and current state is not healthy
  // 2.2. The interval is greater than highEventEmittingInterval.
  private boolean allowedToEmit(boolean isHealthy, long intervalMs)
  {
    return (((_config.getLowEventEmittingInterval() > 0)
        && (intervalMs >= _config.getLowEventEmittingInterval()) && !isHealthy)
        || ((_config.getHighEventEmittingInterval() > 0) && (intervalMs >= _config.getHighEventEmittingInterval())));
  }

  private boolean isClientHealthy(TrackerClient trackerClient, final Map<URI, Integer> pointsMap)
  {
    int perfectHealth = (int) (trackerClient.getPartitionWeight(_partitionId) * _config.getPointsPerWeight());
    return pointsMap.get(trackerClient.getUri()) >= perfectHealth;
  }

  /**
   * Create D2MonitorEvent
   * @param trackerClients
   * @param d2MonitorBuilder
   * @param state
   * @return true if all trackerClients are healthy. False otherwise
   */
  private boolean createD2MonitorEvent(Set<TrackerClient> trackerClients, D2MonitorBuilder d2MonitorBuilder,
      PartitionDegraderLoadBalancerState state)
  {
    List<TrackerClient> healthyClients = new ArrayList<>();
    boolean isHealthy = true;

    for (TrackerClient client : trackerClients)
    {
      if (isClientHealthy(client, state.getPointsMap()))
      {
        healthyClients.add(client);
      }
      else
      {
        // for unhealthy clients, always add them into the D2Monitor
        d2MonitorBuilder.addUriInfoBuilder(client.getUri(), createUriInfoBuilder(client, state));
        isHealthy = false;
      }
    }

    // Randomly pick some healthy clients for reference
    if (!healthyClients.isEmpty())
    {
      addRandomClientsToUriInfo(healthyClients, MAX_HEALTHY_HOSTS_TO_EMIT, d2MonitorBuilder, state);
    }

    return isHealthy;
  }

  private void addRandomClientsToUriInfo(List<TrackerClient> healthyClients, int num, final D2MonitorBuilder builder,
      PartitionDegraderLoadBalancerState state)
  {
    // Randomly pick num of entries and add their UriInfo.
    // The operation is equivalent to shuffle + limit, but we do not have to shuffle the whole list since
    // the number of entries to add is generally much less than the size of health clients.
    Random random = new Random();
    for (int i = 0; i < Math.min(num, healthyClients.size()); ++i)
    {
      Collections.swap(healthyClients, i, random.nextInt(healthyClients.size() - i) + i);
      TrackerClient nextClient = healthyClients.get(i);
      builder.addUriInfoBuilder(nextClient.getUri(), createUriInfoBuilder(nextClient, state));
    }
  }

  // Create UriInfoBuilder from corresponding TrackerClient
  private D2MonitorBuilder.D2MonitorUriInfoBuilder createUriInfoBuilder(TrackerClient client,
      PartitionDegraderLoadBalancerState state)
  {
    D2MonitorBuilder.D2MonitorUriInfoBuilder uriInfoBuilder =
        new D2MonitorBuilder.D2MonitorUriInfoBuilder(client.getUri());
    uriInfoBuilder.copyStats(client.getDegraderControl(_partitionId));
    uriInfoBuilder.setTransmissionPoints(state.getPointsMap().get(client.getUri()));
    DegraderLoadBalancerQuarantine quarantine = state.getQuarantineMap().get(client);
    if (quarantine != null)
    {
      uriInfoBuilder.setQuarantineDuration(quarantine.getTimeTilNextCheck());
    }
    return uriInfoBuilder;
  }

  static class Factory implements PartitionDegraderLoadBalancerStateListener.Factory
  {
    private final String _serviceName;

    public Factory(String serviceName)
    {
      _serviceName = serviceName;
    }

    @Override
    public PartitionDegraderLoadBalancerStateListener create(int partitionId, DegraderLoadBalancerStrategyConfig config)
    {
      return new D2MonitorEventEmitter(_serviceName, config, partitionId);
    }
  }
}
