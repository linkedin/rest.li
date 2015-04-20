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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

/* package private */ class HttpNettyClient implements TransportClient
{
  static final Logger LOG = LoggerFactory.getLogger(HttpNettyClient.class);
  private static final int HTTP_DEFAULT_PORT = 80;
  private static final int HTTPS_DEFAULT_PORT = 443;

  private final ChannelPoolManager _channelPoolManager;
  private final ChannelGroup _allChannels = new DefaultChannelGroup("R2 client channels");

  private final ChannelPoolHandler _handler = new ChannelPoolHandler();
  private final RAPResponseHandler _responseHandler = new RAPResponseHandler();
  private final AtomicReference<State> _state = new AtomicReference<State>(State.RUNNING);

  private enum State { RUNNING, SHUTTING_DOWN, REQUESTS_STOPPING, SHUTDOWN }

  private final ScheduledExecutorService _scheduler;
  private final ExecutorService _callbackExecutor;

  private final int _requestTimeout;
  private final int _shutdownTimeout;
  private final int _maxResponseSize;

  private final String _requestTimeoutMessage;
  private final AbstractJmxManager _jmxManager;
  private final String _name;

  /**
   * Creates a new HttpNettyClient with some default parameters
   *
   * @see #HttpNettyClient(ClientSocketChannelFactory,ScheduledExecutorService,int,int,int,int,int,SSLContext,SSLParameters,int,ExecutorService,int)
   */
  public HttpNettyClient(ClientSocketChannelFactory factory,
                         ScheduledExecutorService executor,
                         int poolSize,
                         int requestTimeout,
                         int idleTimeout,
                         int shutdownTimeout,
                         int maxResponseSize)
  {
    this(factory,
         executor,
         poolSize,
         requestTimeout,
         idleTimeout,
         shutdownTimeout,
         maxResponseSize,
         null,
         null,
         executor,
         Integer.MAX_VALUE);
  }

  /**
   * Creates a new HttpNettyClient
   *
   * @param factory The ClientSocketChannelFactory; it is the caller's responsibility to
   *          shut it down
   * @param executor an executor; it is the caller's responsibility to shut it down
   * @param poolSize Maximum size of the underlying HTTP connection pool
   * @param requestTimeout timeout, in ms, to get a connection from the pool or create one
   * @param idleTimeout interval after which idle connections will be automatically closed
   * @param shutdownTimeout timeout, in ms, the client should wait after shutdown is
   *          initiated before terminating outstanding requests
   * @param maxResponseSize
   * @param sslContext {@link SSLContext}
   * @param sslParameters {@link SSLParameters}with overloaded construct
   * @param callbackExecutor an optional executor to invoke user callback
   * @param poolWaiterSize Maximum waiters waiting on the HTTP connection pool
   */
  public HttpNettyClient(ClientSocketChannelFactory factory,
                         ScheduledExecutorService executor,
                         int poolSize,
                         int requestTimeout,
                         int idleTimeout,
                         int shutdownTimeout,
                         int maxResponseSize,
                         SSLContext sslContext,
                         SSLParameters sslParameters,
                         ExecutorService callbackExecutor,
                         int poolWaiterSize)
  {
    this(factory,
        executor,
        poolSize,
        requestTimeout,
        idleTimeout,
        shutdownTimeout,
        maxResponseSize,
        sslContext,
        sslParameters,
        callbackExecutor,
        poolWaiterSize,
        HttpClientFactory.DEFAULT_CLIENT_NAME,
        HttpClientFactory.NULL_JMX_MANAGER);
  }

  /**
   * legacy constructor for backward-compatibility purpose.
   */
  public HttpNettyClient(ClientSocketChannelFactory factory,
                         ScheduledExecutorService executor,
                         int poolSize,
                         int requestTimeout,
                         int idleTimeout,
                         int shutdownTimeout,
                         int maxResponseSize,
                         SSLContext sslContext,
                         SSLParameters sslParameters,
                         ExecutorService callbackExecutor,
                         int poolWaiterSize,
                         String name,
                         AbstractJmxManager jmxManager)
  {
    this(factory,
         executor,
         poolSize,
         requestTimeout,
         idleTimeout,
         shutdownTimeout,
         maxResponseSize,
         sslContext,
         sslParameters,
         callbackExecutor,
         poolWaiterSize,
         name,
         jmxManager,
         AsyncPoolImpl.Strategy.MRU,
         0);
  }

  /**
   * Creates a new HttpNettyClient
   *
   * @param factory The ClientSocketChannelFactory; it is the caller's responsibility to
   *          shut it down
   * @param executor an executor; it is the caller's responsibility to shut it down
   * @param poolSize Maximum size of the underlying HTTP connection pool
   * @param requestTimeout timeout, in ms, to get a connection from the pool or create one
   * @param idleTimeout interval after which idle connections will be automatically closed
   * @param shutdownTimeout timeout, in ms, the client should wait after shutdown is
   *          initiated before terminating outstanding requests
   * @param maxResponseSize
   * @param sslContext {@link SSLContext}
   * @param sslParameters {@link SSLParameters}with overloaded construct
   * @param callbackExecutor an optional executor to invoke user callback
   * @param poolWaiterSize Maximum waiters waiting on the HTTP connection pool
   * @param name Name of the {@link HttpNettyClient}
   * @param jmxManager A management class that is aware of the creation/shutdown event
   *          of the underlying {@link ChannelPoolManager}
   * @param strategy The strategy used to return pool objects.
   * @param minPoolSize Minimum number of objects in the pool. Set to zero for
   *                no minimum.
   */
  public HttpNettyClient(ClientSocketChannelFactory factory,
                         ScheduledExecutorService executor,
                         int poolSize,
                         int requestTimeout,
                         int idleTimeout,
                         int shutdownTimeout,
                         int maxResponseSize,
                         SSLContext sslContext,
                         SSLParameters sslParameters,
                         ExecutorService callbackExecutor,
                         int poolWaiterSize,
                         String name,
                         AbstractJmxManager jmxManager,
                         AsyncPoolImpl.Strategy strategy,
                         int minPoolSize)
  {
    _maxResponseSize = maxResponseSize;
    _name = name;
    _channelPoolManager =
        new ChannelPoolManager(new ChannelPoolFactoryImpl(new ClientBootstrap(factory),
            poolSize,
            idleTimeout,
            sslContext,
            sslParameters,
            poolWaiterSize,
            strategy,
            minPoolSize),
            name + ChannelPoolManager.BASE_NAME);
    _scheduler = executor;
    _callbackExecutor = callbackExecutor;
    _requestTimeout = requestTimeout;
    _shutdownTimeout = shutdownTimeout;
    _requestTimeoutMessage = "Exceeded request timeout of " + _requestTimeout + "ms";
    _jmxManager = jmxManager;
    _jmxManager.onProviderCreate(_channelPoolManager);
  }

  HttpNettyClient(ChannelPoolFactory factory,
                  ScheduledExecutorService executor,
                  int requestTimeout,
                  int shutdownTimeout,
                  int maxResponseSize)
  {
    _maxResponseSize = maxResponseSize;
    _channelPoolManager = new ChannelPoolManager(factory);
    _scheduler = executor;
    _callbackExecutor = executor;
    _requestTimeout = requestTimeout;
    _shutdownTimeout = shutdownTimeout;
    _requestTimeoutMessage = "Exceeded request timeout of " + _requestTimeout + "ms";
    _name = HttpClientFactory.DEFAULT_CLIENT_NAME;
    _jmxManager = HttpClientFactory.NULL_JMX_MANAGER;
    _jmxManager.onProviderCreate(_channelPoolManager);
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
            @SuppressWarnings("unchecked")
            TransportCallback<RestResponse> callback = c.getPipeline().get(RAPResponseHandler.class).removeAttachment(c.getPipeline().getContext(RAPResponseHandler.class));
            if (callback != null)
            {
              errorResponse(callback,
                            new TimeoutException("Operation did not complete before shutdown"));
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
          ChannelGroupFuture future = _allChannels.close();
          future.addListener(new ChannelGroupFutureListener()
          {
            @Override
            public void operationComplete(ChannelGroupFuture channelGroupFuture) throws Exception
            {
              if (!channelGroupFuture.isCompleteSuccess())
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
  }

  private void writeRequestWithTimeout(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
                                       TransportCallback<RestResponse> callback)
  {
    // By wrapping the callback in a Timeout callback before passing it along, we deny the rest
    // of the code access to the unwrapped callback.  This ensures two things:
    // 1. The user callback will always be invoked, since the Timeout will eventually expire
    // 2. The user callback is never invoked more than once
    TimeoutTransportCallback<RestResponse> timeoutCallback =
        new TimeoutTransportCallback<RestResponse>(_scheduler,
                                                   _callbackExecutor,
                                                   _requestTimeout,
                                                   TimeUnit.MILLISECONDS,
                                                   callback,
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
    if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
    {
      errorResponse(callback, new IllegalArgumentException("Unknown scheme: " + scheme
          + " (only http/https is supported)"));
      return;
    }
    String host = uri.getHost();
    int port = uri.getPort();
    if (port == -1) {
      port = scheme.equalsIgnoreCase("http") ? HTTP_DEFAULT_PORT : HTTPS_DEFAULT_PORT;
    }

    final RestRequest newRequest = new RestRequestBuilder(request)
        .overwriteHeaders(WireAttributeHelper.toWireAttributes(wireAttrs))
        .build();

    // TODO investigate DNS resolution and timing
    SocketAddress address = new InetSocketAddress(host, port);
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
        final ChannelPoolHandler channelPoolHandler = channel.getPipeline().get(ChannelPoolHandler.class);
        final ChannelHandlerContext channelPoolHandlerContext = channel.getPipeline().getContext(ChannelPoolHandler.class);
        channelPoolHandler.setAttachment(channelPoolHandlerContext, pool);
        callback.addTimeoutTask(new Runnable()
        {
          @Override
          public void run()
          {
            AsyncPool<Channel> pool = channelPoolHandler.removeAttachment(channelPoolHandlerContext);
            if (pool != null)
            {
              pool.dispose(channel);
            }
          }
        });

        // This handler invokes the callback with the response once it arrives.
        channel.getPipeline().get(RAPResponseHandler.class).setAttachment(
                                                              channel.getPipeline().getContext(RAPResponseHandler.class),
                                                              callback);

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

        channel.write(newRequest);
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

  private class HttpClientPipelineFactory implements ChannelPipelineFactory
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
    public HttpClientPipelineFactory(SSLContext sslContext, SSLParameters sslParameters)
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
    public ChannelPipeline getPipeline() throws Exception
    {
      ChannelPipeline pipeline = Channels.pipeline();

      pipeline.addLast("codec", new HttpClientCodec());
      pipeline.addLast("dechunker", new HttpChunkAggregator(_maxResponseSize));
      pipeline.addLast("rapiCodec", new RAPClientCodec());
      // Could introduce an ExecutionHandler here (before RAPResponseHandler)
      // to execute the response handling on a different thread.
      pipeline.addLast("responseHandler", _responseHandler);
      // Add handler to dynamically configure SSL-related handlers depending on
      // the SSL configuration and request URI.
      if (_sslContext != null)
      {
        pipeline.addLast("sslRequestHandler", new SslRequestHandler(_sslContext,
                                                                    _sslParameters));
      }
      pipeline.addLast("channelManager", _handler);

      return pipeline;
    }
  }

  private class ChannelPoolFactoryImpl implements ChannelPoolFactory
  {
    private final ClientBootstrap _bootstrap;
    private final int _maxPoolSize;
    private final int _idleTimeout;
    private final int _maxPoolWaiterSize;
    private final AsyncPoolImpl.Strategy _strategy;
    private final int _minPoolSize;

    private ChannelPoolFactoryImpl(ClientBootstrap bootstrap,
                                   int maxPoolSize,
                                   int idleTimeout,
                                   SSLContext sslContext,
                                   SSLParameters sslParameters,
                                   int maxPoolWaiterSize,
                                   AsyncPoolImpl.Strategy strategy,
                                   int minPoolSize)
    {
      _bootstrap = bootstrap;
      _bootstrap.setPipelineFactory(new HttpClientPipelineFactory(sslContext,
                                                                  sslParameters));
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
                                        _callbackExecutor,
                                        _maxPoolWaiterSize,
                                        _strategy,
                                        _minPoolSize,
                                        new ExponentialBackOffRateLimiter(0,
                                                            _requestTimeout / 2,
                                                            Math.max(10, _requestTimeout / 32),
                                                            _scheduler)
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

  public int getRequestTimeout()
  {
    return _requestTimeout;
  }

  public int getShutdownTimeout()
  {
    return _shutdownTimeout;
  }

  public int getMaxResponseSize()
  {
    return _maxResponseSize;
  }
}
