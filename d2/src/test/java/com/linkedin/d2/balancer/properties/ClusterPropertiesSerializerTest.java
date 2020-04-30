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

  public static void main(String[] args) throws PropertySerializationException
  {
    new ClusterPropertiesSerializerTest().testClusterPropertiesSerializer();
  }

  @Test(groups = { "small", "back-end" })
  public void testClusterPropertiesSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer jsonSerializer = new ClusterPropertiesJsonSerializer();
    List<String> schemes = new ArrayList<String>();
    Map<String, String> supProperties = new HashMap<String, String>();
    Set<URI> bannedSet = new HashSet<>();
    bannedSet.add(URI.create("https://test1.linkedin.com:12345/test"));
    bannedSet.add(URI.create("https://test2.linkedin.com:56789/test"));


    ClusterProperties property = new ClusterProperties("test");
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

    property = new ClusterProperties("test", schemes);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

    supProperties.put("foo", "bar");
    property = new ClusterProperties("test", schemes, supProperties);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

    property = new ClusterProperties("test", schemes, null);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);


    RangeBasedPartitionProperties rbp = new RangeBasedPartitionProperties("blah", 0, 5000000, 100);
    property = new ClusterProperties("test", schemes, supProperties, bannedSet, rbp);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

    HashBasedPartitionProperties hbp = new HashBasedPartitionProperties("blah", 150, HashBasedPartitionProperties.HashAlgorithm.valueOf("md5".toUpperCase()));
    property = new ClusterProperties("test", schemes, supProperties, bannedSet, hbp);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

    property = new ClusterProperties("test", schemes, supProperties, new HashSet<URI>(), NullPartitionProperties.getInstance(),
        Arrays.asList("principal1", "principal2"), (Map<String, Object>)null, false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);

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

    DarkClusterConfig darkCluster1 = new DarkClusterConfig()
        .setMultiplier(1.5f)
        .setDispatcherOutboundMaxRate(123456)
        .setDispatcherOutboundTargetRate(50);
    DarkClusterConfigMap darkClusterConfigMap = new DarkClusterConfigMap();
    darkClusterConfigMap.put(DARK_CLUSTER1_KEY, darkCluster1);
    ClusterProperties property = new ClusterProperties("test", new ArrayList<String>(), Collections.emptyMap(), new HashSet<URI>(), NullPartitionProperties.getInstance(),
        Arrays.asList("principal1", "principal2"), DarkClustersConverter.toProperties(darkClusterConfigMap), false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);
  }

  @Test
  public void test2DarkClusterJsonSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer jsonSerializer = new ClusterPropertiesJsonSerializer();

    DarkClusterConfig darkCluster1 = new DarkClusterConfig()
        .setMultiplier(1.5f)
        .setDispatcherOutboundMaxRate(123456)
        .setDispatcherOutboundTargetRate(50);
    DarkClusterConfigMap darkClusterConfigMap = new DarkClusterConfigMap();
    darkClusterConfigMap.put(DARK_CLUSTER1_KEY, darkCluster1);
    DarkClusterConfig darkCluster2 = new DarkClusterConfig()
        .setDispatcherOutboundMaxRate(200)
        .setDispatcherOutboundTargetRate(50)
        .setMultiplier(0);
    darkClusterConfigMap.put(DARK_CLUSTER2_KEY, darkCluster2);
    ClusterProperties property = new ClusterProperties("test", new ArrayList<>(), new HashMap<>(), new HashSet<>(),
                                                       NullPartitionProperties.getInstance(),
                                                       Arrays.asList("principal1", "principal2"),
                                                       DarkClustersConverter.toProperties(darkClusterConfigMap), false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);
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
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);
  }

  @Test
  public void testNullDarkClusterJsonSerializer() throws PropertySerializationException
  {
    ClusterPropertiesJsonSerializer jsonSerializer = new ClusterPropertiesJsonSerializer();
    ClusterProperties property = new ClusterProperties("test", new ArrayList<>(), new HashMap<>(), new HashSet<URI>(), NullPartitionProperties.getInstance(),
                                                       Arrays.asList("principal1", "principal2"),
                                                       (Map<String, Object>) null, false);
    assertEquals(jsonSerializer.fromBytes(jsonSerializer.toBytes(property)), property);
  }
}
