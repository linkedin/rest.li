/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.netty.client.http2;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.ObjectCreationTimeoutException;
import com.linkedin.r2.transport.http.client.PoolStats;
import com.linkedin.r2.transport.http.client.TimeoutCallback;
import com.linkedin.util.clock.Clock;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AsyncPool.Lifecycle} for bootstrapping {@link Http2StreamChannel}s.
 * The parent channel is bootstrapped upon first invocation of #create. The parent channel is
 * kept in the state for bootstrapping subsequent stream channels. The parent channel is recreated
 * if the channel is no longer valid. The parent channel is reaped after the parent channel is idle
 * for the configurable timeout period.
 *
 * Implementation of this class is supposed to be thread safe.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
class Http2ChannelLifecycle implements AsyncPool.Lifecycle<Channel>
{
  private static final Logger LOG = LoggerFactory.getLogger(Http2ChannelLifecycle.class);
  public static final int DEFAULT_CHANNEL_CREATION_TIMEOUT_MS = 10000;

  private final SocketAddress _address;
  private final ScheduledExecutorService _scheduler;
  private final Clock _clock;
  private final boolean _ssl;
  private final long _maxContentLength;
  private final long _idleTimeout;
  private final long _channelCreationTimeoutMs;
  private AsyncPool.Lifecycle<Channel> _parentChannelLifecycle;

  /**
   * Read and write to the following members should be synchronized by this lock.
   */
  private final Object _lock = new Object();
  private final Queue<Callback<Channel>> _waiters = new ArrayDeque<>();
  private final ChannelGroup _channelGroup;
  private boolean _bootstrapping = false;
  private Channel _parentChannel = null;
  private long _childChannelCount;
  private long _lastActiveTime;

  Http2ChannelLifecycle(SocketAddress address, ScheduledExecutorService scheduler, Clock clock,
      ChannelGroup channelGroup, boolean ssl, long maxContentLength, long idleTimeout, AsyncPool.Lifecycle<Channel> parentChannelLifecycle)
  {
    _address = address;
    _scheduler = scheduler;
    _clock = clock;
    _channelGroup = channelGroup;
    _ssl = ssl;
    _maxContentLength = maxContentLength;
    _idleTimeout = idleTimeout;
    _parentChannelLifecycle = parentChannelLifecycle;
    _childChannelCount = 0;
    _channelCreationTimeoutMs = DEFAULT_CHANNEL_CREATION_TIMEOUT_MS; // TODO: expose this through cfg2

    _lastActiveTime = _clock.currentTimeMillis();
    _scheduler.scheduleAtFixedRate(this::closeParentIfIdle, idleTimeout, idleTimeout, TimeUnit.MILLISECONDS);
  }

  @Override
  public void create(Callback<Channel> callback)
  {
    Channel parentChannel;
    synchronized (_lock)
    {
      _lastActiveTime = _clock.currentTimeMillis();
      parentChannel = _parentChannel;
    }

    if (!isChannelActive(parentChannel))
    {
      parentChannel = null;
      synchronized (_lock)
      {
        _childChannelCount = 0;
      }
    }

    if (parentChannel == null)
    {
      synchronized (_lock)
      {
        _waiters.add(callback);
        if (_bootstrapping)
        {
          return;
        }
        _bootstrapping = true;
      }

      doBootstrapParentChannel(new Callback<Channel>() {
        @Override
        public void onError(Throwable e)
        {
          notifyWaiters(e);
        }

        @Override
        public void onSuccess(Channel channel)
        {
          doBootstrapWaitersStreamChannel(channel);
        }
      });
    }
    else
    {
      doBootstrapStreamChannel(parentChannel, callback);
    }
  }

  private boolean isChannelActive(Channel channel)
  {
    return channel != null && channel.isActive();
  }

  private void doBootstrapWaitersStreamChannel(Channel channel)
  {
    final List<Callback<Channel>> waiters;

    synchronized (_lock)
    {
      _parentChannel = channel;
      _channelGroup.add(channel);
      waiters = new ArrayList<>(_waiters.size());
      IntStream.range(0, _waiters.size()).forEach(i -> waiters.add(_waiters.poll()));
      _bootstrapping = false;
    }

    for (Callback<Channel> waiter : waiters)
    {
      doBootstrapStreamChannel(channel, waiter);
    }
  }

