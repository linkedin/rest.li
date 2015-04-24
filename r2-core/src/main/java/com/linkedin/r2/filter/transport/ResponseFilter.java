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
import com.linkedin.r2.filter.message.rest.RestResponseFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.bridge.common.NullTransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Filter implementation which invokes a {@link TransportCallback} when processing responses.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class ResponseFilter implements RestResponseFilter
{
  private static final String CALLBACK_ATTR = ResponseFilter.class.getName() + ".callback";

  private static final Logger _log = LoggerFactory.getLogger(ResponseFilter.class);

  /**
   * Register a {@link TransportCallback} to be invoked for success/error responses.
   *
   * @param callback the callback to be invoked on response or error.
   * @param context the {@link RequestContext} to store the callback.
   * @param <T> the Response message subtype for the {@link TransportCallback}.
   */
  public static <T> void registerCallback(TransportCallback<T> callback, RequestContext context)
  {
    context.putLocalAttr(CALLBACK_ATTR, callback);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    final TransportCallback<RestResponse> callback = getCallback(requestContext);
    callback.onResponse(TransportResponseImpl.success(res, wireAttrs));
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<RestRequest, RestResponse> nextFilter)
  {
    final TransportCallback<RestResponse> callback = getCallback(requestContext);
    callback.onResponse(TransportResponseImpl.<RestResponse>error(ex, wireAttrs));
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  @SuppressWarnings("unchecked")
  private <T> TransportCallback<T> getCallback(RequestContext context)
  {
    TransportCallback<T> callback = (TransportCallback<T>) context.getLocalAttr(CALLBACK_ATTR);
    if (callback == null)
    {
      _log.error("No callback registered in local attributes. Caller will not get response. Attributes: " + context);
      callback = new NullTransportCallback<T>();
    }
    return callback;
  }
}
