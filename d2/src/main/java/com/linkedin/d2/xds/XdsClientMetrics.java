package com.linkedin.d2.xds;

public interface XdsClientMetrics {

  void incrementConnectionLostCount();

  void incrementConnectionClosedCount();

  void incrementReconnectionCount();

  void setIsConnected(boolean connected);

  void incrementResourceNotFoundCount(String resourceName, String type);

  void incrementResourceInvalidCount();
}
