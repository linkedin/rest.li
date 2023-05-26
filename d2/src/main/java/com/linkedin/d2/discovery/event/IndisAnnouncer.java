package com.linkedin.d2.discovery.event;

/**
 * This interfaces handles annoucning and deannouncing in LinkedIn's Next Generation Service Discovery ([In]Dis).
 * It will be implemented internally, and we provide a default implementation {@link NoopIndisAnnouncer}
 * that open-source restli users can use (or provide their own implementation for their own service discovery system)
 */
public interface IndisAnnouncer {
  void announce(String cluster, String protocol, String host, int port);

  void deannounce(String cluster, String protocol, String host, int port);
}
