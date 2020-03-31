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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.trace;

public class ZooKeeperPermanentStore<T> extends ZooKeeperStore<T>
{
  private static final Logger                                     _log =
                                                                        LoggerFactory.getLogger(ZooKeeperPermanentStore.class);

  private final ZKStoreWatcher _zkStoreWatcher = new ZKStoreWatcher();
  private final ScheduledExecutorService _executorService;
  private int _zookeeperReadWindowMs;

  public ZooKeeperPermanentStore(ZKConnection client,
                                 PropertySerializer<T> serializer,
                                 String path)
  {
    this(client, serializer, path, null, DEFAULT_READ_WINDOW_MS);
  }

  public ZooKeeperPermanentStore(ZKConnection client,
                                 PropertySerializer<T> serializer,
                                 String path, ScheduledExecutorService executorService, int zookeeperReadWindowMs)
  {
    super(client, serializer, path);
    _executorService = executorService;
    _zookeeperReadWindowMs = zookeeperReadWindowMs;
  }

  @Override
  public void put(final String listenTo, final T discoveryProperties, final Callback<None> callback)
  {
    _putStats.inc();

    trace(_log, "put ", listenTo, ": ", discoveryProperties);

    final String path = getPath(listenTo);

    _zkConn.ensurePersistentNodeExists(path, getExistsCallBack(listenTo, discoveryProperties, callback));
  }

  protected Callback<None> getExistsCallBack(final String listenTo, final T discoveryProperties, final Callback<None> callback)
  {
    final String path = getPath(listenTo);
    return new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        _zkConn.setDataUnsafe(path, _serializer.toBytes(discoveryProperties), callback);
      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    };
  }

  @Override
  public void remove(String listenTo, Callback<None> callback)
  {
    _removeStats.inc();

    trace(_log, "remove: ", listenTo);

    String path = getPath(listenTo);
    _zkConn.removeNodeUnsafe(path, callback);
  }

  @Override
  public void get(String listenTo, Callback<T> callback)
  {
    _getStats.inc();

    trace(_log, "get: ", listenTo);

    String path = getPath(listenTo);

    AsyncCallback.DataCallback dataCallback = ZkUtil.zkDataCallback(callback, _serializer);

    _zk.getData(path, false, dataCallback, null);

  }

  @Override
  public void startPublishing(final String prop)
  {
    trace(_log, "register: ", prop);

    if (_eventBus == null)
    {
      throw new IllegalStateException("_eventBus must not be null when publishing");
    }

    // Zookeeper callbacks (see the listener below) will be executed by the Zookeeper
    // notification thread, in order.  See:
    // http://zookeeper.apache.org/doc/current/zookeeperProgrammers.html#Java+Binding
    // This call occurs on a different thread, the PropertyEventBus callback thread.
    //
    // Publication to the event bus always occurs in the callback which is executed by the
    // ZooKeeper notification thread.  Since ZK guarantees the callbacks will be executed in
    // the same order as the requests were made, we will never publish a stale value to the bus,
    // even if there was a watch set on this property before this call to startPublishing().

    _zkStoreWatcher.addWatch(prop);
    _zk.getData(getPath(prop), _zkStoreWatcher, _zkStoreWatcher, true);
  }

  @Override
  public void stopPublishing(String prop)
  {
    trace(_log, "unregister: ", prop);

    _zkStoreWatcher.cancelWatch(prop);
  }

  public int getListenerCount()
  {
    return _zkStoreWatcher.getWatchCount();
  }

  private class ZKStoreWatcher extends ZooKeeperStore<T>.ZKStoreWatcher
          implements AsyncCallback.DataCallback, AsyncCallback.StatCallback
  {
    @Override
    protected void processWatch(String propertyName, WatchedEvent watchedEvent)
    {
      // Reset the watch
      if (_zookeeperReadWindowMs > 0 && _executorService != null)
      {
        // for the static config we can spread the read across the read Window
        int delay = ThreadLocalRandom.current().nextInt(_zookeeperReadWindowMs);
        _executorService.schedule(() -> _zk.getData(watchedEvent.getPath(), this, this, false),
                                  delay, TimeUnit.MILLISECONDS);
      }
      else
      {
        // Reset the watch and read the data
        _zk.getData(watchedEvent.getPath(), this, this, false);
      }
    }

    /**
     * Callback for getData call
     */
    @Override
    public void processResult(int rc, String path, Object ctx, byte[] bytes, Stat stat)
    {
      KeeperException.Code code = KeeperException.Code.get(rc);
      _log.debug("{}: getData returned {}", path, code);
      String propertyName =  getPropertyForPath(path);
      boolean init = (Boolean)ctx;
      switch (code)
      {
        case OK:
          T propertyValue;
          try
          {
            propertyValue = _serializer.fromBytes(bytes);
          }
          catch (PropertySerializationException e)
          {
            _log.error("Failed to deserialize property " + propertyName + ", value in bytes:" + new String(bytes), e);
            propertyValue = null;
          }
          if (init)
          {
            _eventBus.publishInitialize(propertyName, propertyValue);
            _log.debug("{}: published init", path);
          }
          else
          {
            _eventBus.publishAdd(propertyName, propertyValue);
            _log.debug("{}: published add", path);
          }
          break;

        case NONODE:
          if (init)
          {
            _eventBus.publishInitialize(propertyName, null);
            _log.debug("{}: published init for NONODE event", path);
          }
          else
          {
            _eventBus.publishRemove(propertyName);
            _log.debug("{}: published remove", path);
          }
          // We must call exists to be informed if the node is created
          _log.debug("{}: node not present, calling exists", path);
          _zk.exists(path, this, this, false);
          break;

        default:
          _log.error("getData: unexpected error: {}: {}", code, path);
          break;
      }
    }

    /**
     * Callback for exist call
     */
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat)
    {
      KeeperException.Code code = KeeperException.Code.get(rc);
      _log.debug("{}: exists returned {}", path, code);
      switch (code)
      {
        case OK:
          // The node is back, get its content
          _log.debug("{}: node is back, calling getData");
          _zk.getData(path, this, this, false);
          break;

        case NONODE:
          // The watch is set.
          _log.debug("{}: set exists watch", path);
          break;

        default:
          _log.error("exists: unexpected error: {}: {}", code, path);
      }
    }
  }

}
