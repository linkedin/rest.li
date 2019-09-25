/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.discovery.stores.zk.builder;

import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPropertyMerger;

/**
 * Builder class for {@link ZooKeeperEphemeralStore}
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ZooKeeperEphemeralStoreBuilder<T> implements ZooKeeperStoreBuilder<ZooKeeperEphemeralStore<T>>
{
  private ZKConnection client;
  private PropertySerializer<T> serializer;
  private ZooKeeperPropertyMerger<T> merger;
  private String path;
  private boolean watchChildNodes = false;
  private boolean useNewWatcher = false;

  @Override
  public void setZkConnection(ZKConnection client)
  {
    this.client = client;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setSerializer(PropertySerializer<T> serializer)
  {
    this.serializer = serializer;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setMerger(ZooKeeperPropertyMerger<T> merger)
  {
    this.merger = merger;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setPath(String path)
  {
    this.path = path;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setWatchChildNodes(boolean watchChildNodes)
  {
    this.watchChildNodes = watchChildNodes;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setUseNewWatcher(boolean useNewWatcher)
  {
    this.useNewWatcher = useNewWatcher;
    return this;
  }

  @Override
  public ZooKeeperEphemeralStore<T> build()
  {
    return new ZooKeeperEphemeralStore<>(client, serializer, merger, path, watchChildNodes, useNewWatcher);
  }
}