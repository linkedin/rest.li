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


import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.NextRequestFilter;
import com.linkedin.restli.server.filter.RequestFilter;

import java.util.Collections;
import java.util.List;


/**
 * A chain of {@link com.linkedin.restli.server.filter.RequestFilter}s that are executed before the request is
 * dispatched to the resource implementation. These {@link com.linkedin.restli.server.filter.RequestFilter}s get a
 * chance to process and/or modify various parameters of the incoming request. If an error is encountered while invoking
 * a filter in the filter chain, subsequent filters are NOT invoked. Therefore, in order for the request to be
 * dispatched to the resource implementation, all request filter invocations must be successful.
 * <p/>
 * Filter Order: Filters are invoked in the order in which they are specified in the filter configuration.
 * <p/>
 *
 * @author nshankar
 */
public final class RestLiRequestFilterChain implements NextRequestFilter
{
  private final List<RequestFilter> _filters;
  private final RestLiRequestFilterChainCallback _restLiRequestFilterChainCallback;
  private int _cursor;

  public RestLiRequestFilterChain(final List<RequestFilter> filters,
                                  final RestLiRequestFilterChainCallback restLiRequestFilterChainCallback)
  {
    _filters = filters == null ? Collections.<RequestFilter>emptyList() : filters;
    _cursor = 0;
    _restLiRequestFilterChainCallback = restLiRequestFilterChainCallback;
  }

  @Override
  public void onRequest(final FilterRequestContext requestContext)
  {
    if (_cursor < _filters.size())
    {
      try
      {
        _filters.get(_cursor++).onRequest(requestContext, this);
      }
      catch (Throwable throwable)
      {
        // Now that we have encountered an error in one of the request filters, invoke onError on the filter chain callback.
        _restLiRequestFilterChainCallback.onError(throwable);
      }
    }
    // Now that all the filters have been invoked successfully, invoke onSuccess on the filter chain callback.
    else
    {
      _restLiRequestFilterChainCallback.onSuccess(requestContext.getRequestData());
    }
  }
}
