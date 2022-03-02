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

package com.linkedin.d2.balancer.properties;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.config.DarkClustersConverter;
import com.linkedin.d2.discovery.PropertySerializationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ClusterPropertiesSerializerTest
{
  private static String DARK_CLUSTER1_KEY = "foobar1dark";
  private static String DARK_CLUSTER2_KEY = "foobar2dark";

  private static final DarkClusterConfigMap DARK_CLUSTER_CONFIG_MAP = new DarkClusterConfigMap();

  static {
    DarkClusterConfig darkCluster1 = new DarkClusterConfig()
      .setMultiplier(1.5f)
      .setDispatcherBufferedRequestExpiryInSeconds(10)
      .setDispatcherMaxRequestsToBuffer(100)
      .setDispatcherOutboundTargetRate(50);
    DARK_CLUSTER_CONFIG_MAP.put(DARK_CLUSTER1_KEY, darkCluster1);
  }

  public static void main(String[] args) throws PropertySerializationException
  {
    new ClusterPropertiesSerializerTest().testClusterPropertiesSerializer();
  }

  @Test(groups = { "small", "back-end" })
  public void testClusterPropertiesSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer jsonSerializer = new ClusterPropertiesJsonSerializer();
    List<String> schemes = new ArrayList<>();
    Map<String, String> supProperties = new HashMap<>();
    Set<URI> bannedSet = new HashSet<>();
    bannedSet.add(URI.create("https://test1.linkedin.com:12345/test"));
    bannedSet.add(URI.create("https://test2.linkedin.com:56789/test"));


    ClusterProperties property = new ClusterProperties("test");
    ClusterStoreProperties storeProperties = new ClusterStoreProperties(property, null, null);
    // cluster properties will be serialized then deserialized as cluster store properties
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), storeProperties);
    // cluster store properties will be serialized then deserialized as cluster store properties
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(storeProperties)), storeProperties);

    property = new ClusterProperties("test", schemes);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));

    supProperties.put("foo", "bar");
    property = new ClusterProperties("test", schemes, supProperties);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));

    property = new ClusterProperties("test", schemes, null);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));


    RangeBasedPartitionProperties rbp = new RangeBasedPartitionProperties("blah", 0, 5000000, 100);
    property = new ClusterProperties("test", schemes, supProperties, bannedSet, rbp);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));

    HashBasedPartitionProperties hbp = new HashBasedPartitionProperties("blah", 150, HashBasedPartitionProperties.HashAlgorithm.valueOf("md5".toUpperCase()));
    property = new ClusterProperties("test", schemes, supProperties, bannedSet, hbp);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));

    property = new ClusterProperties("test", schemes, supProperties, new HashSet<>(), NullPartitionProperties.getInstance(),
        Arrays.asList("principal1", "principal2"), (Map<String, Object>)null, false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));

    try
    {
      new HashBasedPartitionProperties("blah", 150, HashBasedPartitionProperties.HashAlgorithm.valueOf("sha-256"));
      fail("Should throw exception for unsupported algorithms");
    }
    catch(IllegalArgumentException e){}
  }

  @Test
  public void testDarkClusterJsonSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer jsonSerializer = new ClusterPropertiesJsonSerializer();

    ClusterProperties property = new ClusterProperties("test", new ArrayList<>(), Collections.emptyMap(), new HashSet<>(), NullPartitionProperties.getInstance(),
                                                       Arrays.asList("principal1", "principal2"), DarkClustersConverter.toProperties(
      DARK_CLUSTER_CONFIG_MAP), false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));
  }

  @Test
  public void test2DarkClusterJsonSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer jsonSerializer = new ClusterPropertiesJsonSerializer();

    DarkClusterConfig darkCluster1 = new DarkClusterConfig()
        .setMultiplier(1.5f)
        .setDispatcherBufferedRequestExpiryInSeconds(10)
        .setDispatcherMaxRequestsToBuffer(100)
        .setDispatcherOutboundTargetRate(50);
    DarkClusterConfigMap darkClusterConfigMap = new DarkClusterConfigMap();
    darkClusterConfigMap.put(DARK_CLUSTER1_KEY, darkCluster1);
    DarkClusterConfig darkCluster2 = new DarkClusterConfig()
        .setDispatcherBufferedRequestExpiryInSeconds(10)
        .setDispatcherMaxRequestsToBuffer(100)
        .setDispatcherOutboundTargetRate(50)
        .setMultiplier(0);
    darkClusterConfigMap.put(DARK_CLUSTER2_KEY, darkCluster2);
    ClusterProperties property = new ClusterProperties("test", new ArrayList<>(), new HashMap<>(), new HashSet<>(),
                                                       NullPartitionProperties.getInstance(),
                                                       Arrays.asList("principal1", "principal2"),
                                                       DarkClustersConverter.toProperties(darkClusterConfigMap), false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));
  }

  @Test
  public void testEmptyDarkClusterJsonSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer jsonSerializer = new ClusterPropertiesJsonSerializer();


    DarkClusterConfigMap darkClusterConfigMap = new DarkClusterConfigMap();
    ClusterProperties property = new ClusterProperties("test", new ArrayList<>(), new HashMap<>(), new HashSet<>(),
                                                       NullPartitionProperties.getInstance(),
                                                       Arrays.asList("principal1", "principal2"),
                                                       DarkClustersConverter.toProperties(darkClusterConfigMap), false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));
  }

  @Test
  public void testNullDarkClusterJsonSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer jsonSerializer = new ClusterPropertiesJsonSerializer();
    ClusterProperties property = new ClusterProperties("test", new ArrayList<>(), new HashMap<>(), new HashSet<>(), NullPartitionProperties.getInstance(),
                                                       Arrays.asList("principal1", "principal2"),
                                                       (Map<String, Object>) null, false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), new ClusterStoreProperties(property, null, null));
  }

  @DataProvider(name = "distributionStrategies")
  public Object[][] getDistributionStrategies() {
    Map<String, Object> percentageProperties = new HashMap<>();
    percentageProperties.put("scope", 0.1);

    Map<String, Object> targetHostsProperties = new HashMap<>();
    targetHostsProperties.put("targetHosts", Arrays.asList("hostA", "hostB"));

    Map<String, Object> targetApplicationsProperties = new HashMap<>();
    targetApplicationsProperties.put("targetApplications", Arrays.asList("appA", "appB"));
    targetApplicationsProperties.put("scope", 0.1);

    return new Object[][] {
      {new CanaryDistributionStrategy("percentage", percentageProperties, Collections.emptyMap(), Collections.emptyMap())},
      {new CanaryDistributionStrategy("targetHosts", Collections.emptyMap(), targetHostsProperties, Collections.emptyMap())},
      {new CanaryDistributionStrategy("targetApplications", Collections.emptyMap(), Collections.emptyMap(), targetApplicationsProperties)}
    };
  }

  @Test(dataProvider = "distributionStrategies")
  public void testClusterPropertiesWithCanary(CanaryDistributionStrategy distributionStrategy) throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer serializer = new ClusterPropertiesJsonSerializer();

    // canary configs adds dark cluster  properties
    ClusterProperties canaryProperty = new ClusterProperties("test", Collections.emptyList(), Collections.emptyMap(), Collections.emptySet(),
                                                             NullPartitionProperties.getInstance(), Arrays.asList("principal1", "principal2"),
                                                             DarkClustersConverter.toProperties(DARK_CLUSTER_CONFIG_MAP), false);

    ClusterStoreProperties property = new ClusterStoreProperties("test", Collections.emptyList(), Collections.emptyMap(), Collections.emptySet(),
                                                                NullPartitionProperties.getInstance(), Collections.emptyList(),
                                                                (Map<String, Object>) null, false, canaryProperty, distributionStrategy);

    assertEquals(serializer.fromBytes(serializer.toBytes(property)), property);
  }

  @Test
  public void testClusterPropertiesWithCanaryEdgeCases()  throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer serializer = new ClusterPropertiesJsonSerializer();

    ClusterProperties property = new ClusterProperties("test");
    ClusterStoreProperties expected = new ClusterStoreProperties(property, null, null);
    // having canary configs but missing distribution strategy will not be taken in.
    ClusterStoreProperties inputProperty = new ClusterStoreProperties(property, property, null);
    assertEquals(serializer.fromBytes(serializer.toBytes(inputProperty)), expected);

    // having distribution strategy but missing canary configs will not be taken in.
    inputProperty = new ClusterStoreProperties(property, null, new CanaryDistributionStrategy("percentage",
        Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
    assertEquals(serializer.fromBytes(serializer.toBytes(inputProperty)), expected);
  }

  @DataProvider(name = "clusterFailoutProperties")
  public Object[][] getClusterFailoutProperties() {

    Map<String, Object> clusterFailoutRedirectConfigs = new HashMap<>();
    clusterFailoutRedirectConfigs.put("fabric", "testfabric");
    clusterFailoutRedirectConfigs.put("weight", 1);

    Map<String, Object> clusterFailoutBucketConfigs = new HashMap<>();
    clusterFailoutRedirectConfigs.put("fabric", "testfabric");
    clusterFailoutBucketConfigs.put("partition", "main");

    List<Map<String, Object>> clusterFailoutRedirectConfigsList =  new ArrayList<Map<String, Object>>();
    clusterFailoutRedirectConfigsList.add(clusterFailoutRedirectConfigs);
    List<Map<String, Object>> clusterFailoutBucketConfigsList =  new ArrayList<Map<String, Object>>();
    clusterFailoutBucketConfigsList.add(clusterFailoutBucketConfigs);
    List<Map<String, Object>> emptyList =  new ArrayList<Map<String, Object>>();
    emptyList.add(Collections.emptyMap());

    return new Object[][]{
        {new ClusterFailoutProperties(clusterFailoutRedirectConfigsList, clusterFailoutBucketConfigsList)},
        {new ClusterFailoutProperties(clusterFailoutRedirectConfigsList, emptyList)},
        {new ClusterFailoutProperties(emptyList, clusterFailoutBucketConfigsList)},
        {new ClusterFailoutProperties(emptyList, emptyList)},
        {new ClusterFailoutProperties(null, null)}};
  };

  @Test(dataProvider = "clusterFailoutProperties")
  public void testClusterFailoutProperties(ClusterFailoutProperties clusterFailoutProperties) throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer serializer = new ClusterPropertiesJsonSerializer();

    // canary configs adds dark cluster  properties
    ClusterProperties canaryProperty = new ClusterProperties("test", Collections.emptyList(), Collections.emptyMap(), Collections.emptySet(),
        NullPartitionProperties.getInstance(), Arrays.asList("principal1", "principal2"),
        DarkClustersConverter.toProperties(DARK_CLUSTER_CONFIG_MAP), false);

    ClusterStoreProperties property = new ClusterStoreProperties("test", Collections.emptyList(), Collections.emptyMap(), Collections.emptySet(),
        NullPartitionProperties.getInstance(), Collections.emptyList(),
        (Map<String, Object>) null, false, canaryProperty,
        new CanaryDistributionStrategy("distributionStrategy", Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()),
        clusterFailoutProperties);

    assertEquals(serializer.fromBytes(serializer.toBytes(property)), property);
  }
}
