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

/* $Id$ */
package com.linkedin.util.degrader;

import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * @author Swee Lim
 * @version $Revision$
 */
public class TestDegrader
{
  private static final Logger log = LoggerFactory.getLogger(TestDegrader.class);

  private static final String   _defaultName = "degrader";
  private static final long _defaultInterval = Time.milliseconds(5000);
  private static final DegraderImpl.LatencyToUse _defaultLatencyToUse = DegraderImpl.LatencyToUse.AVERAGE;
  private static final Double _defaultOverrideDropRate = -1.0;
  private static final Double _defaultMaxDropRate = 0.90;
  private static final long _defaultMaxDropDuration = Time.milliseconds(60000);
  private static final Double _defaultUpStep = 0.20;
  private static final Double _defaultDownStep = 0.25;
  private static final Integer _defaultMinCallCount = 1;
  private static final long _defaultHighLatency = Time.milliseconds(4000);
  private static final long _defaultLowLatency = Time.milliseconds(500);
  private static final Double _defaultHighErrorRate = 0.50;
  private static final Double _defaultLowErrorRate = 0.10;
  private static final long _defaultHighOutstanding = Time.milliseconds(10000);
  private static final long _defaultLowOutstanding = Time.milliseconds(500);
  private static final Integer _defaultMinOutstandingCount = 2;

  private static final DegraderImpl.LatencyToUse _testLatencyToUse = DegraderImpl.LatencyToUse.PCT99;
  private static final Double _testOverrideDropRate = 0.66;
  private static final Double _testMaxDropRate = 0.55;
  private static final long _testMaxDropDuration = Time.milliseconds(99999);
  private static final Double _testUpStep = 0.005;
  private static final Double _testDownStep = 0.006;
  private static final Integer _testMinCallCount = 13;
  private static final long _testHighLatency = Time.milliseconds(5555);
  private static final long _testLowLatency = Time.milliseconds(555);
  private static final Double _testHighErrorRate = 0.95;
  private static final Double _testLowErrorRate = 0.05;
  private static final long _testHighOutstanding = Time.milliseconds(15555);
  private static final long _testLowOutstanding = Time.milliseconds(1555);
  private static final Integer _testMinOutstandingCount = 23;
  private static final double _testInitialDropRate = 0.99;
  private static final double _testSlowStartThreshold = 0.2;
  private static final double _testPreemptiveRequestTimeoutRate = 0.75;

  private static final long _defaultMidLatency = Time.milliseconds((_defaultHighLatency + _defaultLowLatency) / 2);

  private SettableClock _clock;
  private CallTracker _callTracker;
  private DegraderImpl.Config _config;
  private DegraderImpl _degrader;
  private DegraderControl _control;
  private long _startTime;
  private long _lastSetClockToNextInterval;

  @BeforeMethod
  public void setUp() throws Exception
  {
    _clock = new SettableClock();
    _callTracker = new CallTrackerImpl(_defaultInterval, _clock);

    _config = new DegraderImpl.Config();

    _config.setName(_defaultName);
    _config.setCallTracker(_callTracker);
    _config.setClock(_clock);
    _config.setLatencyToUse(_defaultLatencyToUse);
    _config.setOverrideDropRate(_defaultOverrideDropRate);
    _config.setMaxDropRate(_defaultMaxDropRate);
    _config.setMaxDropDuration(_defaultMaxDropDuration);
    _config.setUpStep(_defaultUpStep);
    _config.setDownStep(_defaultDownStep);
    _config.setMinCallCount(_defaultMinCallCount);
    _config.setHighLatency(_defaultHighLatency);
    _config.setLowLatency(_defaultLowLatency);
    _config.setHighErrorRate(_defaultHighErrorRate);
    _config.setLowErrorRate(_defaultLowErrorRate);
    _config.setHighOutstanding(_defaultHighOutstanding);
    _config.setLowOutstanding(_defaultLowOutstanding);
    _config.setMinOutstandingCount(_defaultMinOutstandingCount);

    _degrader = new DegraderImpl(_config);
    _control = new DegraderControl(_degrader);
    _startTime = _clock.currentTimeMillis();
    _lastSetClockToNextInterval = _startTime;

    if (log.isDebugEnabled())
    {
      // debugging use only
      _callTracker.addStatsRolloverEventListener(new CallTracker.StatsRolloverEventListener()
      {
        public void onStatsRollover(CallTracker.StatsRolloverEvent event)
        {
          log.debug(event.getCallStats().toString());
        }
      });
    }
  }

  private static void assertConfigEquals(DegraderImpl.ImmutableConfig config1, DegraderImpl.ImmutableConfig config2)
  {
    assertTrue(config1.getClock() == config2.getClock());
    assertTrue(config1.getLatencyToUse() == config2.getLatencyToUse());
    assertTrue(config1.getOverrideDropRate() == config2.getOverrideDropRate());
    assertTrue(config1.getMaxDropRate() == config2.getMaxDropRate());
    assertTrue(config1.getMaxDropDuration() == config2.getMaxDropDuration());
    assertTrue(config1.getUpStep() == config2.getUpStep());
    assertTrue(config1.getDownStep() == config2.getDownStep());
    assertTrue(config1.getMinCallCount() == config2.getMinCallCount());
    assertTrue(config1.getHighLatency() == config2.getHighLatency());
    assertTrue(config1.getLowLatency() == config2.getLowLatency());
    assertTrue(config1.getHighErrorRate() == config2.getHighErrorRate());
    assertTrue(config1.getLowErrorRate() == config2.getLowErrorRate());
    assertTrue(config1.getHighOutstanding() == config2.getHighOutstanding());
    assertTrue(config1.getLowOutstanding() == config2.getLowOutstanding());
    assertTrue(config1.getMinOutstandingCount() == config2.getMinOutstandingCount());
  }

