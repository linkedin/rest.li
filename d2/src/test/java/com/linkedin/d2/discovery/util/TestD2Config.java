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

package com.linkedin.d2.discovery.util;

import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorFactory;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

public class TestD2Config
{
  private static final Logger _log = LoggerFactory.getLogger(TestD2Config.class);

  private static ZKServer _zkServer;
  private static String _zkUriString;
  private static String _zkHosts;
  private static ZKConnection _zkclient;

  private static final String ZK_HOST = "127.0.0.1";
  private static final int ZK_PORT = 11712;
  private static final String ECHO_SERVER_HOST = "127.0.0.1";

  private static List<LoadBalancerEchoServer> _echoServerList = new ArrayList<LoadBalancerEchoServer>();

  {
    _zkHosts = ZK_HOST+":"+ZK_PORT;
    _zkUriString = "zk://"+_zkHosts;
  }

  @BeforeTest
  public void testSetup() throws IOException, Exception
  {
    // Startup zookeeper server
    try
    {
      _zkServer = new ZKServer(ZK_PORT);
      _zkServer.startup();
    }
    catch (IOException e)
    {
      fail("unable to instantiate real zk server on port " + ZK_PORT);
      e.printStackTrace();
    }

    // Client
    try
    {
      _zkclient = ZKTestUtil.getConnection(_zkHosts, 10000);
    }
    catch (Exception e)
    {
      fail("unable to startup zk client.");
      e.printStackTrace();
    }
  }

  @AfterTest
  public void teardown() throws IOException, InterruptedException
  {
    for (LoadBalancerEchoServer echoServer : _echoServerList)
    {
      try
      {
        echoServer.stopServer();
        _log.info("Executed echoserver shutdown. ");
      }
      catch (Exception e)
      {
        _log.info("echoserver shutdown failed. EchoServer:"+echoServer);
        e.printStackTrace();
      }
    }

    try
    {
      _zkclient.shutdown();
      _log.info("Executed cli shutdown. ");
    }
    catch (Exception e)
    {
      _log.info("zkclient shutdown failed.");
    }

    try
    {
      _zkServer.shutdown();
      _log.info("Executed zkServer shutdown. ");
    }
    catch (Exception e)
    {
      _log.info("zk server shutdown failed.");
    }
  }

  @Test
  public static void testSingleCluster() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    final int echoServerPort1 = 2341;
    final int echoServerPort2 = 2342;

    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>()
    {{
      put("cluster-1", Arrays.asList(new String[]{"service-1_1", "service-1_2"}));
    }};

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    // Start Echo server on cluster-1
    _echoServerList.add(startEchoServer(echoServerPort1, "cluster-1"));
    _echoServerList.add(startEchoServer(echoServerPort2, "cluster-1"));

    verifyClusterProperties("cluster-1");
    verifyServiceProperties("cluster-1", "service-1_1", "/service-1_1", null);
    verifyServiceProperties("cluster-1", "service-1_2", "/service-1_2", null);

