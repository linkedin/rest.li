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

package com.linkedin.r2.transport.http.client.stream;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
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

import com.linkedin.pegasus.io.netty.channel.EventLoopGroup;
import com.linkedin.pegasus.io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract stream based abstract class implementation of {@link TransportClient} on top of Netty
 * libraries. Inheriting this class is a good starting point for protocol specific implementation
 * of TransportClient.
 *
 * @author Steven Ihde
 * @author Ang Xu
 * @author Zhenkai Zhu
 */

public abstract class AbstractNettyStreamClient extends AbstractNettyClient<StreamRequest, StreamResponse>
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
  public AbstractNettyStreamClient(EventLoopGroup eventLoopGroup, ScheduledExecutorService executor, long requestTimeout,
                                   long shutdownTimeout, ExecutorService callbackExecutors, AbstractJmxManager jmxManager,
                                   ChannelPoolManager channelPoolManager, ChannelPoolManager sslChannelPoolManager)
  {
    super(executor, requestTimeout, shutdownTimeout, jmxManager, channelPoolManager, sslChannelPoolManager);
    _callbackExecutors = callbackExecutors == null ? eventLoopGroup : callbackExecutors;
  }

  /* Constructor for test purpose ONLY. */
  public AbstractNettyStreamClient(ChannelPoolFactory factory,
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
  protected TransportCallback<StreamResponse> getExecutionCallback(TransportCallback<StreamResponse> callback)
  {
    return new StreamExecutionCallback(_callbackExecutors, callback);
  }

  protected abstract void doWriteRequestWithWireAttrHeaders(Request request, final RequestContext requestContext, SocketAddress address,
                                                            Map<String, String> wireAttrs, TimeoutTransportCallback<StreamResponse> callback,
                                                            long requestTimeout);

  @Override
  protected void doWriteRequest(StreamRequest request, final RequestContext requestContext, SocketAddress address,
                                Map<String, String> wireAttrs, TimeoutTransportCallback<StreamResponse> callback,
                                long requestTimeout)
  {
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
          errorResponse(callback, e);
        }

        @Override
        public void onSuccess(RestRequest restRequest)
        {
          doWriteRequestWithWireAttrHeaders(restRequest, requestContext, address, wireAttrs, callback, requestTimeout);
        }
      });
    }
    else
    {
      doWriteRequestWithWireAttrHeaders(requestWithWireAttrHeaders, requestContext, address, wireAttrs, callback, requestTimeout);
    }
  }

  private static boolean isFullRequest(RequestContext requestContext)
  {
    Object isFull = requestContext.getLocalAttr(R2Constants.IS_FULL_REQUEST);
    return isFull != null && (Boolean)isFull;
  }
}
