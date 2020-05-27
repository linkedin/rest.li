package com.linkedin.d2.balancer.strategies;

import com.linkedin.d2.balancer.clients.TrackerClient;
import java.util.Set;


/**
 *
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
  void updateStateByRequest(Set<TrackerClient> trackerClients, int partitionId, long clusterGenerationId);
}
