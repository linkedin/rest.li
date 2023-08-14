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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;


/**
 * Default watcher manager impl that add/update/remove watchers.
 */
public class DefaultD2ClientJmxDualReadModeWatcherManagerImpl implements D2ClientJmxDualReadModeWatcherManager
{

  private final DualReadStateManager _dualReadStateManager;

  private D2ClientJmxDualReadModeWatcher<SimpleLoadBalancer> _lbDualReadModeWatcher;
  private D2ClientJmxDualReadModeWatcher<SimpleLoadBalancerState> _lbStateDualReadModeWatcher;
  private D2ClientJmxDualReadModeWatcher<FileStore<UriProperties>> _fileStoreUriPropertiesDualReadModeWatcher;
  private D2ClientJmxDualReadModeWatcher<FileStore<ClusterProperties>> _fileStoreClusterPropertiesDualReadModeWatcher;
  private D2ClientJmxDualReadModeWatcher<FileStore<ServiceProperties>> _fileStoreServicePropertiesDualReadModeWatcher;
  private final ConcurrentMap<String, D2ClientJmxDualReadModeWatcher<LoadBalancerStateItem<ServiceProperties>>>
      _servicePropertiesDualReadModeWatchers;
  private final ConcurrentMap<String, D2ClientJmxDualReadModeWatcher<LoadBalancerStrategy>> _lbStrategyDualReadModeWatchers;
  private final ConcurrentMap<String, D2ClientJmxDualReadModeWatcher<ClusterInfoItem>> _clusterInfoDualReadModeWatchers;

  public DefaultD2ClientJmxDualReadModeWatcherManagerImpl(@Nonnull DualReadStateManager dualReadStateManager)
  {
    _dualReadStateManager = dualReadStateManager;
    _lbDualReadModeWatcher = null;
    _lbStateDualReadModeWatcher = null;
    _fileStoreUriPropertiesDualReadModeWatcher = null;
    _fileStoreClusterPropertiesDualReadModeWatcher = null;
    _fileStoreServicePropertiesDualReadModeWatcher = null;
    _servicePropertiesDualReadModeWatchers = new ConcurrentHashMap<>();
    _lbStrategyDualReadModeWatchers = new ConcurrentHashMap<>();
    _clusterInfoDualReadModeWatchers = new ConcurrentHashMap<>();
  }

  public void updateWatcher(SimpleLoadBalancer balancer, BiConsumer<SimpleLoadBalancer, DualReadModeProvider.DualReadMode> callback)
  {
    if (_lbDualReadModeWatcher == null)
    {
      _lbDualReadModeWatcher = new D2ClientJmxDualReadModeWatcher<>(balancer, callback);
      _dualReadStateManager.addGlobalWatcher(_lbDualReadModeWatcher);
    }
    _lbDualReadModeWatcher.setLatestJmxProperty(balancer);
  }

  public void updateWatcher(SimpleLoadBalancerState state, BiConsumer<SimpleLoadBalancerState, DualReadModeProvider.DualReadMode> callback)
  {
    if (_lbStateDualReadModeWatcher == null)
    {
      _lbStateDualReadModeWatcher = new D2ClientJmxDualReadModeWatcher<>(state, callback);
      _dualReadStateManager.addGlobalWatcher(_lbStateDualReadModeWatcher);
    }
    _lbStateDualReadModeWatcher.setLatestJmxProperty(state);
  }

  public void updateWatcher(String serviceName, String scheme, LoadBalancerStrategy strategy,
      BiConsumer<LoadBalancerStrategy, DualReadModeProvider.DualReadMode> callback)
  {
    D2ClientJmxDualReadModeWatcher<LoadBalancerStrategy> currentWatcher =
        _lbStrategyDualReadModeWatchers.computeIfAbsent(getWatcherNameForLoadBalancerStrategy(serviceName, scheme), k ->
        {
          D2ClientJmxDualReadModeWatcher<LoadBalancerStrategy> watcher = new D2ClientJmxDualReadModeWatcher<>(strategy, callback);
          _dualReadStateManager.addServiceWatcher(serviceName, watcher);
          return watcher;
        });
    currentWatcher.setLatestJmxProperty(strategy);
  }

