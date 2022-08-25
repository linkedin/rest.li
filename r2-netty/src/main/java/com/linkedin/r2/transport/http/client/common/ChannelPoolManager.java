/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncPoolStats;
import com.linkedin.r2.transport.http.client.PoolStats;
import com.linkedin.r2.transport.http.client.PoolStatsProvider;
import com.linkedin.pegasus.io.netty.channel.Channel;
import com.linkedin.pegasus.io.netty.channel.group.ChannelGroup;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;

/**
 * Interface of a ChannelPoolManager that manages the lifecycle and returns on demand connection pools to a specific
 * host/port
 *
 *  @author Francesco Capponi (fcapponi@linkedin.com)
 */
public interface ChannelPoolManager extends PoolStatsProvider
{
  void shutdown(final Callback<None> callback, final Runnable callbackStopRequest, final Runnable callbackShutdown, long shutdownTimeout);

  Collection<Callback<Channel>> cancelWaiters();

  AsyncPool<Channel> getPoolForAddress(SocketAddress address) throws IllegalStateException;

  /**
   * Get statistics from each pool. The map keys represent pool names.
   * The values are the corresponding {@link AsyncPoolStats} objects.
   *
   * @return A map of pool names and statistics.
   */
  @Override
  Map<String, PoolStats> getPoolStats();

  @Override
  String getName();

  ChannelGroup getAllChannels();
}
