package com.linkedin.d2.balancer.servers;

import com.linkedin.test.util.retry.ThreeRetries;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.callback.MultiCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZKConnectionBuilder;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.test.util.AssertionMethods;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;



/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 * @author Ang Xu
 */
public class ZookeeperConnectionManagerTest
{
  private static final Logger LOG = LoggerFactory.getLogger(ZookeeperConnectionManagerTest.class);

  public static final int PORT = 11811;

  protected ZKServer _zkServer;
  private String _uri;
  private String _cluster;
  private int testId = 0;
  private static final double WEIGHT = 0.5d;
  private static final int PARTITION1_ID = 1;
  private static final int PARTITION2_ID = 2;
  private static final double PARTITION1_WEIGHT = 1.5d;
  private static final double PARTITION2_WEIGHT = 2.5d;

  @BeforeMethod
  public void setUp() throws InterruptedException
  {
    LOG.info("Starting ZK");
    try
    {
      _zkServer = new ZKServer(PORT);
      _zkServer.startup();
    }
    catch (IOException e)
    {
      fail("unable to instantiate real zk server on port " + PORT);
    }

    testId++;
    _uri = "http://cluster-" + testId + "/test";
    _cluster = "cluster-" + testId;
  }

  @AfterMethod
  public void tearDown() throws IOException
  {
    LOG.info("Stopping ZK");
    _zkServer.shutdown();
  }

