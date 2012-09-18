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

package com.linkedin.d2.balancer.zkfs;

import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.util.TogglingLoadBalancer;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.toggling.TogglingPublisher;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKFSComponentFactory implements ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory
{
  @Override
  public TogglingLoadBalancer createBalancer(SimpleLoadBalancer balancer,
                                             SimpleLoadBalancerState state,
                                             TogglingPublisher<ClusterProperties> clusterToggle,
                                             TogglingPublisher<ServiceProperties> serviceToggle,
                                             TogglingPublisher<UriProperties> uriToggle)
  {
    return new TogglingLoadBalancer(balancer, clusterToggle, serviceToggle, uriToggle);
  }

  @Override
  public TogglingPublisher<ClusterProperties> createClusterToggle(
          ZooKeeperPermanentStore<ClusterProperties> zk, FileStore<ClusterProperties> fs,
          PropertyEventBus<ClusterProperties> bus)
  {
    return new TogglingPublisher<ClusterProperties>(zk, fs, bus);
  }

  @Override
  public TogglingPublisher<ServiceProperties> createServiceToggle(
          ZooKeeperPermanentStore<ServiceProperties> zk, FileStore<ServiceProperties> fs,
          PropertyEventBus<ServiceProperties> bus)
  {
    return new TogglingPublisher<ServiceProperties>(zk, fs, bus);
  }

  @Override
  public TogglingPublisher<UriProperties> createUriToggle(ZooKeeperEphemeralStore<UriProperties> zk,
                                                          FileStore<UriProperties> fs,
                                                          PropertyEventBus<UriProperties> bus)
  {
    return new TogglingPublisher<UriProperties>(zk, fs, bus);
  }
}
