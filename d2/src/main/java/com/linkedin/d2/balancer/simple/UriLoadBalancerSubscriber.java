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

import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.degrader.DegraderConfigFactory;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.degrader.DegraderImpl;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

/**
 * Subscriber to the uri data to update the SimpleLoadBalancerState
 */
class UriLoadBalancerSubscriber extends
  AbstractLoadBalancerSubscriber<UriProperties>
{

  private static final Logger _log =
    LoggerFactory.getLogger(UriLoadBalancerSubscriber.class);

  private SimpleLoadBalancerState _simpleLoadBalancerState;

  public UriLoadBalancerSubscriber(PropertyEventBus<UriProperties> uPropertyEventBus, SimpleLoadBalancerState simpleLoadBalancerState)
  {
    super(LoadBalancerState.LoadBalancerStateListenerCallback.CLUSTER, uPropertyEventBus);
    _simpleLoadBalancerState = simpleLoadBalancerState;
  }

  @Override
  protected void handlePut(final String listenTo, final UriProperties discoveryProperties)
  {
    // add tracker clients for uris that we aren't already tracking
    if (discoveryProperties != null)
    {
      String clusterName = discoveryProperties.getClusterName();

      Set<String> serviceNames = _simpleLoadBalancerState.getServicesPerCluster().get(clusterName);
      //updates all the services that these uris provide
      if (serviceNames != null)
      {
        for (String serviceName : serviceNames)
        {
          Map<URI, TrackerClient> trackerClients =
            _simpleLoadBalancerState.getTrackerClients().get(serviceName);
          if (trackerClients == null)
          {
            trackerClients = new ConcurrentHashMap<URI, TrackerClient>();
            _simpleLoadBalancerState.getTrackerClients().put(serviceName, trackerClients);
          }
          LoadBalancerStateItem<ServiceProperties> servicePropertiesItem = _simpleLoadBalancerState.getServiceProperties().get(serviceName);
          ServiceProperties serviceProperties = servicePropertiesItem == null ? null : servicePropertiesItem.getProperty();
          DegraderImpl.Config config = null;
          Clock clk = SystemClock.instance();

          if (servicePropertiesItem == null || serviceProperties == null ||
            serviceProperties.getDegraderProperties() == null)
          {
            debug(_log, "trying to see if there's a special degraderImpl properties but serviceInfo is null " +
              "for serviceName = " + serviceName + " so we'll set config to default");
          }
          else
          {
            Map<String, String> degraderImplProperties =
              serviceProperties.getDegraderProperties();
            config = DegraderConfigFactory.toDegraderConfig(degraderImplProperties);
          }
          if (servicePropertiesItem != null && serviceProperties != null &&
            serviceProperties.getLoadBalancerStrategyProperties() != null)
          {
            Map<String, Object> loadBalancerStrategyProperties =
              serviceProperties.getLoadBalancerStrategyProperties();
            clk = MapUtil.getWithDefault(loadBalancerStrategyProperties, PropertyKeys.CLOCK, SystemClock.instance(), Clock.class);
          }

          long trackerClientInterval = SimpleLoadBalancerState.getTrackerClientInterval(serviceProperties);
          String errorStatusPattern = SimpleLoadBalancerState.getErrorStatusPattern(serviceProperties);

          for (URI uri : discoveryProperties.Uris())
          {
            Map<Integer, PartitionData> partitionDataMap = discoveryProperties.getPartitionDataMap(uri);
            TrackerClient client = trackerClients.get(uri);
            if (client == null || !client.getParttitionDataMap().equals(partitionDataMap))
            {
              client = _simpleLoadBalancerState.buildTrackerClient(serviceName,
                uri,
                partitionDataMap,
                new DegraderImpl.Config(config),
                clk,
                trackerClientInterval,
                errorStatusPattern,
                discoveryProperties.getUriSpecificProperties().get(uri));

              if (client != null)
              {
                debug(_log, "adding new tracker client from updated uri properties: ", client);

                // notify listeners of the added client
                for (SimpleLoadBalancerState.SimpleLoadBalancerStateListener listener : _simpleLoadBalancerState.getListeners())
                {
                  listener.onClientAdded(serviceName, client);
                }

                trackerClients.put(uri, client);
              }
            }
          }
        }
      }

    }

    // replace the URI properties
    _simpleLoadBalancerState.getUriProperties().put(listenTo,
      new LoadBalancerStateItem<>(discoveryProperties,
        _simpleLoadBalancerState.getVersionAccess().incrementAndGet(),
        System.currentTimeMillis()));

    // now remove URIs that we're tracking, but have been removed from the new uri
    // properties
    if (discoveryProperties != null)
    {
      Set<String> serviceNames = _simpleLoadBalancerState.getServicesPerCluster().get(discoveryProperties.getClusterName());
      if (serviceNames != null)
      {
        for (String serviceName : serviceNames)
        {
          Map<URI, TrackerClient> trackerClients = _simpleLoadBalancerState.getTrackerClients().get(serviceName);
          if (trackerClients != null)
          {
            for (Iterator<URI> it = trackerClients.keySet().iterator(); it.hasNext(); )
            {
              URI uri = it.next();

              if (!discoveryProperties.Uris().contains(uri))
              {
                TrackerClient client = trackerClients.remove(uri);

                debug(_log, "removing dead tracker client: ", client);

                // notify listeners of the removed client
                for (SimpleLoadBalancerState.SimpleLoadBalancerStateListener listener : _simpleLoadBalancerState.getListeners())
                {
                  listener.onClientRemoved(serviceName, client);
                }
                // We don't shut down the dead TrackerClient, because TrackerClients hold no
                // resources and simply point to the common cluster client (from _serviceeClients).
              }
            }
          }
        }
      }
    }
    else
    {
      // uri properties was null, we'll just log the event and continues.
      // The reasoning is we might receive a null event when there's a problem writing/reading
      // cache file, or we just started listening to a cluster without any uris yet.
      warn(_log, "received a null uri properties for cluster: ", listenTo);
    }
  }

  @Override
  protected void handleRemove(final String listenTo)
  {
    _simpleLoadBalancerState.getUriProperties().remove(listenTo);
    warn(_log, "received a uri properties event remove() for cluster: ", listenTo);
    _simpleLoadBalancerState.removeTrackerClients(listenTo);
  }
}
