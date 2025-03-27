package com.linkedin.d2.xds;

import com.linkedin.d2.jmx.XdsClientJmx;


public final class XdsClientMetricsAdapter implements XdsClientMetrics {

  private final XdsClientJmx _xdsClientJmx;

  public XdsClientMetricsAdapter(XdsClientJmx xdsClientJmx) {
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
}
