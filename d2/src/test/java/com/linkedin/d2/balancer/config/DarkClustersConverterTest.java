/*
   Copyright (c) 2019 LinkedIn Corp.

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.linkedin.d2.D2TransportClientProperties;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.DarkClusterStrategyName;
import com.linkedin.d2.DarkClusterStrategyNameArray;
import com.linkedin.d2.balancer.properties.PropertyKeys;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.properties.ClusterProperties.DARK_CLUSTER_DEFAULT_MULTIPLIER;
import static com.linkedin.d2.balancer.properties.ClusterProperties.DARK_CLUSTER_DEFAULT_TARGET_RATE;
import static com.linkedin.d2.balancer.properties.ClusterProperties.DARK_CLUSTER_DEFAULT_MAX_REQUESTS_TO_BUFFER;
import static com.linkedin.d2.balancer.properties.ClusterProperties.DARK_CLUSTER_DEFAULT_BUFFERED_REQUEST_EXPIRY_IN_SECONDS;


public class DarkClustersConverterTest
{
  private static String DARK_CLUSTER_KEY = "foobar1dark";

  @DataProvider
  public Object[][] provideKeys()
  {
    return new Object[][] {
        new Object[] {true, new DarkClusterConfig()
            .setMultiplier(0.5f)
            .setDispatcherOutboundTargetRate(0)
            .setDispatcherMaxRequestsToBuffer(50)
            .setDispatcherBufferedRequestExpiryInSeconds(10)},
        // multiplier is default, the default will be filled in
        new Object[] {false, new DarkClusterConfig()},
        // test zeros
        new Object[] {true, new DarkClusterConfig()
            .setMultiplier(0.0f)
            .setDispatcherOutboundTargetRate(0)
            .setDispatcherMaxRequestsToBuffer(50)
            .setDispatcherBufferedRequestExpiryInSeconds(10)},
        // negative multiplier not allowed
        new Object[] {false, new DarkClusterConfig()
            .setMultiplier(-1.0f)
            .setDispatcherOutboundTargetRate(0)
            .setDispatcherMaxRequestsToBuffer(50)
            .setDispatcherBufferedRequestExpiryInSeconds(10)}
    };
  }

  @Test
  public void testDarkClustersConverterEmpty()
  {
    DarkClusterConfigMap configMap = new DarkClusterConfigMap();
    DarkClusterConfigMap resultConfigMap = DarkClustersConverter.toConfig(DarkClustersConverter.toProperties(configMap));
    Assert.assertEquals(resultConfigMap, configMap);
  }

  @Test(dataProvider = "provideKeys")
  public void testDarkClustersConverter(boolean successExpected, DarkClusterConfig darkClusterConfig)
  {
    DarkClusterConfigMap configMap = new DarkClusterConfigMap();
    configMap.put(DARK_CLUSTER_KEY, darkClusterConfig);
    try
    {
      Assert.assertEquals(DarkClustersConverter.toConfig(DarkClustersConverter.toProperties(configMap)), configMap);
    }
    catch (Exception | AssertionError e)
    {
      if (successExpected)
      {
        Assert.fail("expected success for conversion of: " + darkClusterConfig, e);
      }
    }
  }

  @Test
  public void testDarkClustersConverterDefaults()
  {
    DarkClusterConfigMap configMap = new DarkClusterConfigMap();
    DarkClusterConfig config = new DarkClusterConfig();
    configMap.put(DARK_CLUSTER_KEY, config);

    DarkClusterConfig resultConfig = DarkClustersConverter.toConfig(DarkClustersConverter.toProperties(configMap)).get(DARK_CLUSTER_KEY);
    Assert.assertEquals(resultConfig.getMultiplier(), DARK_CLUSTER_DEFAULT_MULTIPLIER);
    Assert.assertEquals(resultConfig.getDispatcherOutboundTargetRate(), DARK_CLUSTER_DEFAULT_TARGET_RATE);
    Assert.assertEquals((int)resultConfig.getDispatcherMaxRequestsToBuffer(), DARK_CLUSTER_DEFAULT_MAX_REQUESTS_TO_BUFFER);
    Assert.assertEquals((int)resultConfig.getDispatcherBufferedRequestExpiryInSeconds(), DARK_CLUSTER_DEFAULT_BUFFERED_REQUEST_EXPIRY_IN_SECONDS);
    Assert.assertEquals(resultConfig.getDarkClusterStrategyPrioritizedList().size(), 1, "default strategy list should be size 1");
    Assert.assertFalse(resultConfig.hasTransportClientProperties(), "default shouldn't have transportProperties");
  }

  @Test
  public void testEntriesInClusterConfig()
  {
    DarkClusterConfigMap configMap = new DarkClusterConfigMap();
    DarkClusterStrategyNameArray multiplierStrategyTypeArray = new DarkClusterStrategyNameArray();
    multiplierStrategyTypeArray.add(DarkClusterStrategyName.RELATIVE_TRAFFIC.RELATIVE_TRAFFIC);
    D2TransportClientProperties transportClientProperties = new D2TransportClientProperties()
      .setRequestTimeout(1000);
    DarkClusterConfig config = new DarkClusterConfig()
        .setDarkClusterStrategyPrioritizedList(multiplierStrategyTypeArray)
        .setTransportClientProperties(transportClientProperties);

    configMap.put(DARK_CLUSTER_KEY, config);

    DarkClusterConfigMap expectedConfigMap = new DarkClusterConfigMap();
    DarkClusterConfig expectedConfig = new DarkClusterConfig(config.data());
    expectedConfig.setMultiplier(0);
    expectedConfig.setDispatcherOutboundTargetRate(0);
    expectedConfig.setDispatcherMaxRequestsToBuffer(1);
    expectedConfig.setDispatcherBufferedRequestExpiryInSeconds(1);
    expectedConfigMap.put(DARK_CLUSTER_KEY, expectedConfig);
    DarkClusterConfigMap resultConfigMap = DarkClustersConverter.toConfig(DarkClustersConverter.toProperties(configMap));
    Assert.assertEquals(resultConfigMap, expectedConfigMap);
    // verify values are converted properly.
    DarkClusterConfig darkClusterConfig = resultConfigMap.get(DARK_CLUSTER_KEY);
    Assert.assertEquals(darkClusterConfig.getMultiplier(),DARK_CLUSTER_DEFAULT_MULTIPLIER, "unexpected multiplier");
    Assert.assertEquals(darkClusterConfig.getDarkClusterStrategyPrioritizedList().size(), 1, "there should be one strategy");
    Assert.assertEquals(darkClusterConfig.getDarkClusterStrategyPrioritizedList().get(0), DarkClusterStrategyName.RELATIVE_TRAFFIC,
                        "expected RELATIVE_TRAFFIC strategy");
    Assert.assertTrue(darkClusterConfig.hasTransportClientProperties());
    D2TransportClientProperties returnedTransportClientProperties = darkClusterConfig.getTransportClientProperties();
    Assert.assertNotNull(returnedTransportClientProperties);
    Assert.assertTrue(returnedTransportClientProperties.hasRequestTimeout());
    Assert.assertEquals(Objects.requireNonNull(returnedTransportClientProperties.getRequestTimeout()).longValue(),
                        1000, "expected 1000 request Timeout");

  }

  @Test
  public void testMultipleStrategies()
  {
    DarkClusterConfigMap configMap = new DarkClusterConfigMap();
    DarkClusterStrategyNameArray darkClusterStrategyNameArray = new DarkClusterStrategyNameArray();
    darkClusterStrategyNameArray.add(DarkClusterStrategyName.RELATIVE_TRAFFIC);
    darkClusterStrategyNameArray.add(DarkClusterStrategyName.CONSTANT_QPS);
    DarkClusterConfig config = new DarkClusterConfig()
      .setDarkClusterStrategyPrioritizedList(darkClusterStrategyNameArray);

    configMap.put(DARK_CLUSTER_KEY, config);

    // these are defaults that will be set if the fields are missing.
    config.setMultiplier(DARK_CLUSTER_DEFAULT_MULTIPLIER);
    DarkClusterConfigMap expectedConfigMap = new DarkClusterConfigMap();
    DarkClusterConfig expectedConfig = new DarkClusterConfig(config.data());
    expectedConfig.setMultiplier(0);
    expectedConfig.setDispatcherOutboundTargetRate(0);
    expectedConfig.setDispatcherMaxRequestsToBuffer(1);
    expectedConfig.setDispatcherBufferedRequestExpiryInSeconds(1);
    expectedConfigMap.put(DARK_CLUSTER_KEY, expectedConfig);
    DarkClusterConfigMap resultConfigMap = DarkClustersConverter.toConfig(DarkClustersConverter.toProperties(configMap));
    Assert.assertEquals(resultConfigMap, expectedConfigMap);
    Assert.assertEquals(resultConfigMap.get(DARK_CLUSTER_KEY).getDarkClusterStrategyPrioritizedList().get(0), DarkClusterStrategyName.RELATIVE_TRAFFIC,
                        "expected first strategy to be RELATIVE_TRAFFIC");
    Assert.assertEquals(resultConfigMap.get(DARK_CLUSTER_KEY).getDarkClusterStrategyPrioritizedList().get(1), DarkClusterStrategyName.CONSTANT_QPS,
                        "expected first strategy to be CONSTANT_QPS");
  }

  @Test
  public void testBadStrategies()
  {
    Map<String, Object> props = new HashMap<>();
    List<String> myStrategyList = new ArrayList<>();
    myStrategyList.add("RELATIVE_TRAFFIC");
    myStrategyList.add("BLAH_BLAH");

    Map<String, Object> darkClusterMap = new HashMap<>();
    darkClusterMap.put(PropertyKeys.DARK_CLUSTER_STRATEGY_LIST, myStrategyList);
    props.put(DARK_CLUSTER_KEY, darkClusterMap);
    DarkClusterConfigMap configMap = DarkClustersConverter.toConfig(props);
    DarkClusterStrategyNameArray strategyList = configMap.get(DARK_CLUSTER_KEY).getDarkClusterStrategyPrioritizedList();
    Assert.assertEquals(strategyList.get(0), DarkClusterStrategyName.RELATIVE_TRAFFIC, "first strategy should be RELATIVE_TRAFFIC");

    // the bad strategy BLAH_BLAH gets converted to unknown on access
    Assert.assertEquals(strategyList.get(1), DarkClusterStrategyName.$UNKNOWN, "second strategy should be unknown");
  }
}
