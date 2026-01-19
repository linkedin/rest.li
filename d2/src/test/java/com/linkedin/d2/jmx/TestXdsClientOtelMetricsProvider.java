package com.linkedin.d2.jmx;

import java.util.ArrayList;
import java.util.List;

/**
 * Test implementation of {@link XdsClientOtelMetricsProvider} that tracks method calls
 * and their parameters for verification purposes.
 *
 * This class can be used as a simple no-op implementation or for detailed call verification
 * in tests that need to inspect metrics provider interactions.
 */
public class TestXdsClientOtelMetricsProvider implements XdsClientOtelMetricsProvider {

  private final List<MetricsInvocation> _calls = new ArrayList<>();

  @Override
  public void recordConnectionLost(String clientName) {
    _calls.add(new MetricsInvocation("recordConnectionLost", clientName));
  }

  @Override
  public void recordConnectionClosed(String clientName) {
    _calls.add(new MetricsInvocation("recordConnectionClosed", clientName));
  }

  @Override
  public void recordReconnection(String clientName) {
    _calls.add(new MetricsInvocation("recordReconnection", clientName));
  }

  @Override
  public void recordRequestSent(String clientName) {
    _calls.add(new MetricsInvocation("recordRequestSent", clientName));
  }

  @Override
  public void recordResponseReceived(String clientName) {
    _calls.add(new MetricsInvocation("recordResponseReceived", clientName));
  }

  @Override
  public void recordInitialResourceVersionSent(String clientName, int count) {
    _calls.add(new MetricsInvocation("recordInitialResourceVersionSent", clientName, count));
  }

  @Override
  public void recordResourceNotFound(String clientName) {
    _calls.add(new MetricsInvocation("recordResourceNotFound", clientName));
  }

  @Override
  public void recordResourceInvalid(String clientName) {
    _calls.add(new MetricsInvocation("recordResourceInvalid", clientName));
  }

  @Override
  public void recordServerLatency(String clientName, long latencyMs) {
    _calls.add(new MetricsInvocation("recordServerLatency", clientName, latencyMs));
  }

  @Override
  public void updateConnectionState(String clientName, boolean isConnected) {
    _calls.add(new MetricsInvocation("updateConnectionState", clientName, isConnected));
  }

  @Override
  public void updateActiveInitialWaitTime(String clientName, long waitTimeMs) {
    _calls.add(new MetricsInvocation("updateActiveInitialWaitTime", clientName, waitTimeMs));
  }

  // Helper methods for verification

  public int getCallCount(String methodName) {
    return (int) _calls.stream()
        .filter(call -> call.methodName.equals(methodName))
        .count();
  }

  public String getLastClientName(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.clientName;
      }
    }
    return null;
  }

  public Integer getLastCount(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.count;
      }
    }
    return null;
  }

  public Long getLastLatency(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.latencyMs;
      }
    }
    return null;
  }

  public Boolean getLastConnectionState(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.isConnected;
      }
    }
    return null;
  }

  public Long getLastWaitTime(String methodName) {
    for (int i = _calls.size() - 1; i >= 0; i--) {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName)) {
        return call.waitTimeMs;
      }
    }
    return null;
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
    String clientName;
    Integer count;
    Long latencyMs;
    Boolean isConnected;
    Long waitTimeMs;

    MetricsInvocation(String methodName, String clientName) {
      this.methodName = methodName;
      this.clientName = clientName;
    }

    MetricsInvocation(String methodName, String clientName, int count) {
      this(methodName, clientName);
      this.count = count;
    }

    MetricsInvocation(String methodName, String clientName, long value) {
      this(methodName, clientName);
      // Determine if it's latency or wait time based on method name
      if (methodName.equals("recordServerLatency")) {
        this.latencyMs = value;
      } else if (methodName.equals("updateActiveInitialWaitTime")) {
        this.waitTimeMs = value;
      }
    }

    MetricsInvocation(String methodName, String clientName, boolean isConnected) {
      this(methodName, clientName);
      this.isConnected = isConnected;
    }
  }
}
