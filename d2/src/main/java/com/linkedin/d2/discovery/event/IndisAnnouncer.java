package com.linkedin.d2.discovery.event;

import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import java.net.URI;
import java.util.Map;

/**
 * This interfaces handles announcing and deannouncing in LinkedIn's Next Generation Service Discovery ([In]Dis).
 * It will be implemented internally, and we provide a default implementation {@link NoopIndisAnnouncer}
 * that open-source restli users can use (or provide their own implementation for their own service discovery system)
 */
public interface IndisAnnouncer {
  void announce(String cluster, URI uri, Map<Integer, PartitionData> partitionDataMap,
      Map<String, Object> uriSpecificProperties, UriProperties d2UriProperties);

  void deannounce(String cluster, URI uri);
}
