/*
   Copyright (c) 2023 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.d2.jmx;

import com.linkedin.d2.xds.XdsClientImpl;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

import static com.linkedin.d2.jmx.D2JmxConstants.NO_VALUE;


public class XdsClientJmx implements XdsClientJmxMBean
{

  private final AtomicInteger _connectionLostCount = new AtomicInteger();
  private final AtomicInteger _connectionClosedCount = new AtomicInteger();
  private final AtomicInteger _reconnectionCount = new AtomicInteger();
  private final AtomicLong _resquestSentCount = new AtomicLong();
  private final AtomicLong _irvSentCount = new AtomicLong();
  private final AtomicLong _responseReceivedCount = new AtomicLong();

  private final AtomicBoolean _isConnected = new AtomicBoolean();
  private final AtomicInteger _resourceNotFoundCount = new AtomicInteger();
  private final AtomicInteger _resourceInvalidCount = new AtomicInteger();
  private final XdsServerMetricsProvider _xdsServerMetricsProvider;
  private final XdsClientOtelMetricsProvider _xdsClientOtelMetricsProvider;
  private final AtomicReference<String> _clientName = new AtomicReference<>(NO_VALUE);
  @Nullable private XdsClientImpl _xdsClient = null;

  @Deprecated
  public XdsClientJmx()
  {
    this(new NoOpXdsServerMetricsProvider(), null);
  }

  public XdsClientJmx(XdsServerMetricsProvider xdsServerMetricsProvider)
  {
    this(xdsServerMetricsProvider, null);
  }

  public XdsClientJmx(XdsServerMetricsProvider xdsServerMetricsProvider, 
                      XdsClientOtelMetricsProvider xdsClientOtelMetricsProvider)
  {
    _xdsServerMetricsProvider = xdsServerMetricsProvider == null ?
        new NoOpXdsServerMetricsProvider() : xdsServerMetricsProvider;
     _xdsClientOtelMetricsProvider = xdsClientOtelMetricsProvider == null ? 
        new NoOpXdsClientOtelMetricsProvider() : xdsClientOtelMetricsProvider;
  }

  /**
   * Sets the client name for this XDS client instance.
   * Used for identifying metrics associated with this client.
   *
   * @param clientName the name to identify this XDS client
   */
  public void setClientName(String clientName) {
    _clientName.compareAndSet(NO_VALUE, clientName);
  }

  /**
   * Gets the client name for this XDS client instance.
   *
   * @return the client name, or "-" if not set
   */
  public String getClientName() {
    return _clientName.get();
  }

  public void setXdsClient(XdsClientImpl xdsClient)
  {
    _xdsClient = xdsClient;
  }

  @Override
  public int getConnectionLostCount()
  {
    return _connectionLostCount.get();
  }

  @Override
  public int getConnectionClosedCount()
  {
    return _connectionClosedCount.get();
  }

  @Override
  public int getReconnectionCount()
  {
    return _reconnectionCount.get();
  }

  @Override
  public long getRequestSentCount()
  {
    return _resquestSentCount.get();
  }

  @Override
  public long getIrvSentCount()
  {
    return _irvSentCount.get();
  }

  @Override
  public long getResponseReceivedCount()
  {
    return _responseReceivedCount.get();
  }

  @Override
  public int getResourceNotFoundCount()
  {
    return _resourceNotFoundCount.get();
  }

  @Override
  public int getResourceInvalidCount()
  {
    return _resourceInvalidCount.get();
  }

  @Override
  public long getXdsServerLatencyMin() {
    return _xdsServerMetricsProvider.getLatencyMin();
  }

  @Override
  public double getXdsServerLatencyAverage()
  {
    return _xdsServerMetricsProvider.getLatencyAverage();
  }

  @Override
  public long getXdsServerLatency50Pct()
  {
    return _xdsServerMetricsProvider.getLatency50Pct();
  }

  @Override
  public long getXdsServerLatency99Pct()
  {
    return _xdsServerMetricsProvider.getLatency99Pct();
  }

  @Override
  public long getXdsServerLatency99_9Pct() {
    return _xdsServerMetricsProvider.getLatency99_9Pct();
  }

  @Override
  public long getXdsServerLatencyMax() {
    return _xdsServerMetricsProvider.getLatencyMax();
  }

  @Override
  public int isDisconnected()
  {
    return _isConnected.get() ? 0 : 1;
  }

  @Override
  public long getActiveInitialWaitTimeMillis()
  {
    long waitTime = -1;
    if (_xdsClient != null)
    {
      waitTime = _xdsClient.getActiveInitialWaitTimeMillis();
      _xdsClientOtelMetricsProvider.updateActiveInitialWaitTime(getClientName(), waitTime);
    }
    return waitTime;
  }

  public void incrementConnectionLostCount()
  {
    _connectionLostCount.incrementAndGet();
    _xdsClientOtelMetricsProvider.recordConnectionLost(getClientName());
  }

  public void incrementConnectionClosedCount()
  {
    _connectionClosedCount.incrementAndGet();
    _xdsClientOtelMetricsProvider.recordConnectionClosed(getClientName());
  }

  public void incrementReconnectionCount()
  {
    _reconnectionCount.incrementAndGet();
    _xdsClientOtelMetricsProvider.recordReconnection(getClientName());
  }

  public void incrementRequestSentCount()
  {
    _resquestSentCount.incrementAndGet();
    _xdsClientOtelMetricsProvider.recordRequestSent(getClientName());
  }

  public void addToIrvSentCount(int delta)
  {
    _irvSentCount.addAndGet(delta);
    _xdsClientOtelMetricsProvider.recordInitialResourceVersionSent(getClientName(), delta);
  }

  public void incrementResponseReceivedCount()
  {
    _responseReceivedCount.incrementAndGet();
    _xdsClientOtelMetricsProvider.recordResponseReceived(getClientName());
  }

  public void setIsConnected(boolean connected)
  {
    _isConnected.getAndSet(connected);
    _xdsClientOtelMetricsProvider.updateConnectionState(getClientName(), connected);
  }

  public void incrementResourceNotFoundCount()
  {
    _resourceNotFoundCount.incrementAndGet();
    _xdsClientOtelMetricsProvider.recordResourceNotFound(getClientName());
  }

  public void incrementResourceInvalidCount()
  {
    _resourceInvalidCount.incrementAndGet();
    _xdsClientOtelMetricsProvider.recordResourceInvalid(getClientName());
  }
}
