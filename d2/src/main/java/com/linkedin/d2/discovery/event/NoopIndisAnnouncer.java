package com.linkedin.d2.discovery.event;

public class NoopIndisAnnouncer implements IndisAnnouncer {
  @Override
  public void emitAnnouncement(String cluster, String host, int port,
      HealthStatus healthStatus) {
    // do nothing
  }
}
