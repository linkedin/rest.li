/*
   Copyright (c) 2016 LinkedIn Corp.

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

import com.linkedin.d2.ConsistentHashAlgorithm;
import com.linkedin.d2.D2RingProperties;
import com.linkedin.d2.HashMethod;
import com.linkedin.d2.balancer.util.hashing.MPConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.util.degrader.CallTracker;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * {@link RingFactory} decorator that delegates to the correct factory implementation
 * based on the provided properties.
 *
 * @author Ang Xu
 */
public class DelegatingRingFactory<T> implements RingFactory<T>
{
  public static final String POINT_BASED_CONSISTENT_HASH = "pointBased";
  public static final String MULTI_PROBE_CONSISTENT_HASH = "multiProbe";
  public static final String DISTRIBUTION_NON_HASH = "distributionBased";

  private static final Logger _log = LoggerFactory.getLogger(DelegatingRingFactory.class);

  private final RingFactory<T> _ringFactory;

  public DelegatingRingFactory(DegraderLoadBalancerStrategyConfig config)
  {
    this(toD2RingProperties(config));
  }

  public DelegatingRingFactory(D2RingProperties ringProperties)
  {
    RingFactory<T> factory;

    ConsistentHashAlgorithm consistentHashAlgorithm = ringProperties.getConsistentHashAlgorithm();
    HashMethod hashMethod = getOrDefault(ringProperties.getHashMethod(), HashMethod.RANDOM);
    int numProbes = getOrDefault(ringProperties.getNumberOfProbes(), DegraderLoadBalancerStrategyConfig.DEFAULT_NUM_PROBES);
    int numPointsPerHost = getOrDefault(ringProperties.getNumberOfPointsPerHost(), DegraderLoadBalancerStrategyConfig.DEFAULT_POINTS_PER_HOST);

    if (consistentHashAlgorithm == null)
    {
      // Choose the right algorithm if consistentHashAlgorithm is not specified
      if (isAffinityRoutingEnabled(hashMethod))
      {
        _log.info("URI Regex hash is specified, use multiProbe algorithm for consistent hashing");
        factory = new MPConsistentHashRingFactory<>(numProbes, numPointsPerHost);
      }
      else
      {
        _log.info("DistributionBased algorithm is used for consistent hashing");
        factory = new DistributionNonDiscreteRingFactory<>();
      }
    }
    else if (consistentHashAlgorithm == ConsistentHashAlgorithm.POINT_BASED)
    {
      double hashPointCleanupRate = getOrDefault(ringProperties.getHashRingPointCleanupRate(),
                                                 DegraderLoadBalancerStrategyConfig.DEFAULT_HASHRING_POINT_CLEANUP_RATE);
      factory = new PointBasedConsistentHashRingFactory<>(hashPointCleanupRate);
    }
    else if (consistentHashAlgorithm == ConsistentHashAlgorithm.MULTI_PROBE)
    {
      factory = new MPConsistentHashRingFactory<>(numProbes, numPointsPerHost);
    }
    else if (consistentHashAlgorithm == ConsistentHashAlgorithm.DISTRIBUTION_BASED) {
      if (isAffinityRoutingEnabled(hashMethod))
      {
        _log.warn("URI Regex hash is specified but distribution based ring is picked, falling back to multiProbe ring");
        factory = new MPConsistentHashRingFactory<>(numProbes, numPointsPerHost);
      }
      else
      {
        factory = new DistributionNonDiscreteRingFactory<>();
      }
    }
    else
    {
      _log.warn("Unknown consistent hash algorithm {}, falling back to multiprobe hash ring with default settings", consistentHashAlgorithm);
      factory = new MPConsistentHashRingFactory<>(MPConsistentHashRing.DEFAULT_NUM_PROBES, MPConsistentHashRing.DEFAULT_POINTS_PER_HOST);
    }

    double boundedLoadBalancingFactor = getOrDefault(ringProperties.getBoundedLoadBalancingFactor(),
                                                     DegraderLoadBalancerStrategyConfig.DEFAULT_BOUNDED_LOAD_BALANCING_FACTOR);
    if (boundedLoadBalancingFactor > 1) {
      factory = new BoundedLoadConsistentHashRingFactory<>(factory, boundedLoadBalancingFactor);
    }

    _ringFactory = factory;
  }

  @Override
  public Ring<T> createRing(Map<T, Integer> pointsMap) {
    return _ringFactory.createRing(pointsMap);
  }

  @Override
  public Ring<T> createRing(Map<T, Integer> pointsMap, Map<T, CallTracker> callTrackerMap) {
    return _ringFactory.createRing(pointsMap, callTrackerMap);
  }

  private boolean isAffinityRoutingEnabled(HashMethod hashMethod) {
    return hashMethod == HashMethod.URI_REGEX;
  }

  private static D2RingProperties toD2RingProperties(DegraderLoadBalancerStrategyConfig config)
  {
    D2RingProperties ringProperties = new D2RingProperties()
      .setNumberOfProbes(config.getNumProbes())
      .setNumberOfPointsPerHost(config.getPointsPerHost())
      .setBoundedLoadBalancingFactor(config.getBoundedLoadBalancingFactor());

    if (config.getConsistentHashAlgorithm() != null)
    {
      ringProperties.setConsistentHashAlgorithm(toConsistentHashAlgorithm(config.getConsistentHashAlgorithm()));
    }
    if (config.getHashMethod() != null)
    {
      ringProperties.setHashMethod(toHashMethod(config.getHashMethod()));
    }

    return ringProperties;
  }

  private static ConsistentHashAlgorithm toConsistentHashAlgorithm(String consistentHashAlgorithm)
  {
    switch (consistentHashAlgorithm)
    {
      case POINT_BASED_CONSISTENT_HASH:
        return ConsistentHashAlgorithm.POINT_BASED;
      case MULTI_PROBE_CONSISTENT_HASH:
        return ConsistentHashAlgorithm.MULTI_PROBE;
      default:
        return ConsistentHashAlgorithm.DISTRIBUTION_BASED;
    }
  }

  private static HashMethod toHashMethod(String hashMethod)
  {
    switch (hashMethod)
    {
      case DegraderLoadBalancerStrategyV3.HASH_METHOD_URI_REGEX:
        return HashMethod.URI_REGEX;
      default:
        return HashMethod.RANDOM;
    }
  }

  private <R> R getOrDefault(R value, R defaultValue)
  {
    return value == null ? defaultValue : value;
  }
}
