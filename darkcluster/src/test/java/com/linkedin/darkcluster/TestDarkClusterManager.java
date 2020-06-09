package com.linkedin.darkcluster;

import java.net.URI;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.api.NoOpDarkClusterStrategy;
import com.linkedin.darkcluster.impl.DarkClusterManagerImpl;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import static com.linkedin.darkcluster.DarkClusterTestUtil.createRelativeTrafficMultiplierConfig;
import static com.linkedin.darkcluster.TestDarkClusterStrategyFactory.DARK_CLUSTER_NAME;
import static com.linkedin.darkcluster.TestDarkClusterStrategyFactory.SOURCE_CLUSTER_NAME;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestDarkClusterManager
{
  private static final String METHOD_SAFE = "GET";
  private static final String METHOD_UNSAFE = "POST";

  @DataProvider
  public Object[][] provideKeys()
  {
    return new Object[][] {
      // whitelist, blacklist, httpMethod, expected white count, expected black count
      {null, null, METHOD_SAFE, 1, 1},
      {null, null, METHOD_UNSAFE, 0, 0},
      {".*white.*", null, METHOD_SAFE, 1, 1},
      {".*white.*", null, METHOD_UNSAFE, 1, 0},
      {".*white.*", ".*black.*", METHOD_SAFE, 1, 0},
      {".*white.*", ".*black.*", METHOD_UNSAFE, 1, 0},
      {null, ".*black.*", METHOD_SAFE, 1, 0},
      {null, ".*black.*", METHOD_UNSAFE, 0, 0}
    };
  }

  @Test(dataProvider = "provideKeys")
  public void testBasic(String whitelist, String blacklist, String httpMethod, int expectedWhiteCount, int expectedBlackCount)
  {
    MockClusterInfoProvider clusterInfoProvider = new MockClusterInfoProvider();
    Facilities facilities = new MockFacilities(clusterInfoProvider);
    MockStrategyFactory strategyFactory = new MockStrategyFactory();
    DarkClusterManager darkClusterManager = new DarkClusterManagerImpl(SOURCE_CLUSTER_NAME,
                                                                       facilities,
                                                                       strategyFactory,
                                                                       whitelist,
                                                                       blacklist,
                                                                       new DoNothingNotifier());

    strategyFactory.start();

    // This configuration will choose the RelativeTrafficMultiplierDarkClusterStrategy
    DarkClusterConfig darkClusterConfig = createRelativeTrafficMultiplierConfig(1.0f);
    clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig);

    RestRequest restRequest1 = new RestRequestBuilder(URI.create("/white")).setMethod(httpMethod).build();
    boolean whiteStatus = darkClusterManager.handleDarkRequest(restRequest1, new RequestContext());
    RestRequest restRequest2 = new RestRequestBuilder(URI.create("/black")).setMethod(httpMethod).build();
    boolean blackStatus = darkClusterManager.handleDarkRequest(restRequest2, new RequestContext());

    Assert.assertEquals(whiteStatus, expectedWhiteCount > 0, "white uri requests not as expected");
    Assert.assertEquals(blackStatus, expectedBlackCount > 0, "black uri requests not as expected");
    Assert.assertEquals(strategyFactory.strategyGetOrCreateCount, expectedWhiteCount + expectedBlackCount,
                        "unexpected strategy GetOrCreateCount");
  }

  private static class MockStrategyFactory implements DarkClusterStrategyFactory
  {
    // Always return true from the strategy so that we can count reliably
    private static final DarkClusterStrategy NO_OP_STRATEGY = new NoOpDarkClusterStrategy(true);

    int strategyGetOrCreateCount;

    @Override
    public DarkClusterStrategy get(String darkClusterName, DarkClusterConfig darkClusterConfig)
    {
      strategyGetOrCreateCount++;
      return NO_OP_STRATEGY;
    }

    @Override
    public void start()
    {

    }

    @Override
    public void shutdown()
    {

    }
  }
}
