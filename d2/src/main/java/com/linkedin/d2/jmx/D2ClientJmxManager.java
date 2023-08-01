/*
   Copyright (c) 2019 LinkedIn Corp.

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

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.dualread.DualReadLoadBalancerJmx;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.simple.ClusterInfoItem;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState.SimpleLoadBalancerStateListener;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.util.ArgumentUtil;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMX manager to register the D2 client components
 */
public class D2ClientJmxManager
{
  private static final Logger _log = LoggerFactory.getLogger(D2ClientJmxManager.class);

  private final JmxManager _jmxManager;

  // Service discovery source type: ZK, xDS, etc.
  private final DiscoverySourceType _discoverySourceType;

  /*
  When dual read state manager is null, only one discovery source is working (could be a new source other than ZK). We keep using the same Jmx/sensor
  names as the ZK one so users can still monitor the same metrics.

  When dual read state manager is not null, it means dual read load balancer is in use, and there are two sets of load balancer, lb state, and FS backup
  registering Jmx/sensors for different service discovery sources.
  Depending on the specific dual read mode that is dynamically changing, controlled by lix on d2 service level, one source is primary, the other is secondary.
  Jmx/sensor names need to be carefully handled to:
     1) for the primary source, use the primary names (the one ZK was using) so users can still monitor the same metrics.
     2) for the secondary source, use different names that include the source type to avoid conflicting the primary names.
  */
  private final DualReadStateManager _dualReadStateManager;

  private final String _primaryGlobalPrefix;

  private final String _secondaryGlobalPrefix;

  private static final String _primaryPrefixForLbPropertyJmxName = "";

  private final String _secondaryPrefixForLbPropertyJmxName;

  @SuppressWarnings("rawtypes")
  private final ConcurrentMap<String, D2ClientJmxDualReadModeWatcher> _dualReadStateWatchers;

  public enum DiscoverySourceType
  {
    ZK("ZK"),
    XDS("xDS");

    private final String _printName;

    DiscoverySourceType(String printName)
    {
      _printName = printName;
    }

    public String getPrintName()
    {
      return _printName;
    }
  }

  public D2ClientJmxManager(String prefix, @Nonnull JmxManager jmxManager)
  {
    this(prefix, jmxManager, DiscoverySourceType.ZK, null);
  }

  public D2ClientJmxManager(String prefix,
      @Nonnull JmxManager jmxManager,
      @Nonnull DiscoverySourceType discoverySourceType,
      @Nullable DualReadStateManager dualReadStateManager)
  {
    ArgumentUtil.ensureNotNull(jmxManager,"jmxManager");
    _primaryGlobalPrefix = prefix;
    _jmxManager = jmxManager;
    _discoverySourceType = discoverySourceType;
    _dualReadStateManager = dualReadStateManager;
    _secondaryGlobalPrefix = String.format("%s-%s", _primaryGlobalPrefix, _discoverySourceType.getPrintName());
    _secondaryPrefixForLbPropertyJmxName = String.format("%s-", _discoverySourceType.getPrintName());
    _dualReadStateWatchers = new ConcurrentHashMap<>();
  }

  @SuppressWarnings({"unchecked"})
  public void setSimpleLoadBalancer(SimpleLoadBalancer balancer)
  {
    if (_dualReadStateManager != null)
    {
      String watcherName = balancer.getClass().getSimpleName();
      D2ClientJmxDualReadModeWatcher<SimpleLoadBalancer> currentWatcher = _dualReadStateWatchers.computeIfAbsent(watcherName, k ->
      {
        D2ClientJmxDualReadModeWatcher<SimpleLoadBalancer> watcher = new D2ClientJmxDualReadModeWatcher<>(balancer, this::doRegisterLoadBalancer);
        _dualReadStateManager.addGlobalWatcher(watcher);
        return watcher;
      });
      currentWatcher.setLatestJmxProperty(balancer);
    }
    doRegisterLoadBalancer(balancer);
  }

