package com.linkedin.d2.jmx;


/**
 * NoOp implementation of {@link XdsServerMetricsProvider}
 */
public class NoOpXdsServerMetricsProvider implements XdsServerMetricsProvider {
  @Override
  public long getLatencyMin() {
    return 0;
  }

  @Override
  public double getLatencyAverage() {
    return 0;
  }

  @Override
  public long getLatency50Pct() {
    return 0;
  }

  @Override
  public long getLatency99Pct() {
    return 0;
  }

  @Override
  public long getLatency99_9Pct() {
    return 0;
  }

  @Override
  public long getLatencyMax() {
    return 0;
  }

  @Override
  public void trackLatency(long latency) {
  }
}
