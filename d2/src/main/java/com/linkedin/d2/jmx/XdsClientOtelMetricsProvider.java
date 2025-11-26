package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for XDS Client.
 */
public interface XdsClientOtelMetricsProvider {
  
  /**
   * Records a connection lost event in the OpenTelemetry counter.
   *
   * @param clientName the name of the XDS client
   */
  void recordConnectionLost(String clientName);

  /**
   * Records a connection closed event in the OpenTelemetry counter.
   *
   * @param clientName the name of the XDS client
   */
  void recordConnectionClosed(String clientName);

  /**
   * Records a reconnection event in the OpenTelemetry counter.
   *
   * @param clientName the name of the XDS client
   */
  void recordReconnection(String clientName);
  
  /**
   * Records a request sent event in the OpenTelemetry counter.
   *
   * @param clientName the name of the XDS client
   */
  void recordRequestSent(String clientName);

  /**
   * Records a response received event in the OpenTelemetry counter.
   *
   * @param clientName the name of the XDS client
   */
  void recordResponseReceived(String clientName);

  /**
   * Records initial resource version sent count in the OpenTelemetry counter.
   *
   * @param clientName the name of the XDS client
   * @param count the count to add
   */
  void recordInitialResourceVersionSent(String clientName, int count);
  
  /**
   * Records a resource not found error in the OpenTelemetry counter.
   *
   * @param clientName the name of the XDS client
   */
  void recordResourceNotFound(String clientName);

  /**
   * Records a resource invalid error in the OpenTelemetry counter.
   *
   * @param clientName the name of the XDS client
   */
  void recordResourceInvalid(String clientName);
  
  /**
   * Records server latency in the OpenTelemetry histogram.
   *
   * @param clientName the name of the XDS client
   * @param latencyMs the latency in milliseconds
   */
  void recordServerLatency(String clientName, long latencyMs);
  
   /**
   * Updates the connection state for a client.
   *
   * @param clientName the name of the XDS client
   * @param isConnected whether the client is connected
   */
  void updateConnectionState(String clientName, boolean isConnected);

  /**
   * Updates the active initial wait time for a client.
   *
   * @param clientName the name of the XDS client
   * @param waitTimeMs the wait time in milliseconds
   */
  void updateActiveInitialWaitTime(String clientName, long waitTimeMs);
}
