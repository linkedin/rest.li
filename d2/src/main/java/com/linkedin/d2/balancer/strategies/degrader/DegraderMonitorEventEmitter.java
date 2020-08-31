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
package com.linkedin.d2.balancer.strategies.degrader;

import java.util.Map;
import java.util.Set;

import com.linkedin.d2.balancer.event.D2MonitorEventEmitter;

/**
 * Adapter for emitting D2 events from {@link DegraderLoadBalancerStrategyV3}.
 */
public class DegraderMonitorEventEmitter implements PartitionDegraderLoadBalancerStateListener
{
  private final D2MonitorEventEmitter _d2MonitorEventEmitter;

  public DegraderMonitorEventEmitter(D2MonitorEventEmitter d2MonitorEventEmitter)
  {
    _d2MonitorEventEmitter = d2MonitorEventEmitter;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void onUpdate(PartitionDegraderLoadBalancerState state)
  {
    _d2MonitorEventEmitter.emitEvent(new D2MonitorEventEmitter.ClusterStatsProvider(state.getPointsMap(),
                                                                                    (Map) state.getQuarantineMap(),
                                                                                    (Set) state.getTrackerClients(),
                                                                                    state.getCurrentClusterCallCount(),
                                                                                    state.getCurrentAvgClusterLatency(),
                                                                                    state.getCurrentClusterDropCount(),
                                                                                    state.getCurrentClusterErrorCount(),
                                                                                    state.getCurrentOverrideDropRate()));
  }

  public static class Factory implements PartitionDegraderLoadBalancerStateListener.Factory
  {
    private final String _serviceName;

    public Factory(String serviceName)
    {
      _serviceName = serviceName;
    }

    @Override
    public PartitionDegraderLoadBalancerStateListener create(int partitionId, DegraderLoadBalancerStrategyConfig config)
    {
      D2MonitorEventEmitter d2MonitorEventEmitter = new D2MonitorEventEmitter(config.getClusterName(),
                                                                              _serviceName,
                                                                              partitionId,
                                                                              config.getClock(),
                                                                              config.getEventEmitter(),
                                                                              config.getHighEventEmittingInterval(),
                                                                              config.getPointsPerWeight());
      return new DegraderMonitorEventEmitter(d2MonitorEventEmitter);
    }
  }
}
