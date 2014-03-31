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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

public class ZooKeeperServerTest
{
  public static final int PORT = 11711;

  protected ZKServer      _zkServer;

  @Test(groups = { "small", "back-end" })
  public void testZkServer() throws InterruptedException,
      URISyntaxException,
      IOException,
      PropertyStoreException,
      ExecutionException
  {
    URI uri1 = URI.create("http://cluster-1/test");
    URI uri2 = URI.create("http://cluster-1-again/test");
    ZKConnection zkClient = new ZKConnection("localhost:" + PORT, 5000);
    zkClient.start();

    ZooKeeperEphemeralStore<UriProperties> store =
        new ZooKeeperEphemeralStore<UriProperties>(zkClient,
                                                   new UriPropertiesJsonSerializer(),
                                                   new UriPropertiesMerger(),
                                                   "/echo/lb/uris");
    FutureCallback<None> callback = new FutureCallback<None>();
    store.start(callback);
    callback.get();

    ZooKeeperServer server = new ZooKeeperServer(store);

    final String cluster = "cluster-1";

    assertNull(store.get(cluster));
    assertNull(store.get("cluster-2"));

    // bring up uri1
    markUp(server, cluster, uri1, 0.5d);

    UriProperties properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(uri1).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 0.5d);
    assertEquals(properties.Uris().size(), 1);

    // test mark up when already up call
    markUp(server, cluster, uri1, 2d);

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(uri1).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 2d);
    assertEquals(properties.Uris().size(), 1);

    // bring up uri 2
    markUp(server, cluster, uri2, 1.5d);

    properties = store.get(cluster);
    assertEquals(properties.getPartitionDataMap(uri1).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 2d);
    assertEquals(properties.getPartitionDataMap(uri2).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 1.5d);
    assertEquals(properties.Uris().size(), 2);

    // bring down uri 1
    markDown(server, cluster, uri1);

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(uri2).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 1.5d);
    assertEquals(properties.Uris().size(), 1);

    // test bring down when already down
    markDown(server, cluster, uri1);

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(uri2).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 1.5d);
    assertEquals(properties.Uris().size(), 1);

    // bring down uri 2
    markDown(server, cluster, uri2);

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 0);

    // test bad cluster doesn't exist
    markDown(server, "BAD CLUSTER", uri1);

    properties = store.get("BAD CLUSTER");
    assertNull(properties);

    // bring up uri1

    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(5, new PartitionData(0.3d));
    partitionWeight.put(15, new PartitionData(0.7d));
    markUp(server, cluster, uri1, partitionWeight, null);
    properties = store.get(cluster);
    assertNotNull(properties);

    assertEquals(properties.getPartitionDataMap(uri1), partitionWeight);

    Map<String, Object> uri2SpecificProperties = new HashMap<String, Object>();
    uri2SpecificProperties.put("foo", "fooValue");
    uri2SpecificProperties.put("bar", 1);

    partitionWeight.put(10, new PartitionData(1d));

    // bring up uri2 with uri specific properties

    markUp(server, cluster, uri2, partitionWeight, uri2SpecificProperties);

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 2);
    assertEquals(properties.getPartitionDataMap(uri2), partitionWeight);

    assertNotNull(properties.getUriSpecificProperties());
    assertEquals(properties.getUriSpecificProperties().size(), 1);
    assertEquals(properties.getUriSpecificProperties().get(uri2), uri2SpecificProperties);

    // bring down uri1 and bring it back up again with properties

    markDown(server, cluster, uri1);

    Map<String, Object> uri1SpecificProperties = new HashMap<String, Object>();
    uri1SpecificProperties.put("baz", "bazValue");

    // use new partition data so that we can test the mapping later on

    Map<Integer, PartitionData> newUri1PartitionWeights = new HashMap<Integer, PartitionData>(partitionWeight);
    newUri1PartitionWeights.remove(10);
    markUp(server, cluster, uri1, newUri1PartitionWeights, uri1SpecificProperties);

    properties = store.get(cluster);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 2);
    assertEquals(properties.getPartitionDataMap(uri1), newUri1PartitionWeights);
    assertEquals(properties.getPartitionDataMap(uri2), partitionWeight);

    assertNotNull(properties.getUriSpecificProperties());
    assertEquals(properties.getUriSpecificProperties().size(), 2);
    assertEquals(properties.getUriSpecificProperties().get(uri1), uri1SpecificProperties);
    assertEquals(properties.getUriSpecificProperties().get(uri2), uri2SpecificProperties);

    Set<URI> uriSet = new HashSet<URI>();
    uriSet.add(uri1);
    uriSet.add(uri2);

    assertEquals(properties.getUriBySchemeAndPartition("http", 5), uriSet);

    uriSet.remove(uri1);

    assertEquals(properties.getUriBySchemeAndPartition("http", 10), uriSet);
  }

  private void markUp(ZooKeeperServer server, String cluster, URI uri, double weight)
  {
    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    markUp(server, cluster, uri,  partitionWeight, null);
  }

  private void markUp(ZooKeeperServer server,
                      String cluster,
                      URI uri,
                      Map<Integer, PartitionData> partitionDataMap,
                      Map<String, Object> uriSpecificProperties)
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    if (uriSpecificProperties == null)
    {
      server.markUp(cluster, uri, partitionDataMap, callback);
    }
    else
    {
      server.markUp(cluster, uri, partitionDataMap, uriSpecificProperties, callback);
    }
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      fail("Failed to mark server up", e);
    }
  }

  private void markDown(ZooKeeperServer server, String cluster, URI uri)
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    server.markDown(cluster, uri, callback);
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      fail("Failed to mark server down", e);
    }
  }

  @BeforeSuite
  public void doOneTimeSetUp() throws InterruptedException
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

  @AfterSuite
  public void doOneTimeTearDown() throws IOException
  {
    _zkServer.shutdown();
  }

}
