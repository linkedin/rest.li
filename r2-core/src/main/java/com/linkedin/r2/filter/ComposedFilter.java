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

import java.util.Map;

import com.linkedin.r2.filter.message.MessageFilter;
import com.linkedin.r2.filter.message.RequestFilter;
import com.linkedin.r2.filter.message.ResponseFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;

/**
 * A filter that composes independent request and response filters into a single message
 * filter.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
/* package private */class ComposedFilter implements MessageFilter
{
  private final RequestFilter  _reqFilter;
  private final ResponseFilter _resFilter;

  public ComposedFilter(RequestFilter reqFilter, ResponseFilter resFilter)
  {
    _reqFilter = reqFilter;
    _resFilter = resFilter;
  }

  @Override
  public void onRequest(Request req,
                        RequestContext requestContext,
                        Map<String, String> wireAttrs,
                        NextFilter<Request, Response> nextFilter)
  {
    if (_reqFilter != null)
    {
      _reqFilter.onRequest(req, requestContext, wireAttrs, nextFilter);
    }
    else
    {
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }
  }

  @Override
  public void onResponse(Response res,
                         RequestContext requestContext,
                         Map<String, String> wireAttrs,
                         NextFilter<Request, Response> nextFilter)
  {
    if (_resFilter != null)
    {
      _resFilter.onResponse(res, requestContext, wireAttrs, nextFilter);
    }
    else
    {
      nextFilter.onResponse(res, requestContext, wireAttrs);
    }
  }

  @Override
  public void onError(Throwable ex,
                      RequestContext requestContext,
                      Map<String, String> wireAttrs,
                      NextFilter<Request, Response> nextFilter)
  {
    if (_resFilter != null)
    {
      _resFilter.onError(ex, requestContext, wireAttrs, nextFilter);
    }
    else
    {
      nextFilter.onError(ex, requestContext, wireAttrs);
    }
  }
}
