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

package com.linkedin.d2.balancer.strategies.relative;

import java.net.URI;
import java.util.Map;


/**
 * The helper class for {@link RelativeLoadBalancerStrategy} related tests
 */
public class RelativeLoadBalancerTestHelper {

  /**
   * Get points map for a given partition
   *
   * @param strategy The object of the strategy
   * @param partitionId The id of the partition
   * @return The points map
   */
  public static Map<URI, Integer> getPointsMap(RelativeLoadBalancerStrategy strategy, int partitionId)
  {
    return strategy.getPointsMap(partitionId);
  }
}
