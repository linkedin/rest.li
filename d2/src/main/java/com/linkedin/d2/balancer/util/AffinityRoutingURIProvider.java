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
   * Setter to associate machine URI for the cluster name. URI includes scheme, hostname and port for the cluster. target host
   * URI is returned by D2 load balancer after picking a box from D2 hash ring. Cluster to URI mapping
   * should be done per inbound request level in order to uniformly distribute load across different machines.
   *
   * @param clusterName cluster name for which URI is requested
   * @param targetHostURI Host URI returned by D2 load balancer after picking a box from D2 hash ring.
   */
  void setTargetHostURI(String clusterName, URI targetHostURI);
}
