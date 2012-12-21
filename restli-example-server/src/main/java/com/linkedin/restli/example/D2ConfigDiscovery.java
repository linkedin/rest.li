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
