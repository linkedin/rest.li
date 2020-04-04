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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.linkedin.common.util.Notifier;
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
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcherImpl;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;

import org.testng.annotations.Test;

public class TestDarkClusterFilter
{
  @Test
  public void testDarkClusterAssembly()
  {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient(false));
    ClusterInfoProvider clusterInfoProvider = new SimpleLoadBalancer(new LoadBalancerTestState(), scheduledExecutorService);
    Facilities facilities = new MockFacilities(clusterInfoProvider);
    Notifier notifier = new DoNothingNotifier();
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    String sourceClusterName = "MyCluster";
    DarkClusterVerifier darkClusterVerifier = new NoOpDarkClusterVerifier();
    DarkClusterVerifierManager verifierManager = new DarkClusterVerifierManagerImpl(darkClusterVerifier, executorService);
    Random random = new Random();

    DarkClusterStrategyFactory darkClusterStrategyFactory = new DarkClusterStrategyFactoryImpl(facilities, sourceClusterName,
                                                                                               darkClusterDispatcher,
                                                                                               notifier, random,
                                                                                               verifierManager);

    DarkClusterManager darkClusterManager = new DarkClusterManagerImpl(sourceClusterName,
                                                                       facilities,
                                                                       darkClusterStrategyFactory,
                                                                       "", "",
                                                                       notifier);
    DarkClusterFilter darkClusterFilter = new DarkClusterFilter(darkClusterManager, verifierManager);

    darkClusterStrategyFactory.start();
    RestRequest restRequest = new RestRequestBuilder(URI.create("foo")).build();
    darkClusterFilter.onRestRequest(restRequest, new RequestContext(), new HashMap<>(), new DummyNextFilter());
    darkClusterFilter.onRestError(new RuntimeException("test"), new RequestContext(), new HashMap<>(), new DummyNextFilter());
    darkClusterFilter.onRestResponse(new RestResponseBuilder().build(), new RequestContext(), new HashMap<>(), new DummyNextFilter());
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
