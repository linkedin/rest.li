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
package test.r2.integ.helper;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class CaptureWireAttributesFilter implements RestFilter, StreamFilter
{
  private volatile Map<String, String> _request;
  private volatile Map<String, String> _response;
  private volatile RequestContext _requestContext;

  public Map<String, String> getResponse()
  {
    return _response;
  }

  public Map<String, String> getRequest()
  {
    return _request;
  }

  public RequestContext getRequestContext()
  {
    return  _requestContext;
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
                        NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _request = new HashMap<String, String>(wireAttrs);
    _requestContext = requestContext;
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
                         NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _response = new HashMap<String, String>(wireAttrs);
    _requestContext = requestContext;
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
                      NextFilter<RestRequest, RestResponse> nextFilter)
  {
    _requestContext = requestContext;
    _response = new HashMap<String, String>(wireAttrs);
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  @Override
  public void onStreamRequest(StreamRequest req,
                              RequestContext requestContext,
                              Map<String, String> wireAttrs,
                              NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    _requestContext = requestContext;
    _request = new HashMap<String, String>(wireAttrs);
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onStreamResponse(StreamResponse res,
                               RequestContext requestContext,
                               Map<String, String> wireAttrs,
                               NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    _requestContext = requestContext;
    _response = new HashMap<String, String>(wireAttrs);
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onStreamError(Throwable ex,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    _requestContext = requestContext;
    _response = new HashMap<String, String>(wireAttrs);
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

}
