package com.linkedin.d2.balancer.dualread;

import com.google.common.collect.ImmutableMap;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.r2.util.NamedThreadFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.dualread.UriPropertiesDualReadMonitor.*;
import static com.linkedin.d2.util.TestDataHelper.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


public class UriPropertiesDualReadMonitorTest {
  private static final String CLUSTER_1 = "cluster1";
  private static final String CLUSTER_2 = "cluster2";
  private static final Map<Integer, PartitionData> WEIGHT_1_PARTITION_DATA = ImmutableMap.of(0, new PartitionData(1));
  private static final Map<Integer, PartitionData> WEIGHT_2_PARTITION_DATA = ImmutableMap.of(0, new PartitionData(2));
  private static final Map<String, Object> SIZE_ONE_URI_SPECIFIC_PROPERTIES = ImmutableMap.of("foo", "foo-value");
  private static final Map<String, Object> SIZE_TWO_URI_SPECIFIC_PROPERTIES = ImmutableMap.of(
      "foo", "foo-value",
      "bar", 1);
  private static final UriProperties URI_PROPERTIES_1 = new UriProperties(CLUSTER_1,
      ImmutableMap.of(URI_1, WEIGHT_1_PARTITION_DATA),
      ImmutableMap.of(URI_1, Collections.emptyMap()));

  private static final UriProperties URI_PROPERTIES_2 = new UriProperties(CLUSTER_1,
      ImmutableMap.of(URI_2, WEIGHT_1_PARTITION_DATA),
      ImmutableMap.of(URI_2, Collections.emptyMap()));

  private static final UriProperties URI_PROPERTIES_URI_1_AND_2 = new UriProperties(CLUSTER_1,
      ImmutableMap.of(URI_1, WEIGHT_1_PARTITION_DATA, URI_2, WEIGHT_1_PARTITION_DATA),
      ImmutableMap.of(URI_1, Collections.emptyMap(), URI_2, Collections.emptyMap()));

  private static final UriProperties URI_PROPERTIES_URI_3_AND_4 = new UriProperties(CLUSTER_2,
      ImmutableMap.of(URI_3, WEIGHT_1_PARTITION_DATA, URI_4, WEIGHT_1_PARTITION_DATA),
      ImmutableMap.of(URI_3, Collections.emptyMap(), URI_4, SIZE_ONE_URI_SPECIFIC_PROPERTIES));

  private static final UriProperties URI_PROPERTIES_URI_3_DIFF_SPECIFIC_PROPERTIES_AND_4_DIFF_WEIGHT =
      new UriProperties(CLUSTER_2,
          ImmutableMap.of(URI_3, WEIGHT_1_PARTITION_DATA, URI_4, WEIGHT_2_PARTITION_DATA),
          ImmutableMap.of(URI_3, SIZE_ONE_URI_SPECIFIC_PROPERTIES, URI_4, SIZE_ONE_URI_SPECIFIC_PROPERTIES));

  private static final UriProperties URI_PROPERTIES_URI_3_ANOTHER_DIFF_SPECIFIC_PROPERTIES_AND_4_DIFF_WEIGHT =
      new UriProperties(CLUSTER_2,
          ImmutableMap.of(URI_3, WEIGHT_1_PARTITION_DATA, URI_4, WEIGHT_2_PARTITION_DATA),
          ImmutableMap.of(URI_3, SIZE_TWO_URI_SPECIFIC_PROPERTIES, URI_4, SIZE_ONE_URI_SPECIFIC_PROPERTIES));

