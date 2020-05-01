package com.linkedin.d2.balancer.strategies.framework;

/**
 * Define the correlation between latency and call count
 */
public interface LatencyQPSCorrelation {

  /**
   * Given the requests per interval, calculate the latency
   * @param requestsPerInterval the number of requests received in the interval
   * @return Expected latency
   */
  long getLatency(int requestsPerInterval);
}
