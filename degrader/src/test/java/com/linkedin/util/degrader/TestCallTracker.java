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
 * $Id: TestCallTracker.java 142668 2010-10-07 21:03:16Z dmessink $ */
package com.linkedin.util.degrader;

import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.Time;
import org.testng.annotations.Test;

/**
 * @author Dave Messink
 * @author Swee Lim
 * @version $Rev: 142668 $
 */

public class TestCallTracker
{
  private static final long INTERVAL = Time.minutes(1);

  private static final long FIVE_MS = Time.milliseconds(5);
  private static final long TEN_MS = Time.milliseconds(10);

  private CallTrackerImpl _callTracker;
  private long _interval = INTERVAL;
  private SettableClock _clock;

  @BeforeMethod
  protected void setUp() throws Exception
  {
    _clock = new SettableClock();
    _callTracker = new CallTrackerImpl(_interval, _clock);
  }

  @AfterMethod
  protected void tearDown() throws Exception
  {
    _callTracker = null;
  }

  @org.testng.annotations.Test public void testEmptyTracker()
  {
    long lastResetTime = _callTracker.getLastResetTime();

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(),
                        lastResetTime - INTERVAL, "Empty interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), lastResetTime,
                        "Empty interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Empty interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 0,
                        "Empty call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 0,
                        "Empty call count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 0,
                        "Empty call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 0,
                        "Empty call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallsPerSecond(), 0.0,
                        "Empty calls per second is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 0,
                        "Empty error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), 0,
                        "Empty error count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Empty error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 0,
                        "Empty max concurrent is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 0.0,
                        "Empty average call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Empty call time standard deviation is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getMinimum(), 0,
                        "Empty minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getMaximum(), 0,
                        "Empty maximum call time is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Empty 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 0,
                        "Empty 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 0,
                        "Empty 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 0,
                        "Empty 99 percentile call time is incorrect");

    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), -1,
                        "Empty time since last call is incorrect");
    Assert.assertEquals(_callTracker.getLastResetTime(), lastResetTime,
                        "Empty last reset time is incorrect");

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 0,
                        "Empty total calls is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 0,
                        "Empty total call starts is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 0,
                        "Empty total errors is incorrect");
  }

  @org.testng.annotations.Test public void testCallTracker()
  {
    long jitter = 500;
    long now = _clock.currentTimeMillis();
    long startTime = now;
    long startCallCountTotal = _callTracker.getCurrentCallCountTotal();
    long startErrorCountTotal = _callTracker.getCurrentErrorCountTotal();

    Listener listener = new Listener();
    _callTracker.addStatsRolloverEventListener(listener);

    _clock.addDuration(FIVE_MS);

    List<CallCompletion> dones = startCall(_callTracker, 3);

    _clock.addDuration(FIVE_MS);

    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), 5,
                        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 3, "Concurrency is incorrect");

    endCall(dones, 3);

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 3,
                        "Current call count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 0,
                        "Current error count total is incorrect");

    _clock.setCurrentTimeMillis(INTERVAL + startTime + jitter);
    now = _clock.currentTimeMillis();

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(), startTime,
                        "Interval interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 3,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), startCallCountTotal + 3,
                        "Interval call count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 3,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(),
                        startCallCountTotal + 3, "Interval call start count total is incorrect");
    Assert.assertEquals((3.0 / INTERVAL * 1000.0), _callTracker.getCallStats().getCallsPerSecond(),
                        0.001, "Interval calls per second is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 3,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(),
                        startCallCountTotal + 3, "Interval call start count total is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 0,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), startErrorCountTotal + 0,
                        "Interval error count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 3,
                        "Interval concurrent max  is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 5.0,
                        "Interval average call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Interval call time standard deviation is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getMinimum(), 5,
                        "Interval minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getMaximum(), 5,
                        "Interval maximum call time is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 5,
                        "Interval 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 5,
                        "Interval 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 5,
                        "Interval 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 5,
                        "Interval 99 percentile call time is incorrect");
    CallTracker.CallStats stats1 = _callTracker.getCallStats();

    _clock.addDuration(FIVE_MS);
    List<CallCompletion> dones2 = startCall(_callTracker, 4);
    _clock.addDuration(FIVE_MS);
    _clock.addDuration(FIVE_MS);
    _clock.addDuration(FIVE_MS);

    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), 15,
                        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 4, "Concurrency is incorrect");

    endCall(dones2, 4);

    jitter = 1000;
    _clock.setCurrentTimeMillis(INTERVAL * 2 + startTime + jitter);
    now = _clock.currentTimeMillis();

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(), startTime + INTERVAL,
                        "Interval interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL * 2,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 4,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), startCallCountTotal + 7,
                        "Interval call count total is incorrect");
    Assert.assertEquals((4.0 / INTERVAL * 1000.0), _callTracker.getCallStats().getCallsPerSecond(),
                        0.001, "Interval calls per second is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 4,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(),
                        startCallCountTotal + 7, "Interval call start count total is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 0,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), startErrorCountTotal + 0,
                        "Interval error count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 4,
                        "Interval concurrent max  is incorrect");

    Assert.assertEquals(15, _callTracker.getCallStats().getCallTimeStats().getAverage(), 0.001,
                        "Interval average call time is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getMinimum(), 15,
                        "Interval minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getMaximum(), 15,
                        "Interval maximum call time is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 15,
                        "Interval 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 15,
                        "Interval 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 15,
                        "Interval 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 15,
                        "Interval 99 percentile call time is incorrect");

    Assert.assertEquals(stats1.stale(now), true, "Interval stale is incorrect");

    _clock.setCurrentTimeMillis(INTERVAL * 10 + startTime + jitter);
    now = _clock.currentTimeMillis();
    _callTracker.getCallStats();

    // check listener
    Assert.assertEquals(listener.getRecord(0).getCallStats().getCallCountTotal(), 3,
                        "Interval 1st notification total call count is incorrect");

    Assert.assertEquals(listener.getRecord(1).getCallStats().getIntervalStartTime(),
                        startTime + INTERVAL, "Interval 2nd notification startTime is incorrect");
    Assert.assertEquals(listener.getRecord(1).isReset(), false,
                        "Interval 2nd notification reset is incorrect");
    Assert.assertEquals(listener.getRecord(1).getCallStats().getCallCount(), 4,
                        "Interval 2nd notification call count is incorrect");
    Assert.assertEquals(listener.getRecord(1).getCallStats().getCallCountTotal(), 7,
                        "Interval 2nd notification total call count is incorrect");

    Assert.assertEquals(listener.getRecord(2).getCallStats().getIntervalStartTime(),
                        startTime + INTERVAL * 2,
                        "Interval 3rd notification startTime is incorrect");
    Assert.assertEquals(listener.getRecord(2).isReset(), false,
                        "Interval 3rd notification reset is incorrect");
    Assert.assertEquals(listener.getRecord(2).getCallStats().getCallCount(), 0,
                        "Interval 3rd notification call count is incorrect");
    Assert.assertEquals(listener.getRecord(2).getCallStats().getCallCountTotal(), 7,
                        "Interval 3rd notification total call count is incorrect");

    Assert.assertEquals(listener.getRecord(3).getCallStats().getIntervalStartTime(),
                        startTime + INTERVAL * 9,
                        "Interval 4th notification startTime is incorrect");
    Assert.assertEquals(listener.getRecord(3).isReset(), false,
                        "Interval 4th notification reset is incorrect");
    Assert.assertEquals(listener.getRecord(3).getCallStats().getCallCount(), 0,
                        "Interval 4th notification call count is incorrect");
    Assert.assertEquals(listener.getRecord(3).getCallStats().getCallCountTotal(), 7,
                        "Interval 4th notification total call count is incorrect");

    // unregister listener
    _callTracker.removeStatsRolloverEventListener(listener);
    _clock.setCurrentTimeMillis(INTERVAL * 20 + startTime + jitter * 2);

    List<CallCompletion> dones3 = startCall(_callTracker, 3);
    _clock.addDuration(FIVE_MS);
    _clock.addDuration(FIVE_MS);
    endCall(dones3, 3);

    Assert.assertEquals(listener.getRecords().size(), 4, "Interval notification count is incorrect");
  }

  @Test
  public void testRecord()
  {
    long jitter = 500;
    long now = _clock.currentTimeMillis();
    long startTime = now;
    Listener listener = new Listener();
    _callTracker.addStatsRolloverEventListener(listener);

    _clock.addDuration(FIVE_MS);

    List<CallCompletion> dones = startCall(_callTracker, 3);

    _clock.addDuration(FIVE_MS);

    for (CallCompletion callCompletion : dones)
    {
      callCompletion.record();
    }

    _clock.addDuration(FIVE_MS);
    _clock.addDuration(FIVE_MS);

    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), 15,
        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 3, "Concurrency is incorrect");

    endCall(dones, 3);

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 3,
        "Current call count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 0,
        "Current error count total is incorrect");

    _clock.setCurrentTimeMillis(INTERVAL + startTime + jitter);
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 5.0,
        "Interval average call time is incorrect");
  }

  @org.testng.annotations.Test public void testOutstanding()
  {
    long jitter = 500;
    long now = _clock.currentTimeMillis();
    long startTime = now;

    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), now,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingStartTimeAvg(), 0,
                        "Interval outstanding start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingCount(), 0,
                        "Interval outstanding count is incorrect");

    _clock.setCurrentTimeMillis(startTime + INTERVAL + jitter);

    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingCount(), 0,
                        "Interval outstanding count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingStartTimeAvg(), 0,
                        "Interval outstanding start time average is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 0,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 0,
                        "Interval call count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 0,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 0,
                        "Interval call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 0,
                        "Interval concurrent max is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingCount(), 0,
                        "Interval outstanding count is incorrect");

    _clock.setCurrentTimeMillis(startTime + INTERVAL + INTERVAL / 2);
    CallCompletion done = _callTracker.startCall();

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 0,
                        "Current call count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 1,
                        "Current call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 1, "Current concurrency is incorrect");

    //
    // Test snap on startCall.
    //
    _clock.setCurrentTimeMillis(startTime + INTERVAL * 2 + INTERVAL / 2);
    CallCompletion done2 = _callTracker.startCall();

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 0,
                        "Current call count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 2,
                        "Current call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 2, "Current concurrency is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL * 2,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingStartTimeAvg(), INTERVAL / 2,
                        "Interval outstanding start time average is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingCount(), 1,
                        "Interval outstanding count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 0,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 0,
                        "Interval call count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 1,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 1,
                        "Interval call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 1,
                        "Interval concurrent max is incorrect");

    //
    // Test snap on endCall
    //
    _clock.setCurrentTimeMillis(startTime + INTERVAL * 3 + jitter);
    done2.endCall();

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 1,
                        "Current call count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 2,
                        "Current call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 1, "Current concurrency is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL * 3,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingStartTimeAvg(), INTERVAL,
                        "Interval outstanding start time average is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingCount(), 2,
                        "Interval outstanding count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 0,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 0,
                        "Interval call count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 1,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 2,
                        "Interval call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 2,
                        "Interval concurrent max is incorrect");

    //
    // Test snap on endCall
    //
    _clock.setCurrentTimeMillis(startTime + INTERVAL * 4 + jitter);
    done.endCall();

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 2,
                        "Current call count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 2,
                        "Current call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Current concurrency is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL * 4,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingStartTimeAvg(),
                        INTERVAL * 2 + INTERVAL / 2,
                        "Interval outstanding start time average is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingCount(), 1,
                        "Interval outstanding count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 1,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 1,
                        "Interval call count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 0,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 2,
                        "Interval call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 2,
                        "Interval concurrent max is incorrect");

    //
    // Test snap on getStats
    //
    _clock.setCurrentTimeMillis(startTime + INTERVAL * 5 + jitter);
    _callTracker.getCallStats();

    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL * 5,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingStartTimeAvg(), 0,
                        "Interval outstanding start time average is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getOutstandingCount(), 0,
                        "Interval outstanding count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 1,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 2,
                        "Interval call count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 0,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 2,
                        "Interval call start count total is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 1,
                        "Interval concurrent max is incorrect");
  }

  @org.testng.annotations.Test public void testListenerNotifications()
  {
    long jitter = 500;
    long now = _clock.currentTimeMillis();
    long startTime = now;

    Listener listener = new Listener();
    _callTracker.addStatsRolloverEventListener(listener);

    _clock.addDuration(Time.milliseconds(88));

    // Check event notification on reset

    _callTracker.reset();
    startTime = _clock.currentTimeMillis();

    Assert.assertEquals(listener.getRecord(0).getCallStats().getIntervalStartTime(),
                        startTime - INTERVAL, "Interval 1st notification startTime is incorrect");
    Assert.assertEquals(listener.getRecord(0).isReset(), true,
                        "Interval 1st notification has reset is incorrect");

    _clock.addDuration(FIVE_MS);
    List<CallCompletion> dones3 = startCall(_callTracker, 3);
    _clock.addDuration(FIVE_MS);
    _clock.addDuration(FIVE_MS);
    endCall(dones3, 3);

    // Check event notification occurs on end call

    _clock.setCurrentTimeMillis(startTime + INTERVAL);
    _clock.addDuration(FIVE_MS);
    _callTracker.startCall().endCall();

    Assert.assertEquals(listener.getRecord(1).getCallStats().getIntervalStartTime(), startTime,
                        "Interval 2nd notification startTime is incorrect");
    Assert.assertEquals(listener.getRecord(1).isReset(), false,
                        "Interval 2nd notification has reset is incorrect");
    Assert.assertEquals(listener.getRecord(1).getCallStats().getCallCount(), 3,
                        "Interval 2nd notification call time is incorrect");

    // Check event notification occurs on getStats.

    _clock.setCurrentTimeMillis(startTime + INTERVAL * 2);
    _clock.addDuration(FIVE_MS);
    _clock.addDuration(FIVE_MS);
    CallTracker.CallStats stats = _callTracker.getCallStats();

    Assert.assertEquals(listener.getRecord(2).getCallStats().getIntervalStartTime(),
                        startTime + INTERVAL, "Interval 3rd notification startTime is incorrect");
    Assert.assertEquals(listener.getRecord(2).isReset(), false,
                        "Interval 3rd notification has reset is incorrect");
    Assert.assertEquals(listener.getRecord(2).getCallStats().getCallCount(), 1,
                        "Interval 3rd notification call time is incorrect");

    // Check event notification on reset

    _clock.addDuration(Time.milliseconds(88));
    _callTracker.reset();
    startTime = _clock.currentTimeMillis();

    Assert.assertEquals(listener.getRecord(3).getCallStats().getIntervalStartTime(),
                        startTime - INTERVAL, "Interval 4th notification startTime is incorrect");
    Assert.assertEquals(listener.getRecord(3).isReset(), true,
                        "Interval 4th notification has reset is incorrect");
    Assert.assertEquals(listener.getRecord(3).getCallStats().getCallCount(), 0,
                        "Interval 4th notification call time is incorrect");
    int count = listener.getRecords().size();

    // Remove notifications

    _callTracker.removeStatsRolloverEventListener(listener);

    // Generate some events

    _callTracker.startCall().endCall();
    _clock.setCurrentTimeMillis(startTime + INTERVAL * 10 + 388);

    _callTracker.getCallStats();
    Assert.assertEquals(listener.getRecords().size(), count,
                        "Interval count after reset is incorrect");
  }

  @org.testng.annotations.Test public void testStandardDeviationWithSomeVariance()
  {
    List<CallCompletion> dones = startCall(_callTracker, 4);
    _clock.addDuration(FIVE_MS);
    endCall(dones, 2);
    _clock.addDuration(TEN_MS);
    endCall(dones, 2);

    _clock.setCurrentTimeMillis(_callTracker.getLastResetTime() + INTERVAL);
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 5.00,
                        "Interval standard deviation is incorrect");
  }

  @org.testng.annotations.Test public void testStandardDeviationWithNonIntegerVariance()
  {
    List<CallCompletion> dones = startCall(_callTracker, 3);
    _clock.addDuration(FIVE_MS);
    endCall(dones, 1);
    _clock.addDuration(FIVE_MS);
    endCall(dones, 1);
    _clock.addDuration(FIVE_MS);
    endCall(dones, 1);

    _clock.setCurrentTimeMillis(_callTracker.getLastResetTime() + INTERVAL);
    Assert.assertEquals(4.082,
                        _callTracker.getCallStats().getCallTimeStats().getStandardDeviation(),
                        0.001, "Interval standard deviation is incorrect");
  }

  @org.testng.annotations.Test public void testStandardDeviationWithSmallVarianceAndLargeSample()
  {
    long interval = 7200000;
    _callTracker = new CallTrackerImpl(interval, _clock);

    List<CallCompletion> dones = startCall(_callTracker, 50 * 1000);
    _clock.addDuration(Time.minutes(60));
    endCall(dones, 25 * 1000);
    _clock.addDuration(Time.milliseconds(2));
    endCall(dones, 25 * 1000);

    _clock.setCurrentTimeMillis(_callTracker.getLastResetTime() + interval);
    Assert.assertEquals(1.0, _callTracker.getCallStats().getCallTimeStats().getStandardDeviation(),
                        0.001, "Interval Standard deviation is incorrect");
  }

  @org.testng.annotations.Test public void testCallStorage()
  {
    long startTime = _clock.currentTimeMillis();
    long startCallCountTotal = _callTracker.getCurrentCallCountTotal();
    long startErrorCountTotal = _callTracker.getCurrentErrorCountTotal();

    List<CallCompletion> dones = startCall(_callTracker, 14);
    CallCompletion doneErr = _callTracker.startCall();

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 15, "Concurrency is incorrect");
    _clock.addDuration(FIVE_MS);
    endCall(dones, 5);

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 10, "Concurrency is incorrect");
    _clock.addDuration(FIVE_MS);
    endCall(dones, 4);

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 6, "Concurrency is incorrect");
    doneErr.endCallWithError();

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 5, "Concurrency is incorrect");
    _clock.addDuration(FIVE_MS);
    endCall(dones, 5);

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), startCallCountTotal + 15,
                        "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), startErrorCountTotal + 1,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), 15,
                        "Time since last call is incorrect");

    _clock.setCurrentTimeMillis(INTERVAL + startTime);

    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(), startTime,
                        "Interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL,
                        "Interval end time is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 15,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), startCallCountTotal + 15,
                        "Interval total call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 15,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(),
                        startCallCountTotal + 15, "Interval total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 1,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), startErrorCountTotal + 1,
                        "Interval total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 1.0 / 15.0,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 15,
                        "Interval max concurrent is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 10.0,
                        "Interval average time is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getMinimum(), 5,
                        "Interval minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getMaximum(), 15,
                        "Interval maximum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 10,
                        "Interval 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 15,
                        "Interval 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 15,
                        "Interval 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 15,
                        "Interval 99 percentile call time is incorrect");

  }

  @org.testng.annotations.Test public void testErrorTracking()
  {
    long startTime = _clock.currentTimeMillis();
    long startCallCountTotal = _callTracker.getCurrentCallCountTotal();
    long startErrorCountTotal = _callTracker.getCurrentErrorCountTotal();

    List<CallCompletion> dones = startCall(_callTracker, 20);

    dones.remove(0).endCallWithError();
    endCall(dones, 3);
    dones.remove(0).endCallWithError();

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 15, "Concurrency is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), startCallCountTotal + 5,
                        "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), startCallCountTotal + 20,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), startErrorCountTotal + 2,
                        "Total error count is incorrect");

    _clock.setCurrentTimeMillis(startTime + INTERVAL);

    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 2, "Error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), startErrorCountTotal + 2,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.4, "Error rate is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 20,
                        "Max concurrent is incorrect");

    endCall(dones, 4);

    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 11, "Concurrency is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), startCallCountTotal + 9,
                        "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), startCallCountTotal + 20,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), startErrorCountTotal + 2,
                        "Total error count is incorrect");

    dones.remove(0).endCallWithError();
    dones.remove(0).endCallWithError(ErrorType.REMOTE_INVOCATION_EXCEPTION);
    dones.remove(0).endCallWithError(ErrorType.CLOSED_CHANNEL_EXCEPTION);
    dones.remove(0).endCallWithError(ErrorType.CLOSED_CHANNEL_EXCEPTION);
    dones.remove(0).endCallWithError(ErrorType.CONNECT_EXCEPTION);
    dones.remove(0).endCallWithError(ErrorType.CONNECT_EXCEPTION);
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 5, "Concurrency is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), startCallCountTotal + 15,
                        "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), startCallCountTotal + 20,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), startErrorCountTotal + 8,
                        "Total error count is incorrect");

    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.REMOTE_INVOCATION_EXCEPTION), new Integer(1),
                        "Current remote invocation exception count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.CLOSED_CHANNEL_EXCEPTION), new Integer(2),
                        "Current closed channel exception count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.CONNECT_EXCEPTION), new Integer(2),
                        "Current connect exception count is incorrect");

    _clock.setCurrentTimeMillis(startTime + INTERVAL * 2);

    //getCallStats needs to wait for an interval before it produces the stats from previous interval
    Map<ErrorType, Integer> errorTypeCounts = _callTracker.getCallStats().getErrorTypeCounts();
    Map<ErrorType, Integer> errorTypeCountsTotal = _callTracker.getCallStats().getErrorTypeCountsTotal();
    Assert.assertEquals(errorTypeCounts.get(ErrorType.REMOTE_INVOCATION_EXCEPTION), new Integer(1),
                        "Remote invocation exception count is incorrect");
    Assert.assertEquals(errorTypeCounts.get(ErrorType.CLOSED_CHANNEL_EXCEPTION), new Integer(2),
                        "Closed channel exception count is incorrect");
    Assert.assertEquals(errorTypeCounts.get(ErrorType.CONNECT_EXCEPTION), new Integer(2),
                        "Connect exception count is incorrect");

    Assert.assertEquals(errorTypeCountsTotal.get(ErrorType.REMOTE_INVOCATION_EXCEPTION), new Integer(1),
                        "Total remote invocation exception count is incorrect");
    Assert.assertEquals(errorTypeCountsTotal.get(ErrorType.CLOSED_CHANNEL_EXCEPTION), new Integer(2),
                        "Total closed channel exception count is incorrect");
    Assert.assertEquals(errorTypeCountsTotal.get(ErrorType.CONNECT_EXCEPTION), new Integer(2),
                        "Total connect exception count is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 6, "Error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), startErrorCountTotal + 8,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.6, "Error rate is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 15,
                        "Max concurrent is incorrect");

    //simulate that in the middle of interval we increment the error counts
    dones.remove(0).endCallWithError(ErrorType.REMOTE_INVOCATION_EXCEPTION);
    dones.remove(0).endCallWithError(ErrorType.REMOTE_INVOCATION_EXCEPTION);
    dones.remove(0).endCallWithError(ErrorType.REMOTE_INVOCATION_EXCEPTION);

    //this change should be reflected in getCurrentErrorTypeCountsTotal
    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.REMOTE_INVOCATION_EXCEPTION), new Integer(4),
                        "Current remote invocation exception count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.CLOSED_CHANNEL_EXCEPTION), new Integer(2),
                        "Current closed channel exception count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.CONNECT_EXCEPTION), new Integer(2),
                        "Current connect exception count is incorrect");

    //another simulation of change in the middle of interval
    dones.remove(0).endCallWithError(ErrorType.CLOSED_CHANNEL_EXCEPTION);
    dones.remove(0).endCallWithError(ErrorType.CLOSED_CHANNEL_EXCEPTION);

    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.REMOTE_INVOCATION_EXCEPTION), new Integer(4),
                        "Current remote invocation exception count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.CLOSED_CHANNEL_EXCEPTION), new Integer(4),
                        "Current closed channel exception count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorTypeCountsTotal().get(ErrorType.CONNECT_EXCEPTION), new Integer(2),
                        "Current connect exception count is incorrect");

     _clock.setCurrentTimeMillis(startTime + INTERVAL * 3);

    //now the getErrorTypeCounts and getErrorTypeCountsTotal should be different because at previous interval
    //we only receive 3 REMOTE_INVOCATION_ERROR and 2 HTTP_400_ERROR
    errorTypeCounts = _callTracker.getCallStats().getErrorTypeCounts();
    errorTypeCountsTotal = _callTracker.getCallStats().getErrorTypeCountsTotal();

    Assert.assertEquals(errorTypeCounts.get(ErrorType.REMOTE_INVOCATION_EXCEPTION), new Integer(3),
                        "Remote invocation exception count is incorrect");
    Assert.assertEquals(errorTypeCounts.get(ErrorType.CLOSED_CHANNEL_EXCEPTION), new Integer(2),
                        "Closed channel exception count is incorrect");
    Assert.assertNull(errorTypeCounts.get(ErrorType.CONNECT_EXCEPTION), "Connect exception count is incorrect");

    Assert.assertEquals(errorTypeCountsTotal.get(ErrorType.REMOTE_INVOCATION_EXCEPTION), new Integer(4),
                        "Total remote invocation exception count is incorrect");
    Assert.assertEquals(errorTypeCountsTotal.get(ErrorType.CLOSED_CHANNEL_EXCEPTION), new Integer(4),
                        "Total closed channel exception count is incorrect");
    Assert.assertEquals(errorTypeCountsTotal.get(ErrorType.CONNECT_EXCEPTION), new Integer(2),
                        "Total connect exception count is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 5, "Error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), startErrorCountTotal + 13,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 1.0, "Error rate is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 5,
                        "Max concurrent is incorrect");

  }

  @org.testng.annotations.Test public void testReset()
  {
    Assert.assertEquals(_callTracker.getLastResetTime(), _clock.currentTimeMillis(),
                        "Initial lastResetTime is incorrect");

    long startTime = _clock.currentTimeMillis();
    long startCallCountTotal = _callTracker.getCurrentCallCountTotal();
    long startErrorCountTotal = _callTracker.getCurrentErrorCountTotal();
    long jitter = 500;

    List<CallCompletion> dones = startCall(_callTracker, 5);
    _clock.addDuration(FIVE_MS);
    endCall(dones, 5);

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 5, "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 5,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 0,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), 5,
                        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");

    _clock.setCurrentTimeMillis(INTERVAL + startTime + jitter);
    long now = _clock.currentTimeMillis();

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(), startTime,
                        "Interval interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), startTime + INTERVAL,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 5.0,
                        "Interval average time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Interval standard deviation is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Interval error rate is incorrect");

    _clock.addDuration(FIVE_MS);
    _callTracker.reset();
    long lastResetTime = now = _clock.currentTimeMillis();
    _clock.addDuration(FIVE_MS);

    Assert.assertEquals(_callTracker.getLastResetTime(), lastResetTime,
                        "lastResetTime is incorrect after reset");

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 0, "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 0,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 0,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), -1,
                        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(),
                        lastResetTime - INTERVAL, "Interval interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), lastResetTime,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 0,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 0,
                        "Interval total call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 0,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 0,
                        "Interval total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 0,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), 0,
                        "Interval total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 0,
                        "Interval max concurrent is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 0.0,
                        "Interval average time is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval maximum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 0,
                        "Interval 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 0,
                        "Interval 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 0,
                        "Interval 99 percentile call time is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 0.0,
                        "Interval average time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Interval standard deviation is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Interval error rate is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 0,
                        "Interval max concurrency is incorrect");

    long lastCallTime = _clock.currentTimeMillis();
    dones = startCall(_callTracker, 5);
    _clock.addDuration(FIVE_MS);
    endCall(dones, 5);

    _clock.addDuration(FIVE_MS);
    now = _clock.currentTimeMillis();

    Assert.assertEquals(_callTracker.getLastResetTime(), lastResetTime,
                        "lastResetTime is incorrect after reset");

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 5, "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 5,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 0,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), now - lastCallTime,
                        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");

    _clock.setCurrentTimeMillis(INTERVAL + lastResetTime + jitter);
    now = _clock.currentTimeMillis();
    long expectedEndTime = now - (now - lastResetTime) % INTERVAL;

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(),
                        expectedEndTime - INTERVAL, "Interval interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), expectedEndTime,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 5,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 5,
                        "Interval total call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 5,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 5,
                        "Interval total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 0,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), 0,
                        "Interval total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 5,
                        "Interval max concurrent is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 5.0,
                        "Interval average time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Interval standard deviation is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 5,
                        "Interval minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 5,
                        "Interval maximum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 5,
                        "Interval 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 5,
                        "Interval 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 5,
                        "Interval 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 5,
                        "Interval 99 percentile call time is incorrect");
  }

  @org.testng.annotations.Test public void testResetOnTheFly()
  {
    Assert.assertEquals(_callTracker.getLastResetTime(), _clock.currentTimeMillis(),
                        "Initial lastResetTime is incorrect");

    long startTime = _clock.currentTimeMillis();
    long startCallCountTotal = _callTracker.getCurrentCallCountTotal();
    long startErrorCountTotal = _callTracker.getCurrentErrorCountTotal();
    long jitter = 500;

    List<CallCompletion> dones = startCall(_callTracker, 5);
    _clock.addDuration(FIVE_MS);

    dones.remove(0).endCall();
    dones.remove(0).endCallWithError();

    long lastResetTime = _clock.currentTimeMillis();
    _callTracker.reset();

    Assert.assertEquals(_callTracker.getLastResetTime(), lastResetTime,
                        "lastResetTime is incorrect after reset");

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 0, "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 0,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 0,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), -1,
                        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 3, "Concurrency is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(),
                        lastResetTime - INTERVAL, "Interval interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), lastResetTime,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 0,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 0,
                        "Interval total call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 0,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 0,
                        "Interval total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 0,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), 0,
                        "Interval total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 3,
                        "Interval max concurrent is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 0.0,
                        "Interval average time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Interval standard deviation is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval maximum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 0,
                        "Interval 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 0,
                        "Interval 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 0,
                        "Interval 99 percentile call time is incorrect");

    endCall(dones, 3);

    Assert.assertEquals(_callTracker.getLastResetTime(), lastResetTime,
                        "lastResetTime is incorrect after reset");

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 0, "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 0,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 0,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), -1,
                        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(),
                        lastResetTime - INTERVAL, "Interval interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), lastResetTime,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 0,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 0,
                        "Interval total call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 0,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 0,
                        "Interval total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 0,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), 0,
                        "Interval total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.0,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 3,
                        "Interval max concurrent is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 0.0,
                        "Interval average time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Interval standard deviation is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval maximum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 0,
                        "Interval 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 0,
                        "Interval 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 0,
                        "Interval 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 0,
                        "Interval 99 percentile call time is incorrect");

    long lastCallTime = _clock.currentTimeMillis();
    dones = startCall(_callTracker, 5);
    _clock.addDuration(FIVE_MS);
    endCall(dones, 4);
    dones.remove(0).endCallWithError();
    _clock.addDuration(FIVE_MS);
    long now = _clock.currentTimeMillis();

    Assert.assertEquals(_callTracker.getLastResetTime(), lastResetTime,
                        "lastResetTime is incorrect after reset");

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 5, "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 5,
                        "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentErrorCountTotal(), 1,
                        "Total error count is incorrect");
    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), now - lastCallTime,
                        "Time since last call is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");

    _clock.setCurrentTimeMillis(INTERVAL + lastResetTime + jitter);
    now = _clock.currentTimeMillis();

    Assert.assertEquals(_callTracker.getCallStats().getIntervalStartTime(), lastResetTime,
                        "Interval interval start time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getIntervalEndTime(), lastResetTime + INTERVAL,
                        "Interval interval end time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getInterval(), INTERVAL,
                        "Interval interval is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 5,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), 5,
                        "Interval total call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 5,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(), 5,
                        "Interval total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 1,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), 1,
                        "Interval total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 0.2,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 5,
                        "Interval max concurrent is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 5.0,
                        "Interval average time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Interval standard deviation is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 5,
                        "Interval minimum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 5,
                        "Interval maximum call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get50Pct(), 5,
                        "Interval 50 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get90Pct(), 5,
                        "Interval 90 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get95Pct(), 5,
                        "Interval 95 percentile call time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().get99Pct(), 5,
                        "Interval 99 percentile call time is incorrect");
  }

  @org.testng.annotations.Test public void testEndMoreCallsThanStarted()
  {
    long startTime = _clock.currentTimeMillis();
    long startCallCountTotal = _callTracker.getCurrentCallCountTotal();
    long startErrorCountTotal = _callTracker.getCurrentErrorCountTotal();

    CallCompletion done = _callTracker.startCall();
    _clock.addDuration(FIVE_MS);
    done.endCallWithError();
    done.endCall();

    Assert.assertEquals(_callTracker.getCurrentCallCountTotal(), 1, "Total call count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentCallStartCountTotal(), 1,
                        "Total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCurrentConcurrency(), 0, "Concurrency is incorrect");
    Assert.assertEquals(_callTracker.getTimeSinceLastCallStart(), 5,
                        "Time since last call is incorrect");

    _clock.setCurrentTimeMillis(startTime + INTERVAL);

    Assert.assertEquals(_callTracker.getCallStats().getCallCount(), 1,
                        "Interval call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallCountTotal(), startCallCountTotal + 1,
                        "Interval total call count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCount(), 1,
                        "Interval call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallStartCountTotal(),
                        startCallCountTotal + 1, "Interval total call start count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCount(), 1,
                        "Interval error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorCountTotal(), startErrorCountTotal + 1,
                        "Interval total error count is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getErrorRate(), 1.0,
                        "Interval error rate is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getConcurrentMax(), 1,
                        "Interval max concurrent is incorrect");

    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getAverage(), 5.0,
                        "Interval average time is incorrect");
    Assert.assertEquals(_callTracker.getCallStats().getCallTimeStats().getStandardDeviation(), 0.0,
                        "Interval standard deviation is incorrect");
  }

   private List<CallCompletion> startCall(CallTracker callTracker, int count)
  {
    List<CallCompletion> dones = new ArrayList<CallCompletion>();
    for (int x = 0; x < count; x++)
    {
       dones.add(callTracker.startCall());
    }
    return dones;
  }

  private void endCall(List<CallCompletion> dones, int count)
  {
    for (int x = 0; x < count; x++)
    {
      CallCompletion done = dones.remove(0);
      done.endCall();
    }
  }

  private class Listener implements CallTracker.StatsRolloverEventListener
  {
    private final ArrayList<CallTracker.StatsRolloverEvent> _intervalRecords;

    private Listener()
    {
      _intervalRecords = new ArrayList<CallTracker.StatsRolloverEvent>();
    }

    private List<CallTracker.StatsRolloverEvent> getRecords()
    {
      return _intervalRecords;
    }
    private CallTracker.StatsRolloverEvent getRecord(int recordNumber)
    {
      return _intervalRecords.get(recordNumber);
    }
    public void onStatsRollover(CallTracker.StatsRolloverEvent event)
    {
      _intervalRecords.add(event);
    }
  }

}
