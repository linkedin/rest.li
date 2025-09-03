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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.linkedin.d2.jmx.XdsClientJmx;
import indis.XdsD2;
import io.grpc.Status;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;


public abstract class XdsClient
{
  public abstract static class ResourceWatcher
  {
    private final ResourceType _type;

    /**
     * Defining a private constructor means only classes that are defined in this file can extend this class. This way,
     * it can be defined at compile-time that there can only be two implementations: {@link NodeResourceWatcher} and
     * {@link D2URIMapResourceWatcher}, and the remainder of the code can be greatly simplified.
     */
    private ResourceWatcher(ResourceType type)
    {
      _type = type;
    }

    final ResourceType getType()
    {
      return _type;
    }

    /**
     * Called when the resource discovery RPC encounters some transient error.
     */
    public abstract void onError(Status error);

    /**
     * Called when the resource discovery RPC reestablishes connection.
     */
    public abstract void onReconnect();

    abstract void onChanged(ResourceUpdate update);
  }

  public static abstract class NodeResourceWatcher extends ResourceWatcher
  {
    public NodeResourceWatcher()
    {
      super(ResourceType.NODE);
    }

    public abstract void onChanged(NodeUpdate update);

    @Override
    final void onChanged(ResourceUpdate update)
    {
      onChanged((NodeUpdate) update);
    }
  }

  public static abstract class D2URIMapResourceWatcher extends ResourceWatcher
  {
    public D2URIMapResourceWatcher()
    {
      super(ResourceType.D2_URI_MAP);
    }

    public abstract void onChanged(D2URIMapUpdate update);

    @Override
    final void onChanged(ResourceUpdate update)
    {
      onChanged((D2URIMapUpdate) update);
    }
  }

  public static abstract class D2UriResourceWatcher extends ResourceWatcher
  {
    public D2UriResourceWatcher()
    {
      super(ResourceType.D2_URI);
    }

    public abstract void onChanged(D2URIUpdate update);

    @Override
    final void onChanged(ResourceUpdate update)
    {
      onChanged((D2URIUpdate) update);
    }
  }

  public static abstract class WildcardResourceWatcher
  {
    private final ResourceType _type;

    /**
     * Defining a private constructor means only classes that are defined in this file can extend this class (see
     * {@link ResourceWatcher}).
     */
    WildcardResourceWatcher(ResourceType type)
    {
      _type = type;
    }

    final ResourceType getType()
    {
      return _type;
    }

    /**
     * Called when the resource discovery RPC encounters some transient error.
     */
    public abstract void onError(Status error);

    /**
     * Called when the resource discovery RPC reestablishes connection.
     */
    public abstract void onReconnect();

    /**
     * Called when a resource is added or updated.
     * @param resourceName the name of the resource that was added or updated.
     * @param update       the new data {@link ResourceUpdate} for the resource.
     */
    abstract void onChanged(String resourceName, ResourceUpdate update);

    /**
     * Called when a resource is removed.
     * @param resourceName the name of the resource that was removed.
     */
    public abstract void onRemoval(String resourceName);

    /**
     * Just a signal to notify that all resources (including both changed and removed ones) in all response chunks (if
     * any) have been processed.
     * Default implementation does nothing.
     */
    public void onAllResourcesProcessed()
    {
      // do nothing
    }
  }

  public static abstract class WildcardNodeResourceWatcher extends WildcardResourceWatcher
  {
    public WildcardNodeResourceWatcher()
    {
      super(ResourceType.NODE);
    }

    /**
     * Called when a node resource is added or updated.
     * @param resourceName the resource name of the {@link NodeUpdate} that was added or updated.
     * @param update       the new data for the {@link NodeUpdate}, including D2 cluster and service information.
     */
    public abstract void onChanged(String resourceName, NodeUpdate update);

    @Override
    final void onChanged(String resourceName, ResourceUpdate update)
    {
      onChanged(resourceName, (NodeUpdate) update);
    }
  }

  public static abstract class WildcardD2URIMapResourceWatcher extends WildcardResourceWatcher
  {
    public WildcardD2URIMapResourceWatcher()
    {
      super(ResourceType.D2_URI_MAP);
    }

    /**
     * Called when a {@link D2URIMapUpdate} resource is added or updated.
     * @param resourceName the resource name of the {@link D2URIMapUpdate} map resource that was added or updated.
     *                     like the /d2/uris/clusterName
     * @param update       the new data for the {@link D2URIMapUpdate} resource
     */
    public abstract void onChanged(String resourceName, D2URIMapUpdate update);

    @Override
    final void onChanged(String resourceName, ResourceUpdate update)
    {
      onChanged(resourceName, (D2URIMapUpdate) update);
    }
  }

  public static abstract class WildcardD2ClusterOrServiceNameResourceWatcher extends WildcardResourceWatcher
  {
    public WildcardD2ClusterOrServiceNameResourceWatcher()
    {
      super(ResourceType.D2_CLUSTER_OR_SERVICE_NAME);
    }

