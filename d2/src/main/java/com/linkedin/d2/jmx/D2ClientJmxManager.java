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

import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.dualread.DualReadLoadBalancerJmx;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.ClusterInfoItem;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState.SimpleLoadBalancerStateListener;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.util.ArgumentUtil;
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
  When dual read state manager is null, only one discovery source is working (could be a new source other than ZK). We keep using
  the same Jmx/sensor names as the ZK one so users can still monitor the same metrics.

  When dual read state manager is not null, it means dual read load balancer is in use, and there are two sets of load balancer, lb
  state, and FS backup registering Jmx/sensors for different service discovery sources.
  Depending on the specific dual read mode that is dynamically changing, controlled by lix on d2 service level, one source is primary,
  the other is secondary. Jmx/sensor names need to be carefully handled to:
     1) for the primary source, use the primary names (the one ZK was using) so users can still monitor the same metrics.
     2) for the secondary source, use different names that include the source type to avoid conflicting the primary names.
  */
  private final DualReadStateManager _dualReadStateManager;

  private final String _primaryGlobalPrefix;

  private final String _secondaryGlobalPrefix;

  private static final String PRIMARY_PREFIX_FOR_LB_PROPERTY_JMX_NAME = "";

  private final String _secondaryPrefixForLbPropertyJmxName;

  private final D2ClientJmxDualReadModeWatcherManager _watcherManager;


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
    _watcherManager = _dualReadStateManager == null ? new NoOpD2ClientJmxDualReadModeWatcherManagerImpl()
        : new DefaultD2ClientJmxDualReadModeWatcherManagerImpl(_dualReadStateManager);
  }

  public void setSimpleLoadBalancer(SimpleLoadBalancer balancer)
  {
    _watcherManager.updateWatcher(balancer, this::doRegisterLoadBalancer);
    doRegisterLoadBalancer(balancer, null);
  }

  public void setSimpleLoadBalancerState(SimpleLoadBalancerState state)
  {
    _watcherManager.updateWatcher(state, this::doRegisterLoadBalancerState);
    doRegisterLoadBalancerState(state, null);

    state.register(new SimpleLoadBalancerStateListener()
    {
      @Override
      public void onStrategyAdded(String serviceName, String scheme, LoadBalancerStrategy strategy)
      {
        _watcherManager.updateWatcher(serviceName, scheme, strategy,
            (item, mode) -> doRegisterLoadBalancerStrategy(serviceName, scheme, item, mode));
        doRegisterLoadBalancerStrategy(serviceName, scheme, strategy, null);
      }

      @Override
      public void onStrategyRemoved(String serviceName, String scheme, LoadBalancerStrategy strategy)
      {
        _watcherManager.removeWatcherForLoadBalancerStrategy(serviceName, scheme);
        _jmxManager.unregister(getLoadBalancerStrategyJmxName(serviceName, scheme, null));
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
          _watcherManager.updateWatcher(clusterName, clusterInfoItem,
              (item, mode) -> doRegisterClusterInfo(clusterName, item, mode));
          doRegisterClusterInfo(clusterName, clusterInfoItem, null);
        }
      }

      @Override
      public void onClusterInfoRemoval(ClusterInfoItem clusterInfoItem)
      {
        if (clusterInfoItem != null && clusterInfoItem.getClusterPropertiesItem() != null
            && clusterInfoItem.getClusterPropertiesItem().getProperty() != null)
        {
          String clusterName = clusterInfoItem.getClusterPropertiesItem().getProperty().getClusterName();
          _watcherManager.removeWatcherForClusterInfoItem(clusterName);
          _jmxManager.unregister(getClusterInfoJmxName(clusterName, null));
        }
      }

      @Override
      public void onServicePropertiesUpdate(LoadBalancerStateItem<ServiceProperties> serviceProperties)
      {
        if (serviceProperties != null && serviceProperties.getProperty() != null)
        {
          String serviceName = serviceProperties.getProperty().getServiceName();
          _watcherManager.updateWatcher(serviceName, serviceProperties,
              (item, mode) -> doRegisterServiceProperties(serviceName, item, mode));
          doRegisterServiceProperties(serviceName, serviceProperties, null);
        }
      }

      @Override
      public void onServicePropertiesRemoval(LoadBalancerStateItem<ServiceProperties> serviceProperties)
      {
        if (serviceProperties != null && serviceProperties.getProperty() != null)
        {
          String serviceName = serviceProperties.getProperty().getServiceName();
          _watcherManager.removeWatcherForServiceProperties(serviceName);
          _jmxManager.unregister(getServicePropertiesJmxName(serviceName, null));
        }
      }

      private void doRegisterLoadBalancerStrategy(String serviceName, String scheme, LoadBalancerStrategy strategy,
          @Nullable DualReadModeProvider.DualReadMode mode)
      {
        // Set scheme for OTEL metrics if strategy is RelativeLoadBalancerStrategy
        if (strategy instanceof RelativeLoadBalancerStrategy)
        {
          ((RelativeLoadBalancerStrategy) strategy).setScheme(scheme);
        }
        String jmxName = getLoadBalancerStrategyJmxName(serviceName, scheme, mode);
        _jmxManager.registerLoadBalancerStrategy(jmxName, strategy);
      }

      private void doRegisterClusterInfo(String clusterName, ClusterInfoItem clusterInfoItem,
          @Nullable DualReadModeProvider.DualReadMode mode)
      {
        String jmxName = getClusterInfoJmxName(clusterName, mode);
        _jmxManager.registerClusterInfo(jmxName, clusterInfoItem);
      }

      private void doRegisterServiceProperties(String serviceName, LoadBalancerStateItem<ServiceProperties> serviceProperties,
          @Nullable DualReadModeProvider.DualReadMode mode)
      {
        _jmxManager.registerServiceProperties(getServicePropertiesJmxName(serviceName, mode), serviceProperties);
      }

      private String getClusterInfoJmxName(String clusterName, @Nullable DualReadModeProvider.DualReadMode mode)
      {
        return String.format("%s%s-ClusterInfo", getClusterPrefixForLBPropertyJmxNames(clusterName, mode), clusterName);
      }

      private String getServicePropertiesJmxName(String serviceName, @Nullable DualReadModeProvider.DualReadMode mode)
      {
        return String.format("%s%s-ServiceProperties", getServicePrefixForLBPropertyJmxNames(serviceName, mode), serviceName);
      }

      private String getLoadBalancerStrategyJmxName(String serviceName, String scheme, @Nullable DualReadModeProvider.DualReadMode mode)
      {
        return String.format("%s%s-%s-LoadBalancerStrategy", getServicePrefixForLBPropertyJmxNames(serviceName, mode), serviceName, scheme);
      }
    });
  }

  public <T> void setZkUriRegistry(ZooKeeperEphemeralStore<T> uriRegistry)
  {
    if (_discoverySourceType != DiscoverySourceType.ZK)
    {
      _log.warn("Setting ZkUriRegistry for Non-ZK source type: {}", _discoverySourceType);
    }
    final String jmxName = String.format("%s-ZooKeeperUriRegistry", getGlobalPrefix(null));
    _jmxManager.registerZooKeeperEphemeralStore(jmxName, uriRegistry);
  }

  public <T> void setZkClusterRegistry(ZooKeeperPermanentStore<T> clusterRegistry)
  {
    if (_discoverySourceType != DiscoverySourceType.ZK)
    {
      _log.warn("Setting ZkClusterRegistry for Non-ZK source type: {}", _discoverySourceType);
    }
    final String jmxName = String.format("%s-ZooKeeperClusterRegistry", getGlobalPrefix(null));
    _jmxManager.registerZooKeeperPermanentStore(jmxName, clusterRegistry);
  }

  public <T> void setZkServiceRegistry(ZooKeeperPermanentStore<T> serviceRegistry)
  {
    if (_discoverySourceType != DiscoverySourceType.ZK)
    {
      _log.warn("Setting ZkServiceRegistry for Non-ZK source type: {}", _discoverySourceType);
    }
    final String jmxName = String.format("%s-ZooKeeperServiceRegistry", getGlobalPrefix(null));
    _jmxManager.registerZooKeeperPermanentStore(jmxName, serviceRegistry);
  }

  public void setFsUriStore(FileStore<UriProperties> uriStore)
  {
    _watcherManager.updateWatcherForFileStoreUriProperties(uriStore, this::doRegisterUriFileStore);
    doRegisterUriFileStore(uriStore, null);
  }

  public void setFsClusterStore(FileStore<ClusterProperties> clusterStore)
  {
    _watcherManager.updateWatcherForFileStoreClusterProperties(clusterStore, this::doRegisterClusterFileStore);
    doRegisterClusterFileStore(clusterStore, null);
  }

  public void setFsServiceStore(FileStore<ServiceProperties> serviceStore)
  {
    _watcherManager.updateWatcherForFileStoreServiceProperties(serviceStore, this::doRegisterServiceFileStore);
    doRegisterServiceFileStore(serviceStore, null);
  }

  public void registerDualReadLoadBalancerJmx(DualReadLoadBalancerJmx dualReadLoadBalancerJmx)
  {
    if (_discoverySourceType != DiscoverySourceType.XDS)
    {
      _log.warn("Setting DualReadLoadBalancerJmx for Non-XDS source type: {}", _discoverySourceType);
    }
    final String jmxName = String.format("%s-DualReadLoadBalancerJmx", getGlobalPrefix(null));
    _jmxManager.registerDualReadLoadBalancerJmxBean(jmxName, dualReadLoadBalancerJmx);
  }

  public void registerXdsClientJmx(XdsClientJmx xdsClientJmx)
  {
    if (_discoverySourceType != DiscoverySourceType.XDS)
    {
      _log.warn("Setting XdsClientJmx for Non-XDS source type: {}", _discoverySourceType);
    }
    // Get the client name from global prefix
    String clientName = getGlobalPrefix(null);
    if(clientName != null && !clientName.isEmpty())
    {
      xdsClientJmx.setClientName(clientName);
    }
    else
    {
      _log.warn("Client name is empty, unable to set client name for XdsClientJmx");
    }
    final String jmxName = String.format("%s-XdsClientJmx", clientName);
    _jmxManager.registerXdsClientJmxBean(jmxName, xdsClientJmx);
  }

  private void doRegisterLoadBalancer(SimpleLoadBalancer balancer, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    final String jmxName = String.format("%s-LoadBalancer", getGlobalPrefix(mode));
    _jmxManager.registerLoadBalancer(jmxName, balancer);
  }

  private void doRegisterLoadBalancerState(SimpleLoadBalancerState state, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    final String jmxName = String.format("%s-LoadBalancerState", getGlobalPrefix(mode));
    _jmxManager.registerLoadBalancerState(jmxName, state);
  }

  private <T> void doRegisterUriFileStore(FileStore<T> uriStore, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    final String jmxName = String.format("%s-FileStoreUriStore", getGlobalPrefix(mode));
    _jmxManager.registerFileStore(jmxName, uriStore);
  }

  private <T> void doRegisterClusterFileStore(FileStore<T> clusterStore, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    final String jmxName = String.format("%s-FileStoreClusterStore", getGlobalPrefix(mode));
    _jmxManager.registerFileStore(jmxName, clusterStore);
  }

  private <T> void doRegisterServiceFileStore(FileStore<T> serviceStore, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    final String jmxName = String.format("%s-FileStoreServiceStore", getGlobalPrefix(mode));
    _jmxManager.registerFileStore(jmxName, serviceStore);
  }

  // mode is null when the dual read mode is unknown and needs to be fetched from dual read manager
  private String getGlobalPrefix(@Nullable DualReadModeProvider.DualReadMode mode)
  {
    return isGlobalPrimarySource(mode) ? _primaryGlobalPrefix : _secondaryGlobalPrefix;
  }

  // mode is null when the dual read mode is unknown and needs to be fetched from dual read manager
  private String getServicePrefixForLBPropertyJmxNames(String serviceName, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    return isServicePrimarySource(serviceName, mode) ? PRIMARY_PREFIX_FOR_LB_PROPERTY_JMX_NAME : _secondaryPrefixForLbPropertyJmxName;
  }

  // mode is null when the dual read mode is unknown and needs to be fetched from dual read manager
  private String getClusterPrefixForLBPropertyJmxNames(String clusterName, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    return isClusterPrimarySource(clusterName, mode) ? PRIMARY_PREFIX_FOR_LB_PROPERTY_JMX_NAME : _secondaryPrefixForLbPropertyJmxName;
  }

  private boolean isGlobalPrimarySource(@Nullable DualReadModeProvider.DualReadMode mode)
  {
    if (_dualReadStateManager == null)
    {
      return true; // only one source, it is the primary.
    }
    return isPrimarySourceHelper(mode == null ? _dualReadStateManager.getGlobalDualReadMode() : mode);
  }

  private boolean isServicePrimarySource(String serviceName, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    if (_dualReadStateManager == null)
    {
      return true; // only one source, it is the primary.
    }
    return isPrimarySourceHelper(mode == null ? _dualReadStateManager.getServiceDualReadMode(serviceName) : mode);
  }

  private boolean isClusterPrimarySource(String clusterName, @Nullable DualReadModeProvider.DualReadMode mode)
  {
    if (_dualReadStateManager == null)
    {
      return true; // only one source, it is the primary.
    }
    return isPrimarySourceHelper(mode == null ? _dualReadStateManager.getClusterDualReadMode(clusterName) : mode);
  }

  private boolean isPrimarySourceHelper(@Nonnull DualReadModeProvider.DualReadMode dualReadMode)
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
}
