package com.linkedin.d2.xds;

import indis.Diskzk;
import io.grpc.Status;
import java.util.Map;


public abstract class XdsClient
{
  private static final String D2_NODE_TYPE_URL = "type.googleapis.com/indis.D2Node";
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

  interface D2NodeMapResourceWatcher extends ResourceWatcher
  {
    void onChanged(D2NodeMapUpdate update);
  }

  interface ResourceUpdate
  {
  }


  static final class D2NodeUpdate implements ResourceUpdate
  {
    Diskzk.D2Node _nodeData;

    D2NodeUpdate(Diskzk.D2Node nodeData)
    {
      _nodeData = nodeData;
    }

    Diskzk.D2Node getNodeData()
    {
      return _nodeData;
    }
  }

  static final class D2NodeMapUpdate implements ResourceUpdate
  {
    Map<String, Diskzk.D2Node> _nodeDataMap;

    D2NodeMapUpdate(Map<String, Diskzk.D2Node> nodeDataMap)
    {
      _nodeDataMap = nodeDataMap;
    }

    public Map<String, Diskzk.D2Node> getNodeDataMap()
    {
      return _nodeDataMap;
    }
  }

  enum ResourceType
  {
    UNKNOWN, D2_NODE, D2_NODE_MAP;

    static ResourceType fromTypeUrl(String typeUrl)
    {
      if (typeUrl.equals(D2_NODE_TYPE_URL))
      {
        return D2_NODE;
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
}
