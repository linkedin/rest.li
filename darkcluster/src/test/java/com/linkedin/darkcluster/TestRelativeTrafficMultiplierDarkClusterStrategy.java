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

import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.impl.BaseDarkClusterDispatcherImpl;
import com.linkedin.darkcluster.impl.RelativeTrafficMultiplierDarkClusterStrategy;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcher;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestRelativeTrafficMultiplierDarkClusterStrategy
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

  @Test(dataProvider = "multiplierKeys")
  public void testStrategy(int numIterations, float multiplier, int numSourceInstances, int numDarkInstances)
  {
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcher(new MockClient(false));
    BaseDarkClusterDispatcherImpl baseDispatcher = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME,
                                                                                 darkClusterDispatcher,
                                                                                 new DoNothingNotifier(),
                                                                                 new CountingVerifierManager());
    MockClusterInfoProvider mockClusterInfoProvider = new MockClusterInfoProvider();
    mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, numDarkInstances);
    mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, numSourceInstances);
    RelativeTrafficMultiplierDarkClusterStrategy strategy = new RelativeTrafficMultiplierDarkClusterStrategy(SOURCE_CLUSTER_NAME,
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
