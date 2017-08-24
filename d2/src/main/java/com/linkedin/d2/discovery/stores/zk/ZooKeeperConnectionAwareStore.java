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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;
import com.linkedin.d2.discovery.stores.zk.builder.ZooKeeperStoreBuilder;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ZooKeeper Store Wrapper that monitors the connection and recreates the store if the connection gets re-established
 * This allows the higher level code to not worry about handling zk connection events, and instead just handle
 * eventbus/store events.
 *
 * When the bus is added to the class, in reality it is added directly to the wrapped ZK Store. This means
 * all the startPublishing/stopPublishing calls will be executed directly on the wrapped bus, and there is no need
 * to implement them in this class.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ZooKeeperConnectionAwareStore<TYPE, STORE extends ZooKeeperStore<TYPE>> implements PropertyEventPublisher<TYPE>
{
  private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperConnectionAwareStore.class);

  private final ZKPersistentConnection _zkPersistentConnection;
  private ZooKeeperStore<TYPE> _wrappedZkStore;
  private ZooKeeperStoreBuilder<STORE> _zkStoreBuilder;
  private PropertyEventBus<TYPE> _bus;

  private ConcurrentLinkedQueue<Runnable> _afterStartupCallbacks = new ConcurrentLinkedQueue<>();

  private volatile boolean _startupCompleted = false;

  public ZooKeeperConnectionAwareStore(ZooKeeperStoreBuilder<STORE> zooKeeperStoreBuilder,
      ZKPersistentConnection zkPersistentConnection)
  {
    _zkPersistentConnection = zkPersistentConnection;
    _zkPersistentConnection.addListeners(Collections.singleton(new Listener()));
    _zkStoreBuilder = zooKeeperStoreBuilder;
  }

  public void setBus(PropertyEventBusImpl<TYPE> bus)
  {
    _bus = bus;
    if (_wrappedZkStore != null)
    {
      bus.setPublisher(_wrappedZkStore);
    }
  }

  @Override
  public void setBus(PropertyEventBus<TYPE> bus)
  {
    if (!(bus instanceof PropertyEventBusImpl))
    {
      throw new IllegalArgumentException(
          "The bus used in LastSeenZKStore should be a PropertyEventBusImpl and not a " + bus.getClass().getName());
    }
    setBus(bus);
  }

  /**
   * This method is not supposed to be called directly.
   * Every call to startPublishing to the store should go through the PropertyEventBus.
   * Therefore, once the PropertyEventBus has been set, it will call the startPublishing method
   * directly on the wrapped zk store
   */
  @Override
  public void startPublishing(String prop)
  {
    throw new UnsupportedOperationException("This method should be called through the EventBus associated with this class");
  }

  /**
   * This method is not supposed to be called directly.
   * Every call to stopPublishing to the store should go through the PropertyEventBus.
   * Therefore, once the PropertyEventBus has been set, it will call the stopPublishing method
   * directly on the wrapped zk store
   */
  @Override
  public void stopPublishing(String prop)
  {
    throw new UnsupportedOperationException("This method should be called through the EventBus associated with this class");
  }

  // ################## life cycle section #####################

  /**
   * It's assumed that the Persistent connection has been started outside of this class
   */
  @Override
  public void start(Callback<None> callback)
  {
    _afterStartupCallbacks.add(() -> callback.onSuccess(None.none()));
    fireAfterStartupCallbacks();
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _afterStartupCallbacks.add(() -> _wrappedZkStore.shutdown(callback));
    fireAfterStartupCallbacks();
  }

  private void fireAfterStartupCallbacks()
  {
    if (_startupCompleted)
    {
      Runnable runnable;
      while ((runnable = _afterStartupCallbacks.poll()) != null)
      {
        runnable.run();
      }
    }
  }

  private void startStore()
  {
    _wrappedZkStore.start(new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        LOG.error("Failed to start " + _wrappedZkStore.getClass().getName(), e);
      }

      @Override
      public void onSuccess(None result)
      {
        // use setPublisher so the bus will re-startPublish the keys
        if (_bus != null)
        {
          _bus.setPublisher(_wrappedZkStore);
        }
        fireAfterStartupCallbacks();
      }
    });
  }

  /**
   * Helper class that re-creates the store when a new session is established
   */
  private class Listener extends ZKPersistentConnection.EventListenerNotifiers
  {
    @Override
    public void sessionEstablished(ZKPersistentConnection.Event event)
    {
      _zkStoreBuilder.setZkConnection(_zkPersistentConnection.getZKConnection());
      _wrappedZkStore = _zkStoreBuilder.build();
      _startupCompleted = true;
      startStore();
    }

    @Override
    public void sessionExpired(ZKPersistentConnection.Event event)
    {
      _wrappedZkStore.shutdown(Callbacks.empty());
    }
  }
}