  public void updateWatcher(String clusterName, ClusterInfoItem clusterInfoItem,
      BiConsumer<ClusterInfoItem, DualReadModeProvider.DualReadMode> callback)
  {
    D2ClientJmxDualReadModeWatcher<ClusterInfoItem> currentWatcher =
        _clusterInfoDualReadModeWatchers.computeIfAbsent(clusterName, k ->
        {
          D2ClientJmxDualReadModeWatcher<ClusterInfoItem> watcher = new D2ClientJmxDualReadModeWatcher<>(clusterInfoItem, callback);
          _dualReadStateManager.addClusterWatcher(clusterName, watcher);
          return watcher;
        });
    currentWatcher.setLatestJmxProperty(clusterInfoItem);
  }

  public void updateWatcher(String serviceName, LoadBalancerStateItem<ServiceProperties> serviceProperties,
      BiConsumer<LoadBalancerStateItem<ServiceProperties>, DualReadModeProvider.DualReadMode> callback)
  {
    D2ClientJmxDualReadModeWatcher<LoadBalancerStateItem<ServiceProperties>> currentWatcher =
        _servicePropertiesDualReadModeWatchers.computeIfAbsent(serviceName, k ->
        {
          D2ClientJmxDualReadModeWatcher<LoadBalancerStateItem<ServiceProperties>> watcher =
              new D2ClientJmxDualReadModeWatcher<>(serviceProperties, callback);
          _dualReadStateManager.addServiceWatcher(serviceName, watcher);
          return watcher;
        });
    currentWatcher.setLatestJmxProperty(serviceProperties);
  }

  public void updateWatcherForFileStoreUriProperties(FileStore<UriProperties> uriStore,
      BiConsumer<FileStore<UriProperties>, DualReadModeProvider.DualReadMode> callback)
  {
    if (_fileStoreUriPropertiesDualReadModeWatcher == null)
    {
      _fileStoreUriPropertiesDualReadModeWatcher = new D2ClientJmxDualReadModeWatcher<>(uriStore, callback);
      _dualReadStateManager.addGlobalWatcher(_fileStoreUriPropertiesDualReadModeWatcher);
    }
    _fileStoreUriPropertiesDualReadModeWatcher.setLatestJmxProperty(uriStore);
  }

  public void updateWatcherForFileStoreClusterProperties(FileStore<ClusterProperties> clusterStore,
      BiConsumer<FileStore<ClusterProperties>, DualReadModeProvider.DualReadMode> callback)
  {
    if (_fileStoreClusterPropertiesDualReadModeWatcher == null)
    {
      _fileStoreClusterPropertiesDualReadModeWatcher = new D2ClientJmxDualReadModeWatcher<>(clusterStore, callback);
      _dualReadStateManager.addGlobalWatcher(_fileStoreClusterPropertiesDualReadModeWatcher);
    }
    _fileStoreClusterPropertiesDualReadModeWatcher.setLatestJmxProperty(clusterStore);
  }

  public void updateWatcherForFileStoreServiceProperties(FileStore<ServiceProperties> serviceStore,
      BiConsumer<FileStore<ServiceProperties>, DualReadModeProvider.DualReadMode> callback)
  {
    if (_fileStoreServicePropertiesDualReadModeWatcher == null)
    {
      _fileStoreServicePropertiesDualReadModeWatcher = new D2ClientJmxDualReadModeWatcher<>(serviceStore, callback);
      _dualReadStateManager.addGlobalWatcher(_fileStoreServicePropertiesDualReadModeWatcher);
    }
    _fileStoreServicePropertiesDualReadModeWatcher.setLatestJmxProperty(serviceStore);
  }

  public void removeWatcherForLoadBalancerStrategy(String serviceName, String scheme)
  {
    DualReadStateManager.DualReadModeWatcher watcher = _lbStrategyDualReadModeWatchers.remove(
        getWatcherNameForLoadBalancerStrategy(serviceName, scheme));
    if (watcher != null)
    {
      _dualReadStateManager.removeServiceWatcher(serviceName, watcher);
    }
  }

  public void removeWatcherForClusterInfoItem(String clusterName)
  {
    DualReadStateManager.DualReadModeWatcher watcher = _clusterInfoDualReadModeWatchers.remove(clusterName);
    if (watcher != null)
    {
      _dualReadStateManager.removeClusterWatcher(clusterName, watcher);
    }
  }

  public void removeWatcherForServiceProperties(String serviceName)
  {
    DualReadStateManager.DualReadModeWatcher watcher = _servicePropertiesDualReadModeWatchers.remove(serviceName);
    if (watcher != null)
    {
      _dualReadStateManager.removeServiceWatcher(serviceName, watcher);
    }
  }

  private String getWatcherNameForLoadBalancerStrategy(String serviceName, String scheme)
  {
    return String.format("%s-%s", serviceName, scheme);
  }
}
