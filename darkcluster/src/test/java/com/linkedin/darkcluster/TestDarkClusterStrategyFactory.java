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
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterStrategyNameArray;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.NoOpDarkClusterStrategy;
import com.linkedin.darkcluster.impl.RelativeTrafficMultiplierDarkClusterStrategy;
import com.linkedin.darkcluster.impl.DarkClusterStrategyFactoryImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcher;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import static com.linkedin.d2.DarkClusterStrategyName.CONSTANT_QPS;
import static com.linkedin.d2.DarkClusterStrategyName.RELATIVE_TRAFFIC;
import static com.linkedin.darkcluster.DarkClusterTestUtil.createRelativeTrafficMultiplierConfig;
import static org.testng.Assert.fail;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestDarkClusterStrategyFactory
{
  static final String SOURCE_CLUSTER_NAME = "FooCluster";
  static final String DARK_CLUSTER_NAME = "FooCluster-dark";
  private static final int SEED = 2;
  private DarkClusterStrategyFactory _strategyFactory;
  private MockClusterInfoProvider _clusterInfoProvider;

  @BeforeMethod
  public void setup()
  {
    _clusterInfoProvider = new MockClusterInfoProvider();
    Facilities facilities = new MockFacilities(_clusterInfoProvider);
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcher(new MockClient(false));
    _strategyFactory = new DarkClusterStrategyFactoryImpl(facilities,
                                                          SOURCE_CLUSTER_NAME,
                                                          darkClusterDispatcher,
                                                          new DoNothingNotifier(),
                                                          new Random(SEED),
                                                          new CountingVerifierManager());
    _strategyFactory.start();
  }
  @Test
  public void testCreateStrategiesWithNoDarkClusters()
  {
    DarkClusterStrategy strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, new DarkClusterConfig());
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
    boolean requestSent = strategy.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
    Assert.assertTrue(strategy instanceof NoOpDarkClusterStrategy);
    Assert.assertFalse(requestSent, "default empty strategy should not send request");
  }

  @Test
  public void testNoChangeStrategyOnNotification()
  {
    DarkClusterConfig darkClusterConfig1 = createRelativeTrafficMultiplierConfig(0.5f);
    DarkClusterConfig darkClusterConfig2 = createRelativeTrafficMultiplierConfig(1.0f);
    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy instanceof RelativeTrafficMultiplierDarkClusterStrategy);
    Assert.assertEquals(((RelativeTrafficMultiplierDarkClusterStrategy) strategy).getMultiplier(), 0.5f, "expected 0.5f multiplier");

    // update Strategy, then simulating a notification on the source cluster.
    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig2);
    _clusterInfoProvider.notifyListenersClusterAdded(SOURCE_CLUSTER_NAME);
    // Nothing should have been changed, since we should be ignoring source cluster changes.
    DarkClusterStrategy strategy2 = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy2 instanceof RelativeTrafficMultiplierDarkClusterStrategy);
    Assert.assertEquals(((RelativeTrafficMultiplierDarkClusterStrategy) strategy2).getMultiplier(), 0.5f, "expected 0.5f multiplier");
  }

  @Test
  public void testUpdateStrategyDarkClusterChange()
  {
    DarkClusterConfig darkClusterConfig1 = createRelativeTrafficMultiplierConfig(0.5f);
    DarkClusterConfig darkClusterConfig2 = createRelativeTrafficMultiplierConfig(0.1f);
    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy instanceof RelativeTrafficMultiplierDarkClusterStrategy);
    Assert.assertEquals(((RelativeTrafficMultiplierDarkClusterStrategy) strategy).getMultiplier(), 0.5f, "expected 0.5f multiplier");

    // update the strategy.
    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig2);

    // now trigger a refresh on the dark cluster. Note that darkClusterConfig1 is ignored since there should already be an entry for this
    // dark cluster, and we should get the strategy associated with darkClusterConfig2 back.
    _clusterInfoProvider.notifyListenersClusterAdded(DARK_CLUSTER_NAME);
    DarkClusterStrategy strategy3 = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy3 instanceof RelativeTrafficMultiplierDarkClusterStrategy);
    Assert.assertEquals(((RelativeTrafficMultiplierDarkClusterStrategy)strategy3).getMultiplier(), 0.1f, "expected 0.1f multiplier");

    // if someone has a handle to old strategies, those should still be usable.
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
    strategy.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
  }

  @Test
  public void testChangingStrategiesAfterStoppingListener()
  {
    DarkClusterConfig darkClusterConfig1 = createRelativeTrafficMultiplierConfig(0.5f);
    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy instanceof RelativeTrafficMultiplierDarkClusterStrategy);
    Assert.assertEquals(((RelativeTrafficMultiplierDarkClusterStrategy)strategy).getMultiplier(), 0.5f, "expected 0.5f multiplier");

    _strategyFactory.shutdown();

    // now trigger a refresh on the dark cluster. Note that darkClusterConfig1 is ignored since there should already be an entry for this
    // dark cluster, and we should get the strategy associated with darkClusterConfig2 back.
    _clusterInfoProvider.notifyListenersClusterAdded(DARK_CLUSTER_NAME);
    // Nothing should have been changed, since we should be ignoring source cluster changes.
    DarkClusterStrategy strategy2 = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy2 instanceof RelativeTrafficMultiplierDarkClusterStrategy);
    Assert.assertEquals(((RelativeTrafficMultiplierDarkClusterStrategy)strategy2).getMultiplier(), 0.5f, "expected 0.5f multiplier");
  }

  @Test
  public void testStrategyRaceCondition()
  {
    int noopStrategyCount = 0;

    DarkClusterConfig darkClusterConfig1 = createRelativeTrafficMultiplierConfig(0.5f);
    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
    Assert.assertTrue(strategy instanceof RelativeTrafficMultiplierDarkClusterStrategy);
    Assert.assertEquals(((RelativeTrafficMultiplierDarkClusterStrategy) strategy).getMultiplier(), 0.5f, "expected 0.5f multiplier");

    // this was registered after DarkClusterStrategyFactoryImpl registered it's clusterListener.
    _clusterInfoProvider.registerClusterListener(new DeletingClusterListener(_clusterInfoProvider));

    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    final CountDownLatch latch = new CountDownLatch(1);

    try
    {
      scheduledExecutorService.scheduleAtFixedRate(() -> {
        _clusterInfoProvider.notifyListenersClusterAdded(DARK_CLUSTER_NAME);
        latch.countDown();
      }, 0, 1, TimeUnit.MILLISECONDS);

      if (!latch.await(30, TimeUnit.SECONDS))
      {
        fail("unable to execute task on executor");
      }

      for (int i = 0; i< 100000; i++)
      {
        strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);
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

  @Test
  public void testStrategyFallThru()
  {
    DarkClusterConfig darkClusterConfig1 = createRelativeTrafficMultiplierConfig(0.5f);
    DarkClusterStrategyNameArray darkClusterStrategyList = new DarkClusterStrategyNameArray();
    darkClusterStrategyList.addAll(Arrays.asList(CONSTANT_QPS, RELATIVE_TRAFFIC));
    darkClusterConfig1.setMultiplierStrategyList(darkClusterStrategyList);

    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);

    // test that we didn't find a strategy corresponding to Constant QPS and fell through to Relative traffic
    Assert.assertTrue(strategy instanceof RelativeTrafficMultiplierDarkClusterStrategy);
    Assert.assertEquals(((RelativeTrafficMultiplierDarkClusterStrategy)strategy).getMultiplier(), 0.5f, "expected 0.5f multiplier");
  }

  @Test
  public void testStrategyFallThruWithNoFallback()
  {
    DarkClusterConfig darkClusterConfig1 = createRelativeTrafficMultiplierConfig(0.5f);
    DarkClusterStrategyNameArray darkClusterStrategyList = new DarkClusterStrategyNameArray();
    // Only ConstantQPS strategy is present, with no alternative.
    darkClusterStrategyList.addAll(Collections.singletonList(CONSTANT_QPS));
    darkClusterConfig1.setMultiplierStrategyList(darkClusterStrategyList);

    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);

    // test that we didn't find a strategy corresponding to Constant QPS and fell through. It will end up with the NoOpStrategy.
    Assert.assertTrue(strategy instanceof NoOpDarkClusterStrategy);
  }

  @Test
  public void testStrategyZeroMultiplier()
  {
    DarkClusterConfig darkClusterConfig1 = createRelativeTrafficMultiplierConfig(0f);
    DarkClusterStrategyNameArray darkClusterStrategyList = new DarkClusterStrategyNameArray();
    darkClusterStrategyList.addAll(Collections.singletonList(RELATIVE_TRAFFIC));
    darkClusterConfig1.setMultiplierStrategyList(darkClusterStrategyList);

    _clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig1);
    DarkClusterStrategy strategy = _strategyFactory.getOrCreate(DARK_CLUSTER_NAME, darkClusterConfig1);

    // test that we choose a NoOpDarkClusterStrategy because we want to allow RelativeTrafficMultiplierStrategy with a zero muliplier to be
    // a NoOp. This allows clients to easily turn off traffic without adjusting multiple values.
    Assert.assertTrue(strategy instanceof NoOpDarkClusterStrategy);
  }

  private static class DeletingClusterListener implements LoadBalancerClusterListener
  {
    // handle to MockClusterInfoProvider so it can call triggerCluster actions.
    private final MockClusterInfoProvider mockClusterInfoProvider;

    DeletingClusterListener(MockClusterInfoProvider mockProvider)
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
      mockClusterInfoProvider.notifyListenersClusterRemoved(clusterName);
    }

    @Override
    public void onClusterRemoved(String clusterName)
    {
      // Don't use the mockClusterInfoProvider here to avoid infinite looping.
    }
  }
}
