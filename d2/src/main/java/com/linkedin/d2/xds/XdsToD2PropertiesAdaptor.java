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
import org.apache.commons.lang3.StringUtils;
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
  private final List<XdsConnectionListener> _xdsConnectionListeners;

  private final ServicePropertiesJsonSerializer _servicePropertiesJsonSerializer;
  private final ClusterPropertiesJsonSerializer _clusterPropertiesJsonSerializer;
  private final UriPropertiesJsonSerializer _uriPropertiesJsonSerializer;
  private final UriPropertiesMerger _uriPropertiesMerger;
  private final DualReadStateManager _dualReadStateManager;
  private final ConcurrentMap<String, XdsClient.NodeResourceWatcher> _watchedClusterResources;
  private final ConcurrentMap<String, XdsClient.SymlinkNodeResourceWatcher> _watchedSymlinkResources;
  private final ConcurrentMap<String, XdsClient.NodeResourceWatcher> _watchedServiceResources;
  private final ConcurrentMap<String, XdsClient.D2URIMapResourceWatcher> _watchedUriResources;
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

  private Boolean _isAvailable;
  private PropertyEventBus<UriProperties> _uriEventBus;
  private PropertyEventBus<ServiceProperties> _serviceEventBus;
  private PropertyEventBus<ClusterProperties> _clusterEventBus;

  public XdsToD2PropertiesAdaptor(XdsClient xdsClient, DualReadStateManager dualReadStateManager,
      ServiceDiscoveryEventEmitter eventEmitter)
  {
    _xdsClient = xdsClient;
    _dualReadStateManager = dualReadStateManager;
    _xdsConnectionListeners = Collections.synchronizedList(new ArrayList<>());
    _servicePropertiesJsonSerializer = new ServicePropertiesJsonSerializer();
    _clusterPropertiesJsonSerializer = new ClusterPropertiesJsonSerializer();
    _uriPropertiesJsonSerializer = new UriPropertiesJsonSerializer();
    _uriPropertiesMerger = new UriPropertiesMerger();
    // set to null so that the first notification on connection establishment success/failure is always sent
    _isAvailable = null;
    _watchedClusterResources = new ConcurrentHashMap<>();
    _watchedSymlinkResources = new ConcurrentHashMap<>();
    _watchedServiceResources = new ConcurrentHashMap<>();
    _watchedUriResources = new ConcurrentHashMap<>();
    _eventEmitter = eventEmitter;
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
      _watchedClusterResources.computeIfAbsent(clusterName, k ->
      {
        XdsClient.NodeResourceWatcher watcher = getClusterResourceWatcher(clusterName);
        _xdsClient.watchXdsResource(resourceName, XdsClient.ResourceType.NODE, watcher);
        return watcher;
      });
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
      _watchedUriResources.computeIfAbsent(clusterName, k ->
      {
        XdsClient.D2URIMapResourceWatcher watcher = getUriResourceWatcher(clusterName);
        _xdsClient.watchXdsResource(resourceName, XdsClient.ResourceType.D2_URI_MAP, watcher);
        return watcher;
      });
    }
  }

  public void listenToService(String serviceName)
  {
    _watchedServiceResources.computeIfAbsent(serviceName, k ->
    {
      XdsClient.NodeResourceWatcher watcher = getServiceResourceWatcher(serviceName);
      _xdsClient.watchXdsResource(D2_SERVICE_NODE_PREFIX + serviceName, XdsClient.ResourceType.NODE, watcher);
      return watcher;
    });
  }

  private void listenToSymlink(String name, String fullResourceName)
  {
    // use full resource name ("/d2/clusters/$FooClusterMater", "/d2/uris/$FooClusterMaster") as the key
    // instead of just the symlink name ("$FooClusterMaster") to differentiate clusters and uris symlink resources.
    _watchedSymlinkResources.computeIfAbsent(fullResourceName, k ->
    {
      // use symlink name "$FooClusterMaster" to create the watcher
      XdsClient.SymlinkNodeResourceWatcher watcher = getSymlinkResourceWatcher(name);
      _xdsClient.watchXdsResource(k, XdsClient.ResourceType.NODE, watcher);
      return watcher;
    });
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
          catch (PropertySerializationException e)
          {
            LOG.error("Failed to parse D2 service properties from xDS update. Service name: " + serviceName, e);
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
            // For symlink clusters, ClusterLoadBalancerSubscriber subscribed to the symlinks instead of the actual node in event bus,
            // so we need to publish under the symlink names.
            // For other clusters, publish under its original name. Note that these clusters could be either:
            // 1) regular clusters requested normally.
            // 2) clusters that were pointed by a symlink previously, but no longer the case after the symlink points to other clusters.
            // For case #2: the symlinkAndActualNode map will no longer has an entry for this cluster (removed in
            // D2SymlinkNodeResourceWatcher::onChanged), thus the updates will be published under the original cluster name
            // (like "FooCluster-prod-ltx1"), which has no symlink subscribers anyway, so no harm to publish.
            String publishName = StringUtils.defaultIfEmpty(getSymlink(clusterName), clusterName);
            _clusterEventBus.publishInitialize(publishName, clusterProperties);

            if (_dualReadStateManager != null)
            {
              _dualReadStateManager.reportData(publishName, clusterProperties, true);
            }
          }
          catch (PropertySerializationException e)
          {
            LOG.error("Failed to parse D2 cluster properties from xDS update. Cluster name: " + clusterName, e);
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

  XdsClient.D2URIMapResourceWatcher getUriResourceWatcher(String clusterName)
  {
    return new UriPropertiesResourceWatcher(clusterName);
  }

  XdsClient.SymlinkNodeResourceWatcher getSymlinkResourceWatcher(String symlinkName)
  {
    return new XdsClient.SymlinkNodeResourceWatcher()
    {
      @Override
      public void onChanged(String resourceName, XdsClient.NodeUpdate update)
      {
        // Update maps between symlink name and actual node name
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

  private String getSymlink(String actualNodeName) {
    synchronized (_symlinkAndActualNodeLock) {
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

  private Map<String, UriProperties> toUriProperties(Map<String, XdsD2.D2URI> uriDataMap)
      throws PropertySerializationException
  {
    Map<String, UriProperties> parsedMap = new HashMap<>();

    for (Map.Entry<String, XdsD2.D2URI> entry : uriDataMap.entrySet())
    {
      XdsD2.D2URI d2URI = entry.getValue();
      UriProperties uriProperties = _uriPropertiesJsonSerializer.fromProto(d2URI);
      if (uriProperties.getVersion() < 0)
      {
        LOG.warn("xDS data for {} has invalid version: {}", entry.getKey(), uriProperties.getVersion());
      }
      parsedMap.put(entry.getKey(), uriProperties);
    }

    return parsedMap;
  }

  private class UriPropertiesResourceWatcher implements XdsClient.D2URIMapResourceWatcher
  {
    final String _clusterName;
    final AtomicBoolean _isInit;
    final long _initFetchStart;

    Map<String, UriProperties> _currentData = new HashMap<>();

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

      if (_uriEventBus != null)
      {
        try
        {
          Map<String, UriProperties> updates = toUriProperties(update.getURIMap());
          if (!isInit)
          {
            emitSDStatusUpdateReceiptEvents(updates);
          }
          _currentData = updates;

          // For symlink clusters, UriLoadBalancerSubscriber subscribed to the symlinks instead of the actual node in event bus,
          // so we need to publish under the symlink names.
          // For other clusters, publish under its original name. Note that these clusters could be either:
          // 1) regular clusters requested normally.
          // 2) clusters that were pointed by a symlink previously, but no longer the case after the symlink points to other clusters.
          // For case #2: the actualResourceToSymlink map will no longer has an entry for this cluster (removed in
          // D2SymlinkNodeResourceWatcher::onChanged), thus the updates will be published under the original cluster name
          // (like "FooCluster-prod-ltx1"), which has no subscribers anyway, so no harm to publish. Yet, we still emit the tracking
          // events about receiving uri updates of this cluster for measuring update propagation latencies.
          String publishName = StringUtils.defaultIfEmpty(getSymlink(_clusterName), _clusterName);
          UriProperties mergedUriProperties = _uriPropertiesMerger.merge(publishName, _currentData.values());
          _uriEventBus.publishInitialize(publishName, mergedUriProperties);

          if (_dualReadStateManager != null)
          {
            _dualReadStateManager.reportData(publishName, mergedUriProperties, true);
          }
        }
        catch (PropertySerializationException e)
        {
          LOG.error("Failed to parse D2 uri properties from xDS update. Cluster name: " + _clusterName, e);
        }
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

    private void emitSDStatusUpdateReceiptEvents(Map<String, UriProperties> updates)
    {
      if (_eventEmitter == null)
      {
        LOG.info("Service discovery event emitter in XdsToD2PropertiesAdaptor is null. Skipping emitting events.");
        return;
      }

      long timestamp = System.currentTimeMillis();

      MapDifference<String, UriProperties> mapDifference = Maps.difference(_currentData, updates);
      Map<String, UriProperties> markedDownUris = mapDifference.entriesOnlyOnLeft();
      Map<String, UriProperties> markedUpUris = mapDifference.entriesOnlyOnRight();

      emitSDStatusUpdateReceiptEvents(markedUpUris, true, timestamp);
      emitSDStatusUpdateReceiptEvents(markedDownUris, false, timestamp);
    }

    private void emitSDStatusUpdateReceiptEvents(Map<String, UriProperties> updates, boolean isMarkUp, long timestamp)
    {
      updates.forEach((nodeName, data) ->
      {
        String nodePath = D2_URI_NODE_PREFIX + _clusterName + "/" + nodeName;
        data.Uris().forEach(uri ->
            _eventEmitter.emitSDStatusUpdateReceiptEvent(
                _clusterName,
                uri.getHost(),
                uri.getPort(),
                isMarkUp ? ServiceDiscoveryEventEmitter.StatusUpdateActionType.MARK_READY :
                    ServiceDiscoveryEventEmitter.StatusUpdateActionType.MARK_DOWN,
                true,
                _xdsClient.getXdsServerAuthority(),
                nodePath,
                data.toString(),
                0,
                nodePath,
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
