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


public class XdsLoadBalancer implements LoadBalancerWithFacilities
{
  private static final Logger _log = LoggerFactory.getLogger(XdsLoadBalancer.class);

  private final TogglingLoadBalancer _loadBalancer;
  private final XdsToD2PropertiesAdaptor _xdsAdaptor;

  public XdsLoadBalancer(XdsToD2PropertiesAdaptor xdsAdaptor, ScheduledExecutorService executorService,
      XdsTogglingLoadBalancerFactory factory)
  {
    _xdsAdaptor = xdsAdaptor;
    _loadBalancer = factory.create(executorService, xdsAdaptor);
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
    throw new UnsupportedOperationException();
  }

  @Override
  public void getLoadBalancedServiceProperties(String serviceName, Callback<ServiceProperties> clientCallback)
  {
    _loadBalancer.getLoadBalancedServiceProperties(serviceName, clientCallback);
  }

  @Override
  public void getLoadBalancedClusterProperties(String clusterName,
      Callback<Pair<ClusterProperties, UriProperties>> callback)
  {
    _loadBalancer.getLoadBalancedClusterProperties(clusterName, callback);
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
    shutdown.done();
  }
}
