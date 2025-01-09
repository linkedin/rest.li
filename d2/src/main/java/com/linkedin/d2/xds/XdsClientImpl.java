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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.linkedin.d2.jmx.NoOpXdsServerMetricsProvider;
import com.linkedin.d2.jmx.XdsClientJmx;
import com.linkedin.d2.jmx.XdsServerMetricsProvider;
import com.linkedin.d2.xds.GlobCollectionUtils.D2UriIdentifier;
import com.linkedin.util.RateLimitedLogger;
import com.linkedin.util.clock.SystemClock;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link XdsClient} interface.
 */
public class XdsClientImpl extends XdsClient
{
  private static final Logger _log = LoggerFactory.getLogger(XdsClientImpl.class);
  private static final RateLimitedLogger RATE_LIMITED_LOGGER =
      new RateLimitedLogger(_log, TimeUnit.MINUTES.toMillis(10), SystemClock.instance());
  public static final long DEFAULT_READY_TIMEOUT_MILLIS = 2000L;

  /**
   * The resource subscriber map stores the subscribers to specific resources of a given type. Note that it only has 2
   * keys: {@link ResourceType#D2_URI_MAP} and {@link ResourceType#NODE}. The {@link ResourceType#D2_URI} is absent from
   * this map because it should not be used as a key, as glob collection updates are translated to appear as normal map
   * updates to subscribers.
   */
  private final Map<ResourceType, Map<String, ResourceSubscriber>> _resourceSubscribers = Maps.immutableEnumMap(
      Stream.of(ResourceType.NODE, ResourceType.D2_URI_MAP)
          .collect(Collectors.toMap(Function.identity(), e -> new HashMap<>())));
  private final Map<ResourceType, WildcardResourceSubscriber> _wildcardSubscribers = Maps.newEnumMap(ResourceType.class);
  private final Map<String, D2UriSubscriber> _d2UriSubscribers = new HashMap<>();
  private final Node _node;
  private final ManagedChannel _managedChannel;
  private final ScheduledExecutorService _executorService;
  private final boolean _subscribeToUriGlobCollection;
  private final BackoffPolicy.Provider _backoffPolicyProvider = new ExponentialBackoffPolicy.Provider();
  private BackoffPolicy _retryBackoffPolicy;
  private AdsStream _adsStream;
  private boolean _shutdown;
  private ScheduledFuture<?> _retryRpcStreamFuture;
  private ScheduledFuture<?> _readyTimeoutFuture;
  private final long _readyTimeoutMillis;

  private final XdsClientJmx _xdsClientJmx;
  private final XdsServerMetricsProvider _serverMetricsProvider;

  @Deprecated
  public XdsClientImpl(Node node, ManagedChannel managedChannel, ScheduledExecutorService executorService)
  {
    this(node, managedChannel, executorService, DEFAULT_READY_TIMEOUT_MILLIS);
  }

  @Deprecated
  public XdsClientImpl(Node node, ManagedChannel managedChannel, ScheduledExecutorService executorService,
      long readyTimeoutMillis)
  {
    this(node, managedChannel, executorService, readyTimeoutMillis, false);
  }

  @Deprecated
  public XdsClientImpl(Node node, ManagedChannel managedChannel, ScheduledExecutorService executorService,
      long readyTimeoutMillis, boolean subscribeToUriGlobCollection)
  {
    this(node, managedChannel, executorService, readyTimeoutMillis, subscribeToUriGlobCollection,
        new NoOpXdsServerMetricsProvider());
  }

