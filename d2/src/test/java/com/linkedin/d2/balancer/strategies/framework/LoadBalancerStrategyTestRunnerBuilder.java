package com.linkedin.d2.balancer.strategies.framework;

import com.google.common.collect.ImmutableMap;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.loadBalancerStrategyType;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.test.util.ClockedExecutor;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Builder class to build a {@link LoadBalancerStrategyTestRunner}
 */
public class LoadBalancerStrategyTestRunnerBuilder {
  public final static long INTERVAL_IN_MILLIS = 5000L;
  private final static String URI_PREFIX = "http://test.qa";
  private final static String URI_SUFFIX = ".com:5555";

  private LoadBalancerStrategy _strategy;
  private Map<String, Object> _strategyProperties = new HashMap<>();
  private Map<URI, Map<Integer, PartitionData>> _partitionDataMap = new HashMap<>();
  private String _serviceName;
  private List<URI> _uris;
  private List<MockTransportClient> _transportClients;
  private int _numIntervals;
  private LatencyManager _latencyManager;
  private RequestCountManager _requestCountManager;
  private final loadBalancerStrategyType _type;
  private final ClockedExecutor _clockedExecutor = new ClockedExecutor();
  private final Object _lock = new Object();

  // Performance stats
  private Map<URI, Integer> _lastRequestCountMap = new HashMap<>();
  private Map<URI, Integer> _currentRequestCountMap = new HashMap<>();
  private Map<URI, Integer> _callCountMap = new HashMap<>();
  private Map<URI, Long> _latencySumMap = new HashMap<>();

  public LoadBalancerStrategyTestRunnerBuilder(final loadBalancerStrategyType type,
      Map<String, Object> strategyProperties, final String serviceName, final int numHosts) {
    if (strategyProperties != null) {
      _strategyProperties.putAll(strategyProperties);
    }
    this._strategyProperties.put(PropertyKeys.CLOCK, _clockedExecutor);
    this._strategyProperties.put(PropertyKeys.HTTP_LB_QUARANTINE_EXECUTOR_SERVICE, _clockedExecutor);
    this._type = type;
    this._serviceName = serviceName;
    this._numIntervals = 1;

    // Create server hosts
    this._uris = new ArrayList<>();
    for (int i = 0; i < numHosts; i++) {
      this._uris.add(URI.create(URI_PREFIX + i + URI_SUFFIX));
    }
  }

  /**
   * Provide easy access for user to add properties in the test
   */
  public LoadBalancerStrategyTestRunnerBuilder addStrategyProperty(String key, Object value) {
    this._strategyProperties.put(key, value);
    return this;
  }

  /**
   * Set the partition map for trackerClients
   * @param partitionMap Specifies partition id to weight map for each server host
   */
  public LoadBalancerStrategyTestRunnerBuilder setPartitionMap(Map<URI, Map<Integer, PartitionData>> partitionMap) {
    this._partitionDataMap = partitionMap;
    return this;
  }

  /**
   * Set the number of intervals that the test is going to execute.
   * The calculation updates the new state of the balancer on each interval, within one interval the point of each host should stay the same
   * @param numIntervals Number of intervals to execute
   */
  public LoadBalancerStrategyTestRunnerBuilder setNumIntervals(int numIntervals) {
    this._numIntervals = numIntervals;
    return this;
  }

  /**
   * Set a constant call count for all the intervals
   * @param requestCountPerInterval The number of calls sent for each interval
   */
  public LoadBalancerStrategyTestRunnerBuilder setConstantRequestCount(int requestCountPerInterval) {
    this._requestCountManager = new ConstantRequestCountManager(requestCountPerInterval);
    return this;
  }

  /**
   * Set the call count for each interval with a predefined list
   * @param requestCounts Predefined call count list. The length of the list should equal to numIntervals
   */
  public LoadBalancerStrategyTestRunnerBuilder setFixedRequestCount(List<Integer> requestCounts) {
    if (requestCounts.size() != this._numIntervals) {
      throw new IllegalArgumentException("The call count list size has to match with the intervals");
    }
    this._requestCountManager = new FixedRequestCountManager(requestCounts);
    return this;
  }

  /**
   * Set a constant latency for different hosts in all intervals
   * @param latencyForHosts The constant latency to set for each host
   */
  public LoadBalancerStrategyTestRunnerBuilder setConstantLatency(List<Long> latencyForHosts) {
    if (latencyForHosts.size() != this._uris.size()) {
      throw new IllegalArgumentException("The latency list size has to match with the host size");
    }
    Map<URI, Long> latencyMap = new HashMap<>();
    for (int i = 0; i < latencyForHosts.size(); i++) {
      latencyMap.put(this._uris.get(i), latencyForHosts.get(i));
    }
    this._latencyManager = new ConstantLatencyManager(latencyMap);
    return this;
  }

