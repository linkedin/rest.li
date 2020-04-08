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

package com.linkedin.d2.balancer.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.d2.balancer.LoadBalancerClusterListener;

public class MockClusterListener implements LoadBalancerClusterListener
{
  // Store if notified
  private Map<String, AtomicInteger> clusterAddedCounter = new HashMap<>();
  private Map<String, AtomicInteger> clusterRemovedCounter = new HashMap<>();

  @Override
  public void onClusterAdded(String clusterName)
  {
    AtomicInteger counter = clusterAddedCounter.computeIfAbsent(clusterName, (name) -> new AtomicInteger());
    counter.incrementAndGet();
  }

  @Override
  public void onClusterRemoved(String clusterName)
  {
    AtomicInteger counter = clusterRemovedCounter.computeIfAbsent(clusterName, (name) -> new AtomicInteger());
    counter.incrementAndGet();
  }

  public int getClusterAddedCount(String clusterName)
  {
    return clusterAddedCounter.getOrDefault(clusterName, new AtomicInteger(0)).intValue();
  }

  public int getClusterRemovedCount(String clusterName)
  {
    return clusterRemovedCounter.getOrDefault(clusterName, new AtomicInteger(0)).intValue();
  }
}
