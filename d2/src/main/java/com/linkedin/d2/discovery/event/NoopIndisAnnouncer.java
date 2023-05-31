package com.linkedin.d2.discovery.event;

public class NoopIndisAnnouncer implements IndisAnnouncer {
  @Override
  public void start() {
    // do nothing
  }

  @Override
  public void stop() {
    // do nothing
  }

  @Override
  public void announce(String cluster, String protocol, String host, int port) {
    // do nothing
  }

  @Override
  public void deannounce(String cluster, String protocol, String host, int port) {
    // do nothing
  }
}
