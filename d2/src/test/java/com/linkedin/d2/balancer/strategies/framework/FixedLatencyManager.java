package com.linkedin.d2.balancer.strategies.framework;

import java.net.URI;
import java.util.List;
import java.util.Map;


/**
 * Create predefined fixed latency for each host in each interval
 */
class FixedLatencyManager implements LatencyManager {
  private final Map<URI, List<Long>> _latencyMapForIntervals;

  FixedLatencyManager(Map<URI, List<Long>> latencyMapForIntervals) {
    _latencyMapForIntervals = latencyMapForIntervals;
  }

  @Override
  public long getLatency(URI uri, int hostRequestCount, int intervalIndex) {
    return _latencyMapForIntervals.get(uri).get(intervalIndex);
  }
}

