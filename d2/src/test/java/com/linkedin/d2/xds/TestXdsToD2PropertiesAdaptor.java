/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.xds;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ClusterStoreProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ServiceStoreProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter;
import indis.XdsD2;
import java.net.URI;
import java.util.Collections;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


public class TestXdsToD2PropertiesAdaptor {
  private static final String CLUSTER_NODE_PREFIX = "/d2/clusters/";
  private static final String URI_NODE_PREFIX = "/d2/uris/";
  private static final String SYMLINK_NAME = "$FooClusterMaster";
  private static final String PRIMARY_CLUSTER_NAME = "FooClusterMaster-prod-ltx1";
  private static final String PRIMARY_CLUSTER_NAME_2 = "FooClusterMaster-prod-lor1";
  private static final String CLUSTER_SYMLINK_RESOURCE_NAME = CLUSTER_NODE_PREFIX + SYMLINK_NAME;
  private static final String PRIMARY_CLUSTER_RESOURCE_NAME = CLUSTER_NODE_PREFIX + PRIMARY_CLUSTER_NAME;
  private static final ClusterStoreProperties PRIMARY_CLUSTER_PROPERTIES = new ClusterStoreProperties(PRIMARY_CLUSTER_NAME);
  private static final String URI_SYMLINK_RESOURCE_NAME = URI_NODE_PREFIX + SYMLINK_NAME;
  private static final String PRIMARY_URI_RESOURCE_NAME = URI_NODE_PREFIX + PRIMARY_CLUSTER_NAME;
  private static final long VERSION = 123;
  private static final String LOCAL_HOST = "localhost";
  private static final int PORT = 8443;
  private static final URI LOCAL_HOST_URI = URI.create("https://" + LOCAL_HOST + ":" + PORT);
  private static final String TRACING_ID = "5678";
  private static final String XDS_SERVER = "dummy-observer-host";
  private static final String URI_NAME = "ltx1-dummyhost456";

  private final UriPropertiesJsonSerializer uriSerializer = new UriPropertiesJsonSerializer();

  @Test
  public void testListenToService()
  {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    String serviceName = "FooService";
    fixture.getSpiedAdaptor().listenToService(serviceName);

    verify(fixture._xdsClient).watchXdsResource(eq("/d2/services/" + serviceName), eq(XdsClient.ResourceType.NODE), any());

    XdsClient.NodeResourceWatcher symlinkNodeWatcher =
        (XdsClient.NodeResourceWatcher) fixture._clusterWatcherArgumentCaptor.getValue();
    symlinkNodeWatcher.onChanged(new XdsClient.NodeUpdate("", XdsD2.Node.newBuilder()
        .setData(
            ByteString.copyFrom(
                new ServicePropertiesJsonSerializer().toBytes(
                    new ServiceProperties(
                        serviceName,
                        PRIMARY_CLUSTER_NAME,
                        "",
                        Collections.singletonList("relative")
                    )
                )
            )
        )
        .setStat(XdsD2.Stat.newBuilder().setMzxid(1L).build())
        .build())
    );
    verify(fixture._serviceEventBus).publishInitialize(serviceName,
        new ServiceStoreProperties(serviceName, PRIMARY_CLUSTER_NAME, "",
            Collections.singletonList("relative"))
    );
  }