  boolean checkDrop(double v)
  {
    return _degrader.checkDrop(v);
  }

  boolean checkPreemptiveTimeout()
  {
    return _degrader.checkPreemptiveTimeout();
  }

  CallCompletion[] startCall(int count)
  {
    CallCompletion[] cc = new CallCompletion[count];
    for (int i = 0; i < count; ++i)
    {
      cc[i] = _callTracker.startCall();
    }
    return cc;
  }
  void endCall(CallCompletion[] cc, boolean isError)
  {
    for (int i = 0; i < cc.length; ++i)
    {
      if (isError)
        cc[i].endCallWithError(ErrorType.CLOSED_CHANNEL_EXCEPTION);
      else
        cc[i].endCall();
    }

  }
  private void makeCall(int count, long latency, boolean isError)
  {
    long now = _clock.currentTimeMillis();
    CallCompletion[] cc = startCall(count);
    _clock.setCurrentTimeMillis(now + latency);
    endCall(cc, isError);
  }

  private void advanceClock(long millis)
  {
    _clock.setCurrentTimeMillis(_clock.currentTimeMillis() + millis);
  }

  private void setClockToInterval(int intervalNo)
  {
    _clock.setCurrentTimeMillis(_startTime + _defaultInterval * intervalNo);
  }

  private void setClockToNextInterval()
  {
    long now = _clock.currentTimeMillis();
    long interval = _defaultInterval;
    long newTime;
    if (now == _lastSetClockToNextInterval)
    {
      newTime = now + interval;
    }
    else
    {
      long offset = now - _startTime;
      newTime = (_startTime + ((offset + interval - 1) / interval) * interval);
    }
    _clock.setCurrentTimeMillis(newTime);
    _lastSetClockToNextInterval = newTime;
  }

  @Test
  public void testConfig()
  {
    DegraderImpl.Config config = new DegraderImpl.Config();
    assertTrue(config.getClock() == DegraderImpl.DEFAULT_CLOCK);
    assertTrue(config.getLatencyToUse() == DegraderImpl.DEFAULT_LATENCY_TO_USE);
    assertTrue(config.getOverrideDropRate() == DegraderImpl.DEFAULT_OVERRIDE_DROP_RATE);
    assertTrue(config.getMaxDropRate() == DegraderImpl.DEFAULT_MAX_DROP_RATE);
    assertTrue(config.getMaxDropDuration() == DegraderImpl.DEFAULT_MAX_DROP_DURATION);
    assertTrue(config.getUpStep() == DegraderImpl.DEFAULT_UP_STEP);
    assertTrue(config.getDownStep() == DegraderImpl.DEFAULT_DOWN_STEP);
    assertTrue(config.getMinCallCount() == DegraderImpl.DEFAULT_MIN_CALL_COUNT);
    assertTrue(config.getHighLatency() == DegraderImpl.DEFAULT_HIGH_LATENCY);
    assertTrue(config.getLowLatency() == DegraderImpl.DEFAULT_LOW_LATENCY);
    assertTrue(config.getHighErrorRate() == DegraderImpl.DEFAULT_HIGH_ERROR_RATE);
    assertTrue(config.getLowErrorRate() == DegraderImpl.DEFAULT_LOW_ERROR_RATE);
    assertTrue(config.getHighOutstanding() == DegraderImpl.DEFAULT_HIGH_OUTSTANDING);
    assertTrue(config.getLowOutstanding() == DegraderImpl.DEFAULT_LOW_OUTSTANDING);
    assertTrue(config.getMinOutstandingCount() == DegraderImpl.DEFAULT_MIN_OUTSTANDING_COUNT);
    assertTrue(config.getPreemptiveRequestTimeoutRate() == DegraderImpl.DEFAULT_PREEMPTIVE_REQUEST_TIMEOUT_RATE);
    assertTrue(config.getLogger() == DegraderImpl.DEFAULT_LOGGER);

    String testName = "aaaa";
    config.setName(testName);
    assertTrue(config.getName() == testName);

    CallTracker testCallTracker = _callTracker;
    config.setCallTracker(testCallTracker);
    assertTrue(config.getCallTracker() == testCallTracker);

    Clock testClock = _clock;
    config.setClock(testClock);
    assertTrue(config.getClock() == testClock);

    config.setLatencyToUse(_testLatencyToUse);
    assertTrue(config.getLatencyToUse() == _testLatencyToUse);

    config.setOverrideDropRate(_testOverrideDropRate);
    assertTrue(config.getOverrideDropRate() == _testOverrideDropRate);

    config.setMaxDropRate(_testMaxDropRate);
    assertTrue(config.getMaxDropRate() == _testMaxDropRate);

    config.setMaxDropDuration(_testMaxDropDuration);
    assertTrue(config.getMaxDropDuration() == _testMaxDropDuration);

    config.setUpStep(_testUpStep);
    assertTrue(config.getUpStep() == _testUpStep);

    config.setDownStep(_testDownStep);
    assertTrue(config.getDownStep() == _testDownStep);

    config.setMinCallCount(_testMinCallCount);
    assertTrue(config.getMinCallCount() == _testMinCallCount);

    config.setHighLatency(_testHighLatency);
    assertTrue(config.getHighLatency() == _testHighLatency);

    config.setLowLatency(_testLowLatency);
    assertTrue(config.getLowLatency() == _testLowLatency);

    config.setHighErrorRate(_testHighErrorRate);
    assertTrue(config.getHighErrorRate() == _testHighErrorRate);

    config.setLowErrorRate(_testLowErrorRate);
    assertTrue(config.getLowErrorRate() == _testLowErrorRate);

    config.setHighOutstanding(_testHighOutstanding);
    assertTrue(config.getHighOutstanding() == _testHighOutstanding);

    config.setLowOutstanding(_testLowOutstanding);
    assertTrue(config.getLowOutstanding() == _testLowOutstanding);

    config.setMinOutstandingCount(_testMinOutstandingCount);
    assertTrue(config.getMinOutstandingCount() == _testMinOutstandingCount);

    config.setInitialDropRate(_testInitialDropRate);
    assertEquals(config.getInitialDropRate(), _testInitialDropRate);

    config.setSlowStartThreshold(_testSlowStartThreshold);
    assertEquals(config.getSlowStartThreshold(), _testSlowStartThreshold);

    config.setPreemptiveRequestTimeoutRate(_testPreemptiveRequestTimeoutRate);
    assertEquals(config.getPreemptiveRequestTimeoutRate(), _testPreemptiveRequestTimeoutRate);

    DegraderImpl.ImmutableConfig immutableConfig = new DegraderImpl.ImmutableConfig(config);
    assertConfigEquals(immutableConfig, config);

    DegraderImpl.Config configFromImmutableConfig = new DegraderImpl.Config(immutableConfig);
    assertConfigEquals(configFromImmutableConfig, config);
    assertConfigEquals(configFromImmutableConfig, immutableConfig);
  }

