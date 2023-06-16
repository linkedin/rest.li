package com.linkedin.d2.discovery.event;

import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import java.net.URI;
import java.util.Map;


public class NoopIndisAnnouncer implements IndisAnnouncer {
  @Override
  public void announce(String cluster, URI uri, Map<Integer,
      PartitionData> partitionDataMap, Map<String, Object> uriSpecificProperties, UriProperties d2UriProperties) {
    // do nothing
  }

  @Override
  public void deannounce(String cluster, URI uri) {
    // do nothing
  }
}
