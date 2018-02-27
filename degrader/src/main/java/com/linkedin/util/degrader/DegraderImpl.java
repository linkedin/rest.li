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

/*
 * $Id$
 */

package com.linkedin.util.degrader;

/**
 * @author Swee Lim
 * @version $Rev$
 */

import com.linkedin.common.stats.LongStats;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.clock.Time;
import com.linkedin.common.util.ConfigHelper;

/**
 * A Degrader that uses completed call latencies, error rate and outstanding requests
 * to determine the computed drop rate.
 *
 * The degrader has a high watermark and low watermark. At each sampling interval,
 * the degrader obtains statistics from its CallTracker and checks the statistics
 * to determine if the high or low watermark condition has been met or neither.
 *
 * If either high or low watermark condition has been met, it adjusts the
 * computedDropRate. The computedDropRate is a value from 0 (inclusive) to 1.0 (exclusive)
 * that is the fraction of requests that should be dropped to reduce load.
 *
 * If the high watermark condition has been met (@see DegraderImpl.isHigh()), then
 * the degrader will increase the computedDropRate by upStep, i.e.
 * increasing the fraction of requests that should be dropped. The maximum value
 * of computedDropRate is capped by maxDropRate.
 *
 * If the low watermark condition has been met (@see DegraderImpl.()), then
 * the degrader will decrease the computedDropRate by downStep, i.e.
 * reducing the fraction of requests that should be dropped. The minimum value
 * of computedDropRate is 0.
 *
 * The actual dropRate is either the computedDropRate or the overrideDropRate.
 * The overrideDropRate provides for runtime override. If overrideDropRate is enabled
 * (i.e. its value is >= 0.0), then the actual dropRate is always set to the
 * overrideDropRate, else it is set to the computedDropRate. The overrideDropRate
 * can be set to 0.0 can be used to use the degrader in a passive observe mode, i.e.
 * it can calculate computedDropRate but does not actually suggest any requests be
 * dropped. This may useful for tuning the configuration parameters for high and
 * low watermarks during initial deployment and observing the interaction of the
 * degrader with the live services.
 *
 * The highLatency, highErrorRate and highOutstanding configuration parameters
 * are used to determine if the high watermark condition has been met. The high
 * watermark condition is met if (there are sufficient samples in the interval AND
 * (the completed call latency >= highLatency OR the error rate is >= highErrorRate))
 * OR (there are sufficient outstanding calls AND the average of the outstanding calls
 * is >= highOutstanding) (@see DegraderImpl.isHigh()).
 *
 * The lowLatency, lowErrorRate and lowOutstanding configuration parameters
 * are used to determine if the low watermark condition has been met. The low
 * watermark condition is met if there are sufficient samples in the interval AND
 * the completed call latency <= lowLatency AND the error rate is <= lowErrorRate AND
 * (there are insufficient outstanding calls OR the average of the outstanding calls
 * is <= lowOutstanding) (@see DegraderImpl.()).
 *
 * Sufficient number of calls is determined by the minCallCount configuration parameter
 * and the dropRate. The minCallCount is the minimum number of samples that must be
 * present in the interval when no load should be shed (i.e. dropRate == 0.0)
 * for the degrader to use the latency and errorRate statistics provided by CallTracker
 * to make computedDropRate adjustments. The adjustedMinCallCount is adjusted to reflect
 * reduced calls when dropRate is non-zero using max(1, round((1.0 - dropRate) * minCallCount)).
 * For example, if all calls should be dropped, then adjustedMinCallCount should be 1, if
 * no calls should be dropped, adjustedMinCallCount should be the same as minCallCount.
 *
 * Sufficient number of outstanding calls is determined by the minOustandingCount
 * configuration parameter. The number of outstanding calls should be >= minOutstandingCount
 * for the degrader to use the average outstanding latency to determine if high and low
 * watermark condition has been met.
 *
 * If call rate is extremely low and dropRate approaches 1.0, then it is possible that almost
 * all calls will be dropped, creating the problem that there is no new signal to enable
 * the degrader to detect that the service has improved. To account for this, there is
 * a maxDropDuration configuration parameter that specifies that the maximum duration that is
 * allowed when all requests are dropped. For example, if maxDropDuration is 1 min and the last
 * request that should not be dropped is older than 1 min, then the next request should not be
 * dropped.
 *
 * In some cases, the proportion of traffic sent to the degrader may be adjusted dynamically by some
 * other entity such as a load balancer or some other entity managing the drop rate, in such a way
 * that the degrader is not aware of these actions. When this occurs, this other entity may want
 * to override the minCallCount to account for the reduced traffic sent to the degrader. The
 * overrideMinCallCount enabled overriding the default minCallCount to account for the reduced
 * traffic sent to the degrader. The overrideMinCallCount enabled overriding the default minCallCount
 * dynamic adjustment logic. If overrideMinCallCount is >= 0.0, this value overrides the computed/
 * dynamically adjusting min call count logic and always use this value.
 *
 * The latency metric from CallTracker that compared against highLatency and lowLatency
 * is determined by the latencyToUse configuration parameter. It can be the average,
 * 50, 90, 95, 99th percentile latency.
 */

