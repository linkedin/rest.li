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

import com.google.common.collect.HashBiMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter;
import com.linkedin.d2.discovery.stores.zk.SymlinkUtil;
import indis.XdsD2;
import io.grpc.Status;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsToD2PropertiesAdaptor
{
  private static final Logger LOG = LoggerFactory.getLogger(XdsToD2PropertiesAdaptor.class);
  private static final String D2_CLUSTER_NODE_PREFIX = "/d2/clusters/";
  private static final String D2_SERVICE_NODE_PREFIX = "/d2/services/";
  private static final String D2_URI_NODE_PREFIX = "/d2/uris/";
  private static final char PATH_SEPARATOR = '/';
  private static final String NON_EXISTENT_CLUSTER = "NonExistentCluster";

  private final XdsClient _xdsClient;
  private final List<XdsConnectionListener> _xdsConnectionListeners = Collections.synchronizedList(new ArrayList<>());

  private final ServicePropertiesJsonSerializer _servicePropertiesJsonSerializer;
  private final ClusterPropertiesJsonSerializer _clusterPropertiesJsonSerializer = new ClusterPropertiesJsonSerializer();
  private final UriPropertiesJsonSerializer _uriPropertiesJsonSerializer = new UriPropertiesJsonSerializer();
  private final UriPropertiesMerger _uriPropertiesMerger = new UriPropertiesMerger();
  private final DualReadStateManager _dualReadStateManager;
  private final ConcurrentMap<String, XdsClient.NodeResourceWatcher> _watchedClusterResources =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<String, XdsClient.NodeResourceWatcher> _watchedSymlinkResources =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<String, XdsClient.NodeResourceWatcher> _watchedServiceResources =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<String, XdsClient.ResourceWatcher> _watchedUriResources =
      new ConcurrentHashMap<>();
  // Mapping between a symlink name, like "$FooClusterMaster" and the actual node name it's pointing to, like
  // "FooCluster-prod-ltx1".
  // (Note that this name does NOT include the full path so that it works for both cluster symlink
  // "/d2/clusters/$FooClusterMaster" and uri-parent symlink "/d2/uris/$FooClusterMaster").
  private final HashBiMap<String, String> _symlinkAndActualNode = HashBiMap.create();
  // lock for the above BiMap. Note that currently xDSClientImpl just use a single-thread executor service,
  // so the xDS resource update is processed one-by-one, meaning a read and an update to the above map will never
  // happen concurrently. We still make this thread-safe just in case we need to add threads to xDSClient in the
  // future.
  private final Object _symlinkAndActualNodeLock = new Object();
  private final ServiceDiscoveryEventEmitter _eventEmitter;

  // set to null so that the first notification on connection establishment success/failure is always sent
  private Boolean _isAvailable = null;
  private PropertyEventBus<UriProperties> _uriEventBus;
  private PropertyEventBus<ServiceProperties> _serviceEventBus;
  private PropertyEventBus<ClusterProperties> _clusterEventBus;

  public XdsToD2PropertiesAdaptor(XdsClient xdsClient, DualReadStateManager dualReadStateManager,
      ServiceDiscoveryEventEmitter eventEmitter)
  {
    this(xdsClient, dualReadStateManager, eventEmitter, Collections.emptyMap());
  }

  public XdsToD2PropertiesAdaptor(XdsClient xdsClient, DualReadStateManager dualReadStateManager,
      ServiceDiscoveryEventEmitter eventEmitter, Map<String, Map<String, Object>> clientServicesConfig)
  {
    _xdsClient = xdsClient;
    _dualReadStateManager = dualReadStateManager;
    _eventEmitter = eventEmitter;
    _servicePropertiesJsonSerializer = new ServicePropertiesJsonSerializer(clientServicesConfig);
  }

  public void start()
  {
    _xdsClient.startRpcStream();
    // Watch any resource to get notified of xds connection updates, including initial connection establishment.
    // TODO: Note, this is a workaround since the xDS client implementation currently integrates connection
    //   error/success notifications along with the resource updates. This can be improved in a future refactor.
    listenToCluster(NON_EXISTENT_CLUSTER);
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
    // if cluster name is a symlink, watch for D2SymlinkNode instead
    String resourceName = D2_CLUSTER_NODE_PREFIX + clusterName;
    if (SymlinkUtil.isSymlinkNodeOrPath(clusterName))
    {
      listenToSymlink(clusterName, resourceName);
    }
    else
    {
      XdsClient.ResourceWatcher watcher =
          _watchedClusterResources.computeIfAbsent(clusterName, this::getClusterResourceWatcher);
      _xdsClient.watchXdsResource(resourceName, watcher);
    }
  }

  public void listenToUris(String clusterName)
  {
    // if cluster name is a symlink, watch for D2SymlinkNode instead
    String resourceName = D2_URI_NODE_PREFIX + clusterName;
    if (SymlinkUtil.isSymlinkNodeOrPath(clusterName))
    {
      listenToSymlink(clusterName, resourceName);
    }
    else
    {
      XdsClient.ResourceWatcher watcher =
          _watchedUriResources.computeIfAbsent(clusterName, this::getUriResourceWatcher);
      _xdsClient.watchXdsResource(resourceName, watcher);
    }
  }

  public void listenToService(String serviceName)
  {
    XdsClient.ResourceWatcher watcher =
        _watchedServiceResources.computeIfAbsent(serviceName, this::getServiceResourceWatcher);
    _xdsClient.watchXdsResource(D2_SERVICE_NODE_PREFIX + serviceName, watcher);
  }

  private void listenToSymlink(String name, String fullResourceName)
  {
    // use full resource name ("/d2/clusters/$FooClusterMater", "/d2/uris/$FooClusterMaster") as the key
    // instead of just the symlink name ("$FooClusterMaster") to differentiate clusters and uris symlink resources.
    XdsClient.ResourceWatcher watcher =
        _watchedSymlinkResources.computeIfAbsent(fullResourceName, k -> getSymlinkResourceWatcher(k, name));
    // use symlink name "$FooClusterMaster" to create the watcher
    _xdsClient.watchXdsResource(fullResourceName, watcher);
  }

  XdsClient.NodeResourceWatcher getServiceResourceWatcher(String serviceName)
  {
    return new XdsClient.NodeResourceWatcher()
    {
      @Override
      public void onChanged(XdsClient.NodeUpdate update)
      {
        if (_serviceEventBus != null)
        {
          try
          {
            ServiceProperties serviceProperties = toServiceProperties(update.getNodeData());
            _serviceEventBus.publishInitialize(serviceName, serviceProperties);
            if (_dualReadStateManager != null)
            {
              _dualReadStateManager.reportData(serviceName, serviceProperties, true);
            }
          }
          catch (Exception e)
          {
            // still notify event bus to avoid timeout in case some subscribers are waiting for the data
            LOG.warn(
                "Failed to parse D2 service properties from xDS update. Service name: {}.  Publishing null to event bus",
                serviceName);
            _serviceEventBus.publishInitialize(serviceName, null);
          }
        }
        else
        {
          // Received xds resource update while service event bus is not set. Notify that the xds source becomes
          // available instead.
          onReconnect();
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

  XdsClient.NodeResourceWatcher getClusterResourceWatcher(String clusterName)
  {
    return new XdsClient.NodeResourceWatcher()
    {
      @Override
      public void onChanged(XdsClient.NodeUpdate update)
      {
        if (_clusterEventBus != null)
        {
          try
          {
            ClusterProperties clusterProperties = toClusterProperties(update.getNodeData());
            // For symlink clusters, ClusterLoadBalancerSubscriber subscribed to the symlinks ($FooClusterMaster) instead of
            // the original cluster (FooCluster-prod-ltx1) in event bus, so we need to publish under the symlink names.
            // Also, rarely but possibly, calls can be made directly to the colo-suffixed service (FooService-prod-ltx1) under
            // the original cluster (FooCluster-prod-ltx1) via curli, hard-coded custom code, etc, so there could be direct
            // subscribers to the original cluster, thus we need to publish under the original cluster too.
            //
            // For other clusters, publish under its original name. Note that these clusters could be either:
            // 1) regular clusters requested normally.
            // 2) clusters that were pointed by a symlink previously, but no longer the case after the symlink points to other clusters.
            // For case #2: the symlinkAndActualNode map will no longer has an entry for this cluster (removed in
            // D2SymlinkNodeResourceWatcher::onChanged), thus the updates will be published under the original cluster name
            // (like "FooCluster-prod-ltx1") in case there are direct subscribers.
            String symlinkName = getSymlink(clusterName);
            if (symlinkName != null)
            {
              publishClusterData(symlinkName, clusterProperties);
            }
            publishClusterData(clusterName, clusterProperties);
          }
          catch (Exception e)
          {
            // still notify event bus to avoid timeout in case some subscribers are waiting for the data
            LOG.warn(
                "Failed to parse D2 cluster properties from xDS update. Cluster name: {}, Publishing null to event bus",
                clusterName);
            _clusterEventBus.publishInitialize(clusterName, null);
          }
        }
        else
        {
          // Received xds resource update while cluster event bus is not set. Notify that the xds source becomes
          // available instead.
          onReconnect();
        }
      }

      private void publishClusterData(String clusterName, ClusterProperties properties)
      {
        _clusterEventBus.publishInitialize(clusterName, properties);
        if (_dualReadStateManager != null)
        {
          _dualReadStateManager.reportData(clusterName, properties, true);
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

  XdsClient.D2URIMapResourceWatcher getUriResourceWatcher(String clusterName)
  {
    return new UriPropertiesResourceWatcher(clusterName);
  }

  XdsClient.NodeResourceWatcher getSymlinkResourceWatcher(String resourceName, String symlinkName)
  {
    return new XdsClient.NodeResourceWatcher()
    {
      @Override
      public void onChanged(XdsClient.NodeUpdate update)
      {
        // Update maps between symlink name and actual node name
        try
        {
          String actualResourceName = update.getNodeData().getData().toString(StandardCharsets.UTF_8);
          String actualNodeName = getNodeName(actualResourceName);
          updateSymlinkAndActualNodeMap(symlinkName, actualNodeName);
          // listen to the actual nodes
          // Note: since cluster symlink and uri parent symlink always point to the same actual node name, and it's a
          // redundancy and a burden for the symlink-update tool to maintain two symlinks for the same actual node name,
          // we optimize here to use the cluster symlink to listen to the actual nodes for both cluster
          // and uri parent.
          listenToCluster(actualNodeName);
          listenToUris(actualNodeName);
        }
        catch (Exception e)
        {
          if (resourceName.startsWith(D2_CLUSTER_NODE_PREFIX))
          {
            LOG.error("Failed to parse cluster symlink data from xDS update. Symlink name: {}", symlinkName, e);
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

  private void updateSymlinkAndActualNodeMap(String symlinkName, String actualNodeName) {
    synchronized (_symlinkAndActualNodeLock) {
      _symlinkAndActualNode.put(symlinkName, actualNodeName);
    }
  }

  private String removeSymlink(String symlinkName)
  {
    synchronized (_symlinkAndActualNodeLock)
    {
      return _symlinkAndActualNode.remove(symlinkName);
    }
  }

  private String getSymlink(String actualNodeName)
  {
    synchronized (_symlinkAndActualNodeLock)
    {
      return _symlinkAndActualNode.inverse().get(actualNodeName);
    }
  }

  private static String getNodeName(String path)
  {
    return path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1);
  }

  private void notifyAvailabilityChanges(boolean isAvailable)
  {
    synchronized (_xdsConnectionListeners)
    {
      if (_isAvailable == null || _isAvailable != isAvailable)
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

  private ServiceProperties toServiceProperties(XdsD2.Node serviceProperties) throws PropertySerializationException
  {
    return _servicePropertiesJsonSerializer.fromBytes(serviceProperties.getData(),
        serviceProperties.getStat().getMzxid());
  }

  private ClusterProperties toClusterProperties(XdsD2.Node clusterProperties) throws PropertySerializationException
  {
    return _clusterPropertiesJsonSerializer.fromBytes(clusterProperties.getData(),
        clusterProperties.getStat().getMzxid());
  }

  private class UriPropertiesResourceWatcher extends XdsClient.D2URIMapResourceWatcher
  {
    final String _clusterName;
    final AtomicBoolean _isInit;
    final long _initFetchStart;

    Map<String, XdsAndD2Uris> _currentData = new HashMap<>();

    private class XdsAndD2Uris
    {
      final String _uriName;
      final XdsD2.D2URI _xdsUri;
      final UriProperties _d2Uri;

      XdsAndD2Uris(String uriName, XdsD2.D2URI xdsUri, UriProperties d2Uri)
      {
        _uriName = uriName;
        _xdsUri = xdsUri;
        _d2Uri = d2Uri;
      }
    }

    public UriPropertiesResourceWatcher(String clusterName)
    {
      _clusterName = clusterName;
      _isInit = new AtomicBoolean(true);
      _initFetchStart = System.nanoTime();
    }

    @Override
    public void onChanged(XdsClient.D2URIMapUpdate update)
    {
      boolean isInit = _isInit.compareAndSet(true, false);
      if (isInit)
      {
        emitSDStatusInitialRequestEvent(_clusterName, true);
      }
      Map<String, XdsAndD2Uris> updates;
      try
      {
        updates = update.getURIMap().entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey, e ->
                new XdsAndD2Uris(e.getKey(), e.getValue(), toUriProperties(e.getKey(), e.getValue())))
        );
        updates.values().removeIf(u ->
        {
          if (u._d2Uri == null)
          {
            LOG.warn("Failed to parse D2 uri properties for uri: {} in cluster: {} from xDS D2URI: {}."
                    + " Removing it from the update.",
                u._uriName, _clusterName, u._xdsUri);
          }
          return u._d2Uri == null;
        });
      }
      catch (Exception e)
      {
        LOG.warn("Failed to parse D2 uri properties from xDS update. Cluster name: {}. Publishing null to event bus",
            _clusterName);
        _uriEventBus.publishInitialize(_clusterName, null);
        return;
      }

      if (!isInit && !_currentData.isEmpty())
      {
        emitSDStatusUpdateReceiptEvents(updates);
      }
      _currentData = updates;

      // For symlink clusters, UriLoadBalancerSubscriber subscribed to the symlinks ($FooClusterMaster) instead of
      // the original cluster (FooCluster-prod-ltx1) in event bus, so we need to publish under the symlink names.
      // Also, rarely but possibly, calls can be made directly to the colo-suffixed service (FooService-prod-ltx1) under
      // the original cluster (FooCluster-prod-ltx1) via curli, hard-coded custom code, etc, so there could be direct
      // subscribers to the original cluster, thus we need to publish under the original cluster too.
      //
      // For other clusters, publish under its original name. Note that these clusters could be either:
      // 1) regular clusters requested normally.
      // 2) clusters that were pointed by a symlink previously, but no longer the case after the symlink points to other clusters.
      // For case #2: the symlinkAndActualNode map will no longer has an entry for this cluster (removed in
      // D2SymlinkNodeResourceWatcher::onChanged), thus the updates will be published under the original cluster name
      // (like "FooCluster-prod-ltx1") in case there are direct subscribers.
      String symlinkName = getSymlink(_clusterName);
      if (symlinkName != null)
      {
        mergeAndPublishUris(symlinkName); // under symlink name, merge data and publish it
      }
      mergeAndPublishUris(_clusterName); // under original cluster name, merge data and publish it
    }

    private UriProperties toUriProperties(String uriName, XdsD2.D2URI xdsUri)
    {
      UriProperties uriProperties = null;
      try {
        uriProperties = _uriPropertiesJsonSerializer.fromProto(xdsUri);
        if (uriProperties.getVersion() < 0)
        {
          LOG.warn("xDS data: {} for uri: {} in cluster: {} has invalid version: {}",
              xdsUri, uriName, _clusterName, uriProperties.getVersion());
        }
      }
      catch (PropertySerializationException e)
      {
        LOG.error(String.format("Failed to parse D2 uri properties for uri: %s in cluster: %s from xDS data: %s",
            uriName, _clusterName, xdsUri), e);
      }

      return uriProperties;
    }

    private void mergeAndPublishUris(String clusterName)
    {
      UriProperties mergedUriProperties = _uriPropertiesMerger.merge(clusterName,
          _currentData.values().stream().map(xdsAndD2Uris -> xdsAndD2Uris._d2Uri).collect(Collectors.toList()));
      if (mergedUriProperties.getVersion() == -1)
      {
        LOG.warn("xDS UriProperties has invalid version -1. Raw uris: {}", _currentData.values());
      }

      if (_uriEventBus != null)
      {
        _uriEventBus.publishInitialize(clusterName, mergedUriProperties);
      }
      else
      {
        // Received xds resource update while uri event bus is not set. Notify that the xds source becomes
        // available instead.
        onReconnect();
      }

      if (_dualReadStateManager != null)
      {
        _dualReadStateManager.reportData(clusterName, mergedUriProperties, true);
      }
    }

    @Override
    public void onError(Status error)
    {
      if (_isInit.get())
      {
        emitSDStatusInitialRequestEvent(_clusterName, false);
      }
      notifyAvailabilityChanges(false);
    }

    @Override
    public void onReconnect()
    {
      notifyAvailabilityChanges(true);
    }

    private void emitSDStatusInitialRequestEvent(String cluster, boolean succeeded)
    {
      if (_eventEmitter == null)
      {
        LOG.info("Service discovery event emitter in XdsToD2PropertiesAdaptor is null. Skipping emitting events.");
        return;
      }

      // measure request duration and convert to milli-seconds
      long initialFetchDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - _initFetchStart);
      if (initialFetchDurationMillis < 0)
      {
        LOG.warn("Failed to log ServiceDiscoveryStatusInitialRequest event, initialFetchStartAt time is greater than current time.");
        return;
      }
      // emit service discovery status initial request event for success
      _eventEmitter.emitSDStatusInitialRequestEvent(cluster, true, initialFetchDurationMillis, succeeded);

    }

    private void emitSDStatusUpdateReceiptEvents(Map<String, XdsAndD2Uris> updates)
    {
      if (_eventEmitter == null)
      {
        LOG.info("Service discovery event emitter in XdsToD2PropertiesAdaptor is null. Skipping emitting events.");
        return;
      }

      long timestamp = System.currentTimeMillis();

      MapDifference<String, XdsAndD2Uris> mapDifference = Maps.difference(_currentData, updates);
      Map<String, XdsAndD2Uris> markedDownUris = mapDifference.entriesOnlyOnLeft();
      Map<String, XdsAndD2Uris> markedUpUris = mapDifference.entriesOnlyOnRight();

      emitSDStatusUpdateReceiptEvents(markedUpUris, true, timestamp);
      emitSDStatusUpdateReceiptEvents(markedDownUris, false, timestamp);
    }

    private void emitSDStatusUpdateReceiptEvents(Map<String, XdsAndD2Uris> updates, boolean isMarkUp, long timestamp)
    {
      updates.values().forEach(xdsAndD2Uris ->
      {
        UriProperties d2Uri = xdsAndD2Uris._d2Uri;
        XdsD2.D2URI xdsUri = xdsAndD2Uris._xdsUri;
        String nodePath = D2_URI_NODE_PREFIX + _clusterName + "/" + xdsAndD2Uris._uriName;
        d2Uri.Uris().forEach(uri ->
            _eventEmitter.emitSDStatusUpdateReceiptEvent(
                _clusterName,
                uri.getHost(),
                uri.getPort(),
                isMarkUp ? ServiceDiscoveryEventEmitter.StatusUpdateActionType.MARK_READY :
                    ServiceDiscoveryEventEmitter.StatusUpdateActionType.MARK_DOWN,
                true,
                _xdsClient.getXdsServerAuthority(),
                nodePath,
                xdsUri.toString(),
                (int) xdsUri.getVersion(),
                xdsUri.getTracingId(),
                timestamp)
        );
      });
    }
  }

  public interface XdsConnectionListener
  {
    void onError();

    void onReconnect();
  }
}
