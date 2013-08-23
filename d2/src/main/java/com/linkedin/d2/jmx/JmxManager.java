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

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.servers.ZooKeeperAnnouncer;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV2;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperTogglingStore;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import static com.linkedin.d2.discovery.util.LogUtil.warn;

public class JmxManager
{
  private static final Logger _log = LoggerFactory.getLogger(JmxManager.class);

  private final MBeanServer   _server;
  private final Set<ObjectName> _registeredNames = new HashSet<ObjectName>();

  public JmxManager()
  {
    _server = ManagementFactory.getPlatformMBeanServer();
  }

  public synchronized void shutdown()
  {
    // Copy the set to avoid ConcurrentModificationException since unregister
    // removes the object from the set
    for (ObjectName name : new HashSet<ObjectName>(_registeredNames))
    {
      unregister(name);
    }
  }

  public synchronized <T> JmxManager registerFileStore(String name, FileStore<T> store)
  {
    checkReg(new FileStoreJmx<T>(store), name);

    return this;
  }

  public synchronized <T> JmxManager registerZooKeeperPermanentStore(String name,
                                                                     ZooKeeperPermanentStore<T> store)
  {
    checkReg(new ZooKeeperPermanentStoreJmx<T>(store), name);

    return this;
  }

  public synchronized <T> JmxManager registerZooKeeperEphemeralStore(String name,
                                                                     ZooKeeperEphemeralStore<T> store)
  {
    checkReg(new ZooKeeperEphemeralStoreJmx<T>(store), name);

    return this;
  }

  public synchronized <T> JmxManager registerZooKeeperTogglingStore(String name,
                                                                    ZooKeeperTogglingStore<T> store)
  {
    checkReg(new ZooKeeperTogglingStoreJmx<T>(store), name);

    return this;
  }

  @Deprecated
  public synchronized JmxManager registerPropertyEventThread(String name,
                                                             PropertyEventThread thread)
  {
    checkReg(new PropertyEventThreadJmx(thread), name);

    return this;
  }

  public synchronized JmxManager registerScheduledThreadPoolExecutor(String name,
                                                                     ScheduledThreadPoolExecutor executor)
  {
    checkReg(new ScheduledThreadPoolExecutorJmx(executor), name);

    return this;
  }

  public synchronized JmxManager registerZooKeeperServer(String name,
                                                         ZooKeeperServer zkServer)
  {
    checkReg(new ZooKeeperServerJmx(zkServer), name);

    return this;
  }

  public synchronized JmxManager registerLoadBalancerState(String name,
                                                           SimpleLoadBalancerState state)
  {
    checkReg(new SimpleLoadBalancerStateJmx(state), name);

    return this;
  }

  public synchronized JmxManager registerLoadBalancer(String name,
                                                      SimpleLoadBalancer balancer)
  {
    checkReg(new SimpleLoadBalancerJmx(balancer), name);

    return this;
  }

  public synchronized JmxManager registerLoadBalancerStrategy(String name,
                                                              LoadBalancerStrategy strategy)
  {
    if (strategy instanceof DegraderLoadBalancerStrategyV2)
    {
      checkReg(new DegraderLoadBalancerStrategyV2Jmx((DegraderLoadBalancerStrategyV2) strategy),
          name);
    }
    else if (strategy instanceof DegraderLoadBalancerStrategyV3)
    {
      checkReg(new DegraderLoadBalancerStrategyV3Jmx((DegraderLoadBalancerStrategyV3) strategy), name);
    }
    else
    {
      warn(_log, "unable to register a jmx bean for unknown strategy: ", strategy);
    }

    return this;
  }

  // Register the jmx bean passed in with the jmx manager.
  public synchronized JmxManager registerLoadBalancerStrategyV2JmxBean(String name,
                                             DegraderLoadBalancerStrategyV2JmxMBean strategyJmx)
  {
    checkReg(strategyJmx, name);
    return this;
  }

  // Register the jmx bean passed in with the jmx manager.
  public synchronized JmxManager registerLoadBalancerStrategyV3JmxBean(String name,
                                                                       DegraderLoadBalancerStrategyV3JmxMBean strategyJmx)
  {
    checkReg(strategyJmx, name);
    return this;
  }

  public synchronized JmxManager registerZooKeeperAnnouncer(String name,
                                                            ZooKeeperAnnouncer announcer)
  {
    checkReg(new ZooKeeperAnnouncerJmx(announcer), name);

    return this;
  }

  public synchronized JmxManager unregister(String name)
  {
    ObjectName oName;
    try
    {
      oName = getName(name);
    }
    catch (Exception e)
    {
      _log.error("Failed to get MBean ObjectName for " + name, e);
      return this;
    }

    unregister(oName);

    return this;

  }

  public void unregister(ObjectName oName)
  {
    try
    {
      if (_server.isRegistered(oName))
      {
        _server.unregisterMBean(oName);
      }
      _registeredNames.remove(oName);
      _log.info("Unregistered MBean {}", oName);
    }
    catch (Exception e)
    {
      _log.warn("Failed to unregister MBean " + oName, e);
    }
  }

  public ObjectName getName(String name) throws MalformedObjectNameException
  {
    return new ObjectName("com.linkedin.d2:type=" + ObjectName.quote(name));
  }

  public void checkReg(Object o, String name)
  {
    ObjectName oName;
    try
    {
      oName = getName(name);
    }
    catch (Exception e)
    {
      _log.warn("Failed to get object name for {}", name);
      return;
    }

    unregister(oName);

    try
    {
      _server.registerMBean(o, oName);
      _registeredNames.add(oName);
      _log.info("Registered MBean {}", oName);
    }
    catch (Exception e)
    {
      _log.warn("Failed to register MBean with name " + oName, e);
    }
  }

  public boolean isRegistered(String name)
  {
    ObjectName oName;
    try
    {
      oName = getName(name);
      return _server.isRegistered(oName);
    }
    catch (Exception e)
    {
      _log.warn("Failed to get object name for {}", name);
      return false;
    }
  }
}
