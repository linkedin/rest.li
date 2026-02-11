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

package com.linkedin.d2.balancer.config;

import com.linkedin.d2.ClientType;
import com.linkedin.d2.ClientTypeArray;
import com.linkedin.d2.HashFunction;
import com.linkedin.d2.LeastRequestLbConfig;
import com.linkedin.d2.LoadBalancingPolicy;
import com.linkedin.d2.LoadBalancingPolicyArray;
import com.linkedin.d2.LoadBalancingPolicyConfig;
import com.linkedin.d2.RingHashLbConfig;
import com.linkedin.d2.RoundRobinLbConfig;
import com.linkedin.d2.SlowStartProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


public class LoadBalancingPoliciesConverterTest
{
  @Test
  public void testEmptyPolicies()
  {
    LoadBalancingPolicyArray empty = new LoadBalancingPolicyArray();
    List<Map<String, Object>> props = LoadBalancingPoliciesConverter.toProperties(empty);
    Assert.assertNotNull(props);
    Assert.assertTrue(props.isEmpty());

    LoadBalancingPolicyArray roundTrip = LoadBalancingPoliciesConverter.toConfig(props);
    Assert.assertNotNull(roundTrip);
    Assert.assertTrue(roundTrip.isEmpty());
  }

  @Test
  public void testNullPolicies()
  {
    Assert.assertEquals(LoadBalancingPoliciesConverter.toProperties(null), Collections.emptyList());
    LoadBalancingPolicyArray empty = LoadBalancingPoliciesConverter.toConfig(null);
    Assert.assertNotNull(empty);
    Assert.assertTrue(empty.isEmpty());
  }

  @Test
  public void testRingHashLbConfigRoundTrip()
  {
    RingHashLbConfig ringConfig = new RingHashLbConfig()
        .setMinimum_ring_size(100)
        .setMaximum_ring_size(1024)
        .setHash_function(HashFunction.MURMUR_HASH_2);

    LoadBalancingPolicy policy = new LoadBalancingPolicy()
        .setConfig(LoadBalancingPolicyConfig.createWithRingHashLbConfig(ringConfig));

    ClientTypeArray clientTypes = new ClientTypeArray();
    clientTypes.add(ClientType.GRPC);
    clientTypes.add(ClientType.ENVOY);
    policy.setClientTypes(clientTypes);

    LoadBalancingPolicyArray array = new LoadBalancingPolicyArray();
    array.add(policy);

    List<Map<String, Object>> props = LoadBalancingPoliciesConverter.toProperties(array);
    Assert.assertEquals(props.size(), 1);
    Map<String, Object> policyMap = props.get(0);
    Assert.assertTrue(policyMap.containsKey("config"));
    Assert.assertTrue(policyMap.containsKey("clientTypes"));
    @SuppressWarnings("unchecked")
    Map<String, Object> configMap = (Map<String, Object>) policyMap.get("config");
    Assert.assertTrue(configMap.containsKey("ringHashLbConfig"));
    @SuppressWarnings("unchecked")
    Map<String, Object> ringMap = (Map<String, Object>) configMap.get("ringHashLbConfig");
    Assert.assertEquals(ringMap.get("minimum_ring_size"), 100);
    Assert.assertEquals(ringMap.get("maximum_ring_size"), 1024);
    Assert.assertEquals(ringMap.get("hash_function"), "MURMUR_HASH_2");
    @SuppressWarnings("unchecked")
    List<String> ctList = (List<String>) policyMap.get("clientTypes");
    Assert.assertEquals(ctList.size(), 2);
    Assert.assertTrue(ctList.contains("GRPC"));
    Assert.assertTrue(ctList.contains("ENVOY"));

    LoadBalancingPolicyArray roundTrip = LoadBalancingPoliciesConverter.toConfig(props);
    Assert.assertEquals(roundTrip.size(), 1);
    LoadBalancingPolicy roundTripPolicy = roundTrip.get(0);
    Assert.assertTrue(roundTripPolicy.getConfig().isRingHashLbConfig());
    RingHashLbConfig roundTripRing = roundTripPolicy.getConfig().getRingHashLbConfig();
    Assert.assertEquals(roundTripRing.getMinimum_ring_size(), Integer.valueOf(100));
    Assert.assertEquals(roundTripRing.getMaximum_ring_size(), Integer.valueOf(1024));
    Assert.assertEquals(roundTripRing.getHash_function(), HashFunction.MURMUR_HASH_2);
    Assert.assertEquals(roundTripPolicy.getClientTypes().size(), 2);
  }

