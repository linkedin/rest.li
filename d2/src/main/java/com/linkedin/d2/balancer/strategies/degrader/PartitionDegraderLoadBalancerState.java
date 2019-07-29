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
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.util.degrader.CallTracker;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A helper class that contains all state for the degrader load balancer. This allows us
 * to overwrite state with a single write.
 *
 * @author criccomini
 *
 */
public class PartitionDegraderLoadBalancerState
{
  // These are the different strategies we have for handling load and bad situations:
  // load balancing (involves adjusting the number of points for a tracker client in the hash ring). or
  // call dropping (drop a fraction of traffic that otherwise would have gone to a particular Tracker client.
  public enum Strategy
  {
    LOAD_BALANCE,
    CALL_DROPPING
  }

  private final RingFactory<URI> _ringFactory;
  private final Ring<URI> _ring;
  private final long _clusterGenerationId;
  private final String _serviceName;
  private final Map<String, String> _degraderProperties;

  private final Map<URI, Integer>                  _pointsMap;

  // Used to keep track of Clients that have been ramped down to the minimum level in the hash
  // ring, and are slowly being ramped up until they start receiving traffic again.
  private final Map<TrackerClient,Double>          _recoveryMap;

  // quarantineMap is the active quarantine for trackerClient
  private final Map<TrackerClient, DegraderLoadBalancerQuarantine> _quarantineMap;
  // quarantineHistory saves all previous trackerClients that are once quarantined
  private final Map<TrackerClient, DegraderLoadBalancerQuarantine> _quarantineHistory;

  // Because we will alternate between Load Balancing and Call Dropping strategies, we keep track of
  // the strategy to try to aid us in alternating strategies when updatingState. There is a setter
  // to manipulate the strategy tried if one particular strategy is desired for the next updatePartitionState.
  // This can't be moved into the _DegraderLoadBalancerState because we
  private final Strategy                           _strategy;
  private final long      _lastUpdated;

  private final double      _currentOverrideDropRate;
  private final double      _currentAvgClusterLatency;
  private final long        _currentClusterCallCount;
  private final long        _currentClusterDropCount;
  private final long        _currentClusterErrorCount;
  private final int         _unHealthyClientNumber;


  // We consider this PartitionDegraderLoadBalancerState to be initialized when after an updatePartitionState.
  private final boolean   _initialized;

  private final Map<TrackerClient, Double> _previousMaxDropRate;

  private final Set<TrackerClient> _trackerClients;

  /**
   * This constructor will copy the internal data structure shallowly unlike the other constructor.
   * It also resets several states to 0;
   */
  PartitionDegraderLoadBalancerState(PartitionDegraderLoadBalancerState state,
      long clusterGenerationId,
      long lastUpdated)
  {
    _clusterGenerationId = clusterGenerationId;
    _ringFactory = state._ringFactory;
    _ring = state._ring;
    _pointsMap = state._pointsMap;
    _strategy = state._strategy;
    _currentOverrideDropRate = state._currentOverrideDropRate;
    _currentAvgClusterLatency = 0;
    _currentClusterDropCount = 0;
    _currentClusterErrorCount = 0;
    _recoveryMap = state._recoveryMap;
    _initialized = state._initialized;
    _lastUpdated = lastUpdated;
    _serviceName = state._serviceName;
    _degraderProperties = state._degraderProperties;
    _previousMaxDropRate = new HashMap<TrackerClient, Double>();
    _currentClusterCallCount = 0;
    _quarantineMap = state._quarantineMap;
    _quarantineHistory = state._quarantineHistory;
    _trackerClients = state._trackerClients;
    _unHealthyClientNumber = state._unHealthyClientNumber;
  }