  @SuppressWarnings({"unchecked"})
  public void setSimpleLoadBalancerState(SimpleLoadBalancerState state)
  {
    if (_dualReadStateManager != null)
    {
      String watcherName = state.getClass().getSimpleName();
      D2ClientJmxDualReadModeWatcher<SimpleLoadBalancerState> currentWatcher = _dualReadStateWatchers.computeIfAbsent(watcherName, k ->
      {
        D2ClientJmxDualReadModeWatcher<SimpleLoadBalancerState> watcher = new D2ClientJmxDualReadModeWatcher<>(state, this::doRegisterLoadBalancerState);
        _dualReadStateManager.addGlobalWatcher(watcher);
        return watcher;
      });
      currentWatcher.setLatestJmxProperty(state);
    }
    doRegisterLoadBalancerState(state);

    state.register(new SimpleLoadBalancerStateListener()
    {
      @Override
      public void onStrategyAdded(String serviceName, String scheme, LoadBalancerStrategy strategy)
      {
        if (_dualReadStateManager != null)
        {
          D2ClientJmxDualReadModeWatcher<LoadBalancerStrategy> currentWatcher =
              _dualReadStateWatchers.computeIfAbsent(getWatcherNameForLoadBalancerStrategy(serviceName, scheme), k ->
              {
                Consumer<LoadBalancerStrategy> callback = i -> doRegisterLoadBalancerStrategy(serviceName, scheme, i);
                D2ClientJmxDualReadModeWatcher<LoadBalancerStrategy> watcher = new D2ClientJmxDualReadModeWatcher<>(strategy, callback);
                _dualReadStateManager.addServiceWatcher(serviceName, watcher);
                return watcher;
              });
          currentWatcher.setLatestJmxProperty(strategy);
        }
        doRegisterLoadBalancerStrategy(serviceName, scheme, strategy);
      }

      @Override
      public void onStrategyRemoved(String serviceName, String scheme, LoadBalancerStrategy strategy)
      {
        DualReadStateManager.DualReadModeWatcher watcher = _dualReadStateWatchers.remove(getWatcherNameForLoadBalancerStrategy(serviceName, scheme));
        if (_dualReadStateManager != null && watcher != null)
        {
          _dualReadStateManager.removeServiceWatcher(serviceName, watcher);
        }
        _jmxManager.unregister(getLoadBalancerStrategyJmxName(serviceName, scheme));
      }

      @Override
      public void onClientAdded(String clusterName, TrackerClient client)
      {
        // We currently think we can make this no-op as the info provided is not helpful
        //  _jmxManager.checkReg(new DegraderControl((DegraderImpl) client.getDegrader(DefaultPartitionAccessor.DEFAULT_PARTITION_ID)),
        //                   _prefix + "-" + clusterName + "-" + client.getUri().toString().replace("://", "-") + "-TrackerClient-Degrader");
      }

      @Override
      public void onClientRemoved(String clusterName, TrackerClient client)
      {
        // We currently think we can make this no-op as the info provided is not helpful
        //  _jmxManager.unregister(_prefix + "-" + clusterName + "-" + client.getUri().toString().replace("://", "-") + "-TrackerClient-Degrader");
      }

      @Override
      public void onClusterInfoUpdate(ClusterInfoItem clusterInfoItem)
      {
        if (clusterInfoItem != null && clusterInfoItem.getClusterPropertiesItem() != null
            && clusterInfoItem.getClusterPropertiesItem().getProperty() != null)
        {
          String clusterName = clusterInfoItem.getClusterPropertiesItem().getProperty().getClusterName();
          if (_dualReadStateManager != null)
          {
            D2ClientJmxDualReadModeWatcher<ClusterInfoItem> currentWatcher =
                _dualReadStateWatchers.computeIfAbsent(getWatcherNameForClusterInfoItem(clusterName), k ->
                {
                  Consumer<ClusterInfoItem> callback = i -> doRegisterClusterInfo(clusterName, i);
                  D2ClientJmxDualReadModeWatcher<ClusterInfoItem> watcher = new D2ClientJmxDualReadModeWatcher<>(clusterInfoItem, callback);
                  _dualReadStateManager.addClusterWatcher(clusterName, watcher);
                  return watcher;
                });
            currentWatcher.setLatestJmxProperty(clusterInfoItem);
          }
          doRegisterClusterInfo(clusterName, clusterInfoItem);
        }
      }

      @Override
      public void onClusterInfoRemoval(ClusterInfoItem clusterInfoItem)
      {
        if (clusterInfoItem != null && clusterInfoItem.getClusterPropertiesItem() != null
            && clusterInfoItem.getClusterPropertiesItem().getProperty() != null)
        {
          String clusterName = clusterInfoItem.getClusterPropertiesItem().getProperty().getClusterName();
          DualReadStateManager.DualReadModeWatcher watcher = _dualReadStateWatchers.remove(getWatcherNameForClusterInfoItem(clusterName));
          if (_dualReadStateManager != null && watcher != null)
          {
            _dualReadStateManager.removeClusterWatcher(clusterName, watcher);
          }
          _jmxManager.unregister(getClusterInfoJmxName(clusterName));
        }
      }

      @Override
      public void onServicePropertiesUpdate(LoadBalancerStateItem<ServiceProperties> serviceProperties)
      {
        if (serviceProperties != null && serviceProperties.getProperty() != null)
        {
          String serviceName = serviceProperties.getProperty().getServiceName();
          if (_dualReadStateManager != null)
          {
            D2ClientJmxDualReadModeWatcher<LoadBalancerStateItem<ServiceProperties>> currentWatcher =
                _dualReadStateWatchers.computeIfAbsent(getWatcherNameForServiceProperties(serviceName), k ->
                {
                  Consumer<LoadBalancerStateItem<ServiceProperties>> callback = i -> doRegisterServiceProperties(serviceName, i);
                  D2ClientJmxDualReadModeWatcher<LoadBalancerStateItem<ServiceProperties>> watcher =
                      new D2ClientJmxDualReadModeWatcher<>(serviceProperties, callback);
                  _dualReadStateManager.addServiceWatcher(serviceName, watcher);
                  return watcher;
                });
            currentWatcher.setLatestJmxProperty(serviceProperties);
          }
          doRegisterServiceProperties(serviceName, serviceProperties);
        }
      }

      @Override
      public void onServicePropertiesRemoval(LoadBalancerStateItem<ServiceProperties> serviceProperties)
      {
        if (serviceProperties != null && serviceProperties.getProperty() != null)
        {
          String serviceName = serviceProperties.getProperty().getServiceName();
          DualReadStateManager.DualReadModeWatcher watcher = _dualReadStateWatchers.remove(getWatcherNameForServiceProperties(serviceName));
          if (_dualReadStateManager != null && watcher != null)
          {
            _dualReadStateManager.removeServiceWatcher(serviceName, watcher);
          }
          _jmxManager.unregister(getServicePropertiesJmxName(serviceName));
        }
      }

      private void doRegisterLoadBalancerStrategy(String serviceName, String scheme, LoadBalancerStrategy strategy)
      {
        String jmxName = getLoadBalancerStrategyJmxName(serviceName, scheme);
        _jmxManager.registerLoadBalancerStrategy(jmxName, strategy);
      }

      private void doRegisterClusterInfo(String clusterName, ClusterInfoItem clusterInfoItem)
      {
        String jmxName = getClusterInfoJmxName(clusterName);
        _jmxManager.registerClusterInfo(jmxName, clusterInfoItem);
      }

      private void doRegisterServiceProperties(String serviceName, LoadBalancerStateItem<ServiceProperties> serviceProperties)
      {
        _jmxManager.registerServiceProperties(getServicePropertiesJmxName(serviceName), serviceProperties);
      }

      private String getWatcherNameForLoadBalancerStrategy(String serviceName, String scheme)
      {
        return String.format("%s-%s-LoadBalancerStrategy", serviceName, scheme);
      }

      private String getWatcherNameForClusterInfoItem(String clusterName) {
        return String.format("%s-ClusterInfoItem", clusterName);
      }

      private String getWatcherNameForServiceProperties(String serviceName) {
        return String.format("%s-LoadBalancerStateItem-ServiceProperties", serviceName);
      }

      private String getClusterInfoJmxName(String clusterName)
      {
        return String.format("%s%s-ClusterInfo", getClusterPrefixForLBPropertyJmxNames(clusterName), clusterName);
      }

      private String getServicePropertiesJmxName(String serviceName)
      {
        return String.format("%s%s-ServiceProperties", getServicePrefixForLBPropertyJmxNames(serviceName), serviceName);
      }

      private String getLoadBalancerStrategyJmxName(String serviceName, String scheme)
      {
        return String.format("%s%s-%s-LoadBalancerStrategy", getServicePrefixForLBPropertyJmxNames(serviceName), serviceName, scheme);
      }
    });
  }

