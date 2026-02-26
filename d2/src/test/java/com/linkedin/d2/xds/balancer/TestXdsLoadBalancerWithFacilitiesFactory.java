package com.linkedin.d2.xds.balancer;

import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.util.WarmUpLoadBalancer;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.jmx.NoOpXdsClientOtelMetricsProvider;
import com.linkedin.d2.jmx.TestXdsClientOtelMetricsProvider;
import com.linkedin.d2.jmx.XdsClientOtelMetricsProvider;
import java.lang.reflect.Field;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests for {@link XdsLoadBalancerWithFacilitiesFactory}.
 */
public class TestXdsLoadBalancerWithFacilitiesFactory {

  @Test
  public void testFactoryIsIndisOnly() {
    XdsLoadBalancerWithFacilitiesFactory factory = new XdsLoadBalancerWithFacilitiesFactory();
    assertTrue(factory.isIndisOnly());
  }

  @Test
  public void testConfigWithDefaultMetricsProvider() {
    // Verify the config field that the factory uses exists and has a default
    D2ClientConfig config = new D2ClientConfig();
    assertNotNull(config.xdsClientOtelMetricsProvider);
    assertTrue(config.xdsClientOtelMetricsProvider instanceof NoOpXdsClientOtelMetricsProvider);
  }

  @Test
  public void testConfigWithCustomMetricsProvider() {
    // Verify the config field that the factory uses can be set to a custom provider
    D2ClientConfig config = new D2ClientConfig();
    XdsClientOtelMetricsProvider customProvider = new TestXdsClientOtelMetricsProvider();
    config.xdsClientOtelMetricsProvider = customProvider;

    assertSame(config.xdsClientOtelMetricsProvider, customProvider);
  }

  /**
   * Regression test for https://github.com/linkedin/rest.li/pull/1147
   *
   * Verifies that indisWarmUpTimeoutSeconds and indisWarmUpConcurrentRequests are passed to
   * WarmUpLoadBalancer in the correct order. Both parameters are int, so a swap is not caught
   * by the compiler but leads to silent misconfiguration at runtime.
   */
  @Test
  public void testWarmUpLoadBalancerParamsPassedInCorrectOrder() throws Exception {
    XdsLoadBalancerWithFacilitiesFactory factory = new XdsLoadBalancerWithFacilitiesFactory();

    D2ClientConfig config = new D2ClientConfig();
    // Use values that are clearly distinct from each other and from the defaults
    // (DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS=60, DEFAULT_CONCURRENT_REQUESTS=1)
    // so that a swap would cause the wrong field to be set.
    config.indisWarmUpTimeoutSeconds = 42;
    config.indisWarmUpConcurrentRequests = 7;

    LoadBalancerWithFacilities mockBalancer = Mockito.mock(LoadBalancerWithFacilities.class);
    XdsLoadBalancer mockXdsLoadBalancer = Mockito.mock(XdsLoadBalancer.class);
    DownstreamServicesFetcher mockFetcher = Mockito.mock(DownstreamServicesFetcher.class);

    WarmUpLoadBalancer warmUpLB =
        factory.buildWarmUpLoadBalancer(mockBalancer, mockXdsLoadBalancer, config, mockFetcher);

    // WarmUpLoadBalancer converts warmUpTimeoutSeconds -> _warmUpTimeoutMillis (* 1000)
    Field warmUpTimeoutMillisField = WarmUpLoadBalancer.class.getDeclaredField("_warmUpTimeoutMillis");
    warmUpTimeoutMillisField.setAccessible(true);
    int actualTimeoutMillis = (int) warmUpTimeoutMillisField.get(warmUpLB);
    assertEquals(actualTimeoutMillis, config.indisWarmUpTimeoutSeconds * 1000,
        "indisWarmUpTimeoutSeconds was not passed in the correct constructor position");

    Field concurrentRequestsField = WarmUpLoadBalancer.class.getDeclaredField("_concurrentRequests");
    concurrentRequestsField.setAccessible(true);
    int actualConcurrentRequests = (int) concurrentRequestsField.get(warmUpLB);
    assertEquals(actualConcurrentRequests, config.indisWarmUpConcurrentRequests,
        "indisWarmUpConcurrentRequests was not passed in the correct constructor position");
  }
}
