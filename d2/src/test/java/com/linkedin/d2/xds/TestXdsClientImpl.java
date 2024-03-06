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
import org.testng.annotations.Test;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.*;

public class TestXdsClientImpl
{
    private static final byte[] DATA = "data".getBytes();
    private static String RESOURCE_NAME = "FooService";
    private static String CLUSTER_NAME = "FooClusterMaster-prod-ltx1";
    private static String URI1 = "TestURI";
    private static String URI2 = "TestURI2";
    private static String VERSION1 = "1";
    private static String VERSION2 = "2";
    private static String NONCE = "nonce";
    private static final XdsD2.Node.Builder NODE_BUILDER = XdsD2.Node.newBuilder();
    private static XdsD2.Node NODE_WITH_DATA = NODE_BUILDER.setData(ByteString.copyFrom(DATA)).build();
    private final XdsD2.Node NODE_WITH_EMPTY_DATA = NODE_BUILDER.build();
    private final Any PACKED_NODE_WITH_DATA = Any.pack(NODE_WITH_DATA);
    private final Any PACKED_NODE_WITH_EMPTY_DATA = Any.pack(NODE_WITH_EMPTY_DATA);
    private static final XdsClient.NodeUpdate NODE_UPDATE1 = new XdsClient.NodeUpdate(VERSION1, NODE_WITH_DATA);
    private static final XdsClient.NodeUpdate NODE_UPDATE2 = new XdsClient.NodeUpdate(VERSION2, NODE_WITH_DATA);
    private final List<Resource> NODE_RESOURCES_WITH_DATA1 =
            Arrays.asList(Resource.newBuilder().setVersion(VERSION1).setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_DATA).build());
    private final List<Resource> NODE_RESOURCES_WITH_DATA2 =
            Arrays.asList(Resource.newBuilder().setVersion(VERSION2).setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_DATA).build());
    private final List<Resource> NODE_RESOURCES_WITH_EMPTY_DATA =
            Arrays.asList(Resource.newBuilder().setVersion(VERSION1).setName(RESOURCE_NAME).setResource(PACKED_NODE_WITH_EMPTY_DATA).build());

    XdsD2.D2URI.Builder URI_BUILDER1 =
            XdsD2.D2URI.newBuilder().setVersion(Long.parseLong(VERSION1)).setClusterName(CLUSTER_NAME).setUri(URI1);
    XdsD2.D2URI.Builder URI_BUILDER2 =
            XdsD2.D2URI.newBuilder().setVersion(Long.parseLong(VERSION1)).setClusterName(CLUSTER_NAME).setUri(URI1);
    private final XdsD2.D2URIMap.Builder D2_URI_MAP_BUILDER = XdsD2.D2URIMap.newBuilder();
    private final XdsD2.D2URIMap D2_URI_MAP_WITH_EMPTY_DATA = D2_URI_MAP_BUILDER.build();
    private final XdsD2.D2URIMap D2_URI_MAP_WITH_DATA1 = D2_URI_MAP_BUILDER.putUris(RESOURCE_NAME,
            URI_BUILDER1.build()).build();
    private final XdsD2.D2URIMap D2_URI_MAP_WITH_DATA2 = D2_URI_MAP_BUILDER.putUris(RESOURCE_NAME,
            URI_BUILDER2.build()).build();
    private final XdsClient.D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_DATA1 = new XdsClient.D2URIMapUpdate(VERSION1,
            D2_URI_MAP_WITH_DATA1.getUrisMap());
    private final XdsClient.D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_DATA2 = new XdsClient.D2URIMapUpdate(VERSION1,
            D2_URI_MAP_WITH_DATA2.getUrisMap());
    private final Any PACKED_D2_URI_MAP_WITH_DATA1 = Any.pack(D2_URI_MAP_WITH_DATA1);
    private final Any PACKED_D2_URI_MAP_WITH_DATA2 = Any.pack(D2_URI_MAP_WITH_DATA2);
    private final Any PACKED_D2_URI_MAP_WITH_EMPTY_DATA = Any.pack(D2_URI_MAP_WITH_EMPTY_DATA);
    private final List<Resource> URI_MAP_RESOURCE_WITH_DATA1 =
            Arrays.asList(Resource.newBuilder().setVersion(VERSION1).setName(RESOURCE_NAME).setResource(PACKED_D2_URI_MAP_WITH_DATA1).build());
    private final List<Resource> URI_MAP_RESOURCE_WITH_DATA2 =
            Arrays.asList(Resource.newBuilder().setVersion(VERSION1).setName(RESOURCE_NAME).setResource(PACKED_D2_URI_MAP_WITH_DATA2).build());
    private final List<Resource> URI_MAP_RESOURCE_WITH_EMPTY_DATA =
            Arrays.asList(Resource.newBuilder().setVersion(VERSION2).setName(RESOURCE_NAME).setResource(PACKED_D2_URI_MAP_WITH_EMPTY_DATA).build());
    private final List<String> REMOVED_RESOURCE = Arrays.asList(RESOURCE_NAME);
    private final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA1 = new XdsClientImpl.DiscoveryResponseData(NODE,
            NODE_RESOURCES_WITH_DATA1, null, NONCE, null);
    private final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA2 = new XdsClientImpl.DiscoveryResponseData(NODE,
            NODE_RESOURCES_WITH_DATA2, null, NONCE, null);
    private final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_EMPTY_DATA =
            new XdsClientImpl.DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_EMPTY_DATA, null, NONCE, null);
    private final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA1 =
            new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_DATA1, null, NONCE, null);
    private final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA2 =
            new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_DATA2, null, NONCE, null);
    private final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_EMPTY_DATA =
            new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_EMPTY_DATA, null, NONCE, null);
    private final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL =
            new XdsClientImpl.DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_EMPTY_DATA, REMOVED_RESOURCE, NONCE,
                    null);
    private final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL =
            new XdsClientImpl.DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_EMPTY_DATA, REMOVED_RESOURCE,
                    NONCE, null);

    @Test
    public void testHandleD2NodeResponseWithData()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        // subscriber original data is null
        fixture._nodeResourceSubscriber.setData(null);
        fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA1);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        verify(fixture._xdsClientImpl, times(1)).getResourceSubscriberMap(NODE);
        verify(fixture._nodeResourceSubscriber, times(1)).notifyWatcher(eq(fixture._resourceWatcher), any());
        XdsClient.NodeUpdate _data = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
        // subscriber data should be updated to NODE_UPDATE1
        assertEquals(NODE_UPDATE1.getVersion(), _data.getVersion());
        assertEquals(NODE_UPDATE1.getNodeData(), _data.getNodeData());

        // subscriber data should be updated to NODE_UPDATE2
        fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA2);
        _data = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
        assertEquals(NODE_UPDATE2.getVersion(), _data.getVersion());
        assertEquals(NODE_UPDATE2.getNodeData(), _data.getNodeData());
    }

    @Test
    public void testHandleD2URIMapResponseWithData()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        // subscriber original data is null
        fixture._nodeResourceSubscriber.setData(null);
        fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA1);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        verify(fixture._xdsClientImpl, times(1)).getResourceSubscriberMap(D2_URI_MAP);
        verify(fixture._uriMapResourceSubscriber, times(1)).notifyWatcher(any(), any());
        XdsClient.D2URIMapUpdate _data = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
        // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA1
        assertEquals(D2_URI_MAP_UPDATE_WITH_DATA1.getVersion(), _data.getVersion());
        assertEquals(D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap(), _data.getURIMap());
        fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA2);
        _data = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
        // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA2
        assertEquals(D2_URI_MAP_UPDATE_WITH_DATA2.getVersion(), _data.getVersion());
        assertEquals(D2_URI_MAP_UPDATE_WITH_DATA2.getURIMap(), _data.getURIMap());
    }

    @Test
    public void testHandleD2NodeResponseWithEmptyData()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        fixture._nodeResourceSubscriber.setData(NODE_UPDATE1);
        fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_EMPTY_DATA);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        verify(fixture._xdsClientImpl, times(1)).getResourceSubscriberMap(NODE);
        verify(fixture._nodeResourceSubscriber, times(1)).notifyWatcher(any(), any());
        XdsClient.NodeUpdate _data = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
        // empty date will not overwrite the original data
        assertEquals(NODE_UPDATE1.getVersion(), _data.getVersion());
        assertEquals(NODE_UPDATE1.getNodeData(), _data.getNodeData());
    }

    @Test
    public void testHandleD2URIMapResponseWithEmptyData()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        fixture._uriMapResourceSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
        fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_EMPTY_DATA);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        verify(fixture._xdsClientImpl, times(1)).getResourceSubscriberMap(D2_URI_MAP);
        verify(fixture._uriMapResourceSubscriber, times(1)).notifyWatcher(any(), any());
        XdsClient.D2URIMapUpdate _data = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
        // empty date will not overwrite the original data
        assertEquals(D2_URI_MAP_UPDATE_WITH_DATA1.getVersion(), _data.getVersion());
        assertEquals(D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap(), _data.getURIMap());
    }

    @Test
    public void testHandleD2NodeResponseWithRemoval()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        fixture._nodeResourceSubscriber.setData(NODE_UPDATE1);
        fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        verify(fixture._xdsClientImpl, times(2)).getResourceSubscriberMap(NODE);
        verify(fixture._nodeResourceSubscriber, times(2)).notifyWatcher(any(), any());
        XdsClient.NodeUpdate _data = (XdsClient.NodeUpdate) fixture._nodeResourceSubscriber.getData();
        //  removed resource will not overwrite the original data
        assertEquals(NODE_UPDATE1.getVersion(), _data.getVersion());
        assertEquals(NODE_UPDATE1.getNodeData(), _data.getNodeData());
    }

    @Test
    public void testHandleD2URIMapResponseWithRemoval()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        fixture._uriMapResourceSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
        fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        verify(fixture._xdsClientImpl, times(2)).getResourceSubscriberMap(D2_URI_MAP);
        verify(fixture._uriMapResourceSubscriber, times(2)).notifyWatcher(any(), any());
        XdsClient.D2URIMapUpdate _data = (XdsClient.D2URIMapUpdate) fixture._uriMapResourceSubscriber.getData();
        // removed resource will not overwrite the original data
        assertEquals(D2_URI_MAP_UPDATE_WITH_DATA1.getVersion(), _data.getVersion());
        assertEquals(D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap(), _data.getURIMap());
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
