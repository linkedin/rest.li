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

import com.linkedin.r2.transport.http.client.AsyncPoolLifecycleStats;
import com.linkedin.r2.transport.http.client.AsyncPoolStats;
import com.linkedin.r2.transport.http.client.AsyncPoolStatsTracker;
import com.linkedin.r2.transport.http.client.PoolStats;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 * @version $Revision: $
 */
public class TestAsyncPoolStatsTracker
{
  private static final PoolStats.LifecycleStats LIFECYCLE_STATS = new AsyncPoolLifecycleStats(0, 0, 0, 0);

  private static final int MAX_SIZE = Integer.MAX_VALUE;
  private static final int MIN_SIZE = 0;
  private static final int IDLE_SIZE = 100;
  private static final int WAITERS_SIZE = 150;
  private static final int POOL_SIZE = 200;
  private static final int CHECKED_OUT = 300;

  private static final int DESTROY_ERROR_INCREMENTS = 10;
  private static final int DESTROY_INCREMENTS = 20;
  private static final int TIMEOUT_INCREMENTS = 30;
  private static final int CREATE_ERROR_INCREMENTS = 40;
  private static final int BAD_DESTROY_INCREMENTS = 50;
  private static final int CREATED_INCREMENTS = 60;

  private int _poolSize = POOL_SIZE;
  private int _checkedOut = CHECKED_OUT;

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
        () -> WAITERS_SIZE);

    AsyncPoolStats stats = tracker.getStats();
    Assert.assertSame(stats.getLifecycleStats(), LIFECYCLE_STATS);
    Assert.assertEquals(stats.getMaxPoolSize(), MAX_SIZE);
    Assert.assertEquals(stats.getMinPoolSize(), MIN_SIZE);
    Assert.assertEquals(stats.getIdleCount(), IDLE_SIZE);
    Assert.assertEquals(stats.getWaitersCount(), WAITERS_SIZE);
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
        () -> WAITERS_SIZE);

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

  @Test
  public void testSamplers()
  {
    AsyncPoolStatsTracker tracker = new AsyncPoolStatsTracker(
        () -> LIFECYCLE_STATS,
        () -> MAX_SIZE,
        () -> MIN_SIZE,
        () -> _poolSize,
        () -> _checkedOut,
        () -> IDLE_SIZE,
        () -> WAITERS_SIZE);

    // Samples the max pool size at POOL_SIZE
    tracker.sampleMaxPoolSize();

    // Samples the max pool size at POOL_SIZE - 10
    _poolSize = POOL_SIZE - 10;
    tracker.sampleMaxPoolSize();
    Assert.assertEquals(tracker.getStats().getSampleMaxPoolSize(), POOL_SIZE);

    // Samples the max pool size at POOL_SIZE + 10
    _poolSize = POOL_SIZE + 10;
    tracker.sampleMaxPoolSize();
    Assert.assertEquals(tracker.getStats().getSampleMaxPoolSize(), POOL_SIZE + 10);

    // Samples the max checked out at CHECKED_OUT
    tracker.sampleMaxCheckedOut();

    _checkedOut = CHECKED_OUT - 10;
    tracker.sampleMaxCheckedOut();
    Assert.assertEquals(tracker.getStats().getSampleMaxCheckedOut(), CHECKED_OUT);

    _checkedOut = CHECKED_OUT + 10;
    tracker.sampleMaxCheckedOut();
    Assert.assertEquals(tracker.getStats().getSampleMaxCheckedOut(), CHECKED_OUT + 10);
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
        () -> WAITERS_SIZE);

    for (int i = 0; i < 10; i++)
    {
      _poolSize++;
      _checkedOut++;
      Assert.assertEquals(tracker.getStats().getPoolSize(), _poolSize);
      Assert.assertEquals(tracker.getStats().getCheckedOut(), _checkedOut);
    }
  }
}
