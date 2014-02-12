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

import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

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
  private volatile ZKConnection _zkConnection;
  private final int _limit;
  private final boolean _exponentialBackoff;
  private final ScheduledExecutorService _scheduler;
  private final long _initInterval;

  public ZooKeeperConnectionManager(String zkConnectString, int zkSessionTimeout,
                                    String zkBasePath,
                                    ZKStoreFactory<UriProperties,ZooKeeperEphemeralStore<UriProperties>> factory,
                                    ZooKeeperAnnouncer... servers)
  {
    this(zkConnectString, zkSessionTimeout, zkBasePath, factory, 0, servers);
  }

  public ZooKeeperConnectionManager(String zkConnectString, int zkSessionTimeout,
                                    String zkBasePath,
                                    ZKStoreFactory<UriProperties,ZooKeeperEphemeralStore<UriProperties>> factory,
                                    int retryLimit,
                                    ZooKeeperAnnouncer... servers)
  {
    this(zkConnectString, zkSessionTimeout, zkBasePath, factory, retryLimit, false, null, 0, servers);
  }

  public ZooKeeperConnectionManager(String zkConnectString, int zkSessionTimeout,
                                    String zkBasePath,
                                    ZKStoreFactory<UriProperties,ZooKeeperEphemeralStore<UriProperties>> factory,
                                    int retryLimit,
                                    boolean exponentialBackoff,
                                    ScheduledExecutorService scheduler,
                                    long initInterval,
                                    ZooKeeperAnnouncer... servers)
  {
    _zkConnectString = zkConnectString;
    _zkSessionTimeout = zkSessionTimeout;
    _zkBasePath = zkBasePath;
    _factory = factory;
    _servers = servers;
    _limit = retryLimit;
    _exponentialBackoff = exponentialBackoff;
    _scheduler = scheduler;
    _initInterval = initInterval;
  }

  public void start(Callback<None> callback)
  {
    if (!_startupCallback.compareAndSet(null, callback))
    {
      throw new IllegalStateException("Already starting");
    }
    _zkConnection = new ZKConnection(_zkConnectString,
                                    _zkSessionTimeout,
                                    _limit,
                                    _exponentialBackoff,
                                    _scheduler,
                                    _initInterval);
    _zkConnection.addStateListener(new Listener());
    for (ZooKeeperAnnouncer server : _servers)
    {
      server.setStore(_factory.createStore(_zkConnection, ZKFSUtil.uriPath(_zkBasePath)));
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
        _zkConnection.getZooKeeper().close();
        return none;
      }
    };
    Callback<None> multiCallback = Callbacks.countDown(zkCloseCallback, _servers.length);
    for (ZooKeeperAnnouncer server : _servers)
    {
      server.shutdown(multiCallback);
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

  private class Listener implements ZKConnection.StateListener
  {
    @Override
    public void notifyStateChange(Watcher.Event.KeeperState state)
    {
      LOG.info("Received KeeperState {}", state);
      switch (state)
        {
          case SyncConnected:
          {
            Callback<None> callback = _startupCallback.getAndSet(null);
            if (callback != null)
            {
              final Callback<None> multiCallback = Callbacks.countDown(callback, _servers.length);
              for (final ZooKeeperAnnouncer server : _servers)
              {
                LOG.info("Starting an announcer");
                server.start(new Callback<None>()
                {
                  @Override
                  public void onSuccess(final None none)
                  {
                    server.markUp(multiCallback);
                  }

                  @Override
                  public void onError(Throwable e)
                  {
                    LOG.error("Failed to start server", e);
                    multiCallback.onError(e);
                  }
                });
                LOG.info("Started an announcer");
              }
              LOG.info("Started {} announcers", (_servers.length));
            }
            else
            {
              LOG.warn("Ignored SyncConnected");
            }
            break;
          }
          case Disconnected:
          {
            break;
          }
          case Expired:
          {
            // In this case ephemeral nodes will have been deleted, we need to fully reinitialize
            Callback<None> callback = new Callback<None>()
            {
              @Override
              public void onSuccess(None none)
              {
                LOG.info("Restarted ZK servers after session expiration");
              }

              @Override
              public void onError(Throwable e)
              {
                LOG.error("Failed to restart ZK servers after session expiration", e);
              }
            };
            start(callback);
            break;
          }
          default:
          {
            LOG.info("Ignored unknown KeeperState {}", state);
            break;
          }
        }
    }
  }

  public interface ZKStoreFactory<P, Z extends ZooKeeperStore<P>>
  {
    Z createStore(ZKConnection connection, String path);
  }
}
