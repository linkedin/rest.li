package com.linkedin.d2.jmx;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test suite for {@link RelativeLoadBalancerStrategyOtelMetricsProvider} interface.
 * Uses a test implementation to verify that all methods are called with expected parameters.
 */
public class RelativeLoadBalancerStrategyOtelMetricsProviderTest {

  private TestRelativeLoadBalancerStrategyOtelMetricsProvider _testProvider;

  @BeforeMethod
  public void setUp() {
    _testProvider = new TestRelativeLoadBalancerStrategyOtelMetricsProvider();
  }

  @DataProvider(name = "intMethodProvider")
  public Object[][] intMethodProvider() {
    return new Object[][] {
        {"updateTotalHostsInAllPartitionsCount", 100},
        {"updateUnhealthyHostsCount", 3},
        {"updateQuarantineHostsCount", 2},
        {"updateTotalPointsInHashRing", 1000}
    };
  }

  @Test
  public void testRecordHostLatency() {
    String serviceName = "test-service-latency";
    String scheme = "http";
    long latencyMs = 150L;

    _testProvider.recordHostLatency(serviceName, scheme, latencyMs);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 1);
    assertEquals(_testProvider.getLastServiceName("recordHostLatency"), serviceName);
    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), latencyMs);
  }

  @Test
  public void testRecordMultipleHostLatencies() {
    String serviceName = "test-service-multi-latency";
    String scheme = "https";

    // Simulate recording latencies for multiple hosts in a cluster
    _testProvider.recordHostLatency(serviceName, scheme, 100L);
    _testProvider.recordHostLatency(serviceName, scheme, 150L);
    _testProvider.recordHostLatency(serviceName, scheme, 200L);
    _testProvider.recordHostLatency(serviceName, scheme, 120L);
    _testProvider.recordHostLatency(serviceName, scheme, 180L);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 5);
    assertEquals(_testProvider.getLastServiceName("recordHostLatency"), serviceName);
    assertEquals(_testProvider.getLastScheme("recordHostLatency"), scheme);
    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), 180L);

    // Verify all latency values were recorded (OTEL histogram will compute p50, p90, p99 from these)
    List<Long> allLatencies = _testProvider.getAllLatencyValues(serviceName, scheme);
    assertEquals(allLatencies.size(), 5);
    assertEquals(allLatencies, Arrays.asList(100L, 150L, 200L, 120L, 180L));
  }

  @Test(dataProvider = "intMethodProvider")
  public void testIntUpdateMethods(String methodName, int value) {
    String serviceName = "test-service-" + methodName;
    String scheme = "http";

    // Call the appropriate method based on methodName
    switch (methodName) {
      case "updateTotalHostsInAllPartitionsCount":
        _testProvider.updateTotalHostsInAllPartitionsCount(serviceName, scheme, value);
        break;
      case "updateUnhealthyHostsCount":
        _testProvider.updateUnhealthyHostsCount(serviceName, scheme, value);
        break;
      case "updateQuarantineHostsCount":
        _testProvider.updateQuarantineHostsCount(serviceName, scheme, value);
        break;
      case "updateTotalPointsInHashRing":
        _testProvider.updateTotalPointsInHashRing(serviceName, scheme, value);
        break;
      default:
        throw new IllegalArgumentException("Unknown method: " + methodName);
    }

    assertEquals(_testProvider.getCallCount(methodName), 1);
    assertEquals(_testProvider.getLastServiceName(methodName), serviceName);
    assertEquals(_testProvider.getLastScheme(methodName), scheme);
    assertEquals(_testProvider.getLastIntValue(methodName).intValue(), value);
  }

  @Test
  public void testDifferentServiceNames() {
    _testProvider.updateTotalHostsInAllPartitionsCount("service-A", "http", 10);
    _testProvider.updateTotalHostsInAllPartitionsCount("service-B", "https", 20);
    _testProvider.updateTotalHostsInAllPartitionsCount("service-C", "http", 30);

    assertEquals(_testProvider.getCallCount("updateTotalHostsInAllPartitionsCount"), 3);
    assertEquals(_testProvider.getLastServiceName("updateTotalHostsInAllPartitionsCount"), "service-C");
    assertEquals(_testProvider.getLastScheme("updateTotalHostsInAllPartitionsCount"), "http");
    assertEquals(_testProvider.getLastIntValue("updateTotalHostsInAllPartitionsCount").intValue(), 30);
  }

  @Test
  public void testAllMethodsCalled() {
    String serviceName = "comprehensive-service";
    String scheme = "https";

    // Record host latency (histogram - OTEL computes percentiles, std dev, etc. automatically)
    _testProvider.recordHostLatency(serviceName, scheme, 100L);
    _testProvider.recordHostLatency(serviceName, scheme, 150L);
    _testProvider.recordHostLatency(serviceName, scheme, 200L);

    // Update all gauge metrics
    _testProvider.updateTotalHostsInAllPartitionsCount(serviceName, scheme, 100);
    _testProvider.updateUnhealthyHostsCount(serviceName, scheme, 3);
    _testProvider.updateQuarantineHostsCount(serviceName, scheme, 2);
    _testProvider.updateTotalPointsInHashRing(serviceName, scheme, 1000);

    // Verify latency histogram was populated
    assertEquals(_testProvider.getCallCount("recordHostLatency"), 3);

    // Verify all gauge methods were called exactly once
    assertEquals(_testProvider.getCallCount("updateTotalHostsInAllPartitionsCount"), 1);
    assertEquals(_testProvider.getCallCount("updateUnhealthyHostsCount"), 1);
    assertEquals(_testProvider.getCallCount("updateQuarantineHostsCount"), 1);
    assertEquals(_testProvider.getCallCount("updateTotalPointsInHashRing"), 1);
  }

  @Test
  public void testReset() {
    String serviceName = "test-service-reset";
    String scheme = "http";

    _testProvider.recordHostLatency(serviceName, scheme, 100L);
    _testProvider.updateTotalHostsInAllPartitionsCount(serviceName, scheme, 10);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 1);
    assertEquals(_testProvider.getCallCount("updateTotalHostsInAllPartitionsCount"), 1);

    _testProvider.reset();

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 0);
    assertEquals(_testProvider.getCallCount("updateTotalHostsInAllPartitionsCount"), 0);
  }

  @Test
  public void testZeroValues() {
    String serviceName = "test-service-zero";
    String scheme = "http";

    _testProvider.recordHostLatency(serviceName, scheme, 0L);
    _testProvider.updateTotalHostsInAllPartitionsCount(serviceName, scheme, 0);
    _testProvider.updateUnhealthyHostsCount(serviceName, scheme, 0);
    _testProvider.updateQuarantineHostsCount(serviceName, scheme, 0);

    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), 0L);
    assertEquals(_testProvider.getLastIntValue("updateTotalHostsInAllPartitionsCount").intValue(), 0);
    assertEquals(_testProvider.getLastIntValue("updateUnhealthyHostsCount").intValue(), 0);
    assertEquals(_testProvider.getLastIntValue("updateQuarantineHostsCount").intValue(), 0);
  }

  @Test
  public void testNoOpProviderDoesNotThrow() {
    // Test that NoOp provider doesn't throw any exceptions
    NoOpRelativeLoadBalancerStrategyOtelMetricsProvider noOpProvider =
        new NoOpRelativeLoadBalancerStrategyOtelMetricsProvider();

    // Histogram metrics should execute without throwing
    noOpProvider.recordHostLatency("service", "http", 150L);

    // Gauge metrics should execute without throwing
    noOpProvider.updateTotalHostsInAllPartitionsCount("service", "https", 100);
    noOpProvider.updateUnhealthyHostsCount("service", "http", 3);
    noOpProvider.updateQuarantineHostsCount("service", "https", 2);
    noOpProvider.updateTotalPointsInHashRing("service", "http", 1000);
  }

  @Test
  public void testLatencyHistogramDistribution() {
    // Simulate a realistic cluster with varying host latencies
    String serviceName = "production-service";
    String scheme = "https";

    // Record latencies from 10 hosts with varying performance
    long[] hostLatencies = {50L, 55L, 60L, 65L, 70L, 75L, 100L, 150L, 200L, 500L};
    for (long latency : hostLatencies) {
      _testProvider.recordHostLatency(serviceName, scheme, latency);
    }

    // Verify all data points were recorded
    List<Long> recordedLatencies = _testProvider.getAllLatencyValues(serviceName, scheme);
    assertEquals(recordedLatencies.size(), 10);

    // OTEL histogram will automatically compute:
    // - p50 (median) ~ 72.5ms
    // - p90 ~ 200ms
    // - p99 ~ 500ms
    // - average ~ 132.5ms
    // - min = 50ms
    // - max = 500ms
    // - standard deviation, etc.
  }
}
