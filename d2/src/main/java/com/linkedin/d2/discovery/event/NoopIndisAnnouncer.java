package com.linkedin.d2.discovery.event;

import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import java.util.Map;


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
  public void announce(String cluster, String protocol, String host, int port, String path, Map<Integer,
      PartitionData> partitionDataMap, Map<String, Object> uriSpecificProperties, UriProperties d2UriProperties) {
    // do nothing
  }

  @Override
  public void deannounce(String cluster, String protocol, String host, int port, String path) {
    // do nothing
  }
}
