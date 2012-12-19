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
 * $Id: CallTrackerImpl.java 151859 2010-11-19 21:43:47Z slim $
 */
package com.linkedin.util.degrader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.linkedin.common.stats.LongStats;
import com.linkedin.common.stats.LongTracking;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;


/**
 * As the name implies CallTrackerImpl does call tracking i.e. counts, error, latency, concurrency, etc.
 * There are 4 classes that is involved in call tracking.
 * 1.) CallTrackerImpl counts the total events, be it total call counts, total error count, etc.
 * 2.) CallTrackerImpl.Tracker counts the events happening in one interval and publish an event to its listener
 * periodically.
 * 3.) CallTrackerImpl.CallTrackerStats is the actual data that is being moved around. You can think of
 * CallTrackerStats as an immutable DTO.
 * 4.) LongTracking is used in CallTrackerImpl.Tracker to calculate the statistics of the call.
 *
 * @author Dave Messink
 * @author Chris Pettitt
 * @author Swee Lim
 * @version $Rev: 151859 $
 */

public class CallTrackerImpl implements CallTracker
{
  private static final Clock DEFAULT_CLOCK = SystemClock.instance();

  private final Object _lock = new Object();

  private final Clock _clock;
  private final long _interval;

  private final Tracker _tracker;

  private volatile long _lastStartTime;
  private volatile long _lastResetTime;
  private long _callCountTotal;
  private long _callStartCountTotal;
  private long _errorCountTotal;
  private int _concurrency;
  private long _sumOfOutstandingStartTimes;
  //Total counts of specific types of error like RemoteInvocation error, 400 errors, 500 errors
  private Map<String, Integer> _totalErrorCountsMap;

  private Pending _pending = null;

  // This CallTrackerListener list is immutable and copy-on-write.
  private volatile List<StatsRolloverEventListener> _listeners = new ArrayList<StatsRolloverEventListener>();

  public CallTrackerImpl(long interval)
  {
    this(interval, DEFAULT_CLOCK);
  }

  public CallTrackerImpl(long interval, Clock clock)
  {
    _clock = clock;
    _interval = interval;
    _lastStartTime = -1;
    _lastResetTime = _clock.currentTimeMillis();
    _totalErrorCountsMap = new HashMap<String, Integer>();
    /* create trackers for each resolution */
    _tracker = new Tracker();
  }

  @Override
  public CallCompletion startCall()
  {
    long currentTime;
    Pending pending;
    synchronized (_lock)
    {
      currentTime = _clock.currentTimeMillis();
      _tracker.getStatsWithCurrentTime(currentTime);
      _callStartCountTotal++;
      _tracker._callStartCount++;
      _concurrency++;
      if (_concurrency > _tracker._concurrentMax)
      {
        _tracker._concurrentMax = _concurrency;
      }
      _lastStartTime = currentTime;
      _sumOfOutstandingStartTimes += currentTime;
      pending = checkForPending();
    }
    if (pending != null)
    {
      pending.deliver();
    }
    return new CallCompletionImpl(currentTime);
  }

  @Override
  public CallStats getCallStats()
  {
    return getStatsWithCurrentTime(_clock.currentTimeMillis());
  }

  private CallStats getStatsWithCurrentTime(long currentTimeMillis)
  {
    CallStats stats;
    stats = _tracker.getMostRecentStats();
    if (stats.stale(currentTimeMillis))
    {
      Pending pending;
      synchronized (_lock)
      {
        stats = _tracker.getStatsWithCurrentTime(currentTimeMillis);
        pending = checkForPending();
      }
      // Always deliver events without holding _lock to avoid deadlocks.
      if (pending != null)
      {
        pending.deliver();
      }
    }
    return stats;
  }

  @Override
  public long getInterval()
  {
    return _interval;
  }

  @Override
  public void addStatsRolloverEventListener(StatsRolloverEventListener listener)
  {
    synchronized (_lock)
    {
      // Since addListener and removeListener should be rare
      // compared to read access to deliver events
      // which cannot be be done while holding _lock,
      // copy-on-write is implemented for _listeners.
      List<StatsRolloverEventListener> copy = new ArrayList<StatsRolloverEventListener>(_listeners);
      copy.add(listener);
      _listeners = Collections.unmodifiableList(copy);
    }
  }