  public <T> void setZkUriRegistry(ZooKeeperEphemeralStore<T> uriRegistry)
  {
    if (_discoverySourceType != DiscoverySourceType.ZK)
    {
      _log.warn("Setting ZkUriRegistry for Non-ZK source type: {}", _discoverySourceType);
    }
    final String jmxName = String.format("%s-ZooKeeperUriRegistry", getGlobalPrefix());
    _jmxManager.registerZooKeeperEphemeralStore(jmxName, uriRegistry);
  }

  public <T> void setZkClusterRegistry(ZooKeeperPermanentStore<T> clusterRegistry)
  {
    if (_discoverySourceType != DiscoverySourceType.ZK)
    {
      _log.warn("Setting ZkClusterRegistry for Non-ZK source type: {}", _discoverySourceType);
    }
    final String jmxName = String.format("%s-ZooKeeperClusterRegistry", getGlobalPrefix());
    _jmxManager.registerZooKeeperPermanentStore(jmxName, clusterRegistry);
  }

  public <T> void setZkServiceRegistry(ZooKeeperPermanentStore<T> serviceRegistry)
  {
    if (_discoverySourceType != DiscoverySourceType.ZK)
    {
      _log.warn("Setting ZkServiceRegistry for Non-ZK source type: {}", _discoverySourceType);
    }
    final String jmxName = String.format("%s-ZooKeeperServiceRegistry", getGlobalPrefix());
    _jmxManager.registerZooKeeperPermanentStore(jmxName, serviceRegistry);
  }

