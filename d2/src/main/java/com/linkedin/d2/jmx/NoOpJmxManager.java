/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.servers.ZooKeeperAnnouncer;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperTogglingStore;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.management.ObjectName;

/**
 * Dummy JmxManager which doesn't actually register anything to JMX
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class NoOpJmxManager extends JmxManager
{

  public NoOpJmxManager()
  {
  }

  public synchronized void shutdown()
  {
  }

  public synchronized <T> NoOpJmxManager registerFileStore(String name, FileStore<T> store)
  {
    return this;
  }

  public synchronized <T> NoOpJmxManager registerZooKeeperPermanentStore(String name, ZooKeeperPermanentStore<T> store)
  {
    return this;
  }

  public synchronized <T> NoOpJmxManager registerZooKeeperEphemeralStore(String name, ZooKeeperEphemeralStore<T> store)
  {
    return this;
  }

  public synchronized <T> NoOpJmxManager registerZooKeeperTogglingStore(String name, ZooKeeperTogglingStore<T> store)
  {
    return this;
  }

  public synchronized NoOpJmxManager registerScheduledThreadPoolExecutor(String name, ScheduledThreadPoolExecutor executor)
  {
    return this;
  }

  public synchronized NoOpJmxManager registerZooKeeperServer(String name, ZooKeeperServer zkServer)
  {

    return this;
  }

  public synchronized NoOpJmxManager registerLoadBalancerState(String name, SimpleLoadBalancerState state)
  {
    return this;
  }

  public synchronized NoOpJmxManager registerLoadBalancer(String name, SimpleLoadBalancer balancer)
  {
    return this;
  }

  public synchronized NoOpJmxManager registerLoadBalancerStrategy(String name, LoadBalancerStrategy strategy)
  {
    return this;
  }

  public synchronized NoOpJmxManager registerLoadBalancerStrategyV3JmxBean(String name, DegraderLoadBalancerStrategyV3JmxMBean strategyJmx)
  {
    return this;
  }

  public synchronized NoOpJmxManager registerZooKeeperAnnouncer(String name, ZooKeeperAnnouncer announcer)
  {
    return this;
  }

  public synchronized NoOpJmxManager unregister(String name)
  {
    return this;
  }

  public void unregister(ObjectName oName)
  {
  }

  public ObjectName getName(String name)
  {
    throw new UnsupportedOperationException();
  }

  public void checkReg(Object o, String name)
  {
  }

  public boolean isRegistered(String name)
  {
    throw new UnsupportedOperationException();
  }
}
