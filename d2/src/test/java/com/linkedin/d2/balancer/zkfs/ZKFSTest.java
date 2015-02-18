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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.zkfs;


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKFSTest
{
  private static final String BASE_PATH = "/d2";
  private static final int PORT = 5678;

  private ZKServer _zkServer;
  private File _tmpdir;


  @BeforeClass
  public void enableLog()
  {
    //org.apache.log4j.BasicConfigurator.configure();
    //org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
  }

  @BeforeMethod
  public void createTempdir() throws IOException
  {
    _tmpdir = LoadBalancerUtil.createTempDirectory("d2FileStore");
  }

  @AfterMethod
  public void removeTempdir() throws IOException
  {
    if (_tmpdir != null)
    {
      rmrf(_tmpdir);
      _tmpdir = null;
    }
  }

  private void rmrf(File f) throws IOException
  {
    if (f.isDirectory())
    {
      for (File contained : f.listFiles())
      {
        rmrf(contained);
      }
    }
    if (!f.delete())
    {
      throw new IOException("Failed to delete file: " + f);
    }
  }

  private void startServer() throws IOException, InterruptedException
  {
    _zkServer = new ZKServer(PORT);
    _zkServer.startup();
  }

  private void stopServer() throws IOException
  {
    _zkServer.shutdown();
  }

  private ZKFSLoadBalancer getBalancer()
  {
    ZKFSComponentFactory f = new ZKFSComponentFactory();
    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
            new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();

    loadBalancerStrategyFactories.put("degrader",
                                      new DegraderLoadBalancerStrategyFactoryV3());

    Map<String, TransportClientFactory> clientFactories =
            new HashMap<String, TransportClientFactory>();

    clientFactories.put("http", new HttpClientFactory());

    // We rely on _tmpdir below being fresh for each test case.  Otherwise, leftover files in
    // _tmpdir from a previous test could affect another test.  This is accomplished with the
    // @BeforeMethod and @AfterMethod annotations.
    ZKFSTogglingLoadBalancerFactoryImpl f2 = new ZKFSTogglingLoadBalancerFactoryImpl(
            f,
            5, TimeUnit.SECONDS, BASE_PATH, _tmpdir.getAbsolutePath(),
            clientFactories,
            loadBalancerStrategyFactories);
    ZKFSLoadBalancer balancer = new ZKFSLoadBalancer("localhost:"+PORT, 60000, 5000, f2, null, BASE_PATH);
    return balancer;
  }

  @Test
  public void testNormalStartup()
          throws ExecutionException, TimeoutException, InterruptedException, IOException
  {
    startServer();
    try
    {
      ZKFSLoadBalancer balancer = getBalancer();
      FutureCallback<None> callback = new FutureCallback<None>();

      balancer.start(callback);

      callback.get(5, TimeUnit.SECONDS);
    }
    finally
    {
      stopServer();
    }

  }

  @Test
  public void testServerDownStartup()
          throws ExecutionException, TimeoutException, InterruptedException
  {
    ZKFSLoadBalancer balancer = getBalancer();
    FutureCallback<None> callback = new FutureCallback<None>();
    balancer.start(callback);
    callback.get(15, TimeUnit.SECONDS);

  }

  @Test
  public void testExpiration()
          throws ExecutionException, TimeoutException, InterruptedException, IOException
  {
    startServer();
    try
    {
      ZKFSLoadBalancer balancer = getBalancer();
      FutureCallback<None> callback = new FutureCallback<None>();
      balancer.start(callback);
      callback.get(5, TimeUnit.SECONDS);

      // By using the same sessionID/password to close the session, we will generate an
      // Expired event on the original connection
      ZKTestUtil.expireSession("localhost:" + PORT, balancer.zkConnection().getZooKeeper(), 30, TimeUnit.SECONDS);

      // Should receive an expired event
      Thread.sleep(5000);
    }
    finally
    {
      stopServer();
    }
  }

  @Test
  public void testServiceDirectory() throws Exception
  {
    final String TEST_SERVICE_NAME = "testingService";
    startServer();
    try
    {
      ZKFSLoadBalancer balancer = getBalancer();
      FutureCallback<None> callback = new FutureCallback<None>();
      balancer.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      Directory dir = balancer.getDirectory();

      ZKConnection conn = new ZKConnection("localhost:" + PORT, 30000);
      conn.start();

      ZooKeeperPermanentStore<ServiceProperties> store =
              new ZooKeeperPermanentStore<ServiceProperties>(conn, new ServicePropertiesJsonSerializer(), ZKFSUtil.servicePath(BASE_PATH));
      callback = new FutureCallback<None>();
      store.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      ServiceProperties props = new ServiceProperties(TEST_SERVICE_NAME, "someCluster", "/somePath", Arrays.asList("someStrategy"));
      store.put(TEST_SERVICE_NAME, props);

      FutureCallback<List<String>> serviceCallback = new FutureCallback<List<String>>();
      dir.getServiceNames(serviceCallback);

      Assert.assertEquals(serviceCallback.get(30, TimeUnit.SECONDS), Collections.singletonList(TEST_SERVICE_NAME));
    }
    finally
    {
      stopServer();
    }
  }

  @Test
  public void testClusterDirectory() throws Exception
  {
    final String TEST_CLUSTER_NAME = "testingService";
    startServer();
    try
    {
      ZKFSLoadBalancer balancer = getBalancer();
      FutureCallback<None> callback = new FutureCallback<None>();
      balancer.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      Directory dir = balancer.getDirectory();

      ZKConnection conn = new ZKConnection("localhost:" + PORT, 30000);
      conn.start();

      ZooKeeperPermanentStore<ClusterProperties> store =
              new ZooKeeperPermanentStore<ClusterProperties>(conn, new ClusterPropertiesJsonSerializer(),
                                                             ZKFSUtil.clusterPath(BASE_PATH));
      callback = new FutureCallback<None>();
      store.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      ClusterProperties props = new ClusterProperties(TEST_CLUSTER_NAME);
      store.put(TEST_CLUSTER_NAME, props);

      FutureCallback<List<String>> clusterCallback = new FutureCallback<List<String>>();
      dir.getClusterNames(clusterCallback);

      Assert.assertEquals(clusterCallback.get(30, TimeUnit.SECONDS), Collections.singletonList(TEST_CLUSTER_NAME));
    }
    finally
    {
      stopServer();
    }
  }

  @Test
  public void testKeyMapper() throws Exception
  {
    final String TEST_SERVICE_NAME = "test-service";
    final String TEST_CLUSTER_NAME = "test-cluster";
    final URI TEST_SERVER_URI1 = URI.create("http://test-host-1/");
    final URI TEST_SERVER_URI2 = URI.create("http://test-host-2/");
    final int NUM_ITERATIONS = 5;
    startServer();
    try
    {
      ZKFSLoadBalancer balancer = getBalancer();

      FutureCallback<None> callback = new FutureCallback<None>();
      balancer.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      ZKConnection conn = balancer.zkConnection();

      ZooKeeperPermanentStore<ServiceProperties> serviceStore =
              new ZooKeeperPermanentStore<ServiceProperties>(conn,
                                                             new ServicePropertiesJsonSerializer(),
                                                             ZKFSUtil.servicePath(BASE_PATH));

      ServiceProperties props = new ServiceProperties(TEST_SERVICE_NAME, TEST_CLUSTER_NAME, "/test",
                                                      Arrays.asList("degrader"),
                                                      Collections.<String, Object> emptyMap(),
                                                      null,
                                                      null,
                                                      Arrays.asList("http"),
                                                      null);
      serviceStore.put(TEST_SERVICE_NAME, props);

      ClusterProperties clusterProperties = new ClusterProperties(TEST_CLUSTER_NAME);
      ZooKeeperPermanentStore<ClusterProperties> clusterStore =
              new ZooKeeperPermanentStore<ClusterProperties>(conn, new ClusterPropertiesJsonSerializer(), ZKFSUtil.clusterPath(BASE_PATH));
      clusterStore.put(TEST_CLUSTER_NAME, clusterProperties);

      ZooKeeperEphemeralStore<UriProperties> uriStore =
              new ZooKeeperEphemeralStore<UriProperties>(conn,
                                                         new UriPropertiesJsonSerializer(),
                                                         new UriPropertiesMerger(),
                                                         ZKFSUtil.uriPath(BASE_PATH));
      Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
      Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
      partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1.0d));
      uriData.put(TEST_SERVER_URI1, partitionData);
      uriData.put(TEST_SERVER_URI2, partitionData);

      UriProperties uriProps = new UriProperties(TEST_CLUSTER_NAME, uriData);

      callback = new FutureCallback<None>();
      uriStore.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      uriStore.put(TEST_CLUSTER_NAME, uriProps);


      Set<Integer> keys = new HashSet<Integer>();
      for (int ii=0; ii<100; ++ii)
      {
        keys.add(ii);
      }

      for (int ii=0; ii<NUM_ITERATIONS; ++ii)
      {
        KeyMapper mapper = balancer.getKeyMapper();
        MapKeyResult<URI, Integer> batches = mapper.mapKeysV2(URI.create("d2://" + TEST_SERVICE_NAME), keys);
        Assert.assertEquals(batches.getMapResult().size(), 2);
        for (Map.Entry<URI, Collection<Integer>> oneBatch : batches.getMapResult().entrySet())
        {
          Assert.assertTrue(oneBatch.getKey().toString().startsWith("http://test-host-"));
          Assert.assertTrue(keys.containsAll(oneBatch.getValue()));
        }
      }
    }
    finally
    {
      stopServer();
    }
  }

  @Test
  public void testClientFactoryProvider() throws Exception
  {
    startServer();
    try
    {
      ZKFSLoadBalancer balancer = getBalancer();
      FutureCallback<None> callback = new FutureCallback<None>();
      balancer.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      Facilities facilities = balancer.getFacilities();
      TransportClientFactory factory = facilities.getClientFactory("http");
      Assert.assertNotNull(factory);
      Assert.assertTrue(factory instanceof HttpClientFactory);
    }
    finally
    {
      stopServer();
    }
  }

  @Test
  public void testZKDown() throws Exception
  {
    final String TEST_SERVICE_NAME = "testingService";
    final String TEST_CLUSTER_NAME = "someCluster";
    startServer();
    try
    {
      ZKFSLoadBalancer balancer = getBalancer();
      FutureCallback<None> callback = new FutureCallback<None>();
      balancer.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      ZKConnection conn = new ZKConnection("localhost:" + PORT, 30000);
      conn.start();

      ZooKeeperPermanentStore<ServiceProperties> store =
              new ZooKeeperPermanentStore<ServiceProperties>(conn, new ServicePropertiesJsonSerializer(), ZKFSUtil.servicePath(BASE_PATH));
      callback = new FutureCallback<None>();
      store.start(callback);
      callback.get(30, TimeUnit.SECONDS);


      ServiceProperties props = new ServiceProperties(TEST_SERVICE_NAME, TEST_CLUSTER_NAME, "/somePath",
                                                      Arrays.asList("degrader"),
                                                      Collections.<String, Object>emptyMap(),
                                                      null,
                                                      null,
                                                      Arrays.asList("http"),
                                                      null);
      store.put(TEST_SERVICE_NAME, props);

      ZooKeeperPermanentStore<ClusterProperties> clusterStore =
              new ZooKeeperPermanentStore<ClusterProperties>(conn, new ClusterPropertiesJsonSerializer(), ZKFSUtil.clusterPath(BASE_PATH));
      callback = new FutureCallback<None>();
      clusterStore.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      ClusterProperties clusterProps = new ClusterProperties("someCluster");
      clusterStore.put(TEST_CLUSTER_NAME, clusterProps);

      ZKConnection serverConn = new ZKConnection("localhost:" + PORT, 30000);
      serverConn.start();
      ZooKeeperEphemeralStore<UriProperties> uriStore = new ZooKeeperEphemeralStore<UriProperties>(serverConn, new UriPropertiesJsonSerializer(), new UriPropertiesMerger(), ZKFSUtil.uriPath(BASE_PATH));
      callback = new FutureCallback<None>();
      uriStore.start(callback);
      callback.get(30, TimeUnit.SECONDS);

      ZooKeeperServer server = new ZooKeeperServer(uriStore);
      callback = new FutureCallback<None>();
      Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>();
      partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1.0));
      server.markUp(TEST_CLUSTER_NAME, URI.create("http://test.uri"), partitionDataMap, callback);
      callback.get(30, TimeUnit.SECONDS);

      URIRequest request = new URIRequest("d2://" + TEST_SERVICE_NAME + "/foo");
      TransportClient client = balancer.getClient(request, new RequestContext());

      // Stop the server to cause a disconnect event
      stopServer();
      // Sleep to ensure the disconnect has propagated; ideally the Toggle should expose
      // some interface to allow detection that the toggle occurred
      Thread.sleep(1000);

      // Now see if it still works
      client = balancer.getClient(request, new RequestContext());


    }
    finally
    {
      stopServer();
    }
  }

}
