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
}