  public PartitionDegraderLoadBalancerState(long clusterGenerationId,
      long lastUpdated,
      boolean initState,
      RingFactory<URI> ringFactory,
      Map<URI,Integer> pointsMap,
      Strategy strategy,
      double currentOverrideDropRate,
      double currentAvgClusterLatency,
      Map<TrackerClient,Double> recoveryMap,
      String serviceName,
      Map<String, String> degraderProperties,
      long currentClusterCallCount,
      long currentClusterDropCount,
      long currentClusterErrorCount,
      Map<TrackerClient, DegraderLoadBalancerQuarantine> quarantineMap,
      Map<TrackerClient, DegraderLoadBalancerQuarantine> quarantineHistory,
      Set<TrackerClient> trackerClients,
      int unHealthyClientNumber)
  {
    _clusterGenerationId = clusterGenerationId;
    _ringFactory = ringFactory;
    _pointsMap = (pointsMap != null) ?
        Collections.unmodifiableMap(new HashMap<URI,Integer>(pointsMap)) :
        Collections.<URI,Integer>emptyMap();

    Map<URI, CallTracker> callTrackerMap = (trackerClients != null) ?
        Collections.unmodifiableMap(
            trackerClients.stream()
                .collect(Collectors.toMap(TrackerClient::getUri, TrackerClient::getCallTracker))) :
        Collections.<URI, CallTracker>emptyMap();

    _ring = ringFactory.createRing(pointsMap, callTrackerMap);
    _strategy = strategy;
    _currentOverrideDropRate = currentOverrideDropRate;
    _currentAvgClusterLatency = currentAvgClusterLatency;
    _currentClusterDropCount = currentClusterDropCount;
    _currentClusterErrorCount = currentClusterErrorCount;
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
    _quarantineMap = quarantineMap;
    _quarantineHistory = quarantineHistory;
    _trackerClients = trackerClients;
    _unHealthyClientNumber = unHealthyClientNumber;
  }

  public Map<String, String> getDegraderProperties()
  {
    return _degraderProperties;
  }

  public String getServiceName()
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

  public Map<TrackerClient, DegraderLoadBalancerQuarantine> getQuarantineMap()
  {
    return _quarantineMap;
  }

  public Map<TrackerClient, DegraderLoadBalancerQuarantine> getQuarantineHistory()
  {
    return _quarantineHistory;
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

  public RingFactory<URI> getRingFactory() {
    return _ringFactory;
  }

  public long getCurrentClusterDropCount()
  {
    return _currentClusterDropCount;
  }

  public long getCurrentClusterErrorCount()
  {
    return _currentClusterErrorCount;
  }

  public int getUnHealthyClientNumber()
  {
    return _unHealthyClientNumber;
  }

  public Set<TrackerClient> getTrackerClients()
  {
    return Collections.unmodifiableSet(_trackerClients == null ? Collections.emptySet() : _trackerClients);
  }

  @Override
  public String toString()
  {
    final int LOG_RECOVERY_MAP_HOSTS = 10;

    return "DegraderLoadBalancerState [_serviceName="+ _serviceName
        + ", _currentClusterCallCount=" + _currentClusterCallCount
        + ", _currentAvgClusterLatency=" + _currentAvgClusterLatency
        + ", _currentOverrideDropRate=" + _currentOverrideDropRate
        + ", _currentClusterDropCount=" + _currentClusterDropCount
        + ", _currentClusterErrorCount=" + _currentClusterErrorCount
        + ", _clusterGenerationId=" + _clusterGenerationId
        + ", _unHealthyClientNumber=" + _unHealthyClientNumber
        + ", _strategy=" + _strategy
        + ", _numHostsInCluster=" + (getTrackerClients().size())
        + ", _recoveryMap={" + _recoveryMap.entrySet().stream().limit(LOG_RECOVERY_MAP_HOSTS)
        .map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(","))
        + (_recoveryMap.size() <= LOG_RECOVERY_MAP_HOSTS ? "}" : "...(total " + _recoveryMap.size() + ")}")
        + ", _quarantineList=" + _quarantineMap.values()
        + "]";
  }
}
