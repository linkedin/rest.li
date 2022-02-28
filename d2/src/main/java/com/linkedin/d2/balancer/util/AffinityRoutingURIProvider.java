package com.linkedin.d2.balancer.util;

import java.net.URI;
import java.util.Optional;

public interface AffinityRoutingURIProvider {
  String AFFINITY_ROUTING_URI_PROVIDER = "D2_AFFINITY_ROUTING_URI_PROVIDER";

  /**
   * Returns boolean value indicating if URI based optimized affinity routing is enabled or disabled
   * @return
   */
  boolean isEnabled();

  /**
   * Returns machine URI including scheme, hostname and port for the cluster name. If no URI is returned, default D2 routing will
   * take place
   * @param clusterName cluster name for which URI is requested
   * @return
   */
  Optional<URI> getTargetHostURI(String clusterName);

  /**
   * Setter to associate machine URI for the cluster name. URI includes scheme, hostname and port for the cluster. Cluster to URI mapping
   * should be done per inbound request level, i.e. treeId of the request should be used to create request level mapping between
   * cluster name and URI.
   * @param clusterName
   * @param targetHostURI
   */
  void setTargetHostURI(String clusterName, URI targetHostURI);
}
