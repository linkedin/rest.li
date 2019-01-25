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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.MultiCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.PoolStats;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

/**
 * {@link ChannelPoolManagerFactory} class that re-uses already created {@link ChannelPoolManager} instances
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ConnectionSharingChannelPoolManagerFactory implements ChannelPoolManagerFactory
{

  private final ChannelPoolManagerFactory _channelPoolManagerFactory;

  private final Map<ChannelPoolManagerKey, ChannelPoolManager> channelPoolManagerMapRest = new ConcurrentHashMap<>();
  private final Map<ChannelPoolManagerKey, ChannelPoolManager> channelPoolManagerMapStream = new ConcurrentHashMap<>();
  private final Map<ChannelPoolManagerKey, ChannelPoolManager> channelPoolManagerMapHttp2Stream = new ConcurrentHashMap<>();

  public ConnectionSharingChannelPoolManagerFactory(ChannelPoolManagerFactory channelPoolManagerFactory)
  {
    _channelPoolManagerFactory = channelPoolManagerFactory;
  }

  @Override
  public ChannelPoolManager buildRest(ChannelPoolManagerKey channelPoolManagerKey)
  {
    return getSharedChannelPoolManager(channelPoolManagerMapRest, channelPoolManagerKey, _channelPoolManagerFactory::buildRest);
  }

  @Override
  public ChannelPoolManager buildStream(ChannelPoolManagerKey channelPoolManagerKey)
  {
    return getSharedChannelPoolManager(channelPoolManagerMapStream, channelPoolManagerKey, _channelPoolManagerFactory::buildStream);
  }

  @Override
  public ChannelPoolManager buildHttp2Stream(ChannelPoolManagerKey channelPoolManagerKey)
  {
    return getSharedChannelPoolManager(channelPoolManagerMapHttp2Stream, channelPoolManagerKey, _channelPoolManagerFactory::buildHttp2Stream);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    MultiCallback multiCallback = new MultiCallback(callback, 3);
    shutdownChannelPoolManagers(multiCallback, channelPoolManagerMapRest);
    shutdownChannelPoolManagers(multiCallback, channelPoolManagerMapStream);
    shutdownChannelPoolManagers(multiCallback, channelPoolManagerMapHttp2Stream);
  }

  private void shutdownChannelPoolManagers(Callback<None> callback, Map<ChannelPoolManagerKey, ChannelPoolManager> channelPoolManagerMap)
  {
    if (channelPoolManagerMap.size() == 0)
    {
      callback.onSuccess(None.none());
    }
    else
    {
      MultiCallback multiCallback = new MultiCallback(callback, channelPoolManagerMap.size());
      channelPoolManagerMap.forEach((channelPoolManagerKey, channelPoolManager) -> channelPoolManager.shutdown(multiCallback,
        () -> {},
        () -> {},
        1000));
    }
  }

  private ChannelPoolManager getSharedChannelPoolManager(Map<ChannelPoolManagerKey, ChannelPoolManager> channelPoolManagerMap,
                                                         ChannelPoolManagerKey channelPoolManagerKey,
                                                         Function<ChannelPoolManagerKey, ChannelPoolManager> channelPoolManagerSupplier)
  {
    return new ShutdownDisabledChannelPoolManager(channelPoolManagerMap.computeIfAbsent(channelPoolManagerKey, channelPoolManagerSupplier));
  }

  /**
   * When Sharing Connection is enabled, the ChannelPoolManager's shutdown is not per managed per Client but by
   * HttpClientFactory. Therefore we must execute a stubbed shutdown when the method is called.
   * As a consequence if a client shuts down itself, it won't shut down the ChannelPoolManager which could be possibly used
   * by other clients
   */
  private static class ShutdownDisabledChannelPoolManager implements ChannelPoolManager
  {

    private final ChannelPoolManager channelPoolManager;

    private ShutdownDisabledChannelPoolManager(ChannelPoolManager channelPoolManager)
    {
      this.channelPoolManager = channelPoolManager;
    }

    /**
     * It executes a stub shutdown
     */
    @Override
    public void shutdown(final Callback<None> callback, final Runnable callbackStopRequest, final Runnable callbackShutdown, long shutdownTimeout)
    {
      callbackStopRequest.run();
      callbackShutdown.run();
      callback.onSuccess(None.none());
    }

    // ############# delegating section ##############

    @Override
    public Collection<Callback<Channel>> cancelWaiters()
    {
      return channelPoolManager.cancelWaiters();
    }

    @Override
    public AsyncPool<Channel> getPoolForAddress(SocketAddress address) throws IllegalStateException
    {
      return channelPoolManager.getPoolForAddress(address);
    }

    @Override
    public Map<String, PoolStats> getPoolStats()
    {
      return channelPoolManager.getPoolStats();
    }

    @Override
    public String getName()
    {
      return channelPoolManager.getName();
    }

    @Override
    public ChannelGroup getAllChannels()
    {
      return channelPoolManager.getAllChannels();
    }
  }
}
