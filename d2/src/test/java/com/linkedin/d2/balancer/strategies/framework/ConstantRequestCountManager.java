package com.linkedin.d2.balancer.strategies.framework;

/**
 * Create constant request count in each interval
 */
class ConstantRequestCountManager implements RequestCountManager {
  private final int _requestsPerInterval;

  ConstantRequestCountManager(int requestsPerInterval) {
    _requestsPerInterval = requestsPerInterval;
  }

  @Override
  public int getRequestCount(int intervalIndex) {
    return _requestsPerInterval;
  }
}
