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

package com.linkedin.d2.balancer.strategies.framework;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.clients.DegraderTrackerClientImpl;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.clients.TrackerClientImpl;
import com.linkedin.d2.balancer.config.RelativeStrategyPropertiesConverter;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.degrader.DegraderConfigFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategyFactory;
import com.linkedin.d2.loadBalancerStrategyType;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.test.util.ClockedExecutor;
import com.linkedin.util.degrader.CallTrackerImpl;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.eclipse.jetty.http.HttpStatus;


/**
 * Builder class to build a {@link LoadBalancerStrategyTestRunner}
 */
public class LoadBalancerStrategyTestRunnerBuilder
{
  public static final long INTERVAL_IN_MILLIS = 5000L;
  private static final String URI_PREFIX = "http://test.qa";
  private static final String URI_SUFFIX = ".com:5555";
  public static final String DEFAULT_CLUSTER_NAME = "dummyCluster";
  public static final String DEFAULT_PATH = "/path";
  // This strategy list is not in use, please use the loadBalancerType to indicate the type of strategy
  public static final List<String> DEFAULT_STRATEGY_LIST = Arrays.asList("DEGRADER", "RANDOM", "RELATIVE");
  public static final int HEALTHY_ERROR_COUNT = 0;
  @SuppressWarnings("serial")
  private static final Map<Integer, PartitionData> DEFAULT_PARTITION_DATA_MAP = new HashMap<Integer, PartitionData>()
  {{
    put(LoadBalancerStrategyTestRunner.DEFAULT_PARTITION_ID, new PartitionData(1.0));
  }};

  private LoadBalancerStrategy _strategy;
  private ServiceProperties _serviceProperties;
  private Map<URI, Map<Integer, PartitionData>> _partitionDataMap = new HashMap<>();
  private Map<Integer, Set<URI>> _partitionUrisMap = new HashMap<>();
  private String _serviceName;
  private List<URI> _uris;
  private List<MockTransportClient> _transportClients;
  private int _numIntervals;
  private boolean _enableServerReportedLoad;
  private LatencyManager _latencyManager;
  private ErrorCountManager _errorCountManager;
  private RequestCountManager _requestCountManager;
  private ServerReportedLoadManager _serverReportedLoadManager;
  private final loadBalancerStrategyType _type;
  private final ClockedExecutor _clockedExecutor = new ClockedExecutor();

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
   * @param partitionDataMap Specifies partition id to weight map for each server host
   */
  public LoadBalancerStrategyTestRunnerBuilder addPartitionDataMap(int uriIndex, Map<Integer, PartitionData> partitionDataMap)
  {
    _partitionDataMap.put(_uris.get(uriIndex), partitionDataMap);
    return this;
  }

