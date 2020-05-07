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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.degrader.DegraderConfigFactory;
import com.linkedin.d2.loadBalancerStrategyType;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.test.util.ClockedExecutor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.eclipse.jetty.http.HttpStatus;

import static com.linkedin.d2.balancer.properties.PropertyKeys.*;


/**
 * Builder class to build a {@link LoadBalancerStrategyTestRunner}
 */
public class LoadBalancerStrategyTestRunnerBuilder
{
  public static final long INTERVAL_IN_MILLIS = 5000L;
  private static final String URI_PREFIX = "http://test.qa";
  private static final String URI_SUFFIX = ".com:5555";

  // Some default values to build default test scenarios
  private static final String DEFAULT_SERVICE_NAME = "dummyService";
  private static final String DEFAULT_CLUSTER_NAME = "dummyCluster";
  private static final String DEFAULT_PATH = "/path";
  // This strategy list is not in use, please use the loadBalancerType to indicate the type of strategy
  private static final List<String> DEFAULT_STRATEGY_LIST = Arrays.asList("DEGRADER", "RANDOM", "RELATIVE");
  private static final int DEFAULT_NUM_HOSTS = 5;
  private static final int DEFAULT_REQUESTS_PER_INTERVAL = 1000;
  private static final String DEFAULT_HIGH_ERROR_RATE = "0.2";
  private static final String DEFAULT_LOW_ERROR_RATE = "0.05";
  private static final int HEALTHY_ERROR_COUNT = 0;
  private static final int UNHEALTHY_ERROR_COUNT = 100;
  private static final long UNHEALTHY_HOST_CONSTANT_LATENCY = 1000L;
  private static final long HEALTHY_HOST_CONSTANT_LATENCY = 50L;
  private static final LatencyCorrelation HEALTHY_HOST_LATENCY_CORRELATION =
      (callCount, intervalIndex) -> HEALTHY_HOST_CONSTANT_LATENCY;
  private static final ErrorCountCorrelation HEALTHY_HOST_ERROR_COUNT_CORRELATION =
      (callCount, intervalIndex) -> HEALTHY_ERROR_COUNT;
  // As time goes, the host latency becomes longer and longer
  private static final LatencyCorrelation HOST_BECOMING_UNHEALTHY_LATENCY =
      (callCount, intervalIndex) -> Long.min(HEALTHY_HOST_CONSTANT_LATENCY + intervalIndex * 500L, UNHEALTHY_HOST_CONSTANT_LATENCY);
  // As time goes, the host latency becomes shorter and shorter and recovers to healthy state
  private static final LatencyCorrelation HOST_RECOVERING_TO_HEALTHY_LATENCY =
      (callCount, intervalIndex) -> Long.max(UNHEALTHY_HOST_CONSTANT_LATENCY - intervalIndex * 100L, HEALTHY_HOST_CONSTANT_LATENCY);
  // As time goes, the host latency becomes bigger and bigger
  private static final ErrorCountCorrelation HOST_BECOMING_UNHEALTHY_ERROR =
      (callCount, intervalIndex) -> Integer.min(HEALTHY_ERROR_COUNT + intervalIndex * 10, UNHEALTHY_ERROR_COUNT);
  // As time goes, the host error count comes to 0
  private static final ErrorCountCorrelation HOST_RECOVERING_TO_HEALTHY_ERROR =
      (callCount, intervalIndex) -> Integer.max(UNHEALTHY_ERROR_COUNT - intervalIndex * 10, HEALTHY_ERROR_COUNT);
  @SuppressWarnings("serial")
  private static final Map<Integer, PartitionData> DEFAULT_PARTITION_DATA_MAP = new HashMap<Integer, PartitionData>()
  {{
      put(0, new PartitionData(1.0));
  }};
  @SuppressWarnings("serial")
  private static final Map<String, String> DEGRADER_PROPERTIES_WITH_HIGH_LOW_ERROR = new HashMap<String, String>()
  {{
      put(DEGRADER_HIGH_ERROR_RATE, DEFAULT_HIGH_ERROR_RATE);
      put(DEGRADER_LOW_ERROR_RATE, DEFAULT_LOW_ERROR_RATE);
  }};

