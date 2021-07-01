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

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.clients.DegraderTrackerClientImpl;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.clients.TrackerClientImpl;
import com.linkedin.d2.balancer.config.RelativeStrategyPropertiesConverter;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;


/**
 * The benchmark to measure the execution of
 * {@link com.linkedin.d2.balancer.strategies.LoadBalancerStrategy#getTrackerClient(Request, RequestContext, long, int, Map)}
 * using different type of strategies
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
public class LoadBalancerStrategyBenchmark
{
  private static final String DUMMY_SERVICE_NAME = "dummyService";
  private static final Map<Integer, PartitionData> DEFAULT_PARTITION_DATA_MAP = new HashMap<>();
  private static final String DEFAULT_CLUSTER_NAME = "dummyCluster";
  private static final String DEFAULT_PATH = "/path";
  private static final List<String> DEFAULT_STRATEGY_LIST = Arrays.asList("DEGRADER", "RELATIVE");
  private static final String URI_PREFIX = "http://test.qa";
  private static final String URI_SUFFIX = ".com:5555";
  private static final Clock CLOCK = SystemClock.instance();
  private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("executor"));

  static
  {
    DEFAULT_PARTITION_DATA_MAP.put(0, new PartitionData(1.0));
  }

  @State(Scope.Benchmark)
  public static class LoadBalancerStrategy_10Hosts
  {
    URIRequest _uriRequest = new URIRequest("d2://" + DUMMY_SERVICE_NAME);
    LoadBalancerStrategy _degraderStrategy = buildDegraderLoadBalancerStrategy();
    LoadBalancerStrategy _relativeStrategy = buildRelativeLoadBalancerStrategy();
    Map<URI, TrackerClient> _degraderTrackerClients = createDegraderTrackerClients(10);
    Map<URI, TrackerClient> _trackerClients = createTrackerClients(10);
    RestRequest _restRequest = new RestRequestBuilder(_uriRequest.getURI()).build();
  }

  @Benchmark
  public TrackerClient measureDegraderStrategy10Hosts(LoadBalancerStrategy_10Hosts state)
  {
    RequestContext requestContext = new RequestContext();
    return state._degraderStrategy.getTrackerClient(state._restRequest, requestContext, 0, 0, state._degraderTrackerClients);
  }

  @Benchmark
  public TrackerClient measureRelativeStrategy10Hosts(LoadBalancerStrategy_10Hosts state)
  {
    RequestContext requestContext = new RequestContext();
    return state._relativeStrategy.getTrackerClient(state._restRequest, requestContext, 0, 0, state._trackerClients);
  }

  @State(Scope.Benchmark)
  public static class DegraderLoadBalancerStrategyInitialize
  {
    URIRequest _uriRequest = new URIRequest("d2://" + DUMMY_SERVICE_NAME);
    LoadBalancerStrategy _degraderStrategy;
    Map<URI, TrackerClient> _degraderTrackerClients = createDegraderTrackerClients(10);
    RestRequest _restRequest = new RestRequestBuilder(_uriRequest.getURI()).build();

    @Setup(Level.Iteration)
    public void setup()
    {
      // Always start with a new strategy that is not initialized yet
      _degraderStrategy = buildDegraderLoadBalancerStrategy();
    }
  }

  @State(Scope.Benchmark)
  public static class RelativeLoadBalancerStrategyInitialize
  {
    URIRequest _uriRequest = new URIRequest("d2://" + DUMMY_SERVICE_NAME);
    LoadBalancerStrategy _relativeStrategy;
    Map<URI, TrackerClient> _trackerClients = createTrackerClients(10);
    RestRequest _restRequest = new RestRequestBuilder(_uriRequest.getURI()).build();

    @Setup(Level.Iteration)
    public void setup()
    {
      // Always start with a new strategy that is not initialized yet
      _relativeStrategy = buildRelativeLoadBalancerStrategy();
    }
  }

  /**
   * Measure the performance of initialization from the very first request for {@link DegraderLoadBalancerStrategyV3}
   */
  @Warmup(iterations = 0)
  @Benchmark
  public TrackerClient measureDegraderStrategyInitialization(DegraderLoadBalancerStrategyInitialize state)
  {
    RequestContext requestContext = new RequestContext();
    return state._degraderStrategy.getTrackerClient(state._restRequest, requestContext, 0, 0, state._degraderTrackerClients);
  }

  /**
   * Measure the performance of initialization from the very first request for {@link RelativeLoadBalancerStrategy}
   */
  @Warmup(iterations = 0)
  @Benchmark
  public TrackerClient measureRelativeStrategyInitialization(RelativeLoadBalancerStrategyInitialize state)
  {
    RequestContext requestContext = new RequestContext();
    return state._relativeStrategy.getTrackerClient(state._restRequest, requestContext, 0, 0, state._trackerClients);
  }

  @State(Scope.Benchmark)
  public static class DegraderLoadBalancerStrategyClusterChange
  {
    URIRequest _uriRequest = new URIRequest("d2://" + DUMMY_SERVICE_NAME);
    LoadBalancerStrategy _degraderStrategy = buildDegraderLoadBalancerStrategy();
    Map<URI, TrackerClient> _degraderTrackerClients;
    URI _removedUri = URI.create(URI_PREFIX + 0 + URI_SUFFIX);
    RestRequest _restRequest = new RestRequestBuilder(_uriRequest.getURI()).build();

    @Setup(Level.Iteration)
    public void setup()
    {
      // Always initialize the partition state in each iteration
      _degraderTrackerClients = createDegraderTrackerClients(10);
      RequestContext requestContext = new RequestContext();
      _degraderStrategy.getTrackerClient(_restRequest, requestContext, 0, 0, _degraderTrackerClients);
    }
  }

  @State(Scope.Benchmark)
  public static class RelativeLoadBalancerStrategyClusterChange
  {
    URIRequest _uriRequest = new URIRequest("d2://" + DUMMY_SERVICE_NAME);
    LoadBalancerStrategy _relativeStrategy = buildRelativeLoadBalancerStrategy();
    Map<URI, TrackerClient> _trackerClients;
    URI _removedUri = URI.create(URI_PREFIX + 0 + URI_SUFFIX);
    RestRequest _restRequest = new RestRequestBuilder(_uriRequest.getURI()).build();

    @Setup(Level.Iteration)
    public void setup()
    {
      // Always initialize the partition state in each iteration
      _trackerClients = createTrackerClients(10);
      RequestContext requestContext = new RequestContext();
      _relativeStrategy.getTrackerClient(_restRequest, requestContext, 0, 0, _trackerClients);
    }
  }

  /**
   * Measure the performance when cluster id changed, cluster id changed from 0 to 1
   * For {@link DegraderLoadBalancerStrategyV3} it will update the state synchronously when processing the first request after cluster change
   */
  @Warmup(iterations = 0)
  @Benchmark
  public TrackerClient measureDegraderStrategyClusterChange(DegraderLoadBalancerStrategyClusterChange state)
  {
    // Remove one host from the cluster and perform the test
    state._degraderTrackerClients.remove(state._removedUri);
    RequestContext requestContext = new RequestContext();
    return state._degraderStrategy.getTrackerClient(state._restRequest, requestContext, 1, 0, state._degraderTrackerClients);
  }

  /**
   * Measure the performance when cluster id changed, cluster id changed from 0 to 1
   * For {@link RelativeLoadBalancerStrategy} it will update the state asynchronously, it should take shorter time
   */
  @Warmup(iterations = 0)
  @Benchmark
  public TrackerClient measureRelativeStrategyClusterChange(RelativeLoadBalancerStrategyClusterChange state)
  {
    // Remove one host from the cluster and perform the test
    state._trackerClients.remove(state._removedUri);
    RequestContext requestContext = new RequestContext();
    return state._relativeStrategy.getTrackerClient(state._restRequest, requestContext, 1, 0, state._trackerClients);
  }

  @State(Scope.Benchmark)
  public static class LoadBalancerStrategy_100Hosts
  {
    URIRequest _uriRequest = new URIRequest("d2://" + DUMMY_SERVICE_NAME);
    LoadBalancerStrategy _degraderStrategy = buildDegraderLoadBalancerStrategy();
    LoadBalancerStrategy _relativeStrategy = buildRelativeLoadBalancerStrategy();
    Map<URI, TrackerClient> _degraderTrackerClients = createDegraderTrackerClients(100);
    Map<URI, TrackerClient> _trackerClients = createTrackerClients(100);
    RestRequest _restRequest = new RestRequestBuilder(_uriRequest.getURI()).build();
  }

  @Benchmark
  public TrackerClient measureDegraderStrategy100Hosts(LoadBalancerStrategy_100Hosts state)
  {
    RequestContext requestContext = new RequestContext();
    return state._degraderStrategy.getTrackerClient(state._restRequest, requestContext, 0, 0, state._degraderTrackerClients);
  }

  @Benchmark
  public TrackerClient measureRelativeStrategy100Hosts(LoadBalancerStrategy_100Hosts state)
  {
    RequestContext requestContext = new RequestContext();
    return state._relativeStrategy.getTrackerClient(state._restRequest, requestContext, 0, 0, state._trackerClients);
  }

  private static Map<URI, TrackerClient> createDegraderTrackerClients(int numHosts)
  {
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    for (int i = 0; i < numHosts; i++)
    {
      URI uri = URI.create(URI_PREFIX + i + URI_SUFFIX);
      trackerClients.put(uri, new DegraderTrackerClientImpl(uri, DEFAULT_PARTITION_DATA_MAP, new BaseTransportTestClient(), CLOCK, null));
    }
    return trackerClients;
  }

  private static Map<URI, TrackerClient> createTrackerClients(int numHosts)
  {
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    for (int i = 0; i < numHosts; i++)
    {
      URI uri = URI.create(URI_PREFIX + i + URI_SUFFIX);
      trackerClients.put(uri, new TrackerClientImpl(uri, DEFAULT_PARTITION_DATA_MAP, new BaseTransportTestClient(), CLOCK,
          RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS, (status) -> status >= 500 && status <= 599));
    }
    return trackerClients;
  }

  private static RelativeLoadBalancerStrategy buildRelativeLoadBalancerStrategy()
  {
    D2RelativeStrategyProperties relativeStrategyProperties = new D2RelativeStrategyProperties();
    ServiceProperties serviceProperties = new ServiceProperties(DUMMY_SERVICE_NAME, DEFAULT_CLUSTER_NAME, DEFAULT_PATH, DEFAULT_STRATEGY_LIST,
        null, null, null, null, null,
        null, null, RelativeStrategyPropertiesConverter.toMap(relativeStrategyProperties));
    return new RelativeLoadBalancerStrategyFactory(EXECUTOR_SERVICE, null, new ArrayList<>(), null, SystemClock.instance(),
        false)
        .newLoadBalancer(serviceProperties);
  }

  private static DegraderLoadBalancerStrategyV3 buildDegraderLoadBalancerStrategy()
  {
    ServiceProperties serviceProperties = new ServiceProperties(DUMMY_SERVICE_NAME, DEFAULT_CLUSTER_NAME, DEFAULT_PATH, DEFAULT_STRATEGY_LIST,
        new HashMap<>(), null, new HashMap<>(), null, null,
        null, null, null);
    return new DegraderLoadBalancerStrategyFactoryV3(null, EXECUTOR_SERVICE, null, new ArrayList<>())
        .newLoadBalancer(serviceProperties);
  }
}
