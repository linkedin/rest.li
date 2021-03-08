/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.D2Client;
import com.linkedin.d2.balancer.D2ClientBuilder;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.LastSeenBalancerWithFacilitiesFactory;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.servers.ZooKeeperAnnouncer;
import com.linkedin.d2.balancer.servers.ZooKeeperConnectionManager;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;
import com.linkedin.d2.balancer.servers.ZooKeeperUriStoreFactory;
import com.linkedin.d2.balancer.util.HostSet;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.test.util.retry.ThreeRetries;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;




public class SharedZkConnectionProviderTest {
  private SharedZkConnectionProvider _provider;
  private ZKServer _zkServer;

  private int NUM_DEFAULT_SHAREABLE_BUILDERS = 5;

  private static final String CLUSTER_NAME = "testCluster";
  private static final String SERVICE_NAME = "testService";
  private static final String ZKBASE_PATH = "/d2";
  private static final int ZK_PORT = 2120;
  private static final int ZK_TIMEOUT = 5000;
  private static final int BLOCKING_CALL_TIMEOUT = 5000;

  private ZooKeeperPermanentStore<ServiceProperties> _serviceRegistry;
  private ZooKeeperPermanentStore<ClusterProperties> _clusterRegistry;
  private ZooKeeperEphemeralStore<UriProperties> _verificationStore;
  private ExecutorService _threadPoolExecutor;

  @BeforeMethod
  public void setUp() throws Exception {
    _provider = new SharedZkConnectionProvider();
    try {
      _zkServer = new ZKServer(ZK_PORT);
      _zkServer.startup();
    } catch (IOException e) {
      fail("unable to instantiate real zk server on port " + ZK_PORT);
    }

    ZKConnection serviceZkConn = new ZKConnectionBuilder("localhost:" + ZK_PORT).setTimeout(5000).setWaitForConnected(true).build();
    ZKConnection clusterZkConn = new ZKConnectionBuilder("localhost:" + ZK_PORT).setTimeout(5000).setWaitForConnected(true).build();

    _serviceRegistry =
        new ZooKeeperPermanentStore<ServiceProperties>(serviceZkConn, new ServicePropertiesJsonSerializer(),
            ZKFSUtil.servicePath(ZKBASE_PATH));
    _clusterRegistry =
        new ZooKeeperPermanentStore<ClusterProperties>(clusterZkConn, new ClusterPropertiesJsonSerializer(),
            ZKFSUtil.clusterPath(ZKBASE_PATH));

    FutureCallback<None> storesStartupCallBack = new FutureCallback<None>();
    Callback<None> multiStartupCallback = Callbacks.countDown(storesStartupCallBack, 2);
    serviceZkConn.start();
    clusterZkConn.start();
    _serviceRegistry.start(multiStartupCallback);
    _clusterRegistry.start(multiStartupCallback);
    storesStartupCallBack.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    FutureCallback<None> propertiesSetupCallback = new FutureCallback<None>();
    Callback<None> multiPropertiesCallback = Callbacks.countDown(propertiesSetupCallback, 2);

    ServiceProperties serviceProps =
        new ServiceProperties(SERVICE_NAME, CLUSTER_NAME, "/testService", Arrays.asList("degrader"),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Arrays.asList("http"),
            Collections.emptySet());
    _serviceRegistry.put(SERVICE_NAME, serviceProps, multiPropertiesCallback);
    ClusterProperties clusterProps = new ClusterProperties(CLUSTER_NAME);
    _clusterRegistry.put(CLUSTER_NAME, clusterProps, multiPropertiesCallback);
    propertiesSetupCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    _verificationStore = createAndStartVerificationStore();
    _threadPoolExecutor = Executors.newFixedThreadPool(10);
  }

