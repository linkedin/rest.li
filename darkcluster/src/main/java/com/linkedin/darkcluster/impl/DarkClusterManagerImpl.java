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

import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.servers.ZooKeeperAnnouncer;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.darkcluster.api.DarkGateKeeper;
import com.linkedin.darkcluster.api.DarkRequestHeaderGenerator;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import com.linkedin.util.RateLimitedLogger;
import com.linkedin.util.clock.SystemClock;

import static com.linkedin.r2.message.QueryTunnelUtil.HEADER_METHOD_OVERRIDE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DarkClusterManagerImpl verifies that the request to copy is safe to send, rewrites the request, and hands it off the to strategy to send.
 */
public class DarkClusterManagerImpl implements DarkClusterManager
{
  private static final Logger _rateLimitedLogger =
      new RateLimitedLogger(LoggerFactory.getLogger(DarkClusterManagerImpl.class), TimeUnit.MINUTES.toMillis(1), SystemClock.instance());

  private final Pattern _whiteListRegEx;
  private final Pattern _blackListRegEx;
  private final Notifier _notifier;
  private final Facilities _facilities;
  private final String _sourceClusterName;
  private final DarkClusterStrategyFactory _darkClusterStrategyFactory;
  private final DarkGateKeeper _darkGateKeeper;
  private final List<DarkRequestHeaderGenerator> _darkRequestHeaderGenerators;
  private Map<String, AtomicReference<URIRewriter>> _uriRewriterMap;
  private final List<ZooKeeperAnnouncer> _announcers;

  public DarkClusterManagerImpl(@Nonnull String sourceClusterName, @Nonnull Facilities facilities,
                                @Nonnull DarkClusterStrategyFactory strategyFactory, String whiteListRegEx,
                                String blackListRegEx, @Nonnull Notifier notifier)
  {
    this(sourceClusterName, facilities, strategyFactory, whiteListRegEx, blackListRegEx, notifier, null);
  }

  public DarkClusterManagerImpl(@Nonnull String sourceClusterName,
      @Nonnull Facilities facilities,
      @Nonnull DarkClusterStrategyFactory strategyFactory,
      String whiteListRegEx,
      String blackListRegEx,
      @Nonnull Notifier notifier,
      DarkGateKeeper darkGateKeeper)
  {
    this(sourceClusterName, facilities, strategyFactory, whiteListRegEx, blackListRegEx, notifier, darkGateKeeper, null);
  }

  public DarkClusterManagerImpl(@Nonnull String sourceClusterName,
      @Nonnull Facilities facilities,
      @Nonnull DarkClusterStrategyFactory strategyFactory,
      String whiteListRegEx,
      String blackListRegEx,
      @Nonnull Notifier notifier,
      DarkGateKeeper darkGateKeeper,
      List<DarkRequestHeaderGenerator> darkRequestHeaderGenerators)
  {
    this(sourceClusterName, facilities, strategyFactory, whiteListRegEx, blackListRegEx, notifier, darkGateKeeper,
        darkRequestHeaderGenerators, Collections.emptyList());
  }

  public DarkClusterManagerImpl(@Nonnull String sourceClusterName,
      @Nonnull Facilities facilities,
      @Nonnull DarkClusterStrategyFactory strategyFactory,
      String whiteListRegEx,
      String blackListRegEx,
      @Nonnull Notifier notifier,
      DarkGateKeeper darkGateKeeper,
      List<DarkRequestHeaderGenerator> darkRequestHeaderGenerators,
      @Nonnull List<ZooKeeperAnnouncer> announcers)
  {
    _whiteListRegEx = whiteListRegEx == null ? null : Pattern.compile(whiteListRegEx);
    _blackListRegEx = blackListRegEx == null ? null : Pattern.compile(blackListRegEx);
    _notifier = notifier;
    _facilities = facilities;
    _sourceClusterName = sourceClusterName;
    _darkClusterStrategyFactory = strategyFactory;
    _uriRewriterMap = new HashMap<>();
    // if null, initialize this to a noop which returns true always
    _darkGateKeeper = darkGateKeeper == null ? DarkGateKeeper.NO_OP_DARK_GATE_KEEPER : darkGateKeeper;
    _darkRequestHeaderGenerators = darkRequestHeaderGenerators == null ? Collections.emptyList() : darkRequestHeaderGenerators;
    _announcers = announcers;
  }

  @Override
  public boolean handleDarkRequest(RestRequest originalRequest, RequestContext originalRequestContext)
  {
    for (ZooKeeperAnnouncer announcer : _announcers) {
      if (announcer.isWarmingUp()) {
          return false;
      }
    }
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
      if ((isSafe(originalRequest) || whiteListed) && !blackedListed)
      {
        // the request is already immutable, and a new requestContext will be created in BaseDarkClusterDispatcher.
        // We don't need to copy them here, but doing it just for safety.
        RestRequest reqCopy = originalRequest.builder().build();
        RequestContext newRequestContext = new RequestContext(originalRequestContext);
        DarkClusterConfigMap configMap = _facilities.getClusterInfoProvider().getDarkClusterConfigMap(_sourceClusterName);
        for (String darkClusterName : configMap.keySet())
        {
          if (_darkGateKeeper.shouldDispatchToDark(originalRequest, originalRequestContext, darkClusterName))
          {
            RestRequest newD2Request = rewriteRequest(reqCopy, darkClusterName);
            int partitionId = getPartitionId(newD2Request);
            // now find the strategy appropriate for each dark cluster
            DarkClusterStrategy strategy = _darkClusterStrategyFactory.get(darkClusterName, partitionId);
            darkRequestSent |= strategy.handleRequest(reqCopy, newD2Request, newRequestContext);
          }
        }

      }
    }
    catch (RuntimeException | ServiceUnavailableException e)
    {
      _notifier.notify(() -> new RuntimeException("DarkCanaryDispatcherFilter failed to send request: " + uri, e));
    }
    return darkRequestSent;
  }

  private int getPartitionId(RestRequest request)
  {
    try
    {
      String serviceName = com.linkedin.d2.balancer.util.LoadBalancerUtil.getServiceNameFromUri(request.getURI());
      com.linkedin.d2.balancer.util.partitions.PartitionAccessor accessor = _facilities.getPartitionInfoProvider().getPartitionAccessor(serviceName);
      return accessor.getPartitionId(request.getURI());
    }
    catch (RuntimeException | PartitionAccessException | ServiceUnavailableException e)
    {
      _rateLimitedLogger.error("Cannot find partition id for request: {}, defaulting to 0", request.getURI(), e);
      return com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor.DEFAULT_PARTITION_ID;
    }
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
      _rateLimitedLogger.error("Invalid HttpMethod: {}", req.getMethod());
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
    RestRequestBuilder darkRequestBuilder = originalRequest.builder().setURI(rewriter.rewriteURI(originalRequest.getURI()));
    _darkRequestHeaderGenerators.forEach(darkRequestHeaderGenerator -> {
      darkRequestHeaderGenerator.get(darkServiceName).ifPresent(headerNameValuePair -> {
        darkRequestBuilder.setHeader(headerNameValuePair.getName(), headerNameValuePair.getValue());
      });
    });
    return darkRequestBuilder.build();
  }
}
