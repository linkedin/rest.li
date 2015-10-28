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
package com.linkedin.r2.filter;

import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;

import java.util.List;

/**
 * Factory methods for creating new {@link FilterChain}s.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class FilterChains
{
  private static final FilterChain EMPTY_FILTER_CHAIN = new FilterChainImpl();

  /**
   * Returns an empty {@link FilterChain}.
   * @return an empty FilterChain
   */
  public static FilterChain empty()
  {
    return EMPTY_FILTER_CHAIN;
  }

  /**
   * Returns a {@link FilterChain} constructed with the rest filters inserted in the order they appear
   * in the supplied list.
   *
   * @param filters the rest filters to use to create a new filter chain
   * @return the new filter chain
   */
  public static FilterChain createRestChain(RestFilter... filters)
  {
    FilterChain fc = empty();
    for (RestFilter filter : filters)
    {
      fc = fc.addLastRest(filter);
    }
    return fc;
  }

  /**
   * Returns a {@link FilterChain} constructed with the steram filters inserted in the order they appear
   * in the supplied list.
   *
   * @param filters the stream filters to use to create a new filter chain
   * @return the new filter chain
   */
  public static FilterChain createStreamChain(StreamFilter... filters)
  {
    FilterChain fc = empty();
    for (StreamFilter filter : filters)
    {
      fc = fc.addLast(filter);
    }
    return fc;
  }

  /**
   * Returns a {@link FilterChain} constructed with the rest filters and stream filters inserted in the order they appear
   * in the supplied list.
   *
   * @param restFilters the rest filters to use to construct the filter chain
   * @param streamFilters the stream filters to use to construct the filter chain
   * @return the new filter chain
   */
  public static FilterChain create(List<RestFilter> restFilters, List<StreamFilter> streamFilters)
  {
    FilterChain fc = empty();
    if (restFilters != null) {
      for (RestFilter filter: restFilters)
      {
        fc = fc.addLastRest(filter);
      }
    }

    if (streamFilters != null) {
      for (StreamFilter filter: streamFilters)
      {
        fc = fc.addLast(filter);
      }
    }

    return fc;
  }
}
