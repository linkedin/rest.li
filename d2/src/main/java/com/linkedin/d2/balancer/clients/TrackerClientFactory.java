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

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.HttpStatusCodeRange;
import com.linkedin.d2.balancer.config.RelativeStrategyPropertiesConverter;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategyFactory;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.degrader.DegraderConfigFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.util.degrader.DegraderImpl;
import com.linkedin.util.RateLimitedLogger;

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
    return createTrackerClient(uri, uriProperties, serviceProperties, loadBalancerStrategyName, transportClient, Clock.systemUTC());
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

    boolean doNotSlowStart = false;
    Map<String, Object> uriSpecificProperties = uriProperties.getUriSpecificProperties().get(uri);
    if (uriSpecificProperties != null && uriSpecificProperties.containsKey(PropertyKeys.DO_NOT_SLOW_START)
        && Boolean.parseBoolean(uriSpecificProperties.get(PropertyKeys.DO_NOT_SLOW_START).toString()))
    {
      doNotSlowStart = true;
    }

    switch (loadBalancerStrategyName)
    {
      case (DegraderLoadBalancerStrategyV3.DEGRADER_STRATEGY_NAME):
        trackerClient = createDegraderTrackerClient(uri, uriProperties, serviceProperties,  loadBalancerStrategyName, transportClient, clock, doNotSlowStart);
        break;
      case (RelativeLoadBalancerStrategy.RELATIVE_LOAD_BALANCER_STRATEGY_NAME):
        trackerClient = createTrackerClientImpl(uri, uriProperties, serviceProperties, loadBalancerStrategyName,
            transportClient, clock, false, doNotSlowStart);
        break;
      default:
        trackerClient = createTrackerClientImpl(uri, uriProperties, serviceProperties, loadBalancerStrategyName,
            transportClient, clock, true, doNotSlowStart);
    }

    return trackerClient;
  }

  private static DegraderTrackerClient createDegraderTrackerClient(URI uri,
                                                                   UriProperties uriProperties,
                                                                   ServiceProperties serviceProperties,
                                                                   String loadBalancerStrategyName,
                                                                   TransportClient transportClient,
                                                                   Clock clock,
                                                                   boolean doNotSlowStart)
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
                                     doNotSlowStart);
  }

  private static long getInterval(String loadBalancerStrategyName, ServiceProperties serviceProperties)
  {
    long interval = TrackerClientImpl.DEFAULT_CALL_TRACKER_INTERVAL;
    if (serviceProperties != null)
    {
      switch (loadBalancerStrategyName)
      {
        case (RelativeLoadBalancerStrategy.RELATIVE_LOAD_BALANCER_STRATEGY_NAME):
          Map<String, Object> relativeLoadBalancerProperties = serviceProperties.getRelativeStrategyProperties();
          if (relativeLoadBalancerProperties != null)
          {
            interval = MapUtil.getWithDefault(serviceProperties.getRelativeStrategyProperties(),
                PropertyKeys.UPDATE_INTERVAL_MS,
                RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS,
                Long.class);
          }
          break;
        case (DegraderLoadBalancerStrategyV3.DEGRADER_STRATEGY_NAME):
        default:
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
    D2RelativeStrategyProperties relativeStrategyProperties =
        RelativeStrategyPropertiesConverter.toProperties(serviceProperties.getRelativeStrategyProperties());
    if (relativeStrategyProperties.getErrorStatusFilter() == null)
    {
      return RelativeLoadBalancerStrategyFactory.DEFAULT_ERROR_STATUS_FILTER;
    }
    return relativeStrategyProperties.getErrorStatusFilter();
  }

  private static TrackerClientImpl createTrackerClientImpl(URI uri,
                                                           UriProperties uriProperties,
                                                           ServiceProperties serviceProperties,
                                                           String loadBalancerStrategyName,
                                                           TransportClient transportClient,
                                                           Clock clock,
                                                           boolean percentileTrackingEnabled,
                                                           boolean doNotSlowStart)
  {
    List<HttpStatusCodeRange> errorStatusCodeRanges = getErrorStatusRanges(serviceProperties);
    Predicate<Integer> isErrorStatus = (status) -> {
      for(HttpStatusCodeRange statusCodeRange : errorStatusCodeRanges)
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
                                 isErrorStatus,
                                 percentileTrackingEnabled,
                                 doNotSlowStart);
  }
}
