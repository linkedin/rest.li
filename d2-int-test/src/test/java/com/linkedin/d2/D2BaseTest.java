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

package com.linkedin.d2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.util.D2ConfigTestUtil;

public class D2BaseTest implements D2TestConstants
{
  private static final Logger _log = LoggerFactory.getLogger(D2BaseTest.class);

  protected static String generateMessage(String str)
  {
    return "MESSAGE:" + System.currentTimeMillis()+"."+str;
  }

  protected static String getHost(String hostPortString)
  {
    return hostPortString.split(":")[0];
  }

  protected static int getPort(String hostPortString)
  {
    return Integer.parseInt(hostPortString.split(":")[1]);
  }

  protected static void runDiscovery(String zkHosts,
                                     Map<String, List<String>> clusterData)
  throws Exception
  {
    D2ConfigTestUtil d2Conf = new D2ConfigTestUtil(clusterData);
    assertEquals(d2Conf.runDiscovery(zkHosts), 0);
  }

  protected static LoadBalancerEchoServer startEchoServer(String zkHost,
                                                          int zkPort,
                                                          String echoServerHost,
                                                          int echoServerPort,
                                                          String cluster,
                                                          String... services) throws Exception
  {
    LoadBalancerEchoServer echoServer = new LoadBalancerEchoServer(zkHost, zkPort, echoServerHost, echoServerPort, "http","/d2", cluster, services);
    echoServer.startServer();
    echoServer.markUp();

    assertFalse(echoServer.isStopped());

    return echoServer;
  }

  protected static LoadBalancerEchoServer startEchoServer(String zkHost,
                                                          int zkPort,
                                                          String echoServerHost,
                                                          int echoServerPort,
                                                          String cluster,
                                                          Map<Integer, Double> partitionWeight,
                                                          boolean disableEchoOutput,
                                                          String... services) throws Exception
  {
    _log.debug("Starting echo server " + echoServerHost + " " + echoServerPort + " in cluster " + cluster);
    LoadBalancerEchoServer echoServer = new LoadBalancerEchoServer(zkHost, zkPort, echoServerHost, echoServerPort,
        5000, "http","/d2", cluster, null, disableEchoOutput, services);
    echoServer.startServer();
    echoServer.markUp(partitionWeight);

    assertFalse(echoServer.isStopped());

    return echoServer;
  }

  public static void assertMatch(String result, String[] possibleResults)
  {
    int count =0;

    for (String possibleResult : possibleResults)
    {
      Pattern pattern = Pattern.compile(possibleResult);
      Matcher matcher = pattern.matcher(result);
      if (matcher.find())
      {
        count++;
        break;
      }
    }
    assertTrue(count > 0, "Actual:" + result + " was not found in the list of possible Expected results:" + print(possibleResults));
  }

  public void assertAllEchoServersRunning(List<LoadBalancerEchoServer> servers) throws Exception
  {
    for (LoadBalancerEchoServer server : servers)
    {
      assertFalse(server.isStopped(), "Echo server (port "+server.getPort()+") is stopped.");
    }
  }

  public boolean allEchoServersRunning(List<LoadBalancerEchoServer> servers) throws Exception
  {
    int count = 0;
    for (LoadBalancerEchoServer server : servers)
    {
     if (server.isStopped())
     {
       _log.info("Echo server (port "+server.getPort()+") is stopped.");
       count++;
     }
    }

    if (count == 0)
    {
      return true;
    }

    return false;
  }

  public void assertAllEchoServersRegistered(ZKConnection zkClient,
                                             String zkUriString,
                                             List<LoadBalancerEchoServer> servers) throws Exception
  {
    String stores = LoadBalancerClientCli.printStores(zkClient, zkUriString, "/d2");
    for (LoadBalancerEchoServer server : servers)
    {
      assertTrue(stores.contains(server.getHost() + ":" + server.getPort()), "Echo server (port " + server.getPort() + ") is not registered with zk quorum.\nStores:" +  stores);
    }
  }

  public void assertAllEchoServersUnregistered(ZKConnection zkClient,
                                               String zkUriString,
                                               List<LoadBalancerEchoServer> servers) throws Exception
  {
    String stores = LoadBalancerClientCli.printStores(zkClient, zkUriString, "/d2");
    for (LoadBalancerEchoServer server : servers)
    {
      assertFalse(stores.contains(server.getHost() + ":" + server.getPort()), "Echo server (port " + server.getPort() + ") is still registered with zk quorum.\nStores:" + stores);
    }
  }