    /**
     * Called when a D2ClusterOrServiceName resource is added or updated.
     * @param resourceName the resource name of the D2ClusterOrServiceName that was added or updated.
     * @param update       the new data for the D2ClusterOrServiceName resource
     */
    public abstract void onChanged(String resourceName, D2ClusterOrServiceNameUpdate update);

    @Override
    final void onChanged(String resourceName, ResourceUpdate update)
    {
      onChanged(resourceName, (D2ClusterOrServiceNameUpdate) update);
    }
  }

  public interface ResourceUpdate
  {
    boolean isValid();
  }

  public static final class NodeUpdate implements ResourceUpdate
  {
    XdsD2.Node _nodeData;

    NodeUpdate(XdsD2.Node nodeData)
    {
      _nodeData = nodeData;
    }

    public XdsD2.Node getNodeData()
    {
      return _nodeData;
    }

    @Override
    public boolean equals(Object object)
    {
      if (this == object)
      {
        return true;
      }
      if (object == null || getClass() != object.getClass())
      {
        return false;
      }
      NodeUpdate that = (NodeUpdate) object;
      return Objects.equals(_nodeData, that._nodeData);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_nodeData);
    }

    @Override
    public boolean isValid()
    {
      return _nodeData != null && !_nodeData.getData().isEmpty();
    }

