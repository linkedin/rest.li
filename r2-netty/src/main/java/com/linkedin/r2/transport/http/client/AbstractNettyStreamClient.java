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


package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.MessageType;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.common.HttpBridge;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract stream based abstract class implementation of {@link TransportClient} on top of Netty
 * libraries. Inheriting this class is a good starting point for protocol specific implementation
 * of TransportClient.
 *
 * @author Steven Ihde
 * @author Ang Xu
 * @author Zhenkai Zhu
 * @version $Revision: $
 */

abstract class AbstractNettyStreamClient implements TransportClient
{
  static final Logger LOG = LoggerFactory.getLogger(AbstractNettyStreamClient.class);

  enum State { RUNNING, SHUTTING_DOWN, REQUESTS_STOPPING, SHUTDOWN }

  private static final int HTTP_DEFAULT_PORT = 80;
  private static final int HTTPS_DEFAULT_PORT = 443;

  protected final ChannelGroup _allChannels;

  protected final AtomicReference<State> _state = new AtomicReference<State>(State.RUNNING);

  protected final ScheduledExecutorService _scheduler;
  protected final ExecutorService _callbackExecutors;

  protected final long _requestTimeout;
  protected final long _shutdownTimeout;
  protected final long _maxResponseSize;
  protected final int _maxConcurrentConnections;

  protected final String _requestTimeoutMessage;
  protected final AbstractJmxManager _jmxManager;

  /**
   * Creates a new HttpNettyClient
   *
   * @param eventLoopGroup            The NioEventLoopGroup; it is the caller's responsibility to
   *                                  shut it down
   * @param executor                  An executor; it is the caller's responsibility to shut it down
   * @param requestTimeout            Timeout, in ms, to get a connection from the pool or create one
   * @param shutdownTimeout           Timeout, in ms, the client should wait after shutdown is
   *                                  initiated before terminating outstanding requests
   * @param maxResponseSize           Maximum size of a HTTP response
   * @param callbackExecutors         An optional EventExecutorGroup to invoke user callback
   * @param jmxManager                A management class that is aware of the creation/shutdown event
   *                                  of the underlying {@link ChannelPoolManager}
   * @param maxConcurrentConnections  Maximum number of concurrent connection attempts the HTTP
   *                                  connection pool can make.
   */
  public AbstractNettyStreamClient(NioEventLoopGroup eventLoopGroup,
      ScheduledExecutorService executor,
      long requestTimeout,
      long shutdownTimeout,
      long maxResponseSize,
      ExecutorService callbackExecutors,
      AbstractJmxManager jmxManager,
      int maxConcurrentConnections)
  {
    _maxResponseSize = maxResponseSize;
    _maxConcurrentConnections = maxConcurrentConnections;
    _scheduler = executor;
    _callbackExecutors = callbackExecutors == null ? eventLoopGroup : callbackExecutors;
    _requestTimeout = requestTimeout;
    _shutdownTimeout = shutdownTimeout;
    _requestTimeoutMessage = "Exceeded request timeout of " + _requestTimeout + "ms";
    _jmxManager = jmxManager;
    _allChannels = new DefaultChannelGroup("R2 client channels", eventLoopGroup.next());
  }

  /* Constructor for test purpose ONLY. */
  AbstractNettyStreamClient(ChannelPoolFactory factory,
      ScheduledExecutorService executor,
      int requestTimeout,
      int shutdownTimeout,
      long maxResponseSize)
  {
    _maxResponseSize = maxResponseSize;
    _scheduler = executor;
    _callbackExecutors = new DefaultEventExecutorGroup(1);
    _requestTimeout = requestTimeout;
    _shutdownTimeout = shutdownTimeout;
    _requestTimeoutMessage = "Exceeded request timeout of " + _requestTimeout + "ms";
    _jmxManager = AbstractJmxManager.NULL_JMX_MANAGER;
    _maxConcurrentConnections = Integer.MAX_VALUE;
    _allChannels = new DefaultChannelGroup("R2 client channels", GlobalEventExecutor.INSTANCE);
  }

  /**
   * Gets statistics from each channel pool. The map keys represent pool names.
   * The values are the corresponding {@link AsyncPoolStats} objects.
   *
   * @return A map of pool names and statistics.
   */
  public abstract Map<String, PoolStats> getPoolStats();

