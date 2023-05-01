package com.linkedin.d2.discovery.event;

import java.util.List;


/**
 * This interfaces handles annoucning and deannouncing in LinkedIn's Next Generation Service Discovery ([In]Dis).
 * It will be implemented internally, and we provide a default implementation {@link NoopIndisAnnouncer}
 * that open-source restli users can use (or provide their own implementation for their own service discovery system)
 */
public interface IndisAnnouncer {
  void emitIndisAnnouncement(List<String> clustersClaimed, String host, int port, String tracingId, long timestamp);
}