  private LoadBalancerStrategy _strategy;
  private ServiceProperties _serviceProperties;
  private Map<URI, Map<Integer, PartitionData>> _partitionDataMap = new HashMap<>();
  private String _serviceName;
  private List<URI> _uris;
  private List<MockTransportClient> _transportClients;
  private int _numIntervals;
  private LatencyManager _latencyManager;
  private ErrorCountManager _errorCountManager;
  private RequestCountManager _requestCountManager;
  private final loadBalancerStrategyType _type;
  private final ClockedExecutor _clockedExecutor = new ClockedExecutor();
  private final Object _lock = new Object();

  // Performance stats
  private Map<URI, Integer> _currentErrorCountMap = new HashMap<>();
  private Map<URI, Integer> _lastRequestCountMap = new HashMap<>();
  private Map<URI, Integer> _currentRequestCountMap = new HashMap<>();
  private Map<URI, Integer> _callCountMap = new HashMap<>();
  private Map<URI, Long> _latencySumMap = new HashMap<>();

  public LoadBalancerStrategyTestRunnerBuilder(final loadBalancerStrategyType type, final String serviceName, final int numHosts)
  {
    _type = type;
    _serviceName = serviceName;
    _numIntervals = 1;

    // Create server hosts
    _uris = new ArrayList<>();
    Map<URI, Integer> errorCounMap = new HashMap<>();
    for (int i = 0; i < numHosts; i++)
    {
      URI uri = URI.create(URI_PREFIX + i + URI_SUFFIX);
      _uris.add(uri);
      errorCounMap.put(uri, HEALTHY_ERROR_COUNT);
    }
    _errorCountManager = new ConstantErrorCountManager(errorCounMap);
  }

  /**
   * Set the partition map for trackerClients
   * @param partitionMap Specifies partition id to weight map for each server host
   */
  public LoadBalancerStrategyTestRunnerBuilder setPartitionMap(Map<URI, Map<Integer, PartitionData>> partitionMap)
  {
    _partitionDataMap = partitionMap;
    return this;
  }

  /**
   * Set the number of intervals that the test is going to execute.
   * The calculation updates the new state of the balancer on each interval, within one interval the point of each host should stay the same
   * @param numIntervals Number of intervals to execute
   */
  public LoadBalancerStrategyTestRunnerBuilder setNumIntervals(int numIntervals)
  {
    _numIntervals = numIntervals;
    return this;
  }

  /**
   * Set a constant call count for all the intervals
   * @param requestCountPerInterval The number of calls sent for each interval
   */
  public LoadBalancerStrategyTestRunnerBuilder setConstantRequestCount(int requestCountPerInterval)
  {
    _requestCountManager = new ConstantRequestCountManager(requestCountPerInterval);
    return this;
  }

  /**
   * Set the call count for each interval with a predefined list
   * @param requestCounts Predefined call count list. The length of the list should equal to numIntervals
   */
  public LoadBalancerStrategyTestRunnerBuilder setFixedRequestCount(List<Integer> requestCounts)
  {
    if (requestCounts.size() != _numIntervals)
    {
      throw new IllegalArgumentException("The call count list size has to match with the intervals");
    }
    _requestCountManager = new FixedRequestCountManager(requestCounts);
    return this;
  }

  /**
   * Set a constant latency for different hosts in all intervals
   * @param latencyForHosts The constant latency to set for each host
   */
  public LoadBalancerStrategyTestRunnerBuilder setConstantLatency(List<Long> latencyForHosts)
  {
    if (latencyForHosts.size() != _uris.size())
    {
      throw new IllegalArgumentException("The latency list size has to match with the host size");
    }
    Map<URI, Long> latencyMap = new HashMap<>();
    for (int i = 0; i < latencyForHosts.size(); i++)
    {
      latencyMap.put(_uris.get(i), latencyForHosts.get(i));
    }
    _latencyManager = new ConstantLatencyManager(latencyMap);
    return this;
  }

