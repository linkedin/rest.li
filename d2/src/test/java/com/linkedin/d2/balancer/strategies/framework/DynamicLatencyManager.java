package com.linkedin.d2.balancer.strategies.framework;

import java.net.URI;
import java.util.Map;


/**
 * Create dynamic latency using the QPS correlation
 */
class DynamicLatencyManager implements LatencyManager {
  private final Map<URI, LatencyQPSCorrelation> _latencyCalculationMap;

  DynamicLatencyManager(Map<URI, LatencyQPSCorrelation> latencyCalculationMap) {
    _latencyCalculationMap = latencyCalculationMap;
  }

  @Override
  public long getLatency(URI uri, int hostRequestCount, int intervalIndex) {
    return _latencyCalculationMap.get(uri).getLatency(hostRequestCount);
  }
}