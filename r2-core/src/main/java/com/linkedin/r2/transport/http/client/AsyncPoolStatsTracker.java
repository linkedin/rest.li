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

package com.linkedin.r2.transport.http.client;

import com.linkedin.common.stats.LongStats;
import com.linkedin.common.stats.LongTracker;
import com.linkedin.common.stats.LongTracking;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.clock.Time;
import java.util.function.Supplier;


/**
 * Tracks statistics from a {@link com.linkedin.r2.transport.http.client.AsyncPool} and produces
 * a {@link com.linkedin.r2.transport.http.client.AsyncPoolStats} upon request. The implementation
 * itself is not thread safe. Use external synchronization if needed.
 *
 * @author Sean Sheng
 * @version $Revision: $
 */
public class AsyncPoolStatsTracker
{
  /**
   * The default minimum sampling period. Calls to getStats() within the same sample period will
   * obtain the same sampled results. A minimum is enforced to ensure a reasonable result. The
   * alternative is to enforce a minimum sample size. We chose a time based solution since many
   * monitoring systems are also time based.
   */
  private static final long MINIMUM_SAMPLING_PERIOD = Time.minutes(1L);

  /**
   * These are total counts over the entire lifetime of the pool
   */
  private int _totalCreated = 0;
  private int _totalDestroyed = 0;
  private int _totalCreateErrors = 0;
  private int _totalDestroyErrors = 0;
  private int _totalBadDestroyed = 0;
  private int _totalTimedOut = 0;
  private int _totalWaiterTimedOut = 0;
  private int _totalCreationIgnored = 0;


  /**
   * These counters are sampled and reset based on sampling rules
   */
  private int _sampleMaxCheckedOut = 0;
  private int _sampleMaxPoolSize = 0;
  private long _sampleMaxWaitTime = 0;
  private int _currentMaxCheckedOut = 0;
  private int _currentMaxPoolSize = 0;
  private long _currentMaxWaitTime = 0;

  private final Supplier<PoolStats.LifecycleStats> _lifecycleStatsSupplier;
  private final Supplier<Integer> _maxSizeSupplier;
  private final Supplier<Integer> _minSizeSupplier;
  private final Supplier<Integer> _poolSizeSupplier;
  private final Supplier<Integer> _checkedOutSupplier;
  private final Supplier<Integer> _idleSizeSupplier;
  private final LongTracker _waitTimeTracker;

  private final Clock _clock;
  private long _lastSamplingTime = 0L;

  @Deprecated
  public AsyncPoolStatsTracker(
      Supplier<PoolStats.LifecycleStats> lifecycleStatsSupplier,
      Supplier<Integer> maxSizeSupplier,
      Supplier<Integer> minSizeSupplier,
      Supplier<Integer> poolSizeSupplier,
      Supplier<Integer> checkedOutSupplier,
      Supplier<Integer> idleSizeSupplier)
  {
    this(lifecycleStatsSupplier,
        maxSizeSupplier,
        minSizeSupplier,
        poolSizeSupplier,
        checkedOutSupplier,
        idleSizeSupplier,
        SystemClock.instance(),
        new LongTracking());
  }

  public AsyncPoolStatsTracker(
      Supplier<PoolStats.LifecycleStats> lifecycleStatsSupplier,
      Supplier<Integer> maxSizeSupplier,
      Supplier<Integer> minSizeSupplier,
      Supplier<Integer> poolSizeSupplier,
      Supplier<Integer> checkedOutSupplier,
      Supplier<Integer> idleSizeSupplier,
      Clock clock,
      LongTracker waitTimeTracker)
  {
    _lifecycleStatsSupplier = lifecycleStatsSupplier;
    _maxSizeSupplier = maxSizeSupplier;
    _minSizeSupplier = minSizeSupplier;
    _poolSizeSupplier = poolSizeSupplier;
    _checkedOutSupplier = checkedOutSupplier;
    _idleSizeSupplier = idleSizeSupplier;
    _clock = clock;
    _waitTimeTracker = waitTimeTracker;
  }

  public void incrementCreated()
  {
    _totalCreated++;
  }

  public void incrementIgnoredCreation()
  {
    _totalCreationIgnored++;
  }

  public void incrementDestroyed()
  {
    _totalDestroyed++;
  }

  public void incrementCreateErrors()
  {
    _totalCreateErrors++;
  }

  public void incrementDestroyErrors()
  {
    _totalDestroyErrors++;
  }

  public void incrementBadDestroyed()
  {
    _totalBadDestroyed++;
  }

  public void incrementTimedOut()
  {
    _totalTimedOut++;
  }

  public void incrementWaiterTimedOut()
  {
    _totalWaiterTimedOut++;
  }

  public void sampleMaxPoolSize()
  {
    _currentMaxPoolSize = Math.max(_poolSizeSupplier.get(), _currentMaxPoolSize);
  }

  public void sampleMaxCheckedOut()
  {
    _currentMaxCheckedOut = Math.max(_checkedOutSupplier.get(), _currentMaxCheckedOut);
  }

  public void sampleMaxWaitTime(long waitTimeMillis)
  {
    _currentMaxWaitTime = Math.max(waitTimeMillis, _currentMaxWaitTime);
  }

  public void trackWaitTime(long waitTimeMillis)
  {
    _waitTimeTracker.addValue(waitTimeMillis);
  }

  public AsyncPoolStats getStats()
  {
    long now = _clock.currentTimeMillis();
    if (now - _lastSamplingTime > MINIMUM_SAMPLING_PERIOD)
    {
      _sampleMaxCheckedOut = _currentMaxCheckedOut;
      _sampleMaxPoolSize = _currentMaxPoolSize;
      _sampleMaxWaitTime = _currentMaxWaitTime;

      _currentMaxCheckedOut = _checkedOutSupplier.get();
      _currentMaxPoolSize = _poolSizeSupplier.get();
      _currentMaxWaitTime = 0L;

      _lastSamplingTime = now;
    }

    LongStats waitTimeStats = _waitTimeTracker.getStats();
    AsyncPoolStats stats = new AsyncPoolStats(
        _totalCreated,
        _totalDestroyed,
        _totalCreateErrors,
        _totalDestroyErrors,
        _totalBadDestroyed,
        _totalTimedOut,
        _totalWaiterTimedOut,
        _totalCreationIgnored,
        _checkedOutSupplier.get(),
        _maxSizeSupplier.get(),
        _minSizeSupplier.get(),
        _poolSizeSupplier.get(),
        _sampleMaxCheckedOut,
        _sampleMaxPoolSize,
        _sampleMaxWaitTime,
        _idleSizeSupplier.get(),
        waitTimeStats.getAverage(),
        waitTimeStats.get50Pct(),
        waitTimeStats.get95Pct(),
        waitTimeStats.get99Pct(),
        _lifecycleStatsSupplier.get()
    );

    _waitTimeTracker.reset();
    return stats;
  }
}