  @Override
  public boolean removeStatsRolloverEventListener(StatsRolloverEventListener listener)
  {
    boolean removed = false;
    synchronized (_lock)
    {
      // Since addListener and removeListener should be rare
      // compared to read access to deliver events
      // which cannot be be done while holding _lock,
      // copy-on-write is implemented for _listeners.
      if (_listeners.contains(listener))
      {
        List<StatsRolloverEventListener> copy = new ArrayList<StatsRolloverEventListener>(_listeners);
        removed = copy.remove(listener);
        _listeners = Collections.unmodifiableList(copy);
      }
    }
    return removed;
  }

  @Override
  public long getCurrentCallCountTotal()
  {
    return _callCountTotal;
  }

  public Map<String, Integer> getTotalErrorCountsMap()
  {
    return _totalErrorCountsMap;
  }

  @Override
  public long getCurrentCallStartCountTotal()
  {
    return _callStartCountTotal;
  }

  @Override
  public long getCurrentErrorCountTotal()
  {
    return _errorCountTotal;
  }

  @Override
  public int getCurrentConcurrency()
  {
    return _concurrency;
  }

  @Override
  public long getTimeSinceLastCallStart()
  {
    long lastStartTime = _lastStartTime;
    return lastStartTime == -1 ? -1 : _clock.currentTimeMillis() - lastStartTime;
  }

  @Override
  public void reset()
  {
    Pending pending;
    synchronized (_lock)
    {
      _lastStartTime = -1;
      _lastResetTime = _clock.currentTimeMillis();
      _callCountTotal = 0;
      _callStartCountTotal = 0;
      _errorCountTotal = 0;
      _tracker.reset();
      _totalErrorCountsMap.clear();
      pending = checkForPending();
    }
    // Always deliver pending events without holding _lock to avoid deadlocks.
    if (pending != null)
    {
      pending.deliver();
    }
  }

  /**
   * Add a pending event that will be delivered to listeners after releasing _lock.
   *
   * @param stats is statistics associated with the event.
   * @param reset indicates whether the statistics were reset.
   */
  private void addPending(CallStats stats, boolean reset)
  {
    if (!_listeners.isEmpty())
    {
      if (_pending == null)
      {
        _pending = new Pending(_listeners);
      }
      _pending.add(stats, reset);
    }
  }

  /**
   * Check if there are pending events to be delivered.
   *
   * If there are pending events, return the object holding pending events, and
   * clear reference to this object. Returning the object allows the caller to
   * deliver the events after releasing _lock. Clearing the reference ensures
   * only this caller will deliver the pending events.
   *
   * Must be called while holding _lock.
   *
   * @return pending events
   */
  private Pending checkForPending()
  {
    if (_pending == null)
    {
      return null;
    }
    else
    {
      Pending pending = _pending;
      _pending = null;
      return pending;
    }
  }

  @Override
  public long getLastResetTime()
  {
    return _lastResetTime;
  }

  private class CallCompletionImpl implements CallCompletion
  {
    private final AtomicBoolean _done = new AtomicBoolean();
    private final long _start;

    private CallCompletionImpl(long currentTime)
    {
      _start = currentTime;
    }

    @Override
    public void endCall()
    {
      endCall(false, null);
    }

    @Override
    public void endCallWithError()
    {
      endCall(true, null);
    }

    @Override
    public void endCallWithError(Map<String, Integer> errorCounts)
    {
      endCall(true, errorCounts);
    }

    private void endCall(boolean hasError, Map<String, Integer> errorCounts)
    {
      if (_done.compareAndSet(false, true))
      {
        Pending pending;
        synchronized (_lock)
        {
          long currentTime = _clock.currentTimeMillis();
          long duration = currentTime - _start;

          if (_start >= _lastResetTime)
          {
            addCallData(duration, hasError, currentTime, errorCounts);
          }

          // Concurrency is not reset
          if (_concurrency > 0)
          {
            _concurrency--;
          }

          // Sum of outstanding start times is not reset
          _sumOfOutstandingStartTimes -= _start;
          if (_concurrency == 0 && _sumOfOutstandingStartTimes != 0)
          {
            _sumOfOutstandingStartTimes = 0;
          }
          pending = checkForPending();
        }
        // Always deliver events without holding _lock to avoid deadlocks.
        if (pending != null)
        {
          pending.deliver();
        }
      }
    }
  }

