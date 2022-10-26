/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.netty.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.MultiCallback;
import com.linkedin.common.stats.LongStats;
import com.linkedin.common.stats.LongTracker;
import com.linkedin.common.stats.LongTracking;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingImportance;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.netty.callback.StreamExecutionCallback;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.netty.common.NettyClientState;
import com.linkedin.r2.netty.common.ShutdownTimeoutException;
import com.linkedin.r2.netty.common.StreamingTimeout;
import com.linkedin.r2.netty.common.UnknownSchemeException;
import com.linkedin.r2.netty.handler.common.SslHandshakeTimingHandler;
import com.linkedin.r2.transport.common.MessageType;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.client.InvokedOnceTransportCallback;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import com.linkedin.r2.transport.http.common.HttpBridge;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.r2.util.RequestTimeoutUtil;
import com.linkedin.r2.util.Timeout;
import com.linkedin.util.ArgumentUtil;
import com.linkedin.util.clock.Clock;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.http.HttpScheme;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of {@link TransportClient}
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class HttpNettyClient implements TransportClient
{
  private static final Logger LOG = LoggerFactory.getLogger(HttpNettyClient.class);
  private static final TimingKey TIMING_KEY = TimingKey.registerNewKey("dns_resolution_new", TimingImportance.LOW);
  private static final String HTTP_SCHEME = HttpScheme.HTTP.toString();
  private static final String HTTPS_SCHEME = HttpScheme.HTTPS.toString();
  private static final int HTTP_DEFAULT_PORT = 80;
  private static final int HTTPS_DEFAULT_PORT = 443;
  private static final int DEFAULT_STREAMING_TIMEOUT = -1;

  private final EventLoopGroup _eventLoopGroup;
  private final ScheduledExecutorService _scheduler;
  private final ExecutorService _callbackExecutor;
  private final ChannelPoolManager _channelPoolManager;
  private final ChannelPoolManager _sslChannelPoolManager;
  private final Clock _clock;
  private final HttpProtocolVersion _protocolVersion;
  private final long _requestTimeout;
  private final long _streamingTimeout;
  private final long _shutdownTimeout;
  private final String _udsAddress;

  private final AtomicReference<NettyClientState> _state;

  private final AtomicLong _dnsResolutionErrors = new AtomicLong(0);
  private final AtomicLong _dnsResolutions = new AtomicLong(0);
  private final LongTracker _dnsResolutionLatency = new LongTracking();
  private final Object _lock = new Object();

  @Deprecated
  public HttpNettyClient(
      EventLoopGroup eventLoopGroup,
      ScheduledExecutorService scheduler,
      ExecutorService callbackExecutor,
      ChannelPoolManager channelPoolManager,
      ChannelPoolManager sslChannelPoolManager,
      HttpProtocolVersion protocolVersion,
      Clock clock,
      long requestTimeout,
      long streamingTimeout,
      long shutdownTimeout) {
    this(eventLoopGroup, scheduler, callbackExecutor, channelPoolManager, sslChannelPoolManager, protocolVersion,
        clock, requestTimeout, streamingTimeout, shutdownTimeout, null);
  }

  /**
   * Creates a new instance of {@link HttpNettyClient}.
   *
   * @param eventLoopGroup Non-blocking event loop group implementation for selectors and channels
   * @param callbackExecutor Executor service for executing user callbacks. The executor must be provided
   *                         because user callbacks can potentially be blocking. If executed with the
   *                         event loop group, threads might be blocked and cause channels to hang.
   * @param channelPoolManager Channel pool manager for non-SSL channels
   * @param sslChannelPoolManager Channel pool manager for SSL channels
   * @param protocolVersion HTTP version the client uses to send requests and receive responses
   * @param clock Clock to get current time
   * @param requestTimeout Time in milliseconds before an error response is returned in the callback
   *                       with a {@link TimeoutException}
   * @param shutdownTimeout Client shutdown timeout
   * @param udsAddress Unix Domain Socket Address, used while using side car proxy for external communication
   */
  public HttpNettyClient(
      EventLoopGroup eventLoopGroup,
      ScheduledExecutorService scheduler,
      ExecutorService callbackExecutor,
      ChannelPoolManager channelPoolManager,
      ChannelPoolManager sslChannelPoolManager,
      HttpProtocolVersion protocolVersion,
      Clock clock,
      long requestTimeout,
      long streamingTimeout,
      long shutdownTimeout,
      String udsAddress)
  {
    ArgumentUtil.notNull(eventLoopGroup, "eventLoopGroup");
    ArgumentUtil.notNull(scheduler, "scheduler");
    ArgumentUtil.notNull(callbackExecutor, "callbackExecutor");
    ArgumentUtil.notNull(channelPoolManager, "channelPoolManager");
    ArgumentUtil.notNull(sslChannelPoolManager, "sslChannelPoolManager");
    ArgumentUtil.notNull(clock, "clock");
    ArgumentUtil.checkArgument(requestTimeout >= 0, "requestTimeout");
    ArgumentUtil.checkArgument(streamingTimeout >= DEFAULT_STREAMING_TIMEOUT, "streamingTimeout");
    ArgumentUtil.checkArgument(shutdownTimeout >= 0, "shutdownTimeout");

    // If StreamingTimeout is greater than RequestTimeout then its as good as not being set
    if (streamingTimeout >= requestTimeout)
    {
      streamingTimeout = DEFAULT_STREAMING_TIMEOUT;
    }

    _eventLoopGroup = eventLoopGroup;
    _scheduler = scheduler;
    _callbackExecutor = callbackExecutor;
    _channelPoolManager = channelPoolManager;
    _sslChannelPoolManager = sslChannelPoolManager;
    _clock = clock;
    _protocolVersion = protocolVersion;
    _requestTimeout = requestTimeout;
    _streamingTimeout = streamingTimeout;
    _shutdownTimeout = shutdownTimeout;
    _udsAddress = udsAddress;


    _state = new AtomicReference<>(NettyClientState.RUNNING);
  }

  /**
   * Keeps track of the callbacks attached to the user's requests and in case of shutdown, it fires them
   * with a Timeout Exception
   */
  private final Set<TransportCallback<StreamResponse>> _userCallbacks = ConcurrentHashMap.newKeySet();

  @Override
  public void restRequest(RestRequest request,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      TransportCallback<RestResponse> callback)
  {
    sendRequest(request, requestContext, wireAttrs, Messages.toStreamTransportCallback(callback));
  }

  @Override
  public void streamRequest(StreamRequest request,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      TransportCallback<StreamResponse> callback)
  {
    // We treat full request (already fully in memory) and real stream request (not fully buffered in memory)
    // differently. For the latter we have to use streaming handshakes to read the data as the data not fully buffered in memory.
    // For the former we can avoid using streaming which has following benefits:
    // 1) Avoid the cost associated with streaming handshakes (even though it is negligible)
    // 2) Avoid the use of chunked encoding during http/1.1 transport to slightly save cost of transmitting over the wire
    // 3) more importantly legacy R2 servers cannot work with chunked transfer encoding (http/1.1), so this allow the new client
    // talk to legacy R2 servers without problem if they're just using restRequest (full request) with http/1.1
    if(isFullRequest(requestContext))
    {
      sendStreamRequestAsRestRequest(request, requestContext, wireAttrs, callback);
    }
    else
    {
      sendRequest(request, requestContext, wireAttrs, callback);
    }
  }



  @Override
  public void shutdown(Callback<None> callback)
  {
    LOG.info("Shutdown requested");
    if (_state.compareAndSet(NettyClientState.RUNNING, NettyClientState.SHUTTING_DOWN))
    {
      LOG.info("Shutting down");
      MultiCallback poolShutdown = new MultiCallback(
          new Callback<None>()
          {
            private void releaseCallbacks()
            {
              _userCallbacks.forEach(transportCallback -> transportCallback.onResponse(
                  TransportResponseImpl.error(new TimeoutException("Operation did not complete before shutdown"))));
            }

            @Override
            public void onError(Throwable e)
            {
              releaseCallbacks();
              callback.onError(e);
            }

            @Override
            public void onSuccess(None result)
            {
              releaseCallbacks();
              callback.onSuccess(result);
            }
          }, 2);

      _channelPoolManager.shutdown(poolShutdown,
          () -> _state.set(NettyClientState.REQUESTS_STOPPING),
          () -> _state.set(NettyClientState.SHUTDOWN),
          _shutdownTimeout);
      _sslChannelPoolManager.shutdown(poolShutdown,
          () -> _state.set(NettyClientState.REQUESTS_STOPPING),
          () -> _state.set(NettyClientState.SHUTDOWN),
          _shutdownTimeout);
    }
    else
    {
      callback.onError(new IllegalStateException("Shutdown has already been requested."));
    }
    TimingKey.unregisterKey(TIMING_KEY);
  }

  private void sendStreamRequestAsRestRequest(StreamRequest request, RequestContext requestContext,
      Map<String, String> wireAttrs, TransportCallback<StreamResponse> callback)
  {
    Messages.toRestRequest(request, new Callback<RestRequest>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onResponse(TransportResponseImpl.error(e));
      }

      @Override
      public void onSuccess(RestRequest restRequest)
      {
        sendRequest(restRequest, requestContext, wireAttrs, callback);
      }
    });
  }

  private static boolean isFullRequest(RequestContext requestContext)
  {
    Object isFullRequest = requestContext.getLocalAttr(R2Constants.IS_FULL_REQUEST);
    return isFullRequest != null && (Boolean)isFullRequest;
  }

  /**
   * Sends the request to the {@link ChannelPipeline}.
   */
  private void sendRequest(Request request, RequestContext requestContext, Map<String, String> wireAttrs, TransportCallback<StreamResponse> callback)
  {
    final TransportCallback<StreamResponse> decoratedCallback = decorateUserCallback(request, callback);

    final NettyClientState state = _state.get();
    if (state != NettyClientState.RUNNING)
    {
      decoratedCallback.onResponse(TransportResponseImpl.error(new IllegalStateException("Client is not running")));
      return;
    }

    final long resolvedRequestTimeout = resolveRequestTimeout(requestContext, _requestTimeout);

    // Timeout ensures the request callback is always invoked and is cancelled before the
    // responsibility of invoking the callback is handed over to the pipeline.
    final Timeout<None> timeout = new Timeout<>(_scheduler, resolvedRequestTimeout, TimeUnit.MILLISECONDS, None.none());
    timeout.addTimeoutTask(() -> decoratedCallback.onResponse(TransportResponseImpl.error(
        new TimeoutException("Exceeded request timeout of " + resolvedRequestTimeout + "ms" +
            (requestContext.getLocalAttr(R2Constants.REMOTE_SERVER_ADDR) == null ? " (timeout during DNS resolution)" : "")))));

    // resolve address
    final SocketAddress address;

    if (StringUtils.isEmpty(_udsAddress)) {
      try {
        TimingContextUtil.markTiming(requestContext, TIMING_KEY);
        _dnsResolutions.incrementAndGet();
        long startTime = _clock.currentTimeMillis();
        address = resolveAddress(request, requestContext);
        synchronized (_lock) {
          _dnsResolutionLatency.addValue(_clock.currentTimeMillis() - startTime);
        }
        TimingContextUtil.markTiming(requestContext, TIMING_KEY);
      } catch (Exception e) {
        _dnsResolutionErrors.incrementAndGet();
        decoratedCallback.onResponse(TransportResponseImpl.error(e));
        return;
      }
    } else {
      try {
        address = new DomainSocketAddress(_udsAddress);
      } catch (Exception e) {
        decoratedCallback.onResponse(TransportResponseImpl.error(e));
        return;
      }
    }

    // Serialize wire attributes
    final Request requestWithWireAttrHeaders;

    if (request instanceof StreamRequest)
    {
      requestWithWireAttrHeaders = buildRequestWithWireAttributes((StreamRequest)request, wireAttrs);
    }
    else
    {
      MessageType.setMessageType(MessageType.Type.REST, wireAttrs);
      requestWithWireAttrHeaders = buildRequestWithWireAttributes((RestRequest)request, wireAttrs);
    }

    // Gets channel pool
    final AsyncPool<Channel> pool;
    try
    {
      pool = getChannelPoolManagerPerRequest(requestWithWireAttrHeaders).getPoolForAddress(address);
    }
    catch (IllegalStateException e)
    {
      decoratedCallback.onResponse(TransportResponseImpl.error(e));
      return;
    }

    // Saves protocol version in request context
    requestContext.putLocalAttr(R2Constants.HTTP_PROTOCOL_VERSION, _protocolVersion);

    final Cancellable pendingGet = pool.get(new ChannelPoolGetCallback(
        pool, requestWithWireAttrHeaders, requestContext, decoratedCallback, timeout, resolvedRequestTimeout, _streamingTimeout));

    if (pendingGet != null)
    {
      timeout.addTimeoutTask(pendingGet::cancel);
    }
  }

  private StreamRequest buildRequestWithWireAttributes(StreamRequest request, Map<String, String> wireAttrs)
  {
    return request.builder()
        .overwriteHeaders(WireAttributeHelper.toWireAttributes(wireAttrs))
        .build(request.getEntityStream());
  }

  private RestRequest buildRequestWithWireAttributes(RestRequest request, Map<String, String> wireAttrs)
  {
    return new RestRequestBuilder(request)
        .overwriteHeaders(WireAttributeHelper.toWireAttributes(wireAttrs))
        .build();
  }

  /**
   * Implementation of {@link Callback} for getting a {@link Channel} from the {@link ChannelPool}.
   */
  private class ChannelPoolGetCallback implements Callback<Channel>
  {
    private final AsyncPool<Channel> _pool;
    private final Request _request;
    private final RequestContext _requestContext;
    private final TransportCallback<StreamResponse> _callback;
    private final Timeout<None> _timeout;
    private final long _resolvedRequestTimeout;
    private final long _streamingTimeout;

    ChannelPoolGetCallback(
        AsyncPool<Channel> pool,
        Request request,
        RequestContext requestContext,
        TransportCallback<StreamResponse> callback,
        Timeout<None> timeout,
        long resolvedRequestTimeout,
        long streamingTimeout)
    {
      _pool = pool;
      _request = request;
      _requestContext = requestContext;
      _callback = callback;
      _timeout = timeout;
      _resolvedRequestTimeout = resolvedRequestTimeout;
      _streamingTimeout = streamingTimeout;
    }

    @Override
    public void onSuccess(final Channel channel)
    {
      // Cancels previous timeout and takes over the responsibility of invoking the request callback
      _timeout.getItem();

      // Sets channel attributes relevant to the request
      channel.attr(NettyChannelAttributes.CHANNEL_POOL).set(_pool);

      TransportCallback<StreamResponse> sslTimingCallback = SslHandshakeTimingHandler.getSslTimingCallback(channel, _requestContext, _callback);

      channel.attr(NettyChannelAttributes.RESPONSE_CALLBACK).set(sslTimingCallback);

      // Set the session validator requested by the user
      final SslSessionValidator sslSessionValidator = (SslSessionValidator) _requestContext.getLocalAttr(R2Constants.REQUESTED_SSL_SESSION_VALIDATOR);
      channel.attr(NettyChannelAttributes.SSL_SESSION_VALIDATOR).set(sslSessionValidator);

      final NettyClientState state = _state.get();
      if (state == NettyClientState.REQUESTS_STOPPING || state == NettyClientState.SHUTDOWN)
      {
        // Channel is created but the client is either shutting down or already shutdown. We need to
        // invoke request callback we haven't already and return channel back to the channel pool. By
        // firing an exception to the channel pipeline we can rely on the handlers to perform these
        // tasks upon catching the exception.
        channel.pipeline().fireExceptionCaught(new ShutdownTimeoutException("Operation did not complete before shutdown"));
        return;
      }

      // Schedules a timeout exception to be fired after specified request timeout
      final ScheduledFuture<ChannelPipeline> timeoutFuture = _scheduler.schedule(
          () -> channel.pipeline().fireExceptionCaught(
              new TimeoutException("Exceeded request timeout of " + _resolvedRequestTimeout + "ms")),
          _resolvedRequestTimeout,
          TimeUnit.MILLISECONDS);

      // Schedules a stream timeout exception to be fired after specified stream idle time
      if (isStreamingTimeoutEnabled())
      {
        final StreamingTimeout streamingTimeout = new StreamingTimeout(_scheduler, _streamingTimeout, channel, _clock);
        channel.attr(NettyChannelAttributes.STREAMING_TIMEOUT_FUTURE).set(streamingTimeout);
      }

      channel.attr(NettyChannelAttributes.TIMEOUT_FUTURE).set(timeoutFuture);

      // Here we want the exception in outbound operations to be passed back through pipeline so that
      // the user callback would be invoked with the exception and the channel can be put back into the pool
      channel.writeAndFlush(_request).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    private boolean isStreamingTimeoutEnabled()
    {
      return _streamingTimeout > HttpClientFactory.DEFAULT_STREAMING_TIMEOUT;
    }

    @Override
    public void onError(Throwable e)
    {
      _callback.onResponse(TransportResponseImpl.error(e));
    }
  }

  /**
   * Decorates user callback with the follow properties.
   * <p><ul>
   * <li> Callback can be invoked at most once
   * <li> Callback is executed on the callback executor
   * <li> Callback is added to the user callback set and removed upon execution
   * <li> Callback is not sensitive to response status code, see {@link HttpBridge} #streamToHttpCallback
   * </ul><p>
   */
  private TransportCallback<StreamResponse> decorateUserCallback(Request request, TransportCallback<StreamResponse> callback)
  {
    final TransportCallback<StreamResponse> httpCallback = HttpBridge.streamToHttpCallback(callback, request);
    final TransportCallback<StreamResponse> executionCallback = getExecutionCallback(httpCallback);
    final TransportCallback<StreamResponse> shutdownAwareCallback = getShutdownAwareCallback(executionCallback);
    return shutdownAwareCallback;
  }

  /**
   * Given a callback, returns the wrapped callback that will be executed on a custom executor
   */
  private TransportCallback<StreamResponse> getExecutionCallback(TransportCallback<StreamResponse> callback)
  {
    return new StreamExecutionCallback(_callbackExecutor, callback);
  }

  /**
   * Register the callback in a structure that allows to fire the callback in case of shutdown
   */
  private TransportCallback<StreamResponse> getShutdownAwareCallback(TransportCallback<StreamResponse> callback)
  {
    // Used InvokedOnceTransportCallback to avoid to trigger onResponse twice, in case of concurrent shutdown and firing
    // the callback from the normal flow
    final TransportCallback<StreamResponse> onceTransportCallback = new InvokedOnceTransportCallback<>(callback);
    _userCallbacks.add(onceTransportCallback);
    return response ->
    {
      _userCallbacks.remove(onceTransportCallback);
      onceTransportCallback.onResponse(response);
    };
  }

  private ChannelPoolManager getChannelPoolManagerPerRequest(Request request)
  {
    return isSslRequest(request) ? _sslChannelPoolManager : _channelPoolManager;
  }

  private static boolean isSslRequest(Request request)
  {
    return HTTPS_SCHEME.equals(request.getURI().getScheme());
  }

  /**
   * Resolves the request timeout based on the client configured timeout, request timeout, and preemptive
   * request timeout rate.
   *
   * @param context Request context
   * @param requestTimeout client configured timeout
   * @return Resolve request timeout
   */
  public static long resolveRequestTimeout(RequestContext context, long requestTimeout)
  {
    long resolvedRequestTimeout = requestTimeout;
    Number requestTimeoutRaw = (Number) context.getLocalAttr(R2Constants.REQUEST_TIMEOUT);
    if (requestTimeoutRaw != null)
    {
      resolvedRequestTimeout = requestTimeoutRaw.longValue();
    }

    Double preemptiveTimeoutRate = (Double) context.getLocalAttr(R2Constants.PREEMPTIVE_TIMEOUT_RATE);
    if (preemptiveTimeoutRate != null)
    {
      resolvedRequestTimeout = RequestTimeoutUtil.applyPreemptiveTimeoutRate(resolvedRequestTimeout, preemptiveTimeoutRate);
    }

    return resolvedRequestTimeout;
  }

  /**
   * Resolves the IP Address from the URI host
   *
   * @param request Request object
   * @param requestContext Request's context
   * @return SocketAddress resolved from the URI host
   */
  public static SocketAddress resolveAddress(Request request, RequestContext requestContext)
      throws UnknownHostException, UnknownSchemeException
  {
    final URI uri = request.getURI();
    final String scheme = uri.getScheme();

    if (!HTTP_SCHEME.equalsIgnoreCase(scheme) && !HTTPS_SCHEME.equalsIgnoreCase(scheme))
    {
      throw new UnknownSchemeException("Unknown scheme: " + scheme + " (only http/https is supported)");
    }

    final String host = uri.getHost();
    int port = uri.getPort();
    if (port == -1)
    {
      port = HTTP_SCHEME.equalsIgnoreCase(scheme) ? HTTP_DEFAULT_PORT : HTTPS_DEFAULT_PORT;
    }

    final InetAddress inetAddress = InetAddress.getByName(host);

    final SocketAddress address = new InetSocketAddress(inetAddress, port);
    requestContext.putLocalAttr(R2Constants.REMOTE_SERVER_ADDR, inetAddress.getHostAddress());
    requestContext.putLocalAttr(R2Constants.REMOTE_SERVER_PORT, port);

    return address;
  }

  public long getDnsResolutions() { return _dnsResolutions.get(); }

  public long getDnsResolutionErrors() {
    return _dnsResolutionErrors.get();
  }

  public LongStats getDnsResolutionLatency() {
    return _dnsResolutionLatency.getStats();
  }
}
