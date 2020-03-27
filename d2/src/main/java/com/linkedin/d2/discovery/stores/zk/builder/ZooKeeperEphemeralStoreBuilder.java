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
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.annotation.Nullable;


/**
 * Builder class for {@link ZooKeeperEphemeralStore}
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ZooKeeperEphemeralStoreBuilder<T> implements ZooKeeperStoreBuilder<ZooKeeperEphemeralStore<T>>
{
  private static final String URIS_VALUES_DIRECTORY = "urisValues";
  private ZKConnection _client;
  private PropertySerializer<T> _serializer;
  private ZooKeeperPropertyMerger<T> _merger;
  private String _path;
  private boolean _watchChildNodes = false;
  private boolean _useNewWatcher = false;
  private String _fsD2DirPathForBackup = null;
  private ScheduledExecutorService executorService;
  private int readWindowMs = ZooKeeperStore.DEFAULT_READ_WINDOW_MS;
  private List<Consumer<ZooKeeperEphemeralStore<T>>> _onBuildListeners = new ArrayList<>();

  @Override
  public void setZkConnection(ZKConnection client)
  {
    _client = client;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setSerializer(PropertySerializer<T> serializer)
  {
    _serializer = serializer;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setMerger(ZooKeeperPropertyMerger<T> merger)
  {
    _merger = merger;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setPath(String path)
  {
    _path = path;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setWatchChildNodes(boolean watchChildNodes)
  {
    _watchChildNodes = watchChildNodes;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setUseNewWatcher(boolean useNewWatcher)
  {
    _useNewWatcher = useNewWatcher;
    return this;
  }

  /**
   * Set null to disable
   */
  public ZooKeeperEphemeralStoreBuilder<T> setBackupStoreFilePath(@Nullable String fsd2DirPathForBackup)
  {
    _fsD2DirPathForBackup = fsd2DirPathForBackup;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setExecutorService(ScheduledExecutorService executorService)
  {
    this.executorService = executorService;
    return this;
  }

  public ZooKeeperEphemeralStoreBuilder<T> setReadWindowMs(int readWindowMs)
  {
    this.readWindowMs = readWindowMs;
    return this;
  }

  @Override
  public ZooKeeperEphemeralStoreBuilder<T> addOnBuildListener(Consumer<ZooKeeperEphemeralStore<T>> onBuildListener)
  {
    _onBuildListeners.add(onBuildListener);
    return this;
  }

  @Override
  public ZooKeeperEphemeralStore<T> build()
  {
    String backupStoreFilePath = null;
    if (_fsD2DirPathForBackup != null) {
      backupStoreFilePath = _fsD2DirPathForBackup + File.separator + URIS_VALUES_DIRECTORY;
    }

    ZooKeeperEphemeralStore<T> zooKeeperEphemeralStore =
      new ZooKeeperEphemeralStore<>(_client, _serializer, _merger, _path, _watchChildNodes, _useNewWatcher,
                                    backupStoreFilePath, executorService, readWindowMs);

    for (Consumer<ZooKeeperEphemeralStore<T>> onBuildListener : _onBuildListeners)
    {
      onBuildListener.accept(zooKeeperEphemeralStore);
    }

    return zooKeeperEphemeralStore;
  }
}