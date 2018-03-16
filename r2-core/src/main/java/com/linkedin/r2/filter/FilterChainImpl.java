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

package com.linkedin.r2.filter;

import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 */
/* package private */ class FilterChainImpl implements FilterChain
{
  private final List<RestFilter> _restFilters;
  private final List<StreamFilter> _streamFilters;

  public FilterChainImpl()
  {
    _restFilters = Collections.emptyList();
    _streamFilters = Collections.emptyList();
  }

  private FilterChainImpl(List<RestFilter> restFilters, List<StreamFilter> streamFilters)
  {
    _restFilters = Collections.unmodifiableList(new ArrayList<RestFilter>(restFilters));
    _streamFilters = Collections.unmodifiableList(new ArrayList<StreamFilter>(streamFilters));
  }

  @Override
  public FilterChain addFirstRest(RestFilter filter)
  {
    notNull(filter, "filter");
    return new FilterChainImpl(doAddFirst(_restFilters, decorateRestFilter(filter)), _streamFilters);
  }

  @Override
  public FilterChain addLastRest(RestFilter filter)
  {
    notNull(filter, "filter");
    return new FilterChainImpl(doAddLast(_restFilters, decorateRestFilter(filter)), _streamFilters);
  }

  @Override
  public FilterChain addFirst(StreamFilter filter)
  {
    notNull(filter, "filter");
    return new FilterChainImpl(_restFilters, doAddFirst(_streamFilters, decorateStreamFilter(filter)));
  }

  @Override
  public FilterChain addLast(StreamFilter filter)
  {
    notNull(filter, "filter");
    return new FilterChainImpl(_restFilters, doAddLast(_streamFilters, decorateStreamFilter(filter)));
  }

  private RestFilter decorateRestFilter(RestFilter filter)
  {
    return new TimedRestFilter(filter);
  }

  private StreamFilter decorateStreamFilter(StreamFilter filter)
  {
    return new TimedStreamFilter(filter);
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs)
  {
    new FilterChainIterator.FilterChainRestIterator(_restFilters, 0)
            .onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs)
  {
    new FilterChainIterator.FilterChainRestIterator(_restFilters, _restFilters.size())
            .onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Exception ex, RequestContext requestContext,
                          Map<String, String> wireAttrs)
  {
    new FilterChainIterator.FilterChainRestIterator(_restFilters, _restFilters.size())
            .onError(ex, requestContext, wireAttrs);
  }

  @Override
  public void onStreamRequest(StreamRequest req,
                       RequestContext requestContext,
                       Map<String, String> wireAttrs)
  {
    new FilterChainIterator.FilterChainStreamIterator(_streamFilters, 0)
        .onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onStreamResponse(StreamResponse res,
                        RequestContext requestContext,
                        Map<String, String> wireAttrs)
  {
    new FilterChainIterator.FilterChainStreamIterator(_streamFilters, _streamFilters.size())
        .onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onStreamError(Exception ex,
                     RequestContext requestContext,
                     Map<String, String> wireAttrs)
  {
    new FilterChainIterator.FilterChainStreamIterator(_streamFilters, _streamFilters.size())
        .onError(ex, requestContext, wireAttrs);
  }

  private <T> List<T> doAddFirst(List<T> list, T obj)
  {
    final List<T> newFilters = new ArrayList<T>(list.size() + 1);
    newFilters.add(obj);
    newFilters.addAll(list);
    return newFilters;
  }

  private <T> List<T> doAddLast(List<T> list, T obj)
  {
    final List<T> newFilters = new ArrayList<T>(list.size() + 1);
    newFilters.addAll(list);
    newFilters.add(obj);
    return newFilters;
  }

  private static void notNull(Object obj, String name)
  {
    if (obj == null)
    {
      throw new IllegalArgumentException(name + " is null");
    }
  }
}
