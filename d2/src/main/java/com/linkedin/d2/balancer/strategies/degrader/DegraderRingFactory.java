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

package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.d2.D2RingProperties;
import com.linkedin.d2.balancer.strategies.DelegatingRingFactory;


/**
 * Please use {@link com.linkedin.d2.balancer.strategies.DelegatingRingFactory} instead
 */
@Deprecated
public class DegraderRingFactory<T> extends DelegatingRingFactory<T>
{
  public static final String POINT_BASED_CONSISTENT_HASH = DelegatingRingFactory.POINT_BASED_CONSISTENT_HASH;
  public static final String MULTI_PROBE_CONSISTENT_HASH = DelegatingRingFactory.MULTI_PROBE_CONSISTENT_HASH;
  public static final String DISTRIBUTION_NON_HASH = DelegatingRingFactory.DISTRIBUTION_NON_HASH;

  public DegraderRingFactory(DegraderLoadBalancerStrategyConfig config) {
    super(config);
  }

  public DegraderRingFactory(D2RingProperties ringProperties) {
    super(ringProperties);
  }
}