    @SuppressWarnings("serial")
    Map<URI, Double> urisWeights = new HashMap<URI, Double>()
    {{
      put(URI.create("http://127.0.0.1:"+echoServerPort1+"/cluster-1"), 1.0);
      put(URI.create("http://127.0.0.1:"+echoServerPort2+"/cluster-1"), 1.0);
    }};
    verifyUriProperties("cluster-1", urisWeights);

  }

  // preliminary test for partitioning cluster
  @Test
  public static void testSingleClusterRangePartitions() throws IOException, InterruptedException, URISyntaxException, Exception
  {

    @SuppressWarnings("serial")
    final Map<String,List<String>> clustersData = new HashMap<String,List<String>>()
    {{
        put("partitioned-cluster", Arrays.asList(new String[]{"partitioned-service-1", "partitioned-service-2"}));
      }};

    final Map<String, Object> partitionProperties = new HashMap<String, Object>();
    Map<String, Object> rangeBased = new HashMap<String, Object>();
    rangeBased.put("partitionKeyRegex", "\\bid\\b=(\\d+)");
    rangeBased.put("keyRangeStart", "0");
    rangeBased.put("partitionCount", "10");
    rangeBased.put("partitionSize", "100");
    rangeBased.put("partitionType", "RANGE");
    partitionProperties.put("partitionProperties", rangeBased);

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, partitionProperties);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyPartitionProperties("partitioned-cluster", partitionProperties);

    final ClusterProperties clusterprops = getClusterProperties(_zkclient, "partitioned-cluster" );
    final PartitionAccessor accessor = PartitionAccessorFactory.getPartitionAccessor(clusterprops.getPartitionProperties());
    try
    {
      accessor.getPartitionId(-1 + "");
      fail("Exception expected");
    }
    catch (PartitionAccessException e){}

    try
    {
      accessor.getPartitionId(1000 + "");
      fail("Exception expected");
    }
    catch (PartitionAccessException e){}

    assertEquals(0, accessor.getPartitionId(0 + ""));
    assertEquals(0, accessor.getPartitionId(99 + ""));
    assertEquals(1, accessor.getPartitionId(176 + ""));
    assertEquals(8, accessor.getPartitionId(833 + ""));


    final String legalUri1 = "/profiles?field=position&id=100";
    final String legalUri2 = "/profiles?wid=99&id=176&randid=301";
    final String illegalUri1 = "/profiles?wid=99";
    final String illegalUri2 = "/profiles?id=1000000000000000000000000000000000000000000000111111111";

    try
    {
      accessor.getPartitionId(URI.create(illegalUri1));
      fail("Exception expected");
    }
    catch (PartitionAccessException e) {}

    try
    {
      accessor.getPartitionId(URI.create(illegalUri2));
      fail("Exception expected");
    }
    catch (PartitionAccessException e) {}

    assertEquals(1, accessor.getPartitionId(URI.create(legalUri1)));
    assertEquals(1, accessor.getPartitionId(URI.create(legalUri2)));

    // Start Echo server on cluster-1

    Map<Integer, Double> serverConfig1 = new HashMap<Integer, Double>();
    serverConfig1.put(0, 0.5d);
    serverConfig1.put(3, 0.5d);
    Map<Integer, Double> serverConfig2 = new HashMap<Integer, Double>();
    serverConfig2.put(0, 0.25d);
    serverConfig2.put(1, 0.5d);
    serverConfig2.put(2, 0.5d);

    final int echoServerPort1 = 2346;
    final int echoServerPort2 = 2347;
    _echoServerList.add(startEchoServer(echoServerPort1, "partitioned-cluster", serverConfig1));
    _echoServerList.add(startEchoServer(echoServerPort2, "partitioned-cluster", serverConfig2));

    Map<URI, Map<Integer, Double>> partitionWeights = new HashMap<URI, Map<Integer, Double>>();
    partitionWeights.put(URI.create("http://127.0.0.1:"+echoServerPort1+"/partitioned-cluster"),
        serverConfig1);
    partitionWeights.put(URI.create("http://127.0.0.1:"+echoServerPort2+"/partitioned-cluster"),
        serverConfig2);

    verifyPartitionedUriProperties("partitioned-cluster", partitionWeights);
  }

  // preliminary test for partitioning cluster
  @Test
  public static void testSingleClusterHashPartitions() throws IOException, InterruptedException, URISyntaxException, Exception
  {

    @SuppressWarnings("serial")
    final Map<String,List<String>> clustersData = new HashMap<String,List<String>>()
    {{
        put("partitioned-cluster", Arrays.asList(new String[]{"partitioned-service-1", "partitioned-service-2"}));
      }};

    final Map<String, Object> partitionProperties = new HashMap<String, Object>();
    Map<String, Object> hashBased = new HashMap<String, Object>();
    hashBased.put("partitionKeyRegex", "\\bid\\b=(\\d+)");
    hashBased.put("partitionCount", "10");
    hashBased.put("hashAlgorithm", "modulo");
    hashBased.put("partitionType", "HASH");
    partitionProperties.put("partitionProperties", hashBased);


    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, partitionProperties);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyPartitionProperties("partitioned-cluster", partitionProperties);

    final ClusterProperties clusterprops = getClusterProperties(_zkclient, "partitioned-cluster" );
    final PartitionAccessor accessor = PartitionAccessorFactory.getPartitionAccessor(clusterprops.getPartitionProperties());

    assertEquals(0, accessor.getPartitionId(0 + ""));
    assertEquals(9, accessor.getPartitionId(99 + ""));
    assertEquals(6, accessor.getPartitionId(176 + ""));
    assertEquals(3, accessor.getPartitionId(833 + ""));

  }


  @Test
  public static void testMultipleClustersWithServiceGroups() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    final int echoServerPort1 = 2343;
    final int echoServerPort2 = 2344;
    final int echoServerPort3 = 2345;

    @SuppressWarnings("serial")
    Map<String,String> servicesData = new HashMap<String,String>()
    {{
      put("service1", "testService");
      put("service2", "testService");
      put("service3", "testService");
      put("service4", "testService");
    }};

    @SuppressWarnings("serial")
    Map<String,String> serviceGroupsData = new HashMap<String,String>()
    {{
      put("ServiceGroup1", "Cluster1");
      put("ServiceGroup2", "Cluster2");
      put("ServiceGroup3", "Cluster3");
    }};

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil("TestServices", servicesData, serviceGroupsData);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    // Start Echo server on clusters
    _echoServerList.add(startEchoServer(echoServerPort1, "someCluster1"));
    _echoServerList.add(startEchoServer(echoServerPort2, "someCluster1"));
    _echoServerList.add(startEchoServer(echoServerPort3, "someCluster3"));

    verifyClusterProperties("TestServices");
    verifyServiceProperties("TestServices", "service1", "/testService", null);
    verifyServiceProperties("TestServices", "service2", "/testService", null);
    verifyServiceProperties("TestServices", "service3", "/testService", null);
    verifyServiceProperties("TestServices", "service4", "/testService", null);

    verifyClusterProperties("Cluster1");
    verifyServiceProperties("Cluster1", "service1", "/testService", "ServiceGroup1");
    verifyServiceProperties("Cluster1", "service2", "/testService", "ServiceGroup1");
    verifyServiceProperties("Cluster1", "service3", "/testService", "ServiceGroup1");
    verifyServiceProperties("Cluster1", "service4", "/testService", "ServiceGroup1");

    verifyClusterProperties("Cluster2");
    verifyServiceProperties("Cluster2", "service1", "/testService", "ServiceGroup2");
    verifyServiceProperties("Cluster2", "service2", "/testService", "ServiceGroup2");
    verifyServiceProperties("Cluster2", "service3", "/testService", "ServiceGroup2");
    verifyServiceProperties("Cluster2", "service4", "/testService", "ServiceGroup2");

    verifyClusterProperties("Cluster3");
    verifyServiceProperties("Cluster3", "service1", "/testService", "ServiceGroup3");
    verifyServiceProperties("Cluster3", "service2", "/testService", "ServiceGroup3");
    verifyServiceProperties("Cluster3", "service3", "/testService", "ServiceGroup3");
    verifyServiceProperties("Cluster3", "service4", "/testService", "ServiceGroup3");

    @SuppressWarnings("serial")
    Map<URI, Double> urisWeights = new HashMap<URI, Double>()
    {{
      put(URI.create("http://127.0.0.1:"+echoServerPort1+"/someCluster1"), 1.0);
      put(URI.create("http://127.0.0.1:"+echoServerPort2+"/someCluster1"), 1.0);
    }};
    verifyUriProperties("someCluster1", urisWeights);

    @SuppressWarnings("serial")
    Map<URI, Double> urisWeights2 = new HashMap<URI, Double>()
    {{
      put(URI.create("http://127.0.0.1:"+echoServerPort3+"/someCluster3"), 1.0);
    }};
    verifyUriProperties("someCluster3", urisWeights2);

  }

  @Test
  public static void testClusterNameWithRouting() throws Exception
  {
    String clusterNameWithRouting;

    clusterNameWithRouting = D2Config.clusterNameWithRouting("clusterName",
                                                             "destinationColo",
                                                             "defaultColo",
                                                             "masterColo",
                                                             false,
                                                             false);
    assertEquals("clusterName-defaultColo", clusterNameWithRouting);

    clusterNameWithRouting = D2Config.clusterNameWithRouting("clusterName",
                                                             "destinationColo",
                                                             "defaultColo",
                                                             "masterColo",
                                                             true,
                                                             false);
    assertEquals("clusterName-masterColo", clusterNameWithRouting);

    clusterNameWithRouting = D2Config.clusterNameWithRouting("clusterName",
                                                             "destinationColo",
                                                             "defaultColo",
                                                             "masterColo",
                                                             true,
                                                             true);
    assertEquals(D2Utils.getSymlinkNameForMaster("clusterName"), clusterNameWithRouting);

    clusterNameWithRouting = D2Config.clusterNameWithRouting("clusterName",
                                                             "",
                                                             "defaultColo",
                                                             "masterColo",
                                                             false,
                                                             false);
    assertEquals("clusterName", clusterNameWithRouting);

    clusterNameWithRouting = D2Config.clusterNameWithRouting("clusterName",
                                                             "",
                                                             "defaultColo",
                                                             "masterColo",
                                                             true,
                                                             false);
    assertEquals("clusterName", clusterNameWithRouting);
  }

  @Test
  public static void testClustersWithDuplicateServices() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    // D2Config error message - "Identical service name found in multiple clusters!"

    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>()
    {{
      put("cluster-2", Arrays.asList(new String[]{"service-2_1", "service-3_2"}));
      put("cluster-3", Arrays.asList(new String[]{"service-3_1", "service-3_2"}));
    }};

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 2);

    assertNull(getClusterProperties(_zkclient, "cluster-2"));
    assertNull(getClusterProperties(_zkclient, "cluster-3"));
  }

  @Test
  public static void testWithNonUniqueServiceGroupClusterVariants() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    // D2Config error : "Service group has variants of the same cluster"

    Map<String, Object> clusterServiceConfigurations = new HashMap<String, Object>();
    Map<String, Object> serviceVariants = new HashMap<String, Object>();

    //Cluster Service Configurations
    // Services With Variants
    Map<String,Object> services = new HashMap<String,Object>();
    services.put("services",D2ConfigTestUtil.generateServicesMap(2, "service", "testService"));

    // Service variants
    @SuppressWarnings("serial")
    Map<String,Object> clusterVariants = new HashMap<String,Object>()
    {{
      put("zCluster1",new HashMap<String,Object>());
      put("zCluster2",new HashMap<String,Object>());
    }};

    services.put("clusterVariants", clusterVariants);

    // Services Clusters
    // ContentServices Cluster
    clusterServiceConfigurations.put("zServices", services);

    // Cluster variants
    // serviceGroup1
    @SuppressWarnings("serial")
    Map<String,Object> serviceGroup1 = new HashMap<String,Object>()
    {{
      put("type", "clusterVariantsList");
      put("clusterList", Arrays.asList(new String[]{"zCluster1"}));
    }};

    // serviceGroup2
    @SuppressWarnings("serial")
     Map<String,Object> serviceGroup2 = new HashMap<String,Object>()
     {{
       put("type", "clusterVariantsList");
       put("clusterList", Arrays.asList(new String[]{"zCluster2", "zCluster1"}));
     }};

    serviceVariants.put("ServiceGroup1", serviceGroup1);
    serviceVariants.put("ServiceGroup2", serviceGroup2);

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil();
    d2Conf.setDefaults();
    d2Conf.setClusterServiceConfigurations(clusterServiceConfigurations);
    d2Conf.setServiceVariants(serviceVariants);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 2);

    assertNull(getClusterProperties(_zkclient, "zCluster1"));
    assertNull(getClusterProperties(_zkclient, "zCluster2"));

  }

  @Test
  public static void testWithUnknownCluster() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    // D2Config error : "Unknown cluster specified"

    Map<String, Object> clusterServiceConfigurations = new HashMap<String, Object>();
    Map<String, Object> serviceVariants = new HashMap<String, Object>();

    //Cluster Service Configurations
    // Services With Variants
    Map<String,Object> services = new HashMap<String,Object>();
    services.put("services",D2ConfigTestUtil.generateServicesMap(1, "service", "testService"));

    // Service variants
    @SuppressWarnings("serial")
    Map<String,Object> clusterVariants = new HashMap<String,Object>()
    {{
      put("cluster1",new HashMap<String,Object>());
    }};

    services.put("clusterVariants", clusterVariants);

    // Services Clusters
    clusterServiceConfigurations.put("zServices", services);

    // Cluster variants
    // serviceGroup1
    @SuppressWarnings("serial")
    Map<String,Object> serviceGroup1 = new HashMap<String,Object>()
    {{
      put("type", "clusterVariantsList");
      put("clusterList", Arrays.asList(new String[]{"cluster1"}));
    }};

    // serviceGroup2
    @SuppressWarnings("serial")
     Map<String,Object> serviceGroup2 = new HashMap<String,Object>()
     {{
       put("type", "clusterVariantsList");
       put("clusterList", Arrays.asList(new String[]{"zCluster2",}));
     }};

    serviceVariants.put("ServiceGroup1", serviceGroup1);
    serviceVariants.put("ServiceGroup2", serviceGroup2);

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil();
    d2Conf.setDefaults();
    d2Conf.setClusterServiceConfigurations(clusterServiceConfigurations);
    d2Conf.setServiceVariants(serviceVariants);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 2);

    assertNull(getClusterProperties(_zkclient, "cluster1"));
    assertNull(getClusterProperties(_zkclient, "zCluster2"));

  }

  @Test
  public static void testUnknownServiceVariantType() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    // D2Config error : "unknown serviceVariant type"

    Map<String, Object> clusterServiceConfigurations = new HashMap<String, Object>();
    Map<String, Object> serviceVariants = new HashMap<String, Object>();

    //Cluster Service Configurations
    // Services With Variants
    Map<String,Object> services = new HashMap<String,Object>();
    services.put("services",D2ConfigTestUtil.generateServicesMap(1, "service", "testService"));

    // Service variants
    @SuppressWarnings("serial")
    Map<String,Object> clusterVariants = new HashMap<String,Object>()
    {{
      put("cluster1",new HashMap<String,Object>());
      put("cluster2",new HashMap<String,Object>());
    }};

    services.put("clusterVariants", clusterVariants);

    // Services Clusters
    clusterServiceConfigurations.put("zServices", services);

    // Cluster variants

    // serviceGroup1
    @SuppressWarnings("serial")
    Map<String,Object> serviceGroup1 = new HashMap<String,Object>()
    {{
      put("type", "clusterVariantsList");
      put("clusterList", Arrays.asList(new String[]{"cluster1"}));
    }};

    // serviceGroup2
    @SuppressWarnings("serial")
     Map<String,Object> serviceGroup2 = new HashMap<String,Object>()
     {{
       put("type", "someVariantsList");
       put("clusterList", Arrays.asList(new String[]{"cluster2",}));
     }};

    serviceVariants.put("ServiceGroup1", serviceGroup1);
    serviceVariants.put("ServiceGroup2", serviceGroup2);

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil();
    d2Conf.setDefaults();
    d2Conf.setClusterServiceConfigurations(clusterServiceConfigurations);
    d2Conf.setServiceVariants(serviceVariants);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 2);

    assertNull(getClusterProperties(_zkclient, "cluster1"));
    assertNull(getClusterProperties(_zkclient, "cluster2"));

  }

  @Test
  public static void testNonUniqueClusterVariantName() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    // D2Config error message: "Cluster variant name: ... is not unique!"

    Map<String, Object> clusterServiceConfigurations = new HashMap<String, Object>();
    Map<String, Object> serviceVariants = new HashMap<String, Object>();

    //Cluster Service Configurations
    // Services With Variants
    Map<String,Object> services = new HashMap<String,Object>();
    services.put("services",D2ConfigTestUtil.generateServicesMap(1, "service", "testService"));

    // Service variants
    @SuppressWarnings("serial")
    Map<String,Object> clusterVariants = new HashMap<String,Object>()
    {{
      put("Cluster#1",new HashMap<String,Object>());
      put("Cluster#2",new HashMap<String,Object>());
    }};
    services.put("clusterVariants",clusterVariants);

    // Services Clusters
    clusterServiceConfigurations.put("Cluster#1", services);

    // Cluster variants
    // serviceGroup1
    @SuppressWarnings("serial")
    Map<String,Object> serviceGroup1 = new HashMap<String,Object>()
    {{
      put("type", "clusterVariantsList");
      put("clusterList", Arrays.asList(new String[]{"Cluster#1"}));
    }};

    // serviceGroup2
    @SuppressWarnings("serial")
     Map<String,Object> serviceGroup2 = new HashMap<String,Object>()
     {{
       put("type", "clusterVariantsList");
       put("clusterList", Arrays.asList(new String[]{"Cluster#2"}));
     }};

    serviceVariants.put("ServiceGroup1", serviceGroup1);
    serviceVariants.put("ServiceGroup2", serviceGroup2);

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil();
    d2Conf.setDefaults();
    d2Conf.setClusterServiceConfigurations(clusterServiceConfigurations);
    d2Conf.setServiceVariants(serviceVariants);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 2);

    assertNull(getClusterProperties(_zkclient, "Cluster#1"));
    assertNull(getClusterProperties(_zkclient, "Cluster#2"));

  }

  private static LoadBalancerEchoServer startEchoServer(int echoServerPort, String cluster,String... services) throws Exception
  {
    LoadBalancerEchoServer echoServer = new LoadBalancerEchoServer(ZK_HOST, ZK_PORT, ECHO_SERVER_HOST, echoServerPort, "http","/d2", cluster, services);
    echoServer.startServer();
    echoServer.markUp();

    return echoServer;
  }

  private static LoadBalancerEchoServer startEchoServer(int echoServerPort, String cluster, Map<Integer, Double> partitionWeight, String... services) throws Exception
  {
    LoadBalancerEchoServer echoServer = new LoadBalancerEchoServer(ZK_HOST, ZK_PORT, ECHO_SERVER_HOST, echoServerPort, "http", "/d2", cluster, partitionWeight, services);
    echoServer.startServer();
    echoServer.markUp();
    return echoServer;
  }

  private static ClusterProperties getClusterProperties(ZKConnection zkclient, String cluster) throws IOException, URISyntaxException, PropertyStoreException
  {
    String clstoreString = _zkUriString + ZKFSUtil.clusterPath("/d2");

    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry =
      (ZooKeeperPermanentStore<ClusterProperties>) LoadBalancerClientCli.getStore(zkclient,
                                                            clstoreString,
                                                            new ClusterPropertiesJsonSerializer());

    return zkClusterRegistry.get(cluster);
  }

  private static ServiceProperties getServiceProperties(ZKConnection zkclient, String service, String serviceGroup) throws IOException, URISyntaxException, PropertyStoreException
  {
    String scstoreString = _zkUriString + ZKFSUtil.servicePath("/d2");

    if (serviceGroup != null)
    {
      scstoreString = _zkUriString + ZKFSUtil.servicePath("/d2", serviceGroup);
    }

    ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry =
      (ZooKeeperPermanentStore<ServiceProperties>) LoadBalancerClientCli.getStore(zkclient,
                                                            scstoreString,
                                                            new ServicePropertiesJsonSerializer());

    return zkServiceRegistry.get(service);
  }

  private static UriProperties getUriProperties(ZKConnection zkclient, String cluster) throws IOException, URISyntaxException, PropertyStoreException
  {
    String uristoreString = _zkUriString + ZKFSUtil.uriPath("/d2");

    ZooKeeperEphemeralStore<UriProperties> zkUriRegistry =
      (ZooKeeperEphemeralStore<UriProperties>) LoadBalancerClientCli.getEphemeralStore(zkclient,
                                                                 uristoreString,
                                                                 new UriPropertiesJsonSerializer(),
                                                                 new UriPropertiesMerger());

    return zkUriRegistry.get(cluster);
  }

  public static void verifyClusterProperties(String cluster) throws IOException, URISyntaxException, PropertyStoreException
  {
    ClusterProperties clusterprops = getClusterProperties(_zkclient, cluster);

    assertEquals(clusterprops.getClusterName(), cluster);
    assertEquals(clusterprops.getPrioritizedSchemes(), Arrays.asList(new String[] {"http"}));
    assertEquals(clusterprops.getProperties().get("requestTimeout"), String.valueOf(10000));
    assertEquals(clusterprops.getBanned(), new TreeSet<URI>());
  }

  public static void verifyPartitionProperties(String cluster, Map<String, Object>propertiesMap) throws IOException, URISyntaxException, PropertyStoreException
  {
    final ClusterProperties clusterprops = getClusterProperties(_zkclient, cluster);
    if (propertiesMap.get("partitionProperties") != null)
    {
      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>)propertiesMap.get("partitionProperties");
      PartitionProperties.PartitionType partitionType = PartitionProperties.PartitionType.valueOf(
         (String)properties.get("partitionType"));
      switch (partitionType)
      {
        case RANGE:
        {
          long keyRangeStart = ((Number)properties.get("keyRangeStart")).longValue();
          long partitionSize = ((Number)properties.get("partitionSize")).longValue();
          int partitionCount = ((Number)properties.get("partitionCount")).intValue();

          String regex = (String)properties.get("partitionKeyRegex");
          RangeBasedPartitionProperties rbp = (RangeBasedPartitionProperties) clusterprops.getPartitionProperties();
          assertEquals(keyRangeStart, rbp.getKeyRangeStart());
          assertEquals(partitionSize, rbp.getPartitionSize());
          assertEquals(partitionCount, rbp.getPartitionCount());
          assertEquals(regex, rbp.getPartitionKeyRegex());
        }
        break;
        case HASH:
        {
          int partitionCount = ((Number)properties.get("partitionCount")).intValue();
          String regex = (String)properties.get("partitionKeyRegex");
          String algorithm = (String)properties.get("hashAlgorithm");
          HashBasedPartitionProperties.HashAlgorithm hashAlgorithm = HashBasedPartitionProperties.HashAlgorithm.valueOf(algorithm.toUpperCase());
          HashBasedPartitionProperties hbp = (HashBasedPartitionProperties) clusterprops.getPartitionProperties();
          assertEquals(partitionCount, hbp.getPartitionCount());
          assertEquals(regex, hbp.getPartitionKeyRegex());
          assertEquals(hashAlgorithm, hbp.getHashAlgorithm());
        }
        break;
        default: break;
      }

    }
  }

  public static void verifyServiceProperties(String cluster, String service, String path, String serviceGroup) throws IOException, URISyntaxException, PropertyStoreException
  {
    ServiceProperties serviceprops = getServiceProperties(_zkclient, service, serviceGroup);

    assertEquals(serviceprops.getClusterName(), cluster);
    assertEquals(serviceprops.getServiceName(), service);

    assertEquals(serviceprops.getPath(), path);
    assertEquals(serviceprops.getLoadBalancerStrategyList(), Arrays.asList(new String[] {"degrader","degraderV3"}));
    assertEquals(serviceprops.getLoadBalancerStrategyProperties().get("maxClusterLatencyWithoutDegrading"), String.valueOf(500));
    assertEquals(serviceprops.getLoadBalancerStrategyProperties().get("updateIntervalsMs"), String.valueOf(5000));
    assertEquals(serviceprops.getLoadBalancerStrategyProperties().get("defaultSuccessfulTransmissionWeight"), String.valueOf(1.0));
    assertEquals(serviceprops.getLoadBalancerStrategyProperties().get("pointsPerWeight"), String.valueOf(100));
  }

  public static void verifyUriProperties(String cluster, Map<URI, Double> urisWeights)
      throws IOException, URISyntaxException, PropertyStoreException
  {
    UriProperties     uriprops     = getUriProperties(_zkclient,cluster);

    assertEquals(uriprops.getClusterName(), cluster);

    for (URI uri : urisWeights.keySet())
    {
      assertEquals(uriprops.getPartitionDataMap(uri).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), urisWeights.get(uri));
    }
  }

  public static void verifyPartitionedUriProperties(String cluster, Map<URI, Map<Integer, Double>> partitionWeights)
      throws IOException, URISyntaxException, PropertyStoreException
  {
    UriProperties     uriprops     = getUriProperties(_zkclient,cluster);
    assertEquals(uriprops.getClusterName(), cluster);

    if (partitionWeights != null)
    {
      Map<Integer, Set<URI>> partitionUris = new HashMap<Integer, Set<URI>>();
      for (final URI uri : partitionWeights.keySet())
      {
        for(final int partitionId : partitionWeights.get(uri).keySet())
        {
          Set<URI> uriSet = partitionUris.get(partitionId);
          if (uriSet == null)
          {
            uriSet = new HashSet<URI>();
            partitionUris.put(partitionId, uriSet);
          }
          uriSet.add(uri);
        }
      }

      for (final int partitionId : partitionUris.keySet())
      {
        assertEquals(uriprops.getUriBySchemeAndPartition("http", partitionId), partitionUris.get(partitionId));
      }

      for (URI uri : partitionWeights.keySet())
      {
        Map<Integer, Double> weights = partitionWeights.get(uri);
        for (int partitionId : weights.keySet())
        {
          assertEquals(weights.get(partitionId), uriprops.getPartitionDataMap(uri).get(partitionId).getWeight());
        }
      }
    }
  }

  @Test
  public void testSuffixAppender()
  {
    String baseName1 = "Foobar";
    String baseName2 = "BarBaz";
    String suffix1 = "WestCoast";
    String suffix2 = "EastCoast";

    Assert.assertEquals(D2Utils.addSuffixToBaseName(baseName1, suffix1), baseName1 + "-" + suffix1);
    Assert.assertEquals(D2Utils.addSuffixToBaseName(baseName1, suffix2), baseName1 + "-" + suffix2);
    Assert.assertEquals(D2Utils.addSuffixToBaseName(baseName2, suffix1), baseName2 + "-" + suffix1);
    Assert.assertEquals(D2Utils.addSuffixToBaseName(baseName2, suffix2), baseName2 + "-" + suffix2);
    Assert.assertEquals(D2Utils.addSuffixToBaseName(baseName2, null), baseName2);
  }


  @Test
  public static void testSingleClusterNoColo() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    clustersData.put("cluster-1", serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add("cluster-1");

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    Map<String,String> masterColoList = new HashMap<String,String>();
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put("cluster-1", clusterProperties);
    String defaultColo = "";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo);
  }

  @Test
  public static void testSingleColoCluster() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("WestCoast");
    peerColos.add("EastCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put("coloVariants", peerColos);
    String masterColo = "WestCoast";
    clusterProperties.put("masterColo", masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "EastCoast";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo);
  }

  @Test
  public static void testSingleClusterNoColoWithDefaultColo()
    throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    clustersData.put("cluster-1", serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add("cluster-1");

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    Map<String,String> masterColoList = new HashMap<String,String>();
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put("cluster-1", clusterProperties);
    String defaultColo = "EastCoast";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo);
  }

  // make sure the only thing that will trigger the generation of coloVariants is the presence of
  // a "coloVariants" cluster property.
  @Test
  public static void testSingleClusterNoColoWithDefaultColoMasterColo() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    clustersData.put("cluster-1", serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add("cluster-1");

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    String masterColo = "WestCoast";
    clusterProperties.put("masterColo", masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();

    clustersProperties.put("cluster-1", clusterProperties);
    String defaultColo = "EastCoast";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo);
  }

  @Test
  public static void testOneColoClusterOneNoColoCluster() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> serviceList2 = new ArrayList<String>();
    serviceList2.add("service-2_1");
    serviceList2.add("service-2_2");
    String cluster2Name = "cluster-2";
    clustersData.put(cluster2Name, serviceList2);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);
    clusterList.add(cluster2Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("WestCoast");
    peerColos.add("EastCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put("coloVariants", peerColos);
    String masterColo = "WestCoast";
    clusterProperties.put("masterColo", masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "EastCoast";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo);
  }

  @Test
  public static void testClusterWithEmptyColoVariants() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put("coloVariants", peerColos);
    Map<String,String> masterColoList = new HashMap<String,String>();
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "EastCoast";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo);
  }

  @Test
  public static void testSingleColoClusterWithClusterVariants()
    throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");

    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    final String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("WestCoast");
    peerColos.add("EastCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put(PropertyKeys.COLO_VARIANTS, peerColos);
    String masterColo = "WestCoast";
    clusterProperties.put(PropertyKeys.MASTER_COLO, masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);

    // add in clusterVariants
    Map<String,Map<String,Object>> clusterVariants = new HashMap<String, Map<String, Object>>();
    final String cluster1Variant1Name = "cluster1Foo";
    final String cluster1Variant2Name = "cluster1Bar";
    clusterVariants.put(cluster1Variant1Name, Collections.<String,Object>emptyMap());
    clusterVariants.put(cluster1Variant2Name, Collections.<String,Object>emptyMap());
    List<String> clusterVariantsList = new ArrayList<String>();
    clusterVariantsList.add(cluster1Variant1Name);
    clusterVariantsList.add(cluster1Variant2Name);
    clusterProperties.put(PropertyKeys.CLUSTER_VARIANTS, clusterVariants);
    Map<String,List<String>> clusterVariantsMapping = new HashMap<String, List<String>>();

    clusterVariantsMapping.put(cluster1Name,clusterVariantsList);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "EastCoast";

    @SuppressWarnings("serial")
    Map<String,List<String>> serviceGroupsData = new HashMap<String,List<String>>()
    {{
        put("ServiceGroup1", Collections.singletonList(cluster1Variant1Name));
        put("ServiceGroup2", Collections.singletonList(cluster1Variant2Name));
      }};

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties,
                                                    serviceGroupsData);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo, clusterVariantsMapping);

    // It's hard to validate the serviceGroups without replicating all the temporary structures
    // needed inside D2Config. Just doing it manually here.
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_1", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-WestCoast", "service-1_1Master", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-WestCoast", "service-1_1-WestCoast", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_1-EastCoast", "/service-1_1", "ServiceGroup1");

    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_2", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-WestCoast", "service-1_2Master", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-WestCoast", "service-1_2-WestCoast", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_2-EastCoast", "/service-1_2", "ServiceGroup1");

    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_1", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-WestCoast", "service-1_1Master", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-WestCoast", "service-1_1-WestCoast", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_1-EastCoast", "/service-1_1", "ServiceGroup2");

    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_2", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-WestCoast", "service-1_2Master", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-WestCoast", "service-1_2-WestCoast", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_2-EastCoast", "/service-1_2", "ServiceGroup2");
  }

  @Test
  public static void testSingleColoClusterWithClusterVariants2()
    throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");

    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    final String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("WestCoast");
    peerColos.add("EastCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put(PropertyKeys.COLO_VARIANTS, peerColos);
    String masterColo = "EastCoast";
    clusterProperties.put(PropertyKeys.MASTER_COLO, masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);

    // add in clusterVariants
    Map<String,Map<String,Object>> clusterVariants = new HashMap<String, Map<String, Object>>();
    final String cluster1Variant1Name = "cluster1Foo";
    final String cluster1Variant2Name = "cluster1Bar";
    clusterVariants.put(cluster1Variant1Name, Collections.<String,Object>emptyMap());
    clusterVariants.put(cluster1Variant2Name, Collections.<String,Object>emptyMap());
    List<String> clusterVariantsList = new ArrayList<String>();
    clusterVariantsList.add(cluster1Variant1Name);
    clusterVariantsList.add(cluster1Variant2Name);
    clusterProperties.put(PropertyKeys.CLUSTER_VARIANTS, clusterVariants);
    Map<String,List<String>> clusterVariantsMapping = new HashMap<String, List<String>>();

    clusterVariantsMapping.put(cluster1Name,clusterVariantsList);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "EastCoast";

    @SuppressWarnings("serial")
    Map<String,List<String>> serviceGroupsData = new HashMap<String,List<String>>()
    {{
        put("ServiceGroup1", Collections.singletonList(cluster1Variant1Name));
        put("ServiceGroup2", Collections.singletonList(cluster1Variant2Name));
      }};

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties,
                                                    serviceGroupsData);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo, clusterVariantsMapping);

    // It's hard to validate the serviceGroups without replicating all the temporary structures
    // needed inside D2Config. Just doing it manually here.
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_1", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_1Master", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-WestCoast", "service-1_1-WestCoast", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_1-EastCoast", "/service-1_1", "ServiceGroup1");

    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_2", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_2Master", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-WestCoast", "service-1_2-WestCoast", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_2-EastCoast", "/service-1_2", "ServiceGroup1");

    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_1", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_1Master", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-WestCoast", "service-1_1-WestCoast", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_1-EastCoast", "/service-1_1", "ServiceGroup2");

    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_2", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_2Master", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-WestCoast", "service-1_2-WestCoast", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_2-EastCoast", "/service-1_2", "ServiceGroup2");
  }

  // flip the order of the peerColos to make sure there's no order dependency
  @Test
  public static void testSingleColoClusterWithClusterVariants3()
    throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");

    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    final String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("EastCoast");
    peerColos.add("WestCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put(PropertyKeys.COLO_VARIANTS, peerColos);
    String masterColo = "EastCoast";
    clusterProperties.put(PropertyKeys.MASTER_COLO, masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);

    // add in clusterVariants
    Map<String,Map<String,Object>> clusterVariants = new HashMap<String, Map<String, Object>>();
    final String cluster1Variant1Name = "cluster1Foo";
    final String cluster1Variant2Name = "cluster1Bar";
    clusterVariants.put(cluster1Variant1Name, Collections.<String,Object>emptyMap());
    clusterVariants.put(cluster1Variant2Name, Collections.<String,Object>emptyMap());
    List<String> clusterVariantsList = new ArrayList<String>();
    clusterVariantsList.add(cluster1Variant1Name);
    clusterVariantsList.add(cluster1Variant2Name);
    clusterProperties.put(PropertyKeys.CLUSTER_VARIANTS, clusterVariants);
    Map<String,List<String>> clusterVariantsMapping = new HashMap<String, List<String>>();

    clusterVariantsMapping.put(cluster1Name,clusterVariantsList);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "EastCoast";

    @SuppressWarnings("serial")
    Map<String,List<String>> serviceGroupsData = new HashMap<String,List<String>>()
    {{
        put("ServiceGroup1", Collections.singletonList(cluster1Variant1Name));
        put("ServiceGroup2", Collections.singletonList(cluster1Variant2Name));
      }};

    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties,
                                                    serviceGroupsData);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo, clusterVariantsMapping);

    // It's hard to validate the serviceGroups without replicating all the temporary structures
    // needed inside D2Config. Just doing it manually here.
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_1", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_1Master", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-WestCoast", "service-1_1-WestCoast", "/service-1_1", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_1-EastCoast", "/service-1_1", "ServiceGroup1");

    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_2", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_2Master", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-WestCoast", "service-1_2-WestCoast", "/service-1_2", "ServiceGroup1");
    verifyServiceProperties("cluster1Foo-EastCoast", "service-1_2-EastCoast", "/service-1_2", "ServiceGroup1");

    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_1", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_1Master", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-WestCoast", "service-1_1-WestCoast", "/service-1_1", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_1-EastCoast", "/service-1_1", "ServiceGroup2");

    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_2", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_2Master", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-WestCoast", "service-1_2-WestCoast", "/service-1_2", "ServiceGroup2");
    verifyServiceProperties("cluster1Bar-EastCoast", "service-1_2-EastCoast", "/service-1_2", "ServiceGroup2");
  }

  @Test
  public static void testSingleColoClusterWithOneExcludedService() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    String excludedService = "service-1_22346";
    serviceList.add(excludedService);
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> excludeServiceList = Arrays.asList(excludedService);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("WestCoast");
    peerColos.add("EastCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put("coloVariants", peerColos);
    String masterColo = "WestCoast";
    clusterProperties.put("masterColo", masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "EastCoast";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties,
                                                    new HashMap<String,List<String>>(), excludeServiceList);

    assertEquals(d2Conf.runDiscovery(_zkHosts), 0);

    String coloClusterName = D2Utils.addSuffixToBaseName(cluster1Name, defaultColo);
    verifyClusterProperties(coloClusterName);
    verifyServiceProperties(coloClusterName, excludedService, "/" + excludedService, null);
    try
    {
      String badService = D2Utils.addSuffixToBaseName(excludedService, defaultColo);
      verifyServiceProperties(coloClusterName, badService, "/" + excludedService, null);
      Assert.fail("Shouldn't have see this service created: " + badService);
    }
    catch (NullPointerException e)
    {
      // this NPE is expected because that coloVariant of this service shouldn't exist
    }
  }

  @Test
  public static void testDefaultColoIsOneOfPeerColo() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("WestCoast");
    peerColos.add("EastCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put("coloVariants", peerColos);
    String masterColo = "WestCoast";
    clusterProperties.put("masterColo", masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "MiddleKingdom";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties);

    try
    {
      d2Conf.runDiscovery(_zkHosts);
      Assert.fail("Should have failed here because defaultColo isn't one of the peerColos");
    }
    catch (IllegalStateException e)
    {
      // expected because the defaultColo isn't one of the peerColos
    }
  }

  @Test
  public static void testMasterColoIsOneOfPeerColo() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service-1_1");
    serviceList.add("service-1_2");
    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    String cluster1Name = "cluster-1";
    clustersData.put(cluster1Name, serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add("WestCoast");
    peerColos.add("EastCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put("coloVariants", peerColos);
    String masterColo = "MiddleKingdom";
    clusterProperties.put("masterColo", masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    String defaultColo = "EastCoast";
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil( clustersData, defaultColo, clustersProperties);

    try
    {
      d2Conf.runDiscovery(_zkHosts);
      Assert.fail("Should have failed here because masterColo isn't one of the peerColos");
    }
    catch (IllegalStateException e)
    {
      // expected because the masterColo isn't one of the peerColos
    }
  }

  /**
   * In the colo EastCoast for a service available in colos {EastCoast, WestCoast} and where the
   * master colo is WestCoast verify that by default services use EastCoast and when configured for
   * default routing to master that they use WestCoast.
   */
  @Test
  public static void testDefaultRoutingToMaster() throws IOException, InterruptedException, URISyntaxException, Exception
  {
    final String masterColo = "WestCoast";
    final String defaultColo = "EastCoast";

    List<String> serviceList = new ArrayList<String>();
    serviceList.add("service1");
    serviceList.add("service2");

    @SuppressWarnings("serial")
    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    String cluster1Name = "cluster1";
    clustersData.put(cluster1Name, serviceList);

    List<String> clusterList = new ArrayList<String>();
    clusterList.add(cluster1Name);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();

    List<String> peerColos = new ArrayList<String>();
    peerColos.add("WestCoast");
    peerColos.add("EastCoast");
    Map<String,List<String>> peerColoList = new HashMap<String,List<String>>();
    peerColoList.put(cluster1Name, peerColos);
    clusterProperties.put("coloVariants", peerColos);
    clusterProperties.put("masterColo", masterColo);
    Map<String,String> masterColoList = new HashMap<String,String>();
    masterColoList.put(cluster1Name, masterColo);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);

    Set<String> servicesWithDefaultRoutingToMaster = Collections.singleton("service2");
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil(clustersData, defaultColo, clustersProperties,
                                                   servicesWithDefaultRoutingToMaster);
    d2Conf.runDiscovery(_zkHosts);

    // Verify default routing
    verifyServiceProperties("cluster1-EastCoast", "service1", "/service1", null);
    verifyServiceProperties("cluster1-WestCoast", "service2", "/service2", null);

    // Verify explicit routing
    verifyServiceProperties("cluster1-EastCoast", "service1-EastCoast", "/service1", null);
    verifyServiceProperties("cluster1-WestCoast", "service1-WestCoast", "/service1", null);
    verifyServiceProperties("cluster1-WestCoast", "service1Master", "/service1", null);
    verifyServiceProperties("cluster1-EastCoast", "service2-EastCoast", "/service2", null);
    verifyServiceProperties("cluster1-WestCoast", "service2-WestCoast", "/service2", null);
    verifyServiceProperties("cluster1-WestCoast", "service2Master", "/service2", null);
  }


  @Test
  public static void testServiceVariantsWithDefaultRoutingToMaster() throws Exception {
    final String masterColo = PropertyKeys.MASTER_COLO;
    final String defaultColo = PropertyKeys.DEFAULT_COLO;
    final String service1 = "service1";
    final String service2 = "service2";
    final String cluster1Name = "cluster1";
    final String clusterVariantName = "clusterVariant1";
    final String serviceGroupName = "serviceGroup1";

    List<String> serviceList = new ArrayList<String>();
    serviceList.add(service1);
    serviceList.add(service2);

    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    clustersData.put(cluster1Name, serviceList);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add(masterColo);
    peerColos.add(defaultColo);
    @SuppressWarnings("serial")
    Map<String,Object> clusterVariants = new HashMap<String,Object>()
    {{
        put(clusterVariantName,new HashMap<String,Object>());
      }};

    clusterProperties.put(PropertyKeys.COLO_VARIANTS, peerColos);
    clusterProperties.put(PropertyKeys.MASTER_COLO, masterColo);
    clusterProperties.put(PropertyKeys.CLUSTER_VARIANTS, clusterVariants);
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(cluster1Name, clusterProperties);
    @SuppressWarnings("serial")
    Map<String,Object> serviceGroup = new HashMap<String,Object>()
    {{
        put(PropertyKeys.TYPE, PropertyKeys.CLUSTER_VARIANTS_LIST);
        put(PropertyKeys.CLUSTER_LIST, Arrays.asList(new String[]{clusterVariantName}));
      }};
    Map<String, Object> serviceVariants = new HashMap<String, Object>();
    serviceVariants.put(serviceGroupName, serviceGroup);

    Set<String> servicesWithDefaultRoutingToMaster = Collections.singleton(service2);
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil(clustersData, defaultColo, clustersProperties,
        servicesWithDefaultRoutingToMaster);
    d2Conf.setServiceVariants(serviceVariants);

    d2Conf.runDiscovery(_zkHosts);

    // Verify default routing
    verifyServiceProperties(D2Utils.addSuffixToBaseName(clusterVariantName, defaultColo), service1, "/"+service1, serviceGroupName);
    verifyServiceProperties(D2Utils.addSuffixToBaseName(clusterVariantName, masterColo), service2, "/"+service2, serviceGroupName);
  }

  @Test
  public static void testServiceWithDefaultRoutingToMasterAndSymlinkEnabled() throws Exception
  {
    final String masterColo = PropertyKeys.MASTER_COLO;
    final String defaultColo = PropertyKeys.DEFAULT_COLO;
    final String service1 = "service1";
    final String service2 = "service2";
    final String clusterName = "cluster";

    List<String> serviceList = new ArrayList<String>();
    serviceList.add(service1);
    serviceList.add(service2);

    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    clustersData.put(clusterName, serviceList);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add(masterColo);
    peerColos.add(defaultColo);

    clusterProperties.put(PropertyKeys.COLO_VARIANTS, peerColos);
    clusterProperties.put(PropertyKeys.MASTER_COLO, masterColo);
    clusterProperties.put(PropertyKeys.ENABLE_SYMLINK, "true");
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(clusterName, clusterProperties);

    Set<String> servicesWithDefaultRoutingToMaster = Collections.singleton("service2");
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil(clustersData, defaultColo, clustersProperties,
        servicesWithDefaultRoutingToMaster);

    d2Conf.runDiscovery(_zkHosts);

    // verify master routing
    verifyServiceProperties(D2Utils.getSymlinkNameForMaster(clusterName), D2Utils.addMasterToBaseName(service1), "/"+service1, null);
    verifyServiceProperties(D2Utils.getSymlinkNameForMaster(clusterName), D2Utils.addMasterToBaseName(service2), "/"+service2, null);
    // verify default routing
    verifyServiceProperties(D2Utils.addSuffixToBaseName(clusterName, defaultColo), service1, "/"+service1, null);
    verifyServiceProperties(D2Utils.getSymlinkNameForMaster(clusterName), service2, "/"+service2, null);
  }

  @Test
  public static void testServiceVariantsWithDefaultRoutingToMasterAndSymlinkEnabled() throws Exception {
    final String masterColo = PropertyKeys.MASTER_COLO;
    final String defaultColo = PropertyKeys.DEFAULT_COLO;
    final String service1 = "service1";
    final String service2 = "service2";
    final String clusterName = "cluster";
    final String clusterVariantName = "clusterVariant";
    final String serviceGroupName = "serviceGroup";

    List<String> serviceList = new ArrayList<String>();
    serviceList.add(service1);
    serviceList.add(service2);

    Map<String,List<String>> clustersData = new HashMap<String,List<String>>();
    clustersData.put(clusterName, serviceList);

    Map<String,Object> clusterProperties = new HashMap<String,Object>();
    List<String> peerColos = new ArrayList<String>();
    peerColos.add(masterColo);
    peerColos.add(defaultColo);
    @SuppressWarnings("serial")
    Map<String,Object> clusterVariants = new HashMap<String,Object>()
    {{
        put(clusterVariantName,new HashMap<String,Object>());
      }};

    clusterProperties.put(PropertyKeys.COLO_VARIANTS, peerColos);
    clusterProperties.put(PropertyKeys.MASTER_COLO, masterColo);
    clusterProperties.put(PropertyKeys.CLUSTER_VARIANTS, clusterVariants);
    clusterProperties.put(PropertyKeys.ENABLE_SYMLINK, "true");
    Map<String,Map<String,Object>> clustersProperties = new HashMap<String,Map<String,Object>>();
    clustersProperties.put(clusterName, clusterProperties);
    @SuppressWarnings("serial")
    Map<String,Object> serviceGroup = new HashMap<String,Object>()
    {{
        put(PropertyKeys.TYPE, PropertyKeys.CLUSTER_VARIANTS_LIST);
        put(PropertyKeys.CLUSTER_LIST, Arrays.asList(new String[]{clusterVariantName}));
      }};
    Map<String, Object> serviceVariants = new HashMap<String, Object>();
    serviceVariants.put(serviceGroupName, serviceGroup);

    Set<String> servicesWithDefaultRoutingToMaster = Collections.singleton(service2);
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil(clustersData, defaultColo, clustersProperties,
        servicesWithDefaultRoutingToMaster);
    d2Conf.setServiceVariants(serviceVariants);

    d2Conf.runDiscovery(_zkHosts);

    // verify master routing
    verifyServiceProperties(D2Utils.getSymlinkNameForMaster(clusterVariantName), D2Utils.addMasterToBaseName(service1), "/"+service1, serviceGroupName);
    verifyServiceProperties(D2Utils.getSymlinkNameForMaster(clusterVariantName), D2Utils.addMasterToBaseName(service2), "/"+service2, serviceGroupName);
    // verify default routing
    verifyServiceProperties(D2Utils.addSuffixToBaseName(clusterVariantName, defaultColo), service1, "/"+service1, serviceGroupName);
    verifyServiceProperties(D2Utils.getSymlinkNameForMaster(clusterVariantName), service2, "/"+service2, serviceGroupName);
  }

  private static void verifyColoClusterAndServices(Map<String,List<String>> clustersData,
                                                   Map<String,List<String>> peerColoList,
                                                   Map<String,String> masterColoList, String defaultColo)
    throws IOException, InterruptedException, URISyntaxException, PropertyStoreException
  {
    verifyColoClusterAndServices(clustersData, peerColoList, masterColoList, defaultColo, null);
  }

  private static void verifyColoClusterAndServices(Map<String,List<String>> clustersData,
                                            Map<String,List<String>> peerColoList,
                                            Map<String,String> masterColoList, String defaultColo,
                                            Map<String,List<String>> clusterVariantsMap)
    throws IOException, InterruptedException, URISyntaxException, PropertyStoreException
  {
    for (Map.Entry<String,List<String>> entry : clustersData.entrySet())
    {
      String clusterName = entry.getKey();
      List<String> serviceList = entry.getValue();
      List<String> peerColos;
      peerColos = getOrCreatePeerList(clusterName, peerColoList);

      String masterColo;
      if( masterColoList != null)
      {
        masterColo = masterColoList.get(clusterName);
      }
      else
      {
        masterColo = null;
      }
      for (String colo : peerColos)
      {
        String coloClusterName = D2Utils.addSuffixToBaseName(clusterName, colo);

        for (String serviceName : serviceList)
        {
          String coloServiceName = D2Utils.addSuffixToBaseName(serviceName, colo);
          // yes, we don't need to check the masterServiceName for each service, but there's no harm
          String masterClusterName = D2Utils.addSuffixToBaseName(clusterName, ("".matches(colo) ? null :masterColo));
          String masterServiceName = D2Utils.addSuffixToBaseName(serviceName, ("".matches(colo) ? null :masterColo));
          String defaultClusterName = D2Utils.addSuffixToBaseName(clusterName, ("".matches(colo) ? null : defaultColo));
          verifyClusterProperties(coloClusterName);
          verifyServiceProperties(coloClusterName, coloServiceName, "/" + serviceName, null);
          verifyServiceProperties(masterClusterName, masterServiceName, "/" + serviceName, null);
          verifyServiceProperties(defaultClusterName, serviceName, "/" + serviceName, null);
        }

        if (clusterVariantsMap != null)
        {
          List<String> clusterVariants = clusterVariantsMap.get(clusterName);
          for(String varName : clusterVariants)
          {
            String coloVarName = D2Utils.addSuffixToBaseName(varName, colo);
            verifyClusterProperties(coloVarName);
            // eh, modifying input map, this is just unit test, need this below. Alternative is to create
            // another map, which I don't want to do.
            peerColoList.put(coloVarName, peerColos);
          }
        }
      } // end for each colo
    } // end for each cluster
  }

  private static List<String> getOrCreatePeerList(String clusterName, Map<String,List<String>> peerColoList)
  {
    List<String> peerColos;
    if ( peerColoList !=null)
    {
      peerColos = peerColoList.get(clusterName);
    }
    else
    {
      peerColos = null;
    }
    if (peerColos == null || peerColos.isEmpty())
    {
      // we add an empty String because we want to iterate the default colo
      peerColos = new ArrayList<String>();
      peerColos.add("");
    }
    return peerColos;
  }
}
