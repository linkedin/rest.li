package com.linkedin.darkcluster;

import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.r2.transport.common.TransportClientFactory;

/**
 * MockFacilities is needed because the ClusterInfoProvider isn't available in the D2 client until
 * after start, because the loadBalancer isn't available til then. To get around this, store the
 * pointer to the Facilities.
 */
public class MockFacilities implements Facilities
{
  private final ClusterInfoProvider _clusterInfoProvider;

  public MockFacilities(ClusterInfoProvider clusterInfoProvider)
  {
    _clusterInfoProvider = clusterInfoProvider;
  }

  @Override
  public Directory getDirectory()
  {
    return null;
  }

  @Override
  public PartitionInfoProvider getPartitionInfoProvider()
  {
    return null;
  }

  @Override
  public HashRingProvider getHashRingProvider()
  {
    return null;
  }

  @Override
  public KeyMapper getKeyMapper()
  {
    return null;
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    return null;
  }

  @Override
  public ClusterInfoProvider getClusterInfoProvider()
  {
    return _clusterInfoProvider;
  }
}
