package com.linkedin.r2.transport.http.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.util.SslHandlerUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import java.util.Collections;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpNettyServerPipelineInitializer extends ChannelInitializer<NioSocketChannel>
{
  private static final Logger LOG = LoggerFactory.getLogger(HttpNettyServerPipelineInitializer.class);

  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;
  private final EventExecutorGroup _eventExecutors;
  private final boolean _restOverStream;
  private final HttpDispatcher _dispatcher;


  HttpNettyServerPipelineInitializer(HttpDispatcher dispatcher, EventExecutorGroup eventExecutors,
                                     SSLContext sslContext, SSLParameters sslParameters,
                                     boolean restOverStream)
  {
    _dispatcher = dispatcher;
    _sslContext = sslContext;
    _sslParameters = sslParameters;
    _eventExecutors = eventExecutors;
    _restOverStream = restOverStream;
  }

  @Override
  protected void initChannel(NioSocketChannel ch) throws Exception
  {
    // If _sslContext is not NULL, we should first add SSL handler to the pipeline to secure the channel.
    if (_sslContext != null)
    {
      final SslHandler sslHandler = SslHandlerUtil.getServerSslHandler(_sslContext, _sslParameters);
      ch.pipeline().addLast(SslHandlerUtil.PIPELINE_SSL_HANDLER, sslHandler);
    }
    ch.pipeline().addLast("decoder", new HttpRequestDecoder());
    ch.pipeline().addLast("aggregator", new HttpObjectAggregator(1048576));
    ch.pipeline().addLast("encoder", new HttpResponseEncoder());
    ch.pipeline().addLast("rapi", new RAPServerCodec());
    ch.pipeline().addLast(_eventExecutors, "handler", _restOverStream ? new StreamHandler() : new RestHandler());
  }

  private class RestHandler extends SimpleChannelInboundHandler<RestRequest>
  {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RestRequest request) throws Exception
    {
      final Channel ch = ctx.channel();
      TransportCallback<RestResponse> writeResponseCallback = new TransportCallback<RestResponse>()
      {
        @Override
        public void onResponse(TransportResponse<RestResponse> response)
        {
          final RestResponseBuilder responseBuilder;
          if (response.hasError())
          {
            // This onError is only getting called in cases where:
            // (1) the exception was thrown by the handleRequest() method, and the upper layer
            // dispatcher did not catch the exception or caught it and passed it here without
            // turning it into a Response, or
            // (2) the HttpBridge-installed callback's onError declined to convert the exception to a
            // response and passed it along to here.
            responseBuilder =
                new RestResponseBuilder(RestStatus.responseForError(RestStatus.INTERNAL_SERVER_ERROR, response.getError()));
          }
          else
          {
            responseBuilder = new RestResponseBuilder(response.getResponse());
          }

          responseBuilder
              .unsafeOverwriteHeaders(WireAttributeHelper.toWireAttributes(response.getWireAttributes()))
              .build();

          ch.writeAndFlush(responseBuilder.build());
        }
      };
      try
      {
        _dispatcher.handleRequest(request, writeResponseCallback);
      }
      catch (Exception ex)
      {
        writeResponseCallback.onResponse(TransportResponseImpl.<RestResponse> error(ex, Collections.<String, String> emptyMap()));
      }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
      LOG.error("Exception caught on channel: " + ctx.channel().remoteAddress(), cause);
      ctx.close();
    }
  }

  private class StreamHandler extends SimpleChannelInboundHandler<RestRequest>
  {
    private void writeError(Channel ch, TransportResponse<StreamResponse> response, Throwable ex)
    {
      RestResponseBuilder responseBuilder =
          new RestResponseBuilder(RestStatus.responseForError(RestStatus.INTERNAL_SERVER_ERROR, ex))
              .unsafeOverwriteHeaders(WireAttributeHelper.toWireAttributes(response.getWireAttributes()));

      ch.writeAndFlush(responseBuilder.build());
    }

    private void writeResponse(Channel ch, TransportResponse<StreamResponse> response,  RestResponse restResponse)
    {
      RestResponseBuilder responseBuilder = restResponse.builder()
          .unsafeOverwriteHeaders(WireAttributeHelper.toWireAttributes(response.getWireAttributes()));

      ch.writeAndFlush(responseBuilder.build());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RestRequest request) throws Exception
    {
      final Channel ch = ctx.channel();
      TransportCallback<StreamResponse> writeResponseCallback = new TransportCallback<StreamResponse>()
      {
        @Override
        public void onResponse(final TransportResponse<StreamResponse> response)
        {

          if (response.hasError())
          {
            // This onError is only getting called in cases where:
            // (1) the exception was thrown by the handleRequest() method, and the upper layer
            // dispatcher did not catch the exception or caught it and passed it here without
            // turning it into a Response, or
            // (2) the HttpBridge-installed callback's onError declined to convert the exception to a
            // response and passed it along to here.
            writeError(ch, response, response.getError());
          }
          else
          {
            Messages.toRestResponse(response.getResponse(), new Callback<RestResponse>()
            {
              @Override
              public void onError(Throwable e)
              {
                writeError(ch, response, e);
              }

              @Override
              public void onSuccess(RestResponse result)
              {
                writeResponse(ch, response, result);
              }
            });
          }
        }
      };
      try
      {
        _dispatcher.handleRequest(Messages.toStreamRequest(request), writeResponseCallback);
      }
      catch (Exception ex)
      {
        writeResponseCallback.onResponse(TransportResponseImpl.<StreamResponse> error(ex,
            Collections.<String, String> emptyMap()));
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
      LOG.error("Exception caught on channel: " + ctx.channel().remoteAddress(), cause);
      ctx.close();
    }
  }
}
