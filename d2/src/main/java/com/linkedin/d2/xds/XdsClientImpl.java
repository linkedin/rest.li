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
import com.google.protobuf.util.Timestamps;
import com.google.rpc.Code;
import com.linkedin.d2.jmx.NoOpXdsServerMetricsProvider;
import com.linkedin.d2.jmx.XdsClientJmx;
import com.linkedin.d2.jmx.XdsServerMetricsProvider;
import com.linkedin.d2.xds.GlobCollectionUtils.D2UriIdentifier;
import com.linkedin.util.RateLimitedLogger;
import com.linkedin.util.clock.Clock;
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
import java.nio.ByteBuffer;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link XdsClient} interface.
 */
public class XdsClientImpl extends XdsClient
{
  private static final Logger _log = LoggerFactory.getLogger(XdsClientImpl.class);
  private static final RateLimitedLogger RATE_LIMITED_LOGGER =
      new RateLimitedLogger(_log, TimeUnit.MINUTES.toMillis(1), SystemClock.instance());
  public static final long DEFAULT_READY_TIMEOUT_MILLIS = 2000L;
  public static final Integer DEFAULT_MAX_RETRY_BACKOFF_SECS = 30; // default value for max retry backoff seconds

  /**
   * The resource subscribers maps the resource type to its subscribers. Note that the {@link ResourceType#D2_URI}
   * should absent be used, as glob collection updates are translated to appear as normal map updates to subscribers.
   */
  private final Map<ResourceType, Map<String, ResourceSubscriber>> _resourceSubscribers = Maps.immutableEnumMap(
      Stream.of(ResourceType.values())
          .collect(Collectors.toMap(Function.identity(), e -> new HashMap<>())));
  private final Map<ResourceType, WildcardResourceSubscriber> _wildcardSubscribers = Maps.newEnumMap(ResourceType.class);
  /**
   * The resource version maps the resource type to the resource name and its version.
   * Note that all the subscribed resources & it's version would be present in this map.
   * Resource type in this map is adjust type for glob collection.
   */
  private final Map<ResourceType, Map<String, String>> _resourceVersions = Maps.newEnumMap(
      Stream.of(ResourceType.values()).collect(Collectors.toMap(Function.identity(), e -> new HashMap<>())));
  private final Node _node;
  private final ManagedChannel _managedChannel;
  private final ScheduledExecutorService _executorService;
  private final boolean _subscribeToUriGlobCollection;
  private final BackoffPolicy.Provider _backoffPolicyProvider = new ExponentialBackoffPolicy.Provider();
  private BackoffPolicy _retryBackoffPolicy;
  private final Long _maxRetryBackoffNanos;
  @VisibleForTesting
  AdsStream _adsStream;
  private boolean _isXdsStreamShutdown;
  @VisibleForTesting
  ScheduledFuture<?> _retryRpcStreamFuture;
  private ScheduledFuture<?> _readyTimeoutFuture;
  private final long _readyTimeoutMillis;

