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

package com.linkedin.r2.transport.http.client.rest;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.netty.common.NettyClientState;
import com.linkedin.r2.netty.handler.common.SslHandshakeTimingHandler;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.http.client.AbstractJmxManager;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.TimeoutTransportCallback;
import com.linkedin.r2.transport.http.client.common.AbstractNettyClient;
import com.linkedin.r2.transport.http.client.common.ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.client.common.ErrorChannelFutureListener;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.pegasus.io.netty.channel.Channel;
import com.linkedin.pegasus.io.netty.channel.EventLoopGroup;
import com.linkedin.pegasus.io.netty.util.concurrent.DefaultEventExecutorGroup;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * @author Steven Ihde
 * @author Ang Xu
 */

public class HttpNettyClient extends AbstractNettyClient<RestRequest, RestResponse>
{
  private final ExecutorService _callbackExecutors;

  /**
   * Creates a new HttpNettyClient
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
  public HttpNettyClient(EventLoopGroup eventLoopGroup,
                         ScheduledExecutorService executor,
                         long requestTimeout,
                         long shutdownTimeout,
                         ExecutorService callbackExecutors,
                         AbstractJmxManager jmxManager,
                         ChannelPoolManager channelPoolManager,
                         ChannelPoolManager sslChannelPoolManager)
  {
    super(executor, requestTimeout, shutdownTimeout, jmxManager, channelPoolManager, sslChannelPoolManager);
    _callbackExecutors = callbackExecutors == null ? eventLoopGroup : callbackExecutors;
  }

  /* Constructor for test purpose ONLY. */
  public HttpNettyClient(ChannelPoolFactory factory, ScheduledExecutorService executor, int requestTimeout,
      int shutdownTimeout)
  {
    super(factory, executor, requestTimeout, shutdownTimeout);
    _callbackExecutors = new DefaultEventExecutorGroup(1);
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
                            TransportCallback<StreamResponse> callback)
  {
    throw new UnsupportedOperationException("Stream is not supported.");
  }

  @Override
  protected TransportCallback<RestResponse> getExecutionCallback(TransportCallback<RestResponse> callback)
  {
    return new ExecutionCallback<>(_callbackExecutors, callback);
  }

  @Override
  protected void doWriteRequest(RestRequest request, RequestContext requestContext, SocketAddress address,
                                Map<String, String> wireAttrs, final TimeoutTransportCallback<RestResponse> callback,
                                long requestTimeout) {

    final RestRequest newRequest = new RestRequestBuilder(request)
        .overwriteHeaders(WireAttributeHelper.toWireAttributes(wireAttrs))
        .build();

    requestContext.putLocalAttr(R2Constants.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1);

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

    final Cancellable pendingGet = pool.get(new Callback<Channel>()
    {
      @Override
      public void onSuccess(final Channel channel)
      {
        // This handler ensures the channel is returned to the pool at the end of the
        // Netty pipeline.
        channel.attr(ChannelPoolHandler.CHANNEL_POOL_ATTR_KEY).set(pool);
        callback.addTimeoutTask(() ->
        {
          AsyncPool<Channel> pool1 = channel.attr(ChannelPoolHandler.CHANNEL_POOL_ATTR_KEY).getAndSet(null);
          if (pool1 != null)
          {
            pool1.dispose(channel);
          }
        });

        TransportCallback<RestResponse> sslTimingCallback = SslHandshakeTimingHandler.getSslTimingCallback(channel, requestContext, callback);

        // This handler invokes the callback with the response once it arrives.
        channel.attr(RAPResponseHandler.CALLBACK_ATTR_KEY).set(sslTimingCallback);

        // Set the session validator requested by the user
        SslSessionValidator sslSessionValidator = (SslSessionValidator) requestContext.getLocalAttr(R2Constants.REQUESTED_SSL_SESSION_VALIDATOR);
        channel.attr(NettyChannelAttributes.SSL_SESSION_VALIDATOR).set(sslSessionValidator);

        final NettyClientState state = _state.get();
        if (state == NettyClientState.REQUESTS_STOPPING || state == NettyClientState.SHUTDOWN)
        {
          // In this case, we acquired a channel from the pool as request processing is halting.
          // The shutdown task might not timeout this callback, since it may already have scanned
          // all the channels for pending requests before we set the callback as the channel
          // attachment.  The TimeoutTransportCallback ensures the user callback in never
          // invoked more than once, so it is safe to invoke it unconditionally.
          errorResponse(sslTimingCallback,
            new TimeoutException("Operation did not complete before shutdown"));

          // The channel is usually release in two places: timeout or in the netty pipeline.
          // Since we call the callback above, the timeout associated will be never invoked. On top of that
          // we never send the request to the pipeline (due to the return statement), and nobody is releasing the channel
          // until the channel is forcefully closed by the shutdownTimeout. Therefore we have to release it here
          AsyncPool<Channel> pool = channel.attr(ChannelPoolHandler.CHANNEL_POOL_ATTR_KEY).getAndSet(null);
          if (pool != null)
          {
            pool.put(channel);
          }
          return;
        }

        // here we want the exception in outbound operations to be passed back through pipeline so that
        // the user callback would be invoked with the exception and the channel can be put back into the pool
        channel.writeAndFlush(newRequest).addListener(new ErrorChannelFutureListener());
      }

      @Override
      public void onError(Throwable e)
      {
        errorResponse(callback, e);
      }
    });
    if (pendingGet != null)
    {
      callback.addTimeoutTask(pendingGet::cancel);
    }
  }
}
