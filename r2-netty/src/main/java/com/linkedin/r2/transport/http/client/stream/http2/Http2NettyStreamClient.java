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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client.stream.http2;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.common.RequestWithCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.client.AbstractJmxManager;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.TimeoutAsyncPoolHandle;
import com.linkedin.r2.transport.http.client.TimeoutTransportCallback;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import com.linkedin.r2.transport.http.client.stream.AbstractNettyStreamClient;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.util.Cancellable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steven Ihde
 * @author Ang Xu
 * @author Zhenkai Zhu
 * @author Sean Sheng
 */

public class Http2NettyStreamClient extends AbstractNettyStreamClient
{
  static final Logger LOG = LoggerFactory.getLogger(Http2NettyStreamClient.class);

  /**
   * Creates a new Http2NettyStreamClient
   *
   * @param eventLoopGroup            The NioEventLoopGroup; it is the caller's responsibility to
   *                                  shut it down
   * @param scheduler                  An executor; it is the caller's responsibility to shut it down
   * @param requestTimeout            Timeout, in ms, to get a connection from the pool or create one
   * @param shutdownTimeout           Timeout, in ms, the client should wait after shutdown is
   *                                  initiated before terminating outstanding requests
   * @param callbackExecutors         An optional EventExecutorGroup to invoke user callback
   * @param jmxManager                A management class that is aware of the creation/shutdown event
   *                                  of the underlying {@link ChannelPoolManager}
   * @param channelPoolManager        channelPoolManager instance to retrieve http only channels
   * @param sslChannelPoolManager     channelPoolManager instance to retrieve https only connection
   */
  public Http2NettyStreamClient(NioEventLoopGroup eventLoopGroup, ScheduledExecutorService scheduler,
                                long requestTimeout, long shutdownTimeout,
                                ExecutorService callbackExecutors,
                                AbstractJmxManager jmxManager,
                                ChannelPoolManager channelPoolManager,
                                ChannelPoolManager sslChannelPoolManager)
  {
    super(eventLoopGroup, scheduler, requestTimeout, shutdownTimeout, callbackExecutors,
      jmxManager, channelPoolManager, sslChannelPoolManager);
  }

  @Override
  protected void doWriteRequestWithWireAttrHeaders(Request request, final RequestContext requestContext, SocketAddress address,
                                                   Map<String, String> wireAttrs, TimeoutTransportCallback<StreamResponse> callback)
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

    requestContext.putLocalAttr(R2Constants.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_2);

    Callback<Channel> getCallback = new ChannelPoolGetCallback(pool, request, requestContext, callback);
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

    ChannelPoolGetCallback(AsyncPool<Channel> pool, Request request, RequestContext requestContext, TimeoutTransportCallback<StreamResponse> callback)
    {
      _pool = pool;
      _request = request;
      _requestContext = requestContext;
      _callback = callback;
    }

    @Override
    public void onSuccess(Channel channel)
    {
      State state = _state.get();
      if (state == AbstractNettyStreamClient.State.REQUESTS_STOPPING || state == AbstractNettyStreamClient.State.SHUTDOWN)
      {
        // In this case, we acquired a channel from the pool as request processing is halting.
        // The shutdown task might not timeout this callback, since it may already have scanned
        // all the channels for pending requests before we set the callback as the channel
        // attachment.  The TimeoutTransportCallback ensures the user callback in never
        // invoked more than once, so it is safe to invoke it unconditionally.
        _callback.onResponse(TransportResponseImpl.error(new TimeoutException("Operation did not complete before shutdown")));
        return;
      }

      SslSessionValidator expectedCertPrincipal = (SslSessionValidator) _requestContext.getLocalAttr(R2Constants.REQUESTED_SSL_SESSION_VALIDATOR);
      if (expectedCertPrincipal != null)
      {
        LOG.warn("Verification of server's certificate is not supported yet on a Http2 connection and SSL, " +
          "the requirement will be ignored");
      }

      // By wrapping the channel and the pool in a timeout handle we can guarantee the following
      // 1. using the handle is the only mean to return a channel back to the pool because the reference to the
      //    channel pool is not otherwise passed along
      // 2. the channel can be returned back to the pool at most once through the handle
      // 3. the channel will eventually be returned to the pool due to timeout of handle
      TimeoutAsyncPoolHandle<Channel> handle = new TimeoutAsyncPoolHandle<>(
        _pool, _scheduler, _requestTimeout, TimeUnit.MILLISECONDS, channel);

      RequestWithCallback<Request, TimeoutTransportCallback<StreamResponse>, TimeoutAsyncPoolHandle<Channel>> request =
        new RequestWithCallback<>(_request, _callback, handle);

      // here we want the exception in outbound operations to be passed back through pipeline so that
      // the user callback would be invoked with the exception and the channel can be put back into the pool
      channel.writeAndFlush(request).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void onError(Throwable e)
    {
      _callback.onResponse(TransportResponseImpl.error(e));
    }
  }
}