  public XdsClientImpl(Node node, ManagedChannel managedChannel, ScheduledExecutorService executorService,
      long readyTimeoutMillis, boolean subscribeToUriGlobCollection, XdsServerMetricsProvider serverMetricsProvider)
  {
    _readyTimeoutMillis = readyTimeoutMillis;
    _node = node;
    _managedChannel = managedChannel;
    _executorService = executorService;
    _subscribeToUriGlobCollection = subscribeToUriGlobCollection;
    if (_subscribeToUriGlobCollection)
    {
      _log.info("Glob collection support enabled");
    }

    _xdsClientJmx = new XdsClientJmx(serverMetricsProvider);
    _serverMetricsProvider = serverMetricsProvider == null ? new NoOpXdsServerMetricsProvider() : serverMetricsProvider;
  }

  @Override
  public void watchXdsResource(String resourceName, ResourceWatcher watcher)
  {
    _executorService.execute(() ->
    {
      Map<String, ResourceSubscriber> resourceSubscriberMap = getResourceSubscriberMap(watcher.getType());
      ResourceSubscriber subscriber = resourceSubscriberMap.get(resourceName);
      if (subscriber == null)
      {
        subscriber = new ResourceSubscriber(watcher.getType(), resourceName, _xdsClientJmx);
        resourceSubscriberMap.put(resourceName, subscriber);
        ResourceType type;
        String adjustedResourceName;
        if (watcher.getType() == ResourceType.D2_URI_MAP && _subscribeToUriGlobCollection)
        {
          type = ResourceType.D2_URI;
          adjustedResourceName = GlobCollectionUtils.globCollectionUrlForClusterResource(resourceName);
        }
        else
        {
          type = watcher.getType();
          adjustedResourceName = resourceName;
        }
        _log.info("Subscribing to {} resource: {}", type, adjustedResourceName);

        if (_adsStream == null && !isInBackoff())
        {
          startRpcStreamLocal();
        }
        if (_adsStream != null)
        {
          _adsStream.sendDiscoveryRequest(type, Collections.singletonList(adjustedResourceName));
        }
      }
      subscriber.addWatcher(watcher);
    });
  }

  @Override
  public void watchAllXdsResources(WildcardResourceWatcher watcher)
  {
    _executorService.execute(() ->
    {
      WildcardResourceSubscriber subscriber = getWildcardResourceSubscriber(watcher.getType());
      if (subscriber == null)
      {
        subscriber = new WildcardResourceSubscriber(watcher.getType());
        _wildcardSubscribers.put(watcher.getType(), subscriber);

        ResourceType adjustedType =
            (watcher.getType() == ResourceType.D2_URI_MAP && _subscribeToUriGlobCollection)
                ? ResourceType.D2_URI
                : watcher.getType();

        _log.info("Subscribing to wildcard for resource type: {}", adjustedType);

        if (_adsStream == null && !isInBackoff())
        {
          startRpcStreamLocal();
        }
        if (_adsStream != null)
        {
          _adsStream.sendDiscoveryRequest(adjustedType, Collections.singletonList("*"));
        }
      }

      subscriber.addWatcher(watcher);
    });
  }

  @Override
  public void watchD2Uri(String cluster, String uri, D2UriResourceWatcher watcher)
  {
    _executorService.execute(() ->
    {
      String urn = GlobCollectionUtils.globCollectionUrn(cluster, uri);
      D2UriSubscriber subscriber = getD2UriSubscribers().get(urn);
      if (subscriber == null)
      {
        subscriber = new D2UriSubscriber(urn);
        getD2UriSubscribers().put(urn, subscriber);

        _log.info("Subscribing to D2URI: {}", urn);

        if (_adsStream == null && !isInBackoff())
        {
          startRpcStreamLocal();
        }
        if (_adsStream != null)
        {
          _adsStream.sendDiscoveryRequest(ResourceType.D2_URI, Collections.singletonList(urn));
        }
      }

      subscriber.addWatcher(watcher);
    });
  }

