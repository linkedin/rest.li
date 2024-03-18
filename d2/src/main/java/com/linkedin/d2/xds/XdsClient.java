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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;


public abstract class XdsClient
{
  interface ResourceWatcher
  {
    /**
     * Called when the resource discovery RPC encounters some transient error.
     */
    void onError(Status error);

    /**
     * Called when the resource discovery RPC reestablishes connection.
     */
    void onReconnect();
  }

  interface NodeResourceWatcher extends ResourceWatcher
  {
    void onChanged(NodeUpdate update);

  }

  interface SymlinkNodeResourceWatcher extends ResourceWatcher
  {
    void onChanged(String resourceName, NodeUpdate update);

  }

  interface D2URIMapResourceWatcher extends ResourceWatcher
  {
    void onChanged(D2URIMapUpdate update);

  }

  interface D2URICollectionResourceWatcher extends ResourceWatcher
  {
    void onChanged(D2URICollectionUpdate update);
  }

  interface ResourceUpdate
  {
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
  }

  static final class D2URICollectionUpdate implements ResourceUpdate
  {
    private final Map<String, XdsD2.D2URI> _uris = new HashMap<>();
    private final List<String> _removedUris = new ArrayList<>();

    public D2URICollectionUpdate addUri(String name, XdsD2.D2URI uri)
    {
      _uris.put(name, uri);
      return this;
    }

    public Map<String, XdsD2.D2URI> getUris()
    {
      return _uris;
    }

    public D2URICollectionUpdate addRemovedUri(String uriName)
    {
      _removedUris.add(uriName);
      return this;
    }

    public List<String> getRemovedUris()
    {
      return _removedUris;
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
      D2URICollectionUpdate that = (D2URICollectionUpdate) o;
      return Objects.equals(_uris, that._uris) && Objects.equals(_removedUris, that._removedUris);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_uris, _removedUris);
    }
  }

  enum ResourceType
  {
    NODE("type.googleapis.com/indis.Node"),
    D2_URI_MAP("type.googleapis.com/indis.D2URIMap"),
    D2_URI("type.googleapis.com/indis.D2URI");

    private static final Map<String, ResourceType> TYPE_URL_TO_ENUM = Arrays.stream(values())
        .filter(e -> e.typeUrl() != null)
        .collect(Collectors.toMap(ResourceType::typeUrl, Function.identity()));

    private final String _typeUrl;

    ResourceType(String typeUrl)
    {
      _typeUrl = typeUrl;
    }

    String typeUrl()
    {
      return _typeUrl;
    }

    @Nullable
    static ResourceType fromTypeUrl(String typeUrl)
    {
      return TYPE_URL_TO_ENUM.get(typeUrl);
    }
  }

  abstract void watchXdsResource(String resourceName, ResourceType type, ResourceWatcher watcher);

  abstract void startRpcStream();

  abstract void shutdown();

  abstract String getXdsServerAuthority();

  abstract public XdsClientJmx getXdsClientJmx();
}
