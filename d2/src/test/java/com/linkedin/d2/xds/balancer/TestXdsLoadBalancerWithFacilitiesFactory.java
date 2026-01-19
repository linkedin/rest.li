package com.linkedin.d2.xds.balancer;

import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.jmx.NoOpXdsClientOtelMetricsProvider;
import com.linkedin.d2.jmx.TestXdsClientOtelMetricsProvider;
import com.linkedin.d2.jmx.XdsClientOtelMetricsProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Minimal test for {@link XdsLoadBalancerWithFacilitiesFactory}.
 * Verifies that config.xdsClientOtelMetricsProvider can be configured.
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
}
