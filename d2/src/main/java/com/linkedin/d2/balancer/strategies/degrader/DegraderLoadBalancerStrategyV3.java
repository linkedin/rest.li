/*
   Copyright (c) 2012 LinkedIn Corp.

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


import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.RandomHash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.util.degrader.DegraderControl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.warn;


/**
 * Implementation of {@link LoadBalancerStrategy}. The difference between this class and
 * {@link DegraderLoadBalancerStrategyV2} is the former supports partitioning of services whereas
 * the latter does not.
 *
 * @author David Hoa (dhoa@linkedin.com)
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 * @author Zhenkai Zhu (zzhu@linkedin.com)
 */
public class DegraderLoadBalancerStrategyV3 implements LoadBalancerStrategy
{
  public static final String HASH_METHOD_NONE = "none";
  public static final String HASH_METHOD_URI_REGEX = "uriRegex";
  public static final double EPSILON = 10e-6;

  private static final Logger _log = LoggerFactory.getLogger(DegraderLoadBalancerStrategyV3.class);

  private boolean                                     _updateEnabled;
  private volatile DegraderLoadBalancerStrategyConfig _config;
  private volatile HashFunction<Request>              _hashFunction;
  private final DegraderLoadBalancerState _state;

  public DegraderLoadBalancerStrategyV3(DegraderLoadBalancerStrategyConfig config,
                                        String serviceName,
                                        Map<String, String> degraderProperties)
  {
    _updateEnabled = true;
    setConfig(config);
    if (degraderProperties == null)
    {
      degraderProperties = Collections.<String, String>emptyMap();
    }
    _state = new DegraderLoadBalancerState(serviceName, degraderProperties, config);
  }

  @Override
  public TrackerClient getTrackerClient(Request request,
                                        RequestContext requestContext,
                                        long clusterGenerationId,
                                        int partitionId,
                                        List<TrackerClient> trackerClients)
  {
    debug(_log,
          "getTrackerClient with generation id ",
          clusterGenerationId,
          " partition id: ",
          partitionId,
          " on tracker clients: ",
          trackerClients);

    if (trackerClients == null || trackerClients.size() == 0)
    {
      warn(_log,
           "getTrackerClient called with null/empty trackerClients, so returning null");

      return null;
    }

    // only one thread will be allowed to enter updatePartitionState for any partition
    checkUpdatePartitionState(clusterGenerationId, partitionId, trackerClients);
    Ring<URI> ring =  _state.getRing(partitionId);

    URI targetHostUri = KeyMapper.TargetHostHints.getRequestContextTargetHost(requestContext);
    URI hostHeaderUri = targetHostUri;

    //no valid target host header was found in the request
    if (targetHostUri == null)
    {
      // Compute the hash code
      int hashCode = _hashFunction.hash(request);

      // we operate only on URIs to ensure that we never hold on to an old tracker client
      // that the cluster manager has removed
      targetHostUri = (ring == null) ? null : ring.get(hashCode);
    }
    else
    {
      debug(_log, "Degrader honoring target host header in request, skipping hashing.  URI: " + targetHostUri.toString());
    }

    TrackerClient client = null;

    if (targetHostUri != null)
    {
      // These are the clients that were passed in, NOT necessarily the clients that make up the
      // consistent hash ring! Therefore, this linear scan is the best we can do.
      client = searchClientFromUri(targetHostUri, trackerClients);

      if (client == null && hostHeaderUri == null) {
        // The consistent hash ring might not be updated as quickly as the trackerclients,
        // so there could be an inconsistent situation where the trackerclient is already deleted
        // while the uri still exists in the hash ring.
        //
        // When this happens, instead of failing the search, we simply return the next uri in the ring
        // that is available in the trackerclient list.
        debug(_log, "Degrader load balancer state is inconsistent with cluster manager (URI " + targetHostUri +
                " does not exist), iterating through the hash ring for the next available host");
        Iterator<URI> iter = ring.getIterator(_hashFunction.hash(request));
        while (client == null && iter.hasNext())
        {
          targetHostUri = iter.next();
          client = searchClientFromUri(targetHostUri, trackerClients);
        }
      }

      if (client == null)
      {
        warn(_log, "No client found for " + targetHostUri + (hostHeaderUri == null ?
                ", degrader load balancer state is inconsistent with cluster manager" :
                ", target host specified is no longer part of cluster"));
      }
    }
    else
    {
      warn(_log, "unable to find a URI to use");
    }

    boolean dropCall = client == null;

    if (!dropCall)
    {
      dropCall = client.getDegrader(partitionId).checkDrop();

      if (dropCall)
      {
        warn(_log, "client's degrader is dropping call for: ", client);
      }
      else
      {
        debug(_log, "returning client: ", client);
      }
    }

    return (!dropCall) ? client : null;
  }