    @Override
    public String toString()
    {
      return MoreObjects.toStringHelper(this).add("_nodeData", _nodeData).toString();
    }
  }

  public static final class D2ClusterOrServiceNameUpdate implements ResourceUpdate
  {
    XdsD2.D2ClusterOrServiceName _nameData;

    D2ClusterOrServiceNameUpdate(XdsD2.D2ClusterOrServiceName nameData)
    {
      _nameData = nameData;
    }

    public XdsD2.D2ClusterOrServiceName getNameData()
    {
      return _nameData;
    }

    @Override
    public boolean equals(Object object)
    {
      if (this == object)
      {
        return true;
      }
      if (object == null || getClass() != object.getClass())
      {
        return false;
      }
      D2ClusterOrServiceNameUpdate that = (D2ClusterOrServiceNameUpdate) object;
      return Objects.equals(_nameData, that._nameData);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_nameData);
    }

    @Override
    public boolean isValid()
    {
      return _nameData != null
          && (!Strings.isNullOrEmpty(_nameData.getClusterName()) || !Strings.isNullOrEmpty(_nameData.getServiceName()));
    }

    @Override
    public String toString()
    {
      return MoreObjects.toStringHelper(this).add("_nameData", _nameData).toString();
    }
  }

  public static final class D2URIMapUpdate implements ResourceUpdate
  {
    Map<String, XdsD2.D2URI> _uriMap;
    private final boolean _globCollectionEnabled;
    private final Set<String> _updatedUrisName = new HashSet<>();
    private final Set<String> _removedUrisName = new HashSet<>();
    private final Map<String, Boolean> _isStaleModifiedTimes = new HashMap<>();

    D2URIMapUpdate(Map<String, XdsD2.D2URI> uriMap)
    {
      this(uriMap, false);
    }

    D2URIMapUpdate(Map<String, XdsD2.D2URI> uriMap, boolean globCollectionEnabled)
    {
      _uriMap = uriMap;
      _globCollectionEnabled = globCollectionEnabled;
    }

    public Map<String, XdsD2.D2URI> getURIMap()
    {
      return _uriMap;
    }

    /**
     * Returns whether the glob collection is enabled.
     *
     * @return {@code true} if glob collection is enabled, {@code false} otherwise
     */
    public boolean isGlobCollectionEnabled()
    {
      return _globCollectionEnabled;
    }

    /**
     * Returns the names of the updated URIs.
     * The updated URIs are valid only if {@link #isGlobCollectionEnabled} is {@code true}.
     * Otherwise, when {@link #isGlobCollectionEnabled} is {@code false}, the updated URIs are not tracked and should be ignored.
     *
     * @return a set of updated URI names
     */
    public Set<String> getUpdatedUrisName()
    {

      return _updatedUrisName;
    }


    /**
     * Returns the names of the removed URIs.
     * The removed URIs are valid only if {@link #isGlobCollectionEnabled} is {@code true}.
     * Otherwise, when {@link #isGlobCollectionEnabled} is {@code false}, the removed URIs are not tracked and should be ignored.
     *
     * @return a set of removed URI names
     */
    public Set<String> getRemovedUrisName()
    {
      return _removedUrisName;
    }

    D2URIMapUpdate putUri(String name, XdsD2.D2URI uri)
    {
      if (_uriMap == null)
      {
        _uriMap = new HashMap<>();
      }
      _uriMap.put(name, uri);
      _updatedUrisName.add(name);
      _removedUrisName.remove(name);
      return this;
    }

    D2URIMapUpdate removeUri(String name)
    {
      if (_uriMap != null)
      {
        _uriMap.remove(name);
      }
      _removedUrisName.add(name);
      _updatedUrisName.remove(name);
      return this;
    }

    public boolean isStaleModifiedTime(String name)
    {
      return _isStaleModifiedTimes.getOrDefault(name, false);
    }

    public void setIsStaleModifiedTime(String name, boolean isStaleModifiedTime)
    {
      _isStaleModifiedTimes.put(name, isStaleModifiedTime);
    }

    @Override
    public boolean equals(Object object)
    {
      if (this == object)
      {
        return true;
      }
      if (object == null || getClass() != object.getClass())
      {
        return false;
      }
      D2URIMapUpdate that = (D2URIMapUpdate) object;
      return Objects.equals(_uriMap, that._uriMap);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_uriMap);
    }

    @Override
    public boolean isValid()
    {
      return _uriMap != null;
    }

    @Override
    public String toString()
    {
      return MoreObjects.toStringHelper(this).add("_uriMap", _uriMap).toString();
    }
  }

  public static final class D2URIUpdate implements ResourceUpdate
  {
    private final XdsD2.D2URI _d2Uri;
    private boolean _isStaleModifiedTime = false;

    D2URIUpdate(XdsD2.D2URI d2Uri)
    {
      _d2Uri = d2Uri;
    }

    /**
     * Returns the {@link XdsD2.D2URI} that was received, or {@code null} if the URI was deleted.
     */
    @Nullable
    public XdsD2.D2URI getD2Uri()
    {
      return _d2Uri;
    }

    @Override
    public boolean isValid()
    {
      // For this update type, the subscriber needs to be notified of deletions, so all D2URIUpdates are valid.
      return true;
    }

    public boolean isStaleModifiedTime()
    {
      return _isStaleModifiedTime;
    }

    public void setIsStaleModifiedTime(boolean isStaleModifiedTime)
    {
      _isStaleModifiedTime = isStaleModifiedTime;
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
      {
        return true;
      }
      if (o == null || getClass() != o.getClass())
      {
        return false;
      }
      D2URIUpdate that = (D2URIUpdate) o;
      return Objects.equals(_d2Uri, that._d2Uri);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_d2Uri);
    }

    @Override
    public String toString()
    {
      return MoreObjects.toStringHelper(this).add("_d2Uri", _d2Uri).toString();
    }
  }

  public static final NodeUpdate EMPTY_NODE_UPDATE = new NodeUpdate(null);
  public static final D2URIMapUpdate EMPTY_D2_URI_MAP_UPDATE = new D2URIMapUpdate(null);
  public static final D2ClusterOrServiceNameUpdate EMPTY_D2_CLUSTER_OR_SERVICE_NAME_UPDATE =
      new D2ClusterOrServiceNameUpdate(null);

  enum ResourceType
  {
    NODE("type.googleapis.com/indis.Node", EMPTY_NODE_UPDATE),
    D2_URI_MAP("type.googleapis.com/indis.D2URIMap", EMPTY_D2_URI_MAP_UPDATE),
    D2_URI("type.googleapis.com/indis.D2URI", EMPTY_D2_URI_MAP_UPDATE),
    D2_CLUSTER_OR_SERVICE_NAME("type.googleapis.com/indis.D2ClusterOrServiceName",
        EMPTY_D2_CLUSTER_OR_SERVICE_NAME_UPDATE);

    private static final Map<String, ResourceType> TYPE_URL_TO_ENUM = Arrays.stream(values())
        .filter(e -> e.typeUrl() != null)
        .collect(Collectors.toMap(ResourceType::typeUrl, Function.identity()));


    private final String _typeUrl;
    private final ResourceUpdate _emptyData;

    ResourceType(String typeUrl, ResourceUpdate emptyData)
    {
      _typeUrl = typeUrl;
      _emptyData = emptyData;
    }

    String typeUrl()
    {
      return _typeUrl;
    }

    ResourceUpdate emptyData()
    {
      return _emptyData;
    }

    @Nullable
    static ResourceType fromTypeUrl(String typeUrl)
    {
      return TYPE_URL_TO_ENUM.get(typeUrl);
    }
  }

  public abstract void start();

  /**
   * Subscribes the given {@link ResourceWatcher} to the resource of the given name. The watcher will be notified when
   * the resource is received from the backend. Repeated calls to this function with the same resource name and watcher
   * will always notify the given watcher of the current data if it is already present, even if the given watcher was
   * already subscribed to said resource. However, the subscription will only be added once.
   */
  public abstract void watchXdsResource(String resourceName, ResourceWatcher watcher);

  /**
   * Subscribes the given {@link WildcardResourceWatcher} to all the resources of the corresponding type. The watcher
   * will be notified whenever a resource is added or removed. Repeated calls to this function with the same watcher
   * will always notify the given watcher of the current data.
   */
  public abstract void watchAllXdsResources(WildcardResourceWatcher watcher);

  /**
   * Shuts down the xDS client.
   */
  public abstract void shutdown();

  /**
   * Returns the authority of the xDS server.
   */
  public abstract String getXdsServerAuthority();

  /**
   * Returns the JMX bean for the xDS client.
   */
  public abstract XdsClientJmx getXdsClientJmx();
}
