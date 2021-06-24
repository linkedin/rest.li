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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.simulator;

import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;

import java.net.InetAddress;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Reporter;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class R2D2Server
{
  private final Map<String, List<LoadBalancerEchoServer>> _clusters;
  private final String _zookeeperHost = "127.0.0.1";
  private final int    _zookeeperPort = 2181;
  private final String      _basePath = "/echo/lb";

  public static void main(String[] args) throws Exception
  {
    new R2D2Server().run();
  }

  public R2D2Server() throws Exception
  {
    int port = 9876;
    _clusters = new HashMap<>();

    // create two clusters. ten servers each. three services per cluster.
    for (String clusterName : new String[] { "cluster-1", "cluster-2" })
    {
      List<LoadBalancerEchoServer> servers = new ArrayList<>();

      for (int i = 0; i < 10; ++i)
      {
        String[] serviceNames =
            new String[] { "service-1-" + clusterName, "service-2-" + clusterName,
                "service-3-" + clusterName };

        servers.add(new LoadBalancerEchoServer(_zookeeperHost, _zookeeperPort, InetAddress.getLocalHost().getHostName(), port++, "http", _basePath, clusterName, serviceNames));
      }

      _clusters.put(clusterName, servers);
    }
  }

  public void run() throws Exception
  {
    // start everything
    for (Map.Entry<String, List<LoadBalancerEchoServer>> servers : _clusters.entrySet())
    {
      List<String> schemes = new ArrayList<>();

      schemes.add("http");

      putCluster(new ClusterProperties(servers.getKey(), schemes));

      for (final LoadBalancerEchoServer server : servers.getValue())
      {
        for (int i = 1; i <= 3; ++i)
        {
          putService(new ServiceProperties("service-" + i + "-" + servers.getKey(),
                                           servers.getKey(),
                                           File.separator + "service-" + i + "-"
                                               + servers.getKey(),
                                           Arrays.asList("degrader")));
        }

        server.startServer();
        server.markUp();
      }
    }
  }

  private void putService(ServiceProperties serviceProperties) throws Exception
  {
    System.err.println("put: " + serviceProperties);

    ZKConnection client = new ZKConnection(_zookeeperHost+":"+_zookeeperPort, 30000);

    PropertyStore<ServiceProperties> store =
        new ZooKeeperPermanentStore<>(client,
                                      new ServicePropertiesJsonSerializer(),
                                      _basePath+"/services");

    store.put(serviceProperties.getServiceName(), serviceProperties);
    client.getZooKeeper().close();
  }

  private void putCluster(ClusterProperties clusterProperties) throws Exception
  {
    System.err.println("put: " + clusterProperties);

    ZKConnection client = new ZKConnection(_zookeeperHost+":"+_zookeeperPort, 30000);
    PropertyStore<ClusterProperties> store =
        new ZooKeeperPermanentStore<>(client,
                                      new ClusterPropertiesJsonSerializer(),
                                      _basePath + "/clusters");

    store.put(clusterProperties.getClusterName(), clusterProperties);
    client.getZooKeeper().close();
  }
}
