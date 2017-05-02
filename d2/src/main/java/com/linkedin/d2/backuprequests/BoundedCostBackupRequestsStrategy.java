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

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.IntCountsHistogram;
import org.HdrHistogram.ShortCountsHistogram;

import com.linkedin.d2.balancer.util.BurstyBarrier;


/**
 * This is an implementation of a {@link BackupRequestsStrategy} that limits cost by keeping number of backup requests
 * close to specified percent of overall number of requests.
 * <p>
 * For discussion about {@code percent} and {@code maxBurst} parameters see {@link BurstyBarrier} class.
 * <p>
 * This class is thread safe.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 *
 */
public class BoundedCostBackupRequestsStrategy implements BackupRequestsStrategy
{
  private static final int UNREASONABLE_HISTORY_LENGTH = (128 * 1024 * 1024) / 8;  // 128M occupied by a history is probably unreasonable
  static final long LOW = 1000L; //1 microsecond
  static final long HIGH = 100000000000L; //100 seconds

  private final AbstractHistogram _histogram; //needs ~38K memory if history <= 32767, ~76K memory otherwise
  private final long[] _history;

  private int _historyIdx = 0;
  private boolean _histogramReady = false;
  private final int _historyLength;
  private final int _requiredHistory;
  private final double _percentile;
  private final long _minBackupDelayNano;

  private final BurstyBarrier _costLimiter;

  private final Object _lock = new Object();

  public BoundedCostBackupRequestsStrategy(double percent, int maxBurst, int historyLength, int requiredHistory,
      int minBackupDelayMs)
  {
    if (percent <= 0 || percent >= 100)
    {
      throw new IllegalArgumentException(
          "percent parameter has to be within range: (0, 100), excluding 0 and 100, got: " + percent);
    }
    if (maxBurst <= 0)
    {
      throw new IllegalArgumentException("maxBurst parameter has to be a positive number, got: " + maxBurst);
    }
    if (historyLength <= 99 || historyLength >= UNREASONABLE_HISTORY_LENGTH)
    {
      throw new IllegalArgumentException(
          "historyLength parameter has to be within range: (100, " + (UNREASONABLE_HISTORY_LENGTH - 1) + "), got: " + historyLength);
    }
    if (requiredHistory <= 99)
    {
      throw new IllegalArgumentException(
          "requiredHistory parameter has to be a number greater than 99, got: " + requiredHistory);
    }
    if (minBackupDelayMs < 0)
    {
      throw new IllegalArgumentException(
          "minBackupDelayMs parameter must not be a negative number, got: " + minBackupDelayMs);
    }

    _historyLength = historyLength;
    if (historyLength <= Short.MAX_VALUE)
    {
      _histogram = new ShortCountsHistogram(LOW, HIGH, 3);
    } else
    {
      _histogram = new IntCountsHistogram(LOW, HIGH, 3);
    }
    _history = new long[historyLength];
    _requiredHistory = requiredHistory;
    _percentile = 100d - percent;
    _costLimiter = new BurstyBarrier(percent, maxBurst);
    _minBackupDelayNano = 1000L * 1000L * minBackupDelayMs;
  }

  @Override
  public boolean isBackupRequestAllowed()
  {
    return _costLimiter.canPassThrough();
  }

  @Override
  public Optional<Long> getTimeUntilBackupRequestNano()
  {
    _costLimiter.arrive();

    synchronized (_lock)
    {
      if (_histogramReady)
      {
        return Optional.of(Math.max(_minBackupDelayNano, _histogram.getValueAtPercentile(_percentile)));
      } else
      {
        return Optional.empty();
      }
    }
  }

  @Override
  public void recordCompletion(long duration)
  {

    //make sure that duration is within the bounds
    duration = Math.max(LOW, duration);
    duration = Math.min(HIGH, duration);

    synchronized (_lock)
    {
      _historyIdx += 1;
      if (_historyIdx == _requiredHistory)
        _histogramReady = true;
      _historyIdx %= _historyLength;

      //remove old result from the histogram
      if (_history[_historyIdx] != 0)
      {
        _histogram.recordValueWithCount(_history[_historyIdx], -1);
      }

      //update histogram with new result
      _histogram.recordValueWithCount(duration, 1);

      _history[_historyIdx] = duration;
    }
  }

  public int getHistoryLength()
  {
    return _historyLength;
  }

  public int getRequiredHistory()
  {
    return _requiredHistory;
  }

  public double getPercent()
  {
    return 100d - _percentile;
  }

  public long getMinBackupDelayNano()
  {
    return _minBackupDelayNano;
  }
}