  public <T> void setFsUriStore(FileStore<T> uriStore)
  {
    addDualReadModeWatcherForFileStore("UriProperties", uriStore, this::doRegisterUriFileStore);
    doRegisterUriFileStore(uriStore);
  }

  public <T> void setFsClusterStore(FileStore<T> clusterStore)
  {
    addDualReadModeWatcherForFileStore("ClusterProperties", clusterStore, this::doRegisterClusterFileStore);
    doRegisterClusterFileStore(clusterStore);
  }

  public <T> void setFsServiceStore(FileStore<T> serviceStore)
  {
    addDualReadModeWatcherForFileStore("ServiceProperties", serviceStore, this::doRegisterServiceFileStore);
    doRegisterServiceFileStore(serviceStore);
  }

  public void registerDualReadLoadBalancerJmx(DualReadLoadBalancerJmx dualReadLoadBalancerJmx)
  {
    if (_discoverySourceType != DiscoverySourceType.XDS)
    {
      _log.warn("Setting DualReadLoadBalancerJmx for Non-XDS source type: {}", _discoverySourceType);
    }
    final String jmxName = String.format("%s-DualReadLoadBalancerJmx", getGlobalPrefix());
    _jmxManager.registerDualReadLoadBalancerJmxBean(jmxName, dualReadLoadBalancerJmx);
  }

  private void doRegisterLoadBalancer(SimpleLoadBalancer balancer)
  {
    final String jmxName = String.format("%s-LoadBalancer", getGlobalPrefix());
    _jmxManager.registerLoadBalancer(jmxName, balancer);
  }

  private void doRegisterLoadBalancerState(SimpleLoadBalancerState state)
  {
    final String jmxName = String.format("%s-LoadBalancerState", getGlobalPrefix());
    _jmxManager.registerLoadBalancerState(jmxName, state);
  }

  private <T> void doRegisterUriFileStore(FileStore<T> uriStore)
  {
    final String jmxName = String.format("%s-FileStoreUriStore", getGlobalPrefix());
    _jmxManager.registerFileStore(jmxName, uriStore);
  }

