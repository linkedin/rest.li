/*
   Copyright (c) 2024 LinkedIn Corp.

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

import java.util.ArrayList;
import java.util.List;

/**
 * Test implementation of {@link RelativeLoadBalancerStrategyOtelMetricsProvider} that tracks method calls
 * and their parameters for verification purposes.
 */
public class TestRelativeLoadBalancerStrategyOtelMetricsProvider implements RelativeLoadBalancerStrategyOtelMetricsProvider {

  private final List<MetricsInvocation> _calls = new ArrayList<>();

  @Override
  public void recordHostLatency(String serviceName, String scheme, long hostLatencyMs) {
    _calls.add(new MetricsInvocation("recordHostLatency", serviceName, scheme, hostLatencyMs));
  }

  @Override
  public void updateTotalHostsInAllPartitionsCount(String serviceName, String scheme, int totalHostsInAllPartitionsCount) {
    _calls.add(new MetricsInvocation("updateTotalHostsInAllPartitionsCount", serviceName, scheme, totalHostsInAllPartitionsCount));
  }

  @Override
  public void updateUnhealthyHostsCount(String serviceName, String scheme, int unhealthyHostsCount) {
    _calls.add(new MetricsInvocation("updateUnhealthyHostsCount", serviceName, scheme, unhealthyHostsCount));
  }

  @Override
  public void updateQuarantineHostsCount(String serviceName, String scheme, int quarantineHostsCount) {
    _calls.add(new MetricsInvocation("updateQuarantineHostsCount", serviceName, scheme, quarantineHostsCount));
  }

  @Override
  public void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing) {
    _calls.add(new MetricsInvocation("updateTotalPointsInHashRing", serviceName, scheme, totalPointsInHashRing));
  }

  // Helper methods for verification

  public int getCallCount(String methodName) {
    return (int) _calls.stream()
        .filter(call -> call.methodName.equals(methodName))
        .count();
  }

  public String getLastServiceName(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.serviceName;
      }
    }
    return null;
  }

  public String getLastScheme(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.scheme;
      }
    }
    return null;
  }

  public Long getLastLongValue(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.longValue;
      }
    }
    return null;
  }

  public Integer getLastIntValue(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.intValue;
      }
    }
    return null;
  }

  /**
   * Returns all recorded latency values for a given service name and scheme.
   * Useful for verifying histogram data points.
   *
   * @param serviceName the service name to filter by, or null for all services
   * @param scheme the scheme to filter by, or null for all schemes
   * @return list of recorded latency values
   */
  public List<Long> getAllLatencyValues(String serviceName, String scheme) {
    List<Long> latencies = new ArrayList<>();
    for (MetricsInvocation call : _calls) {
      if (call.methodName.equals("recordHostLatency")
          && (serviceName == null || serviceName.equals(call.serviceName))
          && (scheme == null || scheme.equals(call.scheme))) {
        latencies.add(call.longValue);
      }
    }
    return latencies;
  }

  /**
   * Clears all recorded calls. Useful for resetting state between tests.
   */
  public void reset() {
    _calls.clear();
  }

  /**
   * Inner class to represent a metrics invocation with its parameters
   */
  private static class MetricsInvocation {
    String methodName;
    String serviceName;
    String scheme;
    Long longValue;
    Integer intValue;

    MetricsInvocation(String methodName, String serviceName, String scheme, long longValue) {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.longValue = longValue;
    }

    MetricsInvocation(String methodName, String serviceName, String scheme, int intValue) {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.intValue = intValue;
    }
  }
}