  /*
   * checkUpdatePartitionState
   *
   * checkUpdatePartitionState will only allow one thread to update the state for each partition at one time.
   * Those threads who want to access the same partition state will wait on a lock until the updating thread finishes
   * the attempt to update.
   *
   * In the event there's an exception when a thread updates the state, there is no side-effect on the state itself
   * or on the trackerclients. Other threads will attempt the update the state as if the previous attempt did not happen.
   *
   * @param clusterGenerationId
   * @param partitionId
   * @param trackerClients
   */
  private void checkUpdatePartitionState(long clusterGenerationId, int partitionId, List<TrackerClient> trackerClients)
  {
    DegraderLoadBalancerStrategyConfig config = getConfig();
    final Partition partition = _state.getPartition(partitionId);
    final Lock lock = partition.getLock();

    if (!partition.getState().isInitialized())
    {
      // threads attempt to access the state would block here if partition state is not initialized
      lock.lock();
      try
      {
        if (!partition.getState().isInitialized())
        {
          debug(_log, "initializing partition state for partition: ", partitionId);
          updatePartitionState(clusterGenerationId, partition, trackerClients, config);
          if(!partition.getState().isInitialized())
          {
            _log.error("Failed to initialize partition state for patition: ", partitionId);
          }
        }
      }
      finally
      {
        lock.unlock();
      }
    }
    else if(shouldUpdatePartition(clusterGenerationId, partition.getState(), config, _updateEnabled))
    {
      // threads attempt to update the state would return immediately if some thread is already in the updating process
      // NOTE: possible racing condition -- if tryLock() fails and the current updating process does not pick up the
      // new clusterGenerationId (ie current updating is still processing the previous request), it will causes the
      // inconsistency between trackerClients and the hash ring, because hash ring does not get updated to match the
      // new trackerClients. We need either to lock/wait here (for bad performance) or to fix the errors caused by
      // the inconsistency later on. The decision is to handle the errors later.
      if(lock.tryLock())
      {
        try
        {
          if(shouldUpdatePartition(clusterGenerationId, partition.getState(), config, _updateEnabled))
          {
            debug(_log, "updating for cluster generation id: ", clusterGenerationId, ", partitionId: ", partitionId);
            debug(_log, "old state was: ", partition.getState());
            updatePartitionState(clusterGenerationId, partition, trackerClients, config);
          }
        }
        finally
        {
          lock.unlock();
        }
      }
    }
  }

  private TrackerClient searchClientFromUri(URI uri, List<TrackerClient> trackerClients)
  {
    for (TrackerClient trackerClient : trackerClients) {
      if (trackerClient.getUri().equals(uri)) {
        return trackerClient;
      }
    }
    return null;
  }

  private void updatePartitionState(long clusterGenerationId, Partition partition, List<TrackerClient> trackerClients, DegraderLoadBalancerStrategyConfig config)
  {
    PartitionDegraderLoadBalancerState partitionState = partition.getState();

    List<TrackerClientUpdater> clientUpdaters = new ArrayList<TrackerClientUpdater>();
    for (TrackerClient client: trackerClients)
    {
      clientUpdaters.add(new TrackerClientUpdater(client, partition.getId()));
    }

    // doUpdatePartitionState has no side effects on _state or trackerClients.
    // all changes to the trackerClients would be recorded in clientUpdaters
    partitionState = doUpdatePartitionState(clusterGenerationId, partition.getId(), partitionState,
                                          config, clientUpdaters);
    partition.setState(partitionState);

    // only if state update succeeded, do we actually apply the recorded changes to trackerClients
    for (TrackerClientUpdater clientUpdater : clientUpdaters)
    {
      clientUpdater.update();
    }
  }


  static boolean isNewStateHealthy(PartitionDegraderLoadBalancerState newState,
                                   DegraderLoadBalancerStrategyConfig config,
                                   List<TrackerClientUpdater> trackerClientUpdaters,
                                   int partitionId)
  {
    if (newState.getCurrentAvgClusterLatency() > config.getLowWaterMark())
    {
      return false;
    }
    Map<URI, Integer> pointsMap = newState.getPointsMap();
    for (TrackerClientUpdater clientUpdater : trackerClientUpdaters)
    {
      TrackerClient client = clientUpdater.getTrackerClient();
      int perfectHealth = (int) (client.getPartitionWeight(partitionId) * config.getPointsPerWeight());
      Integer point = pointsMap.get(client.getUri());
      if (point < perfectHealth)
      {
        return false;
      }
    }
    return true;
  }

  static boolean isOldStateTheSameAsNewState(PartitionDegraderLoadBalancerState oldState,
                                                     PartitionDegraderLoadBalancerState newState)
  {
    return oldState.getClusterGenerationId() == newState.getClusterGenerationId() &&
        oldState.getCurrentOverrideDropRate() == newState.getCurrentOverrideDropRate() &&
        oldState.getPointsMap().equals(newState.getPointsMap()) &&
        oldState.getRecoveryMap().equals(newState.getRecoveryMap());
  }

  private static void logState(PartitionDegraderLoadBalancerState oldState,
                        PartitionDegraderLoadBalancerState newState,
                        int partitionId,
                        DegraderLoadBalancerStrategyConfig config,
                        List<TrackerClientUpdater> trackerClientUpdaters)
  {
    if (_log.isDebugEnabled())
    {
      _log.debug("Strategy updated: partitionId= " + partitionId + ", newState=" + newState +
                     ", unhealthyClients = "
                     + getUnhealthyTrackerClients(trackerClientUpdaters,
                                                  newState._pointsMap,
                                                  config,
                                                  partitionId) + ", config=" + config +
                     ", HashRing coverage=" + newState.getRing());
    }
    else
    {
      if (!isNewStateHealthy(newState, config, trackerClientUpdaters, partitionId) ||
          !isOldStateTheSameAsNewState(oldState, newState))
      {
        _log.info("Strategy updated: partitionId= " + partitionId + ", newState=" + newState +
                      ", unhealthyClients = "
                      + getUnhealthyTrackerClients(trackerClientUpdaters,
                                                   newState._pointsMap,
                                                   config,
                                                   partitionId) +
                      ", oldState =" +
                      oldState + ", new state's config=" + config);
      }
    }
  }

