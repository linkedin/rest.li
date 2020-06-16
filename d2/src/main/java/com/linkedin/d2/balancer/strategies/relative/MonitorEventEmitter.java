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

import com.linkedin.d2.balancer.event.D2MonitorEventEmitter;
import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.strategies.PartitionStateUpdateListener;
import com.linkedin.util.clock.Clock;


/**
 * Adapter for emitting D2 events from {@link RelativeStateUpdater}.
 */
public class MonitorEventEmitter implements PartitionStateUpdateListener<PartitionState>
{
  private final D2MonitorEventEmitter _d2MonitorEventEmitter;

  public MonitorEventEmitter(D2MonitorEventEmitter d2MonitorEventEmitter)
  {
    _d2MonitorEventEmitter = d2MonitorEventEmitter;
  }

  public void onUpdate(PartitionState state)
  {
    // Please note that cluster level drop is deprecated in the relative load balancer, so there is no cluster level dropped calls and drop level
    _d2MonitorEventEmitter.emitEvent(new D2MonitorEventEmitter.ClusterStatsProvider(state.getPointsMap(),
                                                                                    state.getQuarantineMap(),
                                                                                    state.getTrackerClients(),
                                                                                    state.getPartitionStats().getClusterCallCount(),
                                                                                    state.getPartitionStats().getAvgClusterLatency(),
                                                                                    -1,
                                                                                    state.getPartitionStats().getClusterErrorCount(),
                                                                                    -1));
  }

  public static class Factory implements PartitionStateUpdateListener.Factory<PartitionState>
  {
    private final String _serviceName;
    private final String _clusterName;
    private final Clock _clock;
    private final long _emitIntervalMs;
    private final int _pointsPerWeight;
    private final EventEmitter _eventEmitter;

    public Factory(String serviceName, String clusterName, Clock clock, long emitIntervalMs, int pointsPerWeight,
        EventEmitter eventEmitter)
    {
      _serviceName = serviceName;
      _clusterName = clusterName;
      _clock = clock;
      _emitIntervalMs = emitIntervalMs;
      _pointsPerWeight = pointsPerWeight;
      _eventEmitter = eventEmitter;
    }

    @Override
    public MonitorEventEmitter create(int partitionId)
    {
      D2MonitorEventEmitter d2MonitorEventEmitter = new D2MonitorEventEmitter(_clusterName,
                                                                              _serviceName,
                                                                              partitionId,
                                                                              _clock,
                                                                              _eventEmitter,
                                                                              _emitIntervalMs,
                                                                              _pointsPerWeight);
      return new MonitorEventEmitter(d2MonitorEventEmitter);
    }
  }
}
