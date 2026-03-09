package com.linkedin.d2.jmx;

import java.util.ArrayList;
import java.util.List;

/**
 * Test implementation of {@link DegraderLoadBalancerStrategyV3OtelMetricsProvider} that tracks method calls
 * and their parameters for verification purposes.
 */
public class TestDegraderLoadBalancerStrategyV3OtelMetricsProvider implements DegraderLoadBalancerStrategyV3OtelMetricsProvider {

  private final List<MetricsInvocation> _calls = new ArrayList<>();

  @Override
  public void recordHostLatency(String serviceName, String scheme, long hostLatencyMs) {
    _calls.add(new MetricsInvocation("recordHostLatency", serviceName, scheme, hostLatencyMs));
  }

  @Override
  public void updateOverrideClusterDropRate(String serviceName, String scheme, double overrideClusterDropRate) {
    _calls.add(new MetricsInvocation("updateOverrideClusterDropRate", serviceName, scheme, overrideClusterDropRate));
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

  public Double getLastDoubleValue(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.doubleValue;
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
   * Returns all recorded raw latency values for a given service name and scheme.
   * Useful for verifying histogram data points.
   *
   * @param serviceName the service name to filter by, or null for all services
   * @param scheme the scheme to filter by, or null for all schemes
   * @return list of recorded latency values in order they were recorded
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
   * Inner class representing a single metrics method invocation and its parameters.
   */
  private static class MetricsInvocation {
    String methodName;
    String serviceName;
    String scheme;
    Long longValue;
    Double doubleValue;
    Integer intValue;

    MetricsInvocation(String methodName, String serviceName, String scheme, long longValue) {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.longValue = longValue;
    }

    MetricsInvocation(String methodName, String serviceName, String scheme, double doubleValue) {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.doubleValue = doubleValue;
    }

    MetricsInvocation(String methodName, String serviceName, String scheme, int intValue) {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.intValue = intValue;
    }
  }
}
