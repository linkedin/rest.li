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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.MapUtil;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.clients.DegraderTrackerClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.DelegatingRingFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.RandomHash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.hashing.SeededRandomHash;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheck;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckClientBuilder;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.message.timing.TimingImportance;
import com.linkedin.r2.message.timing.TimingNameConstants;
import com.linkedin.util.degrader.Degrader;
import com.linkedin.util.degrader.DegraderControl;
import com.linkedin.util.degrader.DegraderImpl;
import com.linkedin.util.RateLimitedLogger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.warn;


/**
 * Implementation of {@link LoadBalancerStrategy} with additional supports partitioning of services whereas
 * the the prior implementations do not.
 *
 * @author David Hoa (dhoa@linkedin.com)
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 * @author Zhenkai Zhu (zzhu@linkedin.com)
 */
public class DegraderLoadBalancerStrategyV3 implements LoadBalancerStrategy
{
  public static final String DEGRADER_STRATEGY_NAME = "degrader";
  public static final String HASH_METHOD_NONE = "none";
  public static final String HASH_METHOD_URI_REGEX = "uriRegex";
  public static final String HASH_SEED = "hashSeed";
  public static final long DEFAULT_SEED = 123456789L;
  public static final double EPSILON = 10e-6;

  private static final Logger _log = LoggerFactory.getLogger(DegraderLoadBalancerStrategyV3.class);
  private static final int MAX_HOSTS_TO_CHECK_QUARANTINE = 10;
  private static final int MAX_RETRIES_TO_CHECK_QUARANTINE = 5;
  private static final double SLOW_START_THRESHOLD = 0.0;
  private static final double FAST_RECOVERY_THRESHOLD = 1.0;
  private static final double FAST_RECOVERY_MAX_DROPRATE = 0.5;
  private static final TimingKey TIMING_KEY = TimingKey.registerNewKey(TimingNameConstants.D2_UPDATE_PARTITION, TimingImportance.LOW);

  private boolean                                     _updateEnabled;
  private volatile DegraderLoadBalancerStrategyConfig _config;
  private volatile HashFunction<Request>              _hashFunction;
  private final DegraderLoadBalancerState _state;

  private final RateLimitedLogger _rateLimitedLogger;

  public DegraderLoadBalancerStrategyV3(DegraderLoadBalancerStrategyConfig config, String serviceName,
      Map<String, String> degraderProperties,
      List<PartitionDegraderLoadBalancerStateListener.Factory> degraderStateListenerFactories)
  {
    _updateEnabled = true;
    setConfig(config);
    if (degraderProperties == null)
    {
      degraderProperties = Collections.emptyMap();
    }
    _state = new DegraderLoadBalancerState(serviceName, degraderProperties, config, degraderStateListenerFactories);
    _rateLimitedLogger = new RateLimitedLogger(_log, config.DEFAULT_UPDATE_INTERVAL_MS, config.getClock());

  }

  @Override
  public String getName()
  {
    return DEGRADER_STRATEGY_NAME;
  }

  private List<DegraderTrackerClient> castToDegraderTrackerClients(Map<URI, TrackerClient> trackerClients)
  {
    List<DegraderTrackerClient> degraderTrackerClients = new ArrayList<>(trackerClients.size());

    for (TrackerClient trackerClient: trackerClients.values())
    {
      if (trackerClient instanceof DegraderTrackerClient)
      {
        degraderTrackerClients.add((DegraderTrackerClient) trackerClient);
      }
      else
      {
        warn(_log,
             "Client passed to DegraderV3 not an instance of DegraderTrackerClient, will not load balance to it.",
             trackerClient);
      }
    }

    return degraderTrackerClients;
  }

  @Override
  public TrackerClient getTrackerClient(Request request,
                                        RequestContext requestContext,
                                        long clusterGenerationId,
                                        int partitionId,
                                        Map<URI, TrackerClient> trackerClients)
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

    List<DegraderTrackerClient> degraderTrackerClients = castToDegraderTrackerClients(trackerClients);

    // only one thread will be allowed to enter updatePartitionState for any partition
    TimingContextUtil.markTiming(requestContext, TIMING_KEY);
    checkUpdatePartitionState(clusterGenerationId, partitionId, degraderTrackerClients);
    TimingContextUtil.markTiming(requestContext, TIMING_KEY);

    Ring<URI> ring =  _state.getRing(partitionId);

    URI targetHostUri = KeyMapper.TargetHostHints.getRequestContextTargetHost(requestContext);
    Set<URI> excludedUris = ExcludedHostHints.getRequestContextExcludedHosts(requestContext);
    if (excludedUris == null)
    {
      excludedUris = new HashSet<>();
    }

    //no valid target host header was found in the request
    DegraderTrackerClient client;
    if (targetHostUri == null)
    {
      client = findValidClientFromRing(request, ring, degraderTrackerClients, excludedUris, requestContext);
    }
    else
    {
      debug(_log, "Degrader honoring target host header in request, skipping hashing.  URI: ", targetHostUri);
      client = searchClientFromUri(targetHostUri, degraderTrackerClients);
      if (client == null)
      {
        warn(_log, "No client found for ", targetHostUri, ". Target host specified is no longer part of cluster");
      }
      else
      {
        // if this flag is set to be true, that means affinity routing is preferred but backup requests are still acceptable
        Boolean otherHostAcceptable = KeyMapper.TargetHostHints.getRequestContextOtherHostAcceptable(requestContext);
        if (otherHostAcceptable != null && otherHostAcceptable)
        {
          ExcludedHostHints.addRequestContextExcludedHost(requestContext, targetHostUri);
        }
      }
    }

