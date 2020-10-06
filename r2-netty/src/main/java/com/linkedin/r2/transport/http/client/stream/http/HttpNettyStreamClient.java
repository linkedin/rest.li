/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.stream.http;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.netty.common.NettyClientState;
import com.linkedin.r2.netty.handler.common.SslHandshakeTimingHandler;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.client.AbstractJmxManager;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.TimeoutTransportCallback;
import com.linkedin.r2.transport.http.client.common.ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.client.common.ErrorChannelFutureListener;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import com.linkedin.r2.transport.http.client.stream.AbstractNettyStreamClient;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.r2.util.Timeout;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Steven Ihde
 * @author Ang Xu
 * @author Zhenkai Zhu
 */

public class HttpNettyStreamClient extends AbstractNettyStreamClient
{

  /**
   * Creates a new HttpNettyStreamClient
   *
   * @param eventLoopGroup            The EventLoopGroup; it is the caller's responsibility to shut
   *                                  it down
   * @param executor                  An executor; it is the caller's responsibility to shut it down
   * @param requestTimeout            Timeout, in ms, to get a connection from the pool or create one
   * @param shutdownTimeout           Timeout, in ms, the client should wait after shutdown is
   *                                  initiated before terminating outstanding requests
   * @param callbackExecutors         An optional EventExecutorGroup to invoke user callback
   * @param jmxManager                A management class that is aware of the creation/shutdown event
   *                                  of the underlying {@link ChannelPoolManager}
   * @param channelPoolManager        channelPoolManager instance to retrieve http only channels
   * @param sslChannelPoolManager     channelPoolManager instance to retrieve https only connection
   */
  public HttpNettyStreamClient(EventLoopGroup eventLoopGroup,
                               ScheduledExecutorService executor,
                               long requestTimeout,
                               long shutdownTimeout,
                               ExecutorService callbackExecutors,
                               AbstractJmxManager jmxManager,
                               ChannelPoolManager channelPoolManager,
                               ChannelPoolManager sslChannelPoolManager)
  {
    super(eventLoopGroup, executor, requestTimeout, shutdownTimeout, callbackExecutors,
      jmxManager, channelPoolManager, sslChannelPoolManager);
  }

  /* Constructor for test purpose ONLY. */
  public HttpNettyStreamClient(ChannelPoolFactory factory,
                        ScheduledExecutorService executor,
                        int requestTimeout,
                        int shutdownTimeout)
  {
    super(factory, executor, requestTimeout, shutdownTimeout);
  }

  @Override
  protected void doWriteRequestWithWireAttrHeaders(Request request, RequestContext requestContext, SocketAddress address,
                                                   Map<String, String> wireAttrs,
                                                   TimeoutTransportCallback<StreamResponse> callback, long requestTimeout)
  {
    final AsyncPool<Channel> pool;
    try
    {
      pool = getChannelPoolManagerPerRequest(request).getPoolForAddress(address);
    }
    catch (IllegalStateException e)
    {
      errorResponse(callback, e);
      return;
    }

    requestContext.putLocalAttr(R2Constants.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1);

    Callback<Channel> getCallback = new ChannelPoolGetCallback(pool, request, requestContext, callback, requestTimeout);
    final Cancellable pendingGet = pool.get(getCallback);
    if (pendingGet != null)
    {
      callback.addTimeoutTask(pendingGet::cancel);
    }
  }

  private class ChannelPoolGetCallback implements Callback<Channel>
  {
    private final AsyncPool<Channel> _pool;
    private final Request _request;
    private RequestContext _requestContext;
    private final TimeoutTransportCallback<StreamResponse> _callback;
    private final long _requestTimeout;

    ChannelPoolGetCallback(AsyncPool<Channel> pool, Request request, RequestContext requestContext, TimeoutTransportCallback<StreamResponse> callback, long requestTimeout)
    {
      _pool = pool;
      _request = request;
      _requestContext = requestContext;
      _callback = callback;
      _requestTimeout = requestTimeout;
    }

    @Override
    public void onSuccess(final Channel channel)
    {
      // This handler ensures the channel is returned to the pool at the end of the
      // Netty pipeline.
      channel.attr(ChannelPoolStreamHandler.CHANNEL_POOL_ATTR_KEY).set(_pool);
      _callback.addTimeoutTask(() -> {
        AsyncPool<Channel> pool = channel.attr(ChannelPoolStreamHandler.CHANNEL_POOL_ATTR_KEY).getAndSet(null);
        if (pool != null)
        {
          pool.dispose(channel);
        }
      });

      Timeout<None> streamingTimeout = new Timeout<>(_scheduler, _requestTimeout, TimeUnit.MILLISECONDS, None.none());
      _callback.addTimeoutTask(() -> {
        Timeout<None> timeout = channel.attr(RAPStreamResponseDecoder.TIMEOUT_ATTR_KEY).getAndSet(null);
        if (timeout != null)
        {
          // stop the timeout for streaming since streaming of response would not happen
          timeout.getItem();
        }
      });

      TransportCallback<StreamResponse> sslTimingCallback = SslHandshakeTimingHandler.getSslTimingCallback(channel, _requestContext, _callback);

      // This handler invokes the callback with the response once it arrives.
      channel.attr(RAPStreamResponseHandler.CALLBACK_ATTR_KEY).set(sslTimingCallback);
      channel.attr(RAPStreamResponseDecoder.TIMEOUT_ATTR_KEY).set(streamingTimeout);

      // Set the session validator requested by the user
      SslSessionValidator sslSessionValidator = (SslSessionValidator) _requestContext.getLocalAttr(R2Constants.REQUESTED_SSL_SESSION_VALIDATOR);
      channel.attr(NettyChannelAttributes.SSL_SESSION_VALIDATOR).set(sslSessionValidator);

      NettyClientState state = _state.get();
      if (state == NettyClientState.REQUESTS_STOPPING || state == NettyClientState.SHUTDOWN)
      {
        // In this case, we acquired a channel from the pool as request processing is halting.
        // The shutdown task might not timeout this callback, since it may already have scanned
        // all the channels for pending requests before we set the callback as the channel
        // attachment.  The TimeoutTransportCallback ensures the user callback in never
        // invoked more than once, so it is safe to invoke it unconditionally.
        _callback.onResponse(TransportResponseImpl.error(
          new TimeoutException("Operation did not complete before shutdown")));

        // The channel is usually release in two places: timeout or in the netty pipeline.
        // Since we call the callback above, the timeout associated will be never invoked. On top of that
        // we never send the request to the pipeline (due to the return statement), and nobody is releasing the channel
        // until the channel is forcefully closed by the shutdownTimeout. Therefore we have to release it here
        AsyncPool<Channel> pool = channel.attr(ChannelPoolStreamHandler.CHANNEL_POOL_ATTR_KEY).getAndSet(null);
        if (pool != null)
        {
          pool.put(channel);
        }
        return;
      }

      // here we want the exception in outbound operations to be passed back through pipeline so that
      // the user callback would be invoked with the exception and the channel can be put back into the pool
      channel.writeAndFlush(_request).addListener(new ErrorChannelFutureListener());
    }

    @Override
    public void onError(Throwable e)
    {
      _callback.onResponse(TransportResponseImpl.error(e));
    }
  }
}