/*
   Copyright (c) 2021 LinkedIn Corp.

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

import com.linkedin.darkcluster.impl.ConstantQpsDarkClusterStrategy;
import com.linkedin.r2.transport.http.client.ConstantQpsRateLimiter;
import com.linkedin.r2.transport.http.client.EvictingCircularBuffer;
import com.linkedin.test.util.ClockedExecutor;
import com.linkedin.util.clock.Clock;
import java.net.URI;

import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.impl.BaseDarkClusterDispatcherImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcher;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;

import java.time.temporal.ChronoUnit;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestConstantQpsDarkClusterStrategy
{
  private static final String SOURCE_CLUSTER_NAME = "FooCluster";
  private static final String DARK_CLUSTER_NAME = "fooCluster-dark";
  private static final float ERR_PCT = 0.30f; // 5%

  private static final int TEST_CAPACITY = 5;
  private static final int TEST_TTL = 5;
  private static final ChronoUnit TEST_TTL_UNIT = ChronoUnit.SECONDS;

  @DataProvider
  public Object[][] qpsKeys()
  {
    return new Object[][]{
        // numIterations, qps, numSourceInstances, numDarkInstances
        {0, 0, 10, 10},
        {10, 10, 10, 0},
        {0, 100, 10, 10},
        {1000, 10, 10, 10},
        {1000, 30, 10, 10},
        {1000, 50, 10, 10},
        {1000, 100, 10, 10},
        {1000, 150, 10, 10},
        {100, 200, 10, 10},
        // now test typical case of differing qps with different instance sizes
        {1000, 100, 10, 1},
        {1000, 90, 10, 1},
        {1000, 120, 10, 1},
        {1000, 100, 10, 2},
        {1000, 100, 40, 3},
        {1000, 200, 10, 1},
        {1000, 250, 10, 1},
        {1000, 400, 10, 1}
    };
  }

  @Test(dataProvider = "qpsKeys")
  public void testStrategy(int numIterations, int qps, int numSourceInstances, int numDarkInstances)
  {
    IntStream.of(1, 1000, 1000000).forEach(capacity ->
    {
      DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcher(new MockClient(false));
      BaseDarkClusterDispatcherImpl baseDispatcher = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME,
          darkClusterDispatcher,
          new DoNothingNotifier(),
          new CountingVerifierManager());
      MockClusterInfoProvider mockClusterInfoProvider = new MockClusterInfoProvider();
      mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME, numDarkInstances);
      mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, numSourceInstances);
      ClockedExecutor executor = new ClockedExecutor();

      EvictingCircularBuffer buffer = TestConstantQpsDarkClusterStrategy.getBuffer(executor);
      ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, buffer);
      rateLimiter.setBufferCapacity(capacity);
      ConstantQpsDarkClusterStrategy strategy = new ConstantQpsDarkClusterStrategy(SOURCE_CLUSTER_NAME,
          DARK_CLUSTER_NAME,
          qps,
          baseDispatcher,
          new DoNothingNotifier(),
          mockClusterInfoProvider,
          rateLimiter);
      for (int i=0; i < numIterations; i++)
      {
        RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
        strategy.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
      }
      executor.runFor(1000);
      int expectedCount = ((numIterations == 0 ? 0 : 1) * qps * numDarkInstances)/(numSourceInstances);
      int actualCount = baseDispatcher.getRequestCount();
      Assert.assertEquals(actualCount, expectedCount, expectedCount * ERR_PCT, "count not within expected range");
    });
  }

  static EvictingCircularBuffer getBuffer(Clock clock)
  {
    return new EvictingCircularBuffer(TEST_CAPACITY, TEST_TTL, TEST_TTL_UNIT, clock);
  }
}
