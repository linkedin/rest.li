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


import com.linkedin.r2.message.rest.RestException;
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
      // Convert the object returned by the resource to response data.
      AugmentedRestLiResponseData responseData = _responseHandler.buildRestLiResponseData(_request, _method, result);
      Exception exceptionFromFilters = null;
      // Invoke the response filters.
      if (_responseFilters != null && !_responseFilters.isEmpty())
      {
        // Construct the filter response context from the partial response.
        FilterResponseContextInternal responseContext = new FilterResponseContextAdapter(responseData);
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
        // Update the response data based on what's returned by the filters.
        responseData = responseContext.getAugmentedRestLiResponseData();
        if (exceptionFromFilters != null)
        {
          // Convert the caught exception to a rest exception and invoke the callback.
          RestException restEx =
              _responseHandler.buildRestException(exceptionFromFilters,
                                                  _responseHandler.buildPartialResponse(_method, responseData));
          _callback.onError(restEx, executionReport);
          return;
        }
      }
      // Convert response data to partial rest response.
      PartialRestResponse response = _responseHandler.buildPartialResponse(_method, responseData);
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
    AugmentedRestLiResponseData responseData = convertExceptionToRestLiResponseData(exception);
    // Invoke the response filters.
    if (_responseFilters != null && !_responseFilters.isEmpty())
    {
      // Construct the filter response context from the partial response.
      FilterResponseContextInternal responseContext = new FilterResponseContextAdapter(responseData);
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
      // Update the response data based on what's returned by the filters.
      responseData = responseContext.getAugmentedRestLiResponseData();
    }
    // Invoke the callback.
    _callback.onError(_responseHandler.buildRestException(exception,
                                                          _responseHandler.buildPartialResponse(_method, responseData)),
                      executionReport);
  }

  private RestException toRestException(final Throwable e)
  {
    PartialRestResponse partialResponse =
        _responseHandler.buildPartialResponse(_method, convertExceptionToRestLiResponseData(e));
    return _responseHandler.buildRestException(e, partialResponse);
  }

  private AugmentedRestLiResponseData convertExceptionToRestLiResponseData(Throwable e)
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
    return _responseHandler.buildErrorResponseData(_request, _method, restLiServiceException, headers);
  }

  private void invokeResponseFilters(final FilterResponseContextInternal responseContext)
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
        responseContext.setAugmentedRestLiResponseData(convertExceptionToRestLiResponseData(ex));
      }
    }
    // If an exception was thrown by the last filter in the filter chain, rethrow it.
    if (lastException != null)
    {
      throw lastException;
    }
  }

  /* Package private for testing purposes */
  static class FilterResponseContextAdapter implements FilterResponseContextInternal
  {
    private AugmentedRestLiResponseData _responseData;
    public FilterResponseContextAdapter(final AugmentedRestLiResponseData response)
    {
      _responseData = response;
    }

    @Override
    public void setHttpStatus(HttpStatus status)
    {
      _responseData.setStatus(status);
    }

    @Override
    public Map<String, String> getResponseHeaders()
    {
      return _responseData.getHeaders();
    }

    @Override
    public HttpStatus getHttpStatus()
    {
      return _responseData.getStatus();
    }

    @Override
    public RestLiResponseData getResponseData()
    {
      return _responseData;
    }

    @Override
    public void setAugmentedRestLiResponseData(AugmentedRestLiResponseData data)
    {
      _responseData = data;
    }

    @Override
    public AugmentedRestLiResponseData getAugmentedRestLiResponseData()
    {
      return _responseData;
    }
  }
}
