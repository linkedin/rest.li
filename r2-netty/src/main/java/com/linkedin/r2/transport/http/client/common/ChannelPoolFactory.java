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

package com.linkedin.r2.transport.http.client.common;


import com.linkedin.r2.transport.http.client.AsyncPool;
import io.netty.channel.Channel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.unix.DomainSocketChannel;
import java.net.SocketAddress;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */
public interface ChannelPoolFactory
{
  /**
   * Returns a new pool of Channels to a specific host. The pool will manage the lifecycle of creation
   * and destruction of the channels.
   */
  AsyncPool<Channel> getPool(SocketAddress address);

  default Class<? extends DomainSocketChannel> getDomainSocketClass() {
    if (Epoll.isAvailable()) {
     return EpollDomainSocketChannel.class;
    } else if (KQueue.isAvailable()){
      return KQueueDomainSocketChannel.class;
    } else {
      throw new IllegalStateException("Neither Epoll or Kqueue domain socket transport available");
    }
  }
}
