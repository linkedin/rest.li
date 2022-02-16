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

import com.google.common.collect.ImmutableMap;
import com.linkedin.d2.D2CanaryDistributionStrategy;
import com.linkedin.d2.PercentageStrategyProperties;
import com.linkedin.d2.StrategyType;
import com.linkedin.d2.TargetApplicationsStrategyProperties;
import com.linkedin.d2.TargetHostsStrategyProperties;
import com.linkedin.d2.balancer.properties.CanaryDistributionStrategy;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.data.template.StringArray;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Test the behavior of {@link CanaryDistributionStrategyConverter}
 */
public class CanaryDistributionStrategyConverterTest
{
  private static final Double SCOPE = 0.1;
  private static final D2CanaryDistributionStrategy DISABLED_CONFIG = new D2CanaryDistributionStrategy().setStrategy(StrategyType.DISABLED);
  private static final List<String> HOSTS = Arrays.asList("hostA", "hostB");
  private static final List<String> APPS = Arrays.asList("appA", "appB");
  private static final Map<String, Object> PERCENTAGE_PROPERTIES = new HashMap<>();
  private static final Map<String, Object> TARGET_HOSTS_PROPERTIES = new HashMap<>();
  private static final Map<String, Object> EMPTY_MAP = Collections.emptyMap();

  private static final TargetHostsStrategyProperties TARGET_HOSTS_FOR_CONFIG = new TargetHostsStrategyProperties();
  private static final D2CanaryDistributionStrategy TARGET_HOSTS_CONFIG = new D2CanaryDistributionStrategy()
    .setStrategy(StrategyType.TARGET_HOSTS)
    .setTargetHostsStrategyProperties(TARGET_HOSTS_FOR_CONFIG);

  static
  {
    PERCENTAGE_PROPERTIES.put(PropertyKeys.PERCENTAGE_SCOPE, SCOPE);
    TARGET_HOSTS_PROPERTIES.put(PropertyKeys.TARGET_HOSTS, HOSTS);
    TARGET_HOSTS_FOR_CONFIG.setTargetHosts(new StringArray(HOSTS));
  }

  /**
   * Return test objects with the structure:
   * {
   *   CanaryDistributionStrategy - input canary distribution strategy
   *   D2CanaryDistributionStrategy - Expected D2CanaryDistributionStrategy
   * }
   */
  @DataProvider(name = "distributionStrategyPropertiesAndConfigs")
  public Object[][] getDistributionStrategyPropertiesAndConfigs()
  {
    Map<String, Object> targetApplicationsProperties = new HashMap<>();
    targetApplicationsProperties.put(PropertyKeys.TARGET_APPLICATIONS, APPS);
    targetApplicationsProperties.put(PropertyKeys.PERCENTAGE_SCOPE, SCOPE);

    PercentageStrategyProperties percentageForConfig = new PercentageStrategyProperties().setScope(SCOPE);
    TargetApplicationsStrategyProperties targetAppsForConfig = new TargetApplicationsStrategyProperties();
    targetAppsForConfig.setTargetApplications(new StringArray(APPS));
    targetAppsForConfig.setScope(SCOPE);

    CanaryDistributionStrategy disabledProperties =
      new CanaryDistributionStrategy("disabled", EMPTY_MAP, EMPTY_MAP, EMPTY_MAP);

    return new Object[][]
      {
        {new CanaryDistributionStrategy("percentage", PERCENTAGE_PROPERTIES, EMPTY_MAP, EMPTY_MAP),
          new D2CanaryDistributionStrategy().setStrategy(StrategyType.PERCENTAGE).setPercentageStrategyProperties(percentageForConfig)
        },
        {new CanaryDistributionStrategy(PropertyKeys.TARGET_HOSTS, EMPTY_MAP, TARGET_HOSTS_PROPERTIES, EMPTY_MAP),
          new D2CanaryDistributionStrategy().setStrategy(StrategyType.TARGET_HOSTS).setTargetHostsStrategyProperties(TARGET_HOSTS_FOR_CONFIG)
        },
        {new CanaryDistributionStrategy(PropertyKeys.TARGET_APPLICATIONS, EMPTY_MAP, EMPTY_MAP, targetApplicationsProperties),
          new D2CanaryDistributionStrategy().setStrategy(StrategyType.TARGET_APPLICATIONS).setTargetApplicationsStrategyProperties(targetAppsForConfig)
        },
        {disabledProperties, DISABLED_CONFIG}
      };
  }