  /**
   * Signals that the client has entered shutdown phase. Performs tasks necessary to shutdown the client and
   * invokes the callback after shutdown tasks are complete.
   *
   * @param callback callback invoked after shutdown is complete
   */
  protected abstract void doShutdown(final Callback<None> callback);

  /**
   * Writes the given request to the given socket address and invokes the callback after request is sent.
   *
   * @param request Request to send
   * @param callback Callback invoked after request is sent
   * @param address Socket address to send the request to
   */
  protected abstract void doWriteRequest(final Request request, final SocketAddress address,
      final TimeoutTransportCallback<StreamResponse> callback);

  @Override
  public void restRequest(RestRequest request,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      final TransportCallback<RestResponse> callback)
  {
    throw new UnsupportedOperationException("This client only handles streaming.");
  }

  @Override
  public void streamRequest(StreamRequest request,
      RequestContext requestContext,
      Map<String, String> wireAttrs,
      TransportCallback<StreamResponse> callback)
  {
    MessageType.setMessageType(MessageType.Type.REST, wireAttrs);
    writeRequestWithTimeout(request, requestContext, wireAttrs, HttpBridge.streamToHttpCallback(callback, request));
  }

  @Override
  public void shutdown(final Callback<None> callback)
  {
    LOG.info("Shutdown requested");
    if (_state.compareAndSet(State.RUNNING, State.SHUTTING_DOWN))
    {
      LOG.info("Shutting down");
      doShutdown(callback);
    }
    else
    {
      callback.onError(new IllegalStateException("Shutdown has already been requested."));
    }
  }

  private void writeRequestWithTimeout(final StreamRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
      TransportCallback<StreamResponse> callback)
  {
    StreamExecutionCallback executionCallback = new StreamExecutionCallback(_callbackExecutors, callback);
    // By wrapping the callback in a Timeout callback before passing it along, we deny the rest
    // of the code access to the unwrapped callback.  This ensures two things:
    // 1. The user callback will always be invoked, since the Timeout will eventually expire
    // 2. The user callback is never invoked more than once
    final TimeoutTransportCallback<StreamResponse> timeoutCallback =
        new TimeoutTransportCallback<StreamResponse>(_scheduler,
            _requestTimeout,
            TimeUnit.MILLISECONDS,
            executionCallback,
            _requestTimeoutMessage);

    final StreamRequest requestWithWireAttrHeaders = request.builder()
        .overwriteHeaders(WireAttributeHelper.toWireAttributes(wireAttrs))
        .build(request.getEntityStream());

    // We treat full request (already fully in memory) and real stream request (not fully buffered in memory)
    // differently. For the latter we have to use chunked transfer encoding. For the former we can avoid
    // using chunked encoding which has two benefits: 1) slightly save cost of transmitting over the wire; 2) more
    // importantly legacy R2 servers cannot work with chunked transfer encoding, so this allow the new client
    // talk to legacy R2 servers without problem if they're just using restRequest (full request).
    if(isFullRequest(requestContext))
    {
      Messages.toRestRequest(requestWithWireAttrHeaders, new Callback<RestRequest>()
      {
        @Override
        public void onError(Throwable e)
        {
          errorResponse(timeoutCallback, e);
        }

        @Override
        public void onSuccess(RestRequest restRequest)
        {
          writeRequest(restRequest, requestContext, timeoutCallback);
        }
      });
    }
    else
    {
      writeRequest(requestWithWireAttrHeaders, requestContext, timeoutCallback);
    }
  }

  private void writeRequest(final Request request, final RequestContext requestContext, final TimeoutTransportCallback<StreamResponse> callback)
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

    final SocketAddress address;
    try
    {
      // TODO investigate DNS resolution and timing
      InetAddress inetAddress = InetAddress.getByName(host);
      address = new InetSocketAddress(inetAddress, port);
      requestContext.putLocalAttr(R2Constants.REMOTE_SERVER_ADDR, inetAddress.getHostAddress());
    }
    catch (UnknownHostException e)
    {
      errorResponse(callback, e);
      return;
    }

    doWriteRequest(request, address, callback);
  }

  static <T> void errorResponse(TransportCallback<T> callback, Throwable e)
  {
    callback.onResponse(TransportResponseImpl.<T>error(e));
  }

  static boolean isFullRequest(RequestContext requestContext)
  {
    Object isFull = requestContext.getLocalAttr(R2Constants.IS_FULL_REQUEST);
    return isFull != null && (Boolean)isFull;
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
