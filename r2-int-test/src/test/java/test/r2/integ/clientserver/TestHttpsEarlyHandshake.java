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

package test.r2.integ.clientserver;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.netty.common.SslHandlerUtil;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerFactoryImpl;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerKey;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerKeyBuilder;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractEchoServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.common.SslContextUtil;
import test.r2.integ.clientserver.providers.server.ServerProvider;

/**
 * Test Http1.1 and 2 early handshake connection before first request comes in
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class TestHttpsEarlyHandshake extends AbstractEchoServiceTest
{
  private static boolean SSL_SESSION_RESUMPTION_ENABLED = true;

  @Factory(dataProvider = "allHttps", dataProviderClass = ClientServerConfiguration.class)
  public TestHttpsEarlyHandshake(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Test
  public void testHttpsEarlyHandshakeHttp1() throws Exception
  {
    NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    ChannelPoolManagerFactoryImpl channelPoolManagerFactory =
        new ChannelPoolManagerFactoryImpl(eventLoopGroup, scheduler, SSL_SESSION_RESUMPTION_ENABLED,
            _clientProvider.getUsePipelineV2(), HttpClientFactory.DEFAULT_CHANNELPOOL_WAITER_TIMEOUT,
            HttpClientFactory.DEFAULT_CONNECT_TIMEOUT, HttpClientFactory.DEFAULT_SSL_HANDSHAKE_TIMEOUT);
    SSLContext context = SslContextUtil.getContext();

    ChannelPoolManagerKey key = new ChannelPoolManagerKeyBuilder()
      // min pool set to one in such a way a connection is opened before the request
      .setMinPoolSize(1)
      // set the context to enable ssl request
      .setSSLContext(context)
      .setSSLParameters(context.getDefaultSSLParameters())
      .build();

    ChannelPoolManager channelPoolManager = channelPoolManagerFactory.buildRest(key);

    InetAddress inetAddress = InetAddress.getByName("localhost");
    final SocketAddress address = new InetSocketAddress(inetAddress, _port);

    // get the channel, when it is returned it might not be active yet
    FutureCallback<Channel> futureCallback = new FutureCallback<>();
    AsyncPool<Channel> poolForAddress = channelPoolManager.getPoolForAddress(address);
    poolForAddress.get(futureCallback);
    final Channel channel = futureCallback.get(5, TimeUnit.SECONDS);

    // wait until it gets active
    FutureCallback<Future<? super Void>> futureActiveCallback = new FutureCallback<>();
    channel.newSucceededFuture().addListener(futureActiveCallback::onSuccess);
    futureActiveCallback.get(5, TimeUnit.SECONDS);

    // retrieve the ssl handler from the pipeline and wait till the handshake happens
    SslHandler sslHandler = (SslHandler) channel.pipeline().get(SslHandlerUtil.PIPELINE_SSL_HANDLER);

    FutureCallback<Future<? super Channel>> futureHandshakeCallback = new FutureCallback<>();
    sslHandler.handshakeFuture().addListener(f -> {
      if (f.isSuccess())
      {
        futureHandshakeCallback.onSuccess(f);
      }
      else
      {
        futureHandshakeCallback.onError(f.cause());
      }
    });
    futureHandshakeCallback
      // retrieve the result
      .get(5, TimeUnit.SECONDS)
      // retrieve the channel
      .get(5, TimeUnit.SECONDS);

    poolForAddress.dispose(channel);
    // shutdown the pool
    FutureCallback<None> futureShutdownCallback = new FutureCallback<>();
    channelPoolManager.shutdown(futureShutdownCallback, () -> {}, () -> {}, 5000);
    futureShutdownCallback.get(5, TimeUnit.SECONDS);

    // shutdown the client executors
    scheduler.shutdown();
    eventLoopGroup.shutdownGracefully();
  }

}