package com.linkedin.d2.util;

import com.google.common.collect.ImmutableMap;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.discovery.event.D2ServiceDiscoveryEventHelper;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;

import static org.testng.Assert.*;


public class TestDataHelper {
  public static final String CLUSTER_NAME = "TestCluster";
  public static final String HOST_1 = "google.com";
  public static final String HOST_2 = "linkedin.com";
  public static final String HOST_3 = "youtube.com";
  public static final String HOST_4 = "facebook.com";
  public static final int PORT_1 = 1;
  public static final int PORT_2 = 2;
  public static final int PORT_3 = 3;
  public static final int PORT_4 = 4;
  public static final URI URI_1 = URI.create("http://" + HOST_1 + ":" + PORT_1);
  public static final URI URI_2 = URI.create("http://" + HOST_2 + ":" + PORT_2);
  public static final URI URI_3 = URI.create("https://" + HOST_3 + ":" + PORT_3);
  public static final URI URI_4 = URI.create("https://" + HOST_4 + ":" + PORT_4);

  public static final UriProperties PROPERTIES_1;
  public static final UriProperties PROPERTIES_2;
  public static final UriProperties PROPERTIES_3;
  public static final UriProperties PROPERTIES_4;

  public static final Map<Integer, PartitionData> MAP_1 = ImmutableMap.of(
    0, new PartitionData(1),
    1, new PartitionData(2));

  public static final Map<Integer, PartitionData> MAP_2 = Collections.singletonMap(1, new PartitionData(0.5));

  public static final Map<Integer, PartitionData> MAP_3 = ImmutableMap.of(
    1, new PartitionData(2),
    3, new PartitionData(3.5),
    4, new PartitionData(1));

  public static final Map<Integer, PartitionData> MAP_4 = ImmutableMap.of(
    0, new PartitionData(1),
    1, new PartitionData(3));

  static {
    PROPERTIES_1 = new UriProperties(CLUSTER_NAME, Collections.singletonMap(URI_1, MAP_1));
    PROPERTIES_2 = new UriProperties(CLUSTER_NAME, Collections.singletonMap(URI_2, MAP_2));
    PROPERTIES_3 = new UriProperties(CLUSTER_NAME, Collections.singletonMap(URI_3, MAP_3));
    PROPERTIES_4 = new UriProperties(CLUSTER_NAME, Collections.singletonMap(URI_4, MAP_4));
  }


  public static MockD2ServiceDiscoveryEventHelper getMockD2ServiceDiscoveryEventHelper() {
    return new MockD2ServiceDiscoveryEventHelper();
  }

  public static MockServiceDiscoveryEventEmitter getMockServiceDiscoveryEventEmitter() {
    return new MockServiceDiscoveryEventEmitter();
  }

  public static class MockD2ServiceDiscoveryEventHelper implements D2ServiceDiscoveryEventHelper {
    public final List<String> _activeUpdateIntentAndWriteClusters = new ArrayList<>();
    public final List<Boolean> _activeUpdateIntentAndWriteIsMarkUpFlags = new ArrayList<>();
    public final List<Boolean> _activeUpdateIntentAndWriteSucceededFlags = new ArrayList<>();

    @Override
    public void emitSDStatusActiveUpdateIntentAndWriteEvents(String cluster, boolean isMarkUp, boolean succeeded,
        long startAt) {
      _activeUpdateIntentAndWriteClusters.add(cluster);
      _activeUpdateIntentAndWriteIsMarkUpFlags.add(isMarkUp);
      _activeUpdateIntentAndWriteSucceededFlags.add(succeeded);
    }

    public void verifySDStatusActiveUpdateIntentAndWriteEvents(List<String> clusters, List<Boolean> isMarkUpFlags, List<Boolean> succeededFlags) {
      assertEquals(clusters, _activeUpdateIntentAndWriteClusters, "incorrect clusters");
      assertEquals(isMarkUpFlags, _activeUpdateIntentAndWriteIsMarkUpFlags, "incorrect isMarkUp flags");
      assertEquals(succeededFlags, _activeUpdateIntentAndWriteSucceededFlags, "incorrect succeeded flags");
    }
  }