public class DegraderImpl implements Degrader
{
  public static final String MODULE = Degrader.class.getName();
  private static final Logger LOG = LoggerFactory.getLogger(MODULE);

  public static final Clock    DEFAULT_CLOCK = SystemClock.instance();
  public static final Boolean  DEFAULT_LOG_ENABLED = false;
  public static final LatencyToUse DEFAULT_LATENCY_TO_USE = LatencyToUse.AVERAGE;
  public static final Double   DEFAULT_OVERRIDE_DROP_RATE = -1.0;
  public static final Double   DEFAULT_MAX_DROP_RATE = 1.00;
  public static final long     DEFAULT_MAX_DROP_DURATION = Time.milliseconds(60000);
  public static final Double   DEFAULT_UP_STEP = 0.20;
  public static final Double   DEFAULT_DOWN_STEP = 0.20;
  public static final Integer  DEFAULT_MIN_CALL_COUNT = 10;
  public static final long     DEFAULT_HIGH_LATENCY = Time.milliseconds(3000);
  public static final long     DEFAULT_LOW_LATENCY  = Time.milliseconds( 500);
  public static final Double   DEFAULT_HIGH_ERROR_RATE = 1.1;
  public static final Double   DEFAULT_LOW_ERROR_RATE  = 1.1;
  public static final long     DEFAULT_HIGH_OUTSTANDING = Time.milliseconds(10000);
  public static final long     DEFAULT_LOW_OUTSTANDING  = Time.milliseconds(  500);
  public static final Integer  DEFAULT_MIN_OUTSTANDING_COUNT = 5;
  public static final Integer  DEFAULT_OVERRIDE_MIN_CALL_COUNT = -1;
  public static final double   DEFAULT_INITIAL_DROP_RATE = 0.0d;
  public static final double   DEFAULT_SLOW_START_THRESHOLD = 0.0d;
  public static final double   DEFAULT_LOG_THRESHOLD = 0.5d;

  private ImmutableConfig _config;
  private String _name;
  private CallTracker _callTracker;
  private CallTracker.CallStats _callTrackerStats;
  private Clock _clock;
  private volatile long _maxDropDuration;
  private double _computedDropRate;
  private volatile double _dropRate;
  private long _latency;
  private long _outstandingLatency;
  private long _lastIntervalCountTotal;
  private long _lastIntervalDroppedCountTotal;
  private double _lastIntervalDroppedRate;
  private volatile long _lastResetTime;
  private final AtomicLong _lastNotDroppedTime = new AtomicLong();
  private final AtomicLong _countTotal = new AtomicLong();
  private final AtomicLong _noOverrideDropCountTotal = new AtomicLong();
  private final AtomicLong _droppedCountTotal = new AtomicLong();
  private final Logger _rateLimitedLogger;

  public DegraderImpl(Config config)
  {
    _config = new ImmutableConfig(config);
    _name = _config.getName();
    _clock = config.getClock();
    _callTracker = config.getCallTracker();
    _callTrackerStats = _callTracker.getCallStats();
    _maxDropDuration = config.getMaxDropDuration();
    _rateLimitedLogger = config.getLogger();

    reset();

    // Added cast below for backward compatibilty. Remove when possible
    _callTracker.addStatsRolloverEventListener(new CallTracker.StatsRolloverEventListener()
    {
      public void onStatsRollover(CallTracker.StatsRolloverEvent event)
      {
        rolloverStats(event.getCallStats());
      }
    });
  }

