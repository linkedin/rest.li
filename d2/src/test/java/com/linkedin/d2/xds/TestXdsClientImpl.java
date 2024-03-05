package com.linkedin.d2.xds;

import com.google.protobuf.Any;
import indis.XdsD2;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.Test;
import com.linkedin.d2.jmx.XdsClientJmx;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import com.linkedin.d2.xds.XdsClientImpl.DiscoveryResponseData;

import java.util.*;

import static com.linkedin.d2.xds.XdsClient.ResourceType.D2_URI_MAP;
import static com.linkedin.d2.xds.XdsClient.ResourceType.NODE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertTrue;

public class TestXdsClientImpl
{
    private final String = "data".getBytes().toString();
    private final XdsD2.Node.Builder NODE_BUILDER = XdsD2.Node.newBuilder();
    private final XdsD2.Node NODE_WITH_DATA = NODE_BUILDER.setData(DATA).build();

    private final XdsD2.Node NODE_WITH_EMPTY_DATA = NODE_BUILDER.build();

    private final Any PACKED_NODE_WITH_EMPTY_DATA = Any.pack(NODE_WITH_EMPTY_DATA);

    private final XdsD2.D2URIMap.Builder D2_URI_MAP_BUILDER = XdsD2.D2URIMap.newBuilder();
    private final XdsD2.D2URIMap D2_URI_MAP_WITH_EMPTY_DATA = D2_URI_MAP_BUILDER.build();
    private final Any PACKED_D2_URI_MAP_WITH_EMPTY_DATA = Any.pack(D2_URI_MAP_WITH_EMPTY_DATA);

    private final List<Resource> NODE_RESOURCES_WITH_EMPTY_DATA = Arrays.asList(
            Resource.newBuilder().setVersion("1").setName("FooService").setResource(PACKED_NODE_WITH_EMPTY_DATA).build()
    );

    private final List<Resource> URI_MAP_RESOURCE = Arrays.asList(
            Resource.newBuilder().setVersion("1").setName("FooService").setResource(PACKED_D2_URI_MAP_WITH_EMPTY_DATA).build(
            ));
    private final List<String> removedResources = Arrays.asList("FooService");

    private final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_NULL_DATA = new XdsClientImpl.DiscoveryResponseData(
            NODE, NODE_RESOURCES_WITH_EMPTY_DATA, null, "nonce", null
    );

    private final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_NULL_DATA = new XdsClientImpl.DiscoveryResponseData(
            D2_URI_MAP, URI_MAP_RESOURCE, null, "nonce", null
    );

    private final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL = new XdsClientImpl.DiscoveryResponseData(
            NODE, NODE_RESOURCES_WITH_EMPTY_DATA, removedResources, "nonce", null
    );

    private final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL = new XdsClientImpl.DiscoveryResponseData(
            D2_URI_MAP, URI_MAP_RESOURCE, removedResources, "nonce", null
    );

    @Test
    public void testHandleD2NodeResponseWithEmptyData()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_NULL_DATA);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        assertTrue(fixture._nodeResourceSubscriber.isEmptyData(any()));
        verify(fixture._xdsClientImpl, times(1)).getResourceSubscriberMap(NODE);
        verify(fixture._nodeResourceSubscriber, times(1)).notifyWatcher(any(), any());
    }

    @Test
    public void testHandleD2URIMapResponseWithEmptyData()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_NULL_DATA);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        assertTrue(fixture._uriMapResourceSubscriber.isEmptyData(any()));
        verify(fixture._xdsClientImpl, times(1)).getResourceSubscriberMap(D2_URI_MAP);
        verify(fixture._uriMapResourceSubscriber, times(1)).notifyWatcher(any(), any());
    }

    @Test
    public void testHandleD2NodeResponseWithRemoval()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        fixture.getSpiedXdsClientImpl().handleD2NodeResponse(DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        verify(fixture._xdsClientImpl, times(2)).getResourceSubscriberMap(NODE);
        assertTrue(fixture._nodeResourceSubscriber.isEmptyData(any()));
        verify(fixture._nodeResourceSubscriber, times(2)).notifyWatcher(any(), any());
    }

    @Test
    public void testHandleD2URIMapResponseWithRemoval()
    {
        XdsClientImplFixture fixture = new XdsClientImplFixture();
        fixture.getSpiedXdsClientImpl().handleD2URIMapResponse(DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL);
        verify(fixture._xdsClientImpl, times(1)).handleResourceUpdate(any(), any());
        verify(fixture._xdsClientImpl, times(1)).sendAckOrNack(any(), any(), any());
        verify(fixture._xdsClientImpl, times(2)).getResourceSubscriberMap(D2_URI_MAP);
        assertTrue(fixture._uriMapResourceSubscriber.isEmptyData(any()));
        verify(fixture._uriMapResourceSubscriber, times(2)).notifyWatcher(any(), any());
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
            _nodeResourceSubscriber = spy(new XdsClientImpl.ResourceSubscriber(NODE, "FooService"));
            _nodeResourceSubscriber.addWatcher(_resourceWatcher);
            _uriMapResourceSubscriber = spy(new XdsClientImpl.ResourceSubscriber(D2_URI_MAP, "FooService"));
            _uriMapResourceSubscriber.addWatcher(_resourceWatcher);
            doNothing().when(_nodeResourceSubscriber).notifyWatcher(any(), any());
            doNothing().when(_uriMapResourceSubscriber).notifyWatcher(any(), any());
            _d2NodeSubscribers.put("FooService", _nodeResourceSubscriber);
            _d2URIMapSubscribers.put("FooService", _uriMapResourceSubscriber);
        }

        XdsClientImpl getSpiedXdsClientImpl()
        {
            _xdsClientImpl = spy(new XdsClientImpl(null, null, null, 0));
            doNothing().when(_xdsClientImpl).sendAckOrNack(any(), any(), any());
            when(_xdsClientImpl.getXdsClientJmx()).thenReturn(_xdsClientJmx);
            when(_xdsClientImpl.getResourceSubscriberMap(NODE)).thenReturn(_d2NodeSubscribers);
            when(_xdsClientImpl.getResourceSubscriberMap(D2_URI_MAP)).thenReturn(_d2URIMapSubscribers);
            when(_xdsClientImpl.getResourceSubscriberMap(XdsClient.ResourceType.D2_URI_MAP)).thenReturn(_d2URIMapSubscribers);
            return _xdsClientImpl;
        }
    }
}