  private static List<String> getUnhealthyTrackerClients(List<TrackerClientUpdater> trackerClientUpdaters,
                                                         Map<URI, Integer> pointsMap,
                                                         DegraderLoadBalancerStrategyConfig config,
                                                         int partitionId)
  {
    List<String> unhealthyClients = new ArrayList<String>();
    for (TrackerClientUpdater clientUpdater : trackerClientUpdaters)
    {
      TrackerClient client = clientUpdater.getTrackerClient();
      int perfectHealth = (int) (client.getPartitionWeight(partitionId) * config.getPointsPerWeight());
      Integer point = pointsMap.get(client.getUri());
      if (point < perfectHealth)
      {
        unhealthyClients.add(client.getUri() + ":" + point + "/" + perfectHealth);
      }
    }
    return unhealthyClients;
  }

  /**
   * updatePartitionState
   *
   * We have two mechanisms to influence the health and traffic patterns of the client. They are
   * by load balancing (switching traffic from one host to another) and by degrading service
   * (dropping calls). We load balance by allocating points in a consistent hash ring based on the
   * computedDropRate of the individual TrackerClients, which takes into account the latency
   * seen by that TrackerClient's requests. We can alternatively, if the cluster is
   * unhealthy (by using a high latency watermark) drop a portion of traffic across all tracker
   * clients corresponding to this cluster.
   *
   * The reason we do not currently consider error rate when adjusting the hash ring is that
   * there are legitimate errors that servers can send back for clients to handle, such as
   * 400 return codes. A potential improvement would be to catch transport level exceptions and 500
   * level return codes, but the implication of that would need to be carefully understood and documented.
   *
   * We don't want both to reduce hash points and allow clients to manage their own drop rates
   * because the clients do not have a global view that the load balancing strategy does. Without
   * a global view, the clients won't know if it already has a reduced number of hash points. If the
   * client continues to drop at the same drop rate as before their points have been reduced, then
   * the client would have its outbound request reduced by both reduction in points and the client's
   * drop rate. To avoid this, the drop rate is managed globally by the load balancing strategy and
   * provided to each client. The strategy will alternate between adjusting the hash ring points or
   * the global drop rate in order to avoid double penalizing a client.
   *
   * We also have a mechanism for recovery if the number of points in the hash ring is not
   * enough to receive traffic. The initialRecoveryLevel is a number between 0.0 and 1.0, and
   * corresponds to a weight of the tracker client's full hash points.
   * The reason for the weight is to allow an initialRecoveryLevel that corresponds to
   * less than one hash point. This would be useful if a "cooling off" period is desirable for the
   * misbehaving tracker clients, ie , given a full weight of 100 hash points,0.005 means that
   * there will be one cooling off period before the client is reintroduced into the hash ring.
   *
   * The second configuration, rampFactor, will geometrically increase the
   * previous recoveryLevel if traffic still hasn't been seen for that tracker client.
   *
   * For example, given initialRecoveryLevel = 0.01, rampFactor = 2, and default tracker client hash
   * points of 100, we will increase the hash points in this pattern on successive update States:
   *  0.01, 0.02, 0.04, 0.08, 0.16, 0.32, etc., aborting as soon as
   * calls are recorded for that tracker client.
   *
   * We also have highWaterMark and lowWaterMark as properties of the DegraderLoadBalancer strategy
   * so that the strategy can make decisions on whether to start dropping traffic globally across
   * all tracker clients for this cluster. The amount of traffic to drop is controlled by the
   * globalStepUp and globalStepDown properties, where globalStepUp controls how much the global
   * drop rate increases per interval, and globalStepDown controls how much the global drop rate
   * decreases per interval. We only step up the global drop rate when the average cluster latency
   * is higher than the highWaterMark, and only step down the global drop rate when the average
   * cluster latency is lower than the global drop rate.
   *
   * This code is thread reentrant. Multiple threads can potentially call this concurrently, and so
   * callers must pass in the DegraderLoadBalancerState that they based their shouldUpdate() call on.
   * The multiple threads may have different views of the trackerClients latency, but this is
   * ok as the new state in the end will have only taken one action (either loadbalance or
   * call-dropping with at most one step). Currently we will not call this concurrently, as
   * checkUpdatePartitionState will control entry to a single thread.
   *
   * @param clusterGenerationId
   * @param trackerClientUpdaters
   * @param oldState
   * @param config
   */
  private static PartitionDegraderLoadBalancerState doUpdatePartitionState(long clusterGenerationId, int partitionId,
                                                                         PartitionDegraderLoadBalancerState oldState,
                                                                         DegraderLoadBalancerStrategyConfig config,
                                                                         List<TrackerClientUpdater> trackerClientUpdaters)
  {
    debug(_log, "updating state for: ", trackerClientUpdaters);

    double sumOfClusterLatencies = 0.0;
    double computedClusterDropSum = 0.0;
    double computedClusterDropRate;
    double computedClusterWeight = 0.0;
    long totalClusterCallCount = 0;
    double clientDropRate;
    double newMaxDropRate;
    boolean hashRingChanges = false;
    boolean recoveryMapChanges = false;

    PartitionDegraderLoadBalancerState.Strategy strategy = oldState.getStrategy();
    Map<TrackerClient,Double> oldRecoveryMap = oldState.getRecoveryMap();
    Map<TrackerClient,Double> newRecoveryMap = new HashMap<TrackerClient, Double>(oldRecoveryMap);
    double currentOverrideDropRate = oldState.getCurrentOverrideDropRate();
    double initialRecoveryLevel = config.getInitialRecoveryLevel();
    double ringRampFactor = config.getRingRampFactor();
    int pointsPerWeight = config.getPointsPerWeight();
    PartitionDegraderLoadBalancerState newState;

    for (TrackerClientUpdater clientUpdater : trackerClientUpdaters)
    {
      TrackerClient client = clientUpdater.getTrackerClient();
      DegraderControl degraderControl = client.getDegraderControl(partitionId);
      double averageLatency = degraderControl.getLatency();
      long callCount = degraderControl.getCallCount();
      oldState.getPreviousMaxDropRate().put(client, clientUpdater.getMaxDropRate());
      double clientWeight =  client.getPartitionWeight(partitionId);

      sumOfClusterLatencies += averageLatency * callCount;
      totalClusterCallCount += callCount;
      clientDropRate = degraderControl.getCurrentComputedDropRate();
      computedClusterDropSum += clientWeight * clientDropRate;

      computedClusterWeight += clientWeight;

      boolean recoveryMapContainsClient = newRecoveryMap.containsKey(client);

      // The following block of code calculates and updates the maxDropRate if the client had been
      // fully degraded in the past and has not received any requests since being fully degraded.
      // To increase the chances of the client receiving a request, we change the maxDropRate, which
      // influences the maximum value of computedDropRate, which is used to compute the number of
      // points in the hash ring for the clients.
      if (callCount == 0)
      {
        // if this client is enrolled in the program, decrease the maxDropRate
        // it is important to note that this excludes clients that haven't gotten traffic
        // due solely to low volume.
        if (recoveryMapContainsClient)
        {
          double oldMaxDropRate = clientUpdater.getMaxDropRate();
          double transmissionRate = 1.0 - oldMaxDropRate;
          if( transmissionRate <= 0.0)
          {
            // We use the initialRecoveryLevel to indicate how many points to initially set
            // the tracker client to when traffic has stopped flowing to this node.
            transmissionRate = initialRecoveryLevel;
          }
          else
          {
            transmissionRate *= ringRampFactor;
            transmissionRate = Math.min(transmissionRate, 1.0);
          }
          newMaxDropRate = 1.0 - transmissionRate;

          if (strategy == PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE)
          {
            // if it's the hash ring's turn to adjust, then adjust the maxDropRate.
            // Otherwise, we let the call dropping strategy take it's turn, even if
            // it may do nothing.
            clientUpdater.setMaxDropRate(newMaxDropRate);
          }
          recoveryMapChanges = true;
        }
      }
      else if(recoveryMapContainsClient)
      {
        // else if the recovery map contains the client and the call count was > 0

        // tough love here, once the rehab clients start taking traffic, we
        // restore their maxDropRate to it's original value, and unenroll them
        // from the program.
        // This is safe because the hash ring points are controlled by the
        // computedDropRate variable, and the call dropping rate is controlled by
        // the overrideDropRate. The maxDropRate only serves to cap the computedDropRate and
        // overrideDropRate.
        // We store the maxDropRate and restore it here because the initialRecoveryLevel could
        // potentially be higher than what the default maxDropRate allowed. (the maxDropRate doesn't
        // necessarily have to be 1.0). For instance, if the maxDropRate was 0.99, and the
        // initialRecoveryLevel was 0.05  then we need to store the old maxDropRate.
        clientUpdater.setMaxDropRate(newRecoveryMap.get(client));
        newRecoveryMap.remove(client);
        recoveryMapChanges = true;
      }
    }

    computedClusterDropRate = computedClusterDropSum / computedClusterWeight;
    debug(_log, "total cluster call count: ", totalClusterCallCount);
    debug(_log,
          "computed cluster drop rate for ",
          trackerClientUpdaters.size(),
          " nodes: ",
          computedClusterDropRate);

    if (oldState.getClusterGenerationId() == clusterGenerationId
        && totalClusterCallCount <= 0 && !recoveryMapChanges)
    {
      // if the cluster has not been called recently (total cluster call count is <= 0)
      // and we already have a state with the same set of URIs (same cluster generation),
      // and no clients are in rehab, then don't change anything.
      debug(_log, "New state is the same as the old state so we're not changing anything. Old state = ", oldState
          ,", config= ", config);
      return new PartitionDegraderLoadBalancerState(oldState, clusterGenerationId,
                                                    config.getClock().currentTimeMillis());
    }

    // update our overrides.
    double newCurrentAvgClusterLatency = -1;

    if(totalClusterCallCount > 0)
    {
      newCurrentAvgClusterLatency = sumOfClusterLatencies / totalClusterCallCount;
    }

    debug(_log, "average cluster latency: ", newCurrentAvgClusterLatency);

    // compute points for every node in the cluster
    double computedClusterSuccessRate = computedClusterWeight - computedClusterDropRate;

    // This points map stores how many hash map points to allocate for each tracker client.

    Map<URI, Integer> points = new HashMap<URI, Integer>();
    Map<URI, Integer> oldPointsMap = oldState.getPointsMap();

    for (TrackerClientUpdater clientUpdater : trackerClientUpdaters)
    {
      TrackerClient client = clientUpdater.getTrackerClient();
      double successfulTransmissionWeight;
      URI clientUri = client.getUri();

      // Don't take into account cluster health when calculating the number of points
      // for each client. This is because the individual clients already take into account
      // latency and errors, and a successfulTransmissionWeight can and should be made
      // independent of other nodes in the cluster. Otherwise, one unhealthy client in a small
      // cluster can take down the entire cluster if the avg latency is too high.
      // The global drop rate will take into account the cluster latency. High cluster-wide error
      // rates are not something d2 can address.
      //
      // this client's maxDropRate and currentComputedDropRate may have been adjusted if it's in the
      // rehab program (to gradually send traffic it's way).
      DegraderControl degraderControl = client.getDegraderControl(partitionId);
      double dropRate = Math.min(degraderControl.getCurrentComputedDropRate(),
                                 clientUpdater.getMaxDropRate());

      // calculate the weight as the probability of successful transmission to this
      // node divided by the probability of successful transmission to the entire
      // cluster
      double clientWeight = client.getPartitionWeight(partitionId);
      successfulTransmissionWeight = clientWeight * (1.0 - dropRate);

      // calculate the weight as the probability of a successful transmission to this node
      // multiplied by the client's self-defined weight. thus, the node's final weight
      // takes into account both the self defined weight (to account for different
      // hardware in the same cluster) and the performance of the node (as defined by the
      // node's degrader).
      debug(_log,
            "computed new weight for uri ",
            clientUri,
            ": ",
            successfulTransmissionWeight);

      // keep track if we're making actual changes to the Hash Ring in this updatePartitionState.
      int newPoints = (int) (successfulTransmissionWeight * pointsPerWeight);

      // We only enroll the tracker client in the recovery program when clientWeight is not zero but we got zero points.
      // ClientWeight can be zero when the server's clientWeight in zookeeper is explicitly set to zero,
      // in order to put the server into standby. In this particular case, we should not put the tracker
      // client into the recovery program, because we don't want this tracker client to get any traffic.
      if (newPoints == 0 && clientWeight > EPSILON )
      {
        // We are choking off traffic to this tracker client.
        // Enroll this tracker client in the recovery program so that
        // we can make sure it still gets some traffic
        Double oldMaxDropRate = clientUpdater.getMaxDropRate();

        // set the default recovery level.
        newPoints = (int) (initialRecoveryLevel * pointsPerWeight);

        // Keep track of the original maxDropRate
        if (!newRecoveryMap.containsKey(client))
        {
          // keep track of this client,
          newRecoveryMap.put(client, oldMaxDropRate);
          clientUpdater.setMaxDropRate(1.0 - initialRecoveryLevel);
        }
      }

      points.put(clientUri, newPoints);
      if (!oldPointsMap.containsKey(clientUri) || oldPointsMap.get(clientUri) != newPoints)
      {
        hashRingChanges = true;
      }
    }

    // Here is where we actually make the decision what compensating action to take, if any.
    // if the strategy to try is Load balancing and there are new changes to the hash ring, or
    // if there were changes to the members of the cluster
    if ((strategy == PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE && hashRingChanges) ||
        oldState.getClusterGenerationId() != clusterGenerationId)
    {
      // atomic overwrite
      // try Call Dropping next time we updatePartitionState.
      newState =
          new PartitionDegraderLoadBalancerState(clusterGenerationId, config.getClock().currentTimeMillis(), true, points,
                                        PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING,
                                        currentOverrideDropRate,
                                        newCurrentAvgClusterLatency,
                                        newRecoveryMap,
                                        oldState.getServiceName(),
                                        oldState.getDegraderProperties(),
                                        totalClusterCallCount);

      logState(oldState, newState, partitionId, config, trackerClientUpdaters);
    }
    else
    {
      // time to try call dropping strategy, if necessary.

      // we are explicitly setting the override drop rate to a number between 0 and 1, inclusive.
      double newDropLevel = Math.max(0.0, currentOverrideDropRate);

      // if the cluster is unhealthy (above high water mark)
      // then increase the override drop rate
      //
      // note that the tracker clients in the recovery list are also affected by the global
      // overrideDropRate, and that their hash ring bump ups will also alternate with this
      // overrideDropRate adjustment, if necessary. This is fine because the first priority is
      // to get the cluster latency stabilized
      if (newCurrentAvgClusterLatency > 0 && totalClusterCallCount >= config.getMinClusterCallCountHighWaterMark())
      {
        // if we enter here that means we have enough call counts to be confident that our average latency is
        // statistically significant
        if (newCurrentAvgClusterLatency >= config.getHighWaterMark() && currentOverrideDropRate != 1.0)
        {
          // if the cluster latency is too high and we can drop more traffic
          newDropLevel = Math.min(1.0, newDropLevel + config.getGlobalStepUp());
        }
        else if (newCurrentAvgClusterLatency <= config.getLowWaterMark() && currentOverrideDropRate != 0.0)
        {
          // else if the cluster latency is good and we can reduce the override drop rate
          newDropLevel = Math.max(0.0, newDropLevel - config.getGlobalStepDown());
        }
        // else the averageClusterLatency is between Low and High, or we can't change anything more,
        // then do not change anything.
      }
      else if (newCurrentAvgClusterLatency > 0 && totalClusterCallCount >= config.getMinClusterCallCountLowWaterMark())
      {
        //if we enter here that means, we don't have enough calls to the cluster. We shouldn't degrade more
        //but we might recover a bit if the latency is healthy
        if (newCurrentAvgClusterLatency <= config.getLowWaterMark() && currentOverrideDropRate != 0.0)
        {
          // the cluster latency is good and we can reduce the override drop rate
          newDropLevel = Math.max(0.0, newDropLevel - config.getGlobalStepDown());
        }
        // else the averageClusterLatency is somewhat high but since the qps is not that high, we shouldn't degrade
      }
      else
      {
        // if we enter here that means we have very low traffic. We should reduce the overrideDropRate, if possible.
        // when we have below 1 QPS traffic, we should be pretty confident that the cluster can handle very low
        // traffic. Of course this is depending on the MinClusterCallCountLowWaterMark that the service owner sets.
        // Another reason is this might have happened if we had somehow choked off all traffic to the cluster, most
        // likely in a one node/small cluster scenario. Obviously, we can't check latency here,
        // we'll have to rely on the metric in the next updatePartitionState. If the cluster is still having
        // latency problems, then we will oscillate between off and letting a little traffic through,
        // and that is acceptable. If the latency, though high, is deemed acceptable, then the
        // watermarks can be adjusted to let more traffic through.
        newDropLevel = Math.max(0.0, newDropLevel - config.getGlobalStepDown());
      }

      if (newDropLevel != currentOverrideDropRate)
      {
        overrideClusterDropRate(partitionId, newDropLevel, trackerClientUpdaters);
      }

      // don't change the points map or the recoveryMap, but try load balancing strategy next time.
      newState =
              new PartitionDegraderLoadBalancerState(clusterGenerationId, config.getClock().currentTimeMillis(), true, oldPointsMap,
                                           PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE,
                                            newDropLevel,
                                            newCurrentAvgClusterLatency,
                                            oldRecoveryMap,
                                            oldState.getServiceName(),
                                            oldState.getDegraderProperties(),
                                            oldState.getCurrentClusterCallCount());

      logState(oldState, newState, partitionId, config, trackerClientUpdaters);

      points = oldPointsMap;
    }

    // adjust the min call count for each client based on the hash ring reduction and call dropping
    // fraction.
    overrideMinCallCount(partitionId, currentOverrideDropRate,trackerClientUpdaters, points, pointsPerWeight);

    return newState;
  }