  public synchronized void reset()
  {
    setComputedDropRate(_config.getInitialDropRate());
    _lastIntervalCountTotal = 0;
    _lastIntervalDroppedCountTotal = 0;
    _lastIntervalDroppedRate = 0.0;
    _lastResetTime = _clock.currentTimeMillis();
    _lastNotDroppedTime.set(_lastResetTime);
    _countTotal.set(0);
    _noOverrideDropCountTotal.set(0);
    _droppedCountTotal.set(0);
  }

  public String getName()
  {
    return _name;
  }

  public long getLastResetTime()
  {
    return _lastResetTime;
  }

  public synchronized ImmutableConfig getConfig()
  {
    return _config;
  }

  public synchronized void setConfig(Config config)
  {
    if (!config.getName().equals(_config.getName()) ||
        config.getCallTracker() != _config.getCallTracker() ||
        config.getClock() != _config.getClock())
    {
       throw new IllegalArgumentException("Degrader Name, CallTracker and Clock cannot be changed");
    }
    _config = new ImmutableConfig(config);
    _maxDropDuration = config.getMaxDropDuration();
    setComputedDropRate(_computedDropRate); // overrideDropRate may have changed
  }

  /**
   * Determine if a request should be dropped to reduce load.
   *
   * @see Degrader#checkDrop(double)
   */
  public boolean checkDrop(double code)
  {
    long now = _clock.currentTimeMillis();
    checkStale(now);
    _countTotal.incrementAndGet();
    double dropRate = _dropRate;
    double computedDropRate = _computedDropRate;
    boolean drop;
    if (code < dropRate)
    {
      long lastNotDropped = _lastNotDroppedTime.get();
      if ((lastNotDropped + _maxDropDuration) <= now)
      {
        drop = !_lastNotDroppedTime.compareAndSet(lastNotDropped, now);
      }
      else
      {
        drop = true;
      }
      if (drop)
      {
        _droppedCountTotal.incrementAndGet();
      }
    }
    else
    {
      drop = false;
      _lastNotDroppedTime.set(now);
    }
    if (code < computedDropRate)
    {
      _noOverrideDropCountTotal.incrementAndGet();
    }
    return drop;
  }

  /**
   * Same as checkDrop but uses internally generated random number.
   *
   * @see Degrader#checkDrop()
   */
  public boolean checkDrop()
  {
    return checkDrop(ThreadLocalRandom.current().nextDouble());
  }

  /**
   * choose logger to use: always use the default logger if isHigh() return true, logEnabled flag is set, or debug
   * is enabled. Otherwise go with rateLimitedLogger
   */
  public Logger getLogger()
  {
    if (isHigh() || _config.isLogEnabled() || LOG.isDebugEnabled())
    {
      return LOG;
    }
    return _rateLimitedLogger;
  }

  public synchronized Stats getStats()
  {
    checkStale(_clock.currentTimeMillis());
    return new Stats(_dropRate, _computedDropRate,
                     _countTotal.get(),
                     _noOverrideDropCountTotal.get(),
                     _droppedCountTotal.get(),
                     _lastNotDroppedTime.get(),
                     _callTrackerStats.getInterval(),
                     _callTrackerStats.getIntervalEndTime(), _lastIntervalDroppedRate,
                     _callTrackerStats.getCallCount(),
                     _latency, _callTrackerStats.getErrorRate(),
                     _outstandingLatency, _callTrackerStats.getOutstandingCount(),
                     _callTrackerStats.getErrorTypeCounts(),
                     _callTrackerStats.getCallTimeStats());
  }

