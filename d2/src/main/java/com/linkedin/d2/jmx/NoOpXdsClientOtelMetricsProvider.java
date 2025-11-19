package com.linkedin.d2.jmx;

/**
 * No-Op implementation of XdsClientOtelMetricsProvider.
 * Used when OpenTelemetry metrics are disabled.
 */
public class NoOpXdsClientOtelMetricsProvider implements XdsClientOtelMetricsProvider {

  @Override
  public void recordConnectionLost(String clientName) {
    // No-op
  }

  @Override
  public void recordConnectionClosed(String clientName) {
    // No-op
  }

  @Override
  public void recordReconnection(String clientName) {
    // No-op
  }

  @Override
  public void recordRequestSent(String clientName) {
    // No-op
  }

  @Override
  public void recordResponseReceived(String clientName) {
    // No-op
  }

  @Override
  public void recordInitialResourceVersionSent(String clientName, int count) {
    // No-op
  }

  @Override
  public void recordResourceNotFound(String clientName) {
    // No-op
  }

  @Override
  public void recordResourceInvalid(String clientName) {
    // No-op
  }

  @Override
  public void recordServerLatency(String clientName, long latencyMs) {
    // No-op
  }

  @Override
  public void updateConnectionState(String clientName, boolean isConnected) {
    // No-op
  }

  @Override
  public void updateActiveInitialWaitTime(String clientName, long waitTimeMs) {
    // No-op
  }
}