/*
   Copyright (c) 2017 LinkedIn Corp.

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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.Lock;


class Partition
{
  private final int _id;
  private final Lock _lock;
  private volatile PartitionDegraderLoadBalancerState _state;
  private final Set<PartitionDegraderLoadBalancerStateListener> _listeners;

  Partition(int id, Lock lock, PartitionDegraderLoadBalancerState state,
      Set<PartitionDegraderLoadBalancerStateListener> listeners)
  {
    _id = id;
    _lock = lock;
    _state = state;
    _listeners = listeners;
  }

  public int getId()
  {
    return _id;
  }

  /** this controls access to updatePartitionState for each partition:
   * only one thread should update the state for a particular partition at any one time.
   */
  public Lock getLock()
  {
    return _lock;
  }

  public PartitionDegraderLoadBalancerState getState()
  {
    return _state;
  }

  public void setState(PartitionDegraderLoadBalancerState state)
  {
    _state = state;
  }

  public Set<PartitionDegraderLoadBalancerStateListener> getListeners()
  {
    return Collections.unmodifiableSet(_listeners);
  }

  @Override
  public String toString()
  {
    return String.valueOf(_state);
  }
}
