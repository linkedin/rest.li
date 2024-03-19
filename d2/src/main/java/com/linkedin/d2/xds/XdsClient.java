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

import com.linkedin.d2.jmx.XdsClientJmx;
import indis.XdsD2;
import io.grpc.Status;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;


public abstract class XdsClient
{
  public static abstract class ResourceWatcher
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

  interface ResourceUpdate
  {
    boolean isEmpty();
  }

  static final class NodeUpdate implements ResourceUpdate
  {
    XdsD2.Node _nodeData;

    NodeUpdate(XdsD2.Node nodeData)
    {
      _nodeData = nodeData;
    }

    XdsD2.Node getNodeData()
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
    public boolean isEmpty()
    {
      return _nodeData == null || _nodeData.getData().isEmpty();
    }
  }

  static final class D2URIMapUpdate implements ResourceUpdate
  {
    Map<String, XdsD2.D2URI> _uriMap;

    D2URIMapUpdate(Map<String, XdsD2.D2URI> uriMap)
    {
      _uriMap = uriMap;
    }

    public Map<String, XdsD2.D2URI> getURIMap()
    {
      return _uriMap;
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
    public boolean isEmpty()
    {
      return _uriMap == null || _uriMap.isEmpty();
    }
  }

  enum ResourceType
  {
    NODE("type.googleapis.com/indis.Node", new NodeUpdate(null)),
    D2_URI_MAP("type.googleapis.com/indis.D2URIMap", new D2URIMapUpdate(null)),
    D2_URI("type.googleapis.com/indis.D2URI", new D2URIMapUpdate(null));

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

  abstract void watchXdsResource(String resourceName, ResourceWatcher watcher);

  abstract void startRpcStream();

  abstract void shutdown();

  abstract String getXdsServerAuthority();

  abstract public XdsClientJmx getXdsClientJmx();
}
