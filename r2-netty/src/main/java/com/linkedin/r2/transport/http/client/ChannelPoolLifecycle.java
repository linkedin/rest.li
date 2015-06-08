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
import com.linkedin.common.stats.LongStats;
import com.linkedin.common.stats.LongTracking;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import java.net.SocketAddress;


/**
* @author Steven Ihde
* @version $Revision: $
*/
class ChannelPoolLifecycle implements AsyncPool.Lifecycle<Channel>
{
  private final SocketAddress _remoteAddress;
  private final Bootstrap _bootstrap;
  private final ChannelGroup _channelGroup;
  private final boolean _tcpNoDelay;
  private final LongTracking _createTimeTracker = new LongTracking();


  public ChannelPoolLifecycle(SocketAddress address, Bootstrap bootstrap, ChannelGroup channelGroup, boolean tcpNoDelay)
  {
    _remoteAddress = address;
    _bootstrap = bootstrap;
    _channelGroup = channelGroup;
    _tcpNoDelay = tcpNoDelay;
  }

  @Override
  public void create(final Callback<Channel> channelCallback)
  {
    final long start = System.currentTimeMillis();
    _bootstrap.connect(_remoteAddress).addListener(new ChannelFutureListener()
    {
      @Override
      public void operationComplete(ChannelFuture channelFuture) throws Exception
      {
        if (channelFuture.isSuccess())
        {
          synchronized (_createTimeTracker)
          {
            _createTimeTracker.addValue(System.currentTimeMillis() - start);
          }
          Channel c = channelFuture.channel();
          if (_tcpNoDelay)
          {
            c.config().setOption(ChannelOption.TCP_NODELAY, true);
          }
          _channelGroup.add(c);
          channelCallback.onSuccess(c);
        }
        else
        {
          channelCallback.onError(HttpNettyStreamClient.toException(channelFuture.cause()));
        }
      }
    });
  }

  @Override
  public boolean validateGet(Channel c)
  {
    return c.isActive();
  }

  @Override
  public boolean validatePut(Channel c)
  {
    return c.isActive();
  }

  @Override
  public void destroy(final Channel channel, final boolean error, final Callback<Channel> channelCallback)
  {
    if (channel.isOpen())
    {
      channel.close().addListener(new ChannelFutureListener()
      {
        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception
        {
          if (channelFuture.isSuccess())
          {
            channelCallback.onSuccess(channelFuture.channel());
          }
          else
          {
            channelCallback.onError(HttpNettyStreamClient.toException(channelFuture.cause()));
          }
        }
      });
    }
    else
    {
      channelCallback.onSuccess(channel);
    }
  }

  @Override
  public PoolStats.LifecycleStats getStats()
  {
    synchronized (_createTimeTracker)
    {
      LongStats stats = _createTimeTracker.getStats();
      _createTimeTracker.reset();
      return new AsyncPoolLifecycleStats(stats.getAverage(),
                                         stats.get50Pct(),
                                         stats.get95Pct(),
                                         stats.get99Pct());
    }
  }
}
