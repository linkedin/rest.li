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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import org.apache.zookeeper.KeeperException;
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
  private Map<String, Object> _uriSpecificProperties;

  private boolean _isUp;
  private final Deque<Callback<None>> _pendingMarkDown;
  private final Deque<Callback<None>> _pendingMarkUp;


  public ZooKeeperAnnouncer(ZooKeeperServer server)
  {
    _server = server;
    /* the initial state of announcer is up */
    _isUp = true;
    _pendingMarkDown = new ArrayDeque<Callback<None>>();
    _pendingMarkUp = new ArrayDeque<Callback<None>>();
  }

  /**
   * Start the announcer. Needs to be called whenever there is
   * a new zk session established.
   */
  public synchronized void start(Callback<None> callback)
  {
    if (_isUp)
    {
      markUp(callback);
    }
    // No need to manually markDown since we are getting a brand new session
  }

  /**
   * Retry last failed markUp or markDown operation if there is any. This method needs
   * to be called whenever the zookeeper connection is lost and then back again(zk session
   * is still valid).
   */
  /* package private */synchronized void retry(Callback<None> callback)
  {
    // If we have pending operations failed because of a connection loss,
    // retry the last one.
    // Note that we use _isUp to record the last requested operation, so changing
    // its value should be the first operation done in #markUp and #markDown.
    if (!_pendingMarkDown.isEmpty() || !_pendingMarkUp.isEmpty())
    {
      if (_isUp)
      {
        markUp(callback);
      }
      else
      {
        markDown(callback);
      }
    }
    // No need to retry the successful operation because the ephemeral node
    // will not go away if we were marked up.
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

  public synchronized void markUp(final Callback<None> callback)
  {
    _isUp = true;
    _server.markUp(_cluster, _uri, _partitionDataMap, _uriSpecificProperties, new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        if (e instanceof KeeperException.ConnectionLossException)
        {
          synchronized (ZooKeeperAnnouncer.this)
          {
            _pendingMarkUp.add(callback);
          }
          _log.warn("failed to mark up uri {} due to ConnectionLossException.", _uri);
        }
        else
        {
          callback.onError(e);
        }
      }

      @Override
      public void onSuccess(None result)
      {
        _log.info("markUp for uri = {} succeeded.", _uri);
        callback.onSuccess(result);
        // Note that the pending callbacks we see at this point are
        // from the requests that are filed before us because zookeeper
        // guarantees the ordering of callback being invoked.
        synchronized (ZooKeeperAnnouncer.this)
        {
          // drain _pendingMarkDown with CancellationException.
          drain(_pendingMarkDown, new CancellationException("Cancelled because a more recent markUp request succeeded."));
          // drain _pendingMarkUp with successful result.
          drain(_pendingMarkUp, null);
        }
      }
    });
    _log.info("overrideMarkUp is called for uri = " + _uri);

  }

  public synchronized void markDown(final Callback<None> callback)
  {
    _isUp = false;
    _server.markDown(_cluster, _uri, new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        if (e instanceof KeeperException.ConnectionLossException)
        {
          synchronized (ZooKeeperAnnouncer.this)
          {
            _pendingMarkDown.add(callback);
          }
          _log.warn("failed to mark down uri {} due to ConnectionLossException.", _uri);
        }
        else
        {
          callback.onError(e);
        }
      }

      @Override
      public void onSuccess(None result)
      {
        _log.info("markDown for uri = {} succeeded.", _uri);
        callback.onSuccess(result);
        // Note that the pending callbacks we see at this point are
        // from the requests that are filed before us because zookeeper
        // guarantees the ordering of callback being invoked.
        synchronized (ZooKeeperAnnouncer.this)
        {
          // drain _pendingMarkUp with CancellationException.
          drain(_pendingMarkUp, new CancellationException("Cancelled because a more recent markDown request succeeded."));
          // drain _pendingMarkDown with successful result.
          drain(_pendingMarkDown, null);
        }
      }
    });
    _log.info("overrideMarkDown is called for uri = " + _uri );
  }

  private void drain(Deque<Callback<None>> callbacks, Throwable t)
  {
    for (;!callbacks.isEmpty();)
    {
      if (t != null)
      {
        callbacks.poll().onError(t);
      }
      else
      {
        callbacks.poll().onSuccess(None.none());
      }
    }
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
