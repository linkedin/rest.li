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
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.test.util.AssertionMethods;
import com.linkedin.test.util.ClockedExecutor;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the Publisher part of the PermanentStore (ServiceProperties and ClusterProperties),
 * which keeps track of children of a node with randomized read/watch to avoid thundering herd
 *
 * @author Nizar Mankulangara (nmankulangara@linkedin.com)
 */
public class ZooKeeperPermanentStoreDelayedWatcherTest
{
  private ZKConnection _zkClient;
  private ZKServer _zkServer;
  private int _port;
  private ClockedExecutor _clockedExecutor = new ClockedExecutor();
  private PropertyEventBusImpl<String> _eventBus;

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

  private void updateNodeData(String key, String value)
    throws InterruptedException, KeeperException
  {
    Stat stat = _zkClient.getZooKeeper().exists(key, false);
    _zkClient.getZooKeeper().setData(key, value.getBytes(), stat.getVersion());
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
  public Object[][] dataNumOfChangesReadWindow()
  {
    Object[][] data = new Object[100][2];
    for (int i = 0; i < 100; i++)
    {
      data[i][0] = ThreadLocalRandom.current().nextInt(100) + 1;
      data[i][1] = ThreadLocalRandom.current().nextInt(120000);
    }

    return data;
  }

  @Test(dataProvider = "dataNumOfChangesReadWindow")
  public void testNodeValueChangedWatchUpdates(int numberOfDataChanges, int readWindowMs)
    throws Exception
  {

    ZKConnection client = getZookeeperConnection();
    client.start();

    final ZooKeeperPermanentStore<String> publisher = getPermanentStorePublisher(readWindowMs, client);

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);

    final AtomicReference<String> dataFromZookeeperPublisher = new AtomicReference<>();
    final PropertyEventSubscriber<String> subscriber = new PropertyEventSubscriber<String>()
    {
      @Override
      public void onInitialize(String propertyName, String propertyValue)
      {
        if (propertyValue != null)
        {
          dataFromZookeeperPublisher.set(propertyValue);
        }

        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, String propertyValue)
      {
        dataFromZookeeperPublisher.set(propertyValue);
        if (propertyValue.equals(Integer.toString(numberOfDataChanges)))
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

    String valueUpdatedInZookeeper = null;
    for (int i = 1; i <= numberOfDataChanges; i++)
    {
      updateNodeData("/bucket", Integer.toString(i));
      valueUpdatedInZookeeper = Integer.toString(i);
      _clockedExecutor.runFor(readWindowMs);
    }

    AssertionMethods.assertWithTimeout(5000, () -> {
      _clockedExecutor.runFor(readWindowMs);
      Assert.assertEquals(addLatch.getCount(), 0, "didn't get notified for the updated node");
    });

    Assert.assertEquals(dataFromZookeeperPublisher.get(), valueUpdatedInZookeeper);
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  private ZKConnection getZookeeperConnection()
  {
    return new ZKConnection("localhost:" + _port, 5000);
  }

  private ZooKeeperPermanentStore<String> getPermanentStorePublisher(int readWindowMs, ZKConnection client)
    throws IOException
  {
    PropertySerializer<String> stringPropertySerializer = new PropertySerializer<String>()
    {
      @Override
      public byte[] toBytes(String property)
      {
        return property.getBytes();
      }

      @Override
      public String fromBytes(byte[] bytes)
        throws PropertySerializationException
      {
        if (bytes == null)
        {
          return "";
        }

        return new String(bytes);
      }
    };

    return new ZooKeeperPermanentStore<>(client, stringPropertySerializer, "/", _clockedExecutor, readWindowMs);
  }
}
