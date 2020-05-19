/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.client.multiplexer;


import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.client.ActionsBuilders;

import com.linkedin.test.util.retry.SingleRetry;
import java.util.concurrent.ExecutionException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Integration tests for multiplexer that verify that multiplexer does not introduce delays.
 *
 * @author Dmitriy Yefremov
 */
public class TestMultiplexerDelays extends RestLiIntegrationTest
{
  private static final int DELAY_MILLIS = 100;
  private static final int NUM_REQUESTS = 10;
  private static final double TOLERANCE = 0.5; // +/- 50%

  private final ActionsBuilders actionsBuilders = new ActionsBuilders();

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test(retryAnalyzer = SingleRetry.class) // Often fails the first invocation; needs warmup
  public void parallelTasksCreationDelay() throws Exception
  {
    MultiplexedRequestBuilder muxRequestBuilder = MultiplexedRequestBuilder.createParallelRequest();
    for (int i = 0; i < NUM_REQUESTS; i++)
    {
      ActionRequest<Void> request = actionsBuilders.actionTaskCreationDelay().paramDelay(DELAY_MILLIS).build();
      muxRequestBuilder.addRequest(request, Callbacks.<Response<Void>>empty());
    }
    long actualTime = measureExecutionTime(muxRequestBuilder.build());
    // execution plans for individual requests are created sequentially, that linearly increases total processing time
    int expectedTime = DELAY_MILLIS * NUM_REQUESTS;
    assertInRange(actualTime, expectedTime, TOLERANCE);
  }

  @Test(retryAnalyzer = SingleRetry.class) // Often fails the first invocation; needs warmup
  public void parallelTasksExecutionDelay() throws Exception
  {
    MultiplexedRequestBuilder muxRequestBuilder = MultiplexedRequestBuilder.createParallelRequest();
    for (int i = 0; i < NUM_REQUESTS; i++)
    {
      ActionRequest<Void> request = actionsBuilders.actionTaskExecutionDelay().paramDelay(DELAY_MILLIS).build();
      muxRequestBuilder.addRequest(request, Callbacks.<Response<Void>>empty());
    }
    long actualTime = measureExecutionTime(muxRequestBuilder.build());
    // execution plans for individual requests are executed in parallel, so total time is equal to the longest of individual precessing times
    int expectedTime = DELAY_MILLIS;
    assertInRange(actualTime, expectedTime, TOLERANCE);
  }

  private long measureExecutionTime(MultiplexedRequest multiplexedRequest) throws ExecutionException, InterruptedException
  {
    FutureCallback<MultiplexedResponse> aggregatedCallback = new FutureCallback<MultiplexedResponse>();
    long startTime = System.currentTimeMillis();
    getClient().sendRequest(multiplexedRequest, aggregatedCallback);
    MultiplexedResponse multiplexedResponse = aggregatedCallback.get();
    Assert.assertEquals(multiplexedResponse.getStatus(), HttpStatus.S_200_OK.getCode());
    return System.currentTimeMillis() - startTime;
  }

  private static void assertInRange(long actual, long expected, double tolerance)
  {
    double lowEnd = expected * (1 - tolerance);
    double highEnd = expected * (1 + tolerance);
    Assert.assertTrue(actual > lowEnd && actual < highEnd, actual + "is not in range " + lowEnd + " - " + highEnd);
  }
}
