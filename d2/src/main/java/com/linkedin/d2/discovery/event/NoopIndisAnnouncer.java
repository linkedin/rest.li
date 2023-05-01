package com.linkedin.d2.discovery.event;

import java.util.List;


public class NoopIndisAnnouncer implements IndisAnnouncer {

  @Override
  public void emitIndisAnnouncement(List<String> clustersClaimed, String host, int port, String tracingId,
      long timestamp) {
    // do nothing
  }
}
