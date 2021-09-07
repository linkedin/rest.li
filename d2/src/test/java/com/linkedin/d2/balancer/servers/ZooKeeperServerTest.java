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
import com.linkedin.d2.balancer.properties.PropertyKeys;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ZooKeeperServerTest
{
  private static final int    PORT = 11711;
  private static final String BAD_CLUSTER = "BAD CLUSTER";
  private static final URI URI_1 = URI.create("http://cluster-1/test");
  private static final URI URI_2 = URI.create("http://cluster-1-again/test");
  public static final String CLUSTER_1 = "cluster-1";

  private ZKServer _zkServer;
  private ZooKeeperServer _server;
  private ZooKeeperEphemeralStore<UriProperties> _store;
  private Map<Integer, PartitionData> _partitionWeight;
  private Map<String, Object> _uri1SpecificProperties;
  private Map<String, Object> _uri2SpecificProperties;


  @BeforeMethod
  public void setUp() throws Exception
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

    ZKConnection zkClient = new ZKConnection("localhost:" + PORT, 5000);
    zkClient.start();

    _store = new ZooKeeperEphemeralStore<>(zkClient,
                                           new UriPropertiesJsonSerializer(),
                                           new UriPropertiesMerger(),
                                           "/echo/lb/uris");
    FutureCallback<None> callback = new FutureCallback<>();
    _store.start(callback);
    callback.get();

    _server = new ZooKeeperServer(_store);

    _partitionWeight = new HashMap<>();
    _uri1SpecificProperties = new HashMap<>();
    _uri2SpecificProperties = new HashMap<>();
  }

  @Test(groups = { "small", "back-end" })
  public void testZkServer() throws Exception
  {
    assertNull(_store.get(CLUSTER_1));
    assertNull(_store.get("cluster-2"));

    // bring up uri1
    markUp(_server, CLUSTER_1, URI_1, 0.5d);

    UriProperties properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI_1).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 0.5d);
    assertEquals(properties.Uris().size(), 1);

    // test mark up when already up call
    markUp(_server, CLUSTER_1, URI_1, 2d);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI_1).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 2d);
    assertEquals(properties.Uris().size(), 1);

    // bring up uri 2
    markUp(_server, CLUSTER_1, URI_2, 1.5d);

    properties = _store.get(CLUSTER_1);
    assertEquals(properties.getPartitionDataMap(URI_1).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 2d);
    assertEquals(properties.getPartitionDataMap(URI_2).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 1.5d);
    assertEquals(properties.Uris().size(), 2);

    // bring down uri 1
    markDown(_server, CLUSTER_1, URI_1);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI_2).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 1.5d);
    assertEquals(properties.Uris().size(), 1);

    // test bring down when already down
    markDown(_server, CLUSTER_1, URI_1);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI_2).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 1.5d);
    assertEquals(properties.Uris().size(), 1);

    // bring down uri 2
    markDown(_server, CLUSTER_1, URI_2);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 0);

    // test bad cluster doesn't exist
    markDown(_server, BAD_CLUSTER, URI_1);

    properties = _store.get("BAD CLUSTER");
    assertNull(properties);

    // bring up uri1
    _partitionWeight.put(5, new PartitionData(0.3d));
    _partitionWeight.put(15, new PartitionData(0.7d));
    markUp(_server, CLUSTER_1, URI_1, _partitionWeight, null);
    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);

    assertEquals(properties.getPartitionDataMap(URI_1), _partitionWeight);

    _uri2SpecificProperties.put("foo", "fooValue");
    _uri2SpecificProperties.put("bar", 1);

    _partitionWeight.put(10, new PartitionData(1d));

    // bring up uri2 with uri specific properties

    markUp(_server, CLUSTER_1, URI_2, _partitionWeight, _uri2SpecificProperties);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 2);
    assertEquals(properties.getPartitionDataMap(URI_2), _partitionWeight);

    assertNotNull(properties.getUriSpecificProperties());
    assertEquals(properties.getUriSpecificProperties().size(), 1);
    assertEquals(properties.getUriSpecificProperties().get(URI_2), _uri2SpecificProperties);

    // bring down uri1 and bring it back up again with properties

    markDown(_server, CLUSTER_1, URI_1);

    _uri1SpecificProperties.put("baz", "bazValue");

    // use new partition data so that we can test the mapping later on

    Map<Integer, PartitionData> newUri1PartitionWeights = new HashMap<>(_partitionWeight);
    newUri1PartitionWeights.remove(10);
    markUp(_server, CLUSTER_1, URI_1, newUri1PartitionWeights, _uri1SpecificProperties);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.Uris().size(), 2);
    assertEquals(properties.getPartitionDataMap(URI_1), newUri1PartitionWeights);
    assertEquals(properties.getPartitionDataMap(URI_2), _partitionWeight);

    assertNotNull(properties.getUriSpecificProperties());
    assertEquals(properties.getUriSpecificProperties().size(), 2);
    assertEquals(properties.getUriSpecificProperties().get(URI_1), _uri1SpecificProperties);
    assertEquals(properties.getUriSpecificProperties().get(URI_2), _uri2SpecificProperties);

    Set<URI> uriSet = new HashSet<>();
    uriSet.add(URI_1);
    uriSet.add(URI_2);

    assertEquals(properties.getUriBySchemeAndPartition("http", 5), uriSet);

    uriSet.remove(URI_1);

    assertEquals(properties.getUriBySchemeAndPartition("http", 10), uriSet);

    // reset uri1 and changeWeight of uri1 with no preexisting uri properties
    markDown(_server, CLUSTER_1, URI_1);
    markUp(_server, CLUSTER_1, URI_1, 0.5d);

    changeWeight(_server, CLUSTER_1, URI_1, true, 1d);

    _uri1SpecificProperties.clear();
    _uri1SpecificProperties.put(PropertyKeys.DO_NOT_SLOW_START, true);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI_1).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 1d);
    assertEquals(properties.getUriSpecificProperties().get(URI_1), _uri1SpecificProperties);

    // changeWeight of uri2 with preexisting uri properties
    changeWeight(_server, CLUSTER_1, URI_2, true, 0.9d);

    _uri2SpecificProperties.put(PropertyKeys.DO_NOT_SLOW_START, true);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI_2).get(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getWeight(), 0.9d);
    assertEquals(properties.getUriSpecificProperties().get(URI_2), _uri2SpecificProperties);

    // changeWeight on uri1 using partitionData
    changeWeight(_server, CLUSTER_1, URI_1, true, _partitionWeight);
    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getPartitionDataMap(URI_1), _partitionWeight);
    assertEquals(properties.getUriSpecificProperties().get(URI_1).get(PropertyKeys.DO_NOT_SLOW_START), true);

    // changeWeight on a cluster that doesn't exist
    try
    {
      changeWeight(_server, BAD_CLUSTER, URI_1, true, 0.5d);
    }
    catch (RuntimeException e)
    {

    }
    properties = _store.get(BAD_CLUSTER);
    assertNull(properties);

    // changeWeight on a uri that doesn't exist
    markDown(_server, CLUSTER_1, URI_2);
    try
    {
      changeWeight(_server, CLUSTER_1, URI_2, true, 0.5d);
    }
    catch (RuntimeException e)
    {

    }
    properties = _store.get(CLUSTER_1);
    assertTrue(!properties.Uris().contains(URI_2));

    // changeWeight properly changes existing doNotSlowStart from true to false
    properties = _store.get(CLUSTER_1);
    assertEquals(properties.getUriSpecificProperties().get(URI_1).get(PropertyKeys.DO_NOT_SLOW_START), true);
    changeWeight(_server, CLUSTER_1, URI_1, false, 1.0d);
    properties = _store.get(CLUSTER_1);
    assertEquals(properties.getUriSpecificProperties().get(URI_1).get(PropertyKeys.DO_NOT_SLOW_START), false);
  }

  @Test
  public void testAddUriSpecificProperty() throws Exception
  {
    markUp(_server, CLUSTER_1, URI_1, 1);

    // add uri specific property
    final String propertyKey = "propertyKey";
    final int propertyValue = 123;
    _uri1SpecificProperties.put(propertyKey, propertyValue);

    addUriSpecificProperty(CLUSTER_1, URI_1, _partitionWeight, propertyKey, propertyValue);

    UriProperties properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getUriSpecificProperties().get(URI_1), _uri1SpecificProperties);

    // change the value
    final int propertyValue2 = 456;
    _uri1SpecificProperties.put(propertyKey, propertyValue2);

    addUriSpecificProperty(CLUSTER_1, URI_1, _partitionWeight, propertyKey, propertyValue2);

    properties = _store.get(CLUSTER_1);
    assertNotNull(properties);
    assertEquals(properties.getUriSpecificProperties().get(URI_1), _uri1SpecificProperties);

    boolean error = false;
    // invoke on a cluster that doesn't exist
    try
    {
      addUriSpecificProperty(BAD_CLUSTER, URI_1, _partitionWeight, propertyKey, propertyValue);
    }
    catch (RuntimeException e)
    {
      error = true;
    }
    Assert.assertTrue(error);

    error = false;

    // invoke on a uri that doesn't exist
    markDown(_server, CLUSTER_1, URI_2);
    try
    {
      addUriSpecificProperty(CLUSTER_1, URI_2, _partitionWeight, propertyKey, propertyValue);
    }
    catch (RuntimeException e)
    {
      error = true;
    }
    Assert.assertTrue(error);
  }

  private void markUp(ZooKeeperServer server, String cluster, URI uri, double weight)
  {
    Map<Integer, PartitionData> partitionWeight = new HashMap<>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    markUp(server, cluster, uri,  partitionWeight, null);
  }

  private void markUp(ZooKeeperServer server,
                      String cluster,
                      URI uri,
                      Map<Integer, PartitionData> partitionDataMap,
                      Map<String, Object> uriSpecificProperties)
  {
    FutureCallback<None> callback = new FutureCallback<>();
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
    FutureCallback<None> callback = new FutureCallback<>();
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

  private void changeWeight(ZooKeeperServer server,
                            String cluster,
                            URI uri,
                            boolean doNotSlowStart,
                            double weight)
  {
    Map<Integer, PartitionData> partitionWeight = new HashMap<>();
    partitionWeight.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    changeWeight(server, cluster, uri, doNotSlowStart, partitionWeight);
  }

  private void changeWeight(ZooKeeperServer server,
                            String cluster,
                            URI uri,
                            boolean doNotSlowStart,
                            Map<Integer, PartitionData> partitionDataMap)
  {
    FutureCallback<None> callback = new FutureCallback<>();
    server.changeWeight(cluster, uri, partitionDataMap, doNotSlowStart, callback);
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  private void addUriSpecificProperty(String cluster,
                                      URI uri,
                                      Map<Integer, PartitionData> partitionDataMap,
                                      String uriSpecificPropertiesName,
                                      Object uriSpecificPropertiesValue)
  {
    FutureCallback<None> callback = new FutureCallback<>();
    _server.addUriSpecificProperty(cluster, "addUriSpecificProperty", uri, partitionDataMap, uriSpecificPropertiesName, uriSpecificPropertiesValue, callback);
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  @AfterMethod
  public void tearDown() throws IOException
  {
    _zkServer.shutdown();
  }

}
