package com.linkedin.d2.jmx;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test suite for {@XdsClientOtelMetricsProvider} interface.
 * Uses a test implementation to verify that all methods are called with expected parameters.
 */
public class XdsClientOtelMetricsProviderTest {

  private TestXdsClientOtelMetricsProvider _testProvider;

  @BeforeMethod
  public void setUp() {
    _testProvider = new TestXdsClientOtelMetricsProvider();
  }

  @DataProvider(name = "simpleMethodProvider")
  public Object[][] simpleMethodProvider() {
    return new Object[][] {
        {"recordConnectionLost"},
        {"recordConnectionClosed"},
        {"recordReconnection"},
        {"recordRequestSent"},
        {"recordResponseReceived"},
        {"recordResourceNotFound"},
        {"recordResourceInvalid"}
    };
  }

  @Test(dataProvider = "simpleMethodProvider")
  public void testSimpleRecordMethods(String methodName) {
    String clientName = "test-client-" + methodName;
    
    // Call the appropriate method based on methodName
    switch (methodName) {
      case "recordConnectionLost":
        _testProvider.recordConnectionLost(clientName);
        break;
      case "recordConnectionClosed":
        _testProvider.recordConnectionClosed(clientName);
        break;
      case "recordReconnection":
        _testProvider.recordReconnection(clientName);
        break;
      case "recordRequestSent":
        _testProvider.recordRequestSent(clientName);
        break;
      case "recordResponseReceived":
        _testProvider.recordResponseReceived(clientName);
        break;
      case "recordResourceNotFound":
        _testProvider.recordResourceNotFound(clientName);
        break;
      case "recordResourceInvalid":
        _testProvider.recordResourceInvalid(clientName);
        break;
      default:
        throw new IllegalArgumentException("Unknown method: " + methodName);
    }

    assertEquals(_testProvider.getCallCount(methodName), 1);
    assertEquals(_testProvider.getLastClientName(methodName), clientName);
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
}
