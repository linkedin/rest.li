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

package com.linkedin.d2.xds.balancer;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.d2.balancer.util.TogglingLoadBalancer;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapper;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.xds.XdsToD2PropertiesAdaptor;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A load balancer which does service discovery through xDS protocol.
 *
 * @see <a href="https://www.envoyproxy.io/docs/envoy/latest/api-docs/xds_protocol">xDS protocol</a>
 *
 * It connects to xDS server and reads back D2 properties through {@link XdsToD2PropertiesAdaptor}.
 * When xDS connection is temporarily unavailable, it switches back to discover from backup file store.
 * It reconnects and rebuilds state when the connection is back alive.
 */
public class XdsLoadBalancer implements LoadBalancerWithFacilities
{
  private static final Logger _log = LoggerFactory.getLogger(XdsLoadBalancer.class);

  private final TogglingLoadBalancer _loadBalancer;
  private final XdsToD2PropertiesAdaptor _xdsAdaptor;
  private final ScheduledExecutorService _executorService;

  public XdsLoadBalancer(XdsToD2PropertiesAdaptor xdsAdaptor, ScheduledExecutorService executorService,
      XdsTogglingLoadBalancerFactory factory)
  {
    _xdsAdaptor = xdsAdaptor;
    _loadBalancer = factory.create(executorService, xdsAdaptor);
    _executorService = executorService;
    registerXdsFSToggle();
  }

  private void registerXdsFSToggle()
  {
    _xdsAdaptor.registerXdsConnectionListener(new XdsToD2PropertiesAdaptor.XdsConnectionListener()
    {
      @Override
      public void onError()
      {
        _loadBalancer.enableBackup(new Callback<None>()
        {
          @Override
          public void onSuccess(None result)
          {
            _log.info("Enabled backup stores");
          }

          @Override
          public void onError(Throwable e)
          {
            _log.info("Failed to enable backup stores", e);
          }
        });
      }

      @Override
      public void onReconnect()
      {
        _loadBalancer.enablePrimary(new Callback<None>()
        {
          @Override
          public void onSuccess(None result)
          {
            _log.info("Enabled primary stores");
          }

          @Override
          public void onError(Throwable e)
          {
            _log.info("Failed to enable primary stores", e);
          }
        });
      }
    });
  }

  @Override
  public void getClient(Request request, RequestContext requestContext, Callback<TransportClient> clientCallback)
  {
    _loadBalancer.getClient(request, requestContext, clientCallback);
  }

  @Override
  public Directory getDirectory()
  {
    // TODO: get a list of all ZK services and clusters names
    throw new UnsupportedOperationException();
  }

  @Override
  public void getLoadBalancedServiceProperties(String serviceName, Callback<ServiceProperties> clientCallback)
  {
    _loadBalancer.getLoadBalancedServiceProperties(serviceName, clientCallback);
  }

  @Override
  public void getLoadBalancedClusterAndUriProperties(String clusterName,
      Callback<Pair<ClusterProperties, UriProperties>> callback)
  {
    _loadBalancer.getLoadBalancedClusterAndUriProperties(clusterName, callback);
  }

  @Override
  public PartitionInfoProvider getPartitionInfoProvider()
  {
    return _loadBalancer;
  }

  @Override
  public HashRingProvider getHashRingProvider()
  {
    return _loadBalancer;
  }

  @Override
  public KeyMapper getKeyMapper()
  {
    return new ConsistentHashKeyMapper(_loadBalancer, _loadBalancer);
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    return _loadBalancer.getClientFactory(scheme);
  }

  @Override
  public ClusterInfoProvider getClusterInfoProvider()
  {
    return _loadBalancer;
  }

  @Override
  public void start(Callback<None> callback)
  {
    _xdsAdaptor.start();
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    _xdsAdaptor.shutdown();
    _executorService.shutdown();
    shutdown.done();
  }
}
