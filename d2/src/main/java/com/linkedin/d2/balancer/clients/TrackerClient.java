/*
   Copyright (c) 2020 LinkedIn Corp.

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
package com.linkedin.d2.balancer.clients;

import java.util.Map;

import javax.annotation.Nullable;

import com.linkedin.d2.balancer.LoadBalancerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.util.degrader.CallTracker;

/**
 * A client that tracks call stats and supports partitioning.
 */
public interface TrackerClient extends LoadBalancerClient
{

  /**
   * @return CallStats tracked in the latest interval.
   */
  CallTracker.CallStats getLatestCallStats();

  /**
   * @return {@link PartitionData} map.
   */
  Map<Integer, PartitionData> getPartitionDataMap();

  /**
   * @return {@link TransportClient} that sends the requests.
   */
  TransportClient getTransportClient();

  /**
   * @param doNotSlowStart Should the host skip performing slow start
   */
  default void setDoNotSlowStart(boolean doNotSlowStart)
  {
  }

  /**
   * @return Should the host skip performing slow start
   */
  boolean doNotSlowStart();

  /**
   * @return Whether the host should receive any health score updates.
   */
  boolean doNotLoadBalance();

  /**
   * @param partitionId Partition ID key.
   * @return Weight of specified partition or null if no partition with the ID exists.
   */
  @Nullable
  default Double getPartitionWeight(int partitionId)
  {
    PartitionData partitionData = getPartitionDataMap().get(partitionId);
    return partitionData == null ? null : partitionData.getWeight();
  }

  /**
   * @param partitionId Partition ID key.
   * @param subsetWeight Weight of the tracker client in the subset
   */
  default void setSubsetWeight(int partitionId, double subsetWeight)
  {
  }

  default double getSubsetWeight(int partitionId)
  {
    return 1D;
  }

  /**
   * @return CallTracker.
   */
  CallTracker getCallTracker();

  /**
   * Sets a listener that is invoked for each completed call with the duration (in ms) the server
   * was perceived to have contributed, and a {@link PerCallDurationSemantics} value describing
   * what that duration measures (full round trip vs. streaming TTFB).
   *
   * <p><b>Why this is a setter and not a constructor parameter.</b> A {@link TrackerClient} is
   * constructed by the discovery-side {@code TrackerClientFactory} before any
   * {@link com.linkedin.d2.balancer.strategies.LoadBalancerStrategy} that consumes its metrics
   * exists. The strategy registers its listener when it first observes the tracker client during
   * a partition-state update.
   *
   * <p><b>Single-owner contract.</b> A {@link TrackerClient} supports at most one duration
   * listener at a time. Calling this method replaces any previously-registered listener (last
   * writer wins) — listeners are <em>not</em> composed.
   *
   * <p><b>Threading.</b> The listener fires <em>synchronously</em> on the transport-completion
   * thread, immediately before the wrapped {@code TransportCallback} runs (REST path) or from
   * the entity stream's {@code onDone}/{@code onError} (streaming path). Implementations
   * <b>MUST NOT block</b> &mdash; any latency added here directly delays request completion
   * and the downstream user callback. See {@link PerCallDurationListener} for the full contract.
   *
   * <p>Passing {@code null} resets the listener to a no-op.
   *
   * @param listener primitive-{@code long} duration sink; may be {@code null}
   */
  default void setPerCallDurationListener(PerCallDurationListener listener)
  {
  }
}
