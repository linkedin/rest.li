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
package com.linkedin.r2.caprep;


import com.linkedin.r2.caprep.db.DbSink;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.RequestFilter;
import com.linkedin.r2.filter.message.rest.RestResponseFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class CaptureFilter implements RequestFilter, RestResponseFilter
{
  private static final Logger _log = LoggerFactory.getLogger(CaptureFilter.class);

  private static final String REQ_ATTR = CaptureFilter.class.getName() + ".req";

  private final DbSink _db;

  /**
   * Construct a new instance with the specified DbSink.
   *
   * @param db DbSink to be used as the target for this filter.
   */
  public CaptureFilter(DbSink db)
  {
    _db = db;
  }

  @Override
  public void onRequest(Request req, RequestContext requestContext, Map<String, String> wireAttrs,
                        NextFilter<Request, Response> nextFilter)
  {
    // Save request so that it can be associated with the response
    requestContext.putLocalAttr(REQ_ATTR, req);

    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    saveResponse(res, requestContext);
    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<RestRequest, RestResponse> nextFilter)
  {
    if (ex instanceof RestException)
    {
      saveResponse(((RestException)ex).getResponse(), requestContext);
    }

    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  private void saveResponse(Response res, RequestContext requestContext)
  {
    final Request req = (Request) requestContext.removeLocalAttr(REQ_ATTR);
    if (req != null)
    {
      _log.debug("Saving response for request: " + req.getURI());
      try
      {
        _db.record(req, res);
      }
      catch (IOException e)
      {
        _log.debug("Failed to save request", e);
      }
    }
  }
}
