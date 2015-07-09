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
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.MessageType;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.common.HttpBridge;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.r2.util.TimeoutRunnable;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steven Ihde
 * @author Ang Xu
 * @version $Revision: $
 */

/* package private */ class HttpNettyClient implements TransportClient
{
  static final Logger LOG = LoggerFactory.getLogger(HttpNettyClient.class);
  private static final int HTTP_DEFAULT_PORT = 80;
  private static final int HTTPS_DEFAULT_PORT = 443;

  private final ChannelPoolManager _channelPoolManager;
  private final ChannelGroup _allChannels;

  private final ChannelPoolHandler _handler = new ChannelPoolHandler();
  private final RAPResponseHandler _responseHandler = new RAPResponseHandler();
  private final AtomicReference<State> _state = new AtomicReference<State>(State.RUNNING);

  private enum State { RUNNING, SHUTTING_DOWN, REQUESTS_STOPPING, SHUTDOWN }

  private final ScheduledExecutorService _scheduler;
  private final ExecutorService _callbackExecutors;

  private final long _requestTimeout;
  private final long _shutdownTimeout;
  private final int _maxResponseSize;
  private final int _maxHeaderSize;
  private final int _maxChunkSize;
  private final int _maxConcurrentConnections;

  private final String _requestTimeoutMessage;
  private final AbstractJmxManager _jmxManager;

  /**
   * Creates a new HttpNettyClient
   *
   * @param eventLoopGroup            The NioEventLoopGroup; it is the caller's responsibility to
   *                                  shut it down
   * @param executor                  An executor; it is the caller's responsibility to shut it down
   * @param poolSize                  Maximum size of the underlying HTTP connection pool
   * @param requestTimeout            Timeout, in ms, to get a connection from the pool or create one
   * @param idleTimeout               Interval after which idle connections will be automatically closed
   * @param shutdownTimeout           Timeout, in ms, the client should wait after shutdown is
   *                                  initiated before terminating outstanding requests
   * @param maxResponseSize           Maximum size of a HTTP response
   * @param sslContext                {@link SSLContext}
   * @param sslParameters             {@link SSLParameters}with overloaded construct
   * @param callbackExecutors         An optional EventExecutorGroup to invoke user callback
   * @param poolWaiterSize            Maximum waiters waiting on the HTTP connection pool
   * @param name                      Name of the {@link HttpNettyClient}
   * @param jmxManager                A management class that is aware of the creation/shutdown event
   *                                  of the underlying {@link ChannelPoolManager}
   * @param strategy                  The strategy used to return pool objects.
   * @param minPoolSize               Minimum number of objects in the pool. Set to zero for no minimum.
   * @param maxHeaderSize             Maximum size of all HTTP headers
   * @param maxChunkSize              Maximum size of a HTTP chunk
   * @param maxConcurrentConnections  Maximum number of concurrent connection attempts the HTTP
   *                                  connection pool can make.
   */
  public HttpNettyClient(NioEventLoopGroup eventLoopGroup,
                         ScheduledExecutorService executor,
                         int poolSize,
                         long requestTimeout,
                         long idleTimeout,
                         long shutdownTimeout,
                         int maxResponseSize,
                         SSLContext sslContext,
                         SSLParameters sslParameters,
                         ExecutorService callbackExecutors,
                         int poolWaiterSize,
                         String name,
                         AbstractJmxManager jmxManager,
                         AsyncPoolImpl.Strategy strategy,
                         int minPoolSize,
                         int maxHeaderSize,
                         int maxChunkSize,
                         int maxConcurrentConnections)
  {
    Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup)
                                .channel(NioSocketChannel.class)
                                .handler(new HttpClientPipelineInitializer(sslContext, sslParameters));

    _channelPoolManager = new ChannelPoolManager(
        new ChannelPoolFactoryImpl(bootstrap,
            poolSize,
            idleTimeout,
            poolWaiterSize,
            strategy,
            minPoolSize),
        name + ChannelPoolManager.BASE_NAME);

    _maxResponseSize = maxResponseSize;
    _maxHeaderSize = maxHeaderSize;
    _maxChunkSize = maxChunkSize;
    _maxConcurrentConnections = maxConcurrentConnections;
    _scheduler = executor;
    _callbackExecutors = callbackExecutors == null ? eventLoopGroup : callbackExecutors;
    _requestTimeout = requestTimeout;
    _shutdownTimeout = shutdownTimeout;
    _requestTimeoutMessage = "Exceeded request timeout of " + _requestTimeout + "ms";
    _jmxManager = jmxManager;
    _allChannels = new DefaultChannelGroup("R2 client channels", eventLoopGroup.next());
    _jmxManager.onProviderCreate(_channelPoolManager);
  }

  /* Constructor for test purpose ONLY. */
  HttpNettyClient(ChannelPoolFactory factory,
                  ScheduledExecutorService executor,
                  int requestTimeout,
                  int shutdownTimeout,
                  int maxResponseSize)
  {
    _maxResponseSize = maxResponseSize;
    _channelPoolManager = new ChannelPoolManager(factory);
    _scheduler = executor;
    _callbackExecutors = new DefaultEventExecutorGroup(1);
    _requestTimeout = requestTimeout;
    _shutdownTimeout = shutdownTimeout;
    _requestTimeoutMessage = "Exceeded request timeout of " + _requestTimeout + "ms";
    _jmxManager = AbstractJmxManager.NULL_JMX_MANAGER;
    _jmxManager.onProviderCreate(_channelPoolManager);
    _maxHeaderSize = 8192;
    _maxChunkSize = 8192;
    _maxConcurrentConnections = Integer.MAX_VALUE;
    _allChannels = new DefaultChannelGroup("R2 client channels", GlobalEventExecutor.INSTANCE);
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<RestResponse> callback)
  {
    MessageType.setMessageType(MessageType.Type.REST, wireAttrs);
    writeRequestWithTimeout(request, requestContext, wireAttrs, HttpBridge.restToHttpCallback(callback, request));
  }

  @Override
  public void shutdown(final Callback<None> callback)
  {
    LOG.info("Shutdown requested");
    if (_state.compareAndSet(State.RUNNING, State.SHUTTING_DOWN))
    {
      LOG.info("Shutting down");
      final long deadline = System.currentTimeMillis() + _shutdownTimeout;
      TimeoutCallback<None> closeChannels =
          new TimeoutCallback<None>(_scheduler,
                                    _shutdownTimeout,
                                    TimeUnit.MILLISECONDS,
                                    new Callback<None>()
                                    {
        private void finishShutdown()
        {
          _state.set(State.REQUESTS_STOPPING);
          // Timeout any waiters which haven't received a Channel yet
          for (Callback<Channel> callback : _channelPoolManager.cancelWaiters())
          {
            callback.onError(new TimeoutException("Operation did not complete before shutdown"));
          }

          // Timeout any requests still pending response
          for (Channel c : _allChannels)
          {
            TransportCallback<RestResponse> callback = c.attr(RAPResponseHandler.CALLBACK_ATTR_KEY).getAndRemove();
            if (callback != null)
            {
              errorResponse(callback, new TimeoutException("Operation did not complete before shutdown"));
            }
          }

          // Close all active and idle Channels
          final TimeoutRunnable afterClose = new TimeoutRunnable(
                  _scheduler, deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS, new Runnable()
                  {
                    @Override
                    public void run()
                    {
                      _state.set(State.SHUTDOWN);
                      LOG.info("Shutdown complete");
                      callback.onSuccess(None.none());
                    }
                  }, "Timed out waiting for channels to close, continuing shutdown");
          _allChannels.close().addListener(new ChannelGroupFutureListener()
          {
            @Override
            public void operationComplete(ChannelGroupFuture channelGroupFuture) throws Exception
            {
              if (!channelGroupFuture.isSuccess())
              {
                LOG.warn("Failed to close some connections, ignoring");
              }
              afterClose.run();
            }
          });
        }

        @Override
        public void onSuccess(None none)
        {
          LOG.info("All connection pools shut down, closing all channels");
          finishShutdown();
        }

        @Override
        public void onError(Throwable e)
        {
          LOG.warn("Error shutting down HTTP connection pools, ignoring and continuing shutdown", e);
          finishShutdown();
        }
      }, "Connection pool shutdown timeout exceeded (" + _shutdownTimeout + "ms)");
      _channelPoolManager.shutdown(closeChannels);
      _jmxManager.onProviderShutdown(_channelPoolManager);
    }
    else
    {
      callback.onError(new IllegalStateException("Shutdown has already been requested."));
    }
  }

  private void writeRequestWithTimeout(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
                                       TransportCallback<RestResponse> callback)
  {
    ExecutionCallback<RestResponse> executionCallback = new ExecutionCallback<RestResponse>(_callbackExecutors, callback);
    // By wrapping the callback in a Timeout callback before passing it along, we deny the rest
    // of the code access to the unwrapped callback.  This ensures two things:
    // 1. The user callback will always be invoked, since the Timeout will eventually expire
    // 2. The user callback is never invoked more than once
    TimeoutTransportCallback<RestResponse> timeoutCallback =
        new TimeoutTransportCallback<RestResponse>(_scheduler,
                                                   _requestTimeout,
                                                   TimeUnit.MILLISECONDS,
                                                   executionCallback,
                                                   _requestTimeoutMessage);
    writeRequest(request, requestContext, wireAttrs, timeoutCallback);
  }

  private void writeRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
                            final TimeoutTransportCallback<RestResponse> callback)
  {
    State state = _state.get();
    if (state != State.RUNNING)
    {
      errorResponse(callback, new IllegalStateException("Client is " + state));
      return;
    }
    URI uri = request.getURI();
    String scheme = uri.getScheme();
    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
    {
      errorResponse(callback, new IllegalArgumentException("Unknown scheme: " + scheme
          + " (only http/https is supported)"));
      return;
    }
    String host = uri.getHost();
    int port = uri.getPort();
    if (port == -1) {
      port = "http".equalsIgnoreCase(scheme) ? HTTP_DEFAULT_PORT : HTTPS_DEFAULT_PORT;
    }

    final RestRequest newRequest = new RestRequestBuilder(request)
        .overwriteHeaders(WireAttributeHelper.toWireAttributes(wireAttrs))
        .build();

    final SocketAddress address;
    try
    {
      // TODO investigate DNS resolution and timing
      InetAddress inetAddress = InetAddress.getByName(host);
      address = new InetSocketAddress(inetAddress, port);
    }
    catch (UnknownHostException e)
    {
      errorResponse(callback, e);
      return;
    }

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
        callback.addTimeoutTask(new Runnable()
        {
          @Override
          public void run()
          {
            AsyncPool<Channel> pool = channel.attr(ChannelPoolHandler.CHANNEL_POOL_ATTR_KEY).getAndRemove();
            if (pool != null)
            {
              pool.dispose(channel);
            }
          }
        });

        // This handler invokes the callback with the response once it arrives.
        channel.attr(RAPResponseHandler.CALLBACK_ATTR_KEY).set(callback);

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

        channel.writeAndFlush(newRequest);
      }

      @Override
      public void onError(Throwable e)
      {
        errorResponse(callback, e);
      }
    });
    if (pendingGet != null)
    {
      callback.addTimeoutTask(new Runnable()
      {
        @Override
        public void run()
        {
          pendingGet.cancel();
        }
      });
    }
  }

  static <T> void errorResponse(TransportCallback<T> callback, Throwable e)
  {
    callback.onResponse(TransportResponseImpl.<T>error(e));
  }

  static Exception toException(Throwable t)
  {
    if (t instanceof Exception)
    {
      return (Exception)t;
    }
    // This could probably be improved...
    return new Exception("Wrapped Throwable", t);
  }

  private class HttpClientPipelineInitializer extends ChannelInitializer<NioSocketChannel>
  {
    private final SSLContext    _sslContext;
    private final SSLParameters _sslParameters;

    /**
     * Creates new instance.
     *
     * @param sslContext {@link SSLContext} to be used for TLS-enabled channel pipeline.
     * @param sslParameters {@link SSLParameters} to configure {@link SSLEngine}s created
     *          from sslContext. This is somewhat redundant to
     *          SSLContext.getDefaultSSLParameters(), but those turned out to be
     *          exceedingly difficult to configure, so we can't pass all desired
     *          configuration in sslContext.
     */
    public HttpClientPipelineInitializer(SSLContext sslContext, SSLParameters sslParameters)
    {
      // Check if requested parameters are present in the supported params of the context.
      // Log warning for those not present. Throw an exception if none present.
      if (sslParameters != null)
      {
        if (sslContext == null)
        {
          throw new IllegalArgumentException("SSLParameters passed with no SSLContext");
        }

        SSLParameters supportedSSLParameters = sslContext.getSupportedSSLParameters();

        if (sslParameters.getCipherSuites() != null)
        {
          checkContained(supportedSSLParameters.getCipherSuites(),
                         sslParameters.getCipherSuites(),
                         "cipher suite");
        }

        if (sslParameters.getProtocols() != null)
        {
          checkContained(supportedSSLParameters.getProtocols(),
                         sslParameters.getProtocols(),
                         "protocol");
        }
      }
      _sslContext = sslContext;
      _sslParameters = sslParameters;
    }

    /**
     * Checks if an array is completely or partially contained in another. Logs warnings
     * for one array values not contained in the other. Throws IllegalArgumentException if
     * none are.
     *
     * @param containingArray array to contain another.
     * @param containedArray array to be contained in another.
     * @param valueName - name of the value type to be included in log warning or
     *          exception.
     */
    private void checkContained(String[] containingArray,
                                String[] containedArray,
                                String valueName)
    {
      Set<String> containingSet = new HashSet<String>(Arrays.asList(containingArray));
      Set<String> containedSet = new HashSet<String>(Arrays.asList(containedArray));

      boolean changed = containedSet.removeAll(containingSet);
      if (!changed)
      {
        throw new IllegalArgumentException("None of the requested " + valueName
                                           + "s: " + containedSet + " are found in SSLContext");
      }

      if (!containedSet.isEmpty())
      {
        for (String paramValue : containedSet)
        {
          LOG.warn("{} {} requested but not found in SSLContext", valueName, paramValue);
        }
      }
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception
    {
      ch.pipeline().addLast("codec", new HttpClientCodec(4096, _maxHeaderSize, _maxChunkSize));
      ch.pipeline().addLast("dechunker", new HttpObjectAggregator(_maxResponseSize));
      ch.pipeline().addLast("rapiCodec", new RAPClientCodec());
      ch.pipeline().addLast("responseHandler", _responseHandler);
      if (_sslContext != null)
      {
        ch.pipeline().addLast("sslRequestHandler", new SslRequestHandler(_sslContext, _sslParameters));
      }
      ch.pipeline().addLast("channelManager", _handler);
    }
  }

  private class ChannelPoolFactoryImpl implements ChannelPoolFactory
  {
    private final Bootstrap _bootstrap;
    private final int _maxPoolSize;
    private final long _idleTimeout;
    private final int _maxPoolWaiterSize;
    private final AsyncPoolImpl.Strategy _strategy;
    private final int _minPoolSize;

    private ChannelPoolFactoryImpl(Bootstrap bootstrap,
                                   int maxPoolSize,
                                   long idleTimeout,
                                   int maxPoolWaiterSize,
                                   AsyncPoolImpl.Strategy strategy,
                                   int minPoolSize)
    {
      _bootstrap = bootstrap;
      _maxPoolSize = maxPoolSize;
      _idleTimeout = idleTimeout;
      _maxPoolWaiterSize = maxPoolWaiterSize;
      _strategy = strategy;
      _minPoolSize = minPoolSize;
    }

    @Override
    public AsyncPool<Channel> getPool(SocketAddress address)
    {
      return new AsyncPoolImpl<Channel>(address.toString() + " HTTP connection pool",
                                        new ChannelPoolLifecycle(address,
                                                                 _bootstrap,
                                                                 _allChannels),
                                        _maxPoolSize,
                                        _idleTimeout,
                                        _scheduler,
                                        _maxPoolWaiterSize,
                                        _strategy,
                                        _minPoolSize,
                                        new ExponentialBackOffRateLimiter(0,
                                                            _requestTimeout / 2,
                                                            Math.max(10, _requestTimeout / 32),
                                                            _scheduler,
                                                            _maxConcurrentConnections)
                                        );
    }
  }

  /**
   * Get statistics from each channel pool. The map keys represent pool names.
   * The values are the corresponding {@link AsyncPoolStats} objects.
   *
   * @return A map of pool names and statistics.
   */
  public Map<String, PoolStats> getPoolStats()
  {
    return _channelPoolManager.getPoolStats();
  }

  // Test support

  public long getRequestTimeout()
  {
    return _requestTimeout;
  }

  public long getShutdownTimeout()
  {
    return _shutdownTimeout;
  }

  public long getMaxResponseSize()
  {
    return _maxResponseSize;
  }
}
