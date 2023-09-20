package com.linkedin.d2.xds;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterStoreProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServiceStoreProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter;
import indis.XdsD2;
import java.util.Collections;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

  private static final XdsClient.D2NodeMapUpdate DUMMY_NODE_MAP_UPDATE = new XdsClient.D2NodeMapUpdate("",
      Collections.emptyMap());

  @Test
  public void testListenToService()
  {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    String serviceName = "FooService";
    fixture.getSpiedAdaptor().listenToService(serviceName);

    verify(fixture._xdsClient).watchXdsResource(eq("/d2/services/" + serviceName), eq(XdsClient.ResourceType.D2_NODE), any());

    XdsClient.D2NodeResourceWatcher symlinkNodeWatcher =
        (XdsClient.D2NodeResourceWatcher) fixture._watcherArgumentCaptor.getValue();
    symlinkNodeWatcher.onChanged(new XdsClient.D2NodeUpdate("", XdsD2.D2Node.newBuilder()
        .setData(Struct.newBuilder().putAllFields(
            ImmutableMap.of(
                "serviceName", getProtoStringValue(serviceName),
                "clusterName", getProtoStringValue(PRIMARY_CLUSTER_NAME),
                "path", getProtoStringValue(""),
                "loadBalancerStrategyList", Value.newBuilder().setListValue(
                    ListValue.newBuilder().addValues(getProtoStringValue("relative")).build()
                ).build()
            )
        ))
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

    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_CLUSTER_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_NODE), any());
    verifyClusterNodeUpdate(fixture, PRIMARY_CLUSTER_NAME, PRIMARY_CLUSTER_NAME, PRIMARY_CLUSTER_PROPERTIES);
  }

  @Test
  public void testListenToClusterSymlink() {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    fixture.getSpiedAdaptor().listenToCluster(SYMLINK_NAME);

    verify(fixture._xdsClient).watchXdsResource(eq(CLUSTER_SYMLINK_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_SYMLINK_NODE), any());

    XdsClient.D2SymlinkNodeResourceWatcher symlinkNodeWatcher =
        (XdsClient.D2SymlinkNodeResourceWatcher) fixture._watcherArgumentCaptor.getValue();
    symlinkNodeWatcher.onChanged(CLUSTER_SYMLINK_RESOURCE_NAME, getSymlinkNodeUpdate(PRIMARY_CLUSTER_RESOURCE_NAME));

    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_CLUSTER_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_NODE), any());

    XdsClient.D2NodeResourceWatcher clusterNodeWatcher =
        (XdsClient.D2NodeResourceWatcher) fixture._watcherArgumentCaptor.getValue();
    clusterNodeWatcher.onChanged(getClusterNodeUpdate(PRIMARY_CLUSTER_NAME));

    verify(fixture._clusterEventBus).publishInitialize(SYMLINK_NAME, PRIMARY_CLUSTER_PROPERTIES);

    // test update symlink to a new primary cluster
    String primaryClusterResourceName2 = CLUSTER_NODE_PREFIX + PRIMARY_CLUSTER_NAME_2;
    ClusterStoreProperties primaryClusterProperties2 = new ClusterStoreProperties(PRIMARY_CLUSTER_NAME_2);

    symlinkNodeWatcher.onChanged(CLUSTER_SYMLINK_RESOURCE_NAME, getSymlinkNodeUpdate(primaryClusterResourceName2));

    verify(fixture._xdsClient).watchXdsResource(eq(primaryClusterResourceName2), eq(XdsClient.ResourceType.D2_NODE), any());
    verifyClusterNodeUpdate(fixture, PRIMARY_CLUSTER_NAME_2, SYMLINK_NAME, primaryClusterProperties2);

    // if the old primary cluster gets an update, it will be published under its original cluster name
    // since the symlink points to the new primary cluster now.
    clusterNodeWatcher.onChanged(getClusterNodeUpdate(PRIMARY_CLUSTER_NAME_2));

    verify(fixture._clusterEventBus).publishInitialize(PRIMARY_CLUSTER_NAME, primaryClusterProperties2);
  }

  @Test
  public void testListenToNormalUri()
  {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    fixture.getSpiedAdaptor().listenToUris(PRIMARY_CLUSTER_NAME);

    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_URI_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_NODE_MAP), any());
    verifyUriUpdate(fixture, PRIMARY_CLUSTER_NAME, PRIMARY_CLUSTER_NAME);
  }

  @Test
  public void testListenToUriSymlink()
  {
    XdsToD2PropertiesAdaptorFixture fixture = new XdsToD2PropertiesAdaptorFixture();
    fixture.getSpiedAdaptor().listenToUris(SYMLINK_NAME);

    verify(fixture._xdsClient).watchXdsResource(eq(URI_SYMLINK_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_SYMLINK_NODE), any());

    XdsClient.D2SymlinkNodeResourceWatcher symlinkNodeWatcher =
        (XdsClient.D2SymlinkNodeResourceWatcher) fixture._watcherArgumentCaptor.getValue();
    symlinkNodeWatcher.onChanged(URI_SYMLINK_RESOURCE_NAME, getSymlinkNodeUpdate(PRIMARY_URI_RESOURCE_NAME));

    verify(fixture._xdsClient).watchXdsResource(eq(PRIMARY_URI_RESOURCE_NAME), eq(XdsClient.ResourceType.D2_NODE_MAP), any());

    XdsClient.D2NodeMapResourceWatcher watcher =
        (XdsClient.D2NodeMapResourceWatcher) fixture._watcherArgumentCaptor.getValue();
    watcher.onChanged(DUMMY_NODE_MAP_UPDATE);

    verify(fixture._uriEventBus).publishInitialize(SYMLINK_NAME, getDefaultUriProperties(PRIMARY_CLUSTER_NAME));

    // test update symlink to a new primary cluster
    String primaryUriResourceName2 = URI_NODE_PREFIX + PRIMARY_CLUSTER_NAME_2;
    symlinkNodeWatcher.onChanged(URI_SYMLINK_RESOURCE_NAME, getSymlinkNodeUpdate(primaryUriResourceName2));

    verify(fixture._xdsClient).watchXdsResource(eq(primaryUriResourceName2), eq(XdsClient.ResourceType.D2_NODE_MAP), any());
    verifyUriUpdate(fixture, PRIMARY_CLUSTER_NAME_2, SYMLINK_NAME);

    // if the old primary cluster gets an update, it will be published under its original cluster name
    // since the symlink points to the new primary cluster now.
    watcher.onChanged(DUMMY_NODE_MAP_UPDATE);

    verify(fixture._uriEventBus).publishInitialize(PRIMARY_CLUSTER_NAME, getDefaultUriProperties(PRIMARY_CLUSTER_NAME));
  }

  private static Value getProtoStringValue(String v)
  {
    return Value.newBuilder().setStringValue(v).build();
  }

  private static XdsClient.D2SymlinkNodeUpdate getSymlinkNodeUpdate(String primaryClusterResourceName)
  {
    return new XdsClient.D2SymlinkNodeUpdate("",
        XdsD2.D2SymlinkNode.newBuilder()
            .setMasterClusterNodePath(primaryClusterResourceName)
            .build()
    );
  }

  private static XdsClient.D2NodeUpdate getClusterNodeUpdate(String clusterName)
  {
    return new XdsClient.D2NodeUpdate("", XdsD2.D2Node.newBuilder()
        .setData(Struct.newBuilder().putFields("clusterName", getProtoStringValue(clusterName)))
        .setStat(XdsD2.Stat.newBuilder().setMzxid(1L).build())
        .build()
    );
  }

  private void verifyClusterNodeUpdate(XdsToD2PropertiesAdaptorFixture fixture, String clusterName, String expectedPublishName,
      ClusterStoreProperties expectedPublishProp)
  {
    XdsClient.D2NodeResourceWatcher watcher = (XdsClient.D2NodeResourceWatcher) fixture._watcherArgumentCaptor.getValue();
    watcher.onChanged(getClusterNodeUpdate(clusterName));
    verify(fixture._clusterEventBus).publishInitialize(expectedPublishName, expectedPublishProp);
  }

  private void verifyUriUpdate(XdsToD2PropertiesAdaptorFixture fixture, String clusterName, String expectedPublishName)
  {
    XdsClient.D2NodeMapResourceWatcher watcher = (XdsClient.D2NodeMapResourceWatcher) fixture._watcherArgumentCaptor.getValue();
    watcher.onChanged(DUMMY_NODE_MAP_UPDATE);
    verify(fixture._uriEventBus).publishInitialize(expectedPublishName, getDefaultUriProperties(clusterName));
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
    ArgumentCaptor<XdsClient.ResourceWatcher> _watcherArgumentCaptor;

    XdsToD2PropertiesAdaptor _adaptor;

    XdsToD2PropertiesAdaptorFixture()
    {
      MockitoAnnotations.initMocks(this);
      doNothing().when(_xdsClient).watchXdsResource(any(), any(), _watcherArgumentCaptor.capture());
      doNothing().when(_clusterEventBus).publishInitialize(any(), any());
      doNothing().when(_serviceEventBus).publishInitialize(any(), any());
      doNothing().when(_uriEventBus).publishInitialize(any(), any());
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
