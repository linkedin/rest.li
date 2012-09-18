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

package com.linkedin.d2.balancer.strategies.degrader;

import java.util.Collections;
import java.util.Map;

import com.linkedin.common.util.MapUtil;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;

public class DegraderLoadBalancerStrategyConfig
{
  private final double _maxClusterLatencyWithoutDegrading;
  private final long   _updateIntervalMs;
  private final double _defaultSuccessfulTransmissionWeight;
  private final int    _pointsPerWeight;
  private final String _hashMethod;
  private final Map<String,Object> _hashConfig;
  private final Clock _clock;

  // this initialRecoveryLevel is the minimum proportion of hash ring points that a Tracker Client
  // can have, and is a number from 0-1. A value of zero will remove the TC completely forever from
  // the hash ring (until the app is bounced or d2-config-cmdline is rerun).
  // The default value is 0.01, which given a default of 100 points, will give the Tracker Client a
  // minimum of 1 point in the hash ring.
  // A value less than 0.01 (0.005, for example), when used in conjunction with the ringRampFactor,
  // can be used to remove the TC from the hash ring for a number of time intervals, 1 time interval
  // in this case.
  private final double _initialRecoveryLevel;

  // The ringRampFactor is the multiplicative factor that will be used to geometrically
  // increase the number of hash ring points for a Tracker Client that had been ramped down to
  // the minimum level and has not yet received traffic since then. The default value is 1, meaning
  // that we will not grow the number of hash ring points. If the ringRampFactor is changed to 2, then
  // that will grow the default initialRecoveryLevel of .01 to .02, .04, .08, .16, .32, .64, 1.0,
  // stopping at the stage where the TrackerClient receives any call.
  private final double _ringRampFactor;
  private final double _highWaterMark;
  private final double _lowWaterMark;
  private final double _globalStepUp;
  private final double _globalStepDown;

  public static final Clock DEFAULT_CLOCK = SystemClock.instance();
  public static final double DEFAULT_INITIAL_RECOVERY_LEVEL = 0.01;
  public static final double DEFAULT_RAMP_FACTOR = 1.0;

  // I think that these two will require tuning, based upon the service SLA.
  // Using degrader's defaults.
  public static final double DEFAULT_HIGH_WATER_MARK = 3000;
  public static final double DEFAULT_LOW_WATER_MARK = 500;

  // even though the degrader has it's own stepUp and stepDown, we need new knobs to turn for
  // the globalStepUp and globalStepDown drop rates.
  public static final double DEFAULT_GLOBAL_STEP_UP = 0.20;
  public static final double DEFAULT_GLOBAL_STEP_DOWN = 0.20;

  public DegraderLoadBalancerStrategyConfig(long updateIntervalMs,
                                            double maxClusterLatencyWithoutDegrading)
  {
    this(updateIntervalMs, maxClusterLatencyWithoutDegrading, 1d, 100, null, Collections.<String, Object>emptyMap(),
         DEFAULT_CLOCK, DEFAULT_INITIAL_RECOVERY_LEVEL, DEFAULT_RAMP_FACTOR, DEFAULT_HIGH_WATER_MARK, DEFAULT_LOW_WATER_MARK,
         DEFAULT_GLOBAL_STEP_UP, DEFAULT_GLOBAL_STEP_DOWN);
  }

  public DegraderLoadBalancerStrategyConfig(DegraderLoadBalancerStrategyConfig config)
  {
    this(config.getUpdateIntervalMs(),
         config.getMaxClusterLatencyWithoutDegrading(),
         config.getDefaultSuccessfulTransmissionWeight(),
         config.getPointsPerWeight(),
         config.getHashMethod(),
         config.getHashConfig(),
         config.getClock(),
         config.getInitialRecoveryLevel(),
         config.getRingRampFactor(),
         config.getHighWaterMark(),
         config.getLowWaterMark(),
         config.getGlobalStepUp(),
         config.getGlobalStepDown());
  }

  public DegraderLoadBalancerStrategyConfig(long updateIntervalMs,
                                            double maxClusterLatencyWithoutDegrading,
                                            double defaultSuccessfulTransmissionWeight,
                                            int pointsPerWeight,
                                            String hashMethod,
                                            Map<String,Object> hashConfig,
                                            Clock clock,
                                            double initialRecoveryLevel,
                                            double ringRampFactor,
                                            double highWaterMark,
                                            double lowWaterMark,
                                            double globalStepUp,
                                            double globalStepDown)
  {
    _updateIntervalMs = updateIntervalMs;
    _maxClusterLatencyWithoutDegrading = maxClusterLatencyWithoutDegrading;
    _defaultSuccessfulTransmissionWeight = defaultSuccessfulTransmissionWeight;
    _pointsPerWeight = pointsPerWeight;
    _hashMethod = hashMethod;
    _hashConfig = Collections.unmodifiableMap(hashConfig);
    _clock = clock;
    _initialRecoveryLevel = initialRecoveryLevel;
    _ringRampFactor = ringRampFactor;
    _highWaterMark = highWaterMark;
    _lowWaterMark = lowWaterMark;
    _globalStepUp = globalStepUp;
    _globalStepDown = globalStepDown;
  }

