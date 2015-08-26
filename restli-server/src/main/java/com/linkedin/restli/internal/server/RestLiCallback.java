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


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.filter.FilterResponseContextInternal;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReport;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.ResponseFilter;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class RestLiCallback<T> implements RequestExecutionCallback<T>
{
  private final RoutingResult _method;
  private final RestLiResponseHandler _responseHandler;
  private final RequestExecutionCallback<RestResponse> _callback;
  private final RestRequest _request;
  private final List<ResponseFilter> _responseFilters;
  private final FilterRequestContext _filterRequestContext;

  public RestLiCallback(final RestRequest request,
                        final RoutingResult method,
                        final RestLiResponseHandler responseHandler,
                        final RequestExecutionCallback<RestResponse> callback,
                        final List<ResponseFilter> responseFilters,
                        final FilterRequestContext filterRequestContext)
  {
    _request = request;
    _method = method;
    _responseHandler = responseHandler;
    _callback = callback;
    if (responseFilters != null)
    {
      _responseFilters = responseFilters;
    }
    else
    {
      _responseFilters = new ArrayList<ResponseFilter>();
    }
    _filterRequestContext = filterRequestContext;
  }

  @Override
  public void onSuccess(final T result, RequestExecutionReport executionReport)
  {
    try
    {
      // Convert the object returned by the resource to response data.
      final RestLiResponseEnvelope responseData = _responseHandler.buildRestLiResponseData(_request, _method, result);
      // Invoke the response filters.
      if (!_responseFilters.isEmpty())
      {
        invokeFiltersAndProcessResults(executionReport, responseData);
      }
      else
      {
        // Convert response data to partial rest response.
        final PartialRestResponse response = _responseHandler.buildPartialResponse(_method, responseData);
        // Invoke the callback.
        _callback.onSuccess(_responseHandler.buildResponse(_method, response), executionReport);
      }
    }
    catch (Exception e)
    {
      // Convert the caught exception to a rest exception and invoke the callback.
      //This is so that exceptions returned early on by buildRestLiResponseData() due to null resource method
      //responses are still given the opportunity to have filters run on them.
      onError(e, executionReport);
    }
  }

  @Override
  public void onError(final Throwable e, RequestExecutionReport executionReport)
  {
    final RestLiResponseEnvelope responseData = convertExceptionToRestLiResponseData(e);
    // Invoke the response filters.
    if (!_responseFilters.isEmpty())
    {
      invokeFiltersAndProcessResults(executionReport, responseData);
    }
    else
    {
      // Invoke the callback with the exception obtained from the resource.
      _callback.onError(_responseHandler.buildRestException(e, _responseHandler.buildPartialResponse(_method,
                                                                                                     responseData)),
                        executionReport);
    }
  }

  private void invokeFiltersAndProcessResults(RequestExecutionReport executionReport,
                                              final RestLiResponseEnvelope responseData)
  {
    // Construct the filter response context from the partial response.
    final FilterResponseContextInternal responseContext = new FilterResponseContextAdapter(responseData);
    invokeResponseFilters(responseContext);
    final PartialRestResponse response = _responseHandler.buildPartialResponse(_method,
                                                                               responseContext.getRestLiResponseEnvelope());
    if (responseContext.getRestLiResponseEnvelope().isErrorResponse())
    {
      // If the updated response from the filter is an error response, then call onError on the underlying callback.
      Exception e = responseContext.getRestLiResponseEnvelope().getServiceException();
      // Invoke onError on the R2 callback since we received an exception from the filters.
      _callback.onError(_responseHandler.buildRestException(e, response), executionReport);
    }
    else
    {
      // Invoke onSuccess on the underlying callback.
      _callback.onSuccess(_responseHandler.buildResponse(_method, response), executionReport);
    }
  }

  /* Package private for testing purposes */
  RestLiResponseEnvelope convertExceptionToRestLiResponseData(Throwable e)
  {
    RestLiServiceException restLiServiceException;
    if (e instanceof RestLiServiceException)
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
      restLiServiceException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }

    Map<String, String> requestHeaders = _request.getHeaders();
    Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
                ProtocolVersionUtil.extractProtocolVersion(requestHeaders).toString());
    headers.put(HeaderUtil.getErrorResponseHeaderName(requestHeaders), RestConstants.HEADER_VALUE_ERROR);
    return _responseHandler.buildExceptionResponseData(_request, _method, restLiServiceException, headers, Collections.<HttpCookie>emptyList());
  }

  private void invokeResponseFilters(final FilterResponseContextInternal responseContext)
  {
    for (ResponseFilter filter : _responseFilters)
    {
      try
      {
        filter.onResponse(_filterRequestContext, responseContext);
      }
      catch (Throwable t)
      {
        responseContext.setRestLiResponseEnvelope(convertExceptionToRestLiResponseData(t));
      }
    }
  }

  /* Package private for testing purposes */
  static class FilterResponseContextAdapter implements FilterResponseContextInternal
  {
    private RestLiResponseEnvelope _responseData;
    public FilterResponseContextAdapter(final RestLiResponseEnvelope response)
    {
      _responseData = response;
    }

    @Override
    public RestLiResponseData getResponseData()
    {
      return _responseData;
    }

    @Override
    public void setRestLiResponseEnvelope(RestLiResponseEnvelope data)
    {
      _responseData = data;
    }

    @Override
    public RestLiResponseEnvelope getRestLiResponseEnvelope()
    {
      return _responseData;
    }
  }
}
