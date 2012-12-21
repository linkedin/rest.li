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

package com.linkedin.d2.balancer.util;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.linkedin.d2.discovery.stores.zk.ZKServer;

public class TestLoadBalancerClientCli
{
  private static final Logger                  _log = LoggerFactory.getLogger(TestLoadBalancerClientCli.class);
  private static ZKServer                           _zkServer;
  private LoadBalancerClientCli                     _cli;
  private static String                             _zkHosts;
  private static final String               ZK_HOST = "127.0.0.1";
  private static final File _d2ConfigFileName = new File("d2config.json");

  private static final String configData =
    "{" +
      "\"clusterDefaults\": {\"prioritizedSchemes\":[\"http\"], \"properties\":{\"requestTimeout\":10000}}," +
      "\"serviceDefaults\": { \"loadBalancerStrategyProperties\":{\"maxClusterLatencyWithoutDegrading\":500, \"updateIntervalsMs\":5000, \"defaultSuccessfulTransmissionWeight\":1.0, \"pointsPerWeight\":100}, \"loadBalancerStrategyList\":[\"degraderV3\"]}," +
      "\"serviceVariants\":{}," +
      "\"extraClusterServiceConfigurations\":" +
      "{" +
        "\"cluster-5\":{\"services\":{\"service-5_1\":{\"path\":\"/service-5_1\"}}}," +
        "\"PartitionedCluster-6\":{\"services\":{\"service-6_1\":{\"path\":\"/service-6_1\", \"loadBalancerStrategyList\":[\"degraderV3\"]}}, \"partitionProperties\":{\"partitionSize\":\"100\", \"keyRangeStart\":\"0\", \"partitionKeyRegex\":\"/(\\\\d+)\",\"partitionType\":\"RANGE\", \"partitionCount\":\"2\"}}" +
      "}," +
      "\"clusterServiceConfigurations\":" +
      "{" +
        "\"cluster-1\":{\"services\":{\"service-1_1\":{\"path\":\"/service-1_1\"}, \"service-1_2\":{\"path\":\"/service-1_2\"}, \"service-1_3\":{\"path\":\"/service-1_3\"}}}," +
        "\"cluster-2\":{\"services\":" +
        "{\"service-2_1\":{ \"loadBalancerStrategyProperties\":{\"hashConfig\":{\"regexes\":[\"key=([A-Za-z0-9\\\\+\\\\/\\\\=\\\\-\\\\_]+)\"]}, \"hashMethod\":\"uriRegex\"}, \"path\":\"/service-2_1\", \"loadBalancerStrategyList\":[\"degraderV2\", \"degrader\"]}, " +
        "\"service-2_2\":{ \"loadBalancerStrategyProperties\":{\"hashConfig\":{\"regexes\":[\"key=(-?\\\\d+)\"]}, \"hashMethod\":\"uriRegex\"}, \"path\":\"/service-2_2\", \"loadBalancerStrategyList\":[\"degraderV2\", \"degrader\"]}}}," +
        "\"cluster-3\":{\"services\":{\"service-3_1\":{\"path\":\"/service-3_1\"}, \"service-3_2\":{ \"loadBalancerStrategyProperties\":{\"hashConfig\":{\"regexes\":[\"/service-3_2/(\\\\d+)\"]}, \"hashMethod\":\"uriRegex\"}, \"path\":\"/service-3_2\", \"loadBalancerStrategyList\":[\"degraderV2\", \"degrader\"]}}}," +
       "\"PartitionedCluster-4\":{\"services\":{\"generalService\":{\"partitioned\":true, \"path\":\"/generalService\"}, \"unpartitionedService\":{\"path\":\"/generalService\"}}, \"partitionedClusterList\":{\"Cluster3\":{}, \"Cluster2\":{}, \"Cluster1\":{}}}" +
      "}" +
    "}";

  @BeforeMethod
  public void testSetup() throws IOException, Exception
  {
    // Startup zookeeper server
    try
    {
      _zkServer = new ZKServer();
      _zkServer.startup();
      _zkHosts = ZK_HOST+":" + _zkServer.getPort();
    }
    catch (IOException e)
    {
      fail("unable to instantiate real zk server .");
      e.printStackTrace();
    }
  }

  @AfterMethod
  public void teardown() throws IOException, InterruptedException
  {
    try
    {
      _zkServer.shutdown(true);
      _log.info("Executed zkServer shutdown. ");
    }
    catch (Exception e)
    {
      _log.info("zk server shutdown failed.");
    }
    try
    {
      _cli.shutdown();
    }
    catch (Exception e)
    {
    }
    try
    {
      _d2ConfigFileName.delete();
    }
    catch (Exception e)
    {
    }
  }

  @Test
  public void testCliWithD2ConfigString() throws Exception
  {
    _cli = new LoadBalancerClientCli(_zkHosts, "/d2");
    _cli.runDiscovery(_zkHosts, "/d2", configData);

    validate(LoadBalancerClientCli.printStores(_cli.getZKClient(), "zk://"+_zkHosts, "/d2"));
  }

