/*
   Copyright (c) 2026 LinkedIn Corp.

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

package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


public class PartitionRoutingTableTest
{
  private static final String CLUSTER_NAME = "test-cluster";
  private static final String SERVICE_NAME = "test-service";

  @Test
  public void testBuildWithNullInputs()
  {
    UriProperties uriProps = createUriProperties(Collections.emptyMap());
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();
    Map<URI, TrackerClient> trackerClients = Collections.emptyMap();

    Assert.assertNull(PartitionRoutingTable.build(null, serviceProps, clusterProps, trackerClients));
    Assert.assertNull(PartitionRoutingTable.build(uriProps, null, clusterProps, trackerClients));
    Assert.assertNull(PartitionRoutingTable.build(uriProps, serviceProps, null, trackerClients));
    Assert.assertNull(PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, null));
  }

  @Test
  public void testBuildWithEmptyPartitions()
  {
    UriProperties uriProps = createUriProperties(Collections.emptyMap());
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps,
        Collections.emptyMap());

    Assert.assertNotNull(table);
    Assert.assertTrue(table.getPartitionClients("http", 0).isEmpty());
    Assert.assertTrue(table.getWeightedUris("http", 0).isEmpty());
  }

  @Test
  public void testBuildWithBasicUris()
  {
    URI uri1 = URI.create("http://host1:8080/service");
    URI uri2 = URI.create("http://host2:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));
    partitions.put(uri2, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    TrackerClient client1 = createMockTrackerClient(uri1);
    TrackerClient client2 = createMockTrackerClient(uri2);
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, client1);
    trackerClients.put(uri2, client2);

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    Map<URI, TrackerClient> clients = table.getPartitionClients("http", 0);
    Assert.assertEquals(clients.size(), 2);
    Assert.assertSame(clients.get(uri1), client1);
    Assert.assertSame(clients.get(uri2), client2);
  }

  @Test
  public void testBuildWithMultiplePartitions()
  {
    URI uri1 = URI.create("http://host1:8080/service");
    URI uri2 = URI.create("http://host2:8080/service");

    Map<Integer, PartitionData> uri1Partitions = new HashMap<>();
    uri1Partitions.put(0, new PartitionData(1.0));
    uri1Partitions.put(1, new PartitionData(0.5));

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, uri1Partitions);
    partitions.put(uri2, Collections.singletonMap(1, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    TrackerClient client1 = createMockTrackerClient(uri1);
    TrackerClient client2 = createMockTrackerClient(uri2);
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, client1);
    trackerClients.put(uri2, client2);

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);

    // Partition 0 should only have uri1
    Map<URI, TrackerClient> partition0 = table.getPartitionClients("http", 0);
    Assert.assertEquals(partition0.size(), 1);
    Assert.assertSame(partition0.get(uri1), client1);

    // Partition 1 should have both
    Map<URI, TrackerClient> partition1 = table.getPartitionClients("http", 1);
    Assert.assertEquals(partition1.size(), 2);
    Assert.assertSame(partition1.get(uri1), client1);
    Assert.assertSame(partition1.get(uri2), client2);
  }

  @Test
  public void testBuildWithBannedUris()
  {
    URI uri1 = URI.create("http://host1:8080/service");
    URI bannedUri = URI.create("http://banned:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));
    partitions.put(bannedUri, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties(bannedUri);
    ClusterProperties clusterProps = createClusterProperties();

    TrackerClient client1 = createMockTrackerClient(uri1);
    TrackerClient bannedClient = createMockTrackerClient(bannedUri);
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, client1);
    trackerClients.put(bannedUri, bannedClient);

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    Map<URI, TrackerClient> clients = table.getPartitionClients("http", 0);
    Assert.assertEquals(clients.size(), 1);
    Assert.assertSame(clients.get(uri1), client1);
    Assert.assertFalse(clients.containsKey(bannedUri));
  }

  @Test
  public void testBuildWithClusterBannedUris()
  {
    URI uri1 = URI.create("http://host1:8080/service");
    URI bannedUri = URI.create("http://banned:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));
    partitions.put(bannedUri, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties(bannedUri);

    TrackerClient client1 = createMockTrackerClient(uri1);
    TrackerClient bannedClient = createMockTrackerClient(bannedUri);
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, client1);
    trackerClients.put(bannedUri, bannedClient);

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    Map<URI, TrackerClient> clients = table.getPartitionClients("http", 0);
    Assert.assertEquals(clients.size(), 1);
    Assert.assertFalse(clients.containsKey(bannedUri));
  }

  @Test
  public void testWeightedUris()
  {
    URI uri1 = URI.create("http://host1:8080/service");
    URI uri2 = URI.create("http://host2:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(0.8)));
    partitions.put(uri2, Collections.singletonMap(0, new PartitionData(0.3)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, createMockTrackerClient(uri1));
    trackerClients.put(uri2, createMockTrackerClient(uri2));

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    Map<URI, Double> weights = table.getWeightedUris("http", 0);
    Assert.assertEquals(weights.size(), 2);
    Assert.assertEquals(weights.get(uri1), 0.8, 0.001);
    Assert.assertEquals(weights.get(uri2), 0.3, 0.001);
  }

  @Test
  public void testWeightedUrisExcludesBannedUris()
  {
    URI uri1 = URI.create("http://host1:8080/service");
    URI bannedUri = URI.create("http://banned:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));
    partitions.put(bannedUri, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties(bannedUri);
    ClusterProperties clusterProps = createClusterProperties();

    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, createMockTrackerClient(uri1));
    trackerClients.put(bannedUri, createMockTrackerClient(bannedUri));

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    Map<URI, Double> weights = table.getWeightedUris("http", 0);
    Assert.assertEquals(weights.size(), 1);
    Assert.assertFalse(weights.containsKey(bannedUri));
  }

  @Test
  public void testMissingSchemeReturnsEmptyMap()
  {
    URI uri1 = URI.create("http://host1:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, createMockTrackerClient(uri1));

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    Assert.assertTrue(table.getPartitionClients("https", 0).isEmpty());
    Assert.assertTrue(table.getWeightedUris("https", 0).isEmpty());
  }

  @Test
  public void testMissingPartitionReturnsEmptyMap()
  {
    URI uri1 = URI.create("http://host1:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, createMockTrackerClient(uri1));

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    Assert.assertTrue(table.getPartitionClients("http", 99).isEmpty());
    Assert.assertTrue(table.getWeightedUris("http", 99).isEmpty());
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testPartitionClientsImmutability()
  {
    URI uri1 = URI.create("http://host1:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, createMockTrackerClient(uri1));

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    table.getPartitionClients("http", 0).put(URI.create("http://evil:8080"), createMockTrackerClient(uri1));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testWeightedUrisImmutability()
  {
    URI uri1 = URI.create("http://host1:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, createMockTrackerClient(uri1));

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);
    table.getWeightedUris("http", 0).put(URI.create("http://evil:8080"), 1.0);
  }

  @Test
  public void testMultipleSchemes()
  {
    URI httpUri = URI.create("http://host1:8080/service");
    URI httpsUri = URI.create("https://host2:8443/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(httpUri, Collections.singletonMap(0, new PartitionData(1.0)));
    partitions.put(httpsUri, Collections.singletonMap(0, new PartitionData(1.0)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    TrackerClient httpClient = createMockTrackerClient(httpUri);
    TrackerClient httpsClient = createMockTrackerClient(httpsUri);
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(httpUri, httpClient);
    trackerClients.put(httpsUri, httpsClient);

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);

    Map<URI, TrackerClient> httpClients = table.getPartitionClients("http", 0);
    Assert.assertEquals(httpClients.size(), 1);
    Assert.assertSame(httpClients.get(httpUri), httpClient);

    Map<URI, TrackerClient> httpsClients = table.getPartitionClients("https", 0);
    Assert.assertEquals(httpsClients.size(), 1);
    Assert.assertSame(httpsClients.get(httpsUri), httpsClient);
  }

  @Test
  public void testUriWithNoTrackerClient()
  {
    URI uri1 = URI.create("http://host1:8080/service");
    URI uri2 = URI.create("http://host2:8080/service");

    Map<URI, Map<Integer, PartitionData>> partitions = new HashMap<>();
    partitions.put(uri1, Collections.singletonMap(0, new PartitionData(1.0)));
    partitions.put(uri2, Collections.singletonMap(0, new PartitionData(0.5)));

    UriProperties uriProps = createUriProperties(partitions);
    ServiceProperties serviceProps = createServiceProperties();
    ClusterProperties clusterProps = createClusterProperties();

    // Only uri1 has a TrackerClient
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri1, createMockTrackerClient(uri1));

    PartitionRoutingTable table = PartitionRoutingTable.build(uriProps, serviceProps, clusterProps, trackerClients);

    Assert.assertNotNull(table);

    // Only uri1 should appear in partition clients
    Map<URI, TrackerClient> clients = table.getPartitionClients("http", 0);
    Assert.assertEquals(clients.size(), 1);
    Assert.assertTrue(clients.containsKey(uri1));

    // But both should appear in weighted URIs (weights come from UriProperties, not TrackerClient)
    Map<URI, Double> weights = table.getWeightedUris("http", 0);
    Assert.assertEquals(weights.size(), 2);
    Assert.assertEquals(weights.get(uri1), 1.0, 0.001);
    Assert.assertEquals(weights.get(uri2), 0.5, 0.001);
  }

  // -- Helper methods --

  private static UriProperties createUriProperties(Map<URI, Map<Integer, PartitionData>> partitions)
  {
    return new UriProperties(CLUSTER_NAME, partitions);
  }

  private static ServiceProperties createServiceProperties(URI... bannedUris)
  {
    return new ServiceProperties(SERVICE_NAME, CLUSTER_NAME, "/service",
        Arrays.asList("random"), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
        Collections.emptyList(), new HashSet<>(Arrays.asList(bannedUris)));
  }

  private static ClusterProperties createClusterProperties(URI... bannedUris)
  {
    return new ClusterProperties(CLUSTER_NAME, Collections.emptyList(), Collections.emptyMap(),
        new HashSet<>(Arrays.asList(bannedUris)));
  }

  private static TrackerClient createMockTrackerClient(URI uri)
  {
    TrackerClient client = Mockito.mock(TrackerClient.class);
    Mockito.when(client.getUri()).thenReturn(uri);
    return client;
  }
}
