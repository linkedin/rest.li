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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * Iterates through a filter chain and executes each filter's logic.
 *
 * @author gye
 */
public class RestLiFilterChainIterator
{
  private List<Filter> _filters;
  private FilterChainCallback _filterChainCallback;
  private int _cursor;


   RestLiFilterChainIterator(List<Filter> filters, FilterChainCallback filterChainCallback)
  {
    _filters = filters;
    _filterChainCallback = filterChainCallback;
    _cursor = 0;
  }

  public void onRequest(FilterRequestContext requestContext,
                        RestLiFilterResponseContextFactory filterResponseContextFactory,
                        RestLiCallback restLiCallback)
  {
    if (_cursor < _filters.size())
    {
      CompletableFuture<Void> filterFuture;
      try
      {
        filterFuture = _filters.get(_cursor++).onRequest(requestContext);
      }
      catch (Throwable th)
      {
        onError(th, requestContext, filterResponseContextFactory.fromThrowable(th), null);
        return;
      }
      filterFuture.thenAccept((v) ->
        onRequest(requestContext, filterResponseContextFactory, restLiCallback)
      );
      filterFuture.exceptionally((throwable) -> {
        onError(throwable, requestContext, filterResponseContextFactory.fromThrowable(throwable), null);
        return null;
      });
    }
    else
    {
      // Now that all the filters have been invoked successfully, invoke onSuccess on the filter chain callback.
      _filterChainCallback.onRequestSuccess(requestContext.getRequestData(), restLiCallback);
    }
  }

  public void onResponse(FilterRequestContext requestContext, FilterResponseContext responseContext,
                         RestLiResponseAttachments responseAttachments)
  {
    if (_cursor > 0)
    {
      CompletableFuture<Void> filterFuture;
      try
      {
        filterFuture = _filters.get(--_cursor).onResponse(requestContext, responseContext);
      }
      catch (Throwable th)
      {
        updateResponseContextWithError(th, responseContext);
        onError(th, requestContext, responseContext, responseAttachments);
        return;
      }
      filterFuture.thenAccept((v) ->
        onResponse(requestContext, responseContext, responseAttachments)
      );
      filterFuture.exceptionally((throwable) -> {
        updateResponseContextWithError(throwable, responseContext);
        onError(throwable, requestContext, responseContext, responseAttachments);
        return null;
      });
    }
    else
    {
      // Now that we are done invoking all the filters, invoke the response filter callback.
      _filterChainCallback.onResponseSuccess(responseContext.getResponseData(), responseAttachments);
    }
  }

  public void onError(Throwable th, FilterRequestContext requestContext, FilterResponseContext responseContext,
                      RestLiResponseAttachments responseAttachments)
  {
    if (_cursor > 0)
    {
      CompletableFuture<Void> filterFuture;
      try
      {
        filterFuture = _filters.get(--_cursor).onError(th, requestContext, responseContext);
      }
      catch (Throwable t)
      {
        // update response context with latest error.
        updateResponseContextWithError(t, responseContext);
        onError(t, requestContext, responseContext, responseAttachments);
        return;
      }

      filterFuture.thenAccept((v) -> {
        removeErrorResponseHeader(responseContext);
        // if the filter future completes without an error, this means the previous error has been corrected
        // therefore, the filter chain will invoke the onResponse() method.
        onResponse(requestContext, responseContext, responseAttachments);
      });
      filterFuture.exceptionally((throwable) -> {
        // update response context with latest error
        updateResponseContextWithError(throwable, responseContext);
        onError(throwable, requestContext, responseContext, responseAttachments);
        return null;
      });
    }
    else
    {
      // Now that we are done invoking all the filters, invoke the the response filter callback.
      _filterChainCallback.onError(th, responseContext.getResponseData(), responseAttachments);
    }
  }

  private void updateResponseContextWithError(Throwable throwable, FilterResponseContext responseContext)
  {
    assert throwable != null;

    setErrorResponseHeader(responseContext);

    RestLiServiceException restLiServiceException = RestLiServiceException.fromThrowable(throwable);
    responseContext.getResponseData().getResponseEnvelope().setExceptionInternal(restLiServiceException);
  }

  private void setErrorResponseHeader(FilterResponseContext responseContext)
  {
    Map<String, String> requestHeaders = responseContext.getResponseData().getHeaders();
    requestHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
        ProtocolVersionUtil.extractProtocolVersion(requestHeaders).toString());
    requestHeaders.put(HeaderUtil.getErrorResponseHeaderName(requestHeaders), RestConstants.HEADER_VALUE_ERROR);
  }

  private void removeErrorResponseHeader(FilterResponseContext responseContext)
  {
    Map<String, String> requestHeaders = responseContext.getResponseData().getHeaders();
    requestHeaders.remove(HeaderUtil.getErrorResponseHeaderName(requestHeaders));
  }
}
