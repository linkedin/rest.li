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

package com.linkedin.d2.balancer.servers;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerServer;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

public class
        ZooKeeperServer implements LoadBalancerServer
{
  private static final Logger _log = LoggerFactory.getLogger(ZooKeeperServer.class);

  private volatile ZooKeeperEphemeralStore<UriProperties> _store;

  public ZooKeeperServer()
  {
  }

  public ZooKeeperServer(ZooKeeperEphemeralStore<UriProperties> store)
  {
    _store = store;
  }

  @Override
  public void start(Callback<None> callback)
  {
    _store.start(callback);
  }

  @Override
  public void shutdown(final Callback<None> callback)
  {
    _store.shutdown(callback);
  }

  @Override
  public void markUp(final String clusterName,
                     final URI uri,
                     final Map<Integer, PartitionData> partitionDataMap,
                     final Callback<None> callback)
  {
    markUp(clusterName, uri, partitionDataMap, Collections.<String, Object>emptyMap(), callback);
  }

  @Override
  public void markUp(final String clusterName,
                     final URI uri,
                     final Map<Integer, PartitionData> partitionDataMap,
                     final Map<String, Object> uriSpecificProperties,
                     final Callback<None> callback)
  {
    final Callback<None> doPutCallback = new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        Map<URI, Map<Integer, PartitionData>> partitionDesc = new HashMap<>();
        partitionDesc.put(uri, partitionDataMap);

        Map<URI, Map<String, Object>> myUriSpecificProperties;
        if (uriSpecificProperties != null && !uriSpecificProperties.isEmpty())
        {
          myUriSpecificProperties = new HashMap<>();
          myUriSpecificProperties.put(uri, uriSpecificProperties);
        }
        else
        {
          myUriSpecificProperties = Collections.emptyMap();
        }

        if (_log.isInfoEnabled())
        {
          StringBuilder sb = new StringBuilder();
          sb.append(_store);
          sb.append(" marked up for cluster: ");
          sb.append(clusterName);
          sb.append(", uri: ");
          sb.append(uri);
          sb.append(", announcing [partitionId: weight]s: {");
          for (final int partitionId : partitionDataMap.keySet())
          {
            sb.append("[");
            sb.append(partitionId);
            sb.append(" : ");
            sb.append(partitionDataMap.get(partitionId));
            sb.append("]");
          }
          sb.append("}");
          info(_log, sb);
        }
        _store.put(clusterName, new UriProperties(clusterName, partitionDesc, myUriSpecificProperties), callback);

      }

      @Override
      public void onError(Throwable e)
      {
        // if the node has already been deleted, we don't care and we can just put the new one
        if (e instanceof KeeperException.NoNodeException)
        {
          onSuccess(None.none());
          return;
        }
        info(_log, _store + " failed to mark up for cluster: " + clusterName + ", uri: " + uri);
        callback.onError(e);
      }
    };

    Callback<UriProperties> getCallback = new Callback<UriProperties>()
    {
      @Override
      public void onSuccess(UriProperties uris)
      {
        if (uris != null && uris.Uris().contains(uri))
        {
          warn(_log,
               "markUp called on a uri that already exists in cluster ",
               clusterName,
               ": ",
               uri);

          // mark down before marking up with the new weight
          markDown(clusterName, uri, doPutCallback);
        }
        else
        {
          doPutCallback.onSuccess(None.none());
        }
      }

      @Override
      public void onError(Throwable e)
      {
        info(_log, _store + " failed to get current status on ZK for cluster: " + clusterName + ", uri: " + uri);
        callback.onError(e);
      }
    };

    storeGet(clusterName, getCallback);
  }

  @Override
  public void markDown(final String clusterName, final URI uri, final Callback<None> callback)
  {
    Callback<UriProperties> getCallback = new Callback<UriProperties>()
    {
      @Override
      public void onSuccess(UriProperties uris)
      {
        if (uris == null)
        {
          warn(_log, "markDown called on a cluster that doesn't exist in zk: ", clusterName);
          callback.onSuccess(None.none());
        }
        else if (!uris.Uris().contains(uri))
        {
          warn(_log,
               "markDown called on a uri that doesn't exist in cluster ",
               clusterName,
               ": ",
               uri);
          callback.onSuccess(None.none());
        }
        else
        {
          warn(_log, _store, " marked down for cluster ", clusterName, "with uri: ", uri);
          Map<URI, Map<Integer, PartitionData>> partitionData = new HashMap<>(2);
          partitionData.put(uri, Collections.emptyMap());
          _store.removePartial(clusterName, new UriProperties(clusterName, partitionData), callback);
        }

      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    };

    storeGet(clusterName, getCallback);
  }

  /**
   * 1. Gets existing {@link UriProperties} for given cluster and add doNotSlowStart property
   * for given uri.
   * 2. Mark down existing node.
   * 3. Mark up new node for uri with modified UriProperties and given partitionDataMap.
   *
   * @param doNotSlowStart Flag to let clients know if slow start should be avoided for a host.
   */
  @Override
  public void changeWeight(String clusterName,
                           URI uri,
                           Map<Integer, PartitionData> partitionDataMap,
                           boolean doNotSlowStart,
                           Callback<None> callback)
  {
    addUriSpecificProperty(clusterName,
                           "changeWeight",
                           uri,
                           partitionDataMap,
                           PropertyKeys.DO_NOT_SLOW_START,
                           doNotSlowStart,
                           callback);
  }

  /**
   * 1. Gets existing {@link UriProperties} for given cluster and add/remove property
   * for given uri.
   * 2. Mark down existing node.
   * 3. Mark up new node for uri with modified UriProperties.
   */
  @Override
  public void addUriSpecificProperty(String clusterName,
                                     String operationName,
                                     URI uri,
                                     Map<Integer, PartitionData> partitionDataMap,
                                     String uriSpecificPropertiesName,
                                     Object uriSpecificPropertiesValue,
                                     Callback<None> callback)
  {
    Callback<UriProperties> getCallback = new Callback<UriProperties>()
    {
      @Override
      public void onSuccess(UriProperties uriProperties)
      {
        if (uriProperties == null)
        {
          warn(_log,
               operationName,
               " called on a cluster that doesn't exist in zookeeper: ",
               clusterName);
          callback.onError(new ServiceUnavailableException("cluster: " + clusterName, "Cluster does not exist in zookeeper."));
        }
        else if (!uriProperties.Uris().contains(uri))
        {
          warn(_log,
               operationName,
               " called on a uri that doesn't exist in cluster ",
               clusterName,
               ": ",
               uri);
          callback.onError(new ServiceUnavailableException(String.format("cluster: %s, uri: %s", clusterName, uri), "Uri does not exist in cluster."));
        }
        else
        {
          Map<String, Object> uriSpecificProperties = uriProperties.getUriSpecificProperties().getOrDefault(uri, new HashMap<>());
          uriSpecificProperties.put(uriSpecificPropertiesName, uriSpecificPropertiesValue);

          Callback<None> markUpCallback = new Callback<None>()
          {
            @Override
            public void onError(Throwable e)
            {
              callback.onError(e);
            }

            @Override
            public void onSuccess(None result)
            {
              markUp(clusterName, uri, partitionDataMap, uriSpecificProperties, callback);
            }
          };
          markDown(clusterName, uri, markUpCallback);
        }
      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    };

    storeGet(clusterName, getCallback);
  }

  public void setStore(ZooKeeperEphemeralStore<UriProperties> store)
  {
    _store = store;

    info(_log, "store set to new store: ", _store);
  }

  public void shutdown()
  {
    info(_log, "shutting down zk server");

    final FutureCallback<None> callback = new FutureCallback<>();
    _store.shutdown(callback);
    try
    {
      callback.get(5, TimeUnit.SECONDS);
      info(_log, "shutting down complete");
    }
    catch (TimeoutException e)
    {
      warn(_log, "unable to shut down propertly");
    }
    catch (InterruptedException | ExecutionException e)
    {
      warn(_log, "unable to shut down propertly.. got interrupt exception while waiting");
    }
  }

  private void storeGet(final String clusterName, final Callback<UriProperties> callback)
  {
    if (_store == null)
    {
      callback.onError(new Throwable("ZK connection not ready yet"));
    }
    else
    {
      _store.get(clusterName, callback);
    }
  }

}
