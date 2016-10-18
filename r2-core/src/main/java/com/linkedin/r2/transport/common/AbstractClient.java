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
package com.linkedin.r2.transport.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * AbstractClient simplifies implementation of the {@link Client} interface, by providing
 * implementations of overloaded convenience methods implemented in terms of the most general
 * versions. Classes extending AbstractClient should implement:
 *
 *   void streamRequest(StreamRequest request,
 *                      RequestContext requestContext,
 *                      Callback<StreamResponse> callback);
 *
 * The default behavior is to support restRequest using streamRequest. If that's not the desired behavior,
 * the subclasses should override:
 *  void restRequest(RestRequest request,
 *                   RequestContext requestContext,
 *                   Callback<RestResponse> callback);
 *
 *
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 * @version $Revision$
 */
public abstract class AbstractClient implements Client
{

  @Override
  public Future<RestResponse> restRequest(RestRequest request)
  {
    return restRequest(request, new RequestContext());
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext)
  {
    final FutureCallback<RestResponse> future = new FutureCallback<RestResponse>();
    restRequest(request, requestContext, future);
    return future;
  }

  @Override
  public void restRequest(RestRequest request, Callback<RestResponse> callback)
  {
    restRequest(request, new RequestContext(), callback);
  }

  @Override
  public void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
  {
    streamRequest(request, new RequestContext(), callback);
  }

  @Override
  public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    StreamRequest streamRequest = Messages.toStreamRequest(request);
    // IS_FULL_REQUEST flag, if set true, would result in the request being sent without using chunked transfer encoding
    // This is needed as the legacy R2 server (before 2.8.0) does not support chunked transfer encoding.
    requestContext.putLocalAttr(R2Constants.IS_FULL_REQUEST, true);
    // here we add back the content-length header for the response because some client code depends on this header
    streamRequest(streamRequest, requestContext, Messages.toStreamCallback(callback, true));
  }

  @Override
  public Map<String, Object> getMetadata(URI uri)
  {
    return Collections.emptyMap();
  }
}
