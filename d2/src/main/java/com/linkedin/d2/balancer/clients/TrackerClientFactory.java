package com.linkedin.d2.balancer.clients;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

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
   * Creates a {@link TrackerClient}.
   *
   * @param uri URI of the server for this client.
   * @param loadBalancerStrategyName Name of the strategy. eg "degrader"
   * @param serviceProperties Properties for the service this URI belongs to.
   * @param uriProperties URI properties.
   * @param transportClient Inner TransportClient.
   * @return TrackerClient
   */
  @Nullable
  public static TrackerClient createTrackerClient(URI uri,
                                           UriProperties uriProperties,
                                           ServiceProperties serviceProperties,
                                           String loadBalancerStrategyName,
                                           TransportClient transportClient)
  {
    TrackerClient trackerClient;

    switch (loadBalancerStrategyName)
    {
      case (DegraderLoadBalancerStrategyV3.DEGRADER_STRATEGY_NAME):
        trackerClient = createDegraderTrackerClient(uri, uriProperties, serviceProperties,  loadBalancerStrategyName, transportClient);
        break;
      default:
        trackerClient = createTrackerClientImpl(uri, uriProperties, serviceProperties, loadBalancerStrategyName, transportClient);
    }

    return trackerClient;
  }

  private static DegraderTrackerClient createDegraderTrackerClient(URI uri,
                                                            UriProperties uriProperties,
                                                            ServiceProperties serviceProperties,
                                                            String loadBalancerStrategyName,
                                                            TransportClient transportClient)
  {
    DegraderImpl.Config config = null;
    Clock clock = SystemClock.instance();

    if (serviceProperties.getDegraderProperties() != null && !serviceProperties.getDegraderProperties().isEmpty())
    {
      config = DegraderConfigFactory.toDegraderConfig(serviceProperties.getDegraderProperties());
      config.setLogger(new RateLimitedLogger(LOG, LOG_RATE_MS, clock));
    }

    if (serviceProperties.getLoadBalancerStrategyProperties() != null)
    {
      Map<String, Object> loadBalancerStrategyProperties =
        serviceProperties.getLoadBalancerStrategyProperties();
      clock = MapUtil.getWithDefault(loadBalancerStrategyProperties, PropertyKeys.CLOCK, SystemClock.instance(), Clock.class);
    }

    long trackerClientInterval = getInterval(loadBalancerStrategyName, serviceProperties);
    Pattern errorStatusPattern = getErrorStatusPattern(loadBalancerStrategyName, serviceProperties);

    return new DegraderTrackerClient(uri,
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

  private static Pattern getErrorStatusPattern(String loadBalancerStrategyName, ServiceProperties serviceProperties)
  {
    String regex = TrackerClientImpl.DEFAULT_ERROR_STATUS_REGEX;
    if (serviceProperties != null)
    {
      switch (loadBalancerStrategyName)
      {
        case (DegraderLoadBalancerStrategyV3.DEGRADER_STRATEGY_NAME):
          regex = MapUtil.getWithDefault(serviceProperties.getLoadBalancerStrategyProperties(),
                                           PropertyKeys.HTTP_LB_ERROR_STATUS_REGEX,
                                           TrackerClientImpl.DEFAULT_ERROR_STATUS_REGEX,
                                           String.class);
          break;
      }
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

  private static TrackerClientImpl createTrackerClientImpl(URI uri,
                                                    UriProperties uriProperties,
                                                    ServiceProperties serviceProperties,
                                                    String loadBalancerStrategyName,
                                                    TransportClient transportClient)
  {
    return new TrackerClientImpl(uri,
                                 uriProperties.getPartitionDataMap(uri),
                                 transportClient,
                                 SystemClock.instance(),
                                 getInterval(loadBalancerStrategyName, serviceProperties),
                                 getErrorStatusPattern(loadBalancerStrategyName, serviceProperties));
  }
}
