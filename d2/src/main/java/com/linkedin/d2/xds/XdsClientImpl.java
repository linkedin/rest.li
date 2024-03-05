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
import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.linkedin.d2.jmx.XdsClientJmx;
import indis.XdsD2;
import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.Resource;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.internal.BackoffPolicy;
import io.grpc.internal.ExponentialBackoffPolicy;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsClientImpl extends XdsClient
{
  private static final Logger _log = LoggerFactory.getLogger(XdsClientImpl.class);
  public static final long DEFAULT_READY_TIMEOUT_MILLIS = 2000L;

  private final Map<String, ResourceSubscriber> _d2NodeSubscribers = new HashMap<>();
  private final Map<String, ResourceSubscriber> _d2URIMapSubscribers = new HashMap<>();

  private final Node _node;
  private final ManagedChannel _managedChannel;
  private final ScheduledExecutorService _executorService;
  private final BackoffPolicy.Provider _backoffPolicyProvider = new ExponentialBackoffPolicy.Provider();

  private BackoffPolicy _retryBackoffPolicy;
  private AdsStream _adsStream;
  private boolean _shutdown;
  private ScheduledFuture<?> _retryRpcStreamFuture;
  private ScheduledFuture<?> _readyTimeoutFuture;
  private final long _readyTimeoutMillis;

  private final XdsClientJmx _xdsClientJmx;

  @Deprecated
  public XdsClientImpl(Node node, ManagedChannel managedChannel, ScheduledExecutorService executorService)
  {
    this(node, managedChannel, executorService, DEFAULT_READY_TIMEOUT_MILLIS);
  }

  public XdsClientImpl(Node node, ManagedChannel managedChannel, ScheduledExecutorService executorService,
      long readyTimeoutMillis)
  {
    _readyTimeoutMillis = readyTimeoutMillis;
    _node = node;
    _managedChannel = managedChannel;
    _executorService = executorService;
    _xdsClientJmx = new XdsClientJmx();
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

        if (_adsStream == null && !isInBackoff())
        {
          startRpcStreamLocal();
        }
        if (_adsStream != null) {
          _adsStream.sendDiscoveryRequest(type, Collections.singletonList(resourceName));
        }
      }
      subscriber.addWatcher(watcher);
    });
  }

  @Override
  public void startRpcStream()
  {
    _executorService.execute(() -> {
      if (!isInBackoff()) {
        startRpcStreamLocal();
      }
    });
  }

  @Override
  public XdsClientJmx getXdsClientJmx()
  {
    return _xdsClientJmx;
  }

  // Start RPC stream. Must be called from the executor, and only if we're not backed off.
  private void startRpcStreamLocal() {
    if (_shutdown) {
      _log.warn("RPC stream cannot be started after shutdown!");
      return;
    }
    // Check rpc stream is null to ensure duplicate RPC retry tasks are no-op
    if (_adsStream != null) {
      _log.warn("Tried to create duplicate RPC stream, ignoring!");
      return;
    }
    AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub =
        AggregatedDiscoveryServiceGrpc.newStub(_managedChannel);
    _adsStream = new AdsStream(stub);
    _readyTimeoutFuture = _executorService.schedule(() -> {
      _log.warn("ADS stream not ready within {} milliseconds", _readyTimeoutMillis);
      // notify subscribers about the error and wait for the stream to be ready by keeping it open.
      notifyStreamError(Status.DEADLINE_EXCEEDED);
      // note: no need to start a retry task explicitly since xds stream internally will keep on retrying to connect
      // to one of the sub-channels (unless an error or complete callback is called).
    }, _readyTimeoutMillis, TimeUnit.MILLISECONDS);
    _adsStream.start();
    _log.info("ADS stream started, connected to server: {}", _managedChannel.authority());
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
  }

  @Override
  String getXdsServerAuthority()
  {
    return _managedChannel.authority();
  }

  /**
   * The client may be in backoff if there are RPC stream failures, and if it's waiting to establish the stream again.
   * NOTE: Must be called from the executor.
   * @return {@code true} if the client is in backoff
   */
  private boolean isInBackoff()
  {
    return _adsStream == null && _retryRpcStreamFuture != null && !_retryRpcStreamFuture.isDone();
  }

  /**
   * Handles ready callbacks from the RPC stream. Must be called from the executor.
   */
  private void readyHandler()
  {
    _log.debug("Received ready callback from the ADS stream");
    if (_adsStream == null || isInBackoff())
    {
      _log.warn("Unexpected state, ready called on null or backed off ADS stream!");
      return;
    }
    // confirm ready state to neglect spurious callbacks; we'll get another callback whenever it is ready again.
    if (_adsStream.isReady())
    {
      // if the ready timeout future is non-null, a reconnect notification hasn't been sent yet.
      if (_readyTimeoutFuture != null)
      {
        // timeout task will be cancelled only if it hasn't already executed.
        boolean cancelledTimeout = _readyTimeoutFuture.cancel(false);
        _log.info("ADS stream ready, cancelled timeout task: {}", cancelledTimeout);
        _readyTimeoutFuture = null; // set it to null to avoid repeat notifications to subscribers.
        _xdsClientJmx.incrementReconnectionCount();
        notifyStreamReconnect();
      }
      _xdsClientJmx.setIsConnected(true);
    }
  }

  private void handleD2NodeResponse(DiscoveryResponseData data)
  {
    Map<String, NodeUpdate> updates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    for (Resource resource : data.getResourcesList())
    {
      String resourceName = resource.getName();
      try
      {
        XdsD2.Node d2Node = resource.getResource().unpack(XdsD2.Node.class);
        if (d2Node != null && d2Node.getData().isEmpty())
        {
          _log.warn("Received a Node response with no data, resource is : {}", resourceName);
        }
        updates.put(resourceName, new NodeUpdate(resource.getVersion(), d2Node));
      }
      catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack Node response", e);
        errors.add("Failed to unpack Node response");
      }
    }
    sendAckOrNack(data.getResourceType(), data.getNonce(), errors);
    handleResourceUpdate(updates, data.getResourceType());
    handleResourceRemoval(data.getRemovedResources(), data.getResourceType());
  }

  private void handleD2URIMapResponse(DiscoveryResponseData data)
  {
    Map<String, D2URIMapUpdate> updates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    for (Resource resource: data.getResourcesList())
    {
      String resourceName = resource.getName();
      try
      {
        XdsD2.D2URIMap uriMap = resource.getResource().unpack(XdsD2.D2URIMap.class);
        Map<String, XdsD2.D2URI> nodeData = uriMap.getUrisMap();
        if (nodeData.isEmpty())
        {
          _log.warn("Received a D2URIMap response with no data, resource is : {}", resourceName);
        }
        updates.put(resourceName, new D2URIMapUpdate(resource.getVersion(), nodeData));
      }
      catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack D2URIMap response", e);
        errors.add("Failed to unpack D2URIMap response");
      }
    }
    sendAckOrNack(data.getResourceType(), data.getNonce(), errors);
    handleResourceUpdate(updates, data.getResourceType());
    handleResourceRemoval(data.getRemovedResources(), data.getResourceType());
  }

  private void sendAckOrNack(ResourceType type, String nonce, List<String> errors)
  {
    if (errors.isEmpty())
    {
      _adsStream.sendAckRequest(type, nonce);
    }
    else
    {
      String errorDetail = Joiner.on('\n').join(errors);
      _adsStream.sendNackRequest(type, nonce, errorDetail);
    }
  }

  private void handleResourceUpdate(Map<String, ? extends ResourceUpdate> updates, ResourceType type)
  {
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

  private void handleResourceRemoval(List<String> removedResources, ResourceType type)
  {
    for (String resourceName : removedResources)
    {
      _xdsClientJmx.incrementResourceNotFoundCount();
      _log.warn("Received response that resource {} is removed", resourceName);
      ResourceSubscriber subscriber = getResourceSubscriberMap(type).get(resourceName);
      if (subscriber != null)
      {
        subscriber.onRemoval();
      }
    }
  }

  private void notifyStreamError(Status error) {
    for (ResourceSubscriber subscriber : _d2NodeSubscribers.values()) {
      subscriber.onError(error);
    }
    for (ResourceSubscriber subscriber : _d2URIMapSubscribers.values()) {
      subscriber.onError(error);
    }
  }

  private void notifyStreamReconnect() {
    for (ResourceSubscriber subscriber : _d2NodeSubscribers.values()) {
      subscriber.onReconnect();
    }
    for (ResourceSubscriber subscriber : _d2URIMapSubscribers.values()) {
      subscriber.onReconnect();
    }
  }

  private Map<String, ResourceSubscriber> getResourceSubscriberMap(ResourceType type)
  {
    switch (type)
    {
      case NODE:
        return _d2NodeSubscribers;
      case D2_URI_MAP:
        return _d2URIMapSubscribers;
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
        case NODE:
          if (watcher instanceof  NodeResourceWatcher) {
            ((NodeResourceWatcher) watcher).onChanged((NodeUpdate) update);
          } else {
            ((SymlinkNodeResourceWatcher) watcher).onChanged(_resource, (NodeUpdate) update);
          }
          break;
        case D2_URI_MAP:
          ((D2URIMapResourceWatcher) watcher).onChanged((D2URIMapUpdate) update);
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
      if (!isEmptyData(data))
      { // null value guard to avoid overwriting the property with null
        _data = data;
      }
      for (ResourceWatcher watcher : _watchers)
      {
        notifyWatcher(watcher, data);
      }
    }


    private boolean isEmptyData(ResourceUpdate data)
    {
      if (data == null)
      {
        return true;
      }
      switch (_type)
      {
        case NODE:
          NodeUpdate nodeUpdate = (NodeUpdate) data;
          return nodeUpdate.getNodeData() == null || nodeUpdate.getNodeData().getData().isEmpty();
        case D2_URI_MAP:
          D2URIMapUpdate uriMapUpdate = (D2URIMapUpdate) data;
          return uriMapUpdate.getURIMap() == null || uriMapUpdate.getURIMap().isEmpty();
        case UNKNOWN:
        default:
          return true;
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

    private void onRemoval()
    {
      // When the client receive the removal data from INDIS or ZK,
      // client side doesn't delete the data from the cache which is just a design choice by now.
      // So to avoid the eventbus watcher timeout, in there directly notify the watcher with cache data
      for (ResourceWatcher watcher : _watchers)
      {
        notifyWatcher(watcher, _data);
      }
    }
  }

  final class RpcRetryTask implements Runnable {
    @Override
    public void run() {
      startRpcStreamLocal();
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
    private final List<String> _removedResources;
    private final String _nonce;
    @Nullable
    private final String _controlPlaneIdentifier;

    DiscoveryResponseData(ResourceType resourceType, List<Resource> resources, List<String> removedResources, String nonce,
                        @Nullable String controlPlaneIdentifier)
    {
      _resourceType = resourceType;
      _resources = resources;
      _nonce = nonce;
      _removedResources = removedResources;
      _controlPlaneIdentifier = controlPlaneIdentifier;
    }

    static DiscoveryResponseData fromEnvoyProto(DeltaDiscoveryResponse proto)
    {
      return new DiscoveryResponseData(ResourceType.fromTypeUrl(proto.getTypeUrl()), proto.getResourcesList(),
              proto.getRemovedResourcesList(), proto.getNonce(), Strings.emptyToNull(proto.getControlPlane().getIdentifier()));
    }

    ResourceType getResourceType()
    {
      return _resourceType;
    }

    List<Resource> getResourcesList()
    {
      return _resources;
    }

    List<String> getRemovedResources()
    {
      return _removedResources;
    }

    String getNonce()
    {
      return _nonce;
    }

    @Nullable
    String getControlPlaneIdentifier()
    {
      return _controlPlaneIdentifier;
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

    public boolean isReady() {
      return _requestWriter != null && ((ClientCallStreamObserver<?>) _requestWriter).isReady();
    }

    private void start()
    {
      StreamObserver<DeltaDiscoveryResponse> responseReader =
          new ClientResponseObserver<DeltaDiscoveryRequest, DeltaDiscoveryResponse>() {
            @Override
            public void beforeStart(ClientCallStreamObserver<DeltaDiscoveryRequest> requestStream) {
              requestStream.setOnReadyHandler(() -> _executorService.execute(XdsClientImpl.this::readyHandler));
            }

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
      if (!_responseReceived && response.getControlPlaneIdentifier() != null)
      {
        _log.info("Successfully established stream with ADS server: {}", response.getControlPlaneIdentifier());
      }
      _responseReceived = true;
      String respNonce = response.getNonce();
      ResourceType resourceType = response.getResourceType();
      switch (resourceType)
      {
        case NODE:
          handleD2NodeResponse(response);
          break;
        case D2_URI_MAP:
          handleD2URIMapResponse(response);
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
      _xdsClientJmx.incrementConnectionLostCount();
      _xdsClientJmx.setIsConnected(false);
      handleRpcStreamClosed(Status.fromThrowable(t));
    }

    private void handleRpcCompleted()
    {
      _xdsClientJmx.incrementConnectionClosedCount();
      _xdsClientJmx.setIsConnected(false);
      handleRpcStreamClosed(Status.UNAVAILABLE.withDescription("ADS stream closed by server"));
    }

    // Must be called from the executor.
    private void handleRpcStreamClosed(Status error)
    {
      if (_closed)
      {
        return;
      }
      _log.error("ADS stream closed with status {}: {}. Cause: {}", error.getCode(), error.getDescription(),
          error.getCause());
      _closed = true;
      notifyStreamError(error);
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
      _retryRpcStreamFuture = _executorService.schedule(new RpcRetryTask(), delayNanos, TimeUnit.NANOSECONDS);
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
