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
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.TestGroupNames;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.transport.common.bridge.client.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.http.client.common.ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.stream.AbstractNettyStreamClient;
import com.linkedin.r2.transport.http.client.stream.http.HttpNettyStreamClient;
import com.linkedin.r2.transport.http.client.stream.http2.Http2NettyStreamClient;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.util.AsciiString;
import org.eclipse.jetty.server.Server;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Steven Ihde
 * @author Ang Xu
 * @author Sean Sheng
 * @version $Revision: $
 */

public class TestHttpNettyStreamClient
{
  private static final String HOST = "127.0.0.1";
  private static final String SCHEME = "http";
  private static final int PORT = 8080;
  private static final String URL = SCHEME + "://" + HOST + ":" + PORT + "/echo";

  private static final int REQUEST_COUNT = 100;
  private static final AsciiString HOST_NAME = new AsciiString(HOST + ':' + PORT);

  private static final String HTTP_GET = "GET";
  private static final String HTTP_POST = "POST";

  private static final int NO_CONTENT = 0;
  private static final int SMALL_CONTENT = 8 * 1024;
  private static final int LARGE_CONTENT = 128 * 1024;

  private NioEventLoopGroup _eventLoop;
  private ScheduledExecutorService _scheduler;

  private static final int TEST_MAX_RESPONSE_SIZE = 500000;
  private static final int TEST_MAX_HEADER_SIZE = 5000;
  private static final int TEST_HEADER_SIZE_BUFFER = 50;

  private static final int RESPONSE_OK = 1;
  private static final int TOO_LARGE = 2;

  @BeforeClass
  public void setup()
  {
    _eventLoop = new NioEventLoopGroup();
    _scheduler = Executors.newSingleThreadScheduledExecutor();
  }

  @AfterClass
  public void tearDown()
  {
    _scheduler.shutdown();
    _eventLoop.shutdownGracefully();
  }

  @Test
  public void testNoChannelTimeout()
      throws InterruptedException
  {
    HttpNettyStreamClient client = new HttpNettyStreamClient(new NoCreations(_scheduler), _scheduler, 500, 500);

    RestRequest r = new RestRequestBuilder(URI.create("http://localhost/")).build();

    FutureCallback<StreamResponse> cb = new FutureCallback<StreamResponse>();
    TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<StreamResponse>(cb);

    client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String, String>(), callback);

