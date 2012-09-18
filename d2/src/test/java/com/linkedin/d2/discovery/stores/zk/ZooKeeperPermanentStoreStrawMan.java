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

package com.linkedin.d2.discovery.stores.zk;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.PropertyStringSerializer;

public class ZooKeeperPermanentStoreStrawMan
{
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws IOException, InterruptedException,
          PropertyStoreException
  {
    ZKConnection zkClient = new ZKConnection("localhost:2181", 1000);
    Set<String> listenTos = new HashSet<String>();
    ZooKeeperPermanentStore<String> zk =
        new ZooKeeperPermanentStore<String>(zkClient,
                                            new PropertyStringSerializer(),
                                            "/test/lb/test-property");

    listenTos.add("foo12");

    PropertyEventBus<String> bus = new PropertyEventBusImpl<String>(new PropertyEventThread("ZK test"), zk);

    bus.register(listenTos, new PropertyEventSubscriber<String>()
    {

      @Override
      public void onAdd(String propertyName, String propertyValue)
      {
        System.err.println("onAdd: " + propertyName + "\t" + propertyValue);
      }

      @Override
      public void onInitialize(String propertyName, String propertyValue)
      {
        System.err.println("onInitialize: " + propertyName + "\t" + propertyValue);
      }

      @Override
      public void onRemove(String propertyName)
      {
        System.err.println("onRemove: " + propertyName);
      }
    });

    zk.put("foo12", "TEST1");
    zk.put("foo12", "TEST2");
    zk.put("foo12", "TEST3");
    zk.put("foo12", "TEST4");
    zk.put("foo12", "TEST5");
    zk.remove("foo12");

    try
    {
      Thread.sleep(10000);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }

    zkClient.getZooKeeper().close();
  }
}
