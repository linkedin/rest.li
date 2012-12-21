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

package com.linkedin.restli.example.impl;


import com.linkedin.d2.balancer.servers.ZKUriStoreFactory;
import com.linkedin.d2.balancer.servers.ZooKeeperAnnouncer;
import com.linkedin.d2.balancer.servers.ZooKeeperConnectionManager;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;


/**
 * @author Keren Jin
 */
public class ZooKeeperConnectionBuilder
{
  public ZooKeeperConnectionBuilder setZooKeeperHostname(String zkHostname)
  {
    _zkHostname = zkHostname;
    return this;
  }

  public ZooKeeperConnectionBuilder setZooKeeperPort(int zkPort)
  {
    _zkPort = zkPort;
    return this;
  }

  public ZooKeeperConnectionBuilder setSessionTimeout(int sessionTimeoutInMs)
  {
    _sessionTimeoutInMs = sessionTimeoutInMs;
    return this;
  }

  public ZooKeeperConnectionBuilder setBasePath(String basePath)
  {
    _basePath = basePath;
    return this;
  }

  public ZooKeeperConnectionBuilder setCluster(String clusterName)
  {
    _announcer.setCluster(clusterName);
    return this;
  }

  public ZooKeeperConnectionBuilder setUri(String uri)
  {
    _announcer.setUri(uri);
    return this;
  }

  public ZooKeeperConnectionManager build()
  {
    _announcer.setWeight(1d);

    return new ZooKeeperConnectionManager(_zkHostname + ":" + _zkPort,
                                          _sessionTimeoutInMs,
                                          _basePath,
                                          new ZKUriStoreFactory(),
                                          _announcer);
  }

  private String _zkHostname = "localhost";
  private int _zkPort = 2121;
  private int _sessionTimeoutInMs = 5000;
  private String _basePath = "/d2";
  private ZooKeeperAnnouncer _announcer = new ZooKeeperAnnouncer(new ZooKeeperServer());
}
