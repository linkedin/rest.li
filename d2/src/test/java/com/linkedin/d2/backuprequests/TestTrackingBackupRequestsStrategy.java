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
import static org.testng.Assert.assertNotNull;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.testng.annotations.Test;


public class TestTrackingBackupRequestsStrategy
{

  @Test
  public void testNoActivityStats()
  {
    TrackingBackupRequestsStrategy trackingStrategy =
        new TrackingBackupRequestsStrategy(new MockBackupRequestsStrategy(() -> Optional.of(10000000L), () -> true));
    BackupRequestsStrategyStats stats = trackingStrategy.getStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 0);
    assertEquals(stats.getSuccessful(), 0);
    assertEquals(stats.getMinDelayNano(), 0);
    assertEquals(stats.getMaxDelayNano(), 0);
    assertEquals(stats.getAvgDelayNano(), 0);
    stats = trackingStrategy.getStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 0);
    assertEquals(stats.getSuccessful(), 0);
    assertEquals(stats.getMinDelayNano(), 0);
    assertEquals(stats.getMaxDelayNano(), 0);
    assertEquals(stats.getAvgDelayNano(), 0);
  }

  @Test
  public void testNoActivityDiffStats()
  {
    TrackingBackupRequestsStrategy trackingStrategy =
        new TrackingBackupRequestsStrategy(new MockBackupRequestsStrategy(() -> Optional.of(10000000L), () -> true));
    BackupRequestsStrategyStats stats = trackingStrategy.getDiffStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 0);
    assertEquals(stats.getSuccessful(), 0);
    assertEquals(stats.getMinDelayNano(), 0);
    assertEquals(stats.getMaxDelayNano(), 0);
    assertEquals(stats.getAvgDelayNano(), 0);
    stats = trackingStrategy.getDiffStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 0);
    assertEquals(stats.getSuccessful(), 0);
    assertEquals(stats.getMinDelayNano(), 0);
    assertEquals(stats.getMaxDelayNano(), 0);
    assertEquals(stats.getAvgDelayNano(), 0);
  }

  @Test
  public void testGetStatsConstantDelay()
  {
    final long constantDelay = 10000000L;

    TrackingBackupRequestsStrategy trackingStrategy = new TrackingBackupRequestsStrategy(
        new MockBackupRequestsStrategy(() -> Optional.of(constantDelay), () -> true));
    for (int i = 0; i < 100; i++)
    {
      trackingStrategy.isBackupRequestAllowed();
    }
    for (int i = 0; i < 100; i++)
    {
      trackingStrategy.backupRequestSuccess();
    }
    for (int i = 0; i < 100; i++)
    {
      trackingStrategy.getTimeUntilBackupRequestNano();
    }

    BackupRequestsStrategyStats stats = trackingStrategy.getStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 100);
    assertEquals(stats.getSuccessful(), 100);
    assertEquals(stats.getMinDelayNano(), constantDelay);
    assertEquals(stats.getMaxDelayNano(), constantDelay);
    assertEquals(stats.getAvgDelayNano(), constantDelay);
    stats = trackingStrategy.getStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 100);
    assertEquals(stats.getSuccessful(), 100);
    assertEquals(stats.getMinDelayNano(), constantDelay);
    assertEquals(stats.getMaxDelayNano(), constantDelay);
    assertEquals(stats.getAvgDelayNano(), constantDelay);
    stats = trackingStrategy.getDiffStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 100);
    assertEquals(stats.getSuccessful(), 100);
    assertEquals(stats.getMinDelayNano(), constantDelay);
    assertEquals(stats.getMaxDelayNano(), constantDelay);
    assertEquals(stats.getAvgDelayNano(), constantDelay);
    stats = trackingStrategy.getDiffStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 0);
    assertEquals(stats.getSuccessful(), 0);
    assertEquals(stats.getMinDelayNano(), 0);
    assertEquals(stats.getMaxDelayNano(), 0);
    assertEquals(stats.getAvgDelayNano(), 0);

    for (int i = 0; i < 100; i++)
    {
      trackingStrategy.isBackupRequestAllowed();
    }
    for (int i = 0; i < 100; i++)
    {
      trackingStrategy.backupRequestSuccess();
    }
    for (int i = 0; i < 100; i++)
    {
      trackingStrategy.getTimeUntilBackupRequestNano();
    }

    stats = trackingStrategy.getStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 200);
    assertEquals(stats.getSuccessful(), 200);
    assertEquals(stats.getMinDelayNano(), constantDelay);
    assertEquals(stats.getMaxDelayNano(), constantDelay);
    assertEquals(stats.getAvgDelayNano(), constantDelay);
    stats = trackingStrategy.getStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 200);
    assertEquals(stats.getSuccessful(), 200);
    assertEquals(stats.getMinDelayNano(), constantDelay);
    assertEquals(stats.getMaxDelayNano(), constantDelay);
    assertEquals(stats.getAvgDelayNano(), constantDelay);
    stats = trackingStrategy.getDiffStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 100);
    assertEquals(stats.getSuccessful(), 100);
    assertEquals(stats.getMinDelayNano(), constantDelay);
    assertEquals(stats.getMaxDelayNano(), constantDelay);
    assertEquals(stats.getAvgDelayNano(), constantDelay);
    stats = trackingStrategy.getDiffStats();
    assertNotNull(stats);
    assertEquals(stats.getAllowed(), 0);
    assertEquals(stats.getSuccessful(), 0);
    assertEquals(stats.getMinDelayNano(), 0);
    assertEquals(stats.getMaxDelayNano(), 0);
    assertEquals(stats.getAvgDelayNano(), 0);
  }

  @Test
  public void testGetStats()
  {

    Distribution distribution = new Distribution();

    TrackingBackupRequestsStrategy trackingStrategy =
        new TrackingBackupRequestsStrategy(new MockBackupRequestsStrategy(() -> Optional.of(distribution.next()),
            () -> ThreadLocalRandom.current().nextBoolean()));

    long totalAllowed = 0;
    long totalSuccessful = 0;
    long totalMin = Long.MAX_VALUE;
    long totalMax = Long.MIN_VALUE;

    for (int round = 0; round < 100; round++)
    {
      for (int i = 0; i < 100000; i++)
      {
        if (trackingStrategy.isBackupRequestAllowed())
        {
          if (ThreadLocalRandom.current().nextBoolean())
          {
            trackingStrategy.backupRequestSuccess();
          }
        }
        trackingStrategy.getTimeUntilBackupRequestNano();
      }
      BackupRequestsStrategyStats stats = trackingStrategy.getDiffStats();
      assertEquals((double) stats.getAllowed(), 100000d / 2, 1000d);
      assertEquals((double) stats.getSuccessful(), 100000d / 4, 1000d);
      assertEquals(stats.getMinDelayNano(), distribution._min);
      assertEquals(stats.getMaxDelayNano(), distribution._max);
      assertEquals((double) stats.getAvgDelayNano(), (double) Distribution.AVG, 1000000d);
      totalAllowed += stats.getAllowed();
      totalSuccessful += stats.getSuccessful();
      totalMin = Math.min(totalMin, stats.getMinDelayNano());
      totalMax = Math.max(totalMax, stats.getMaxDelayNano());
      distribution._min = Long.MAX_VALUE;
      distribution._max = Long.MIN_VALUE;
    }

    // total stats
    BackupRequestsStrategyStats stats = trackingStrategy.getStats();
    assertEquals(totalAllowed, stats.getAllowed());
    assertEquals(totalSuccessful, stats.getSuccessful());
    assertEquals(stats.getMinDelayNano(), totalMin);
    assertEquals(stats.getMaxDelayNano(), totalMax);
    assertEquals((double) stats.getAvgDelayNano(), (double) Distribution.AVG, 1000000d);
  }

  private static class Distribution
  {
    static long STD_DEV = 10000L;
    static long AVG = 100000000L;

    long _min = Long.MAX_VALUE;
    long _max = Long.MIN_VALUE;
    final GaussianResponseTimeDistribution _distribution =
        new GaussianResponseTimeDistribution(0, AVG, STD_DEV, TimeUnit.NANOSECONDS);

    public long next()
    {
      long value = _distribution.responseTimeNanos();
      _min = Math.min(_min, value);
      _max = Math.max(_max, value);
      return value;
    }
  }

  @Test
  public void testStatsOverflow()
  {
    final long largeDelay = Long.MAX_VALUE / 10;
    TrackingBackupRequestsStrategy trackingStrategy =
        new TrackingBackupRequestsStrategy(new MockBackupRequestsStrategy(() -> Optional.of(largeDelay), () -> true));
    for (int i = 0; i < 10; i++)
    {
      trackingStrategy.isBackupRequestAllowed();
      trackingStrategy.backupRequestSuccess();
      trackingStrategy.getTimeUntilBackupRequestNano();
      BackupRequestsStrategyStats stats = trackingStrategy.getDiffStats();
      assertNotNull(stats);
      assertEquals(stats.getAllowed(), 1);
      assertEquals(stats.getSuccessful(), 1);
      assertEquals(stats.getMinDelayNano(), largeDelay);
      assertEquals(stats.getMaxDelayNano(), largeDelay);
      assertEquals(stats.getAvgDelayNano(), largeDelay);
    }
    trackingStrategy.isBackupRequestAllowed();
    trackingStrategy.backupRequestSuccess();
    trackingStrategy.getTimeUntilBackupRequestNano();

    BackupRequestsStrategyStats overflownStats = trackingStrategy.getDiffStats();
    assertEquals(overflownStats.getAllowed(), 1);
    assertEquals(overflownStats.getSuccessful(), 1);
    assertEquals(overflownStats.getMinDelayNano(), 0);
    assertEquals(overflownStats.getMaxDelayNano(), 0);
    assertEquals(overflownStats.getAvgDelayNano(), 0);

    for (int i = 0; i < 9; i++)
    {
      trackingStrategy.isBackupRequestAllowed();
      trackingStrategy.backupRequestSuccess();
      trackingStrategy.getTimeUntilBackupRequestNano();
      BackupRequestsStrategyStats stats = trackingStrategy.getDiffStats();
      assertNotNull(stats);
      assertEquals(stats.getAllowed(), 1);
      assertEquals(stats.getSuccessful(), 1);
      assertEquals(stats.getMinDelayNano(), largeDelay);
      assertEquals(stats.getMaxDelayNano(), largeDelay);
      assertEquals(stats.getAvgDelayNano(), largeDelay);
    }
  }

  private static class MockBackupRequestsStrategy implements BackupRequestsStrategy
  {

    private final Supplier<Optional<Long>> _timeUntilBackupRequestNano;
    private final Supplier<Boolean> _backupRequestAllowed;

    public MockBackupRequestsStrategy(Supplier<Optional<Long>> timeUntilBackupRequestNano,
        Supplier<Boolean> backupRequestAllowed)
    {
      _timeUntilBackupRequestNano = timeUntilBackupRequestNano;
      _backupRequestAllowed = backupRequestAllowed;
    }

    @Override
    public Optional<Long> getTimeUntilBackupRequestNano()
    {
      return _timeUntilBackupRequestNano.get();
    }

    @Override
    public void recordCompletion(long responseTime)
    {
    }

    @Override
    public boolean isBackupRequestAllowed()
    {
      return _backupRequestAllowed.get();
    }

  }
}
