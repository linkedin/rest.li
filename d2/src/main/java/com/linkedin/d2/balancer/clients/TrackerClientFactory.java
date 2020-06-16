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

package com.linkedin.d2.balancer.clients;

import com.linkedin.d2.HttpStatusCodeRange;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategyFactory;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.degrader.DegraderConfigFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.util.RateLimitedLogger;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.degrader.DegraderImpl;

import static com.linkedin.d2.discovery.util.LogUtil.warn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link TrackerClient}s.
 */
public class TrackerClientFactory
{
  private static final Logger LOG = LoggerFactory.getLogger(TrackerClientFactory.class);

  private static final int LOG_RATE_MS = 20000;

  /**
   * @see #createTrackerClient(URI, UriProperties, ServiceProperties, String, TransportClient, Clock)
   */
  public static TrackerClient createTrackerClient(URI uri,
                                           UriProperties uriProperties,
                                           ServiceProperties serviceProperties,
                                           String loadBalancerStrategyName,
                                           TransportClient transportClient)
  {
    return createTrackerClient(uri, uriProperties, serviceProperties, loadBalancerStrategyName, transportClient, SystemClock.instance());
  }

  /**
   * Creates a {@link TrackerClient}.
   *
   * @param uri URI of the server for this client.
   * @param loadBalancerStrategyName Name of the strategy. eg "degrader"
   * @param serviceProperties Properties for the service this URI belongs to.
   * @param uriProperties URI properties.
   * @param transportClient Inner TransportClient.
   * @param clock Clock used for internal call tracking.
   * @return TrackerClient
   */
  public static TrackerClient createTrackerClient(URI uri,
                                                  UriProperties uriProperties,
                                                  ServiceProperties serviceProperties,
                                                  String loadBalancerStrategyName,
                                                  TransportClient transportClient,
                                                  Clock clock)
  {
    TrackerClient trackerClient;

    switch (loadBalancerStrategyName)
    {
      case (DegraderLoadBalancerStrategyV3.DEGRADER_STRATEGY_NAME):
        trackerClient = createDegraderTrackerClient(uri, uriProperties, serviceProperties,  loadBalancerStrategyName, transportClient, clock);
        break;
      default:
        trackerClient = createTrackerClientImpl(uri, uriProperties, serviceProperties, loadBalancerStrategyName, transportClient, clock);
    }

    return trackerClient;
  }

  private static DegraderTrackerClient createDegraderTrackerClient(URI uri,
                                                                   UriProperties uriProperties,
                                                                   ServiceProperties serviceProperties,
                                                                   String loadBalancerStrategyName,
                                                                   TransportClient transportClient,
                                                                   Clock clock)
  {
    DegraderImpl.Config config = null;

    if (serviceProperties.getLoadBalancerStrategyProperties() != null)
    {
      Map<String, Object> loadBalancerStrategyProperties =
        serviceProperties.getLoadBalancerStrategyProperties();
      clock = MapUtil.getWithDefault(loadBalancerStrategyProperties, PropertyKeys.CLOCK, clock, Clock.class);
    }

    if (serviceProperties.getDegraderProperties() != null && !serviceProperties.getDegraderProperties().isEmpty())
    {
      config = DegraderConfigFactory.toDegraderConfig(serviceProperties.getDegraderProperties());
      config.setLogger(new RateLimitedLogger(LOG, LOG_RATE_MS, clock));
    }

    long trackerClientInterval = getInterval(loadBalancerStrategyName, serviceProperties);
    Pattern errorStatusPattern = getErrorStatusPattern(serviceProperties);

    return new DegraderTrackerClientImpl(uri,
                                     uriProperties.getPartitionDataMap(uri),
                                     transportClient,
                                     clock,
                                     config,
                                     trackerClientInterval,
                                     errorStatusPattern,
                                     uriProperties.getUriSpecificProperties().get(uri));
  }

  private static long getInterval(String loadBalancerStrategyName, ServiceProperties serviceProperties)
  {
    long interval = TrackerClientImpl.DEFAULT_CALL_TRACKER_INTERVAL;
    if (serviceProperties != null)
    {
      switch (loadBalancerStrategyName)
      {
        case (DegraderLoadBalancerStrategyV3.DEGRADER_STRATEGY_NAME):
          Map<String, Object> loadBalancerStrategyProperties = serviceProperties.getLoadBalancerStrategyProperties();
          if (loadBalancerStrategyProperties != null)
          {
            interval = MapUtil.getWithDefault(serviceProperties.getLoadBalancerStrategyProperties(),
                                              PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
                                              DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS,
                                              Long.class);
          }
          break;
      }
    }
    return interval;
  }

  private static Pattern getErrorStatusPattern(ServiceProperties serviceProperties)
  {
    String regex = TrackerClientImpl.DEFAULT_ERROR_STATUS_REGEX;
    if (serviceProperties != null)
    {
      regex = MapUtil.getWithDefault(serviceProperties.getLoadBalancerStrategyProperties(),
          PropertyKeys.HTTP_LB_ERROR_STATUS_REGEX,
          TrackerClientImpl.DEFAULT_ERROR_STATUS_REGEX,
          String.class);
    }

    Pattern errorPattern;
    try
    {
      errorPattern = Pattern.compile(regex);
    }
    catch (PatternSyntaxException ex)
    {
      warn(LOG, "Invalid error status regex: ", regex, ". Falling back to default regex: ", TrackerClientImpl.DEFAULT_ERROR_STATUS_REGEX);
      errorPattern = TrackerClientImpl.DEFAULT_ERROR_STATUS_PATTERN;
    }
    return errorPattern;
  }

  private static List<HttpStatusCodeRange> getErrorStatusRanges(ServiceProperties serviceProperties)
  {
    if (serviceProperties.getRelativeStrategyProperties() == null
        || serviceProperties.getRelativeStrategyProperties().getErrorStatusFilter() == null)
    {
      return RelativeLoadBalancerStrategyFactory.DEFAULT_ERROR_STATUS_FILTER;
    }
    return serviceProperties.getRelativeStrategyProperties().getErrorStatusFilter();
  }

  private static TrackerClientImpl createTrackerClientImpl(URI uri,
                                                           UriProperties uriProperties,
                                                           ServiceProperties serviceProperties,
                                                           String loadBalancerStrategyName,
                                                           TransportClient transportClient,
                                                           Clock clock)
  {
    TrackerClientImpl.ErrorStatusMatch errorStatusRangeMatch = (status) -> {
      for(HttpStatusCodeRange statusCodeRange : getErrorStatusRanges(serviceProperties))
      {
        if (status >= statusCodeRange.getLowerBound() && status <= statusCodeRange.getUpperBound())
        {
          return true;
        }
      }
      return false;
    };
    return new TrackerClientImpl(uri,
                                 uriProperties.getPartitionDataMap(uri),
                                 transportClient,
                                 clock,
                                 getInterval(loadBalancerStrategyName, serviceProperties),
                                 errorStatusRangeMatch);
  }
}
