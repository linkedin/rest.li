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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;


/**
 * Wrapper for {@link BackupRequestsStrategy} that keeps track of statistics and exposes some of them through
 * {@link BackupRequestsStrategyStatsProvider} interface.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 *
 */
public class TrackingBackupRequestsStrategy implements BackupRequestsStrategy, BackupRequestsStrategyStatsProvider
{

  private final BackupRequestsStrategy _delegate;

  private final LongAdder _totalAllowedCount = new LongAdder();
  private final LongAdder _totalSuccessCount = new LongAdder();

  private final AtomicReference<DelayStats> _lastDelayStats = new AtomicReference<>();

  private final AtomicReference<BackupRequestsStrategyStats> _snapshotStats = new AtomicReference<>();
  private final AtomicReference<DelayStats> _snapshotDelayStats = new AtomicReference<>();

  private final LatencyMetric _latencyWithBackup = new LatencyMetric();
  private final LatencyMetric _latencyWithoutBackup = new LatencyMetric();

  public TrackingBackupRequestsStrategy(BackupRequestsStrategy delegate)
  {
    _delegate = delegate;
  }

  @Override
  public Optional<Long> getTimeUntilBackupRequestNano()
  {
    final Optional<Long> delay = _delegate.getTimeUntilBackupRequestNano();
    delay.ifPresent(this::recordDelay);
    return delay;
  }

  private void recordDelay(long delay)
  {
    while (true)
    {
      DelayStats prev = _lastDelayStats.get();
      DelayStats next = (prev == null) ? DelayStats.create(delay) : prev.recordDelay(delay);
      if (_lastDelayStats.compareAndSet(prev, next))
      {
        break;
      }
    }
  }

  @Override
  public void recordCompletion(long responseTime)
  {
    _delegate.recordCompletion(responseTime);
  }

  @Override
  public boolean isBackupRequestAllowed()
  {
    final boolean allowed = _delegate.isBackupRequestAllowed();
    if (allowed)
    {
      _totalAllowedCount.increment();;
    }
    return allowed;
  }

  public void backupRequestSuccess()
  {
    _totalSuccessCount.increment();
  }

  @Override
  public BackupRequestsStrategyStats getStats()
  {
    return getStats(_lastDelayStats.get());
  }

  private BackupRequestsStrategyStats getStats(DelayStats delayStats)
  {
    if (delayStats == null)
    {
      return new BackupRequestsStrategyStats(_totalAllowedCount.sum(), _totalSuccessCount.sum(), 0, 0, 0);
    } else
    {
      return new BackupRequestsStrategyStats(_totalAllowedCount.sum(), _totalSuccessCount.sum(),
          delayStats._totalDelayMin, delayStats._totalDelayMax,
          delayStats._totalDelaySum / delayStats._totalDelayCount);
    }
  }

  @Override
  public BackupRequestsStrategyStats getDiffStats()
  {
    BackupRequestsStrategyStats stats = doGetDiffStats();
    while (stats == null)
    {
      stats = doGetDiffStats();
    }
    return stats;
  }

  private BackupRequestsStrategyStats doGetDiffStats()
  {
    final BackupRequestsStrategyStats snapshotStats = _snapshotStats.get();
    if (snapshotStats == null)
    {
      final DelayStats lastDelayStats = _lastDelayStats.get();
      if (lastDelayStats != null)
      {
        if (!_lastDelayStats.compareAndSet(lastDelayStats, lastDelayStats.resetNonTotal()))
        {
          return null;
        }
        _snapshotDelayStats.set(_lastDelayStats.get());
      }
      BackupRequestsStrategyStats stats = getStats(_snapshotDelayStats.get());
      if (_snapshotStats.compareAndSet(null, stats))
      {
        return stats;
      } else
      {
        return doGetDiffStats();
      }
    } else
    {
      return getDiffStats(snapshotStats);
    }
  }

  private BackupRequestsStrategyStats getDiffStats(final BackupRequestsStrategyStats snapshotStats)
  {
    while (true)
    {
      final DelayStats lastDelayStats = _lastDelayStats.get();
      if (lastDelayStats == null)
      {
        //no delay stats
        if (_snapshotStats.compareAndSet(snapshotStats,
            new BackupRequestsStrategyStats(_totalAllowedCount.sum(), _totalSuccessCount.sum(), 0, 0, 0)))
        {
          return new BackupRequestsStrategyStats(_totalAllowedCount.sum() - snapshotStats.getAllowed(),
              _totalSuccessCount.sum() - snapshotStats.getSuccessful(), 0, 0, 0);
        } else
        {
          return null;
        }
      } else
      {
        if (_lastDelayStats.compareAndSet(lastDelayStats, lastDelayStats.resetNonTotal()))
        {
          final DelayStats snapshotDelayStats = _snapshotDelayStats.get();
          if (_snapshotDelayStats.compareAndSet(snapshotDelayStats, lastDelayStats))
          {
            if (snapshotDelayStats == null)
            {
              //we just created first snapshot of delay stats
              if (_snapshotStats.compareAndSet(snapshotStats,
                  new BackupRequestsStrategyStats(_totalAllowedCount.sum(), _totalSuccessCount.sum(),
                      lastDelayStats._totalDelayMin, lastDelayStats._totalDelayMax,
                      lastDelayStats._totalDelaySum / lastDelayStats._totalDelayCount)))
              {
                return new BackupRequestsStrategyStats(_totalAllowedCount.sum() - snapshotStats.getAllowed(),
                    _totalSuccessCount.sum() - snapshotStats.getSuccessful(), lastDelayStats._totalDelayMin,
                    lastDelayStats._totalDelayMax, lastDelayStats._totalDelaySum / lastDelayStats._totalDelayCount);
              } else
              {
                return null;
              }
            } else
            {
              return getDiffStats(snapshotStats, snapshotDelayStats, lastDelayStats);
            }
          } // else loop
        }
      }
    }
  }

