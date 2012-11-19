package com.linkedin.restli.example;


import com.linkedin.d2.balancer.util.LoadBalancerClientCli;

import java.io.File;


/**
 * @author Keren Jin
 */
public class D2ConfigDiscovery
{
  public static void main(String[] args) throws Exception
  {
    // write D2-related configuration to ZooKeeper
    // all the configuration here are permanent, no need to re-write as long as ZooKeeper still has the data
    final File configFile = new File(args[0]);
    final int discoveryResult = LoadBalancerClientCli.runDiscovery(ZOOKEEPER_HOSTNAME + ":" + ZOOKEEPER_PORT,
                                                                   ZOOKEEPER_BASE_PATH,
                                                                   configFile);
    if (discoveryResult != 0)
    {
      throw new RuntimeException("Unable to configure ZooKeeper. Subsequent D2 request may fail.");
    }
  }

  public static final String ZOOKEEPER_HOSTNAME = "localhost";
  public static final int ZOOKEEPER_PORT = 2121;
  public static final String ZOOKEEPER_BASE_PATH = "/d2";
}
