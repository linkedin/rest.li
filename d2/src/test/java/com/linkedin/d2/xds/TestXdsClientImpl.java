package com.linkedin.d2.xds;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.linkedin.d2.jmx.XdsClientJmx;
import com.linkedin.d2.jmx.XdsServerMetricsProvider;
import com.linkedin.d2.xds.XdsClient.D2URIMapUpdate;
import com.linkedin.d2.xds.XdsClient.ResourceType;
import com.linkedin.d2.xds.XdsClientImpl.DiscoveryResponseData;
import com.linkedin.d2.xds.XdsClientImpl.ResourceSubscriber;
import com.linkedin.d2.xds.XdsClientImpl.WildcardResourceSubscriber;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.clock.Time;
import indis.XdsD2;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.GreaterThan;
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
  private static final String VERSION3 = "3";
  private static final String NONCE = "nonce";

  private static final long TEN_MINS = Time.minutes(10);
  private static final XdsD2.Node NODE_WITH_DATA = createNodeWithData(DATA,
      SystemClock.instance().currentTimeMillis() - TEN_MINS);
  private static final XdsD2.Node NODE_WITH_DATA2 = createNodeWithData(DATA2,
      SystemClock.instance().currentTimeMillis() - TEN_MINS);
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

  private static final XdsD2.D2URI D2URI_1 = createD2URIWithData(URI1, CLUSTER_NAME, VERSION1, null,
      SystemClock.instance().currentTimeMillis() - TEN_MINS);
  private static final XdsD2.D2URI D2URI_1_1 = createD2URIWithData(URI1, CLUSTER_NAME, VERSION2,
          Collections.singletonMap(0, 2.0),
      SystemClock.instance().currentTimeMillis() - TEN_MINS);
  private static final XdsD2.D2URI D2URI_2 = createD2URIWithData(URI2, CLUSTER_NAME, VERSION1, null,
      SystemClock.instance().currentTimeMillis() - TEN_MINS);
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

  private static final D2URIMapUpdate D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1 =
      new D2URIMapUpdate(D2_URI_MAP_WITH_DATA1.getUrisMap(), true);

  private static final D2URIMapUpdate D2_URI_MAP_UPDATE_WITH_EMPTY_MAP = new D2URIMapUpdate(Collections.emptyMap(), true);
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

  private static final String URI_URN1 = GlobCollectionUtils.globCollectionUrn(CLUSTER_NAME, URI1);
  private static final String URI_URN2 = GlobCollectionUtils.globCollectionUrn(CLUSTER_NAME, URI2);
  private static final DiscoveryResponseData RESPONSE_D2URI1 = new DiscoveryResponseData(D2_URI, Collections.singletonList(
      Resource.newBuilder()
          .setVersion(VERSION1)
          .setName(URI_URN1)
          .setResource(Any.pack(D2URI_1))
          .build()
  ), null, NONCE, null);
  private static final DiscoveryResponseData RESPONSE_D2URI2_DELETE_URI1 = new DiscoveryResponseData(D2_URI, Collections.singletonList(
      Resource.newBuilder()
          .setVersion(VERSION1)
          .setName(URI_URN2)
          .setResource(Any.pack(D2URI_2))
          .build()
  ), Collections.singletonList(URI_URN1), NONCE, null);
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

    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(NODE_UPDATE1);
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(SERVICE_RESOURCE_NAME, NODE_UPDATE1);
    // subscriber data should be updated to NODE_UPDATE1
    Assert.assertEquals(fixture._nodeSubscriber.getData(), NODE_UPDATE1);
    Assert.assertEquals(fixture._nodeWildcardSubscriber.getData(SERVICE_RESOURCE_NAME), NODE_UPDATE1);

    // subscriber data should be updated to NODE_UPDATE2
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA2);
    fixture.verifyAckSent(2);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(NODE_UPDATE2);
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0)).
        onChanged(SERVICE_RESOURCE_NAME, NODE_UPDATE2);
    // subscriber data should be updated
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
    verify(fixture._d2UriSubscriber, times(0)).onData(any(), any(), anyBoolean());
    verify(fixture._clusterSubscriber, times(0)).onData(any(), any(), anyBoolean());
    verify(fixture._uriMapWildcardSubscriber, times(0)).onData(any(), any(), any(), anyBoolean());
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
      boolean toWatchIndividual, boolean toWatchWildcard)
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
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(NODE.emptyData());
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0)).onChanged(any(), eq(NODE.emptyData()));
    Assert.assertEquals(fixture._nodeSubscriber.getData(), NODE.emptyData());

    fixture._nodeSubscriber.setData(NODE_UPDATE1);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyAckOrNack(nackExpected, 2);
    // current data is not null, bad data will not overwrite the original valid data and watchers won't be notified.
    Assert.assertEquals(fixture._nodeSubscriber.getData(), NODE_UPDATE1);
    verify(fixture._resourceWatcher, times(0)).onChanged(NODE_UPDATE1);
    verify(fixture._wildcardResourceWatcher, times(0)).onChanged(any(), eq(NODE_UPDATE1));
  }

  @Test
  public void testExecutorServiceNotUsedAfterShutdown() {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._executorService.shutdown();

    // once the _executorService is shutdown, all of these calls should be no-ops and not throw
    // RejectedExecutionExceptions due to the checks in checkShutdownAndExecute and checkShutdownAndSchedule
    fixture._xdsClientImpl.startRpcStream();
    fixture._xdsClientImpl.watchXdsResource(CLUSTER_RESOURCE_NAME, fixture._resourceWatcher);
    fixture._xdsClientImpl.watchAllXdsResources(fixture._wildcardResourceWatcher);

    fixture._xdsClientImpl.testRetryTask(fixture._adsStream);
    fixture._xdsClientImpl.startRpcStreamLocal();

    verify(fixture._executorService, never()).execute(any());
    verify(fixture._executorService, never()).schedule((Runnable) any(), anyLong(), any());
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
    verify(fixture._resourceWatcher).onChanged(NODE_UPDATE1);
    verify(fixture._wildcardResourceWatcher).onRemoval(SERVICE_RESOURCE_NAME);
    verify(fixture._nodeSubscriber).onRemoval();
    verify(fixture._nodeWildcardSubscriber).onRemoval(SERVICE_RESOURCE_NAME);
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
    verify(fixture._wildcardResourceWatcher).onChanged(SERVICE_RESOURCE_NAME, SERVICE_NAME_DATA_UPDATE);
    verify(fixture._wildcardResourceWatcher).onChanged(SERVICE_RESOURCE_NAME_2, SERVICE_NAME_DATA_UPDATE_2);
    verify(fixture._wildcardResourceWatcher).onAllResourcesProcessed();
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(SERVICE_RESOURCE_NAME), SERVICE_NAME_DATA_UPDATE);
    Assert.assertEquals(fixture._nameWildcardSubscriber.getData(SERVICE_RESOURCE_NAME_2), SERVICE_NAME_DATA_UPDATE_2);
    verifyZeroInteractions(fixture._serverMetricsProvider); // D2ClusterOrServiceName response does not track latency
  }

  @Test
  public void testHandleD2ClusterOrServiceNameEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    fixture._xdsClientImpl.handleResponse(RESPONSE_WITH_EMPTY_NAMES);
    fixture.verifyAckSent(1);
    verify(fixture._nameWildcardSubscriber, times(0)).onData(any(), any(), any(), anyBoolean());
  }

  @Test
  public void testHandleD2ClusterOrServiceNameResponseWithBadData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    // when current data is null, all watchers should be notified for bad data to stop waiting.
    fixture._xdsClientImpl.handleResponse(RESPONSE_WITH_NULL_NAMES);
    fixture.verifyAckOrNack(true, 1);
    verify(fixture._wildcardResourceWatcher).onChanged(CLUSTER_RESOURCE_NAME, D2_CLUSTER_OR_SERVICE_NAME.emptyData());
    verify(fixture._wildcardResourceWatcher).onChanged(SERVICE_RESOURCE_NAME, D2_CLUSTER_OR_SERVICE_NAME.emptyData());
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
        .onChanged(CLUSTER_RESOURCE_NAME, CLUSTER_NAME_DATA_UPDATE);
    verify(fixture._wildcardResourceWatcher, times(0))
        .onChanged(SERVICE_RESOURCE_NAME, SERVICE_NAME_DATA_UPDATE);
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
    // subscriber original data is null, watchers and subscribers will be notified/updated for new valid data
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(D2_URI_MAP_UPDATE_WITH_DATA1);
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(CLUSTER_RESOURCE_NAME, D2_URI_MAP_UPDATE_WITH_DATA1);
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_DATA1);
    Assert.assertEquals(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME), D2_URI_MAP_UPDATE_WITH_DATA1);

    // subscriber original data is not null, new data will overwrite the original valid data, and watchers will be
    // notified
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_DATA2); // updated uri1, added uri2
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(D2_URI_MAP_UPDATE_WITH_DATA2);
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(CLUSTER_RESOURCE_NAME, D2_URI_MAP_UPDATE_WITH_DATA2);
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_DATA2);
    Assert.assertEquals(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME), D2_URI_MAP_UPDATE_WITH_DATA2);
    fixture.verifyAckSent(2);

    // new data with an empty uri map will update the original data, watchers will be notified
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_EMPTY);
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(D2_URI_MAP_UPDATE_WITH_EMPTY_MAP);
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(CLUSTER_RESOURCE_NAME, D2_URI_MAP_UPDATE_WITH_EMPTY_MAP);
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_EMPTY_MAP);
    Assert.assertEquals(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME),
        D2_URI_MAP_UPDATE_WITH_EMPTY_MAP);
    fixture.verifyAckSent(3);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    Assert.assertFalse(actualData.isGlobCollectionEnabled());
    Assert.assertTrue(actualData.getUpdatedUrisName().isEmpty());
    Assert.assertTrue(actualData.getRemovedUrisName().isEmpty());
  }

  @Test
  public void testHandleD2URIMapUpdateWithEmptyResponse()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    // Sanity check that the code handles empty responses
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_WITH_EMPTY_URI_MAP_RESPONSE);
    fixture.verifyAckSent(1);
    verify(fixture._clusterSubscriber, times(0)).onData(any(), any(), anyBoolean());
    verify(fixture._uriMapWildcardSubscriber, times(0)).onData(any(), any(), any(), anyBoolean());
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
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(D2_URI_MAP.emptyData());
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(CLUSTER_RESOURCE_NAME, D2_URI_MAP.emptyData());
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP.emptyData());
    Assert.assertEquals(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME), D2_URI_MAP.emptyData());

    // current data is not null, bad data will not overwrite the original valid data and watchers won't be notified.
    fixture._clusterSubscriber.setData(D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._uriMapWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, D2_URI_MAP_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_URI_MAP_RESOURCE_IS_NULL);

    fixture.verifyAckOrNack(true, 2);
    verify(fixture._resourceWatcher, times(0)).onChanged(D2_URI_MAP_UPDATE_WITH_DATA1);
    verify(fixture._wildcardResourceWatcher, times(0))
        .onChanged(any(), eq(D2_URI_MAP_UPDATE_WITH_DATA1));
    // bad data will not overwrite the original valid data
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_UPDATE_WITH_DATA1);
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    Assert.assertFalse(actualData.isGlobCollectionEnabled());
    Assert.assertTrue(actualData.getUpdatedUrisName().isEmpty());
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
    verify(fixture._resourceWatcher).onChanged(D2_URI_MAP_UPDATE_WITH_DATA1);
    verify(fixture._wildcardResourceWatcher).onRemoval(CLUSTER_RESOURCE_NAME);
    verify(fixture._clusterSubscriber).onRemoval();
    verify(fixture._uriMapWildcardSubscriber).onRemoval(CLUSTER_RESOURCE_NAME);
    verifyZeroInteractions(fixture._serverMetricsProvider);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // removed resource will not overwrite the original valid data
    Assert.assertEquals(Objects.requireNonNull(actualData).getURIMap(), D2_URI_MAP_UPDATE_WITH_DATA1.getURIMap());
    Assert.assertFalse(actualData.isGlobCollectionEnabled());
    Assert.assertTrue(actualData.getRemovedUrisName().isEmpty());
  }

  @Test
  public void testHandleD2URICollectionResponseWithData()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();

    fixture._xdsClientImpl.handleResponse(RESPONSE_D2URI1);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    verify(fixture._wildcardResourceWatcher).onChanged(CLUSTER_RESOURCE_NAME, D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_UPDATE_WITH_DATA1
    Assert.assertEquals(Objects.requireNonNull(actualData).getURIMap(), D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1.getURIMap());
    actualData = (D2URIMapUpdate) fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME);
    Assert.assertEquals(Objects.requireNonNull(actualData).getURIMap(), D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1.getURIMap());
    Assert.assertTrue(actualData.isGlobCollectionEnabled());
    Assert.assertEquals(actualData.getUpdatedUrisName(), Collections.singleton(URI1));
    Assert.assertTrue(actualData.getRemovedUrisName().isEmpty());

    // create Uri2 and delete uri1
    fixture._xdsClientImpl.handleResponse(RESPONSE_D2URI2_DELETE_URI1);
    actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA2
    D2URIMapUpdate expectedUpdate = new D2URIMapUpdate(Collections.singletonMap(URI2, D2URI_2), true);
    verify(fixture._resourceWatcher).onChanged(expectedUpdate);
    verify(fixture._wildcardResourceWatcher).onChanged(CLUSTER_RESOURCE_NAME, expectedUpdate);
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    actualData = (D2URIMapUpdate) fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME);
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    Assert.assertTrue(actualData.isGlobCollectionEnabled());
    Assert.assertEquals(actualData.getUpdatedUrisName(), Collections.singleton(URI2));
    Assert.assertEquals(actualData.getRemovedUrisName(), Collections.singleton(URI1));
    fixture.verifyAckSent(2);

    // Finally sanity check that the client correctly handles the deletion of the final URI in the collection
    DiscoveryResponseData deleteUri2 =
        new DiscoveryResponseData(D2_URI, null, Collections.singletonList(URI_URN2), NONCE, null);
    fixture._xdsClientImpl.handleResponse(deleteUri2);
    actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    // subscriber data should be updated to empty map
    expectedUpdate = new D2URIMapUpdate(Collections.emptyMap(), true);
    verify(fixture._resourceWatcher).onChanged(expectedUpdate);
    verify(fixture._wildcardResourceWatcher).onChanged(CLUSTER_RESOURCE_NAME, expectedUpdate);
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    actualData = (D2URIMapUpdate) fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME);
    Assert.assertEquals(actualData.getURIMap(), expectedUpdate.getURIMap());
    Assert.assertTrue(actualData.isGlobCollectionEnabled());
    Assert.assertTrue(actualData.getUpdatedUrisName().isEmpty());
    Assert.assertEquals(actualData.getRemovedUrisName(), Collections.singleton(URI2));
    fixture.verifyAckSent(3);
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
    verify(fixture._resourceWatcher, times(toWatchIndividual ? 1 : 0)).onChanged(D2_URI_MAP.emptyData());
    verify(fixture._wildcardResourceWatcher, times(toWatchWildcard ? 1 : 0))
        .onChanged(any(), eq(D2_URI_MAP.emptyData()));
    verifyZeroInteractions(fixture._serverMetricsProvider);

    // current data is not null, bad data will not overwrite the original valid data and watchers won't be notified.
    fixture._clusterSubscriber.setData(D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    fixture._uriMapWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(badData);
    fixture.verifyNackSent(2);
    verify(fixture._resourceWatcher, times(0)).onChanged(D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    verify(fixture._wildcardResourceWatcher, times(0))
        .onChanged(any(), eq(D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1));
    verifyZeroInteractions(fixture._serverMetricsProvider);
    Assert.assertEquals(fixture._clusterSubscriber.getData(), D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    // Verify that bad data doesn't affect the updated and removed URIs
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    Assert.assertTrue(actualData.isGlobCollectionEnabled());
    Assert.assertTrue(actualData.getUpdatedUrisName().isEmpty());
    Assert.assertTrue(actualData.getRemovedUrisName().isEmpty());
  }

  @Test
  public void testHandleD2URICollectionResponseWithRemoval()
  {
    DiscoveryResponseData removeClusterResponse =
        new DiscoveryResponseData(D2_URI, null, Collections.singletonList(CLUSTER_GLOB_COLLECTION), NONCE, null);

    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchAllResourceAndWatcherTypes();
    fixture._clusterSubscriber.setData(D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    fixture._uriMapWildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    fixture._xdsClientImpl.handleResponse(removeClusterResponse);
    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher).onChanged(D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    verify(fixture._wildcardResourceWatcher).onRemoval(CLUSTER_RESOURCE_NAME);
    verify(fixture._clusterSubscriber).onRemoval();
    verify(fixture._uriMapWildcardSubscriber).onRemoval(CLUSTER_RESOURCE_NAME);
    verifyZeroInteractions(fixture._serverMetricsProvider);
    // removed resource will not overwrite the original valid data
    D2URIMapUpdate actualData = (D2URIMapUpdate) fixture._clusterSubscriber.getData();
    Assert.assertEquals(actualData, D2_URI_MAP_GLOB_COLLECTION_UPDATE_WITH_DATA1);
    Assert.assertNull(fixture._uriMapWildcardSubscriber.getData(CLUSTER_RESOURCE_NAME));
    Assert.assertTrue(actualData.isGlobCollectionEnabled());
    Assert.assertTrue(actualData.getUpdatedUrisName().isEmpty());
    Assert.assertTrue(actualData.getRemovedUrisName().isEmpty());
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
    verify(watcher, times(10)).onChanged(update);

    WildcardResourceSubscriber wildcardSubscriber = new WildcardResourceSubscriber(D2_CLUSTER_OR_SERVICE_NAME);
    XdsClient.WildcardResourceWatcher _wildcardWatcher = Mockito.mock(XdsClient.WildcardResourceWatcher.class);
    wildcardSubscriber.addWatcher(_wildcardWatcher);
    verify(_wildcardWatcher, times(0)).onChanged(any(), any());

    wildcardSubscriber.setData(CLUSTER_RESOURCE_NAME, CLUSTER_NAME_DATA_UPDATE);
    for (int i = 0; i < 10; i++)
    {
      wildcardSubscriber.addWatcher(_wildcardWatcher);
    }
    verify(_wildcardWatcher, times(10)).onChanged(CLUSTER_RESOURCE_NAME, CLUSTER_NAME_DATA_UPDATE);
  }

  @DataProvider(name = "provideUseGlobCollectionAndIRV")
  public Object[][] provideUseGlobCollectionAndIRV()
  {
    // {
    //   useGlobCollection --- whether to use glob collection
    // }
    return new Object[][]
        {
            {true, true},
            {true, false},
            {false, true},
            {false, false}
    };
  }

  @Test(dataProvider = "provideUseGlobCollectionAndIRV", timeOut = 2000)
  // Retry task should re-subscribe the resources registered in each subscriber type.
  public void testRetry(boolean useGlobCollection, boolean useIRV) throws ExecutionException, InterruptedException
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture(useGlobCollection, useIRV);
    fixture.watchAllResourceAndWatcherTypes();
    fixture._xdsClientImpl.testRetryTask(fixture._adsStream);
    fixture._xdsClientImpl._retryRpcStreamFuture.get();

    // get all the resource types and names sent in the discovery requests and verify them
    List<ResourceType> types = fixture._resourceTypesArgumentCaptor.getAllValues();
    List<Collection<String>> nameLists = fixture._resourceNamesArgumentCaptor.getAllValues();

    Map<ResourceType, Set<String>> resourceNames = new HashMap<>();
    for (int i = 0; i < types.size(); i++)
    {
      resourceNames.computeIfAbsent(types.get(i), k -> new HashSet<>()).addAll(nameLists.get(i));
    }

    Assert.assertEquals(resourceNames.get(NODE), ImmutableSet.of(SERVICE_RESOURCE_NAME, "*"));
    Assert.assertEquals(resourceNames.get(D2_CLUSTER_OR_SERVICE_NAME), ImmutableSet.of("*"));
    if (useGlobCollection)
    {
      Assert.assertEquals(resourceNames.get(D2_URI), ImmutableSet.of(CLUSTER_GLOB_COLLECTION, URI_URN1, "*"));
    }
    else
    {
      Assert.assertEquals(resourceNames.get(D2_URI), ImmutableSet.of(URI_URN1));
      Assert.assertEquals(resourceNames.get(D2_URI_MAP), ImmutableSet.of(CLUSTER_RESOURCE_NAME, "*"));
    }

    List<Map<String, String>> resourceVersions = fixture._resourceVersionsArgumentCaptor.getAllValues();
    if (useIRV)
    {
      Assert.assertEquals(resourceVersions, useGlobCollection ?
          Arrays.asList(ImmutableMap.of(SERVICE_RESOURCE_NAME, VERSION1),
              ImmutableMap.of(
                  CLUSTER_RESOURCE_NAME, VERSION1,
                  URI_URN1, VERSION1
              ),
              ImmutableMap.of(
                  CLUSTER_RESOURCE_NAME, VERSION1,
                  URI_URN1, VERSION1
              ),
              ImmutableMap.of(CLUSTER_NAME, VERSION1)) :
          Arrays.asList(
              ImmutableMap.of(SERVICE_RESOURCE_NAME, VERSION1),
              ImmutableMap.of(CLUSTER_RESOURCE_NAME, VERSION1),
              ImmutableMap.of(URI_URN1, VERSION1),
              ImmutableMap.of(CLUSTER_NAME, VERSION1)));
    }
    else
    {
      resourceVersions.forEach(x -> Assert.assertEquals(x.size(), 0));
    }
  }

  @Test
  public void testUpdateResourceVersions()
  {
    // validate resource version update
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    Assert.assertTrue(fixture._resourceVersions.get(NODE).containsKey(SERVICE_RESOURCE_NAME)
        && fixture._resourceVersions.get(NODE).get(SERVICE_RESOURCE_NAME).equals(VERSION1));

    // validate, node with null resource fields in response, updates the resource version coming in response.
    DiscoveryResponseData responseWithoutResource =
        new DiscoveryResponseData(
            NODE,
            Collections.singletonList(Resource.newBuilder().setVersion(VERSION3).setName(SERVICE_RESOURCE_NAME)
                // not set resource field
                .build()),
            null,
            NONCE,
            null);

    fixture._xdsClientImpl.handleResponse(responseWithoutResource);
    Assert.assertTrue(fixture._resourceVersions.get(NODE).containsKey(SERVICE_RESOURCE_NAME)
        && fixture._resourceVersions.get(NODE).get(SERVICE_RESOURCE_NAME).equals(VERSION3));

    // validate, node with empty resources in response, updates the resource version coming in response.
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_NULL_DATA_IN_RESOURCE_FIELD);
    Assert.assertTrue(fixture._resourceVersions.get(NODE).containsKey(SERVICE_RESOURCE_NAME)
        && fixture._resourceVersions.get(NODE).get(SERVICE_RESOURCE_NAME).equals(VERSION1));

    // validate that empty node response, does not change the resource version
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_WITH_EMPTY_NODE_RESPONSE);
    Assert.assertTrue(fixture._resourceVersions.get(NODE).containsKey(SERVICE_RESOURCE_NAME)
        && fixture._resourceVersions.get(NODE).get(SERVICE_RESOURCE_NAME).equals(VERSION1));


    // validate SERVICE_RESOURCE_NAME version updates in resource version map
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA2);
    Assert.assertTrue(fixture._resourceVersions.get(NODE).containsKey(SERVICE_RESOURCE_NAME)
        && fixture._resourceVersions.get(NODE).get(SERVICE_RESOURCE_NAME).equals(VERSION2));

    // validate that removed resources in response, removed the resource version from the map
    DiscoveryResponseData removeServiceResponse =
        new DiscoveryResponseData(NODE, null, Collections.singletonList(SERVICE_RESOURCE_NAME), NONCE, null);
    fixture._xdsClientImpl.handleResponse(removeServiceResponse);
    Assert.assertFalse(fixture._resourceVersions.get(NODE).containsKey(SERVICE_RESOURCE_NAME));
  }

  @Test
  public void testWatchD2Uri()
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture();
    fixture.watchUriResource();

    DiscoveryResponseData badD2URIUpdate = new DiscoveryResponseData(
        D2_URI,
        Collections.singletonList(Resource.newBuilder()
            .setVersion("bad")
            .setName(URI_URN1)
            // This has no resource data
            .build()),
        null,
        NONCE,
        null
    );

    fixture._xdsClientImpl.handleResponse(badD2URIUpdate);
    fixture.verifyNackSent(1);
    // If there was no previous data, but an invalid D2URI was received, the watcher should be notified of a deletion.
    verify(fixture._resourceWatcher, times(1)).onChanged(new XdsClient.D2URIUpdate(null));

    // URI added
    fixture._xdsClientImpl.handleResponse(new DiscoveryResponseData(
        D2_URI,
        Collections.singletonList(Resource.newBuilder()
            .setVersion("123")
            .setName(URI_URN1)
            .setResource(Any.pack(D2URI_1))
            .build()),
        null,
        NONCE,
        null
    ));

    fixture.verifyAckSent(1);
    verify(fixture._resourceWatcher, times(1)).onChanged(new XdsClient.D2URIUpdate(D2URI_1));

    // Send the bad data again, here there should be no interactions with the watcher.
    fixture._xdsClientImpl.handleResponse(badD2URIUpdate);
    fixture.verifyNackSent(2);
    // times(2) is used here since the mock was interacted with exactly twice.
    verify(fixture._resourceWatcher, times(2)).onChanged(any());

    // URI deleted
    fixture._xdsClientImpl.handleResponse(new DiscoveryResponseData(
        D2_URI,
        null,
        Collections.singletonList(URI_URN1),
        NONCE,
        null
    ));

    fixture.verifyAckSent(2);
    // times(2) is used here since there was a previous interaction where the D2URIUpdate was null.
    verify(fixture._resourceWatcher, times(2)).onChanged(new XdsClient.D2URIUpdate(null));
  }

  @DataProvider(name = "provideIrvEnabled")
  public Object[][] provideIrvEnabled()
  {
    // Params:
    //    isIrvEnabled --- whether IRV is enabled in the XdsClientImpl
    return new Object[][]
        {
            {true},
            {false}
        };
  }
  @Test(dataProvider = "provideIrvEnabled")
  public void testTrackLatencyForD2Node(boolean isIrvEnabled)
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture(true, isIrvEnabled);
    fixture.watchNodeResource();
    fixture.watchNodeResourceViaWildcard();
    XdsServerMetricsProvider metrics = fixture._serverMetricsProvider;

    // first fetch, subscribe time is after modified time, so the latency is calculated with subscribe time
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA1);
    verifyPositiveLagLessThanOneSec(2, metrics);
    fixture._nodeWildcardSubscriber.onAllResourcesProcessed(); // end of first fetch

    // Update data. Verify latency is tracked with modified time only if irv is enabled.
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA2);
    if (isIrvEnabled)
    {
      verifyLagGreaterThanTenMins(2, metrics);
    }
    else
    {
      verifyPositiveLagLessThanOneSec(4, metrics);
    }

    // set subscribedAt to 10min ago
    fixture._nodeSubscriber.setSubscribedAt(SystemClock.instance().currentTimeMillis() - TEN_MINS);
    fixture._nodeWildcardSubscriber.setSubscribedAt(SystemClock.instance().currentTimeMillis() - TEN_MINS);
    // set resource modified time to 200ms ago, verify the latency is calculated with modified time
    XdsD2.Node nodeChangedAfter = createNodeWithData(DATA,
        SystemClock.instance().currentTimeMillis() - Time.milliseconds(200));
    List<Resource> resources = Collections.singletonList(
        Resource.newBuilder().setVersion("3").setName(SERVICE_RESOURCE_NAME).setResource(Any.pack(nodeChangedAfter))
            .build());
    fixture._xdsClientImpl.handleResponse(new DiscoveryResponseData(NODE, resources, null, NONCE, null));
    verifyPositiveLagLessThanOneSec(isIrvEnabled ? 4 : 6, metrics);

    // simulate onReconnect call, will reset subscribedAt to current time. Verify latency is tracked with
    // modified time only if irv is enabled.
    fixture._nodeSubscriber.onReconnect();
    fixture._nodeWildcardSubscriber.onReconnect();
    fixture._xdsClientImpl.handleResponse(DISCOVERY_RESPONSE_NODE_DATA2);
    if (isIrvEnabled)
    {
      verifyLagGreaterThanTenMins(4, metrics);
    }
    else
    {
      verifyPositiveLagLessThanOneSec(8, metrics);
    }
  }

  @Test(dataProvider = "provideIrvEnabled")
  public void testTrackLatencyForD2URI(boolean isIrvEnabled)
  {
    XdsClientImplFixture fixture = new XdsClientImplFixture(true, isIrvEnabled);
    XdsServerMetricsProvider metrics = fixture._serverMetricsProvider;
    fixture.watchAllResourceAndWatcherTypes();

    // first fetch, subscribe time is after modified time, so the latency is calculated with subscribe time
    fixture._xdsClientImpl.handleResponse(RESPONSE_D2URI1);
    verifyPositiveLagLessThanOneSec(3, metrics);
    fixture._uriMapWildcardSubscriber.onAllResourcesProcessed(); // end of first fetch

    // Add uri2 delete uri1. Verify latency is tracked for uri2 only (deleting uri not tracked), and tracked with
    // modified time only if irv is enabled. Also since there is no D2_URI individual resource subscriber on URI2
    // (only on URI1), only two D2_URI_MAP subscribers will track latency
    fixture._xdsClientImpl.handleResponse(RESPONSE_D2URI2_DELETE_URI1);
    if (isIrvEnabled)
    {
      verifyLagGreaterThanTenMins(2, metrics);
    } else
    {
      verifyPositiveLagLessThanOneSec(5, metrics);
    }

    // set subscribedAt to 10min ago
    fixture._clusterSubscriber.setSubscribedAt(SystemClock.instance().currentTimeMillis() - TEN_MINS);
    fixture._uriMapWildcardSubscriber.setSubscribedAt(SystemClock.instance().currentTimeMillis() - TEN_MINS);
    // set resource modified time to 200ms ago, verify the latency is calculated with modified time
    XdsD2.D2URI uriChangedAfter = createD2URIWithData(URI2, CLUSTER_NAME, VERSION2, Collections.singletonMap(0, 2.0),
        SystemClock.instance().currentTimeMillis() - Time.milliseconds(200));
    List<Resource> resources = Collections.singletonList(
        Resource.newBuilder().setVersion(VERSION2).setName(URI_URN2).setResource(Any.pack(uriChangedAfter)).build());
    fixture._xdsClientImpl.handleResponse(new DiscoveryResponseData(D2_URI, resources, null, NONCE, null));
    verifyPositiveLagLessThanOneSec(isIrvEnabled ? 5 : 7, metrics);

    // simulate onReconnect call, will reset subscribedAt to current time. Verify latency is tracked with
    // modified time only if irv is enabled.
    fixture._d2UriSubscriber.onReconnect();
    fixture._clusterSubscriber.onReconnect();
    fixture._uriMapWildcardSubscriber.onReconnect();
    // Now create URI1 again, with a modified time before the subscribe time. Verify the latency is calculated with
    // modified time only if irv is disabled
    fixture._xdsClientImpl.handleResponse(RESPONSE_D2URI1);
    if (isIrvEnabled)
    {
      verifyLagGreaterThanTenMins(5, metrics);
    } else
    {
      verifyPositiveLagLessThanOneSec(10, metrics);
    }
  }

  private static XdsD2.Node createNodeWithData(byte[] data, long mtimeMillis)
  {
    return XdsD2.Node.newBuilder()
        .setData(ByteString.copyFrom(data))
        .setStat(XdsD2.Stat.newBuilder().setMtime(mtimeMillis).build())
        .build();
  }

  private static XdsD2.D2URI createD2URIWithData(String uri, String clusterName, String version,
      Map<Integer, Double> partitionDesc, long modifiedTimeMillis)
  {
    XdsD2.D2URI.Builder builder = XdsD2.D2URI.newBuilder()
        .setVersion(Long.parseLong(version))
        .setClusterName(clusterName)
        .setUri(uri)
        .setModifiedTime(Timestamp.newBuilder()
            .setSeconds(modifiedTimeMillis / 1000)
            .setNanos((int) (modifiedTimeMillis % 1000 * 1_000_000))
            .build());
    if (partitionDesc != null)
    {
      builder.putAllPartitionDesc(partitionDesc);
    }
    return builder.build();
  }

  private static void verifyPositiveLagLessThanOneSec(int times, XdsServerMetricsProvider serverMetricsProvider)
  {
    verify(serverMetricsProvider, times(times))
        .trackLatency(longThat(new PositiveLagLessThan(Time.seconds(1))));
  }

  private static void verifyLagGreaterThanTenMins(int times, XdsServerMetricsProvider serverMetricsProvider)
  {
    verify(serverMetricsProvider, times(times))
        .trackLatency(longThat(new GreaterThan<>(TEN_MINS)));
  }

  private static final class PositiveLagLessThan extends ArgumentMatcher<Long>
  {
    private final long _threshold;

    PositiveLagLessThan(long threshold)
    {
      _threshold = threshold;
    }

    // negative deviation < arg < threshold + deviation
    @Override
    public boolean matches(Object arg)
    {
      return arg != null && (Long) arg > 0 && (Long) arg < _threshold;
    }
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
    ResourceSubscriber _d2UriSubscriber;
    XdsClientImpl.WildcardResourceSubscriber _nodeWildcardSubscriber;
    XdsClientImpl.WildcardResourceSubscriber _uriMapWildcardSubscriber;
    XdsClientImpl.WildcardResourceSubscriber _nameWildcardSubscriber;
    Map<ResourceType, Map<String, ResourceSubscriber>> _subscribers = Maps.immutableEnumMap(
        Stream.of(ResourceType.values())
            .collect(Collectors.toMap(Function.identity(), e -> new HashMap<>())));
    Map<ResourceType, XdsClientImpl.WildcardResourceSubscriber> _wildcardSubscribers = Maps.newEnumMap(ResourceType.class);

    private final Map<ResourceType, Map<String, String>> _resourceVersions = Maps.newEnumMap(
        Stream.of(ResourceType.values()).collect(Collectors.toMap(Function.identity(), e -> new HashMap<>())));

    @Mock
    XdsClient.ResourceWatcher _resourceWatcher;
    @Mock
    XdsClient.WildcardResourceWatcher _wildcardResourceWatcher;
    @Mock
    XdsServerMetricsProvider _serverMetricsProvider;
    @Mock
    Clock _clock;

    @Captor
    ArgumentCaptor<ResourceType> _resourceTypesArgumentCaptor;
    @Captor
    ArgumentCaptor<Collection<String>> _resourceNamesArgumentCaptor;

    @Captor
    ArgumentCaptor<Map<String, String>> _resourceVersionsArgumentCaptor;

    ScheduledExecutorService _executorService;

    XdsClientImplFixture()
    {
      this(false, false);
    }

    XdsClientImplFixture(boolean useGlobCollections, boolean useIRV)
    {
      MockitoAnnotations.initMocks(this);
      // Make sure subscribedAt time is before current time, in other words the response handle time, in the tests
      when(_clock.currentTimeMillis()).thenAnswer(invocation -> System.currentTimeMillis() - 200);
      _nodeSubscriber = spy(new ResourceSubscriber(NODE, SERVICE_RESOURCE_NAME, _xdsClientJmx, _clock));
      _clusterSubscriber = spy(new ResourceSubscriber(D2_URI_MAP, CLUSTER_RESOURCE_NAME, _xdsClientJmx, _clock));
      _d2UriSubscriber = spy(new ResourceSubscriber(D2_URI, URI_URN1, _xdsClientJmx, _clock));
      _nodeWildcardSubscriber = spy(new WildcardResourceSubscriber(NODE, _clock));
      _uriMapWildcardSubscriber = spy(new WildcardResourceSubscriber(D2_URI_MAP, _clock));
      _nameWildcardSubscriber = spy(new WildcardResourceSubscriber(D2_CLUSTER_OR_SERVICE_NAME, _clock));

      doNothing().when(_resourceWatcher).onChanged(any());
      doNothing().when(_wildcardResourceWatcher).onChanged(any(), any());
      doNothing().when(_serverMetricsProvider).trackLatency(anyLong());

      for (ResourceSubscriber subscriber : Lists.newArrayList(_nodeSubscriber, _clusterSubscriber, _d2UriSubscriber))
      {
        _subscribers.get(subscriber.getType()).put(subscriber.getResource(), subscriber);
      }
      for (WildcardResourceSubscriber subscriber : Lists.newArrayList(_nodeWildcardSubscriber,
          _uriMapWildcardSubscriber, _nameWildcardSubscriber))
      {
        _wildcardSubscribers.put(subscriber.getType(), subscriber);
      }
      if (useIRV)
      {
        setResourceVersions(useGlobCollections);
      }

      _executorService = spy(Executors.newScheduledThreadPool(1));

      _xdsClientImpl = spy(new XdsClientImpl(null, null,
          _executorService,
          0, useGlobCollections, _serverMetricsProvider, useIRV));
      _xdsClientImpl._adsStream = _adsStream;

      doNothing().when(_xdsClientImpl).startRpcStreamLocal();
      doNothing().when(_xdsClientImpl).sendAckOrNack(any(), any(), any());

      doNothing().when(_adsStream).sendDiscoveryRequest(_resourceTypesArgumentCaptor.capture(),
          _resourceNamesArgumentCaptor.capture(),
          _resourceVersionsArgumentCaptor.capture());

      when(_xdsClientImpl.getXdsClientJmx()).thenReturn(_xdsClientJmx);
      when(_xdsClientImpl.getResourceSubscribers()).thenReturn(_subscribers);
      when(_xdsClientImpl.getResourceVersions()).thenReturn(_resourceVersions);
      when(_xdsClientImpl.getWildcardResourceSubscribers()).thenReturn(_wildcardSubscribers);
    }

    private void setResourceVersions(boolean useGlobCollections)
    {
      _resourceVersions.computeIfAbsent(NODE, k -> new HashMap<>()).put(SERVICE_RESOURCE_NAME, VERSION1);
      _resourceVersions.computeIfAbsent(D2_CLUSTER_OR_SERVICE_NAME, k -> new HashMap<>()).put(CLUSTER_NAME, VERSION1);
      _resourceVersions.computeIfAbsent(D2_URI, k -> new HashMap<>()).put(URI_URN1, VERSION1);
      if (useGlobCollections)
      {
        _resourceVersions.computeIfAbsent(D2_URI, k -> new HashMap<>()).put(CLUSTER_RESOURCE_NAME, VERSION1);
      }
      else
      {
        _resourceVersions.computeIfAbsent(D2_URI_MAP, k -> new HashMap<>()).put(CLUSTER_RESOURCE_NAME, VERSION1);
      }
    }

    void watchAllResourceAndWatcherTypes()
    {
      for (ResourceSubscriber subscriber : Lists.newArrayList(_nodeSubscriber, _clusterSubscriber, _d2UriSubscriber))
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

    void watchUriResource()
    {
      _d2UriSubscriber.addWatcher(_resourceWatcher);
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
