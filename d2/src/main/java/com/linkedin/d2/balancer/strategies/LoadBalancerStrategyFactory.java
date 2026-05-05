/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies;

import com.linkedin.d2.balancer.properties.ServiceProperties;

/**
 * Factory for LoadBalancerStrategies.  The factory is expected to be immutable.
 * @param <T>
 */
public interface LoadBalancerStrategyFactory<T extends LoadBalancerStrategy>
{
  /**
   * Create new {@link LoadBalancerStrategy} for a service.
   *
   * @param serviceProperties {@link ServiceProperties}.
   * @return Load balancer strategy.
   */
  T newLoadBalancer(ServiceProperties serviceProperties);

  /**
   * Create new {@link LoadBalancerStrategy} for a service, with the URI scheme it will serve known
   * up front. Strategies that emit OpenTelemetry metrics tagged by scheme should override this
   * overload so the scheme is set at construction time, eliminating the bootstrap window between
   * {@link #newLoadBalancer(ServiceProperties)} and
   * {@code D2ClientJmxManager.doRegisterLoadBalancerStrategy(...)} during which per-call listener
   * emissions and gauge updates would otherwise be silently dropped.
   *
   * <p>The default implementation delegates to {@link #newLoadBalancer(ServiceProperties)} for
   * backward source compatibility with factories (including out-of-tree implementations) that
   * have not been migrated.
   *
   * @param serviceProperties {@link ServiceProperties}.
   * @param scheme            URI scheme this strategy instance will serve (e.g. {@code "http"} or
   *                          {@code "https"}); may be {@code null} if unknown at the call site.
   * @return Load balancer strategy.
   */
  default T newLoadBalancer(ServiceProperties serviceProperties, String scheme)
  {
    T strategy = newLoadBalancer(serviceProperties);
    if (strategy instanceof SchemeAware && scheme != null)
    {
      ((SchemeAware) strategy).setScheme(scheme);
    }
    return strategy;
  }
}
