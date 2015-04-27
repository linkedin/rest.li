/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */
class ChannelPoolManager implements PoolStatsProvider
{
  private static final Logger LOG = LoggerFactory.getLogger(ChannelPoolManager.class);

  public static final String BASE_NAME = "ChannelPools";

  // All modifications of _pool and all access to _state must be locked on _mutex.
  // READS of _pool are allowed without synchronization
  private final Object _mutex = new Object();
  // We set update concurrency to 1 because all updates occur in a synchronized block
  private final ConcurrentMap<SocketAddress,AsyncPool<Channel>> _pool =
          new ConcurrentHashMap<SocketAddress,AsyncPool<Channel>>(256, 0.75f, 1);
  private enum State { RUNNING, SHUTTING_DOWN, SHUTDOWN }
  private State _state = State.RUNNING;

  private final ChannelPoolFactory _channelPoolFactory;
  private final String _name;

  public ChannelPoolManager(ChannelPoolFactory channelPoolFactory)
  {
    this(channelPoolFactory,
         HttpClientFactory.DEFAULT_CLIENT_NAME + BASE_NAME);
  }

  public ChannelPoolManager(ChannelPoolFactory channelPoolFactory,
                            String name)
  {
    _channelPoolFactory = channelPoolFactory;
    _name = name;
  }

  public void shutdown(final Callback<None> callback)
  {
    final Collection<AsyncPool<Channel>> pools;
    final State state;
    synchronized (_mutex)
    {
      state = _state;
      pools = _pool.values();
      if (state == State.RUNNING)
      {
        _state = State.SHUTTING_DOWN;
      }
    }
    if (state != State.RUNNING)
    {
      callback.onError(new IllegalStateException("ChannelPoolManager is " + state));
      return;
    }

    LOG.info("Shutting down {} connection pools", pools.size());
    Callback<None> poolCallback = Callbacks.countDown(new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        synchronized (_mutex)
        {
          _state = State.SHUTDOWN;
        }
        LOG.info("All connection pools shutdown");
        callback.onSuccess(None.none());
      }

      @Override
      public void onError(Throwable e)
      {
        synchronized (_mutex)
        {
          _state = State.SHUTDOWN;
        }
        LOG.error("Error shutting down connection pools", e);
        callback.onError(e);
      }
    }, pools.size());
    for (AsyncPool<Channel> pool : pools)
    {
      pool.shutdown(poolCallback);
    }

  }

  public Collection<Callback<Channel>> cancelWaiters()
  {
    Collection<Callback<Channel>> cancelled = new ArrayList<Callback<Channel>>();
    final Collection<AsyncPool<Channel>> pools;
    synchronized (_mutex)
    {
      pools = _pool.values();
    }
    for (AsyncPool<Channel> pool : pools)
    {
      cancelled.addAll(pool.cancelWaiters());
    }
    return cancelled;
  }

  public AsyncPool<Channel> getPoolForAddress(SocketAddress address) throws IllegalStateException
  {
    /*
        Unsynchronized get is safe because this is a ConcurrentHashMap
        We don't need to check whether we're shutting down, because each
        pool maintains its own shutdown state.  Synchronizing for get is
        undesirable, because every request for every address comes through this path and it
        would essentially be a global request lock.
    */
    AsyncPool<Channel> pool = _pool.get(address);
    if (pool != null)
    {
      return pool;
    }

    synchronized (_mutex)
    {
      if (_state != State.RUNNING)
      {
        throw new IllegalStateException("ChannelPoolManager is shutting down");
      }
      // Retry the get while synchronized
      pool = _pool.get(address);
      if (pool == null)
      {
        pool = _channelPoolFactory.getPool(address);
        pool.start();
        _pool.put(address, pool);
      }
    }
    return pool;
  }

  /**
   * Get statistics from each pool. The map keys represent pool names.
   * The values are the corresponding {@link AsyncPoolStats} objects.
   *
   * @return A map of pool names and statistics.
   */
  @Override
  public Map<String, PoolStats> getPoolStats()
  {
    final Map<String, PoolStats> stats = new HashMap<String, PoolStats>();
    for(AsyncPool<Channel> pool : _pool.values())
    {
      stats.put(pool.getName(), pool.getStats());
    }
    return stats;
  }

  @Override
  public String getName()
  {
    return _name;
  }
}
