/*
   Copyright (c) 2020 LinkedIn Corp.

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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.stores.PropertySetStringMerger;
import com.linkedin.d2.discovery.stores.PropertySetStringSerializer;
import com.linkedin.test.util.AssertionMethods;
import com.linkedin.test.util.ClockedExecutor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.pegasus.org.apache.zookeeper.CreateMode;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.ZooDefs;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the Publisher part of the EphemeralStore, which keeps track of children of a node
 * with batched read/watch
 *
 * @author Nizar Mankulangara (nmankulangara@linkedin.com)
 */
public class ZooKeeperEphemeralStoreChildrenDelayedWatcherTest
{
  private ZKConnection _zkClient;
  private ZKServer _zkServer;
  private int _port;
  private ClockedExecutor _clockedExecutor = new ClockedExecutor();
  private PropertyEventBusImpl<Set<String>> _eventBus;

  @BeforeSuite
  public void setup()
    throws InterruptedException
  {
    try
    {
      _zkServer = new ZKServer();
      _zkServer.startup();
      _port = _zkServer.getPort();
      _zkClient = getZookeeperConnection();
      _zkClient.start();
    }
    catch (IOException e)
    {
      Assert.fail("unable to instantiate real zk server on port " + _port);
    }
  }

  @AfterSuite
  public void tearDown()
    throws IOException, InterruptedException
  {
    _zkClient.shutdown();
    _zkServer.shutdown();
    _clockedExecutor.shutdown();
  }

  @BeforeMethod
  public void setupMethod()
    throws ExecutionException, InterruptedException, TimeoutException
  {
    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.ensurePersistentNodeExists("/bucket", callback);
    callback.get(5, TimeUnit.SECONDS);
  }

