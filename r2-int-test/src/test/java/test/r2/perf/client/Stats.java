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

package test.r2.perf.client;

import com.linkedin.common.stats.LongStats;
import com.linkedin.common.stats.LongTracking;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class Stats
{
  private final long _startTime;
  private final AtomicLong _sent = new AtomicLong();
  private final AtomicLong _success = new AtomicLong();
  private final AtomicLong _error = new AtomicLong();
  private volatile Exception _lastError = null;

  private final LongTracking _latencyTracker = new LongTracking();

  public Stats(long startTime)
  {
    _startTime = startTime;
  }

  public void sent()
  {
    _sent.incrementAndGet();
  }
  public void success(long elapsedTime)
  {
    _success.incrementAndGet();
    synchronized (_latencyTracker)
    {
      _latencyTracker.addValue(elapsedTime);
    }
  }

  public void error(Exception error)
  {
    _error.incrementAndGet();
    _lastError = error;
  }

  public long getElapsedTime()
  {
    return System.currentTimeMillis() - _startTime;
  }

  public long getSentCount()
  {
    return _sent.get();
  }

  public long getSuccessCount()
  {
    return _success.get();
  }

  public long getErrorCount()
  {
    return _error.get();
  }

  public Exception getLastError()
  {
    return _lastError;
  }

  public LongStats getLatencyStats()
  {
    synchronized (_latencyTracker)
    {
      return _latencyTracker.getStats();
    }
  }
}
