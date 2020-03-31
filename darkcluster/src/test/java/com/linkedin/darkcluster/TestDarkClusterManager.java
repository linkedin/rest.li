package com.linkedin.darkcluster;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.darkcluster.impl.DarkClusterManagerImpl;
import com.linkedin.darkcluster.impl.DarkClusterStrategyFactoryImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcherImpl;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import static com.linkedin.darkcluster.TestDarkClusterStrategyFactory.DARK_CLUSTER_NAME;
import static com.linkedin.darkcluster.TestDarkClusterStrategyFactory.SEED;
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
    MockClient mockClient = new MockClient(false);
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(mockClient);
    DarkClusterStrategyFactory strategyFactory = new DarkClusterStrategyFactoryImpl(clusterInfoProvider,
                                                                                    SOURCE_CLUSTER_NAME,
                                                                                    darkClusterDispatcher,
                                                                                    new DoNothingNotifier(),
                                                                                    new Random(SEED),
                                                                                    new CountingVerifierManager());
    DarkClusterManager darkClusterManager = new DarkClusterManagerImpl(SOURCE_CLUSTER_NAME,
                                                                       clusterInfoProvider,
                                                                       strategyFactory,
                                                                       whitelist,
                                                                       blacklist,
                                                                       new DoNothingNotifier());

    // This configuration will choose the ConstantMultiplierDarkClusterStrategy
    DarkClusterConfig darkClusterConfig = new DarkClusterConfig()
      .setMultiplier(1.0f);
    clusterInfoProvider.addDarkClusterConfig(SOURCE_CLUSTER_NAME, DARK_CLUSTER_NAME, darkClusterConfig);
    RestRequest restRequest1 = new RestRequestBuilder(URI.create("/white")).setMethod(httpMethod).build();
    boolean whiteStatus = darkClusterManager.handleDarkRequest(restRequest1, new RequestContext());
    RestRequest restRequest2 = new RestRequestBuilder(URI.create("/black")).setMethod(httpMethod).build();
    boolean blackStatus = darkClusterManager.handleDarkRequest(restRequest2, new RequestContext());
    Assert.assertEquals(whiteStatus, expectedWhiteCount > 0, "white uri requests not as expected");
    Assert.assertEquals(blackStatus, expectedBlackCount > 0, "black uri requests not as expected");
    Assert.assertEquals(mockClient.requestAuthorityMap.getOrDefault(DARK_CLUSTER_NAME, new AtomicInteger()).get(),
                        expectedWhiteCount + expectedBlackCount, "Uri rewritten count is wrong");
  }
}
