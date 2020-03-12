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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.d2.balancer.util.D2URIRewriter;
import com.linkedin.d2.balancer.util.URIRewriter;
import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DarkClusterManagerImpl verifies that the request to copy is safe to send, rewrites the request, and hands it off the to strategy to send.
 */
public class DarkClusterManagerImpl implements DarkClusterManager
{
  private static final Logger _log = LoggerFactory.getLogger(DarkClusterManagerImpl.class);

  private final Pattern _whiteListRegEx;
  private final Pattern _blackListRegEx;
  private final Notifier _notifier;
  private final ClusterInfoProvider _clusterInfoProvider;
  private String _clusterName;
  private final DarkClusterStrategyFactory _darkClusterStrategyFactory;
  private Map<String, AtomicReference<URIRewriter>> _uriRewriterMap;

  public DarkClusterManagerImpl(@Nonnull String clusterName, @Nonnull ClusterInfoProvider clusterInfoProvider,
                                @Nonnull DarkClusterStrategyFactory strategyFactory, String whiteListRegEx,
                                String blackListRegEx, @Nonnull Notifier notifier)
  {
    _whiteListRegEx = whiteListRegEx == null ? null : Pattern.compile(whiteListRegEx);
    _blackListRegEx = blackListRegEx == null ? null : Pattern.compile(blackListRegEx);
    _notifier = notifier;
    _clusterInfoProvider = clusterInfoProvider;
    _clusterName = clusterName;
    _darkClusterStrategyFactory = strategyFactory;
    _uriRewriterMap = new HashMap<>();
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

        DarkClusterConfigMap configMap = _clusterInfoProvider.getDarkClusterConfigMap(_clusterName);
        for (Map.Entry<String, DarkClusterConfig> darkClusterConfigEntry : configMap.entrySet())
        {
          String darkClusterName = darkClusterConfigEntry.getKey();
          DarkClusterConfig darkClusterConfig = darkClusterConfigEntry.getValue();

          RestRequest newD2Request = rewriteRequest(request, darkClusterName);
          // now find the strategy appropriate for each dark cluster
          DarkClusterStrategy strategy = _darkClusterStrategyFactory.getOrCreate(darkClusterName, darkClusterConfig);
          darkRequestSent = strategy.handleRequest(newD2Request, request, newRequestContext);
        }

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

  /**
   * RewriteRequest takes the original request and creates a new one with the dark service name.
   * The original request URI is actually of the form "/<restli-resource>/rest-of-path" because it is being
   * processed in the r2 filter chain.
   * @param originalRequest
   * @return
   */
  private RestRequest rewriteRequest(RestRequest originalRequest, String darkServiceName)
  {
    _uriRewriterMap.computeIfAbsent(darkServiceName, k -> {
      URI configuredURI = URI.create("d2://" + darkServiceName);
      URIRewriter rewriter = new D2URIRewriter(configuredURI);
      return new AtomicReference<>(rewriter);
    });
    URIRewriter rewriter = _uriRewriterMap.get(darkServiceName).get();
    return originalRequest.builder().setURI(rewriter.rewriteURI(originalRequest.getURI())).build();
  }
}