  /**
   * checks if the stats that we used to compute drop rate is stale or not.
   * Stale meaning, we use a certain interval (configured to be 5 seconds)
   * to count the number of calls, latency, etc during that 5 seconds. So every 5 seconds we'll
   * want to refresh the window of interval for counting.
   *
   * So if it's stale, we'll call CallTrackerImpl to refresh the window
   *
   * @param now
   */
  private void checkStale(long now)
  {
    if (_callTrackerStats.stale(now))
    {
      //this code is a bit strange at first i.e. why it's not _callTrackerStats = _callTracker.getCallStats();
      //but instead it's just _callTracker.getCallStats(); even though getCallStats returns a new Object.
      //but this is fine because getCallStats() will eventually call Tracker.rolloverStats() which has a listener!
      //the listener is actually {@see DegraderImpl.rollOverStats(CallTracker.CallStats)} which will update
      //_callTrackerStats with the new stats. So we're fine.
      _callTracker.getCallStats();
    }
  }

  private synchronized void rolloverStats(CallTracker.CallStats stats)
  {
    _callTrackerStats = stats;

    snapLatency();
    snapOutstandingLatency();

    Logger log = getLogger();
    if (_config.isLogEnabled())
    {
      log.info(_config.getName() + " " + _callTrackerStats);
    }

    long countTotal = _countTotal.get();
    long noOverrideDropCountTotal = _noOverrideDropCountTotal.get();
    long droppedCountTotal = _droppedCountTotal.get();
    long dropped = droppedCountTotal - _lastIntervalDroppedCountTotal;
    long count = countTotal - _lastIntervalCountTotal;

    double lastIntervalDroppedRate = count == 0 ? 0.0 : (double) dropped / (double) count;

    double oldDropRate = _computedDropRate;
    double newDropRate = oldDropRate;
    if (oldDropRate < _config.getMaxDropRate() && isHigh())
    {
      newDropRate = Math.min(_config.getMaxDropRate(), oldDropRate + _config.getUpStep());
    }
    else if (oldDropRate > 0.0 && isLow())
    {
      double oldTransmissionRate = 1.0 - oldDropRate;
      // if the transmissionRate is less than slow start threshold,
      // we'll slowly ramp up the traffic by just doubling the transmissionRate.
      if (oldTransmissionRate < _config.getSlowStartThreshold())
      {
        newDropRate = oldTransmissionRate > 0.0 ? Math.max(0.0, 1 - 2 * oldTransmissionRate) : 0.99;
      }
      else
      {
        newDropRate = Math.max(0.0, oldDropRate - _config.getDownStep());
      }
    }

    if (oldDropRate != newDropRate && log.isWarnEnabled() && newDropRate >= _config.getLogThreshold())
    {
      String logMessage = _config.getName() + " ComputedDropRate " +
          (oldDropRate > newDropRate ? "decreased" : "increased") +
          " from " + oldDropRate + " to " + newDropRate +
          ", OverrideDropRate=" + _config.getOverrideDropRate() +
          ", AdjustedMinCallCount=" + adjustedMinCallCount() +
          ", CallCount=" + _callTrackerStats.getCallCount() +
          ", Latency=" + _latency +
          ", ErrorRate=" + getErrorRateToDegrade() +
          ", OutstandingLatency=" + _outstandingLatency +
          ", OutstandingCount=" + stats.getOutstandingCount() +
          ", NoOverrideDropCountTotal=" + noOverrideDropCountTotal +
          ", DroppedCountTotal=" + droppedCountTotal +
          ", LastIntervalDroppedRate=" + lastIntervalDroppedRate;
      if (oldDropRate < newDropRate)
      {
        // Log as 'warn' only if dropRate is increasing
        log.warn(logMessage);
      }
      else
      {
        log.info(logMessage);
      }
    }
    else
    {
      if (_config.isLogEnabled() && log.isInfoEnabled())
      {
        log.info(_config.getName() +
                " ComputedDropRate=" + newDropRate +
                ", OverrideDropRate=" + _config.getOverrideDropRate() +
                ", AdjustedMinCallCount=" + adjustedMinCallCount() +
                ", CallCount=" + _callTrackerStats.getCallCount() +
                ", Latency=" + _latency +
                ", ErrorRate=" + getErrorRateToDegrade() +
                ", OutstandingLatency=" + _outstandingLatency +
                ", OutstandingCount=" + stats.getOutstandingCount() +
                ", CountTotal=" + countTotal +
                ", NoOverrideDropCountTotal=" + noOverrideDropCountTotal +
                ", DroppedCountTotal=" + droppedCountTotal +
                ", LastIntervalDroppedRate=" + lastIntervalDroppedRate);
      }
      else if (log.isDebugEnabled())
      {
        log.debug(_config.getName() +
                         " ComputedDropRate=" + newDropRate +
                         ", OverrideDropRate=" + _config.getOverrideDropRate() +
                         ", AdjustedMinCallCount=" + adjustedMinCallCount() +
                         ", CallCount=" + _callTrackerStats.getCallCount() +
                         ", Latency=" + _latency +
                         ", ErrorRate=" + getErrorRateToDegrade() +
                         ", OutstandingLatency=" + _outstandingLatency +
                         ", OutstandingCount=" + stats.getOutstandingCount() +
                         ", CountTotal=" + countTotal +
                         ", NoOverrideDropCountTotal=" + noOverrideDropCountTotal +
                         ", DroppedCountTotal=" + droppedCountTotal +
                         ", LastIntervalDroppedRate=" + lastIntervalDroppedRate);
      }
    }

    _lastIntervalCountTotal = countTotal;
    _lastIntervalDroppedCountTotal = droppedCountTotal;
    _lastIntervalDroppedRate = lastIntervalDroppedRate;
    setComputedDropRate(newDropRate);
  }

