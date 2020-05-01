package com.linkedin.d2.balancer.strategies.framework;

import java.util.List;


class FixedRequestCountManager implements RequestCountManager {
  private final List<Integer> _requestsPerIntervalList;

  FixedRequestCountManager(List<Integer> requestsPerIntervalList) {
    _requestsPerIntervalList = requestsPerIntervalList;
  }

  @Override
  public int getRequestCount(int intervalIndex) {
    return _requestsPerIntervalList.get(intervalIndex);
  }
}
