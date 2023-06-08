package com.linkedin.d2.discovery.event;

import com.linkedin.d2.balancer.properties.PartitionData;
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
  public void announce(String cluster, String protocol, String host, int port, String path, Map<Integer, PartitionData> partitionDataMap, Map<String, Object> uriSpecificProperties) {
    // do nothing
  }

  @Override
  public void deannounce(String cluster, String protocol, String host, int port, String path) {
    // do nothing
  }
}
