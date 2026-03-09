package com.linkedin.d2.jmx;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Test suite for {@link DegraderLoadBalancerStrategyV3OtelMetricsProvider} interface.
 * Uses a test implementation to verify that all methods are called with the expected parameters.
 */
public class DegraderLoadBalancerStrategyV3OtelMetricsProviderTest {

  private TestDegraderLoadBalancerStrategyV3OtelMetricsProvider _testProvider;

  @BeforeMethod
  public void setUp() {
    _testProvider = new TestDegraderLoadBalancerStrategyV3OtelMetricsProvider();
  }

  // -------------------------------------------------------------------------
  // recordHostLatency (histogram)
  // -------------------------------------------------------------------------

  @Test
  public void testRecordHostLatency() {
    String serviceName = "test-service-latency";
    String scheme = "http";
    long latencyMs = 150L;

    _testProvider.recordHostLatency(serviceName, scheme, latencyMs);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 1);
    assertEquals(_testProvider.getLastServiceName("recordHostLatency"), serviceName);
    assertEquals(_testProvider.getLastScheme("recordHostLatency"), scheme);
    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), latencyMs);
  }

  @Test
  public void testRecordMultipleHostLatencies() {
    String serviceName = "test-service-multi-latency";
    String scheme = "https";

    // Simulate recording raw per-call latencies for multiple requests from a single host
    _testProvider.recordHostLatency(serviceName, scheme, 100L);
    _testProvider.recordHostLatency(serviceName, scheme, 150L);
    _testProvider.recordHostLatency(serviceName, scheme, 200L);
    _testProvider.recordHostLatency(serviceName, scheme, 120L);
    _testProvider.recordHostLatency(serviceName, scheme, 180L);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 5);
    assertEquals(_testProvider.getLastServiceName("recordHostLatency"), serviceName);
    assertEquals(_testProvider.getLastScheme("recordHostLatency"), scheme);
    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), 180L);

    // Verify all raw latency values were forwarded to the histogram
    // OTEL histogram will compute LatencyStandardDeviation, MaxLatencyRelativeFactor,
    // and Pct99LatencyRelativeFactor from these observations
    List<Long> allLatencies = _testProvider.getAllLatencyValues(serviceName, scheme);
    assertEquals(allLatencies.size(), 5);
    assertEquals(allLatencies, Arrays.asList(100L, 150L, 200L, 120L, 180L));
  }

  @Test
  public void testRecordHostLatencyZero() {
    _testProvider.recordHostLatency("test-service", "http", 0L);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 1);
    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), 0L);
  }

  // -------------------------------------------------------------------------
  // updateOverrideClusterDropRate (gauge)
  // -------------------------------------------------------------------------

  @Test
  public void testUpdateOverrideClusterDropRate() {
    String serviceName = "test-service-drop-rate";
    String scheme = "http";
    double dropRate = 0.25;

    _testProvider.updateOverrideClusterDropRate(serviceName, scheme, dropRate);

    assertEquals(_testProvider.getCallCount("updateOverrideClusterDropRate"), 1);
    assertEquals(_testProvider.getLastServiceName("updateOverrideClusterDropRate"), serviceName);
    assertEquals(_testProvider.getLastScheme("updateOverrideClusterDropRate"), scheme);
    assertEquals(_testProvider.getLastDoubleValue("updateOverrideClusterDropRate"), dropRate, 1e-9);
  }

  @Test
  public void testUpdateOverrideClusterDropRateZero() {
    _testProvider.updateOverrideClusterDropRate("test-service", "http", 0.0);

    assertEquals(_testProvider.getCallCount("updateOverrideClusterDropRate"), 1);
    assertEquals(_testProvider.getLastDoubleValue("updateOverrideClusterDropRate"), 0.0, 1e-9);
  }

  @Test
  public void testUpdateOverrideClusterDropRateFull() {
    _testProvider.updateOverrideClusterDropRate("test-service", "https", 1.0);

    assertEquals(_testProvider.getCallCount("updateOverrideClusterDropRate"), 1);
    assertEquals(_testProvider.getLastDoubleValue("updateOverrideClusterDropRate"), 1.0, 1e-9);
  }

  // -------------------------------------------------------------------------
  // updateTotalPointsInHashRing (gauge)
  // -------------------------------------------------------------------------

  @Test
  public void testUpdateTotalPointsInHashRing() {
    String serviceName = "test-service-ring";
    String scheme = "https";
    int totalPoints = 1000;

    _testProvider.updateTotalPointsInHashRing(serviceName, scheme, totalPoints);

    assertEquals(_testProvider.getCallCount("updateTotalPointsInHashRing"), 1);
    assertEquals(_testProvider.getLastServiceName("updateTotalPointsInHashRing"), serviceName);
    assertEquals(_testProvider.getLastScheme("updateTotalPointsInHashRing"), scheme);
    assertEquals(_testProvider.getLastIntValue("updateTotalPointsInHashRing").intValue(), totalPoints);
  }

  @Test
  public void testUpdateTotalPointsInHashRingZero() {
    _testProvider.updateTotalPointsInHashRing("test-service", "http", 0);

    assertEquals(_testProvider.getCallCount("updateTotalPointsInHashRing"), 1);
    assertEquals(_testProvider.getLastIntValue("updateTotalPointsInHashRing").intValue(), 0);
  }

  // -------------------------------------------------------------------------
  // Multi-service / scheme isolation
  // -------------------------------------------------------------------------

  @Test
  public void testDifferentServiceNames() {
    _testProvider.updateTotalPointsInHashRing("service-A", "http", 100);
    _testProvider.updateTotalPointsInHashRing("service-B", "https", 200);
    _testProvider.updateTotalPointsInHashRing("service-C", "http", 300);

    assertEquals(_testProvider.getCallCount("updateTotalPointsInHashRing"), 3);
    assertEquals(_testProvider.getLastServiceName("updateTotalPointsInHashRing"), "service-C");
    assertEquals(_testProvider.getLastScheme("updateTotalPointsInHashRing"), "http");
    assertEquals(_testProvider.getLastIntValue("updateTotalPointsInHashRing").intValue(), 300);
  }

  @Test
  public void testDifferentSchemes() {
    _testProvider.updateOverrideClusterDropRate("my-service", "http", 0.1);
    _testProvider.updateOverrideClusterDropRate("my-service", "https", 0.2);

    assertEquals(_testProvider.getCallCount("updateOverrideClusterDropRate"), 2);
    assertEquals(_testProvider.getLastScheme("updateOverrideClusterDropRate"), "https");
    assertEquals(_testProvider.getLastDoubleValue("updateOverrideClusterDropRate"), 0.2, 1e-9);
  }

  @Test
  public void testLatencyIsolatedByServiceAndScheme() {
    _testProvider.recordHostLatency("service-A", "http", 100L);
    _testProvider.recordHostLatency("service-A", "https", 200L);
    _testProvider.recordHostLatency("service-B", "http", 300L);

    List<Long> serviceAHttp = _testProvider.getAllLatencyValues("service-A", "http");
    List<Long> serviceAHttps = _testProvider.getAllLatencyValues("service-A", "https");
    List<Long> serviceBHttp = _testProvider.getAllLatencyValues("service-B", "http");

    assertEquals(serviceAHttp, Arrays.asList(100L));
    assertEquals(serviceAHttps, Arrays.asList(200L));
    assertEquals(serviceBHttp, Arrays.asList(300L));
  }

  // -------------------------------------------------------------------------
  // Comprehensive / combined
  // -------------------------------------------------------------------------

  @Test
  public void testAllMethodsCalled() {
    String serviceName = "comprehensive-service";
    String scheme = "https";

    // Raw per-call latencies recorded to histogram (OTEL computes derived metrics automatically)
    _testProvider.recordHostLatency(serviceName, scheme, 100L);
    _testProvider.recordHostLatency(serviceName, scheme, 150L);
    _testProvider.recordHostLatency(serviceName, scheme, 200L);

    // Gauge metrics updated each partition-state cycle
    _testProvider.updateOverrideClusterDropRate(serviceName, scheme, 0.0);
    _testProvider.updateTotalPointsInHashRing(serviceName, scheme, 1000);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 3);
    assertEquals(_testProvider.getCallCount("updateOverrideClusterDropRate"), 1);
    assertEquals(_testProvider.getCallCount("updateTotalPointsInHashRing"), 1);
  }

  @Test
  public void testDropRateIncreasedDuringDegradation() {
    String serviceName = "degrading-service";
    String scheme = "http";

    // Simulate progressive drop rate increase as cluster degrades
    _testProvider.updateOverrideClusterDropRate(serviceName, scheme, 0.0);
    _testProvider.updateOverrideClusterDropRate(serviceName, scheme, 0.1);
    _testProvider.updateOverrideClusterDropRate(serviceName, scheme, 0.3);
    _testProvider.updateOverrideClusterDropRate(serviceName, scheme, 0.5);

    assertEquals(_testProvider.getCallCount("updateOverrideClusterDropRate"), 4);
    assertEquals(_testProvider.getLastDoubleValue("updateOverrideClusterDropRate"), 0.5, 1e-9);
  }

  // -------------------------------------------------------------------------
  // Reset
  // -------------------------------------------------------------------------

  @Test
  public void testReset() {
    String serviceName = "test-service-reset";
    String scheme = "http";

    _testProvider.recordHostLatency(serviceName, scheme, 100L);
    _testProvider.updateOverrideClusterDropRate(serviceName, scheme, 0.2);
    _testProvider.updateTotalPointsInHashRing(serviceName, scheme, 500);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 1);
    assertEquals(_testProvider.getCallCount("updateOverrideClusterDropRate"), 1);
    assertEquals(_testProvider.getCallCount("updateTotalPointsInHashRing"), 1);

    _testProvider.reset();

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 0);
    assertEquals(_testProvider.getCallCount("updateOverrideClusterDropRate"), 0);
    assertEquals(_testProvider.getCallCount("updateTotalPointsInHashRing"), 0);
  }

  // -------------------------------------------------------------------------
  // NoOp provider
  // -------------------------------------------------------------------------

  @Test
  public void testNoOpProviderDoesNotThrow() {
    NoOpDegraderLoadBalancerStrategyV3OtelMetricsProvider noOpProvider =
        new NoOpDegraderLoadBalancerStrategyV3OtelMetricsProvider();

    // Histogram metric
    noOpProvider.recordHostLatency("service", "http", 150L);

    // Gauge metrics
    noOpProvider.updateOverrideClusterDropRate("service", "https", 0.25);
    noOpProvider.updateTotalPointsInHashRing("service", "http", 1000);
  }

  // -------------------------------------------------------------------------
  // Realistic latency histogram distribution
  // -------------------------------------------------------------------------

  @Test
  public void testLatencyHistogramDistribution() {
    // Simulate raw per-call latencies from a cluster of hosts with varying performance.
    // These raw values are forwarded to the OTEL histogram, which then computes
    // LatencyStandardDeviation, MaxLatencyRelativeFactor, and Pct99LatencyRelativeFactor.
    String serviceName = "production-service";
    String scheme = "https";

    long[] callLatencies = {50L, 55L, 60L, 65L, 70L, 75L, 100L, 150L, 200L, 500L};
    for (long latency : callLatencies) {
      _testProvider.recordHostLatency(serviceName, scheme, latency);
    }

    List<Long> recorded = _testProvider.getAllLatencyValues(serviceName, scheme);
    assertEquals(recorded.size(), callLatencies.length);

    // Verify boundary values are present
    assertNotNull(_testProvider.getLastLongValue("recordHostLatency"));
    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), 500L);
  }
}
