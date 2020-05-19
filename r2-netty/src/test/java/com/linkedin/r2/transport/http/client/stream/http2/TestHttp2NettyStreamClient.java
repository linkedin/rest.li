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

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.DrainReader;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.transport.common.bridge.common.FutureTransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.http.client.HttpClientBuilder;
import com.linkedin.r2.testutils.server.HttpServerBuilder;
import com.linkedin.test.util.ExceptionTestUtil;
import com.linkedin.test.util.retry.SingleRetry;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.util.AsciiString;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.server.Server;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 */
public class TestHttp2NettyStreamClient
{
  private static final int REQUEST_SIZE = 1024;
  private static final long TEST_TIMEOUT = 5000;
  private static final String METHOD = "GET";
  private static final String HOST = "127.0.0.1";
  private static final String SCHEME = "http";
  private static final int PORT = 8080;
  private static final String URL = SCHEME + "://" + HOST + ":" + PORT + "/any";
  private static final AsciiString HOST_NAME = new AsciiString(HOST + ':' + PORT);

  private NioEventLoopGroup _eventLoop;
  private ScheduledExecutorService _scheduler;

  @BeforeClass
  public void doBeforeClass()
  {
    _eventLoop = new NioEventLoopGroup();
    _scheduler = Executors.newSingleThreadScheduledExecutor();
  }

  @AfterClass
  public void doAfterClass()
  {
    _scheduler.shutdown();
    _eventLoop.shutdownGracefully();
  }

