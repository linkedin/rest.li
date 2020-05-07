package com.linkedin.d2.balancer.strategies.framework;

import java.net.URI;


interface ErrorCountManager {
  /**
   * Provide the total error count for a given interval
   * @param uri The uri of the server host
   * @param hostRequestCount The request count the host received in the last interval
   * @param intervalIndex The index of the current interval
   * @return The total call count that the test will send in the interval
   */
  int getErrorCount(URI uri, int hostRequestCount, int intervalIndex);
}
