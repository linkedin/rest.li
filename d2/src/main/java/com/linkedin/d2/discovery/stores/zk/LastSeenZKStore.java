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
import com.linkedin.common.callback.MultiCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventBusRequestsThrottler;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.builder.ZooKeeperStoreBuilder;
import com.linkedin.r2.transport.http.client.TimeoutCallback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The LastSeenZKStore keeps an offline internal copy of the data fetched from ZK in the previous executions.
 * When the store starts up, it triggers an update request to ZK for all the props saved on disk which might have
 * stale values. This set of requests are throttled by a max number of concurrent requests.
 *
 * There is no guarantee that the onInit/onAdd for same prop-value is triggered only once. When ZK creates
 * a new session, all the value will be re-issued on the bus. Some might have changed, others not
 *
 * When requesting a prop from the bus, it will be immediately published to the same bus if present on disk,
 * otherwise it will take the time to retrieve it from ZK
 *
 * The LastSeenZKStore, like the other stores, doesn't manage the connection, but it only listen to its events.
 * This allow the connection to be shared among multiple objects which never take fully ownership of the it, and
 * leave the duty of coordination to the user of these objects.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class LastSeenZKStore<T> implements PropertyEventPublisher<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(LastSeenZKStore.class);

  private final FileStore<T> _fsStore;
  private final ZooKeeperConnectionAwareStore<T, ? extends ZooKeeperStore<T>> _zkAwareStore;
  private final ZkBusUpdater _zkBusUpdaterSubscriber;
  private final ScheduledExecutorService _executorService;
  private final int _warmUpTimeoutSeconds;
  private PropertyEventBus<T> _clientBus;
  private PropertyEventBus<T> _zkToFsBus;
  private final int _concurrentRequests;

  public LastSeenZKStore(FileStore<T> fsStore,
                         ZooKeeperStoreBuilder<? extends ZooKeeperStore<T>> zooKeeperStoreBuilder,
      ZKPersistentConnection zkPersistentConnection, ScheduledExecutorService executorService, int warmUpTimeoutSeconds,
      int concurrentRequests)
  {
    _executorService = executorService;
    _warmUpTimeoutSeconds = warmUpTimeoutSeconds;
    _concurrentRequests = concurrentRequests;
    _fsStore = fsStore;
    _zkToFsBus = new PropertyEventBusImpl<>(executorService);
    _zkBusUpdaterSubscriber = new ZkBusUpdater();
    _zkAwareStore = new ZooKeeperConnectionAwareStore<>(zooKeeperStoreBuilder, zkPersistentConnection);
    _zkToFsBus.setPublisher(_zkAwareStore);
  }

  /**
   * @param bus the bus should be a {@link PropertyEventBusImpl} because we rely on the fact that it keeps an
   *            internal copy of the values and it triggers {@link #startPublishing} only once for each property.
   *            Multiple calls to {@link #startPublishing} would mean multiple calls to FS/ZK
   */
  @Override
  public void setBus(PropertyEventBus<T> bus)
  {
    if (!(bus instanceof PropertyEventBusImpl))
    {
      LOG.warn("The bus used in LastSeenZKStore should be a PropertyEventBusImpl and not a " + bus.getClass().getName());
    }
    _clientBus = bus;
  }

  @Override
  public void startPublishing(String prop)
  {
    _executorService.submit(() -> {
      T valueInFileStore = _fsStore.get(prop);
      if (valueInFileStore != null)
      {
        _clientBus.publishInitialize(prop, valueInFileStore);
      }
      else
      {
        _zkToFsBus.register(Collections.singleton(prop), _zkBusUpdaterSubscriber);
      }
    });
  }

  @Override
  public void stopPublishing(String prop)
  {
    _zkToFsBus.unregister(Collections.singleton(prop), _zkBusUpdaterSubscriber);
    _executorService.submit(() -> {
      _fsStore.remove(prop);
    });
  }

  /**
   * Receives the updated data from Zk and updates the fsStore and the clientBus
   */
  class ZkBusUpdater implements PropertyEventSubscriber<T>
  {
    void updateFsStore(String propertyName, T propertyValue)
    {
      if (propertyValue != null)
      {
        _fsStore.put(propertyName, propertyValue);
      } else
      {
        _fsStore.remove(propertyName);
      }
    }

    @Override
    public void onInitialize(String propertyName, T propertyValue)
    {
      updateFsStore(propertyName, propertyValue);
      _clientBus.publishInitialize(propertyName, propertyValue);
    }

    @Override
    public void onAdd(String propertyName, T propertyValue)
    {
      updateFsStore(propertyName, propertyValue);
      _clientBus.publishAdd(propertyName, propertyValue);
    }

    @Override
    public void onRemove(String propertyName)
    {
      _fsStore.remove(propertyName);
      _clientBus.publishRemove(propertyName);
    }
  }

  // ################## lifecycle section #####################

  /**
   * starts the stores and request the latest data from zk
   */
  @Override
  public void start(Callback<None> callback)
  {
    Callback<None> warmUpCallback = new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(None result)
      {
        warmUp(callback);
      }
    };

    // there is no need to wait for zookeeper store to be started
    // since we have the fs store.
    _zkAwareStore.start(Callbacks.empty());

    _fsStore.start(warmUpCallback);
  }

  /**
   * Starts listening to all the props present on disk. The process can take up to _warmUpTimeoutSeconds before
   * the callback is called. Eventually all the values will be retrieved from ZK
   */
  private void warmUp(Callback<None> callback)
  {
    // we want to be certain that the warm up doesn't take more than warmUpTimeoutSeconds
    Callback<None> timeoutCallback =
        new TimeoutCallback<>(_executorService, _warmUpTimeoutSeconds, TimeUnit.SECONDS, new Callback<None>()
        {
          @Override
          public void onError(Throwable e)
          {
            LOG.info(
                "EventBus Throttler didn't send all requests in time, continuing startup. The WarmUp will continue in background");
            callback.onSuccess(None.none());
          }

          @Override
          public void onSuccess(None result)
          {
            LOG.info("EventBus Throttler sent all requests");
            callback.onSuccess(None.none());
          }
        }, "This message will never be used, even in case of timeout, no exception should be passed up");

    // make warmup requests through requests throttler
    List<String> fileListWithoutExtension = new ArrayList<>(_fsStore.getAll().keySet());
    PropertyEventBusRequestsThrottler<T> throttler =
        new PropertyEventBusRequestsThrottler<>(_zkToFsBus, _zkBusUpdaterSubscriber, fileListWithoutExtension,
            _concurrentRequests, true);
    throttler.sendRequests(timeoutCallback);
  }

  @Override
  public void shutdown(Callback<None> shutdown)
  {
    MultiCallback multiCallback = new MultiCallback(shutdown, 2);
    _fsStore.shutdown(multiCallback);
    _zkAwareStore.shutdown(multiCallback);
  }
}