  private void setComputedDropRate(double newDropRate)
  {
    double overrideDropRate = _config.getOverrideDropRate();
    _dropRate = overrideDropRate >= 0.0 ? overrideDropRate : newDropRate;
    _computedDropRate = newDropRate;
  }

  private void snapLatency()
  {
    CallTracker.CallStats stats = _callTrackerStats;
    switch (_config._latencyToUse)
    {
      case PCT50   : _latency = stats.getCallTimeStats().get50Pct(); break;
      case PCT90   : _latency = stats.getCallTimeStats().get90Pct(); break;
      case PCT95   : _latency = stats.getCallTimeStats().get95Pct(); break;
      case PCT99   : _latency = stats.getCallTimeStats().get99Pct(); break;
      case AVERAGE : _latency = Math.round(stats.getCallTimeStats().getAverage()); break;
      default      : throw new IllegalArgumentException("Latency to use " + _config._latencyToUse + " is unknown");
    }
  }

  private void snapOutstandingLatency()
  {
    CallTracker.CallStats stats = _callTrackerStats;
    _outstandingLatency = stats.getOutstandingStartTimeAvg();
  }

  private int adjustedMinCallCount()
  {
    int overrideMinCallCount = _config.getOverrideMinCallCount();
    if (overrideMinCallCount < 0)
    {
      return Math.max((int) Math.round((1.0 - _dropRate) * _config.getMinCallCount()), 1);
    }
    else
    {
      return Math.max(overrideMinCallCount, 1);
    }
  }

  protected boolean isHigh()
  {
    return (_callTrackerStats.getCallCount() >= adjustedMinCallCount() &&
            (_latency >= _config.getHighLatency() ||
             getErrorRateToDegrade() >= _config.getHighErrorRate())) ||
           (_callTrackerStats.getOutstandingCount() >= _config.getMinOutstandingCount() &&
            _outstandingLatency >= _config.getHighOutstanding());
  }

  protected boolean isLow()
  {
    return _callTrackerStats.getCallCount() >= adjustedMinCallCount() &&
           _latency <= _config.getLowLatency() &&
           getErrorRateToDegrade() <= _config.getLowErrorRate() &&
           (_callTrackerStats.getOutstandingCount() < _config.getMinOutstandingCount() ||
            _outstandingLatency <= _config.getLowOutstanding());
  }