  @AfterMethod
  public void tearDown() throws IOException, InterruptedException, ExecutionException, TimeoutException{
    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    Callback<None> multiCallback = Callbacks.countDown(shutdownCallback,3);
    _serviceRegistry.shutdown(multiCallback);
    _clusterRegistry.shutdown(multiCallback);
    _verificationStore.shutdown(multiCallback);
    _threadPoolExecutor.shutdown();
    shutdownCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);
    _zkServer.shutdown();
  }

  /**
   * create and start a uri store to verify announcement
   */
  private ZooKeeperEphemeralStore<UriProperties> createAndStartVerificationStore()
      throws IOException, ExecutionException, InterruptedException, TimeoutException{
    ZKConnection zkClient = new ZKConnection("localhost:" + ZK_PORT, 5000);
    zkClient.start();

    ZooKeeperEphemeralStore<UriProperties> store =
        new ZooKeeperEphemeralStore<UriProperties>(zkClient, new UriPropertiesJsonSerializer(),
            new UriPropertiesMerger(), ZKFSUtil.uriPath(ZKBASE_PATH));
    FutureCallback<None> callback = new FutureCallback<None>();
    store.start(callback);
    callback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);
    return store;
  }

  /**
   * Generate some fake host names for testing.
   */
  private List<URI> prepareHostNames(int count, String name) throws Exception {
    List<URI> hostNames = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      hostNames.add(new URI("http://" + name + "_" + i + ".test.com"));
    }
    return hostNames;
  }

  /**
   * For each given uri, generate a zookeeperConnectionManager for announcement
   */

  private List<ZooKeeperConnectionManager> prepareConnectionManagers(List<URI> hostNames) throws Exception {
    List<ZooKeeperConnectionManager> connectionManagers = new ArrayList<>();
    for (URI uri : hostNames) {
      ZooKeeperServer server = new ZooKeeperServer();
      ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(server, true);
      announcer.setCluster(CLUSTER_NAME);
      announcer.setUri(uri.toString());
      Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
      partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(0.5d));
      announcer.setPartitionData(partitionWeight);

      ZooKeeperConnectionManager.ZKStoreFactory<UriProperties,ZooKeeperEphemeralStore<UriProperties>> factory = new ZooKeeperUriStoreFactory();
      ZKConnectionBuilder connectionBuilder = new ZKConnectionBuilder("localhost:" + ZK_PORT);
      connectionBuilder.setTimeout(ZK_TIMEOUT);
      ZKPersistentConnection connection = _provider.getZKPersistentConnection(connectionBuilder);
      ZooKeeperConnectionManager connectionManager =
          new ZooKeeperConnectionManager(connection, ZKBASE_PATH, factory, announcer);
      connectionManagers.add(connectionManager);
    }
    return connectionManagers;
  }

  private void shutdownConnectionManagers(List<ZooKeeperConnectionManager> managers) throws Exception {
    FutureCallback<None> shutdownCallback = new FutureCallback<None>();
    Callback<None> shutdownMulitCallback = Callbacks.countDown(shutdownCallback, managers.size());
    for (ZooKeeperConnectionManager manager : managers) {
      _threadPoolExecutor.submit(() -> manager.shutdown(shutdownMulitCallback));
    }
    shutdownCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);
  }

  private void startConnectionManagers(List<ZooKeeperConnectionManager> managers) throws Exception {
    FutureCallback<None> markupCallback = new FutureCallback<>();
    Callback<None> markupMultiCallback = Callbacks.countDown(markupCallback, managers.size());
    for (ZooKeeperConnectionManager manager : managers) {
      _threadPoolExecutor.submit(() -> {
        try {
          //Using random sleep to introduce delay to simulate uncertainty during real environment.
          Thread.sleep(Math.abs(new Random().nextInt()) % 100);
          manager.start(markupMultiCallback);
        } catch (Exception e) {
          markupMultiCallback.onError(new RuntimeException("Announcing failed for host: " + manager.getAnnouncers()[0].getUri() + " due to: " + e.getMessage(), e));
        }
      });
    }
    markupCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);
  }

  private List<ZKConnectionBuilder> identicalBuildersSetUp() {
    List<ZKConnectionBuilder> builders = new ArrayList<>();
    for (int i = 0; i < NUM_DEFAULT_SHAREABLE_BUILDERS; i++) {
      ZKConnectionBuilder builder = new ZKConnectionBuilder("localhost:2120");
      builder.setInitInterval(20);
      builder.setRetryLimit(10);
      builder.setTimeout(100);
      builder.setExponentialBackoff(true);
      builder.setIsSymlinkAware(true);
      builder.setShutdownAsynchronously(true);
      builders.add(builder);
    }
    return builders;
  }

  private Callback<RestResponse> decorateNoneCallback(Callback<None> callback) {
    return Callbacks.handle(result -> callback.onSuccess(None.none()), callback);
  }

  /**
   * Obtain the d2client with the same setup.
   */
  private D2Client getD2Client(Map<String, TransportClientFactory> transportClientFactoryMap) {
    ZKConnectionBuilder connectionBuilder = new ZKConnectionBuilder("localhost:" + ZK_PORT);
    connectionBuilder.setTimeout(ZK_TIMEOUT);
    ZKPersistentConnection zkConnectionToUse = _provider.getZKPersistentConnection(connectionBuilder);
    D2ClientBuilder d2ClientBuilder = new D2ClientBuilder();
    d2ClientBuilder.setZkHosts("localhost:" + ZK_PORT)
        .setZkSessionTimeout(ZK_TIMEOUT, TimeUnit.MILLISECONDS)
        .setZKConnectionForloadBalancer(zkConnectionToUse)
        .setLoadBalancerWithFacilitiesFactory(new LastSeenBalancerWithFacilitiesFactory())
        .setClientFactories(transportClientFactoryMap);
    return d2ClientBuilder.build();
  }

  private void fireTestRequests(D2Client client, int numRequest, FutureCallback<None> finishCallback) throws Exception {
    Callback<None> reqMultiCallback = Callbacks.countDown(finishCallback, numRequest);
    for (int i = 0; i < numRequest; i++) {
      RestRequestBuilder builder = new RestRequestBuilder(new URI("d2://testService"));
      client.restRequest(builder.build(), decorateNoneCallback(reqMultiCallback));
    }
  }
  /**
   * Tests begin
   */

  @Test
  public void TestZkConnectionProviderBasic() {
    List<ZKConnectionBuilder> builders = identicalBuildersSetUp();
    List<ZKPersistentConnection> connections = new ArrayList<>();
    for (int i = 0; i < NUM_DEFAULT_SHAREABLE_BUILDERS; i++) {
      connections.add(_provider.getZKPersistentConnection(builders.get(i)));
    }
    ZKPersistentConnection firstConn = connections.get(0);
    for (ZKPersistentConnection conn : connections) {
      Assert.assertSame(conn, firstConn);
    }

    ZKConnectionBuilder differentBuilder = new ZKConnectionBuilder("localhost:2122");
    ZKPersistentConnection differentConnection = _provider.getZKPersistentConnection(differentBuilder);
    Assert.assertNotSame(differentConnection, firstConn);

    assertEquals(_provider.getZkConnectionCount(), 2);
  }

  /**
   * Test both markUp and markDown when using only one connection.
   */
  @Test(groups = "needZk")
  public void testMarkUpAndMarkDownSharingConnection() throws Exception {
    List<URI> hostNames = prepareHostNames(5, "testMarkUpAndMarkDownSharingConnection");
    List<ZooKeeperConnectionManager> connectionManagers = prepareConnectionManagers(hostNames);

    //announce all five hosts
    startConnectionManagers(connectionManagers);

    UriProperties properties = _verificationStore.get(CLUSTER_NAME);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 5);


    FutureCallback<None> markdownCallback = new FutureCallback<>();
    Callback<None> markdownMultiCallback = Callbacks.countDown(markdownCallback, 2);
    //markdown three hosts
    for (ZooKeeperConnectionManager manager : connectionManagers.subList(0, 2)) {
      _threadPoolExecutor.submit(() -> {
        try {
          //Using random sleep to introduce delay to simulate uncertainty during real environment.
          Thread.sleep(Math.abs(new Random().nextInt()) % 100);
          manager.getAnnouncers()[0].markDown(markdownMultiCallback);
        } catch (Exception e) {
          markdownMultiCallback.onError(new RuntimeException("MarkDown failed for host: " + manager.getAnnouncers()[0].getUri() + "due to: " + e.getMessage(), e));
        }
      });
    }

    markdownCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    UriProperties newProperties = _verificationStore.get(CLUSTER_NAME);
    assertNotNull(newProperties);
    assertEquals(newProperties.Uris().size(), 3);

    shutdownConnectionManagers(connectionManagers);
  }

  /**
   * Test announcing many hosts using one connection concurrently
   */
  @Test(groups = "needZk", retryAnalyzer = ThreeRetries.class)
  public void testManyHostsAnnouncementSharingConnections() throws Exception {
    List<URI> hostNames = prepareHostNames(100, "testManyHostsAnnouncementSharingConnections");
    List<ZooKeeperConnectionManager> connectionManagers = prepareConnectionManagers(hostNames);

    startConnectionManagers(connectionManagers);

    UriProperties newProperties = _verificationStore.get(CLUSTER_NAME);
    assertNotNull(newProperties);
    assertEquals(newProperties.Uris().size(), 100);

    shutdownConnectionManagers(connectionManagers);
  }

  /**
   * Testing sharing connections between announcers and d2client
   * @throws Exception
   */
  @Test(groups = "needZk", retryAnalyzer = ThreeRetries.class)
  public void testAnnouncerAndClientSharing() throws Exception {
    //connection shared to announcers
    List<URI> hostNames = prepareHostNames(20, "testAnnouncerAndClientSharing");
    List<ZooKeeperConnectionManager> connectionManagers = prepareConnectionManagers(hostNames);
    int l = 1;
    //set up a mock transport client
    Map<String, TransportClientFactory> transportClientMap = new HashMap<>();
    TestTransportClientFactory testClientFactory = new TestTransportClientFactory();
    transportClientMap.put("http", testClientFactory);

    //connection shared to d2client
    D2Client client = getD2Client(transportClientMap);

    //there should only be one connection
    assertEquals(_provider.getZkConnectionCount(), 1);


    //start both announcers and client
    FutureCallback<None> startUpCallback = new FutureCallback<None>();
    Callback<None> startUpMultiCallback = Callbacks.countDown(startUpCallback, connectionManagers.size() + 1);

    _threadPoolExecutor.submit(() -> client.start(startUpMultiCallback));
    for (ZooKeeperConnectionManager manager : connectionManagers) {
      _threadPoolExecutor.submit(() -> manager.start(startUpMultiCallback));
    }
    startUpCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    //verify zookeeper is updated
    UriProperties properties = _verificationStore.get(CLUSTER_NAME);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 20);

    //fire some requests to make sure announcement is successful and hosts properties can be retrieved successfully.
    int requestRepeat = 1000;
    FutureCallback<None> reqCallback = new FutureCallback<None>();
    fireTestRequests(client, requestRepeat, reqCallback);
    reqCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    //verify d2client received the changes
    HostSet hosts = client.getFacilities().getKeyMapper().getAllPartitionsMultipleHosts(new URI("d2://testService"), 20);
    Assert.assertEquals(hosts.getAllHosts().size(), 20);
    Assert.assertEquals(testClientFactory.requestCount.get(), 1000);


    //Markdown half of the hosts and test the results
    FutureCallback<None> hostsMarkdownCallback = new FutureCallback<None>();
    Callback<None> hostsMarkdownMultiCallback = Callbacks.countDown(hostsMarkdownCallback,10);
    for (ZooKeeperConnectionManager manager : connectionManagers.subList(0,10)) {
      _threadPoolExecutor.submit(() -> manager.getAnnouncers()[0].markDown(hostsMarkdownMultiCallback));
    }
    hostsMarkdownCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    //verify zookeeper is updated
    properties = _verificationStore.get(CLUSTER_NAME);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 10);

    //fire some requests to make sure announcement is successful and hosts properties can be retrieved successfully.
    FutureCallback<None> secondReqCallback = new FutureCallback<None>();
    fireTestRequests(client, requestRepeat, secondReqCallback);
    secondReqCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    //verify d2client can read the zookeeper updates.
    hosts = client.getFacilities().getKeyMapper().getAllPartitionsMultipleHosts(new URI("d2://testService"), 20);
    Assert.assertEquals(hosts.getAllHosts().size(), 10);
    Assert.assertEquals(testClientFactory.requestCount.get(), 2000);

    //Mix announcements with request firing to test connection robustness.
    FutureCallback<None> thirdReqCallback = new FutureCallback<None>();
    Callback<None> thirdReqMultiCallback = Callbacks.countDown(thirdReqCallback, requestRepeat + 10);
    for (int i = 0; i < requestRepeat; i++) {
      _threadPoolExecutor.submit(() -> {
        try{
          RestRequestBuilder builder = new RestRequestBuilder(new URI("d2://testService"));
          client.restRequest(builder.build(), decorateNoneCallback(thirdReqMultiCallback));
        }catch (Exception e){
          throw new RuntimeException(e);
        }
      });
      if (i % 100 == 0) {
        //markup one host every 100 requests
        ZooKeeperConnectionManager manager = connectionManagers.get(i / 100);
        _threadPoolExecutor.submit(() -> {
          try{
            manager.getAnnouncers()[0].markUp(thirdReqMultiCallback);
          }catch (Exception e){
            throw new RuntimeException(e);
          }
        });
      }
    }

    thirdReqCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertEquals(testClientFactory.requestCount.get(), 3000);


    //announcers can be shutdown after announcing, without affecting client. This should not happen though.
    FutureCallback<None> announcerShutdownCallback = new FutureCallback<None>();
    Callback<None> announcersShutdownCallback = Callbacks.countDown(announcerShutdownCallback, connectionManagers.size());
    for (ZooKeeperConnectionManager manager : connectionManagers) {
      manager.shutdown(announcersShutdownCallback);
    }
    announcerShutdownCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);


    //fire some requests to make sure d2client is still usable.
    FutureCallback<None> fourthReqCallback = new FutureCallback<None>();
    fireTestRequests(client, requestRepeat, fourthReqCallback);
    thirdReqCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    hosts = client.getFacilities().getKeyMapper().getAllPartitionsMultipleHosts(new URI("d2://testService"), 20);
    Assert.assertEquals(hosts.getAllHosts().size(), 20);
    Assert.assertEquals(testClientFactory.requestCount.get(), 4000);


    //test done!
    FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
    client.shutdown(clientShutdownCallback);
    clientShutdownCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    //make sure the connection is properly stopped.
    ZKPersistentConnection connection = _provider.getZKPersistentConnection(new ZKConnectionBuilder("localhost:" + ZK_PORT).setTimeout(ZK_TIMEOUT));
    Assert.assertNotNull(connection);
    Assert.assertTrue(connection.isConnectionStopped());
  }

  /**
   * Test that when there is an zookeeper property update, d2client can receive the update correctly
   */
  @Test(groups = "needZK")
  public void testZKPropertyUpdate() throws Exception {
    List<URI> hosts = prepareHostNames(5, "testZKPropertyUpdate");
    List<ZooKeeperConnectionManager> connectionManagers = prepareConnectionManagers(hosts);

    Map<String, TransportClientFactory> transportClientMap = new HashMap<String, TransportClientFactory>();
    transportClientMap.put("http", new TestTransportClientFactory());

    // connection shared to d2client
    D2Client client = getD2Client(transportClientMap);

    FutureCallback<None> startupCallback = new FutureCallback<None>();
    client.start(startupCallback);
    startupCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);

    startConnectionManagers(connectionManagers);

    Directory d2Directory = client.getFacilities().getDirectory();

    List<String> serviceList = new ArrayList<>();
    ServiceProperties serviceProps =
        new ServiceProperties("newTestService", CLUSTER_NAME, "/newTestService", Arrays.asList("degrader"),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Arrays.asList("http"),
            Collections.emptySet());

    FutureCallback<None> propertyCallback = new FutureCallback<None>();
    _serviceRegistry.put("newTestService", serviceProps, propertyCallback);
    propertyCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);


    FutureCallback<None> finishCallback = new FutureCallback<None>();
    d2Directory.getServiceNames(new Callback<List<String>>() {
      @Override
      public void onError(Throwable e) {
        finishCallback.onError(e);
      }

      @Override
      public void onSuccess(List<String> result) {
        serviceList.addAll(result);
        finishCallback.onSuccess(None.none());
      }
    });

    finishCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertEquals(serviceList.size(), 2);
    Assert.assertTrue(serviceList.contains("newTestService"));
    Assert.assertTrue(serviceList.contains("testService"));

    shutdownConnectionManagers(connectionManagers);
    FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
    client.shutdown(clientShutdownCallback);
    clientShutdownCallback.get(BLOCKING_CALL_TIMEOUT, TimeUnit.MILLISECONDS);
  }

  /**
   * Hosts should only be announced when ZookeeperConnectionManager is started.
   */
  @Test(groups = "needZk")
  public void testAnnouncerNoStartup() throws Exception {
    List<URI> hosts = prepareHostNames(5, "testAnnouncerNoStartup");
    List<ZooKeeperConnectionManager> connectionManagers = prepareConnectionManagers(hosts);
    List<ZooKeeperConnectionManager> managersToStart = connectionManagers.subList(0,3);
    assertEquals(_provider.getZkConnectionCount(), 1);

    startConnectionManagers(connectionManagers.subList(0,3));

    //verify that only three managers are started.
    UriProperties properties = _verificationStore.get(CLUSTER_NAME);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 3);

    shutdownConnectionManagers(connectionManagers);
  }

  public static class TestTransportClientFactory implements TransportClientFactory {

    public Map<String, ?> _properties;
    public int getClientCount;
    public AtomicInteger requestCount = new AtomicInteger(0);

    @Override
    public TransportClient getClient(Map<String, ?> properties) {
      getClientCount++;
      _properties = properties;
      return new TransportClient() {
        @Override
        public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
            TransportCallback<RestResponse> callback) {
          requestCount.getAndIncrement();
          callback.onResponse(new TransportResponse<RestResponse>() {
            @Override
            public RestResponse getResponse() {
              return null;
            }

            @Override
            public boolean hasError() {
              return false;
            }

            @Override
            public Throwable getError() {
              return null;
            }

            @Override
            public Map<String, String> getWireAttributes() {
              return null;
            }
          });
        }

        @Override
        public void shutdown(Callback<None> callback) {
          callback.onSuccess(None.none());
        }
      };
    }

    @Override
    public void shutdown(Callback<None> callback) {
      callback.onSuccess(None.none());
    }
  }
}


