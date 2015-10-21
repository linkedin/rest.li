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
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.util.List;
import java.util.Map;

/**
* @author Chris Pettitt
* @author Zhenkai Zhu
* @version $Revision$
*/
/* package private */ abstract class FilterChainIterator<F, REQ extends Request, RES extends Response>
        implements NextFilter<REQ, RES>
{
  private final List<F> _filters;
  private int _cursor;

  protected FilterChainIterator(List<F> filters, int cursor)
  {
    _filters = filters;
    _cursor = cursor;
  }

  @Override
  public void onRequest(REQ req, RequestContext requestContext, Map<String, String> wireAttrs)
  {
    if (_cursor < _filters.size())
    {
      try
      {
        doOnRequest(_filters.get(_cursor++), req, requestContext, wireAttrs, this);
      }
      catch (RuntimeException e)
      {
        onError(e, requestContext, wireAttrs);
      }
    }
  }

  @Override
  public void onResponse(RES res, RequestContext requestContext, Map<String, String> wireAttrs)
  {
    if (_cursor > 0)
    {
      try
      {
        doOnResponse(_filters.get(--_cursor), res, requestContext, wireAttrs, this);
      }
      catch (RuntimeException e)
      {
        onError(e, requestContext, wireAttrs);
      }
    }
  }

  @Override
  public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
  {
    if (_cursor > 0)
    {
      try
      {
        doOnError(_filters.get(--_cursor), ex, requestContext, wireAttrs, this);
      }
      catch (RuntimeException e)
      {
        onError(e, requestContext, wireAttrs);
      }
    }
  }

  protected abstract void doOnRequest(F filter,
                                      REQ req,
                                      RequestContext requestContext,
                                      Map<String, String> wireAttrs,
                                      NextFilter<REQ, RES> nextFilter);

  protected abstract void doOnResponse(F filter,
                                       RES res,
                                       RequestContext requestContext,
                                       Map<String, String> wireAttrs,
                                       NextFilter<REQ, RES> nextFilter);

  protected abstract void doOnError(F filter,
                                    Throwable ex,
                                    RequestContext requestContext,
                                    Map<String, String> wireAttrs,
                                    NextFilter<REQ, RES> nextFilter);


  /* package private */static class FilterChainRestIterator extends FilterChainIterator<RestFilter, RestRequest, RestResponse>
  {
    public FilterChainRestIterator(List<RestFilter> filters, int cursor)
    {
      super(filters, cursor);
    }

    @Override
    protected void doOnRequest(RestFilter filter,
                               RestRequest req,
                               RequestContext requestContext,
                               Map<String, String> wireAttrs,
                               NextFilter<RestRequest, RestResponse> nextFilter)
    {
      filter.onRestRequest(req, requestContext, wireAttrs, nextFilter);
    }

    @Override
    protected void doOnResponse(RestFilter filter,
                                RestResponse res,
                                RequestContext requestContext,
                                Map<String, String> wireAttrs,
                                NextFilter<RestRequest, RestResponse> nextFilter)
    {
      filter.onRestResponse(res, requestContext, wireAttrs, nextFilter);
    }

    @Override
    protected void doOnError(RestFilter filter,
                             Throwable ex,
                             RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
    {
      filter.onRestError(ex, requestContext, wireAttrs, nextFilter);
    }
  }

  /*package private */static class FilterChainStreamIterator extends FilterChainIterator<StreamFilter, StreamRequest, StreamResponse>
  {
    public FilterChainStreamIterator(List<StreamFilter> filters, int cursor)
    {
      super(filters, cursor);
    }

    @Override
    protected void doOnRequest(StreamFilter filter,
                               StreamRequest req,
                               RequestContext requestContext,
                               Map<String, String> wireAttrs,
                               NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      filter.onStreamRequest(req, requestContext, wireAttrs, nextFilter);
    }

    @Override
    protected void doOnResponse(StreamFilter filter,
                                StreamResponse res,
                                RequestContext requestContext,
                                Map<String, String> wireAttrs,
                                NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      filter.onStreamResponse(res, requestContext, wireAttrs, nextFilter);
    }

    @Override
    protected void doOnError(StreamFilter filter,
                             Throwable ex,
                             RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      filter.onStreamError(ex, requestContext, wireAttrs, nextFilter);
    }
  }

}
