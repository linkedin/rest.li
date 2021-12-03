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
import com.linkedin.common.util.None;
import com.linkedin.r2.netty.common.ChannelPoolFactory;
import com.linkedin.r2.netty.common.ChannelPoolManager;
import com.linkedin.r2.netty.common.ChannelPoolManagerImpl;
import com.linkedin.r2.util.Cancellable;
import io.netty.channel.Channel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestChannelPoolManager
{

  @Test
  public void test()
  {
    ChannelPoolFactory factory = new ChannelPoolFactory()
    {
      @Override
      public AsyncPool<Channel> getPool(SocketAddress address)
      {
        return new FakePool<>();
      }
    };
    ChannelPoolManager m = new ChannelPoolManagerImpl(factory, null, null);

    final int NUM = 100;
    List<SocketAddress> addresses = new ArrayList<>(NUM);
    for (int i = 0; i < NUM; i++)
    {
      addresses.add(new InetSocketAddress(i));
    }
    List<AsyncPool<Channel>> pools = new ArrayList<>(NUM);
    for (int i = 0; i < NUM; i++)
    {
      pools.add(m.getPoolForAddress(addresses.get(i)));
    }
    for (int i = 0; i < NUM; i++)
    {
      Assert.assertEquals(m.getPoolForAddress(addresses.get(i)), pools.get(i));
    }
  }

  private static class FakePool<T> implements AsyncPool<T>
  {
    @Override
    public String getName()
    {
      return "fake pool";
    }

    @Override
    public void start()
    {

    }

    @Override
    public void shutdown(Callback<None> callback)
    {

    }

    @Override
    public Collection<Callback<T>> cancelWaiters()
    {
      return null;
    }

    @Override
    public Cancellable get(Callback<T> tCallback)
    {
      return null;
    }

    @Override
    public void put(T obj)
    {

    }

    @Override
    public void dispose(T obj)
    {

    }

    @Override
    public AsyncPoolStats getStats()
    {
      return null;
    }
  }
}