    if (client == null)
    {
      return null;
    }

    // Decides whether to drop the call
    Degrader degrader = client.getDegrader(partitionId);
    if (degrader.checkDrop())
    {
      warn(_log, "client's degrader is dropping call for: ", client);
      return null;
    }

    debug(_log, "returning client: ", client);

    // Decides whether to degrade call at the transport layer
    if (degrader.checkPreemptiveTimeout())
    {
      DegraderControl degraderControl = client.getDegraderControl(partitionId);
      requestContext.putLocalAttr(R2Constants.PREEMPTIVE_TIMEOUT_RATE, degraderControl.getPreemptiveRequestTimeoutRate());
    }

    return client;
  }

  private DegraderTrackerClient findValidClientFromRing(Request request, Ring<URI> ring, List<DegraderTrackerClient> trackerClients, Set<URI> excludedUris, RequestContext requestContext)
  {
    // Compute the hash code
    int hashCode = _hashFunction.hash(request);

    if (ring == null)
    {
      warn(_log, "Can not find hash ring to use");
    }

    Map<URI, DegraderTrackerClient> trackerClientMap = new HashMap<>(trackerClients.size());

    for (DegraderTrackerClient trackerClient : trackerClients)
    {
      trackerClientMap.put(trackerClient.getUri(), trackerClient);
    }

    // we operate only on URIs to ensure that we never hold on to an old tracker client
    // that the cluster manager has removed
    URI mostWantedURI = ring.get(hashCode);
    DegraderTrackerClient client = trackerClientMap.get(mostWantedURI);

    if (client != null && !excludedUris.contains(mostWantedURI))
    {
      ExcludedHostHints.addRequestContextExcludedHost(requestContext, mostWantedURI);
      return client;
    }

    // Getting an iterator from the ring is usually an expensive operation. So we only get the iterator
    // if the most wanted URI is unavailable
    Iterator<URI> iterator = ring.getIterator(hashCode);

    // Now we get a URI from the ring. We need to make sure it's valid:
    // 1. It's not in the set of excluded hosts
    // 2. The consistent hash ring might not be updated as quickly as the trackerclients,
    // so there could be an inconsistent situation where the trackerclient is already deleted
    // while the uri still exists in the hash ring. When this happens, instead of failing the search,
    // we simply return the next uri in the ring that is available in the trackerclient list.
    URI targetHostUri = null;

    while (iterator.hasNext())
    {
      targetHostUri = iterator.next();
      client = trackerClientMap.get(targetHostUri);

      if (targetHostUri != mostWantedURI && !excludedUris.contains(targetHostUri) && client != null)
      {
        ExcludedHostHints.addRequestContextExcludedHost(requestContext, targetHostUri);
        return client;
      }
    }

    if (client == null)
    {
      warn(_log, "No client found. Degrader load balancer state is inconsistent with cluster manager");
    }
    else if (excludedUris.contains(targetHostUri))
    {
      client = null;
      warn(_log, "No client found. We have tried all hosts in the cluster");
    }

    return client;
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
  private void checkUpdatePartitionState(long clusterGenerationId, int partitionId, List<DegraderTrackerClient> trackerClients)
  {
    DegraderLoadBalancerStrategyConfig config = getConfig();
    final Partition partition = _state.getPartition(partitionId);
    final Lock lock = partition.getLock();
    boolean partitionUpdated = false;

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
            _log.error("Failed to initialize partition state for partition: ", partitionId);
          }
          else
          {
            partitionUpdated = true;
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
            partitionUpdated = true;
          }
        }
        finally
        {
          lock.unlock();
        }
      }
    }
    if (partitionUpdated)
    {
      // Notify the listeners the state update. We need to do it now because we do not want
      // to hold the lock when notifying the listeners.
      for (PartitionDegraderLoadBalancerStateListener listener : partition.getListeners())
      {
        listener.onUpdate(partition.getState());
      }
    }
  }

  private DegraderTrackerClient searchClientFromUri(URI uri, List<DegraderTrackerClient> trackerClients)
  {
    for (DegraderTrackerClient trackerClient : trackerClients) {
      if (trackerClient.getUri().equals(uri)) {
        return trackerClient;
      }
    }
    return null;
  }

  private void updatePartitionState(long clusterGenerationId, Partition partition, List<DegraderTrackerClient> trackerClients, DegraderLoadBalancerStrategyConfig config)
  {
    PartitionDegraderLoadBalancerState partitionState = partition.getState();

    List<DegraderTrackerClientUpdater> clientUpdaters = new ArrayList<DegraderTrackerClientUpdater>();
    for (DegraderTrackerClient client: trackerClients)
    {
      clientUpdaters.add(new DegraderTrackerClientUpdater(client, partition.getId()));
    }

    boolean quarantineEnabled = _state.isQuarantineEnabled();
    if (config.getQuarantineMaxPercent() > 0.0 && !quarantineEnabled)
    {
      // if quarantine is configured but not enabled, and we haven't tried MAX_RETRIES_TIMES
      // check the hosts to see if the quarantine can be enabled.
      if (_state.incrementAndGetQuarantineRetries() <= MAX_RETRIES_TO_CHECK_QUARANTINE)
      {
        _config.getExecutorService().submit(()->checkQuarantineState(clientUpdaters, config));
      }
    }

    // doUpdatePartitionState has no side effects on _state or trackerClients.
    // all changes to the trackerClients would be recorded in clientUpdaters
    partitionState = doUpdatePartitionState(clusterGenerationId, partition.getId(), partitionState,
                                            config, clientUpdaters, quarantineEnabled);
    partition.setState(partitionState);

    // only if state update succeeded, do we actually apply the recorded changes to trackerClients
    for (DegraderTrackerClientUpdater clientUpdater : clientUpdaters)
    {
      clientUpdater.update();
    }
  }


  static boolean isNewStateHealthy(PartitionDegraderLoadBalancerState newState,
                                   DegraderLoadBalancerStrategyConfig config,
                                   List<DegraderTrackerClientUpdater> degraderTrackerClientUpdaters,
                                   int partitionId)
  {
    if (newState.getCurrentAvgClusterLatency() > config.getLowWaterMark())
    {
      return false;
    }
    return getUnhealthyTrackerClients(degraderTrackerClientUpdaters, newState.getPointsMap(), newState.getQuarantineMap(), config, partitionId).isEmpty();
  }

  private static boolean isNewStateHealthy(PartitionDegraderLoadBalancerState newState,
                                           DegraderLoadBalancerStrategyConfig config,
                                           List<DegraderTrackerClient> unHealthyClients)
  {
    return (newState.getCurrentAvgClusterLatency() <= config.getLowWaterMark()) && unHealthyClients.isEmpty();
  }

  static boolean isOldStateTheSameAsNewState(PartitionDegraderLoadBalancerState oldState,
                                                     PartitionDegraderLoadBalancerState newState)
  {
    // clusterGenerationId check is removed from here. Reasons:
    // 1. Points map will be probably different when clusterGenerationId is different
    // 2. When points map and recovery map both remain the same, we probably don't want to log it here.
    return oldState.getCurrentOverrideDropRate() == newState.getCurrentOverrideDropRate() &&
        oldState.getPointsMap().equals(newState.getPointsMap()) &&
        oldState.getRecoveryMap().equals(newState.getRecoveryMap()) &&
        oldState.getQuarantineMap().equals(newState.getQuarantineMap());
  }

  private static void logState(PartitionDegraderLoadBalancerState oldState,
                        PartitionDegraderLoadBalancerState newState,
                        int partitionId,
                        DegraderLoadBalancerStrategyConfig config,
                        List<DegraderTrackerClient> unHealthyClients,
                        boolean clientDegraded)
  {
    Map<URI, Integer> pointsMap = newState.getPointsMap();
    final int LOG_UNHEALTHY_CLIENT_NUMBERS = 10;

    if (_log.isDebugEnabled())
    {
      _log.debug("Strategy updated: partitionId= " + partitionId + ", newState=" + newState +
          ", unhealthyClients = ["
          + (unHealthyClients.stream().map(client -> getClientStats(client, partitionId, pointsMap, config))
          .collect(Collectors.joining(",")))
          + "], config=" + config +
          ", HashRing coverage=" + newState.getRing());
    }
    else if (allowToLog(oldState, newState, clientDegraded))
    {
      _log.info("Strategy updated: partitionId= " + partitionId + ", newState=" + newState +
          ", unhealthyClients = ["
          + (unHealthyClients.stream().limit(LOG_UNHEALTHY_CLIENT_NUMBERS)
          .map(client -> getClientStats(client, partitionId, pointsMap, config)).collect(Collectors.joining(",")))
          + (unHealthyClients.size() > LOG_UNHEALTHY_CLIENT_NUMBERS ? "...(total " + unHealthyClients.size() + ")" : "")
          + "], oldState =" + oldState + ", new state's config=" + config);
    }
  }

  // We do not always want to log the state when it changes to avoid excessive messages. allowToLog# check the requirements
  private static boolean allowToLog(PartitionDegraderLoadBalancerState oldState, PartitionDegraderLoadBalancerState newState,
      boolean clientDegraded)

  {
    // always log if the cluster level drop rate changes
    if (oldState.getCurrentOverrideDropRate() != newState.getCurrentOverrideDropRate())
    {
      return true;
    }

    // if host number changes or there are clients degraded
    if (oldState.getPointsMap().size() != newState.getPointsMap().size() || clientDegraded)
    {
      return true;
    }

    // if the unHealthyClient number changes
    if (oldState.getUnHealthyClientNumber() != newState.getUnHealthyClientNumber())
    {
      return true;
    }

    // if hosts number changes in recoveryMap or quarantineMap
    return oldState.getRecoveryMap().size() != newState.getRecoveryMap().size()
        || oldState.getQuarantineMap().size() != newState.getQuarantineMap().size();
  }

  private static String getClientStats(DegraderTrackerClient client, int partitionId, Map<URI, Integer> pointsMap,
                                       DegraderLoadBalancerStrategyConfig config)
  {
    DegraderControl degraderControl = client.getDegraderControl(partitionId);
    return client.getUri() + ":" + pointsMap.get(client.getUri()) + "/" +
        String.valueOf(client.getPartitionWeight(partitionId) * client.getSubsetWeight(partitionId) * config.getPointsPerWeight())
        + "(" + degraderControl.getCallTimeStats().getAverage() + "ms)";
  }

  private static List<DegraderTrackerClient> getUnhealthyTrackerClients(List<DegraderTrackerClientUpdater> degraderTrackerClientUpdaters,
                                                                        Map<URI, Integer> pointsMap,
                                                                        Map<DegraderTrackerClient, LoadBalancerQuarantine> quarantineMap,
                                                                        DegraderLoadBalancerStrategyConfig config,
                                                                        int partitionId)
  {
    List<DegraderTrackerClient> unhealthyClients = new ArrayList<>();
    for (DegraderTrackerClientUpdater clientUpdater : degraderTrackerClientUpdaters)
    {
      DegraderTrackerClient client = clientUpdater.getTrackerClient();
      int perfectHealth = (int) (client.getPartitionWeight(partitionId) * client.getSubsetWeight(partitionId) * config.getPointsPerWeight());
      URI uri = client.getUri();
      if (!pointsMap.containsKey(uri))
      {
        _log.warn("Client with URI {} is absent in point map, pointMap={}, quarantineMap={}",
            new Object[] {uri, pointsMap, quarantineMap});
        continue;
      }
      Integer point = pointsMap.get(uri);
      if (point < perfectHealth)
      {
        unhealthyClients.add(client);
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
   * Currently only 500 level return codes are counted into error rate when adjusting the hash ring.
   * The reason we do not consider other errors is that there are legitimate errors that servers can
   * send back for clients to handle, such as 400 return codes.
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
   */
  private static PartitionDegraderLoadBalancerState doUpdatePartitionState(long clusterGenerationId, int partitionId,
                                                                         PartitionDegraderLoadBalancerState oldState,
                                                                         DegraderLoadBalancerStrategyConfig config,
                                                                         List<DegraderTrackerClientUpdater> degraderTrackerClientUpdaters,
                                                                         boolean isQuarantineEnabled)
  {
    debug(_log, "updating state for: ", degraderTrackerClientUpdaters);

    double sumOfClusterLatencies = 0.0;
    long totalClusterCallCount = 0;
    boolean hashRingChanges = false;
    boolean clientDegraded = false;
    boolean recoveryMapChanges = false;
    boolean quarantineMapChanged = false;

    PartitionDegraderLoadBalancerState.Strategy strategy = oldState.getStrategy();
    Map<DegraderTrackerClient, Double> oldRecoveryMap = oldState.getRecoveryMap();
    Map<DegraderTrackerClient, Double> newRecoveryMap = new HashMap<>(oldRecoveryMap);
    double currentOverrideDropRate = oldState.getCurrentOverrideDropRate();
    double initialRecoveryLevel = config.getInitialRecoveryLevel();
    double ringRampFactor = config.getRingRampFactor();
    int pointsPerWeight = config.getPointsPerWeight();
    PartitionDegraderLoadBalancerState newState;
    Map<DegraderTrackerClient, LoadBalancerQuarantine> quarantineMap = oldState.getQuarantineMap();
    Map<DegraderTrackerClient, LoadBalancerQuarantine> quarantineHistory = oldState.getQuarantineHistory();
    Set<DegraderTrackerClient> activeClients = new HashSet<>();
    long clk = config.getClock().currentTimeMillis();
    long clusterErrorCount = 0;
    long clusterDropCount = 0;

    for (DegraderTrackerClientUpdater clientUpdater : degraderTrackerClientUpdaters)
    {
      DegraderTrackerClient client = clientUpdater.getTrackerClient();
      DegraderControl degraderControl = client.getDegraderControl(partitionId);
      double averageLatency = degraderControl.getLatency();
      long callCount = degraderControl.getCallCount();
      clusterDropCount += (int)(degraderControl.getCurrentDropRate() * callCount);
      clusterErrorCount += (int)(degraderControl.getErrorRate() * callCount);

      oldState.getPreviousMaxDropRate().put(client, clientUpdater.getMaxDropRate());

      sumOfClusterLatencies += averageLatency * callCount;
      totalClusterCallCount += callCount;

      activeClients.add(client);
      if (isQuarantineEnabled)
      {
        // Check/update quarantine state if current client is already under quarantine
        LoadBalancerQuarantine quarantine = quarantineMap.get(client);
        if (quarantine != null && quarantine.checkUpdateQuarantineState())
        {
          // Evict client from quarantine
          quarantineMap.remove(client);
          quarantineHistory.put(client, quarantine);
          _log.info("TrackerClient {} evicted from quarantine @ {}", client.getUri(), clk);

          // Next need to put the client to slow-start/recovery mode to gradually pick up traffic.
          // For now simply force the weight to the initialRecoveryLevel so the client can gradually recover
          // RecoveryMap is used here to track the clients that just evicted from quarantine
          // They'll not be quarantined again in the recovery phase even though the effective
          // weight is within the range.
          newRecoveryMap.put(client, degraderControl.getMaxDropRate());
          clientUpdater.setMaxDropRate(1.0 - initialRecoveryLevel);

          quarantineMapChanged = true;
        }
      }

      if (newRecoveryMap.containsKey(client))
      {
        recoveryMapChanges = handleClientInRecoveryMap(degraderControl, clientUpdater, initialRecoveryLevel, ringRampFactor,
            callCount, newRecoveryMap, strategy);
      }
    }

    // trackerClientUpdaters includes all trackerClients for the service of the partition.
    // Check the quarantineMap/quarantineHistory and remove the trackerClients that do not exist
    // in TrackerClientUpdaters -- those URIs were removed from zookeeper
    if (isQuarantineEnabled)
    {
      quarantineMap.entrySet().removeIf(e -> !activeClients.contains(e.getKey()));
      quarantineHistory.entrySet().removeIf(e -> !activeClients.contains(e.getKey()));
    }
    // Also remove the clients from recoveryMap if they are gone
    newRecoveryMap.entrySet().removeIf(e -> !activeClients.contains(e.getKey()));

    if (oldState.getClusterGenerationId() == clusterGenerationId && totalClusterCallCount <= 0
        && !recoveryMapChanges && !quarantineMapChanged)
    {
      // if the cluster has not been called recently (total cluster call count is <= 0)
      // and we already have a state with the same set of URIs (same cluster generation),
      // and no clients are in rehab or evicted from quarantine, then don't change anything.
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

    // This points map stores how many hash map points to allocate for each tracker client.
    Map<URI, Integer> points = new HashMap<URI, Integer>();
    Map<URI, Integer> oldPointsMap = oldState.getPointsMap();

    for (DegraderTrackerClientUpdater clientUpdater : degraderTrackerClientUpdaters)
    {
      DegraderTrackerClient client = clientUpdater.getTrackerClient();
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
      double clientWeight = client.getPartitionWeight(partitionId) * client.getSubsetWeight(partitionId);
      double successfulTransmissionWeight = clientWeight * (1.0 - dropRate);

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

      boolean quarantineEffect = false;

      if (isQuarantineEnabled)
      {
        if (quarantineMap.containsKey(client))
        {
          // If the client is still in quarantine, keep the points to 0 so no real traffic will be used
          newPoints = 0;
          quarantineEffect = true;
        }
        // To put a TrackerClient into quarantine, it needs to meet all the following criteria:
        // 1. its effective weight is less than or equal to the threshold (0.0).
        // 2. The call state in current interval is becoming worse, eg the latency or error rate is
        //    higher than the threshold.
        // 3. its clientWeight is greater than 0 (ClientWeight can be zero when the server's
        //    clientWeight in zookeeper is explicitly set to zero in order to put the server
        //    into standby. In this particular case, we should not put the tracker client into
        //    the quarantine).
        // 4. The total clients in the quarantine is less than the pre-configured number (decided by
        //    HTTP_LB_QUARANTINE_MAX_PERCENT)
        else if (successfulTransmissionWeight <= 0.0 && clientWeight > EPSILON && degraderControl.isHigh())
        {
          if (1.0 * quarantineMap.size() < Math.ceil(degraderTrackerClientUpdaters.size() * config.getQuarantineMaxPercent()))
          {
            // Put the client into quarantine
            LoadBalancerQuarantine quarantine = quarantineHistory.remove(client);
            if (quarantine == null)
            {
              quarantine = new LoadBalancerQuarantine(clientUpdater.getTrackerClient(), config, oldState.getServiceName());
            }

            quarantine.reset(clk);
            quarantineMap.put(client, quarantine);

            newPoints = 0;     // reduce the points to 0 so no real traffic will be used
            _log.warn("TrackerClient {} is put into quarantine {}. OverrideDropRate = {}, callCount = {}, latency = {},"
                + " errorRate = {}",
                new Object[] { client.getUri(), quarantine, degraderControl.getMaxDropRate(),
                    degraderControl.getCallCount(), degraderControl.getLatency(), degraderControl.getErrorRate()});
            quarantineEffect = true;
          }
          else
          {
            _log.error("Quarantine for service {} is full! Could not add {}", oldState.getServiceName(), client);
          }
        }
      }

      // We only enroll the tracker client in the recovery program when clientWeight is not zero but we got zero points.
      // ClientWeight can be zero when the server's clientWeight in zookeeper is explicitly set to zero,
      // in order to put the server into standby. In this particular case, we should not put the tracker
      // client into the recovery program, because we don't want this tracker client to get any traffic.
      if (!quarantineEffect && newPoints == 0 && clientWeight > EPSILON)
      {
        // We are choking off traffic to this tracker client.
        // Enroll this tracker client in the recovery program so that
        // we can make sure it still gets some traffic
        Double oldMaxDropRate = clientUpdater.getMaxDropRate();

        // set the default recovery level.
        newPoints = (int) (initialRecoveryLevel * pointsPerWeight);

        // Keep track of the original maxDropRate
        // We want to exclude the RecoveryMap and MaxDropRate updates during CALL_DROPPING phase because the corresponding
        // pointsMap won't get updated during CALL_DROP phase. In the past this is done by dropping newRecoveryMap for
        // that phase. Now we want to keep newRecoveryMap because fastRecovery and Quarantine can add new clients to the map.
        // Therefore we end up with adding this client to the Map only if it is in LOAD_BALANCE phase.
        if (!newRecoveryMap.containsKey(client) && strategy == PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE)
        {
          // keep track of this client,
          newRecoveryMap.put(client, oldMaxDropRate);
          clientUpdater.setMaxDropRate(1.0 - initialRecoveryLevel);
        }
      }

      // also enroll new client into the recoveryMap if possible
      enrollNewClientInRecoveryMap(newRecoveryMap, oldState, config, degraderControl, clientUpdater);

      points.put(clientUri, newPoints);
      if (!oldPointsMap.containsKey(clientUri) || oldPointsMap.get(clientUri) != newPoints)
      {
        hashRingChanges = true;
        clientDegraded |= oldPointsMap.containsKey(clientUri) && (newPoints < oldPointsMap.get(clientUri));
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
      List<DegraderTrackerClient> unHealthyClients = getUnhealthyTrackerClients(degraderTrackerClientUpdaters, points, quarantineMap, config, partitionId);
      newState =
          new PartitionDegraderLoadBalancerState(clusterGenerationId,
                                                 config.getClock().currentTimeMillis(),
                                                 true,
                                                 oldState.getRingFactory(),
                                                 points,
                                                 PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING,
                                                 currentOverrideDropRate,
                                                 newCurrentAvgClusterLatency,
                                                 newRecoveryMap,
                                                 oldState.getServiceName(),
                                                 oldState.getDegraderProperties(),
                                                 totalClusterCallCount,
                                                 clusterDropCount,
                                                 clusterErrorCount,
                                                 quarantineMap,
                                                 quarantineHistory,
                                                 activeClients,
                                                 unHealthyClients.size());

      logState(oldState, newState, partitionId, config, unHealthyClients, clientDegraded);
    }
    else
    {
      // time to try call dropping strategy, if necessary.
      double newDropLevel = calculateNewDropLevel(config, currentOverrideDropRate, newCurrentAvgClusterLatency,
          totalClusterCallCount);

      if (newDropLevel != currentOverrideDropRate)
      {
        overrideClusterDropRate(partitionId, newDropLevel, degraderTrackerClientUpdaters);
      }

      // don't change the points map, but try load balancing strategy next time.
      // recoveryMap needs to update if quarantine or fastRecovery is enabled. This is because the client will not
      // have chance to get in in next interval (already evicted from quarantine or not a new client anymore).
      List<DegraderTrackerClient> unHealthyClients = getUnhealthyTrackerClients(degraderTrackerClientUpdaters, oldPointsMap, quarantineMap, config, partitionId);
      newState =
              new PartitionDegraderLoadBalancerState(clusterGenerationId, config.getClock().currentTimeMillis(), true,
                                            oldState.getRingFactory(),
                                            oldPointsMap,
                                            PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE,
                                            newDropLevel,
                                            newCurrentAvgClusterLatency,
                                            newRecoveryMap,
                                            oldState.getServiceName(),
                                            oldState.getDegraderProperties(),
                                            totalClusterCallCount,
                                            clusterDropCount,
                                            clusterErrorCount,
                                            quarantineMap,
                                            quarantineHistory,
                                            activeClients,
                                            unHealthyClients.size());

      logState(oldState, newState, partitionId, config, unHealthyClients, clientDegraded);

      points = oldPointsMap;
    }

    // adjust the min call count for each client based on the hash ring reduction and call dropping
    // fraction.
    overrideMinCallCount(partitionId, currentOverrideDropRate, degraderTrackerClientUpdaters, points, pointsPerWeight);

    return newState;
  }

  /**
   /**
   * Enroll new client into RecoveryMap
   *
   * When fastRecovery mode is enabled, we want to enroll the new client into recoveryMap to help its recovery
   *
   */
  private static void enrollNewClientInRecoveryMap(Map<DegraderTrackerClient, Double> recoveryMap,
      PartitionDegraderLoadBalancerState state, DegraderLoadBalancerStrategyConfig config,
      DegraderControl degraderControl, DegraderTrackerClientUpdater clientUpdater)
  {
    DegraderTrackerClient client = clientUpdater.getTrackerClient();

    if (!recoveryMap.containsKey(client)                                      // client is not in the map yet
        && !state.getTrackerClients().contains(client)                        // client is new
        && config.getRingRampFactor() > FAST_RECOVERY_THRESHOLD               // Fast recovery is enabled
        && degraderControl.getInitialDropRate() > SLOW_START_THRESHOLD        // Slow start is enabled
        && !degraderControl.isHigh())                                         // current client is not degrading or QPS is too low
    {
      recoveryMap.put(client, clientUpdater.getMaxDropRate());
      // also set the maxDropRate to the computedDropRate if not 1;
      double maxDropRate = 1.0 - config.getInitialRecoveryLevel();
      clientUpdater.setMaxDropRate(Math.min(degraderControl.getCurrentComputedDropRate(), maxDropRate));
    }
  }

  /**
   * Unsynchronized
   *
   * @param override
   * @param degraderTrackerClientUpdaters
   */
  public static void overrideClusterDropRate(int partitionId, double override, List<DegraderTrackerClientUpdater> degraderTrackerClientUpdaters)
  {
    debug(_log,
            "partitionId=",
            partitionId,
            "overriding degrader drop rate to ",
            override,
            " for clients: ", degraderTrackerClientUpdaters);

    for (DegraderTrackerClientUpdater clientUpdater : degraderTrackerClientUpdaters)
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
   * @param degraderTrackerClientUpdaters
   * @param pointsMap
   * @param pointsPerWeight
   */
  public static void overrideMinCallCount(int partitionId, double newOverrideDropRate, List<DegraderTrackerClientUpdater> degraderTrackerClientUpdaters,
                                   Map<URI,Integer> pointsMap, int pointsPerWeight)
  {
    for (DegraderTrackerClientUpdater clientUpdater : degraderTrackerClientUpdaters)
    {
      DegraderTrackerClient client = clientUpdater.getTrackerClient();
      DegraderControl degraderControl = client.getDegraderControl(partitionId);
      int currentOverrideMinCallCount = client.getDegraderControl(partitionId).getOverrideMinCallCount();
      double hashFactor = pointsMap.get(client.getUri()) / pointsPerWeight;
      double transmitFactor = 1.0 - newOverrideDropRate;
      int newOverrideMinCallCount = (int) Math.max(Math.round(degraderControl.getMinCallCount() *
                                                       hashFactor * transmitFactor), 1);

      if (newOverrideMinCallCount != currentOverrideMinCallCount)
      {
        clientUpdater.setOverrideMinCallCount(newOverrideMinCallCount);
        // log min call count change if current value != initial value
        if (currentOverrideMinCallCount != DegraderImpl.DEFAULT_OVERRIDE_MIN_CALL_COUNT)
        {
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
  }

  /**
   * We should update if we have no prior state, or the state's generation id is older
   * than the current cluster generation if we don't want to update it only at interval,
   * or the state was last updated more than _updateIntervalMs ago.
   *
   * isUpdateOnlyAtInterval is used to determine whether want the update only happening
   * at the end of each interval. Updating partition state is expensive because of the reconstruction
   * of hash ring, so enabling this config may save some time for clusters changed very frequently.
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
            !config.isUpdateOnlyAtInterval() && partitionState.getClusterGenerationId() != clusterGenerationId ||
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
      _hashFunction = hashConfig.containsKey(HASH_SEED)
          ? new SeededRandomHash(MapUtil.getWithDefault(hashConfig, HASH_SEED, DEFAULT_SEED)) : new RandomHash();
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

  @Nonnull
  @Override
  public Ring<URI> getRing(long clusterGenerationId, int partitionId, Map<URI, TrackerClient> trackerClients)
  {
    if (trackerClients.isEmpty())
    {
      // returning empty ring (any implementation) and preventing to update the state with no trackers
      // to be consistent with the behavior in getTrackerClient
      return new DelegatingRingFactory<URI>(_config).createRing(Collections.emptyMap(), Collections.emptyMap());
    }

    checkUpdatePartitionState(clusterGenerationId, partitionId, castToDegraderTrackerClients(trackerClients));
    return _state.getRing(partitionId);
  }

  /**
   * this call returns the ring. Ring can be null depending whether the state has been initialized or not
   * @param partitionId partition id
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

  /**
   * @return hashfunction used on requests to determine sticky routing key (if enabled).
   */
  public HashFunction<Request> getHashFunction()
  {
    return _hashFunction;
  }

  @Override
  public void shutdown()
  {
    _state.shutdown(_config);
  }

  /**
   * checkQuarantineState decides if the D2Quarantine can be enabled or not, by health
   * checking all the trackerClients once. It enables quarantine only if at least one of the
   * clients return success for the checking.
   *
   * The reasons for this checking include:
   *
   * . The default method "OPTIONS" is not always enabled by the service
   * . The user can config any path/method for checking. We do a sanity checking to
   *   make sure the configuration is correct, and service/host responds in time.
   *   Otherwise the host can be kept in quarantine forever if we blindly enable it.
   *
   * This check actually can warm up the R2 connection pool by making a connection to
   * each trackerClient. However since the check happens before any real requests are sent,
   * it generally takes much longer time to get the results, due to different warming up
   * requirements. Therefore the checking will be retried in next update if current check
   * fails.

   *
   * This function is supposed to be protected by the update lock.
   *
   * @param clients
   * @param config
   */
  private void checkQuarantineState(List<DegraderTrackerClientUpdater> clients, DegraderLoadBalancerStrategyConfig config)
  {
    Callback<None>  healthCheckCallback = new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        // Do nothing as the quarantine is disabled by default
        if (!_state.isQuarantineEnabled())
        {
          // No need to log the error message if quarantine is already enabled
          _rateLimitedLogger.warn("Error enabling quarantine. Health checking failed for service {}: ",
              _state.getServiceName(), e);
        }
      }

      @Override
      public void onSuccess(None result)
      {
        if (_state.tryEnableQuarantine())
        {
          _log.info("Quarantine is enabled for service {}", _state.getServiceName());
        }
      }
    };

    // Ideally we would like to healthchecking all the service hosts (ie all TrackerClients) because
    // this can help to warm up the R2 connections to the service hosts, thus speed up the initial access
    // speed when d2client starts to access those hosts. However this can expose/expedite the problem that
    // the d2client host needs too many connections or file handles to all the hosts, when the downstream
    // services have large amount of hosts. Before that problem is addressed, we limit the number of hosts
    // for pre-healthchecking to a small number
    clients.stream().limit(MAX_HOSTS_TO_CHECK_QUARANTINE)
        .forEach(client -> {
          try
          {
            HealthCheck healthCheckClient = _state.getHealthCheckMap().get(client);
            if (healthCheckClient == null)
            {
              // create a new client if not exits
              healthCheckClient =  new HealthCheckClientBuilder()
                  .setHealthCheckOperations(config.getHealthCheckOperations())
                  .setHealthCheckPath(config.getHealthCheckPath())
                  .setServicePath(config.getServicePath())
                  .setClock(config.getClock())
                  .setLatency(config.getQuarantineLatency())
                  .setMethod(config.getHealthCheckMethod())
                  .setClient(client.getTrackerClient())
                  .build();
              _state.putHealthCheckClient(client, healthCheckClient);
            }
            healthCheckClient.checkHealth(healthCheckCallback);
          }
          catch (URISyntaxException e)
          {
            _log.error("Error to build healthCheckClient ", e);
          }
        });

    // also remove the entries that the corresponding trackerClientUpdaters do not exist anymore
    for (DegraderTrackerClientUpdater client : _state.getHealthCheckMap().keySet())
    {
      if (!clients.contains(client))
      {
        _state.getHealthCheckMap().remove(client);
      }
    }
  }

  /**
   * recoveryMap is the incubator for client to recovery in case the QPS is low. This function decides the fate for the
   * clients in the recoveryMap:
   * 1. Keep them in the map, but increase their chances to get requests (when qps is too low)
   * 2. Keep them in the map and wait their recovery (when fastRecovery is enabled and the clients are healthy)
   * 3. Get them out
   *
   * @return: true if the recoveryMap changed, false otherwise.
   */
  private static boolean handleClientInRecoveryMap(DegraderControl degraderControl, DegraderTrackerClientUpdater clientUpdater,
                                                   double initialRecoveryLevel, double ringRampFactor, long callCount,
                                                   Map<DegraderTrackerClient, Double> newRecoveryMap, PartitionDegraderLoadBalancerState.Strategy strategy)
  {
    if (callCount < degraderControl.getMinCallCount())
    {
      // The following block of code calculates and updates the maxDropRate if the client had been
      // fully degraded in the past and has not received enough requests since being fully degraded.
      // To increase the chances of the client receiving a request, we change the maxDropRate, which
      // influences the maximum value of computedDropRate, which is used to compute the number of
      // points in the hash ring for the clients.
      if (strategy == PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE)
      {
        // if it's the hash ring's turn to adjust, then adjust the maxDropRate.
        // Otherwise, we let the call dropping strategy take it's turn, even if
        // it may do nothing.

        // if this client is enrolled in the program, and the traffic is too low (so it won't be able to recvoer),
        // decrease the maxDropRate
        double oldMaxDropRate = clientUpdater.getMaxDropRate();
        double transmissionRate = 1.0 - oldMaxDropRate;
        if (transmissionRate <= 0.0)
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

        clientUpdater.setMaxDropRate(1.0 - transmissionRate);
      }
    }
    // It is generally harder for the low average QPS hosts to recover, because the healthy clients have
    // much higher chances to get the requests. We introduce the new FAST_RECOVERY mode to address this type
    // of problem. The idea is to keep the client enrolled in the recoveryMap even if it gets traffic, until
    // the computed droprate is less than the given threshold (currently defined as lesser of 0.5 or the maxDropRate).
    //
    // Note:
    // 1. in this mode the client is kept in the map only if it is still healthy (ie latency < degrader.highLatency &&
    //    errorRate < degrader.highErrorRate).
    // 2. rampFactor has no effect on the computedDropRate. But the computedDropRate is used for points calculation
    //    when the client gets out of recoveryMap. That's why we want to keep the client in the map until its
    //    calculatedDropRate catch up with the maxDropRate. Here is an example (assume slowStart is enabled,
    //    rampFactor is 2, and degrader.downStep is 0.2):
    //    PreCallCount  MaxDropRate/transmissionRate  ComputedDropRate  HashRingPoints    In RecoveryMap?  Comments
    //         0                  99/1                    99                  1                   y
    //         0                  98/2                    99                  2                   y
    //         ...
    //         0                  84/16                   99                  16                  y        No traffic so far
    //         1                  84/16                   98 (99 - 1)         16                  y        ComputedDropRate recovering
    //         1                  84/16                   96 (98 - 2)         16                  y
    //         0                  68/32                   96                  32                  y        No traffic again, MaxDropRate updated
    //         1                  68/32                   92                  32                  y        recovering with traffic
    //         ...
    //         1                  68/32                   84                  32                  y        slowStart recovery done
    //         1                  68/32                   64 (84 - 20)        36                  y
    //         1                  68/32                   44 (64 - 20)        56                  n        get out of recoveryMap
    //         1                  100 (restored)          24 (44 - 20)        76                  n        continue recovering
    //         1                  100                     4  (24 - 20)        96                  n
    //         1                  100                     0                   100                 n        fully recovered
    //
    else if (ringRampFactor > FAST_RECOVERY_THRESHOLD && !degraderControl.isHigh()
        && degraderControl.getCurrentComputedDropRate() > Math.min(FAST_RECOVERY_MAX_DROPRATE, clientUpdater.getMaxDropRate()))
    {
      // If we come to this block, it means:
      //    1. we're getting traffic and it's healthy (so we're recovering, ie computedDropRate is going up)
      //    2. the computedDropRate is still higher than the threshold. The threshold is defined as min(0.5, maxDropRate).
      //       If we already force the maxDropRate to a rate lower than 0.5, we want to keep the client recovers
      //       beyond that point before get it out.
      //
      // Keep the client in the map and wait for the client further recovering.
    }
    else
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
      DegraderTrackerClient client = clientUpdater.getTrackerClient();
      clientUpdater.setMaxDropRate(newRecoveryMap.get(client));
      newRecoveryMap.remove(client);
    }
    // Always return true to bypass early return (ie get the state update).
    return true;
  }

  // Calculate DropRate
  private static double calculateNewDropLevel(DegraderLoadBalancerStrategyConfig config,
      double currentOverrideDropRate, double newCurrentAvgClusterLatency,
      long totalClusterCallCount)
  {
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
    return newDropLevel;
  }

  // for unit testing, this allows the strategy to be forced for the next time updatePartitionState
  // is called. This is not to be used in prod code.
  void setStrategy(int partitionId, PartitionDegraderLoadBalancerState.Strategy strategy)
  {
    final Partition partition = _state.getPartition(partitionId);
    PartitionDegraderLoadBalancerState oldState = partition.getState();
    PartitionDegraderLoadBalancerState newState =
        new PartitionDegraderLoadBalancerState(oldState.getClusterGenerationId(), oldState.getLastUpdated(), oldState.isInitialized(),
                                             oldState.getRingFactory(),
                                             oldState.getPointsMap(),
                                             strategy,
                                             oldState.getCurrentOverrideDropRate(),
                                             oldState.getCurrentAvgClusterLatency(),
                                             oldState.getRecoveryMap(),
                                             oldState.getServiceName(),
                                             oldState.getDegraderProperties(),
                                             oldState.getCurrentClusterCallCount(),
                                             oldState.getCurrentClusterDropCount(),
                                             oldState.getCurrentClusterErrorCount(),
                                             oldState.getQuarantineMap(),
                                             oldState.getQuarantineHistory(),
                                             oldState.getTrackerClients(),
                                             oldState.getUnHealthyClientNumber());

    partition.setState(newState);
  }

  @Override
  public String toString()
  {
    return "DegraderLoadBalancerStrategyV3 [_config=" + _config
        + ", _state=" + _state + "]";
  }
}

