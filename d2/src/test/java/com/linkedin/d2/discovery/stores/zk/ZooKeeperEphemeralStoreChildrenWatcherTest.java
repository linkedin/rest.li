/*
   Copyright (c) 2017 LinkedIn Corp.

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
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.stores.PropertySetStringMerger;
import com.linkedin.d2.discovery.stores.PropertySetStringSerializer;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Tests for the Publisher part of the EphemeralStore, which keeps track of children of a node
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ZooKeeperEphemeralStoreChildrenWatcherTest
{
  private ZKConnection _zkClient;
  private ZKServer _zkServer;
  private int _port;
  private ExecutorService _executor = Executors.newSingleThreadExecutor();
  private PropertyEventBusImpl<Set<String>> _eventBus;
  private volatile Set<String> _outputData;
  private Map<String, String> _testData;

  @BeforeSuite
  public void setup() throws InterruptedException, ExecutionException, IOException
  {

    try
    {
      _zkServer = new ZKServer();
      _zkServer.startup();
      _port = _zkServer.getPort();
      _zkClient = new ZKConnection("localhost:" + _port, 5000);
      _zkClient.start();
    }
    catch (IOException e)
    {
      Assert.fail("unable to instantiate real zk server on port " + _port);
    }
  }

  @AfterSuite
  public void tearDown() throws IOException, InterruptedException
  {
    _zkClient.shutdown();
    _zkServer.shutdown();
    _executor.shutdown();
  }

  private void generateTestData()
  {
    _testData = new HashMap<>();
    _testData.put("/bucket/child-1", "1");
    _testData.put("/bucket/child-2", "2");
    _testData.put("/bucket/child-3", "3");
  }

  @BeforeMethod
  public void setupMethod()
    throws ExecutionException, InterruptedException, TimeoutException, KeeperException
  {
    generateTestData();
    FutureCallback<None> callback = new FutureCallback<>();

    _zkClient.ensurePersistentNodeExists("/bucket", callback);
    callback.get(5, TimeUnit.SECONDS);

    for (Map.Entry<String, String> entry : _testData.entrySet())
    {
      addNode(entry.getKey(), entry.getValue());
    }
  }

  private void addNode(String key, String value) throws InterruptedException, ExecutionException, TimeoutException, KeeperException
  {
    _zkClient.getZooKeeper().create(key, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
  }

  @AfterMethod
  public void tearDownMethod() throws ExecutionException, InterruptedException
  {
    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.removeNodeUnsafeRecursive("/bucket", callback);
    callback.get();
  }

  @Test
  public void testChildDataChangedNotNotified() throws IOException, InterruptedException, ExecutionException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperEphemeralStore<Set<String>> publisher =
      new ZooKeeperEphemeralStore<>(client, new PropertySetStringSerializer(),
        new PropertySetStringMerger(), "/");

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<Set<String>> subscriber = new PropertyEventSubscriber<Set<String>>()
    {
      @Override
      public void onInitialize(String propertyName, Set<String> propertyValue)
      {
        _outputData = propertyValue;
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Set<String> propertyValue)
      {
        addLatch.countDown();
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
        _eventBus = new PropertyEventBusImpl<>(_executor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }
    if (!initLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to publish initial property value");
    }

    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.setDataUnsafe("/bucket/child-1", "4".getBytes(), callback);
    callback.get();

    if (addLatch.await(2, TimeUnit.SECONDS))
    {
      Assert.fail("The EphemeralStore shouldn't watch for data change");
    }
    Assert.assertEquals(_outputData, new HashSet<>(_testData.values()));
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  @Test
  public void testChildNodeAdded() throws IOException, InterruptedException, ExecutionException, TimeoutException, KeeperException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperEphemeralStore<Set<String>> publisher =
      new ZooKeeperEphemeralStore<>(client, new PropertySetStringSerializer(),
        new PropertySetStringMerger(), "/");

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<Set<String>> subscriber = new PropertyEventSubscriber<Set<String>>()
    {
      @Override
      public void onInitialize(String propertyName, Set<String> propertyValue)
      {
        _outputData = propertyValue;
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Set<String> propertyValue)
      {
        _outputData = propertyValue;
        addLatch.countDown();
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
        _eventBus = new PropertyEventBusImpl<>(_executor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }
    if (!initLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to publish initial property value");
    }
    addNode("/bucket/child-4", "4");

    if (!addLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("didn't get notified for the new node");
    }
    _testData.put("/bucket/child-4", "4");
    Assert.assertEquals(_outputData, new HashSet<>(_testData.values()));
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  @Test
  public void testChildNodeRemoved() throws IOException, InterruptedException, ExecutionException, TimeoutException, KeeperException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperEphemeralStore<Set<String>> publisher =
      new ZooKeeperEphemeralStore<>(client, new PropertySetStringSerializer(),
        new PropertySetStringMerger(), "/");

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<Set<String>> subscriber = new PropertyEventSubscriber<Set<String>>()
    {
      @Override
      public void onInitialize(String propertyName, Set<String> propertyValue)
      {
        _outputData = propertyValue;
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Set<String> propertyValue)
      {
        _outputData = propertyValue;
        addLatch.countDown();
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
        _eventBus = new PropertyEventBusImpl<>(_executor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }
    if (!initLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to publish initial property value");
    }

    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.removeNodeUnsafe("/bucket/child-1", callback);
    callback.get();

    if (!addLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("didn't get notified for the removed node");
    }
    _testData.remove("/bucket/child-1");
    Assert.assertEquals(_outputData, new HashSet<>(_testData.values()));
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }
}
