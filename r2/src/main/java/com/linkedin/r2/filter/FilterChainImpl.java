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


import com.linkedin.r2.filter.message.MessageFilter;
import com.linkedin.r2.filter.message.RequestFilter;
import com.linkedin.r2.filter.message.ResponseFilter;
import com.linkedin.r2.filter.message.rest.RestRequestFilter;
import com.linkedin.r2.filter.message.rest.RestResponseFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Chris Pettitt
 */
/* package private */ class FilterChainImpl implements FilterChain
{
  private final List<MessageFilter> _rpcFilters;
  private final List<MessageFilter> _restFilters;

  public FilterChainImpl()
  {
    _rpcFilters = Collections.emptyList();
    _restFilters = Collections.emptyList();
  }

  private FilterChainImpl(List<MessageFilter> rpcFilters, List<MessageFilter> restFilters)
  {
    _rpcFilters = Collections.unmodifiableList(new ArrayList<MessageFilter>(rpcFilters));
    _restFilters = Collections.unmodifiableList(new ArrayList<MessageFilter>(restFilters));
  }

  @Override
  public FilterChain addFirst(Filter filter)
  {
    return new FilterChainImpl(addFirstRpc(filter), addFirstRest(filter));
  }

  @Override
  public FilterChain addLast(Filter filter)
  {
    return new FilterChainImpl(addLastRpc(filter), addLastRest(filter));
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs)
  {
    new FilterChainIterator<RestRequest, RestResponse>(_restFilters, 0)
            .onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs)
  {
    new FilterChainIterator<RestRequest, RestResponse>(_restFilters, _restFilters.size())
            .onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Exception ex, RequestContext requestContext,
                          Map<String, String> wireAttrs)
  {
    new FilterChainIterator<RestRequest, RestResponse>(_restFilters, _restFilters.size())
            .onError(ex, requestContext, wireAttrs);
  }

  private List<MessageFilter> addFirstRpc(Filter filter)
  {
    return doAddFirst(_rpcFilters, adaptRpcFilter(filter));
  }

  private List<MessageFilter> addFirstRest(Filter filter)
  {
    return doAddFirst(_restFilters, adaptRestFilter(filter));
  }

  @Deprecated
  private List<MessageFilter> addLastRpc(Filter filter)
  {
    return doAddLast(_rpcFilters, adaptRpcFilter(filter));
  }

  private List<MessageFilter> addLastRest(Filter filter)
  {
    return doAddLast(_restFilters, adaptRestFilter(filter));
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

  private MessageFilter adaptRpcFilter(Filter filter)
  {
    final RequestFilter reqFilter;
    if (filter instanceof RequestFilter)
    {
      reqFilter = (RequestFilter) filter;
    }
    else
    {
      reqFilter = null;
    }

    final ResponseFilter resFilter;
    if (filter instanceof ResponseFilter)
    {
      resFilter = (ResponseFilter) filter;
    }
    else
    {
      resFilter = null;
    }

    return new ComposedFilter(reqFilter, resFilter);
  }

  private static MessageFilter adaptRestFilter(Filter filter)
  {
    final RequestFilter reqFilter;
    if (filter instanceof RestRequestFilter)
    {
      reqFilter = adaptRestRequestFilter((RestRequestFilter) filter);
    }
    else if (filter instanceof RequestFilter)
    {
      reqFilter = (RequestFilter) filter;
    }
    else
    {
      reqFilter = null;
    }

    final ResponseFilter resFilter;
    if (filter instanceof RestResponseFilter)
    {
      resFilter = adaptRestResponseFilter((RestResponseFilter) filter);
    }
    else if (filter instanceof ResponseFilter)
    {
      resFilter = (ResponseFilter) filter;
    }
    else
    {
      resFilter = null;
    }

    return new ComposedFilter(reqFilter, resFilter);
  }

  private static RequestFilter adaptRestRequestFilter(final RestRequestFilter restFilter)
  {
    return new RestRequestFilterAdapter(restFilter);
  }

  private static ResponseFilter adaptRestResponseFilter(final RestResponseFilter restFilter)
  {
    return new RestResponseFilterAdapter(restFilter);
  }

  @SuppressWarnings("unchecked")
  private static NextFilter<RestRequest, RestResponse> adaptRestNextFilter(NextFilter<?, ?> nextFilter)
  {
    return (NextFilter<RestRequest, RestResponse>)nextFilter;
  }

  private static final class RestRequestFilterAdapter implements RequestFilter
  {
    private final RestRequestFilter _restFilter;

    private RestRequestFilterAdapter(RestRequestFilter restFilter)
    {
      _restFilter = restFilter;
    }

    @Override
    public void onRequest(Request req,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<Request, Response> nextFilter)
    {
      _restFilter.onRestRequest((RestRequest) req,
                                requestContext,
                                wireAttrs,
                                adaptRestNextFilter(nextFilter));
    }
  }

  private static final class RestResponseFilterAdapter implements ResponseFilter
  {
    private final RestResponseFilter _restFilter;

    private RestResponseFilterAdapter(RestResponseFilter restFilter)
    {
      _restFilter = restFilter;
    }

    @Override
    public void onResponse(Response res, RequestContext requestContext,
                           Map<String, String> wireAttrs,
                           NextFilter<Request, Response> nextFilter)
    {
      _restFilter.onRestResponse((RestResponse) res,
                                 requestContext,
                                 wireAttrs,
                                 adaptRestNextFilter(nextFilter));
    }

    @Override
    public void onError(Throwable ex,
                        RequestContext requestContext,
                        Map<String, String> wireAttrs,
                        NextFilter<Request, Response> nextFilter)
    {
      _restFilter.onRestError(ex,
                              requestContext,
                              wireAttrs,
                              adaptRestNextFilter(nextFilter));
    }
  }
}
