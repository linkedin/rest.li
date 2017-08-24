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

package com.linkedin.d2.discovery.stores.zk;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.PropertyStringMerger;
import com.linkedin.d2.discovery.stores.PropertyStringSerializer;
import com.linkedin.d2.discovery.stores.zk.builder.ZooKeeperEphemeralStoreBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public final class ZkStoreTestOnlyUtil
{
  private static final Map<Integer, List<ZKPersistentConnection>> _zkPersistentConnection = new HashMap<>();

  private static final String ZK_PATH = "/testPath";

  public static ZKConnection getZKConnection(int port) throws IOException {
    ZKConnection zkConnection = getZkConnectionBuilder(port).build();
    zkConnection.start();
    return zkConnection;
  }

  public static synchronized ZKPersistentConnection getZkPersistentConnection(int port) {
    return getZkPersistentConnection(port, false);
  }

  public static synchronized ZKPersistentConnection getZkPersistentConnection(int port, boolean forceNew) {
    List<ZKPersistentConnection> available = _zkPersistentConnection.computeIfAbsent(port, integer -> new ArrayList<>());
    if (available.size() == 0 || forceNew)
    {
      available.add(new ZKPersistentConnection(getZkConnectionBuilder(port)));
    }
    return available.get(available.size() - 1);
  }

  public static ZKConnectionBuilder getZkConnectionBuilder(int port) {
    return new ZKConnectionBuilder("localhost:" + port).setTimeout(5000);
  }

  // ########################### STORES ###########################

  public static ZooKeeperEphemeralStoreBuilder<String> getZooKeeperEphemeralStoreBuilder() {
    return new ZooKeeperEphemeralStoreBuilder<String>().setSerializer(new PropertyStringSerializer())
        .setPath(ZKFSUtil.uriPath(ZK_PATH))
        .setMerger(new PropertyStringMerger());
  }

  public static ZooKeeperConnectionAwareStore<String, ZooKeeperEphemeralStore<String>> getZKAwareStore(int port)
      throws IOException, PropertyStoreException, InterruptedException, ExecutionException, TimeoutException {


    // The store need a new connection since it needs to register new listeners to it and it can be done only on a
    // not-started-yet connection
    ZKPersistentConnection zkPersistentConnection = getZkPersistentConnection(port, true);

    ZooKeeperEphemeralStoreBuilder<String> zooKeeperStoreBuilder = getZooKeeperEphemeralStoreBuilder();

    ZooKeeperConnectionAwareStore<String, ZooKeeperEphemeralStore<String>> zkAware =
        new ZooKeeperConnectionAwareStore<>(zooKeeperStoreBuilder, zkPersistentConnection);

    zkPersistentConnection.start();
    FutureCallback<None> callback = new FutureCallback<>();

    zkAware.start(callback);
    callback.get(5, TimeUnit.SECONDS);

    return zkAware;
  }

  public static ZooKeeperEphemeralStore<String> getZooKeeperEphemeralStore(int port)
      throws InterruptedException, ExecutionException, TimeoutException, IOException {
    ZooKeeperEphemeralStoreBuilder<String> zooKeeperEphemeralStoreBuilder = getZooKeeperEphemeralStoreBuilder();
    zooKeeperEphemeralStoreBuilder.setZkConnection(getZKConnection(port));
    ZooKeeperEphemeralStore<String> store = zooKeeperEphemeralStoreBuilder.build();

    FutureCallback<None> callback = new FutureCallback<>();
    store.start(callback);
    callback.get(5, TimeUnit.SECONDS);

    return store;
  }

  public static LastSeenZKStore<String> getLastSeenZKStore(String path, int port)
      throws InterruptedException, ExecutionException, TimeoutException, IOException {

    // The store need a new connection since it needs to register new listeners to it and it can be done only on a
    // not-started-yet connection
    ZKPersistentConnection zkPersistentConnection = ZkStoreTestOnlyUtil.getZkPersistentConnection(port, true);
    LastSeenZKStore<String> lastSeenZKStore =
        new LastSeenZKStore<>(path, new PropertyStringSerializer(), ZkStoreTestOnlyUtil.getZooKeeperEphemeralStoreBuilder(),
            zkPersistentConnection, Executors.newSingleThreadScheduledExecutor(), 1, 10);

    zkPersistentConnection.start();

    FutureCallback<None> callback = new FutureCallback<>();
    lastSeenZKStore.start(callback);
    callback.get(5, TimeUnit.SECONDS);

    return lastSeenZKStore;
  }
}

