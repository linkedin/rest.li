/*
   Copyright (c) 2015 LinkedIn Corp.

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


import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.NextResponseFilter;
import com.linkedin.restli.server.filter.ResponseFilter;

import java.util.Collections;
import java.util.List;


/**
 * A chain of {@link com.linkedin.restli.server.filter.ResponseFilter}s that are executed after the request has been
 * processed by the resource implementation and before the response is handed to the underlying R2 stack. These {@link
 * com.linkedin.restli.server.filter.ResponseFilter}s get a chance to process and/or modify various parameters of the
 * outgoing response. If an error is encountered while invoking a filter in the filter chain, subsequent filters get an
 * opportunity to handle/process the error.
 * <p/>
 * Filter Order: Filters are invoked in the order in which they are specified in the filter configuration.
 *
 * @author nshankar
 */
public final class RestLiResponseFilterChain implements NextResponseFilter
{
  private final List<ResponseFilter> _filters;
  private final RestLiResponseFilterContextFactory _responseFilterContextFactory;
  private final RestLiResponseFilterChainCallback _restLiResponseFilterChainCallback;
  private final RestLiAttachmentReader _requestAttachmentReader;
  private final RestLiResponseAttachments _restLiResponseAttachments;
  private int _cursor;

  public RestLiResponseFilterChain(List<ResponseFilter> filters,
                                   final RestLiResponseFilterContextFactory responseFilterContextFactory,
                                   final RestLiResponseFilterChainCallback restLiResponseFilterChainCallback,
                                   final RestLiAttachmentReader requestAttachmentReader,
                                   final RestLiResponseAttachments responseAttachments)
  {
    _filters = filters == null ? Collections.<ResponseFilter>emptyList() : filters;
    _cursor = 0;
    _restLiResponseFilterChainCallback = restLiResponseFilterChainCallback;
    _responseFilterContextFactory = responseFilterContextFactory;
    _requestAttachmentReader = requestAttachmentReader;
    _restLiResponseAttachments = responseAttachments;
  }

  @Override
  public void onResponse(final FilterRequestContext requestContext, final FilterResponseContext responseContext)
  {
    if (_cursor < _filters.size())
    {
      try
      {
        _filters.get(_cursor++).onResponse(requestContext, responseContext, this);
      }
      catch (Throwable t)
      {
        // Convert the throwable to a filter response context and invoke the next filter.
        onResponse(requestContext, _responseFilterContextFactory.fromThrowable(t));
      }
    }
    else
    {
      // Now that we are done invoking all the filters, invoke the the response filter callback.
      _restLiResponseFilterChainCallback.onCompletion(responseContext.getResponseData(),
              _requestAttachmentReader, _restLiResponseAttachments);
    }
  }
}
