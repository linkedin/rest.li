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

package com.linkedin.r2.transport.http.client.stream.http2;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.testutils.server.HttpServerBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerFactoryImpl;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerKey;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerKeyBuilder;
import com.linkedin.test.util.AssertionMethods;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Server;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
@SuppressWarnings("rawtypes")
public class TestEarlyUpgrade
{
  private final boolean SSL_SESSION_RESUMPTION_ENABLED = true;

  private NioEventLoopGroup _eventLoopGroup;
  private ScheduledExecutorService _scheduler;
  private final boolean _newPipelineEnabled;

  @Factory(dataProvider = "pipelines")
  public TestEarlyUpgrade(boolean newPipelineEnabled)
  {
    _newPipelineEnabled = newPipelineEnabled;
  }


  @BeforeClass
  public void doBeforeClass()
  {
    _eventLoopGroup = new NioEventLoopGroup();
    _scheduler = Executors.newSingleThreadScheduledExecutor();
  }

  @AfterClass
  public void doAfterClass()
  {
    _scheduler.shutdown();
    _eventLoopGroup.shutdownGracefully();
  }

  /**
   * The aim is having the pool upgrading the http1 connection to http2 even before a request comes in
   */
  @Test
  public void testEarlyUpgrade() throws Exception
  {
    ChannelPoolManagerFactoryImpl channelPoolManagerFactory =
        new ChannelPoolManagerFactoryImpl(_eventLoopGroup, _scheduler,
            SSL_SESSION_RESUMPTION_ENABLED, _newPipelineEnabled, HttpClientFactory.DEFAULT_CHANNELPOOL_WAITER_TIMEOUT,
            HttpClientFactory.DEFAULT_CONNECT_TIMEOUT, HttpClientFactory.DEFAULT_SSL_HANDSHAKE_TIMEOUT);

    ChannelPoolManagerKey key = new ChannelPoolManagerKeyBuilder()
      // min pool set to one in such a way a connection is opened before the request
      .setMinPoolSize(1)
      .build();
    ChannelPoolManager channelPoolManager = channelPoolManagerFactory.buildHttp2Stream(key);

    HttpServerBuilder.HttpServerStatsProvider httpServerStatsProvider = new HttpServerBuilder.HttpServerStatsProvider();

    Server server = new HttpServerBuilder().serverStatsProvider(httpServerStatsProvider).build();
    try
    {
      server.start();
      InetAddress inetAddress = InetAddress.getByName("localhost");
      final SocketAddress address = new InetSocketAddress(inetAddress, HttpServerBuilder.HTTP_PORT);

      // since min pool size is 1, it automatically creates a channel
      channelPoolManager.getPoolForAddress(address);

      // We need the assertWithTimeout because, even if we get the channel,
      // it doesn't mean it connected to the server yet
      AssertionMethods.assertWithTimeout(2000,
        // it is expected 1 connection to be opened and 1 option request
        () -> Assert.assertEquals(httpServerStatsProvider.clientConnections().size(), 1));
      Assert.assertEquals(httpServerStatsProvider.requestCount(), 1);
    }
    finally
    {
      server.stop();
    }
    FutureCallback<None> futureCallback = new FutureCallback<>();

    channelPoolManager.shutdown(futureCallback, () -> {}, () -> {}, 5);
    futureCallback.get(5, TimeUnit.SECONDS);
  }

  @DataProvider
  public static Object[][] pipelines()
  {
    Object[][] pipelineCombinations = {{true},{false}};
    return pipelineCombinations;
  }
}
