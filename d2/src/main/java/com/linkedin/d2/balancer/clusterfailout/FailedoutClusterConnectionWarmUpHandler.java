package com.linkedin.d2.balancer.clusterfailout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A handler for handling connection warm up to peer clusters.
 */
public interface FailedoutClusterConnectionWarmUpHandler
{
  /**
   * Warms up connections to the given cluster with provided failout config.
   */
  void warmUpConnections(@Nonnull String clusterName, @Nullable FailoutConfig config);

  /**
   * Shuts down this handler.
   */
  void shutdown();
}
