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
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import java.util.Map;


/**
 * Filter implementation which sends requests through a specified {@link TransportClient}.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class ClientRequestFilter implements StreamFilter, RestFilter
{
  private final TransportClient _client;

  /**
   * Construct a new instance using the specified client.
   *
   * @param client the {@link TransportClient} used to send requests.
   */
  public ClientRequestFilter(TransportClient client)
  {
    _client = client;
  }


  @Override
  public void onRestRequest(RestRequest req, final RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            final NextFilter<RestRequest, RestResponse> nextFilter)
  {
    markOnRequestTimings(requestContext);

    try
    {
      _client.restRequest(req, requestContext, wireAttrs, createCallback(requestContext, nextFilter));
    }
    catch (Exception e)
    {
      nextFilter.onError(e, requestContext, WireAttributeHelper.newWireAttributes());
    }
  }

  @Override
  public void onStreamRequest(StreamRequest req, final RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            final NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    markOnRequestTimings(requestContext);

    try
    {
      _client.streamRequest(req, requestContext, wireAttrs, createCallback(requestContext, nextFilter));
    }
    catch (Exception e)
    {
      nextFilter.onError(e, requestContext, WireAttributeHelper.newWireAttributes());
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

  private static void markOnRequestTimings(RequestContext requestContext)
  {
    TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.CLIENT_REQUEST_R2_FILTER_CHAIN.key());
    TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.CLIENT_REQUEST_R2.key());
    TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.CLIENT_REQUEST.key());
  }

  private static void markOnResponseTimings(RequestContext requestContext)
  {
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.CLIENT_RESPONSE.key());
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_R2.key());
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_R2_FILTER_CHAIN.key());
  }
}
