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

import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import java.util.function.Consumer;


/**
 * Interface for creating ZooKeeper-based stores.
 *
 * The underlying store shouldn't manage the ZKConnection, which is always passed by the builder.
 * Having the stores managing the lifecycle, would limit sharing the connection with other structures
 * and forcing creating multiple connections to ZK from the same application
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public interface ZooKeeperStoreBuilder<STORE extends ZooKeeperStore<?>>
{
  /**
   * Set the ZK connection that will be used building the store
   */
  void setZkConnection(ZKConnection client);

  /**
   * Set an action to be run when the store is built
   */
  ZooKeeperStoreBuilder<STORE> addOnBuildListener(Consumer<STORE> onBuildAction);

  STORE build();
}