package com.linkedin.d2.jmx;


/**
 * NoOp implementation of {@link IndisObserverMetricsProvider}
 */
public class NoOpIndisObserverMetricsProvider implements IndisObserverMetricsProvider {
  @Override
  public long getLatency50Pct() {
    return 0;
  }

  @Override
  public long getLatency99Pct() {
    return 0;
  }

  @Override
  public double getLatencyAverage() {
    return 0;
  }

  @Override
  public void trackLatency(long latency) {
  }
}
