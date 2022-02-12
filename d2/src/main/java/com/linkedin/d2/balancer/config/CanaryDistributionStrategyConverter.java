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

package com.linkedin.d2.balancer.config;

import com.linkedin.d2.D2CanaryDistributionStrategy;
import com.linkedin.d2.PercentageStrategyProperties;
import com.linkedin.d2.StrategyType;
import com.linkedin.d2.TargetApplicationsStrategyProperties;
import com.linkedin.d2.TargetHostsStrategyProperties;
import com.linkedin.d2.balancer.properties.CanaryDistributionStrategy;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.data.template.StringArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.linkedin.d2.balancer.properties.util.PropertyUtil.coerce;


/**
 * This class converts between {@link D2CanaryDistributionStrategy} and {@link CanaryDistributionStrategy}.
 * {@link D2CanaryDistributionStrategy} is used in d2 client for running config canaries.
 * {@link CanaryDistributionStrategy} is to be stored in service registry.
 */
public class CanaryDistributionStrategyConverter
{

  private static final Logger LOG = LoggerFactory.getLogger(CanaryDistributionStrategyConverter.class);
  private static final Map<String, StrategyType> strategyTypes = new HashMap<>();

  private static final String STRATEGY_NAME_PERCENTAGE = "percentage";
  private static final String STRATEGY_NAME_TARGET_HOSTS = "targetHosts";
  private static final String STRATEGY_NAME_TARGET_APPLICATIONS = "targetApplications";
  private static final String STRATEGY_NAME_DISABLED = "disabled";

  static
  {
    strategyTypes.put(STRATEGY_NAME_PERCENTAGE, StrategyType.PERCENTAGE);
    strategyTypes.put(STRATEGY_NAME_TARGET_HOSTS, StrategyType.TARGET_HOSTS);
    strategyTypes.put(STRATEGY_NAME_TARGET_APPLICATIONS, StrategyType.TARGET_APPLICATIONS);
    strategyTypes.put(STRATEGY_NAME_DISABLED, StrategyType.DISABLED);
  }