  /**
   * Set the latency to be a relationship of the call count that each host gets
   * @param latencyCalculationList A correlation formula list for each host, the size of the map should equal the number of uris.
   */
  public LoadBalancerStrategyTestRunnerBuilder setDynamicLatency(List<LatencyCorrelation> latencyCalculationList)
  {
    if (latencyCalculationList.size() != _uris.size())
    {
      throw new IllegalArgumentException("The dynamic latency list size has to match with the host size");
    }

    Map<URI, LatencyCorrelation> latencyCalculationMap = new HashMap<>();
    for (int i = 0; i < latencyCalculationList.size(); i++)
    {
      latencyCalculationMap.put(_uris.get(i), latencyCalculationList.get(i));
    }

    _latencyManager = new DynamicLatencyManager(latencyCalculationMap);
    return this;
  }

  /**
   * Set a constant error count for different hosts in all intervals
   * @param errorCountForHosts The constant error count to set for each host
   */
  public LoadBalancerStrategyTestRunnerBuilder setConstantErrorCount(List<Integer> errorCountForHosts)
  {
    if (errorCountForHosts.size() != _uris.size())
    {
      throw new IllegalArgumentException("The error count list size has to match with the host size");
    }
    Map<URI, Integer> errorCountMap = new HashMap<>();
    for (int i = 0; i < errorCountForHosts.size(); i++)
    {
      errorCountMap.put(_uris.get(i), errorCountForHosts.get(i));
    }
    _errorCountManager = new ConstantErrorCountManager(errorCountMap);
    return this;
  }

  /**
   * Set the error count to be a relationship of the call count and intervals
   * @param errorCountCalculationList A correlation formula list for each host, the size of the map should equal the number of uris.
   */
  public LoadBalancerStrategyTestRunnerBuilder setDynamicErrorCount(List<ErrorCountCorrelation> errorCountCalculationList)
  {
    if (errorCountCalculationList.size() != _uris.size())
    {
      throw new IllegalArgumentException("The dynamic error count list size has to match with the host size");
    }

    Map<URI, ErrorCountCorrelation> errorCountCalculationMap = new HashMap<>();
    for (int i = 0; i < errorCountCalculationList.size(); i++)
    {
      errorCountCalculationMap.put(_uris.get(i), errorCountCalculationList.get(i));
    }

    _errorCountManager = new DynamicErrorCountManager(errorCountCalculationMap);
    return this;
  }

  public LoadBalancerStrategyTestRunnerBuilder setDegraderStrategies(Map<String, Object> strategyProperties,
      Map<String, String> degraderProperties)
  {
    // Copy a new map in case the original map is immutable
    Map<String, Object> strategyPropertiesCopy = new HashMap<>();
    if (strategyProperties != null)
    {
      strategyPropertiesCopy.putAll(strategyProperties);
    }
    strategyPropertiesCopy.put(PropertyKeys.CLOCK, _clockedExecutor);
    strategyPropertiesCopy.put(PropertyKeys.HTTP_LB_QUARANTINE_EXECUTOR_SERVICE, _clockedExecutor);

    Map<String, String> degraderPropertiesCopy = new HashMap<>();
    // Set default high low latency because the default highErrorRate is 1.1 which means error rate does not impact load balancer by default
    degraderPropertiesCopy.put(DEGRADER_HIGH_ERROR_RATE, DEFAULT_HIGH_ERROR_RATE);
    degraderPropertiesCopy.put(DEFAULT_LOW_ERROR_RATE, DEFAULT_LOW_ERROR_RATE);
    if (degraderPropertiesCopy != null)
    {
      degraderPropertiesCopy.putAll(degraderProperties);
    }

    _serviceProperties = new ServiceProperties(_serviceName, DEFAULT_CLUSTER_NAME, DEFAULT_PATH, DEFAULT_STRATEGY_LIST,
        strategyPropertiesCopy, null, degraderPropertiesCopy, null, null);
    return this;
  }

