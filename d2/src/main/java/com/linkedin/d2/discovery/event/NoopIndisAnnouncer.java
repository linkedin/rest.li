package com.linkedin.d2.discovery.event;

import java.util.List;


public class NoopIndisAnnouncer implements IndisAnnouncer {
  @Override
  public void emitAnnouncement(String cluster, String host, int port,
      ServiceDiscoveryEventEmitter.StatusUpdateActionType actionType, String uriProperties) {
    // do nothing
  }
}
