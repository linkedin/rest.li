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
import com.linkedin.restli.internal.server.filter.RestLiResponseFilterChain;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReport;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.ResponseFilter;
import com.linkedin.restli.internal.server.filter.*;

import java.util.ArrayList;
import java.util.List;


public class RestLiCallback<T> implements RequestExecutionCallback<T>
{
  private final RoutingResult _method;
  private final RestLiResponseHandler _responseHandler;
  private final RequestExecutionCallback<RestResponse> _callback;
  private final RestRequest _request;
  private final List<ResponseFilter> _responseFilters;
  private final FilterRequestContext _filterRequestContext;
  private final RestLiResponseFilterContextFactory _responseFilterContextFactory;

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
    _responseFilterContextFactory = new RestLiResponseFilterContextFactory(_request, _method, _responseHandler);
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
    final FilterResponseContext responseContext;
    try
    {
      responseContext = _responseFilterContextFactory.fromResult(result);
    }
    catch (Exception e)
    {
      // Invoke the onError method if we run into any exception while creating the response context from result.
      onError(e, executionReport);
      return;
    }
    // Now kick off the response filters.
    RestLiResponseFilterChain restLiResponseFilterChain = new RestLiResponseFilterChain(_responseFilters,
                                                                                        _responseFilterContextFactory,
                                                                                        new RestLiResponseFilterChainCallbackImpl(
                                                                                            executionReport));
    restLiResponseFilterChain.onResponse(_filterRequestContext, responseContext);
  }

  @Override
  public void onError(final Throwable e, RequestExecutionReport executionReport)
  {
    final FilterResponseContext responseContext = _responseFilterContextFactory.fromThrowable(e);
    // Now kick off the response filters.
    RestLiResponseFilterChain restLiResponseFilterChain = new RestLiResponseFilterChain(_responseFilters,
                                                                                        _responseFilterContextFactory,
                                                                                        new RestLiResponseFilterChainCallbackImpl(
                                                                                            executionReport));
    restLiResponseFilterChain.onResponse(_filterRequestContext, responseContext);
  }

  /**
   * Concrete implementation of {@link RestLiResponseFilterChainCallback}.
   */
  private class RestLiResponseFilterChainCallbackImpl implements RestLiResponseFilterChainCallback
  {
    private  final RequestExecutionReport _executionReport;

    public RestLiResponseFilterChainCallbackImpl(final RequestExecutionReport executionReport)
    {
      _executionReport = executionReport;
    }

    @Override
    public void onCompletion(final RestLiResponseData responseData)
    {
      final PartialRestResponse response = _responseHandler.buildPartialResponse(_method, responseData);
      if (responseData.isErrorResponse())
      {
        // If the updated response from the filter is an error response, then call onError on the underlying callback.
        RestLiServiceException e = responseData.getServiceException();
        // Invoke onError on the R2 callback since we received an exception from the filters.
        _callback.onError(_responseHandler.buildRestException(e, response), _executionReport);
      }
      else
      {
        // Invoke onSuccess on the underlying callback.
        _callback.onSuccess(_responseHandler.buildResponse(_method, response), _executionReport);
      }
    }
  }
}