  private <T> void doRegisterClusterFileStore(FileStore<T> clusterStore)
  {
    final String jmxName = String.format("%s-FileStoreClusterStore", getGlobalPrefix());
    _jmxManager.registerFileStore(jmxName, clusterStore);
  }

  private <T> void doRegisterServiceFileStore(FileStore<T> serviceStore)
  {
    final String jmxName = String.format("%s-FileStoreServiceStore", getGlobalPrefix());
    _jmxManager.registerFileStore(jmxName, serviceStore);
  }

  @SuppressWarnings("unchecked")
  private <T> void addDualReadModeWatcherForFileStore(String watcherNameSuffix, FileStore<T> store, Consumer<FileStore<T>> watcherCallback)
  {
    if (_dualReadStateManager != null)
    {
      String watcherName = String.format("%s-%s", store.getClass().getSimpleName(), watcherNameSuffix);
      D2ClientJmxDualReadModeWatcher<FileStore<T>> currentWatcher = _dualReadStateWatchers.computeIfAbsent(watcherName, k ->
      {
        D2ClientJmxDualReadModeWatcher<FileStore<T>> watcher = new D2ClientJmxDualReadModeWatcher<>(store, watcherCallback);
        _dualReadStateManager.addGlobalWatcher(watcher);
        return watcher;
      });
      currentWatcher.setLatestJmxProperty(store);
    }
  }

  private String getGlobalPrefix()
  {
    return isGlobalPrimarySource() ? _primaryGlobalPrefix : _secondaryGlobalPrefix;
  }

  private String getServicePrefixForLBPropertyJmxNames(String serviceName)
  {
    return isServicePrimarySource(serviceName) ? _primaryPrefixForLbPropertyJmxName : _secondaryPrefixForLbPropertyJmxName;
  }

  private String getClusterPrefixForLBPropertyJmxNames(String clusterName)
  {
    return isClusterPrimarySource(clusterName) ? _primaryPrefixForLbPropertyJmxName : _secondaryPrefixForLbPropertyJmxName;
  }

  private boolean isGlobalPrimarySource()
  {
    if (_dualReadStateManager == null)
    {
      return true; // only one source, it is the primary.
    }
    return isPrimarySourceHelper(_dualReadStateManager.getGlobalDualReadMode());
  }

  private boolean isServicePrimarySource(String serviceName)
  {
    if (_dualReadStateManager == null)
    {
      return true; // only one source, it is the primary.
    }
    return isPrimarySourceHelper(_dualReadStateManager.getServiceDualReadMode(serviceName));
  }

  private boolean isClusterPrimarySource(String clusterName)
  {
    if (_dualReadStateManager == null)
    {
      return true; // only one source, it is the primary.
    }
    return isPrimarySourceHelper(_dualReadStateManager.getClusterDualReadMode(clusterName));
  }

  private boolean isPrimarySourceHelper(DualReadModeProvider.DualReadMode dualReadMode)
  {
    switch (dualReadMode)
    {
      case NEW_LB_ONLY:
        return _discoverySourceType == DiscoverySourceType.XDS;
      case DUAL_READ:
      case OLD_LB_ONLY:
        return _discoverySourceType == DiscoverySourceType.ZK;
      default:
        _log.warn("Unknown dual read mode {}, falling back to ZK as primary source.", dualReadMode);
        return _discoverySourceType == DiscoverySourceType.ZK;
    }
  }

  static final class D2ClientJmxDualReadModeWatcher<T> implements DualReadStateManager.DualReadModeWatcher
  {
    private T _latestJmxProperty;
    private final Consumer<T> _callback;

    D2ClientJmxDualReadModeWatcher(T initialJmxProperty, Consumer<T> callback)
    {
      _latestJmxProperty = initialJmxProperty;
      _callback = callback;
    }

    @VisibleForTesting
    T getLatestJmxProperty()
    {
      return _latestJmxProperty;
    }

    public void setLatestJmxProperty(T latestJmxProperty)
    {
      _latestJmxProperty = latestJmxProperty;
    }

    @Override
    public void onChanged()
    {
      _callback.accept(_latestJmxProperty);
    }
  }
}
