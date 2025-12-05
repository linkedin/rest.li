package com.linkedin.d2.jmx;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test suite for {@XdsClientOtelMetricsProvider} interface.
 * Uses a test implementation to verify that all methods are called with expected parameters.
 */
public class XdsClientOtelMetricsProviderTest {

  private TestMetricsProvider _testProvider;

  @BeforeMethod
  public void setUp() {
    _testProvider = new TestMetricsProvider();
  }

  @Test
  public void testRecordConnectionLost() {
    String clientName = "test-client-1";
    _testProvider.recordConnectionLost(clientName);

    assertEquals(_testProvider.getCallCount("recordConnectionLost"), 1);
    assertEquals(_testProvider.getLastClientName("recordConnectionLost"), clientName);
  }

  @Test
  public void testRecordConnectionClosed() {
    String clientName = "test-client-2";
    _testProvider.recordConnectionClosed(clientName);

    assertEquals(_testProvider.getCallCount("recordConnectionClosed"), 1);
    assertEquals(_testProvider.getLastClientName("recordConnectionClosed"), clientName);
  }

  @Test
  public void testRecordReconnection() {
    String clientName = "test-client-3";
    _testProvider.recordReconnection(clientName);

    assertEquals(_testProvider.getCallCount("recordReconnection"), 1);
    assertEquals(_testProvider.getLastClientName("recordReconnection"), clientName);
  }

  @Test
  public void testRecordRequestSent() {
    String clientName = "test-client-4";
    _testProvider.recordRequestSent(clientName);

    assertEquals(_testProvider.getCallCount("recordRequestSent"), 1);
    assertEquals(_testProvider.getLastClientName("recordRequestSent"), clientName);
  }

  @Test
  public void testRecordResponseReceived() {
    String clientName = "test-client-5";
    _testProvider.recordResponseReceived(clientName);

    assertEquals(_testProvider.getCallCount("recordResponseReceived"), 1);
    assertEquals(_testProvider.getLastClientName("recordResponseReceived"), clientName);
  }

  @Test
  public void testRecordInitialResourceVersionSent() {
    String clientName = "test-client-6";
    int count = 42;
    _testProvider.recordInitialResourceVersionSent(clientName, count);

    assertEquals(_testProvider.getCallCount("recordInitialResourceVersionSent"), 1);
    assertEquals(_testProvider.getLastClientName("recordInitialResourceVersionSent"), clientName);
    assertEquals(_testProvider.getLastCount("recordInitialResourceVersionSent").intValue(), count);
  }

  @Test
  public void testRecordResourceNotFound() {
    String clientName = "test-client-7";
    _testProvider.recordResourceNotFound(clientName);

    assertEquals(_testProvider.getCallCount("recordResourceNotFound"), 1);
    assertEquals(_testProvider.getLastClientName("recordResourceNotFound"), clientName);
  }

  @Test
  public void testRecordResourceInvalid() {
    String clientName = "test-client-8";
    _testProvider.recordResourceInvalid(clientName);

    assertEquals(_testProvider.getCallCount("recordResourceInvalid"), 1);
    assertEquals(_testProvider.getLastClientName("recordResourceInvalid"), clientName);
  }

  @Test
  public void testRecordServerLatency() {
    String clientName = "test-client-9";
    long latencyMs = 150L;
    _testProvider.recordServerLatency(clientName, latencyMs);

    assertEquals(_testProvider.getCallCount("recordServerLatency"), 1);
    assertEquals(_testProvider.getLastClientName("recordServerLatency"), clientName);
    assertEquals(_testProvider.getLastLatency("recordServerLatency").longValue(), latencyMs);
  }

  @Test
  public void testUpdateConnectionState() {
    String clientName = "test-client-10";
    boolean isConnected = true;
    _testProvider.updateConnectionState(clientName, isConnected);

    assertEquals(_testProvider.getCallCount("updateConnectionState"), 1);
    assertEquals(_testProvider.getLastClientName("updateConnectionState"), clientName);
    assertTrue(_testProvider.getLastConnectionState("updateConnectionState"));
  }

