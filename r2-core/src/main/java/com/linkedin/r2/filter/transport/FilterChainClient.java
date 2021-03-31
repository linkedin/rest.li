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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.TimedRestFilter;
import com.linkedin.r2.filter.TimedStreamFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * {@link TransportClient} adapter which composes a {@link TransportClient}
 * and a {@link FilterChain}.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class FilterChainClient implements TransportClient
{
  private final TransportClient _client;
  private final FilterChain _filters;

  /**
   * Construct a new instance by composing the specified {@link TransportClient}
   * and {@link FilterChain}.
   *
   * @param client the {@link TransportClient} to be composed.
   * @param filters the {@link FilterChain} to be composed.
   */
  public FilterChainClient(TransportClient client, FilterChain filters)
  {
    _client = client;

    final ResponseFilter responseFilter = new ResponseFilter();
    final ClientRequestFilter requestFilter = new ClientRequestFilter(_client);

    _filters = filters
            .addFirstRest(responseFilter)
            .addLastRest(requestFilter)
            .addFirst(responseFilter)
            .addLast(requestFilter);
  }

  @Override
  public void restRequest(RestRequest request,
                   RequestContext requestContext,
                   Map<String, String> wireAttrs,
                   TransportCallback<RestResponse> callback)
  {
    ResponseFilter.registerCallback(createWrappedClientTimingCallback(requestContext, callback), requestContext);
    markOnRequestTimings(requestContext);
    _filters.onRestRequest(request, requestContext, wireAttrs);
  }

  @Override
  public void streamRequest(StreamRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<StreamResponse> callback)
  {
    ResponseFilter.registerCallback(createWrappedClientTimingCallback(requestContext, callback), requestContext);
    markOnRequestTimings(requestContext);
    _filters.onStreamRequest(request, requestContext, wireAttrs);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _client.shutdown(callback);

    _filters.getStreamFilters().stream().filter(TimedStreamFilter.class::isInstance)
      .map(TimedStreamFilter.class::cast).forEach(TimedStreamFilter::onShutdown);

    _filters.getRestFilters().stream().filter(TimedRestFilter.class::isInstance)
      .map(TimedRestFilter.class::cast).forEach(TimedRestFilter::onShutdown);
  }

  /**
   * Creates a thin wrapper around the given callback which simply marks the end of the R2 client response filter chain
   * before executing the wrapped callback.
   *
   * @param requestContext request context
   * @param callback callback to wrap
   * @param <T> callback value type (rest or stream response)
   * @return wrapped callback
   */
  private static <T extends Response> TransportCallback<T> createWrappedClientTimingCallback(RequestContext requestContext,
      TransportCallback<T> callback)
  {
    return (TransportResponse<T> response) -> {
      TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_R2_FILTER_CHAIN.key());
      callback.onResponse(response);
    };
  }

  private static void markOnRequestTimings(RequestContext requestContext)
  {
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.CLIENT_REQUEST_R2_FILTER_CHAIN.key());
  }
}