  private final XdsClientJmx _xdsClientJmx;
  private final XdsServerMetricsProvider _serverMetricsProvider;
  private final boolean _initialResourceVersionsEnabled;

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
        new NoOpXdsServerMetricsProvider(), false);
  }

  @Deprecated
  public XdsClientImpl(Node node,
      ManagedChannel managedChannel,
      ScheduledExecutorService executorService,
      long readyTimeoutMillis,
      boolean subscribeToUriGlobCollection,
      XdsServerMetricsProvider serverMetricsProvider)
  {
    this(node,
        managedChannel,
        executorService,
        readyTimeoutMillis,
        subscribeToUriGlobCollection,
        serverMetricsProvider,
        false);
  }

  @Deprecated
  public XdsClientImpl(Node node,
      ManagedChannel managedChannel,
      ScheduledExecutorService executorService,
      long readyTimeoutMillis,
      boolean subscribeToUriGlobCollection,
      XdsServerMetricsProvider serverMetricsProvider,
      boolean irvSupport)
  {
    this(node,
        managedChannel,
        executorService,
        readyTimeoutMillis,
        subscribeToUriGlobCollection,
        serverMetricsProvider,
        irvSupport, null);
  }

  public XdsClientImpl(Node node,
      ManagedChannel managedChannel,
      ScheduledExecutorService executorService,
      long readyTimeoutMillis,
      boolean subscribeToUriGlobCollection,
      XdsServerMetricsProvider serverMetricsProvider,
      boolean irvSupport,
      Integer maxRetryBackoffSeconds)
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
    _initialResourceVersionsEnabled = irvSupport;
    if (_initialResourceVersionsEnabled)
    {
      _log.info("XDS initial resource versions support enabled");
    }

    _retryBackoffPolicy = _backoffPolicyProvider.get();
    Integer backoffSecs = (maxRetryBackoffSeconds != null && maxRetryBackoffSeconds > 0)
        ? maxRetryBackoffSeconds : DEFAULT_MAX_RETRY_BACKOFF_SECS;
    _log.info("Max retry backoff seconds: {}", backoffSecs);
    _maxRetryBackoffNanos = backoffSecs * TimeUnit.SECONDS.toNanos(1);
  }

  @Override
  public void start()
  {
    _xdsClientJmx.setXdsClient(this);
    startRpcStream();
  }

  @Override
  public void watchXdsResource(String resourceName, ResourceWatcher watcher)
  {
    checkShutdownAndExecute(() ->
    {
      ResourceType originalType = watcher.getType();
      Map<String, ResourceSubscriber> resourceSubscriberMap = getResourceSubscriberMap(originalType);
      ResourceSubscriber subscriber = resourceSubscriberMap.get(resourceName);
      if (subscriber == null)
      {
        subscriber = new ResourceSubscriber(originalType, resourceName, _xdsClientJmx);
        resourceSubscriberMap.put(resourceName, subscriber);
        ResourceType adjustedType;
        String adjustedResourceName;
        if (shouldSubscribeUriGlobCollection(originalType))
        {
          adjustedType = ResourceType.D2_URI;
          adjustedResourceName = GlobCollectionUtils.globCollectionUrlForClusterResource(resourceName);
        }
        else
        {
          adjustedType = originalType;
          adjustedResourceName = resourceName;
        }
        _log.info("Subscribing to {} resource: {}", adjustedType, adjustedResourceName);

        if (_adsStream == null && !isInBackoff())
        {
          startRpcStreamLocal();
        }
        if (_adsStream != null)
        {
          _adsStream.sendDiscoveryRequest(adjustedType, Collections.singletonList(adjustedResourceName), Collections.emptyMap());
        }
      }
      subscriber.addWatcher(watcher);
    });
  }

  @Override
  public void watchAllXdsResources(WildcardResourceWatcher watcher)
  {
    checkShutdownAndExecute(() ->
    {
      ResourceType originalType = watcher.getType();
      WildcardResourceSubscriber subscriber = getWildcardResourceSubscriber(originalType);
      if (subscriber == null)
      {
        subscriber = new WildcardResourceSubscriber(originalType);
        getWildcardResourceSubscribers().put(originalType, subscriber);

        ResourceType adjustedType = shouldSubscribeUriGlobCollection(originalType) ? ResourceType.D2_URI : originalType;
        _log.info("Subscribing to wildcard for resource type: {}", adjustedType);

        if (_adsStream == null && !isInBackoff())
        {
          startRpcStreamLocal();
        }
        if (_adsStream != null)
        {
          _adsStream.sendDiscoveryRequest(adjustedType, Collections.singletonList("*"), Collections.emptyMap());
        }
      }

      subscriber.addWatcher(watcher);
    });
  }

  @Override
  public XdsClientJmx getXdsClientJmx()
  {
    return _xdsClientJmx;
  }

  public long getActiveInitialWaitTimeMillis()
  {
    AtomicLong res = new AtomicLong(0);
    long now = System.currentTimeMillis();
    getResourceSubscribers().values().forEach(
        map -> map.values().forEach(subscriber -> res.addAndGet(subscriber.getActiveInitialWaitTimeMillis(now)))
    );
    getWildcardResourceSubscribers().values().forEach(
        subscriber -> res.addAndGet(subscriber.getActiveInitialWaitTimeMillis(now))
    );
    return res.get();
  }

  @VisibleForTesting
  void startRpcStream()
  {
    checkShutdownAndExecute(() ->
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

  // Start RPC stream. Must be called from the executor, and only if we're not backed off.
  @VisibleForTesting
  void startRpcStreamLocal()
  {
    if (_isXdsStreamShutdown)
    {
      _log.warn("RPC stream cannot be started after shutdown!");
      return;
    }
    // Check rpc stream is null to ensure duplicate RPC retry tasks are no-op
    if (_adsStream != null)
    {
      _log.warn("Tried to create duplicate RPC stream, ignoring!");
      return;
    }
    AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub =
        AggregatedDiscoveryServiceGrpc.newStub(_managedChannel);
    AdsStream stream = new AdsStream(stub);
    _adsStream = stream;
    _readyTimeoutFuture = checkShutdownAndSchedule(() ->
    {
      // There is a race condition where the task can be executed right as it's being cancelled. This checks whether
      // the current state is still pointing to the right stream, and whether it is ready before notifying of an error.
      if (_adsStream != stream || stream.isReady())
      {
        return;
      }
      _log.warn("ADS stream not ready within {} milliseconds. Underlying grpc channel will keep retrying to connect to "
          + "xds servers.", _readyTimeoutMillis);
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
    // _executorService will be shutdown by the caller, so we don't need to do it here.
    _executorService.execute(() ->
    {
      _isXdsStreamShutdown = true;
      _log.info("Shutting down");
      if (_adsStream != null)
      {
        _adsStream.close(Status.CANCELLED.withDescription("shutdown").asException());
      }

      if(!_managedChannel.isShutdown())
      {
        _managedChannel.shutdown();
      }
    });
  }

  private ScheduledFuture<?> checkShutdownAndSchedule(Runnable runnable, long delay, TimeUnit unit) {
    if (_executorService.isShutdown())
    {
      _log.warn("Attempting to schedule a task after _executorService was shutdown, will do nothing");
      return null;
    }

    return _executorService.schedule(runnable, delay, unit);
  }

  private void checkShutdownAndExecute(Runnable runnable)
  {
    if (_executorService.isShutdown())
    {
      _log.warn("Attempting to execute a task after _executorService was shutdown, will do nothing");
      return;
    }

    _executorService.execute(runnable);
  }

  @Override
  public String getXdsServerAuthority()
  {
    return _managedChannel.authority();
  }

  /**
   * The client may be in backoff if there are RPC stream failures, and if it's waiting to establish the stream again.
   * NOTE: Must be called from the executor.
   *
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
    updateResourceVersions(response);
    ResourceType resourceType = response.getResourceType();
    switch (resourceType)
    {
      case NODE:
        handleD2NodeResponse(response);
        break;
      case D2_CLUSTER_OR_SERVICE_NAME:
        handleD2ClusterOrServiceNameResponse(response);
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
    notifyOnLastChunk(response);
  }

  /**
   * Updates the resource versions map with the latest version of the resources received in the response.
   * This is used to send the initial_resource_version to the server when the client re-connect.
   */
  private void updateResourceVersions(DiscoveryResponseData response)
  {
    ResourceType resourceType = response.getResourceType();
    Map<String, String> resourceVersions = getResourceVersions().get(resourceType);
    for (Resource res : response.getResourcesList())
    {
      resourceVersions.put(res.getName(), res.getVersion());
    }

    for (String removedResource : response.getRemovedResources())
    {
      resourceVersions.remove(removedResource);
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
      catch (Exception e)
      {
        String errMsg = String.format("Failed to unpack Node for resource: %s", resourceName);
        _log.warn(errMsg, e);
        errors.add(errMsg);
        // Assume that the resource doesn't exist if it cannot be deserialized instead of simply ignoring it. This way
        // any call waiting on the response can be satisfied instead of timing out.
        updates.put(resourceName, EMPTY_NODE_UPDATE);
      }
    }
    sendAckOrNack(data.getResourceType(), data.getNonce(), errors);
    processResourceChanges(data.getResourceType(), updates, data.getRemovedResources());
  }

  private void handleD2ClusterOrServiceNameResponse(DiscoveryResponseData data)
  {
    Map<String, D2ClusterOrServiceNameUpdate> updates = new HashMap<>();
    List<String> errors = new ArrayList<>();

    for (Resource resource : data.getResourcesList())
    {
      String resourceName = resource.getName();
      try
      {
        XdsD2.D2ClusterOrServiceName clusterOrServiceName = resource.getResource()
            .unpack(XdsD2.D2ClusterOrServiceName.class);
        updates.put(resourceName, new D2ClusterOrServiceNameUpdate(clusterOrServiceName));
      }
      catch (Exception e)
      {
        String errMsg = String.format("Failed to unpack D2ClusterOrServiceName for resource: %s.", resourceName);
        _log.warn(errMsg, e);
        errors.add(errMsg);
        // Assume that the resource doesn't exist if it cannot be deserialized instead of simply ignoring it. This way
        // any call waiting on the response can be satisfied instead of timing out.
        updates.put(resourceName, EMPTY_D2_CLUSTER_OR_SERVICE_NAME_UPDATE);
      }
    }
    sendAckOrNack(data.getResourceType(), data.getNonce(), errors);
    processResourceChanges(data.getResourceType(), updates, data.getRemovedResources());
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
          RATE_LIMITED_LOGGER.warn("Received a D2URIMap response with no data, resource is : {}", resourceName);
        }
        updates.put(resourceName, new D2URIMapUpdate(nodeData));
      }
      catch (Exception e)
      {
        String errMsg = String.format("Failed to unpack D2URIMap for resource: %s", resourceName);
        _log.warn(errMsg, e);
        errors.add(errMsg);
        // Assume that the resource doesn't exist if it cannot be deserialized instead of simply ignoring it. This way
        // any call waiting on the response can be satisfied instead of timing out.
        updates.put(resourceName, EMPTY_D2_URI_MAP_UPDATE);
      }
    }
    sendAckOrNack(data.getResourceType(), data.getNonce(), errors);
    processResourceChanges(data.getResourceType(), updates, data.getRemovedResources());
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

      ResourceSubscriber clusterSubscriber =
          getResourceSubscriberMap(ResourceType.D2_URI_MAP).get(uriId.getClusterResourceName());
      ResourceSubscriber uriSubscriber = getResourceSubscriberMap(ResourceType.D2_URI).get(resourceName);
      WildcardResourceSubscriber wildcardSubscriber = getWildcardResourceSubscriber(ResourceType.D2_URI_MAP);
      if (clusterSubscriber == null && wildcardSubscriber == null && uriSubscriber == null)
      {
        String msg = String.format("Ignoring D2URI resource update for untracked cluster: %s", resourceName);
        _log.warn(msg);
        errors.add(msg);
        return;
      }

      // uri will be null if the data was invalid, or if the resource is being deleted.
      XdsD2.D2URI uri = null;
      if (resource != null)
      {
        try
        {
          uri = resource.getResource().unpack(XdsD2.D2URI.class);
        }
        catch (Exception e)
        {
          String errMsg = String.format("Failed to unpack D2URI for resource: %s", resourceName);
          _log.warn(errMsg, e);
          errors.add(errMsg);
        }
      }

      if (uriSubscriber != null)
      {
        // Special case for the D2URI subscriber: the URI could not be deserialized. If a previous version of the data
        // is present, do nothing and drop the update on the floor. If no previous version is present however, notify
        // the subscriber that the URI is deleted/doesn't exist. This behavior is slightly different from the other
        // types, which do not support deletions.
        if (uri != null // The URI is being updated
            || resource == null  // The URI is being deleted
            || uriSubscriber.getData() == null // The URI was corrupted and there was no previous version of this URI
        )
        {
          uriSubscriber.onData(new D2URIUpdate(uri), _serverMetricsProvider, _initialResourceVersionsEnabled);
        }
      }

      if (clusterSubscriber == null && wildcardSubscriber == null)
      {
        return;
      }

      // Get or create a new D2URIMapUpdate which is a copy of the existing data for that cluster.
      D2URIMapUpdate update = updates.computeIfAbsent(uriId.getClusterResourceName(), k ->
      {
        D2URIMapUpdate currentData;
        // Use the existing data from whichever subscriber is present. If both are present, they will point to the same
        // D2URIMapUpdate.
        if (clusterSubscriber != null)
        {
          currentData = (D2URIMapUpdate) clusterSubscriber._data;
        }
        else
        {
          currentData = (D2URIMapUpdate) wildcardSubscriber._data.get(uriId.getClusterResourceName());
        }
        if (currentData == null || !currentData.isValid())
        {
          return new D2URIMapUpdate(null, true);
        }
        else
        {
          return new D2URIMapUpdate(new HashMap<>(currentData.getURIMap()), true);
        }
      });

      // If the resource is null, it's being deleted
      if (resource == null)
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
      // Only put valid URIs in the map. Because the D2URIMapUpdate is still created by this loop, the subscriber will
      // receive an update, unblocking any waiting futures, so there is no need to insert null/invalid URIs in the map.
      else if (uri != null)
      {
        update.putUri(uriId.getUriName(), uri);
      }
    });
    sendAckOrNack(data.getResourceType(), data.getNonce(), errors);
    processResourceChanges(ResourceType.D2_URI_MAP, updates, removedClusters);
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

  private void processResourceChanges(ResourceType type, Map<String, ? extends ResourceUpdate> updates,
      Collection<String> removedResources)
  {
    handleResourceUpdate(updates, type);
    handleResourceRemoval(removedResources, type);
  }

  private void handleResourceUpdate(Map<String, ? extends ResourceUpdate> updates, ResourceType type)
  {
    Map<String, ResourceSubscriber> subscribers = getResourceSubscriberMap(type);
    WildcardResourceSubscriber wildcardSubscriber = getWildcardResourceSubscriber(type);

    for (Map.Entry<String, ? extends ResourceUpdate> entry : updates.entrySet())
    {
      ResourceSubscriber subscriber = subscribers.get(entry.getKey());
      if (subscriber != null)
      {
        subscriber.onData(entry.getValue(), _serverMetricsProvider, _initialResourceVersionsEnabled);
      }

      if (wildcardSubscriber != null)
      {
        wildcardSubscriber.onData(entry.getKey(), entry.getValue(), _serverMetricsProvider, _initialResourceVersionsEnabled);
      }
    }
  }

  private void handleResourceRemoval(Collection<String> removedResources, ResourceType type)
  {
    if (removedResources == null || removedResources.isEmpty())
    {
      return;
    }

    Map<String, ResourceSubscriber> subscribers = getResourceSubscriberMap(type);
    WildcardResourceSubscriber wildcardSubscriber = getWildcardResourceSubscriber(type);
    for (String resourceName : removedResources)
    {
      _xdsClientJmx.incrementResourceNotFoundCount();
      _log.warn("Received response that {} {} was removed", type, resourceName);

      ResourceSubscriber subscriber = subscribers.get(resourceName);
      if (subscriber != null)
      {
        subscriber.onRemoval();
      }

      if (wildcardSubscriber != null)
      {
        wildcardSubscriber.onRemoval(resourceName);
      }
    }
  }

  // Notify the wildcard subscriber for having processed all resources if either of these conditions met:
  // 1) the nonce indicates that this is the last chunk of the response.
  // 2) failed to parse a malformed or absent nonce.
  // Details of the nonce format can be found here:
  // https://github.com/linkedin/diderot/blob/b7418ea227eec45056a9de4deee2eb50387f63e8/ads/ads.go#L276
  private void notifyOnLastChunk(DiscoveryResponseData response)
  {
    ResourceType type = response.getResourceType();
    // For the D2URI with glob collection, we need to get D2URIMap WildcardSubscriber
    ResourceType originalType =
        (type == ResourceType.D2_URI && shouldSubscribeUriGlobCollection(ResourceType.D2_URI_MAP))
            ? ResourceType.D2_URI_MAP
            : type;
    WildcardResourceSubscriber wildcardResourceSubscriber = getWildcardResourceSubscriber(originalType);
    if (wildcardResourceSubscriber == null)
    {
      return;
    }
    int remainingChunks;
    try
    {
      byte[] bytes = Hex.decodeHex(response.getNonce().toCharArray());
      ByteBuffer bb = ByteBuffer.wrap(bytes, 8, 4);
      remainingChunks = bb.getInt();
    }
    catch (Exception e)
    {
      RATE_LIMITED_LOGGER.warn("Failed to decode nonce: {}", response.getNonce(), e);
      remainingChunks = -1;
    }

    if (remainingChunks <= 0)
    {
      _log.debug("Notifying wildcard subscriber of type {} for the end of response chunks.", type);
      wildcardResourceSubscriber.onAllResourcesProcessed();
    }
  }

  private void notifyStreamError(Status error)
  {
    for (Map<String, ResourceSubscriber> subscriberMap : getResourceSubscribers().values())
    {
      for (ResourceSubscriber subscriber : subscriberMap.values())
      {
        subscriber.onError(error);
      }
    }
    for (WildcardResourceSubscriber wildcardResourceSubscriber : getWildcardResourceSubscribers().values())
    {
      wildcardResourceSubscriber.onError(error);
    }
    _xdsClientJmx.setIsConnected(false);
  }

  private void notifyStreamReconnect()
  {
    for (Map<String, ResourceSubscriber> subscriberMap : getResourceSubscribers().values())
    {
      for (ResourceSubscriber subscriber : subscriberMap.values())
      {
        subscriber.onReconnect();
      }
    }
    for (WildcardResourceSubscriber wildcardResourceSubscriber : getWildcardResourceSubscribers().values())
    {
      wildcardResourceSubscriber.onReconnect();
    }
    _xdsClientJmx.setIsConnected(true);
  }

  Map<String, ResourceSubscriber> getResourceSubscriberMap(ResourceType type)
  {
    return getResourceSubscribers().get(type);
  }

  @VisibleForTesting
  Map<ResourceType, Map<String, ResourceSubscriber>> getResourceSubscribers()
  {
    return _resourceSubscribers;
  }

  @VisibleForTesting
  Map<ResourceType, Map<String, String>> getResourceVersions()
  {
    return _resourceVersions;
  }

  WildcardResourceSubscriber getWildcardResourceSubscriber(ResourceType type)
  {
    return getWildcardResourceSubscribers().get(type);
  }

  @VisibleForTesting
  Map<ResourceType, WildcardResourceSubscriber> getWildcardResourceSubscribers()
  {
    return _wildcardSubscribers;
  }

  private enum SubscriberFetchState
  {
    INIT_PENDING, // the very first fetch since the subscriber is created
    PENDING_AFTER_RECONNECT, // the first fetch after each reconnect
    FETCHED // received a response (either data or removal) after INIT_PENDING or PENDING_AFTER_RECONNECT
  }

  static class ResourceSubscriber
  {
    private final ResourceType _type;
    private final String _resource;
    private final Set<ResourceWatcher> _watchers = new HashSet<>();
    private final XdsClientJmx _xdsClientJmx;
    @Nullable
    private ResourceUpdate _data;
    private final Clock _clock;
    private long _subscribedAt;
    private SubscriberFetchState _fetchState = SubscriberFetchState.INIT_PENDING;

    ResourceSubscriber(ResourceType type, String resource, XdsClientJmx xdsClientJmx)
    {
      this(type, resource, xdsClientJmx, SystemClock.instance());
    }

    @VisibleForTesting
    ResourceSubscriber(ResourceType type, String resource, XdsClientJmx xdsClientJmx, Clock clock)
    {
      _type = type;
      _resource = resource;
      _xdsClientJmx = xdsClientJmx;
      _clock = clock;
      _subscribedAt = _clock.currentTimeMillis();
    }

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

    void addWatcher(ResourceWatcher watcher)
    {
      _watchers.add(watcher);
      if (_data != null)
      {
        watcher.onChanged(_data);
        _log.debug("Notifying watcher of current data for resource {} of type {}: {}", _resource, _type, _data);
      }
    }

    @VisibleForTesting
    void onData(ResourceUpdate data, XdsServerMetricsProvider metricsProvider, boolean isIrvEnabled)
    {
      if (Objects.equals(_data, data))
      {
        _log.debug("Received resource update data equal to the current data. Will not perform the update.");
        return;
      }

      SubscriberFetchState curFetchState = _fetchState;
      _fetchState = SubscriberFetchState.FETCHED;

      // null value guard to avoid overwriting the property with null
      if (data != null && data.isValid())
      {
        trackServerLatency(data, _data, metricsProvider, _subscribedAt, isIrvEnabled, curFetchState); // data updated, track xds server latency
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

        if (_data == null)
        {
          _log.info("Initializing {} {} to empty data.", _type, _resource);
          _data = _type.emptyData();
        }
        else
        {
          // no update to the existing data, don't need to notify the watcher
          return;
        }
      }

      for (ResourceWatcher watcher : _watchers)
      {
        watcher.onChanged(_data);
      }
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

    @VisibleForTesting
    void onReconnect()
    {
      reset(); // Reconnected needs to reset the subscribe time to the current time for tracking purposes.
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
      _fetchState = SubscriberFetchState.FETCHED;

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

    private void reset()
    {
      _subscribedAt = _clock.currentTimeMillis();
      _fetchState = SubscriberFetchState.PENDING_AFTER_RECONNECT;
    }

    @VisibleForTesting
    void setSubscribedAt(long subscribedAt)
    {
      _subscribedAt = subscribedAt;
    }

    long getActiveInitialWaitTimeMillis(long end)
    {
      if (SubscriberFetchState.FETCHED.equals(_fetchState))
      {
        return 0;
      }
      return end - _subscribedAt;
    }
  }

  static class WildcardResourceSubscriber
  {
    private final ResourceType _type;
    private final Set<WildcardResourceWatcher> _watchers = new HashSet<>();
    private final Map<String, ResourceUpdate> _data = new HashMap<>();
    private final Clock _clock;
    private long _subscribedAt;
    private SubscriberFetchState _fetchState = SubscriberFetchState.INIT_PENDING;

    WildcardResourceSubscriber(ResourceType type)
    {
      this(type, SystemClock.instance());
    }

    WildcardResourceSubscriber(ResourceType type, Clock clock)
    {
      _type = type;
      _clock = clock;
      _subscribedAt = _clock.currentTimeMillis();
    }

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

    @VisibleForTesting
    void onData(String resourceName, ResourceUpdate data, XdsServerMetricsProvider metricsProvider, boolean isIrvEnabled)
    {
      if (Objects.equals(_data.get(resourceName), data))
      {
        _log.debug("Received resource update data equal to the current data. Will not perform the update.");
        return;
      }
      // null value guard to avoid overwriting the property with null
      if (data != null && data.isValid())
      {
        trackServerLatency(data, _data.get(resourceName), metricsProvider, _subscribedAt, isIrvEnabled, _fetchState);
        _data.put(resourceName, data);
      }
      else
      {
        // invalid data is received, log a warning and check if existing data is present.
        if (_type == ResourceType.D2_URI_MAP || _type == ResourceType.D2_URI)
        {
          RATE_LIMITED_LOGGER.warn("Received invalid data for {} {}, data: {}", _type, resourceName, data);
        }
        else
        {
          _log.warn("Received invalid data for {} {}, data: {}", _type, resourceName, data);
        }
        // if no data has ever been set, init it to an empty data in case watchers are waiting for it
        if (_data.get(resourceName) == null)
        {
          _log.info("Initializing {} {} to empty data.", _type, resourceName);
          _data.put(resourceName, _type.emptyData());
        }
        else
        {
          // no update to the existing data, don't need to notify the watcher
          return;
        }
      }

      for (WildcardResourceWatcher watcher : _watchers)
      {
        watcher.onChanged(resourceName, _data.get(resourceName));
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

    @VisibleForTesting
    void onReconnect()
    {
      reset(); // Reconnected needs to reset the subscribe time to the current time for tracking purposes.
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

    @VisibleForTesting
    void onAllResourcesProcessed()
    {
      _fetchState = SubscriberFetchState.FETCHED;

      for (WildcardResourceWatcher watcher : _watchers)
      {
        watcher.onAllResourcesProcessed();
      }
    }

    private void reset()
    {
      _subscribedAt = _clock.currentTimeMillis();
      _fetchState = SubscriberFetchState.PENDING_AFTER_RECONNECT;
    }

    @VisibleForTesting
    void setSubscribedAt(long subscribedAt)
    {
      _subscribedAt = subscribedAt;
    }

    long getActiveInitialWaitTimeMillis(long end)
    {
      if (SubscriberFetchState.FETCHED.equals(_fetchState))
      {
        return 0;
      }
      return end - _subscribedAt;
    }
  }

  /**
   * This is a test-only method to simulate the retry task being executed. It should only be called from tests.
   *
   * @param testStream test ads stream
   */
  @VisibleForTesting
  void testRetryTask(AdsStream testStream)
  {
    if (_adsStream != null && _adsStream != testStream)
    {
      _log.warn("Non-testing ADS stream exists, ignoring test call");
      return;
    }
    _adsStream = testStream;
    _retryRpcStreamFuture = checkShutdownAndSchedule(new RpcRetryTask(), 0, TimeUnit.NANOSECONDS);
  }

  // Return true if the client should subscribe to URI glob collection for the given resource type.
  private boolean shouldSubscribeUriGlobCollection(ResourceType type)
  {
    return _subscribeToUriGlobCollection && type == ResourceType.D2_URI_MAP;
  }

  private static void trackServerLatency(ResourceUpdate resourceUpdate, ResourceUpdate currentData,
      XdsServerMetricsProvider metricsProvider, long subscribedAt, boolean isIrvEnabled, SubscriberFetchState fetchState)
  {
    long now = SystemClock.instance().currentTimeMillis();
    if (resourceUpdate instanceof NodeUpdate)
    {
      XdsD2.Node nodeData = ((NodeUpdate) resourceUpdate).getNodeData();
      if (nodeData == null)
      {
        return;
      }
      trackServerLatencyHelper(metricsProvider, now, nodeData.getStat().getMtime(), subscribedAt,
          isIrvEnabled, fetchState);
    }
    else if (resourceUpdate instanceof D2URIMapUpdate)
    {
      D2URIMapUpdate update = (D2URIMapUpdate) resourceUpdate;
      // only track server latency for the updated/new uris in the update
      Map<String, XdsD2.D2URI> currentUriMap = currentData == null ? Collections.emptyMap()
          : ((D2URIMapUpdate) currentData).getURIMap();
      MapDifference<String, XdsD2.D2URI> rawDiff = Maps.difference(update.getURIMap(),
          currentUriMap == null ? Collections.emptyMap() : currentUriMap);
      Map<String, XdsD2.D2URI> updatedUris = rawDiff.entriesDiffering().entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> e.getValue().leftValue()) // new data of updated uris
          );
      trackServerLatencyForUris(updatedUris, update, metricsProvider, now, subscribedAt, isIrvEnabled, fetchState);
      trackServerLatencyForUris(rawDiff.entriesOnlyOnLeft(), update, metricsProvider, now, subscribedAt,
          isIrvEnabled, fetchState); // newly added uris
    }
    else if (resourceUpdate instanceof D2URIUpdate)
    {
      D2URIUpdate update = (D2URIUpdate) resourceUpdate;
      XdsD2.D2URI uri = update.getD2Uri();
      if (uri != null)
      {
        update.setIsStaleModifiedTime(
            trackServerLatencyHelper(metricsProvider, now, Timestamps.toMillis(uri.getModifiedTime()), subscribedAt,
            isIrvEnabled, fetchState)
        );
      }
    }
  }

  private static void trackServerLatencyForUris(Map<String, XdsD2.D2URI> uriMap, D2URIMapUpdate update,
      XdsServerMetricsProvider metricsProvider, long end, long subscribedAt, boolean isIrvEnabled,
      SubscriberFetchState fetchState)
  {
    uriMap.forEach((k, v) -> {
          boolean isStaleModifiedTime = trackServerLatencyHelper(metricsProvider, end, Timestamps.toMillis(v.getModifiedTime()), subscribedAt,
              isIrvEnabled, fetchState);
          update.setIsStaleModifiedTime(k, isStaleModifiedTime);
        }
      );
  }

  // -- When IRV is not enabled, the client will receive all its interested resources every time it (re-)connects,
  // so the latency should be tracked based on the max of (resource modified time, subscribed time). Caveat in
  // this is that if some resource is modified and the update is not received for network issues, then the
  // client reconnects, the latency will be tracked based on the new subscribed time, and the real latency of
  // that update is lost.
  // -- When IRV is enabled, the caveat above will be fixed. Since the client will never receive resources that it already
  // received with IRV, except the first fetch, so after skipping the first fetch we can track latency always based
  // on the resource modified time.
  private static boolean trackServerLatencyHelper(XdsServerMetricsProvider metricsProvider,
      long end, long modifiedAt, long subscribedAt, boolean isIrvEnabled, SubscriberFetchState fetchState)
  {
    long start;
    boolean isStaleModifiedAt;
    if (isIrvEnabled && !SubscriberFetchState.INIT_PENDING.equals(fetchState))
    {
      start = modifiedAt;
      isStaleModifiedAt = false;
    }
    else
    {
      start = Math.max(modifiedAt, subscribedAt);
      isStaleModifiedAt = modifiedAt < subscribedAt;
    }
    metricsProvider.trackLatency(end - start);
    return isStaleModifiedAt;
  }

  final class RpcRetryTask implements Runnable
  {
    @Override
    public void run()
    {
      startRpcStreamLocal();

      for (ResourceType originalType : ResourceType.values())
      {
        Set<String> resources = new HashSet<>(getResourceSubscriberMap(originalType).keySet());
        boolean isGlobCollection = shouldSubscribeUriGlobCollection(originalType);
        ResourceType adjustedType = isGlobCollection ? ResourceType.D2_URI : originalType;

        if (isGlobCollection)
        {
          resources = resources.stream()
              .map(GlobCollectionUtils::globCollectionUrlForClusterResource)
              .collect(Collectors.toCollection(HashSet::new));
        }

        if (getWildcardResourceSubscribers().containsKey(originalType))
        {
          resources.add("*");
        }

        if (resources.isEmpty())
        {
          continue;
        }

        Map<String, String> irv = _initialResourceVersionsEnabled
            ? getResourceVersions().get(adjustedType) : Collections.emptyMap();
        _adsStream.sendDiscoveryRequest(adjustedType, resources, irv);
      }
    }
  }

  private static final class DiscoveryRequestData
  {
    private final Node _node;
    private final ResourceType _resourceType;
    private final Collection<String> _resourceNames;
    private Map<String, String> _initialResourceVersions;

    DiscoveryRequestData(Node node, ResourceType resourceType, Collection<String> resourceNames, Map<String, String> irv)
    {
      _node = node;
      _resourceType = resourceType;
      _resourceNames = resourceNames;
      _initialResourceVersions = irv;
    }

    DeltaDiscoveryRequest toEnvoyProto()
    {
      DeltaDiscoveryRequest.Builder builder = DeltaDiscoveryRequest.newBuilder()
          .setNode(_node.toEnvoyProtoNode())
          .addAllResourceNamesSubscribe(_resourceNames)
          .setTypeUrl(_resourceType.typeUrl());

      // initial resource versions are only set when client is re-connected to the server.
      if (_initialResourceVersions != null && !_initialResourceVersions.isEmpty())
      {
        _log.debug("setting up IRV version in request, initialResourceVersions: {}", _initialResourceVersions);
        builder.putAllInitialResourceVersions(_initialResourceVersions);
      }
      return builder.build();
    }

    @Override
    public String toString()
    {
      return "DiscoveryRequestData{" + "_node=" + _node + ", _resourceType=" + _resourceType + ", _resourceNames="
          + _resourceNames + ", _initialResourceVersions=" + _initialResourceVersions + '}';
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

  @VisibleForTesting
  class AdsStream
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

    public boolean isReady()
    {
      return _requestWriter != null && ((ClientCallStreamObserver<?>) _requestWriter).isReady();
    }

    private void start()
    {
      StreamObserver<DeltaDiscoveryResponse> responseReader =
          new ClientResponseObserver<DeltaDiscoveryRequest, DeltaDiscoveryResponse>()
          {
            @Override
            public void beforeStart(ClientCallStreamObserver<DeltaDiscoveryRequest> requestStream)
            {
              requestStream.setOnReadyHandler(() -> checkShutdownAndExecute((XdsClientImpl.this::readyHandler)));
            }

            @Override
            public void onNext(DeltaDiscoveryResponse response)
            {
              checkShutdownAndExecute(() ->
              {
                _xdsClientJmx.incrementResponseReceivedCount();
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
              checkShutdownAndExecute((() -> handleRpcError(t)));
            }

            @Override
            public void onCompleted()
            {
              checkShutdownAndExecute(() -> handleRpcCompleted());
            }
          };
      _requestWriter = _stub.withWaitForReady().deltaAggregatedResources(responseReader);
    }

    /**
     * Sends a client-initiated discovery request.
     */
    @VisibleForTesting
    void sendDiscoveryRequest(ResourceType type, Collection<String> resources, Map<String, String> resourceVersions)
    {
      _log.info("Sending {} request for resources: {}, resourceVersions size: {}",
          type, resources, resourceVersions.size());
      _xdsClientJmx.incrementRequestSentCount();
      _xdsClientJmx.addToIrvSentCount(resourceVersions.size());
      DeltaDiscoveryRequest request = new DiscoveryRequestData(_node, type, resources, resourceVersions).toEnvoyProto();
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
      _log.warn("ADS stream closed with status {}: {}", error.getCode(), error.getDescription(), error.getCause());
      _closed = true;
      notifyStreamError(error);
      cleanUp();
      if (_responseReceived || _retryBackoffPolicy == null)
      {
        // Reset the backoff sequence if had received a response, or backoff sequence
        // has never been initialized.
        _retryBackoffPolicy = _backoffPolicyProvider.get();
      }
      long delayNanos = 0;
      if (!_responseReceived)
      {
        delayNanos = Math.min(_retryBackoffPolicy.nextBackoffNanos(), _maxRetryBackoffNanos);
      }
      _log.info("Retry ADS stream in {} ns", delayNanos);
      _retryRpcStreamFuture = checkShutdownAndSchedule(new RpcRetryTask(), delayNanos, TimeUnit.NANOSECONDS);
    }

    private void close(Exception error)
    {
      if (_closed)
      {
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
