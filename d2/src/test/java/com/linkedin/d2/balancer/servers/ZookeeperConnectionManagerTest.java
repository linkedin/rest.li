package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;


/**
 * @author Ang Xu
 */
public class ZookeeperConnectionManagerTest
{
  public static final int PORT = 11811;

  protected ZKServer _zkServer;

  @BeforeMethod
  public void setUp() throws InterruptedException
  {
    try
    {
      _zkServer = new ZKServer(PORT);
      _zkServer.startup();
    }
    catch (IOException e)
    {
      fail("unable to instantiate real zk server on port " + PORT);
    }
  }

  @AfterMethod
  public void tearDown() throws IOException
  {
    _zkServer.shutdown();
  }

  @Test
  public void testMarkUp()
      throws IOException, ExecutionException, InterruptedException, PropertyStoreException
  {
    final String uri = "http://cluster-1/test";
    final String cluster = "cluster-1";
    final double weight = 0.5d;

    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(new ZooKeeperServer());
    announcer.setCluster(cluster);
    announcer.setUri(uri);
    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    announcer.setPartitionData(partitionWeight);

    ZooKeeperConnectionManager manager = createManager(announcer);

    FutureCallback<None> managerStartCallback = new FutureCallback<None>();
    manager.start(managerStartCallback);
    managerStartCallback.get();

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(
        properties.getPartitionDataMap(URI.create(uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(),
        weight);
    assertEquals(properties.Uris().size(), 1);
  }

  @Test
  public void testMarkUpAndMarkDown()
      throws IOException, ExecutionException, InterruptedException, PropertyStoreException
  {
    final String uri = "http://cluster-2/test";
    final String cluster = "cluster-2";
    final double weight = 0.5d;

    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(new ZooKeeperServer());
    announcer.setCluster(cluster);
    announcer.setUri(uri);
    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    announcer.setPartitionData(partitionWeight);

    ZooKeeperConnectionManager manager = createManager(announcer);
    FutureCallback<None> managerStartCallback = new FutureCallback<None>();
    manager.start(managerStartCallback);
    managerStartCallback.get();

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(
        properties.getPartitionDataMap(URI.create(uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(),
        weight);
    assertEquals(properties.Uris().size(), 1);

    FutureCallback<None> markDownCallback = new FutureCallback<None>();
    announcer.markDown(markDownCallback);
    markDownCallback.get();

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 0);
  }

  @Test
  public void testMarkUpDuringDisconnection()
      throws ExecutionException, InterruptedException, IOException, PropertyStoreException
  {
    final String uri = "http://cluster-3/test";
    final String cluster = "cluster-3";
    final double weight = 0.5d;

    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(new ZooKeeperServer());
    announcer.setCluster(cluster);
    announcer.setUri(uri);
    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    announcer.setPartitionData(partitionWeight);

    ZooKeeperConnectionManager manager = createManager(announcer);

    _zkServer.shutdown(false);

    FutureCallback<None> managerStartCallback = new FutureCallback<None>();
    manager.start(managerStartCallback);

    _zkServer.restart();
    managerStartCallback.get();

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(
        properties.getPartitionDataMap(URI.create(uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(),
        weight);
    assertEquals(properties.Uris().size(), 1);
  }

  @Test
  public void testMarkDownDuringDisconnection()
      throws IOException, ExecutionException, InterruptedException, PropertyStoreException
  {
    final String uri = "http://cluster-4/test";
    final String cluster = "cluster-4";
    final double weight = 0.5d;

    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(new ZooKeeperServer());
    announcer.setCluster(cluster);
    announcer.setUri(uri);
    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    announcer.setPartitionData(partitionWeight);

    ZooKeeperConnectionManager manager = createManager(announcer);
    FutureCallback<None> managerStartCallback = new FutureCallback<None>();
    manager.start(managerStartCallback);
    managerStartCallback.get();

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(
        properties.getPartitionDataMap(URI.create(uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(),
        weight);
    assertEquals(properties.Uris().size(), 1);

    _zkServer.shutdown(false);

    FutureCallback<None> markDownCallback = new FutureCallback<None>();
    announcer.markDown(markDownCallback);

    // ugly, but we need to wait for a while just so that Disconnect event is propagated
    // to the caller before we restart zk sever.
    Thread.sleep(1000);
    _zkServer.restart();
    markDownCallback.get();

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 0);
  }

  @Test
  public void testMarkDownAndUpDuringDisconnection()
      throws IOException, ExecutionException, InterruptedException, PropertyStoreException, TimeoutException
  {
    final String uri = "http://cluster-5/test";
    final String cluster = "cluster-5";
    final double weight = 0.5d;

    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(new ZooKeeperServer());
    announcer.setCluster(cluster);
    announcer.setUri(uri);
    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    announcer.setPartitionData(partitionWeight);

    ZooKeeperConnectionManager manager = createManager(announcer);
    FutureCallback<None> managerStartCallback = new FutureCallback<None>();
    manager.start(managerStartCallback);
    managerStartCallback.get();

    ZooKeeperEphemeralStore<UriProperties> store = createAndStartUriStore();
    UriProperties properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(
        properties.getPartitionDataMap(URI.create(uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(),
        weight);
    assertEquals(properties.Uris().size(), 1);

    _zkServer.shutdown(false);

    FutureCallback<None> markDownCallback = new FutureCallback<None>();
    announcer.markDown(markDownCallback);
    FutureCallback<None> markUpCallback = new FutureCallback<None>();
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

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(
        properties.getPartitionDataMap(URI.create(uri)).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(),
        weight);
    assertEquals(properties.Uris().size(), 1);
  }

  private ZooKeeperConnectionManager createManager(ZooKeeperAnnouncer... announcers)
  {
    return new ZooKeeperConnectionManager("localhost:" + PORT, 5000, "/d2",
        new ZooKeeperConnectionManager.ZKStoreFactory<UriProperties, ZooKeeperEphemeralStore<UriProperties>>()
        {
          @Override
          public ZooKeeperEphemeralStore<UriProperties> createStore(ZKConnection connection, String path)
          {
            return new ZooKeeperEphemeralStore<UriProperties>(connection,
                new UriPropertiesJsonSerializer(),
                new UriPropertiesMerger(),
                path);
          }
        }, announcers);
  }

  private ZooKeeperEphemeralStore<UriProperties> createAndStartUriStore()
      throws IOException, ExecutionException, InterruptedException
  {
    ZKConnection zkClient = new ZKConnection("localhost:" + PORT, 5000);
    zkClient.start();

    ZooKeeperEphemeralStore<UriProperties> store =
        new ZooKeeperEphemeralStore<UriProperties>(zkClient,
            new UriPropertiesJsonSerializer(),
            new UriPropertiesMerger(),
            "/d2/uris");
    FutureCallback<None> callback = new FutureCallback<None>();
    store.start(callback);
    callback.get();
    return store;
  }
}
