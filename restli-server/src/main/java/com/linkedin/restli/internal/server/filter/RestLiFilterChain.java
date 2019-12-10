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

import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import java.util.Collections;
import java.util.List;


/**
 * Filter chain contains filters that are applied to requests and responses.
 *
 * @author gye
 */
public class RestLiFilterChain
{
  private final RestLiFilterChainIterator _filterChainIterator;

  public RestLiFilterChain(List<Filter> filters,
      FilterChainDispatcher filterChainDispatcher,
      FilterChainCallback filterChainCallback)
  {
    filters = filters == null ? Collections.emptyList() : filters;
    _filterChainIterator = new RestLiFilterChainIterator(filters, filterChainDispatcher, filterChainCallback);
  }

  /**
   * Creates a filter chain iterator and passes the request through it.
   *
   * @param requestContext
   *          {@link FilterRequestContext}
   */
  public void onRequest(FilterRequestContext requestContext,
                        RestLiFilterResponseContextFactory filterResponseContextFactory)
  {
    // Initiate the filter chain iterator. The RestLiCallback will be passed to the method invoker at the end of the
    // filter chain.
    _filterChainIterator.onRequest(requestContext, filterResponseContextFactory,
                                   new RestLiCallback(requestContext, filterResponseContextFactory, this));
  }

  /**
   * Creates a filter chain iterator and passes the response through it.
   *  @param requestContext
   *          {@link FilterRequestContext}
   * @param responseContext
   *          {@link FilterResponseContext}
   */
  public void onResponse(FilterRequestContext requestContext, FilterResponseContext responseContext)
  {
    _filterChainIterator.onResponse(requestContext, responseContext);
  }

  /**
   * Creates a filter chain iterator and passes an error response through it.
   *  @param th
   *          {@link Throwable}
   * @param requestContext
   *          {@link FilterRequestContext}
   * @param responseContext
   *          {@link FilterResponseContext}
   */
  public void onError(Throwable th, FilterRequestContext requestContext, FilterResponseContext responseContext)
  {
    _filterChainIterator.onError(th, requestContext, responseContext);
  }
}
