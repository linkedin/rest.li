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
package com.linkedin.r2.transport.http.common;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

import java.net.URI;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class HttpBridge
{
  /**
   * Wrap application callback for incoming RestResponse with a "generic" HTTP callback.
   *
   * @param callback the callback to receive the incoming RestResponse
   * @param request the request, used only to provide useful context in case an error
   *          occurs
   * @return the callback to receive the incoming HTTP response
   */
  public static TransportCallback<RestResponse> restToHttpCallback(final TransportCallback<RestResponse> callback,
                                                        RestRequest request)
  {
    final URI uri = request.getURI();
    return new TransportCallback<RestResponse>()
    {
      @Override
      public void onResponse(TransportResponse<RestResponse> response)
      {
        if (response.hasError())
        {
          response =
              TransportResponseImpl.error(new RemoteInvocationException("Failed to get response from server for URI "
                                                                            + uri,
                                                                        response.getError()),
                                          response.getWireAttributes());
        }
        else if (!RestStatus.isOK(response.getResponse().getStatus()))
        {
          response =
              TransportResponseImpl.error(new RestException(response.getResponse(),
                                                            "Received error "
                                                                + response.getResponse()
                                                                          .getStatus()
                                                                + " from server for URI "
                                                                + uri),
                                          response.getWireAttributes());
        }

        callback.onResponse(response);
      }
    };
  }

  /**
   * Combine the specified {@link RestRequest} and map of headers to construct a new
   * {@link RestRequest}.
   *
   * @param request the {@link RestRequest} to be used as a source object.
   * @param headers the headers to set on the request. These override any headers on the
   *          source request.
   * @return a new {@link RestRequest} which combines the specified request and headers.
   */
  public static RestRequest toRestRequest(RestRequest request, Map<String, String> headers)
  {
    return new RestRequestBuilder(request)
            .unsafeSetHeaders(headers)
            .build();
  }

  /**
   * Wrap transport callback for outgoing "generic" http response with a callback to pass
   * to the application REST server.
   *
   * @param callback the callback to receive the outgoing HTTP response
   * @return the callback to receive the outgoing REST response
   */
  public static TransportCallback<RestResponse> httpToRestCallback(final TransportCallback<RestResponse> callback)
  {
    return new TransportCallback<RestResponse>()
    {
      @Override
      public void onResponse(TransportResponse<RestResponse> response)
      {
        if (response.hasError())
        {
          final Throwable ex = response.getError();
          if (ex instanceof RestException)
          {
            callback.onResponse(TransportResponseImpl.success(((RestException) ex).getResponse(),
                                                              response.getWireAttributes()));
            return;
          }
        }

        callback.onResponse(response);
      }
    };
  }
}
