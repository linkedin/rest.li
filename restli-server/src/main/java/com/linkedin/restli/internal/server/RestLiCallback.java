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

package com.linkedin.restli.internal.server;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RestLiCallback<T> implements Callback<T>
{
  private final RoutingResult          _method;
  private final RestLiResponseHandler  _responseHandler;
  private final Callback<RestResponse> _callback;
  private final RestRequest            _request;

  public RestLiCallback(final RestRequest request,
                        final RoutingResult method,
                        final RestLiResponseHandler responseHandler,
                        final Callback<RestResponse> callback)
  {
    _request = request;
    _method = method;
    _responseHandler = responseHandler;
    _callback = callback;
  }

  @Override
  public void onSuccess(final T result)
  {
    try
    {
      RestResponse response = _responseHandler.buildResponse(_request, _method, result);
      _callback.onSuccess(response);
    }
    catch (Exception e)
    {
      // safe to assume it is a post processing error.
      onErrorPost(e);
    }
  }

  @Override
  public void onError(final Throwable e)
  {
    // "generic" error (atm looks the same as an app error)
    Map<String, String> headers = Collections.emptyMap();
    onErrorWithHeaders(e, headers);
  }

  public void onErrorPre(final Throwable e)
  {
    Map<String, String> headers =
        Collections.singletonMap(HeaderUtil.getErrorResponseHeaderName(_request.getHeaders()),
                                 RestConstants.HEADER_VALUE_ERROR_PREPROCESSING);
    onErrorWithHeaders(e, headers);
  }

  public void onErrorApp(final Throwable e)
  {
    Map<String, String> headers =
        Collections.singletonMap(HeaderUtil.getErrorResponseHeaderName(_request.getHeaders()),
                                 RestConstants.HEADER_VALUE_ERROR_APPLICATION);
    onErrorWithHeaders(e, headers);
  }

  public void onErrorPost(final Throwable e)
  {
    Map<String, String> headers =
        Collections.singletonMap(HeaderUtil.getErrorResponseHeaderName(_request.getHeaders()),
                                 RestConstants.HEADER_VALUE_ERROR_POSTPROCESSING);
    onErrorWithHeaders(e, headers);
  }

  private void onErrorWithHeaders(final Throwable e,
                                  final Map<String, String> givenHeaders)
  {
    RestLiServiceException restLiServiceException;
    Map<String, String> headers = new HashMap<String, String>();

    if (e instanceof RestException)
    {
      // assuming we don't need to do anything...
      _callback.onError(e);
      return;
    }
    else if (e instanceof RestLiServiceException)
    {
      restLiServiceException = (RestLiServiceException) e;
    }
    else if (e instanceof RoutingException)
    {
      RoutingException routingException = (RoutingException) e;

      restLiServiceException =
          new RestLiServiceException(HttpStatus.fromCode(routingException.getStatus()),
                                     routingException.getMessage(),
                                     routingException);

    }
    else
    {
      restLiServiceException =
          new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                     e.getMessage(),
                                     e);
    }

    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, _request.getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION));
    PartialRestResponse partialResponse =
        _responseHandler.buildErrorResponse(null, null, restLiServiceException, headers);

    headers.putAll(givenHeaders); // should overwrite app error with correct error

    RestResponseBuilder builder = new RestResponseBuilder().setHeaders(headers)
            .setStatus(partialResponse.getStatus().getCode());

    if (partialResponse.hasData())
    {
      DataMap dataMap = partialResponse.getDataMap();

      ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
      DataMapUtils.write(dataMap, null, baos, true); //partialResponse.getSchema()
      builder.setEntity(baos.toByteArray());
    }

    RestResponse restResponse = builder.build();

    // TODO: pass message?  I'm not sure what's happened to it after all this.
    RestException restException = new RestException(restResponse, e);

    _callback.onError(restException);

  }

}