  /**
   * Counts the rate of CONNECT_EXCEPTION, CLOSED_CHANNEL_EXCEPTION and SERVER_ERROR that happens during an interval.
   * We only consider this type of exception for degrading trackerClient. Other errors maybe legitimate
   * so we don't want to punish the server for exceptions that the server is not responsible for e.g.
   * bad user input, frameTooLongException, etc.
   */
  private double getErrorRateToDegrade()
  {
    Map<ErrorType, Integer> errorTypeCounts = _callTrackerStats.getErrorTypeCounts();
    Integer connectExceptionCount = errorTypeCounts.getOrDefault(ErrorType.CONNECT_EXCEPTION, 0);
    Integer closedChannelExceptionCount = errorTypeCounts.getOrDefault(ErrorType.CLOSED_CHANNEL_EXCEPTION, 0);
    Integer serverErrorCount = errorTypeCounts.getOrDefault(ErrorType.SERVER_ERROR, 0);
    Integer timeoutExceptionCount = errorTypeCounts.getOrDefault(ErrorType.TIMEOUT_EXCEPTION, 0);
    return safeDivide(connectExceptionCount + closedChannelExceptionCount + serverErrorCount + timeoutExceptionCount,
        _callTrackerStats.getCallCount());
  }

  private double safeDivide(double numerator, double denominator)
  {
    return denominator != 0 ? numerator / denominator : 0;
  }

  public static enum LatencyToUse
  {
    AVERAGE,
    PCT50,
    PCT90,
    PCT95,
    PCT99
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append("[name = " + _name + ",");
    builder.append(" maxDropDuration = " + _maxDropDuration + ",");
    builder.append(" computedDropRate = " + _computedDropRate + ",");
    builder.append(" dropRate = " + _dropRate + ",");
    builder.append(" latency = " + _latency + ",");
    builder.append(" outstandingLatency = " + _outstandingLatency + ",");
    builder.append(" lastIntervalDroppedRate = " + _lastIntervalDroppedRate + ",");
    builder.append(" callCount = " + _callTrackerStats.getCallCount() + ",");
    builder.append(" droppedCountTotal = " + _droppedCountTotal + "]");
    return builder.toString();
  }

  public static class Stats
  {
    private final double _currentDropRate;
    private final double _currentComputedDropRate;
    private final long   _currentCountTotal;
    private final long   _currentNoOverrideDropCountTotal;
    private final long   _currentDroppedCountTotal;
    private final long   _lastNotDroppedTime;
    private final long   _interval;
    private final long   _intervalEndTime;
    private final double _droppedRate;
    private final int    _callCount;
    private final long   _latency;
    private final double _errorRate;
    private final long   _outstandingLatency;
    private final int    _outstandingCount;
    private final Map<ErrorType, Integer> _errorCountsMap;
    private final LongStats _callTimeStats;


    private Stats(double currentDropRate, double currentComputedDropRate,
                  long currentCountTotal,
                  long currentNoOverrideDropCountTotal,
                  long currentDroppedCountTotal,
                  long lastNotDroppedTime,
                  long interval, long intervalEndTime, double droppedRate,
                  int callCount,
                  long latency,
                  double errorRate,
                  long outstandingLatency,
                  int outstandingCount,
                  Map<ErrorType,Integer> errorCountsMap,
                  LongStats callTimeStats)
    {
      _currentDropRate = currentDropRate;
      _currentComputedDropRate = currentComputedDropRate;
      _currentCountTotal = currentCountTotal;
      _currentNoOverrideDropCountTotal = currentNoOverrideDropCountTotal;
      _currentDroppedCountTotal = currentDroppedCountTotal;
      _lastNotDroppedTime = lastNotDroppedTime;
      _interval = interval;
      _intervalEndTime = intervalEndTime;
      _droppedRate = droppedRate;
      _callCount = callCount;
      _latency = latency;
      _errorRate = errorRate;
      _outstandingLatency = outstandingLatency;
      _outstandingCount = outstandingCount;
      _errorCountsMap = errorCountsMap;
      _callTimeStats = callTimeStats;
    }

