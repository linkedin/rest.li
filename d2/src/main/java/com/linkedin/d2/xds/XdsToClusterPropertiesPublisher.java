package com.linkedin.d2.xds;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;


public class XdsToClusterPropertiesPublisher implements PropertyEventPublisher<ClusterProperties>
{
  private final XdsToD2PropertiesAdaptor _adaptor;

  public XdsToClusterPropertiesPublisher(XdsToD2PropertiesAdaptor adaptor)
  {
    _adaptor = adaptor;
  }

  @Override
  public void setBus(PropertyEventBus<ClusterProperties> bus)
  {
    _adaptor.setClusterEventBus(bus);
  }

  @Override
  public void startPublishing(String clusterName)
  {
    _adaptor.listenToCluster(clusterName);
  }

  @Override
  public void stopPublishing(String clusterName)
  {
    // TODO
  }

  @Override
  public void start(Callback<None> callback)
  {

  }

  @Override
  public void shutdown(Callback<None> callback)
  {

  }
}