  @Test
  public void testListenToNormalCluster()
  {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    fixture.getSpiedAdaptor().listenToCluster(PRIMARY_CLUSTER_NAME);

    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_CLUSTER_RESOURCE_NAME), eq(XdsClient.ResourceType.NODE), any());
    verifyClusterNodeUpdate(fixture, PRIMARY_CLUSTER_NAME, null, PRIMARY_CLUSTER_PROPERTIES);
  }

  @Test
  public void testListenToClusterSymlink()
  {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    fixture.getSpiedAdaptor().listenToCluster(SYMLINK_NAME);

    // verify symlink is watched
    verify(fixture._xdsClient).watchXdsResource(eq(CLUSTER_SYMLINK_RESOURCE_NAME), eq(XdsClient.ResourceType.NODE), any());

    // update symlink data
    XdsClient.SymlinkNodeResourceWatcher symlinkNodeWatcher =
        (XdsClient.SymlinkNodeResourceWatcher) fixture._clusterWatcherArgumentCaptor.getValue();
    symlinkNodeWatcher.onChanged(CLUSTER_SYMLINK_RESOURCE_NAME, getSymlinkNodeUpdate(PRIMARY_CLUSTER_RESOURCE_NAME));

    // verify both cluster and uri data of the actual cluster is watched
    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_CLUSTER_RESOURCE_NAME), eq(XdsClient.ResourceType.NODE), any());
    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_URI_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_URI_MAP), any());

    // update cluster data
    XdsClient.NodeResourceWatcher clusterNodeWatcher =
        (XdsClient.NodeResourceWatcher) fixture._clusterWatcherArgumentCaptor.getValue();
    clusterNodeWatcher.onChanged(getClusterNodeUpdate(PRIMARY_CLUSTER_NAME));

    // verify cluster data is published under symlink name and actual cluster name
    verify(fixture._clusterEventBus).publishInitialize(SYMLINK_NAME, PRIMARY_CLUSTER_PROPERTIES);
    verify(fixture._clusterEventBus).publishInitialize(PRIMARY_CLUSTER_NAME, PRIMARY_CLUSTER_PROPERTIES);

    // test update symlink to a new primary cluster
    String primaryClusterResourceName2 = CLUSTER_NODE_PREFIX + PRIMARY_CLUSTER_NAME_2;
    ClusterStoreProperties primaryClusterProperties2 = new ClusterStoreProperties(PRIMARY_CLUSTER_NAME_2);

    symlinkNodeWatcher.onChanged(CLUSTER_SYMLINK_RESOURCE_NAME, getSymlinkNodeUpdate(primaryClusterResourceName2));

    verify(fixture._xdsClient).watchXdsResource(eq(primaryClusterResourceName2), eq(XdsClient.ResourceType.NODE), any());
    verify(fixture._xdsClient).watchXdsResource(eq(URI_NODE_PREFIX + PRIMARY_CLUSTER_NAME_2),
        eq(XdsClient.ResourceType.D2_URI_MAP), any());
    verifyClusterNodeUpdate(fixture, PRIMARY_CLUSTER_NAME_2, SYMLINK_NAME, primaryClusterProperties2);

    // if the old primary cluster gets an update, it will be published under its original cluster name
    // since the symlink points to the new primary cluster now.
    clusterNodeWatcher.onChanged(getClusterNodeUpdate(PRIMARY_CLUSTER_NAME_2));

    verify(fixture._clusterEventBus).publishInitialize(PRIMARY_CLUSTER_NAME, primaryClusterProperties2);
    // verify symlink is published just once
    verify(fixture._clusterEventBus).publishInitialize(SYMLINK_NAME, primaryClusterProperties2);
  }

  @Test
  public void testListenToNormalUri() throws PropertySerializationException
  {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    fixture.getSpiedAdaptor().listenToUris(PRIMARY_CLUSTER_NAME);

    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_URI_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_URI_MAP), any());
    verifyUriUpdate(fixture, PRIMARY_CLUSTER_NAME, null);
  }

  @Test
  public void testListenToUriSymlink() throws PropertySerializationException
  {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    fixture.getSpiedAdaptor().listenToUris(SYMLINK_NAME);

    // verify symlink is watched
    verify(fixture._xdsClient).watchXdsResource(eq(URI_SYMLINK_RESOURCE_NAME), eq(XdsClient.ResourceType.NODE), any());

    // update symlink data
    XdsClient.SymlinkNodeResourceWatcher symlinkNodeWatcher =
        (XdsClient.SymlinkNodeResourceWatcher) fixture._clusterWatcherArgumentCaptor.getValue();
    symlinkNodeWatcher.onChanged(URI_SYMLINK_RESOURCE_NAME, getSymlinkNodeUpdate(PRIMARY_URI_RESOURCE_NAME));

    // verify actual cluster of the uris is watched
    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_URI_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_URI_MAP), any());

    // update uri data
    XdsClient.D2URIMapResourceWatcher watcher =
        (XdsClient.D2URIMapResourceWatcher) fixture._uriWatcherArgumentCaptor.getValue();
    watcher.onChanged(new XdsClient.D2URIMapUpdate("", Collections.emptyMap()));

    // verify uri data is merged and published under symlink name and the actual cluster name
    verify(fixture._uriEventBus).publishInitialize(SYMLINK_NAME, getDefaultUriProperties(SYMLINK_NAME));
    verify(fixture._uriEventBus).publishInitialize(PRIMARY_CLUSTER_NAME, getDefaultUriProperties(PRIMARY_CLUSTER_NAME));

    // test update symlink to a new primary cluster
    String primaryUriResourceName2 = URI_NODE_PREFIX + PRIMARY_CLUSTER_NAME_2;
    symlinkNodeWatcher.onChanged(URI_SYMLINK_RESOURCE_NAME, getSymlinkNodeUpdate(primaryUriResourceName2));

    verify(fixture._xdsClient).watchXdsResource(eq(primaryUriResourceName2), eq(XdsClient.ResourceType.D2_URI_MAP), any());
    verifyUriUpdate(fixture, PRIMARY_CLUSTER_NAME_2, SYMLINK_NAME);

    // if the old primary cluster gets an update, it will be published under its original cluster name
    // since the symlink points to the new primary cluster now.

    XdsD2.D2URI protoUri = getD2URI(PRIMARY_CLUSTER_NAME);
    UriProperties uriProps = new UriPropertiesJsonSerializer().fromProto(protoUri);

    watcher.onChanged(new XdsClient.D2URIMapUpdate("", Collections.singletonMap(URI_NAME, protoUri)));

    verify(fixture._uriEventBus).publishInitialize(PRIMARY_CLUSTER_NAME, uriProps);
    verify(fixture._eventEmitter).emitSDStatusUpdateReceiptEvent(
        eq(PRIMARY_CLUSTER_NAME),
        eq(LOCAL_HOST),
        eq(PORT),
        eq(ServiceDiscoveryEventEmitter.StatusUpdateActionType.MARK_READY),
        eq(true),
        eq(XDS_SERVER),
        eq(URI_NODE_PREFIX + PRIMARY_CLUSTER_NAME + "/" + URI_NAME),
        eq(uriProps.toString()),
        eq((int) VERSION),
        eq(TRACING_ID),
        anyLong()
    );
  }

  @Test
  public void testURIPropertiesDeserialization() throws PropertySerializationException
  {
    UriProperties properties = new UriPropertiesJsonSerializer().fromProto(getD2URI(PRIMARY_CLUSTER_NAME));
    Assert.assertEquals(properties.getClusterName(), PRIMARY_CLUSTER_NAME);
    Assert.assertEquals(properties.getVersion(), VERSION);
    Assert.assertEquals(properties.getUriSpecificProperties(),
        Collections.singletonMap(LOCAL_HOST_URI, Collections.singletonMap("foo", "bar")));
    Assert.assertEquals(properties.getPartitionDesc(),
        Collections.singletonMap(LOCAL_HOST_URI, ImmutableMap.of(
            0, new PartitionData(42),
            1, new PartitionData(27)
        )));
  }

  private XdsD2.D2URI getD2URI(String clusterName)
  {
    return XdsD2.D2URI.newBuilder()
        .setVersion(VERSION)
        .setUri(LOCAL_HOST_URI.toString())
        .setClusterName(clusterName)
        .setUriSpecificProperties(Struct.newBuilder()
            .putFields("foo", Value.newBuilder().setStringValue("bar").build())
            .build())
        .putPartitionDesc(0, 42)
        .putPartitionDesc(1, 27)
        .setTracingId(TRACING_ID)
        .build();
  }

  private static XdsClient.NodeUpdate getSymlinkNodeUpdate(String primaryClusterResourceName)
  {
    return new XdsClient.NodeUpdate("",
        XdsD2.Node.newBuilder()
            .setData(ByteString.copyFromUtf8(primaryClusterResourceName))
            .build()
    );
  }

  private static XdsClient.NodeUpdate getClusterNodeUpdate(String clusterName)
  {
    return new XdsClient.NodeUpdate("", XdsD2.Node.newBuilder()
        .setData(
            ByteString.copyFrom(
                new ClusterPropertiesJsonSerializer().toBytes(
                    new ClusterProperties(clusterName)
                )
            )
        )
        .setStat(XdsD2.Stat.newBuilder().setMzxid(1L).build())
        .build()
    );
  }

  private void verifyClusterNodeUpdate(XdsToD2PropertiesAdaptorFixture fixture, String clusterName, String symlinkName,
      ClusterStoreProperties expectedPublishProp)
  {
    XdsClient.NodeResourceWatcher watcher = (XdsClient.NodeResourceWatcher)
        fixture._clusterWatcherArgumentCaptor.getValue();
    watcher.onChanged(getClusterNodeUpdate(clusterName));
    verify(fixture._clusterEventBus).publishInitialize(clusterName, expectedPublishProp);
    if (symlinkName != null)
    {
      verify(fixture._clusterEventBus).publishInitialize(symlinkName, expectedPublishProp);
    }
  }

  private void verifyUriUpdate(XdsToD2PropertiesAdaptorFixture fixture, String clusterName, String symlinkName)
      throws PropertySerializationException
  {
    XdsClient.D2URIMapResourceWatcher watcher = (XdsClient.D2URIMapResourceWatcher)
        fixture._uriWatcherArgumentCaptor.getValue();
    XdsD2.D2URI protoUri = getD2URI(clusterName);
    watcher.onChanged(new XdsClient.D2URIMapUpdate("", Collections.singletonMap(URI_NAME, protoUri)));
    verify(fixture._uriEventBus).publishInitialize(clusterName, uriSerializer.fromProto(protoUri));
    if (symlinkName != null)
    {
      verify(fixture._uriEventBus).publishInitialize(symlinkName,
          uriSerializer.fromProto(getD2URI(symlinkName)));
    }
  }

  private UriProperties getDefaultUriProperties(String clusterName)
  {
    return new UriProperties(clusterName, Collections.emptyMap(), Collections.emptyMap(), -1);
  }

  private static class XdsToD2PropertiesAdaptorFixture
  {
    @Mock
    XdsClient _xdsClient;
    @Mock
    ServiceDiscoveryEventEmitter _eventEmitter;
    @Mock
    PropertyEventBus<ClusterProperties> _clusterEventBus;
    @Mock
    PropertyEventBus<ServiceProperties> _serviceEventBus;
    @Mock
    PropertyEventBus<UriProperties> _uriEventBus;
    @Captor
    ArgumentCaptor<XdsClient.ResourceWatcher> _clusterWatcherArgumentCaptor;
    @Captor
    ArgumentCaptor<XdsClient.ResourceWatcher> _uriWatcherArgumentCaptor;

    XdsToD2PropertiesAdaptor _adaptor;

    XdsToD2PropertiesAdaptorFixture()
    {
      MockitoAnnotations.initMocks(this);
      doNothing().when(_xdsClient).watchXdsResource(any(), eq(XdsClient.ResourceType.NODE),
          _clusterWatcherArgumentCaptor.capture());
      doNothing().when(_xdsClient).watchXdsResource(any(), eq(XdsClient.ResourceType.D2_URI_MAP),
          _uriWatcherArgumentCaptor.capture());
      doNothing().when(_clusterEventBus).publishInitialize(any(), any());
      doNothing().when(_serviceEventBus).publishInitialize(any(), any());
      doNothing().when(_uriEventBus).publishInitialize(any(), any());
      when(_xdsClient.getXdsServerAuthority()).thenReturn(XDS_SERVER);
      doNothing().when(_eventEmitter)
          .emitSDStatusUpdateReceiptEvent(
              any(), any(), anyInt(), any(), anyBoolean(), any(), any(), any(), any(), any(), anyLong());
    }

    XdsToD2PropertiesAdaptor getSpiedAdaptor() {
      _adaptor = spy(new XdsToD2PropertiesAdaptor(_xdsClient, null, _eventEmitter));
      _adaptor.setClusterEventBus(_clusterEventBus);
      _adaptor.setServiceEventBus(_serviceEventBus);
      _adaptor.setUriEventBus(_uriEventBus);
      return _adaptor;
    }
  }
}
