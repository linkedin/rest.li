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
import java.util.concurrent.atomic.AtomicReference;


public class DualReadLoadBalancerJmx implements DualReadLoadBalancerJmxMBean
{
  private final AtomicInteger _servicePropertiesErrorCount = new AtomicInteger();
  private final AtomicInteger _clusterPropertiesErrorCount = new AtomicInteger();

  private final AtomicInteger _servicePropertiesEvictCount = new AtomicInteger();
  private final AtomicInteger _clusterPropertiesEvictCount = new AtomicInteger();

  private final AtomicInteger _servicePropertiesOutOfSyncCount = new AtomicInteger();
  private final AtomicInteger _clusterPropertiesOutOfSyncCount = new AtomicInteger();

  private final AtomicReference<Double> _uriPropertiesSimilarity = new AtomicReference<>(0d);


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

  @Deprecated
  @Override
  public int getUriPropertiesErrorCount()
  {
    return 0;
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

  @Deprecated
  @Override
  public int getUriPropertiesEvictCount()
  {
    return 0;
  }

  @Override
  public int getServicePropertiesOutOfSyncCount()
  {
    return _servicePropertiesOutOfSyncCount.get();
  }

  @Override
  public int getClusterPropertiesOutOfSyncCount()
  {
    return _clusterPropertiesOutOfSyncCount.get();
  }

  @Deprecated
  @Override
  public int getUriPropertiesOutOfSyncCount()
  {
    return 0;
  }

  @Override
  public double getUriPropertiesSimilarity()
  {
    return _uriPropertiesSimilarity.get();
  }

  public void incrementServicePropertiesErrorCount()
  {
    _servicePropertiesErrorCount.incrementAndGet();
  }

  public void incrementClusterPropertiesErrorCount()
  {
    _clusterPropertiesErrorCount.incrementAndGet();
  }

  public void incrementServicePropertiesEvictCount()
  {
    _servicePropertiesEvictCount.incrementAndGet();
  }

  public void incrementClusterPropertiesEvictCount()
  {
    _clusterPropertiesEvictCount.incrementAndGet();
  }

  public void incrementServicePropertiesOutOfSyncCount()
  {
    _servicePropertiesOutOfSyncCount.incrementAndGet();
  }

  public void incrementClusterPropertiesOutOfSyncCount()
  {
    _clusterPropertiesOutOfSyncCount.incrementAndGet();
  }

  public void decrementServicePropertiesOutOfSyncCount()
  {
    _servicePropertiesOutOfSyncCount.decrementAndGet();
  }

  public void decrementClusterPropertiesOutOfSyncCount()
  {
    _clusterPropertiesOutOfSyncCount.decrementAndGet();
  }

  public void setUriPropertiesSimilarity(double similarity)
  {
    _uriPropertiesSimilarity.set(similarity);
  }
}