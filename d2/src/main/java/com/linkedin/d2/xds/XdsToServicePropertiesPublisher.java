package com.linkedin.d2.xds;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;


public class XdsToServicePropertiesPublisher implements PropertyEventPublisher<ServiceProperties>
{
  private final XdsToD2PropertiesAdaptor _adaptor;

  public XdsToServicePropertiesPublisher(XdsToD2PropertiesAdaptor adaptor)
  {
    _adaptor = adaptor;
  }

  @Override
  public void setBus(PropertyEventBus<ServiceProperties> bus)
  {
    _adaptor.setServiceEventBus(bus);
  }

  @Override
  public void startPublishing(String serviceName)
  {
    _adaptor.listenToService(serviceName);
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
