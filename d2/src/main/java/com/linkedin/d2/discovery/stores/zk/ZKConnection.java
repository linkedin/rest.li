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

package com.linkedin.d2.discovery.stores.zk;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very very lightweight holder for a ZK connection which allows monitoring of the connection state.
 * It uses the default watcher for connection state monitoring.  Do NOT set a different default watcher
 * on this connection.  Use of the async methods that set a watch to be delivered to the default
 * watcher is discouraged, as such watches will be ignored when triggered.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKConnection
{
  private static final Logger LOG = LoggerFactory.getLogger(ZKConnection.class);
  private static final int MAX_RETRIES = 10;

  private final String _connectString;
  private final int _timeout;
  private final int _retryLimit;
  private final boolean _exponentialBackoff;
  private final ScheduledExecutorService _scheduler;
  private final long _initInterval;

  private final AtomicReference<ZooKeeper> _zkRef = new AtomicReference<ZooKeeper>();

  // _mutex protects the two fields below: _listeners and _currentState
  private final Object _mutex = new Object();
  private final Set<StateListener> _listeners = new HashSet<StateListener>();
  private Watcher.Event.KeeperState _currentState;

  public interface StateListener
  {
    void notifyStateChange(Watcher.Event.KeeperState state);
  }

  public ZKConnection(String connectString, int timeout)
  {
    this(connectString, timeout, 0);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit)
  {
    this(connectString, timeout, retryLimit, false, null, 0);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit, boolean exponentialBackoff,
                      ScheduledExecutorService scheduler, long initInterval)
  {
    _connectString = connectString;
    _timeout = timeout;
    _retryLimit = retryLimit;
    _exponentialBackoff = exponentialBackoff;
    _scheduler = scheduler;
    _initInterval = initInterval;
  }

  public void start() throws IOException
  {
    if (_zkRef.get() != null)
    {
      throw new IllegalStateException("Already started");
    }

    // We take advantage of the fact that the default watcher is always
    // notified of connection state changes (without having to explicitly register)
    // and never notified of anything else.
    ZooKeeper  zk;
    if (_retryLimit <= 0)
    {
      zk = new ZooKeeper(_connectString, _timeout, new DefaultWatcher());
      LOG.info("Using vanilla ZooKeeper without retry.");
    }
    else
    {
      zk = new RetryZooKeeper(_connectString,
                              _timeout,
                              new DefaultWatcher(), _retryLimit,
                              _exponentialBackoff,
                              _scheduler,
                              _initInterval);
      LOG.info("Using RetryZooKeeper with retry limit set to " + _retryLimit);
      if (_exponentialBackoff)
      {
        LOG.info("Exponential backoff enabled. Initial retry interval set to " + _initInterval + " ms.");
      }
      else
      {
        LOG.info("Exponential backoff disabled.");
      }
    }
    if (!_zkRef.compareAndSet(null, zk))
    {
      try
      {
        zk.close();
      }
      catch (InterruptedException e)
      {
        LOG.warn("Failed to shutdown extra ZooKeeperConnection", e);
      }
      throw new IllegalStateException("Already started");
    }
  }

  public void shutdown() throws InterruptedException
  {
    ZooKeeper zk = _zkRef.get();
    if (zk == null || !_zkRef.compareAndSet(zk, null))
    {
      throw new IllegalStateException("Already shutdown");
    }
    zk.close();
  }

  private ZooKeeper zk()
  {
    ZooKeeper zk = _zkRef.get();
    if (zk == null)
    {
      throw new IllegalStateException("ZKConnection not yet started");
    }
    return zk;
  }

  public ZooKeeper getZooKeeper()
  {
    return zk();
  }

  public void waitForState(KeeperState state, long timeout, TimeUnit timeUnit)
    throws InterruptedException, TimeoutException
  {
    long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);

    synchronized (_mutex)
    {
      while(!state.equals(_currentState))
      {
        long waitTime = endTime - System.currentTimeMillis();

        if(waitTime > 0)
        {
          _mutex.wait(waitTime);
        }
        else
        {
          throw new TimeoutException("timeout expired without state being reached");
        }
      }
    }
  }

  public void addStateListener(StateListener listener)
  {
    synchronized (_mutex)
    {
      _listeners.add(listener);
    }
  }

  /**
   * checks if the path in zk exist or not. If it doesn't exist, will create the node.
   * Warning: this method will create the path recursively but since the path will
   * be smaller every recursive call, it should terminate.
   * @param path
   * @param callback
   */
  public void ensurePersistentNodeExists(String path, final Callback<None> callback)
  {
    final ZooKeeper zk = zk();
    // Remove any trailing slash except for when we just want the root
    while (path.endsWith("/") && path.length() > 1)
    {
      path = path.substring(0, path.length() - 1);
    }
    final String normalizedPath = path;
    AsyncCallback.StringCallback createCallback = new AsyncCallback.StringCallback()
    {
      @Override
      public void processResult(int rc, String unused, Object ctx, String name)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
          case NODEEXISTS:
            callback.onSuccess(None.none());
            break;
          case NONODE:
            // create parent and retry
            String parent = normalizedPath.substring(0, normalizedPath.lastIndexOf('/'));
            ensurePersistentNodeExists(parent, new Callback<None>()
            {
              @Override
              public void onSuccess(None none)
              {
                ensurePersistentNodeExists(normalizedPath, callback);
              }

              @Override
              public void onError(Throwable e)
              {
                callback.onError(e);
              }
            });
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    };
    try
    {
      zk.create(normalizedPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, createCallback, null);
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * Sets the data associated with a node in zookeeper. Use optimistic concurrency to set the data and
   * will try up to MAX_RETRIES (default config is set to 10)
   *
   * The "unsafe" at the end of this method's name signifies that this method is thread unsafe.
   * It doesn't mean the data is set to be unsafe.
   *
   * @param path
   * @param data
   * @param callback
   */
  public void setDataUnsafe(String path, byte[] data, Callback<None> callback)
  {
    setDataUnsafe(path, data, callback, 0);
  }

  private void setDataUnsafe(final String path, final byte[] data, final Callback<None> callback, final int count)
  {
    final ZooKeeper zk = zk();
    final AsyncCallback.StatCallback dataCallback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            callback.onSuccess(None.none());
            break;
          case BADVERSION:
            if (count < MAX_RETRIES)
            {
              LOG.info("setDataUnsafe: ignored BADVERSION for {}", path);
              setDataUnsafe(path, data, callback, count + 1);
            }
            else
            {
              callback.onError(KeeperException.create(code));
            }
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    };
    final AsyncCallback.StatCallback statCallback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            zk.setData(path, data, stat.getVersion(), dataCallback, null);
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    };
    try
    {
      zk.exists(path, false, statCallback, null);
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * remove a node in zookeeper. Use optimistic concurrency to remove the node and
   * will try up to MAX_RETRIES (default config is set to 10)
   *
   * The "unsafe" at the end of this method's name signifies that this method is thread unsafe.
   * It doesn't mean the data is set to be unsafe.
   *
   * @param path
   * @param callback
   */
  public void removeNodeUnsafe(String path, Callback<None> callback)
  {
    removeNodeUnsafe(path, callback, 0);
  }

  private void removeNodeUnsafe(final String path, final Callback<None> callback, final int count)
  {
    final ZooKeeper zk = zk();

    final AsyncCallback.VoidCallback deleteCallback = new AsyncCallback.VoidCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            callback.onSuccess(None.none());
            break;
          case BADVERSION:
            // Need to retry
            if (count < MAX_RETRIES)
            {
              LOG.info("removeNodeUnsafe: retrying after ignoring BADVERSION for {}", path);
              removeNodeUnsafe(path, callback, count + 1);
            }
            else
            {
              callback.onError(KeeperException.create(code));
            }
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    };

    final AsyncCallback.StatCallback existsCallback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            zk.delete(path, stat.getVersion(), deleteCallback, null);
            break;
          case NONODE:
            callback.onSuccess(None.none());
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    };

    try
    {
      zk.exists(path, false, existsCallback, null);
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * {@see removeNodeUnsafe} but remove recursively
   *
   * @param path
   * @param callback
   */
  public void removeNodeUnsafeRecursive(final String path, final Callback<None> callback)
  {
    final ZooKeeper zk = zk();

    final Callback<None> deleteThisNodeCallback = new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        removeNodeUnsafe(path, callback);
      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    };

    // Note ChildrenCallback is compatible with a ZK 3.2 server; Children2Callback is
    // compatible only with ZK 3.3+ server.
    final AsyncCallback.ChildrenCallback childCallback = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            Callback<None> multiCallback = Callbacks.countDown(deleteThisNodeCallback, children.size());
            for (String child : children)
            {
              removeNodeUnsafeRecursive(path + "/" + child, multiCallback);
            }
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }

      }
    };

    try
    {
      zk.getChildren(path, false, childCallback, null);
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }


  private class DefaultWatcher implements Watcher
  {
    @Override
    public void process(WatchedEvent watchedEvent)
    {
      ZooKeeper zk = zk();
      if (watchedEvent.getType() == Event.EventType.None)
      {
        Event.KeeperState state = watchedEvent.getState();
        LOG.info("Received state notification {} for session 0x{}", state, Long.toHexString(zk.getSessionId()));
        Set<StateListener> listeners = Collections.emptySet();
        synchronized (_mutex)
        {
          if (_currentState != state)
          {
            _currentState = state;
            _mutex.notifyAll();
            listeners = new HashSet<StateListener>(_listeners);
          }
        }
        for (StateListener listener : listeners)
        {
          listener.notifyStateChange(state);
        }

      }
      else
      {
        LOG.warn("Received unexpected event of type {} for session 0x{}", watchedEvent.getType(), Long.toHexString(zk.getSessionId()));
      }
    }
  }

}