  public static class MockServiceDiscoveryEventEmitter implements ServiceDiscoveryEventEmitter {
    public final List<List<String>> _clustersClaimedList = new ArrayList<>();
    public final List<StatusUpdateActionType> _activeUpdateIntentActionTypes = new ArrayList<>();
    public final List<String> _activeUpdateIntentTracingIds = new ArrayList<>();

    public final List<String> _writeClusters = new ArrayList<>();
    public final List<String> _writeHosts = new ArrayList<>();
    public final List<StatusUpdateActionType> _writeActionTypes = new ArrayList<>();
    public final List<String> _writeServiceRegistryKeys = new ArrayList<>();
    public final List<String> _writeServiceRegistryValues = new ArrayList<>();
    public final List<Integer> _writeServiceRegistryVersions = new ArrayList<>();
    public final List<String> _writeTracingIds = new ArrayList<>();
    public final List<Boolean> _writeSucceededFlags = new ArrayList<>();

    public final Set<String> _receiptMarkUpClusters = new HashSet<>();
    public final Set<String> _receiptMarkUpHosts = new HashSet<>();
    public final Set<Integer> _receiptMarkUpPorts = new HashSet<>();
    public final Set<String> _receiptMarkUpPaths = new HashSet<>();
    public final Set<String> _receiptMarkUpProperties = new HashSet<>();
    public final Set<String> _receiptMarkUpTracingIds = new HashSet<>();

    public final Set<String> _receiptMarkDownClusters = new HashSet<>();
    public final Set<String> _receiptMarkDownHosts = new HashSet<>();
    public final Set<Integer> _receiptMarkDownPorts = new HashSet<>();
    public final Set<String> _receiptMarkDownPaths = new HashSet<>();
    public final Set<String> _receiptMarkDownProperties = new HashSet<>();
    public final Set<String> _receiptMarkDownTracingIds = new HashSet<>();

    public final List<String> _initialRequestClusters = new ArrayList<>();
    public final List<Long> _initialRequestDurations = new ArrayList<>();
    public final List<Boolean> _initialRequestSucceededFlags = new ArrayList<>();

    @Override
    public void emitSDStatusActiveUpdateIntentEvent(List<String> clustersClaimed, StatusUpdateActionType actionType,
        boolean isNextGen, String tracingId, long timestamp) {
      _clustersClaimedList.add(clustersClaimed);
      _activeUpdateIntentActionTypes.add(actionType);
      _activeUpdateIntentTracingIds.add(tracingId);
    }

    @Override
    public void emitSDStatusWriteEvent(String cluster, String host, int port, StatusUpdateActionType actionType,
        String serviceRegistry, String serviceRegistryKey, String serviceRegistryValue, Integer serviceRegistryVersion,
        String tracingId, boolean succeeded, long timestamp) {
      _writeClusters.add(cluster);
      _writeHosts.add(host);
      _writeActionTypes.add(actionType);
      _writeServiceRegistryKeys.add(serviceRegistryKey);
      _writeServiceRegistryValues.add(serviceRegistryValue);
      _writeServiceRegistryVersions.add(serviceRegistryVersion);
      _writeTracingIds.add(tracingId);
      _writeSucceededFlags.add(succeeded);
    }

    @Override
    public void emitSDStatusUpdateReceiptEvent(String cluster, String host, int port, StatusUpdateActionType actionType,
        boolean isNextGen, String serviceRegistry, String serviceRegistryKey, String serviceRegistryValue,
        Integer serviceRegistryVersion, String tracingId, long timestamp) {
      if (actionType == StatusUpdateActionType.MARK_READY) {
        _receiptMarkUpClusters.add(cluster);
        _receiptMarkUpHosts.add(host);
        _receiptMarkUpPorts.add(port);
        _receiptMarkUpPaths.add(serviceRegistryKey);
        _receiptMarkUpProperties.add(serviceRegistryValue);
        _receiptMarkUpTracingIds.add(tracingId);
      } else if (actionType == StatusUpdateActionType.MARK_DOWN){
        _receiptMarkDownClusters.add(cluster);
        _receiptMarkDownHosts.add(host);
        _receiptMarkDownPorts.add(port);
        _receiptMarkDownPaths.add(serviceRegistryKey);
        _receiptMarkDownProperties.add(serviceRegistryValue);
        _receiptMarkDownTracingIds.add(tracingId);
      } else {
        Assert.fail("Invalid action type in status update receipt. In D2, status update received should be either MARK_READY or MARK_DOWN.");
      }
      assertFalse(isNextGen);
      assertEquals(serviceRegistryVersion.intValue(), 0);
    }

