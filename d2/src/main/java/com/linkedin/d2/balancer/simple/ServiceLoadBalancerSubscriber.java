/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.config.CanaryDistributionStrategyConverter;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesWithCanary;
import com.linkedin.d2.balancer.util.CanaryDistributionProvider;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscriber to the service data to update the SimpleLoadBalancerState
 */
class ServiceLoadBalancerSubscriber extends AbstractLoadBalancerSubscriber<ServiceProperties>
{
  private static final Logger _log =
    LoggerFactory.getLogger(ServiceLoadBalancerSubscriber.class);

  private SimpleLoadBalancerState _simpleLoadBalancerState;

  public ServiceLoadBalancerSubscriber(PropertyEventBus<ServiceProperties> eventBus,
                                       SimpleLoadBalancerState simpleLoadBalancerState)
  {
    super(LoadBalancerState.LoadBalancerStateListenerCallback.SERVICE, eventBus);

    _simpleLoadBalancerState = simpleLoadBalancerState;
  }

  @Override
  protected void handlePut(final String listenTo, final ServiceProperties discoveryProperties)
  {
    LoadBalancerStateItem<ServiceProperties> oldServicePropertiesItem =
      _simpleLoadBalancerState.getServiceProperties().get(listenTo);

    ServiceProperties pickedProperties = discoveryProperties;
    if (discoveryProperties instanceof ServicePropertiesWithCanary) {
      // has canary config, distribute to use either stable config or canary config
      ServicePropertiesWithCanary propertiesWithCanary = (ServicePropertiesWithCanary) discoveryProperties;
      CanaryDistributionProvider.Distribution distribution = _simpleLoadBalancerState.getCanaryDistributionProvider()
              .distribute(CanaryDistributionStrategyConverter.toConfig(propertiesWithCanary.getCanaryDistributionStrategy()));
      if (distribution == CanaryDistributionProvider.Distribution.CANARY) {
        pickedProperties = propertiesWithCanary.getCanaryConfigs();
      }
      // TODO: set canary config metric
    }

    _simpleLoadBalancerState.getServiceProperties().put(listenTo,
      new LoadBalancerStateItem<>(pickedProperties,
        _simpleLoadBalancerState.getVersionAccess().incrementAndGet(),
        System.currentTimeMillis()));

    // always refresh strategies when we receive service event
    if (pickedProperties != null)
    {
      //if this service changes its cluster, we should update the cluster -> service map saying that
      //this service is no longer hosted in the old cluster.
      if (oldServicePropertiesItem != null)
      {
        ServiceProperties oldServiceProperties = oldServicePropertiesItem.getProperty();
        if (oldServiceProperties != null && oldServiceProperties.getClusterName() != null &&
          !oldServiceProperties.getClusterName().equals(pickedProperties.getClusterName()))
        {
          Set<String> serviceNames =
            _simpleLoadBalancerState.getServicesPerCluster().get(oldServiceProperties.getClusterName());
          if (serviceNames != null)
          {
            serviceNames.remove(oldServiceProperties.getServiceName());
          }
        }
      }

      _simpleLoadBalancerState.refreshServiceStrategies(pickedProperties);
      _simpleLoadBalancerState.refreshClients(pickedProperties);

      // refresh state for which services are on which clusters
      Set<String> serviceNames =
        _simpleLoadBalancerState.getServicesPerCluster().get(pickedProperties.getClusterName());

      if (serviceNames == null)
      {
        serviceNames =
          Collections.newSetFromMap(new ConcurrentHashMap<>());
        _simpleLoadBalancerState.getServicesPerCluster().put(pickedProperties.getClusterName(), serviceNames);
      }

      serviceNames.add(pickedProperties.getServiceName());
    }
    else if (oldServicePropertiesItem != null)
    {
      // if we've replaced a service properties with null, update the cluster ->
      // service state that the service is no longer on its cluster.
      ServiceProperties oldServiceProperties = oldServicePropertiesItem.getProperty();

      if (oldServiceProperties != null)
      {
        Set<String> serviceNames =
          _simpleLoadBalancerState.getServicesPerCluster().get(oldServiceProperties.getClusterName());

        if (serviceNames != null)
        {
          serviceNames.remove(oldServiceProperties.getServiceName());
        }
      }
    }

    if (discoveryProperties == null)
    {
      // we'll just ignore the event and move on.
      // we could receive a null if the file store properties cannot read/write a file.
      // in this case it's better to leave the state intact and not do anything
      _log.warn("We receive a null service properties for {}. ", listenTo);
    }
  }

  @Override
  protected void handleRemove(final String listenTo)
  {
    _log.warn("Received a service properties event to remove() for service = " + listenTo);
    LoadBalancerStateItem<ServiceProperties> serviceItem =
      _simpleLoadBalancerState.getServiceProperties().remove(listenTo);

    if (serviceItem != null && serviceItem.getProperty() != null)
    {
      ServiceProperties serviceProperties = serviceItem.getProperty();

      // remove this service from the cluster -> services map
      Set<String> serviceNames =
        _simpleLoadBalancerState.getServicesPerCluster().get(serviceProperties.getClusterName());

      if (serviceNames != null)
      {
        serviceNames.remove(serviceProperties.getServiceName());
      }

      _simpleLoadBalancerState.shutdownClients(listenTo);

    }
  }
}
