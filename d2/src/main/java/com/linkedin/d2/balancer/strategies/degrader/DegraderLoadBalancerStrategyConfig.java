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

import com.linkedin.d2.balancer.properties.PropertyKeys;
import java.util.Collections;
import java.util.Map;

import com.linkedin.common.util.MapUtil;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DegraderLoadBalancerStrategyConfig
{
  private final double _maxClusterLatencyWithoutDegrading;
  private final long   _updateIntervalMs;
  private final double _defaultSuccessfulTransmissionWeight;
  private final int    _pointsPerWeight;
  private final String _hashMethod;
  private final Map<String,Object> _hashConfig;
  private final Clock _clock;
  private static final Logger _log = LoggerFactory.getLogger(DegraderLoadBalancerStrategyConfig.class);

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
  public static final long DEFAULT_UPDATE_INTERVAL_MS = 5000L;
  public static final double DEFAULT_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING = 500d;
  public static final double DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT = 1d;
  public static final int DEFAULT_POINTS_PER_WEIGHT = 100;
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

  private static <V> V getWithDefault(Map<String,Object> map, String oldKey, String httpKey, V defaultValue,
                                      Class<V> valueClass)
  {

    V oldValue = MapUtil.getWithDefault(map, oldKey, null, valueClass);
    V httpValue = MapUtil.getWithDefault(map, httpKey, null, valueClass);
    return chooseNewValueOverOldValueGivenDefaultValue(oldValue, httpValue, defaultValue, oldKey, httpKey);
  }

  /**
   * Creates a DegraderLoadBalancerStrategyConfig that prefers to use HTTP specific properties key.
   * If the properties cannot be found we will attempt to use the default properties key. If
   * we can't find the default properties key then we'll use the default value.
   *
   * Example use case:
   * We want to have 2 LoadBalancerStrategy depending if the scheme is "http" or "not-http"
   * let's say if the scheme is http we want to make the strategy ring ramp factor to be 0.2
   * if it's rest we want to make the ring ramp factor to be 0.1
   *
   * We can define 2 different config keys in the D2 service properties:
   * one is ringRampFactor = 0.1
   * and http.loadBalancer.ringRampFactor = 0.2
   *
   * When someone invoke this method, we will honor the use of http.loadBalancer>ringRampFactor (0.2) to
   * create the strategy. However if we can't find http.loadBalancer.ringRampFactor in the config, we'll use
   * the value in ringRampFactor.
   */
  public static DegraderLoadBalancerStrategyConfig createHttpConfigFromMap(Map<String,Object> map)
  {
    Clock clock = MapUtil.getWithDefault(map, PropertyKeys.CLOCK,
                                         DEFAULT_CLOCK, Clock.class);

    Long updateIntervalMs =
        getWithDefault(map, PropertyKeys.LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
                       PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
                       DEFAULT_UPDATE_INTERVAL_MS, Long.class);

    Double maxClusterLatencyWithoutDegrading =
        getWithDefault(map,
                       PropertyKeys.LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING,
                       PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING,
                       DEFAULT_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING, Double.class);

    Double defaultSuccessfulTransmissionWeight =
        getWithDefault(map,
                       PropertyKeys.LB_STRATEGY_PROPERTIES_DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT,
                       PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT,
                       DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT, Double.class);

    Integer pointsPerWeight =
        getWithDefault(map, PropertyKeys.LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT,
                       PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT,
                       DEFAULT_POINTS_PER_WEIGHT, Integer.class);

    String hashMethod =
        getWithDefault(map, PropertyKeys.LB_HASH_METHOD,
                       PropertyKeys.HTTP_LB_HASH_METHOD, null, String.class);

    Double initialRecoveryLevel =
        getWithDefault(map, PropertyKeys.LB_INITIAL_RECOVERY_LEVEL,
                       PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL,
                       DEFAULT_INITIAL_RECOVERY_LEVEL, Double.class);

    Double ringRampFactor =
        getWithDefault(map, PropertyKeys.LB_RING_RAMP_FACTOR,
                       PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, DEFAULT_RAMP_FACTOR,
                       Double.class);

    Double highWaterMark =
        getWithDefault(map, PropertyKeys.LB_HIGH_WATER_MARK, PropertyKeys.HTTP_LB_HIGH_WATER_MARK,
                       DEFAULT_HIGH_WATER_MARK, Double.class);

    Double lowWaterMark =
        getWithDefault(map, PropertyKeys.LB_LOW_WATER_MARK, PropertyKeys.HTTP_LB_LOW_WATER_MARK,
                       DEFAULT_LOW_WATER_MARK, Double.class);

    Double globalStepUp = getWithDefault(map, PropertyKeys.LB_GLOBAL_STEP_UP, PropertyKeys.HTTP_LB_GLOBAL_STEP_UP,
                                         DEFAULT_GLOBAL_STEP_UP, Double.class);

    Double globalStepDown = getWithDefault(map, PropertyKeys.LB_GLOBAL_STEP_DOWN, PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN,
                                           DEFAULT_GLOBAL_STEP_DOWN, Double.class);

    @SuppressWarnings("unchecked")
    Map<String,Object> hashConfig = getWithDefault(map, PropertyKeys.LB_HASH_CONFIG,
                                                   PropertyKeys.HTTP_LB_HASH_CONFIG,
                                                   Collections.emptyMap(),
                                                   Map.class);

    return new DegraderLoadBalancerStrategyConfig(
        updateIntervalMs, maxClusterLatencyWithoutDegrading,
        defaultSuccessfulTransmissionWeight, pointsPerWeight, hashMethod, hashConfig,
        clock, initialRecoveryLevel, ringRampFactor, highWaterMark, lowWaterMark,
        globalStepUp, globalStepDown);
  }

  /**
   * constructs a  DegraderLoadBalancerStrategyConfig from the given map.
   */
  public static DegraderLoadBalancerStrategyConfig configFromMap(Map<String,Object> map)
  {
    Long updateIntervalMs = MapUtil.getWithDefault(map,
                                         PropertyKeys.LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
                                         DEFAULT_UPDATE_INTERVAL_MS);
    Double maxClusterLatencyWithoutDegrading = MapUtil.getWithDefault(map,
            PropertyKeys.LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING,
            DEFAULT_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING);
    Double defaultSuccessfulTransmissionWeight = MapUtil.getWithDefault(map,
            PropertyKeys.LB_STRATEGY_PROPERTIES_DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT,
            DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT);
    Integer pointsPerWeight = MapUtil.getWithDefault(map,
                                            PropertyKeys.LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT,
                                            DEFAULT_POINTS_PER_WEIGHT);
    String hashMethod = MapUtil.getWithDefault(map,
        PropertyKeys.LB_HASH_METHOD, null, String.class);

    Clock clock = MapUtil.getWithDefault(map, PropertyKeys.CLOCK,
                                         DEFAULT_CLOCK, Clock.class);

    Double initialRecoveryLevel = MapUtil.getWithDefault(map,
                                                         PropertyKeys.LB_INITIAL_RECOVERY_LEVEL,
                                                         DEFAULT_INITIAL_RECOVERY_LEVEL);

    Double ringRampFactor = MapUtil.getWithDefault(map,
                                                   PropertyKeys.LB_RING_RAMP_FACTOR,
                                                   DEFAULT_RAMP_FACTOR);

    Double highWaterMark = MapUtil.getWithDefault(map,
                                                  PropertyKeys.LB_HIGH_WATER_MARK,
                                                  DEFAULT_HIGH_WATER_MARK);
    Double lowWaterMark = MapUtil.getWithDefault(map,
                                                 PropertyKeys.LB_LOW_WATER_MARK,
                                                 DEFAULT_LOW_WATER_MARK);

    Double globalStepUp = MapUtil.getWithDefault(map,
                                                 PropertyKeys.LB_GLOBAL_STEP_UP,
                                                 DEFAULT_GLOBAL_STEP_UP);
    Double globalStepDown = MapUtil.getWithDefault(map,
                                                   PropertyKeys.LB_GLOBAL_STEP_DOWN,
                                                   DEFAULT_GLOBAL_STEP_DOWN);

    @SuppressWarnings("unchecked")
    Map<String,Object> hashConfig = (Map<String,Object>)map.get(PropertyKeys.LB_HASH_CONFIG);
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

  // helper method to choose new value over old value and print log debug if both new value and old
  // value are defined. If both are not defined, will choose the default value
  private static <T> T chooseNewValueOverOldValueGivenDefaultValue(T oldValue, T newValue, T defaultValue,
                                                                   String oldName, String newName)
  {
    if (newValue != null && oldValue != null)
    {
      _log.debug("Both property key:" + oldName + " and " + newName + " are defined in config 2.0. we are using " +
      newName + " with value " + newValue + " to construct DegraderLoadBalancerStrategyConfig");
      return newValue;
    }
    if (newValue != null)
    {
      _log.debug("Property key: " + newName + " is found with value " + newValue);
      return newValue;
    }
    if (oldValue != null)
    {
      _log.debug("Property key : " + newName + " cannot be found so we are using " + oldName);
      return oldValue;
    }
    _log.debug("Cannot find both property key: " + oldName + " and " + newName + " we are using default value " +
      defaultValue);
    return defaultValue;
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
