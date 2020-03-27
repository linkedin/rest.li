package com.linkedin.darkcluster;

import java.net.URI;
import java.util.Random;

import com.linkedin.darkcluster.api.BaseDarkClusterDispatcher;
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
  private static final float ERR_PCT = 0.05f; // 5%

  @DataProvider
  public Object[][] multiplierKeys()
  {
    return new Object[][] {
      // numIterations, multiplier
      {0, 0f},
      {0, 1f},
      {100, 0.1f},
      {100, 0.5f},
      {100, 1f},
      {100, 1.5f},
      {100, 2f},
      {1000, 0.25f},
      {1000, 1f}
    };
  }

  // disabled til ConstantMultiplierDarkClusterStrategy is fully implemented.
  @Test(dataProvider = "multiplierKeys", enabled=false)
  public void testStrategy(int numIterations, float multiplier)
  {
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcherImpl(new MockClient(false));
    BaseDarkClusterDispatcherImpl baseDispatcher = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME,
                                                                                 darkClusterDispatcher,
                                                                                 new DoNothingNotifier(),
                                                                                 new CountingVerifierManager());
    ConstantMultiplierDarkClusterStrategy strategy = new ConstantMultiplierDarkClusterStrategy(SOURCE_CLUSTER_NAME,
                                                                                               DARK_CLUSTER_NAME,
                                                                                               multiplier,
                                                                                               baseDispatcher,
                                                                                               new DoNothingNotifier(),
                                                                                               new Random(SEED));
    for (int i=0; i < numIterations; i++)
    {
      RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
      strategy.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
    }
    int expectedCount = (int) (numIterations * multiplier);
    int actualCount = baseDispatcher.getRequestCount();
    Assert.assertEquals(actualCount, expectedCount, expectedCount * ERR_PCT, "count not within expected range");
  }
}
