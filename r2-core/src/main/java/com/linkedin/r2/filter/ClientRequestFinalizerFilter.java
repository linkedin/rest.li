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

package com.linkedin.r2.filter;

import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.util.RequestContextUtil;
import com.linkedin.r2.util.finalizer.RequestFinalizerManagerImpl;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A filter that allows registration of {@link com.linkedin.r2.util.finalizer.RequestFinalizer}s
 * to be executed at the end of a request. These are intended to be the last executions after a
 * response is returned back to the application.
 *
 * @author Chris Zhang
 */
public class ClientRequestFinalizerFilter implements RestFilter, StreamFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(ClientRequestFinalizerFilter.class);

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    handleRequest(req, requestContext, wireAttrs, nextFilter);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    nextFilter.onResponse(res, requestContext, wireAttrs);

    doFinalizeRequest(requestContext, res, null);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    handleError(ex, requestContext, wireAttrs, nextFilter);
  }

  @Override
  public void onStreamRequest(StreamRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
      NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    handleRequest(req, requestContext, wireAttrs, nextFilter);
  }

  @Override
  public void onStreamResponse(StreamResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
      NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    res.getEntityStream().addObserver(new Observer() {

      @Override
      public void onDataAvailable(ByteString data)
      {
        // do nothing
      }

      @Override
      public void onDone()
      {
        doFinalizeRequest(requestContext, res, null);
      }

      @Override
      public void onError(Throwable e)
      {
        doFinalizeRequest(requestContext, res, e);
      }
    });

    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onStreamError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
      NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    handleError(ex, requestContext, wireAttrs, nextFilter);
  }

  private <REQ extends Request, RES extends Response> void handleRequest(REQ request, RequestContext requestContext,
      Map<String, String> wireAttrs, NextFilter<REQ, RES> nextFilter)
  {
    final RequestFinalizerManagerImpl manager = (RequestFinalizerManagerImpl) requestContext.getLocalAttr(
        R2Constants.CLIENT_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY);

    if (manager == null)
    {
      requestContext.putLocalAttr(R2Constants.CLIENT_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY,
          new RequestFinalizerManagerImpl(request, requestContext));
    }
    else
    {
      if (LOG.isDebugEnabled())
      {
        LOG.debug(String.format("A RequestFinalizerManager already exists in the RequestContext.\nRequest ID: %s\nRequest: %s\nRequestContext ID: %s"
                                  + "\nRequestContext: %s",
                                System.identityHashCode(request), request, System.identityHashCode(requestContext), requestContext),
                  new RuntimeException());
      }
    }

    nextFilter.onRequest(request, requestContext, wireAttrs);
  }

  private <REQ extends Request, RES extends Response> void handleError(Throwable ex, RequestContext requestContext,
      Map<String, String> wireAttrs, NextFilter<REQ, RES> nextFilter)
  {
    nextFilter.onError(ex, requestContext, wireAttrs);

    doFinalizeRequest(requestContext, null, ex);
  }

  private void doFinalizeRequest(RequestContext requestContext, Response response, Throwable ex)
  {
    final RequestFinalizerManagerImpl manager =
        (RequestFinalizerManagerImpl) RequestContextUtil.getClientRequestFinalizerManager(requestContext);

    if (manager == null)
    {
      LOG.warn("Client-side RequestFinalizerManager was not found in request context.");
    }
    else
    {
      final boolean finalized = manager.finalizeRequest(response, ex);

      if (!finalized)
      {
        if (LOG.isDebugEnabled())
        {
          LOG.debug(String.format("Attempted to finalize request from RequestContext ID = %s\nRequestContext = %s",
                                  System.identityHashCode(requestContext), requestContext));
        }
        LOG.warn("Request has already been finalized, but we expect this to be the first time.");
      }
    }
  }
}
