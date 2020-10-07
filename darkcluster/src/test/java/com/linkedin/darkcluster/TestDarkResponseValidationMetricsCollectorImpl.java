package com.linkedin.darkcluster;

import com.google.common.collect.ImmutableMap;
import com.linkedin.darkcluster.api.ResponseValidationMetricsHeader;
import com.linkedin.darkcluster.impl.DarkResponseValidationMetricsCollectorImpl;
import com.linkedin.util.clock.SystemClock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.linkedin.util.clock.Clock;


public class TestDarkResponseValidationMetricsCollectorImpl {
  @Mock
  private Clock _clock;
  private long _collectionFrequencyInMillis = 1;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(_clock.currentTimeMillis()).thenReturn(1L);
  }
  @Test
  public void testAggregateAndGetFromDifferentSources() {
    DarkResponseValidationMetricsCollectorImpl collector = new DarkResponseValidationMetricsCollectorImpl(_clock, _collectionFrequencyInMillis);
    ResponseValidationMetricsHeader header1 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(
            ImmutableMap.of("SUCCESS_COUNT", 6L, "FAILURE_COUNT", 4L), 1L));
    ResponseValidationMetricsHeader header2 = new ResponseValidationMetricsHeader("host2",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 5L, "FAILURE_COUNT", 5L), 1L));
    collector.collect(header1);
    Map<String, Long> metrics1 = collector.get();
    Assert.assertEquals(metrics1.size(), 2);
    Assert.assertEquals(metrics1.get("SUCCESS_COUNT").intValue(), 6);
    Assert.assertEquals(metrics1.get("FAILURE_COUNT").intValue(), 4);
    collector.collect(header2);
    Map<String, Long> metrics2 = collector.get();
    Assert.assertEquals(metrics1.size(), 2);
    Assert.assertEquals(metrics2.get("SUCCESS_COUNT").intValue(), 11);
    Assert.assertEquals(metrics2.get("FAILURE_COUNT").intValue(), 9);
  }

  @Test
  public void testAggregateAndGetFromDifferentSourcesWithMultipleThreads() {
    DarkResponseValidationMetricsCollectorImpl collector = new DarkResponseValidationMetricsCollectorImpl(_clock, _collectionFrequencyInMillis);
    ResponseValidationMetricsHeader header1 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 6L, "FAILURE_COUNT", 4L), 1L));
    ResponseValidationMetricsHeader header2 = new ResponseValidationMetricsHeader("host2",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 5L, "FAILURE_COUNT", 5L), 1L));
    List<Thread> threads = Arrays.asList(header1, header2).stream()
        .map(header -> new Thread(() -> collector.collect(header)))
        .collect(Collectors.toList());
    threads.forEach(Thread::start);
    threads.forEach(t -> {
      try {
        t.join();
      } catch (InterruptedException e) {
        // do nothing
      }
    });
    Map<String, Long> metrics = collector.get();
    Assert.assertEquals(metrics.size(), 2);
    Assert.assertEquals(metrics.get("SUCCESS_COUNT").intValue(), 11);
    Assert.assertEquals(metrics.get("FAILURE_COUNT").intValue(), 9);
  }

  @Test(description = "two headers from same source with increasing metric counts and timestamps")
  public void testAggregateAndGetFromSameSource() {
    DarkResponseValidationMetricsCollectorImpl collector = new DarkResponseValidationMetricsCollectorImpl(_clock, 2);
    ResponseValidationMetricsHeader header1 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 6L, "FAILURE_COUNT", 4L), 1L));
    ResponseValidationMetricsHeader header2 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 10L, "FAILURE_COUNT", 5L), 2L));
    ResponseValidationMetricsHeader header3 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 12L, "FAILURE_COUNT", 6L), 3L));
    Mockito.when(_clock.currentTimeMillis()).thenReturn(1L);
    collector.collect(header1);
    Map<String, Long> metrics1 = collector.get();
    Assert.assertEquals(metrics1.size(), 2);
    Assert.assertEquals(metrics1.get("SUCCESS_COUNT").intValue(), 6);
    Assert.assertEquals(metrics1.get("FAILURE_COUNT").intValue(), 4);
    Mockito.when(_clock.currentTimeMillis()).thenReturn(2L);
    collector.collect(header2);
    Map<String, Long> metrics2 = collector.get();
    Assert.assertEquals(metrics2.size(), 2);
    Assert.assertEquals(metrics2.get("SUCCESS_COUNT").intValue(), 6);
    Assert.assertEquals(metrics2.get("FAILURE_COUNT").intValue(), 4);
    Mockito.when(_clock.currentTimeMillis()).thenReturn(3L);
    collector.collect(header3);
    Map<String, Long> metrics3 = collector.get();
    Assert.assertEquals(metrics3.size(), 2);
    Assert.assertEquals(metrics3.get("SUCCESS_COUNT").intValue(), 12);
    Assert.assertEquals(metrics3.get("FAILURE_COUNT").intValue(), 6);
  }

  @Test(description = "two headers from same source with decreasing metric counts and increasing timestamps")
  public void testAggregateAndGetWithDecreasingMetrics() {
    DarkResponseValidationMetricsCollectorImpl collector = new DarkResponseValidationMetricsCollectorImpl(_clock, _collectionFrequencyInMillis);
    ResponseValidationMetricsHeader header1 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 6L, "FAILURE_COUNT", 4L), 1L));
    ResponseValidationMetricsHeader header2 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 5L, "FAILURE_COUNT", 2L), 2L));
    Mockito.when(_clock.currentTimeMillis()).thenReturn(1L);
    collector.collect(header1);
    Map<String, Long> metrics1 = collector.get();
    Assert.assertEquals(metrics1.size(), 2);
    Assert.assertEquals(metrics1.get("SUCCESS_COUNT").intValue(), 6);
    Assert.assertEquals(metrics1.get("FAILURE_COUNT").intValue(), 4);
    Mockito.when(_clock.currentTimeMillis()).thenReturn(2L);
    collector.collect(header2);
    Map<String, Long> metrics2 = collector.get();
    Assert.assertEquals(metrics1.size(), 2);
    Assert.assertEquals(metrics2.get("SUCCESS_COUNT").intValue(), 11);
    Assert.assertEquals(metrics2.get("FAILURE_COUNT").intValue(), 6);
  }

  @Test(description = "two headers from same source with decreasing metric counts and decreasing timestamps")
  public void testAggregateAndGetWithDecreasingMetricsAndDecreasingTimestamps() {
    DarkResponseValidationMetricsCollectorImpl collector = new DarkResponseValidationMetricsCollectorImpl(_clock, _collectionFrequencyInMillis);
    ResponseValidationMetricsHeader header1 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 6L, "FAILURE_COUNT", 4L), 2L));
    ResponseValidationMetricsHeader header2 = new ResponseValidationMetricsHeader("host1",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(ImmutableMap.of("SUCCESS_COUNT", 5L, "FAILURE_COUNT", 2L), 1L));
    Mockito.when(_clock.currentTimeMillis()).thenReturn(1L);
    collector.collect(header1);
    Map<String, Long> metrics1 = collector.get();
    Assert.assertEquals(metrics1.size(), 2);
    Assert.assertEquals(metrics1.get("SUCCESS_COUNT").intValue(), 6);
    Assert.assertEquals(metrics1.get("FAILURE_COUNT").intValue(), 4);
    Mockito.when(_clock.currentTimeMillis()).thenReturn(2L);
    collector.collect(header2);
    Map<String, Long> metrics2 = collector.get();
    Assert.assertEquals(metrics1.size(), 2);
    Assert.assertEquals(metrics2.get("SUCCESS_COUNT").intValue(), 6);
    Assert.assertEquals(metrics2.get("FAILURE_COUNT").intValue(), 4);
  }

  @Test(description = "1000 incoming headers at times t1 to t10 randomly with varying metrics from varying hosts, and doing a get randomly "
      + "at any time should always result in monotonically increasing metrics")
  public void testMonotonicityWithAggregateAndGetWithMultipleThreads() {
    DarkResponseValidationMetricsCollectorImpl collector = new DarkResponseValidationMetricsCollectorImpl(
        SystemClock.instance(),
        _collectionFrequencyInMillis);
    List<Map<String, Long>> retrievedAggregatedMetrics = new ArrayList<>();
    Random random = new Random();
    List<Thread> collectorThreads = IntStream.range(1, 1001)
        .mapToObj((index) -> {
          ResponseValidationMetricsHeader.ResponseValidationMetrics metrics = new ResponseValidationMetricsHeader.ResponseValidationMetrics(
              ImmutableMap.of("SUCCESS_COUNT", (long) index, "FAILURE_COUNT", (long) index - 1), Math.abs(random.nextLong()) % 100);
          ResponseValidationMetricsHeader header = new ResponseValidationMetricsHeader("host" + index % 100, metrics);
          return new Thread(() -> {
            if (random.nextBoolean()) {
              collector.collect(header);
            } else {
              // locking retrievedAggregatedMetrics here so that this list contains aggregated metrics in the same order as is read by the reader threads.
              // Failing to lock this, will render the list in a random order and we will not be able to assert the nature of metrics monotonicity
              synchronized (retrievedAggregatedMetrics) {
                Map<String, Long> aggregatedMetrics = collector.get();
                if (!aggregatedMetrics.isEmpty()) {
                  retrievedAggregatedMetrics.add(aggregatedMetrics);
                }
              }
            }
          });
        })
        .collect(Collectors.toList());
    collectorThreads.forEach(Thread::start);
    collectorThreads.forEach(t -> {
      try {
        t.join();
      } catch (InterruptedException e) {
        // do nothing
      }
    });
    Map<String, Long> previousVal = new HashMap<>(2);
    previousVal.put("SUCCESS_COUNT", 0L);
    previousVal.put("FAILURE_COUNT", 0L);
    retrievedAggregatedMetrics.forEach(metrics -> {
      Assert.assertTrue(metrics.get("SUCCESS_COUNT") >= previousVal.get("SUCCESS_COUNT"),
          String.format("current: %s, prev: %s", metrics.get("SUCCESS_COUNT"), previousVal.get("SUCCESS_COUNT")));
      Assert.assertTrue(metrics.get("FAILURE_COUNT") >= previousVal.get("FAILURE_COUNT"),
          String.format("current: %s, prev: %s", metrics.get("FAILURE_COUNT"), previousVal.get("FAILURE_COUNT")));
      previousVal.put("SUCCESS_COUNT", metrics.get("SUCCESS_COUNT"));
      previousVal.put("FAILURE_COUNT", metrics.get("FAILURE_COUNT"));
    });
  }
}