  /**
   * Unsynchronized
   *
   * @param override
   * @param trackerClientUpdaters
   */
  public static void overrideClusterDropRate(int partitionId, double override, List<TrackerClientUpdater> trackerClientUpdaters)
  {
    debug(_log,
            "partitionId=",
            partitionId,
            "overriding degrader drop rate to ",
            override,
            " for clients: ",
            trackerClientUpdaters);

    for (TrackerClientUpdater clientUpdater : trackerClientUpdaters)
    {
      clientUpdater.setOverrideDropRate(override);
    }
  }

  /**
   * Both the drop in hash ring points and the global drop rate influence the minimum call count
   * that we should see to qualify for a state update. Currently, both factors are equally weighed,
   * and multiplied together to come up with a scale factor. With this scheme, if either factor is
   * zero, then the overrideMinCallCount will be set to 1. If both factors are at half weight, then
   * the overall weight will be .5 * .5 = .25 of the original minCallCount.
   *
   * @param newOverrideDropRate
   * @param trackerClientUpdaters
   * @param pointsMap
   * @param pointsPerWeight
   */
  public static void overrideMinCallCount(int partitionId, double newOverrideDropRate, List<TrackerClientUpdater> trackerClientUpdaters,
                                   Map<URI,Integer> pointsMap, int pointsPerWeight)
  {
    for (TrackerClientUpdater clientUpdater : trackerClientUpdaters)
    {
      TrackerClient client = clientUpdater.getTrackerClient();
      DegraderControl degraderControl = client.getDegraderControl(partitionId);
      int currentOverrideMinCallCount = client.getDegraderControl(partitionId).getOverrideMinCallCount();
      double hashFactor = pointsMap.get(client.getUri()) / pointsPerWeight;
      double transmitFactor = 1.0 - newOverrideDropRate;
      int newOverrideMinCallCount = (int) Math.max(Math.round(degraderControl.getMinCallCount() *
                                                       hashFactor * transmitFactor), 1);

      if (newOverrideMinCallCount != currentOverrideMinCallCount)
      {
        clientUpdater.setOverrideMinCallCount(newOverrideMinCallCount);
        warn(_log,
             "partitionId=",
             partitionId,
             "overriding Min Call Count to ",
             newOverrideMinCallCount,
             " for client: ",
             client.getUri());
      }
    }
  }

