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

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReport;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.ResponseFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      // Convert the result to a partial rest response.
      PartialRestResponse response = _responseHandler.buildPartialResponse(_request, _method, result);
      Exception exceptionFromFilters = null;
      // Invoke the response filters.
      if (_responseFilters != null && !_responseFilters.isEmpty())
      {
        // Construct the filter response context from the partial response.
        FilterResponseContext responseContext = new FilterResponseContextAdapter(response);
        // Now invoke ResponseFilters.
        try
        {
          invokeResponseFilters(responseContext);
        }
        catch (Exception e)
        {
          // Save the exception thrown from the filter.
          exceptionFromFilters = e;
        }
        // Build the updated partial response with data that was returned from
        // invoking all the filters.
        response =
            new PartialRestResponse.Builder().entity(responseContext.getResponseEntity())
                                             .status(responseContext.getHttpStatus())
                                             .headers(responseContext.getResponseHeaders())
                                             .build();
        if (exceptionFromFilters != null)
        {
          // Convert the caught exception to a rest exception and invoke the callback.
          _callback.onError(_responseHandler.buildRestException(exceptionFromFilters, response), executionReport);
          return;
        }
      }
      // Invoke the callback.
      _callback.onSuccess(_responseHandler.buildResponse(_method, response), executionReport);
    }
    catch (Exception e)
    {
      // Convert the caught exception to a rest exception and invoke the callback.
      _callback.onError(toRestException(e), executionReport);
    }
  }

  @Override
  public void onError(final Throwable e, RequestExecutionReport executionReport)
  {
    if (e instanceof RestException)
    {
      // assuming we don't need to do anything...
      // NOTE: If we receive a rest exception, the exception is handed off to the underlying
      // callback without invoking any of the response filters!
      _callback.onError(e, executionReport);
      return;
    }
    Throwable exception = e;
    PartialRestResponse partialResponse = convertExceptionToPartialResponse(exception);
    // Invoke the response filters.
    if (_responseFilters != null && !_responseFilters.isEmpty())
    {
      // Construct the filter response context from the partial response.
      FilterResponseContext responseContext = new FilterResponseContextAdapter(partialResponse);
      try
      {
        // Invoke response filters.
        invokeResponseFilters(responseContext);
      }
      catch (RuntimeException ex)
      {
        // Update the exception that we are processing to the one thrown by the filter.
        exception = ex;
      }
      // Build the updated partial response with data that was returned from
      // invoking all the filters.
      partialResponse =
          new PartialRestResponse.Builder().entity(responseContext.getResponseEntity())
                                           .status(responseContext.getHttpStatus())
                                           .headers(responseContext.getResponseHeaders())
                                           .build();
    }
    // Invoke the callback.
    _callback.onError(_responseHandler.buildRestException(exception, partialResponse), executionReport);
  }

  private RestException toRestException(final Throwable e)
  {
    return _responseHandler.buildRestException(e, convertExceptionToPartialResponse(e));
  }

  private PartialRestResponse convertExceptionToPartialResponse(Throwable e)
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
    Map<String, String> headers = new HashMap<String, String>();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
                ProtocolVersionUtil.extractProtocolVersion(_request.getHeaders()).toString());
    headers.put(HeaderUtil.getErrorResponseHeaderName(_request.getHeaders()), RestConstants.HEADER_VALUE_ERROR);
    return _responseHandler.buildErrorResponse(null, null, restLiServiceException, headers);
  }

  private void invokeResponseFilters(final FilterResponseContext responseContext)
  {
    // Reference to the last exception thrown from the filter chain.
    RuntimeException lastException = null;
    for (ResponseFilter filter : _responseFilters)
    {
      try
      {
        filter.onResponse(_filterRequestContext, responseContext);
        // This filter has successfully handled the exception that was thrown earlier by the
        // previous filter, if any. Therefore, clear the last exception.
        lastException = null;
      }
      catch (RuntimeException ex)
      {
        // Save the latest exception that's thrown from the filter.
        lastException = ex;
        PartialRestResponse partial = convertExceptionToPartialResponse(ex);
        // Update the response context with info regarding the exception that we just caught.
        responseContext.setHttpStatus(partial.getStatus());
        responseContext.setResponseEntity(partial.getEntity());
        if (partial.getHeaders() != null)
        {
          responseContext.getResponseHeaders().clear();
          responseContext.getResponseHeaders().putAll(partial.getHeaders());
        }
      }
    }
    // If an exception was thrown by the last filter in the filter chain, rethrow it.
    if (lastException != null)
    {
      throw lastException;
    }
  }

  /* Package private for testing purposes */
  static class FilterResponseContextAdapter implements FilterResponseContext
  {
    private RecordTemplate _entity;
    private HttpStatus _status;
    private final Map<String, String> _headers;

    public FilterResponseContextAdapter(final PartialRestResponse response)
    {
      _entity = response.getEntity();
      _status = response.getStatus();
      _headers = new HashMap<String, String>();
      if (response.getHeaders() != null)
      {
        _headers.putAll(response.getHeaders());
      }
    }

    @Override
    public void setResponseEntity(RecordTemplate entity)
    {
      _entity = entity;
    }

    @Override
    public void setHttpStatus(HttpStatus status)
    {
      _status = status;
    }

    @Override
    public Map<String, String> getResponseHeaders()
    {
      return _headers;
    }

    @Override
    public RecordTemplate getResponseEntity()
    {
      return _entity;
    }

    @Override
    public HttpStatus getHttpStatus()
    {
      return _status;
    }
  }
}