    @Override
    public void emitSDStatusInitialRequestEvent(String cluster, boolean isNextGen, long duration, boolean succeeded) {
      _initialRequestClusters.add(cluster);
      _initialRequestDurations.add(duration);
      _initialRequestSucceededFlags.add(succeeded);
      assertFalse(isNextGen);
    }

    public void verifySDStatusActiveUpdateIntentEvents(List<List<String>> clustersClaimedList, List<StatusUpdateActionType> actionTypes,
        List<String> tracingIds) {
      assertEquals(clustersClaimedList, _clustersClaimedList, "incorrect clustersClaimedList");
      assertEquals(actionTypes, _activeUpdateIntentActionTypes, "incorrect action types");
      assertEquals(tracingIds, _activeUpdateIntentTracingIds, "incorrect tracing ids");
    }

    public void verifySDStatusWriteEvents(List<String> clusters, List<String> hosts, List<StatusUpdateActionType> actionTypes, List<String> serviceRegistryKeys,
        List<String> serviceRegistryValues, List<Integer> serviceRegistryVersions, List<String> tracingIds, List<Boolean> succeededFlags) {
      assertEquals(clusters, _writeClusters, "incorrect clusters");
      assertEquals(hosts, _writeHosts, "incorrect hosts");
      assertEquals(actionTypes, _writeActionTypes, "incorrect actionTypes");
      assertEquals(serviceRegistryKeys, _writeServiceRegistryKeys, "incorrect serviceRegistryKeys");
      assertEquals(serviceRegistryValues, _writeServiceRegistryValues, "incorrect serviceRegistryValues");
      assertEquals(serviceRegistryVersions, _writeServiceRegistryVersions, "incorrect serviceRegistryVersions");
      assertEquals(tracingIds, _writeTracingIds, "incorrect tracingIds");
      assertEquals(succeededFlags, _writeSucceededFlags, "incorrect succeededFlags");
    }

    public void verifySDStatusUpdateReceiptEvents(Set<String> clusters, Set<String> hosts, Set<Integer> ports,
        Set<String> nodePaths, Set<String> properties, Set<String> tracingIds, boolean isForMarkUp) {
      assertEquals(clusters, isForMarkUp ? _receiptMarkUpClusters : _receiptMarkDownClusters, "incorrect clusters");
      assertEquals(hosts, isForMarkUp ? _receiptMarkUpHosts : _receiptMarkDownHosts, "incorrect hosts");
      assertEquals(ports, isForMarkUp ? _receiptMarkUpPorts : _receiptMarkDownPorts, "incorrect ports");
      assertEquals(nodePaths, isForMarkUp ? _receiptMarkUpPaths : _receiptMarkDownPaths, "incorrect node paths");
      assertEquals(properties, isForMarkUp ? _receiptMarkUpProperties : _receiptMarkDownProperties, "incorrect node properties");
      assertEquals(tracingIds, isForMarkUp ? _receiptMarkUpTracingIds : _receiptMarkDownTracingIds, "incorrect tracing ids");
    }

    public void verifySDStatusInitialRequestEvents(List<String> clusters, List<Boolean> succeededFlags) {
      assertEquals(clusters, _initialRequestClusters, "incorrect clusters");
      // the duration could be 0 when requests took < 1ms,
      _initialRequestDurations.forEach(duration -> assertTrue(duration >= 0, "incorrect durations"));
      assertEquals(succeededFlags, _initialRequestSucceededFlags, "incorrect succeeded flags");
    }
  }
}
