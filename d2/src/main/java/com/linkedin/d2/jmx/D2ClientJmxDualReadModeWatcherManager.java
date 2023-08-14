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
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.ClusterInfoItem;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.discovery.stores.file.FileStore;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;


/**
 * Manage d2 client jmx dual read mode watchers for different types of load balancing related properties.
 */
public interface D2ClientJmxDualReadModeWatcherManager
{

  void updateWatcher(SimpleLoadBalancer balancer, BiConsumer<SimpleLoadBalancer, DualReadModeProvider.DualReadMode> callback);

  void updateWatcher(SimpleLoadBalancerState state, BiConsumer<SimpleLoadBalancerState, DualReadModeProvider.DualReadMode> callback);

  void updateWatcher(String serviceName, String scheme, LoadBalancerStrategy strategy,
      BiConsumer<LoadBalancerStrategy, DualReadModeProvider.DualReadMode> callback);

  void updateWatcher(String clusterName, ClusterInfoItem clusterInfoItem,
      BiConsumer<ClusterInfoItem, DualReadModeProvider.DualReadMode> callback);

  void updateWatcher(String serviceName, LoadBalancerStateItem<ServiceProperties> serviceProperties,
      BiConsumer<LoadBalancerStateItem<ServiceProperties>, DualReadModeProvider.DualReadMode> callback);

  void updateWatcherForFileStoreUriProperties(FileStore<UriProperties> uriStore,
      BiConsumer<FileStore<UriProperties>, DualReadModeProvider.DualReadMode> callback);

  void updateWatcherForFileStoreClusterProperties(FileStore<ClusterProperties> clusterStore,
      BiConsumer<FileStore<ClusterProperties>, DualReadModeProvider.DualReadMode> callback);

  void updateWatcherForFileStoreServiceProperties(FileStore<ServiceProperties> serviceStore,
      BiConsumer<FileStore<ServiceProperties>, DualReadModeProvider.DualReadMode> callback);

  void removeWatcherForLoadBalancerStrategy(String serviceName, String scheme);

  void removeWatcherForClusterInfoItem(String clusterName);

  void removeWatcherForServiceProperties(String serviceName);


  final class D2ClientJmxDualReadModeWatcher<T> implements DualReadStateManager.DualReadModeWatcher
  {
    private T _latestJmxProperty;
    private final BiConsumer<T, DualReadModeProvider.DualReadMode> _callback;

    D2ClientJmxDualReadModeWatcher(T initialJmxProperty, BiConsumer<T, DualReadModeProvider.DualReadMode>callback)
    {
      _latestJmxProperty = initialJmxProperty;
      _callback = callback;
    }

    public T getLatestJmxProperty()
    {
      return _latestJmxProperty;
    }

    public void setLatestJmxProperty(T latestJmxProperty)
    {
      _latestJmxProperty = latestJmxProperty;
    }

    @Override
    public void onChanged(@Nonnull DualReadModeProvider.DualReadMode mode)
    {
      _callback.accept(_latestJmxProperty, mode);
    }
  }
}
