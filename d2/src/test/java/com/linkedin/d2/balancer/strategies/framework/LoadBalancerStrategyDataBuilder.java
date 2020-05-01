package com.linkedin.d2.balancer.strategies.framework;

import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategy;
import com.linkedin.d2.loadBalancerStrategyType;
import java.util.HashMap;
import java.util.Map;


/**
 * Builder class that helps build different types of {@link LoadBalancerStrategy} with less effort
 */
class LoadBalancerStrategyDataBuilder {
  private final loadBalancerStrategyType _type;
  private final String _serviceName;
  private final Map<String, Object> _strategyProperties;

  public LoadBalancerStrategyDataBuilder(final loadBalancerStrategyType type, final String serviceName,
      final Map<String, Object> strategyProperties) {
    _type = type;
    _serviceName = serviceName;
    _strategyProperties = strategyProperties;
  }

  public LoadBalancerStrategy build() {
    switch (_type) {
      case DEGRADER:
        // TODO: Change the StrategyV3 constructor
        return new DegraderLoadBalancerStrategyFactoryV3().newLoadBalancer(_serviceName, _strategyProperties, new HashMap<>());
      case RANDOM:
      default:
        return new RandomLoadBalancerStrategy();
    }
  }
}
