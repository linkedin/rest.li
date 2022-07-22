package com.linkedin.d2.xds;

import com.google.protobuf.Value;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;


public class XdsToD2PropertiesAdaptor
{
  private final XdsClient _xdsClient;
  private PropertyEventBus<UriProperties> _uriEventBus;
  private PropertyEventBus<ServiceProperties> _serviceEventBus;
  private PropertyEventBus<ClusterProperties> _clusterEventBus;

  public XdsToD2PropertiesAdaptor(XdsClient xdsClient)
  {
    _xdsClient = xdsClient;
  }

  public static void main(String[] args)
  {
    XdsChannelFactory xdsChannelFactory = new XdsChannelFactory();
    XdsClientImpl xdsClient = new XdsClientImpl(Node.DEFAULT_NODE, xdsChannelFactory.createChannel("localhost:15010"),
        Executors.newSingleThreadScheduledExecutor());

    XdsToD2PropertiesAdaptor adaptor = new XdsToD2PropertiesAdaptor(xdsClient);
    adaptor.listenToService("d2-service-polarisfoo1.prod.linkedin.com");

    while (true)
    {
    }
  }

  public void setUriEventBus(PropertyEventBus<UriProperties> uriEventBus)
  {
    _uriEventBus = uriEventBus;
  }

  public void setServiceEventBus(PropertyEventBus<ServiceProperties> serviceEventBus)
  {
    _serviceEventBus = serviceEventBus;
  }

  public void setClusterEventBus(PropertyEventBus<ClusterProperties> clusterEventBus)
  {
    _clusterEventBus = clusterEventBus;
  }

  public void listenToCluster(String clusterName)
  {
    _xdsClient.watchCdsResource(clusterName, cdsUpdate ->
    {
      Map<String, Value> clusterPropertiesMap = cdsUpdate.getClusterProperties();
      if (_clusterEventBus != null)
      {
        _clusterEventBus.publishInitialize(clusterName,
            XdsToClusterPropertiesAdaptor.toClusterProperties(clusterPropertiesMap));
      }

      String serviceName = cdsUpdate.getEdsServiceName();
      _xdsClient.watchEdsResource(serviceName, edsUpdate ->
      {
        List<LocalityLbEndpoints> endpoints = edsUpdate.getLocalityLbEndpoints();

        if (_uriEventBus != null)
        {
          _uriEventBus.publishInitialize(clusterName, XdsToUriPropertiesAdaptor.toUriProperties(endpoints));
        }
      });
    });
  }

  public void listenToService(String serviceName)
  {
    _xdsClient.watchLdsResource(serviceName + "_18080", ldsUpdate ->
    {
      HttpConnectionManager httpConnectionManager = ldsUpdate.getHttpConnectionManager();
      String routeConfigName = httpConnectionManager.getRds().getRouteConfigName();

      _xdsClient.watchRdsResource(routeConfigName, rdsUpdate ->
      {
        Map<String, Value> servicePropertiesMap = rdsUpdate.getServiceProperties();
        if (_serviceEventBus != null)
        {
          _serviceEventBus.publishInitialize(serviceName,
              XdsToServicePropertiesAdaptor.toServiceProperties(servicePropertiesMap));
        }

        List<String> clusterNames = new ArrayList<>();
        for (VirtualHost virtualHost : rdsUpdate.getVirtualHosts())
        {
          for (Route route : virtualHost.getRoutesList())
          {
            String clusterName = route.getRoute().getCluster();
            clusterNames.add(clusterName);
          }
        }

        for (String clusterName : clusterNames)
        {
          listenToCluster(clusterName);
        }
      });
    });
  }

  private static final class XdsToUriPropertiesAdaptor
  {
    static UriProperties toUriProperties(List<LocalityLbEndpoints> endpoints)
    {
      // TODO: implement this
      return null;
    }
  }

  private static final class XdsToServicePropertiesAdaptor
  {
    static ServiceProperties toServiceProperties(Map<String, Value> serviceProperties)
    {
      // TODO: implement this
      return null;
    }
  }

  private static final class XdsToClusterPropertiesAdaptor
  {
    static ClusterProperties toClusterProperties(Map<String, Value> clusterProperties)
    {
      // TODO: implement this
      return null;
    }
  }
}
