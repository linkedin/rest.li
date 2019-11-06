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

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.properties.ClusterProperties.DARK_CLUSTER_DEFAULT_MAX_RATE;
import static com.linkedin.d2.balancer.properties.ClusterProperties.DARK_CLUSTER_DEFAULT_MULTIPLIER;
import static com.linkedin.d2.balancer.properties.ClusterProperties.DARK_CLUSTER_DEFAULT_TARGET_RATE;


public class DarkClustersConverterTest
{
  private static String DARK_CLUSTER_KEY = "foobar1dark";

  @DataProvider
  public Object[][] provideKeys()
  {
    return new Object[][] {
        new Object[] {true, new DarkClusterConfig().setMultiplier(0.5f).setDispatcherOutboundTargetRate(0).setDispatcherOutboundMaxRate(1234566)},
        // multiplier is default, the default will be filled in
        new Object[] {false, new DarkClusterConfig().setDispatcherOutboundTargetRate(456).setDispatcherOutboundMaxRate(1234566)},
        // dynamic multiplier defaults, the default will be filled in
        new Object[] {false, new DarkClusterConfig().setMultiplier(0.5f)},
        // test zeros
        new Object[] {true, new DarkClusterConfig().setMultiplier(0.0f).setDispatcherOutboundTargetRate(0).setDispatcherOutboundMaxRate(0)},
        // negative multiplier not allowed
        new Object[] {false, new DarkClusterConfig().setMultiplier(-1.0f).setDispatcherOutboundTargetRate(0).setDispatcherOutboundMaxRate(1234566)},
        // netative target rate not allowed
        new Object[] {false, new DarkClusterConfig().setMultiplier(0.0f).setDispatcherOutboundTargetRate(-1).setDispatcherOutboundMaxRate(1234566)},
        // negative max rate not allowed
        new Object[] {false, new DarkClusterConfig().setMultiplier(1.0f).setDispatcherOutboundTargetRate(0).setDispatcherOutboundMaxRate(-1)},
        // maxRate should not be greater than OutboundTargetRate, multiplier is set.
        new Object[] {false, new DarkClusterConfig().setMultiplier(1.0f).setDispatcherOutboundTargetRate(500).setDispatcherOutboundMaxRate(400)},
        // maxRate should not be greater than OutboundTargetRate
        new Object[] {false, new DarkClusterConfig().setMultiplier(0.0f).setDispatcherOutboundTargetRate(500).setDispatcherOutboundMaxRate(400)}
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
    Assert.assertEquals((int)resultConfig.getDispatcherOutboundTargetRate(), DARK_CLUSTER_DEFAULT_TARGET_RATE);
    Assert.assertEquals((int)resultConfig.getDispatcherOutboundMaxRate(), DARK_CLUSTER_DEFAULT_MAX_RATE);
  }

  @Test
  public void testEntriesInClusterConfig()
  {
    DarkClusterConfigMap configMap = new DarkClusterConfigMap();
    DarkClusterConfig config = new DarkClusterConfig()
        .setDispatcherOutboundTargetRate(454)
        .setDispatcherOutboundMaxRate(1234566);
    config.data().put("blahblah", "random string");

    configMap.put(DARK_CLUSTER_KEY, config);

    DarkClusterConfigMap expectedConfigMap = new DarkClusterConfigMap();
    DarkClusterConfig expectedConfig = new DarkClusterConfig(config.data());
    expectedConfig.setMultiplier(0);
    expectedConfigMap.put(DARK_CLUSTER_KEY, expectedConfig);
    DarkClusterConfigMap resultConfigMap = DarkClustersConverter.toConfig(DarkClustersConverter.toProperties(configMap));
    Assert.assertEquals(resultConfigMap, expectedConfigMap);
    // random entries in the map are carried over because of the pass thru nature of dataMaps.
    Assert.assertTrue(resultConfigMap.get(DARK_CLUSTER_KEY).data().containsKey("blahblah"));
    // verify values are converted properly.
    Assert.assertEquals(resultConfigMap.get(DARK_CLUSTER_KEY).getMultiplier(),0.0f, "unexpected multiplier");
    Assert.assertEquals((int)resultConfigMap.get(DARK_CLUSTER_KEY).getDispatcherOutboundTargetRate(), 454, "unexpected target rate");
    Assert.assertEquals((int)resultConfigMap.get(DARK_CLUSTER_KEY).getDispatcherOutboundMaxRate(), 1234566, "unexpected maxRate");
  }
}