  private void notifyWaiters(Throwable e)
  {
    final List<Callback<Channel>> waiters;
    synchronized (_lock)
    {
      waiters = new ArrayList<>(_waiters.size());
      IntStream.range(0, _waiters.size()).forEach(i -> waiters.add(_waiters.poll()));
      _bootstrapping = false;
    }
    for (Callback<Channel> waiter : waiters)
    {
      waiter.onError(e);
    }
  }

  /**
   * Bootstraps the parent (connection) channel, awaits for ALPN, and returns the
   * channel through success callback. If exception occurs, the cause is returned
   * through the error callback.
   * @param callback Callback of the parent channel bootstrap.
   */
  private void doBootstrapParentChannel(Callback<Channel> callback)
  {
    // Lets not trust the _parentChannelLifecycle to timely return a response here.
    // Embedding the callback inside a timeout callback (ObjectCreationTimeoutCallback)
    // to force a response within creationTimeout deadline
    _parentChannelLifecycle.create(new TimeoutCallback<>(_scheduler, _channelCreationTimeoutMs, TimeUnit.MILLISECONDS, new Callback<Channel>() {
      @Override
      public void onError(Throwable error)
      {
        callback.onError(error);

        // Make sure to log the object creation timeout error
        if (error instanceof ObjectCreationTimeoutException)
        {
          LOG.error(error.getMessage(), error);
        }
      }

      @Override
      public void onSuccess(Channel channel)
      {
        channel.attr(NettyChannelAttributes.INITIALIZATION_FUTURE).get().addListener(alpnFuture -> {
          if (alpnFuture.isSuccess())
          {
            callback.onSuccess(channel);
          }
          else
          {
            callback.onError(alpnFuture.cause());
          }
        });
      }
    }, () -> new ObjectCreationTimeoutException(
    "Exceeded creation timeout of " + _channelCreationTimeoutMs + "ms: for HTTP/2 parent channel, remote=" + _address)));
  }

  /**
   * Bootstraps the stream channel from the given parent channel. Returns the stream channel
   * through the success callback if bootstrap succeeds; Return the cause if an exception occurs.
   * @param channel Parent channel to bootstrap the stream channel from.
   * @param callback Callback of the stream channel bootstrap.
   */
  private void doBootstrapStreamChannel(Channel channel, Callback<Channel> callback)
  {
    final Http2StreamChannelBootstrap bootstrap =
        new Http2StreamChannelBootstrap(channel).handler(new Http2StreamChannelInitializer(_ssl, _maxContentLength));

    bootstrap.open().addListener(future -> {
      if (future.isSuccess())
      {
        synchronized (_lock)
        {
          _childChannelCount++;
        }
        callback.onSuccess((Http2StreamChannel) future.get());
      }
      else
      {
        channel.close();
        callback.onError(future.cause());
      }
    });
  }

  /**
   * Attempts to close the parent channel if idle timeout has expired.
   */
  private void closeParentIfIdle()
  {
    final Channel channel;
    final long lastActiveTime;
    final long childChannelCount;

    synchronized (_lock)
    {
      channel = _parentChannel;
      lastActiveTime = _lastActiveTime;
      childChannelCount = _childChannelCount;
    }

    if (_clock.currentTimeMillis() - lastActiveTime < _idleTimeout)
    {
      return;
    }

    if (channel == null || !channel.isOpen())
    {
      return;
    }

    if (childChannelCount > 0)
    {
      return;
    }

    synchronized (_lock)
    {
      _parentChannel = null;
      _childChannelCount = 0;
    }

    LOG.info("Closing parent channel due to idle timeout !");
    channel.close().addListener(future -> {
      if (!future.isSuccess())
      {
        LOG.error("Failed to close parent channel after idle timeout, remote={}", _address, future.cause());
      }
    });
  }

  // ############# delegating section ##############

  @Override
  public boolean validateGet(Channel channel)
  {
    return _parentChannelLifecycle.validateGet(channel);
  }

  @Override
  public boolean validatePut(Channel channel)
  {
    return _parentChannelLifecycle.validatePut(channel);
  }

  @Override
  public void destroy(Channel channel, boolean error, Callback<Channel> callback)
  {
    _parentChannelLifecycle.destroy(channel, error, callback);
    synchronized (_lock)
    {
      if (_childChannelCount > 0)
      {
        _childChannelCount--;
      }
    }
  }

  @Override
  public PoolStats.LifecycleStats getStats()
  {
    return _parentChannelLifecycle.getStats();
  }
}
