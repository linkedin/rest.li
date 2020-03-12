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

package com.linkedin.darkcluster.filter;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterVerifier;
import com.linkedin.darkcluster.impl.ResponseImpl;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DarkClusterFilter can be added to the Restli filter chain on either the server or client side, to tee off requests to a
 * dark cluster. It delegates to the DarkClusterManager for sending the dark request and verifying the dark response
 * against the original response, if that is configured.
 *
 * Future enhancements might be to make it a Stream Filter as well.
 */
public class DarkClusterFilter implements RestFilter
{
  private static final Logger _log = LoggerFactory.getLogger(DarkClusterFilter.class);
  private static final String ORIGINAL_REQUEST = DarkClusterFilter.class.getName() + "_originalRequest";

  private final DarkClusterManager _darkClusterManager;
  private final DarkClusterVerifier _darkClusterVerifier;
  private final ExecutorService _executorService;

  public DarkClusterFilter(@Nonnull DarkClusterManager darkClusterManager, @Nonnull DarkClusterVerifier darkClusterVerifier,
                           @Nonnull ExecutorService executorService)
  {
    _darkClusterManager = darkClusterManager;
    _darkClusterVerifier = darkClusterVerifier;
    _executorService = executorService;
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    // use a copy of request & request context to make sure what we do in dispatcher won't affect the processing
    // of the original request
    RestRequest reqCopy = req.builder().build();
    RequestContext newRequestContext = new RequestContext(requestContext);

    boolean verifyResponse = _darkClusterManager.sendDarkRequest(reqCopy, newRequestContext);

    if (verifyResponse)
    {
      requestContext.putLocalAttr(ORIGINAL_REQUEST, req);
    }

    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    Object request = requestContext.getLocalAttr(ORIGINAL_REQUEST);
    if (request instanceof RestRequest)
    {
      //this method does not throw exceptions
      if (_darkClusterVerifier.isEnabled())
      {
        _executorService.execute(() -> _darkClusterVerifier.onResponse((RestRequest) request, ResponseImpl.success(res)));
      }
    }

    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
                          NextFilter<RestRequest, RestResponse> nextFilter)
  {

    Object request = requestContext.getLocalAttr(ORIGINAL_REQUEST);
    if (request instanceof RestRequest)
    {
      // this method does not throw exception
      if (_darkClusterVerifier.isEnabled())
      {
        _executorService.execute(() -> _darkClusterVerifier.onResponse((RestRequest) request, ResponseImpl.error(ex)));
      }
    }
    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