  private void addCallData(long duration, boolean hasError, long currentTime, Map<String, Integer> errorCounts)
  {
    _tracker.addNewData(currentTime, hasError, duration, errorCounts);

    // Has to be after addNewData
    if (hasError)
    {
      _errorCountTotal++;
      if (errorCounts != null)
      {
        for (Map.Entry<String,Integer> entry : errorCounts.entrySet())
        {
          Integer count = _totalErrorCountsMap.get(entry.getKey());
          if (count == null)
          {
            _totalErrorCountsMap.put(entry.getKey(), entry.getValue());
          }
          else
          {
            _totalErrorCountsMap.put(entry.getKey(), count + entry.getValue());
          }
        }
      }
    }

    _callCountTotal++;
  }

  private void trackCall(long duration, boolean hasError)
  {
    Pending pending;
    synchronized (_lock)
    {
      addCallData(duration, hasError, _clock.currentTimeMillis(), null);
      pending = checkForPending();
    }

    // Always deliver events without holding _lock to avoid deadlocks.
    if (pending != null)
    {
      pending.deliver();
    }
  }

  @Override
  public void trackCall(long duration)
  {
    trackCall(duration, false);
  }

  @Override
  public void trackCallWithError(long duration)
  {
    trackCall(duration, true);
  }

  /**
   * Tracker is used to track the statistics of calls in one interval. Notice that this class is an inner class
   * of CallTrackerImpl. This means some instance variables like totalCall refers to the outer class. This is
   * because CallTrackerImpl keeps track of total call whereas Tracker keeps track of call in one interval.
   * Tracker uses the helper class LongTracking for keeping track of statistics like percentage error rate,
   * 95 percentile, max value, etc.
   * Tracker also rollover the call stats every interval to listeners.
   */
  private class Tracker
  {
    private volatile CallStats _stats;

    private long _startTime;
    private int _callStartCount;
    private int _errorCount;
    private int _concurrentMax;
    private final LongTracking _callTimeTracking;
    //this map is used to store the number of specific errors that happened in one interval only
    private final Map<String, Integer> _errorCountsMap;

    private Tracker()
    {
      _callTimeTracking = new LongTracking();
      _errorCountsMap = new HashMap<String, Integer>();
      reset();
    }

    private void resetStats(long startTime)
    {
      _startTime = startTime;
      _callStartCount = 0;
      _errorCount = 0;
      _concurrentMax = _concurrency;
      _callTimeTracking.reset();
      _errorCountsMap.clear();
    }

    private void reset()
    {
      resetStats(_lastResetTime - _interval);
      rolloverStats(_lastResetTime, true);
    }

    /**
     * rollover the stats and inform all the listeners for the new statistics
     */
    private void rolloverStats(long endTime, boolean reset)
    {
      _stats = new CallTrackerStats(
        _interval,
        _startTime,
        endTime,
        _callCountTotal,
        _callStartCount,
        _callStartCountTotal,
        _errorCount,
        _errorCountTotal,
        _concurrentMax,
        _concurrency == 0 ? 0 : (_sumOfOutstandingStartTimes / _concurrency),
        _concurrency,
        _callTimeTracking.getStats(), _totalErrorCountsMap, _errorCountsMap);

      resetStats(endTime);

      addPending(_stats, reset);
    }

    /**
     * this method is called to track the number of calls made during this interval
     */
    private void addNewData(long currentTime, boolean hasError, long duration, Map<String, Integer> errorCounts)
    {
      getStatsWithCurrentTime(currentTime);
      _callTimeTracking.addValue(duration);
      if (hasError)
      {
        _errorCount++;
      }
      if (errorCounts != null)
      {
        for (Map.Entry<String, Integer> entry : errorCounts.entrySet())
        {
          Integer count = _errorCountsMap.get(entry.getKey());
          if (count == null)
          {
            count = 0;
          }
          count += entry.getValue();
        }
      }
    }