  @Test(dataProvider = "distributionStrategyPropertiesAndConfigs")
  public void testToConfigNormalCases(CanaryDistributionStrategy properties, D2CanaryDistributionStrategy config)
  {
    Assert.assertEquals(CanaryDistributionStrategyConverter.toConfig(properties), config, "toConfig failed");
    Assert.assertEquals(CanaryDistributionStrategyConverter.toProperties(config), properties, "toProperties failed");
  }

  /**
   * Return test objects with the structure:
   * {
   *   String - Strategy type,
   *   Map<String, Object> - Percentage properties map,
   *   Map<String, Object> - Target hosts properties map,
   *   Map<String, Object> - Target applications properties map,
   *   D2CanaryDistributionStrategy - Expected D2CanaryDistributionStrategy
   * }
   */
  @DataProvider(name = "getEdgeCasesDistributionPropertiesAndConfigs")
  public Object[][] getEdgeCasesDistributionPropertiesAndConfigs()
  {
    final D2CanaryDistributionStrategy defaultPercentageConfigs = new D2CanaryDistributionStrategy()
        .setStrategy(StrategyType.PERCENTAGE)
        .setPercentageStrategyProperties(new PercentageStrategyProperties().setScope(CanaryDistributionStrategy.DEFAULT_SCOPE));

    final TargetApplicationsStrategyProperties targetAppsWithDefaultScope = new TargetApplicationsStrategyProperties();
    targetAppsWithDefaultScope.setTargetApplications(new StringArray(APPS));
    targetAppsWithDefaultScope.setScope(CanaryDistributionStrategy.DEFAULT_SCOPE);
    final D2CanaryDistributionStrategy defaultTargetAppsConfigs = new D2CanaryDistributionStrategy().setStrategy(StrategyType.TARGET_APPLICATIONS)
        .setTargetApplicationsStrategyProperties(targetAppsWithDefaultScope);

    Map<String, Object> nullPercentageMap = new HashMap<>();
    nullPercentageMap.put(PropertyKeys.PERCENTAGE_SCOPE, null);
    nullPercentageMap.put(PropertyKeys.TARGET_APPLICATIONS, APPS);

    Map<String, Object> nullAppsMap = new HashMap<>();
    nullAppsMap.put(PropertyKeys.TARGET_APPLICATIONS, null);
    nullAppsMap.put(PropertyKeys.PERCENTAGE_SCOPE, 0.3);

    Map<String, Object> nullHostsMap = new HashMap<>();
    nullHostsMap.put(PropertyKeys.TARGET_HOSTS, null);

    return new Object[][] {
        // unknown strategy type will fall back DISABLED
        {"2343xscjfi", EMPTY_MAP, EMPTY_MAP, EMPTY_MAP, DISABLED_CONFIG},
        // empty properties will fall back to DISABLED
        {"percentage", EMPTY_MAP, EMPTY_MAP, EMPTY_MAP, DISABLED_CONFIG},
        {PropertyKeys.TARGET_HOSTS, EMPTY_MAP, EMPTY_MAP, EMPTY_MAP, DISABLED_CONFIG},
        {PropertyKeys.TARGET_APPLICATIONS, EMPTY_MAP, EMPTY_MAP, EMPTY_MAP, DISABLED_CONFIG},
        // multiple properties will only use the one specified in the strategy
        {PropertyKeys.TARGET_HOSTS, PERCENTAGE_PROPERTIES, TARGET_HOSTS_PROPERTIES, EMPTY_MAP, TARGET_HOSTS_CONFIG},

        ///// Invalid Property Types /////
        // percentage strategy with invalid property types will fall back to DISABLED
        {"percentage", nullPercentageMap, EMPTY_MAP, EMPTY_MAP, DISABLED_CONFIG}, // scope is null
        {"percentage", ImmutableMap.of(PropertyKeys.PERCENTAGE_SCOPE, "3xr9"), EMPTY_MAP, EMPTY_MAP, DISABLED_CONFIG}, // non-numeric scope
        // target hosts strategy with invalid property types will fall back to DISABLED
        {PropertyKeys.TARGET_HOSTS, EMPTY_MAP, nullHostsMap, EMPTY_MAP, DISABLED_CONFIG}, // null hosts
        {PropertyKeys.TARGET_HOSTS, EMPTY_MAP, ImmutableMap.of(PropertyKeys.TARGET_HOSTS, "erwf"), EMPTY_MAP, DISABLED_CONFIG}, // hosts non list
        {PropertyKeys.TARGET_HOSTS, EMPTY_MAP, ImmutableMap.of(PropertyKeys.TARGET_HOSTS, Arrays.asList("erwf", 3)), EMPTY_MAP, DISABLED_CONFIG}, // hosts list has invalid value type
        // target apps strategy with invalid property types will fall back to DISABLED
        {PropertyKeys.TARGET_APPLICATIONS, EMPTY_MAP, EMPTY_MAP, nullAppsMap, DISABLED_CONFIG}, // null apps
        {PropertyKeys.TARGET_APPLICATIONS, EMPTY_MAP, EMPTY_MAP, ImmutableMap.of(PropertyKeys.TARGET_APPLICATIONS, 3), DISABLED_CONFIG}, // apps non list
        {PropertyKeys.TARGET_APPLICATIONS, EMPTY_MAP, EMPTY_MAP, nullPercentageMap, DISABLED_CONFIG}, // scope is null
        {PropertyKeys.TARGET_APPLICATIONS, EMPTY_MAP, EMPTY_MAP, ImmutableMap.of(PropertyKeys.TARGET_APPLICATIONS, APPS,
                                                                                  PropertyKeys.PERCENTAGE_SCOPE, "9ejo"), DISABLED_CONFIG}, // non-numeric scope

        ///// Invalid Property Values /////
        // percentage strategy with invalid scope value will use default value
        {"percentage", ImmutableMap.of(PropertyKeys.PERCENTAGE_SCOPE, -1), EMPTY_MAP, EMPTY_MAP, defaultPercentageConfigs},
        {"percentage", ImmutableMap.of(PropertyKeys.PERCENTAGE_SCOPE, 1), EMPTY_MAP, EMPTY_MAP, defaultPercentageConfigs}, // scope >= 1
        // target apps strategy with invalid scope value will use default value
        {PropertyKeys.TARGET_APPLICATIONS, EMPTY_MAP, EMPTY_MAP, ImmutableMap.of(PropertyKeys.TARGET_APPLICATIONS, APPS,
                                                                                  PropertyKeys.PERCENTAGE_SCOPE, 5), defaultTargetAppsConfigs} // scope >= 1
    };
  }
  @Test(dataProvider = "getEdgeCasesDistributionPropertiesAndConfigs")
  public void testToConfigEdgeCases(String strategyType, Map<String, Object> percentageProperties, Map<String, Object> targetHostsProperties,
                                Map<String, Object> targetAppsProperties, D2CanaryDistributionStrategy expected)
  {
    CanaryDistributionStrategy input = new CanaryDistributionStrategy(strategyType, percentageProperties, targetHostsProperties, targetAppsProperties);
    Assert.assertEquals(CanaryDistributionStrategyConverter.toConfig(input), expected);
  }
}
