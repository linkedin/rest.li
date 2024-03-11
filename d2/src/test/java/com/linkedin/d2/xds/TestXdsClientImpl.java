package com.linkedin.d2.xds;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import indis.XdsD2;
import com.linkedin.d2.jmx.XdsClientJmx;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import com.linkedin.d2.xds.XdsClientImpl.DiscoveryResponseData;

import static com.linkedin.d2.xds.XdsClient.ResourceType.NODE;
import static com.linkedin.d2.xds.XdsClient.ResourceType.D2_URI_MAP;

import java.util.*;

import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.mockito.MockitoAnnotations;

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
  private static final XdsClient.NodeUpdate NODE_UPDATE1 = new XdsClient.NodeUpdate(VERSION1, NODE_WITH_DATA);
  private static final XdsClient.NodeUpdate NODE_UPDATE2 = new XdsClient.NodeUpdate(VERSION2, NODE_WITH_DATA);
  private static final List<Resource> NODE_RESOURCES_WITH_DATA1 = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION1).setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_DATA).build());
  private static final List<Resource> NODE_RESOURCES_WITH_DATA2 = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION2).setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_DATA).build());

  private static final List<Resource> RESOURCES_WITH_RESOURCE_IS_NULL =
      Collections.singletonList(Resource.newBuilder().setVersion("").setName(RESOURCE_NAME)
          // not set resource field
          .build());
  private static final List<Resource> NODE_RESOURCES_WITH_NULL_RESOURCE_FILED = Collections.singletonList(
      Resource.newBuilder().setVersion("").setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_EMPTY_DATA).build());

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
      new XdsClient.D2URIMapUpdate(VERSION1, D2_URI_MAP_WITH_DATA1.getUrisMap());
  private static final XdsClient.D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_DATA2 =
      new XdsClient.D2URIMapUpdate(VERSION1, D2_URI_MAP_WITH_DATA2.getUrisMap());
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
      new XdsClientImpl.DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_DATA1, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA2 =
      new XdsClientImpl.DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_DATA2, null, NONCE, null);

  // case 1: Resource in ResourceUpdate is null, failed to parse which causes InvalidProtocolBufferException
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL =
      new XdsClientImpl.DiscoveryResponseData(NODE, RESOURCES_WITH_RESOURCE_IS_NULL, null, NONCE, null);

  // case 2: Resource field in Resource is null
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FILED =
      new XdsClientImpl.DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_NULL_RESOURCE_FILED, null, NONCE, null);

  // case3 : ResourceList is empty
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_WITH_EMPTY_NODE_RESOURCES =
      new XdsClientImpl.DiscoveryResponseData(NODE, Collections.emptyList(), null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA1 =
      new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_DATA1, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA2 =
      new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_DATA2, null, NONCE, null);

  // case1: Resource in ResourceUpdate is null, failed to parse response.resource
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_RESOURCE_IS_NULL =
      new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, RESOURCES_WITH_RESOURCE_IS_NULL, null, NONCE, null);

  // case2 : Resource field in Resource is null
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_NULL_DATA_IN_RESOURCE_FILED =
      new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_NULL_RESOURCE_FILED, null, NONCE, null);

  // case3 : ResourceList is empty
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESOURCES =
      new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, Collections.emptyList(), null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL =
      new XdsClientImpl.DiscoveryResponseData(NODE, Collections.emptyList(), REMOVED_RESOURCE, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL =
      new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, Collections.emptyList(), REMOVED_RESOURCE, NONCE, null);

  @Test
  public void testHandleD2NodeResponseWithData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // subscriber original data is null
    fixture._nodeResourceSubscriber.setData(null);
    fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(NODE);
    verify(fixture._nodeResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher), eq(NODE_UPDATE1));
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    // subscriber data should be updated to NODE_UPDATE1
    Assert.assertEquals(actualData.getVersion(), NODE_UPDATE1.getVersion());
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE1.getNodeData());

    // subscriber data should be updated to NODE_UPDATE2
    fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA2);
    actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    verify(fixture._nodeResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher), eq(NODE_UPDATE2));
    Assert.assertEquals(actualData.getVersion(), NODE_UPDATE2.getVersion());
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE2.getNodeData());
  }

  @Test
  public void testHandleD2URIMapResponseWithData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    // subscriber original data is null
    fixture._nodeResourceSubscriber.setData(null);
    fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA1);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI_MAP);
    verify(fixture._uriMapResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher),
        eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    XdsClient.D2URIMapUpdate actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA1
    Assert.assertEquals(actualData.getVersion(), D2_URI_MAP_UPDATE_WITH_DATA1.getVersion());
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
    fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA2);
    actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA2
    verify(fixture._uriMapResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher),
        eq(D2_URI_MAP_UPDATE_WITH_DATA2));
    Assert.assertEquals(actualData.getVersion(), D2_URI_MAP_UPDATE_WITH_DATA2.getVersion());
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA2.getURIMap());
  }

  @Test
  public void testHandleD2NodeUpdateWithEmptyData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_WITH_EMPTY_NODE_RESOURCES);
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
    fixture._nodeResourceSubscriber.setData(NODE_UPDATE1);
    fixture.getSpiedXdsClientImpl().handleD2NodeResponse(badData);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(NODE);
    verify(fixture._nodeResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher), eq(NODE_UPDATE1));
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    // bad date will not overwrite the original valid data
    Assert.assertEquals(actualData.getVersion(), NODE_UPDATE1.getVersion());
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE1.getNodeData());
  }

  @Test
  public void testHandleD2URIMapUpdateWithEmptyData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESOURCES);
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
    fixture._uriMapResourceSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(badData);
    verify(fixture._xdsClientImpl).handleResourceUpdate(any(), any());
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI_MAP);
    verify(fixture._uriMapResourceSubscriber).notifyWatcher(eq(fixture._resourceWatcher),
        eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    XdsClient.D2URIMapUpdate actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    // bad date will not overwrite the original valid data
    Assert.assertEquals(actualData.getVersion(), D2_URI_MAP_UPDATE_WITH_DATA1.getVersion());
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
  }

  @Test
  public void testHandleD2NodeResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._nodeResourceSubscriber.setData(NODE_UPDATE1);
    fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL);
    verify(fixture._xdsClientImpl).handleResourceUpdate(new HashMap<>(), NODE);
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(NODE);
    verify(fixture._nodeResourceSubscriber).notifyWatcher(any(), any());
    XdsClient.NodeUpdate actualData = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
    //  removed resource will not overwrite the original valid data
    Assert.assertEquals(actualData.getVersion(), NODE_UPDATE1.getVersion());
    Assert.assertEquals(actualData.getNodeData(), NODE_UPDATE1.getNodeData());
  }

  @Test
  public void testHandleD2URIMapResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._uriMapResourceSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL);
    verify(fixture._xdsClientImpl).handleResourceUpdate(new HashMap<>(), D2_URI_MAP);
    verify(fixture._xdsClientImpl).sendAckOrNack(any(), any(), any());
    verify(fixture._xdsClientImpl).getResourceSubscriberMap(D2_URI_MAP);
    verify(fixture._uriMapResourceSubscriber).notifyWatcher(any(), any());
    verify(fixture._uriMapResourceSubscriber).onRemoval();
    XdsClient.D2URIMapUpdate actualData = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
    // removed resource will not overwrite the original valid data
    Assert.assertEquals(actualData.getVersion(), D2_URI_MAP_UPDATE_WITH_DATA1.getVersion());
    Assert.assertEquals(actualData.getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
  }

  private static class XdsClientImplFixture
  {
    XdsClientImpl _xdsClientImpl;
    @Mock
    XdsClientJmx _xdsClientJmx;
    XdsClientImpl.ResourceSubscriber _nodeResourceSubscriber;
    XdsClientImpl.ResourceSubscriber _uriMapResourceSubscriber;
    Map<String, XdsClientImpl.ResourceSubscriber> _d2NodeSubscribers = new HashMap<>();
    Map<String, XdsClientImpl.ResourceSubscriber> _d2URIMapSubscribers = new HashMap<>();
    @Mock
    XdsClient.ResourceWatcher _resourceWatcher;

    XdsClientImplFixture()
    {
      MockitoAnnotations.initMocks(this);
      _nodeResourceSubscriber = spy(new XdsClientImpl.ResourceSubscriber(NODE, RESOURCE_NAME));
      _nodeResourceSubscriber.addWatcher(_resourceWatcher);
      _uriMapResourceSubscriber = spy(new XdsClientImpl.ResourceSubscriber(D2_URI_MAP, RESOURCE_NAME));
      _uriMapResourceSubscriber.addWatcher(_resourceWatcher);
      doNothing().when(_nodeResourceSubscriber).notifyWatcher(any(), any());
      doNothing().when(_uriMapResourceSubscriber).notifyWatcher(any(), any());
      _d2NodeSubscribers.put(RESOURCE_NAME, _nodeResourceSubscriber);
      _d2URIMapSubscribers.put(RESOURCE_NAME, _uriMapResourceSubscriber);
    }

    XdsClientImpl getSpiedXdsClientImpl()
    {
      _xdsClientImpl = spy(new XdsClientImpl(null, null, null, 0));
      doNothing().when(_xdsClientImpl).sendAckOrNack(any(), any(), any());
      when(_xdsClientImpl.getXdsClientJmx()).thenReturn(_xdsClientJmx);
      when(_xdsClientImpl.getResourceSubscriberMap(NODE)).thenReturn(_d2NodeSubscribers);
      when(_xdsClientImpl.getResourceSubscriberMap(D2_URI_MAP)).thenReturn(_d2URIMapSubscribers);
      return _xdsClientImpl;
    }
  }
}
