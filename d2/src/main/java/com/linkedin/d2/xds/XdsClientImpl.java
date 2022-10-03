package com.linkedin.d2.xds;

import com.google.common.base.Joiner;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.listener.v3.Filter;
import io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsClientImpl extends XdsClient
{
  private static final String TYPE_URL_HTTP_CONNECTION_MANAGER =
      "type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager";
  private static final Logger _log = LoggerFactory.getLogger(XdsClientImpl.class);

  private static final String D2_CLUSTER_PROPERTIES_KEY = "d2ClusterProperties";
  private static final String D2_SERVICE_PROPERTIES_KEY = "d2ServiceProperties";

  private final Node _node;
  private final ManagedChannel _managedChannel;
  private final ExecutorService _executorService;

  private final Map<String, ResourceSubscriber> _ldsResourceSubscribers = new HashMap<>();
  private final Map<String, ResourceSubscriber> _rdsResourceSubscribers = new HashMap<>();
  private final Map<String, ResourceSubscriber> _cdsResourceSubscribers = new HashMap<>();
  private final Map<String, ResourceSubscriber> _edsResourceSubscribers = new HashMap<>();

  private AdsStream _adsStream;

  public XdsClientImpl(Node node, ManagedChannel managedChannel, ExecutorService executorService)
  {
    _node = node;
    _managedChannel = managedChannel;
    _executorService = executorService;
  }

  private void startRpcStream()
  {
    AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub =
        AggregatedDiscoveryServiceGrpc.newStub(_managedChannel);
    _adsStream = new AdsStream(stub);
    _adsStream.start();
    _log.info("ADS stream started");
  }

  @Override
  void watchXdsResource(String resourceName, ResourceType type, ResourceWatcher watcher)
  {
    _executorService.execute(() ->
    {
      Map<String, ResourceSubscriber> resourceSubscriberMap = getResourceSubscriberMap(type);
      ResourceSubscriber subscriber = resourceSubscriberMap.get(resourceName);
      if (subscriber == null)
      {
        _log.info("Subscribe {} resource {}", type, resourceName);
        subscriber = new ResourceSubscriber(type, resourceName);
        resourceSubscriberMap.put(resourceName, subscriber);

        if (_adsStream == null)
        {
          startRpcStream();
        }
        _adsStream.sendDiscoveryRequest(type, Collections.singletonList(resourceName));
      }
      subscriber.addWatcher(watcher);
    });
  }

  private void handleLdsResponse(DiscoveryResponseData responseData)
  {
    Map<String, LdsUpdate> ldsUpdates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    for (Resource resource : responseData.getResourcesList())
    {
      try
      {
        Listener listener = resource.getResource().unpack(Listener.class);

        for (FilterChain filterChain : listener.getFilterChainsList())
        {
          for (Filter filter : filterChain.getFiltersList())
          {
            if (filter.hasTypedConfig() && filter.getTypedConfig()
                .getTypeUrl()
                .equals(TYPE_URL_HTTP_CONNECTION_MANAGER))
            {
              HttpConnectionManager httpConnectionManager = filter.getTypedConfig().unpack(HttpConnectionManager.class);
              ldsUpdates.put(listener.getName(), new LdsUpdate(httpConnectionManager));
            }
          }
        }
      } catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack LDS response", e);
        errors.add("Failed to unpack LDS response");
      }
    }

    _log.info("Received LDS response nonce {}, resource updates {}", responseData.getNonce(), ldsUpdates);
    handleResourceUpdate(ldsUpdates, responseData.getResourceType(), responseData.getNonce(), errors);
  }

  private void handleRdsResponse(DiscoveryResponseData responseData)
  {
    Map<String, RdsUpdate> rdsUpdates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    for (Resource resource : responseData.getResourcesList())
    {
      try
      {
        RouteConfiguration routeConfig = resource.getResource().unpack(RouteConfiguration.class);
        List<VirtualHost> virtualHostList = routeConfig.getVirtualHostsList();
        Map<String, Value> serviceProperties = null;
        for (VirtualHost virtualHost : virtualHostList)
        {
          for (Route route : virtualHost.getRoutesList())
          {
            Map<String, Struct> metadataMap = route.getMetadata().getFilterMetadataMap();
            if (metadataMap.containsKey(D2_SERVICE_PROPERTIES_KEY))
            {
              serviceProperties = metadataMap.get(D2_SERVICE_PROPERTIES_KEY).getFieldsMap();
            }
          }
        }
        if (serviceProperties == null)
        {
          _log.warn("Missing service properties in RDS response");
          errors.add("Missing service properties in RDS response");
          continue;
        }
        rdsUpdates.put(routeConfig.getName(), new RdsUpdate(routeConfig.getVirtualHostsList(), serviceProperties));
      } catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack RDS response", e);
        errors.add("Failed to unpack RDS response");
      }
    }

    _log.info("Received RDS response nonce {}, resource updates {}", responseData.getNonce(), rdsUpdates);
    handleResourceUpdate(rdsUpdates, responseData.getResourceType(), responseData.getNonce(), errors);
  }

  private void handleCdsResponse(DiscoveryResponseData responseData)
  {
    Map<String, CdsUpdate> cdsUpdates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    for (Resource resource : responseData.getResourcesList())
    {
      try
      {
        Cluster cluster = resource.getResource().unpack(Cluster.class);
        String clusterName = cluster.getName();
        Map<String, Struct> metadataMap = cluster.getMetadata().getFilterMetadataMap();
        if (!metadataMap.containsKey(D2_CLUSTER_PROPERTIES_KEY))
        {
          _log.warn("Missing cluster properties in CDS response");
          errors.add("Missing cluster properties in CDS response");
          continue;
        }
        cdsUpdates.put(clusterName, new CdsUpdate(clusterName, cluster.getEdsClusterConfig().getServiceName(),
            metadataMap.get(D2_CLUSTER_PROPERTIES_KEY).getFieldsMap()));
      } catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack CDS response", e);
        errors.add("Failed to unpack CDS response");
      }
    }

    _log.info("Received CDS response nonce {}, resource updates {}", responseData.getNonce(), cdsUpdates);
    handleResourceUpdate(cdsUpdates, responseData.getResourceType(), responseData.getNonce(), errors);
  }

  private void handleEdsResponse(DiscoveryResponseData responseData)
  {
    Map<String, EdsUpdate> edsUpdates = new HashMap<>();
    List<String> errors = new ArrayList<>();
    for (Resource resource : responseData.getResourcesList())
    {
      try
      {
        ClusterLoadAssignment clusterLoadAssignment = resource.getResource().unpack(ClusterLoadAssignment.class);
        String clusterName = clusterLoadAssignment.getClusterName();
        edsUpdates.put(clusterName, new EdsUpdate(clusterName, clusterLoadAssignment.getEndpointsList()));
      } catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack EDS response", e);
        errors.add("Failed to unpack EDS response");
      }
    }

    _log.info("Received LDS response nonce {}, resource updates {}", responseData.getNonce(), edsUpdates);
    handleResourceUpdate(edsUpdates, responseData.getResourceType(), responseData.getNonce(), errors);
  }

  private void handleResourceUpdate(Map<String, ? extends ResourceUpdate> updates, ResourceType type, String nonce,
      List<String> errors)
  {
    if (errors.isEmpty())
    {
      _adsStream.sendAckRequest(type, nonce);
    } else
    {
      String errorDetail = Joiner.on('\n').join(errors);
      _adsStream.sendNackRequest(type, nonce, errorDetail);
    }

    for (Map.Entry<String, ? extends ResourceUpdate> entry : updates.entrySet())
    {
      String resourceName = entry.getKey();
      ResourceUpdate resourceUpdate = entry.getValue();
      ResourceSubscriber subscriber = getResourceSubscriberMap(type).get(resourceName);
      if (subscriber != null)
      {
        subscriber.onData(resourceUpdate);
      }
    }
  }

  private Map<String, ResourceSubscriber> getResourceSubscriberMap(ResourceType type)
  {
    switch (type)
    {
      case LDS:
        return _ldsResourceSubscribers;
      case RDS:
        return _rdsResourceSubscribers;
      case CDS:
        return _cdsResourceSubscribers;
      case EDS:
        return _edsResourceSubscribers;
      case UNKNOWN:
      default:
        throw new AssertionError("Unknown resource type");
    }
  }

  private static final class AckOrNack
  {
    private final Node _node;
    private final ResourceType _resourceType;
    private final String _responseNonce;
    @Nullable
    private final com.google.rpc.Status _errorDetail;

    AckOrNack(Node node, ResourceType resourceType, String responseNonce)
    {
      this(node, resourceType, responseNonce, null);
    }

    AckOrNack(Node node, ResourceType resourceType, String responseNonce, @Nullable com.google.rpc.Status errorDetail)
    {
      _node = node;
      _resourceType = resourceType;
      _responseNonce = responseNonce;
      _errorDetail = errorDetail;
    }

    DeltaDiscoveryRequest toEnvoyProto()
    {
      DeltaDiscoveryRequest.Builder builder = DeltaDiscoveryRequest.newBuilder()
          .setNode(_node.toEnvoyProtoNode())
          .setTypeUrl(_resourceType.typeUrl())
          .setResponseNonce(_responseNonce);

      if (_errorDetail != null)
      {
        builder.setErrorDetail(_errorDetail);
      }
      return builder.build();
    }

    @Override
    public String toString()
    {
      return "AckOrNack{" + "_node=" + _node + ", _resourceType=" + _resourceType + ", _responseNonce='"
          + _responseNonce + '\'' + ", _errorDetail=" + _errorDetail + '}';
    }
  }

  private static final class DiscoveryRequestData
  {
    private final Node _node;
    private final ResourceType _resourceType;
    private final Collection<String> _resourceNames;

    DiscoveryRequestData(Node node, ResourceType resourceType, Collection<String> resourceNames)
    {
      _node = node;
      _resourceType = resourceType;
      _resourceNames = resourceNames;
    }

    DeltaDiscoveryRequest toEnvoyProto()
    {
      DeltaDiscoveryRequest.Builder builder = DeltaDiscoveryRequest.newBuilder()
          .setNode(_node.toEnvoyProtoNode())
          .addAllResourceNamesSubscribe(_resourceNames)
          .setTypeUrl(_resourceType.typeUrl());

      return builder.build();
    }

    @Override
    public String toString()
    {
      return "DiscoveryRequestData{" + "_node=" + _node + ", _resourceType=" + _resourceType + ", _resourceNames="
          + _resourceNames + '}';
    }
  }

  private static final class DiscoveryResponseData
  {
    private final ResourceType _resourceType;
    private final List<Resource> _resources;
    private final String _nonce;

    DiscoveryResponseData(ResourceType resourceType, List<Resource> resources, String nonce)
    {
      _resourceType = resourceType;
      _resources = resources;
      _nonce = nonce;
    }

    static DiscoveryResponseData fromEnvoyProto(DeltaDiscoveryResponse proto)
    {
      return new DiscoveryResponseData(ResourceType.fromTypeUrl(proto.getTypeUrl()), proto.getResourcesList(),
          proto.getNonce());
    }

    ResourceType getResourceType()
    {
      return _resourceType;
    }

    List<Resource> getResourcesList()
    {
      return _resources;
    }

    String getNonce()
    {
      return _nonce;
    }

    @Override
    public String toString()
    {
      return "DiscoveryResponseData{" + "_resourceType=" + _resourceType + ", _resources=" + _resources + ", _nonce='"
          + _nonce + '\'' + '}';
    }
  }

  private static final class ResourceSubscriber
  {
    private final ResourceType _type;
    private final String _resource;
    private final Set<ResourceWatcher> _watchers = new HashSet<>();
    @Nullable
    private ResourceUpdate _data;

    ResourceSubscriber(ResourceType type, String resource)
    {
      _type = type;
      _resource = resource;
    }

    void addWatcher(ResourceWatcher watcher)
    {
      if (_watchers.contains(watcher))
      {
        _log.warn("Watcher {} already registered", watcher);
        return;
      }
      _watchers.add(watcher);
      if (_data != null)
      {
        notifyWatcher(watcher, _data);
      }
    }

    private void notifyWatcher(ResourceWatcher watcher, ResourceUpdate update)
    {
      switch (_type)
      {
        case LDS:
          ((LdsResourceWatcher) watcher).onChanged((LdsUpdate) update);
          break;
        case RDS:
          ((RdsResourceWatcher) watcher).onChanged((RdsUpdate) update);
          break;
        case CDS:
          ((CdsResourceWatcher) watcher).onChanged((CdsUpdate) update);
          break;
        case EDS:
          ((EdsResourceWatcher) watcher).onChanged((EdsUpdate) update);
          break;
        case UNKNOWN:
        default:
          throw new AssertionError("should never be here");
      }
    }

    private void onData(ResourceUpdate data)
    {
      if (Objects.equals(_data, data))
      {
        _log.debug("Received resource update data equal to the current data. Will not perform the update.");
        return;
      }
      _data = data;
      for (ResourceWatcher watcher : _watchers)
      {
        notifyWatcher(watcher, data);
      }
    }
  }

  private final class AdsStream
  {
    private final AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub _stub;

    private boolean _closed;
    private StreamObserver<DeltaDiscoveryRequest> _requestWriter;

    private AdsStream(@Nonnull AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub)
    {
      _stub = stub;
      _closed = false;
    }

    private void start()
    {
      StreamObserver<DeltaDiscoveryResponse> responseReader = new StreamObserver<DeltaDiscoveryResponse>()
      {
        @Override
        public void onNext(DeltaDiscoveryResponse response)
        {
          _executorService.execute(() ->
          {
            _log.debug("Received {} response:\n{}", ResourceType.fromTypeUrl(response.getTypeUrl()), response);
            DiscoveryResponseData responseData = DiscoveryResponseData.fromEnvoyProto(response);
            handleResponse(responseData);
          });
        }

        @Override
        public void onError(Throwable t)
        {
          _executorService.execute(() -> handleRpcError(t));
        }

        @Override
        public void onCompleted()
        {
          _executorService.execute(() -> handleRpcCompleted());
        }
      };
      _requestWriter = _stub.withWaitForReady().deltaAggregatedResources(responseReader);
    }

    /**
     * Sends a client-initiated discovery request.
     */
    private void sendDiscoveryRequest(ResourceType type, Collection<String> resources)
    {
      _log.info("Sending {} request for resources: {}", type, resources);
      DiscoveryRequestData request = new DiscoveryRequestData(_node, type, resources);
      _requestWriter.onNext(request.toEnvoyProto());
      _log.debug("Sent DiscoveryRequest\n{}", request);
    }

    private void sendAckRequest(ResourceType resourceType, String nonce)
    {
      AckOrNack ack = new AckOrNack(_node, resourceType, nonce);
      _requestWriter.onNext(ack.toEnvoyProto());
      _log.debug("Sent Ack\n{}", ack);
    }

    private void sendNackRequest(ResourceType resourceType, String nonce, @Nullable String errorDetail)
    {
      com.google.rpc.Status error = null;
      if (errorDetail != null)
      {
        error = com.google.rpc.Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage(errorDetail).build();
      }
      AckOrNack ack = new AckOrNack(_node, resourceType, nonce, error);
      _requestWriter.onNext(ack.toEnvoyProto());
      _log.debug("Sent Nack\n{}", ack);
    }

    private void handleResponse(DiscoveryResponseData response)
    {
      if (_closed)
      {
        return;
      }
      String respNonce = response.getNonce();
      ResourceType resourceType = response.getResourceType();
      switch (resourceType)
      {
        case LDS:
          handleLdsResponse(response);
          break;
        case RDS:
          handleRdsResponse(response);
          break;
        case CDS:
          handleCdsResponse(response);
          break;
        case EDS:
          handleEdsResponse(response);
          break;
        case UNKNOWN:
          _log.warn("Received an unknown type of DiscoveryResponse\n{}", respNonce);
          break;
        default:
          throw new AssertionError("Missing case in enum switch: " + resourceType);
      }
    }

    private void handleRpcError(Throwable t)
    {
      handleStreamClosed(Status.fromThrowable(t));
    }

    private void handleRpcCompleted()
    {
      handleStreamClosed(Status.UNAVAILABLE.withDescription("ADS stream closed by server"));
    }

    private void handleStreamClosed(Status error)
    {
      if (_closed)
      {
        return;
      }
      _log.error("ADS stream closed with status {}: {}. Cause: {}", error.getCode(), error.getDescription(),
          error.getCause());
      _closed = true;
      // TODO: Handle connection retry here
    }
  }
}