  /**
   * We should update if we have no prior state, or the state's generation id is older
   * than the current cluster generation, or the state was last updated more than
   * _updateIntervalMs ago.
   *
   * Now requiring shouldUpdatePartition to take a DegraderLoadBalancerState because we must have a
   * static view of the state, and we don't want the state to change between checking if we should
   * update and actually updating.
   *
   * @param clusterGenerationId
   *          The cluster generation for a set of tracker clients
   * @param partitionState
   *          Current PartitionDegraderLoadBalancerState
   * @param config
   *          Current DegraderLoadBalancerStrategyConfig
   * @param updateEnabled
   *          Whether updates to the strategy state is allowed.
   *
   * @return True if we should update, and false otherwise.
   */
  protected static boolean shouldUpdatePartition(long clusterGenerationId, PartitionDegraderLoadBalancerState partitionState,
                                                 DegraderLoadBalancerStrategyConfig config, boolean updateEnabled)
  {
    return  updateEnabled
        && (
            partitionState.getClusterGenerationId() != clusterGenerationId ||
            config.getClock().currentTimeMillis() - partitionState.getLastUpdated() >= config.getUpdateIntervalMs());
  }


  /**
   * only used in tests
   * both this method and DegraderLoadBalancerState are package private
   *
   * The returned state is not a snapshot, but just the underlying state (which may change at any time).
   */
  public DegraderLoadBalancerState getState()
  {
    return _state;
  }

