/*
   Copyright (c) 2016 LinkedIn Corp.

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

import com.linkedin.util.degrader.CallTracker;
import java.util.Map;

import com.linkedin.d2.balancer.util.hashing.Ring;


/**
 * Factory to generate consistent hash ring with the given points for each object
 *
 */
public interface RingFactory<T> {

  Ring<T> createRing(Map<T, Integer> pointsMap);

  /**
   * Creates a hash ring with the given points and {@link CallTracker} for each object.
   *
   * @param pointsMap       A map between object to store in the ring and its points. The more points
   *                        one has, the higher its weight is.
   * @param callTrackerMap  A map between object to store in the ring and its {@link CallTracker}. The ring might
   *                        need call tracking information to pick the desired object
   * @return  a {@link Ring}
   */
  default Ring<T> createRing(Map<T, Integer> pointsMap, Map<T, CallTracker> callTrackerMap) {
    return createRing(pointsMap);
  }
}
