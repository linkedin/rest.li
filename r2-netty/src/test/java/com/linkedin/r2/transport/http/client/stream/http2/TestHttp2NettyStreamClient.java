package com.linkedin.r2.transport.http.client.stream.http2;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.http.client.HttpClientBuilder;
import com.linkedin.r2.transport.http.client.HttpServerBuilder;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
  private static final String URL = SCHEME + "://" + HOST + ":" + PORT + "/echo";
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
}