    private CallStats getMostRecentStats()
    {
      return _stats;
    }

    public CallStats getStatsWithCurrentTime(long currentTime)
    {
      if (_stats.stale(currentTime))
      {
        long offset = currentTime - _lastResetTime;
        long currentStartOffset = ((offset / _interval) * _interval);
        long lastEnd = _lastResetTime + currentStartOffset;
        long lastStart = lastEnd - _interval;
        if (_startTime == lastStart)
        {
          // Current interval has elapsed.
          // Emit stats and start new current interval.
          rolloverStats(lastEnd, false);
        }
        else if (_startTime < lastStart)
        {
          // Current interval is stale, emit stale accumulated stats.
          rolloverStats(_startTime + _interval, false);
          // Start new interval.
          _startTime = lastStart;
          rolloverStats(lastEnd, false);
        }
      }
      return _stats;
    }
  }

  private static class Pending
  {
    private static class PendingEvent implements StatsRolloverEvent
    {
      private final CallStats _stats;
      private final boolean _reset;

      PendingEvent(CallStats stats, boolean reset)
      {
        _stats = stats;
        _reset = reset;
      }

      @Override
      public CallStats getCallStats()
      {
        return _stats;
      }

      @Override
      public boolean isReset()
      {
        return _reset;
      }
    }

    private final List<PendingEvent> _pendingEvents;
    private final List<StatsRolloverEventListener> _listeners;

    private Pending(List<StatsRolloverEventListener> listeners)
    {
      _pendingEvents = new ArrayList<PendingEvent>(4);
      _listeners = listeners;
    }

    private void add(CallStats stats, boolean reset)
    {
      _pendingEvents.add(new PendingEvent(stats, reset));
    }

    private void deliver()
    {
      for (PendingEvent event : _pendingEvents)
      {
        for (StatsRolloverEventListener listener : _listeners)
        {
          listener.onStatsRollover(event);
        }
      }
    }
  }

  /**
   * @author Swee Lim
   * @version $Rev: 142668 $
   */
  public static class CallTrackerStats implements CallStats
  {
    private final long      _intervalConfigured;
    private final long      _intervalStartTime;
    private final long      _intervalEndTime;
    private final long      _callCountTotal;
    private final int       _callStartCount;
    private final long      _callStartCountTotal;
    private final int       _errorCount;
    private final long      _errorCountTotal;
    private final int       _concurrentMax;
    private final long      _outstandingStartTimeAvg;
    private final int       _outstandingCount;
    private final LongStats _callTimeStats;
    private final Map<String, Integer> _totalErrorCountsMap;
    private final Map<String, Integer> _currentErrorCountsMap;

    public CallTrackerStats()
    {
      this(0);
    }

    public CallTrackerStats(long intervalConfigured)
    {
      _intervalConfigured = intervalConfigured;
      _intervalStartTime = 0;
      _intervalEndTime = 0;

      _callCountTotal = 0;
      _callStartCount = 0;
      _callStartCountTotal = 0;
      _errorCount = 0;
      _errorCountTotal = 0;
      _concurrentMax = 0;

      _outstandingStartTimeAvg = 0;
      _outstandingCount = 0;

      _callTimeStats = new LongStats();
      _totalErrorCountsMap = new HashMap<String, Integer>();
      _currentErrorCountsMap = new HashMap<String, Integer>();
    }

    public CallTrackerStats(
      long intervalConfigured,
      long intervalStartTime,
      long intervalEndTime,
      long callCountTotal,
      int  callStartCount,
      long callStartCountTotal,
      int  errorCount,
      long errorCountTotal,
      int  concurrentMax,
      long outstandingStartTimeAvg,
      int  outstandingCount,
      LongStats callTimeStats,
      Map<String, Integer> totalErrorCountsMap,
      Map<String, Integer> errorCountsMap
    )
    {
      _intervalConfigured = intervalConfigured;
      _intervalStartTime = intervalStartTime;
      _intervalEndTime = intervalEndTime;

      _callCountTotal = callCountTotal;
      _callStartCount = callStartCount;
      _callStartCountTotal = callStartCountTotal;
      _errorCount = errorCount;
      _errorCountTotal = errorCountTotal;
      _concurrentMax = concurrentMax;

      _outstandingStartTimeAvg = outstandingStartTimeAvg;
      _outstandingCount = outstandingCount;

      _callTimeStats = callTimeStats;
      _totalErrorCountsMap = totalErrorCountsMap;
      _currentErrorCountsMap = errorCountsMap;
    }

