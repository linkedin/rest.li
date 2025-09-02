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

public interface XdsClientJmxMBean {

  // when the connection is lost for errors.
  int getConnectionLostCount();

  // when the connection is closed by xDS server.
  int getConnectionClosedCount();

  // when the connection is reconnected
  int getReconnectionCount();

  // the number of requests sent
  long getRequestSentCount();

  // the number of IRV sent
  long getIrvSentCount();

  // the number of responses received
  long getResponseReceivedCount();

  // whether client is disconnected from xDS server: 1 means disconnected; 0 means connected.
  // note: users need to pay attention to disconnected rather than connected state, so setting the metric this way
  // to stress the disconnected state.
  int isDisconnected();

  // when the resource is not found.
  int getResourceNotFoundCount();

  // when the resource is invalid.
  int getResourceInvalidCount();

  /**
   * Get minimum of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getXdsServerLatencyMin();

  /**
   * Get Avg of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  double getXdsServerLatencyAverage();

  /**
   * Get 50 Percentile of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getXdsServerLatency50Pct();

  /**
   * Get 90 Percentile of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getXdsServerLatency99Pct();

  /**
   * Get 99.9 Percentile of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getXdsServerLatency99_9Pct();

  /**
   * Get maximum of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getXdsServerLatencyMax();

  /**
   * Active initial wait time is a sum from all resource subscribers in the xds client that are actively waiting for the
   * initial resource data from xDS server. In normal state, this should be close to 0. In case of missing responses
   * from xDS server or suffering a long delay, this metric will spike up. (Note: once initial data is received,
   * subsequent updates don't count towards this metric, since the client can't know if an update happened at all).
   */
  long getActiveInitialWaitTimeMillis();
}
