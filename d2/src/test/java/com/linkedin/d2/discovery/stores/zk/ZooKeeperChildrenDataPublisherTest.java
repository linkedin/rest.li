package com.linkedin.d2.discovery.stores.zk;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import java.util.concurrent.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import static org.testng.Assert.assertEquals;


/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class ZooKeeperChildrenDataPublisherTest
{
  private ZKConnection  _zkClient;
  private ZKServer _zkServer;
  private int         _port;
  private ExecutorService _executor = Executors.newSingleThreadExecutor();
  private PropertyEventBus<Map<String, String>> _eventBus;
  private Map<String, String> _outputData;
  private Map<String, String> _testData;

  @BeforeSuite
  public void setup() throws InterruptedException, ExecutionException, IOException {
    _port = 11820;

    try
    {
      _zkServer = new ZKServer(_port);
      _zkServer.startup();
      _zkClient = new ZKConnection("localhost:" + _port, 5001);
      _zkClient.start();
    }
    catch (IOException e)
    {
      Assert.fail("unable to instantiate real zk server on port " + _port);
    }
  }


  @AfterSuite
  public void tearDown() throws IOException, InterruptedException {
    _zkClient.shutdown();
    _zkServer.shutdown();
    _executor.shutdown();
  }

  private void generateTestData()
  {
    _testData = new HashMap<>();
    _testData.put("bucket/child-1", "1");
    _testData.put("bucket/child-2", "2");
    _testData.put("bucket/child-3", "3");
  }

  @BeforeMethod
  public void setupMethod()
      throws ExecutionException, InterruptedException, TimeoutException
  {
    generateTestData();
    for (Map.Entry<String, String> entry : _testData.entrySet())
    {
      FutureCallback<None> callback = new FutureCallback<>();
      _zkClient.ensurePersistentNodeExists("/" + entry.getKey(), callback);
      callback.get(30, TimeUnit.SECONDS);
      FutureCallback<None> callback2 = new FutureCallback<>();
      _zkClient.setDataUnsafe("/" + entry.getKey(), entry.getValue().getBytes(), callback2);
      callback2.get(30, TimeUnit.SECONDS);
    }
  }

  @AfterMethod
  public void tearDownMethod() throws ExecutionException, InterruptedException {
    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.removeNodeUnsafeRecursive("/bucket", callback);
    callback.get();
  }

  @Test
  public void testPublishInitialize()
      throws InterruptedException, IOException, PropertyStoreException, ExecutionException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperChildrenDataPublisher<Map<String, String>, String> publisher =
        new ZooKeeperChildrenDataPublisher<>(client, new PropertyStringSerializer(), "/");

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<Map<String,String>> subscriber = new PropertyEventSubscriber<Map<String, String>>() {
      @Override
      public void onInitialize(String propertyName, Map<String, String> propertyValue) {
        _outputData = propertyValue;
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Map<String, String> propertyValue) {
      }

      @Override
      public void onRemove(String propertyName) {
      }
    };

    publisher.start(new Callback<None>() {
      @Override
      public void onError(Throwable e) {
        Assert.fail("publisher start onError called",e);
      }

      @Override
      public void onSuccess(None result) {
        _eventBus = new PropertyEventBusImpl<>(_executor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(60, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }

    if (!initLatch.await(60, TimeUnit.SECONDS)) {
      Assert.fail("unable to publish initial property value");
    }
    assertEquals(_outputData, _testData);
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  @Test
  public void testChildDataChanged() throws IOException, InterruptedException, ExecutionException {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperChildrenDataPublisher<Map<String, String>, String> publisher =
        new ZooKeeperChildrenDataPublisher<>(client, new PropertyStringSerializer(), "/");

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<Map<String,String>> subscriber = new PropertyEventSubscriber<Map<String, String>>() {
      @Override
      public void onInitialize(String propertyName, Map<String, String> propertyValue) {
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Map<String, String> propertyValue) {
        _outputData = propertyValue;
        addLatch.countDown();
      }

      @Override
      public void onRemove(String propertyName) {}
    };

    publisher.start(new Callback<None>() {
      @Override
      public void onError(Throwable e) {
      }

      @Override
      public void onSuccess(None result) {
        _eventBus = new PropertyEventBusImpl<>(_executor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(60, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }
    if (!initLatch.await(60, TimeUnit.SECONDS))
    {
      Assert.fail("unable to publish initial property value");
    }

    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.setDataUnsafe("/bucket/child-1", "4".getBytes(), callback);
    callback.get();

    if (!addLatch.await(60, TimeUnit.SECONDS)) {
      Assert.fail("unable to get publish initialized property value");
    }
    _testData.put("bucket/child-1", "4");
    assertEquals(_outputData, _testData);
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  @Test
  public void testChildDeletion() throws IOException, InterruptedException, ExecutionException {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperChildrenDataPublisher<Map<String, String>, String> publisher =
        new ZooKeeperChildrenDataPublisher<>(client, new PropertyStringSerializer(), "/");

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<Map<String,String>> subscriber = new PropertyEventSubscriber<Map<String, String>>() {
      @Override
      public void onInitialize(String propertyName, Map<String, String> propertyValue) {
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Map<String, String> propertyValue) {
        _outputData = propertyValue;
        addLatch.countDown();
      }

      @Override
      public void onRemove(String propertyName) {}
    };

    publisher.start(new Callback<None>() {
      @Override
      public void onError(Throwable e) {
      }

      @Override
      public void onSuccess(None result) {
        _eventBus = new PropertyEventBusImpl<>(_executor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(60, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }
    if (!initLatch.await(60, TimeUnit.SECONDS))
    {
      Assert.fail("unable to publish initial property value");
    }

    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.removeNodeUnsafe("/bucket/child-1", callback);
    callback.get();

    if (!addLatch.await(60, TimeUnit.SECONDS)) {
      Assert.fail("unable to get publish initialized property value");
    }
    _testData.remove("bucket/child-1");
    assertEquals(_outputData, _testData);
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  @Test
  public void testChildCreation() throws IOException, InterruptedException, ExecutionException {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperChildrenDataPublisher<Map<String, String>, String> publisher =
        new ZooKeeperChildrenDataPublisher<>(client, new PropertyStringSerializer(), "/");

    final CountDownLatch initLatch = new CountDownLatch(1);
    final CountDownLatch addLatch = new CountDownLatch(1);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final PropertyEventSubscriber<Map<String,String>> subscriber = new PropertyEventSubscriber<Map<String, String>>() {
      @Override
      public void onInitialize(String propertyName, Map<String, String> propertyValue) {
        initLatch.countDown();
      }

      @Override
      public void onAdd(String propertyName, Map<String, String> propertyValue) {
        _outputData = propertyValue;
        addLatch.countDown();
      }

      @Override
      public void onRemove(String propertyName) {}
    };

    publisher.start(new Callback<None>() {
      @Override
      public void onError(Throwable e) {
      }

      @Override
      public void onSuccess(None result) {
        _eventBus = new PropertyEventBusImpl<>(_executor, publisher);
        _eventBus.register(Collections.singleton("bucket"), subscriber);
        startLatch.countDown();
      }
    });

    if (!startLatch.await(60, TimeUnit.SECONDS))
    {
      Assert.fail("unable to start ZookeeperChildrenDataPublisher");
    }
    if (!initLatch.await(60, TimeUnit.SECONDS))
    {
      Assert.fail("unable to publish initial property value");
    }

    FutureCallback<None> callback = new FutureCallback<>();
    _zkClient.ensurePersistentNodeExists("/bucket/child-4", callback);
    callback.get();

    if (!addLatch.await(60, TimeUnit.SECONDS)) {
      Assert.fail("unable to get publish initialized property value");
    }
    _testData.put("bucket/child-4", "");
    assertEquals(_outputData, _testData);
    _eventBus.unregister(Collections.singleton("bucket"), subscriber);
    client.shutdown();
  }

  public class PropertyStringSerializer implements PropertySerializer<String>
  {
    @Override
    public String fromBytes(byte[] bytes)
    {
      if (bytes == null) return "";
      try
      {
        return new String(bytes, "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public byte[] toBytes(String property)
    {
      try
      {
        return property.getBytes("UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

}
