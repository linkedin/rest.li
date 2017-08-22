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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZKConnectionBuilder;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manages a ZooKeeper connection and one or more Announcers.  Upon being started, tells the
 * announcers to announce themselves after the connection is ready.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZooKeeperConnectionManager
{
  private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperConnectionManager.class);

  private final String _zkConnectString;
  private final int _zkSessionTimeout;
  private final String _zkBasePath;
  private final ZKStoreFactory<UriProperties,ZooKeeperEphemeralStore<UriProperties>> _factory;
  private final ZooKeeperAnnouncer[] _servers;
  private final AtomicReference<Callback<None>> _startupCallback = new AtomicReference<Callback<None>>();
  private final ZKPersistentConnection _zkConnection;

  private volatile ZooKeeperEphemeralStore<UriProperties> _store;

  public ZooKeeperConnectionManager(String zkConnectString, int zkSessionTimeout,
                                    String zkBasePath,
                                    ZKStoreFactory<UriProperties,ZooKeeperEphemeralStore<UriProperties>> factory,
                                    ZooKeeperAnnouncer... servers)
  {
    _zkConnectString = zkConnectString;
    _zkSessionTimeout = zkSessionTimeout;
    _zkBasePath = zkBasePath;
    _factory = factory;
    _servers = servers;
    _zkConnection = new ZKPersistentConnection(new ZKConnectionBuilder(_zkConnectString).setTimeout(_zkSessionTimeout));
    _zkConnection.addListeners(Collections.singletonList(new Listener()));
  }

  /**
   * @deprecated  Use {@link #ZooKeeperConnectionManager(String, int, String, ZKStoreFactory, ZooKeeperAnnouncer...)} instead.
   */
  @Deprecated
  public ZooKeeperConnectionManager(String zkConnectString, int zkSessionTimeout,
                                    String zkBasePath,
                                    ZKStoreFactory<UriProperties,ZooKeeperEphemeralStore<UriProperties>> factory,
                                    int retryLimit,
                                    ZooKeeperAnnouncer... servers)
  {
    this(zkConnectString, zkSessionTimeout, zkBasePath, factory, servers);
  }

  /**
   * @deprecated  Use {@link #ZooKeeperConnectionManager(String, int, String, ZKStoreFactory, ZooKeeperAnnouncer...)} instead.
   */
  @Deprecated
  public ZooKeeperConnectionManager(String zkConnectString, int zkSessionTimeout,
                                    String zkBasePath,
                                    ZKStoreFactory<UriProperties,ZooKeeperEphemeralStore<UriProperties>> factory,
                                    int retryLimit,
                                    boolean exponentialBackoff,
                                    ScheduledExecutorService scheduler,
                                    long initInterval,
                                    ZooKeeperAnnouncer... servers)
  {
    this(zkConnectString, zkSessionTimeout, zkBasePath, factory, servers);
  }

  public void start(Callback<None> callback)
  {
    if (!_startupCallback.compareAndSet(null, callback))
    {
      throw new IllegalStateException("Already starting");
    }
    try
    {
      _zkConnection.start();
      LOG.info("Started ZooKeeper connection to {}", _zkConnectString);
    }
    catch (Exception e)
    {
      _startupCallback.set(null);
      callback.onError(e);
    }
  }

  public void shutdown(final Callback<None> callback)
  {
    Callback<None> zkCloseCallback = new CallbackAdapter<None,None>(callback)
    {
      @Override
      protected None convertResponse(None none) throws Exception
      {
        _zkConnection.shutdown();
        return none;
      }
    };
    if (_store != null)
    {
      _store.shutdown(zkCloseCallback);
    }
    else
    {
      zkCloseCallback.onSuccess(None.none());
    }
  }

  public void markDownAllServers(final Callback<None> callback)
  {
    Callback<None> markDownCallback;
    if (callback != null)
    {
      markDownCallback = callback;
    }
    else
    {
      markDownCallback = new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          LOG.error("failed to mark down servers", e);
        }

        @Override
        public void onSuccess(None result)
        {
          LOG.info("mark down all servers successful");
        }
      };
    }
    Callback<None> multiCallback = Callbacks.countDown(markDownCallback, _servers.length);
    for (ZooKeeperAnnouncer server : _servers)
    {
      server.markDown(multiCallback);
    }
  }

  public void markUpAllServers(final Callback<None> callback)
  {
    Callback<None> markUpCallback;
    if (callback != null)
    {
      markUpCallback = callback;
    }
    else
    {
      markUpCallback = new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          LOG.error("failed to mark up servers", e);
        }

        @Override
        public void onSuccess(None result)
        {
          LOG.info("mark up all servers successful");
        }
      };
    }
    Callback<None> multiCallback = Callbacks.countDown(markUpCallback, _servers.length);
    for (ZooKeeperAnnouncer server : _servers)
    {
      server.markUp(multiCallback);
    }
  }

  private class Listener implements ZKPersistentConnection.EventListener
  {
    /**
     * a boolean flag to indicate whether _store is successfully started or not
     */
    private volatile boolean _storeStarted = false;

    @Override
    public void notifyEvent(ZKPersistentConnection.Event event)
    {
      LOG.info("Received ZKPersistentConnection Event {}", event);
      switch (event)
      {
        case SESSION_ESTABLISHED:
        {
          _store = _factory.createStore(_zkConnection.getZKConnection(), ZKFSUtil.uriPath(_zkBasePath));
          startStore();
          break;
        }
        case SESSION_EXPIRED:
        {
          _store.shutdown(Callbacks.<None>empty());
          _storeStarted = false;
          break;
        }
        case CONNECTED:
        {
          if (!_storeStarted)
          {
            startStore();
          }
          else
          {
            for (ZooKeeperAnnouncer server : _servers)
            {
              server.retry(Callbacks.<None>empty());
            }
          }
          break;
        }
        case DISCONNECTED:
          // do nothing
          break;
      }
    }

    private void startStore()
    {
      final Callback<None> callback = _startupCallback.getAndSet(null);
      final Callback<None> multiCallback = callback != null ?
          Callbacks.countDown(callback, _servers.length) :
          Callbacks.<None>empty();
      _store.start(new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          LOG.error("Failed to start ZooKeeperEphemeralStore", e);
          if (callback != null)
          {
            callback.onError(e);
          }
        }

        @Override
        public void onSuccess(None result)
        {
          /* mark store as started */
          _storeStarted = true;
          for (ZooKeeperAnnouncer server : _servers)
          {
            server.setStore(_store);
            server.start(new Callback<None>()
            {
              @Override
              public void onError(Throwable e)
              {
                LOG.error("Failed to start server", e);
                multiCallback.onError(e);
              }

              @Override
              public void onSuccess(None result)
              {
                LOG.info("Started an announcer");
                multiCallback.onSuccess(result);
              }
            });
          }
          LOG.info("Starting {} announcers", (_servers.length));
        }
      });
    }
  }

  public interface ZKStoreFactory<P, Z extends ZooKeeperStore<P>>
  {
    Z createStore(ZKConnection connection, String path);
  }

  public ZooKeeperAnnouncer[] getAnnouncers()
  {
    return _servers;
  }
}
