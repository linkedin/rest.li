package com.linkedin.d2.discovery.event;

/**
 * This interfaces handles annoucning and deannouncing in LinkedIn's Next Generation Service Discovery ([In]Dis).
 * It will be implemented internally, and we provide a default implementation {@link NoopIndisAnnouncer}
 * that open-source restli users can use (or provide their own implementation for their own service discovery system)
 */
public interface IndisAnnouncer {
  void emitAnnouncement(String cluster, String host, int port,
      HealthStatus healthStatus);

  enum HealthStatus {
    // Ready to serve traffic
    READY,
    // Running but not ready to serve traffic (or intentionally taken out of service). Expected to be READY in the future
    UP,
    // Not running; in some error state. Not expected to be READY without intervention.
    DOWN
  }
}
