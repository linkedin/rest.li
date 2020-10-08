package com.linkedin.darkcluster;

import com.google.common.collect.ImmutableMap;
import com.linkedin.darkcluster.api.DispatcherResponseValidationMetricsHolder;
import com.linkedin.darkcluster.api.ResponseValidationMetricsHeader;
import com.linkedin.darkcluster.impl.DispatcherResponseValidationMetricsHolderImpl;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.linkedin.util.clock.Clock;

import static org.mockito.Mockito.*;


public class TestDispatcherResponseValidationMetricsHolderImpl {
  @Mock
  private Clock _clock;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetForNonExistingDarkCluster() {
    when(_clock.currentTimeMillis()).thenReturn(1L);
    DispatcherResponseValidationMetricsHolder metricsHolder = new DispatcherResponseValidationMetricsHolderImpl(_clock);
    String darkClusterName = "dark";
    ResponseValidationMetricsHeader.ResponseValidationMetrics metrics = metricsHolder.get(darkClusterName);
    Assert.assertNull(metrics);
  }

  @Test
  public void testAddAndGetForDarkCluster() {
    DispatcherResponseValidationMetricsHolder metricsHolder = new DispatcherResponseValidationMetricsHolderImpl(_clock);
    String darkCluster1 = "dark1";
    String darkCluster2 = "dark2";
    Map<String, Long> metrics1 = ImmutableMap.of("SUCCESS_COUNT", 10L, "FAILURE_COUNT", 5L);
    Map<String, Long> metrics2 = ImmutableMap.of("SUCCESS_COUNT", 15L, "FAILURE_COUNT", 0L);
    // add to darkcluster1 at time t1
    when(_clock.currentTimeMillis()).thenReturn(1L);
    metricsHolder.add(darkCluster1, metrics1);
    ResponseValidationMetricsHeader.ResponseValidationMetrics darkMetrics1 = metricsHolder.get(darkCluster1);
    Assert.assertEquals(darkMetrics1.getTimestamp(), 1L);
    Assert.assertEquals(darkMetrics1.getMetricsMap().size(), 2);
    Assert.assertEquals(darkMetrics1.getMetricsMap().get("SUCCESS_COUNT").intValue(), 10);
    Assert.assertEquals(darkMetrics1.getMetricsMap().get("FAILURE_COUNT").intValue(), 5);

    // add to darkcluster1 again at time t2
    when(_clock.currentTimeMillis()).thenReturn(2L);
    metricsHolder.add(darkCluster1, metrics2);
    ResponseValidationMetricsHeader.ResponseValidationMetrics darkMetrics2 = metricsHolder.get(darkCluster1);
    Assert.assertEquals(darkMetrics2.getTimestamp(), 2L);
    Assert.assertEquals(darkMetrics2.getMetricsMap().size(), 2);
    Assert.assertEquals(darkMetrics2.getMetricsMap().get("SUCCESS_COUNT").intValue(), 25);
    Assert.assertEquals(darkMetrics2.getMetricsMap().get("FAILURE_COUNT").intValue(), 5);

    // add to darkCluster1 at time t1
    when(_clock.currentTimeMillis()).thenReturn(1L);
    Map<String, Long> metrics3 = ImmutableMap.of("MISMATCH_COUNT", 10L, "TOTAL_COUNT", 30L);
    metricsHolder.add(darkCluster2, metrics3);

    ResponseValidationMetricsHeader.ResponseValidationMetrics darkMetrics3 = metricsHolder.get(darkCluster2);
    Assert.assertEquals(darkMetrics3.getMetricsMap().size(), 2);
    Assert.assertEquals(darkMetrics3.getMetricsMap().get("MISMATCH_COUNT").intValue(), 10);
    Assert.assertEquals(darkMetrics3.getMetricsMap().get("TOTAL_COUNT").intValue(), 30);
  }

  @Test(description = "test to assert that when multiple threads add metrics to the same dark cluster, always results in "
      + "sum total of metrics in the end regardless of the order of threads")
  public void testAddAndGetForDarkClusterByMultipleThreads() {
    when(_clock.currentTimeMillis()).thenReturn(1L);
    DispatcherResponseValidationMetricsHolder
        metricsHolder = new DispatcherResponseValidationMetricsHolderImpl(_clock);
    String darkCluster = "dark";
    Map<String, Long> metrics = ImmutableMap.of("SUCCESS_COUNT", 10L, "FAILURE_COUNT", 5L);
    // start 1000 threads which add metrics into holder
    List<Thread> threads = IntStream.range(0, 1000)
        .mapToObj((index) -> new Thread(() -> metricsHolder.add(darkCluster, metrics)))
        .collect(Collectors.toList());
    threads.forEach(Thread::start);
    threads.forEach(t -> {
      try {
        t.join();
      } catch (InterruptedException e) {
        // do nothing
      }
    });
    ResponseValidationMetricsHeader.ResponseValidationMetrics darkMetrics = metricsHolder.get(darkCluster);
    Assert.assertEquals(darkMetrics.getMetricsMap().size(), 2);
    Assert.assertEquals(darkMetrics.getMetricsMap().get("SUCCESS_COUNT").intValue(), 10000);
    Assert.assertEquals(darkMetrics.getMetricsMap().get("FAILURE_COUNT").intValue(), 5000);
  }
}