  /**
   * When the maximum number of concurrent streams is exhausted, the client is expected to throw
   * an {@link StreamException} immediately.
   */
  @Test(timeOut = TEST_TIMEOUT)
  public void testMaxConcurrentStreamExhaustion() throws Exception
  {
    final HttpServerBuilder serverBuilder = new HttpServerBuilder();
    final Server server = serverBuilder.maxConcurrentStreams(0).build();
    final HttpClientBuilder clientBuilder = new HttpClientBuilder(_eventLoop, _scheduler);
    final Http2NettyStreamClient client = clientBuilder.buildHttp2StreamClient();
    final FutureTransportCallback<StreamResponse> callback = new FutureTransportCallback<>();
    final TransportResponse<StreamResponse> response;
    try {
      server.start();
      // Sends the stream request
      final StreamRequestBuilder builder = new StreamRequestBuilder(new URI(URL));
      final StreamRequest request = builder.setMethod(METHOD)
          .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString()).build(
              EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[REQUEST_SIZE]))));
      client.streamRequest(request, new RequestContext(), new HashMap<>(), callback);
      response = callback.get();
    } finally {
      server.stop();
    }

    Assert.assertNotNull(response);
    Assert.assertTrue(response.hasError());
    Assert.assertNotNull(response.getError());
    ExceptionTestUtil.verifyCauseChain(response.getError(), Http2Exception.StreamException.class);
  }

  /**
   * When a request fails due to {@link TimeoutException}, connection should not be destroyed.
   * @throws Exception
   */
  @Test(timeOut = TEST_TIMEOUT)
  public void testChannelReusedAfterRequestTimeout() throws Exception
  {
    final HttpServerBuilder.HttpServerStatsProvider statsProvider = new HttpServerBuilder.HttpServerStatsProvider();
    final HttpServerBuilder serverBuilder = new HttpServerBuilder();
    final Server server = serverBuilder.serverStatsProvider(statsProvider).stopTimeout(0).build();
    final HttpClientBuilder clientBuilder = new HttpClientBuilder(_eventLoop, _scheduler);
    final Http2NettyStreamClient client = clientBuilder.setRequestTimeout(1000).buildHttp2StreamClient();

    final TransportResponse<StreamResponse> response1;
    final TransportResponse<StreamResponse> response2;
    try {
      server.start();

      final StreamRequestBuilder builder1 = new StreamRequestBuilder(new URI(URL));
      final StreamRequest request1 = builder1.setMethod(METHOD)
          .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString())
          .build(EntityStreams.newEntityStream(new TimeoutWriter()));
      final FutureTransportCallback<StreamResponse> callback1 = new FutureTransportCallback<>();
      client.streamRequest(request1, new RequestContext(), new HashMap<>(), callback1);
      response1 = callback1.get();

      final StreamRequestBuilder builder2 = new StreamRequestBuilder(new URI(URL));
      final StreamRequest request2 = builder2.setMethod(METHOD)
          .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString())
          .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[REQUEST_SIZE]))));
      final FutureTransportCallback<StreamResponse> callback2 = new FutureTransportCallback<>();
      client.streamRequest(request2, new RequestContext(), new HashMap<>(), callback2);
      response2 = callback2.get();
    } finally {
      server.stop();
    }

    // The 1st request should be failed with timeout
    Assert.assertNotNull(response1);
    Assert.assertTrue(response1.hasError());
    Assert.assertNotNull(response1.getError());
    ExceptionTestUtil.verifyCauseChain(response1.getError(), TimeoutException.class);

    // The 2nd request should succeed
    Assert.assertNotNull(response2);
    Assert.assertFalse(response2.hasError());
    response2.getResponse().getEntityStream().setReader(new DrainReader());

    // The server should have seen 2 requests but establishes only 1 connection with the client
    Assert.assertEquals(statsProvider.requestCount(), 3);
    Assert.assertEquals(statsProvider.clientConnections().size(), 1);
  }

  /**
   * When a request fails due to {@link TimeoutException}, connection should not be destroyed.
   * @throws Exception
   */
  @Test(timeOut = TEST_TIMEOUT, retryAnalyzer = SingleRetry.class)
  public void testChannelReusedAfterStreamingTimeout() throws Exception
  {
    final HttpServerBuilder.HttpServerStatsProvider statsProvider = new HttpServerBuilder.HttpServerStatsProvider();
    final HttpServerBuilder serverBuilder = new HttpServerBuilder();
    final Server server = serverBuilder.serverStatsProvider(statsProvider).stopTimeout(0).build();
    final HttpClientBuilder clientBuilder = new HttpClientBuilder(_eventLoop, _scheduler);
    final Http2NettyStreamClient client = clientBuilder.setRequestTimeout(1000).buildHttp2StreamClient();

    final TransportResponse<StreamResponse> response1;
    final TransportResponse<StreamResponse> response2;
    try {
      server.start();

      final StreamRequestBuilder builder1 = new StreamRequestBuilder(new URI(URL));
      final StreamRequest request1 = builder1.setMethod(METHOD)
          .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString())
          .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[REQUEST_SIZE]))));
      final FutureTransportCallback<StreamResponse> callback1 = new FutureTransportCallback<>();
      client.streamRequest(request1, new RequestContext(), new HashMap<>(), callback1);
      response1 = callback1.get();

      Assert.assertNotNull(response1);
      Assert.assertFalse(response1.hasError());
      response1.getResponse().getEntityStream().setReader(new TimeoutReader());

      final StreamRequestBuilder builder2 = new StreamRequestBuilder(new URI(URL));
      final StreamRequest request2 = builder2.setMethod(METHOD)
          .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString())
          .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[REQUEST_SIZE]))));
      final FutureTransportCallback<StreamResponse> callback2 = new FutureTransportCallback<>();
      client.streamRequest(request2, new RequestContext(), new HashMap<>(), callback2);
      response2 = callback2.get();
    } finally {
      server.stop();
    }

    // The 2nd request should succeed
    Assert.assertNotNull(response2);
    Assert.assertFalse(response2.hasError());
    response2.getResponse().getEntityStream().setReader(new DrainReader());

    // The server should have seen 3 requests but establishes only 1 connection with the client
    Assert.assertEquals(statsProvider.requestCount(), 3);
    Assert.assertEquals(statsProvider.clientConnections().size(), 1);
  }

  /**
   * Tests the condition that when a client request times out before the request is processed
   * by the server, the servlet implementation throws when attempting to read the request entity.
   */
  @Test(enabled = false)
  public void testRequestTimeout() throws Exception
  {
    final AtomicInteger serverIOExceptions = new AtomicInteger(0);
    final CountDownLatch exceptionLatch = new CountDownLatch(1);
    final CountDownLatch responseLatch = new CountDownLatch(1);
    final CountDownLatch serverLatch = new CountDownLatch(1);
    final HttpServerBuilder serverBuilder = new HttpServerBuilder();
    final Server server = serverBuilder.exceptionListener(throwable -> {
          if (throwable instanceof IOException)
          {
            serverIOExceptions.incrementAndGet();
            exceptionLatch.countDown();
          }
        }).responseLatch(serverLatch).build();
    final HttpClientBuilder clientBuilder = new HttpClientBuilder(_eventLoop, _scheduler);
    final Http2NettyStreamClient client = clientBuilder.setRequestTimeout(500).buildHttp2StreamClient();
    try
    {
      server.start();

      // Sends the stream request
      final StreamRequestBuilder builder = new StreamRequestBuilder(new URI(URL));
      final StreamRequest request = builder.setMethod(METHOD)
          .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString()).build(
          EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[REQUEST_SIZE]))));
      client.streamRequest(request, new RequestContext(), new HashMap<>(), response -> responseLatch.countDown());

      // Waits for request to timeout
      Thread.sleep(1000);

      // Allows server to process request
      serverLatch.countDown();
    }
    finally
    {
      if (!responseLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS))
      {
        Assert.fail("Timeout waiting for response latch");
      }
      if (!exceptionLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS))
      {
        Assert.fail("Timeout waiting for exception latch");
      }
      server.stop();
    }

    // Expects two IOExceptions thrown by the server. One for the initial OPTIONS upgrade request and one for
    // the actual GET request.
    Assert.assertEquals(serverIOExceptions.get(), 2);
  }

  private static class TimeoutWriter implements Writer
  {
    private AtomicBoolean _writeOnce = new AtomicBoolean(true);
    private WriteHandle _wh;

    @Override
    public void onInit(WriteHandle wh)
    {
      _wh = wh;
    }

    @Override
    public void onWritePossible()
    {
      if (_writeOnce.getAndSet(false))
      {
        _wh.write(ByteString.copy(new byte[128]));
      }
    }

    @Override
    public void onAbort(Throwable e)
    {
      throw new IllegalStateException(e);
    }
  }

  private static class TimeoutReader implements Reader
  {
    @Override
    public void onDataAvailable(ByteString data)
    {
    }

    @Override
    public void onDone()
    {
      throw new IllegalStateException();
    }

    @Override
    public void onError(Throwable e)
    {
      throw new IllegalStateException(e);
    }

    @Override
    public void onInit(ReadHandle rh)
    {
    }
  }
}
