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
// This class subclasses MessageCountFilter. Because this class implements RpcFilter and RestFilter
// the base class methods (onRequest, onResponse, onError) should not be invoked.

public class RpcRestCountFilter extends MessageCountFilter implements RestFilter
{
  private int _rpcReqCount;
  private int _rpcResCount;
  private int _rpcErrCount;

  private int _restReqCount;
  private int _restResCount;
  private int _restErrCount;

  public int getRpcReqCount()
  {
    return _rpcReqCount;
  }

  public int getRpcResCount()
  {
    return _rpcResCount;
  }

  public int getRpcErrCount()
  {
    return _rpcErrCount;
  }

  public int getRestReqCount()
  {
    return _restReqCount;
  }

  public int getRestResCount()
  {
    return _restResCount;
  }

  public int getRestErrCount()
  {
    return _restErrCount;
  }

  @Override
  public void reset()
  {
    super.reset();
    _rpcReqCount = _rpcResCount = _rpcErrCount = 0;
    _restReqCount = _restResCount = _restErrCount = 0;
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _restReqCount++;
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _restResCount++;
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _restErrCount++;
    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
