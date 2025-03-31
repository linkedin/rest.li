package com.linkedin.d2.xds;

import com.linkedin.d2.jmx.XdsClientJmx;


/**
 * XdsClientMetricsWrapperImpl is a wrapper implementation of XdsClientMetrics
 * It calls the configured XdsClientJmx to record metrics
 */
public final class XdsClientMetricsWrapperImpl implements XdsClientMetrics {

  private final XdsClientJmx _xdsClientJmx;

  public XdsClientMetricsWrapperImpl(XdsClientJmx xdsClientJmx) {
    _xdsClientJmx = xdsClientJmx;
  }

  public void incrementConnectionLostCount() {
    _xdsClientJmx.incrementConnectionLostCount();
  }

  public void incrementConnectionClosedCount() {
    _xdsClientJmx.incrementConnectionClosedCount();
  }

  public void incrementReconnectionCount() {
    _xdsClientJmx.incrementReconnectionCount();
  }

  public void setIsConnected(boolean isConnected) {
    _xdsClientJmx.setIsConnected(isConnected);
  }

  public void incrementResourceNotFoundCount(String resourceName, String type) {
    _xdsClientJmx.incrementResourceNotFoundCount();
  }

  public void incrementResourceInvalidCount() {
    _xdsClientJmx.incrementResourceInvalidCount();
  }

  public void recordSubscribedResourceCount(long resourceCount, String type) {
  }
}