    public double getCurrentDropRate()
    {
      return _currentDropRate;
    }
    public double getCurrentComputedDropRate()
    {
      return _currentComputedDropRate;
    }
    public long getCurrentCountTotal()
    {
      return _currentCountTotal;
    }
    public long getCurrentNoOverrideDropCountTotal()
    {
      return _currentNoOverrideDropCountTotal;
    }
    public long getCurrentDroppedCountTotal()
    {
      return _currentDroppedCountTotal;
    }
    public long getLastNotDroppedTime()
    {
      return _lastNotDroppedTime;
    }
    public long getInterval()
    {
      return _interval;
    }
    public long getIntervalEndTime()
    {
      return _intervalEndTime;
    }
    public double getDroppedRate()
    {
      return _droppedRate;
    }
    public int getCallCount()
    {
      return _callCount;
    }
    public long getLatency()
    {
      return _latency;
    }
    public double getErrorRate()
    {
      return _errorRate;
    }
    public long getOutstandingLatency()
    {
      return _outstandingLatency;
    }
    public int getOutstandingCount()
    {
      return _outstandingCount;
    }
    public Map<ErrorType, Integer> getErrorCountsMap()
    {
      return _errorCountsMap;
    }
    public LongStats getCallTimeStats()
    {
      return _callTimeStats;
    }
  }

  public static class ImmutableConfig
  {
    protected String _name;
    protected CallTracker _callTracker;
    protected Clock _clock = DEFAULT_CLOCK;
    protected boolean _logEnabled = DEFAULT_LOG_ENABLED;
    protected LatencyToUse _latencyToUse = DEFAULT_LATENCY_TO_USE;
    protected double _overrideDropRate = DEFAULT_OVERRIDE_DROP_RATE;
    protected double _maxDropRate = DEFAULT_MAX_DROP_RATE;
    protected long _maxDropDuration = DEFAULT_MAX_DROP_DURATION;
    protected double _upStep = DEFAULT_UP_STEP;
    protected double _downStep = DEFAULT_DOWN_STEP;
    protected int _minCallCount = DEFAULT_MIN_CALL_COUNT;
    protected long _highLatency = DEFAULT_HIGH_LATENCY;
    protected long _lowLatency = DEFAULT_LOW_LATENCY;
    protected double _highErrorRate = DEFAULT_HIGH_ERROR_RATE;
    protected double _lowErrorRate = DEFAULT_LOW_ERROR_RATE;
    protected long _highOutstanding = DEFAULT_HIGH_OUTSTANDING;
    protected long _lowOutstanding = DEFAULT_LOW_OUTSTANDING;
    protected int _minOutstandingCount = DEFAULT_MIN_OUTSTANDING_COUNT;
    protected int _overrideMinCallCount = DEFAULT_OVERRIDE_MIN_CALL_COUNT;
    protected double _initialDropRate = DEFAULT_INITIAL_DROP_RATE;
    protected double _slowStartThreshold = DEFAULT_SLOW_START_THRESHOLD;
    protected Logger _logger = LoggerFactory.getLogger(ImmutableConfig.class);
    protected double _logThreshold = DEFAULT_LOG_THRESHOLD;

    public ImmutableConfig()
    {
    }

    public ImmutableConfig(ImmutableConfig config)
    {
      this._name = config._name;
      this._callTracker = config._callTracker;
      this._clock = config._clock;
      this._logEnabled = config._logEnabled;
      this._latencyToUse = config._latencyToUse;
      this._overrideDropRate = config._overrideDropRate;
      this._maxDropRate = config._maxDropRate;
      this._maxDropDuration = config._maxDropDuration;
      this._upStep = config._upStep;
      this._downStep = config._downStep;
      this._minCallCount = config._minCallCount;
      this._highLatency = config._highLatency;
      this._lowLatency = config._lowLatency;
      this._highErrorRate = config._highErrorRate;
      this._lowErrorRate = config._lowErrorRate;
      this._highOutstanding = config._highOutstanding;
      this._lowOutstanding = config._lowOutstanding;
      this._minOutstandingCount = config._minOutstandingCount;
      this._overrideMinCallCount = config._overrideMinCallCount;
      this._initialDropRate = config._initialDropRate;
      this._slowStartThreshold = config._slowStartThreshold;
      this._logger = config._logger;
    }

    public String getName()
    {
      return ConfigHelper.getRequired(_name);
    }

    public CallTracker getCallTracker()
    {
      return ConfigHelper.getRequired(_callTracker);
    }

    public Clock getClock()
    {
      return ConfigHelper.getRequired(_clock);
    }

