package com.linkedin.d2.balancer.strategies.framework;

/**
 * The interface to manage the number of requests sent in each interval
 */
interface RequestCountManager {

  /**
   * Provide the total request count for a given interval
   * @param intervalIndex The index of the current interval
   * @return The total call count that the test will send in the interval
   */
  int getRequestCount(int intervalIndex);
}