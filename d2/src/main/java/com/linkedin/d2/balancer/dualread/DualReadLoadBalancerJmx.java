/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.balancer.dualread;

import java.util.concurrent.atomic.AtomicInteger;


public class DualReadLoadBalancerJmx implements DualReadLoadBalancerJmxMBean
{
  private final AtomicInteger _servicePropertiesErrorCount = new AtomicInteger();
  private final AtomicInteger _clusterPropertiesErrorCount = new AtomicInteger();
  private final AtomicInteger _uriPropertiesErrorCount = new AtomicInteger();

  private final AtomicInteger _servicePropertiesEvictCount = new AtomicInteger();
  private final AtomicInteger _clusterPropertiesEvictCount = new AtomicInteger();
  private final AtomicInteger _uriPropertiesEvictCount = new AtomicInteger();

  private final AtomicInteger _servicePropertiesOutOfSyncCount = new AtomicInteger();
  private final AtomicInteger _clusterPropertiesOutOfSyncCount = new AtomicInteger();
  private final AtomicInteger _uriPropertiesOutOfSyncCount = new AtomicInteger();


  @Override
  public int getServicePropertiesErrorCount()
  {
    return _servicePropertiesErrorCount.get();
  }

  @Override
  public int getClusterPropertiesErrorCount()
  {
    return _clusterPropertiesErrorCount.get();
  }

  @Override
  public int getUriPropertiesErrorCount()
  {
    return _uriPropertiesErrorCount.get();
  }

  @Override
  public int getServicePropertiesEvictCount()
  {
    return _servicePropertiesEvictCount.get();
  }

  @Override
  public int getClusterPropertiesEvictCount()
  {
    return _clusterPropertiesEvictCount.get();
  }

  @Override
  public int getUriPropertiesEvictCount()
  {
    return _uriPropertiesEvictCount.get();
  }

  @Override
  public int getServicePropertiesOutOfSyncCount() {
    return _servicePropertiesOutOfSyncCount.get();
  }

  @Override
  public int getClusterPropertiesOutOfSyncCount() {
    return _clusterPropertiesOutOfSyncCount.get();
  }

  @Override
  public int getUriPropertiesOutOfSyncCount() {
    return _uriPropertiesOutOfSyncCount.get();
  }

  public void incrementServicePropertiesErrorCount()
  {
    _servicePropertiesErrorCount.incrementAndGet();
  }

  public void incrementClusterPropertiesErrorCount()
  {
    _clusterPropertiesErrorCount.incrementAndGet();
  }

  public void incrementUriPropertiesErrorCount()
  {
    _uriPropertiesErrorCount.incrementAndGet();
  }

  public void incrementServicePropertiesEvictCount()
  {
    _servicePropertiesEvictCount.incrementAndGet();
  }

  public void incrementClusterPropertiesEvictCount()
  {
    _clusterPropertiesEvictCount.incrementAndGet();
  }

  public void incrementUriPropertiesEvictCount()
  {
    _uriPropertiesEvictCount.incrementAndGet();
  }

  public void incrementServicePropertiesOutOfSyncCount()
  {
    _servicePropertiesOutOfSyncCount.incrementAndGet();
  }

  public void incrementClusterPropertiesOutOfSyncCount()
  {
    _clusterPropertiesOutOfSyncCount.incrementAndGet();
  }

  public void incrementUriPropertiesOutOfSyncCount()
  {
    _uriPropertiesOutOfSyncCount.incrementAndGet();
  }

  public void decrementServicePropertiesOutOfSyncCount()
  {
    _servicePropertiesOutOfSyncCount.decrementAndGet();
  }

  public void decrementClusterPropertiesOutOfSyncCount()
  {
    _clusterPropertiesOutOfSyncCount.decrementAndGet();
  }

  public void decrementUriPropertiesOutOfSyncCount()
  {
    _uriPropertiesOutOfSyncCount.decrementAndGet();
  }
}
