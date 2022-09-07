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
   * Cancels any pending requests to the given cluster.
   */
  default void cancelPendingRequests(@Nonnull String clusterName) {}

  /**
   * Shuts down this handler.
   */
  void shutdown();
}