  @Test
  public void testUpdateActiveInitialWaitTime() {
    String clientName = "test-client-11";
    long waitTimeMs = 5000L;
    _testProvider.updateActiveInitialWaitTime(clientName, waitTimeMs);

    assertEquals(_testProvider.getCallCount("updateActiveInitialWaitTime"), 1);
    assertEquals(_testProvider.getLastClientName("updateActiveInitialWaitTime"), clientName);
    assertEquals(_testProvider.getLastWaitTime("updateActiveInitialWaitTime").longValue(), waitTimeMs);
  }

  @Test
  public void testMultipleCalls() {
    String clientName = "test-client-multi";
    
    _testProvider.recordConnectionLost(clientName);
    _testProvider.recordConnectionLost(clientName);
    _testProvider.recordConnectionLost(clientName);

    assertEquals(_testProvider.getCallCount("recordConnectionLost"), 3);
    assertEquals(_testProvider.getLastClientName("recordConnectionLost"), clientName);
  }

  @Test
  public void testDifferentClientNames() {
    _testProvider.recordRequestSent("client-A");
    _testProvider.recordRequestSent("client-B");
    _testProvider.recordRequestSent("client-C");

    assertEquals(_testProvider.getCallCount("recordRequestSent"), 3);
    assertEquals(_testProvider.getLastClientName("recordRequestSent"), "client-C");
  }

  @Test
  public void testAllMethodsCalled() {
    String clientName = "comprehensive-client";
    
    _testProvider.recordConnectionLost(clientName);
    _testProvider.recordConnectionClosed(clientName);
    _testProvider.recordReconnection(clientName);
    _testProvider.recordRequestSent(clientName);
    _testProvider.recordResponseReceived(clientName);
    _testProvider.recordInitialResourceVersionSent(clientName, 10);
    _testProvider.recordResourceNotFound(clientName);
    _testProvider.recordResourceInvalid(clientName);
    _testProvider.recordServerLatency(clientName, 200L);
    _testProvider.updateConnectionState(clientName, true);
    _testProvider.updateActiveInitialWaitTime(clientName, 3000L);

    // Verify all methods were called exactly once
    assertEquals(_testProvider.getCallCount("recordConnectionLost"), 1);
    assertEquals(_testProvider.getCallCount("recordConnectionClosed"), 1);
    assertEquals(_testProvider.getCallCount("recordReconnection"), 1);
    assertEquals(_testProvider.getCallCount("recordRequestSent"), 1);
    assertEquals(_testProvider.getCallCount("recordResponseReceived"), 1);
    assertEquals(_testProvider.getCallCount("recordInitialResourceVersionSent"), 1);
    assertEquals(_testProvider.getCallCount("recordResourceNotFound"), 1);
    assertEquals(_testProvider.getCallCount("recordResourceInvalid"), 1);
    assertEquals(_testProvider.getCallCount("recordServerLatency"), 1);
    assertEquals(_testProvider.getCallCount("updateConnectionState"), 1);
    assertEquals(_testProvider.getCallCount("updateActiveInitialWaitTime"), 1);
  }

  /**
   * Test implementation of XdsClientOtelMetricsProvider that tracks method calls
   * and their parameters for verification purposes.
   */
  private static class TestMetricsProvider implements XdsClientOtelMetricsProvider {
    
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
      return _calls.stream()
          .filter(call -> call.methodName.equals(methodName))
          .reduce((first, second) -> second)
          .map(call -> call.clientName)
          .orElse(null);
    }

    public Integer getLastCount(String methodName) {
      return _calls.stream()
          .filter(call -> call.methodName.equals(methodName))
          .reduce((first, second) -> second)
          .map(call -> call.count)
          .orElse(null);
    }

    public Long getLastLatency(String methodName) {
      return _calls.stream()
          .filter(call -> call.methodName.equals(methodName))
          .reduce((first, second) -> second)
          .map(call -> call.latencyMs)
          .orElse(null);
    }

    public Boolean getLastConnectionState(String methodName) {
      return _calls.stream()
          .filter(call -> call.methodName.equals(methodName))
          .reduce((first, second) -> second)
          .map(call -> call.isConnected)
          .orElse(null);
    }

    public Long getLastWaitTime(String methodName) {
      return _calls.stream()
          .filter(call -> call.methodName.equals(methodName))
          .reduce((first, second) -> second)
          .map(call -> call.waitTimeMs)
          .orElse(null);
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
}
