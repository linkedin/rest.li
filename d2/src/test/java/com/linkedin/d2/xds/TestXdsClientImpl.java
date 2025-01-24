package com.linkedin.d2.xds;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.linkedin.d2.jmx.XdsClientJmx;
import com.linkedin.d2.jmx.XdsServerMetricsProvider;
import com.linkedin.d2.xds.XdsClient.D2URIMapUpdate;
import com.linkedin.d2.xds.XdsClient.ResourceType;
import com.linkedin.d2.xds.XdsClientImpl.DiscoveryResponseData;
import com.linkedin.d2.xds.XdsClientImpl.ResourceSubscriber;
import com.linkedin.d2.xds.XdsClientImpl.WildcardResourceSubscriber;
import com.linkedin.r2.util.NamedThreadFactory;
import indis.XdsD2;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
  public static final String SERVICE_NAME = "FooService";
  public static final String SERVICE_NAME_2 = "BarService";
  public static final String SERVICE_RESOURCE_NAME = "/d2/services/" + SERVICE_NAME;
  public static final String SERVICE_RESOURCE_NAME_2 = "/d2/services/" + SERVICE_NAME_2;
  public static final String CLUSTER_NAME = "FooClusterMaster-prod-ltx1";
  public static final String CLUSTER_RESOURCE_NAME = "/d2/uris/" + CLUSTER_NAME;
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

  private static final List<Resource> NODE_RESOURCES_WITH_NULL_RESOURCE_FIELD = Collections.singletonList(
      Resource.newBuilder().setVersion(VERSION1).setName(SERVICE_RESOURCE_NAME).setResource(PACKED_NODE_WITH_EMPTY_DATA).build());

  private static final XdsD2.D2ClusterOrServiceName CLUSTER_NAME_DATA = XdsD2.D2ClusterOrServiceName.newBuilder()
      .setClusterName(CLUSTER_NAME).build();
  private static final XdsD2.D2ClusterOrServiceName SERVICE_NAME_DATA = XdsD2.D2ClusterOrServiceName.newBuilder()
      .setServiceName(SERVICE_NAME).build();
  private static final XdsD2.D2ClusterOrServiceName SERVICE_NAME_DATA_2 = XdsD2.D2ClusterOrServiceName.newBuilder()
      .setServiceName(SERVICE_NAME_2).build();
  private static final XdsD2.D2ClusterOrServiceName NAME_DATA_WITH_NULL = XdsD2.D2ClusterOrServiceName.newBuilder().build();
  private static final Any PACKED_SERVICE_NAME_DATA = Any.pack(SERVICE_NAME_DATA);
  private static final Any PACKED_SERVICE_NAME_DATA_2 = Any.pack(SERVICE_NAME_DATA_2);
  private static final Any PACKED_NAME_DATA_WITH_NULL = Any.pack(NAME_DATA_WITH_NULL);
  public static final XdsClient.D2ClusterOrServiceNameUpdate CLUSTER_NAME_DATA_UPDATE =
      new XdsClient.D2ClusterOrServiceNameUpdate(CLUSTER_NAME_DATA);
  public static final XdsClient.D2ClusterOrServiceNameUpdate SERVICE_NAME_DATA_UPDATE =
      new XdsClient.D2ClusterOrServiceNameUpdate(SERVICE_NAME_DATA);
  public static final XdsClient.D2ClusterOrServiceNameUpdate SERVICE_NAME_DATA_UPDATE_2 =
      new XdsClient.D2ClusterOrServiceNameUpdate(SERVICE_NAME_DATA_2);
  private static final List<Resource> SERVICE_NAME_DATA_RESOURCES = Arrays.asList(
      Resource.newBuilder().setVersion(VERSION1).setName(SERVICE_RESOURCE_NAME)
          .setResource(PACKED_SERVICE_NAME_DATA).build(),
      Resource.newBuilder().setVersion(VERSION1).setName(SERVICE_RESOURCE_NAME_2)
          .setResource(PACKED_SERVICE_NAME_DATA_2).build()
  );
  private static final List<Resource> NULL_NAME_RESOURCES = Arrays.asList(
      Resource.newBuilder().setVersion(VERSION1).setName(CLUSTER_RESOURCE_NAME).build(),
      Resource.newBuilder().setVersion(VERSION1).setName(SERVICE_RESOURCE_NAME).setResource(PACKED_NAME_DATA_WITH_NULL).build()
      );

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
  private static final D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_EMPTY_MAP = new D2URIMapUpdate(Collections.emptyMap());
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

  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA1 =
      new DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_DATA1, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA2 =
      new DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_DATA2, null, NONCE, null);
  // Resource in ResourceUpdate is null, failed to parse which causes InvalidProtocolBufferException
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL =
      new DiscoveryResponseData(
          NODE,
          Collections.singletonList(Resource.newBuilder().setVersion(VERSION1).setName(SERVICE_RESOURCE_NAME)
              // not set resource field
              .build()),
          null,
          NONCE,
          null);
  // Resource field in Resource is null
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FIELD =
      new DiscoveryResponseData(NODE, NODE_RESOURCES_WITH_NULL_RESOURCE_FIELD, null, NONCE, null);
  // ResourceList is empty
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_WITH_EMPTY_NODE_RESPONSE =
      new DiscoveryResponseData(NODE, Collections.emptyList(), null, NONCE, null);

  private static final DiscoveryResponseData RESPONSE_WITH_SERVICE_NAMES =
      new DiscoveryResponseData(D2_CLUSTER_OR_SERVICE_NAME, SERVICE_NAME_DATA_RESOURCES, null, NONCE, null);
  private static final DiscoveryResponseData RESPONSE_WITH_NULL_NAMES =
      new DiscoveryResponseData(D2_CLUSTER_OR_SERVICE_NAME, NULL_NAME_RESOURCES, null, NONCE, null);
  private static final DiscoveryResponseData RESPONSE_WITH_EMPTY_NAMES =
      new DiscoveryResponseData(D2_CLUSTER_OR_SERVICE_NAME, Collections.emptyList(), null, NONCE, null);
  private static final DiscoveryResponseData RESPONSE_WITH_NAME_REMOVAL =
      new DiscoveryResponseData(D2_CLUSTER_OR_SERVICE_NAME, Collections.emptyList(),
          Collections.singletonList(SERVICE_RESOURCE_NAME), NONCE, null);

  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA1 =
      new DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_DATA1, null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA2 =
      new DiscoveryResponseData(D2_URI_MAP, URI_MAP_RESOURCE_WITH_DATA2, null, NONCE, null);

  // Resource in ResourceUpdate is null, failed to parse response.resource
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_RESOURCE_IS_NULL =
      new DiscoveryResponseData(
          D2_URI_MAP,
          Collections.singletonList(Resource.newBuilder().setVersion(VERSION1).setName(CLUSTER_RESOURCE_NAME)
              // not set resource field
              .build()),
          null,
          NONCE,
          null);

  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_EMPTY =
      new DiscoveryResponseData(D2_URI_MAP, EMPTY_URI_MAP_RESOURCE, null, NONCE, null);

  // ResourceList is empty
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESPONSE =
      new DiscoveryResponseData(D2_URI_MAP, Collections.emptyList(), null, NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL =
      new DiscoveryResponseData(NODE, Collections.emptyList(), Collections.singletonList(SERVICE_RESOURCE_NAME), NONCE, null);
  private static final DiscoveryResponseData DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL =
      new DiscoveryResponseData(D2_URI_MAP, Collections.emptyList(), Collections.singletonList(CLUSTER_RESOURCE_NAME), NONCE, null);

  private static final String CLUSTER_GLOB_COLLECTION = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/*";
  private static final String URI_URN1 = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/" + URI1;
  private static final String URI_URN2 = "xdstp:///indis.D2URI/" + CLUSTER_NAME + "/" + URI2;

  @DataProvider(name = "providerWatcherFlags")
  public Object[][] watcherFlags()
  {
    // {
    //    toWatchIndividual --- whether to watch resources with individual watcher
    //    toWatchWildcard --- whether to watch resources with wildcard watcher
    // }
    return new Object[][]
        {
            {true, false},
            {false, true},
            {true, true}
        };
  }
  @Test(dataProvider = "providerWatcherFlags")
  public void testHandleD2NodeResponseWithData(boolean toWatchIndividual, boolean toWatchWildcard)
  {
    // make sure the watchers are notified as expected regardless of watching only by its own type, or watching
    // with both via individual and wildcard watchers
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    if (toWatchIndividual)
    {
      fixture.watchNodeResource();
    }
    if (toWatchWildcard)
    {
      fixture.watchNodeResourceViaWildcard();
    }
    // subscriber original data is null
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(eq(NODE_UPDATE1));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(eq(SERVICE_RESOURCE_NAME), eq(NODE_UPDATE1));
    verifyZeroInteractions(fixture._serverMetricsProvider); // initial update should not track latency
    // subscriber data should be updated to NODE_UPDATE1
    Assert.assertEquals(fixture._nodeSubscriber.getData(), NODE_UPDATE1);
    Assert.assertEquals(fixture._nodeWildcardSubscriber.getData(SERVICE_RESOURCE_NAME), NODE_UPDATE1);

    // subscriber original data is invalid, xds server latency won't be tracked
    fixture._nodeSubscriber.setData(new XdsClient.NodeUpdate(null));
    fixture._nodeWildcardSubscriber.setData(SERVICE_RESOURCE_NAME, new XdsClient.NodeUpdate(null));
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    fixture.verifyAckSent(2);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 2 : 0)).onChanged(eq(NODE_UPDATE1));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 2 : 0)).onChanged(eq(SERVICE_RESOURCE_NAME), eq(NODE_UPDATE1));
    verifyZeroInteractions(fixture._serverMetricsProvider);

    // subscriber data should be updated to NODE_UPDATE2
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA2);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(eq(NODE_UPDATE2));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0)).
        onChanged(eq(SERVICE_RESOURCE_NAME), eq(NODE_UPDATE2));
    verify(fixture._serverMetricsProvider).trackLatency(anyLong());
    Assert.assertEquals(fixture._nodeSubscriber.getData(), NODE_UPDATE2);
    Assert.assertEquals(fixture._nodeWildcardSubscriber.getData(SERVICE_RESOURCE_NAME), NODE_UPDATE2);
  }

  @Test
  public void testHandleD2NodeUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_WITH_EMPTY_NODE_RESPONSE);
    fixture.verifyAckSent(1);
    verify(fixture._clusterSubscriber, times(0)).onData(any(), any());
    verify(fixture._uriMapWildcardSubscriber, times(0)).onData(any(), any());
  }

  @DataProvider(name = "badNodeUpdateTestCases")
  public Object[][] provideBadNodeDataTestCases()
  {
    // {
    //    badData --- bad resource data to test
    //    nackExpected --- whether nack is expected
    //    toWatchIndividual --- whether to watch resources with individual watcher
    //    toWatchWildcard --- whether to watch resources with wildcard watcher
    // }
    return new Object[][]{
        {DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL, true, true, false},
        {DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL, true, false, true},
        {DISCOVERY_RESPONSE_NODE_RESOURCE_IS_NULL, true, true, true},
        {DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FIELD, false, true, false},
        {DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FIELD, false, false, true},
        {DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FIELD, false, true, true},
    };
  }

  @Test(dataProvider = "badNodeUpdateTestCases")
  public void testHandleD2NodeUpdateWithBadData(DiscoveryResponseData badData, boolean nackExpected,
      boolean toWatchIndividual, boolean toWatchWildcard )
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    if (toWatchIndividual)
    {
      fixture.watchNodeResource();
    }
    if (toWatchWildcard)
    {
      fixture.watchNodeResourceViaWildcard();
    }
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyAckOrNack(nackExpected, 1);
    // since current data is null, all watchers should be notified for bad data to stop waiting.
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(eq(NODE.emptyData()));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0)).onChanged(any(), eq(NODE.emptyData()));
    Assert.assertEquals(fixture._nodeSubscriber.getData(), NODE.emptyData());

    fixture._nodeSubscriber.setData(NODE_UPDATE1);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyAckOrNack(nackExpected, 2);
    // current data is not null, bad data will not overwrite the original valid data and watchers won't be notified.
    Assert.assertEquals(fixture._nodeSubscriber.getData(), NODE_UPDATE1);
    verify(fixture._resourceWatcher, times(0)).onChanged(eq(NODE_UPDATE1));
    verify(fixture._wildcardResourceWatcher, times(0)).onChanged(any(), eq(NODE_UPDATE1));
  }

  // Removed resource will not overwrite the original valid data for individual subscriber, but will be removed
  // in wildcard subscriber
  @Test
  public void testHandleD2NodeResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    fixture._nodeSubscriber.setData(NODE_UPDATE1);
    fixture._nodeWildcardSubscriber.setData(SERVICE_RESOURCE_NAME, NODE_UPDATE1);
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA_WITH_REMOVAL);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(NODE_UPDATE1));
    verify(fixture._wildcardResourceWatcher).onRemoval(eq(SERVICE_RESOURCE_NAME));
    verify(fixture._nodeSubscriber).onRemoval();
    verify(fixture._nodeWildcardSubscriber).onRemoval(eq(SERVICE_RESOURCE_NAME));
    Assert.assertEquals(fixture._nodeSubscriber.getData(), NODE_UPDATE1);
    Assert.assertNull(fixture._nodeWildcardSubscriber.getData(SERVICE_RESOURCE_NAME));
  }

  @Test
  public void testHandleD2ClusterOrServiceNameResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    // D2ClusterOrServiceName can be subscribed only via wildcard, valid new data should update subscriber data
    fixture._xdsClientImpl.handleResponse(RESPONSE_WITH_SERVICE_NAMES);
    fixture.verifyAckSent(1);
    verify(fixture._wildcardResourceWatcher).onChanged(eq(SERVICE_RESOURCE_NAME), eq(SERVICE_NAME_DATA_UPDATE));
    verify(fixture._wildcardResourceWatcher).onChanged(eq(SERVICE_RESOURCE_NAME_2), eq(SERVICE_NAME_DATA_UPDATE_2));
    verify(fixture._wildcardResourceWatcher).onAllResourcesProcessed();
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(SERVICE_RESOURCE_NAME), SERVICE_NAME_DATA_UPDATE);
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(SERVICE_RESOURCE_NAME_2), SERVICE_NAME_DATA_UPDATE_2);
    verifyZeroInteractions(fixture._serverMetricsProvider); // initial update should not track latency
  }

  @Test
  public void testHandleD2ClusterOrServiceNameEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    fixture._xdsClientImpl.handleResponse(RESPONSE_WITH_EMPTY_NAMES);
    fixture.verifyAckSent(1);
    verify(fixture._nameWildcardSubscriber, times(0)).onData(any(), any());
  }

  @Test
  public void testHandleD2ClusterOrServiceNameResponseWithBadData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    // when current data is null, all watchers should be notified for bad data to stop waiting.
    fixture._xdsClientImpl.handleResponse(RESPONSE_WITH_NULL_NAMES);
    fixture.verifyAckOrNack(true, 1);
    verify(fixture._wildcardResourceWatcher).onChanged(eq(CLUSTER_RESOURCE_NAME),
        eq(D2_CLUSTER_OR_SERVICE_NAME.emptyData()));
    verify(fixture._wildcardResourceWatcher).onChanged(eq(SERVICE_RESOURCE_NAME),
        eq(D2_CLUSTER_OR_SERVICE_NAME.emptyData()));
    verify(fixture._wildcardResourceWatcher).onAllResourcesProcessed();
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME),
        D2_CLUSTER_OR_SERVICE_NAME.emptyData());
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(SERVICE_RESOURCE_NAME),
        D2_CLUSTER_OR_SERVICE_NAME.emptyData());

    // when current data is not null, bad data won't overwrite the original valid data and watchers won't be notified.
    fixture._nameWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, CLUSTER_NAME_DATA_UPDATE);
    fixture._nameWildcardSubscriber.setData(SERVICE_RESOURCE_NAME, SERVICE_NAME_DATA_UPDATE);
    fixture._xdsClientImpl.handleResponse(RESPONSE_WITH_NULL_NAMES);
    fixture.verifyAckOrNack(true, 2);
    verify(fixture._wildcardResourceWatcher, times(0))
        .onChanged(eq(CLUSTER_RESOURCE_NAME), eq(CLUSTER_NAME_DATA_UPDATE));
    verify(fixture._wildcardResourceWatcher, times(0))
        .onChanged(eq(SERVICE_RESOURCE_NAME), eq(SERVICE_NAME_DATA_UPDATE));
    verify(fixture._wildcardResourceWatcher, times(2)).onAllResourcesProcessed();
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME), CLUSTER_NAME_DATA_UPDATE);
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(SERVICE_RESOURCE_NAME), SERVICE_NAME_DATA_UPDATE);
  }

  // Removed resource will be removed in wildcard subscriber, where other resource is still kept intact.
  @Test
  public void testHandleD2ClusterOrServiceNameResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    fixture._nameWildcardSubscriber.setData(SERVICE_RESOURCE_NAME, SERVICE_NAME_DATA_UPDATE);
    fixture._nameWildcardSubscriber.setData(SERVICE_RESOURCE_NAME_2, SERVICE_NAME_DATA_UPDATE_2);
    fixture._xdsClientImpl.handleResponse(RESPONSE_WITH_NAME_REMOVAL);
    fixture.verifyAckSent(1);
    verify(fixture._wildcardResourceWatcher).onRemoval(SERVICE_RESOURCE_NAME);
    verify(fixture._nameWildcardSubscriber).onRemoval(SERVICE_RESOURCE_NAME);
    Assert.assertNull(fixture._nameWildcardSubscriber.getData(SERVICE_RESOURCE_NAME));
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(SERVICE_RESOURCE_NAME_2), SERVICE_NAME_DATA_UPDATE_2);
  }

  @Test(dataProvider = "providerWatcherFlags")
  public void testHandleD2URIMapResponseWithData(boolean toWatchIndividual, boolean toWatchWildcard)
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    if (toWatchIndividual)
    {
      fixture.watchUriMapResource();
    }
    if (toWatchWildcard)
    {
      fixture.watchUriMapResourceViaWildcard();
    }
    // subscriber original data is null, watchers and subscribers will be notified/updated for new valid data, and
    // xds server latency won't be tracked
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(eq(CLUSTER_RESOURCE_NAME), eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_DATA1);
    Assert.assertEquals(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME), D2_URI_MAP_UPDATE_WITH_DATA1);

    // subscriber original data is not null, new data will overwrite the original valid data, and watchers will be
    // notified, and xds server latency will be tracked.
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA2); // updated uri1, added uri2
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA2));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(eq(CLUSTER_RESOURCE_NAME), eq(D2_URI_MAP_UPDATE_WITH_DATA2));
    verify(fixture._serverMetricsProvider, times(2)).trackLatency(anyLong());
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_DATA2);
    Assert.assertEquals(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME), D2_URI_MAP_UPDATE_WITH_DATA2);
    fixture.verifyAckSent(2);

    // new data with an empty uri map will update the original data, watchers will be notified, but xds server latency
    // won't be tracked.
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_EMPTY);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_EMPTY_MAP));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(eq(CLUSTER_RESOURCE_NAME), eq(D2_URI_MAP_UPDATE_WITH_EMPTY_MAP));
    verifyNoMoreInteractions(fixture._serverMetricsProvider); // won't track latency for removed uris
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_EMPTY_MAP);
    Assert.assertEquals(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME),
        D2_URI_MAP_UPDATE_WITH_EMPTY_MAP);
    fixture.verifyAckSent(3);
  }

  @Test
  public void testHandleD2URIMapUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    // Sanity check that the code handles empty responses
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESPONSE);
    fixture.verifyAckSent(1);
    verify(fixture._clusterSubscriber, times(0)).onData(any(), any());
    verify(fixture._uriMapWildcardSubscriber, times(0)).onData(any(), any());
  }

  @Test(dataProvider = "providerWatcherFlags")
  public void testHandleD2URIMapUpdateWithBadData(boolean toWatchIndividual, boolean toWatchWildcard)
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    if (toWatchIndividual)
    {
      fixture.watchUriMapResource();
    }
    if (toWatchWildcard)
    {
      fixture.watchUriMapResourceViaWildcard();
    }
    // current data is null, all watchers should be notified for bad data to stop waiting.
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_RESOURCE_IS_NULL);
    fixture.verifyAckOrNack(true, 1);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(eq(D2_URI_MAP.emptyData()));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(eq(CLUSTER_RESOURCE_NAME), eq(D2_URI_MAP.emptyData()));
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP.emptyData());
    Assert.assertEquals(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME), D2_URI_MAP.emptyData());

    // current data is not null, bad data will not overwrite the original valid data and watchers won't be notified.
    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._uriMapWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_RESOURCE_IS_NULL);

    fixture.verifyAckOrNack(true, 2);
    verify(fixture._resourceWatcher, times(0)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._wildcardResourceWatcher, times(0))
        .onChanged(any(), eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    // bad data will not overwrite the original valid data
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_DATA1);
    verifyZeroInteractions(fixture._serverMetricsProvider);
  }

  @Test
  public void testHandleD2URIMapResponseWithRemoval()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._uriMapWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA_WITH_REMOVAL);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._wildcardResourceWatcher).onRemoval(eq(CLUSTER_RESOURCE_NAME));
    verify(fixture._clusterSubscriber).onRemoval();
    verify(fixture._uriMapWildcardSubscriber).onRemoval(eq(CLUSTER_RESOURCE_NAME));
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
    fixture.watchAllResourceAndWatcherTypes();
    // subscriber original data is null
    fixture._xdsClientImpl.handleResponse(createUri1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._wildcardResourceWatcher).onChanged(eq(CLUSTER_RESOURCE_NAME), eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA1
    Assert.assertEquals(Objects.requireNonNull(actualData).getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
    actualData = (D2URIMapUpdate) fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME);
    Assert.assertEquals(Objects.requireNonNull(actualData).getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());

    // subscriber original data is invalid, xds server latency won't be tracked
    fixture._clusterSubscriber.setData(new D2URIMapUpdate(null));
    fixture._uriMapWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, new D2URIMapUpdate(null));
    fixture._xdsClientImpl.handleResponse(createUri1);
    fixture.verifyAckSent(2);
    verify(fixture._resourceWatcher, times(2)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._wildcardResourceWatcher, times(2)).onChanged(eq(CLUSTER_RESOURCE_NAME), eq(D2_URI_MAP_UPDATE_WITH_DATA1));
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
    verify(fixture._wildcardResourceWatcher).onChanged(eq(CLUSTER_RESOURCE_NAME), eq(expectedUpdate));
    // track latency only for updated/new uri (not for deletion)
    verify(fixture._serverMetricsProvider).trackLatency(anyLong());
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    actualData = (D2URIMapUpdate) fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME);
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
    verify(fixture._wildcardResourceWatcher).onChanged(eq(CLUSTER_RESOURCE_NAME), eq(expectedUpdate));
    verifyNoMoreInteractions(fixture._serverMetricsProvider);
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    actualData = (D2URIMapUpdate) fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME);
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    fixture.verifyAckSent(4);
  }

  @Test
  public void testHandleD2URICollectionUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    // Sanity check that the code handles empty responses
    fixture._xdsClientImpl.handleResponse(new DiscoveryResponseData(D2_URI, null, null, NONCE, null));
    fixture.verifyAckSent(1);
  }

  @Test(dataProvider = "providerWatcherFlags")
  public void testHandleD2URICollectionUpdateWithBadData(boolean toWatchIndividual, boolean toWatchWildcard)
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
    if (toWatchIndividual)
    {
      fixture.watchUriMapResource();
    }
    if (toWatchWildcard)
    {
      fixture.watchUriMapResourceViaWildcard();
    }

    // current data is null, empty placeholder data will be set the subscriber,
    // and all watchers should be notified for bad data to stop waiting.
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyNackSent(1);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(eq(D2_URI_MAP.emptyData()));
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(any(), eq(D2_URI_MAP.emptyData()));
    verifyZeroInteractions(fixture._serverMetricsProvider);

    // current data is not null, bad data will not overwrite the original valid data and watchers won't be notified.
    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._uriMapWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyNackSent(2);
    verify(fixture._resourceWatcher, times(0)).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._wildcardResourceWatcher, times(0))
        .onChanged(any(), eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_DATA1);
  }

  @Test
  public void testHandleD2URICollectionResponseWithRemoval()
  {
    DiscoveryResponseData removeClusterResponse =
        new DiscoveryResponseData(D2_URI, null, Collections.singletonList(CLUSTER_GLOB_COLLECTION), NONCE, null);

    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._uriMapWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(removeClusterResponse);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    verify(fixture._wildcardResourceWatcher).onRemoval(eq(CLUSTER_RESOURCE_NAME));
    verify(fixture._clusterSubscriber).onRemoval();
    verify(fixture._uriMapWildcardSubscriber).onRemoval(eq(CLUSTER_RESOURCE_NAME));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    // removed resource will not overwrite the original valid data
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_DATA1);
    Assert.assertNull(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME));
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

    WildcardResourceSubscriber wildcardSubscriber = new WildcardResourceSubscriber(D2_CLUSTER_OR_SERVICE_NAME);
    XdsClient.WildcardResourceWatcher _wildcardWatcher = Mockito.mock(XdsClient.WildcardResourceWatcher.class);
    wildcardSubscriber.addWatcher(_wildcardWatcher);
    verify(_wildcardWatcher, times(0)).onChanged(any(), any());

    wildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, CLUSTER_NAME_DATA_UPDATE);
    for (int i = 0; i < 10; i++)
    {
      wildcardSubscriber.addWatcher(_wildcardWatcher);
    }
    verify(_wildcardWatcher, times(10)).onChanged(eq(CLUSTER_RESOURCE_NAME), eq(CLUSTER_NAME_DATA_UPDATE));
  }

  @DataProvider(name = "provideUseGlobCollection")
  public Object[][] provideUseGlobCollection()
  {
    // {
    //   useGlobCollection --- whether to use glob collection
    // }
    return new Object[][]{
        {true},
        {false}
    };
  }
  @Test(dataProvider = "provideUseGlobCollection", timeOut = 2000)
  // Retry task should re-subscribe the resources registered in each subscriber type.
  public void testRetry(boolean useGlobCollection) throws ExecutionException, InterruptedException
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture(useGlobCollection);
    fixture.watchAllResourceAndWatcherTypes();
    fixture._xdsClientImpl.testRetryTask(fixture._adsStream);
    fixture._xdsClientImpl._retryRpcStreamFuture.get();

    // get all the resource types and names sent in the discovery requests and verify them
    List<ResourceType> types = fixture._resourceTypesArgumentCaptor.getAllValues();
    List<String> nameLists = fixture._resourceNamesArgumentCaptor.getAllValues().stream()
        .map(names ->
        {
          if (names.size() != 1)
          {
            Assert.fail("Resource names should be a singleton list");
          }
          return names.iterator().next();
        }).collect(Collectors.toList());
    Assert.assertEquals(types.size(), 5);
    Assert.assertEquals(nameLists.size(), 5);

    List<Pair<ResourceType, String>> args = new ArrayList<>();
    for (int i = 0; i < 5; i++)
    {
      args.add(Pair.of(types.get(i), nameLists.get(i)));
    }
    args = args.stream().sorted().collect(Collectors.toList());

    Assert.assertEquals(args, Arrays.asList(
        Pair.of(NODE, "*"),
        Pair.of(NODE, SERVICE_RESOURCE_NAME),
        Pair.of(useGlobCollection ? D2_URI : D2_URI_MAP, "*"),
        Pair.of(useGlobCollection ? D2_URI : D2_URI_MAP, useGlobCollection ? CLUSTER_GLOB_COLLECTION : CLUSTER_RESOURCE_NAME),
        Pair.of(D2_CLUSTER_OR_SERVICE_NAME, "*")
    ));
  }

  private static final class XdsClientImplFixture
  {
    XdsClientImpl _xdsClientImpl;
    @Mock
    XdsClientImpl.AdsStream _adsStream;
    @Mock
    XdsClientJmx _xdsClientJmx;
    ResourceSubscriber _nodeSubscriber;
    ResourceSubscriber _clusterSubscriber;
    XdsClientImpl.WildcardResourceSubscriber _nodeWildcardSubscriber;
    XdsClientImpl.WildcardResourceSubscriber _uriMapWildcardSubscriber;
    XdsClientImpl.WildcardResourceSubscriber _nameWildcardSubscriber;
    Map<ResourceType, Map<String, ResourceSubscriber>> _subscribers = Maps.immutableEnumMap(
        Stream.of(ResourceType.values())
            .collect(Collectors.toMap(Function.identity(), e -> new HashMap<>())));
    Map<ResourceType, XdsClientImpl.WildcardResourceSubscriber> _wildcardSubscribers = Maps.newEnumMap(ResourceType.class);

    @Mock
    XdsClient.ResourceWatcher _resourceWatcher;
    @Mock
    XdsClient.WildcardResourceWatcher _wildcardResourceWatcher;
    @Mock
    XdsServerMetricsProvider _serverMetricsProvider;

    @Captor
    ArgumentCaptor<ResourceType> _resourceTypesArgumentCaptor;
    @Captor
    ArgumentCaptor<Collection<String>> _resourceNamesArgumentCaptor;

    XdsClientImplFixture()
    {
      this(false);
    }

    XdsClientImplFixture(boolean useGlobCollections)
    {
      MockitoAnnotations.initMocks(this);
      _nodeSubscriber = spy(new ResourceSubscriber(NODE, SERVICE_RESOURCE_NAME, _xdsClientJmx));
      _clusterSubscriber = spy(new ResourceSubscriber(D2_URI_MAP, CLUSTER_RESOURCE_NAME, _xdsClientJmx));
      _nodeWildcardSubscriber = spy(new XdsClientImpl.WildcardResourceSubscriber(NODE));
      _uriMapWildcardSubscriber = spy(new XdsClientImpl.WildcardResourceSubscriber(D2_URI_MAP));
      _nameWildcardSubscriber = spy(new XdsClientImpl.WildcardResourceSubscriber(D2_CLUSTER_OR_SERVICE_NAME));

      doNothing().when(_resourceWatcher).onChanged(any());
      doNothing().when(_wildcardResourceWatcher).onChanged(any(), any());
      doNothing().when(_serverMetricsProvider).trackLatency(anyLong());

      for (ResourceSubscriber subscriber : Lists.newArrayList(_nodeSubscriber, _clusterSubscriber))
      {
        _subscribers.get(subscriber.getType()).put(subscriber.getResource(), subscriber);
      }
      for (WildcardResourceSubscriber subscriber : Lists.newArrayList(_nodeWildcardSubscriber,
          _uriMapWildcardSubscriber, _nameWildcardSubscriber))
      {
        _wildcardSubscribers.put(subscriber.getType(), subscriber);
      }

      _xdsClientImpl = spy(new XdsClientImpl(null, null,
          Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("test executor")),
          0, useGlobCollections, _serverMetricsProvider));
      _xdsClientImpl._adsStream = _adsStream;

      doNothing().when(_xdsClientImpl).startRpcStreamLocal();
      doNothing().when(_xdsClientImpl).sendAckOrNack(any(), any(), any());
      doNothing().when(_adsStream).sendDiscoveryRequest(_resourceTypesArgumentCaptor.capture(), _resourceNamesArgumentCaptor.capture());

      when(_xdsClientImpl.getXdsClientJmx()).thenReturn(_xdsClientJmx);
      when(_xdsClientImpl.getResourceSubscribers()).thenReturn(_subscribers);
      when(_xdsClientImpl.getWildcardResourceSubscribers()).thenReturn(_wildcardSubscribers);
    }

    void watchAllResourceAndWatcherTypes()
    {
      for (ResourceSubscriber subscriber : Lists.newArrayList(_nodeSubscriber, _clusterSubscriber))
      {
        subscriber.addWatcher(_resourceWatcher);
      }
      for (WildcardResourceSubscriber subscriber : Lists.newArrayList(_nodeWildcardSubscriber,
          _uriMapWildcardSubscriber, _nameWildcardSubscriber))
      {
        subscriber.addWatcher(_wildcardResourceWatcher);
      }
    }

    void watchNodeResource()
    {
      _nodeSubscriber.addWatcher(_resourceWatcher);
    }

    void watchNodeResourceViaWildcard()
    {
      _nodeWildcardSubscriber.addWatcher(_wildcardResourceWatcher);
    }

    void watchUriMapResource()
    {
      _clusterSubscriber.addWatcher(_resourceWatcher);
    }

    void watchUriMapResourceViaWildcard()
    {
      _uriMapWildcardSubscriber.addWatcher(_wildcardResourceWatcher);
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
