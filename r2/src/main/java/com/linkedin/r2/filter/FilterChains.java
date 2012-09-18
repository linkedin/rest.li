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
   * Returns a {@link FilterChain} constructed with the filters inserted in the order they appear
   * in the supplied list.
   *
   * @param filters the filters to use to create a new filter chain
   * @return the new filter chain
   */
  public static FilterChain create(Filter... filters)
  {
    FilterChain fc = empty();
    for (Filter filter : filters)
    {
      fc = fc.addLast(filter);
    }
    return fc;
  }
}
