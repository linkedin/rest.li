package com.linkedin.darkcluster.impl;

import com.linkedin.common.util.Notifier;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.darkcluster.api.BaseDarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import java.util.Random;


/**
 * The high level goal of this strategy is to send identical requests to all dark clusters configured with this strategy.
 * However, we also ensure that the level of traffic is proportional to itself on any instance in dark cluster accounting
 * for the multiplier. In order to ensure this, it uses the same logic as {@link RelativeTrafficMultiplierDarkClusterStrategy}
 * to determine the traffic QPS to dark clusters
 */
public class IdenticalTrafficMultiplierDarkClusterStrategy implements DarkClusterStrategy {
  private final String _originalClusterName;
  private final String _darkClusterName;
  private final Float _multiplier;
  private final BaseDarkClusterDispatcher _baseDarkClusterDispatcher;
  private final Notifier _notifier;
  private final Random _random;
  private final ClusterInfoProvider _clusterInfoProvider;

  private static final String RANDOM_NUMBER_KEY = "identicalTrafficMultiplier.randomNumber";

  public IdenticalTrafficMultiplierDarkClusterStrategy(String sourceClusterName,
      String darkClusterName,
      Float multiplier,
      BaseDarkClusterDispatcher baseDarkClusterDispatcher,
      Notifier notifier,
      ClusterInfoProvider clusterInfoProvider, Random random)
  {
    _originalClusterName = sourceClusterName;
    _darkClusterName = darkClusterName;
    _multiplier = multiplier;
    _baseDarkClusterDispatcher = baseDarkClusterDispatcher;
    _notifier = notifier;
    _random = random;
    _clusterInfoProvider = clusterInfoProvider;
  }

  @Override
  public boolean handleRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext)
  {
    int numRequestDuplicates = getNumDuplicateRequests(requestContext);
    return _baseDarkClusterDispatcher.sendRequest(originalRequest, darkRequest, requestContext, numRequestDuplicates);
  }

  /**
   * We won't create this strategy if this config isn't valid for this strategy. For instance, we don't want to create
   * the IdenticalTrafficMultiplierDarkClusterStrategy if there's no multiplier or if the multiplier is zero, because we'd
   * be doing pointless work on every getOrCreate. Instead if will go to the next strategy (or NoOpDarkClusterStrategy).
   *
   * This is a static method defined here because we don't want to instantiate a strategy to check this. It cannot be a
   * method that is on the interface because static methods on an interface cannot be overridden by implementations.
   * @param darkClusterConfig
   * @return true if config is valid for this strategy
   */
  public static boolean isValidConfig(DarkClusterConfig darkClusterConfig)
  {
    return darkClusterConfig.hasMultiplier() && darkClusterConfig.getMultiplier() > 0;
  }

  /**
   * The high level goal of this strategy is to send identical traffic to all the dark clusters configured with this
   * strategy. It accomplishes this by persisting the random number generated for a request in {@link RequestContext}
   * and reusing the same so that if a request is chosen to be sent to one dark cluster, it will be sent for all other
   * dark clusters as well.
   *
   * The logic to determine if a request should be sent to dark cluster or not for the first time is determined similar
   * to {@link RelativeTrafficMultiplierDarkClusterStrategy}
   *
   * Example 1:
   * There are 3 dark clusters: A, B and C all of which are configured with same multiplier of 0.1.
   * There is 1 source instance and 1 dark instance in each cluster.
   * Assume that the strategy is called for A, B and C in the same order.
   * For A, there will be no random number persisted in requestContext since we're seeing this request for the first time
   * So we compute random number, say 0.05 and persist the same in requestContext
   * Avg#DarkRequests = 1 * 0.1 / 1 = 0.1
   * since 0.05 < 0.1, request will be sent to A
   * When it comes to B, the random number is already present and since it is < 0.1, request will be sent to B
   * When it comes to C, the random number is already present and since it is < 0.1, request will be sent to C
   * Note that the above logic works regardless of the order in which the 3 dark clusters are called.
   *
   * Example 2:
   * There are 3 dark clusters: A, B and C with multipliers 0.1, 0.2, 0.3 respectively.
   * There is 1 source instance and 1 dark instance in each cluster.
   * Assume that the strategy is called for A, B and C in the same order.
   * For A, there will be no random number persisted in requestContext since we're seeing this request for the first time
   * So we compute random number, say 0.15 and persist the same in requestContext
   * Avg#DarkRequests = 1 * 0.2 / 1 = 0.2
   * since 0.15 < 0.1, so request will NOT be sent to A
   * When it comes to B, the random number is already present and since it is < 0.2, request will be sent to A
   * When it comes to C, the random number is already present and since it is < 0.3, request will be sent to C
   * Note that in this case, we are sending identical requests between B and C but since A happened to have a smaller multiplier,
   * it was not sent to it.
   * This would also work regardless of the order in which the 3 dark clusters are called
   */
  private int getNumDuplicateRequests(RequestContext requestContext)
  {
    try
    {
      // Only support https for now. http support can be added later if truly needed, but would be non-ideal
      // because potentially both dark and source would have to be configured.
      int numDarkClusterInstances = _clusterInfoProvider.getHttpsClusterCount(_darkClusterName);
      int numSourceClusterInstances = _clusterInfoProvider.getHttpsClusterCount(_originalClusterName);
      float randomNumber;
      if (requestContext.getLocalAttr(RANDOM_NUMBER_KEY) == null)
      {
        randomNumber = _random.nextFloat();
        requestContext.putLocalAttr(RANDOM_NUMBER_KEY, randomNumber);
      } else
      {
        randomNumber = (float) requestContext.getLocalAttr(RANDOM_NUMBER_KEY);
      }
      if (numSourceClusterInstances != 0)
      {
        float avgNumDarkRequests = (numDarkClusterInstances * _multiplier) / numSourceClusterInstances;
        float avgDarkDecimalPart = avgNumDarkRequests % 1;
        return randomNumber < avgDarkDecimalPart ? ((int)avgNumDarkRequests) + 1 : (int)avgNumDarkRequests;
      }

      return 0;
    }
    catch (ServiceUnavailableException e)
    {
      _notifier.notify(() -> new RuntimeException("PEGA_0020 unable to compute strategy for source cluster: "
          + _originalClusterName + ", darkClusterName: " + _darkClusterName, e));
      // safe thing is to return 0 so dark traffic isn't sent.
      return 0;
    }
  }
}