  @Test
  public void testCliWithD2ConfigFile() throws Exception
  {
    final PrintStream out = new PrintStream(new FileOutputStream(_d2ConfigFileName));

    try
    {
      out.print(configData);
    }
    finally
    {
      out.close();
    }

    _cli = new LoadBalancerClientCli(_zkHosts, "/d2");
    _cli.runDiscovery(_zkHosts, "/d2", _d2ConfigFileName);

    validate(LoadBalancerClientCli.printStores(_cli.getZKClient(), "zk://"+_zkHosts, "/d2"));
  }

  private void validate(String stores)
  {
    _log.info("\n\nstores:"+stores);
    System.out.println("\n\nstores:"+stores);
    // cluster-1
    validateClusterProperties(stores, "cluster-1");
    validateServiceProperties(stores, "cluster-1", "service-1_1", "service-1_1", "degraderV3");
    validateServiceProperties(stores, "cluster-1", "service-1_2", "service-1_2", "degraderV3");
    validateServiceProperties(stores, "cluster-1", "service-1_3", "service-1_3", "degraderV3");

    // cluster-2
    validateClusterProperties(stores, "cluster-2");
    validateServiceProperties(stores, "cluster-2", "service-2_1", "service-2_1", "degraderV2, degrader", "uriRegex", "key=([A-Za-z0-9\\+\\/\\=\\-\\_]+)");
    validateServiceProperties(stores, "cluster-2", "service-2_2", "service-2_2", "degraderV2, degrader", "uriRegex", "key=(-?\\d+)");

    // cluster-3
    validateClusterProperties(stores, "cluster-3");
    validateServiceProperties(stores, "cluster-3", "service-3_1", "service-3_1", "degraderV3");
    validateServiceProperties(stores, "cluster-3", "service-3_2", "service-3_2", "degraderV2, degrader", "uriRegex", "/service-3_2/(\\d+)");

    // PartitionedCluster-4
    validateClusterProperties(stores, "PartitionedCluster-4");

    validateServiceProperties(stores, "PartitionedCluster-4", "generalService", "generalService", "degraderV3");
    validateServiceProperties(stores, "PartitionedCluster-4", "unpartitionedService", "generalService", "degraderV3");

    // cluster-5
    validateClusterProperties(stores, "cluster-5");
    validateServiceProperties(stores, "cluster-5", "service-5_1", "service-5_1", "degraderV3");

    // PartitionedCluster-6
    validateClusterProperties(stores, "PartitionedCluster-6");
    validateServiceProperties(stores, "PartitionedCluster-6", "service-6_1", "service-6_1", "degraderV3");
  }

  private void validateClusterProperties(String stores, String clusterName)
  {
    String clusterProps = stores.substring(stores.indexOf("Cluster '" + clusterName +"':ClusterProperties"));

    if (clusterProps != null)
    {
      assertContains(clusterProps,"Cluster '" + clusterName +"':ClusterProperties [_clusterName=" + clusterName);
      assertContains(clusterProps,"_prioritizedSchemes=[http]");
      assertContains(clusterProps,"_banned=[]");
      assertContains(clusterProps,"_partitionProperties=com.linkedin.d2.balancer.properties.NullPartitionProperties");
    }
  }

  private void validateServiceProperties(String stores, String clusterName, String serviceName, String servicePath, String loadBalancerStrategyList, String hashMethod, String hashConfig)
  {
    String serviceProps = stores.substring(stores.indexOf("Cluster '" + clusterName +"' UriProperties:nullService '" + serviceName + "':ServiceProperties"));

    if (serviceProps != null)
    {
      validateServiceProperties(stores, clusterName, serviceName,servicePath, loadBalancerStrategyList);
      assertContains(serviceProps,"hashMethod=" + hashMethod);
      assertContains(serviceProps,"hashConfig={regexes=[" + hashConfig + "]}");
    }
  }

  private void validateServiceProperties(String stores, String clusterName, String serviceName, String servicePath, String loadBalancerStrategyList)
  {
    String serviceProps = stores.substring(stores.indexOf("Cluster '" + clusterName +"' UriProperties:nullService '" + serviceName + "':ServiceProperties"));

    if (serviceProps != null)
    {
      assertContains(serviceProps,"Cluster '" + clusterName +"' UriProperties:nullService '" + serviceName + "':ServiceProperties [_clusterName=" + clusterName);
      assertContains(serviceProps,"_path=/" + servicePath);
      assertContains(serviceProps,"_serviceName=" + serviceName);
      assertContains(serviceProps,"_loadBalancerStrategyList=[" + loadBalancerStrategyList + "]");
      assertContains(serviceProps,"maxClusterLatencyWithoutDegrading=500");
      assertContains(serviceProps,"updateIntervalsMs=5000");
      assertContains(serviceProps,"defaultSuccessfulTransmissionWeight=1.0");
      assertContains(serviceProps,"pointsPerWeight=100");
    }
  }

  private void assertContains(String string, String substring)
  {
    assertTrue(string.contains(substring), "\nMismatch. Expected to find substring='" + substring + "' in string '" + string);
  }

}
