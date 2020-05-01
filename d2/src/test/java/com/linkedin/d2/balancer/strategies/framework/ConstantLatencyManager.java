package com.linkedin.d2.balancer.strategies.framework;

import java.net.URI;
import java.util.Map;


/**
 * Return constant latency for each host
 */
class ConstantLatencyManager implements LatencyManager {
  private Map<URI, Long> _constantLatencyMap;

  public ConstantLatencyManager(Map<URI, Long> constantLatencyMap) {
    _constantLatencyMap = constantLatencyMap;
  }
  @Override
  public long getLatency(URI uri, int hostRequestCount, int intervalIndex) {
    return _constantLatencyMap.get(uri);
  }
}