  public void stopAllEchoServers(List<LoadBalancerEchoServer> servers) throws Exception
  {
    for (LoadBalancerEchoServer server : servers)
    {
      try
      {
        server.markDown();
      }
      catch (Exception e)
      {
        _log.warn("Failed to mark down echo server (port" + server.getPort()+").");
      }
      try
      {
        server.stopServer();
      }
      catch (Exception e)
      {
        _log.warn("Failed to shutdown echo server (port" + server.getPort() + ").", e);
      }
    }
  }

  public void assertQuorumProcessAllRequests(int num,
                                             String jsonConfigData,
                                             String zkUriString,
                                             LoadBalancerClientCli cli,
                                             DynamicClient client,
                                             String miscmsg) throws Exception
  {
    Map<String, Object> clustersData = D2ConfigTestUtil.getClusterServiceConfiguration(jsonConfigData);

    for (int j=0; j < num ; j++)
    {
      for (String clusterName : clustersData.keySet())
      {
        String msg = generateMessage(zkUriString);
        String response = null;
        @SuppressWarnings("unchecked")
        Map<String, Object> servicesData = (Map<String, Object>) clustersData.get(clusterName);
        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) servicesData.get("services");
        for (String service : services.keySet())
        {
          try
          {
            response = cli.sendRequest(client, clusterName, service, msg);
            assertTrue(response.contains(LoadBalancerEchoServer.getResponsePostfixString()),"No '"+LoadBalancerEchoServer.getResponsePostfixString()+"' found in response from "+clusterName+"/"+service+". Response:"+response);
            _log.error("Assert pass. Response contains "+LoadBalancerEchoServer.getResponsePostfixString());
          }
          catch (Exception e)
          {
            _log.error("Response for "+clusterName+"/"+service+" failed." + miscmsg + " Error:" + e + "\n",e);
            e.printStackTrace();
            throw new Exception(e);
          }
          _log.debug("Response for "+clusterName+"/"+service+":"+ response);
        }
      }
    }
  }

  public Map<String,AtomicInteger> sendRequests(int num,
                                                int ttlClusters,
                                                String zkUriString,
                                                LoadBalancerClientCli cli,
                                                DynamicClient client,
                                                Map<String,AtomicInteger> counts) throws Exception
  {
    for (int j=0; j < num ; j++)
    {
      for (int i=1; i <= ttlClusters; i++)
      {
        String msg = generateMessage(zkUriString);
        String response = null;

        try
        {
          response = cli.sendRequest(client, "cluster-"+i,"service-"+i+"_1", msg);
          assertTrue(response.contains(LoadBalancerEchoServer.getResponsePostfixString()),"No '"+LoadBalancerEchoServer.getResponsePostfixString()+"' found in response from cluster-"+i+"/service-"+i+"_1. Response:"+response);
          counts.get("passed").getAndIncrement();
        }
        catch (Exception e)
        {
          _log.error("Response for cluster-"+i+"/service-"+i+"_1 failed.\n Error:"+e, e);
          e.printStackTrace();
          counts.get("failed").getAndIncrement();
        }
      }
    }

    return counts;
  }

  public  Map<String, List<String>> generateClusterData(String[] clusters, int addOn)
  {
    Map<String, List<String>> clustersData = new HashMap<>();

    for (int i=0; i < clusters.length; i++)
    {
      clustersData.put("cluster-"+clusters[i], Arrays.asList(new String[] {"service-"+clusters[i]+"_" + (1 + addOn), "service-"+clusters[i]+"_" + (2 + addOn), "service-"+clusters[i]+"_" + (3 + addOn)}));
    }
    return clustersData;
  }

  public Map<String, Object> generatePartitionProperties(String regex, int keyRangeStart, int partitionCount, int partitionSize, String type)
  {
    final Map<String, Object> partitionProperties = new HashMap<>();
    Map<String, Object> map = new HashMap<>();
    map.put("partitionKeyRegex", regex);
    map.put("keyRangeStart", String.valueOf(keyRangeStart));
    map.put("partitionCount", String.valueOf(partitionCount));
    map.put("partitionSize", String.valueOf(partitionSize));
    map.put("partitionType", type);
    partitionProperties.put("partitionProperties", map);

    return partitionProperties;
  }

  protected <T> Map<LoadBalancerEchoServer,T> createLatencyDataHash(List<LoadBalancerEchoServer> servers, T[] latency)
  {
    Map<LoadBalancerEchoServer,T> hash = new HashMap<>();
    int count = 0;
    for (LoadBalancerEchoServer server: servers)
    {
      if (count < latency.length)
      {
        hash.put(server, latency[count++]);
      }
      else
      {
        break;
      }
    }
    return hash;
  }

  protected static Map<LoadBalancerEchoServer,Map<Integer, Double>> createServerWeightDataMap(List<LoadBalancerEchoServer> servers, int partitionId, Double[] weight)
  {
    Map<LoadBalancerEchoServer,Map<Integer, Double>> hash = new HashMap<>();
    int count = 0;
    for (LoadBalancerEchoServer server: servers)
    {
      if (count < weight.length)
      {
        Map<Integer, Double> partitionWeight = new HashMap<>();
        partitionWeight.put(Integer.valueOf(partitionId), weight[count]);
        hash.put(server, partitionWeight);
        count++;
      }
      else
      {
        break;
      }
    }

    return hash;
  }

  protected void assertServersWeighSetup(Map<LoadBalancerEchoServer,Map<Integer, Double> > hostWeightMatrix, LoadBalancerClientCli cli, String zkConnectionString) throws Exception
  {
    String stores = LoadBalancerClientCli.printStores(cli.getZKClient(), zkConnectionString, "/d2");

    for (LoadBalancerEchoServer server : hostWeightMatrix.keySet())
    {
      String str = server.getHost() + ": " + server.getPort() + "/cluster-\\d+=" + hostWeightMatrix.get(server);
      Pattern pattern = Pattern.compile(str, Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(stores);

      assertTrue(matcher.find(), "URIProperty '" + str + "' was not found is current active clusters.\n" + stores);
    }
  }

  protected Map<Integer,AtomicInteger> generateHostResponseCountMap(Map<String,String> responses)
  {
    Map<Integer,AtomicInteger> res = new HashMap<>();

    res.put(Integer.valueOf(ECHO_SERVER_PORT1_1), new AtomicInteger(0));
    res.put(Integer.valueOf(ECHO_SERVER_PORT1_2), new AtomicInteger(0));
    res.put(Integer.valueOf(ECHO_SERVER_PORT2_1), new AtomicInteger(0));
    res.put(Integer.valueOf(ECHO_SERVER_PORT2_2), new AtomicInteger(0));
    res.put(Integer.valueOf(FAILED), new AtomicInteger(0));

    for (String response : responses.values())
    {
      if (response.contains("FAILED"))
      {
        res.get(FAILED).getAndIncrement();
      }
      else
      {
        for (Integer port : res.keySet())
        {
          if (response.contains(port.toString()))
          {
            res.get(port).getAndIncrement();
            break;
          }
        }
      }
    }

    return res;
  }

  public static void markUpAllEchoServers(List<LoadBalancerEchoServer> servers) throws Exception
  {
    for (LoadBalancerEchoServer server: servers)
    {
      server.markUp();
    }
  }

  public static void markDownAllEchoServers(List<LoadBalancerEchoServer> servers) throws Exception
  {
    for (LoadBalancerEchoServer server: servers)
    {
      server.markDown();
    }
  }

  protected static void setAllNodesWeight(List<LoadBalancerEchoServer> servers,
                                          Map<Integer, Double> partitionWeight)
                                          throws PropertyStoreException,
                                          IOException
  {
    for (LoadBalancerEchoServer server: servers)
    {
      server.markUp(partitionWeight);
    }
  }

  public void cleanupTempDir()
  {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    File[] files = tmpDir.listFiles();

    for (File file : files)
    {
      if (file.getName().contains("temp-d2TmpFileStore") || file.getName().contains("0.zkdata") || file.getName().contains("0.zklog"))
      {
        try
        {
          FileUtils.deleteDirectory(file);
        }
        catch (IOException e)
        {
          _log.info("Failed to delete " + file.getAbsolutePath(), e);
        }
      }
    }
  }

  public void printEchoServersStatus(List<LoadBalancerEchoServer> echoServers)
  {
    for (LoadBalancerEchoServer server : echoServers)
    {
      _log.debug(" Echo Server " + server.getHost() + ":" + server.getPort() + " isStopped:" + server.isStopped());
    }
  }

  protected static String printDataMatrix(Map<LoadBalancerEchoServer,?> map)
  {
    StringBuilder sb = new StringBuilder();
    for (LoadBalancerEchoServer server : map.keySet())
    {
      sb.append(((sb.length() > 0) ? "," : ""));
      sb.append("Server Port:");
      sb.append(server.getPort());
      sb.append(",Weight:");
      sb.append(map.get(server));
    }

    return sb.toString();
  }

  protected static String print(String[] arr)
  {
    StringBuilder sb = new StringBuilder();
    for (String a : arr)
    {
      sb.append(((sb.length() > 0) ? "," : ""));
      sb.append(a);
    }

    return sb.toString();
  }
}
