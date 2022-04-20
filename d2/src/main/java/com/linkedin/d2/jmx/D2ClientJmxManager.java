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
import javax.annotation.Nonnull;

/**
 * JMX manager to register the D2 client components
 */
public class D2ClientJmxManager
{
  private final String _prefix;
  private final JmxManager _jmxManager;

  public D2ClientJmxManager(String prefix, @Nonnull JmxManager jmxManager)
  {
    ArgumentUtil.ensureNotNull(jmxManager,"jmxManager");
    _prefix = prefix;
    _jmxManager = jmxManager;
  }

  public void setSimpleLoadBalancer(SimpleLoadBalancer balancer)
  {
    final String jmxName = _prefix + "-LoadBalancer";

    _jmxManager.registerLoadBalancer(jmxName, balancer);
  }

  public void setSimpleLoadBalancerState(SimpleLoadBalancerState state)
  {
    _jmxManager.registerLoadBalancerState(_prefix + "-LoadBalancerState", state);

    state.register(new SimpleLoadBalancerStateListener()
    {
      @Override
      public void onStrategyAdded(String serviceName, String scheme, LoadBalancerStrategy strategy)
      {
        _jmxManager.registerLoadBalancerStrategy(getLoadBalancerStrategyJmxName(serviceName, scheme), strategy);
      }

      @Override
      public void onStrategyRemoved(String serviceName, String scheme, LoadBalancerStrategy strategy)
      {
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
        _jmxManager.registerClusterInfoJmxBean(
            getClusterInfoJmxName(clusterInfoItem.getClusterPropertiesItem().getProperty().getClusterName()),
            new ClusterInfoJmx(clusterInfoItem));
      }

      @Override
      public void onClusterInfoRemoval(ClusterInfoItem clusterInfoItem)
      {
        _jmxManager.unregister(
            getClusterInfoJmxName(clusterInfoItem.getClusterPropertiesItem().getProperty().getClusterName())
        );
      }

      @Override
      public void onServicePropertiesUpdate(LoadBalancerStateItem<ServiceProperties> serviceProperties)
      {
        _jmxManager.registerServicePropertiesJmxBean(
            getServicePropertiesJmxName(serviceProperties.getProperty().getServiceName()),
            new ServicePropertiesJmx(serviceProperties));
      }


      @Override
      public void onServicePropertiesRemoval(LoadBalancerStateItem<ServiceProperties> serviceProperties)
      {
        _jmxManager.unregister(
            getServicePropertiesJmxName(serviceProperties.getProperty().getServiceName())
        );
      }

      private String getClusterInfoJmxName(String clusterName)
      {
        return String.format("%s-ClusterInfo", clusterName);
      }

      private String getServicePropertiesJmxName(String serviceName)
      {
        return String.format("%s-ServiceProperties", serviceName);
      }

      private String getLoadBalancerStrategyJmxName(String serviceName, String scheme)
      {
        return serviceName + "-" + scheme + "-LoadBalancerStrategy";
      }
    });
  }

  public <T> void setZkUriRegistry(ZooKeeperEphemeralStore<T> uriRegistry)
  {
    _jmxManager.registerZooKeeperEphemeralStore(_prefix + "-ZooKeeperUriRegistry", uriRegistry);
  }

  public <T> void setZkClusterRegistry(ZooKeeperPermanentStore<T> clusterRegistry)
  {
    _jmxManager.registerZooKeeperPermanentStore(_prefix + "-ZooKeeperClusterRegistry", clusterRegistry);
  }

  public <T> void setZkServiceRegistry(ZooKeeperPermanentStore<T> serviceRegistry)
  {
    _jmxManager.registerZooKeeperPermanentStore(_prefix + "-ZooKeeperServiceRegistry", serviceRegistry);
  }

  public <T> void setFsUriStore(FileStore<T> uriStore)
  {
    _jmxManager.registerFileStore(_prefix + "-FileStoreUriStore", uriStore);
  }

  public <T> void setFsClusterStore(FileStore<T> clusterStore)
  {
    _jmxManager.registerFileStore(_prefix + "-FileStoreClusterStore", clusterStore);
  }

  public <T> void setFsServiceStore(FileStore<T> serviceStore)
  {
    _jmxManager.registerFileStore(_prefix + "-FileStoreServiceStore", serviceStore);
  }
}
