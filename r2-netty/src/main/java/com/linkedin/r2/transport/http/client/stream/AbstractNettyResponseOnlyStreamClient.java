package com.linkedin.r2.transport.http.client.stream;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.netty.callback.StreamExecutionCallback;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.http.client.AbstractJmxManager;
import com.linkedin.r2.transport.http.client.TimeoutTransportCallback;
import com.linkedin.r2.transport.http.client.common.AbstractNettyClient;
import com.linkedin.r2.transport.http.client.common.ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract stream based abstract class implementation of {@link TransportClient} on top of Netty
 * libraries. Inheriting this class is a good starting point for protocol specific implementation
 * of TransportClient.
 *
 */

public abstract class AbstractNettyResponseOnlyStreamClient extends AbstractNettyClient<RestRequest, StreamResponse>
{
  private final ExecutorService _callbackExecutors;

  /**
   * Creates a new HttpNettyClient
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
   * */
  public AbstractNettyResponseOnlyStreamClient(EventLoopGroup eventLoopGroup, ScheduledExecutorService executor, long requestTimeout,
      long shutdownTimeout, ExecutorService callbackExecutors, AbstractJmxManager jmxManager,
      ChannelPoolManager channelPoolManager, ChannelPoolManager sslChannelPoolManager)
  {
    super(executor, requestTimeout, shutdownTimeout, jmxManager, channelPoolManager, sslChannelPoolManager);
    _callbackExecutors = callbackExecutors == null ? eventLoopGroup : callbackExecutors;
  }

  /* Constructor for test purpose ONLY. */
  public AbstractNettyResponseOnlyStreamClient(ChannelPoolFactory factory,
      ScheduledExecutorService executor,
      int requestTimeout,
      int shutdownTimeout)
  {
    super(factory, executor, requestTimeout, shutdownTimeout);
    _callbackExecutors = new DefaultEventExecutorGroup(1);
  }

  @Override
  public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
      final TransportCallback<RestResponse> callback)
  {
    throw new UnsupportedOperationException("Rest is not supported.");
  }

  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
      TransportCallback<StreamResponse> callback) {
    throw new UnsupportedOperationException("Bidirectional Stream is not supported.");
  }

  @Override
  protected TransportCallback<StreamResponse> getExecutionCallback(TransportCallback<StreamResponse> callback)
  {
    return new StreamExecutionCallback(_callbackExecutors, callback);
  }

  protected abstract void doWriteRequestWithWireAttrHeaders(Request request, final RequestContext requestContext, SocketAddress address,
      Map<String, String> wireAttrs, TimeoutTransportCallback<StreamResponse> callback,
      long requestTimeout);

  @Override
  protected void doWriteRequest(RestRequest request, final RequestContext requestContext, SocketAddress address,
      Map<String, String> wireAttrs, TimeoutTransportCallback<StreamResponse> callback,
      long requestTimeout)
  {
    final RestRequest newRequest = new RestRequestBuilder(request)
        .overwriteHeaders(WireAttributeHelper.toWireAttributes(wireAttrs))
        .build();

    requestContext.putLocalAttr(R2Constants.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1);
    doWriteRequestWithWireAttrHeaders(newRequest, requestContext, address, wireAttrs, callback, requestTimeout);
  }

}