  @Override
  public void startRpcStream()
  {
    _executorService.execute(() ->
    {
      if (!isInBackoff())
      {
        try
        {
          startRpcStreamLocal();
        }
        catch (Throwable t)
        {
          _log.error("Unexpected exception while starting RPC stream", t);
        }
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
    AdsStream stream = new AdsStream(stub);
    _adsStream = stream;
    _readyTimeoutFuture = _executorService.schedule(() ->
    {
      // There is a race condition where the task can be executed right as it's being cancelled. This checks whether
      // the current state is still pointing to the right stream, and whether it is ready before notifying of an error.
      if (_adsStream != stream || stream.isReady())
      {
        return;
      }
      _log.warn("ADS stream not ready within {} milliseconds", _readyTimeoutMillis);
      // notify subscribers about the error and wait for the stream to be ready by keeping it open.
      notifyStreamError(Status.DEADLINE_EXCEEDED);
      // note: no need to start a retry task explicitly since xds stream internally will keep on retrying to connect
      // to one of the sub-channels (unless an error or complete callback is called).
    }, _readyTimeoutMillis, TimeUnit.MILLISECONDS);
    _adsStream.start();
    _log.info("Starting ADS stream, connecting to server: {}", _managedChannel.authority());
  }

  @Override
  public void shutdown()
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
  public String getXdsServerAuthority()
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
    // Confirm ready state to neglect spurious callbacks; we'll get another callback whenever it is ready again.
    // Also confirm ready timeout future is not null to avoid notifying multiple times.
    if (!_adsStream.isReady() || _readyTimeoutFuture == null)
    {
      return;
    }

    // timeout task will be cancelled only if it hasn't already executed.
    boolean cancelledTimeout = _readyTimeoutFuture.cancel(false);
    _log.info("ADS stream ready, cancelled timeout task: {}", cancelledTimeout);
    _readyTimeoutFuture = null; // set it to null to avoid repeat notifications to subscribers.
    if (_retryRpcStreamFuture != null)
    {
      _retryRpcStreamFuture = null;
      _xdsClientJmx.incrementReconnectionCount();
    }
    notifyStreamReconnect();
  }

  @VisibleForTesting
  void handleResponse(DiscoveryResponseData response)
  {
    ResourceType resourceType = response.getResourceType();
    switch (resourceType)
    {
      case NODE:
        handleD2NodeResponse(response);
        break;
      case D2_URI_MAP:
        handleD2URIMapResponse(response);
        break;
      case D2_URI:
        handleD2URICollectionResponse(response);
        break;
      default:
        throw new AssertionError("Missing case in enum switch: " + resourceType);
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
        updates.put(resourceName, new NodeUpdate(d2Node));
      }
      catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack Node response", e);
        errors.add("Failed to unpack Node response");
        // Assume that the resource doesn't exist if it cannot be deserialized instead of simply ignoring it. This way
        // any call waiting on the response can be satisfied instead of timing out.
        updates.put(resourceName, EMPTY_NODE_UPDATE);
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

    for (Resource resource : data.getResourcesList())
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
        updates.put(resourceName, new D2URIMapUpdate(nodeData));
      }
      catch (InvalidProtocolBufferException e)
      {
        _log.warn("Failed to unpack D2URIMap response", e);
        errors.add("Failed to unpack D2URIMap response");
        // Assume that the resource doesn't exist if it cannot be deserialized instead of simply ignoring it. This way
        // any call waiting on the response can be satisfied instead of timing out.
        updates.put(resourceName, EMPTY_D2_URI_MAP_UPDATE);
      }
    }
    sendAckOrNack(data.getResourceType(), data.getNonce(), errors);
    handleResourceUpdate(updates, data.getResourceType());
    handleResourceRemoval(data.getRemovedResources(), data.getResourceType());
  }

