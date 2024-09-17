package com.linkedin.d2.xds;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.linkedin.d2.jmx.XdsClientJmx;
import com.linkedin.d2.jmx.XdsServerMetricsProvider;
import com.linkedin.d2.xds.XdsClient.D2URIMapUpdate;
import com.linkedin.d2.xds.XdsClient.ResourceType;
import com.linkedin.d2.xds.XdsClientImpl.DiscoveryResponseData;
import com.linkedin.d2.xds.XdsClientImpl.ResourceSubscriber;
import indis.XdsD2;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.xds.XdsClient.ResourceType.*;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class TestXdsClientImpl
{
  private static final byte[] DATA = "data".getBytes();
  private static final byte[] DATA2 = "data2".getBytes();
  private static final String SERVICE_RESOURCE_NAME = "/d2/services/FooService";
  private static final String CLUSTER_NAME = "FooClusterMaster-prod-ltx1";
  private static final String CLUSTER_RESOURCE_NAME = "/d2/uris/" + CLUSTER_NAME;
  private static final String URI1 = "TestURI1";
  private static final String URI2 = "TestURI2";
  private static final String VERSION1 = "1";
  private static final String VERSION2 = "2";
  private static final String NONCE = "nonce";
  private static final XdsD2.Node NODE_WITH_DATA = XdsD2.Node.newBuilder().setData(ByteString.copyFrom(DATA)).build();
  private static final XdsD2.Node NODE_WITH_DATA2 = XdsD2.Node.newBuilder().setData(ByteString.copyFrom(DATA2)).build();
  private static final XdsD2.Node NODE_WITH_EMPTY_DATA = XdsD2.Node.newBuilder().build();
  private static final Any PACKED_NODE_WITH_DATA = Any.pack(NODE_WITH_DATA);
  private static final Any PACKED_NODE_WITH_DATA2 = Any.pack(NODE_WITH_DATA2);
  private static final Any PACKED_NODE_WITH_EMPTY_DATA = Any.pack(NODE_WITH_EMPTY_DATA);
  private static final XdsClient.NodeUpdate NODE_UPDATE1 = new XdsClient.NodeUpdate(NODE_WITH_DATA);
  private static final XdsClient.NodeUpdate NODE_UPDATE2 = new XdsClient.NodeUpdate(NODE_WITH_DATA2);
  private static final List<Resource> NODE_RESOURCES_WITH_DATA1 = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION1).setName(SERVICE_RESOURCE_NAME).setResource(PACKED_NODE_WITH_DATA).build());
  private static final List<Resource> NODE_RESOURCES_WITH_DATA2 = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION2).setName(SERVICE_RESOURCE_NAME).setResource(PACKED_NODE_WITH_DATA2).build());

  private static final List<Resource> NODE_RESOURCES_WITH_NULL_RESOURCE_FILED = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION1).setName(SERVICE_RESOURCE_NAME).setResource(PACKED_NODE_WITH_EMPTY_DATA).build());

  private static final XdsD2.D2URI D2URI_1 =
      XdsD2.D2URI.newBuilder().setVersion(Long.parseLong(VERSION1)).setClusterName(CLUSTER_NAME).setUri(URI1).build();
  private static final XdsD2.D2URI D2URI_1_1 =
      XdsD2.D2URI.newBuilder().setVersion(Long.parseLong(VERSION2)).setClusterName(CLUSTER_NAME).setUri(URI1)
          .putPartitionDesc(0, 2.0).build();
  private static final XdsD2.D2URI D2URI_2 =
      XdsD2.D2URI.newBuilder().setVersion(Long.parseLong(VERSION1)).setClusterName(CLUSTER_NAME).setUri(URI2).build();
  private static final XdsD2.D2URIMap D2_URI_MAP_WITH_EMPTY_DATA = XdsD2.D2URIMap.newBuilder().build();
  private static final XdsD2.D2URIMap D2_URI_MAP_WITH_DATA1 = XdsD2.D2URIMap.newBuilder()
      .putUris(URI1, D2URI_1).build();
  private static final XdsD2.D2URIMap D2_URI_MAP_WITH_DATA2 = XdsD2.D2URIMap.newBuilder()
      .putUris(URI1, D2URI_1_1) // updated uri1
      .putUris(URI2, D2URI_2).build(); // added ur2
  private static final D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_DATA1 =
      new D2URIMapUpdate(D2_URI_MAP_WITH_DATA1.getUrisMap());
  private static final D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_DATA2 =
      new D2URIMapUpdate(D2_URI_MAP_WITH_DATA2.getUrisMap());
  private static final Any PACKED_D2_URI_MAP_WITH_DATA1 = Any.pack(D2_URI_MAP_WITH_DATA1);
  private static final Any PACKED_D2_URI_MAP_WITH_DATA2 = Any.pack(D2_URI_MAP_WITH_DATA2);
  private static final Any PACKED_D2_URI_MAP_WITH_EMPTY_DATA = Any.pack(D2_URI_MAP_WITH_EMPTY_DATA);
  private static final List<Resource> URI_MAP_RESOURCE_WITH_DATA1 = Collections.singletonList(Resource.newBuilder()
      .setVersion(VERSION1)
      .setName(CLUSTER_RESOURCE_NAME)
      .setResource(PACKED_D2_URI_MAP_WITH_DATA1)
      .build());

  private static final List<Resource> URI_MAP_RESOURCE_WITH_DATA2 = Collections.singletonList(Resource.newBuilder()
      .setVersion(VERSION1)
      .setName(CLUSTER_RESOURCE_NAME)
      .setResource(PACKED_D2_URI_MAP_WITH_DATA2)
      .build());
  private static final List<Resource> EMPTY_URI_MAP_RESOURCE = Collections.singletonList(
      Resource.newBuilder()
          .setVersion(VERSION2)
          .setName(CLUSTER_RESOURCE_NAME)
          .setResource(PACKED_D2_URI_MAP_WITH_EMPTY_DATA)
          .build());
  //  private static final List<String> REMOVED_RESOURCE = ;
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA1 =
      new DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_DATA1, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA2 =
      new DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_DATA2, null, NONCE, null);

  // case 1: Resource in ResourceUpdate is null, failed to parse which causes InvalidProtocolBufferException
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL =
      new DiscoveryResponseData(
          NODE,
          Collections.singletonList(Resource.newBuilder().setVersion(VERSION1).setName(SERVICE_RESOURCE_NAME)
              // not set resource field
              .build()),
          null,
          NONCE,
          null);

  // case 2: Resource field in Resource is null
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FILED =
      new DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_NULL_RESOURCE_FILED, null, NONCE, null);

  // case3 : ResourceList is empty
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_WITH_EMPTY_NODE_RESPONSE =
      new DiscoveryResponseData(NODE, Collections.emptyList(), null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA1 =
      new DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_DATA1, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA2 =
      new DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_DATA2, null, NONCE, null);

  // case1: Resource in ResourceUpdate is null, failed to parse response.resource
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_RESOURCE_IS_NULL =
      new DiscoveryResponseData(
          D2_URI_MAP,
          Collections.singletonList(Resource.newBuilder().setVersion(VERSION1).setName(CLUSTER_RESOURCE_NAME)
              // not set resource field
              .build()),
          null,
          NONCE,
          null);

  // case2 : Resource field in Resource is null
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_EMPTY_MAP =
      new DiscoveryResponseData(D2_URI_MAP, EMPTY_URI_MAP_RESOURCE, null, NONCE, null);

  // case3 : ResourceList is empty
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESPONSE =
      new DiscoveryResponseData(D2_URI_MAP, null, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL =
      new DiscoveryResponseData(NODE, Collections.emptyList(), Collections.singletonList(SERVICE_RESOURCE_NAME), NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL =
      new DiscoveryResponseData(D2_URI_MAP, Collections.emptyList(), Collections.singletonList(CLUSTER_RESOURCE_NAME), NONCE, null);

  private static final String CLUSTER_GLOB_COLLECTION = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/*";
  private static final String URI_URN1 = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/" + URI1;
  private static final String URI_URN2 = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/" + URI2;

  @Test
  public void testHandleD2NodeResponseWithData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // subscriber original data is null
    fixture._nodeSubscriber.setData(null);
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE1));
    verifyZeroInteractions(fixture._serverMetricsProvider); // initial update should not track latency
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeSubscriber.getData();
    // subscriber data should be updated to NODE_UPDATE1
    Assert.assertEquals(Objects.requireNonNull(actualData).getNodeData(), NODE_UPDATE1.getNodeData());

    // subscriber original data is invalid, xds server latency won't be tracked
    fixture._nodeSubscriber.setData(new XdsClient.NodeUpdate(null));
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    fixture.verifyAckSent(2);
    verify(fixture._resourceWatcher, times(2)).onChanged(eq(NODE_UPDATE1));
    verifyZeroInteractions(fixture._serverMetricsProvider);

    // subscriber data should be updated to NODE_UPDATE2
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA2);
    actualData = (XdsClient.NodeUpdate) fixture._nodeSubscriber.getData();
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE2));
    verify(fixture._serverMetricsProvider).trackLatency(anyLong());
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE2.getNodeData());
  }

  @Test
  public void testHandleD2NodeUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_WITH_EMPTY_NODE_RESPONSE);
    fixture.verifyAckSent(1);
  }

  @DataProvider(name = "badNodeUpdateTestCases")
  public Object[][] provideBadNodeDataTestCases()
  {
    return new Object[][]{
        {DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL, true},
        {DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FILED, false},
    };
  }

  @Test(dataProvider = "badNodeUpdateTestCases")
  public void testHandleD2NodeUpdateWithBadData(DiscoveryResponseData badData, boolean nackExpected)
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._nodeSubscriber.setData(null);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyAckOrNack(nackExpected, 1);
    verify(fixture._resourceWatcher).onChanged(eq(NODE.emptyData()));
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeSubscriber.getData();
    Assert.assertNull(Objects.requireNonNull(actualData).getNodeData());

    fixture._nodeSubscriber.setData(NODE_UPDATE1);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyAckOrNack(nackExpected, 2);
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE1));
    actualData = (XdsClient.NodeUpdate) fixture._nodeSubscriber.getData();
    // bad data will not overwrite the original valid data
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE1.getNodeData());
  }

  @Test
  public void testHandleD2NodeResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._nodeSubscriber.setData(NODE_UPDATE1);
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE1));
    verify(fixture._nodeSubscriber).onRemoval();
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeSubscriber.getData();
    //  removed resource will not overwrite the original valid data
    Assert.assertEquals(Objects.requireNonNull(actualData).getNodeData(), NODE_UPDATE1.getNodeData());
  }

  @Test
  public void testHandleD2URIMapResponseWithData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // subscriber original data is null
    fixture._clusterSubscriber.setData(null);
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA1
    Assert.assertEquals(Objects.requireNonNull(actualData).getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());

    // subscriber original data is invalid, xds server latency won't be tracked
    fixture._clusterSubscriber.setData(new XdsClient.D2URIMapUpdate(null));
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA1);
    verify(fixture._resourceWatcher, times(2)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    fixture.verifyAckSent(2);

    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA2); // updated uri1, added uri2
    actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA2
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA2));
    verify(fixture._serverMetricsProvider, times(2)).trackLatency(anyLong());
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA2.getURIMap());
    fixture.verifyAckSent(3);
  }

  @Test
  public void testHandleD2URIMapUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // Sanity check that the code handles empty responses
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESPONSE);
    fixture.verifyAckSent(1);
  }

  @DataProvider(name = "badD2URIMapUpdateTestCases")
  public Object[][] provideBadD2URIMapDataTestCases()
  {
    return new Object[][]{
        {DISCOVERY_RESPONSE_URI_MAP_RESOURCE_IS_NULL, true},
        {DISCOVERY_RESPONSE_URI_MAP_EMPTY_MAP, false},
    };
  }

  @Test(dataProvider = "badD2URIMapUpdateTestCases")
  public void testHandleD2URIMapUpdateWithBadData(DiscoveryResponseData badData, boolean invalidData)
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._clusterSubscriber.setData(null);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyAckOrNack(invalidData, 1);
    // If the map is empty, we expect an empty map, but if it's invalid we expect a null
    D2URIMapUpdate expectedUpdate =
        invalidData
            ? (D2URIMapUpdate) D2_URI_MAP.emptyData()
            : new D2URIMapUpdate(Collections.emptyMap());
    verify(fixture._resourceWatcher).onChanged(eq(expectedUpdate));
    verify(fixture._clusterSubscriber).setData(eq(null));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    Assert.assertEquals(actualData, expectedUpdate);

    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyAckOrNack(invalidData, 2);
    actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    Objects.requireNonNull(actualData);
    if (invalidData) {
      verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
      // bad data will not overwrite the original valid data
      Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
    } else {
      verify(fixture._resourceWatcher, times(2)).onChanged(eq(expectedUpdate));
      // But an empty cluster should clear the data
      Assert.assertEquals(actualData.getURIMap(), Collections.emptyMap());
    }
    verifyZeroInteractions(fixture._serverMetricsProvider);
  }

  @Test
  public void testHandleD2URIMapResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._clusterSubscriber).onRemoval();
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // removed resource will not overwrite the original valid data
    Assert.assertEquals(Objects.requireNonNull(actualData).getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
  }

  @Test
  public void testHandleD2URICollectionResponseWithData()
  {
    DiscoveryResponseData createUri1 = new DiscoveryResponseData(D2_URI, Collections.singletonList(
        Resource.newBuilder()
            .setVersion(VERSION1)
            .setName(URI_URN1)
            .setResource(Any.pack(D2URI_1))
            .build()
    ), null, NONCE, null);
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // subscriber original data is null
    fixture._clusterSubscriber.setData(null);
    fixture._xdsClientImpl.handleResponse(createUri1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA1
    Assert.assertEquals(Objects.requireNonNull(actualData).getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());

    // subscriber original data is invalid, xds server latency won't be tracked
    fixture._clusterSubscriber.setData(new D2URIMapUpdate(null));
    fixture._xdsClientImpl.handleResponse(createUri1);
    fixture.verifyAckSent(2);
    verify(fixture._resourceWatcher, times(2)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);

    DiscoveryResponseData createUri2Delete1 = new DiscoveryResponseData(D2_URI, Collections.singletonList(
        Resource.newBuilder()
            .setVersion(VERSION1)
            .setName(URI_URN2)
            .setResource(Any.pack(D2URI_2))
            .build()
    ), Collections.singletonList(URI_URN1), NONCE, null);
    fixture._xdsClientImpl.handleResponse(createUri2Delete1);
    actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA2
    D2URIMapUpdate expectedUpdate = new D2URIMapUpdate(Collections.singletonMap(URI2, D2URI_2));
    verify(fixture._resourceWatcher).onChanged(eq(expectedUpdate));
    // track latency only for updated/new uri (not for deletion)
    verify(fixture._serverMetricsProvider).trackLatency(anyLong());
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    fixture.verifyAckSent(3);

    // Finally sanity check that the client correctly handles the deletion of the final URI in the collection
    DiscoveryResponseData deleteUri2 =
        new DiscoveryResponseData(D2_URI, null, Collections.singletonList(URI_URN2), NONCE, null);
    fixture._xdsClientImpl.handleResponse(deleteUri2);
    actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to empty map
    expectedUpdate = new D2URIMapUpdate(Collections.emptyMap());
    verify(fixture._resourceWatcher).onChanged(eq(expectedUpdate));
    verifyNoMoreInteractions(fixture._serverMetricsProvider);
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    fixture.verifyAckSent(4);
  }

  @Test
  public void testHandleD2URICollectionUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // Sanity check that the code handles empty responses
    fixture._xdsClientImpl.handleResponse(new DiscoveryResponseData(D2_URI, null, null, NONCE, null));
    fixture.verifyAckSent(1);
  }

  @Test
  public void testHandleD2URICollectionUpdateWithBadData()
  {
    DiscoveryResponseData badData = new DiscoveryResponseData(
        D2_URI,
        Collections.singletonList(Resource.newBuilder().setVersion(VERSION1).setName(URI_URN1)
            // resource field not set
            .build()),
        null,
        NONCE,
        null);

    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._clusterSubscriber.setData(null);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyNackSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP.emptyData()));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    Assert.assertNull(Objects.requireNonNull(actualData).getURIMap());

    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyNackSent(2);
    // Due to the way glob collection updates are handled, bad data is dropped rather than showing any visible side
    // effects other than NACKing the response.
    verify(fixture._resourceWatcher, times(0)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);
  }

  @Test
  public void testHandleD2URICollectionResponseWithRemoval()
  {
    DiscoveryResponseData removeClusterResponse =
        new DiscoveryResponseData(D2_URI, null, Collections.singletonList(CLUSTER_GLOB_COLLECTION), NONCE, null);

    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(removeClusterResponse);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._clusterSubscriber).onRemoval();
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // removed resource will not overwrite the original valid data
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
  }

  @Test
  public void testWildCardResourceSubscription()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();

    XdsClient.WildcardNodeResourceWatcher nodeWildCardWatcher = Mockito.mock(XdsClient.WildcardNodeResourceWatcher.class);
    XdsClient.WildcardD2URIMapResourceWatcher uriMapWildCardWatcher = Mockito.mock(XdsClient.WildcardD2URIMapResourceWatcher.class);
    fixture._xdsClientImpl.getWildcardResourceSubscriber(NODE).addWatcher(nodeWildCardWatcher);
    fixture._xdsClientImpl.getWildcardResourceSubscriber(D2_URI_MAP).addWatcher(uriMapWildCardWatcher);

    // NODE resource added
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    fixture.verifyAckSent(1);
    nodeWildCardWatcher.onChanged(eq(SERVICE_RESOURCE_NAME) , eq(NODE_UPDATE1));

    // NODE resource removed
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL);
    fixture.verifyAckSent(2);
    nodeWildCardWatcher.onRemoval(eq(SERVICE_RESOURCE_NAME));

    // URI_MAP resource added
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA1);
    fixture.verifyAckSent(3);
    uriMapWildCardWatcher.onChanged(eq(CLUSTER_RESOURCE_NAME), eq(D2_URI_MAP_UPDATE_WITH_DATA1));

    // URI_MAP resource removed
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL);
    fixture.verifyAckSent(4);
    uriMapWildCardWatcher.onRemoval(eq(CLUSTER_RESOURCE_NAME));
  }

  @Test
  public void testResourceSubscriberAddWatcher()
  {
    ResourceSubscriber subscriber = new ResourceSubscriber(NODE, "foo", null);
    XdsClient.ResourceWatcher watcher = Mockito.mock(XdsClient.ResourceWatcher.class);
    subscriber.addWatcher(watcher);
    verify(watcher, times(0)).onChanged(any());

    D2URIMapUpdate update = new D2URIMapUpdate(Collections.emptyMap());
    subscriber.setData(update);
    for (int i = 0; i < 10; i++)
    {
      subscriber.addWatcher(watcher);
    }
    verify(watcher, times(10)).onChanged(eq(update));
  }

  private static class XdsClientImplFixture
  {
    XdsClientImpl _xdsClientImpl;
    @Mock
    XdsClientJmx _xdsClientJmx;
    ResourceSubscriber _nodeSubscriber;
    ResourceSubscriber _clusterSubscriber;
    XdsClientImpl.WildcardResourceSubscriber _nodeWildcardSubscriber;
    XdsClientImpl.WildcardResourceSubscriber _uriMapWildcardSubscriber;
    Map<ResourceType, Map<String, ResourceSubscriber>> _subscribers = new HashMap<>();
    Map<ResourceType, XdsClientImpl.WildcardResourceSubscriber> _wildcardSubscribers = new HashMap<>();

    @Mock
    XdsClient.ResourceWatcher _resourceWatcher;
    @Mock
    XdsServerMetricsProvider _serverMetricsProvider;

    XdsClientImplFixture()
    {
      this(false);
    }

    XdsClientImplFixture(boolean useGlobCollections)
    {
      MockitoAnnotations.initMocks(this);
      _nodeSubscriber = spy(new ResourceSubscriber(NODE, SERVICE_RESOURCE_NAME, _xdsClientJmx));
      _clusterSubscriber = spy(new ResourceSubscriber(D2_URI_MAP, CLUSTER_RESOURCE_NAME, _xdsClientJmx));
      _nodeWildcardSubscriber = new XdsClientImpl.WildcardResourceSubscriber(NODE);
      _uriMapWildcardSubscriber = new XdsClientImpl.WildcardResourceSubscriber(D2_URI_MAP);



      doNothing().when(_resourceWatcher).onChanged(any());
      for (ResourceSubscriber subscriber : Lists.newArrayList(_nodeSubscriber, _clusterSubscriber))
      {
        subscriber.addWatcher(_resourceWatcher);
        _subscribers.put(subscriber.getType(), Collections.singletonMap(subscriber.getResource(), subscriber));
      }
      _wildcardSubscribers.put(NODE, _nodeWildcardSubscriber);
      _wildcardSubscribers.put(D2_URI_MAP, _uriMapWildcardSubscriber);
      doNothing().when(_serverMetricsProvider).trackLatency(anyLong());
      _xdsClientImpl = spy(new XdsClientImpl(null, null, null, 0,
          useGlobCollections, _serverMetricsProvider));
      doNothing().when(_xdsClientImpl).sendAckOrNack(any(), any(), any());
      when(_xdsClientImpl.getXdsClientJmx()).thenReturn(_xdsClientJmx);
      when(_xdsClientImpl.getResourceSubscriberMap(any()))
          .thenAnswer(a -> _subscribers.get((ResourceType) a.getArguments()[0]));
      doNothing().when(_xdsClientImpl).watchAllXdsResources(any());
      when(_xdsClientImpl.getWildcardResourceSubscriber(any()))
          .thenAnswer(a -> _wildcardSubscribers.get((ResourceType) a.getArguments()[0]));

    }

    void verifyAckSent(int count)
    {
      verify(_xdsClientImpl, times(count)).sendAckOrNack(any(), any(), eq(Collections.emptyList()));
    }

    void verifyNackSent(int count)
    {
      verify(_xdsClientImpl, times(count)).sendAckOrNack(any(), any(), argThat(not(Collections.emptyList())));
    }

    void verifyAckOrNack(boolean nackExpected, int count)
    {
      if (nackExpected)
      {
        verifyNackSent(count);
      }
      else
      {
        verifyAckSent(count);
      }
    }
  }
}