  public LoadBalancerStrategyTestRunnerBuilder addPartitionUriMap(int partitionId, List<Integer> uriIndexes)
  {
    Set<URI> uriSet = uriIndexes.stream()
        .map(uriIndex -> _uris.get(uriIndex))
        .collect(Collectors.toSet());
    _partitionUrisMap.put(partitionId, uriSet);
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

  public LoadBalancerStrategyTestRunnerBuilder enableServerReportedLoad(boolean enableServerReportedLoad)
  {
    _enableServerReportedLoad = enableServerReportedLoad;
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
   * Set the call count for each interval with a free-formed formula
   * @param requestCountCorrelation The correlation between call count and the interval index
   */
  public LoadBalancerStrategyTestRunnerBuilder setDynamicRequestCount(RequestCountCorrelation requestCountCorrelation)
  {
    _requestCountManager = new DynamicRequestCountManager(requestCountCorrelation);
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
  public LoadBalancerStrategyTestRunnerBuilder setDynamicErrorCount(
      List<ErrorCountCorrelation> errorCountCalculationList)
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

  /**
   * Set the server reported load to be a relationship of the call count that each host gets
   * @param serverReportedLoadCalculationList A correlation formula list for each host, the size of the map should equal the number of uris.
   */
  public LoadBalancerStrategyTestRunnerBuilder setDynamicServerReportedLoad(List<ServerReportedLoadCorrelation> serverReportedLoadCalculationList)
  {
    if (serverReportedLoadCalculationList.size() != _uris.size())
    {
      throw new IllegalArgumentException("The dynamic load score list size has to match with the host size");
    }

    Map<URI, ServerReportedLoadCorrelation> serverReportedLoadCalculationMap = new HashMap<>();
    for (int i = 0; i < serverReportedLoadCalculationList.size(); i++)
    {
      serverReportedLoadCalculationMap.put(_uris.get(i), serverReportedLoadCalculationList.get(i));
    }

    _serverReportedLoadManager = new DynamicServerReportedLoadManager(serverReportedLoadCalculationMap);
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
    if (degraderPropertiesCopy != null)
    {
      degraderPropertiesCopy.putAll(degraderProperties);
    }

    _serviceProperties = new ServiceProperties(_serviceName, DEFAULT_CLUSTER_NAME, DEFAULT_PATH, DEFAULT_STRATEGY_LIST,
        strategyPropertiesCopy, null, degraderPropertiesCopy, null, null);
    return this;
  }

  public LoadBalancerStrategyTestRunnerBuilder setRelativeLoadBalancerStrategies(D2RelativeStrategyProperties relativeLoadBalancerStrategies)
  {
    _serviceProperties = new ServiceProperties(_serviceName, DEFAULT_CLUSTER_NAME, DEFAULT_PATH, DEFAULT_STRATEGY_LIST,
        null, null, null, null, null,
        null, null, RelativeStrategyPropertiesConverter.toMap(relativeLoadBalancerStrategies));
    return this;
  }

  /**
   * Build the test runner
   */
  public LoadBalancerStrategyTestRunner build()
  {
    switch (_type)
    {
      case DEGRADER:
        return buildDegraderStrategy();
      case RELATIVE:
      default:
        return buildRelativeStrategy();
    }
  }

  private LoadBalancerStrategyTestRunner buildDegraderStrategy()
  {
    if (_serviceProperties == null)
    {
      setDegraderStrategies(new HashMap<>(), new HashMap<>());
    }
    _strategy = new DegraderLoadBalancerStrategyFactoryV3().newLoadBalancer(_serviceProperties);

    _transportClients = _uris.stream()
        .map(uri -> new MockTransportClient(_clockedExecutor, _latencyManager, _errorCountManager,
            _serverReportedLoadManager, uri, INTERVAL_IN_MILLIS, _currentErrorCountMap, _lastRequestCountMap,
            _callCountMap, _latencySumMap))
        .collect(Collectors.toList());
    Map<URI, TrackerClient> trackerClientMap = _transportClients.stream()
        .map(transportClient -> {
          // If partition map is not specified, by default we only support one partition
          Map<Integer, PartitionData> partitionDataMap = _partitionDataMap.getOrDefault(transportClient.getUri(),
              DEFAULT_PARTITION_DATA_MAP);

          return new DegraderTrackerClientImpl(transportClient.getUri(), partitionDataMap, transportClient, _clockedExecutor,
              DegraderConfigFactory.toDegraderConfig(_serviceProperties.getDegraderProperties()), TrackerClientImpl.DEFAULT_CALL_TRACKER_INTERVAL,
                  TrackerClientImpl.DEFAULT_ERROR_STATUS_PATTERN, false, _enableServerReportedLoad);
        })
        .collect(Collectors.toMap(TrackerClient::getUri, trackerClient -> trackerClient));

    return buildInternal(trackerClientMap);
  }

  private LoadBalancerStrategyTestRunner buildRelativeStrategy()
  {
    if (_serviceProperties == null)
    {
      setRelativeLoadBalancerStrategies(new D2RelativeStrategyProperties());
    }
    _strategy = new RelativeLoadBalancerStrategyFactory(_clockedExecutor, null, new ArrayList<>(), null, _clockedExecutor, _enableServerReportedLoad)
        .newLoadBalancer(_serviceProperties);

    _transportClients = _uris.stream()
        .map(uri -> new MockTransportClient(_clockedExecutor, _latencyManager, _errorCountManager,
            _serverReportedLoadManager, uri, INTERVAL_IN_MILLIS, _currentErrorCountMap, _lastRequestCountMap,
            _callCountMap, _latencySumMap))
        .collect(Collectors.toList());
    Map<URI, TrackerClient> trackerClientMap = _transportClients.stream()
        .map(transportClient -> {
          // If partition map is not specified, by default we only support one partition
          Map<Integer, PartitionData> partitionDataMap = _partitionDataMap.getOrDefault(transportClient.getUri(),
              DEFAULT_PARTITION_DATA_MAP);

          return new TrackerClientImpl(transportClient.getUri(), partitionDataMap, transportClient, _clockedExecutor,
              INTERVAL_IN_MILLIS, (status) -> status >= 500 && status <= 599, true, false, _enableServerReportedLoad);
        })
        .collect(Collectors.toMap(TrackerClient::getUri, trackerClient -> trackerClient));

    return buildInternal(trackerClientMap);
  }

  private LoadBalancerStrategyTestRunner buildInternal(Map<URI, TrackerClient> trackerClientMap)
  {
    Map<Integer, Map<URI, TrackerClient>> partitionTrackerClientsMap = new HashMap<>();
    if (_partitionUrisMap.size() != 0)
    {
      for (Integer partitionId : _partitionUrisMap.keySet())
      {
        Map<URI, TrackerClient> trackerClientsByPartition = _partitionUrisMap.get(partitionId).stream()
            .map(trackerClientMap::get)
            .collect(Collectors.toMap(TrackerClient::getUri, trackerClient -> trackerClient));
        partitionTrackerClientsMap.put(partitionId, trackerClientsByPartition);
      }
    }
    else
    {
      partitionTrackerClientsMap.put(LoadBalancerStrategyTestRunner.DEFAULT_PARTITION_ID, trackerClientMap);
    }
    return new LoadBalancerStrategyTestRunner(_strategy, _serviceName, _uris, partitionTrackerClientsMap, _numIntervals,
        _requestCountManager, _clockedExecutor, _currentErrorCountMap, _lastRequestCountMap, _currentRequestCountMap,
        _callCountMap, _latencySumMap);
  }

  /**
   * Mock a transport client, the transport client leverages the clockedExecutor to control the latency
   */
  class MockTransportClient implements TransportClient
  {
    private final ClockedExecutor _clockedExecutor;
    private final LatencyManager _latencyManager;
    private final ErrorCountManager _errorCountManager;
    private final ServerReportedLoadManager _serverReportedLoadManager;
    private final URI _uri;
    private final long _intervalMillis;

    private Map<URI, Integer> _currentErrorCountMap;
    private Map<URI, Integer> _lastRequestCountMap;
    private Map<URI, Integer> _callCountMap;
    private Map<URI, Long> _latencySumMap;

    MockTransportClient(
        ClockedExecutor executor, LatencyManager latencyManager, ErrorCountManager errorCountManager,
        ServerReportedLoadManager serverReportedLoadManager, URI uri, long intervalMillis,
        Map<URI, Integer> currentErrorCountMap, Map<URI, Integer> lastRequestCountMap,
        Map<URI, Integer> callCountMap, Map<URI, Long> latencySumMap)
    {
      _clockedExecutor = executor;
      _latencyManager = latencyManager;
      _errorCountManager = errorCountManager;
      _serverReportedLoadManager = serverReportedLoadManager;
      _uri = uri;
      _intervalMillis = intervalMillis;

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
      int serverReportedLoad = _serverReportedLoadManager != null
          ? _serverReportedLoadManager.getServerReportedLoad(_uri, requestCount, currentIntervalIndex)
          : CallTrackerImpl.DEFAULT_SERVER_REPORTED_LOAD;
      Map<String, String> wireAttributes = new HashMap<>();
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
          wireAttributes.put(R2Constants.SERVER_REPORTED_LOAD, Integer.toString(serverReportedLoad));
          callback.onResponse(TransportResponseImpl.success(restResponseBuilder.build(), wireAttributes));
        }
      }, latency, TimeUnit.MILLISECONDS);

      // Collect basic stats
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