  /**
   * Handles glob collection responses by looking up the existing {@link ResourceSubscriber}'s data and applying the
   * patch received from the xDS server.
   */
  private void handleD2URICollectionResponse(DiscoveryResponseData data)
  {
    Map<String, D2URIMapUpdate> updates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    Set<String> removedClusters = new HashSet<>();

    data.forEach((resourceName, resource) ->
    {
      D2UriIdentifier uriId = D2UriIdentifier.parse(resourceName);
      if (uriId == null)
      {
        String msg = String.format("Ignoring D2URI resource update with invalid name: %s", resourceName);
        _log.warn(msg);
        errors.add(msg);
        return;
      }

      ResourceSubscriber subscriber =
          getResourceSubscriberMap(ResourceType.D2_URI_MAP).get(uriId.getClusterResourceName());
      WildcardResourceSubscriber wildcardSubscriber = getWildcardResourceSubscriber(ResourceType.D2_URI_MAP);
      D2UriSubscriber d2UriSubscriber = getD2UriSubscribers().get(resourceName);
      if (subscriber == null && wildcardSubscriber == null && d2UriSubscriber == null)
      {
        String msg = String.format("Ignoring D2URI resource update for untracked cluster: %s", resourceName);
        _log.warn(msg);
        errors.add(msg);
        return;
      }

      XdsD2.D2URI uri;
      if (resource != null)
      {
        try
        {
          uri = resource.getResource().unpack(XdsD2.D2URI.class);
        }
        catch (InvalidProtocolBufferException e)
        {
          _log.warn("Failed to unpack D2URI", e);
          errors.add("Failed to unpack D2URI");
          return;
        }
      }
      else
      {
        uri = null;
      }


      if (d2UriSubscriber != null)
      {
        d2UriSubscriber.onData(uri, _serverMetricsProvider);
      }

      if (subscriber == null && wildcardSubscriber == null)
      {
        return;
      }

      // Get or create a new D2URIMapUpdate which is a copy of the existing data for that cluster.
      D2URIMapUpdate update = updates.computeIfAbsent(uriId.getClusterResourceName(), k ->
      {
        D2URIMapUpdate currentData;
        // Use the existing data from whichever subscriber is present. If both are present, they will point to the same
        // D2URIMapUpdate.
        if (subscriber != null)
        {
          currentData = (D2URIMapUpdate) subscriber._data;
        }
        else
        {
          currentData = (D2URIMapUpdate) wildcardSubscriber._data.get(uriId.getClusterResourceName());
        }
        if (currentData == null || !currentData.isValid())
        {
          return new D2URIMapUpdate(null);
        }
        else
        {
          return new D2URIMapUpdate(new HashMap<>(currentData.getURIMap()));
        }
      });

      // If the uri is null, it's being deleted
      if (uri == null)
      {
        // This is the special case where the entire collection is being deleted. This either means the client
        // subscribed to a cluster that does not exist, or all hosts stopped announcing to the cluster.
        if ("*".equals(uriId.getUriName()))
        {
          removedClusters.add(uriId.getClusterResourceName());
        }
        else
        {
          // Else it's a standard delete for that host.
          update.removeUri(uriId.getUriName());
        }
      }
      else
      {
        update.putUri(uriId.getUriName(), uri);
      }
    });
    sendAckOrNack(data.getResourceType(), data.getNonce(), errors);

    handleResourceUpdate(updates, ResourceType.D2_URI_MAP);
    handleResourceRemoval(removedClusters, ResourceType.D2_URI_MAP);
  }

