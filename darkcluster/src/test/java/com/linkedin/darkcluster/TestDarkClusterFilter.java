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

package com.linkedin.darkcluster;

import com.linkedin.r2.transport.http.client.ConstantQpsRateLimiter;
import com.linkedin.r2.transport.http.client.EvictingCircularBuffer;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.LoadBalancerTestState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.DarkClusterVerifier;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.darkcluster.api.NoOpDarkClusterVerifier;
import com.linkedin.darkcluster.filter.DarkClusterFilter;
import com.linkedin.darkcluster.impl.DarkClusterManagerImpl;
import com.linkedin.darkcluster.impl.DarkClusterStrategyFactoryImpl;
import com.linkedin.darkcluster.impl.DarkClusterVerifierManagerImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcher;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;

import static com.linkedin.darkcluster.DarkClusterTestUtil.createRelativeTrafficMultiplierConfig;
import static com.linkedin.darkcluster.TestDarkClusterStrategyFactory.DARK_CLUSTER_NAME;
import static com.linkedin.darkcluster.TestDarkClusterStrategyFactory.SOURCE_CLUSTER_NAME;

import java.util.function.Supplier;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestDarkClusterFilter
{
  private ScheduledExecutorService _scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
  private MockClient _client;
  private DarkClusterDispatcher _darkClusterDispatcher;
  private ClusterInfoProvider _clusterInfoProvider;
  private Facilities _facilities;
  private Notifier _notifier = new DoNothingNotifier();;
  private ExecutorService _executorService = Executors.newSingleThreadExecutor();
  private DarkClusterVerifier _darkClusterVerifier = new NoOpDarkClusterVerifier();
  private DarkClusterVerifierManager _verifierManager = new DarkClusterVerifierManagerImpl(_darkClusterVerifier, _executorService);
  Clock clock = SystemClock.instance();
  private Supplier<ConstantQpsRateLimiter> _rateLimiterSupplier = () -> new ConstantQpsRateLimiter(
      _scheduledExecutorService, _executorService, clock, new EvictingCircularBuffer(1, 1, ChronoUnit.SECONDS, clock));
  private Random _random = new Random();
  private DarkClusterFilter _darkClusterFilter;
  private DarkClusterStrategyFactory _darkClusterStrategyFactory;

  @BeforeMethod
  public void setup()
  {
    _client = new MockClient(false);
    _darkClusterDispatcher = new DefaultDarkClusterDispatcher(_client);
    _clusterInfoProvider = new SimpleLoadBalancer(new LoadBalancerTestState(), _scheduledExecutorService);
    _facilities = new MockFacilities(_clusterInfoProvider);
    _darkClusterStrategyFactory = new DarkClusterStrategyFactoryImpl(_facilities, SOURCE_CLUSTER_NAME,
                                                                                               _darkClusterDispatcher,
                                                                                               _notifier, _random,
                                                                                               _verifierManager, _rateLimiterSupplier);

    DarkClusterManager darkClusterManager = new DarkClusterManagerImpl(SOURCE_CLUSTER_NAME,
                                                                       _facilities,
                                                                       _darkClusterStrategyFactory,
                                                                       "", "",
                                                                       _notifier);
    _darkClusterFilter = new DarkClusterFilter(darkClusterManager, _verifierManager);
  }

  @Test
  public void testDarkClusterAssemblyNoDarkCluster()
  {
    _darkClusterStrategyFactory.start();
    RestRequest restRequest = new RestRequestBuilder(URI.create("/foo/1")).build();

    // no dark clusters have been added, so no request should be sent.
    _darkClusterFilter.onRestRequest(restRequest, new RequestContext(), new HashMap<>(), new DummyNextFilter());
    Assert.assertEquals(_client.requestAuthorityMap.size(), 0, "expected zero requests to be sent because no dark clusters");
    _darkClusterFilter.onRestError(new RuntimeException("test"), new RequestContext(), new HashMap<>(), new DummyNextFilter());
    _darkClusterFilter.onRestResponse(new RestResponseBuilder().build(), new RequestContext(), new HashMap<>(), new DummyNextFilter());
  }

  @Test
  public void testDarkClusterAssemblyWithDarkCluster()
  {
    // we need to have a Mock clusterInfoProvider in order to set up a dark cluster.
    MockClusterInfoProvider clusterInfoProvider = new MockClusterInfoProvider();
    _facilities = new MockFacilities(clusterInfoProvider);

    _darkClusterStrategyFactory = new DarkClusterStrategyFactoryImpl(_facilities, SOURCE_CLUSTER_NAME,
                                                                     _darkClusterDispatcher,
                                                                     _notifier, _random,
                                                                     _verifierManager, _rateLimiterSupplier);
    _darkClusterStrategyFactory.start();
    DarkClusterManager darkClusterManager = new DarkClusterManagerImpl(SOURCE_CLUSTER_NAME,
                                                                       _facilities,
                                                                       _darkClusterStrategyFactory,
                                                                       "", "",
                                                                       _notifier);
    _darkClusterFilter = new DarkClusterFilter(darkClusterManager, _verifierManager);

    // set the multiplier to 1 so that traffic gets sent.
    DarkClusterConfig darkClusterConfig = createRelativeTrafficMultiplierConfig(1.0f);
    clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig);
    clusterInfoProvider.notifyListenersClusterAdded(SOURCE_CLUSTER_NAME);

    // send the request, expecting it to make it all the way down to the client
    RestRequest restRequest = new RestRequestBuilder(URI.create("foo")).build();
    _darkClusterFilter.onRestRequest(restRequest, new RequestContext(), new HashMap<>(), new DummyNextFilter());
    Assert.assertEquals(_client.requestAuthorityMap.size(), 1, "expected 1 request to be sent");
    _darkClusterFilter.onRestError(new RuntimeException("test"), new RequestContext(), new HashMap<>(), new DummyNextFilter());
    _darkClusterFilter.onRestResponse(new RestResponseBuilder().build(), new RequestContext(), new HashMap<>(), new DummyNextFilter());
  }

  private static class DummyNextFilter implements NextFilter<RestRequest, RestResponse>
  {
    @Override
    public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
    {

    }

    @Override
    public void onResponse(RestResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs)
    {

    }

    @Override
    public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
    {

    }
  }
}
