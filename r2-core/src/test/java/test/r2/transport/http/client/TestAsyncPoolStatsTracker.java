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
package test.r2.transport.http.client;

import com.linkedin.common.stats.LongTracking;
import com.linkedin.r2.transport.http.client.AsyncPoolLifecycleStats;
import com.linkedin.r2.transport.http.client.AsyncPoolStats;
import com.linkedin.r2.transport.http.client.AsyncPoolStatsTracker;
import com.linkedin.r2.transport.http.client.PoolStats;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.Time;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 * @version $Revision: $
 */
public class TestAsyncPoolStatsTracker
{
  private static final PoolStats.LifecycleStats LIFECYCLE_STATS = new AsyncPoolLifecycleStats(0, 0, 0, 0);

  private static final long SAMPLING_DURATION_INCREMENT = Time.minutes(2L);

  private static final int MAX_SIZE = Integer.MAX_VALUE;
  private static final int MIN_SIZE = 0;
  private static final int IDLE_SIZE = 100;
  private static final int POOL_SIZE = 200;
  private static final int CHECKED_OUT = 300;
  private static final long WAIT_TIME = 400;

  private static final int DESTROY_ERROR_INCREMENTS = 10;
  private static final int DESTROY_INCREMENTS = 20;
  private static final int TIMEOUT_INCREMENTS = 30;
  private static final int CREATE_ERROR_INCREMENTS = 40;
  private static final int BAD_DESTROY_INCREMENTS = 50;
  private static final int CREATED_INCREMENTS = 60;

  private static final SettableClock CLOCK = new SettableClock();

  private int _poolSize = POOL_SIZE;
  private int _checkedOut = CHECKED_OUT;

  @BeforeMethod
  public void doBeforeMethod()
  {
    _poolSize = POOL_SIZE;
    _checkedOut = CHECKED_OUT;
  }

  @Test
  public void testDefaults()
  {
    AsyncPoolStatsTracker tracker = new AsyncPoolStatsTracker(
        () -> LIFECYCLE_STATS,
        () -> MAX_SIZE,
        () -> MIN_SIZE,
        () -> POOL_SIZE,
        () -> CHECKED_OUT,
        () -> IDLE_SIZE,
        CLOCK,
        new LongTracking());

    AsyncPoolStats stats = tracker.getStats();
    Assert.assertSame(stats.getLifecycleStats(), LIFECYCLE_STATS);
    Assert.assertEquals(stats.getMaxPoolSize(), MAX_SIZE);
    Assert.assertEquals(stats.getMinPoolSize(), MIN_SIZE);
    Assert.assertEquals(stats.getIdleCount(), IDLE_SIZE);
    Assert.assertEquals(stats.getCheckedOut(), CHECKED_OUT);
    Assert.assertEquals(stats.getPoolSize(), POOL_SIZE);

    Assert.assertEquals(stats.getTotalDestroyErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyed(), 0);
    Assert.assertEquals(stats.getTotalTimedOut(), 0);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalBadDestroyed(), 0);
    Assert.assertEquals(stats.getTotalCreated(), 0);

