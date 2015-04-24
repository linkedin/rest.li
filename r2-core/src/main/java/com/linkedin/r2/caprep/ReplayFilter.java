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


import com.linkedin.r2.caprep.db.DbSource;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestRequestFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter which replays response messages when respective request messages are observed.
 * Message correlation and lookup is delegated to a {@link DbSource} instance.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class ReplayFilter implements RestRequestFilter
{
  private static final Logger _log = LoggerFactory.getLogger(ReplayFilter.class);

  private final DbSource _db;

  /**
   * Construct a new instance with the specified DbSource.
   *
   * @param db DbSource to be used as a source of response messages for replay.
   */
  public ReplayFilter(DbSource db)
  {
    _db = db;
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    if (!replayResponse(req, requestContext, nextFilter))
    {
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }
  }

  private <RES extends Response> boolean replayResponse(Request req, RequestContext requestContext,
                              NextFilter<? extends Request, RES> nextFilter)
  {
    @SuppressWarnings("unchecked")
    final RES res = (RES)_db.replay(req);
    if (res != null)
    {
      _log.debug("Using cached response for request: " + req.getURI());

      // We create an empty map instead of Collections.emptyMap, because upstream filters may
      // try to modify the map.
      final Map<String, String> wireAttrs = new HashMap<String, String>();

      // For symmetry with CaptureFilter - if the REST response is "not OK" then we treat it as an
      // exception.
      if (res instanceof RestResponse && !RestStatus.isOK(((RestResponse)res).getStatus()))
      {
        nextFilter.onError(new RestException((RestResponse)res), requestContext, wireAttrs);
      }
      else
      {
        nextFilter.onResponse(res, requestContext, wireAttrs);
      }
      return true;
    }

    return false;
  }
}
