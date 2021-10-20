package com.linkedin.d2.balancer.config;

import com.linkedin.d2.*;
import com.linkedin.d2.balancer.properties.CanaryDistributionStrategy;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.data.template.StringArray;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class CanaryDistributionStrategyConverterTest
{
  private static final Double SCOPE = 0.1;
  private static final D2CanaryDistributionStrategy DISABLED_CONFIG = new D2CanaryDistributionStrategy().setStrategy(StrategyType.DISABLED);
  private static final List<String> HOSTS = Arrays.asList("hostA", "hostB");
  private static final Map<String, Object> PERCENTAGE_PROPERTIES = new HashMap<>();
  private static final Map<String, Object> TARGET_HOSTS_PROPERTIES = new HashMap<>();
  private static final Map<String, Object> EMPTY_MAP = Collections.emptyMap();

  private static final TargetHostsStrategyProperties TARGET_HOSTS_FOR_CONFIG = new TargetHostsStrategyProperties();
  private static final D2CanaryDistributionStrategy TARGET_HOSTS_CONFIG = new D2CanaryDistributionStrategy()
    .setStrategy(StrategyType.TARGET_HOSTS)
    .setTargetHostsStrategyProperties(TARGET_HOSTS_FOR_CONFIG);

  static {
    PERCENTAGE_PROPERTIES.put(PropertyKeys.PERCENTAGE_SCOPE, SCOPE);
    TARGET_HOSTS_PROPERTIES.put(PropertyKeys.TARGET_HOSTS, HOSTS);
    TARGET_HOSTS_FOR_CONFIG.setTargetHosts(new StringArray(HOSTS));
  }

  @DataProvider(name = "distributionStrategyPropertiesAndConfigs")
  public Object[][] getDistributionStrategyPropertiesAndConfigs()
  {
    List<String> apps = Arrays.asList("appA", "appB");

    Map<String, Object> targetApplicationsProperties = new HashMap<>();
    targetApplicationsProperties.put(PropertyKeys.TARGET_APPLICATIONS, apps);
    targetApplicationsProperties.put(PropertyKeys.PERCENTAGE_SCOPE, SCOPE);

    PercentageStrategyProperties percentageForConfig = new PercentageStrategyProperties().setScope(SCOPE);
    TargetApplicationsStrategyProperties targetAppsForConfig = new TargetApplicationsStrategyProperties();
    targetAppsForConfig.setTargetApplications(new StringArray(apps));
    targetAppsForConfig.setScope(SCOPE);

    CanaryDistributionStrategy disabledProperties =
      new CanaryDistributionStrategy("disabled", EMPTY_MAP, EMPTY_MAP, EMPTY_MAP);

    return new Object[][]{
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
  public void testNormalCases(CanaryDistributionStrategy properties, D2CanaryDistributionStrategy config)
  {
    Assert.assertEquals(CanaryDistributionStrategyConverter.toConfig(properties), config, "toConfig failed");
    Assert.assertEquals(CanaryDistributionStrategyConverter.toProperties(config), properties, "toProperties failed");
  }

  @Test
  public void testEdgeCases() {
    CanaryDistributionStrategy emtpyPercentageProperties = new CanaryDistributionStrategy("percentage", EMPTY_MAP, EMPTY_MAP, EMPTY_MAP);
    CanaryDistributionStrategy emptyTargetHostsProperties = new CanaryDistributionStrategy(PropertyKeys.TARGET_HOSTS, EMPTY_MAP, EMPTY_MAP, EMPTY_MAP);
    CanaryDistributionStrategy emptyTargetApplicationsProperties = new CanaryDistributionStrategy(PropertyKeys.TARGET_APPLICATIONS, EMPTY_MAP, EMPTY_MAP, EMPTY_MAP);
    // empty properties will fall back to DISABLED
    Assert.assertEquals(CanaryDistributionStrategyConverter.toConfig(emtpyPercentageProperties), DISABLED_CONFIG);
    Assert.assertEquals(CanaryDistributionStrategyConverter.toConfig(emptyTargetHostsProperties), DISABLED_CONFIG);
    Assert.assertEquals(CanaryDistributionStrategyConverter.toConfig(emptyTargetApplicationsProperties), DISABLED_CONFIG);
    // multiple properties will only use the one specified in the strategy
    Assert.assertEquals(
      CanaryDistributionStrategyConverter.toConfig(new CanaryDistributionStrategy(PropertyKeys.TARGET_HOSTS, PERCENTAGE_PROPERTIES, TARGET_HOSTS_PROPERTIES, EMPTY_MAP))
      , TARGET_HOSTS_CONFIG
    );
  }
}