  @Test
  public void testReportData() {
    UriPropertiesDualReadMonitorTestFixture fixture = new UriPropertiesDualReadMonitorTestFixture();
    UriPropertiesDualReadMonitor monitor = fixture.getMonitor();

    // new lb has uri 1
    monitor.reportData(CLUSTER_1, URI_PROPERTIES_1, true);
    verifyJmxMetricParams(fixture, CLUSTER_1,
        createMatchRecord(null, URI_PROPERTIES_1, 1, 0),
        0.0);

    // old lb has uri 2
    monitor.reportData(CLUSTER_1, URI_PROPERTIES_2, false);
    verifyJmxMetricParams(fixture, CLUSTER_1,
        createMatchRecord(URI_PROPERTIES_2, URI_PROPERTIES_1, 2, 0),
        0.0);

    // old lb updated with both uri 1 and 2
    monitor.reportData(CLUSTER_1, URI_PROPERTIES_URI_1_AND_2, false);
    verifyJmxMetricParams(fixture, CLUSTER_1,
        createMatchRecord(URI_PROPERTIES_URI_1_AND_2, URI_PROPERTIES_1, 2, 1),
        0.5);

    // new lb updated with both uri 1 and 2
    monitor.reportData(CLUSTER_1, URI_PROPERTIES_URI_1_AND_2, true);
    verifyJmxMetricParams(fixture, CLUSTER_1,
        createMatchRecord(URI_PROPERTIES_URI_1_AND_2, URI_PROPERTIES_URI_1_AND_2, 2, 2),
        1.0);

    // add data for cluster 2, old lb with uri 3 and 4
    monitor.reportData(CLUSTER_2, URI_PROPERTIES_URI_3_AND_4, false);
    assertEquals(monitor.getTotalUris(), 4);
    assertEquals(monitor.getMatchedUris(), 2);
    verifyJmxMetricParams(fixture, CLUSTER_2,
        createMatchRecord(URI_PROPERTIES_URI_3_AND_4, null, 2, 0),
        0.5);

    // new lb updated with uri 3 with different uri specific properties and uri 4 with different weight
    monitor.reportData(CLUSTER_2, URI_PROPERTIES_URI_3_DIFF_SPECIFIC_PROPERTIES_AND_4_DIFF_WEIGHT, true);
    assertEquals(monitor.getTotalUris(), 4);
    assertEquals(monitor.getMatchedUris(), 2);
    verifyJmxMetricParams(fixture, CLUSTER_2,
        createMatchRecord(URI_PROPERTIES_URI_3_AND_4, URI_PROPERTIES_URI_3_DIFF_SPECIFIC_PROPERTIES_AND_4_DIFF_WEIGHT,
            2, 0),
        0.5);

    // old lb updated with uri 3 with still different uri specific properties and uri 4 with same weight as new lb
    monitor.reportData(CLUSTER_2, URI_PROPERTIES_URI_3_ANOTHER_DIFF_SPECIFIC_PROPERTIES_AND_4_DIFF_WEIGHT, false);
    assertEquals(monitor.getTotalUris(), 4);
    assertEquals(monitor.getMatchedUris(), 3);
    verifyJmxMetricParams(fixture, CLUSTER_2,
        createMatchRecord(URI_PROPERTIES_URI_3_ANOTHER_DIFF_SPECIFIC_PROPERTIES_AND_4_DIFF_WEIGHT,
            URI_PROPERTIES_URI_3_DIFF_SPECIFIC_PROPERTIES_AND_4_DIFF_WEIGHT,
            2, 1),
        0.75);

    // delete both lbs data for cluster 2
    monitor.reportData(CLUSTER_2, null, true);
    monitor.reportData(CLUSTER_2, null, false);
    verifyJmxMetricParams(fixture, CLUSTER_2, null, 1.0);
  }

