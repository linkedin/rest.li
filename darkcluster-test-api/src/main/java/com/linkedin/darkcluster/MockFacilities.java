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
