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
import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

import io.netty.handler.codec.http2.Http2Exception;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class HttpBridge
{
  public static final String NETTY_MAX_ACTIVE_STREAM_ERROR_MESSAGE =
      "Maximum active streams violated for this endpoint";

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
    final String uri = getDisplayedURI(request.getURI());
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

  /**
   * Wrap application callback for incoming StreamResponse with a "generic" HTTP callback.
   * If callback returns the error which is in Netty Http2Exception.StreamException type,
   * populate RetriableRequestException instead of RemoteInvocationException.
   *
   * @param callback the callback to receive the incoming RestResponse
   * @param request the request, used only to provide useful context in case an error
   *          occurs
   * @return the callback to receive the incoming HTTP response
   */
  public static TransportCallback<StreamResponse> streamToHttpCallback(final TransportCallback<StreamResponse> callback,
                                                        Request request)
  {
    final String uri = getDisplayedURI(request.getURI());
    return new TransportCallback<StreamResponse>()
    {
      @Override
      public void onResponse(TransportResponse<StreamResponse> response)
      {
        if (response.hasError())
        {
          Throwable responseError = response.getError();
          // If the error is due to the netty max active stream error, wrap it with RetriableRequestException instead
          RemoteInvocationException exception =
              wrapResponseError("Failed to get response from server for URI " + uri, responseError);
          response =
              TransportResponseImpl.error(exception, response.getWireAttributes());
        }
        else if (!RestStatus.isOK(response.getResponse().getStatus()))
        {
          response =
              TransportResponseImpl.error(new StreamException(response.getResponse(),
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

  public static StreamRequest toStreamRequest(StreamRequest request, Map<String, String> headers)
  {
    return request.builder()
        .unsafeSetHeaders(headers)
        .build(request.getEntityStream());
  }

  /**
   * Wrap transport callback for outgoing "generic" http response with a callback to pass
   * to the application REST server.
   *
   * @param callback the callback to receive the outgoing HTTP response
   * @return the callback to receive the outgoing REST response
   */
  public static TransportCallback<StreamResponse> httpToStreamCallback(final TransportCallback<StreamResponse> callback)
  {
    return new TransportCallback<StreamResponse>()
    {
      @Override
      public void onResponse(TransportResponse<StreamResponse> response)
      {
        if (response.hasError())
        {
          final Throwable ex = response.getError();
          if (ex instanceof StreamException)
          {
            callback.onResponse(TransportResponseImpl.success(((StreamException) ex).getResponse(),
                                                              response.getWireAttributes()));
            return;
          }
        }

        callback.onResponse(response);
      }
    };
  }

  /**
   * Check if the error is due to the netty max active stream error.
   * @param responseError Throwable error to check
   * @return True if the error is due to the netty max active stream error, false otherwise
   */
  private static boolean shouldReturnRetriableRequestException(Throwable responseError)
  {
    return responseError instanceof Http2Exception.StreamException
        && responseError.getMessage().contains(NETTY_MAX_ACTIVE_STREAM_ERROR_MESSAGE);
  }

  /**
   * Wrap the response error with the appropriate exception type.
   * If the error is due to the netty max active stream, wrap it with RetriableRequestException.
   * @param errorMessage Error message to wrap
   * @param responseError Throwable error to wrap
   * @return RemoteInvocationException or RetriableRequestException
   */
  private static RemoteInvocationException wrapResponseError(String errorMessage, Throwable responseError) {
    if (shouldReturnRetriableRequestException(responseError))
    {
      return new RetriableRequestException(errorMessage, responseError);
    }
    else
    {
      return new RemoteInvocationException(errorMessage, responseError);
    }
  }

  /**
   * Gets the URI to display in exception messages. The query parameters part of the URI is omitted to prevent
   * displaying sensitive information.
   *
   * @param uri Original URI to extract formatted displayed value
   * @return URI value to display
   */
  private static String getDisplayedURI(URI uri)
  {
    try
    {
      return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, uri.getFragment()).toString();
    }
    catch (URISyntaxException e)
    {
      return "Unknown URI";
    }
  }
}
