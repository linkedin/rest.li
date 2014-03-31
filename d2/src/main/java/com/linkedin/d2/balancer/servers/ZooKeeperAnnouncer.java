/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.util.ArgumentUtil;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ZooKeeperAnnouncer combines a ZooKeeperServer with a configured "desired state", and
 * allows the server to be brought up/down in that state.  The desired state can also
 * be manipulated, for example to allow for administrative manipulation.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZooKeeperAnnouncer
{
  private final ZooKeeperServer _server;
  private static final Logger _log = LoggerFactory.getLogger(ZooKeeperAnnouncer.class);
  private String _cluster;
  private URI _uri;
  private Map<Integer, PartitionData> _partitionDataMap;
  private boolean _isServerMarkedDownThroughOverride;
  private Map<String, Object> _uriSpecificProperties;

  public ZooKeeperAnnouncer(ZooKeeperServer server)
  {
    _server = server;
  }

  public void start(Callback<None> callback)
  {
    _server.start(callback);
  }

  public void shutdown(Callback<None> callback)
  {
    _server.shutdown(callback);
  }

  public void setStore(ZooKeeperEphemeralStore<UriProperties> store)
  {
    _server.setStore(store);
  }

  public void reset(final Callback<None> callback)
  {
    markDown(new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        markUp(callback);
      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    });
  }

  public synchronized boolean isServerMarkedDownThroughOverride()
  {
    return _isServerMarkedDownThroughOverride;
  }

  public synchronized void overrideMarkUp(Callback<None> callback)
  {
    _server.markUp(_cluster, _uri, _partitionDataMap, callback);
    _isServerMarkedDownThroughOverride = false;
    _log.info("overrideMarkUp is called for uri = " + _uri );
  }

  public synchronized void overrideMarkDown(Callback<None> callback)
  {
    _server.markDown(_cluster, _uri, callback);
    _isServerMarkedDownThroughOverride = true;
    _log.info("overrideMarkDown is called for uri = " + _uri );
  }

  public synchronized void markUp(Callback<None> callback)
  {
    if (!_isServerMarkedDownThroughOverride)
    {
      _server.markUp(_cluster, _uri, _partitionDataMap, _uriSpecificProperties, callback);
    }
    else
    {
      callback.onSuccess(None.none());
      _log.warn("{} is not marked up because the server is manually marked-down through override", _uri);
    }
  }

  public synchronized void markDown(Callback<None> callback)
  {
    //it doesn't matter if the server is already marked down (through override or not) because this is commutative
    _server.markDown(_cluster, _uri, callback);
  }

  public String getCluster()
  {
    return _cluster;
  }

  public void setCluster(String cluster)
  {
    _cluster = cluster;
  }

  public String getUri()
  {
    return _uri.toString();
  }

  public void setUri(String uri)
  {
    _uri = URI.create(uri);
  }

  public void setUriSpecificProperties(Map<String, Object> uriSpecificProperties)
  {
    _uriSpecificProperties = Collections.unmodifiableMap(uriSpecificProperties);
  }

  public Map<String, Object> getUriSpecificProperties()
  {
    return (_uriSpecificProperties == null) ? Collections.<String, Object>emptyMap() : _uriSpecificProperties;
  }

  /**
   * This is not the cleanest way of setting weight or partition data. However,
   * this simplifies object create by presenting only one method and by forcing
   * users to set either weight or partition data, but not both.
   *
   * @param data could be either a double or a map of Integer to PartitionData
   */
  public void setWeightOrPartitionData(Object data)
  {
    ArgumentUtil.notNull(data, "weightOrPartitionData");
    if (data instanceof Number)
    {
      setWeight(((Number)data).doubleValue());
    }
    else
    {
      try
      {
        @SuppressWarnings("unchecked")
        Map<Integer, PartitionData> partitionDataMap = (Map<Integer, PartitionData>)data;
        setPartitionData(partitionDataMap);
      }
      catch (ClassCastException e)
      {
        throw new IllegalArgumentException(
            "data: " + data + " is not an instance of Map", e);
      }
    }
  }

  public void setWeight(double weight)
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(1);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    _partitionDataMap = Collections.unmodifiableMap(partitionDataMap);
  }

  public void setPartitionData(Map<Integer, PartitionData> partitionData)
  {
    _partitionDataMap =
        Collections.unmodifiableMap(new HashMap<Integer, PartitionData>(partitionData));
  }

  public Map<Integer, PartitionData> getPartitionData()
  {
    return _partitionDataMap;
  }
}
