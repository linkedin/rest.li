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

package com.linkedin.d2.balancer.strategies.framework;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.test.util.ClockedExecutor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Create the load balancer tests with flexible settings
 * The class creates an instance of the given strategy, run {@link LoadBalancerStrategy#getTrackerClient(Request, RequestContext, long, int, List)}
 * with the settings such as hosts, number of intervals, QPS and latency.
 * Please use {@link LoadBalancerStrategyTestRunnerBuilder} to create the test runner
 *
 * To run the test, use {@link LoadBalancerStrategyTestRunner#run()}
 *
 * Internally we run the test in the following steps:
 * 1. Identify the number of intervals, run iterations
 * 2. In each iteration, send requests based on call count for this interval.
 *    Each iteration is executed at the beginning of an interval
 */
public class LoadBalancerStrategyTestRunner
{
  private static final Logger _log = LoggerFactory.getLogger(LoadBalancerStrategyTestRunner.class);
  private static final long DEFAULT_GENERATION_ID = 0L;
  private static final int DEFAULT_PARTITION_ID = 0;

  private final LoadBalancerStrategy _strategy;
  private final String _serviceName;
  private final List<TrackerClient> _trackerClients;
  private final int _numIntervals;
  private final RequestCountManager _requestsManager;
  private final ClockedExecutor _clockedExecutor;

  // Performance stats
  private Map<URI, Integer> _currentErrorMap;
  private Map<URI, Integer> _lastRequestCountMap;
  private Map<URI, Integer> _currentRequestCountMap;
  private Map<URI, Integer> _callCountMap;
  private Map<URI, Long> _latencySumMap;
  private Map<URI, List<Integer>> _pointHistoryMap = new HashMap<>();

  public LoadBalancerStrategyTestRunner(LoadBalancerStrategy strategy, String serviceName,
      List<TrackerClient> trackerClients,
      int numIntervals, RequestCountManager requestsManager, ClockedExecutor clockedExecutor, Map<URI, Integer> currentErrorMap,
      Map<URI, Integer> lastRequestCountMap, Map<URI, Integer> currentRequestCountMap, Map<URI, Integer> callCountMap, Map<URI, Long> latencySumMap)
  {
    _strategy = strategy;
    _serviceName = serviceName;
    _numIntervals = numIntervals;
    _requestsManager = requestsManager;
    _trackerClients = trackerClients;
    _clockedExecutor = clockedExecutor;

    _currentErrorMap = currentErrorMap;
    _lastRequestCountMap = lastRequestCountMap;
    _currentRequestCountMap = currentRequestCountMap;
    _callCountMap = callCountMap;
    _latencySumMap = latencySumMap;
  }

  public List<TrackerClient> getTrackerClients()
  {
    return _trackerClients;
  }

  public URI getUri(int index)
  {
    return _trackerClients.get(index).getUri();
  }

  /**
   * Get points of the partitions at the end of the test
   * @return The URI to points map
   */
  public Map<URI, Integer> getPoints()
  {
    // TODO: Add other strategy types here
    if (_strategy instanceof DegraderLoadBalancerStrategyV3)
    {
      return ((DegraderLoadBalancerStrategyV3) _strategy).getState().getPartitionState(DEFAULT_PARTITION_ID).getPointsMap();
    }
    // We should not get points if the strategy is not using hash ring
    return new HashMap<>();
  }

  /**
   * Get the points history for past intervals
   */
  public Map<URI, List<Integer>> getPointHistory()
  {
    return _pointHistoryMap;
  }

  /**
   * Get the average latency for all the hosts during the test
   * @return the average latency for all the hosts during the test
   */
  public double getAvgLatency()
  {
    long latencySum = 0;
    int callCountTotal = 0;

    for (URI uri : _callCountMap.keySet())
    {
      callCountTotal += _callCountMap.getOrDefault(uri, 0);
      latencySum += _latencySumMap.getOrDefault(uri, 0L);
    }

    return latencySum / callCountTotal;
  }

  /**
   * Run the test until it finishes
   */
  public void runWait()
  {
    Future<Void> running = run();
    if (running != null)
    {
      try
      {
        running.get();
      }
      catch (InterruptedException | ExecutionException e)
      {
        _log.error("Test running interrupted", e);
      }
    }
  }

  /**
   * Run the mocked test for the given intervals, each interval is scheduled to be run at the fixed interval time
   */
  private Future<Void> run()
  {
    _clockedExecutor.scheduleWithFixedDelay(new Runnable()
    {
      @Override
      public void run()
      {
        runInterval();
      }
    }, 10, LoadBalancerStrategyTestRunnerBuilder.INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS);
    return _clockedExecutor.runFor(LoadBalancerStrategyTestRunnerBuilder.INTERVAL_IN_MILLIS * _numIntervals);
  }

  /**
   * Execute one interval with the given request count
   */
  private void runInterval()
  {
    int currentIntervalIndex = (int) (_clockedExecutor.currentTimeMillis() / LoadBalancerStrategyTestRunnerBuilder.INTERVAL_IN_MILLIS);
    int requestCount = _requestsManager.getRequestCount(currentIntervalIndex);

    for (int i = 0; i < requestCount; i++)
    {
      // construct the requests
      URIRequest uriRequest = new URIRequest("d2://" + _serviceName + "/" + i);
      RestRequest restRequest = new RestRequestBuilder(uriRequest.getURI()).build();
      RequestContext requestContext = new RequestContext();

      // Get client with default generation id and cluster id
      TrackerClient trackerClient =
          _strategy.getTrackerClient(restRequest, requestContext, DEFAULT_GENERATION_ID, DEFAULT_PARTITION_ID, _trackerClients);

      TransportCallback<RestResponse> restCallback = (response) ->
      {
      };
      if (trackerClient != null)
      {
        // Send the request to the picked host if the decision is not DROP
        trackerClient.restRequest(restRequest, requestContext, Collections.emptyMap(), restCallback);

        // Increase the count in the current request count map
        URI uri = trackerClient.getUri();
        if (_currentRequestCountMap.containsKey(trackerClient.getUri()))
        {
          _currentRequestCountMap.put(uri, _currentRequestCountMap.get(uri) + 1);
        } else {
          _currentRequestCountMap.put(uri, 1);
        }
      }
    }
    _currentErrorMap.clear();
    _lastRequestCountMap.clear();
    _lastRequestCountMap.putAll(_currentRequestCountMap);
    _currentRequestCountMap = new HashMap<>();

    // Collect health points stats in this iteration
    Map<URI, Integer> currentPointsMap = getPoints();
    for (URI uri : currentPointsMap.keySet())
    {
      _pointHistoryMap.putIfAbsent(uri,  new ArrayList<>());
      _pointHistoryMap.get(uri).add(currentPointsMap.getOrDefault(uri, 0));
    }
  }
}