  public DegraderLoadBalancerStrategyConfig getConfig()
  {
    return _config;
  }

  public void setConfig(DegraderLoadBalancerStrategyConfig config)
  {
    _config = config;
    String hashMethod = _config.getHashMethod();
    Map<String,Object> hashConfig = _config.getHashConfig();
    if (hashMethod == null || hashMethod.equals(HASH_METHOD_NONE))
    {
      _hashFunction = new RandomHash();
    }
    else if (HASH_METHOD_URI_REGEX.equals(hashMethod))
    {
      _hashFunction  =  new URIRegexHash(hashConfig);
    }
    else
    {
      _log.warn("Unknown hash method {}, falling back to random", hashMethod);
      _hashFunction = new RandomHash();
    }
  }

  @Override
  public Ring<URI> getRing(long clusterGenerationId, int partitionId, List<TrackerClient> trackerClients)
  {
    checkUpdatePartitionState(clusterGenerationId, partitionId, trackerClients);

    return _state.getRing(partitionId);
  }

  /**
   * this call returns the ring. Ring can be null depending whether the state has been initialized or not
   * @param partitionId
   * @return
   */
  public Ring<URI> getRing(int partitionId)
  {
    return _state.getRing(partitionId);
  }

  /**
   * Whether or not the degrader's view of the cluster is allowed to be updated.
   */
  public boolean getUpdateEnabled()
  {
    return _updateEnabled;
  }