  @Test
  public void testDegraderControlSetConfig()
  {
    assertConfigEquals(_config, _degrader.getConfig());

    _control.setLatencyToUse(_testLatencyToUse.toString());
    assertEquals(_control.getLatencyToUse(), _testLatencyToUse.toString());

    _control.setOverrideDropRate(_testOverrideDropRate);
    assertTrue(_control.getOverrideDropRate() == _testOverrideDropRate);

    _control.setMaxDropRate(_testMaxDropRate);
    assertTrue(_control.getMaxDropRate() == _testMaxDropRate);

    _control.setMaxDropDuration(_testMaxDropDuration);
    assertEquals(_control.getMaxDropDuration(), _testMaxDropDuration);

    _control.setUpStep(_testUpStep);
    assertTrue(_control.getUpStep() == _testUpStep);

    _control.setDownStep(_testDownStep);
    assertTrue(_control.getDownStep() == _testDownStep);

    _control.setMinCallCount(_testMinCallCount);
    assertTrue(_control.getMinCallCount() == _testMinCallCount);

    _control.setHighLatency(_testHighLatency);
    assertEquals(_control.getHighLatency(), _testHighLatency);

    _control.setLowLatency(_testLowLatency);
    assertEquals(_control.getLowLatency(), _testLowLatency);

    _control.setHighErrorRate(_testHighErrorRate);
    assertTrue(_control.getHighErrorRate() == _testHighErrorRate);

    _control.setLowErrorRate(_testLowErrorRate);
    assertTrue(_control.getLowErrorRate() == _testLowErrorRate);

    _control.setHighOutstanding(_testHighOutstanding);
    assertEquals(_control.getHighOutstanding(), _testHighOutstanding);

    _control.setLowOutstanding(_testLowOutstanding);
    assertEquals(_control.getLowOutstanding(), _testLowOutstanding);

    _control.setMinOutstandingCount(_testMinOutstandingCount);
    assertTrue(_control.getMinOutstandingCount() == _testMinOutstandingCount);

    _control.setPreemptiveRequestTimeoutRate(_testPreemptiveRequestTimeoutRate);
    assertTrue(_control.getPreemptiveRequestTimeoutRate() == _testPreemptiveRequestTimeoutRate);
  }

