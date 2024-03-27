package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityReader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.Charsets;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.linkedin.r2.transport.http.client.HttpClientFactory.*;


public class TestPipelineV2NettyClient {
  private static final int TIMEOUT_MILLIS = 1_000;
  private static final int PORT = 8080;
  private static final String LOCALHOST = "http://localhost:" + PORT;

  private TestServer _server;
  private HttpClientFactory _clientFactory;
  private TransportClient _client;

  @BeforeMethod
  private void setup() {
    _server = new TestServer();
    _clientFactory = new HttpClientFactory.Builder().setUsePipelineV2(true).build();

    HashMap<String, String> clientProperties = new HashMap<>();
    clientProperties.put(HTTP_REQUEST_TIMEOUT, String.valueOf(TIMEOUT_MILLIS));
    clientProperties.put(HTTP_POOL_SIZE, "1");

    _client = _clientFactory.getClient(clientProperties);
  }

  @AfterMethod
  private void shutdown() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    FutureCallback<None> clientShutdown = new FutureCallback<>();
    FutureCallback<None> factoryShutdown = new FutureCallback<>();

    _client.shutdown(clientShutdown);
    _clientFactory.shutdown(factoryShutdown);

    clientShutdown.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    factoryShutdown.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

    _server.close();
  }

  /**
   * Test response returned before request complete.
   * Connection should not be returned to the pool until after the request payload has been fully uploaded.
   */
  @Test
  public void testResponseReturnedBeforeRequestComplete() throws Exception {
    DelayWriter delayWriter = new DelayWriter(new ByteStringWriter(ByteString.copyString("Hello!", Charsets.UTF_8)));

    verifyResponse(postRequest(EntityStreams.newEntityStream(delayWriter)));

    CompletableFuture<StreamResponse> secondResponseFuture = postRequest(EntityStreams.emptyStream());

    delayWriter.run();

    verifyResponse(secondResponseFuture);
  }

  private CompletableFuture<StreamResponse> postRequest(EntityStream body) throws URISyntaxException {
    StreamRequest streamRequest = new StreamRequestBuilder(new URI(LOCALHOST)).setMethod("POST").build(body);

    CompletableTransportCallback responseFutureCallback = new CompletableTransportCallback();
    _client.streamRequest(streamRequest, new RequestContext(), new HashMap<>(), responseFutureCallback);

    return responseFutureCallback;
  }

  private void verifyResponse(CompletableFuture<StreamResponse> responseFuture) throws Exception {
    StreamResponse response = responseFuture.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

    Assert.assertEquals(response.getStatus(), 200);

    FutureCallback<ByteString> responseBodyFuture = new FutureCallback<>();
    response.getEntityStream().setReader(new FullEntityReader(responseBodyFuture));

    String responseBody = responseBodyFuture.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).asString(StandardCharsets.UTF_8);
    Assert.assertEquals(responseBody, "GOOD");
  }

  @ChannelHandler.Sharable
  private static class TestServer extends ChannelInboundHandlerAdapter implements Closeable {
    private final NioEventLoopGroup _group = new NioEventLoopGroup();
    private final Channel _channel;

    public TestServer() {
      ChannelFuture channelFuture = new ServerBootstrap()
          .group(_group)
          .channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
              ch.pipeline().addLast(new HttpServerCodec(), TestServer.this);
            }
          })
          .bind(new InetSocketAddress(PORT));

      channelFuture.awaitUninterruptibly(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

      _channel = channelFuture.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof HttpRequest) {
        ByteBuf body = Unpooled.copiedBuffer("GOOD", Charsets.UTF_8);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, body);
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, body.readableBytes());
        ctx.writeAndFlush(response);
      }
    }

    @Override
    public void close() throws IOException {
      _channel.close().awaitUninterruptibly(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      _group.shutdownGracefully().awaitUninterruptibly(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }
  }

  private static class DelayWriter implements Writer {
    private final Writer _delegate;
    private final DelayExecutor _executor = new DelayExecutor();

    public DelayWriter(Writer delegate) {
      _delegate = delegate;
    }

    public void run() throws InterruptedException {
      _executor.run();
    }

    @Override
    public void onInit(WriteHandle wh) {
      _executor.execute(() -> _delegate.onInit(new WriteHandle() {
        @Override
        public void write(ByteString data) {
          wh.write(data);
        }

        @Override
        public void done() {
          wh.done();
          _executor.shutdown();
        }

        @Override
        public void error(Throwable throwable) {
          wh.error(throwable);
          _executor.shutdown();
        }

        @Override
        public int remaining() {
          return wh.remaining();
        }
      }));
    }

    @Override
    public void onWritePossible() {
      _executor.execute(_delegate::onWritePossible);
    }

    @Override
    public void onAbort(Throwable e) {
      _executor.execute(() -> _delegate.onAbort(e));
      _executor.shutdown();
    }
  }

  private static class DelayExecutor implements Executor {
    private static final Runnable TERMINATE = () -> {};
    private final BlockingQueue<Runnable> _tasks = new LinkedBlockingQueue<>();
    private final Thread _thread = new Thread(() -> {
      try {
        Runnable task;
        while ((task = _tasks.take()) != TERMINATE) {
          task.run();
        }
      } catch (InterruptedException ignored) {
      }
    });

    @Override
    public void execute(Runnable command) {
      _tasks.add(command);
    }

    public void run() throws InterruptedException {
      _thread.start();
      _thread.join();
    }

    public void shutdown() {
      _tasks.add(TERMINATE);
    }
  }

  private static class CompletableTransportCallback extends CompletableFuture<StreamResponse>
                                                    implements TransportCallback<StreamResponse> {
    @Override
    public void onResponse(TransportResponse<StreamResponse> response) {
      if (response.hasError()) {
        completeExceptionally(response.getError());
      } else {
        complete(response.getResponse());
      }
    }
  }
}