  private BackupRequestsStrategyStats getDiffStats(BackupRequestsStrategyStats snapshotStats,
      DelayStats snapshotDelayStats, DelayStats lastDelayStats)
  {
    final long count = lastDelayStats._totalDelayCount - snapshotDelayStats._totalDelayCount;
    if (count <= 0)
    {
      // no change in delay stats or overflow
      if (_snapshotStats.compareAndSet(snapshotStats,
          new BackupRequestsStrategyStats(_totalAllowedCount.sum(), _totalSuccessCount.sum(), 0, 0, 0)))
      {
        return new BackupRequestsStrategyStats(_totalAllowedCount.sum() - snapshotStats.getAllowed(),
            _totalSuccessCount.sum() - snapshotStats.getSuccessful(), 0, 0, 0);

      } else
      {
        return null;
      }
    } else
    {
      if (_snapshotStats.compareAndSet(snapshotStats,
          new BackupRequestsStrategyStats(_totalAllowedCount.sum(), _totalSuccessCount.sum(), lastDelayStats._delayMin,
              lastDelayStats._delayMax, (lastDelayStats._totalDelaySum - snapshotDelayStats._totalDelaySum) / count)))
      {
        return new BackupRequestsStrategyStats(_totalAllowedCount.sum() - snapshotStats.getAllowed(),
            _totalSuccessCount.sum() - snapshotStats.getSuccessful(), lastDelayStats._delayMin,
            lastDelayStats._delayMax, (lastDelayStats._totalDelaySum - snapshotDelayStats._totalDelaySum) / count);
      } else
      {
        return null;
      }
    }
  }

  @Override
  public String toString()
  {
    return "TrackingBackupRequestsStrategy [delegate=" + _delegate + ", totalAllowedCount=" + _totalAllowedCount
        + ", totalSuccessCount=" + _totalSuccessCount + ", lastDelayStats=" + _lastDelayStats + ", snapshotStats="
        + _snapshotStats + ", snapshotDelayStats=" + _snapshotDelayStats + "]";
  }

  static class DelayStats
  {

    private final long _totalDelayCount;
    private final long _totalDelaySum;
    private final long _totalDelayMax;
    private final long _totalDelayMin;
    private final long _delayMax;
    private final long _delayMin;

    DelayStats(long totalDelayCount, long totalDelaySum, long totalDelayMax, long totalDelayMin, long delayMax,
        long delayMin)
    {
      _totalDelayCount = totalDelayCount;
      _totalDelaySum = totalDelaySum;
      _totalDelayMax = totalDelayMax;
      _totalDelayMin = totalDelayMin;
      _delayMax = delayMax;
      _delayMin = delayMin;
    }

    DelayStats recordDelay(final long delay)
    {
      if (_totalDelaySum + delay > 0)
      {
        return new DelayStats(_totalDelayCount + 1, _totalDelaySum + delay, Math.max(delay, _totalDelayMax),
            Math.min(delay, _totalDelayMin), Math.max(delay, _delayMax), Math.min(delay, _delayMin));
      } else
      {
        return create(delay);
      }
    }

    DelayStats resetNonTotal()
    {
      return new DelayStats(_totalDelayCount, _totalDelaySum, _totalDelayMax, _totalDelayMin, Long.MIN_VALUE,
          Long.MAX_VALUE);
    }

    static DelayStats create(final long delay)
    {
      return new DelayStats(1, delay, delay, delay, delay, delay);
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (_delayMax ^ (_delayMax >>> 32));
      result = prime * result + (int) (_delayMin ^ (_delayMin >>> 32));
      result = prime * result + (int) (_totalDelayCount ^ (_totalDelayCount >>> 32));
      result = prime * result + (int) (_totalDelayMax ^ (_totalDelayMax >>> 32));
      result = prime * result + (int) (_totalDelayMin ^ (_totalDelayMin >>> 32));
      result = prime * result + (int) (_totalDelaySum ^ (_totalDelaySum >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      DelayStats other = (DelayStats) obj;
      if (_delayMax != other._delayMax)
        return false;
      if (_delayMin != other._delayMin)
        return false;
      if (_totalDelayCount != other._totalDelayCount)
        return false;
      if (_totalDelayMax != other._totalDelayMax)
        return false;
      if (_totalDelayMin != other._totalDelayMin)
        return false;
      if (_totalDelaySum != other._totalDelaySum)
        return false;
      return true;
    }

    @Override
    public String toString()
    {
      return "DelayStats [totalDelayCount=" + _totalDelayCount + ", totalDelaySum=" + _totalDelaySum
          + ", totalDelayMin=" + _totalDelayMin + ", totalDelayMax=" + _totalDelayMax + ", delayMin=" + _delayMin
          + ", delayMax=" + _delayMax + "]";
    }

  }

  public LatencyMetric getLatencyWithBackup() {
    return _latencyWithBackup;
  }

  public LatencyMetric getLatencyWithoutBackup() {
    return _latencyWithoutBackup;
  }

}
