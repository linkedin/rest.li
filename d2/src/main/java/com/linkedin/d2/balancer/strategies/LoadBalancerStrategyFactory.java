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

import java.util.Map;

/**
 * Factory for LoadBalancerStrategies.  The factory is expected to be immutable.
 * @param <T>
 */
public interface LoadBalancerStrategyFactory<T extends LoadBalancerStrategy>
{
  /**
   * Creates a new LoadBalancer for a service
   * @param serviceName The service name
   * @param strategyProperties The load balancer strategy properties specified in the service
   * configuration; may be empty.  The semantics of the properties are defined by the particular
   * load balancer strategy receiving the map.  The values of the map are either Strings or nested
   * structures (Lists or Maps); any nested structures will obey the same restriction.
   * @return The LoadBalancer
   */
  T newLoadBalancer(String serviceName, Map<String, Object> strategyProperties);
}
