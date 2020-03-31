package com.linkedin.darkcluster;

import java.net.URI;
import java.util.Random;

import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.impl.BaseDarkClusterDispatcherImpl;
import com.linkedin.darkcluster.impl.ConstantMultiplierDarkClusterStrategy;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcherImpl;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestConstantMultiplierDarkClusterStrategy
{
  private static final String SOURCE_CLUSTER_NAME = "FooCluster";
  private static final String DARK_CLUSTER_NAME = "fooCluster-dark";
  private static final int SEED = 2;
  private static final float ERR_PCT = 0.30f; // 5%

  @DataProvider
  public Object[][] multiplierKeys()
  {
    return new Object[][] {
      // numIterations, multiplier, numSourceInstances, numDarkInstances
      {0, 0f, 10, 10},
      {0, 1f, 10, 10},
      {1000, 0.1f, 10, 10},
      {1000, 0.25f, 10, 10},
      {1000, 0.5f, 10, 10},
      {1000, 1f, 10, 10},
      {1000, 1.5f, 10, 10},
      {100, 2f, 10, 10},
      // now test typical case of multiplier ~1 with different instance sizes
      {1000, 1f, 10, 1},
      {1000, 0.9f, 10, 1},
      {1000, 1.2f, 10, 1},
      {1000, 1f, 10, 2},
      {1000, 1f, 40, 3},
      {1000, 2f, 10, 1},
      {1000, 2.5f, 10, 1},
      {1000, 4f, 10, 1}
    };
  }

  // disabled til ConstantMultiplierDarkClusterStrategy is fully implemented.
  @Test(dataProvider = "multiplierKeys")
  public void testStrategy(int numIterations, float multiplier, int numSourceInstances, int numDarkInstances)
  {
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient(false));
    BaseDarkClusterDispatcherImpl baseDispatcher = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME,
                                                                                 darkClusterDispatcher,
                                                                                 new DoNothingNotifier(),
                                                                                 new CountingVerifierManager());
    MockClusterInfoProvider mockClusterInfoProvider = new MockClusterInfoProvider();
    mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, numDarkInstances);
    mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, numSourceInstances);
    ConstantMultiplierDarkClusterStrategy strategy = new ConstantMultiplierDarkClusterStrategy(SOURCE_CLUSTER_NAME,
                                                                                               DARK_CLUSTER_NAME,
                                                                                               multiplier,
                                                                                               baseDispatcher,
                                                                                               new DoNothingNotifier(),
                                                                                               mockClusterInfoProvider,
                                                                                               new Random(SEED));
    for (int i=0; i < numIterations; i++)
    {
      RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
      strategy.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
    }
    int expectedCount = (int) (numIterations * multiplier * numDarkInstances)/(numSourceInstances);
    int actualCount = baseDispatcher.getRequestCount();
    Assert.assertEquals(actualCount, expectedCount, expectedCount * ERR_PCT, "count not within expected range");
  }
}
