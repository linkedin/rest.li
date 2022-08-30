package com.linkedin.d2.xds;

import com.google.protobuf.Value;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterStoreProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServiceStoreProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsToD2PropertiesAdaptor
{
  private static final Logger _log = LoggerFactory.getLogger(XdsToD2PropertiesAdaptor.class);
  private static final String GLOBAL_LISTENER_PORT_SUFFIX = "_18080";

  private final XdsClient _xdsClient;
  private PropertyEventBus<UriProperties> _uriEventBus;
  private PropertyEventBus<ServiceProperties> _serviceEventBus;
  private PropertyEventBus<ClusterProperties> _clusterEventBus;

  public XdsToD2PropertiesAdaptor(XdsClient xdsClient)
  {
    _xdsClient = xdsClient;
  }

  public static void main(String[] args) throws ParseException
  {
    Options options = new Options();

    Option nodeIdOption = new Option("nodeId", true, "The node identifier for the xds client node");
    nodeIdOption.setRequired(false);
    options.addOption(nodeIdOption);

    Option nodeClusterOption =
        new Option("nodeCluster", true, "The local service cluster name where xds client is running");
    nodeClusterOption.setRequired(false);
    options.addOption(nodeClusterOption);

    Option xdsServerOption = new Option("xds", true, "xDS server address");
    xdsServerOption.setRequired(false);
    options.addOption(xdsServerOption);

    Option serviceNameOption = new Option("service", true, "Service name to discover");
    serviceNameOption.setRequired(false);
    options.addOption(serviceNameOption);

    CommandLineParser parser = new GnuParser();
    CommandLine cmd = parser.parse(options, args);

    Node node = Node.DEFAULT_NODE;
    if (cmd.hasOption("nodeId") && cmd.hasOption("nodeCluster"))
    {
      node = new Node(cmd.getOptionValue("nodeId"), cmd.getOptionValue("nodeCluster"), null);
    }

    String xdsServer = cmd.getOptionValue("xds", "localhost:15010");
    String serviceName = cmd.getOptionValue("service", "grpc-demo-service-1.prod.linkedin.com");

    XdsChannelFactory xdsChannelFactory = new XdsChannelFactory();
    XdsClientImpl xdsClient = new XdsClientImpl(node, xdsChannelFactory.createChannel(xdsServer),
        Executors.newSingleThreadScheduledExecutor());

    XdsToD2PropertiesAdaptor adaptor = new XdsToD2PropertiesAdaptor(xdsClient);
    adaptor.listenToService(serviceName);

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
    _xdsClient.watchXdsResource(clusterName, XdsClient.ResourceType.CDS, (XdsClient.CdsResourceWatcher) cdsUpdate ->
    {
      Map<String, Value> clusterPropertiesMap = cdsUpdate.getClusterProperties();
      if (_clusterEventBus != null)
      {
        _clusterEventBus.publishInitialize(clusterName,
            XdsToClusterPropertiesAdaptor.toClusterProperties(clusterName, clusterPropertiesMap));
      }

      String serviceName = cdsUpdate.getEdsServiceName();
      _xdsClient.watchXdsResource(serviceName, XdsClient.ResourceType.EDS, (XdsClient.EdsResourceWatcher) edsUpdate ->
      {
        List<LocalityLbEndpoints> endpoints = edsUpdate.getLocalityLbEndpoints();

        if (_uriEventBus != null)
        {
          _uriEventBus.publishInitialize(clusterName,
              XdsToUriPropertiesAdaptor.toUriProperties(clusterName, endpoints));
        }
      });
    });
  }

  public void listenToService(String serviceName)
  {
    _xdsClient.watchXdsResource(serviceName + GLOBAL_LISTENER_PORT_SUFFIX, XdsClient.ResourceType.LDS,
        (XdsClient.LdsResourceWatcher) ldsUpdate ->
        {
          HttpConnectionManager httpConnectionManager = ldsUpdate.getHttpConnectionManager();
          String routeConfigName = httpConnectionManager.getRds().getRouteConfigName();

          _xdsClient.watchXdsResource(routeConfigName, XdsClient.ResourceType.RDS,
              (XdsClient.RdsResourceWatcher) rdsUpdate ->
              {
                Map<String, Value> servicePropertiesMap = rdsUpdate.getServiceProperties();

                List<String> clusterNames = new ArrayList<>();
                for (VirtualHost virtualHost : rdsUpdate.getVirtualHosts())
                {
                  for (Route route : virtualHost.getRoutesList())
                  {
                    String clusterName = route.getRoute().getCluster();
                    clusterNames.add(clusterName);
                  }
                }

                if (clusterNames.isEmpty()) {
                  _log.error("Cannot find the belonging cluster in RDS response for service: " + serviceName);
                }

                if (clusterNames.size() != 1) {
                  _log.warn("Service " + serviceName + "should only map to a single cluster");
                }

                String clusterName = clusterNames.get(0);
                if (_serviceEventBus != null) {
                  _serviceEventBus.publishInitialize(serviceName,
                      XdsToServicePropertiesAdaptor.toServiceProperties(serviceName, clusterName, "/greeter", servicePropertiesMap));
                }
                listenToCluster(clusterName);
              });
        });
  }

  private static final class XdsToUriPropertiesAdaptor
  {
    static UriProperties toUriProperties(String clusterName, List<LocalityLbEndpoints> endpoints)
    {
      Map<URI, Map<Integer, PartitionData>> partitionDescriptions = new HashMap<>();
      for (LocalityLbEndpoints localityLbEndpoints : endpoints)
      {
        for (LbEndpoint endpoint : localityLbEndpoints.getLbEndpointsList())
        {
          int weight = endpoint.getLoadBalancingWeight().getValue();
          SocketAddress socketAddress = endpoint.getEndpoint().getAddress().getSocketAddress();
          URI uri = URI.create("http://127.0.0.1:34567");
          Map<Integer, PartitionData> partitionDataMap =
              Collections.singletonMap(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
          partitionDescriptions.put(uri, partitionDataMap);
        }
      }
      return new UriProperties(clusterName, partitionDescriptions);
    }
  }

  private static final class XdsToServicePropertiesAdaptor
  {
    static ServiceProperties toServiceProperties(String serviceName, String clusterName, String path,
        Map<String, Value> serviceProperties)
    {
      return new ServiceStoreProperties(serviceName, clusterName, path, Collections.singletonList("relative"),
          Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.singletonList("http"),
          Collections.emptySet());
    }
  }

  private static final class XdsToClusterPropertiesAdaptor
  {
    static ClusterProperties toClusterProperties(String clusterName, Map<String, Value> clusterProperties)
    {
      return new ClusterStoreProperties(clusterName, Arrays.asList("https", "http"));
    }
  }
}
