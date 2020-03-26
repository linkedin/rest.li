/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.Notifier;
import com.linkedin.darkcluster.api.BaseDarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

/**
 * The BaseDarkClusterDispatcher handles the basic operations of dispatching a dark request. It takes in a custom dispatcher, handles errors,
 * gathers metrics, and calls the verifier if needed.
 *
 * Note that it is the custom dispatcher's job to send the request on a different executor if that's desired.
 */
public class BaseDarkClusterDispatcherImpl implements BaseDarkClusterDispatcher
{
  private final String _darkClusterName;
  private final DarkClusterDispatcher _dispatcher;
  private final Notifier _notifier;

  // Fields keeping track of statistics
  private final AtomicInteger _requestCount = new AtomicInteger(0);
  private final AtomicInteger _successCount = new AtomicInteger(0);
  private final AtomicInteger _exceptionCount = new AtomicInteger(0);
  private final ConcurrentHashMap<String, AtomicInteger> _exceptionCountMap = new ConcurrentHashMap<String, AtomicInteger>();
  private final DarkClusterVerifierManager _verifierManager;

  public BaseDarkClusterDispatcherImpl(@Nonnull String darkClusterName,
                                       @Nonnull final DarkClusterDispatcher dispatcher,
                                       @Nonnull final Notifier notifier,
                                       @Nonnull DarkClusterVerifierManager verifierManager)
  {
    _darkClusterName = darkClusterName;
    _dispatcher = dispatcher;
    _notifier = notifier;
    _verifierManager = verifierManager;
  }

  public boolean sendRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext, int numRequestDuplicates)
  {
    boolean requestSent = false;

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse result)
      {
        _successCount.incrementAndGet();
        // Result of request is discarded if verifier is not enabled
        _verifierManager.onDarkResponse(originalRequest, result, _darkClusterName);
      }

      @Override
      public void onError(Throwable e)
      {
        _exceptionCount.incrementAndGet();

        _notifier.notify(() -> new RuntimeException(
          "Got error response for:  " + darkRequest.getURI() + " from source host " + originalRequest.getURI(),
          e));

        String exceptionName = e.getClass().getSimpleName();
        if (e.getCause() != null)
        {
          exceptionName += "/" + e.getCause().getClass().getSimpleName();
        }
        AtomicInteger oldCount = _exceptionCountMap.putIfAbsent(exceptionName, new AtomicInteger(1));
        if (oldCount != null)
        {
          oldCount.incrementAndGet();
        }
        _verifierManager.onDarkError(originalRequest, e, _darkClusterName);
      }
    };

    for (int i = 0; i < numRequestDuplicates; i++)
    {
      _requestCount.incrementAndGet();
      final RequestContext darkContext = new RequestContext();
      Object requestWasTunneled = requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED);
      if (requestWasTunneled != null && (Boolean) requestWasTunneled)
      {
        darkContext.putLocalAttr(R2Constants.FORCE_QUERY_TUNNEL, true);
      }
      if (_dispatcher.sendRequest(originalRequest, darkRequest, requestContext, callback))
      {
        requestSent = true;
      }
    }
    return requestSent;
  }

  public int getRequestCount()
  {
    return _requestCount.get();
  }

  public int getExceptionCount()
  {
    return _exceptionCount.get();
  }

  public int getSuccessCount()
  {
    return _successCount.get();
  }
}
