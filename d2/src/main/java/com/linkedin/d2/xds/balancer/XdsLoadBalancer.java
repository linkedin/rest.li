package com.linkedin.d2.xds.balancer;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;
import com.linkedin.d2.balancer.util.TogglingLoadBalancer;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapper;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsLoadBalancer implements LoadBalancerWithFacilities
{
  private static final Logger _log = LoggerFactory.getLogger(XdsLoadBalancer.class);

  private final TogglingLoadBalancer _loadBalancer;

  public XdsLoadBalancer(XdsTogglingLoadBalancerFactory factory)
  {
    _loadBalancer = factory.create(Executors.newSingleThreadScheduledExecutor());
    _loadBalancer.enablePrimary(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        _log.info("Error enabling primary stores", e);
      }

      @Override
      public void onSuccess(None result)
      {
        _log.info("Primary store started");
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
    throw new UnsupportedOperationException();
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
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    shutdown.done();
  }
}