  @SuppressWarnings("unchecked")
  public static D2CanaryDistributionStrategy toConfig(CanaryDistributionStrategy properties)
  {
    D2CanaryDistributionStrategy config = new D2CanaryDistributionStrategy();
    StrategyType type = strategyTypes.get(properties.getStrategy());
    if (type == null)
    {
      LOG.warn("Unknown strategy type from CanaryDistributionStrategy: " + properties.getStrategy() + ". Fall back to DISABLED.");
      type = StrategyType.DISABLED;
    }
    config.setStrategy(type);

    switch (type)
    {
      case PERCENTAGE:
        Map<String, Object> fromPercentageProperties = properties.getPercentageStrategyProperties();
        if (fromPercentageProperties == null || !fromPercentageProperties.containsKey(PropertyKeys.PERCENTAGE_SCOPE))
        {
          LOG.warn(
            "No properties set for percentage strategy in CanaryDistributionStrategy: " + fromPercentageProperties + ". Fall back to DISABLED.");
          config.setStrategy(StrategyType.DISABLED);
          break;
        }

        PercentageStrategyProperties toPercentageProperties = new PercentageStrategyProperties();
        toPercentageProperties.setScope(getValidScopeValue(fromPercentageProperties.get(PropertyKeys.PERCENTAGE_SCOPE)));
        config.setPercentageStrategyProperties(toPercentageProperties);
        break;
      case TARGET_HOSTS:
        Map<String, Object> fromTargetHostsProperties = properties.getTargetHostsStrategyProperties();
        if (fromTargetHostsProperties == null || !fromTargetHostsProperties.containsKey(PropertyKeys.TARGET_HOSTS))
        {
          LOG.warn(
            "No properties set for targetHosts strategy in CanaryDistributionStrategy: " + fromTargetHostsProperties + ". Fall back to DISABLED.");
          config.setStrategy(StrategyType.DISABLED);
          break;
        }

        List<String> hosts = (List<String>) fromTargetHostsProperties.get(PropertyKeys.TARGET_HOSTS);
        TargetHostsStrategyProperties toTargetHostsProperties = new TargetHostsStrategyProperties();
        toTargetHostsProperties.setTargetHosts(new StringArray(hosts));
        config.setTargetHostsStrategyProperties(toTargetHostsProperties);
        break;
      case TARGET_APPLICATIONS:
        Map<String, Object> fromTargetAppsProperties = properties.getTargetApplicationsStrategyProperties();
        if (fromTargetAppsProperties == null
          || !fromTargetAppsProperties.containsKey(PropertyKeys.TARGET_APPLICATIONS)
          || !fromTargetAppsProperties.containsKey(PropertyKeys.PERCENTAGE_SCOPE))
        {
          LOG.warn("No properties set for targetApplications strategy in CanaryDistributionStrategy: " + fromTargetAppsProperties
                     + ". Fall back to DISABLED.");
          config.setStrategy(StrategyType.DISABLED);
          break;
        }

        List<String> apps = (List<String>) fromTargetAppsProperties.get(PropertyKeys.TARGET_APPLICATIONS);
        TargetApplicationsStrategyProperties toTargetAppsProperties = new TargetApplicationsStrategyProperties();
        toTargetAppsProperties.setTargetApplications(new StringArray(apps));
        toTargetAppsProperties.setScope(getValidScopeValue(fromTargetAppsProperties.get(PropertyKeys.PERCENTAGE_SCOPE)));
        config.setTargetApplicationsStrategyProperties(toTargetAppsProperties);
        break;
      case DISABLED:
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
    return config;
  }

  public static CanaryDistributionStrategy toProperties(D2CanaryDistributionStrategy config)
  {
    Map<String, Object> percentageStrategyProperties = new HashMap<>();
    Map<String, Object> targetHostsStrategyProperties = new HashMap<>();
    Map<String, Object> targetApplicationsStrategyProperties = new HashMap<>();

    String strategyName;
    switch (config.getStrategy())
    {
      case PERCENTAGE:
        strategyName = STRATEGY_NAME_PERCENTAGE;
        if (config.hasPercentageStrategyProperties())
        {
          percentageStrategyProperties.put(PropertyKeys.PERCENTAGE_SCOPE, config.getPercentageStrategyProperties().getScope());
        }
        break;
      case TARGET_HOSTS:
        strategyName = STRATEGY_NAME_TARGET_HOSTS;
        if (config.hasTargetHostsStrategyProperties())
        {
          targetHostsStrategyProperties.put(PropertyKeys.TARGET_HOSTS, config.getTargetHostsStrategyProperties().getTargetHosts());
        }
        break;
      case TARGET_APPLICATIONS:
        strategyName = STRATEGY_NAME_TARGET_APPLICATIONS;
        if (config.hasTargetApplicationsStrategyProperties())
        {
          TargetApplicationsStrategyProperties configTargetApplicationProperties = config.getTargetApplicationsStrategyProperties();
          targetApplicationsStrategyProperties.put(PropertyKeys.TARGET_APPLICATIONS, configTargetApplicationProperties.getTargetApplications());
          targetApplicationsStrategyProperties.put(PropertyKeys.PERCENTAGE_SCOPE, configTargetApplicationProperties.getScope());
        }
        break;
      case DISABLED:
        strategyName = STRATEGY_NAME_DISABLED;
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + config.getStrategy());
    }

    return new CanaryDistributionStrategy(strategyName, percentageStrategyProperties, targetHostsStrategyProperties,
                                          targetApplicationsStrategyProperties);
  }

  private static Double getValidScopeValue(Object scope)
  {
    Double value;
    try
    {
      value = coerce(scope, Double.class);
      if (value < 0 || value >= 1)
      {
        LOG.warn("Invalid scope value: " + value + ". Use default value 0.");
        value = CanaryDistributionStrategy.DEFAULT_SCOPE;
      }
    }
    catch (Exception ex)
    {
      LOG.warn("Invalid scope value: " + scope.toString() + ". Use default value 0.", ex);
      value = CanaryDistributionStrategy.DEFAULT_SCOPE;
    }
    return value;
  }
}
