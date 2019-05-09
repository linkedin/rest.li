/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.ratelimiter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncRateLimiter;
import com.linkedin.r2.transport.http.client.SmoothRateLimiter;
import com.linkedin.util.clock.Clock;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class TestSmoothRateLimiter extends BaseTestSmoothRateLimiter
{

  @Test(timeOut = TEST_TIMEOUT)
  public void testUnlimitedBurstRate()
  {
    Rate rate = new Rate(3, 1000, UNLIMITED_BURST);
    assertEquals(rate.getEvents(), 3);
    assertEquals(rate.getPeriod(), 1000);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testLowBurstRate()
  {
    Rate rate = new Rate(3, 1000, 1);
    assertEquals(rate.getEvents(), 1);
    assertEquals(rate.getPeriod(), 333);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testBurstRateInOneMillisecond()
  {
    // this should be now supported
    // it should just ending up generating 5 events every ms. If we are refreshing every ms,
    // we cannot really say that it is `bursting`
    new Rate(50, 10, 1);
  }


  @Test(timeOut = TEST_TIMEOUT, expectedExceptions = RejectedExecutionException.class)
  public void testSubmitExceedsMaxBuffered()
  {
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
      _scheduledExecutorService, _executor, _clock, _queue, 0);
    rateLimiter.setRate(ONE_PERMIT_PER_PERIOD, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    FutureCallback<None> callback = new FutureCallback<>();
    rateLimiter.submit(callback);
  }


  protected AsyncRateLimiter getRateLimiter(ScheduledExecutorService executorService, ExecutorService executor, Clock clock)
  {
    return new SmoothRateLimiter(executorService, executor, clock, _queue, MAX_BUFFERED_CALLBACKS);
  }
}
