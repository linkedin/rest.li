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
import java.util.Map;
import java.util.Objects;


public abstract class XdsClient
{
  private static final String NODE_TYPE_URL = "type.googleapis.com/indis.Node";
  private static final String D2_URI_MAP_TYPE_URL = "type.googleapis.com/indis.D2URIMap";

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

  interface ResourceUpdate
  {
  }


  static final class NodeUpdate implements ResourceUpdate
  {
    String _version;
    XdsD2.Node _nodeData;

    NodeUpdate(String version, XdsD2.Node nodeData)
    {
      _version = version;
      _nodeData = nodeData;
    }

    XdsD2.Node getNodeData()
    {
      return _nodeData;
    }

    public String getVersion()
    {
      return _version;
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
      return Objects.equals(_version, that._version) && Objects.equals(_nodeData, that._nodeData);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_version, _nodeData);
    }
  }

  static final class D2URIMapUpdate implements ResourceUpdate
  {
    String _version;
    Map<String, XdsD2.D2URI> _uriMap;

    D2URIMapUpdate(String version, Map<String, XdsD2.D2URI> uriMap)
    {
      _version = version;
      _uriMap = uriMap;
    }

    public Map<String, XdsD2.D2URI> getURIMap()
    {
      return _uriMap;
    }

    public String getVersion()
    {
      return _version;
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
      return Objects.equals(_version, that._version) && Objects.equals(_uriMap, that._uriMap);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_version, _uriMap);
    }
  }

  enum ResourceType
  {
    UNKNOWN, NODE, D2_URI_MAP;

    static ResourceType fromTypeUrl(String typeUrl)
    {
      if (typeUrl.equals(NODE_TYPE_URL))
      {
        return NODE;
      }
      if (typeUrl.equals(D2_URI_MAP_TYPE_URL))
      {
        return D2_URI_MAP;
      }
      return UNKNOWN;
    }

    String typeUrl()
    {
      switch (this)
      {
        case NODE:
          return NODE_TYPE_URL;
        case D2_URI_MAP:
          return D2_URI_MAP_TYPE_URL;
        case UNKNOWN:
        default:
          throw new AssertionError("Unknown or missing case in enum switch: " + this);
      }
    }
  }

  abstract void watchXdsResource(String resourceName, ResourceType type, ResourceWatcher watcher);

  abstract void startRpcStream();

  abstract void shutdown();

  abstract String getXdsServerAuthority();

  abstract public XdsClientJmx getXdsClientJmx();
}
