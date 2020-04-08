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
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.stores.PropertySetStringMerger;
import com.linkedin.d2.discovery.stores.PropertySetStringSerializer;
import com.linkedin.test.util.AssertionMethods;
import com.linkedin.test.util.ClockedExecutor;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Tests for the Publisher part of the EphemeralStore with FileStore
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ZooKeeperEphemeralStoreChildrenWatcherWithFIleStoreTest
{
  private ZKConnection _zkClient;
  private ZKServer _zkServer;
  private int _port;
  private ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();
  private Map<String, String> _testData;

  /**
   * Test that the behavior of the Ephemeral Store with or without FileStore is the same, and even creating a new
   * ones while the disk is already populated
   */
  @Test
  public void testWithAndWithoutFileStoreSameBehaviour() throws Exception
  {
    Set<String> outputDataWithFileStore = new ConcurrentHashSet<>();
    Set<String> outputDataWithoutFileStore = new ConcurrentHashSet<>();
    String tmpDataPath = LoadBalancerUtil.createTempDirectory("EphemeralStoreFileStore").getAbsolutePath();

    // creating two ephemeral stores, and initializing them. The current state will be written on outputData
    Runnable shutdownRunnable = createEphemeralStore(outputDataWithFileStore, tmpDataPath);
    Runnable shutdownRunnable2 = createEphemeralStore(outputDataWithoutFileStore, null);

    retryCheckSame(outputDataWithFileStore, outputDataWithoutFileStore);

    // testing adding new nodes
    addNode("/bucket/child-4", "4");
    addNode("/bucket/child-5", "5");

    retryCheckSame(outputDataWithFileStore, outputDataWithoutFileStore);

    // testing removing a nodes
    removeNode("/bucket/child-1");

    retryCheckSame(outputDataWithFileStore, outputDataWithoutFileStore);

    retryCheckSame(new HashSet<>(_testData.values()), outputDataWithoutFileStore);

    // making some changes and starting a new ephemeral store to see if it picks up new changes
    addNode("/bucket/child-6", "6");
    addNode("/bucket/child-7", "7");
    removeNode("/bucket/child-5");

    Set<String> newOutputDataWithFileStore = new HashSet<>();
    Runnable shutdownRunnable3 = createEphemeralStore(newOutputDataWithFileStore, tmpDataPath);

    retryCheckSame(outputDataWithFileStore, outputDataWithoutFileStore);
    retryCheckSame(newOutputDataWithFileStore, outputDataWithoutFileStore);
    retryCheckSame(new HashSet<>(_testData.values()), outputDataWithoutFileStore);

    shutdownRunnable.run();
    shutdownRunnable2.run();
    shutdownRunnable3.run();
  }

  /**
   * Testing that if the node the store is listening gets delete, the behavior is consistent with and without FileStore
   */
  @Test
  public void testRecreatingNodeListening() throws Exception
  {
    Set<String> outputDataWithFileStore = new ConcurrentHashSet<>();
    Set<String> outputDataWithoutFileStore = new ConcurrentHashSet<>();
    String tmpDataPath = LoadBalancerUtil.createTempDirectory("EphemeralStoreFileStore").getAbsolutePath();

    // creating two ephemeral stores, and initializing them. The current state will be written on outputData
    Runnable shutdownRunnable = createEphemeralStore(outputDataWithFileStore, tmpDataPath);
    Runnable shutdownRunnable2 = createEphemeralStore(outputDataWithoutFileStore, null);

    // checking at same state
    retryCheckSame(outputDataWithFileStore, outputDataWithoutFileStore);
    retryCheckSame(new HashSet<>(_testData.values()), outputDataWithoutFileStore);

    removeNode("/bucket");
    _testData.clear();

    FutureCallback<None> callback = new FutureCallback<None>();
    _zkClient.ensurePersistentNodeExists("/bucket", callback);
    callback.get(5, TimeUnit.SECONDS);

    addNode("/bucket/child-6", "6");

    retryCheckSame(outputDataWithFileStore, outputDataWithoutFileStore);
    retryCheckSame(outputDataWithFileStore, new HashSet<>(_testData.values()));

    shutdownRunnable.run();
    shutdownRunnable2.run();
  }

  // ################################ test lifecycle section ################################

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
    _testData = new ConcurrentHashMap<>();
    _testData.put("/bucket/child-1", "1");
    _testData.put("/bucket/child-2", "2");
    _testData.put("/bucket/child-3", "3");
  }

  @BeforeMethod
  public void setupMethod()
    throws ExecutionException, InterruptedException, TimeoutException, KeeperException
  {
    generateTestData();

    FutureCallback<None> callback = new FutureCallback<None>();
    _zkClient.ensurePersistentNodeExists("/bucket", callback);
    callback.get(5, TimeUnit.SECONDS);

    for (Map.Entry<String, String> entry : _testData.entrySet())
    {
      addNode(entry.getKey(), entry.getValue());
    }
  }

  // ################################ test util section ################################

  private void addNode(String path, String value) throws InterruptedException, ExecutionException, TimeoutException, KeeperException
  {
    _testData.put(path, value);
    _zkClient.getZooKeeper().create(path, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
  }

  private void removeNode(String path) throws ExecutionException, InterruptedException, TimeoutException
  {
    _testData.remove(path);
    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.removeNodeUnsafeRecursive(path, callback);
    callback.get(5, TimeUnit.SECONDS);
  }

  @AfterMethod
  public void tearDownMethod() throws ExecutionException, InterruptedException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    _zkClient.removeNodeUnsafeRecursive("/bucket", callback);
    callback.get();
  }

  private void retryCheckSame(Set<String> outputData, Set<String> outputData2) throws Exception
  {
    AssertionMethods.assertWithTimeout(5000, () ->{
      Assert.assertEquals(outputData, outputData2);
    });
  }

  /**
   * Creates an ephemeral store and update outputData with the subscribed values
   */
  private Runnable createEphemeralStore(Set<String> outputData, String tmpFileStoreDataPath) throws IOException, InterruptedException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();
    final CountDownLatch startLatch = new CountDownLatch(1);

    final ZooKeeperEphemeralStore<Set<String>> publisher =
      new ZooKeeperEphemeralStore<>(client, new PropertySetStringSerializer(),
        new PropertySetStringMerger(), "/", false, true, tmpFileStoreDataPath,
          _executor, 500);
    final PropertyEventSubscriber<Set<String>> subscriber = new SubscriberToOutputData(outputData);

    publisher.start(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
      }

      @Override
      public void onSuccess(None result)
      {
        PropertyEventBusImpl<Set<String>> eventBus = new PropertyEventBusImpl<>(_executor, publisher);
        eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });
    if (!startLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }

    return () -> {
      try
      {
        FutureCallback<None> callback = new FutureCallback<>();
        publisher.shutdown(callback);
        callback.get(1, TimeUnit.SECONDS);
        client.shutdown();
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
        e.printStackTrace();
      }
    };

  }

  class SubscriberToOutputData implements PropertyEventSubscriber<Set<String>>
  {
    private Set<String> _outputData;

    SubscriberToOutputData(Set<String> outputData)
    {
      _outputData = outputData;
    }

    @Override
    public void onInitialize(String propertyName, Set<String> propertyValue)
    {
      _outputData.clear();
      _outputData.addAll(propertyValue);
    }

    @Override
    public void onAdd(String propertyName, Set<String> propertyValue)
    {
      _outputData.clear();
      _outputData.addAll(propertyValue);
    }

    @Override
    public void onRemove(String propertyName)
    {
    }
  }
}