    try
    {
      // This timeout needs to be significantly larger than the getTimeout of the netty client;
      // we're testing that the client will generate its own timeout
      cb.get(30, TimeUnit.SECONDS);
      Assert.fail("Get was supposed to time out");
    }
    catch (TimeoutException e)
    {
      // TimeoutException means the timeout for Future.get() elapsed and nothing happened.
      // Instead, we are expecting our callback to be invoked before the future timeout
      // with a timeout generated by the HttpNettyClient.
      Assert.fail("Unexpected TimeoutException, should have been ExecutionException", e);
    }
    catch (ExecutionException e)
    {
      verifyCauseChain(e, RemoteInvocationException.class, TimeoutException.class);
    }
  }

  @DataProvider(name = "slowReaderTimeoutClientProvider")
  public Object[][] slowReaderTimeoutClientProvider()
  {
    // Sets request timeout to be reasonable small since this unit test will await for the timeout duration
    // however increase the timeout if test is not stable
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler).setRequestTimeout(1000);
    return new Object[][] {
        { builder.buildStreamClient() },
        { builder.buildHttp2StreamClient() }
    };
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testUnsupportedRestRequest() throws UnsupportedOperationException
  {
    TransportClient client = new HttpClientBuilder(_eventLoop, _scheduler).buildStreamClient();

    client.restRequest(null, new RequestContext(), new HashMap<>(), null);
    Assert.fail("The Http Stream clients should throw UnsupportedOperationException when streamRequest is called");
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testUnsupportedRestRequestHttp2() throws UnsupportedOperationException
  {
    TransportClient client = new HttpClientBuilder(_eventLoop, _scheduler).buildHttp2StreamClient();

    client.restRequest(null, new RequestContext(), new HashMap<>(), null);
    Assert.fail("The Http Stream clients should throw UnsupportedOperationException when streamRequest is called");
  }

  /**
   * Tests slow EntityStream {@link Reader} implementation should be subject to streaming timeout even
   * if the entire response entity can be buffered in memory.
   *
   * @throws Exception
   */
  @Test(dataProvider = "slowReaderTimeoutClientProvider")
  public void testSlowReaderTimeout(AbstractNettyStreamClient client) throws Exception
  {
    // Sets the response size to be greater than zero but smaller than the in-memory buffer for HTTP/1.1
    // and smaller than the receiving window size for HTTP/2 so the receiver will not block sender
    Server server = new HttpServerBuilder().responseSize(R2Constants.DEFAULT_DATA_CHUNK_SIZE).build();

    StreamRequest request = new StreamRequestBuilder(new URI(URL))
        .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString())
        .build(EntityStreams.emptyStream());

    final CountDownLatch responseLatch = new CountDownLatch(1);
    final CountDownLatch streamLatch = new CountDownLatch(1);
    final AtomicReference<TransportResponse<StreamResponse>> atomicTransportResponse = new AtomicReference<>();
    final AtomicReference<Throwable> atomicThrowable = new AtomicReference<>();
    try {
      server.start();
      client.streamRequest(request, new RequestContext(), new HashMap<>(), response -> {
        atomicTransportResponse.set(response);
        responseLatch.countDown();

        // Sets a reader that does not consume any byte
        response.getResponse().getEntityStream().setReader(new Reader() {
          @Override
          public void onInit(ReadHandle rh) {
          }

          @Override
          public void onDataAvailable(ByteString data) {
          }

          @Override
          public void onDone() {
          }

          @Override
          public void onError(Throwable e) {
            atomicThrowable.set(e);
            streamLatch.countDown();
          }
        });

      });
    } finally {
      responseLatch.await(5, TimeUnit.SECONDS);
      streamLatch.await(5, TimeUnit.SECONDS);
      server.stop();
    }

    TransportResponse<StreamResponse> transportResponse = atomicTransportResponse.get();
    Assert.assertNotNull(transportResponse, "Expected to receive a response");
    Assert.assertFalse(transportResponse.hasError(), "Expected to receive a response without error");
    Assert.assertNotNull(transportResponse.getResponse());
    Assert.assertNotNull(transportResponse.getResponse().getEntityStream());

    Throwable throwable = atomicThrowable.get();
    Assert.assertNotNull(throwable, "Expected onError invoked with TimeoutException");
    Assert.assertTrue(throwable instanceof RemoteInvocationException);
    Assert.assertNotNull(throwable.getCause());
    Assert.assertTrue(throwable.getCause() instanceof TimeoutException);
  }

  @DataProvider(name = "noResponseClients")
  public Object[][] noResponseClientProvider()
  {
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler)
        .setRequestTimeout(500)
        .setIdleTimeout(10000)
        .setShutdownTimeout(500);
    return new Object[][] {
        { builder.buildStreamClient() },
        { builder.buildHttp2StreamClient() },
    };
  }

  @Test(dataProvider = "noResponseClients")
  public void testNoResponseTimeout(AbstractNettyStreamClient client) throws Exception
  {
    CountDownLatch responseLatch = new CountDownLatch(1);
    Server server = new HttpServerBuilder().responseLatch(responseLatch).build();
    try
    {
      server.start();

      RestRequest r = new RestRequestBuilder(new URI(URL)).build();
      FutureCallback<StreamResponse> cb = new FutureCallback<StreamResponse>();
      TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<StreamResponse>(cb);
      client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String, String>(), callback);

      // This timeout needs to be significantly larger than the getTimeout of the netty client;
      // we're testing that the client will generate its own timeout
      cb.get(30, TimeUnit.SECONDS);
      Assert.fail("Get was supposed to time out");
    }
    catch (TimeoutException e)
    {
      // TimeoutException means the timeout for Future.get() elapsed and nothing happened.
      // Instead, we are expecting our callback to be invoked before the future timeout
      // with a timeout generated by the HttpNettyClient.
      Assert.fail("Unexpected TimeoutException, should have been ExecutionException", e);
    }
    catch (ExecutionException e)
    {
      verifyCauseChain(e, RemoteInvocationException.class, TimeoutException.class);
    }
    finally
    {
      responseLatch.countDown();
      server.stop();
    }
  }

  @DataProvider(name = "badAddressClients")
  public Object[][] badAddressClientsProvider()
  {
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler)
        .setRequestTimeout(30000)
        .setIdleTimeout(10000)
        .setShutdownTimeout(500);
    return new Object[][] {
        { builder.buildStreamClient() },
        { builder.buildHttp2StreamClient() },
    };
  }

  @Test(dataProvider = "badAddressClients")
  public void testBadAddress(AbstractNettyStreamClient client) throws InterruptedException, IOException, TimeoutException
  {
    RestRequest r = new RestRequestBuilder(URI.create("http://this.host.does.not.exist.linkedin.com")).build();
    FutureCallback<StreamResponse> cb = new FutureCallback<StreamResponse>();
    TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<StreamResponse>(cb);
    client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String, String>(), callback);
    try
    {
      cb.get(30, TimeUnit.SECONDS);
      Assert.fail("Get was supposed to fail");
    }
    catch (ExecutionException e)
    {
      verifyCauseChain(e, RemoteInvocationException.class, UnknownHostException.class);
    }
  }

  @DataProvider(name = "remoteClientAddressClients")
  public Object[][] remoteClientAddressClientsProvider()
  {
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler);
    return new Object[][] {
        { builder.buildStreamClient() },
        { builder.buildHttp2StreamClient() },
    };
  }

  @Test(dataProvider = "remoteClientAddressClients")
  public void testRequestContextAttributes(AbstractNettyStreamClient client)
      throws InterruptedException, IOException, TimeoutException
  {
    RestRequest r = new RestRequestBuilder(URI.create("http://localhost")).build();

    FutureCallback<StreamResponse> cb = new FutureCallback<>();
    TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<>(cb);
    RequestContext requestContext = new RequestContext();

    client.streamRequest(Messages.toStreamRequest(r), requestContext, new HashMap<>(), callback);

    final String actualRemoteAddress = (String) requestContext.getLocalAttr(R2Constants.REMOTE_SERVER_ADDR);
    final HttpProtocolVersion actualProtocolVersion = (HttpProtocolVersion) requestContext.getLocalAttr(R2Constants.HTTP_PROTOCOL_VERSION);

    Assert.assertTrue("127.0.0.1".equals(actualRemoteAddress) || "0:0:0:0:0:0:0:1".equals(actualRemoteAddress),
        "Actual remote client address is not expected. " + "The local attribute field must be IP address in string type" + actualRemoteAddress);
    if (client instanceof HttpNettyStreamClient)
    {
      Assert.assertEquals(actualProtocolVersion, HttpProtocolVersion.HTTP_1_1);
    }
    else if (client instanceof Http2NettyStreamClient)
    {
      Assert.assertEquals(actualProtocolVersion, HttpProtocolVersion.HTTP_2);
    }
    else
    {
      Assert.fail("Unexpected client instance type");
    }
  }

  @DataProvider(name = "responseSizeClients")
  public Object[][] responseSizeClientProvider()
  {
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler)
        .setRequestTimeout(50000)
        .setIdleTimeout(10000)
        .setShutdownTimeout(500)
        .setMaxResponseSize(TEST_MAX_RESPONSE_SIZE);
    return new Object[][] {
        { builder.buildStreamClient() },
        { builder.buildHttp2StreamClient() },
    };
  }

  @Test(dataProvider = "responseSizeClients")
  public void testMaxResponseSizeOK(AbstractNettyStreamClient client) throws Exception
  {
    testResponseSize(client, TEST_MAX_RESPONSE_SIZE - 1, RESPONSE_OK);

    testResponseSize(client, TEST_MAX_RESPONSE_SIZE, RESPONSE_OK);
  }

  @Test(dataProvider = "responseSizeClients")
  public void setTestMaxResponseSizeTooLarge(AbstractNettyStreamClient client) throws Exception
  {
    testResponseSize(client, TEST_MAX_RESPONSE_SIZE + 1, TOO_LARGE);
  }

  public void testResponseSize(AbstractNettyStreamClient client, int responseSize, int expectedResult) throws Exception
  {
    Server server = new HttpServerBuilder().responseSize(responseSize).build();
    try
    {
      server.start();
      RestRequest r = new RestRequestBuilder(new URI(URL)).build();
      FutureCallback<StreamResponse> cb = new FutureCallback<StreamResponse>();
      TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<StreamResponse>(cb);
      client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String, String>(), callback);

      StreamResponse response = cb.get(30, TimeUnit.SECONDS);
      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
      response.getEntityStream().setReader(new Reader()
      {
        @Override
        public void onInit(ReadHandle rh)
        {
          rh.request(Integer.MAX_VALUE);
        }

        @Override
        public void onDataAvailable(ByteString data)
        {
        }

        @Override
        public void onDone()
        {
          latch.countDown();
        }

        @Override
        public void onError(Throwable e)
        {
          error.set(e);
          latch.countDown();
        }
      });

      if (!latch.await(30, TimeUnit.SECONDS))
      {
        Assert.fail("Timeout waiting for response");
      }

      if(expectedResult == TOO_LARGE)
      {
        Assert.assertNotNull(error.get(), "Max response size exceeded, expected exception. ");
        verifyCauseChain(error.get(), TooLongFrameException.class);
      }
      if (expectedResult == RESPONSE_OK)
      {
        Assert.assertNull(error.get(), "Unexpected Exception: response size <= max size");
      }
    }
    catch (ExecutionException e)
    {
      if (expectedResult == RESPONSE_OK)
      {
        Assert.fail("Unexpected ExecutionException, response was <= max response size.", e);
      }
      verifyCauseChain(e, RemoteInvocationException.class, TooLongFrameException.class);
    }
    finally
    {
      server.stop();
    }
  }

  @DataProvider(name = "maxHeaderSizeClients")
  public Object[][] maxHeaderSizeClientProvider()
  {
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler)
        .setRequestTimeout(5000)
        .setIdleTimeout(10000)
        .setShutdownTimeout(500)
        .setMaxHeaderSize(TEST_MAX_HEADER_SIZE);
    return new Object[][] {
        { builder.buildStreamClient() }
    };
  }

  @Test(dataProvider = "maxHeaderSizeClients")
  public void testMaxHeaderSize(AbstractNettyStreamClient client) throws Exception
  {
    testHeaderSize(client, TEST_MAX_HEADER_SIZE - TEST_HEADER_SIZE_BUFFER, RESPONSE_OK);

    testHeaderSize(client, TEST_MAX_HEADER_SIZE + TEST_HEADER_SIZE_BUFFER, TOO_LARGE);
  }

  public void testHeaderSize(AbstractNettyStreamClient client, int headerSize, int expectedResult) throws Exception
  {
    Server server = new HttpServerBuilder().headerSize(headerSize).build();
    try
    {
      server.start();
      RestRequest r = new RestRequestBuilder(new URI(URL)).build();
      FutureCallback<StreamResponse> cb = new FutureCallback<StreamResponse>();
      TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<StreamResponse>(cb);
      client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String, String>(), callback);

      cb.get(300, TimeUnit.SECONDS);
      if (expectedResult == TOO_LARGE)
      {
        Assert.fail("Max header size exceeded, expected exception. ");
      }
    }
    catch (ExecutionException e)
    {
      if (expectedResult == RESPONSE_OK)
      {
        Assert.fail("Unexpected ExecutionException, header was <= max header size.", e);
      }

      if (client instanceof HttpNettyStreamClient)
      {
        verifyCauseChain(e, RemoteInvocationException.class, TooLongFrameException.class);
      }
      else if (client instanceof Http2NettyStreamClient)
      {
        verifyCauseChain(e, RemoteInvocationException.class, Http2Exception.class);
      }
      else
      {
        Assert.fail("Unrecognized client");
      }
    }
    finally
    {
      server.stop();
    }
  }

  @DataProvider(name = "shutdownClients")
  public Object[][] shutdownClientProvider()
  {
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler)
        .setRequestTimeout(500)
        .setIdleTimeout(10000)
        .setShutdownTimeout(500);
    return new Object[][] {
        { builder.buildStreamClient() },
        { builder.buildHttp2StreamClient() },
    };
  }

  @Test(dataProvider = "shutdownClients")
  public void testShutdown(AbstractNettyStreamClient client) throws Exception
  {
    FutureCallback<None> shutdownCallback = new FutureCallback<None>();
    client.shutdown(shutdownCallback);
    shutdownCallback.get(30, TimeUnit.SECONDS);

    // Now verify a new request will also fail
    RestRequest r = new RestRequestBuilder(URI.create("http://no.such.host.linkedin.com")).build();
    FutureCallback<StreamResponse> callback = new FutureCallback<StreamResponse>();
    client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String, String>(),
        new TransportCallbackAdapter<StreamResponse>(callback));
    try
    {
      callback.get(30, TimeUnit.SECONDS);
    }
    catch (ExecutionException e)
    {
      // Expected
    }
  }

  @Test
  public void testShutdownStuckInPool()
      throws InterruptedException, ExecutionException, TimeoutException

  {
    // Test that shutdown works when the outstanding request is stuck in the pool waiting for a channel
    HttpNettyStreamClient client = new HttpNettyStreamClient(new NoCreations(_scheduler), _scheduler, 60000, 1);

    RestRequest r = new RestRequestBuilder(URI.create("http://some.host/")).build();
    FutureCallback<StreamResponse> futureCallback = new FutureCallback<StreamResponse>();
    client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String, String>(), new TransportCallbackAdapter<StreamResponse>(futureCallback));

    FutureCallback<None> shutdownCallback = new FutureCallback<None>();
    client.shutdown(shutdownCallback);

    shutdownCallback.get(30, TimeUnit.SECONDS);

    try
    {
      futureCallback.get(30, TimeUnit.SECONDS);
      Assert.fail("get should have thrown exception");
    }
    catch (ExecutionException e)
    {
      verifyCauseChain(e, RemoteInvocationException.class, TimeoutException.class);
    }
  }

  @Test
  public void testShutdownRequestOutstanding() throws Exception
  {
    // Test that it works when the shutdown kills the outstanding request...
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler)
        .setShutdownTimeout(500)
        .setRequestTimeout(60000);
    testShutdownRequestOutstanding(builder.buildStreamClient(), RemoteInvocationException.class, TimeoutException.class);
    testShutdownRequestOutstanding(builder.buildHttp2StreamClient(), RemoteInvocationException.class, TimeoutException.class);
  }

  @Test
  public void testShutdownRequestOutstanding2() throws Exception
  {
    // Test that it works when the request timeout kills the outstanding request...
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler)
        .setShutdownTimeout(60000)
        .setRequestTimeout(500);
    testShutdownRequestOutstanding(builder.buildStreamClient(), RemoteInvocationException.class,
        // sometimes the test fails with ChannelClosedException
        // TimeoutException.class
        Exception.class);
    testShutdownRequestOutstanding(builder.buildHttp2StreamClient(), RemoteInvocationException.class,
        // sometimes the test fails with ChannelClosedException
        // TimeoutException.class
        Exception.class);
  }

  private void testShutdownRequestOutstanding(AbstractNettyStreamClient client, Class<?>... causeChain) throws Exception
  {
    CountDownLatch responseLatch = new CountDownLatch(1);
    Server server = new HttpServerBuilder().responseLatch(responseLatch).build();
    try
    {
      server.start();
      RestRequest r = new RestRequestBuilder(new URI(URL)).build();
      FutureCallback<StreamResponse> cb = new FutureCallback<StreamResponse>();
      TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<StreamResponse>(cb);
      client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String,String>(), callback);

      FutureCallback<None> shutdownCallback = new FutureCallback<None>();
      client.shutdown(shutdownCallback);
      shutdownCallback.get(30, TimeUnit.SECONDS);

      // This timeout needs to be significantly larger than the getTimeout of the netty client;
      // we're testing that the client will generate its own timeout
      cb.get(30, TimeUnit.SECONDS);
      Assert.fail("Get was supposed to time out");
    }
    catch (TimeoutException e)
    {
      // TimeoutException means the timeout for Future.get() elapsed and nothing happened.
      // Instead, we are expecting our callback to be invoked before the future timeout
      // with a timeout generated by the HttpNettyClient.
      Assert.fail("Get timed out, should have thrown ExecutionException", e);
    }
    catch (ExecutionException e)
    {
      verifyCauseChain(e, causeChain);
    }
    finally
    {
      responseLatch.countDown();
      server.stop();
    }
  }

  private static void verifyCauseChain(Throwable throwable, Class<?>... causes)
  {
    Throwable t = throwable;
    for (Class<?> c : causes)
    {
      Throwable cause = t.getCause();
      if (cause == null)
      {
        Assert.fail("Cause chain ended too early", throwable);
      }
      if (!c.isAssignableFrom(cause.getClass()))
      {
        Assert.fail("Expected cause " + c.getName() + " not " + cause.getClass().getName(), throwable);
      }
      t = cause;
    }
  }

  // Test that cannot pass pass SSLParameters without SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testClientPipelineFactory1()
      throws NoSuchAlgorithmException
  {
    try
    {
      new HttpClientBuilder(_eventLoop, _scheduler)
          .setSSLParameters(new SSLParameters()).buildStreamClient();
    }
    catch (IllegalArgumentException e)
    {
      // Check exception message to make sure it's the expected one.
      Assert.assertEquals(e.getMessage(), "SSLParameters passed with no SSLContext");
    }
  }

  // Test that cannot pass pass SSLParameters without SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testHttp2ClientPipelineFactory1()
      throws NoSuchAlgorithmException
  {
    try
    {
      new HttpClientBuilder(_eventLoop, _scheduler)
          .setSSLParameters(new SSLParameters()).buildHttp2StreamClient();
    }
    catch (IllegalArgumentException e)
    {
      // Check exception message to make sure it's the expected one.
      Assert.assertEquals(e.getMessage(), "SSLParameters passed with no SSLContext");
    }
  }

  // Test that cannot set cipher suites in SSLParameters that don't have any match in
  // SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testClientPipelineFactory2Fail()
      throws NoSuchAlgorithmException
  {
    String[] requestedCipherSuites = {"Unsupported"};
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setCipherSuites(requestedCipherSuites);
    try
    {
      new HttpClientBuilder(_eventLoop, _scheduler)
          .setSSLContext(SSLContext.getDefault())
          .setSSLParameters(sslParameters)
          .buildStreamClient();
    }
    catch (IllegalArgumentException e)
    {
      // Check exception message to make sure it's the expected one.
      Assert.assertEquals(e.getMessage(), "None of the requested cipher suites: [Unsupported] are found in SSLContext");
    }
  }

  // Test that cannot set cipher suites in SSLParameters that don't have any match in
  // SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testHttp2ClientPipelineFactory2Fail()
      throws NoSuchAlgorithmException
  {
    String[] requestedCipherSuites = {"Unsupported"};
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setCipherSuites(requestedCipherSuites);
    try
    {
      new HttpClientBuilder(_eventLoop, _scheduler)
          .setSSLContext(SSLContext.getDefault())
          .setSSLParameters(sslParameters)
          .buildHttp2StreamClient();
    }
    catch (IllegalArgumentException e)
    {
      // Check exception message to make sure it's the expected one.
      Assert.assertEquals(e.getMessage(), "None of the requested cipher suites: [Unsupported] are found in SSLContext");
    }
  }

  // Test that can set cipher suites in SSLParameters that have at least one match in
  // SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testClientPipelineFactory2Pass()
      throws NoSuchAlgorithmException
  {
    String[] requestedCipherSuites = {"Unsupported", "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA"};
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setCipherSuites(requestedCipherSuites);
    new HttpClientBuilder(_eventLoop, _scheduler)
        .setSSLContext(SSLContext.getDefault())
        .setSSLParameters(sslParameters).buildStreamClient();
  }

  // Test that can set cipher suites in SSLParameters that have at least one match in
  // SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testHttp2ClientPipelineFactory2Pass()
      throws NoSuchAlgorithmException
  {
    String[] requestedCipherSuites = {"Unsupported", "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA"};
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setCipherSuites(requestedCipherSuites);
    new HttpClientBuilder(_eventLoop, _scheduler)
        .setSSLContext(SSLContext.getDefault())
        .setSSLParameters(sslParameters)
        .buildHttp2StreamClient();
  }

  // Test that cannot set protocols in SSLParameters that don't have any match in
  // SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testClientPipelineFactory3Fail()
      throws NoSuchAlgorithmException
  {
    String[] requestedProtocols = {"Unsupported"};
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(requestedProtocols);
    try
    {
      new HttpClientBuilder(_eventLoop, _scheduler)
          .setSSLContext(SSLContext.getDefault())
          .setSSLParameters(sslParameters)
          .buildStreamClient();
    }
    catch (IllegalArgumentException e)
    {
      // Check exception message to make sure it's the expected one.
      Assert.assertEquals(e.getMessage(), "None of the requested protocols: [Unsupported] are found in SSLContext");
    }
  }

  // Test that cannot set protocols in SSLParameters that don't have any match in
  // SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testHttp2ClientPipelineFactory3Fail()
      throws NoSuchAlgorithmException
  {
    String[] requestedProtocols = {"Unsupported"};
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(requestedProtocols);
    try
    {
      new HttpClientBuilder(_eventLoop, _scheduler)
          .setSSLContext(SSLContext.getDefault())
          .setSSLParameters(sslParameters)
          .buildHttp2StreamClient();
    }
    catch (IllegalArgumentException e)
    {
      // Check exception message to make sure it's the expected one.
      Assert.assertEquals(e.getMessage(), "None of the requested protocols: [Unsupported] are found in SSLContext");
    }
  }

  // Test that can set protocols in SSLParameters that have at least one match in
  // SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testClientPipelineFactory3Pass()
      throws NoSuchAlgorithmException
  {
    String[] requestedProtocols = {"Unsupported", "TLSv1"};
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(requestedProtocols);

    new HttpClientBuilder(_eventLoop, _scheduler)
        .setSSLContext(SSLContext.getDefault())
        .setSSLParameters(sslParameters)
        .buildStreamClient();
  }

  // Test that can set protocols in SSLParameters that have at least one match in
  // SSLContext.
  // This in fact tests HttpClientPipelineFactory constructor through HttpNettyClient
  // constructor.
  @Test
  public void testHttp2ClientPipelineFactory3Pass()
      throws NoSuchAlgorithmException
  {
    String[] requestedProtocols = {"Unsupported", "TLSv1"};
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(requestedProtocols);

    new HttpClientBuilder(_eventLoop, _scheduler)
        .setSSLContext(SSLContext.getDefault())
        .setSSLParameters(sslParameters)
        .buildHttp2StreamClient();
  }

  @DataProvider(name = "poolStatsClients")
  public Object[][] poolStatsClientProvider()
  {
    final CountDownLatch setLatch = new CountDownLatch(1);
    final CountDownLatch removeLatch = new CountDownLatch(1);
    AbstractJmxManager manager = new AbstractJmxManager()
    {
      @Override
      public void onProviderCreate(PoolStatsProvider provider)
      {
        setLatch.countDown();
      }

      @Override
      public void onProviderShutdown(PoolStatsProvider provider)
      {
        removeLatch.countDown();
      }
    };
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler).setJmxManager(manager);
    return new Object[][] {
        { builder.buildStreamClient(), setLatch, removeLatch },
        { builder.buildHttp2StreamClient(), setLatch, removeLatch },
    };
  }

  @Test(dataProvider = "poolStatsClients")
  public void testPoolStatsProviderManager(
      AbstractNettyStreamClient client,
      CountDownLatch setLatch,
      CountDownLatch removeLatch)
      throws Exception
  {
    // test setPoolStatsProvider
    try
    {
      setLatch.await(30, TimeUnit.SECONDS);
    }
    catch (InterruptedException e)
    {
      Assert.fail("PoolStatsAware setPoolStatsProvider didn't get called when creating channel pool.");
    }
    // test removePoolStatsProvider
    FutureCallback<None> shutdownCallback = new FutureCallback<None>();
    client.shutdown(shutdownCallback);
    try
    {
      removeLatch.await(30, TimeUnit.SECONDS);
    }
    catch (InterruptedException e)
    {
      Assert.fail("PoolStatsAware removePoolStatsProvider didn't get called when shutting down channel pool.");
    }
    shutdownCallback.get(30, TimeUnit.SECONDS);
  }

  @Test (enabled = false)
  public void testMakingOutboundHttpsRequest()
      throws NoSuchAlgorithmException, InterruptedException, ExecutionException, TimeoutException
  {
    SSLContext context = SSLContext.getDefault();
    SSLParameters sslParameters = context.getDefaultSSLParameters();

    HttpNettyStreamClient client = new HttpClientBuilder(_eventLoop, _scheduler)
          .setSSLContext(context)
          .setSSLParameters(sslParameters)
          .buildStreamClient();

    RestRequest r = new RestRequestBuilder(URI.create("https://www.howsmyssl.com/a/check")).build();
    FutureCallback<StreamResponse> cb = new FutureCallback<StreamResponse>();
    TransportCallback<StreamResponse> callback = new TransportCallbackAdapter<StreamResponse>(cb);
    client.streamRequest(Messages.toStreamRequest(r), new RequestContext(), new HashMap<String, String>(), callback);
    cb.get(30, TimeUnit.SECONDS);
  }

  private static class NoCreations implements ChannelPoolFactory
  {
    private final ScheduledExecutorService _scheduler;

    public NoCreations(ScheduledExecutorService scheduler)
    {
      _scheduler = scheduler;
    }

    @Override
    public AsyncPool<Channel> getPool(SocketAddress address)
    {
      return new AsyncPoolImpl<Channel>("fake pool", new AsyncPool.Lifecycle<Channel>()
      {
        @Override
        public void create(Callback<Channel> channelCallback)
        {

        }

        @Override
        public boolean validateGet(Channel obj)
        {
          return false;
        }

        @Override
        public boolean validatePut(Channel obj)
        {
          return false;
        }

        @Override
        public void destroy(Channel obj, boolean error, Callback<Channel> channelCallback)
        {

        }

        @Override
        public PoolStats.LifecycleStats getStats()
        {
          return null;
        }
      }, 0, 0, _scheduler);
    }
  }

  @DataProvider(name = "requestResponseParameters")
  public Object[][] parametersProvider() {
    HttpClientBuilder builder = new HttpClientBuilder(_eventLoop, _scheduler);
    // Client, Request Method, Request Size, Response Size, RestOverStream
    return new Object[][] {
        { builder.buildHttp2StreamClient(), HTTP_GET, NO_CONTENT, NO_CONTENT, true },
        { builder.buildHttp2StreamClient(), HTTP_GET, NO_CONTENT, NO_CONTENT, false },
        { builder.buildHttp2StreamClient(), HTTP_GET, SMALL_CONTENT, SMALL_CONTENT, true },
        { builder.buildHttp2StreamClient(), HTTP_GET, SMALL_CONTENT, SMALL_CONTENT, false },
        { builder.buildHttp2StreamClient(), HTTP_GET, LARGE_CONTENT, LARGE_CONTENT, true },
        { builder.buildHttp2StreamClient(), HTTP_GET, LARGE_CONTENT, LARGE_CONTENT, false },
        { builder.buildHttp2StreamClient(), HTTP_POST, NO_CONTENT, NO_CONTENT, true },
        { builder.buildHttp2StreamClient(), HTTP_POST, NO_CONTENT, NO_CONTENT, false },
        { builder.buildHttp2StreamClient(), HTTP_POST, SMALL_CONTENT, SMALL_CONTENT, true },
        { builder.buildHttp2StreamClient(), HTTP_POST, SMALL_CONTENT, SMALL_CONTENT, false },
        { builder.buildHttp2StreamClient(), HTTP_POST, LARGE_CONTENT, LARGE_CONTENT, true },
        { builder.buildHttp2StreamClient(), HTTP_POST, LARGE_CONTENT, LARGE_CONTENT, false },
        { builder.buildStreamClient(), HTTP_GET, NO_CONTENT, NO_CONTENT, true },
        { builder.buildStreamClient(), HTTP_GET, NO_CONTENT, NO_CONTENT, false },
        { builder.buildStreamClient(), HTTP_GET, SMALL_CONTENT, SMALL_CONTENT, true },
        { builder.buildStreamClient(), HTTP_GET, SMALL_CONTENT, SMALL_CONTENT, false },
        { builder.buildStreamClient(), HTTP_GET, LARGE_CONTENT, LARGE_CONTENT, true },
        { builder.buildStreamClient(), HTTP_GET, LARGE_CONTENT, LARGE_CONTENT, false },
        { builder.buildStreamClient(), HTTP_POST, NO_CONTENT, NO_CONTENT, true },
        { builder.buildStreamClient(), HTTP_POST, NO_CONTENT, NO_CONTENT, false },
        { builder.buildStreamClient(), HTTP_POST, SMALL_CONTENT, SMALL_CONTENT, true },
        { builder.buildStreamClient(), HTTP_POST, SMALL_CONTENT, SMALL_CONTENT, false },
        { builder.buildStreamClient(), HTTP_POST, LARGE_CONTENT, LARGE_CONTENT, true },
        { builder.buildStreamClient(), HTTP_POST, LARGE_CONTENT, LARGE_CONTENT, false },
    };
  }

  /**
   * Tests implementations of {@link AbstractNettyStreamClient} with different request dimensions.
   *
   * @param client Client implementation of {@link AbstractNettyStreamClient}
   * @param method HTTP request method
   * @param requestSize Request content size
   * @param responseSize Response content size
   * @param isFullRequest Whether to buffer a full request before stream
   * @throws Exception
   */
  @Test(dataProvider = "requestResponseParameters")
  public void testStreamRequests(
      AbstractNettyStreamClient client,
      String method,
      int requestSize,
      int responseSize,
      boolean isFullRequest) throws Exception
  {
    AtomicInteger succeeded = new AtomicInteger(0);
    AtomicInteger failed = new AtomicInteger(0);
    Server server = new HttpServerBuilder().responseSize(responseSize).build();
    try
    {
      server.start();
      CountDownLatch latch = new CountDownLatch(REQUEST_COUNT);
      for (int i = 0; i < REQUEST_COUNT; i++)
      {
        StreamRequest request = new StreamRequestBuilder(new URI(URL)).setMethod(method)
            .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString())
            .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[requestSize]))));
        RequestContext context = new RequestContext();
        context.putLocalAttr(R2Constants.IS_FULL_REQUEST, isFullRequest);
        client.streamRequest(request, context, new HashMap<>(),
            new TransportCallbackAdapter<>(new Callback<StreamResponse>()
            {
              @Override
              public void onSuccess(StreamResponse response)
              {
                response.getEntityStream().setReader(new Reader()
                {
                  ReadHandle _rh;
                  int _consumed = 0;

                  @Override
                  public void onDataAvailable(ByteString data)
                  {
                    _consumed += data.length();
                    _rh.request(1);
                  }

                  @Override
                  public void onDone()
                  {
                    succeeded.incrementAndGet();
                    latch.countDown();
                  }

                  @Override
                  public void onError(Throwable e)
                  {
                    failed.incrementAndGet();
                    latch.countDown();
                  }

                  @Override
                  public void onInit(ReadHandle rh)
                  {
                    _rh = rh;
                    _rh.request(1);
                  }
                });
              }

              @Override
              public void onError(Throwable e)
              {
                failed.incrementAndGet();
                latch.countDown();
              }
            }));
      }

      if (!latch.await(30, TimeUnit.SECONDS))
      {
        Assert.fail("Timeout waiting for responses. " + succeeded + " requests succeeded and " + failed
            + " requests failed out of total " + REQUEST_COUNT + " requests");
      }

      Assert.assertEquals(latch.getCount(), 0);
      Assert.assertEquals(failed.get(), 0);
      Assert.assertEquals(succeeded.get(), REQUEST_COUNT);

      FutureCallback<None> shutdownCallback = new FutureCallback<>();
      client.shutdown(shutdownCallback);
      shutdownCallback.get(30, TimeUnit.SECONDS);
    }
    finally
    {
      server.stop();
    }
  }

  @Test(dataProvider = "requestResponseParameters", groups = TestGroupNames.TESTNG_GROUP_KNOWN_ISSUE)
  public void testCancelStreamRequests(
      AbstractNettyStreamClient client,
      String method,
      int requestSize,
      int responseSize,
      boolean isFullRequest) throws Exception
  {
    AtomicInteger succeeded = new AtomicInteger(0);
    AtomicInteger failed = new AtomicInteger(0);
    Server server = new HttpServerBuilder().responseSize(responseSize).build();
    try
    {
      server.start();
      CountDownLatch latch = new CountDownLatch(REQUEST_COUNT);
      for (int i = 0; i < REQUEST_COUNT; i++)
      {
        StreamRequest request = new StreamRequestBuilder(new URI(URL)).setMethod(method)
            .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString())
            .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(new byte[requestSize]))));
        RequestContext context = new RequestContext();
        context.putLocalAttr(R2Constants.IS_FULL_REQUEST, isFullRequest);
        client.streamRequest(request, context, new HashMap<>(),
            new TransportCallbackAdapter<>(new Callback<StreamResponse>()
            {
              @Override
              public void onSuccess(StreamResponse response)
              {
                response.getEntityStream().setReader(new Reader()
                {
                  @Override
                  public void onDataAvailable(ByteString data)
                  {
                  }

                  @Override
                  public void onDone()
                  {
                    failed.incrementAndGet();
                    latch.countDown();
                  }

                  @Override
                  public void onError(Throwable e)
                  {
                    failed.incrementAndGet();
                    latch.countDown();
                  }

                  @Override
                  public void onInit(ReadHandle rh)
                  {
                    rh.cancel();
                    succeeded.incrementAndGet();
                    latch.countDown();
                  }
                });
              }

              @Override
              public void onError(Throwable e)
              {
                failed.incrementAndGet();
                latch.countDown();
              }
            }));
      }

      if (!latch.await(30, TimeUnit.SECONDS))
      {
        Assert.fail("Timeout waiting for responses. " + succeeded + " requests succeeded and " + failed
            + " requests failed out of total " + REQUEST_COUNT + " requests");
      }

      Assert.assertEquals(latch.getCount(), 0);
      Assert.assertEquals(failed.get(), 0);
      Assert.assertEquals(succeeded.get(), REQUEST_COUNT);

      FutureCallback<None> shutdownCallback = new FutureCallback<>();
      client.shutdown(shutdownCallback);
      shutdownCallback.get(30, TimeUnit.SECONDS);
    }
    finally
    {
      server.stop();
    }
  }

  @Test(dataProvider = "requestResponseParameters", expectedExceptions = UnsupportedOperationException.class)
  public void testRestRequests(
      AbstractNettyStreamClient client,
      String method,
      int requestSize,
      int responseSize,
      boolean isFullRequest) throws Exception
  {
    Server server = new HttpServerBuilder().responseSize(responseSize).build();
    try
    {
      server.start();
      for (int i = 0; i < REQUEST_COUNT; i++)
      {
        RestRequest request = new RestRequestBuilder(new URI(URL)).setMethod(method)
            .setHeader(HttpHeaderNames.HOST.toString(), HOST_NAME.toString())
            .setEntity(ByteString.copy(new byte[requestSize]))
            .build();
        RequestContext context = new RequestContext();
        context.putLocalAttr(R2Constants.IS_FULL_REQUEST, isFullRequest);
        client.restRequest(request, context, new HashMap<>(),
            new TransportCallbackAdapter<>(new Callback<RestResponse>()
            {
              @Override
              public void onSuccess(RestResponse response)
              {
              }

              @Override
              public void onError(Throwable e)
              {
              }
            }));
      }
    }
    finally
    {
      server.stop();
    }
  }
}
