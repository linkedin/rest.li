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

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.ClientSelector;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.StateUpdater;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.*;


/**
 * This strategy balances traffic to hosts within a service by dynamically adjusting a server's
 * health score based on call statistics compared relatively to the performance of the entire cluster.
 *
 * Health score is rated on a scale from 0.0 - 1.0, with 0.0 meaning most unhealthy (all traffic
 * routed away) and 1.0 meaning most healthy (no traffic routed away). Note that this behavior is
 * inverse of dropRate in the degrader strategy.
 *
 * @see com.linkedin.d2.D2RelativeStrategyProperties
 */
public class RelativeLoadBalancerStrategy implements LoadBalancerStrategy
{
  private static final Logger LOG = LoggerFactory.getLogger(RelativeLoadBalancerStrategy.class);
  public static final String RELATIVE_LOAD_BALANCER_STRATEGY_NAME = "relative";

  /// We should probably directly use the interface name instead of the Impl name, because we should only access the public methods
  private final StateUpdater _stateUpdater;
  private final ClientSelector _clientSelector;

  public RelativeLoadBalancerStrategy(StateUpdater stateUpdater,
                                      ClientSelector clientSelector)
  {
    _stateUpdater = stateUpdater;
    _clientSelector = clientSelector;
  }

  @Override
  public String getName()
  {
    return RELATIVE_LOAD_BALANCER_STRATEGY_NAME;
  }

  @Nullable
  @Override
  public TrackerClient getTrackerClient(Request request,
                                        RequestContext requestContext,
                                        long clusterGenerationId,
                                        int partitionId,
                                        Map<URI, TrackerClient> trackerClients)
  {
    if (trackerClients == null || trackerClients.size() == 0)
    {
      warn(LOG,
          "getTrackerClient called with null/empty trackerClients, so returning null");

      return null;
    }

    _stateUpdater.updateState(new HashSet<>(trackerClients.values()), partitionId, clusterGenerationId);
    Ring<URI> ring = getRing(clusterGenerationId, partitionId, trackerClients);
    return _clientSelector.getTrackerClient(request, requestContext, ring, trackerClients);
  }

  @Nonnull
  @Override
  public Ring<URI> getRing(long clusterGenerationId, int partitionId, Map<URI, TrackerClient> trackerClients)
  {
    _stateUpdater.updateState(new HashSet<>(trackerClients.values()), partitionId, clusterGenerationId);
    return _stateUpdater.getRing(partitionId);
  }

  @Override
  public HashFunction<Request> getHashFunction()
  {
    return _clientSelector.getRequestHashFunction();
  }
}
