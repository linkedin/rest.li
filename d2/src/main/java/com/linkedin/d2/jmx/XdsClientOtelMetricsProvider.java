package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for XDS Client.
 * Provides raw event-based metrics that will be processed by OpenTelemetry in the container.
 */
public interface XdsClientOtelMetricsProvider {
  
  // Connection state changes (event-driven counters)
  void recordConnectionLost(String clientName);
  void recordConnectionClosed(String clientName);
  void recordReconnection(String clientName);
  
  // Request/Response counters (event-driven)
  void recordRequestSent(String clientName);
  void recordResponseReceived(String clientName);
  void recordInitialResourceVersionSent(String clientName, int count);
  
  // Error counters (event-driven)
  void recordResourceNotFound(String clientName);
  void recordResourceInvalid(String clientName);
  
  // Server latency histogram (event-driven)
  void recordServerLatency(String clientName, long latencyMs);
  
  // Gauge updates (on-demand)
  void updateConnectionState(String clientName, boolean isConnected);
  void updateActiveInitialWaitTime(String clientName, long waitTimeMs);
}