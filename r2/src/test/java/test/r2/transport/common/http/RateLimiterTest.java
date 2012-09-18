/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package test.r2.transport.common.http;

import com.linkedin.r2.transport.http.client.RateLimiter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class RateLimiterTest
{

  @Test
  public void testSimple() throws Exception
  {
    final int total = 10;

    // NB on Solaris x86 there seems to be an extra 10ms that gets added to the period; need
    // to figure this out.  For now set the period high enough that period + 10 will be within
    // the tolerance.
    final int period = 100;

    final CountDownLatch latch = new CountDownLatch(total);
    final Runnable incr = new Runnable()
    {
      @Override
      public void run()
      {
        latch.countDown();
      }
    };


    ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
    RateLimiter limiter = new RateLimiter(period, period, period, s);
    limiter.setPeriod(period);
    long start = System.currentTimeMillis();
    long lowTolerance = (total * period) * 4 / 5;
    long highTolerance = (total * period) * 5 / 4;
    for (int i = 0; i < total * period; i++)
    {
      limiter.submit(incr);
    }
    Assert.assertTrue(latch.await(highTolerance, TimeUnit.MILLISECONDS),
                      "Should have finished within " + highTolerance + "ms");
    long t = System.currentTimeMillis() - start;
    Assert.assertTrue(t > lowTolerance, "Should have finished after " + lowTolerance + "ms (took " + t + ")");

  }
}
