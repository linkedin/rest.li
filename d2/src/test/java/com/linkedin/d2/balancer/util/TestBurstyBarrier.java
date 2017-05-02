/*
   Copyright (c) 2017 LinkedIn Corp.

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
package com.linkedin.d2.balancer.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.Assert;
import org.testng.annotations.Test;


public class TestBurstyBarrier
{

  private static final int ITERATIONS = 1000000;
  private static final double EXPECTED_PRECISSION = 0.1;

  @Test
  public void testLimitPrecision()
  {
    for (int percent = 1; percent < 100; percent++)
    {
      for (int burstSize = 8; burstSize < 513; burstSize *= 2)
      {
        double percentOfBackupRequests = testLimitPrecision(percent, burstSize);
        Assert.assertEquals(percentOfBackupRequests, percent, EXPECTED_PRECISSION,
            "percent: " + percent + ", burstSize: " + burstSize + ", result: " + percentOfBackupRequests
                + ", expected: " + percent + " +/- " + EXPECTED_PRECISSION);
      }
    }
  }

  public double testLimitPrecision(int percent, int burstSize)
  {
    BurstyBarrier barrier = new BurstyBarrier(percent, burstSize);
    int counter = 0;
    for (int i = 0; i < ITERATIONS; i++)
    {
      barrier.arrive();
      if (barrier.canPassThrough())
      {
        counter++;
      }
    }
    return (100d * counter) / ITERATIONS;
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testZeroPercent()
  {
    new BurstyBarrier(0, 10);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testNegativePercent()
  {
    new BurstyBarrier(-10, 10);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testZeroBurstSize()
  {
    new BurstyBarrier(10, 0);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testNegativeBurstSize()
  {
    new BurstyBarrier(10, -10);
  }

  @Test
  public void testMathPrecision()
  {
    double d = BurstyBarrier.MAX_ARRIVALS_WITH_PRECISION;
    double prev = d;
    for (int i = 0; i < 10000; i++)
    {
      d = d + 0.01d;
    }
    assertEquals(d - prev, 100d, 4);
  }

  @Test
  public void testBurstiness()
  {
    BurstyBarrier barrier = new BurstyBarrier(10, 64);
    for (int i = 0; i < 100; i++)
    {
      burstinessRound(barrier);
    }
  }

  private void burstinessRound(BurstyBarrier barrier)
  {
    for (int i = 0; i < 1000; i++)
    {
      barrier.arrive();
    }
    //at this point we expect a burst of 64 evenets to be allowed to pass through
    for (int i = 0; i < 64; i++)
    {
      assertTrue(barrier.canPassThrough());
    }
    //65th can't be allowed
    assertFalse(barrier.canPassThrough());
  }
}
