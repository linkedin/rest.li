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
package com.linkedin.d2.backuprequests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.d2.backuprequests.BoundedCostBackupRequestsStrategy;


public class TestBoundedCostBackupRequestsStrategy
{

  private static final int ITERATIONS = 100000;
  private static final double EXPECTED_PRECISSION = 0.5;

  @Test
  public void testNumberOfBackupRequestsMade()
  {
    for (int percent = 1; percent < 6; percent++)
    {
      int burstSize = 64;
      double percentOfBackupRequests = testNumberOfBackupRequestsMade(percent, burstSize);
      double relativePctError = Math.abs((percent - percentOfBackupRequests) / percent);
      Assert.assertTrue(relativePctError < EXPECTED_PRECISSION,
          "percent: " + percent + ", burstSize: " + burstSize + ", result: " + percentOfBackupRequests + ", expected: "
              + percent + ", relative % error: " + relativePctError + ", expected precission: " + EXPECTED_PRECISSION);
    }
  }

  public double testNumberOfBackupRequestsMade(int pct, int burstSize)
  {
    BoundedCostBackupRequestsStrategy strategy = new BoundedCostBackupRequestsStrategy(pct, burstSize, 1024, 128, 0);

    BackupRequestsSimulator simulator = new BackupRequestsSimulator(new PoissonEventsArrival(200, TimeUnit.SECONDS),
        new GaussianResponseTimeDistribution(20, 100, 50, TimeUnit.MILLISECONDS), strategy);
    simulator.simulate(ITERATIONS);
    return ((100d * simulator.getNumberOfBackupRequestsMade()) / ITERATIONS);
  }

  @Test
  public void testMinBackupDelay()
  {
    BoundedCostBackupRequestsStrategy strategy = new BoundedCostBackupRequestsStrategy(5, 64, 1024, 128, 100);

    BackupRequestsSimulator simulator = new BackupRequestsSimulator(new PoissonEventsArrival(200, TimeUnit.SECONDS),
        new GaussianResponseTimeDistribution(10, 50, 10, TimeUnit.MILLISECONDS), strategy);
    simulator.simulate(ITERATIONS);
    assertEquals(simulator.getNumberOfBackupRequestsMade(), 0);
  }

  @Test
  public void testLongTailEffectOfBackupRequests()
  {
    BoundedCostBackupRequestsStrategy strategy = new BoundedCostBackupRequestsStrategy(5, 64, 1024, 128, 0);

    ResponseTimeDistribution hiccupDistribution =
        new GaussianResponseTimeDistribution(500, 1000, 500, TimeUnit.MILLISECONDS);

    BackupRequestsSimulator simulator = new BackupRequestsSimulator(new PoissonEventsArrival(200, TimeUnit.SECONDS),
        new GaussianWithHiccupResponseTimeDistribution(2, 10, 5, TimeUnit.MILLISECONDS, hiccupDistribution, 0.02),
        strategy);

    simulator.simulate(ITERATIONS);

    double withoutBackup99 = simulator.getResponseTimeWithoutBackupRequestsHistogram().getValueAtPercentile(99);
    double withBackup99 = simulator.getResponseTimeWithBackupRequestsHistogram().getValueAtPercentile(99);

    assertTrue(withBackup99 * 10 < withoutBackup99, "99th percentile is expected to be improved 10x, with backup: "
        + withBackup99 / 1000000 + "ms, without backup: " + withoutBackup99 / 1000000 + "ms");
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testZeroPercent()
  {
    new BoundedCostBackupRequestsStrategy(0, 64, 1024, 128, 0);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testNegativePercent()
  {
    new BoundedCostBackupRequestsStrategy(-10, 64, 1024, 128, 0);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testZeroMaxBurst()
  {
    new BoundedCostBackupRequestsStrategy(1, 0, 1024, 128, 0);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testNegativeMaxBurst()
  {
    new BoundedCostBackupRequestsStrategy(1, -10, 1024, 128, 0);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testHistoryTooSmall()
  {
    new BoundedCostBackupRequestsStrategy(1, 64, 10, 128, 0);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testRequiredHistoryTooSmall()
  {
    new BoundedCostBackupRequestsStrategy(1, 64, 1024, 10, 0);
  }

  @Test(expectedExceptions = { IllegalArgumentException.class })
  public void testNegativeMinBackupDelay()
  {
    new BoundedCostBackupRequestsStrategy(1, 64, 1024, 128, -10);
  }

  @Test
  public void testValuesOutOfRange()
  {
    BoundedCostBackupRequestsStrategy strategy = new BoundedCostBackupRequestsStrategy(5, 64, 1024, 128, 0);

    BackupRequestsSimulator simulator = new BackupRequestsSimulator(new PoissonEventsArrival(200, TimeUnit.SECONDS),
        new GaussianResponseTimeDistribution(BoundedCostBackupRequestsStrategy.HIGH,
            2 * BoundedCostBackupRequestsStrategy.HIGH, BoundedCostBackupRequestsStrategy.HIGH, TimeUnit.NANOSECONDS),
        strategy);
    simulator.simulate(ITERATIONS);
    assertTrue(((100d * simulator.getNumberOfBackupRequestsMade()) / ITERATIONS) < 5 + EXPECTED_PRECISSION);
    assertTrue(strategy.getTimeUntilBackupRequestNano().get() >= BoundedCostBackupRequestsStrategy.HIGH);
  }

}
