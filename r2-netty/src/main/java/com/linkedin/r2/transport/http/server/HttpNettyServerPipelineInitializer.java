/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.server;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.netty.common.SslHandlerUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;


public class HttpNettyServerPipelineInitializer extends ChannelInitializer<NioSocketChannel>
{
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
    SslHandlerUtil.validateSslParameters(_sslContext, _sslParameters);
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

    final SimpleChannelInboundHandler<RestRequest> restHandler = _restOverStream ?
        new PipelineStreamHandler(_dispatcher) : new PipelineRestHandler(_dispatcher);
    ch.pipeline().addLast(_eventExecutors, "handler", restHandler);
  }
}
