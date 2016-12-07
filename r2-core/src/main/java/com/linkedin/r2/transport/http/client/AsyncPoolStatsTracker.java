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
import com.linkedin.common.stats.LongTracking;
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
  // These are total counts over the entire lifetime of the pool
  private int _totalCreated = 0;
  private int _totalDestroyed = 0;
  private int _totalCreateErrors = 0;
  private int _totalDestroyErrors = 0;
  private int _totalBadDestroyed = 0;
  private int _totalTimedOut = 0;

  // These counters reset on each call to getStats()
  private int _sampleMaxCheckedOut = 0;
  private int _sampleMaxPoolSize = 0;

  private final Supplier<PoolStats.LifecycleStats> _lifecycleStatsSupplier;
  private final Supplier<Integer> _maxSizeSupplier;
  private final Supplier<Integer> _minSizeSupplier;
  private final Supplier<Integer> _poolSizeSupplier;
  private final Supplier<Integer> _checkedOutSupplier;
  private final Supplier<Integer> _idleSizeSupplier;
  private final Supplier<Integer> _waitersSizeSupplier;
  private final LongTracking _waitTimeTracker;

  public AsyncPoolStatsTracker(
      Supplier<PoolStats.LifecycleStats> lifecycleStatsSupplier,
      Supplier<Integer> maxSizeSupplier,
      Supplier<Integer> minSizeSupplier,
      Supplier<Integer> poolSizeSupplier,
      Supplier<Integer> checkedOutSupplier,
      Supplier<Integer> idleSizeSupplier,
      Supplier<Integer> waitersSizeSupplier)
  {
    _lifecycleStatsSupplier = lifecycleStatsSupplier;
    _maxSizeSupplier = maxSizeSupplier;
    _minSizeSupplier = minSizeSupplier;
    _poolSizeSupplier = poolSizeSupplier;
    _checkedOutSupplier = checkedOutSupplier;
    _idleSizeSupplier = idleSizeSupplier;
    _waitersSizeSupplier = waitersSizeSupplier;
    _waitTimeTracker = new LongTracking();
  }

  public void incrementCreated()
  {
    _totalCreated++;
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

  public void sampleMaxPoolSize()
  {
    _sampleMaxPoolSize = Math.max(_poolSizeSupplier.get(), _sampleMaxPoolSize);
  }

  public void sampleMaxCheckedOut()
  {
    _sampleMaxCheckedOut = Math.max(_checkedOutSupplier.get(), _sampleMaxCheckedOut);
  }

  public void trackWaitTime(long waitTimeMillis)
  {
    _waitTimeTracker.addValue(waitTimeMillis);
  }

  public AsyncPoolStats getStats()
  {
    LongStats waitTimeStats = _waitTimeTracker.getStats();
    AsyncPoolStats stats = new AsyncPoolStats(
        _totalCreated,
        _totalDestroyed,
        _totalCreateErrors,
        _totalDestroyErrors,
        _totalBadDestroyed,
        _totalTimedOut,
        _checkedOutSupplier.get(),
        _maxSizeSupplier.get(),
        _minSizeSupplier.get(),
        _poolSizeSupplier.get(),
        _sampleMaxCheckedOut,
        _sampleMaxPoolSize,
        _idleSizeSupplier.get(),
        _waitersSizeSupplier.get(),
        waitTimeStats.getAverage(),
        waitTimeStats.get50Pct(),
        waitTimeStats.get95Pct(),
        waitTimeStats.get99Pct(),
        _lifecycleStatsSupplier.get()
    );
    _sampleMaxCheckedOut = _checkedOutSupplier.get();
    _sampleMaxPoolSize = _poolSizeSupplier.get();
    _waitTimeTracker.reset();
    return stats;
  }
}

