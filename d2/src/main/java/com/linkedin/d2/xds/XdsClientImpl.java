/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.xds;

import com.google.common.base.Joiner;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import indis.XdsD2;
import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.internal.BackoffPolicy;
import io.grpc.internal.ExponentialBackoffPolicy;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsClientImpl extends XdsClient
{
  private static final Logger _log = LoggerFactory.getLogger(XdsClientImpl.class);

  private final Map<String, ResourceSubscriber> _d2NodeSubscribers = new HashMap<>();
  private final Map<String, ResourceSubscriber> _d2NodeMapSubscribers = new HashMap<>();

  private final Node _node;
  private final ManagedChannel _managedChannel;
  private final ScheduledExecutorService _executorService;
  private final BackoffPolicy.Provider _backoffPolicyProvider = new ExponentialBackoffPolicy.Provider();

  private BackoffPolicy _retryBackoffPolicy;
  private AdsStream _adsStream;
  private boolean _shutdown;

  public XdsClientImpl(Node node, ManagedChannel managedChannel, ScheduledExecutorService executorService)
  {
    _node = node;
    _managedChannel = managedChannel;
    _executorService = executorService;
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

  @Override
  public void startRpcStream()
  {
    AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub =
        AggregatedDiscoveryServiceGrpc.newStub(_managedChannel);
    _adsStream = new AdsStream(stub);
    _adsStream.start();
    _log.info("ADS stream started");
  }

  @Override
  void shutdown()
  {
    _executorService.execute(() ->
    {
      _shutdown = true;
      _log.info("Shutting down");
      if (_adsStream != null) {
        _adsStream.close(Status.CANCELLED.withDescription("shutdown").asException());
      }
    });

    _executorService.shutdown();
  }

  private void handleD2NodeResponse(DiscoveryResponseData data)
  {
    Map<String, D2NodeUpdate> updates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    for (Resource resource: data.getResourcesList())
    {
      String resourceName = resource.getName();
      try
      {
        XdsD2.D2Node d2Node = resource.getResource().unpack(XdsD2.D2Node.class);
        updates.put(resourceName, new D2NodeUpdate(d2Node));
      } catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack D2Node response", e);
        errors.add("Failed to unpack D2Node response");
      }
    }

    handleResourceUpdate(updates, data.getResourceType(), data.getNonce(), errors);
  }

  private void handleD2NodeMapResponse(DiscoveryResponseData data)
  {
    Map<String, D2NodeMapUpdate> updates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    for (Resource resource: data.getResourcesList())
    {
      String resourceName = resource.getName();
      try
      {
        XdsD2.D2NodeMap d2NodeMap = resource.getResource().unpack(XdsD2.D2NodeMap.class);
        Map<String, XdsD2.D2Node> nodeData = d2NodeMap.getNodesMap();
        updates.put(resourceName, new D2NodeMapUpdate(nodeData));
      } catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack D2NodeMap response", e);
        errors.add("Failed to unpack D2NodeMap response");
      }
    }

    handleResourceUpdate(updates, data.getResourceType(), data.getNonce(), errors);
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

  private void handleStreamClosed(Status error) {
    for (ResourceSubscriber subscriber : _d2NodeSubscribers.values()) {
      subscriber.onError(error);
    }
    for (ResourceSubscriber subscriber : _d2NodeMapSubscribers.values()) {
      subscriber.onError(error);
    }
  }

  private void handleStreamRestarted() {
    for (ResourceSubscriber subscriber : _d2NodeSubscribers.values()) {
      subscriber.onReconnect();
    }
    for (ResourceSubscriber subscriber : _d2NodeMapSubscribers.values()) {
      subscriber.onReconnect();
    }
  }

  private Map<String, ResourceSubscriber> getResourceSubscriberMap(ResourceType type)
  {
    switch (type)
    {
      case D2_NODE:
        return _d2NodeSubscribers;
      case D2_NODE_MAP:
        return _d2NodeMapSubscribers;
      case UNKNOWN:
      default:
        throw new AssertionError("Unknown resource type");
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
        case D2_NODE:
          ((D2NodeResourceWatcher) watcher).onChanged((D2NodeUpdate) update);
          break;
        case D2_NODE_MAP:
          ((D2NodeMapResourceWatcher) watcher).onChanged((D2NodeMapUpdate) update);
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

    private void onError(Status error)
    {
      for (ResourceWatcher watcher : _watchers)
      {
        watcher.onError(error);
      }
    }

    private void onReconnect()
    {
      for (ResourceWatcher watcher : _watchers)
      {
        watcher.onReconnect();
      }
    }
  }

  final class RpcRetryTask implements Runnable {
    @Override
    public void run() {
      if (_shutdown) {
        return;
      }
      startRpcStream();
      for (ResourceType type : ResourceType.values()) {
        if (type == ResourceType.UNKNOWN) {
          continue;
        }
        Map<String, ResourceSubscriber> subscriberMap = getResourceSubscriberMap(type);
        Collection<String> resources = subscriberMap.isEmpty() ? null : subscriberMap.keySet();
        if (resources != null) {
          _adsStream.sendDiscoveryRequest(type, resources);
        }
      }
      handleStreamRestarted();
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

  private final class AdsStream
  {
    private final AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub _stub;

    private boolean _closed;
    private boolean _responseReceived;
    private StreamObserver<DeltaDiscoveryRequest> _requestWriter;

    private AdsStream(@Nonnull AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub)
    {
      _stub = stub;
      _closed = false;
      _responseReceived = false;
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
        case D2_NODE:
          handleD2NodeResponse(response);
          break;
        case D2_NODE_MAP:
          handleD2NodeMapResponse(response);
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
      handleRpcStreamClosed(Status.fromThrowable(t));
    }

    private void handleRpcCompleted()
    {
      handleRpcStreamClosed(Status.UNAVAILABLE.withDescription("ADS stream closed by server"));
    }

    private void handleRpcStreamClosed(Status error)
    {
      if (_closed)
      {
        return;
      }
      _log.error("ADS stream closed with status {}: {}. Cause: {}", error.getCode(), error.getDescription(),
          error.getCause());
      _closed = true;
      handleStreamClosed(error);
      cleanUp();
      if (_responseReceived || _retryBackoffPolicy == null) {
        // Reset the backoff sequence if had received a response, or backoff sequence
        // has never been initialized.
        _retryBackoffPolicy = _backoffPolicyProvider.get();
      }
      long delayNanos = 0;
      if (!_responseReceived) {
        delayNanos = _retryBackoffPolicy.nextBackoffNanos();
      }
      _log.info("Retry ADS stream in {} ns", delayNanos);
      _executorService.schedule(new RpcRetryTask(), delayNanos, TimeUnit.NANOSECONDS);
    }

    private void close(Exception error) {
      if (_closed) {
        return;
      }
      _closed = true;
      cleanUp();
      _requestWriter.onError(error);
    }

    private void cleanUp()
    {
      if (_adsStream == this) {
        _adsStream = null;
      }
    }
  }
}
