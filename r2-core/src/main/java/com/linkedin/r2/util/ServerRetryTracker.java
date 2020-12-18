/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.r2.util;

import com.linkedin.util.clock.Clock;
import java.util.LinkedList;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Stores the number of requests categorized by number of retry attempts. It uses the information to estimate
 * a ratio of how many requests are being retried in the cluster. The ratio is then compared with
 * {@link ServerRetryTracker#_maxRequestRetryRatio} to make a decision on whether or not to retry in the
 * next interval. When calculating the ratio, it looks at the last {@link ServerRetryTracker#_aggregatedIntervalNum}
 * intervals by aggregating the recorded requests.
 */
public class ServerRetryTracker
{
  private static final Logger LOG = LoggerFactory.getLogger(ServerRetryTracker.class);
  private final int _retryLimit;
  private final int _aggregatedIntervalNum;
  private final double _maxRequestRetryRatio;
  private final long _updateIntervalMs;
  private final Clock _clock;

  private final Object _counterLock = new Object();
  private final Object _updateLock = new Object();

  @GuardedBy("_updateLock")
  private volatile long _lastRollOverTime;
  private boolean _isBelowRetryRatio;

  @GuardedBy("_counterLock")
  private final LinkedList<int[]> _retryAttemptsCounter;
  private final int[] _aggregatedRetryAttemptsCounter;

  public ServerRetryTracker(int retryLimit, int aggregatedIntervalNum, double maxRequestRetryRatio, long updateIntervalMs, Clock clock)
  {
    _retryLimit = retryLimit;
    _aggregatedIntervalNum = aggregatedIntervalNum;
    _maxRequestRetryRatio = maxRequestRetryRatio;
    _updateIntervalMs = updateIntervalMs;
    _clock = clock;

    _lastRollOverTime = clock.currentTimeMillis();
    _isBelowRetryRatio = true;

    _aggregatedRetryAttemptsCounter = new int[_retryLimit + 1];
    _retryAttemptsCounter = new LinkedList<>();
    _retryAttemptsCounter.add(new int[_retryLimit + 1]);
  }

  public void add(int numberOfRetryAttempts)
  {
    if (numberOfRetryAttempts > _retryLimit)
    {
      LOG.warn("Unexpected number of retry attempts: " + numberOfRetryAttempts + ", current retry limit: " + _retryLimit);
      numberOfRetryAttempts = _retryLimit;
    }

    synchronized (_counterLock)
    {
      _retryAttemptsCounter.getLast()[numberOfRetryAttempts] += 1;
    }
    updateRetryDecision();
  }

  public boolean isBelowRetryRatio()
  {
    return _isBelowRetryRatio;
  }

  private void rollOverStats()
  {
    // rollover the current interval to the aggregated counter
    synchronized (_counterLock)
    {
      int[] intervalToAggregate = _retryAttemptsCounter.getLast();
      for (int i = 0; i <= _retryLimit; i++)
      {
        _aggregatedRetryAttemptsCounter[i] += intervalToAggregate[i];
      }

      if (_retryAttemptsCounter.size() > _aggregatedIntervalNum)
      {
        // discard the oldest interval
        int[] intervalToDiscard = _retryAttemptsCounter.removeFirst();
        for (int i = 0; i <= _retryLimit; i++)
        {
          _aggregatedRetryAttemptsCounter[i] -= intervalToDiscard[i];
        }
      }

      // append a new interval
      _retryAttemptsCounter.addLast(new int[_retryLimit + 1]);
    }
  }

  void updateRetryDecision()
  {
    long currentTime = _clock.currentTimeMillis();

    synchronized (_updateLock)
    {
      // Check if the current interval is stale
      if (currentTime >= _lastRollOverTime + _updateIntervalMs)
      {
        // Rollover stale intervals until the current interval is reached
        for (long time = currentTime; time >= _lastRollOverTime + _updateIntervalMs; time -= _updateIntervalMs)
        {
          rollOverStats();
        }

        _isBelowRetryRatio = getRetryRatio() <= _maxRequestRetryRatio;
        _lastRollOverTime = currentTime;
      }
    }
  }

  double getRetryRatio()
  {
    double retryRatioSum = 0.0;
    int i;

    for (i = 1; i <= _retryLimit; i++)
    {
      if (_aggregatedRetryAttemptsCounter[i] == 0 || _aggregatedRetryAttemptsCounter[i - 1] == 0)
      {
        break;
      }
      double ratio = (double) _aggregatedRetryAttemptsCounter[i] / _aggregatedRetryAttemptsCounter[i - 1];

      // We put more weights to the retry requests with larger number of attempts
      double adjustedRatio = Double.min(ratio * i, 1.0);
      retryRatioSum += adjustedRatio;
    }

    return i > 1 ? retryRatioSum / (i - 1) : 0.0;
  }
}
