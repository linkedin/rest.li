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
package com.linkedin.r2.transport.common.bridge.client;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.AbstractClient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TransportClientAdapter extends AbstractClient
{
  private final TransportClient _client;
  private final boolean _restOverStream;

  /**
   * Construct a new instance which delegates to the specified {@link TransportClient}.
   *
   * @param client {@link TransportClient} to delegate calls.
   */
  public TransportClientAdapter(TransportClient client)
  {
    this(client, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  /**
   * Construct a new instance which delegates to the specified {@link TransportClient}.
   *
   * @param client {@link TransportClient} to delegate calls.
   * @param restOverStream whether to run in rest-over-stream model
   */
  public TransportClientAdapter(TransportClient client, boolean restOverStream)
  {
    _client = client;
    _restOverStream = restOverStream;
  }

  @Override
  public void streamRequest(StreamRequest request,
                          RequestContext requestContext,
                          Callback<StreamResponse> callback)
  {
    final Map<String, String> wireAttrs = new HashMap<String, String>();
    //make a copy of the caller's RequestContext to ensure that we have a unique instance per-request
    _client.streamRequest(request, new RequestContext(requestContext), wireAttrs, new TransportCallbackAdapter<StreamResponse>(callback));
  }

  @Override
  public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    if (!_restOverStream)
    {
      final Map<String, String> wireAttrs = new HashMap<String, String>();
      //make a copy of the caller's RequestContext to ensure that we have a unique instance per-request
      _client.restRequest(request, new RequestContext(requestContext), wireAttrs, new TransportCallbackAdapter<RestResponse>(callback));
    }
    else
    {
      super.restRequest(request, requestContext, callback);
    }
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _client.shutdown(callback);
  }
}