  @Test
  public void testRoundRobinLbConfigRoundTrip()
  {
    RoundRobinLbConfig roundRobinConfig = new RoundRobinLbConfig();
    LoadBalancingPolicy policy = new LoadBalancingPolicy()
        .setConfig(LoadBalancingPolicyConfig.createWithRoundRobinLbConfig(roundRobinConfig));

    LoadBalancingPolicyArray array = new LoadBalancingPolicyArray();
    array.add(policy);

    List<Map<String, Object>> props = LoadBalancingPoliciesConverter.toProperties(array);
    LoadBalancingPolicyArray roundTrip = LoadBalancingPoliciesConverter.toConfig(props);
    Assert.assertEquals(roundTrip.size(), 1);
    Assert.assertTrue(roundTrip.get(0).getConfig().isRoundRobinLbConfig());
  }

  @Test
  public void testLeastRequestLbConfigRoundTrip()
  {
    SlowStartProperties slowStart = new SlowStartProperties()
        .setDisabled(false)
        .setWindowDurationSeconds(60)
        .setAggression(1.5)
        .setMinWeightPercent(0.1);

    LeastRequestLbConfig leastRequestConfig = new LeastRequestLbConfig()
        .setChoice_count(3)
        .setActive_request_bias(1.0)
        .setSlow_start_config(slowStart);

    LoadBalancingPolicy policy = new LoadBalancingPolicy()
        .setConfig(LoadBalancingPolicyConfig.createWithLeastRequestLbConfig(leastRequestConfig));
    policy.setClientTypes(new ClientTypeArray());

    LoadBalancingPolicyArray array = new LoadBalancingPolicyArray();
    array.add(policy);

    List<Map<String, Object>> props = LoadBalancingPoliciesConverter.toProperties(array);
    LoadBalancingPolicyArray roundTrip = LoadBalancingPoliciesConverter.toConfig(props);
    Assert.assertEquals(roundTrip.size(), 1);
    Assert.assertTrue(roundTrip.get(0).getConfig().isLeastRequestLbConfig());
    LeastRequestLbConfig roundTripLr = roundTrip.get(0).getConfig().getLeastRequestLbConfig();
    Assert.assertEquals(roundTripLr.getChoice_count(), Integer.valueOf(3));
    Assert.assertEquals(roundTripLr.getActive_request_bias(), Double.valueOf(1.0));
    Assert.assertTrue(roundTripLr.hasSlow_start_config());
    SlowStartProperties roundTripSlow = roundTripLr.getSlow_start_config();
    Assert.assertFalse(roundTripSlow.isDisabled());
    Assert.assertEquals(roundTripSlow.getWindowDurationSeconds(), Integer.valueOf(60));
  }

  @Test
  public void testMapToConfigRoundTrip()
  {
    Map<String, Object> ringConfigMap = new HashMap<>();
    ringConfigMap.put("minimum_ring_size", 64);
    ringConfigMap.put("maximum_ring_size", 512);
    ringConfigMap.put("hash_function", "XX_HASH");

    Map<String, Object> configMap = new HashMap<>();
    configMap.put("ringHashLbConfig", ringConfigMap);

    Map<String, Object> policyMap = new HashMap<>();
    policyMap.put("config", configMap);
    policyMap.put("clientTypes", Collections.singletonList("D2"));

    List<Map<String, Object>> props = new ArrayList<>();
    props.add(policyMap);

    LoadBalancingPolicyArray array = LoadBalancingPoliciesConverter.toConfig(props);
    Assert.assertEquals(array.size(), 1);
    LoadBalancingPolicy policy = array.get(0);
    Assert.assertTrue(policy.getConfig().isRingHashLbConfig());
    RingHashLbConfig ring = policy.getConfig().getRingHashLbConfig();
    Assert.assertEquals(ring.getMinimum_ring_size(), Integer.valueOf(64));
    Assert.assertEquals(ring.getMaximum_ring_size(), Integer.valueOf(512));
    Assert.assertEquals(ring.getHash_function(), HashFunction.XX_HASH);
    Assert.assertEquals(policy.getClientTypes().size(), 1);
    Assert.assertEquals(policy.getClientTypes().get(0), ClientType.D2);

    List<Map<String, Object>> backToProps = LoadBalancingPoliciesConverter.toProperties(array);
    Assert.assertEquals(backToProps.size(), 1);
    Assert.assertEquals(backToProps.get(0).get("clientTypes"), Collections.singletonList("D2"));
  }
}
