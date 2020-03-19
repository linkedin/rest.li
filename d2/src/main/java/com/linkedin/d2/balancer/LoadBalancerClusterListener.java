package com.linkedin.d2.balancer;

/**
 * Cluster Listeners will get notified for ALL cluster changes. It is up to the users
 * to filter/act upon just the clusters they are interested in. This is done this way
 * because it is very likely that users of the listener will be interested in more than
 * one cluster, and so they will only need one listener with this approach, even if they add or
 * remove clusters that they are interested in.
 */
public interface LoadBalancerClusterListener
{
  /**
   * Take appropriate action if interested in this cluster, otherwise, ignore.
   */
  void onClusterAdded(String clusterName);

  /**
   * Take appropriate action if interested in this cluster, otherwise, ignore.
   */
  void onClusterRemoved(String clusterName);
}
