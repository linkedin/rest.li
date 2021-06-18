/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.discovery.util;

import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorFactory;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestHashFunctions {
  private static final Logger _log = LoggerFactory.getLogger(TestD2Config.class);

  private static ZKServer _zkServer;
  private static String _zkUriString;
  private static String _zkHosts;
  private static ZKConnection _zkclient;

  private static final String ZK_HOST = "127.0.0.1";
  private static final int ZK_PORT = 11712;

  private static PartitionAccessor _XXHashAccessor;
  private static PartitionAccessor _MD5Accessor;

  {
    _zkHosts = ZK_HOST + ":" + ZK_PORT;
    _zkUriString = "zk://" + _zkHosts;
  }

  @BeforeMethod
  public void testSetup() throws IOException, Exception {
    // Startup zookeeper server
    try {
      _zkServer = new ZKServer(ZK_PORT);
      _zkServer.startup();
    } catch (IOException e) {
      fail("unable to instantiate zk server on port " + ZK_PORT);
    }

    // Client
    try {
      _zkclient = ZKTestUtil.getConnection(_zkHosts, 10000);
    } catch (Exception e) {
      fail("unable to startup zk client.");
      e.printStackTrace();
    }

    //Setting up partitionAccessors based on different hash functions.
    final Map<String, List<String>> clustersData = new HashMap<>();
    clustersData.put("partitioned-cluster",
        Arrays.asList(new String[]{"partitioned-service-1", "partitioned-service-2"}));

    @SuppressWarnings("serial")
    final Map<String, Object> partitionPropertiesXXHASH = new HashMap<>();
    Map<String, Object> XXhashBased = new HashMap<>();
    XXhashBased.put("partitionKeyRegex", "\\bid\\b=(\\d+)");
    XXhashBased.put("partitionCount", "10");
    XXhashBased.put("hashAlgorithm", "XXHash");
    XXhashBased.put("partitionType", "HASH");
    partitionPropertiesXXHASH.put("partitionProperties", XXhashBased);
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil(clustersData, partitionPropertiesXXHASH);
    d2Conf.runDiscovery(_zkHosts);
    final ClusterProperties clusterprops = getClusterProperties(_zkclient, "partitioned-cluster");
    _XXHashAccessor = PartitionAccessorFactory.getPartitionAccessor("partitioned-cluster", null,
        clusterprops.getPartitionProperties());

    final Map<String, Object> partitionPropertiesMD5 = new HashMap<>();
    Map<String, Object> MD5HashBased = new HashMap<>();
    MD5HashBased.put("partitionKeyRegex", "\\bid\\b=(\\d+)");
    MD5HashBased.put("partitionCount", "10");
    MD5HashBased.put("hashAlgorithm", "MD5");
    MD5HashBased.put("partitionType", "HASH");
    partitionPropertiesMD5.put("partitionProperties", MD5HashBased);
    D2ConfigTestUtil d2Conf2 = new D2ConfigTestUtil(clustersData, partitionPropertiesMD5);
    d2Conf2.runDiscovery(_zkHosts);
    final ClusterProperties clusterprops2 = getClusterProperties(_zkclient, "partitioned-cluster");
    _MD5Accessor = PartitionAccessorFactory.getPartitionAccessor("partitioned-cluster", null,
        clusterprops2.getPartitionProperties());
  }

  @AfterMethod
  public void teardown() throws IOException, InterruptedException {
    try {
      _zkclient.shutdown();
      _zkServer.shutdown();
    } catch (Exception e) {
      _log.info("shutdown failed.");
    }
  }

  private static ClusterProperties getClusterProperties(ZKConnection zkclient, String cluster)
      throws IOException, URISyntaxException, PropertyStoreException {
    String clstoreString = _zkUriString + ZKFSUtil.clusterPath("/d2");

    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry =
        (ZooKeeperPermanentStore<ClusterProperties>) LoadBalancerClientCli.getStore(zkclient, clstoreString,
            new ClusterPropertiesJsonSerializer());

    return zkClusterRegistry.get(cluster);
  }

  @Test
  public static void testXXHashFunction() throws PartitionAccessException {

    int key1 = _XXHashAccessor.getPartitionId("shortkey");
    int key2 = _XXHashAccessor.getPartitionId("shortkey");
    int key3 = _XXHashAccessor.getPartitionId("aVeryVeryVeryLongKey");

    Assert.assertEquals(key1, key2);
    Assert.assertNotEquals(key1, key3);
  }

  //Performance test, XXHash vs MD5, disabled to save building time. Primary testing shows that XXHash is easily 4 times faster.
  @Test(enabled = false)
  public static void testXXHashPerformanceAgainstMD5()
      throws IOException, InterruptedException, URISyntaxException, Exception {
    long startTimeMD5 = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      _MD5Accessor.getPartitionId("aSuperSuperSuperSuperSuperLongKey");
    }
    long endTimeMD5 = System.currentTimeMillis();
    long MD5Duration = endTimeMD5 - startTimeMD5;

    long startTimeXXHash = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      _XXHashAccessor.getPartitionId("aSuperSuperSuperSuperSuperLongKey");
    }
    long endTimeXXHash = System.currentTimeMillis();
    long XXHashDuration = endTimeXXHash - startTimeXXHash;
    float speedup = (float) MD5Duration / XXHashDuration;
    _log.debug("With 1000 queries, XXHash achieved " + speedup + " times speedup");
    Assert.assertTrue(speedup > 1);
  }
}