  /**
   * Build the test runner
   */
  public LoadBalancerStrategyTestRunner build()
  {
    // TODO: Change the strategy constructor once the new strategy is ready
    if (_serviceProperties == null && _type == loadBalancerStrategyType.DEGRADER)
    {
      setDegraderStrategies(new HashMap<>(), new HashMap<>());
    }
    _strategy = new LoadBalancerStrategyDataBuilder(_type, _serviceName)
        .setDegraderProperties(_serviceProperties.getLoadBalancerStrategyProperties(), _serviceProperties.getDegraderProperties())
        .build();

    _transportClients = _uris.stream()
        .map(uri -> new MockTransportClient(_clockedExecutor, _latencyManager, _errorCountManager, uri, INTERVAL_IN_MILLIS, _lock,
            _currentErrorCountMap, _lastRequestCountMap, _callCountMap, _latencySumMap))
        .collect(Collectors.toList());
    List<TrackerClient> trackerClients = _transportClients.stream()
        .map(transportClient -> {
          // If partition map is not specified, by default we only support one partition
          Map<Integer, PartitionData> partitionDataMap = _partitionDataMap.getOrDefault(transportClient.getUri(),
              DEFAULT_PARTITION_DATA_MAP);

          // TODO: Update the TrackerClient creation once the new TrackerClient constructor is ready
          return new TrackerClient(transportClient.getUri(), partitionDataMap, transportClient, _clockedExecutor,
              DegraderConfigFactory.toDegraderConfig(_serviceProperties.getDegraderProperties()));
        })
        .collect(Collectors.toList());
    return new LoadBalancerStrategyTestRunner(_strategy, _serviceName, trackerClients, _numIntervals,
        _requestCountManager, _clockedExecutor, _currentErrorCountMap, _lastRequestCountMap, _currentRequestCountMap,
        _callCountMap, _latencySumMap);
  }

  /**
   * The following methods create some default test scenarios
   */

