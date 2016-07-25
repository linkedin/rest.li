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

package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.d2.balancer.util.hashing.MPConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.Ring;
import java.util.Map;


/**
 * A ring factory generates {@link MPConsistentHashRing}s.
 *
 * @author Ang Xu
 */
public class MPConsistentHashRingFactory<T> implements RingFactory<T>
{
  private final int _numProbes;

  public MPConsistentHashRingFactory(int numProbes)
  {
    _numProbes = numProbes;
  }

  @Override
  public Ring<T> createRing(Map<T, Integer> points)
  {
    return new MPConsistentHashRing<>(points, _numProbes);
  }
}
