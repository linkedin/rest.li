/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.util;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clusterfailout.ClusterFailoutConfig;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.transport.common.TransportClientFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class DelegatingFacilities implements Facilities
{
  private final DirectoryProvider _directoryProvider;
  private final KeyMapperProvider _keyMapperProvider;
  private final ClientFactoryProvider _clientFactoryProvider;
  private final PartitionInfoProvider _partitionInfoProvider;
  private final HashRingProvider _hashRingProvider;
  private final ClusterInfoProvider _clusterInfoProvider;

  @Deprecated
  public DelegatingFacilities(DirectoryProvider directoryProvider,
                              KeyMapperProvider keyMapperProvider,
                              ClientFactoryProvider clientFactoryProvider)
  {
    this(directoryProvider, keyMapperProvider, clientFactoryProvider, new PartitionInfoProvider()
    {
      @Override
      public <K> HostToKeyMapper<K> getPartitionInformation(URI serviceUri, Collection<K> keys,
          int limitHostPerPartition, int hash) throws ServiceUnavailableException
      {
        return null;
      }

      @Override
      public PartitionAccessor getPartitionAccessor(String serviceName) throws ServiceUnavailableException
      {
        return null;
      }
    }, new HashRingProvider()
    {
      @Override
      public <K> MapKeyResult<Ring<URI>, K> getRings(URI serviceUri, Iterable<K> keys)
          throws ServiceUnavailableException
      {
        return null;
      }

      @Override
      public Map<Integer, Ring<URI>> getRings(URI serviceUri) throws ServiceUnavailableException
      {
        return null;
      }

      @Override
      public HashFunction<Request> getRequestHashFunction(String serviceName) throws ServiceUnavailableException
      {
        return null;
      }
    });
  }

  @Deprecated
  public DelegatingFacilities(DirectoryProvider directoryProvider,
                              KeyMapperProvider keyMapperProvider,
                              ClientFactoryProvider clientFactoryProvider,
                              PartitionInfoProvider partitionInfoProvider,
                              HashRingProvider hashRingProvider)
  {
    this(directoryProvider, keyMapperProvider, clientFactoryProvider, partitionInfoProvider, hashRingProvider,
      new ClusterInfoProvider()
      {
        @Override
        public int getClusterCount(String clusterName, String scheme, int partitionId)
        {
          return 0;
        }

        @Override
        public void getDarkClusterConfigMap(String clusterName, Callback<DarkClusterConfigMap> callback)
        {
        }

        @Override
        public ClusterFailoutConfig getClusterFailoutConfig(String clusterName)
        {
          return null;
        }
      });
  }

  public DelegatingFacilities(DirectoryProvider directoryProvider,
      KeyMapperProvider keyMapperProvider,
      ClientFactoryProvider clientFactoryProvider,
      PartitionInfoProvider partitionInfoProvider,
      HashRingProvider hashRingProvider,
      ClusterInfoProvider clusterInfoProvider)
  {
    _directoryProvider = directoryProvider;
    _keyMapperProvider = keyMapperProvider;
    _clientFactoryProvider = clientFactoryProvider;
    _partitionInfoProvider = partitionInfoProvider;
    _hashRingProvider = hashRingProvider;
    _clusterInfoProvider = clusterInfoProvider;
  }

  @Override
  public Directory getDirectory()
  {
    return _directoryProvider.getDirectory();
  }

  @Override
  public PartitionInfoProvider getPartitionInfoProvider ()
  {
    return _partitionInfoProvider;
  }

  @Override
  public HashRingProvider getHashRingProvider()
  {
    return _hashRingProvider;
  }

  @Override
  public KeyMapper getKeyMapper()
  {
    return _keyMapperProvider.getKeyMapper();
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    return _clientFactoryProvider.getClientFactory(scheme);
  }

  @Override
  public ClusterInfoProvider getClusterInfoProvider() {
    return _clusterInfoProvider;
  }
}
