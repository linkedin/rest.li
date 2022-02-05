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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestConstantQpsDarkClusterStrategy
{
  private static final String SOURCE_CLUSTER_NAME = "FooCluster";
  private static final String DARK_CLUSTER_NAME_ONE = "fooCluster-dark";
  private static final String DARK_CLUSTER_NAME_TWO = "fooCluster-darkAlso";
  private static final String DARK_CLUSTER_NAME_THREE = "fooCluster-darkAlsoAsWell";
  private static final float ERR_PCT = 0.99f; // 1%

  private static final int TEST_CAPACITY = 5;
  private static final int TEST_TTL = 5;
  private static final ChronoUnit TEST_TTL_UNIT = ChronoUnit.SECONDS;

  @DataProvider
  public Object[][] qpsKeys()
  {
    return new Object[][]{
        // duration, inboundQps, outboundQps, numSourceInstances, numDarkInstances
        {0, 10, 0, 10, 10},
        {10, 10, 10, 10, 0},
        {0, 10, 100, 10, 10},
        {1000, 10, 10, 10, 10},
        {1000, 10, 30, 10, 10},
        {1000, 10, 50, 10, 10},
        {1000, 10, 100, 10, 10},
        {1000, 10, 150, 10, 10},
        {100, 10, 200, 10, 10},
        {3600000, 10, 9.5f, 400, 10},
        // now test typical case of differing qps with different instance sizes
        {1000, 10, 100, 10, 1},
        {1000, 10, 90, 10, 1},
        {1000, 10, 120, 10, 1},
        {1000, 10, 100, 10, 2},
        {1000, 10, 100, 40, 3},
        {1000, 10, 200, 10, 1},
        {1000, 10, 250, 10, 1},
        {1000, 10, 400, 10, 1},
        {3600000, 10, 10, 400, 2}
    };
  }

  @Test(dataProvider = "qpsKeys")
  public void testStrategy(int duration, float inboundQps, float outboundQps, int numSourceInstances, int numDarkInstances)
  {
    IntStream.of(1, 1000, 1000000).forEach(capacity ->
    {
      DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcher(new MockClient(false));
      ClockedExecutor executor = new ClockedExecutor();
      Supplier<ConstantQpsRateLimiter> uniqueRateLimiterSupplier = () -> {
        EvictingCircularBuffer uniqueBuffer = TestConstantQpsDarkClusterStrategy.getBuffer(executor);
        ConstantQpsRateLimiter limiter = new ConstantQpsRateLimiter(executor, executor, executor, uniqueBuffer);
        limiter.setBufferCapacity(capacity);
        limiter.setBufferTtl(Integer.MAX_VALUE, ChronoUnit.DAYS);
        return limiter;
      };
      ConstantQpsRateLimiter sharedRateLimiter = uniqueRateLimiterSupplier.get();
      Supplier<ConstantQpsRateLimiter> sharedRateLimiterSupplier = () -> sharedRateLimiter;
      MockClusterInfoProvider mockClusterInfoProvider = new MockClusterInfoProvider();
      mockClusterInfoProvider.putHttpsClusterCount(SOURCE_CLUSTER_NAME, numSourceInstances);

      // dark cluster 1
      BaseDarkClusterDispatcherImpl baseDispatcherOne = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME_ONE,
          darkClusterDispatcher,
          new DoNothingNotifier(),
          new CountingVerifierManager());
      mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME_ONE, numDarkInstances);
      ConstantQpsRateLimiter rateLimiterOne = sharedRateLimiterSupplier.get();

      // dark cluster 2
      BaseDarkClusterDispatcherImpl baseDispatcherTwo = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME_TWO,
          darkClusterDispatcher,
          new DoNothingNotifier(),
          new CountingVerifierManager());
      mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME_TWO, numDarkInstances);
      ConstantQpsRateLimiter rateLimiterTwo = sharedRateLimiterSupplier.get();

      // dark cluster 3
      BaseDarkClusterDispatcherImpl baseDispatcherThree = new BaseDarkClusterDispatcherImpl(DARK_CLUSTER_NAME_THREE,
          darkClusterDispatcher,
          new DoNothingNotifier(),
          new CountingVerifierManager());
      mockClusterInfoProvider.putHttpsClusterCount(DARK_CLUSTER_NAME_THREE, numDarkInstances);
      ConstantQpsRateLimiter rateLimiterThree = uniqueRateLimiterSupplier.get();

      List<ConstantQpsDarkClusterStrategy> strategies = new ArrayList<>();
      strategies.add(new ConstantQpsDarkClusterStrategy(SOURCE_CLUSTER_NAME,
          DARK_CLUSTER_NAME_ONE,
          outboundQps,
          baseDispatcherOne,
          new DoNothingNotifier(),
          mockClusterInfoProvider,
          rateLimiterOne));
      strategies.add(new ConstantQpsDarkClusterStrategy(SOURCE_CLUSTER_NAME,
          DARK_CLUSTER_NAME_TWO,
          outboundQps,
          baseDispatcherTwo,
          new DoNothingNotifier(),
          mockClusterInfoProvider,
          rateLimiterTwo));
      strategies.add(new ConstantQpsDarkClusterStrategy(SOURCE_CLUSTER_NAME,
          DARK_CLUSTER_NAME_THREE,
          outboundQps,
          baseDispatcherThree,
          new DoNothingNotifier(),
          mockClusterInfoProvider,
          rateLimiterThree));

      // simulate receiving the configured qps while dispatching over the duration
      int msBetweenEachInboundRequest = (int) (1000 / inboundQps);
      for (int runTime=0; runTime<duration; runTime=runTime+msBetweenEachInboundRequest)
      {
        RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
        for (ConstantQpsDarkClusterStrategy strategy : strategies)
        {
          strategy.handleRequest(dummyRestRequest, dummyRestRequest, new RequestContext());
        }
        executor.runFor(msBetweenEachInboundRequest);
      }
      double validation = ((duration == 0 ? 0 : duration / 1000.0) * outboundQps * numDarkInstances)/ (double) (numSourceInstances);
      // Cluster One and Two share a rate limiter, so their combined QPS should match the expected value.
      int actualCountClusterOne = baseDispatcherOne.getRequestCount();
      int actualCountClusterTwo = baseDispatcherTwo.getRequestCount();
      int expectedCountClusterOneAndTwo = (int) validation;
      Assert.assertEquals(actualCountClusterOne + actualCountClusterTwo,
                          expectedCountClusterOneAndTwo, expectedCountClusterOneAndTwo * ERR_PCT,
                          "count not within expected range");

      // Cluster Three uses its own so it matches the expected value on its own.
      int expectedCountClusterThree = (int) validation;
      int actualCountClusterThree = baseDispatcherThree.getRequestCount();
      Assert.assertEquals(actualCountClusterThree, expectedCountClusterThree, expectedCountClusterThree * ERR_PCT, "count not within expected range");
    });
  }

  static EvictingCircularBuffer getBuffer(Clock clock)
  {
    return new EvictingCircularBuffer(TEST_CAPACITY, TEST_TTL, TEST_TTL_UNIT, clock);
  }
}
