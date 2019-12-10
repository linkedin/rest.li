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

/* $Id$ */
package com.linkedin.r2.filter.transport;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.BaseConnector;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Filter implementation which sends requests to a {@link TransportDispatcher} for processing.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class DispatcherRequestFilter implements StreamFilter, RestFilter
{
  private final TransportDispatcher _dispatcher;

  /**
   * Construct a new instance, using the specified {@link com.linkedin.r2.transport.common.bridge.server.TransportDispatcher}.
   *
   * @param dispatcher the {@link com.linkedin.r2.transport.common.bridge.server.TransportDispatcher} to be used for processing requests.C
   */
  public DispatcherRequestFilter(TransportDispatcher dispatcher)
  {
    _dispatcher = dispatcher;
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    markOnRequestTimings(requestContext);
    try
    {
      _dispatcher.handleRestRequest(req, wireAttrs, requestContext,
          createCallback(requestContext, nextFilter)
      );
    }
    catch (Exception e)
    {
      nextFilter.onError(e, requestContext, new HashMap<>());
    }
  }

  private <REQ extends Request, RES extends Response> TransportCallback<RES> createCallback(
      final RequestContext requestContext,
      final NextFilter<REQ, RES> nextFilter)
  {
    return res -> {
      markOnResponseTimings(requestContext);
      final Map<String, String> wireAttrs = res.getWireAttributes();
      if (res.hasError())
      {
        nextFilter.onError(res.getError(), requestContext, wireAttrs);
      }
      else
      {
        nextFilter.onResponse(res.getResponse(), requestContext, wireAttrs);
      }
    };
  }

  @Override
  public void onStreamRequest(StreamRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    markOnRequestTimings(requestContext);
    Connector connector = null;
    try
    {
      final AtomicBoolean responded = new AtomicBoolean(false);
      TransportCallback<StreamResponse> callback = createStreamCallback(requestContext, nextFilter, responded);
      connector = new Connector(responded, nextFilter, requestContext, wireAttrs);
      req.getEntityStream().setReader(connector);
      EntityStream newStream = EntityStreams.newEntityStream(connector);
      _dispatcher.handleStreamRequest(req.builder().build(newStream), wireAttrs, requestContext, callback);
    }
    catch (Exception e)
    {
      nextFilter.onError(e, requestContext, new HashMap<>());
      if (connector != null)
      {
        connector.cancel();
      }
    }
  }

  private <REQ extends Request, RES extends Response> TransportCallback<RES> createStreamCallback(
          final RequestContext requestContext,
          final NextFilter<REQ, RES> nextFilter,
          final AtomicBoolean responded)
  {
    return res -> {
      if (responded.compareAndSet(false, true))
      {
        markOnResponseTimings(requestContext);
        final Map<String, String> wireAttrs = res.getWireAttributes();
        if (res.hasError())
        {
          nextFilter.onError(res.getError(), requestContext, wireAttrs);
        }
        else
        {
          nextFilter.onResponse(res.getResponse(), requestContext, wireAttrs);
        }
      }
    };
  }

  private static void markOnRequestTimings(RequestContext requestContext)
  {
    TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST_R2_FILTER_CHAIN.key());
    TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST_R2.key());
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST_RESTLI.key());
  }

  private static void markOnResponseTimings(RequestContext requestContext)
  {
    TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.SERVER_RESPONSE_RESTLI.key());
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.SERVER_RESPONSE_R2.key());
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.SERVER_RESPONSE_R2_FILTER_CHAIN.key());
  }

  private static class Connector extends BaseConnector
  {

    private final AtomicBoolean _responded;
    private final NextFilter<StreamRequest, StreamResponse> _nextFilter;
    private final RequestContext _requestContext;
    private final Map<String, String> _wireAttrs;

    Connector(AtomicBoolean responded, NextFilter<StreamRequest, StreamResponse> nextFilter,
              RequestContext requestContext, Map<String, String> wireAttrs)
    {
      super();
      _responded = responded;
      _nextFilter = nextFilter;
      _requestContext = requestContext;
      _wireAttrs = wireAttrs;
    }

    @Override
    public void onAbort(Throwable e)
    {
      super.onAbort(e);
      if (_responded.compareAndSet(false, true))
      {
        _nextFilter.onError(e, _requestContext, _wireAttrs);
      }
    }
  }
}
