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
package com.linkedin.r2unittest.filter;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcRequestBuilder;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.message.rpc.RpcResponseBuilder;

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

  public static void fireSimpleRpcRequest(FilterChain fc)
  {
    fireRpcRequest(fc, simpleRpcRequest());
  }

  @SuppressWarnings("deprecation")
  public static void fireSimpleRpcResponse(FilterChain fc)
  {
    fc.onRpcResponse(simpleRpcResponse(), emptyRequestContext(), emptyWireAttrs());
  }

  @SuppressWarnings("deprecation")
  public static void fireSimpleRpcError(FilterChain fc)
  {
    fc.onRpcError(simpleError(), emptyRequestContext(), emptyWireAttrs());
  }

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

  @SuppressWarnings("deprecation")
  public static void fireRpcRequest(FilterChain fc, RpcRequest req)
  {
    fc.onRpcRequest(req, emptyRequestContext(), emptyWireAttrs());
  }

  public static void fireRestRequest(FilterChain fc, RestRequest req)
  {
    fc.onRestRequest(req, emptyRequestContext(), emptyWireAttrs());
  }

  public static void fireUntypedRequest(FilterChain fc, Request req)
  {
    if (req instanceof RpcRequest)
    {
      fireRpcRequest(fc, (RpcRequest)req);
    }
    else if (req instanceof RestRequest)
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
  @SuppressWarnings("deprecation")
  public static void fireRpcRequestResponse(FilterChain fc, RpcRequest req, RpcResponse res)
  {
    final RequestContext context = new RequestContext();
    fc.onRpcRequest(req, context, emptyWireAttrs());
    fc.onRpcResponse(res, context, emptyWireAttrs());
  }

  // Fires a request, saving the local attributes, and then fires a response with the local
  // attributes.
  @SuppressWarnings("deprecation")
  public static void fireRestRequestResponse(FilterChain fc, RestRequest req, RestResponse res)
  {
    final RequestContext context = new RequestContext();
    fc.onRestRequest(req, context, emptyWireAttrs());
    fc.onRestResponse(res, context, emptyWireAttrs());
  }

  // Determines the type of the request at runtime.
  public static void fireUntypedRequestResponse(FilterChain fc, Request req, Response res)
  {
    if (req instanceof RpcRequest)
    {
      fireRpcRequestResponse(fc, (RpcRequest)req, (RpcResponse)res);
    }
    else if (req instanceof RestRequest)
    {
      fireRestRequestResponse(fc, (RestRequest)req, (RestResponse)res);
    }
    else
    {
      throw new IllegalArgumentException("Unexpected request type: " + req.getClass());
    }
  }

  @SuppressWarnings("deprecation")
  public static void fireRpcRequestError(FilterChain fc, RpcRequest req, Exception ex)
  {
    final RequestContext context = new RequestContext();
    fc.onRpcRequest(req, context, emptyWireAttrs());
    fc.onRpcError(ex, context, emptyWireAttrs());
  }

  @SuppressWarnings("deprecation")
  public static void fireRestRequestError(FilterChain fc, RestRequest req, Exception ex)
  {
    final RequestContext context = new RequestContext();
    fc.onRestRequest(req, context, emptyWireAttrs());
    fc.onRestError(ex, context, emptyWireAttrs());
  }

  public static void fireUntypedRequestError(FilterChain fc, Request req, Exception ex)
  {
    if (req instanceof RpcRequest)
    {
      fireRpcRequestError(fc, (RpcRequest)req, ex);
    }
    else if (req instanceof RestRequest)
    {
      fireRestRequestError(fc, (RestRequest)req, ex);
    }
    else
    {
      throw new IllegalArgumentException("Unexpected request type: " + req.getClass());
    }
  }

  @SuppressWarnings("deprecation")
  public static RpcRequest simpleRpcRequest()
  {
    return new RpcRequestBuilder(URI.create("simple_uri"))
            .build();
  }

  @SuppressWarnings("deprecation")
  public static RpcResponse simpleRpcResponse()
  {
    return new RpcResponseBuilder()
            .build();
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
