package com.linkedin.d2.jmx;

/**
 * No-Op implementation of {@link XdsClientOtelMetricsProvider}.
 * Used when OpenTelemetry metrics are disabled.
 */
public class NoOpXdsClientOtelMetricsProvider implements XdsClientOtelMetricsProvider {
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void recordConnectionLost(String clientName) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordConnectionClosed(String clientName) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordReconnection(String clientName) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordRequestSent(String clientName) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordResponseReceived(String clientName) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordInitialResourceVersionSent(String clientName, int count) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordResourceNotFound(String clientName) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordResourceInvalid(String clientName) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordServerLatency(String clientName, long latencyMs) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateConnectionState(String clientName, boolean isConnected) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateActiveInitialWaitTime(String clientName, long waitTimeMs) {
    // No-op
  }
}