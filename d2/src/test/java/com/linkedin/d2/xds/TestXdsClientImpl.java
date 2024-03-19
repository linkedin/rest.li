package com.linkedin.d2.xds;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.linkedin.d2.jmx.XdsClientJmx;
import com.linkedin.d2.xds.XdsClient.ResourceType;
import com.linkedin.d2.xds.XdsClientImpl.DiscoveryResponseData;
import com.linkedin.d2.xds.XdsClientImpl.ResourceSubscriber;
import indis.XdsD2;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.xds.XdsClient.ResourceType.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class TestXdsClientImpl
{
  private static final byte[] DATA = "data".getBytes();
  private static final String RESOURCE_NAME = "FooService";
  private static final String CLUSTER_NAME = "FooClusterMaster-prod-ltx1";
  private static final String URI1 = "TestURI";
  private static final String URI2 = "TestURI2";
  private static final String VERSION1 = "1";
  private static final String VERSION2 = "2";
  private static final String NONCE = "nonce";
  private static final XdsD2.Node NODE_WITH_DATA = XdsD2.Node.newBuilder().setData(ByteString.copyFrom(DATA)).build();
  private static final XdsD2.Node NODE_WITH_EMPTY_DATA = XdsD2.Node.newBuilder().build();
  private static final Any PACKED_NODE_WITH_DATA = Any.pack(NODE_WITH_DATA);
  private static final Any PACKED_NODE_WITH_EMPTY_DATA = Any.pack(NODE_WITH_EMPTY_DATA);
  private static final XdsClient.NodeUpdate NODE_UPDATE1 = new XdsClient.NodeUpdate(NODE_WITH_DATA);
  private static final XdsClient.NodeUpdate NODE_UPDATE2 = new XdsClient.NodeUpdate(NODE_WITH_DATA);
  private static final List<Resource> NODE_RESOURCES_WITH_DATA1 = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION1).setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_DATA).build());
  private static final List<Resource> NODE_RESOURCES_WITH_DATA2 = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION2).setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_DATA).build());

  private static final List<Resource> RESOURCES_WITH_RESOURCE_IS_NULL =
      Collections.singletonList(Resource.newBuilder().setVersion(VERSION1).setName(RESOURCE_NAME)
          // not set resource field
          .build());
  private static final List<Resource> NODE_RESOURCES_WITH_NULL_RESOURCE_FILED = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION1).setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_EMPTY_DATA).build());

  private static final XdsD2.D2URI.Builder URI_BUILDER1 =
      XdsD2.D2URI.newBuilder().setVersion(Long.parseLong(VERSION1)).setClusterName(CLUSTER_NAME).setUri(URI1);
  private static final XdsD2.D2URI.Builder URI_BUILDER2 =
      XdsD2.D2URI.newBuilder().setVersion(Long.parseLong(VERSION1)).setClusterName(CLUSTER_NAME).setUri(URI2);
  private static final XdsD2.D2URIMap.Builder D2_URI_MAP_BUILDER = XdsD2.D2URIMap.newBuilder();
  private static final XdsD2.D2URIMap D2_URI_MAP_WITH_EMPTY_DATA = XdsD2.D2URIMap.newBuilder().build();
  private static final XdsD2.D2URIMap D2_URI_MAP_WITH_DATA1 =
      D2_URI_MAP_BUILDER.putUris(RESOURCE_NAME, URI_BUILDER1.build()).build();
  private static final XdsD2.D2URIMap D2_URI_MAP_WITH_DATA2 =
      D2_URI_MAP_BUILDER.putUris(RESOURCE_NAME, URI_BUILDER2.build()).build();
  private static final XdsClient.D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_DATA1 =
      new XdsClient.D2URIMapUpdate(D2_URI_MAP_WITH_DATA1.getUrisMap());
  private static final XdsClient.D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_DATA2 =
      new XdsClient.D2URIMapUpdate(D2_URI_MAP_WITH_DATA2.getUrisMap());
  private static final Any PACKED_D2_URI_MAP_WITH_DATA1 = Any.pack(D2_URI_MAP_WITH_DATA1);
  private static final Any PACKED_D2_URI_MAP_WITH_DATA2 = Any.pack(D2_URI_MAP_WITH_DATA2);
  private static final Any PACKED_D2_URI_MAP_WITH_EMPTY_DATA = Any.pack(D2_URI_MAP_WITH_EMPTY_DATA);
  private static final List<Resource> URI_MAP_RESOURCE_WITH_DATA1 = Collections.singletonList(Resource.newBuilder()
      .setVersion(VERSION1)
      .setName(RESOURCE_NAME)
      .setResource(PACKED_D2_URI_MAP_WITH_DATA1)
      .build());

  private static final List<Resource> URI_MAP_RESOURCE_WITH_DATA2 = Collections.singletonList(Resource.newBuilder()
      .setVersion(VERSION1)
      .setName(RESOURCE_NAME)
      .setResource(PACKED_D2_URI_MAP_WITH_DATA2)
      .build());
  private static final List<Resource> URI_MAP_RESOURCE_WITH_NULL_RESOURCE_FILED = Collections.singletonList(
      Resource.newBuilder()
          .setVersion(VERSION2)
          .setName(RESOURCE_NAME)
          .setResource(PACKED_D2_URI_MAP_WITH_EMPTY_DATA)
          .build());
  private static final List<String> REMOVED_RESOURCE = Collections.singletonList(RESOURCE_NAME);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA1 =
      new DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_DATA1, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA2 =
      new DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_DATA2, null, NONCE, null);

  // case 1: Resource in ResourceUpdate is null, failed to parse which causes InvalidProtocolBufferException
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL =
      new DiscoveryResponseData(NODE, RESOURCES_WITH_RESOURCE_IS_NULL, null, NONCE, null);

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
      new DiscoveryResponseData(D2_URI_MAP, RESOURCES_WITH_RESOURCE_IS_NULL, null, NONCE, null);

  // case2 : Resource field in Resource is null
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_NULL_DATA_IN_RESOURCE_FILED =
      new DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_NULL_RESOURCE_FILED, null, NONCE, null);

  // case3 : ResourceList is empty
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESPONSE =
      new DiscoveryResponseData(D2_URI_MAP, Collections.emptyList(), null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_WITH_EMPTY_URI_COLLECTION_RESPONSE =
      new DiscoveryResponseData(D2_URI, null, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL =
      new DiscoveryResponseData(NODE, Collections.emptyList(), REMOVED_RESOURCE, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL =
      new DiscoveryResponseData(D2_URI_MAP, Collections.emptyList(), REMOVED_RESOURCE, NONCE, null);

  private static final String CLUSTER_GLOB_COLLECTION = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/*";
  private static final String URI_URN1 = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/" + URI1;
  private static final String URI_URN2 = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/" + URI2;
//  private static final D2URICollectionUpdate D2_URI_COLLECTION_UPDATE_WITH_DATA1 =
//      new D2URICollectionUpdate()
//          .addUri(URI1, URI_BUILDER1.build());

  @Test
  public void testHandleD2NodeResponseWithData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // subscriber original data is null
    fixture._nodeResourceSubscriber.setData(null);
    fixture._xdsClientImpl.handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(NODE);
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE1));
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    // subscriber data should be updated to NODE_UPDATE1
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE1.getNodeData());

    // subscriber data should be updated to NODE_UPDATE2
    fixture._xdsClientImpl.handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA2);
    actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE2));
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE2.getNodeData());
  }

  @Test
  public void testHandleD2NodeUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._xdsClientImpl.handleD2NodeResponse(DISCOVERY_RESPONSE_WITH_EMPTY_NODE_RESPONSE);
    verify(fixture._xdsClientImpl).handleResourceUpdate(eq(new HashMap<>()), eq(NODE));
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
  }

  @DataProvider(name = "badNodeUpdateTestCases")
  public Object[][] provideBadNodeDataTestCases()
  {
    return new Object[][]{{DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL},
        {DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FILED},};
  }

  @Test(dataProvider = "badNodeUpdateTestCases")
  public void testHandleD2NodeUpdateWithBadData(DiscoveryResponseData badData)
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._nodeResourceSubscriber.setData(null);
    fixture._xdsClientImpl.handleD2NodeResponse(badData);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(NODE);
    verify(fixture._resourceWatcher).onChanged(eq(NODE.emptyData()));
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    Assert.assertEquals(actualData.getNodeData(), null);

    fixture._nodeResourceSubscriber.setData(NODE_UPDATE1);
    fixture._xdsClientImpl.handleD2NodeResponse(badData);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(NODE);
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE1));
    actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    // bad date will not overwrite the original valid data
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE1.getNodeData());
  }

  @Test
  public void testHandleD2NodeResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._nodeResourceSubscriber.setData(NODE_UPDATE1);
    fixture._xdsClientImpl.handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL);
    verify(fixture._xdsClientImpl).handleResourceUpdate(eq(new HashMap<>()), eq(NODE));
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(NODE);
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE1));
    verify(fixture._nodeResourceSubscriber).onRemoval();
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    //  removed resource will not overwrite the original valid data
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE1.getNodeData());
  }

  @Test
  public void testHandleD2URIMapResponseWithData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // subscriber original data is null
    fixture._nodeResourceSubscriber.setData(null);
    fixture._xdsClientImpl.handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA1);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI_MAP);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    XdsClient.D2URIMapUpdate actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA1
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
    fixture._xdsClientImpl.handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA2);
    actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA2
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA2));
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA2.getURIMap());
  }

  @Test
  public void testHandleD2URIMapUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._xdsClientImpl.handleD2NodeResponse(DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESPONSE);
    verify(fixture._xdsClientImpl).handleResourceUpdate(eq(new HashMap<>()), eq(D2_URI_MAP));
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
  }

  @DataProvider(name = "badD2URIMapUpdateTestCases")
  public Object[][] provideBadD2URIMapDataTestCases()
  {
    return new Object[][]{{DISCOVERY_RESPONSE_URI_MAP_RESOURCE_IS_NULL},
        {DISCOVERY_RESPONSE_URI_MAP_NULL_DATA_IN_RESOURCE_FILED},};
  }

  @Test(dataProvider = "badD2URIMapUpdateTestCases")
  public void testHandleD2URIMapUpdateWithBadData(DiscoveryResponseData badData)
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._uriMapResourceSubscriber.setData(null);
    fixture._xdsClientImpl.handleD2URIMapResponse(badData);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI_MAP);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP.emptyData()));
    XdsClient.D2URIMapUpdate actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    Assert.assertEquals(actualData.getURIMap(), null);

    fixture._uriMapResourceSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleD2URIMapResponse(badData);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI_MAP);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    // bad date will not overwrite the original valid data
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
  }

  @Test
  public void testHandleD2URIMapResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._uriMapResourceSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL);
    verify(fixture._xdsClientImpl).handleResourceUpdate(eq(new HashMap<>()), eq(D2_URI_MAP));
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI_MAP);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._uriMapResourceSubscriber).onRemoval();
    XdsClient.D2URIMapUpdate actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    // removed resource will not overwrite the original valid data
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
  }

//  @Test
//  public void testHandleD2URICollectionUpdateWithEmptyResponse()
//  {
//    XdsClientImplFixture fixture = new XdsClientImplFixture();
//    fixture._xdsClientImpl.handleD2NodeResponse(new DiscoveryResponseData(D2_URI, null, null, NONCE, null));
//    verify(fixture._xdsClientImpl).handleResourceUpdate(eq(new HashMap<>()), eq(D2_URI));
//    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
//  }
//
//  @DataProvider(name = "badD2URICollectionUpdateTestCases")
//  public Object[][] provideBadD2URICollectionDataTestCases()
//  {
//    Resource nullResources = Resource.newBuilder().setVersion(VERSION1).setName(URI_URN1)
//        // do not set resource field
//        .build();
//    return new Object[][]{
//        {new DiscoveryResponseData(D2_URI, Collections.singletonList(nullResources), null, NONCE, null)},
//    };
//  }
//
//  @Test(dataProvider = "badD2URICollectionUpdateTestCases")
//  public void testHandleD2URICollectionUpdateWithBadData(DiscoveryResponseData badData)
//  {
//    XdsClientImplFixture fixture = new XdsClientImplFixture();
//    fixture._uriMapResourceSubscriber.setData(null);
//    fixture._xdsClientImpl.handleD2URICollectionResponse(badData);
//    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
//    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
//    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI);
//    verify(fixture._uriCollectionResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher),
//        eq(new D2URICollectionUpdate()));
//    D2URICollectionUpdate actualData = (D2URICollectionUpdate) fixture._uriCollectionResourceSubscriber.getData();
//    Assert.assertEquals(actualData.getUris(), Collections.emptyMap());
//    Assert.assertEquals(actualData.getRemovedUris(), Collections.emptyList());
//
//    fixture._uriCollectionResourceSubscriber.setData(D2_URI_COLLECTION_UPDATE_WITH_DATA1);
//    fixture._xdsClientImpl.handleD2URICollectionResponse(badData);
//    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
//    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
//    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI);
//    verify(fixture._uriCollectionResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher),
//        eq(D2_URI_COLLECTION_UPDATE_WITH_DATA1));
//    actualData = (D2URICollectionUpdate) fixture._uriCollectionResourceSubscriber.getData();
//    // bad data will not overwrite the original valid data
//    Assert.assertEquals(actualData.getUris(), D2_URI_COLLECTION_UPDATE_WITH_DATA1.getUris());
//  }
//
//  @Test
//  public void testHandleD2URICollectionResponseWithRemoval()
//  {
//    XdsClientImplFixture fixture = new XdsClientImplFixture();
//    fixture._uriMapResourceSubscriber.setData(D2_URI_COLLECTION_UPDATE_WITH_DATA1);
//    fixture._xdsClientImpl.handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL);
//    verify(fixture._xdsClientImpl).handleResourceUpdate(eq(new HashMap<>()), eq(D2_URI_MAP));
//    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
//    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI_MAP);
//    verify(fixture._uriMapResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher), eq(D2_URI_MAP_UPDATE_WITH_DATA1));
//    verify(fixture._uriMapResourceSubscriber).onRemoval();
//    XdsClient.D2URIMapUpdate actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
//    // removed resource will not overwrite the original valid data
//    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
//  }

  private static class XdsClientImplFixture
  {
    XdsClientImpl _xdsClientImpl;
    @Mock
    XdsClientJmx _xdsClientJmx;
    ResourceSubscriber _nodeResourceSubscriber = spy(new ResourceSubscriber(NODE, RESOURCE_NAME));
    ResourceSubscriber _uriMapResourceSubscriber = spy(new ResourceSubscriber(D2_URI_MAP, RESOURCE_NAME));
    ResourceSubscriber _uriCollectionResourceSubscriber = spy(new ResourceSubscriber(D2_URI, CLUSTER_GLOB_COLLECTION));
    Map<ResourceType, Map<String, ResourceSubscriber>> _subscribers = new HashMap<>();
    @Mock
    XdsClient.ResourceWatcher _resourceWatcher;

    XdsClientImplFixture()
    {
      this(false);
    }

    XdsClientImplFixture(boolean useGlobCollections)
    {
      MockitoAnnotations.initMocks(this);

      doNothing().when(_resourceWatcher).onChanged(any());
      for (ResourceSubscriber subscriber : Lists.newArrayList(
          _nodeResourceSubscriber, _uriMapResourceSubscriber, _uriCollectionResourceSubscriber))
      {
        subscriber.addWatcher(_resourceWatcher);
        _subscribers.put(subscriber.getType(), Collections.singletonMap(subscriber.getResource(), subscriber));
      }

      _xdsClientImpl = spy(new XdsClientImpl(null, null, null, 0, useGlobCollections));
      doNothing().when(_xdsClientImpl).sendAckOrNack(any(), any(), any());
      when(_xdsClientImpl.getXdsClientJmx()).thenReturn(_xdsClientJmx);
      when(_xdsClientImpl.getResourceSubscriberMap(any()))
          .thenAnswer(a -> _subscribers.get((ResourceType) a.getArguments()[0]));
    }
  }
}
