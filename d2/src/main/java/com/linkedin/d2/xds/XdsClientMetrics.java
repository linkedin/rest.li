package com.linkedin.d2.xds;

/**
 * XdsClientMetrics interface is used to help record different Xds client metrics
 */
public interface XdsClientMetrics {

  // increment the xds client connection lost count
  void incrementConnectionLostCount();

  // increment the xds client connection closed count
  void incrementConnectionClosedCount();

  // increment the xds client reconnection count
  void incrementReconnectionCount();

  // set IsConnected status for the xds client
  void setIsConnected(boolean connected);

  // increment the xds client resource not found count
  void incrementResourceNotFoundCount(String resourceName, String type);

  // increment the xds client resource invalid count
  void incrementResourceInvalidCount();

  // record subscribed resource count for xds client
  void recordSubscribedResourceCount(long resourceCount, String type);
}