  @Test
  public void testDropRate()
  {
    DegraderImpl.Stats stats;
    long lastNotDroppedTime = _clock.currentTimeMillis();

    assertFalse(checkDrop(0.0));
    assertFalse(checkPreemptiveTimeout());
    makeCall(1, _defaultHighLatency, false);
    setClockToNextInterval();
    double expectedDropRate = _defaultUpStep; // 0.20

    stats = _degrader.getStats();
    assertEquals(1, stats.getCurrentCountTotal());
    assertEquals(1, _control.getCurrentCountTotal());
    assertEquals(0, stats.getCurrentDroppedCountTotal());
    assertEquals(0, _control.getCurrentDroppedCountTotal());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertEquals(_clock.currentTimeMillis(), stats.getIntervalEndTime());
    assertEquals(_defaultInterval, stats.getInterval());
    assertEquals(0.0, stats.getDroppedRate());
    assertEquals(1, stats.getCallCount());
    assertEquals(_defaultHighLatency, stats.getLatency());
    assertEquals(0.0, stats.getErrorRate());
    assertEquals(0, stats.getOutstandingLatency());
    assertEquals(0, stats.getOutstandingCount());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(2, stats.getCurrentCountTotal());
    assertEquals(2, _control.getCurrentCountTotal());
    assertEquals(1, stats.getCurrentDroppedCountTotal());
    assertEquals(1, _control.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(3, stats.getCurrentCountTotal());
    assertEquals(3, _control.getCurrentCountTotal());
    assertEquals(1, stats.getCurrentDroppedCountTotal());
    assertEquals(1, _control.getCurrentDroppedCountTotal());

    makeCall(2, _defaultHighLatency, true);
    setClockToNextInterval();
    expectedDropRate += _defaultUpStep; // 0.40

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertEquals(_clock.currentTimeMillis(), stats.getIntervalEndTime());
    assertEquals(_defaultInterval, stats.getInterval());
    assertEquals(0.50, stats.getDroppedRate());
    assertEquals(2, stats.getCallCount());
    assertEquals(_defaultHighLatency, stats.getLatency());
    assertEquals(1.0, stats.getErrorRate());
    assertEquals(0, stats.getOutstandingLatency());
    assertEquals(0, stats.getOutstandingCount());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(4, stats.getCurrentCountTotal());
    assertEquals(2, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(5, stats.getCurrentCountTotal());
    assertEquals(2, stats.getCurrentDroppedCountTotal());

    // check latency in between high and low

    makeCall(1, _defaultMidLatency, false);
    setClockToNextInterval();

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertEquals(_clock.currentTimeMillis(), stats.getIntervalEndTime());
    assertEquals(_defaultInterval, stats.getInterval());
    assertEquals(0.50, stats.getDroppedRate());
    assertEquals(1, stats.getCallCount());
    assertEquals(_defaultMidLatency, stats.getLatency());
    assertEquals(0.0, stats.getErrorRate());
    assertEquals(0, stats.getOutstandingLatency());
    assertEquals(0, stats.getOutstandingCount());

    assertEquals(new Date(_clock.currentTimeMillis()), _control.getIntervalEndTime());
    assertEquals(_defaultInterval, _control.getInterval());
    assertEquals(0.50, _control.getDroppedRate());
    assertEquals(1, stats.getCallCount());
    assertEquals(_defaultMidLatency, _control.getLatency());
    assertEquals(0.0, _control.getErrorRate());
    assertEquals(0, _control.getOutstandingLatency());
    assertEquals(0, stats.getOutstandingCount());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(6, stats.getCurrentCountTotal());
    assertEquals(3, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(7, stats.getCurrentCountTotal());
    assertEquals(3, stats.getCurrentDroppedCountTotal());

    makeCall(1, _defaultHighLatency, false);
    setClockToNextInterval();
    expectedDropRate += _defaultUpStep; // 0.60

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(8, stats.getCurrentCountTotal());
    assertEquals(4, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(9, stats.getCurrentCountTotal());
    assertEquals(4, stats.getCurrentDroppedCountTotal());

    makeCall(1, _defaultHighLatency, false);
    setClockToNextInterval();
    expectedDropRate += _defaultUpStep; // 0.80

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(10, stats.getCurrentCountTotal());
    assertEquals(5, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(11, stats.getCurrentCountTotal());
    assertEquals(5, stats.getCurrentDroppedCountTotal());

    // check MaxDropRate

    makeCall(1, _defaultHighLatency, false);
    setClockToNextInterval();
    expectedDropRate += _defaultUpStep;
    expectedDropRate = Math.min(expectedDropRate, _config.getMaxDropRate()); // 0.90

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(12, stats.getCurrentCountTotal());
    assertEquals(6, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(13, stats.getCurrentCountTotal());
    assertEquals(6, stats.getCurrentDroppedCountTotal());

    long now = _clock.currentTimeMillis();
    setClockToNextInterval();
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(14, stats.getCurrentCountTotal());
    assertEquals(7, stats.getCurrentDroppedCountTotal());

    // check MaxDropDuration

    _clock.setCurrentTimeMillis(now + _config.getMaxDropDuration());
    assertFalse(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(15, stats.getCurrentCountTotal());
    assertEquals(7, stats.getCurrentDroppedCountTotal());

    _clock.setCurrentTimeMillis(now + _config.getMaxDropDuration() + 1000);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(16, stats.getCurrentCountTotal());
    assertEquals(8, stats.getCurrentDroppedCountTotal());

    _clock.setCurrentTimeMillis(now + _config.getMaxDropDuration() * 2);
    assertFalse(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(17, stats.getCurrentCountTotal());
    assertEquals(8, stats.getCurrentDroppedCountTotal());

    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    expectedDropRate -= _defaultDownStep; // 0.65

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(18, stats.getCurrentCountTotal());
    assertEquals(9, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(19, stats.getCurrentCountTotal());
    assertEquals(9, stats.getCurrentDroppedCountTotal());

    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    expectedDropRate -= _defaultDownStep; // 0.40

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(20, stats.getCurrentCountTotal());
    assertEquals(10, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(21, stats.getCurrentCountTotal());
    assertEquals(10, stats.getCurrentDroppedCountTotal());

    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    expectedDropRate -= _defaultDownStep; // 0.15

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(22, stats.getCurrentCountTotal());
    assertEquals(11, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertTrue(checkPreemptiveTimeout());
    stats = _degrader.getStats();
    lastNotDroppedTime = _clock.currentTimeMillis();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(23, stats.getCurrentCountTotal());
    assertEquals(11, stats.getCurrentDroppedCountTotal());

    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    expectedDropRate -= _defaultDownStep; // -0.10
    expectedDropRate = Math.max(expectedDropRate, 0.0); // 0.00

    stats = _degrader.getStats();
    assertEquals(lastNotDroppedTime, stats.getLastNotDroppedTime());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());
    assertEquals(23, stats.getCurrentCountTotal());
    assertEquals(11, stats.getCurrentDroppedCountTotal());

    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertFalse(checkPreemptiveTimeout());

    setClockToNextInterval();
    long outstandingStartTime = _clock.currentTimeMillis();
    CallCompletion[] cc = startCall(_config.getMinOutstandingCount());

    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(_clock.currentTimeMillis(), stats.getIntervalEndTime());
    assertEquals(_defaultInterval, stats.getInterval());
    assertEquals(0.0, stats.getDroppedRate());
    assertEquals(0, stats.getCallCount());
    assertEquals(0, stats.getLatency());
    assertEquals(0.0, stats.getErrorRate());
    assertEquals(_clock.currentTimeMillis() - outstandingStartTime, stats.getOutstandingLatency());
    assertEquals(_config.getMinOutstandingCount(), stats.getOutstandingCount());

    assertEquals(new Date(_clock.currentTimeMillis()), _control.getIntervalEndTime());
    assertEquals(_defaultInterval, _control.getInterval());
    assertEquals(0.0, _control.getDroppedRate());
    assertEquals(0, _control.getCallCount());
    assertEquals(0, _control.getLatency());
    assertEquals(0.0, _control.getErrorRate());
    assertEquals(_defaultInterval, _control.getOutstandingLatency());
    assertEquals(_config.getMinOutstandingCount(), _control.getOutstandingCount());

    cc[0].endCallWithError();

    // test MinOutstandingCount

    setClockToNextInterval();
    expectedDropRate += _defaultUpStep; // 0.20
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    assertEquals(_clock.currentTimeMillis(), stats.getIntervalEndTime());
    assertEquals(_defaultInterval, stats.getInterval());
    assertEquals(0.0, stats.getDroppedRate());
    assertEquals(1, stats.getCallCount());
    assertEquals(_defaultInterval, stats.getLatency());
    assertEquals(1.0, stats.getErrorRate());
    assertEquals(_defaultInterval * 2, stats.getOutstandingLatency());
    assertEquals(_config.getMinOutstandingCount() - 1, stats.getOutstandingCount());

    assertEquals(new Date(_clock.currentTimeMillis()), _control.getIntervalEndTime());
    assertEquals(_defaultInterval, _control.getInterval());
    assertEquals(0.0, _control.getDroppedRate());
    assertEquals(1, _control.getCallCount());
    assertEquals(_defaultInterval, _control.getLatency());
    assertEquals(1.0, _control.getErrorRate());
    assertEquals(_defaultInterval * 2, _control.getOutstandingLatency());
    assertEquals(_config.getMinOutstandingCount() - 1, _control.getOutstandingCount());

    endCall(cc, false);
    setClockToNextInterval();

    expectedDropRate += _defaultUpStep; // 0.40

    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    // testMinCallCount

    _control.reset();
    _control.setMinCallCount(4);
    expectedDropRate = 0.0;

    makeCall(3, _defaultHighLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(3, stats.getCallCount());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    makeCall(4, _defaultHighLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    expectedDropRate += _defaultUpStep; // 0.20
    // actualMinCallCount = round(3.2)
    assertEquals(4, stats.getCallCount());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    makeCall(2, _defaultHighLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(2, stats.getCallCount());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    makeCall(3, _defaultHighLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    expectedDropRate += _defaultUpStep; // 0.40
    // actualMinCallCount = round(2.4)
    assertEquals(3, stats.getCallCount());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(1, stats.getCallCount());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    makeCall(2, _defaultLowLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    expectedDropRate -= _defaultDownStep; // 0.15
    // actualMinCallCount = round(3.4)
    assertEquals(2, stats.getCallCount());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    makeCall(2, _defaultLowLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(2, stats.getCallCount());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());

    makeCall(3, _defaultLowLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    expectedDropRate -= _defaultDownStep; // -0.10
    expectedDropRate = Math.max(0.0, expectedDropRate);
    // actualMinCallCount = round(3.4)
    assertEquals(3, stats.getCallCount());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    assertEquals(expectedDropRate, stats.getCurrentDropRate());
  }

  @Test
  public void testAboveHigh()
  {
    DegraderImpl.Stats stats;
    double expectedDropRate;

    _control.setMinCallCount(10);

    // ErrorRate

    makeCall(4, _defaultLowLatency, true);
    makeCall(5, _defaultLowLatency, false);
    setClockToNextInterval();
    expectedDropRate = 0; // errorRate < highErrorRate && callCount < minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(5, _defaultLowLatency, true);
    makeCall(4, _defaultLowLatency, false);
    setClockToNextInterval();
    expectedDropRate = 0; // errorRate >= highErrorRate && callCount < minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(4, _defaultLowLatency, true);
    makeCall(6, _defaultLowLatency, false);
    setClockToNextInterval(); // errorRate < highErrorRate && callCount >= minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(5, _defaultLowLatency, true);
    makeCall(5, _defaultLowLatency, false);
    setClockToNextInterval(); // errorRate >= highErrorRate && callCount >= minCallCount
    expectedDropRate = _defaultUpStep; // 0.20
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    // Latency

    makeCall(7, _defaultMidLatency, false);
    setClockToNextInterval(); // latency < highLatency && callCount < minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(8, _defaultMidLatency, false); // callCount == minCallCount
    setClockToNextInterval(); // latency < highLatency && callCount == minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(9, _defaultMidLatency, false);
    setClockToNextInterval(); // latency < highLatency && callCount > minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(7, _defaultHighLatency, false);
    setClockToNextInterval(); // latency >= highLatency && callCount < minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(8, _defaultHighLatency, false);
    setClockToNextInterval(); // latency >= highLatency && callCount >= minCallCount
    expectedDropRate += _defaultUpStep; // 0.40
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    // Outstanding

    CallCompletion[] cc = startCall(1);
    advanceClock(_config.getHighOutstanding());
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    cc[0].endCall();

    _degrader.reset();
    _callTracker.reset();

    stats = _degrader.getStats();
    expectedDropRate = 0.0; // 0.00
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    cc = startCall(_config.getMinOutstandingCount());
    advanceClock(_config.getHighOutstanding());
    expectedDropRate = _defaultUpStep; // 0.20
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    cc[0].endCall();
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
  }

  @Test
  public void testBelowLow()
  {
    DegraderImpl.Stats stats;
    double expectedDropRate;
    double downStep = 0.10;

    _control.setMinCallCount(100);
    _control.setLowErrorRate(0.20);
    _control.setUpStep(_config.getMaxDropRate());
    _control.setDownStep(downStep);
    makeCall(100, _defaultLowLatency, true);
    setClockToNextInterval();
    expectedDropRate = _config.getMaxDropRate(); // 0.90
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    // ErrorRate

    makeCall(2, _defaultLowLatency, true);
    makeCall(7, _defaultLowLatency, false);
    setClockToNextInterval(); // errorRate > lowErroRate && callCount < minCallCall
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(1, _defaultLowLatency, true);
    makeCall(8, _defaultLowLatency, false);
    setClockToNextInterval(); // errorRate < lowErrorRate && callCount < minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(3, _defaultLowLatency, true);
    makeCall(7, _defaultLowLatency, false);
    setClockToNextInterval(); // errorRate > lowErrorRate && callCount >= minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(2, _defaultLowLatency, true);
    makeCall(8, _defaultLowLatency, false);
    setClockToNextInterval(); // errorRate <= lowErrorRate && callCount >= minCallCount
    stats = _degrader.getStats();
    expectedDropRate -= downStep; // 0.80
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    // Latency

    makeCall(19, _defaultMidLatency, false);
    setClockToNextInterval(); // latency > lowLatency && callCount < minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(20, _defaultMidLatency, false); // callCount == minCallCount
    setClockToNextInterval(); // latency > lowLatency && callCount == minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(21, _defaultMidLatency, false); // callCount > minCallCount
    setClockToNextInterval(); // latency > lowLatency && callCount > minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(19, _defaultLowLatency, false);
    setClockToNextInterval(); // latency <= lowLatency && callCount < minCallCount
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    makeCall(20, _defaultLowLatency, false);
    setClockToNextInterval(); // latency <= lowLatency && callCount == minCallCount
    expectedDropRate -= downStep; // 0.80
    stats = _degrader.getStats();
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());

    // Outstanding

    advanceClock(_defaultInterval - (_defaultLowOutstanding - 10));
    CallCompletion[] cc = startCall(19);
    setClockToNextInterval(); // outstanding < lowOutstanding && callCount < minCallCount
    stats = _degrader.getStats();
    assertEquals(_defaultLowOutstanding - 10, stats.getOutstandingLatency());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    endCall(cc, false);

    advanceClock(_defaultInterval - (_defaultLowOutstanding + 10));
    cc = startCall(20);
    setClockToNextInterval(); // outstanding <= lowOutstanding && callCount >= minCallCount
    stats = _degrader.getStats();
    assertEquals(_defaultLowOutstanding + 10, stats.getOutstandingLatency());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    endCall(cc, false);

    setClockToNextInterval();
    _control.setMinCallCount(1);

    advanceClock(_defaultInterval - (_defaultLowOutstanding - 10));
    cc = startCall(_config.getMinOutstandingCount());
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(_defaultLowOutstanding - 10, stats.getOutstandingLatency());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    endCall(cc, false);

    advanceClock(_defaultInterval - _defaultLowOutstanding);
    cc = startCall(_config.getMinOutstandingCount());
    setClockToNextInterval();
    stats = _degrader.getStats();
    expectedDropRate -= downStep; // 0.70
    assertEquals(_defaultLowOutstanding, stats.getOutstandingLatency());
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    endCall(cc, false);

    setClockToNextInterval();
    expectedDropRate -= downStep; // 0.60
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(_defaultLowLatency, _defaultLowOutstanding);
    assertEquals(0, stats.getOutstandingLatency());

    advanceClock(_defaultInterval - (_defaultLowOutstanding - 10));
    cc = startCall(_config.getMinOutstandingCount() - 1);
    assertEquals(expectedDropRate, stats.getCurrentComputedDropRate());
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals((_defaultLowOutstanding - 10), stats.getOutstandingLatency());

    endCall(cc, false);
  }

  private void makeCallRange(long begin, long end)
  {
    for (long i = begin; i < end; i++)
    {
      makeCall(1, i, false);
    }
  }

  @Test
  public void testLatencyToUse()
  {
    DegraderImpl.Stats stats;

    _control.setLatencyToUse("PCT50");
    makeCallRange(1, 21);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(11, stats.getLatency());

    _control.setLatencyToUse("PCT90");
    makeCallRange(1, 21);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(18, stats.getLatency());

    _control.setLatencyToUse("PCT95");
    makeCallRange(1, 21);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(19, stats.getLatency());

    _control.setLatencyToUse("PCT99");
    makeCallRange(1, 21);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(20, stats.getLatency());
  }

  @Test
  public void testOverrideDropRate()
  {
    double computedDropRate;

    makeCall(1, _defaultHighLatency, false);
    computedDropRate = _config.getUpStep(); // 0.20
    setClockToNextInterval();
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    _control.setOverrideDropRate(0.10);
    assertEquals(0.10, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    makeCall(1, _defaultMidLatency, false);
    setClockToNextInterval();
    assertEquals(0.10, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    makeCall(1, _defaultHighLatency, false);
    setClockToNextInterval();
    computedDropRate += _config.getUpStep(); // 0.40
    assertEquals(0.10, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    computedDropRate -= _config.getDownStep(); // 0.15
    assertEquals(0.10, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    _control.setOverrideDropRate(0.99);
    assertEquals(0.99, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    makeCall(1, _defaultMidLatency, false);
    setClockToNextInterval();
    assertEquals(0.99, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    makeCall(1, _defaultHighLatency, false);
    setClockToNextInterval();
    computedDropRate += _config.getUpStep(); // 0.35
    assertEquals(0.99, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    computedDropRate -= _config.getDownStep(); // 0.10
    assertEquals(0.99, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());

    _control.setOverrideDropRate(-0.01);
    assertEquals(computedDropRate, _control.getCurrentDropRate());
    assertEquals(computedDropRate, _control.getCurrentComputedDropRate());
  }

  @Test
  public void testInitialDropRate()
  {
    double expectedDropRate;

    _config.setInitialDropRate(_testInitialDropRate);
    _degrader = new DegraderImpl(_config);
    _control = new DegraderControl(_degrader);

    expectedDropRate = _config.getInitialDropRate(); // 0.99
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkPreemptiveTimeout());

    makeCall(1, _defaultLowLatency, false);
    expectedDropRate -= _config.getDownStep(); // 0.74
    setClockToNextInterval();
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertFalse(checkPreemptiveTimeout());

    makeCall(2, _defaultLowLatency, false);
    expectedDropRate -= _config.getDownStep(); // 0.49
    setClockToNextInterval();
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertFalse(checkPreemptiveTimeout());

    makeCall(5, _defaultLowLatency, false);
    expectedDropRate -= _config.getDownStep(); // 0.24
    setClockToNextInterval();
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertFalse(checkPreemptiveTimeout());

    makeCall(10, _defaultLowLatency, false);
    expectedDropRate -= _config.getDownStep(); // -0.01
    expectedDropRate = Math.max(0.0d, expectedDropRate); // 0.0
    setClockToNextInterval();
    assertFalse(checkDrop(expectedDropRate + 0.05));
    assertFalse(checkPreemptiveTimeout());
  }

  @Test
  public void testPreemptiveRequestTimeout()
  {
    double expectedDropRate = 0.0d;
    _config.setMaxDropRate(1.0d); // set max drop rate to 100%
    _config.setUpStep(0.5d);
    _config.setDownStep(0.25d);
    _degrader = new DegraderImpl(_config);
    _control = new DegraderControl(_degrader);

    // Preemptive timeout should be initially disabled
    assertFalse(checkPreemptiveTimeout());

    DegraderImpl.Stats stats;

    // Preemptive timeout should be enabled as we observe high latency
    makeCall(1, _defaultHighLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(stats.getCallCount(), 1);
    assertEquals(stats.getLatency(), _defaultHighLatency);
    assertTrue(checkPreemptiveTimeout());

    // Preemptive timeout should stay enabled even if there is no request
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(stats.getCallCount(), 0);
    assertEquals(stats.getLatency(), 0);
    assertTrue(checkPreemptiveTimeout());

    // Preemptive timeout should stay enabled if latency stays high
    makeCall(1, _defaultHighLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(stats.getCallCount(), 1);
    assertEquals(stats.getLatency(), _defaultHighLatency);
    assertTrue(checkPreemptiveTimeout());

    // Preemptive timeout should stay enabled even if latency starts to reduce but drop rate is not zero
    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(stats.getCallCount(), 1);
    assertEquals(stats.getLatency(), _defaultLowLatency);
    assertTrue(checkPreemptiveTimeout());

    // Preemptive timeout should be disabled as soon as drop rate is zero
    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    makeCall(1, _defaultLowLatency, false);
    setClockToNextInterval();
    stats = _degrader.getStats();
    assertEquals(stats.getCallCount(), 1);
    assertEquals(stats.getLatency(), _defaultLowLatency);
    assertFalse(checkPreemptiveTimeout());
  }

  @Test
  public void testSlowStartThreshold()
  {
    double expectedDropRate = 0.0;

    _config.setSlowStartThreshold(_testSlowStartThreshold);
    _config.setMaxDropRate(1.0d); // set max drop rate to 100%
    _degrader = new DegraderImpl(_config);
    _control = new DegraderControl(_degrader);

    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate);
    assertFalse(checkDrop(expectedDropRate + 0.05));

    // Stepping up

    makeCall(15, _defaultHighLatency, false);
    expectedDropRate += _config.getUpStep(); // 0.2
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));

    makeCall(10, _defaultHighLatency, false);
    expectedDropRate += _config.getUpStep(); // 0.4
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));

    makeCall(5, _defaultHighLatency, false);
    expectedDropRate += _config.getUpStep(); // 0.6
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));

    makeCall(2, _defaultHighLatency, false);
    expectedDropRate += _config.getUpStep(); // 0.8
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));

    // Max drop rate

    makeCall(1, _defaultHighLatency, false);
    expectedDropRate += _config.getUpStep(); // 1.0

    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate);
    assertTrue(checkDrop(expectedDropRate - 0.05));

    // Slow start

    double transmissionRate = 0.01; // initial slow start transmission rate

    makeCall(1, _defaultLowLatency, false);
    expectedDropRate = 1 - transmissionRate; // 0.99
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertTrue(checkDrop(expectedDropRate - 0.05));

    makeCall(1, _defaultLowLatency, false);
    transmissionRate *= 2;
    expectedDropRate = 1 - transmissionRate; // 0.98
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertTrue(checkDrop(expectedDropRate - 0.05));

    makeCall(2, _defaultLowLatency, false);
    transmissionRate *= 2;
    expectedDropRate = 1 - transmissionRate; // 0.96
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertTrue(checkDrop(expectedDropRate - 0.05));

    makeCall(4, _defaultLowLatency, false);
    transmissionRate *= 2;
    expectedDropRate = 1 - transmissionRate; // 0.92
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));

    makeCall(8, _defaultLowLatency, false);
    transmissionRate *= 2;
    expectedDropRate = 1 - transmissionRate; // 0.84
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));


    makeCall(16, _defaultLowLatency, false);
    transmissionRate *= 2;
    expectedDropRate = 1 - transmissionRate; // 0.68
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));

    // Stepping down

    makeCall(32, _defaultLowLatency, false);
    expectedDropRate -= _config.getDownStep(); // 0.43
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));

    makeCall(57, _defaultLowLatency, false);
    expectedDropRate -= _config.getDownStep(); // 0.18
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertTrue(checkDrop(expectedDropRate - 0.05));
    assertFalse(checkDrop(expectedDropRate + 0.05));

    makeCall(72, _defaultLowLatency, false);
    expectedDropRate -= _config.getDownStep();
    expectedDropRate = Math.max(expectedDropRate, 0.0d); // 0.0
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);
    assertFalse(checkDrop(expectedDropRate + 0.05));
  }

  @Test
  public void testLoggerWithSlowStart()
  {
    double expectedDropRate = 0.0;

    _callTracker = new CallTrackerImpl(_defaultInterval, _clock);
    _config.setCallTracker(_callTracker);
    _config.setSlowStartThreshold(_testSlowStartThreshold);
    _config.setMaxDropRate(1.0d); // set max drop rate to 100%
    _config.setLogger(log);
    _degrader = new DegraderImpl(_config);
    _control = new DegraderControl(_degrader);

    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate);
    assertFalse(checkDrop(expectedDropRate + 0.05));

    // Fully degrading so the expectedDropRate becomes 1
    for (int i = 0; i < 5; ++i)
    {
      makeCall(1, _defaultHighLatency, false);
      setClockToNextInterval();
    }
    expectedDropRate = 1.0;
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);


    // Go through the slowStart steps
    int steps = 0;
    do
    {
      steps++;
      makeCall(1, _defaultLowLatency, false);
      setClockToNextInterval();
      _degrader.getStats();
      assertSame(_degrader.getLogger(), log);
    } while (_control.getCurrentComputedDropRate() > 0);

    assertTrue(steps < 10);
    makeCall(10, _defaultLowLatency, false);
    setClockToNextInterval();
    _degrader.getStats();
    assertEquals(_control.getCurrentComputedDropRate(), 0, 10E-6);
  }


  @Test
  public void testLoggerWithSlowStartAndErrors()
  {
    double expectedDropRate = 0.0;

    _callTracker = new CallTrackerImpl(_defaultInterval, _clock);
    _config.setCallTracker(_callTracker);
    _config.setSlowStartThreshold(_testSlowStartThreshold);
    _config.setMaxDropRate(1.0d); // set max drop rate to 100%
    _config.setLogger(log);
    _degrader = new DegraderImpl(_config);
    _control = new DegraderControl(_degrader);

    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate);
    assertFalse(checkDrop(expectedDropRate + 0.05));

    // Fully degrading so the expectedDropRate becomes 1
    for (int i = 0; i < 5; ++i)
    {
      makeCall(1, _defaultHighLatency, false);
      setClockToNextInterval();
    }
    expectedDropRate = 1.0;
    assertEquals(_control.getCurrentComputedDropRate(), expectedDropRate, 10E-6);

    // Go through the slowStart steps
    makeCall(10, _defaultLowLatency, true);
    setClockToNextInterval();
    assertEquals(_control.getCurrentComputedDropRate(), 1.0, 10E-6);
    assertTrue(_degrader.getLogger() != log);
  }
}
