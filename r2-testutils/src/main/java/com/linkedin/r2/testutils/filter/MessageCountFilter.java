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
package com.linkedin.r2.testutils.filter;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.MessageFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;

import java.util.Map;

/**
* @author Chris Pettitt
* @version $Revision$
*/
public class MessageCountFilter implements MessageFilter
{
  private int _reqCount;
  private int _resCount;
  private int _errCount;

  public int getReqCount()
  {
    return _reqCount;
  }

  public int getResCount()
  {
    return _resCount;
  }

  public int getErrCount()
  {
    return _errCount;
  }

  public void reset()
  {
    _reqCount = _resCount = _errCount = 0;
  }

  @Override
  public void onRequest(Request req, RequestContext requestContext, Map<String, String> wireAttrs,
                        NextFilter<Request, Response> nextFilter)
  {
    _reqCount++;
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onResponse(Response res, RequestContext requestContext, Map<String, String> wireAttrs,
                         NextFilter<Request, Response> nextFilter)
  {
    _resCount++;
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
                      NextFilter<Request, Response> nextFilter)
  {
    _errCount++;
    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
