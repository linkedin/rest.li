/*
   Copyright (c) 2022 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.linkedin.d2.balancer.util;

import java.net.URI;
import java.util.Optional;


/**
 * Interface contract to provide custom affinity routing by clients. Clients need to provide implementation instance
 * of CustomAffinityRoutingURIProvider interface in the RequestContext local attributes map by key CUSTOM_AFFINITY_ROUTING_URI_PROVIDER.
 * If this instance returns target host URI, then default d2 routing algorithm will be skipped in favor of this custom
 * affinity routing provided by the clients.
 *
 * CustomAffinityRoutingURIProvider can also be used to optimize d2 provided affinity routing. For that, for very first downstream request
 * to a cluster, setTargetHostURI will be called. Clients need to cache this URI for that cluster. Next time, getTargetHostURI is called for
 * the same cluster, client needs to return previously cached URI.
 */
public interface CustomAffinityRoutingURIProvider {
  String CUSTOM_AFFINITY_ROUTING_URI_PROVIDER = "D2_CUSTOM_AFFINITY_ROUTING_URI_PROVIDER";

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
