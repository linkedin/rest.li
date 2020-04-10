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

import javax.annotation.Nonnull;

import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterVerifierManager;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DarkClusterFilter can be added to the Restli filter chain on either the server or client side, to tee off requests to a
 * dark cluster. It delegates to the {@link DarkClusterManager} for sending the dark request and verifying the dark response
 * against the original response, if that is configured.
 *
 * Future enhancements might be to make it a Stream Filter as well.
 */
public class DarkClusterFilter implements RestFilter
{
  private static final Logger _log = LoggerFactory.getLogger(DarkClusterFilter.class);
  private static final String ORIGINAL_REQUEST_KEY = DarkClusterFilter.class.getSimpleName() + "_originalRequest";

  private final DarkClusterManager _darkClusterManager;
  private final DarkClusterVerifierManager _darkClusterVerifierManager;

  public DarkClusterFilter(@Nonnull DarkClusterManager darkClusterManager, @Nonnull DarkClusterVerifierManager darkClusterVerifierManager)
  {
    _darkClusterManager = darkClusterManager;
    _darkClusterVerifierManager = darkClusterVerifierManager;
  }

  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    boolean verifyResponse = _darkClusterManager.handleDarkRequest(req, requestContext);

    if (verifyResponse)
    {
      requestContext.putLocalAttr(ORIGINAL_REQUEST_KEY, req);
    }

    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    Object request = requestContext.getLocalAttr(ORIGINAL_REQUEST_KEY);
    if (request instanceof RestRequest)
    {
      _darkClusterVerifierManager.onResponse((RestRequest) request, res);
    }

    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
                          NextFilter<RestRequest, RestResponse> nextFilter)
  {

    Object request = requestContext.getLocalAttr(ORIGINAL_REQUEST_KEY);

    if (request instanceof RestRequest)
    {
      _darkClusterVerifierManager.onError((RestRequest)request, ex);
    }
    nextFilter.onError(ex, requestContext, wireAttrs);
  }
}
