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

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.util.TestDataHelper;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
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

import static com.linkedin.d2.util.TestDataHelper.*;


/**
 * Tests for the Child Watcher of {@link ZooKeeperEphemeralStore}, which keeps track of the uri nodes under a cluster.
 * Also tests the publisher part that publishes to property event bus.
 */
public class ZooKeeperEphemeralStoreChildrenWatcherTest
{
  private ZKConnection _zkClient;
  private ZKServer _zkServer;
  private int _port;
  private ExecutorService _executor = Executors.newSingleThreadExecutor();
  private PropertyEventBusImpl<UriProperties> _eventBus;
  private volatile UriProperties _outputData;
  private Map<String, UriProperties> _testData;

  private static final String CHILD_PATH_1 = "/" + CLUSTER_NAME + "/child-1";
  private static final String CHILD_PATH_2 = "/" + CLUSTER_NAME + "/child-2";
  private static final String CHILD_PATH_3 = "/" + CLUSTER_NAME + "/child-3";
  private static final String CHILD_PATH_4 = "/" + CLUSTER_NAME + "/child-4";

  private static final UriPropertiesJsonSerializer SERIALIZER = new UriPropertiesJsonSerializer();
  private static final UriPropertiesMerger MERGER = new UriPropertiesMerger();

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
    _testData = new TreeMap<>();
    _testData.put(CHILD_PATH_1, PROPERTIES_1);
    _testData.put(CHILD_PATH_2, PROPERTIES_2);
    _testData.put(CHILD_PATH_3, PROPERTIES_3);
  }

  @BeforeMethod
  public void setupMethod()
    throws ExecutionException, InterruptedException, TimeoutException, KeeperException
  {
    generateTestData();
    FutureCallback<None> callback = new FutureCallback<>();

    _zkClient.ensurePersistentNodeExists("/" + CLUSTER_NAME, callback);
    callback.get(5, TimeUnit.SECONDS);

    for (Map.Entry<String, UriProperties> entry : _testData.entrySet())
    {
      addNode(entry.getKey(), entry.getValue());
    }
  }

  private void addNode(String key, UriProperties value) throws InterruptedException, ExecutionException, TimeoutException, KeeperException
  {
    _zkClient.getZooKeeper().create(key, SERIALIZER.toBytes(value), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
  }

  @AfterMethod
  public void tearDownMethod() throws ExecutionException, InterruptedException
  {
    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.removeNodeUnsafeRecursive("/" + CLUSTER_NAME, callback);
    callback.get();
  }

  @Test
  public void testChildDataChangedNotNotified() throws IOException, InterruptedException, ExecutionException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperEphemeralStore<UriProperties> publisher = getStore(client);
    TestDataHelper.MockD2ServiceDiscoveryEventHelper mockEventHelper = getMockD2ServiceDiscoveryEventHelper();
    publisher.setServiceDiscoveryEventHelper(mockEventHelper);

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<UriProperties> subscriber = new PropertyEventSubscriber<UriProperties>()
    {
      @Override
      public void onInitialize(String propertyName, UriProperties propertyValue)
      {
        _outputData = propertyValue;
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, UriProperties propertyValue)
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
        _eventBus.register(Collections.singleton(CLUSTER_NAME), subscriber);
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

    // 1 initial request succeeded
    mockEventHelper.verifySDStatusInitialRequestEvents(Collections.singletonList(CLUSTER_NAME), Collections.singletonList(true));
    // 3 markups
    mockEventHelper.verifySDStatusUpdateReceiptEvents(
        ImmutableSet.of(CLUSTER_NAME, CLUSTER_NAME, CLUSTER_NAME),
        ImmutableSet.of(HOST_1, HOST_2, HOST_3),
        ImmutableSet.of(PORT_1, PORT_2, PORT_3),
        ImmutableSet.of(CHILD_PATH_1, CHILD_PATH_2, CHILD_PATH_3),
        ImmutableSet.of(PROPERTIES_1.toString(), PROPERTIES_2.toString(), PROPERTIES_3.toString()),
        true
    );

    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.setDataUnsafe(CHILD_PATH_1, SERIALIZER.toBytes(PROPERTIES_4), callback);
    callback.get();

    if (addLatch.await(2, TimeUnit.SECONDS))
    {
      Assert.fail("The EphemeralStore shouldn't watch for data change");
    }
    // no more markups
    Assert.assertEquals(mockEventHelper._receiptMarkUpHosts.size(), 3);
    // 0 markdown
    Assert.assertEquals(mockEventHelper._receiptMarkDownHosts.size(), 0);
    Assert.assertEquals(_outputData, MERGER.merge(CLUSTER_NAME, _testData.values()));
    _eventBus.unregister(Collections.singleton(CLUSTER_NAME), subscriber);
    client.shutdown();
  }

  private ZooKeeperEphemeralStore<UriProperties> getStore(ZKConnection client) {
    return new ZooKeeperEphemeralStore<>(client, SERIALIZER, MERGER, "/", false, true);
  }

  @Test
  public void testChildNodeAdded() throws IOException, InterruptedException, ExecutionException, TimeoutException, KeeperException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperEphemeralStore<UriProperties> publisher = getStore(client);
    TestDataHelper.MockD2ServiceDiscoveryEventHelper mockEventHelper = getMockD2ServiceDiscoveryEventHelper();
    publisher.setServiceDiscoveryEventHelper(mockEventHelper);

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<UriProperties> subscriber = new PropertyEventSubscriber<UriProperties>()
    {
      @Override
      public void onInitialize(String propertyName, UriProperties propertyValue)
      {
        _outputData = propertyValue;
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, UriProperties propertyValue)
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
        _eventBus.register(Collections.singleton(CLUSTER_NAME), subscriber);
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
    addNode(CHILD_PATH_4, PROPERTIES_4);

    if (!addLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("didn't get notified for the new node");
    }

    // 1 initial request succeeded
    mockEventHelper.verifySDStatusInitialRequestEvents(Collections.singletonList(CLUSTER_NAME), Collections.singletonList(true));
    // 4 mark ups
    mockEventHelper.verifySDStatusUpdateReceiptEvents(
        ImmutableSet.of(CLUSTER_NAME, CLUSTER_NAME, CLUSTER_NAME, CLUSTER_NAME),
        ImmutableSet.of(HOST_1, HOST_2, HOST_3, HOST_4),
        ImmutableSet.of(PORT_1, PORT_2, PORT_3, PORT_4),
        ImmutableSet.of(CHILD_PATH_1, CHILD_PATH_2, CHILD_PATH_3, CHILD_PATH_4),
        ImmutableSet.of(PROPERTIES_1.toString(), PROPERTIES_2.toString(), PROPERTIES_3.toString(), PROPERTIES_4.toString()),
        true
    );
    _testData.put(CHILD_PATH_4, PROPERTIES_4);
    Assert.assertEquals(_outputData, MERGER.merge(CLUSTER_NAME, _testData.values()));
    _eventBus.unregister(Collections.singleton(CLUSTER_NAME), subscriber);
    client.shutdown();
  }

  @Test
  public void testChildNodeRemoved() throws IOException, InterruptedException, ExecutionException, TimeoutException, KeeperException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperEphemeralStore<UriProperties> publisher = getStore(client);
    TestDataHelper.MockD2ServiceDiscoveryEventHelper mockEventHelper = getMockD2ServiceDiscoveryEventHelper();
    publisher.setServiceDiscoveryEventHelper(mockEventHelper);

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<UriProperties> subscriber = new PropertyEventSubscriber<UriProperties>()
    {
      @Override
      public void onInitialize(String propertyName, UriProperties propertyValue)
      {
        _outputData = propertyValue;
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, UriProperties propertyValue)
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
        _eventBus.register(Collections.singleton(CLUSTER_NAME), subscriber);
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

    // 1 initial request succeeded
    mockEventHelper.verifySDStatusInitialRequestEvents(Collections.singletonList(CLUSTER_NAME), Collections.singletonList(true));
    // 3 markups
    mockEventHelper.verifySDStatusUpdateReceiptEvents(
        ImmutableSet.of(CLUSTER_NAME, CLUSTER_NAME, CLUSTER_NAME),
        ImmutableSet.of(HOST_1, HOST_2, HOST_3),
        ImmutableSet.of(PORT_1, PORT_2, PORT_3),
        ImmutableSet.of(CHILD_PATH_1, CHILD_PATH_2, CHILD_PATH_3),
        ImmutableSet.of(PROPERTIES_1.toString(), PROPERTIES_2.toString(), PROPERTIES_3.toString()),
        true
    );
    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.removeNodeUnsafe(CHILD_PATH_1, callback);
    callback.get();

    if (!addLatch.await(5, TimeUnit.SECONDS))
    {
      Assert.fail("didn't get notified for the removed node");
    }

    // 1 markdown
    mockEventHelper.verifySDStatusUpdateReceiptEvents(ImmutableSet.of(CLUSTER_NAME), ImmutableSet.of(HOST_1), ImmutableSet.of(1),
        ImmutableSet.of(CHILD_PATH_1), ImmutableSet.of(PROPERTIES_1.toString()), false);
    _testData.remove(CHILD_PATH_1);
    Assert.assertEquals(_outputData, MERGER.merge(CLUSTER_NAME, _testData.values()));
    _eventBus.unregister(Collections.singleton(CLUSTER_NAME), subscriber);
    client.shutdown();
  }
}
