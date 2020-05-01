package com.linkedin.d2.balancer.strategies.framework;

import java.net.URI;


/**
 * Defines the latency for a particular host in an interval
 */
interface LatencyManager {

  /**
   * Given an interval, calculate the latency for a host
   * The latency may be correlated to the QPS
   *
   * @param uri The uri of the server host
   * @param hostRequestCount The request count the host received in the last interval
   * @param intervalIndex The index of the current interval
   * @return The expected latency
   */
  long getLatency(URI uri, int hostRequestCount, int intervalIndex);
}