  /**
   * Set the latency for each host in each interval with predefined numbers
   * @param latencyListForHosts A list of latency for intervals for each host,
   *                   The size of the list should equal to the number of hosts
   */
  public LoadBalancerStrategyTestRunnerBuilder setFixedLatency(List<List<Long>> latencyListForHosts) {
    if (latencyListForHosts.size() != this._uris.size()) {
      throw new IllegalArgumentException("The latency list size has to match with the host size");
    }

    Map<URI, List<Long>> latencyMapForIntervals = new HashMap<>();
    for (int i = 0; i < latencyListForHosts.size(); i++) {
      latencyMapForIntervals.put(this._uris.get(i), latencyListForHosts.get(i));
    }
    this._latencyManager = new FixedLatencyManager(latencyMapForIntervals);
    return this;
  }

  /**
   * Set the latency to be a relationship of the call count that each host gets
   * @param latencyCalculationList A correlation formula list for each host, the size of the map should equal the number of uris.
   */
  public LoadBalancerStrategyTestRunnerBuilder setDynamicLatency(List<LatencyQPSCorrelation> latencyCalculationList) {
    if (latencyCalculationList.size() != this._uris.size()) {
      throw new IllegalArgumentException("The dynamic latency list size has to match with the host size");
    }

    Map<URI, LatencyQPSCorrelation> latencyCalculationMap = new HashMap<>();
    for (int i = 0; i < latencyCalculationList.size(); i++) {
      latencyCalculationMap.put(this._uris.get(i), latencyCalculationList.get(i));
    }

    this._latencyManager = new DynamicLatencyManager(latencyCalculationMap);
    return this;
  }

  /**
   * Build the test runner
   */
  public LoadBalancerStrategyTestRunner build() {
    this._strategy = new LoadBalancerStrategyDataBuilder(this._type, this._serviceName, this._strategyProperties).build();

    this._transportClients = this._uris.stream()
        .map(uri -> new MockTransportClient(this._clockedExecutor, this._latencyManager, uri, INTERVAL_IN_MILLIS, this._lock,
            this._lastRequestCountMap, this._callCountMap, this._latencySumMap))
        .collect(Collectors.toList());
    List<TrackerClient> trackerClients = this._transportClients.stream()
        .map(transportClient -> {
          // If partition map is not specified, by default we only support one partition
          Map<Integer, PartitionData> partitionDataMap = _partitionDataMap.getOrDefault(transportClient.getUri(),
              ImmutableMap.of(0, new PartitionData(1.0)));

          return new TrackerClient(transportClient.getUri(), partitionDataMap, transportClient, this._clockedExecutor, null);
        })
        .collect(Collectors.toList());
    return new LoadBalancerStrategyTestRunner(this._strategy, this._serviceName, trackerClients,
        this._numIntervals, this._requestCountManager, this._clockedExecutor, this._lastRequestCountMap, this._currentRequestCountMap,
        this._callCountMap, this._latencySumMap);
  }


  /**
   * Mock a transport client, the transport client leverages the clockedExecutor to control the latency
   */
  class MockTransportClient implements TransportClient {
    private final ClockedExecutor _clockedExecutor;
    private final LatencyManager _latencyManager;
    private final URI _uri;
    private final long _intervalMillis;
    private final Object _lock;

    private Map<URI, Integer> _lastRequestCountMap;
    private Map<URI, Integer> _callCountMap;
    private Map<URI, Long> _latencySumMap;

    MockTransportClient(
        ClockedExecutor executor, LatencyManager latencyManager, URI uri, long intervalMillis, Object lock,
        Map<URI, Integer> lastRequestCountMap, Map<URI, Integer> callCountMap, Map<URI, Long> latencySumMap)
    {
      this._clockedExecutor = executor;
      this._latencyManager = latencyManager;
      this._uri = uri;
      this._intervalMillis = intervalMillis;

      this._lock = lock;
      this._lastRequestCountMap = lastRequestCountMap;
      this._callCountMap = callCountMap;
      this._latencySumMap = latencySumMap;
    }

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
        TransportCallback<RestResponse> callback) {
      int currentIntervalIndex = (int) (this._clockedExecutor.currentTimeMillis() / this._intervalMillis);
      int requestCount = this._lastRequestCountMap.getOrDefault(_uri, 0);
      long latency = this._latencyManager.getLatency(_uri, requestCount, currentIntervalIndex);
      this._clockedExecutor.schedule(new Runnable() {
        @Override
        public void run()
        {
          RestResponseBuilder restResponseBuilder = new RestResponseBuilder().setEntity(request.getURI().getRawPath().getBytes());
          callback.onResponse(TransportResponseImpl.success(restResponseBuilder.build()));
        }
      }, latency, TimeUnit.MILLISECONDS);

      // Collect basic stats
      synchronized (this._lock) {
        this._callCountMap.putIfAbsent(this._uri, 0);
        this._callCountMap.put(this._uri, this._callCountMap.get(this._uri) + 1);
        this._latencySumMap.putIfAbsent(this._uri, 0L);
        this._latencySumMap.put(this._uri, this._latencySumMap.get(this._uri) + latency);
      }

    }

    @Override
    public void shutdown(Callback<None> callback) {
      callback.onSuccess(None.none());
    }

    public URI getUri() {
      return this._uri;
    }
  }
}