    public Map<String, Integer> getTotalErrorCountsMap()
    {
      return new HashMap<String, Integer>(_totalErrorCountsMap);
    }

    public Map<String, Integer> getCurrentErrorCountsMap()
    {
      return new HashMap<String, Integer>(_currentErrorCountsMap);
    }

    @Override
    public boolean stale(long currentTimeMillis)
    {
      return (currentTimeMillis >= (_intervalEndTime + _intervalConfigured));
    }

    @Override
    public long getIntervalStartTime()
    {
      return _intervalStartTime;
    }

    @Override
    public long getIntervalEndTime()
    {
      return _intervalEndTime;
    }

    @Override
    public long getInterval()
    {
      long interval = _intervalEndTime - _intervalStartTime;
      return (interval < 0 ? 0 : interval);
    }

    @Override
    public int getCallCount()
    {
      return _callTimeStats.getCount();
    }

    @Override
    public long getCallCountTotal()
    {
      return _callCountTotal;
    }

    @Override
    public int getCallStartCount()
    {
      return _callStartCount;
    }

    @Override
    public long getCallStartCountTotal()
    {
      return _callStartCountTotal;
    }

    @Override
    public double getCallsPerSecond()
    {
      return safeDivide(getCallCount(), getInterval() / 1000.0);
    }

    @Override
    public int getErrorCount()
    {
      return _errorCount;
    }

    @Override
    public long getErrorCountTotal()
    {
      return _errorCountTotal;
    }

    @Override
    public double getErrorRate()
    {
      return safeDivide(_errorCount, getCallCount());
    }

    @Override
    public int getConcurrentMax()
    {
      return _concurrentMax;
    }

    @Override
    public long getOutstandingStartTimeAvg()
    {
      return _outstandingCount > 0 ? _intervalEndTime - _outstandingStartTimeAvg : 0;
    }

    @Override
    public int getOutstandingCount()
    {
      return _outstandingCount;
    }

    @Override
    public LongStats getCallTimeStats()
    {
      return _callTimeStats;
    }

    private static double safeDivide(double numerator, double denominator)
    {
      return denominator != 0 ? numerator / denominator : 0;
    }

    @Override
    public String toString()
    {
      LongStats callTimeStats = this.getCallTimeStats();
      return
        (
          "EndTime=" + this.getIntervalEndTime() +
          ", Interval=" + this.getInterval() +
          ", CallCount=" + this.getCallCount() +
          ", CallCountTotal=" + this.getCallCountTotal() +
              ", CallStartCountTotal=" + this.getCallStartCountTotal() +
          ", CallsPerSecond=" + this.getCallsPerSecond() +
          ", ErrorCount=" + this.getErrorCount() +
          ", ErrorCountTotal=" + this.getErrorCountTotal() +
          ", ErrorRate=" + this.getErrorRate() +
          ", ConcurrentMax=" + this.getConcurrentMax() +
          ", OutstandingStartTimeAvg=" + this.getOutstandingStartTimeAvg() +
              ", OutstandingCount=" + this.getOutstandingCount() +
          ", CallTimeAvg=" + callTimeStats.getAverage() +
              ", CallTimeStdDev=" + callTimeStats.getStandardDeviation() +
          ", CallTimeMin=" + callTimeStats.getMinimum() +
          ", CallTimeMax=" + callTimeStats.getMaximum() +
          ", CallTime50Pct=" + callTimeStats.get50Pct() +
          ", CallTime90Pct=" + callTimeStats.get90Pct() +
          ", CallTime95Pct=" + callTimeStats.get95Pct() +
          ", CallTime99Pct=" + callTimeStats.get99Pct() +
          ", TotalErrorCountsMap=" + this.getTotalErrorCountsMap() +
          ", CurrentErrorCountsMap=" + this.getCurrentErrorCountsMap()
        );
    }
  }
}
