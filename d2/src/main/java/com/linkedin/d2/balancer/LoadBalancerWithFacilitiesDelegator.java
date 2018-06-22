package com.linkedin.d2.balancer;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;

/**
 * Abstract class implementing the delegating methods for {@link LoadBalancerWithFacilities}
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public abstract class LoadBalancerWithFacilitiesDelegator implements LoadBalancerWithFacilities
{
  final protected LoadBalancerWithFacilities _loadBalancer;

  protected LoadBalancerWithFacilitiesDelegator(LoadBalancerWithFacilities loadBalancer)
  {
    _loadBalancer = loadBalancer;
  }

  @Override
  public Directory getDirectory()
  {
    return _loadBalancer.getDirectory();
  }

  @Override
  public PartitionInfoProvider getPartitionInfoProvider()
  {
    return _loadBalancer.getPartitionInfoProvider();
  }

  @Override
  public HashRingProvider getHashRingProvider()
  {
    return _loadBalancer.getHashRingProvider();
  }

  @Override
  public KeyMapper getKeyMapper()
  {
    return _loadBalancer.getKeyMapper();
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme)
  {
    return _loadBalancer.getClientFactory(scheme);
  }

  @Override
  public TransportClient getClient(Request request, RequestContext requestContext) throws ServiceUnavailableException
  {
    return _loadBalancer.getClient(request, requestContext);
  }

  @Override
  public void start(Callback<None> callback)
  {
    _loadBalancer.start(callback);
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    _loadBalancer.shutdown(shutdown);
  }

  @Override
  public ServiceProperties getLoadBalancedServiceProperties(String serviceName) throws ServiceUnavailableException
  {
    return _loadBalancer.getLoadBalancedServiceProperties(serviceName);
  }
}
