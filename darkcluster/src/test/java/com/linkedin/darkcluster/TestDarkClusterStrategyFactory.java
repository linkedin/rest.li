package com.linkedin.darkcluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.NoOpDarkClusterStrategy;
import com.linkedin.darkcluster.impl.ConstantMultiplierDarkClusterStrategy;
import com.linkedin.darkcluster.impl.DarkClusterStrategyFactoryImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcherImpl;
import com.linkedin.r2.message.RequestContext;

import static org.testng.Assert.fail;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDarkClusterStrategyFactory
{
  private static final String SOURCE_CLUSTER_NAME = "FooCluster";
  private static final String DARK_CLUSTER_NAME = "FooCluster-dark";
  private static final int SEED = 2;

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
    boolean requestSent = strategy.handleRequest(new TestRestRequest(), new TestRestRequest(), new RequestContext());
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
    strategy.handleRequest(new TestRestRequest(), new TestRestRequest(), new RequestContext());
    strategy2.handleRequest(new TestRestRequest(), new TestRestRequest(), new RequestContext());
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
  private static class MockClusterInfoProvider implements ClusterInfoProvider
  {
    Map<String, DarkClusterConfigMap> lookupMap = new HashMap<>();
    List<LoadBalancerClusterListener> clusterListeners = new ArrayList<>();

    @Override
    public int getClusterCount(String clusterName, String scheme, int partitionId)
      throws ServiceUnavailableException
    {
      return 0;
    }

    @Override
    public int getHttpsClusterCount(String clusterName)
      throws ServiceUnavailableException
    {
      return 0;
    }

    @Override
    public DarkClusterConfigMap getDarkClusterConfigMap(String clusterName)
      throws ServiceUnavailableException
    {
      return lookupMap.get(clusterName);
    }

    @Override
    public void registerClusterListener(LoadBalancerClusterListener clusterListener)
    {
      clusterListeners.add(clusterListener);
    }

    /**
     * add the ability to add a dark cluster to a source cluster's darkClusterConfigMap
     */
    void addDarkClusterConfig(String sourceClusterName, String darkClusterName, DarkClusterConfig darkClusterConfig)
    {
      DarkClusterConfigMap darkClusterConfigMap = (lookupMap.containsKey(sourceClusterName)) ? lookupMap.get(sourceClusterName) :
        new DarkClusterConfigMap();

      darkClusterConfigMap.put(darkClusterName, darkClusterConfig);
      lookupMap.put(sourceClusterName, darkClusterConfigMap);
    }

    void triggerClusterRefresh(String clusterName)
    {
      for (LoadBalancerClusterListener listener : clusterListeners)
      {
        listener.onClusterAdded(clusterName);
      }
    }

    void triggerClusterRemove(String clusterName)
    {
      for (LoadBalancerClusterListener listener : clusterListeners)
      {
        listener.onClusterRemoved(clusterName);
      }
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
