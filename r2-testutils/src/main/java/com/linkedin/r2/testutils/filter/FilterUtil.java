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

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class FilterUtil
{
  private FilterUtil() {}

  public static void fireSimpleRestRequest(FilterChain fc)
  {
    fireRestRequest(fc, simpleRestRequest());
  }

  public static void fireSimpleRestResponse(FilterChain fc)
  {
    fc.onRestResponse(simpleRestResponse(), emptyRequestContext(), emptyWireAttrs());
  }

  public static void fireSimpleRestError(FilterChain fc)
  {
    fc.onRestError(simpleError(), emptyRequestContext(), emptyWireAttrs());
  }

  public static void fireRestRequest(FilterChain fc, RestRequest req)
  {
    fc.onRestRequest(req, emptyRequestContext(), emptyWireAttrs());
  }

  public static void fireRestRequest(FilterChain fc, RestRequest req, Map<String, String> wireAttrs)
  {
    fc.onRestRequest(req, emptyRequestContext(), wireAttrs);
  }

  public static void fireUntypedRequest(FilterChain fc, Request req)
  {
    if (req instanceof RestRequest)
    {
      fireRestRequest(fc, (RestRequest)req);
    }
    else
    {
      throw new IllegalArgumentException("Unexpected request type: " + req.getClass());
    }
  }

  // Fires a request, saving the local attributes, and then fires a response with the local
  // attributes.
  public static void fireRestRequestResponse(FilterChain fc, RestRequest req, RestResponse res)
  {
    final RequestContext context = new RequestContext();
    fc.onRestRequest(req, context, emptyWireAttrs());
    fc.onRestResponse(res, context, emptyWireAttrs());
  }

  // Determines the type of the request at runtime.
  public static void fireUntypedRequestResponse(FilterChain fc, Request req, Response res)
  {
    if (req instanceof RestRequest)
    {
      fireRestRequestResponse(fc, (RestRequest)req, (RestResponse)res);
    }
    else
    {
      throw new IllegalArgumentException("Unexpected request type: " + req.getClass());
    }
  }

  public static void fireRestRequestError(FilterChain fc, RestRequest req, Exception ex)
  {
    final RequestContext context = new RequestContext();
    fc.onRestRequest(req, context, emptyWireAttrs());
    fc.onRestError(ex, context, emptyWireAttrs());
  }

  public static void fireUntypedRequestError(FilterChain fc, Request req, Exception ex)
  {
    if (req instanceof RestRequest)
    {
      fireRestRequestError(fc, (RestRequest)req, ex);
    }
    else
    {
      throw new IllegalArgumentException("Unexpected request type: " + req.getClass());
    }
  }

  public static RestRequest simpleRestRequest()
  {
    return new RestRequestBuilder(URI.create("simple_uri"))
            .build();
  }

  public static RestResponse simpleRestResponse()
  {
    return new RestResponseBuilder()
            .build();
  }

  public static Exception simpleError()
  {
    return new Exception("test generated error");
  }

  public static Map<String, String> emptyWireAttrs()
  {
    return new HashMap<String, String>();
  }

  public static RequestContext emptyRequestContext()
  {
    return new RequestContext();
  }
}
