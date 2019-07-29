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

import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.event.NoopEventEmitter;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.util.hashing.MPConsistentHashRing;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.util.degrader.DegraderImpl;
import java.util.Collections;
import java.util.Map;

import com.linkedin.common.util.MapUtil;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DegraderLoadBalancerStrategyConfig
{
  private final long   _updateIntervalMs;
  // The partition state will only be updated when an interval is elapsed if this is set to true
  private final boolean _updateOnlyAtInterval;
  private final int    _pointsPerWeight;
  private final String _hashMethod;
  private final Map<String,Object> _hashConfig;
  private final Clock _clock;
  private static final Logger _log = LoggerFactory.getLogger(DegraderLoadBalancerStrategyConfig.class);
  private final String _clusterName;

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
  private final long _minClusterCallCountHighWaterMark;
  private final long _minClusterCallCountLowWaterMark;

  private final double _hashRingPointCleanUpRate;

  private final String _consistentHashAlgorithm;
  private final int _numProbes;
  private final int _pointsPerHost;

  private final double _boundedLoadBalancingFactor;

  // The servicePath that is used to construct the URI for quarantine probing
  private final String _servicePath;

  // The configs for quarantine
  private final double _quarantineMaxPercent;
  private final ScheduledExecutorService _executorService;
  private final HealthCheckOperations _healthCheckOperations;
  private final String _healthCheckMethod;
  private final String _healthCheckPath;
  private final long _quarantineLatency;           // in Milliseconds

  private final EventEmitter _eventEmitter;
  // lowEventEmittingInterval and highEventEmittingInterval control the interval for d2monitor
  // to emit events. lowEventEmittingInterval is used when there are abnormal events that need
  // to emit at a higher frequency. highEventEmittingInterval is used when all the hosts are in
  // healthy state. 'lowEventEmittingInterval == 0 && highEventEmittingInterval = 0' disables
  // d2monitor emitting.
  //
  // The settings might need to tuned depending on the number of clients and QPS.
  private final long _lowEventEmittingInterval;
  private final long _highEventEmittingInterval;

  public static final Clock DEFAULT_CLOCK = SystemClock.instance();
  public static final double DEFAULT_INITIAL_RECOVERY_LEVEL = 0.01;
  public static final double DEFAULT_RAMP_FACTOR = 2.0;
  public static final long DEFAULT_UPDATE_INTERVAL_MS = 5000L;
  public static final boolean DEFAULT_UPDATE_ONLY_AT_INTERVAL = false;
  public static final int DEFAULT_POINTS_PER_WEIGHT = 100;
  public static final double DEFAULT_HIGH_WATER_MARK = 600;
  public static final double DEFAULT_LOW_WATER_MARK = 200;

  // even though the degrader has it's own stepUp and stepDown, we need new knobs to turn for
  // the globalStepUp and globalStepDown drop rates.
  public static final double DEFAULT_GLOBAL_STEP_UP = 0.20;
  public static final double DEFAULT_GLOBAL_STEP_DOWN = 0.20;
  public static final long DEFAULT_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK = 10;
  public static final long DEFAULT_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK = 5;

  public static final double DEFAULT_HASHRING_POINT_CLEANUP_RATE = 0.20;

  public static final int DEFAULT_NUM_PROBES = MPConsistentHashRing.DEFAULT_NUM_PROBES;
  public static final int DEFAULT_POINTS_PER_HOST = MPConsistentHashRing.DEFAULT_POINTS_PER_HOST;

  public static final double DEFAULT_BOUNDED_LOAD_BALANCING_FACTOR = -1;

  public static final double DEFAULT_QUARANTINE_MAXPERCENT = 0.0;  // 0 means disable quarantine
  public static final long DEFAULT_QUARANTINE_REENTRY_TIME = 30000;  // Milliseconds
  public static final int DEFAULT_QUARANTINE_CHECKNUM = 5;
  public static final long DEFAULT_QUARANTINE_CHECK_INTERVAL = 1000; // Milliseconds
  public static final long MAX_QUARANTINE_LATENCY = 1000;         // Milliseconds
  public static final String DEFAULT_QUARANTINE_METHOD = RestMethod.OPTIONS;
  private static final double QUARANTINE_MAXPERCENT_CAP = 0.5;

  public static final long DEFAULT_LOW_EVENT_EMITTING_INTERVAL = 0;  // Milliseconds. 0 means disable low interval emitting
  public static final long DEFAULT_HIGH_EVENT_EMITTING_INTERVAL = 0; // Milliseconds. 0 means disable high interval emitting

  public static final String DEFAULT_CLUSTER_NAME = "UNDEFINED_CLUSTER";

  // For testing only
  public DegraderLoadBalancerStrategyConfig(long updateIntervalMs)
  {
    this(updateIntervalMs, DEFAULT_UPDATE_ONLY_AT_INTERVAL, 100, null, Collections.<String, Object>emptyMap(),
         DEFAULT_CLOCK, DEFAULT_INITIAL_RECOVERY_LEVEL, DEFAULT_RAMP_FACTOR, DEFAULT_HIGH_WATER_MARK, DEFAULT_LOW_WATER_MARK,
         DEFAULT_GLOBAL_STEP_UP, DEFAULT_GLOBAL_STEP_DOWN,
         DEFAULT_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK,
         DEFAULT_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK,
         DEFAULT_HASHRING_POINT_CLEANUP_RATE, "pointBased",
         DEFAULT_NUM_PROBES, DEFAULT_POINTS_PER_HOST, DEFAULT_BOUNDED_LOAD_BALANCING_FACTOR, null,
         DEFAULT_QUARANTINE_MAXPERCENT,
         null, null, DEFAULT_QUARANTINE_METHOD, null, DegraderImpl.DEFAULT_LOW_LATENCY,
         null, DEFAULT_LOW_EVENT_EMITTING_INTERVAL, DEFAULT_HIGH_EVENT_EMITTING_INTERVAL, DEFAULT_CLUSTER_NAME);
  }

  public DegraderLoadBalancerStrategyConfig(DegraderLoadBalancerStrategyConfig config)
  {
    this(config.getUpdateIntervalMs(),
         config.isUpdateOnlyAtInterval(),
         config.getPointsPerWeight(),
         config.getHashMethod(),
         config.getHashConfig(),
         config.getClock(),
         config.getInitialRecoveryLevel(),
         config.getRingRampFactor(),
         config.getHighWaterMark(),
         config.getLowWaterMark(),
         config.getGlobalStepUp(),
         config.getGlobalStepDown(),
         config.getMinClusterCallCountHighWaterMark(),
         config.getMinClusterCallCountLowWaterMark(),
         config.getHashRingPointCleanUpRate(),
         config.getConsistentHashAlgorithm(),
         config.getNumProbes(),
         config.getPointsPerHost(),
         config.getBoundedLoadBalancingFactor(),
         config.getServicePath(),
         config.getQuarantineMaxPercent(),
         config.getExecutorService(),
         config.getHealthCheckOperations(),
         config.getHealthCheckMethod(),
         config.getHealthCheckPath(),
         config.getQuarantineLatency(),
         config.getEventEmitter(),
         config.getLowEventEmittingInterval(),
         config.getHighEventEmittingInterval(),
         config.getClusterName());
  }

  public DegraderLoadBalancerStrategyConfig(long updateIntervalMs,
                                            boolean updateOnlyAtInterval,
                                            int pointsPerWeight,
                                            String hashMethod,
                                            Map<String,Object> hashConfig,
                                            Clock clock,
                                            double initialRecoveryLevel,
                                            double ringRampFactor,
                                            double highWaterMark,
                                            double lowWaterMark,
                                            double globalStepUp,
                                            double globalStepDown,
                                            long minCallCountHighWaterMark,
                                            long minCallCountLowWaterMark,
                                            double hashRingPointCleanUpRate,
                                            String consistentHashAlgorithm,
                                            int numProbes,
                                            int pointsPerHost,
                                            double boundedLoadBalancingFactor,
                                            String path,
                                            double quarantineMaxPercent,
                                            ScheduledExecutorService executorService,
                                            HealthCheckOperations healthCheckOperations,
                                            String healthCheckMethod,
                                            String healthCheckPath,
                                            long quarantineLatency,
                                            EventEmitter emitter,
                                            long lowEventEmittingInterval,
                                            long highEventEmittingInterval,
                                            String clusterName)
  {
    _updateIntervalMs = updateIntervalMs;
    _updateOnlyAtInterval = updateOnlyAtInterval;
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
    _minClusterCallCountHighWaterMark = minCallCountHighWaterMark;
    _minClusterCallCountLowWaterMark = minCallCountLowWaterMark;
    _hashRingPointCleanUpRate = hashRingPointCleanUpRate;
    _consistentHashAlgorithm = consistentHashAlgorithm;
    _numProbes = numProbes;
    _pointsPerHost = pointsPerHost;
    _boundedLoadBalancingFactor = boundedLoadBalancingFactor;
    _servicePath = path;
    _quarantineMaxPercent = quarantineMaxPercent;
    _executorService = executorService;
    _healthCheckOperations = healthCheckOperations;
    _healthCheckMethod = healthCheckMethod;
    _healthCheckPath = healthCheckPath;
    _quarantineLatency = quarantineLatency;
    _eventEmitter = emitter == null ? new NoopEventEmitter() : emitter;
    _lowEventEmittingInterval = lowEventEmittingInterval;
    _highEventEmittingInterval = highEventEmittingInterval;
    _clusterName = clusterName;
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
  // @Deprecated -- could not be enforced since -Werror option.
  static DegraderLoadBalancerStrategyConfig createHttpConfigFromMap(Map<String,Object> map)
  {
    return createHttpConfigFromMap(map, null, null, null, null);
  }

  static DegraderLoadBalancerStrategyConfig createHttpConfigFromMap(Map<String,Object> map,
      HealthCheckOperations healthCheckOperations, ScheduledExecutorService overrideExecutorService,
      Map<String, String> degraderProperties, EventEmitter emitter)
  {
    Clock clock = MapUtil.getWithDefault(map, PropertyKeys.CLOCK,
                                         DEFAULT_CLOCK, Clock.class);

    Long updateIntervalMs = MapUtil.getWithDefault(map,
                       PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
                       DEFAULT_UPDATE_INTERVAL_MS, Long.class);

    Boolean updateOnlyAtInterval = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_ONLY_AT_INTERVAL,
            DEFAULT_UPDATE_ONLY_AT_INTERVAL, Boolean.class);

    Integer pointsPerWeight = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT,
                       DEFAULT_POINTS_PER_WEIGHT, Integer.class);

    String hashMethod = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_HASH_METHOD, null, String.class);

    Long minClusterCallCountHighWaterMark = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK,
                                                      DEFAULT_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK, Long.class);

    Long minClusterCallCountLowWaterMark = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK,
                                                          DEFAULT_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK, Long.class);

    Double initialRecoveryLevel = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL,
                       DEFAULT_INITIAL_RECOVERY_LEVEL, Double.class);

    Double ringRampFactor = MapUtil.getWithDefault(map,PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, DEFAULT_RAMP_FACTOR,
                       Double.class);

    Double highWaterMark = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_HIGH_WATER_MARK,
                       DEFAULT_HIGH_WATER_MARK, Double.class);

    Double lowWaterMark = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_LOW_WATER_MARK,
                       DEFAULT_LOW_WATER_MARK, Double.class);

    Double globalStepUp = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_GLOBAL_STEP_UP,
                                         DEFAULT_GLOBAL_STEP_UP, Double.class);

    Double globalStepDown = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN,
                                           DEFAULT_GLOBAL_STEP_DOWN, Double.class);

    Object obj = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_HASH_CONFIG,
                                                   Collections.emptyMap(),
                                                   Map.class);
    @SuppressWarnings("unchecked") // // to appease java 7, which appears to have compilation bugs that cause it to ignore some suppressions, needed to first assign to obj, then assign to the map
    Map<String,Object> hashConfig = (Map<String,Object>)obj;

    Double hashRingPointCleanUpRate = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_HASHRING_POINT_CLEANUP_RATE,
        DEFAULT_HASHRING_POINT_CLEANUP_RATE, Double.class);

    String consistentHashAlgorithm = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_CONSISTENT_HASH_ALGORITHM,
        null, String.class);

    Integer numProbes = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_CONSISTENT_HASH_NUM_PROBES,
        DEFAULT_NUM_PROBES);

    Integer pointsPerHost =
        MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_CONSISTENT_HASH_POINTS_PER_HOST, DEFAULT_POINTS_PER_HOST);

    Double boundedLoadBalancingFactor = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_CONSISTENT_HASH_BOUNDED_LOAD_BALANCING_FACTOR,
        DEFAULT_BOUNDED_LOAD_BALANCING_FACTOR, Double.class);

    String servicePath = MapUtil.getWithDefault(map, PropertyKeys.PATH, null, String.class);

    Double quarantineMaxPercent = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT,
        DEFAULT_QUARANTINE_MAXPERCENT, Double.class);
    if (quarantineMaxPercent > QUARANTINE_MAXPERCENT_CAP)
    {
      // if the user configures the max percent to a very high value, it can dramatically limit the capacity of the
      // cluster when something goes wrong. So impose a cap to max percent.
      quarantineMaxPercent = QUARANTINE_MAXPERCENT_CAP;
      _log.warn("MaxPercent value {} is too high. Changed it to {}", quarantineMaxPercent, QUARANTINE_MAXPERCENT_CAP);
    }
    ScheduledExecutorService executorService = MapUtil.getWithDefault(map,
        PropertyKeys.HTTP_LB_QUARANTINE_EXECUTOR_SERVICE, null, ScheduledExecutorService.class);
    String method = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_QUARANTINE_METHOD, DEFAULT_QUARANTINE_METHOD, String.class);

    // lowLatency reflects the expected health threshold for the service so we can use this value as the
    // quarantine health checking latency.
    Long quarantineLatency = (degraderProperties == null) ? DegraderImpl.DEFAULT_LOW_LATENCY
        : MapUtil.getWithDefault(degraderProperties, PropertyKeys.DEGRADER_LOW_LATENCY, DegraderImpl.DEFAULT_LOW_LATENCY, Long.class);

    // However we'd cap the latency value if degrader.LowLatency is too high: health checking does not involve complicated
    // operations of the service therefore should not take that long
    if (quarantineLatency > MAX_QUARANTINE_LATENCY)
    {
      quarantineLatency = MAX_QUARANTINE_LATENCY;
    }

    // health checking method can be customized from d2config.
    // The supported format is "<Restli Method>:<URI path>". Both part are optional.
    // If <Restli method> is missing, the default method is 'OPTIONS'. If the <URI path>
    // is missing, the service path will be used. For example, "OPTIONS:" and
    // "GET:/contextPath/service/resources/1234" are all valid settings. Specifically,
    // "GET:/<contextPath>/admin" can be used for admin node health checking, where
    // <contextPath> has to match the product-spec.json topology configuration.
    String healthCheckMethod = method;
    String healthCheckPath = null;
    int idx = method.indexOf(':');
    if (idx != -1)
    {
      // Currently allows user to specify any method for health checking (including non-idempotent one)
      healthCheckMethod = method.substring(0, idx);
      healthCheckPath = method.substring(idx + 1);
    }
    if (healthCheckMethod.isEmpty())
    {
      healthCheckMethod = DEFAULT_QUARANTINE_METHOD;
    }

    Long lowEmittingInterval = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_LOW_EVENT_EMITTING_INTERVAL,
        DEFAULT_LOW_EVENT_EMITTING_INTERVAL, Long.class);
    Long highEmittingInterval = MapUtil.getWithDefault(map, PropertyKeys.HTTP_LB_HIGH_EVENT_EMITTING_INTERVAL,
        DEFAULT_HIGH_EVENT_EMITTING_INTERVAL, Long.class);

    final String clusterName = MapUtil.getWithDefault(map, PropertyKeys.CLUSTER_NAME, DEFAULT_CLUSTER_NAME, String.class);

    return new DegraderLoadBalancerStrategyConfig(
        updateIntervalMs, updateOnlyAtInterval, pointsPerWeight, hashMethod, hashConfig,
        clock, initialRecoveryLevel, ringRampFactor, highWaterMark, lowWaterMark,
        globalStepUp, globalStepDown, minClusterCallCountHighWaterMark,
        minClusterCallCountLowWaterMark, hashRingPointCleanUpRate,
        consistentHashAlgorithm, numProbes, pointsPerHost, boundedLoadBalancingFactor,
        servicePath, quarantineMaxPercent,
        overrideExecutorService != null ? overrideExecutorService : executorService,
        healthCheckOperations, healthCheckMethod, healthCheckPath, quarantineLatency,
        emitter, lowEmittingInterval, highEmittingInterval, clusterName);
  }

  /**
   * @return How often the degrader updates its state.
   */
  public long getUpdateIntervalMs()
  {
    return _updateIntervalMs;
  }

  /**
   * @return The amount of points to assign a 1.0 weight in the consistent hash ring.
   */
  public int getPointsPerWeight()
  {
    return _pointsPerWeight;
  }

  public long getMinClusterCallCountHighWaterMark()
  {
    return _minClusterCallCountHighWaterMark;
  }

  public String getHashMethod()
  {
    return _hashMethod;
  }

  public Map<String, Object> getHashConfig()
  {
    return _hashConfig;
  }

  public long getMinClusterCallCountLowWaterMark()
  {
    return _minClusterCallCountLowWaterMark;
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

  public boolean isUpdateOnlyAtInterval()
  {
    return _updateOnlyAtInterval;
  }

  public double getHashRingPointCleanUpRate()
  {
    return _hashRingPointCleanUpRate;
  }

  public String getConsistentHashAlgorithm()
  {
    return _consistentHashAlgorithm;
  }

  public int getNumProbes()
  {
    return _numProbes;
  }

  public int getPointsPerHost()
  {
    return _pointsPerHost;
  }

  public double getBoundedLoadBalancingFactor()
  {
    return _boundedLoadBalancingFactor;
  }

  public String getServicePath()
  {
    return _servicePath;
  }

  public double getQuarantineMaxPercent()
  {
    return _quarantineMaxPercent;
  }

  public ScheduledExecutorService getExecutorService()
  {
    return _executorService;
  }

  public HealthCheckOperations getHealthCheckOperations()
  {
    return _healthCheckOperations;
  }

  public String getHealthCheckMethod()
  {
    return _healthCheckMethod;
  }

  public String getHealthCheckPath()
  {
    return _healthCheckPath;
  }

  public long getQuarantineLatency()
  {
    return _quarantineLatency;
  }

  public EventEmitter getEventEmitter()
  {
    return _eventEmitter;
  }

  public long getLowEventEmittingInterval()
  {
    return _lowEventEmittingInterval;
  }

  public long getHighEventEmittingInterval()
  {
    return _highEventEmittingInterval;
  }

  public String getClusterName()
  {
    return _clusterName;
  }

  @Override
  public String toString()
  {
    return "DegraderLoadBalancerStrategyConfig [_highWaterMark=" + _highWaterMark
            + ", _lowWaterMark=" + _lowWaterMark + ", _initialRecoveryLevel=" + _initialRecoveryLevel
            + ", _ringRampFactor=" + _ringRampFactor + ", _globalStepUp=" + _globalStepUp
            + ", _globalStepDown=" + _globalStepDown + ", _pointsPerWeight=" + _pointsPerWeight
            + ", _boundedLoadBalancingFactor=" + _boundedLoadBalancingFactor + "]";
  }
}