    Assert.assertEquals(stats.getWaitTime50Pct(), 0);
    Assert.assertEquals(stats.getWaitTime95Pct(), 0);
    Assert.assertEquals(stats.getWaitTime99Pct(), 0);
    Assert.assertEquals(stats.getWaitTimeAvg(), 0.0);
  }

  @Test
  public void testIncrements()
  {
    AsyncPoolStatsTracker tracker = new AsyncPoolStatsTracker(
        () -> LIFECYCLE_STATS,
        () -> MAX_SIZE,
        () -> MIN_SIZE,
        () -> POOL_SIZE,
        () -> CHECKED_OUT,
        () -> IDLE_SIZE,
        CLOCK,
        new LongTracking());

    IntStream.range(0, DESTROY_ERROR_INCREMENTS).forEach(i -> tracker.incrementDestroyErrors());
    IntStream.range(0, DESTROY_INCREMENTS).forEach(i -> tracker.incrementDestroyed());
    IntStream.range(0, TIMEOUT_INCREMENTS).forEach(i -> tracker.incrementTimedOut());
    IntStream.range(0, CREATE_ERROR_INCREMENTS).forEach(i -> tracker.incrementCreateErrors());
    IntStream.range(0, BAD_DESTROY_INCREMENTS).forEach(i -> tracker.incrementBadDestroyed());
    IntStream.range(0, CREATED_INCREMENTS).forEach(i -> tracker.incrementCreated());

    AsyncPoolStats stats = tracker.getStats();
    Assert.assertEquals(stats.getTotalDestroyErrors(), DESTROY_ERROR_INCREMENTS);
    Assert.assertEquals(stats.getTotalDestroyed(), DESTROY_INCREMENTS);
    Assert.assertEquals(stats.getTotalTimedOut(), TIMEOUT_INCREMENTS);
    Assert.assertEquals(stats.getTotalCreateErrors(), CREATE_ERROR_INCREMENTS);
    Assert.assertEquals(stats.getTotalBadDestroyed(), BAD_DESTROY_INCREMENTS);
    Assert.assertEquals(stats.getTotalCreated(), CREATED_INCREMENTS);
    Assert.assertEquals(stats.getCheckedOut(), CHECKED_OUT);
    Assert.assertEquals(stats.getPoolSize(), POOL_SIZE);
  }

  /**
   * Tests sampled values are the same when #getStats() are called within the same
   * sampling period. Also tests the samplers are correctly updated when #getStats
   * are called in successive sampling periods.
   */
  @Test
  public void testMinimumSamplingPeriod()
  {
    SettableClock clock = new SettableClock();
    AsyncPoolStatsTracker tracker = new AsyncPoolStatsTracker(
        () -> LIFECYCLE_STATS,
        () -> MAX_SIZE,
        () -> MIN_SIZE,
        () -> _poolSize,
        () -> _checkedOut,
        () -> IDLE_SIZE,
        clock,
        new LongTracking());

    // Samples the max values
    tracker.sampleMaxPoolSize();
    tracker.sampleMaxCheckedOut();
    tracker.sampleMaxWaitTime(WAIT_TIME);
    Assert.assertEquals(tracker.getStats().getSampleMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(tracker.getStats().getSampleMaxCheckedOut(), CHECKED_OUT);
    Assert.assertEquals(tracker.getStats().getSampleMaxWaitTime(), WAIT_TIME);

    // Without incrementing time we should still be getting the old sampled values
    _poolSize = POOL_SIZE + 10;
    tracker.sampleMaxPoolSize();
    _checkedOut = CHECKED_OUT + 10;
    tracker.sampleMaxCheckedOut();
    tracker.sampleMaxWaitTime(WAIT_TIME + 100);

    Assert.assertEquals(tracker.getStats().getSampleMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(tracker.getStats().getSampleMaxCheckedOut(), CHECKED_OUT);
    Assert.assertEquals(tracker.getStats().getSampleMaxWaitTime(), WAIT_TIME);

    // After incrementing time we should be getting the new sampled values
    clock.addDuration(SAMPLING_DURATION_INCREMENT);
    Assert.assertEquals(tracker.getStats().getSampleMaxPoolSize(), POOL_SIZE + 10);
    Assert.assertEquals(tracker.getStats().getSampleMaxCheckedOut(), CHECKED_OUT + 10);
    Assert.assertEquals(tracker.getStats().getSampleMaxWaitTime(), WAIT_TIME + 100);
  }

  @Test
  public void testSamplers()
  {
    SettableClock clock = new SettableClock();
    AsyncPoolStatsTracker tracker = new AsyncPoolStatsTracker(
        () -> LIFECYCLE_STATS,
        () -> MAX_SIZE,
        () -> MIN_SIZE,
        () -> _poolSize,
        () -> _checkedOut,
        () -> IDLE_SIZE,
        clock,
        new LongTracking());

    // Samples the max values
    tracker.sampleMaxPoolSize();
    tracker.sampleMaxCheckedOut();
    Assert.assertEquals(tracker.getStats().getSampleMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(tracker.getStats().getSampleMaxCheckedOut(), CHECKED_OUT);

    // Samples at smaller values compared the old samples
    _poolSize = POOL_SIZE - 10;
    _checkedOut = CHECKED_OUT - 10;
    tracker.sampleMaxPoolSize();
    tracker.sampleMaxCheckedOut();

    clock.addDuration(SAMPLING_DURATION_INCREMENT);
    Assert.assertEquals(tracker.getStats().getSampleMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(tracker.getStats().getSampleMaxCheckedOut(), CHECKED_OUT);

    // Samples the max pool size at POOL_SIZE + 10
    _poolSize = POOL_SIZE + 10;
    _checkedOut = CHECKED_OUT + 10;
    tracker.sampleMaxPoolSize();
    tracker.sampleMaxCheckedOut();

    clock.addDuration(SAMPLING_DURATION_INCREMENT);
    Assert.assertEquals(tracker.getStats().getSampleMaxCheckedOut(), CHECKED_OUT + 10);
    Assert.assertEquals(tracker.getStats().getSampleMaxPoolSize(), POOL_SIZE + 10);
  }

  @Test
  public void testSuppliers()
  {
    AsyncPoolStatsTracker tracker = new AsyncPoolStatsTracker(
        () -> LIFECYCLE_STATS,
        () -> MAX_SIZE,
        () -> MIN_SIZE,
        () -> _poolSize,
        () -> _checkedOut,
        () -> IDLE_SIZE,
        CLOCK,
        new LongTracking());

    for (int i = 0; i < 10; i++)
    {
      _poolSize++;
      _checkedOut++;
      Assert.assertEquals(tracker.getStats().getPoolSize(), _poolSize);
      Assert.assertEquals(tracker.getStats().getCheckedOut(), _checkedOut);
    }
  }
}
