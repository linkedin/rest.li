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

import indis.XdsD2;
import io.grpc.Status;
import java.util.Map;


public abstract class XdsClient
{
  private static final String D2_NODE_TYPE_URL = "type.googleapis.com/indis.D2Node";
  private static final String D2_SYMLINK_NODE_TYPE_URL = "type.googleapis.com/indis.D2SymlinkNode";
  private static final String D2_NODE_MAP_TYPE_URL = "type.googleapis.com/indis.D2NodeMap";

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

  interface D2NodeResourceWatcher extends ResourceWatcher
  {
    void onChanged(D2NodeUpdate update);
  }

  interface D2SymlinkNodeResourceWatcher extends ResourceWatcher
  {
    void onChanged(String resourceName, D2SymlinkNodeUpdate update);
  }

  interface D2NodeMapResourceWatcher extends ResourceWatcher
  {
    void onChanged(D2NodeMapUpdate update);
  }

  interface ResourceUpdate
  {
  }


  static final class D2NodeUpdate implements ResourceUpdate
  {
    String _version;
    XdsD2.D2Node _nodeData;

    D2NodeUpdate(String version, XdsD2.D2Node nodeData)
    {
      _version = version;
      _nodeData = nodeData;
    }

    XdsD2.D2Node getNodeData()
    {
      return _nodeData;
    }

    public String getVersion()
    {
      return _version;
    }
  }

  static final class D2SymlinkNodeUpdate implements ResourceUpdate
  {
    String _version;
    XdsD2.D2SymlinkNode _nodeData;

    D2SymlinkNodeUpdate(String version, XdsD2.D2SymlinkNode nodeData)
    {
      _version = version;
      _nodeData = nodeData;
    }

    XdsD2.D2SymlinkNode getNodeData()
    {
      return _nodeData;
    }

    public String getVersion()
    {
      return _version;
    }
  }

  static final class D2NodeMapUpdate implements ResourceUpdate
  {
    String _version;
    Map<String, XdsD2.D2Node> _nodeDataMap;

    D2NodeMapUpdate(String version, Map<String, XdsD2.D2Node> nodeDataMap)
    {
      _version = version;
      _nodeDataMap = nodeDataMap;
    }

    public Map<String, XdsD2.D2Node> getNodeDataMap()
    {
      return _nodeDataMap;
    }

    public String getVersion()
    {
      return _version;
    }
  }

  enum ResourceType
  {
    // TODO: add D2_SYMLINK_NODE type
    UNKNOWN, D2_NODE, D2_SYMLINK_NODE, D2_NODE_MAP;

    static ResourceType fromTypeUrl(String typeUrl)
    {
      if (typeUrl.equals(D2_NODE_TYPE_URL))
      {
        return D2_NODE;
      }
      if (typeUrl.equals(D2_SYMLINK_NODE_TYPE_URL))
      {
        return D2_SYMLINK_NODE;
      }
      if (typeUrl.equals(D2_NODE_MAP_TYPE_URL))
      {
        return D2_NODE_MAP;
      }
      return UNKNOWN;
    }

    String typeUrl()
    {
      switch (this)
      {
        case D2_NODE:
          return D2_NODE_TYPE_URL;
        case D2_SYMLINK_NODE:
          return D2_SYMLINK_NODE_TYPE_URL;
        case D2_NODE_MAP:
          return D2_NODE_MAP_TYPE_URL;
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
}