  @DataProvider
  public Object[][] reportDataInMultiThreadsDataProvider() {
    // simulate different orders that the data is reported between the old and new Lbs.
    return new Object[][]{
        {
            Arrays.asList(
                new ImmutablePair<>(URI_PROPERTIES_1, false),
                new ImmutablePair<>(URI_PROPERTIES_1, true),
                new ImmutablePair<>(URI_PROPERTIES_URI_1_AND_2, false),
                new ImmutablePair<>(URI_PROPERTIES_URI_1_AND_2, true)
            )
        },
        {
            Arrays.asList(
                new ImmutablePair<>(URI_PROPERTIES_1, false),
                new ImmutablePair<>(URI_PROPERTIES_URI_1_AND_2, false),
                new ImmutablePair<>(URI_PROPERTIES_1, true),
                new ImmutablePair<>(URI_PROPERTIES_URI_1_AND_2, true)
            )
        },
        {
            Arrays.asList(
                new ImmutablePair<>(URI_PROPERTIES_1, false),
                new ImmutablePair<>(URI_PROPERTIES_1, true),
                new ImmutablePair<>(URI_PROPERTIES_URI_1_AND_2, true),
                new ImmutablePair<>(URI_PROPERTIES_URI_1_AND_2, false)
            )
        },
        {
            Arrays.asList(
                new ImmutablePair<>(URI_PROPERTIES_1, false),
                new ImmutablePair<>(URI_PROPERTIES_URI_1_AND_2, false),
                new ImmutablePair<>(URI_PROPERTIES_1, true),
                new ImmutablePair<>(URI_PROPERTIES_1, false),
                new ImmutablePair<>(URI_PROPERTIES_URI_1_AND_2, true),
                new ImmutablePair<>(URI_PROPERTIES_1, true)
            )
        }
    };
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test(dataProvider = "reportDataInMultiThreadsDataProvider", invocationCount = 10)
  public void testReportDataInMultiThreads(List<Pair<UriProperties, Boolean>> properties) {
    UriPropertiesDualReadMonitorTestFixture fixture = new UriPropertiesDualReadMonitorTestFixture();
    UriPropertiesDualReadMonitor monitor = fixture.getMonitor();

    ExecutorService executor = Executors.newFixedThreadPool(2,
        new NamedThreadFactory("UriPropertiesDualReadMonitorTest"));
    properties.forEach(p -> executor.execute(() -> reportAndVerifyState(monitor, p.getLeft(), p.getRight())));

    try {
      executor.shutdown();
      boolean completed = executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
      if (completed) {
        // similarity eventually converge to 1
        assertEquals((double) monitor.getMatchedUris() / (double) monitor.getTotalUris(), 1.0);
      }
    } catch (InterruptedException e) {
      Assert.fail("Tasks were interrupted");
    }
  }

  private void reportAndVerifyState(UriPropertiesDualReadMonitor monitor, UriProperties prop, boolean fromNewLb) {
    monitor.reportData(CLUSTER_1, prop, fromNewLb);
    // if reportData on the same cluster are NOT synchronized, the total uris and matched uris counts could become < 0.
    // e.g: when total uris = 1, matched uris = 1, if reporting URI_PROPERTIES_URI_1_AND_2 for new and old Lbs are
    // executed concurrently, total uris will decrement by 1 twice and become -1.
    // We verify that doesn't happen no matter what order the data is reported between the old and new Lbs.
    assertTrue(monitor.getTotalUris() >= 0 && monitor.getTotalUris() <= 2);
    assertTrue(monitor.getMatchedUris() >= 0 && monitor.getMatchedUris() <= 2);
  }

  private ClusterMatchRecord createMatchRecord(UriProperties oldLb, UriProperties newLb, int uris, int matched) {
    ClusterMatchRecord cluster = new ClusterMatchRecord();
    cluster._oldLb = oldLb;
    cluster._newLb = newLb;
    cluster._uris = uris;
    cluster._matched = matched;
    return cluster;
  }

  private void verifyJmxMetricParams(UriPropertiesDualReadMonitorTestFixture fixture, String clusterName,
      ClusterMatchRecord clusterMatchRecord, double totalSimilarity) {
    assertEquals(fixture._clusterNameCaptor.getValue(), clusterName);
    assertEquals(fixture._clusterMatchCaptor.getValue(), clusterMatchRecord);
    assertEquals(fixture._similarityCaptor.getValue(), totalSimilarity);
  }

  private static class UriPropertiesDualReadMonitorTestFixture {
    @Mock
    DualReadLoadBalancerJmx _mockJmx;
    @Captor
    ArgumentCaptor<String> _clusterNameCaptor;
    @Captor
    ArgumentCaptor<ClusterMatchRecord> _clusterMatchCaptor;
    @Captor
    ArgumentCaptor<Double> _similarityCaptor;

    UriPropertiesDualReadMonitorTestFixture() {
      MockitoAnnotations.initMocks(this);
      doNothing().when(_mockJmx).setUriPropertiesSimilarity(_similarityCaptor.capture());
      doNothing().when(_mockJmx).setClusterMatchRecord(_clusterNameCaptor.capture(), _clusterMatchCaptor.capture());
    }

    UriPropertiesDualReadMonitor getMonitor() {
      return new UriPropertiesDualReadMonitor(_mockJmx);
    }
  }
}