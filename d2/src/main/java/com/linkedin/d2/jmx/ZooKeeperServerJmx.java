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

package com.linkedin.d2.jmx;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.servers.ZooKeeperServer;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.stores.PropertyStoreException;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ZooKeeperServerJmx implements ZooKeeperServerJmxMXBean
{
  private final ZooKeeperServer _server;

  public ZooKeeperServerJmx(ZooKeeperServer server)
  {
    _server = server;
  }

  @Override
  public void setMarkDown(String clusterName, String uri) throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    _server.markDown(clusterName, URI.create(uri), callback);
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new PropertyStoreException(e);
    }
  }

  @Override
  public void setMarkUp(String clusterName, String uri, double weight) throws PropertyStoreException
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(1);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    setMarkup(clusterName, uri, partitionDataMap, Collections.<String, Object>emptyMap());
  }

  @Override
  public void setMarkup(String clusterName, String uri, Map<Integer, PartitionData> partitionDataMap) throws PropertyStoreException
  {
    setMarkup(clusterName, uri, partitionDataMap, Collections.<String, Object>emptyMap());
  }

  @Override
  public void setMarkup(String clusterName,
                        String uri,
                        Map<Integer, PartitionData> partitionDataMap,
                        Map<String, Object> uriSpecificProperties)
      throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    _server.markUp(clusterName, URI.create(uri), partitionDataMap, uriSpecificProperties, callback);
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new PropertyStoreException(e);
    }
  }
}
