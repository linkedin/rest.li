/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.canary;

import com.linkedin.d2.D2CanaryDistributionStrategy;
import com.linkedin.d2.PercentageStrategyProperties;
import com.linkedin.d2.TargetApplicationsStrategyProperties;
import com.linkedin.d2.TargetHostsStrategyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Basic implementation of a canary distribution provider. This class distributes canary based on a distribution strategy,
 * with either of the following rules:
 * 1) match running hosts as one of the target host.
 * 2) match running service/application as one of the application, AND the hashing result falls into the ramp scope.
 * 3) hashing result falls into the ramp scope.
 * , where hashing result is the absolute value of the hash code of "serviceName+hostName".
 */
public class BasicCanaryDistributionProviderImpl implements CanaryDistributionProvider
{
  private static final Logger _log = LoggerFactory.getLogger(BasicCanaryDistributionProviderImpl.class);

  private final String _serviceName; // name of the running service/application
  private final String _hostName; // name of the running host

  public BasicCanaryDistributionProviderImpl(String serviceName, String hostName)
  {
    _serviceName = serviceName;
    _hostName = hostName;
  }

  @Override
  public Distribution distribute(D2CanaryDistributionStrategy strategy)
  {
    switch (strategy.getStrategy()) {
      case TARGET_HOSTS:
        return distributeByTargetHosts(strategy);
      case TARGET_APPLICATIONS:
        return distributeByTargetApplications(strategy);
      case PERCENTAGE:
        return distributeByPercentage(strategy);
      case DISABLED:
        return Distribution.STABLE;
      default:
        _log.warn("Invalid distribution strategy type: " + strategy.getStrategy().name());
        return Distribution.STABLE;
    }
  }

  protected Distribution distributeByTargetHosts(D2CanaryDistributionStrategy strategy)
  {
    TargetHostsStrategyProperties targetHostsProperties = strategy.getTargetHostsStrategyProperties();
    if (targetHostsProperties == null) {
      _log.warn("Empty target hosts properties in distribution strategy type.");
      return Distribution.STABLE;
    }
    return targetHostsProperties.getTargetHosts().stream().anyMatch(this::isHostMatch) ? Distribution.CANARY
        : Distribution.STABLE;
  }

  protected Distribution distributeByTargetApplications(D2CanaryDistributionStrategy strategy)
  {
    TargetApplicationsStrategyProperties targetAppsProperties = strategy.getTargetApplicationsStrategyProperties();
    if (targetAppsProperties == null) {
      _log.warn("Empty target applications properties in distribution strategy type.");
      return Distribution.STABLE;
    }
    return targetAppsProperties.getTargetApplications().stream().anyMatch(this::isServiceMatch) && isCanaryByRampScope(
        targetAppsProperties.getScope()) ? Distribution.CANARY : Distribution.STABLE;
  }

  protected Distribution distributeByPercentage(D2CanaryDistributionStrategy strategy)
  {
    PercentageStrategyProperties percentageProperties = strategy.getPercentageStrategyProperties();
    if (percentageProperties == null) {
      _log.warn("Empty percentage properties in distribution strategy type.");
      return Distribution.STABLE;
    }
    return isCanaryByRampScope(percentageProperties.getScope()) ? Distribution.CANARY : Distribution.STABLE;
  }

  protected String getServiceName()
  {
    return _serviceName == null ? "" : _serviceName;
  }

  protected String getHostName()
  {
    return _hostName == null ? "" : _hostName;
  }

  // For testing convenience
  protected String getHashKey() {
    return getServiceName() + getHostName();
  }

  protected int getHashResult()
  {
    return Math.abs(getHashKey().hashCode()); // Get absolute value of the hash code
  }

  protected boolean isServiceMatch(String target) {
    return getServiceName().equals(target);
  }

  protected boolean isHostMatch(String target) {
    return getHostName().equals(target);
  }

  protected boolean isCanaryByRampScope(Double scope)
  {
    // scope is guaranteed >= 0 and < 1, enforced by D2CanaryDistributionStrategy.
    // hash result mod by 100, and compare with the percentage (ramp scope)
    return scope > 0 && getHashResult() % 100 <= (int) (scope * 100);
  }
}