  public static LoadBalancerStrategyTestRunner create1Unhealthy4HealthyHostWithLatency(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setConstantLatency(Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .build();
  }

  public static LoadBalancerStrategyTestRunner create1Receovering4HealthyHostWithLatency(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setDynamicLatency(Arrays.asList(HOST_RECOVERING_TO_HEALTHY_LATENCY,
            HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION,
            HEALTHY_HOST_LATENCY_CORRELATION))
        .build();
  }

  public static LoadBalancerStrategyTestRunner create1GoingBad4HealthyHostWithLatency(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setDynamicLatency(Arrays.asList(HOST_BECOMING_UNHEALTHY_LATENCY,
            HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION,
            HEALTHY_HOST_LATENCY_CORRELATION))
        .build();
  }

  public static LoadBalancerStrategyTestRunner create1Unhealthy4HealthyHostWithError(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .setConstantErrorCount(Arrays.asList(UNHEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT,
            HEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT))
        .setDegraderStrategies(new HashMap<>(), DEGRADER_PROPERTIES_WITH_HIGH_LOW_ERROR)
        .build();
  }

  public static LoadBalancerStrategyTestRunner create1Receovering4HealthyHostWithError(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .setDynamicErrorCount(Arrays.asList(HOST_RECOVERING_TO_HEALTHY_ERROR, HEALTHY_HOST_ERROR_COUNT_CORRELATION,
            HEALTHY_HOST_ERROR_COUNT_CORRELATION, HEALTHY_HOST_ERROR_COUNT_CORRELATION, HEALTHY_HOST_ERROR_COUNT_CORRELATION))
        .setDegraderStrategies(new HashMap<>(), DEGRADER_PROPERTIES_WITH_HIGH_LOW_ERROR)
        .build();
  }

  public static LoadBalancerStrategyTestRunner create1GoingBad4HealthyHostWithError(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .setDynamicErrorCount(Arrays.asList(HOST_BECOMING_UNHEALTHY_ERROR, HEALTHY_HOST_ERROR_COUNT_CORRELATION,
            HEALTHY_HOST_ERROR_COUNT_CORRELATION, HEALTHY_HOST_ERROR_COUNT_CORRELATION, HEALTHY_HOST_ERROR_COUNT_CORRELATION))
        .setDegraderStrategies(new HashMap<>(), DEGRADER_PROPERTIES_WITH_HIGH_LOW_ERROR)
        .build();
  }


  /**
   * Mock a transport client, the transport client leverages the clockedExecutor to control the latency
   */
  class MockTransportClient implements TransportClient
  {
    private final ClockedExecutor _clockedExecutor;
    private final LatencyManager _latencyManager;
    private final ErrorCountManager _errorCountManager;
    private final URI _uri;
    private final long _intervalMillis;
    private final Object _lock;

    private Map<URI, Integer> _currentErrorCountMap;
    private Map<URI, Integer> _lastRequestCountMap;
    private Map<URI, Integer> _callCountMap;
    private Map<URI, Long> _latencySumMap;

    MockTransportClient(
        ClockedExecutor executor, LatencyManager latencyManager, ErrorCountManager errorCountManager, URI uri,
        long intervalMillis, Object lock, Map<URI, Integer> currentErrorCountMap, Map<URI, Integer> lastRequestCountMap,
        Map<URI, Integer> callCountMap, Map<URI, Long> latencySumMap)
    {
      _clockedExecutor = executor;
      _latencyManager = latencyManager;
      _errorCountManager = errorCountManager;
      _uri = uri;
      _intervalMillis = intervalMillis;

      _lock = lock;
      _currentErrorCountMap = currentErrorCountMap;
      _lastRequestCountMap = lastRequestCountMap;
      _callCountMap = callCountMap;
      _latencySumMap = latencySumMap;
    }

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
        TransportCallback<RestResponse> callback)
    {
      int currentIntervalIndex = (int) (_clockedExecutor.currentTimeMillis() / _intervalMillis);
      int requestCount = _lastRequestCountMap.getOrDefault(_uri, 0);
      long latency = _latencyManager.getLatency(_uri, requestCount, currentIntervalIndex);
      boolean hasError = _errorCountManager.getErrorCount(_uri, requestCount, currentIntervalIndex) -
          _currentErrorCountMap.getOrDefault(_uri, 0) > 0;

      _clockedExecutor.schedule(new Runnable()
      {
        @Override
        public void run()
        {
          RestResponseBuilder restResponseBuilder = new RestResponseBuilder().setEntity(request.getURI().getRawPath().getBytes());
          if (hasError)
          {
            restResponseBuilder.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            RestException restException = new RestException(restResponseBuilder.build(), new Throwable("internal error"));
            callback.onResponse(TransportResponseImpl.error(restException));
            return;
          }
          callback.onResponse(TransportResponseImpl.success(restResponseBuilder.build()));
        }
      }, latency, TimeUnit.MILLISECONDS);

      // Collect basic stats
      synchronized (_lock)
      {
        if (hasError)
        {
          _currentErrorCountMap.putIfAbsent(_uri, 0);
          _currentErrorCountMap.put(_uri, _currentErrorCountMap.get(_uri) + 1);
        }
        _callCountMap.putIfAbsent(_uri, 0);
        _callCountMap.put(_uri, _callCountMap.get(_uri) + 1);
        _latencySumMap.putIfAbsent(_uri, 0L);
        _latencySumMap.put(_uri, _latencySumMap.get(_uri) + latency);
      }

    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      callback.onSuccess(None.none());
    }

    public URI getUri()
    {
      return _uri;
    }
  }
}
