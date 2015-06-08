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
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class CaptureLastCallFilter implements RestFilter
{
  private volatile RestRequest _lastReq;
  private volatile RestResponse _lastRes;
  private volatile Throwable _lastErr;

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
                        NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _lastReq = req;
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
                         NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _lastRes = res;
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
                      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _lastErr = ex;
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  public RestRequest getLastReq()
  {
    return _lastReq;
  }

  public RestResponse getLastRes()
  {
    return _lastRes;
  }

  public Throwable getLastErr()
  {
    return _lastErr;
  }
}
