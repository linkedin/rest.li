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
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.http.client.AbstractJmxManager;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.TimeoutCallback;
import com.linkedin.r2.transport.http.client.TimeoutTransportCallback;
import com.linkedin.r2.transport.http.client.common.AbstractNettyClient;
import com.linkedin.r2.transport.http.client.common.ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.client.common.SslRequestHandler;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.r2.util.TimeoutRunnable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Steven Ihde
 * @author Ang Xu
 */

public class HttpNettyClient extends AbstractNettyClient<RestRequest, RestResponse>
{
  static final Logger LOG = LoggerFactory.getLogger(HttpNettyClient.class);
  private final ExecutorService _callbackExecutors;

  /**
   * Creates a new HttpNettyClient
   *
   * @param eventLoopGroup            The NioEventLoopGroup; it is the caller's responsibility to
   *                                  shut it down
   * @param executor                  An executor; it is the caller's responsibility to shut it down
   * @param requestTimeout            Timeout, in ms, to get a connection from the pool or create one
   * @param shutdownTimeout           Timeout, in ms, the client should wait after shutdown is
   *                                  initiated before terminating outstanding requests
   * @param callbackExecutors         An optional EventExecutorGroup to invoke user callback
   * @param jmxManager                A management class that is aware of the creation/shutdown event
   *                                  of the underlying {@link ChannelPoolManager}
   * @param channelPoolManager        channelPoolManager instance to use in the factory
   */
  public HttpNettyClient(NioEventLoopGroup eventLoopGroup,
                         ScheduledExecutorService executor,
                         long requestTimeout,
                         long shutdownTimeout,
                         ExecutorService callbackExecutors,
                         AbstractJmxManager jmxManager,
                         ChannelPoolManager channelPoolManager)
  {
    super(executor, requestTimeout, shutdownTimeout, jmxManager, channelPoolManager);
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

  protected void doShutdown(final Callback<None> callback) {
    final long deadline = System.currentTimeMillis() + _shutdownTimeout;
    TimeoutCallback<None> closeChannels =
        new TimeoutCallback<>(_scheduler, _shutdownTimeout, TimeUnit.MILLISECONDS, new Callback<None>() {
          private void finishShutdown() {
            _state.set(State.REQUESTS_STOPPING);
            // Timeout any waiters which haven't received a Channel yet
            for (Callback<Channel> callback : _channelPoolManager.cancelWaiters()) {
              callback.onError(new TimeoutException("Operation did not complete before shutdown"));
            }

            // Timeout any requests still pending response
            for (Channel c : _allChannels) {
              TransportCallback<RestResponse> callback = c.attr(RAPResponseHandler.CALLBACK_ATTR_KEY).getAndSet(null);
              if (callback != null) {
                errorResponse(callback, new TimeoutException("Operation did not complete before shutdown"));
              }
            }

            // Close all active and idle Channels
            final TimeoutRunnable afterClose =
                new TimeoutRunnable(_scheduler, deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS, () -> {
                  _state.set(State.SHUTDOWN);
                  LOG.info("Shutdown complete");
                  callback.onSuccess(None.none());
                }, "Timed out waiting for channels to close, continuing shutdown");
            _allChannels.close().addListener((ChannelGroupFutureListener) channelGroupFuture -> {
              if (!channelGroupFuture.isSuccess()) {
                LOG.warn("Failed to close some connections, ignoring");
              }
              afterClose.run();
            });
          }

          @Override
          public void onSuccess(None none) {
            LOG.info("All connection pools shut down, closing all channels");
            finishShutdown();
          }

          @Override
          public void onError(Throwable e) {
            LOG.warn("Error shutting down HTTP connection pools, ignoring and continuing shutdown", e);
            finishShutdown();
          }
        }, "Connection pool shutdown timeout exceeded (" + _shutdownTimeout + "ms)");
    _channelPoolManager.shutdown(closeChannels);
  }

  @Override
  protected TransportCallback<RestResponse> getExecutionCallback(TransportCallback<RestResponse> callback)
  {
    return new ExecutionCallback<>(_callbackExecutors, callback);
  }

  @Override
  protected void doWriteRequest(RestRequest request, RequestContext requestContext, SocketAddress address,
                                Map<String, String> wireAttrs, TimeoutTransportCallback<RestResponse> callback) {

    final RestRequest newRequest = new RestRequestBuilder(request)
        .overwriteHeaders(WireAttributeHelper.toWireAttributes(wireAttrs))
        .build();

    requestContext.putLocalAttr(R2Constants.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1);

    final AsyncPool<Channel> pool;
    try
    {
      pool = _channelPoolManager.getPoolForAddress(address);
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

        // This handler invokes the callback with the response once it arrives.
        channel.attr(RAPResponseHandler.CALLBACK_ATTR_KEY).set(callback);

        // Set the expected value by the user of the cert principal name
        String expectedCertPrincipal = (String) requestContext.getLocalAttr(R2Constants.EXPECTED_CERT_PRINCIPAL_NAME);
        channel.attr(SslRequestHandler.EXPECTED_CERT_PRINCIPAL_ATTR_KEY).set(expectedCertPrincipal);

        final State state = _state.get();
        if (state == State.REQUESTS_STOPPING || state == State.SHUTDOWN)
        {
          // In this case, we acquired a channel from the pool as request processing is halting.
          // The shutdown task might not timeout this callback, since it may already have scanned
          // all the channels for pending requests before we set the callback as the channel
          // attachment.  The TimeoutTransportCallback ensures the user callback in never
          // invoked more than once, so it is safe to invoke it unconditionally.
          errorResponse(callback,
            new TimeoutException("Operation did not complete before shutdown"));
          return;
        }

        // here we want the exception in outbound operations to be passed back through pipeline so that
        // the user callback would be invoked with the exception and the channel can be put back into the pool
        channel.writeAndFlush(newRequest).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
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
