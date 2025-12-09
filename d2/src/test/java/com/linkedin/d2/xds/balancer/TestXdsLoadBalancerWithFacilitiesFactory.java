/*
   Copyright (c) 2023 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.d2.xds.balancer;

import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.jmx.NoOpXdsClientOtelMetricsProvider;
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
    XdsClientOtelMetricsProvider customProvider = new TestMetricsProvider();
    config.xdsClientOtelMetricsProvider = customProvider;
    
    assertSame(config.xdsClientOtelMetricsProvider, customProvider);
  }

  /**
   * Minimal test metrics provider implementation.
   */
  private static class TestMetricsProvider implements XdsClientOtelMetricsProvider {
    @Override
    public void recordConnectionLost(String clientName) {}
    @Override
    public void recordConnectionClosed(String clientName) {}
    @Override
    public void recordReconnection(String clientName) {}
    @Override
    public void recordRequestSent(String clientName) {}
    @Override
    public void recordResponseReceived(String clientName) {}
    @Override
    public void recordInitialResourceVersionSent(String clientName, int count) {}
    @Override
    public void recordResourceNotFound(String clientName) {}
    @Override
    public void recordResourceInvalid(String clientName) {}
    @Override
    public void recordServerLatency(String clientName, long latencyMs) {}
    @Override
    public void updateConnectionState(String clientName, boolean isConnected) {}
    @Override
    public void updateActiveInitialWaitTime(String clientName, long waitTimeMs) {}
  }
}