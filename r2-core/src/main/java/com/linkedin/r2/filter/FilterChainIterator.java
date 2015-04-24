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

import com.linkedin.r2.filter.message.MessageFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;

import java.util.List;
import java.util.Map;

/**
* @author Chris Pettitt
* @version $Revision$
*/
/* package private */ final class FilterChainIterator<REQ extends Request, RES extends Response>
        implements NextFilter<REQ, RES>
{
  private final List<MessageFilter> _filters;
  private int _cursor;

  public FilterChainIterator(List<MessageFilter> filters, int cursor)
  {
    _filters = filters;
    _cursor = cursor;
  }

  public void onRequest(REQ req, RequestContext requestContext, Map<String, String> wireAttrs)
  {
    if (_cursor < _filters.size())
    {
      try
      {
        _filters.get(_cursor++).onRequest(req, requestContext, wireAttrs, adaptNextFilter(this));
      }
      catch (RuntimeException e)
      {
        onError(e, requestContext, wireAttrs);
      }
    }
  }

  public void onResponse(RES res, RequestContext requestContext, Map<String, String> wireAttrs)
  {
    if (_cursor > 0)
    {
      try
      {
        _filters.get(--_cursor).onResponse(res, requestContext, wireAttrs, adaptNextFilter(this));
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
        _filters.get(--_cursor).onError(ex, requestContext, wireAttrs, adaptNextFilter(this));
      }
      catch (RuntimeException e)
      {
        onError(e, requestContext, wireAttrs);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private NextFilter<Request, Response> adaptNextFilter(FilterChainIterator<?, ?> nextFilter)
  {
    return (NextFilter<Request, Response>)nextFilter;
  }
}
