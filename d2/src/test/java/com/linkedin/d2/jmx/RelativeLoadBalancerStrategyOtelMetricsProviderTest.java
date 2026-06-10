/*
   Copyright (c) 2026 LinkedIn Corp.

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
package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/** Tests for {@link RelativeLoadBalancerStrategyOtelMetricsProvider}. */
public class RelativeLoadBalancerStrategyOtelMetricsProviderTest
{
  private TestRelativeLoadBalancerStrategyOtelMetricsProvider _testProvider;

  @BeforeMethod
  public void setUp()
  {
    _testProvider = new TestRelativeLoadBalancerStrategyOtelMetricsProvider();
  }

  @DataProvider(name = "intMethodProvider")
  public Object[][] intMethodProvider()
  {
    return new Object[][] {
        {"updateTotalHostsInAllPartitionsCount", 100},
        {"updateTotalPointsInHashRing", 1000}
    };
  }

  @Test
  public void testRecordHostLatency()
  {
    String serviceName = "test-service-latency";
    String scheme = "http";
    long latencyMs = 150L;

    _testProvider.recordHostLatency(serviceName, scheme, latencyMs, PerCallDurationSemantics.FULL_ROUND_TRIP);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 1);
    assertEquals(_testProvider.getLastServiceName("recordHostLatency"), serviceName);
    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), latencyMs);
  }

  @Test
  public void testRecordMultipleHostLatencies()
  {
    String serviceName = "test-service-multi-latency";
    String scheme = "https";

    _testProvider.recordHostLatency(serviceName, scheme, 100L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    _testProvider.recordHostLatency(serviceName, scheme, 150L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    _testProvider.recordHostLatency(serviceName, scheme, 200L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    _testProvider.recordHostLatency(serviceName, scheme, 120L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    _testProvider.recordHostLatency(serviceName, scheme, 180L, PerCallDurationSemantics.FULL_ROUND_TRIP);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 5);
    assertEquals(_testProvider.getLastServiceName("recordHostLatency"), serviceName);
    assertEquals(_testProvider.getLastScheme("recordHostLatency"), scheme);
    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), 180L);

    List<Long> allLatencies = _testProvider.getAllLatencyValues(serviceName, scheme);
    assertEquals(allLatencies.size(), 5);
    assertEquals(allLatencies, Arrays.asList(100L, 150L, 200L, 120L, 180L));
  }

  @Test(dataProvider = "intMethodProvider")
  public void testIntUpdateMethods(String methodName, int value)
  {
    String serviceName = "test-service-" + methodName;
    String scheme = "http";

    switch (methodName)
    {
      case "updateTotalHostsInAllPartitionsCount":
        _testProvider.updateTotalHostsInAllPartitionsCount(serviceName, scheme, value);
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
  public void testDifferentServiceNames()
  {
    _testProvider.updateTotalHostsInAllPartitionsCount("service-A", "http", 10);
    _testProvider.updateTotalHostsInAllPartitionsCount("service-B", "https", 20);
    _testProvider.updateTotalHostsInAllPartitionsCount("service-C", "http", 30);

    assertEquals(_testProvider.getCallCount("updateTotalHostsInAllPartitionsCount"), 3);
    assertEquals(_testProvider.getLastServiceName("updateTotalHostsInAllPartitionsCount"), "service-C");
    assertEquals(_testProvider.getLastScheme("updateTotalHostsInAllPartitionsCount"), "http");
    assertEquals(_testProvider.getLastIntValue("updateTotalHostsInAllPartitionsCount").intValue(), 30);
  }

  @Test
  public void testAllMethodsCalled()
  {
    String serviceName = "comprehensive-service";
    String scheme = "https";

    _testProvider.recordHostLatency(serviceName, scheme, 100L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    _testProvider.recordHostLatency(serviceName, scheme, 150L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    _testProvider.recordHostLatency(serviceName, scheme, 200L, PerCallDurationSemantics.FULL_ROUND_TRIP);

    _testProvider.updateTotalHostsInAllPartitionsCount(serviceName, scheme, 100);
    _testProvider.updateDegradedHostsCount(serviceName, scheme, HostStatus.UNHEALTHY, 3);
    _testProvider.updateDegradedHostsCount(serviceName, scheme, HostStatus.QUARANTINED, 2);
    _testProvider.updateTotalPointsInHashRing(serviceName, scheme, 1000);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 3);

    assertEquals(_testProvider.getCallCount("updateTotalHostsInAllPartitionsCount"), 1);
    assertEquals(_testProvider.getCallCount("updateDegradedHostsCount"), 2);
    assertEquals(_testProvider.getCallCountForHostStatus("updateDegradedHostsCount", HostStatus.UNHEALTHY), 1);
    assertEquals(_testProvider.getCallCountForHostStatus("updateDegradedHostsCount", HostStatus.QUARANTINED), 1);
    assertEquals(_testProvider.getLastIntValueForHostStatus("updateDegradedHostsCount", HostStatus.UNHEALTHY).intValue(), 3);
    assertEquals(_testProvider.getLastIntValueForHostStatus("updateDegradedHostsCount", HostStatus.QUARANTINED).intValue(), 2);
    assertEquals(_testProvider.getCallCount("updateTotalPointsInHashRing"), 1);
  }

  @Test
  public void testReset()
  {
    String serviceName = "test-service-reset";
    String scheme = "http";

    _testProvider.recordHostLatency(serviceName, scheme, 100L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    _testProvider.updateTotalHostsInAllPartitionsCount(serviceName, scheme, 10);

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 1);
    assertEquals(_testProvider.getCallCount("updateTotalHostsInAllPartitionsCount"), 1);

    _testProvider.reset();

    assertEquals(_testProvider.getCallCount("recordHostLatency"), 0);
    assertEquals(_testProvider.getCallCount("updateTotalHostsInAllPartitionsCount"), 0);
  }

  @Test
  public void testZeroValues()
  {
    String serviceName = "test-service-zero";
    String scheme = "http";

    _testProvider.recordHostLatency(serviceName, scheme, 0L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    _testProvider.updateTotalHostsInAllPartitionsCount(serviceName, scheme, 0);
    _testProvider.updateDegradedHostsCount(serviceName, scheme, HostStatus.UNHEALTHY, 0);
    _testProvider.updateDegradedHostsCount(serviceName, scheme, HostStatus.QUARANTINED, 0);

    assertEquals(_testProvider.getLastLongValue("recordHostLatency").longValue(), 0L);
    assertEquals(_testProvider.getLastIntValue("updateTotalHostsInAllPartitionsCount").intValue(), 0);
    assertEquals(_testProvider.getLastIntValueForHostStatus("updateDegradedHostsCount", HostStatus.UNHEALTHY).intValue(), 0);
    assertEquals(_testProvider.getLastIntValueForHostStatus("updateDegradedHostsCount", HostStatus.QUARANTINED).intValue(), 0);
  }

  @Test
  public void testNoOpProviderDoesNotThrow()
  {
    NoOpRelativeLoadBalancerStrategyOtelMetricsProvider noOpProvider =
        new NoOpRelativeLoadBalancerStrategyOtelMetricsProvider();

    noOpProvider.recordHostLatency("service", "http", 150L, PerCallDurationSemantics.FULL_ROUND_TRIP);

    noOpProvider.updateTotalHostsInAllPartitionsCount("service", "https", 100);
    noOpProvider.updateDegradedHostsCount("service", "http", HostStatus.UNHEALTHY, 3);
    noOpProvider.updateDegradedHostsCount("service", "https", HostStatus.QUARANTINED, 2);
    noOpProvider.updateTotalPointsInHashRing("service", "http", 1000);
  }

  @Test
  public void testLatencyHistogramDistribution()
  {
    String serviceName = "production-service";
    String scheme = "https";

    long[] hostLatencies = {50L, 55L, 60L, 65L, 70L, 75L, 100L, 150L, 200L, 500L};
    for (long latency : hostLatencies)
    {
      _testProvider.recordHostLatency(serviceName, scheme, latency, PerCallDurationSemantics.FULL_ROUND_TRIP);
    }

    List<Long> recordedLatencies = _testProvider.getAllLatencyValues(serviceName, scheme);
    assertEquals(recordedLatencies.size(), 10);
  }
}
