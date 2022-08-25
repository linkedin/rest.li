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

package com.linkedin.d2.discovery.stores.zk;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.toggling.TogglingPublisher;
import com.linkedin.pegasus.org.apache.zookeeper.Watcher.Event.KeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.info;
import static com.linkedin.d2.discovery.util.LogUtil.warn;

/**
 * ZooKeeperTogglingStore manages a ZooKeeperStore, a backup store, and an event bus such that if a
 * connection is ever lost with ZooKeeper, the backup store will take the ZooKeeper store's place
 * as the source of truth on the event bus.  No automatic toggle "on" occurs; ZK must be reenabled
 * through JMX or some other code.
 *
 * Note reenabling of ZK is currently not supported; need to add code to reconcile ZK's updated
 * state with the bus listeners.  E.g., entire current state of ZK needs to be read out and
 * published to the bus to make sure that stale values are not cached.
 *
 * <br/>
 * <br/>
 *
 * This class is useful in environments where ZooKeeper is assumed to be highly available,
 * and any fault in ZooKeeper could have major impact on production operations. In such an
 * environment, it's better to disable interaction with the ZooKeeperStore, and fall back
 * on a FileStore (for example), until the state has been manually verified by a human.
 */
public class ZooKeeperTogglingStore<T> extends TogglingPublisher<T>
{
  private static final Logger            _log =
                                                  LoggerFactory.getLogger(ZooKeeperTogglingStore.class);

  public ZooKeeperTogglingStore(final ZooKeeperStore<T> store, PropertyStore<T> backup, PropertyEventBus<T> eventBus, boolean allowToggling)
  {
    super(store, backup, eventBus);

    if (store != null && allowToggling)
    {
      store.getClient().addStateListener(new ZKConnection.StateListener()
      {
        @Override
        public void notifyStateChange(KeeperState state)
        {

          if (state != KeeperState.SyncConnected)
          {
            warn(_log, " lost zk connection, so shutting down, and toggling zk store off");

            // TODO HIGH
            //setEnabled(false);

            // this will block until zk comes back, at which point, shutdown will complete
            store.shutdown(new Callback<None>()
            {
              @Override
              public void onError(Throwable e)
              {
                warn(_log, "shutdown didn't complete");
              }

              @Override
              public void onSuccess(None result)
              {
                info(_log, "shutdown complete");
              }
            });
          }
        }
      });
    }
    else
    {
      warn(_log,
           "got a null store when constructing zk toggling store, so starting disabled");
    }
  }

}
