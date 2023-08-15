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

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.ClusterInfoItem;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.discovery.stores.file.FileStore;
import java.util.function.BiConsumer;


/**
 * No-op manager impl does nothing. Could be used when dual read load balancer is not in use.
 */
public class NoOpD2ClientJmxDualReadModeWatcherManagerImpl implements D2ClientJmxDualReadModeWatcherManager
{

  @Override
  public void updateWatcher(SimpleLoadBalancer balancer,
      BiConsumer<SimpleLoadBalancer, DualReadModeProvider.DualReadMode> callback)
  {
  }

  @Override
  public void updateWatcher(SimpleLoadBalancerState state,
      BiConsumer<SimpleLoadBalancerState, DualReadModeProvider.DualReadMode> callback)
  {
  }

  @Override
  public void updateWatcher(String serviceName, String scheme, LoadBalancerStrategy strategy,
      BiConsumer<LoadBalancerStrategy, DualReadModeProvider.DualReadMode> callback)
  {
  }

  @Override
  public void updateWatcher(String clusterName, ClusterInfoItem clusterInfoItem,
      BiConsumer<ClusterInfoItem, DualReadModeProvider.DualReadMode> callback)
  {
  }

  @Override
  public void updateWatcher(String serviceName, LoadBalancerStateItem<ServiceProperties> serviceProperties,
      BiConsumer<LoadBalancerStateItem<ServiceProperties>, DualReadModeProvider.DualReadMode> callback)
  {
  }

  @Override
  public void updateWatcherForFileStoreUriProperties(FileStore<UriProperties> uriStore,
      BiConsumer<FileStore<UriProperties>, DualReadModeProvider.DualReadMode> callback)
  {
  }

  @Override
  public void updateWatcherForFileStoreClusterProperties(FileStore<ClusterProperties> clusterStore,
      BiConsumer<FileStore<ClusterProperties>, DualReadModeProvider.DualReadMode> callback)
  {
  }

  @Override
  public void updateWatcherForFileStoreServiceProperties(FileStore<ServiceProperties> serviceStore,
      BiConsumer<FileStore<ServiceProperties>, DualReadModeProvider.DualReadMode> callback)
  {
  }

  @Override
  public void removeWatcherForLoadBalancerStrategy(String serviceName, String scheme)
  {
  }

  @Override
  public void removeWatcherForClusterInfoItem(String clusterName)
  {
  }

  @Override
  public void removeWatcherForServiceProperties(String serviceName)
  {
  }
}
