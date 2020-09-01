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

import com.linkedin.darkcluster.api.DarkGateKeeper;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.util.D2URIRewriter;
import com.linkedin.d2.balancer.util.URIRewriter;
import com.linkedin.darkcluster.api.DarkClusterManager;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.api.DarkClusterStrategyFactory;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpMethod;

import static com.linkedin.r2.message.QueryTunnelUtil.HEADER_METHOD_OVERRIDE;
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
  private final Facilities _facilities;
  private final String _sourceClusterName;
  private final DarkClusterStrategyFactory _darkClusterStrategyFactory;
  private final DarkGateKeeper _darkGateKeeper;
  private Map<String, AtomicReference<URIRewriter>> _uriRewriterMap;

  public DarkClusterManagerImpl(@Nonnull String sourceClusterName, @Nonnull Facilities facilities,
                                @Nonnull DarkClusterStrategyFactory strategyFactory, String whiteListRegEx,
                                String blackListRegEx, @Nonnull Notifier notifier)
  {
    this(sourceClusterName, facilities, strategyFactory, whiteListRegEx, blackListRegEx, notifier, null);
  }

  public DarkClusterManagerImpl(@Nonnull String sourceClusterName, @Nonnull Facilities facilities,
      @Nonnull DarkClusterStrategyFactory strategyFactory, String whiteListRegEx,
      String blackListRegEx, @Nonnull Notifier notifier, DarkGateKeeper darkGateKeeper) {
    _whiteListRegEx = whiteListRegEx == null ? null : Pattern.compile(whiteListRegEx);
    _blackListRegEx = blackListRegEx == null ? null : Pattern.compile(blackListRegEx);
    _notifier = notifier;
    _facilities = facilities;
    _sourceClusterName = sourceClusterName;
    _darkClusterStrategyFactory = strategyFactory;
    _uriRewriterMap = new HashMap<>();
    // if null, initialize this to a noop which returns true always
    _darkGateKeeper = darkGateKeeper == null ? (req, context) -> true : darkGateKeeper;
  }

  @Override
  public boolean handleDarkRequest(RestRequest originalRequest, RequestContext originalRequestContext)
  {
    String uri = originalRequest.getURI().toString();
    boolean darkRequestSent = false;
    try
    {
      final boolean whiteListed = _whiteListRegEx != null && _whiteListRegEx.matcher(uri).matches();
      final boolean blackedListed = _blackListRegEx != null && _blackListRegEx.matcher(uri).matches();
      // send to dark iff:
      // 1) request is safe
      // 2) is whitelisted if whitelist regex is provided
      // 3) not blacklisted if blacklist regex is provided
      // 4) custom dark gatekeeper returns true for the given request and requestContext
      if ((isSafe(originalRequest) || whiteListed) && !blackedListed && (_darkGateKeeper.shouldDispatchToDark(originalRequest, originalRequestContext)))
      {
        // the request is already immutable, and a new requestContext will be created in BaseDarkClusterDispatcher.
        // We don't need to copy them here, but doing it just for safety.
        RestRequest reqCopy = originalRequest.builder().build();
        RequestContext newRequestContext = new RequestContext(originalRequestContext);
        DarkClusterConfigMap configMap = _facilities.getClusterInfoProvider().getDarkClusterConfigMap(_sourceClusterName);
        for (String darkClusterName : configMap.keySet())
        {
          RestRequest newD2Request = rewriteRequest(reqCopy, darkClusterName);
          // now find the strategy appropriate for each dark cluster
          DarkClusterStrategy strategy = _darkClusterStrategyFactory.get(darkClusterName);
          darkRequestSent = strategy.handleRequest(reqCopy, newD2Request, newRequestContext);
        }

      }
    }
    catch (Throwable e)
    {
      _notifier.notify(() -> new RuntimeException("DarkCanaryDispatcherFilter failed to send request: " + uri, e));
    }
    return darkRequestSent;
  }

  /**
   * isSafe returns true if the underlying HttpMethod has the expectation of only doing retrieval with no side effects. For further details,
   * see {@link HttpMethod}
   * @param req
   * @return
   */
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
    // computeIfAbsent has performance problems in Java 7/8. Check the Map first
    if (!_uriRewriterMap.containsKey(darkServiceName))
    {
      _uriRewriterMap.computeIfAbsent(darkServiceName, k -> {
        URI configuredURI = URI.create("d2://" + darkServiceName);
        URIRewriter rewriter = new D2URIRewriter(configuredURI);
        return new AtomicReference<>(rewriter);
      });
    }

    URIRewriter rewriter = _uriRewriterMap.get(darkServiceName).get();
    return originalRequest.builder().setURI(rewriter.rewriteURI(originalRequest.getURI())).build();
  }
}