  /**
    * If false, will disable updates to the strategy's view of the cluster.
   */
  public void setUpdateEnabled(boolean enabled)
  {
    _updateEnabled = enabled;
  }


  // for unit testing, this allows the strategy to be forced for the next time updatePartitionState
  // is called. This is not to be used in prod code.
  void setStrategy(int partitionId, PartitionDegraderLoadBalancerState.Strategy strategy)
  {
    final Partition partition = _state.getPartition(partitionId);
    PartitionDegraderLoadBalancerState oldState = partition.getState();
    PartitionDegraderLoadBalancerState newState =
        new PartitionDegraderLoadBalancerState(oldState.getClusterGenerationId(), oldState.getLastUpdated(), oldState.isInitialized(),
                                             oldState.getPointsMap(),
                                             strategy,
                                             oldState.getCurrentOverrideDropRate(),
                                             oldState.getCurrentAvgClusterLatency(),
                                             oldState.getRecoveryMap(),
                                             oldState.getServiceName(),
                                             oldState.getDegraderProperties(),
                                             oldState.getCurrentClusterCallCount());

    partition.setState(newState);
  }

  @Override
  public String toString()
  {
    return "DegraderLoadBalancerStrategyV3 [_config=" + _config
        + ", _state=" + _state + "]";
  }

  private static class Partition
  {
    private final int _id;
    private final Lock _lock;
    private volatile PartitionDegraderLoadBalancerState _state;

    Partition(int id, Lock lock, PartitionDegraderLoadBalancerState state)
    {
      _id = id;
      _lock = lock;
      _state = state;
    }

    public int getId()
    {
      return _id;
    }

    /** this controls access to updatePartitionState for each partition:
     * only one thread should update the state for a particular partition at any one time.
     */
    public Lock getLock()
    {
      return _lock;
    }

    public PartitionDegraderLoadBalancerState getState()
    {
      return _state;
    }

    public void setState(PartitionDegraderLoadBalancerState state)
    {
      _state = state;
    }

    @Override
    public String toString()
    {
      return String.valueOf(_state);
    }
  }

  /** A collection of Partition objects, one for each partition, lazily initialized. */
  public static class DegraderLoadBalancerState
  {
    private final ConcurrentMap<Integer, Partition> _partitions;
    private final String _serviceName;
    private final Map<String, String> _degraderProperties;
    private final DegraderLoadBalancerStrategyConfig _config;

    DegraderLoadBalancerState(String serviceName, Map<String, String> degraderProperties,
                              DegraderLoadBalancerStrategyConfig config)
    {
      _degraderProperties = degraderProperties != null ? degraderProperties : Collections.<String, String>emptyMap();
      _partitions = new ConcurrentHashMap<Integer, Partition>();
      _serviceName = serviceName;
      _config = config;
    }

    private Partition getPartition(int partitionId)
    {
      Partition partition = _partitions.get(partitionId);
      if (partition == null)
      {
        // this is mainly executed in bootstrap time
        // after the system is stabilized, i.e. after all partitionIds have been seen,
        // there will be no need to initialize the map
        // Note that we do this trick because partition count is not available in
        // service configuration (it's in cluster configuration) and we do not want to
        // intermingle the two configurations
        Partition newValue = new Partition(partitionId,
                                           new ReentrantLock(),
                                           new PartitionDegraderLoadBalancerState
                                                                  (-1, _config.getClock().currentTimeMillis(), false,
                                                                   new HashMap<URI, Integer>(),
                                                                   PartitionDegraderLoadBalancerState.Strategy.
                                                                       LOAD_BALANCE,
                                                                   0, 0,
                                                                   new HashMap<TrackerClient, Double>(),
                                                                   _serviceName, _degraderProperties,
                                                                   0));
        Partition oldValue = _partitions.putIfAbsent(partitionId, newValue);
        if (oldValue == null)
          partition = newValue;
        else // another thread already initialized this partition
          partition = oldValue; // newValue is discarded
      }
      return partition;
    }

    private Ring<URI> getRing(int partitionId)
    {
      if (_partitions.get(partitionId) != null)
      {
        PartitionDegraderLoadBalancerState state = _partitions.get(partitionId).getState();
        return state.getRing();
      }
      else
      {
        return null;
      }
    }

    // this method never returns null
    public PartitionDegraderLoadBalancerState getPartitionState(int partitionId)
    {
      return getPartition(partitionId).getState();
    }

    void setPartitionState(int partitionId, PartitionDegraderLoadBalancerState newState)
    {
      getPartition(partitionId).setState(newState);
    }

    @Override
    public String toString()
    {
      return "PartitionStates: [" + _partitions + "]";
    }
  }

