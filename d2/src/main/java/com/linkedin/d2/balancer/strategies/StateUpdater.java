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

package com.linkedin.d2.balancer.strategies;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.util.hashing.Ring;
import java.net.URI;
import java.util.Map;
import java.util.Set;


/**
 * Update the state of a strategy periodically
 */
public interface StateUpdater
{
  /**
   * Update the state of the strategy from a new incoming request
   * The actual update will only be performed if there is any change in cluster hosts or during initialization.
   * Otherwise, the state is updated by the executor service based on fixed intervals, this method will invoke no operation internally
   *
   * @param trackerClients The potential tracker clients to choose from
   * @param partitionId The partition id of the request
   * @param clusterGenerationId The id that identifies a unique set of uris in the current cluster
   */
  void updateState(Set<TrackerClient> trackerClients, int partitionId, long clusterGenerationId);

  Ring<URI> getRing(int partitionId);
}