    public boolean isLogEnabled()
    {
      return _logEnabled;
    }

    public LatencyToUse getLatencyToUse()
    {
      return ConfigHelper.getRequired(_latencyToUse);
    }

    public double getOverrideDropRate()
    {
      return _overrideDropRate;
    }

    public double getMaxDropRate()
    {
      return _maxDropRate;
    }

    public double getInitialDropRate()
    {
      return _initialDropRate;
    }

    public long getMaxDropDuration()
    {
      return _maxDropDuration;
    }

    public double getUpStep()
    {
      return _upStep;
    }

    public double getDownStep()
    {
      return _downStep;
    }

    public int getMinCallCount()
    {
      return _minCallCount;
    }

    public long getHighLatency()
    {
      return _highLatency;
    }

    public long getLowLatency()
    {
      return _lowLatency;
    }

    public double getHighErrorRate()
    {
      return _highErrorRate;
    }

    public double getLowErrorRate()
    {
      return _lowErrorRate;
    }

    public long getHighOutstanding()
    {
      return _highOutstanding;
    }

    public long getLowOutstanding()
    {
      return _lowOutstanding;
    }

    public int getMinOutstandingCount()
    {
      return _minOutstandingCount;
    }

    public int getOverrideMinCallCount()
    {
      return _overrideMinCallCount;
    }

    public double getSlowStartThreshold()
    {
      return _slowStartThreshold;
    }

    public Logger getLogger()
    {
      return _logger;
    }

    public double getLogThreshold()
    {
      return _logThreshold;
    }
  }

  public static class Config extends ImmutableConfig
  {
    public Config()
    {
      super();
    }

    public Config(ImmutableConfig config)
    {
      super(config);
    }

    public void setName(String name)
    {
      _name = name;
    }

    public void setCallTracker(CallTracker callTracker)
    {
      _callTracker = callTracker;
    }

    public void setClock(Clock clock)
    {
      _clock = clock;
    }

    public void setLogEnabled(Boolean logEnabled)
    {
      _logEnabled = logEnabled;
    }

    public void setLatencyToUse(LatencyToUse latencyToUse)
    {
      _latencyToUse = latencyToUse;
    }

    public void setOverrideDropRate(Double overrideDropRate)
    {
      _overrideDropRate = overrideDropRate;
    }

    public void setMaxDropRate(Double maxDropRate)
    {
      _maxDropRate = maxDropRate;
    }

    public void setInitialDropRate(double initialDropRate)
    {
      _initialDropRate = initialDropRate;
    }

    public void setMaxDropDuration(long maxDropDuration)
    {
      _maxDropDuration = maxDropDuration;
    }

    public void setUpStep(Double upStep)
    {
      _upStep = upStep;
    }

    public void setDownStep(Double downStep)
    {
      _downStep = downStep;
    }

    public void setMinCallCount(Integer minCallCount)
    {
      _minCallCount = minCallCount;
    }

    public void setHighLatency(long highLatency)
    {
      _highLatency = highLatency;
    }

    public void setLowLatency(long lowLatency)
    {
      _lowLatency = lowLatency;
    }

    public void setHighErrorRate(Double highErrorRate)
    {
      _highErrorRate = highErrorRate;
    }

    public void setLowErrorRate(Double lowErrorRate)
    {
      _lowErrorRate = lowErrorRate;
    }

    public void setHighOutstanding(long highOutstanding)
    {
      _highOutstanding = highOutstanding;
    }

    public void setLowOutstanding(long lowOutstanding)
    {
      _lowOutstanding = lowOutstanding;
    }

    public void setMinOutstandingCount(Integer minOutstandingCount)
    {
      _minOutstandingCount = minOutstandingCount;
    }

    public void setOverrideMinCallCount(Integer overrideMinCallCount)
    {
      _overrideMinCallCount = overrideMinCallCount;
    }

    public void setSlowStartThreshold(double slowStartThreshold)
    {
      _slowStartThreshold = slowStartThreshold;
    }

    public void setLogger(Logger logger)
    {
      _logger = logger;
    }

    public void setLogThreshold(double threshold)
    {
      _logThreshold = threshold;
    }
  }
}
