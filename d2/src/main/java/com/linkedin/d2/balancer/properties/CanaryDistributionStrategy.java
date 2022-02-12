/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.properties;

import java.util.Map;


/**
 * Configuration for a D2 canary distribution strategy. Canaries are used to ramp new D2 configs
 * with a portion of clients before being fully deployed to all. This is in contrast to stable
 * configs that are verified to be correct, which are picked up by clients by default.
 */
public class CanaryDistributionStrategy
{
  private final String _strategy;
  private final Map<String, Object> _percentageStrategyProperties;
  private final Map<String, Object> _targetHostsStrategyProperties;
  private final Map<String, Object> _targetApplicationsStrategyProperties;

  public static final String DEFAULT_STRATEGY_LABEL = "disabled";
  public static final Double DEFAULT_SCOPE = (double) 0;

  public CanaryDistributionStrategy(String strategy,
                                    Map<String, Object> percentageStrategyProperties,
                                    Map<String, Object> targetHostsStrategyProperties,
                                    Map<String, Object> targetApplicationsStrategyProperties)
  {
    _strategy = strategy;
    _percentageStrategyProperties = percentageStrategyProperties;
    _targetHostsStrategyProperties = targetHostsStrategyProperties;
    _targetApplicationsStrategyProperties = targetApplicationsStrategyProperties;
  }

  public String getStrategy() {
    return _strategy;
  }

  public Map<String, Object> getPercentageStrategyProperties() {
    return _percentageStrategyProperties;
  }

  public Map<String, Object> getTargetHostsStrategyProperties() {
    return _targetHostsStrategyProperties;
  }

  public Map<String, Object> getTargetApplicationsStrategyProperties() {
    return _targetApplicationsStrategyProperties;
  }

  @Override
  public String toString() {
    return "CanaryDistributionStrategy [_strategy=" + _strategy
        + ", _percentageStrategyProperties=" + _percentageStrategyProperties
        + ", _targetHostsStrategyProperties=" + _targetHostsStrategyProperties
        + ", _targetApplicationsStrategyProperties=" + _targetApplicationsStrategyProperties
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _strategy.hashCode();
    result = prime * result + _percentageStrategyProperties.hashCode();
    result = prime * result + _targetHostsStrategyProperties.hashCode();
    result = prime * result + _targetApplicationsStrategyProperties.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    CanaryDistributionStrategy other = (CanaryDistributionStrategy) obj;
    if (!_strategy.equals(other.getStrategy()))
      return false;
    if (!_percentageStrategyProperties.equals(other.getPercentageStrategyProperties()))
      return false;
    if (!_targetHostsStrategyProperties.equals(other.getTargetHostsStrategyProperties()))
      return false;
    return _targetApplicationsStrategyProperties.equals(other.getTargetApplicationsStrategyProperties());
  }
}