  @VisibleForTesting
  void sendAckOrNack(ResourceType type, String nonce, List<String> errors)
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
    Map<String, ResourceSubscriber> subscribers = getResourceSubscriberMap(type);
    for (Map.Entry<String, ? extends ResourceUpdate> entry : updates.entrySet())
    {
      ResourceSubscriber subscriber = subscribers.get(entry.getKey());
      if (subscriber != null)
      {
        subscriber.onData(entry.getValue(), _serverMetricsProvider);
      }
      WildcardResourceSubscriber wildcardSubscriber = getWildcardResourceSubscriber(type);
      if (wildcardSubscriber != null)
      {
        wildcardSubscriber.onData(entry.getKey(), entry.getValue());
      }
    }
  }

  private void handleResourceRemoval(Collection<String> removedResources, ResourceType type)
  {
    if (removedResources == null || removedResources.isEmpty())
    {
      return;
    }
    for (String resourceName : removedResources)
    {
      _xdsClientJmx.incrementResourceNotFoundCount();
      _log.warn("Received response that {} {} was removed", type, resourceName);
      ResourceSubscriber subscriber = getResourceSubscriberMap(type).get(resourceName);
      if (subscriber != null)
      {
        subscriber.onRemoval();
      }
    }
    WildcardResourceSubscriber wildcardSubscriber = getWildcardResourceSubscriber(type);
    if (wildcardSubscriber != null)
    {
      removedResources.forEach(wildcardSubscriber::onRemoval);
    }
  }


  private void notifyStreamError(Status error)
  {
    for (Map<String, ResourceSubscriber> subscriberMap : _resourceSubscribers.values())
    {
      for (ResourceSubscriber subscriber : subscriberMap.values())
      {
        subscriber.onError(error);
      }
    }
    for (WildcardResourceSubscriber wildcardResourceSubscriber : _wildcardSubscribers.values())
    {
      wildcardResourceSubscriber.onError(error);
    }
    for (D2UriSubscriber uriSubscriber : _d2UriSubscribers.values())
    {
      uriSubscriber.onError(error);
    }
    _xdsClientJmx.setIsConnected(false);
  }

  private void notifyStreamReconnect()
  {
    for (Map<String, ResourceSubscriber> subscriberMap : _resourceSubscribers.values())
    {
      for (ResourceSubscriber subscriber : subscriberMap.values())
      {
        subscriber.onReconnect();
      }
    }
    for (WildcardResourceSubscriber wildcardResourceSubscriber : _wildcardSubscribers.values())
    {
      wildcardResourceSubscriber.onReconnect();
    }
    for (D2UriSubscriber uriSubscriber : _d2UriSubscribers.values())
    {
      uriSubscriber.onReconnect();
    }
    _xdsClientJmx.setIsConnected(true);
  }

  @VisibleForTesting
  Map<String, ResourceSubscriber> getResourceSubscriberMap(ResourceType type)
  {
    return _resourceSubscribers.get(type);
  }

  @VisibleForTesting
  WildcardResourceSubscriber getWildcardResourceSubscriber(ResourceType type)
  {
    return _wildcardSubscribers.get(type);
  }

  @VisibleForTesting
  Map<String, D2UriSubscriber> getD2UriSubscribers()
  {
    return _d2UriSubscribers;
  }

  static class ResourceSubscriber
  {
    private final ResourceType _type;
    private final String _resource;
    private final Set<ResourceWatcher> _watchers = new HashSet<>();
    private final XdsClientJmx _xdsClientJmx;
    @Nullable
    private ResourceUpdate _data;

    @VisibleForTesting
    @Nullable
    public ResourceUpdate getData()
    {
      return _data;
    }

    @VisibleForTesting
    public void setData(@Nullable ResourceUpdate data)
    {
      _data = data;
    }

    ResourceSubscriber(ResourceType type, String resource, XdsClientJmx xdsClientJmx)
    {
      _type = type;
      _resource = resource;
      _xdsClientJmx = xdsClientJmx;
    }

    void addWatcher(ResourceWatcher watcher)
    {
      _watchers.add(watcher);
      if (_data != null)
      {
        watcher.onChanged(_data);
        _log.debug("Notifying watcher of current data for resource {} of type {}: {}", _resource, _type, _data);
      }
    }

    private void onData(ResourceUpdate data, XdsServerMetricsProvider metricsProvider)
    {
      if (Objects.equals(_data, data))
      {
        _log.debug("Received resource update data equal to the current data. Will not perform the update.");
        return;
      }
      // null value guard to avoid overwriting the property with null
      if (data != null && data.isValid())
      {
        trackServerLatency(data, metricsProvider); // data updated, track xds server latency
        _data = data;
      }
      else
      {
        if (_type == ResourceType.D2_URI_MAP || _type == ResourceType.D2_URI)
        {
          RATE_LIMITED_LOGGER.warn("Received invalid data for {} {}, data: {}", _type, _resource, data);
        }
        else
        {
          _log.warn("Received invalid data for {} {}, data: {}", _type, _resource, data);
        }
        _xdsClientJmx.incrementResourceInvalidCount();
      }

      if (_data == null)
      {
        _log.info("Initializing {} {} to empty data.", _type, _resource);
        _data = _type.emptyData();
      }
      for (ResourceWatcher watcher : _watchers)
      {
        watcher.onChanged(_data);
      }
    }

    // track rough estimate of latency spent on the xds server in millis = resource receipt time - resource modified time
    private void trackServerLatency(ResourceUpdate resourceUpdate, XdsServerMetricsProvider metricsProvider)
    {
      if (!shouldTrackServerLatency())
      {
        return;
      }

      long now = SystemClock.instance().currentTimeMillis();
      if (resourceUpdate instanceof NodeUpdate)
      {
        XdsD2.Node nodeData = ((NodeUpdate) resourceUpdate).getNodeData();
        if (nodeData == null)
        {
          return;
        }
        metricsProvider.trackLatency(now - nodeData.getStat().getMtime());
      }
      else if (resourceUpdate instanceof D2URIMapUpdate)
      {
        // only track server latency for the updated/new uris in the update
        Map<String, XdsD2.D2URI> currentUriMap = ((D2URIMapUpdate) _data).getURIMap();
        MapDifference<String, XdsD2.D2URI> rawDiff = Maps.difference(((D2URIMapUpdate) resourceUpdate).getURIMap(),
            currentUriMap == null ? Collections.emptyMap() : currentUriMap);
        Map<String, XdsD2.D2URI> updatedUris = rawDiff.entriesDiffering().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().leftValue()) // new data of updated uris
            );
        trackServerLatencyForUris(updatedUris, metricsProvider, now);
        trackServerLatencyForUris(rawDiff.entriesOnlyOnLeft(), metricsProvider, now); // newly added uris
      }
    }

    private boolean shouldTrackServerLatency()
    {
      return _data != null && _data.isValid(); // not initial update and there has been valid update before
    }

    private void trackServerLatencyForUris(Map<String, XdsD2.D2URI> uriMap, XdsServerMetricsProvider metricsProvider,
        long now)
    {
      uriMap.forEach((k, v) -> metricsProvider.trackLatency(now - v.getModifiedTime().getSeconds() * 1000));
    }

    public ResourceType getType()
    {
      return _type;
    }

    public String getResource()
    {
      return _resource;
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

    /**
     * When the client receive the removal data from INDIS, client side doesn't delete the data from the cache
     * which is just a design choice by now.So to avoid the eventbus watcher timeout, in there directly notify the
     * watcher with cache data
     */
    @VisibleForTesting
    void onRemoval()
    {
      if (_data == null)
      {
        _log.info("Initializing {} {} to empty data.", _type, _resource);
        _data = _type.emptyData();
      }
      for (ResourceWatcher watcher : _watchers)
      {
        watcher.onChanged(_data);
      }
    }
  }

  static class WildcardResourceSubscriber
  {
    private final ResourceType _type;
    private final Set<WildcardResourceWatcher> _watchers = new HashSet<>();
    private final Map<String, ResourceUpdate> _data = new HashMap<>();

    @VisibleForTesting
    public ResourceUpdate getData(String resourceName)
    {
      return _data.get(resourceName);
    }

    @VisibleForTesting
    public void setData(String resourceName, ResourceUpdate data)
    {
      _data.put(resourceName, data);
    }

    WildcardResourceSubscriber(ResourceType type)
    {
      _type = type;
    }

    void addWatcher(WildcardResourceWatcher watcher)
    {
      _watchers.add(watcher);
      for (Map.Entry<String, ResourceUpdate> entry : _data.entrySet())
      {
        watcher.onChanged(entry.getKey(), entry.getValue());
        _log.debug("Notifying watcher of current data for resource {} of type {}: {}",
            entry.getKey(), _type, entry.getValue());
      }
    }

    private void onData(String resourceName, ResourceUpdate data)
    {
      if (Objects.equals(_data.get(resourceName), data))
      {
        _log.debug("Received resource update data equal to the current data. Will not perform the update.");
        return;
      }
      // null value guard to avoid overwriting the property with null
      if (data != null && data.isValid())
      {
        _data.put(resourceName, data);
        for (WildcardResourceWatcher watcher : _watchers)
        {
          watcher.onChanged(resourceName, data);
        }
      }
      else
      {
        if (_type == ResourceType.D2_URI_MAP || _type == ResourceType.D2_URI)
        {
          RATE_LIMITED_LOGGER.warn("Received invalid data for {} {}, data: {}", _type, resourceName, data);
        }
        else
        {
          _log.warn("Received invalid data for {} {}, data: {}", _type, resourceName, data);
        }
      }
    }

    public ResourceType getType()
    {
      return _type;
    }

    private void onError(Status error)
    {
      for (WildcardResourceWatcher watcher : _watchers)
      {
        watcher.onError(error);
      }
    }

    private void onReconnect()
    {
      for (WildcardResourceWatcher watcher : _watchers)
      {
        watcher.onReconnect();
      }
    }

    @VisibleForTesting
    void onRemoval(String resourceName)
    {
      _data.remove(resourceName);
      for (WildcardResourceWatcher watcher : _watchers)
      {
        watcher.onRemoval(resourceName);
      }
    }
  }

  static class D2UriSubscriber
  {
    private final Set<D2UriResourceWatcher> _watchers = new HashSet<>();
    private XdsD2.D2URI _d2Uri;
    private final String _name;

    D2UriSubscriber(String name)
    {
      _name = name;
    }


    @VisibleForTesting
    public XdsD2.D2URI getData()
    {
      return _d2Uri;
    }

    @VisibleForTesting
    public void setData(XdsD2.D2URI d2Uri)
    {
      _d2Uri = d2Uri;
    }

    void addWatcher(D2UriResourceWatcher watcher)
    {
      _watchers.add(watcher);
      watcher.onChanged(_d2Uri);
      _log.debug("Notifying watcher of current data for D2URI {}: {}", _name, _d2Uri);
    }

    private void onData(XdsD2.D2URI d2Uri, XdsServerMetricsProvider metricsProvider)
    {
      if (Objects.equals(_d2Uri, d2Uri))
      {
        _log.debug("Received resource update data equal to the current data for {}. Will not perform the update.",
            _name);
        return;
      }
      if (d2Uri == null)
      {
        for (D2UriResourceWatcher watcher : _watchers)
        {
          watcher.onDelete();
        }
      }
      else
      {
        metricsProvider.trackLatency(System.currentTimeMillis() - d2Uri.getModifiedTime().getSeconds() * 1000);
      }
    }

    private void onError(Status error)
    {
      for (D2UriResourceWatcher watcher : _watchers)
      {
        watcher.onError(error);
      }
    }

    private void onReconnect()
    {
      for (D2UriResourceWatcher watcher : _watchers)
      {
        watcher.onReconnect();
      }
    }
  }

  final class RpcRetryTask implements Runnable
  {
    @Override
    public void run()
    {
      startRpcStreamLocal();
      for (ResourceType type : _resourceSubscribers.keySet())
      {
        Set<String> resources = new HashSet<>(getResourceSubscriberMap(type).keySet());
        if (resources.isEmpty() && getWildcardResourceSubscriber(type) == null)
        {
          continue;
        }
        ResourceType rewrittenType;
        if (_subscribeToUriGlobCollection && type == ResourceType.D2_URI_MAP)
        {
          resources = resources.stream()
              .map(GlobCollectionUtils::globCollectionUrlForClusterResource)
              .collect(Collectors.toCollection(HashSet::new));
          rewrittenType = ResourceType.D2_URI;
        }
        else
        {
          rewrittenType = type;
        }
        // If there is a wildcard subscriber, we should always send a wildcard request to the server.
        if (getWildcardResourceSubscriber(type) != null)
        {
          resources.add("*");
        }
        _adsStream.sendDiscoveryRequest(rewrittenType, resources);
      }
      if (!_d2UriSubscribers.isEmpty())
      {
        _adsStream.sendDiscoveryRequest(ResourceType.D2_URI, _d2UriSubscribers.keySet());
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

  static final class DiscoveryResponseData
  {
    private final ResourceType _resourceType;
    private final List<Resource> _resources;
    private final List<String> _removedResources;
    private final String _nonce;
    @Nullable
    private final String _controlPlaneIdentifier;

    DiscoveryResponseData(ResourceType resourceType,
        @Nullable List<Resource> resources,
        @Nullable List<String> removedResources,
        String nonce,
        @Nullable String controlPlaneIdentifier)
    {
      _resourceType = resourceType;
      _resources = (resources == null) ? Collections.emptyList() : resources;
      _removedResources = (removedResources == null) ? Collections.emptyList() : removedResources;
      _nonce = nonce;
      _controlPlaneIdentifier = controlPlaneIdentifier;
    }

    static DiscoveryResponseData fromEnvoyProto(DeltaDiscoveryResponse proto)
    {
      return new DiscoveryResponseData(ResourceType.fromTypeUrl(proto.getTypeUrl()), proto.getResourcesList(),
          proto.getRemovedResourcesList(), proto.getNonce(),
          Strings.emptyToNull(proto.getControlPlane().getIdentifier()));
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

    /**
     * Invokes the given consumer for each resource in this response. If the {@link Resource} is not null, it is being
     * created/modified and if it is null, it is being removed.
     */
    void forEach(BiConsumer<String, Resource> consumer)
    {
      for (Resource resource : _resources)
      {
        consumer.accept(resource.getName(), resource);
      }
      for (String removedResource : _removedResources)
      {
        consumer.accept(removedResource, null);
      }
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
                if (_closed)
                {
                  return;
                }

                ResourceType resourceType = ResourceType.fromTypeUrl(response.getTypeUrl());
                if (resourceType == null)
                {
                  _log.warn("Received unknown response type:\n{}", response);
                  return;
                }
                _log.debug("Received {} response:\n{}", resourceType, response);
                DiscoveryResponseData responseData = DiscoveryResponseData.fromEnvoyProto(response);

                if (!_responseReceived && responseData.getControlPlaneIdentifier() != null)
                {
                  _log.info("Successfully received response from ADS server: {}",
                      responseData.getControlPlaneIdentifier());
                }
                _responseReceived = true;

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
      DeltaDiscoveryRequest request = new DiscoveryRequestData(_node, type, resources).toEnvoyProto();
      _requestWriter.onNext(request);
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
      _log.error("ADS stream closed with status {}: {}", error.getCode(), error.getDescription(), error.getCause());
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
      if (_adsStream == this)
      {
        _adsStream = null;
      }

      if (_readyTimeoutFuture != null)
      {
        _readyTimeoutFuture.cancel(true);
        _readyTimeoutFuture = null;
      }

      if (_retryRpcStreamFuture != null)
      {
        _retryRpcStreamFuture.cancel(true);
        _retryRpcStreamFuture = null;
      }
    }
  }
}