  /**
   * A helper class that contains all state for the degrader load balancer. This allows us
   * to overwrite state with a single write.
   *
   * @author criccomini
   *
   */
  public static class PartitionDegraderLoadBalancerState
  {
    // These are the different strategies we have for handling load and bad situations:
    // load balancing (involves adjusting the number of points for a tracker client in the hash ring). or
    // call dropping (drop a fraction of traffic that otherwise would have gone to a particular Tracker client.
    public enum Strategy
    {
      LOAD_BALANCE,
      CALL_DROPPING
    }

    private final Ring<URI> _ring;
    private final long _clusterGenerationId;
    private final String    _serviceName;
    private final Map<String, String> _degraderProperties;

    private final Map<URI, Integer>                  _pointsMap;

    // Used to keep track of Clients that have been ramped down to the minimum level in the hash
    // ring, and are slowly being ramped up until they start receiving traffic again.
    private final Map<TrackerClient,Double>          _recoveryMap;

    // Because we will alternate between Load Balancing and Call Dropping strategies, we keep track of
    // the strategy to try to aid us in alternating strategies when updatingState. There is a setter
    // to manipulate the strategy tried if one particular strategy is desired for the next updatePartitionState.
    // This can't be moved into the _DegraderLoadBalancerState because we
    private final Strategy                           _strategy;
    private final long      _lastUpdated;

    private final double      _currentOverrideDropRate;
    private final double      _currentAvgClusterLatency;
    private final long        _currentClusterCallCount;


    // We consider this PartitionDegraderLoadBalancerState to be initialized when after an updatePartitionState.
    private final boolean   _initialized;

    private final Map<TrackerClient, Double> _previousMaxDropRate;

    /**
     * This constructor will copy the internal data structure shallowly unlike the other constructor.
     */
    public PartitionDegraderLoadBalancerState(PartitionDegraderLoadBalancerState state,
                                                  long clusterGenerationId,
                                                  long lastUpdated)
    {
      _clusterGenerationId = clusterGenerationId;
      _ring = state._ring;
      _pointsMap = state._pointsMap;
      _strategy = state._strategy;
      _currentOverrideDropRate = state._currentOverrideDropRate;
      _currentAvgClusterLatency = state._currentAvgClusterLatency;
      _recoveryMap = state._recoveryMap;
      _initialized = state._initialized;
      _lastUpdated = lastUpdated;
      _serviceName = state._serviceName;
      _degraderProperties = state._degraderProperties;
      _previousMaxDropRate = new HashMap<TrackerClient, Double>();
      _currentClusterCallCount = state._currentClusterCallCount;
    }

    public PartitionDegraderLoadBalancerState(long clusterGenerationId,
                                         long lastUpdated,
                                         boolean initState,
                                         Map<URI,Integer> pointsMap,
                                         Strategy strategy,
                                         double currentOverrideDropRate,
                                         double currentAvgClusterLatency,
                                         Map<TrackerClient,Double> recoveryMap,
                                         String serviceName,
                                         Map<String, String> degraderProperties,
                                         long currentClusterCallCount)
    {
      _clusterGenerationId = clusterGenerationId;
      _ring = new ConsistentHashRing<URI>(pointsMap);
      _pointsMap = (pointsMap != null) ?
            Collections.unmodifiableMap(new HashMap<URI,Integer>(pointsMap)) :
            Collections.<URI,Integer>emptyMap();
      _strategy = strategy;
      _currentOverrideDropRate = currentOverrideDropRate;
      _currentAvgClusterLatency = currentAvgClusterLatency;
      _recoveryMap = (recoveryMap != null) ?
          Collections.unmodifiableMap(new HashMap<TrackerClient,Double>(recoveryMap)) :
          Collections.<TrackerClient,Double>emptyMap();
      _initialized = initState;
      _lastUpdated = lastUpdated;
      _serviceName = serviceName;
      _degraderProperties = (degraderProperties != null) ?
          Collections.unmodifiableMap(new HashMap<String, String>(degraderProperties)) :
          Collections.<String, String>emptyMap();
      _previousMaxDropRate = new HashMap<TrackerClient, Double>();
      _currentClusterCallCount = currentClusterCallCount;
    }

    public Map<String, String> getDegraderProperties()
    {
      return _degraderProperties;
    }

    private String getServiceName()
    {
      return _serviceName;
    }

    public long getCurrentClusterCallCount()
    {
      return _currentClusterCallCount;
    }

    public long getClusterGenerationId()
    {
      return _clusterGenerationId;
    }

    public long getLastUpdated()
    {
      return _lastUpdated;
    }

    public Ring<URI> getRing()
    {
      return _ring;
    }

    public Map<URI,Integer> getPointsMap()
    {
      return _pointsMap;
    }

    public Strategy getStrategy()
    {
      return _strategy;
    }

    public Map<TrackerClient,Double> getRecoveryMap()
    {
      return _recoveryMap;
    }

    public double getCurrentOverrideDropRate()
    {
      return _currentOverrideDropRate;
    }

    public double getCurrentAvgClusterLatency()
    {
      return _currentAvgClusterLatency;
    }

    public Map<TrackerClient, Double> getPreviousMaxDropRate()
    {
      return _previousMaxDropRate;
    }

    public boolean isInitialized()
    {
      return _initialized;
    }

    @Override
    public String toString()
    {
      return "DegraderLoadBalancerState [_serviceName="+ _serviceName
          + ", _currentClusterCallCount=" + _currentClusterCallCount
          + ", _currentAvgClusterLatency=" + _currentAvgClusterLatency
          + ", _currentOverrideDropRate=" + _currentOverrideDropRate
          + ", _clusterGenerationId=" + _clusterGenerationId
          + ", _lastUpdated=" + _lastUpdated
          + ", _strategy=" + _strategy
          + ", _recoveryMap=" + _recoveryMap
          + "]";
    }
  }

}
