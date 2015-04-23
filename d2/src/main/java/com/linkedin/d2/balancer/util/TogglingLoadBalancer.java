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

package com.linkedin.d2.balancer.util;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.toggling.TogglingPublisher;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * TogglingLoadBalancer encapsulates a load balancer which has a primary and backup source
 * of discovery information and allows toggling between the two.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TogglingLoadBalancer implements LoadBalancer, HashRingProvider, ClientFactoryProvider, PartitionInfoProvider
{
  private final LoadBalancer _balancer;
  private final TogglingPublisher<?>[] _toggles;

  public TogglingLoadBalancer(LoadBalancer balancer, TogglingPublisher<?> ... toggles)
  {
    _balancer = balancer;
    _toggles = toggles;
  }

  public void enablePrimary(Callback<None> callback)
  {
    Callback<None> multiCallback = Callbacks.countDown(callback, _toggles.length);
    for (TogglingPublisher<?> toggle : _toggles)
    {
      toggle.enablePrimary(multiCallback);
    }
  }

  public void enableBackup(Callback<None> callback)
  {
    Callback<None> multiCallback = Callbacks.countDown(callback, _toggles.length);
    for (TogglingPublisher<?> toggle : _toggles)
    {
      toggle.enableBackup(multiCallback);
    }
  }

  @Override
  public void start(Callback<None> callback)
  {
    _balancer.start(callback);
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    _balancer.shutdown(shutdown);
  }

  @Override
  public ServiceProperties getLoadBalancedServiceProperties(String serviceName)
      throws ServiceUnavailableException
  {
    return _balancer.getLoadBalancedServiceProperties(serviceName);
  }

  @Override
  public TransportClient getClient(Request request, RequestContext requestContext) throws ServiceUnavailableException
  {
    return _balancer.getClient(request, requestContext);
  }

  @Override
  public <K> MapKeyResult<Ring<URI>, K> getRings(URI serviceUri, Iterable<K> keys) throws ServiceUnavailableException
  {
    checkLoadBalancer();
    return ((HashRingProvider)_balancer).getRings(serviceUri, keys);
  }

  @Override
  public Map<Integer, Ring<URI>> getRings(URI serviceUri) throws ServiceUnavailableException
  {
    checkLoadBalancer();
    return ((HashRingProvider)_balancer).getRings(serviceUri);
  }

  @Override
  public <K> HostToKeyMapper<K> getPartitionInformation(URI serviceUri, Collection<K> keys, int limitHostPerPartition, int hash) throws ServiceUnavailableException
  {
    checkPartitionInfoProvider();
    return ((PartitionInfoProvider)_balancer).getPartitionInformation(serviceUri, keys, limitHostPerPartition, hash);
  }

  @Override
  public PartitionAccessor getPartitionAccessor(URI serviceUri) throws ServiceUnavailableException
  {
    checkPartitionInfoProvider();
    return ((PartitionInfoProvider)_balancer).getPartitionAccessor(serviceUri);
  }

  private void checkLoadBalancer()
  {
    if (_balancer == null || !(_balancer instanceof HashRingProvider))
    {
      throw new IllegalStateException("No HashRingProvider available to TogglingLoadBalancer - this could be because the load balancer " +
          "is not yet initialized, or because it has been configured with strategies that do not support " +
          "consistent hashing.");
    }
  }

  private void checkPartitionInfoProvider()
  {
    if (_balancer == null || !(_balancer instanceof PartitionInfoProvider))
    {
      throw new IllegalStateException("No PartitionInfoProvider available to TogglingLoadBalancer - this could be because the load balancer " +
                                          "is not yet initialized, or because it has been configured with strategies that do not support " +
                                          "consistent hashing.");
    }
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    if (_balancer == null || !(_balancer instanceof ClientFactoryProvider))
    {
      throw new IllegalStateException("No ClientFactoryProvider available to TogglingLoadBalancer - " +
                                              "this could be because the load balancer " +
                                              "is not yet initialized, or because it has been" +
                                              "configured with a LoadBalancer which does not" +
                                              "support obtaining client factories");
    }
    return ((ClientFactoryProvider)_balancer).getClientFactory(scheme);
  }
}
