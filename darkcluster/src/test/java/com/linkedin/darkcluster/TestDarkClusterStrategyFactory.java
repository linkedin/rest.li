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
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.NoOpDarkClusterStrategy;
import com.linkedin.darkcluster.impl.ConstantMultiplierDarkClusterStrategy;
import com.linkedin.darkcluster.impl.DarkClusterStrategyFactoryImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcherImpl;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import static org.testng.Assert.fail;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDarkClusterStrategyFactory
{
  static final String SOURCE_CLUSTER_NAME = "FooCluster";
  static final String DARK_CLUSTER_NAME = "FooCluster-dark";
  static final int SEED = 2;

  @Test
  public void testCreateStrategiesWithNoDarkClusters()
  {
    ClusterInfoProvider clusterInfoProvider = new MockClusterInfoProvider();
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient(false));
    DarkClusterStrategyFactory strategyFactory = new DarkClusterStrategyFactoryImpl(clusterInfoProvider,
                                                                                    SOURCE_CLUSTER_NAME,
                                                                                    darkClusterDispatcher,
                                                                                    new DoNothingNotifier(),
                                                                                    new Random(SEED),
                                                                                    new CountingVerifierManager());
    DarkClusterStrategy strategy = strategyFactory.getOrCreate(DARK_CLUSTER_NAME, new DarkClusterConfig());
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
    boolean requestSent = strategy.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
    Assert.assertTrue(strategy instanceof NoOpDarkClusterStrategy);
    Assert.assertFalse(requestSent, "default empty strategy should not send request");
  }

  @Test
  public void testChangingStrategiesWithDarkClusters()
  {
    MockClusterInfoProvider clusterInfoProvider = new MockClusterInfoProvider();
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient(false));
    DarkClusterStrategyFactory strategyFactory =
      new DarkClusterStrategyFactoryImpl(clusterInfoProvider, SOURCE_CLUSTER_NAME, darkClusterDispatcher, new DoNothingNotifier(), new Random(SEED),
                                         new CountingVerifierManager());
    DarkClusterConfig darkClusterConfig1 = new DarkClusterConfig()
      .setMultiplier(0.5f);
    DarkClusterConfig darkClusterConfig2 = new DarkClusterConfig()
      .setMultiplier(0.1f);
    clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy instanceof ConstantMultiplierDarkClusterStrategy);
    Assert.assertEquals(((ConstantMultiplierDarkClusterStrategy)strategy).getMultiplier(), 0.5f, "expected 0.5f multiplier");

    // update Strategy, simulating a refresh.
    clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig2);
    clusterInfoProvider.triggerClusterRefresh(SOURCE_CLUSTER_NAME);
    // Nothing should have been changed, since we should be ignoring source cluster changes.
    DarkClusterStrategy strategy2 = strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy2 instanceof ConstantMultiplierDarkClusterStrategy);
    Assert.assertEquals(((ConstantMultiplierDarkClusterStrategy)strategy2).getMultiplier(), 0.5f, "expected 0.5f multiplier");

    // now trigger a refresh on the dark cluster. Note that darkClusterConfig1 is ignored since there should already be an entry for this
    // dark cluster, and we should get the strategy associated with darkClusterConfig2 back.
    clusterInfoProvider.triggerClusterRefresh(DARK_CLUSTER_NAME);
    DarkClusterStrategy strategy3 = strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy3 instanceof ConstantMultiplierDarkClusterStrategy);
    Assert.assertEquals(((ConstantMultiplierDarkClusterStrategy)strategy3).getMultiplier(), 0.1f, "expected 0.1f multiplier");

    // if someone has a handle to old strategies, those should still be usable.
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
    strategy.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
    strategy2.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
  }

  @Test
  public void testRacingStrategies()
  {
    int noopStrategyCount = 0;
    MockClusterInfoProvider clusterInfoProvider = new MockClusterInfoProvider();
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient(false));
    DarkClusterStrategyFactory strategyFactory =
      new DarkClusterStrategyFactoryImpl(clusterInfoProvider, SOURCE_CLUSTER_NAME, darkClusterDispatcher, new DoNothingNotifier(), new Random(SEED),
                                         new CountingVerifierManager());
    DarkClusterConfig darkClusterConfig1 = new DarkClusterConfig().setMultiplier(0.5f);
    clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy instanceof ConstantMultiplierDarkClusterStrategy);
    Assert.assertEquals(((ConstantMultiplierDarkClusterStrategy) strategy).getMultiplier(), 0.5f, "expected 0.5f multiplier");

    // this was registered after DarkClusterStrategyFactoryImpl registered it's clusterListener.
    clusterInfoProvider.registerClusterListener(new DeletingClusterListener(clusterInfoProvider));

    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    final CountDownLatch latch = new CountDownLatch(1);

    try
    {
      scheduledExecutorService.scheduleAtFixedRate(() -> {
        clusterInfoProvider.triggerClusterRefresh(DARK_CLUSTER_NAME);
        latch.countDown();
      }, 0, 1, TimeUnit.MILLISECONDS);

      if (!latch.await(30, TimeUnit.SECONDS))
      {
        fail("unable to execute task on executor");
      }

      for (int i = 0; i< 100000; i++)
      {
        strategy = strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
        // verified that this will catch race conditions, saw it happen 9/100k times.
        Assert.assertNotNull(strategy, "null at iteration: " + i);
        if (strategy instanceof NoOpDarkClusterStrategy)
        {
          noopStrategyCount++;
        }
      }
      System.out.println("noopStrategyCount: " + noopStrategyCount);
    }
    catch (InterruptedException ie)
    {
      fail("got interrupted exception", ie);
    }
    finally
    {
      scheduledExecutorService.shutdown();
    }


  }

  private static class DeletingClusterListener implements LoadBalancerClusterListener
  {
    // handle to MockClusterInfoProvider so it can call triggerCluster actions.
    private final MockClusterInfoProvider mockClusterInfoProvider;

    public DeletingClusterListener(MockClusterInfoProvider mockProvider)
    {
      mockClusterInfoProvider = mockProvider;
    }
    @Override
    public void onClusterAdded(String clusterName)
    {
      // if this cluster listener is added after the strategy's clusterlistener, it should have the effect of
      // deleting whatever the first cluster listener added. It would have been more straightforward to have
      // a handle directly to the other clusterListener, but there's no good reason for the StrategyFactory to
      // expose that or allow it to be passed in, as the clusterListener needs to manipulate internal state.
      mockClusterInfoProvider.triggerClusterRemove(clusterName);
    }

    @Override
    public void onClusterRemoved(String clusterName)
    {
      // Don't use the mockClusterInfoProvider here to avoid infinite looping.
    }
  }
}
