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

package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.HashConfig;
import com.linkedin.d2.HashMethod;
import com.linkedin.d2.HttpMethod;
import com.linkedin.d2.HttpStatusCodeRange;
import com.linkedin.d2.HttpStatusCodeRangeArray;
import com.linkedin.d2.balancer.config.RelativeStrategyPropertiesConverter;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.RandomHash;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.r2.message.Request;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Load balancer factory for {@link RelativeLoadBalancerStrategy}.
 */
public class RelativeLoadBalancerStrategyFactory implements LoadBalancerStrategyFactory<RelativeLoadBalancerStrategy>
{
  // Default load balancer property values
  private static final long DEFAULT_UPDATE_INTERVAL_MS = 5000L;
  ///// TODO It was 0.2 and 0.05 before???
  private static final double DEFAULT_UP_STEP = 0.2;
  private static final double DEFAULT_DOWN_STEP = 0.2;
  private static final double DEFAULT_RELATIVE_LATENCY_HIGH_THRESHOLD_FACTOR = 1.2;
  private static final double DEFAULT_RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR = 1.1;
  private static final double DEFAULT_HIGH_ERROR_RATE = 1.1;
  private static final double DEFAULT_LOW_ERROR_RATE = 1.1;
  private static final int DEFAULT_MIN_CALL_COUNT = 1;
  private static final double DEFAULT_INITIAL_HEALTH_SCORE = 1.0;
  private static final double DEFAULT_SLOW_START_THRESHOLD = 0.0;
  private static final HttpStatusCodeRangeArray DEFAULT_ERROR_STATUS_FILTER =
      new HttpStatusCodeRangeArray(new HttpStatusCodeRange().setLowerBound(500).setUpperBound(599));
  private static final long DEFAULT_EMITTING_INTERVAL_MS = 0L;
  private static final boolean DEFAULT_ENABLE_FAST_RECOVERY = false;
  // Default quarantine properties
  private static final double DEFAULT_QUARANTINE_MAX_PERCENT = 0.0;
  private static final HttpMethod DEFAULT_HTTP_METHOD = HttpMethod.OPTIONS;


  private final ScheduledExecutorService _executorService;
  private final HealthCheckOperations _healthCheckOperations;

  public RelativeLoadBalancerStrategyFactory(ScheduledExecutorService executorService, HealthCheckOperations healthCheckOperations)
  {
    _executorService = executorService;
    _healthCheckOperations = healthCheckOperations;
  }


  @Override
  public RelativeLoadBalancerStrategy newLoadBalancer(ServiceProperties serviceProperties)
  {
    D2RelativeStrategyProperties relativeStrategyProperties = serviceProperties.getRelativeStrategyProperties();
    relativeStrategyProperties = putDefaultValues(relativeStrategyProperties);

    return new RelativeLoadBalancerStrategy(getRelativeStateUpdater(relativeStrategyProperties,
                                            serviceProperties.getServiceName(), serviceProperties.getPath()),
                                            getClientSelector(relativeStrategyProperties));
  }

  private RelativeStateUpdater getRelativeStateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
      String serviceName, String servicePath)
  {
    QuarantineManager quarantineManager = getQuarantineManager(relativeStrategyProperties, serviceName, servicePath);
    return new RelativeStateUpdater(relativeStrategyProperties, quarantineManager, _executorService);
  }

  private ClientSelectorImpl getClientSelector(D2RelativeStrategyProperties relativeStrategyProperties)
  {
    return new ClientSelectorImpl(getRequestHashFunction(relativeStrategyProperties));
  }

  private QuarantineManager getQuarantineManager(D2RelativeStrategyProperties relativeStrategyProperties,
      String serviceName, String servicePath)
  {
    Clock clock = SystemClock.instance();
    return new QuarantineManager(serviceName, servicePath, _healthCheckOperations,
        relativeStrategyProperties.getQuarantineProperties(), relativeStrategyProperties.isEnableFastRecovery(),
        relativeStrategyProperties.getInitialHealthScore(), _executorService, clock,
        relativeStrategyProperties.getUpdateIntervalMs(), relativeStrategyProperties.getRelativeLatencyLowThresholdFactor());
  }

  private HashFunction<Request> getRequestHashFunction(D2RelativeStrategyProperties relativeStrategyProperties)
  {
    if (relativeStrategyProperties.hasRingProperties() && relativeStrategyProperties.getRingProperties().hasHashConfig()) {
      HashMethod hashMethod = relativeStrategyProperties.getRingProperties().getHashMethod();
      HashConfig hashConfig = relativeStrategyProperties.getRingProperties().getHashConfig();
      switch (hashMethod) {
        case URI_REGEX:
          return new URIRegexHash(RelativeStrategyPropertiesConverter.convertHashConfigToMap(hashConfig));
        case RANDOM:
        default:
          return new RandomHash();
      }
    }
    // Fall back to RandomHash if not specified
    return new RandomHash();
  }

  private D2RelativeStrategyProperties putDefaultValues(D2RelativeStrategyProperties properties)
  {
    properties.setUpStep(getOrDefault(properties.getUpStep(), DEFAULT_UP_STEP));
    properties.setDownStep(getOrDefault(properties.getDownStep(), DEFAULT_DOWN_STEP));
    properties.setHighErrorRate(getOrDefault(properties.getHighErrorRate(), DEFAULT_HIGH_ERROR_RATE));
    properties.setLowErrorRate(getOrDefault(properties.getLowErrorRate(), DEFAULT_LOW_ERROR_RATE));
    properties.setRelativeLatencyHighThresholdFactor(getOrDefault(properties.getRelativeLatencyHighThresholdFactor(), DEFAULT_RELATIVE_LATENCY_HIGH_THRESHOLD_FACTOR));
    properties.setRelativeLatencyLowThresholdFactor(getOrDefault(properties.getRelativeLatencyLowThresholdFactor(), DEFAULT_RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR));
    properties.setMinCallCount(getOrDefault(properties.getMinCallCount(), DEFAULT_MIN_CALL_COUNT));
    properties.setUpdateIntervalMs(getOrDefault(properties.getUpdateIntervalMs(), DEFAULT_UPDATE_INTERVAL_MS));
    properties.setInitialHealthScore(getOrDefault(properties.getInitialHealthScore(), DEFAULT_INITIAL_HEALTH_SCORE));
    properties.setSlowStartThreshold(getOrDefault(properties.getInitialHealthScore(), DEFAULT_SLOW_START_THRESHOLD));
    properties.setErrorStatusFilter(getOrDefault(properties.getErrorStatusFilter(), DEFAULT_ERROR_STATUS_FILTER));
    properties.setEmittingIntervalMs(getOrDefault(properties.getEmittingIntervalMs(), DEFAULT_EMITTING_INTERVAL_MS));
    properties.setEnableFastRecovery(getOrDefault(properties.isEnableFastRecovery(), DEFAULT_ENABLE_FAST_RECOVERY));

    D2QuarantineProperties quarantineProperties = properties.hasQuarantineProperties()
        ? properties.getQuarantineProperties() : new D2QuarantineProperties();
    quarantineProperties.setQuarantineMaxPercent(getOrDefault(quarantineProperties.getQuarantineMaxPercent(), DEFAULT_QUARANTINE_MAX_PERCENT));
    quarantineProperties.setHealthCheckMethod(getOrDefault(quarantineProperties.getHealthCheckMethod(), DEFAULT_HTTP_METHOD));
    properties.setQuarantineProperties(quarantineProperties);

    return properties;
  }

  private <R> R getOrDefault(R value, R defaultValue)
  {
    return value == null ? defaultValue : value;
  }
}