  private void addNodeInZookeeper(String key, String value)
    throws InterruptedException, KeeperException
  {
    _zkClient.getZooKeeper().create(key, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
  }

  @AfterMethod
  public void tearDownMethod()
    throws ExecutionException, InterruptedException
  {
    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.removeNodeUnsafeRecursive("/bucket", callback);
    callback.get();
  }

  @DataProvider
  public Object[][] dataNumOfChildrenReadWindow()
  {
    Object[][] data = new Object[100][2];
    for (int i = 0; i < 100; i++)
    {
      data[i][0] = ThreadLocalRandom.current().nextInt(100) + 1;
      data[i][1] = ThreadLocalRandom.current().nextInt(120000);
    }

    return data;
  }

  @DataProvider
  public Object[][] dataNumOfchildrenToAddToRemoveReadWindow()
  {
    ThreadLocalRandom localRandom = ThreadLocalRandom.current();

    Object[][] data = new Object[100][3];
    for (int i = 0; i < 100; i++)
    {
      int numberOfChildren = localRandom.nextInt(100) + 2;
      data[i][0] = numberOfChildren;
      data[i][1] = localRandom.nextInt(Math.max(numberOfChildren - 1, 0)) + 1;
      data[i][2] = localRandom.nextInt(120000);
    }

    return data;
  }

  @Test(dataProvider = "dataNumOfChildrenReadWindow")
  public void testChildNodeAdded(int numberOfAdditionalChildren, int zookeeperReadWindowMs)
    throws Exception
  {

    ZKConnection client = getZookeeperConnection();
    client.start();

    final ZooKeeperEphemeralStore<Set<String>> publisher = getEphemeralStorePublisher(zookeeperReadWindowMs, client);

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);

    final Set<String> childrenFromZookeeperPublisher = new HashSet<>();
    final PropertyEventSubscriber<Set<String>> subscriber = new PropertyEventSubscriber<Set<String>>()
    {
      @Override
      public void onInitialize(String propertyName, Set<String> propertyValue)
      {
        if (propertyValue != null)
        {
          childrenFromZookeeperPublisher.addAll(propertyValue);
        }

        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Set<String> propertyValue)
      {
        childrenFromZookeeperPublisher.clear();
        childrenFromZookeeperPublisher.addAll(propertyValue);

        if (propertyValue.size() == numberOfAdditionalChildren)
        {
          addLatch.countDown();
        }
      }

      @Override
      public void onRemove(String propertyName)
      {
      }
    };

    publisher.start(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
      }

      @Override
      public void onSuccess(None result)
      {
        _eventBus = new PropertyEventBusImpl<>(_clockedExecutor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }

    AssertionMethods.assertWithTimeout(5000, () -> {
      _clockedExecutor.runFor(0);
      Assert.assertEquals(initLatch.getCount(), 0, "unable to publish initial property value");
    });

    Map<String, String> childrenAddedToZookeeper = new HashMap<>();
    for (int i = 0; i < numberOfAdditionalChildren; i++)
    {
      addNodeInZookeeper("/bucket/child-" + i, Integer.toString(i));
      childrenAddedToZookeeper.put("/bucket/child-" + i, Integer.toString(i));
      _clockedExecutor.runFor(zookeeperReadWindowMs);
    }

    AssertionMethods.assertWithTimeout(5000, () -> {
      _clockedExecutor.runFor(zookeeperReadWindowMs);
      Assert.assertEquals(addLatch.getCount(), 0, "didn't get notified for the new node");
    });

    Assert.assertEquals(childrenFromZookeeperPublisher, new HashSet<>(childrenAddedToZookeeper.values()));
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  @Test(dataProvider = "dataNumOfchildrenToAddToRemoveReadWindow")
  public void testChildNodeRemoved(int numberOfAdditionalChildren, int numberOfRemove, int zookeeperReadWindowMs)
    throws Exception
  {
    ZKConnection client = getZookeeperConnection();
    client.start();

    final ZooKeeperEphemeralStore<Set<String>> publisher = getEphemeralStorePublisher(zookeeperReadWindowMs, client);

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch removeLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);

    final Set<String> childrenFromZookeeperPublisher = new HashSet<>();

    final PropertyEventSubscriber<Set<String>> subscriber = new PropertyEventSubscriber<Set<String>>()
    {
      @Override
      public void onInitialize(String propertyName, Set<String> propertyValue)
      {
        if (propertyValue != null)
        {
          childrenFromZookeeperPublisher.addAll(propertyValue);
        }

        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Set<String> propertyValue)
      {
        childrenFromZookeeperPublisher.clear();
        childrenFromZookeeperPublisher.addAll(propertyValue);

        if (propertyValue.size() == numberOfAdditionalChildren)
        {
          addLatch.countDown();
        }

        if (addLatch.getCount() == 0 && propertyValue.size() == (numberOfAdditionalChildren - numberOfRemove))
        {
          removeLatch.countDown();
        }
      }

      @Override
      public void onRemove(String propertyName)
      {
      }
    };

    publisher.start(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
      }

      @Override
      public void onSuccess(None result)
      {
        _eventBus = new PropertyEventBusImpl<>(_clockedExecutor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }

    AssertionMethods.assertWithTimeout(5000, () -> {
      _clockedExecutor.runFor(0);
      Assert.assertEquals(initLatch.getCount(), 0, "unable to publish initial property value");
    });

    Map<String, String> childrenAddedToZookeeper = new HashMap<>();
    for (int i = 0; i < numberOfAdditionalChildren; i++)
    {
      addNodeInZookeeper("/bucket/child-" + i, Integer.toString(i));
      childrenAddedToZookeeper.put("/bucket/child-" + i, Integer.toString(i));
      _clockedExecutor.runFor(zookeeperReadWindowMs);
    }

    AssertionMethods.assertWithTimeout(5000, () -> {
      _clockedExecutor.runFor(zookeeperReadWindowMs);
      Assert.assertEquals(addLatch.getCount(), 0, "didn't get notified for the new node");
    });

    Assert.assertEquals(childrenFromZookeeperPublisher, new HashSet<>(childrenAddedToZookeeper.values()));

    for (int i = 0; i < numberOfRemove; i++)
    {
      String childName = "/bucket/child-" + i;
      FutureCallback<None> callback = new FutureCallback<>();
      _zkClient.removeNodeUnsafe(childName, callback);
      childrenAddedToZookeeper.remove(childName);
      callback.get();

      _clockedExecutor.runFor(zookeeperReadWindowMs);
    }

    AssertionMethods.assertWithTimeout(5000, () -> {
      _clockedExecutor.runFor(zookeeperReadWindowMs);
      Assert.assertEquals(removeLatch.getCount(), 0, "didn't get notified for the removed nodes");
    });

    Assert.assertEquals(childrenFromZookeeperPublisher, new HashSet<>(childrenAddedToZookeeper.values()));
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  private ZKConnection getZookeeperConnection()
  {
    return new ZKConnection("localhost:" + _port, 5000);
  }

  private ZooKeeperEphemeralStore<Set<String>> getEphemeralStorePublisher(int zookeeperReadWindowMs, ZKConnection client)
    throws IOException
  {
    String tmpDataPath = LoadBalancerUtil.createTempDirectory("EphemeralStoreFileStore").getAbsolutePath();
    return new ZooKeeperEphemeralStore<>(client, new PropertySetStringSerializer(), new PropertySetStringMerger(), "/", false, true, tmpDataPath,
                                         _clockedExecutor, zookeeperReadWindowMs);
  }
}
