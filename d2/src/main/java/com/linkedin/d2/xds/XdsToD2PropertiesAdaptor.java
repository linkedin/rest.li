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

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XdsToD2PropertiesAdaptor
{
  private static final Logger _log = LoggerFactory.getLogger(XdsToD2PropertiesAdaptor.class);
  private static final String D2_CLUSTER_NODE_PREFIX = "/d2/clusters/";
  private static final String D2_SERVICE_NODE_PREFIX = "/d2/services/";
  private static final String D2_URI_NODE_PREFIX = "/d2/uris/";
  private static final char SYMLINK_NODE_IDENTIFIER = '$';

  private final XdsClient _xdsClient;
  private final List<XdsConnectionListener> _xdsConnectionListeners;

  private final ServicePropertiesJsonSerializer _servicePropertiesJsonSerializer;
  private final ClusterPropertiesJsonSerializer _clusterPropertiesJsonSerializer;
  private final UriPropertiesJsonSerializer _uriPropertiesJsonSerializer;
  private final UriPropertiesMerger _uriPropertiesMerger;
  private final DualReadStateManager _dualReadStateManager;
  private final ConcurrentMap<String, XdsClient.D2NodeResourceWatcher> _watchedClusterResources;
  private final ConcurrentMap<String, XdsClient.D2SymlinkNodeResourceWatcher> _watchedSymlinkResources;
  private final ConcurrentMap<String, XdsClient.D2NodeResourceWatcher> _watchedServiceResources;
  private final ConcurrentMap<String, XdsClient.D2NodeMapResourceWatcher> _watchedUriResources;
  // Mapping from a symlink name, like "$FooClusterMaster", to the actual node name it's pointing to, like
  // "FooCluster-prod-ltx1".
  // Note that this works for both cluster symlink "/d2/clusters/$FooClusterMaster" and uri-parent symlink
  // "/d2/uris/$FooClusterMaster".
  private final ConcurrentMap<String, String> _symlinkToActualNode;
  // An inverse mapping of the above map.
  private final ConcurrentMap<String, String> _actualNodeToSymlink;
  private final ServiceDiscoveryEventEmitter _eventEmitter;

  private boolean _isAvailable;
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
    _isAvailable = false;
    _watchedClusterResources = new ConcurrentHashMap<>();
    _watchedSymlinkResources = new ConcurrentHashMap<>();
    _watchedServiceResources = new ConcurrentHashMap<>();
    _watchedUriResources = new ConcurrentHashMap<>();
    _symlinkToActualNode = new ConcurrentHashMap<>();
    _actualNodeToSymlink = new ConcurrentHashMap<>();
    _eventEmitter = eventEmitter;
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

  private boolean isSymlinkNode(String nodeNameOrPath)
  {
    return nodeNameOrPath != null && nodeNameOrPath.indexOf(SYMLINK_NODE_IDENTIFIER) >= 0;
  }

  public void registerXdsConnectionListener(XdsConnectionListener listener)
  {
    _xdsConnectionListeners.add(listener);
  }

  public void listenToCluster(String clusterName)
  {
    // if cluster name is a symlink, watch for D2SymlinkNode instead
    String resourceName = D2_CLUSTER_NODE_PREFIX + clusterName;
    if (!checkAndListenToSymlink(clusterName, resourceName))
    {
      _watchedClusterResources.computeIfAbsent(clusterName, k ->
      {
        XdsClient.D2NodeResourceWatcher watcher = getClusterResourceWatcher(clusterName);
        _xdsClient.watchXdsResource(resourceName, XdsClient.ResourceType.D2_NODE,
            watcher);
        return watcher;
      });
    }
  }

  public void listenToUris(String clusterName)
  {
    // if cluster name is a symlink, watch for D2SymlinkNode instead
    String resourceName = D2_URI_NODE_PREFIX + clusterName;
    if (!checkAndListenToSymlink(clusterName, resourceName))
    {
      _watchedUriResources.computeIfAbsent(clusterName, k ->
      {
        XdsClient.D2NodeMapResourceWatcher watcher = getUriResourceWatcher(clusterName);
        _xdsClient.watchXdsResource(resourceName, XdsClient.ResourceType.D2_NODE_MAP,
            watcher);
        return watcher;
      });
    }
  }

  private boolean checkAndListenToSymlink(String symlinkName, String fullResourceName)
  {
    boolean isSymlink = isSymlinkNode(symlinkName);
    if (isSymlink)
    {
      // use full resource name ("/d2/clusters/$FooClusterMater", "/d2/uris/$FooClusterMaster") as the key
      // instead of just the symlink name ("$FooClusterMaster") to differentiate clusters and uris symlink resources.
      _watchedSymlinkResources.computeIfAbsent(fullResourceName, k ->
      {
        // use symlink name "$FooClusterMaster" to create the watcher
        XdsClient.D2SymlinkNodeResourceWatcher watcher = getSymlinkResourceWatcher(symlinkName);
        _xdsClient.watchXdsResource(fullResourceName, XdsClient.ResourceType.D2_SYMLINK_NODE,
            watcher);
        return watcher;
      });
    }

    return isSymlink;
  }

  public void listenToService(String serviceName)
  {
    _watchedServiceResources.computeIfAbsent(serviceName, k ->
    {
      XdsClient.D2NodeResourceWatcher watcher = getServiceResourceWatcher(serviceName);
      _xdsClient.watchXdsResource(D2_SERVICE_NODE_PREFIX + serviceName, XdsClient.ResourceType.D2_NODE,
          watcher);
      return watcher;
    });
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
            ServiceProperties serviceProperties = toServiceProperties(update.getNodeData().getData(),
                update.getNodeData().getStat().getMzxid());
            _serviceEventBus.publishInitialize(serviceName, serviceProperties);
            if (_dualReadStateManager != null)
            {
              _dualReadStateManager.reportData(serviceName, serviceProperties, true);
            }
          }
          catch (InvalidProtocolBufferException | PropertySerializationException e)
          {
            _log.error("Failed to parse D2 service properties from xDS update. Service name: " + serviceName, e);
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

  XdsClient.D2SymlinkNodeResourceWatcher getSymlinkResourceWatcher(String symlinkName)
  {
    return new XdsClient.D2SymlinkNodeResourceWatcher()
    {
      @Override
      public void onChanged(String resourceName, XdsClient.D2SymlinkNodeUpdate update)
      {
        // update maps between symlink name and actual node name, listen to the actual node
        String actualResourceName = update.getNodeData().getMasterClusterNodePath();
        if (resourceName.contains(D2_CLUSTER_NODE_PREFIX))
        {
          String actualNodeName = removeNodePathPrefix(actualResourceName, D2_CLUSTER_NODE_PREFIX);
          updateSymlinkAndActualNodeMaps(symlinkName, actualNodeName);
          listenToCluster(actualNodeName);
        }
        else
        {
          String actualNodeName = removeNodePathPrefix(actualResourceName, D2_URI_NODE_PREFIX);
          updateSymlinkAndActualNodeMaps(symlinkName, actualNodeName);
          listenToUris(actualNodeName);
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

  private void updateSymlinkAndActualNodeMaps(String symlinkName, String actualNodeName)
  {
    // update symlinkToActualNode map.
    _symlinkToActualNode.compute(symlinkName, (k, v) -> {
      // If existing actual node differs from new node, remove the existing entry in actualNodeToSymlink map.
      if (v != null && !v.equals(actualNodeName))
      {
        _actualNodeToSymlink.remove(v);
      }
      return actualNodeName;
    });

    _actualNodeToSymlink.put(actualNodeName, symlinkName);
  }

  private String removeNodePathPrefix(String path, String prefix)
  {
    int idx = path.indexOf(prefix);
    if (idx == -1)
    {
      return path;
    }
    else
    {
      return path.substring(idx + prefix.length());
    }
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
            ClusterProperties clusterProperties = toClusterProperties(update.getNodeData().getData(),
                update.getNodeData().getStat().getMzxid());
            // For symlink clusters, ClusterLoadBalancerSubscriber subscribed to the symlinks, instead of the actual node, in event bus,
            // so we need to publish under the symlink names.
            // For other clusters, publish under its original name. Note that these clusters could be either:
            // 1) regular clusters requested normally.
            // 2) clusters that were pointed by a symlink previously, but no longer the case after the symlink points to other clusters.
            // For case #2: the actualResourceToSymlink map will no longer has an entry for this cluster (removed in
            // D2SymlinkNodeResourceWatcher::onChanged), thus the updates will be published under the original cluster name
            // (like "FooCluster-prod-ltx1"), which has no subscribers anyway, so no harm to publish.
            String clusterNameToPublish = _actualNodeToSymlink.getOrDefault(clusterName, clusterName);
            _clusterEventBus.publishInitialize(clusterNameToPublish, clusterProperties);
            if (_dualReadStateManager != null)
            {
              _dualReadStateManager.reportData(clusterName, clusterProperties, true);
            }
          }
          catch (InvalidProtocolBufferException | PropertySerializationException e)
          {
            _log.error("Failed to parse D2 cluster properties from xDS update. Cluster name: " + clusterName, e);
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
    return new UriPropertiesResourceWatcher(clusterName);
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

  private ServiceProperties toServiceProperties(Struct serviceProperties, long version)
      throws InvalidProtocolBufferException, PropertySerializationException
  {
    return _servicePropertiesJsonSerializer.fromBytes(
        JsonFormat.printer().print(serviceProperties).getBytes(StandardCharsets.UTF_8), version);
  }

  private ClusterProperties toClusterProperties(Struct clusterProperties, long version)
      throws InvalidProtocolBufferException, PropertySerializationException
  {
    return _clusterPropertiesJsonSerializer.fromBytes(
        JsonFormat.printer().print(clusterProperties).getBytes(StandardCharsets.UTF_8), version);
  }

  private Map<String, UriProperties> toUriProperties(Map<String, XdsD2.D2Node> uriDataMap)
      throws InvalidProtocolBufferException, PropertySerializationException
  {
    Map<String, UriProperties> parsedMap = new HashMap<>();

    for (Map.Entry<String, XdsD2.D2Node> entry : uriDataMap.entrySet())
    {
      XdsD2.D2Node d2Node = entry.getValue();
      UriProperties uriProperties = _uriPropertiesJsonSerializer.fromBytes(
          JsonFormat.printer().print(d2Node.getData()).getBytes(StandardCharsets.UTF_8), d2Node.getStat().getMzxid());
      parsedMap.put(entry.getKey(), uriProperties);
    }

    return parsedMap;
  }

  private class UriPropertiesResourceWatcher implements XdsClient.D2NodeMapResourceWatcher
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

    // For symlink clusters, UriLoadBalancerSubscriber subscribed to the symlinks, instead of the actual node, in event bus,
    // so we need to publish under the symlink names.
    // For other clusters, publish under its original name. Note that these clusters could be either:
    // 1) regular clusters requested normally.
    // 2) clusters that were pointed by a symlink previously, but no longer the case after the symlink points to other clusters.
    // For case #2: the actualResourceToSymlink map will no longer has an entry for this cluster (removed in
    // D2SymlinkNodeResourceWatcher::onChanged), thus the updates will be published under the original cluster name
    // (like "FooCluster-prod-ltx1"), which has no subscribers anyway, so no harm to publish. Yet, we still emit the tracking
    // events about receiving uri updates of this cluster for measuring update propagation latencies.
    @Override
    public void onChanged(XdsClient.D2NodeMapUpdate update)
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
          Map<String, UriProperties> updates = toUriProperties(update.getNodeDataMap());
          if (!isInit)
          {
            emitSDStatusUpdateReceiptEvents(updates);
          }
          _currentData = updates;
          UriProperties mergedUriProperties = _uriPropertiesMerger.merge(_clusterName, _currentData.values());

          String clusterNameToPublish = _actualNodeToSymlink.getOrDefault(_clusterName, _clusterName);
          _uriEventBus.publishInitialize(clusterNameToPublish, mergedUriProperties);
          if (_dualReadStateManager != null)
          {
            _dualReadStateManager.reportData(_clusterName, mergedUriProperties, true);
          }
        }
        catch (InvalidProtocolBufferException | PropertySerializationException e)
        {
          _log.error("Failed to parse D2 uri properties from xDS update. Cluster name: " + _clusterName, e);
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
        _log.info("Service discovery event emitter in XdsToD2PropertiesAdaptor is null. Skipping emitting events.");
        return;
      }

      // measure request duration and convert to milli-seconds
      long initialFetchDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - _initFetchStart);
      if (initialFetchDurationMillis < 0)
      {
        _log.warn("Failed to log ServiceDiscoveryStatusInitialRequest event, initialFetchStartAt time is greater than current time.");
        return;
      }
      // emit service discovery status initial request event for success
      _eventEmitter.emitSDStatusInitialRequestEvent(cluster, true, initialFetchDurationMillis, succeeded);

    }

    private void emitSDStatusUpdateReceiptEvents(Map<String, UriProperties> updates)
    {
      if (_eventEmitter == null)
      {
        _log.info("Service discovery event emitter in XdsToD2PropertiesAdaptor is null. Skipping emitting events.");
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