  @Test
  public void testMarkUp()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);

    ZooKeeperConnectionManager manager = createManager(true, announcer);

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(_cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), WEIGHT);
    assertEquals(properties.Uris().size(), 1);

    shutdownManager(manager);
  }

  @Test
  public void testMarkUpWithMultiPartition()
      throws Exception
  {
    double newWeight = 10d;
    ZooKeeperAnnouncer announcer = getZooKeeperMultiPartitionAnnouncer(_cluster, _uri, PARTITION1_ID, PARTITION2_ID, PARTITION1_WEIGHT,  PARTITION2_WEIGHT);

    try
    {
      announcer.setWeight(newWeight);
      Assert.fail("The operation should not be supported since we don't know for which partition we should change weight for.");
    }
    catch(IllegalArgumentException ex)
    {
      // Success
    }

    ZooKeeperConnectionManager manager = createManager(true, announcer);

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(_cluster);
    assertNotNull(properties);
    assertNotEquals(properties.getPartitionDataMap(URI.create(_uri)).get(PARTITION1_ID).getWeight(), newWeight);
    assertNotEquals(properties.getPartitionDataMap(URI.create(_uri)).get(PARTITION2_ID).getWeight(), newWeight);
    assertNull(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID));
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(PARTITION1_ID).getWeight(), PARTITION1_WEIGHT);
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(PARTITION2_ID).getWeight(), PARTITION2_WEIGHT);
    assertEquals(properties.Uris().size(), 1);

    shutdownManager(manager);
  }

  @Test
  public void testMarkUpWithSinglePartition()
      throws Exception
  {
    double newWeight = 10d;
    ZooKeeperAnnouncer announcer = getZooKeeperSinglePartitionAnnouncer(_cluster, _uri, PARTITION1_ID, PARTITION1_WEIGHT);
    announcer.setWeight(newWeight);

    ZooKeeperConnectionManager manager = createManager(true, announcer);

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(_cluster);
    assertNotNull(properties);
    assertNull(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID));
    assertNotEquals(properties.getPartitionDataMap(URI.create(_uri)).get(PARTITION1_ID).getWeight(), PARTITION1_WEIGHT);
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(PARTITION1_ID).getWeight(), newWeight);
    assertEquals(properties.Uris().size(), 1);

    shutdownManager(manager);
  }

  @Test
  public void testDelayMarkUp()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(new ZooKeeperServer(), false);
    announcer.setCluster(_cluster);
    announcer.setUri(_uri);
    Map<Integer, PartitionData> partitionWeight = new HashMap<>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(ZookeeperConnectionManagerTest.WEIGHT));
    announcer.setPartitionData(partitionWeight);

    ZooKeeperConnectionManager manager = createManager(true, announcer);

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(_cluster);
    assertNull(properties);

    FutureCallback<None> markUpCallback = new FutureCallback<>();
    announcer.markUp(markUpCallback);
    markUpCallback.get();

    UriProperties propertiesAfterMarkUp = store.get(_cluster);
    assertNotNull(propertiesAfterMarkUp);
    assertEquals(propertiesAfterMarkUp.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(),
                 ZookeeperConnectionManagerTest.WEIGHT);
    assertEquals(propertiesAfterMarkUp.Uris().size(), 1);

    shutdownManager(manager);
  }

  @Test
  public void testMarkUpAndMarkDown()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);

    ZooKeeperConnectionManager manager = createManager(true, announcer);

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(_cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), WEIGHT);
    assertEquals(properties.Uris().size(), 1);

    FutureCallback<None> markDownCallback = new FutureCallback<>();
    announcer.markDown(markDownCallback);
    markDownCallback.get();

    properties = store.get(_cluster);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 0);

    shutdownManager(manager);
  }

  @Test
  public void testMarkUpDuringDisconnection()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);

    ZooKeeperConnectionManager manager = createManager(false, announcer);

    _zkServer.shutdown(false);

    FutureCallback<None> managerStartCallback = new FutureCallback<>();
    manager.start(managerStartCallback);

    _zkServer.restart();
    managerStartCallback.get();

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(_cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), WEIGHT);
    assertEquals(properties.Uris().size(), 1);

    shutdownManager(manager);
  }

  @Test
  public void testMarkDownDuringDisconnection()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);

    ZooKeeperConnectionManager manager = createManager(true, announcer);

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(_cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), WEIGHT);
    assertEquals(properties.Uris().size(), 1);

    _zkServer.shutdown(false);

    FutureCallback<None> markDownCallback = new FutureCallback<>();
    announcer.markDown(markDownCallback);

    // ugly, but we need to wait for a while just so that Disconnect event is propagated
    // to the caller before we restart zk sever.
    Thread.sleep(1000);
    _zkServer.restart();
    markDownCallback.get();

    properties = store.get(_cluster);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 0);

    shutdownManager(manager);
  }

  @Test
  public void testMarkDownAndUpDuringDisconnection()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);

    ZooKeeperConnectionManager manager = createManager(true, announcer);

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(_cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), WEIGHT);
    assertEquals(properties.Uris().size(), 1);

    _zkServer.shutdown(false);

    FutureCallback<None> markDownCallback = new FutureCallback<>();
    announcer.markDown(markDownCallback);
    FutureCallback<None> markUpCallback = new FutureCallback<>();
    announcer.markUp(markUpCallback);

    // ugly, but we need to wait for a while just so that Disconnect event is propagated
    // to the caller before we restart zk sever.
    Thread.sleep(1000);
    _zkServer.restart();
    markUpCallback.get(10, TimeUnit.SECONDS);
    try
    {
      markDownCallback.get();
      Assert.fail("mark down should have thrown CancellationException.");
    }
    catch (ExecutionException e)
    {
      Assert.assertTrue(e.getCause() instanceof CancellationException);
    }

    properties = store.get(_cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), WEIGHT);
    assertEquals(properties.Uris().size(), 1);

    shutdownManager(manager);
  }

  @Test(invocationCount = 10, timeOut = 10000)
  public void testMarkUpDuringSessionExpiration()
    throws Exception
  {
    // set up
    final double newWeight = 1.5d;

    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);

    ZKPersistentConnection zkPersistentConnection = getZkPersistentConnection();
    ZooKeeperConnectionManager manager = createManager(true, zkPersistentConnection, announcer);

    // the new WEIGHT will be picked up only if the connection is re-established
    announcer.setWeight(newWeight);

    // expiring the connection
    long oldSessionId = zkPersistentConnection.getZooKeeper().getSessionId();
    ZKTestUtil.expireSession("localhost:" + PORT, zkPersistentConnection.getZooKeeper(), 10, TimeUnit.SECONDS);
    // making sure that a new connection has been established.
    ZKTestUtil.waitForNewSessionEstablished(oldSessionId, zkPersistentConnection, 10, TimeUnit.SECONDS);

    // validation
    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();

    AssertionMethods.assertWithTimeout(1000, () -> {
      UriProperties properties = store.get(_cluster);
      assertNotNull(properties);
      if (properties.getPartitionDataMap(URI.create(_uri)) == null)
      {
        Assert.fail("Supposed to have the uri present in ZK");
      }
      assertEquals(properties.getPartitionDataMap(URI.create(_uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), newWeight);
      assertEquals(properties.Uris().size(), 1);
    });

    shutdownManager(manager);
  }

  @Test(invocationCount = 10, timeOut = 10000, retryAnalyzer = ThreeRetries.class)
  public void testMarkUpDuringSessionExpirationManyCallbacks()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);

    ZKPersistentConnection zkPersistentConnection = getZkPersistentConnection();
    ZooKeeperConnectionManager manager = createManager(true, zkPersistentConnection, announcer);

    // set up many concurrent callbacks
    FutureCallback<None> allMarkupsSucceed = new FutureCallback<>();
    int count = 1000;
    Callback<None> markUpAllServersCallback = new MultiCallback(allMarkupsSucceed, 2 * count);

    ExecutorService executorService = Executors.newScheduledThreadPool(100);
    for (int i = 0; i < count; i++)
    {
      executorService.execute(() -> {
        manager.markDownAllServers(new IgnoreCancelledCallback(markUpAllServersCallback));
        manager.markUpAllServers(new IgnoreCancelledCallback(markUpAllServersCallback));
      });
    }

    // expiring the connection
    long oldSessionId = zkPersistentConnection.getZooKeeper().getSessionId();
    ZKTestUtil.expireSession("localhost:" + PORT, zkPersistentConnection.getZooKeeper(), 10, TimeUnit.SECONDS);
    ZKTestUtil.waitForNewSessionEstablished(oldSessionId, zkPersistentConnection, 10, TimeUnit.SECONDS);

    try
    {
      allMarkupsSucceed.get(1, TimeUnit.MILLISECONDS);
      Assert.fail(
        "All the callbacks were resolved before expiring the connection, which means it won't test that callbacks are invoked even after session expiration");
    }
    catch (Throwable e)
    {
      // expected
    }
    allMarkupsSucceed.get();

    // making sure that a new connection has been established. There should be no need to wait, because at least one markup should have been run on
    // the new connection, which means that by this part of code it should already have been established
    ZKTestUtil.waitForNewSessionEstablished(oldSessionId, zkPersistentConnection, 0, TimeUnit.SECONDS);

    // data validation
    dataValidation(_uri, _cluster, WEIGHT);

    shutdownManager(manager);
    executorService.shutdown();
  }

  @Test(invocationCount = 10, timeOut = 10000)
  public void testMarkUpAndDownMultipleTimesFinalDown()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);
    ZooKeeperConnectionManager manager = createManager(true, announcer);

    // set up many concurrent callbacks
    FutureCallback<None> allMarkupsDownsSucceed = new FutureCallback<>();
    int count = 1;
    Callback<None> markUpAllServersCallback = new MultiCallback(allMarkupsDownsSucceed, count * 2);

    ExecutorService executorService = Executors.newScheduledThreadPool(100);
    for (int i = 0; i < count; i++)
    {
      executorService.execute(() -> {
        manager.markUpAllServers(new IgnoreCancelledCallback(markUpAllServersCallback));
        manager.markDownAllServers(new IgnoreCancelledCallback(markUpAllServersCallback));
      });
    }
    allMarkupsDownsSucceed.get();

    // data validation
    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    AssertionMethods.assertWithTimeout(1000, () -> {
      UriProperties properties = store.get(_cluster);
      assertNotNull(properties);
      assertNull(properties.getPartitionDataMap(URI.create(_uri)), _uri);
    });

    shutdownManager(manager);
    executorService.shutdown();
  }

  @Test(invocationCount = 10, timeOut = 10000, groups = { "ci-flaky" })
  public void testMarkUpAndDownMultipleTimesFinalUp()
    throws Exception
  {
    ZooKeeperAnnouncer announcer = getZooKeeperAnnouncer(_cluster, _uri, WEIGHT);
    ZooKeeperConnectionManager manager = createManager(true, announcer);

    FutureCallback<None> managerStartCallback = new FutureCallback<>();
    manager.start(managerStartCallback);
    managerStartCallback.get(10, TimeUnit.SECONDS);

    // set up many concurrent callbacks
    FutureCallback<None> allMarkupsDownsSucceed = new FutureCallback<>();
    int count = 1000;
    Callback<None> markUpAllServersCallback = new MultiCallback(allMarkupsDownsSucceed, count * 2);

    ExecutorService executorService = Executors.newScheduledThreadPool(100);
    for (int i = 0; i < count; i++)
    {
      executorService.execute(() -> {
        manager.markDownAllServers(new IgnoreCancelledCallback(markUpAllServersCallback));
        manager.markUpAllServers(new IgnoreCancelledCallback(markUpAllServersCallback));
      });
    }
    allMarkupsDownsSucceed.get();

    // data validation
    dataValidation(_uri, _cluster, WEIGHT);

    shutdownManager(manager);
    executorService.shutdown();
  }

  // ################################# Tooling section #################################

  private static class IgnoreCancelledCallback implements Callback<None>
  {
    private final Callback<None> _callback;

    IgnoreCancelledCallback(Callback<None> callback)
    {
      _callback = callback;
    }

    @Override
    public void onError(Throwable e)
    {
      if (e instanceof CancellationException || e.getCause() instanceof CancellationException || (e.getCause().getCause() != null && e.getCause()
        .getCause() instanceof CancellationException))
      {
        _callback.onSuccess(None.none());
      }
      else
      {
        _callback.onError(e);
      }
    }

    @Override
    public void onSuccess(None result)
    {
      _callback.onSuccess(result);
    }
  }

  private static void dataValidation(String uri, String cluster, double weight)
    throws Exception
  {
    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();

    AssertionMethods.assertWithTimeout(1000, () -> {
      UriProperties properties = store.get(cluster);
      assertNotNull(properties);
      if (properties.getPartitionDataMap(URI.create(uri)) == null)
      {
        Assert.fail();
      }
      assertEquals(properties.getPartitionDataMap(URI.create(uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), weight);
      assertEquals(properties.Uris().size(), 1);
    });
  }

  private static void shutdownManager(ZooKeeperConnectionManager manager)
    throws InterruptedException, ExecutionException
  {
    FutureCallback<None> noneCallback = new FutureCallback<>();
    manager.shutdown(noneCallback);
    noneCallback.get();
  }

  private static ZKPersistentConnection getZkPersistentConnection()
  {
    return new ZKPersistentConnection(new ZKConnectionBuilder("localhost:" + PORT).setTimeout(5000));
  }

  private static ZooKeeperConnectionManager createManager(boolean startManager, ZKPersistentConnection zkPersistentConnection,
                                                          ZooKeeperAnnouncer... announcers)
    throws ExecutionException, InterruptedException, TimeoutException
  {
    ZooKeeperConnectionManager zooKeeperConnectionManager = new ZooKeeperConnectionManager(zkPersistentConnection, "/d2",
                                                                                           (connection, path) -> new ZooKeeperEphemeralStore<>(
                                                                                             connection, new UriPropertiesJsonSerializer(),
                                                                                             new UriPropertiesMerger(), path), announcers);
    if (startManager)
    {
      FutureCallback<None> managerStartCallback = new FutureCallback<>();
      zooKeeperConnectionManager.start(managerStartCallback);
      managerStartCallback.get(10, TimeUnit.SECONDS);
    }
    return zooKeeperConnectionManager;
  }

  private static ZooKeeperConnectionManager createManager(boolean startManager, ZooKeeperAnnouncer... announcer)
    throws ExecutionException, InterruptedException, TimeoutException
  {
    ZKPersistentConnection zkPersistentConnection = getZkPersistentConnection();

    return createManager(startManager, zkPersistentConnection, announcer);
  }

  private static ZooKeeperEphemeralStore<UriProperties> createAndStartUriStore()
    throws IOException, ExecutionException, InterruptedException
  {
    ZKConnection zkClient = new ZKConnection("localhost:" + PORT, 5000);
    zkClient.start();

    ZooKeeperEphemeralStore<UriProperties> store =
      new ZooKeeperEphemeralStore<>(zkClient, new UriPropertiesJsonSerializer(), new UriPropertiesMerger(), "/d2/uris");
    FutureCallback<None> callback = new FutureCallback<>();
    store.start(callback);
    callback.get();
    return store;
  }

  private static ZooKeeperAnnouncer getZooKeeperAnnouncer(String cluster, String uri, double weight)
  {
    return getZooKeeperSinglePartitionAnnouncer(cluster, uri, DefaultPartitionAccessor.DEFAULT_PARTITION_ID, weight);
  }

  private static ZooKeeperAnnouncer getZooKeeperSinglePartitionAnnouncer(String cluster, String uri, int partitionId, double weight)
  {
    Map<Integer, PartitionData> partitionWeight = new HashMap<>();
    partitionWeight.put(partitionId, new PartitionData(weight));
    return getZookeeperAnnouncer(cluster, uri, partitionWeight);
  }

  private static ZooKeeperAnnouncer getZooKeeperMultiPartitionAnnouncer(String cluster, String uri, int partition1Id, int partition2Id, double partition1Weight, double partition2Weight)
  {
    Map<Integer, PartitionData> partitionWeight = new HashMap<>();
    partitionWeight.put(partition1Id, new PartitionData(partition1Weight));
    partitionWeight.put(partition2Id, new PartitionData(partition2Weight));
    return getZookeeperAnnouncer(cluster, uri, partitionWeight);
  }

  private static ZooKeeperAnnouncer getZookeeperAnnouncer(String cluster, String uri, Map<Integer, PartitionData> partitionWeight)
  {
    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(new ZooKeeperServer());
    announcer.setCluster(cluster);
    announcer.setUri(uri);
    announcer.setPartitionData(partitionWeight);
    return announcer;
  }
}
