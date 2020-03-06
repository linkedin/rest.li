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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import com.linkedin.common.util.Notifier;
import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterVerifier;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.HttpMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DarkClusterManagerImpl verifies that the request to copy is safe to send, and hands it off the to strategy if it is.
 */
public class DarkClusterManagerImpl implements DarkClusterManager
{
  private static final Logger _log = LoggerFactory.getLogger(DarkClusterManagerImpl.class);

  private final Pattern _whiteListRegEx;
  private final Pattern _blackListRegEx;
  private final ExecutorService _dispatcherExecutor;
  private final Notifier _notifier;
  private final DarkClusterStrategy _strategy;
  private final DarkClusterVerifier _verifier;

  public DarkClusterManagerImpl(ExecutorService dispatcherExecutor, String whiteListRegEx,
                                String blackListRegEx, Notifier notifier, DarkClusterStrategy strategy,
                                DarkClusterVerifier verifier)
  {
    _whiteListRegEx = whiteListRegEx == null ? null : Pattern.compile(whiteListRegEx);
    _blackListRegEx = blackListRegEx == null ? null : Pattern.compile(blackListRegEx);
    _dispatcherExecutor = dispatcherExecutor;
    _notifier = notifier;
    _strategy = strategy;
    _verifier = verifier;
  }

  @Override
  public boolean sendDarkRequest(RestRequest request, RequestContext requestContext)
  {
    // the request is already immutable, and a new requestContext will be created in BaseDarkClusterDispatcher.
    // We don't need to copy them here, but doing it just for safety.
    RequestContext newRequestContext = new RequestContext(requestContext);
    String uri = request.getURI().toString();
    boolean darkRequestSent = false;
    try
    {
      final boolean whiteListed = _whiteListRegEx != null && _whiteListRegEx.matcher(uri).matches();
      final boolean blackedListed = _blackListRegEx != null && _blackListRegEx.matcher(uri).matches();
      if ((isSafe(request) || whiteListed) && !blackedListed)
      {
        darkRequestSent = _strategy.handleRequest(request, newRequestContext);
      }
    }
    catch (Throwable e)
    {
      _notifier.notify(() -> new RuntimeException("DarkCanaryDispatcherFilter failed to send request: " + uri, e));
    }
    return darkRequestSent;
  }

  private boolean isSafe(RestRequest req)
  {
    try
    {
      Map<String, String> headers = req.getHeaders();
      HttpMethod method;
      if (headers != null && headers.containsKey(HEADER_METHOD_OVERRIDE))
      {
        // This request method was converted from another method. (see com.linkedin.r2.message.rest.QueryTunnelUtil.java)
        method = HttpMethod.valueOf(headers.get(HEADER_METHOD_OVERRIDE));
      }
      else
      {
        method = HttpMethod.valueOf(req.getMethod());
      }
      return method.isSafe();
    }
    catch (Exception e)
    {
      _log.error("Invalid HttpMethod: {}" + req.getMethod());
      return false;
    }
  }

  @Override
  public boolean hasVerifier()
  {
    return false;
  }

  @Override
  public void verifyResponse(RestRequest originalRequest, RestResponse originalResponse)
  {
    // To be implemented
  }

  @Override
  public void verifyError(RestRequest originalRequest, Throwable originalError)
  {
    // To be implemented
  }
}