  public static DegraderLoadBalancerStrategyConfig configFromMap(Map<String,Object> map)
  {
    Long updateIntervalMs = MapUtil.getWithDefault(map, "updateIntervalMs", 5000L);
    Double maxClusterLatencyWithoutDegrading = MapUtil.getWithDefault(map,
            "maxClusterLatencyWithoutDegrading", 500d);
    Double defaultSuccessfulTransmissionWeight = MapUtil.getWithDefault(map,
            "defaultSuccessfulTransmissionWeight", 1d);
    Integer pointsPerWeight = MapUtil.getWithDefault(map, "pointsPerWeight", 100);
    String hashMethod = MapUtil.getWithDefault(map, "hashMethod", null, String.class);

    Clock clock = MapUtil.getWithDefault(map, "clock", DEFAULT_CLOCK, Clock.class);

    Double initialRecoveryLevel = MapUtil.getWithDefault(map,
                                                         "initialRecoveryLevel", DEFAULT_INITIAL_RECOVERY_LEVEL);
    Double recoveryStepCoefficient = MapUtil.getWithDefault(map,
                                                            "ringRampFactor", DEFAULT_RAMP_FACTOR);

    Double ringRampFactor = MapUtil.getWithDefault(map,
                                                   "ringRampFactor", DEFAULT_RAMP_FACTOR);

    Double highWaterMark = MapUtil.getWithDefault(map,
                                                  "highWaterMark", DEFAULT_HIGH_WATER_MARK);
    Double lowWaterMark = MapUtil.getWithDefault(map,
                                                 "lowWaterMark", DEFAULT_LOW_WATER_MARK);

    Double globalStepUp = MapUtil.getWithDefault(map,
                                                 "globalStepUp", DEFAULT_GLOBAL_STEP_UP);
    Double globalStepDown = MapUtil.getWithDefault(map,
                                                   "globalStepDown", DEFAULT_GLOBAL_STEP_DOWN);

    @SuppressWarnings("unchecked")
    Map<String,Object> hashConfig = (Map<String,Object>)map.get("hashConfig");
    if (hashConfig == null)
    {
      hashConfig = Collections.emptyMap();
    }

    return new DegraderLoadBalancerStrategyConfig(
            updateIntervalMs, maxClusterLatencyWithoutDegrading,
            defaultSuccessfulTransmissionWeight, pointsPerWeight, hashMethod, hashConfig,
            clock, initialRecoveryLevel, ringRampFactor, highWaterMark, lowWaterMark,
            globalStepUp, globalStepDown);
  }

  /**
   * @return The maximum cluster latency to tolerate before the degrader will begin
   *         allowing nodes to drop traffic.
   */
  public double getMaxClusterLatencyWithoutDegrading()
  {
    return _maxClusterLatencyWithoutDegrading;
  }

  /**
   * @return How often the degrader updates its state.
   */
  public long getUpdateIntervalMs()
  {
    return _updateIntervalMs;
  }

  /**
   * @return The weight to be assigned to a node by default if no calls have been made to
   *         a cluster recently.
   */
  public double getDefaultSuccessfulTransmissionWeight()
  {
    return _defaultSuccessfulTransmissionWeight;
  }

  /**
   * @return The amount of points to assign a 1.0 weight in the consistent hash ring.
   */
  public int getPointsPerWeight()
  {
    return _pointsPerWeight;
  }

  public String getHashMethod()
  {
    return _hashMethod;
  }

  public Map<String, Object> getHashConfig()
  {
    return _hashConfig;
  }

  public Clock getClock()
  {
    return _clock;
  }

  public double getInitialRecoveryLevel()
  {
    return _initialRecoveryLevel;
  }

  public double getRingRampFactor()
  {
    return _ringRampFactor;
  }

  public double getHighWaterMark()
  {
    return _highWaterMark;
  }

  public double getLowWaterMark()
  {
    return _lowWaterMark;
  }

  public double getGlobalStepUp()
  {
    return _globalStepUp;
  }

  public double getGlobalStepDown()
  {
    return _globalStepDown;
  }

  @Override
  public String toString()
  {
    return "DegraderLoadBalancerStrategyConfig [_highWaterMark=" + _highWaterMark
            + ", _lowWaterMark=" + _lowWaterMark + ", _initialRecoveryLevel=" + _initialRecoveryLevel
            + ", _ringRampFactor=" + _ringRampFactor + ", _globalStepUp=" + _globalStepUp
            + ", _globalStepDown=" + _globalStepDown + ", _pointsPerWeight=" + _pointsPerWeight
            + ", _defaultTransmissionRate=" + _defaultSuccessfulTransmissionWeight + "]";
  }
}
