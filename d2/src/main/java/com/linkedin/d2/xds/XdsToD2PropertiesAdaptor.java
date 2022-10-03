package com.linkedin.d2.xds;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import indis.Diskzk;
import io.grpc.Status;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsToD2PropertiesAdaptor
{
  private static final Logger _log = LoggerFactory.getLogger(XdsToD2PropertiesAdaptor.class);
  private static final String D2_CLUSTER_NODE_PREFIX = "/d2/clusters/";
  private static final String D2_SERVICE_NODE_PREFIX = "/d2/services/";
  private static final String D2_URI_NODE_PREFIX = "/d2/uris/";

  private final XdsClient _xdsClient;
  private final List<XdsConnectionListener> _xdsConnectionListeners;

  private final ServicePropertiesJsonSerializer _servicePropertiesJsonSerializer;
  private final ClusterPropertiesJsonSerializer _clusterPropertiesJsonSerializer;
  private final UriPropertiesJsonSerializer _uriPropertiesJsonSerializer;
  private final UriPropertiesMerger _uriPropertiesMerger;
  private boolean _isAvailable;

  private PropertyEventBus<UriProperties> _uriEventBus;
  private PropertyEventBus<ServiceProperties> _serviceEventBus;
  private PropertyEventBus<ClusterProperties> _clusterEventBus;

  public XdsToD2PropertiesAdaptor(XdsClient xdsClient)
  {
    _xdsClient = xdsClient;
    _xdsConnectionListeners = Collections.synchronizedList(new ArrayList<>());

    _servicePropertiesJsonSerializer = new ServicePropertiesJsonSerializer();
    _clusterPropertiesJsonSerializer = new ClusterPropertiesJsonSerializer();
    _uriPropertiesJsonSerializer = new UriPropertiesJsonSerializer();
    _uriPropertiesMerger = new UriPropertiesMerger();
    _isAvailable = false;
  }

  public void start()
  {
    _xdsClient.startRpcStream();
    notifyAvailabilityChanges(true);
  }

  public void shutdown()
  {
    _xdsClient.shutdown();
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

  public void registerXdsConnectionListener(XdsConnectionListener listener)
  {
    _xdsConnectionListeners.add(listener);
  }

  public void listenToCluster(String clusterName)
  {
    _xdsClient.watchXdsResource(D2_CLUSTER_NODE_PREFIX + clusterName, XdsClient.ResourceType.D2_NODE,
        getClusterResourceWatcher(clusterName));

    _xdsClient.watchXdsResource(D2_URI_NODE_PREFIX + clusterName, XdsClient.ResourceType.D2_NODE_MAP,
        getUriResourceWatcher(clusterName));
  }

  public void listenToService(String serviceName)
  {
    _xdsClient.watchXdsResource(D2_SERVICE_NODE_PREFIX + serviceName, XdsClient.ResourceType.D2_NODE,
        getServiceResourceWatcher(serviceName));
  }

  XdsClient.D2NodeResourceWatcher getServiceResourceWatcher(String serviceName)
  {
    return new XdsClient.D2NodeResourceWatcher()
    {
      @Override
      public void onChanged(XdsClient.D2NodeUpdate update)
      {
        if (_serviceEventBus != null)
        {
          try
          {
            _serviceEventBus.publishInitialize(serviceName, toServiceProperties(update.getNodeData().getData()));
          } catch (InvalidProtocolBufferException | PropertySerializationException e)
          {
            _log.error("Failed to parse D2 service properties from xDS update", e);
          }
        }
      }

      @Override
      public void onError(Status error)
      {
        notifyAvailabilityChanges(false);
      }

      @Override
      public void onReconnect()
      {
        notifyAvailabilityChanges(true);
      }
    };
  }

  XdsClient.D2NodeResourceWatcher getClusterResourceWatcher(String clusterName)
  {
    return new XdsClient.D2NodeResourceWatcher()
    {
      @Override
      public void onChanged(XdsClient.D2NodeUpdate update)
      {
        if (_clusterEventBus != null)
        {
          try
          {
            _clusterEventBus.publishInitialize(clusterName, toClusterProperties(update.getNodeData().getData()));
          } catch (InvalidProtocolBufferException | PropertySerializationException e)
          {
            _log.error("Failed to parse D2 cluster properties from xDS update", e);
          }
        }
      }

      @Override
      public void onError(Status error)
      {
        notifyAvailabilityChanges(false);
      }

      @Override
      public void onReconnect()
      {
        notifyAvailabilityChanges(true);
      }
    };
  }

  XdsClient.D2NodeMapResourceWatcher getUriResourceWatcher(String clusterName)
  {
    return new XdsClient.D2NodeMapResourceWatcher()
    {
      @Override
      public void onChanged(XdsClient.D2NodeMapUpdate update)
      {
        if (_uriEventBus != null)
        {
          try
          {
            _uriEventBus.publishInitialize(clusterName, toUriProperties(clusterName, update.getNodeDataMap()));
          }  catch (InvalidProtocolBufferException | PropertySerializationException e)
          {
            _log.error("Failed to parse D2 URI properties from xDS update", e);
          }
        }
      }

      @Override
      public void onError(Status error)
      {
        notifyAvailabilityChanges(false);
      }

      @Override
      public void onReconnect()
      {
        notifyAvailabilityChanges(true);
      }
    };
  }

  private void notifyAvailabilityChanges(boolean isAvailable)
  {
    synchronized (_xdsConnectionListeners)
    {
      if (_isAvailable != isAvailable)
      {
        _isAvailable = isAvailable;

        for (XdsConnectionListener xdsConnectionListener : _xdsConnectionListeners)
        {
          if (_isAvailable)
          {
            xdsConnectionListener.onReconnect();
          }
          else
          {
            xdsConnectionListener.onError();
          }
        }
      }
    }
  }

  private ServiceProperties toServiceProperties(Struct serviceProperties)
      throws InvalidProtocolBufferException, PropertySerializationException
  {
    return _servicePropertiesJsonSerializer.fromBytes(
        JsonFormat.printer().print(serviceProperties).getBytes(StandardCharsets.UTF_8));
  }

  private ClusterProperties toClusterProperties(Struct clusterProperties)
      throws InvalidProtocolBufferException, PropertySerializationException
  {
    return _clusterPropertiesJsonSerializer.fromBytes(
        JsonFormat.printer().print(clusterProperties).getBytes(StandardCharsets.UTF_8));
  }

  private UriProperties toUriProperties(String clusterName, Map<String, Diskzk.D2Node> uriDataMap)
      throws InvalidProtocolBufferException, PropertySerializationException
  {
    List<UriProperties> allUriProperties = new ArrayList<>();

    for (Diskzk.D2Node d2Node : uriDataMap.values())
    {
      UriProperties uriProperties = _uriPropertiesJsonSerializer.fromBytes(
          JsonFormat.printer().print(d2Node.getData()).getBytes(StandardCharsets.UTF_8));
      allUriProperties.add(uriProperties);
    }

    return _uriPropertiesMerger.merge(clusterName, allUriProperties);
  }

  public interface XdsConnectionListener
  {
    void onError();

    void onReconnect();
  }
}
