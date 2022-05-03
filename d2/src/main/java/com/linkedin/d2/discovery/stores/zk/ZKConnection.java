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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.PropertySerializer;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
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
  private final boolean _shutdownAsynchronously;
  private final boolean _isSymlinkAware;
  private final Function<ZooKeeper, ZooKeeper> _zkDecorator;
  private PropertySerializer<String> _symlinkSerializer = new SymlinkAwareZooKeeper.DefaultSerializer();
  private final boolean _isWaitForConnected;

  // _countDownLatch signals when _zkRef is ready to be used
  private final CountDownLatch _zkRefLatch = new CountDownLatch(1);
  private final AtomicReference<ZooKeeper> _zkRef = new AtomicReference<>();

  // _mutex protects the two fields below: _listeners and _currentState
  private final Object _mutex = new Object();
  private final Set<StateListener> _listeners = new HashSet<>();
  private Watcher.Event.KeeperState _currentState;

  public interface StateListener
  {
    void notifyStateChange(Watcher.Event.KeeperState state);
  }

  public ZKConnection(String connectString, int timeout)
  {
    this(connectString, timeout, false);
  }

  public ZKConnection(String connectString, int timeout, boolean shutdownAsynchronously)
  {
    this(connectString, timeout, 0, shutdownAsynchronously);
  }

  public ZKConnection(String connectString, int timeout, boolean shutdownAsynchronously, boolean isSymlinkAware)
  {
    this(connectString, timeout, 0, shutdownAsynchronously, isSymlinkAware);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit)
  {
    this(connectString, timeout, retryLimit, false);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit, boolean shutdownAsynchronously)
  {
    this(connectString, timeout, retryLimit, false, null, 0, shutdownAsynchronously);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit, boolean shutdownAsynchronously,
                      boolean isSymlinkAware)
  {
    this(connectString, timeout, retryLimit, false, null, 0, shutdownAsynchronously, isSymlinkAware);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit, boolean exponentialBackoff,
                      ScheduledExecutorService scheduler, long initInterval)
  {
    this(connectString, timeout, retryLimit, exponentialBackoff, scheduler, initInterval, false);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit, boolean exponentialBackoff,
                      ScheduledExecutorService scheduler, long initInterval, boolean shutdownAsynchronously)
  {
    this(connectString, timeout, retryLimit, exponentialBackoff, scheduler, initInterval, shutdownAsynchronously, false);
  }


  public ZKConnection(String connectString, int timeout, int retryLimit, boolean exponentialBackoff,
                      ScheduledExecutorService scheduler, long initInterval, boolean shutdownAsynchronously,
                      boolean isSymlinkAware)
  {
    this(connectString, timeout, retryLimit, exponentialBackoff, scheduler, initInterval, shutdownAsynchronously,
      isSymlinkAware, null, false);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit, boolean exponentialBackoff,
                      ScheduledExecutorService scheduler, long initInterval, boolean shutdownAsynchronously,
                      boolean isSymlinkAware, Function<ZooKeeper,ZooKeeper> zkDecorator)
  {
    this(connectString, timeout, retryLimit, exponentialBackoff, scheduler, initInterval, shutdownAsynchronously,
         isSymlinkAware, zkDecorator, false);
  }

  public ZKConnection(String connectString, int timeout, int retryLimit, boolean exponentialBackoff,
                      ScheduledExecutorService scheduler, long initInterval, boolean shutdownAsynchronously,
                      boolean isSymlinkAware, Function<ZooKeeper,ZooKeeper> zkDecorator, boolean isWaitForConnected)
  {
    _connectString = connectString;
    _timeout = timeout;
    _retryLimit = retryLimit;
    _exponentialBackoff = exponentialBackoff;
    _scheduler = scheduler;
    _initInterval = initInterval;
    _shutdownAsynchronously = shutdownAsynchronously;
    _isSymlinkAware = isSymlinkAware;
    _isWaitForConnected = isWaitForConnected;
    if (zkDecorator == null)
    {
      // if null, just return itself
      zkDecorator = zooKeeper -> zooKeeper;
    }
    _zkDecorator = zkDecorator;
  }

  public void start() throws IOException
  {
    if (_zkRef.get() != null)
    {
      throw new IllegalStateException("Already started");
    }

    final CountDownLatch connectionLatch = new CountDownLatch(1);
    StateListener connectionListener = state -> {
      if (state == Watcher.Event.KeeperState.SyncConnected || state == Watcher.Event.KeeperState.ConnectedReadOnly)
      {
        connectionLatch.countDown();
      }
    };

    if (_isWaitForConnected)
    {
      addStateListener(connectionListener);
    }

    // We take advantage of the fact that the default watcher is always
    // notified of connection state changes (without having to explicitly register)
    // and never notified of anything else.
    Watcher defaultWatcher = new DefaultWatcher();
    ZooKeeper zk = new VanillaZooKeeperAdapter(_connectString, _timeout, defaultWatcher);

    zk = _zkDecorator.apply(zk);
    if (_retryLimit <= 0)
    {
      if (_isSymlinkAware)
      {
        zk = new SymlinkAwareZooKeeper(zk, defaultWatcher, _symlinkSerializer);
        LOG.info("Using symlink aware ZooKeeper without retry");
      }
      else
      {
        // do nothing
        LOG.info("Using vanilla ZooKeeper without retry.");
      }
    }
    else
    {
      zk = new RetryZooKeeper(zk, _retryLimit, _exponentialBackoff, _scheduler, _initInterval);
      if (_isSymlinkAware)
      {
        zk = new SymlinkAwareRetryZooKeeper((RetryZooKeeper)zk, defaultWatcher, _symlinkSerializer);
        LOG.info("Using symlink aware RetryZooKeeper with retry limit set to " + _retryLimit);
      }
      else
      {
        LOG.info("Using RetryZooKeeper with retry limit set to " + _retryLimit);
      }
      if (_exponentialBackoff)
      {
        LOG.info("Exponential backoff enabled. Initial retry interval set to " + _initInterval + " ms.");
      }
      else
      {
        LOG.info("Exponential backoff disabled.");
      }
    }
    LOG.debug("Going to set zkRef");
    if (!_zkRef.compareAndSet(null, zk))
    {
      try
      {
        doShutdown(zk);
      }
      catch (InterruptedException e)
      {
        LOG.warn("Failed to shutdown extra ZooKeeperConnection", e);
      }
      throw new IllegalStateException("Already started");
    }
    LOG.debug("counting down");
    _zkRefLatch.countDown();

    // wait for connection establishes.
    if (_isWaitForConnected)
    {
      try
      {
        if (!connectionLatch.await(_timeout, TimeUnit.MILLISECONDS))
        {
          LOG.error("Error: Timeout waiting for zk connection");
        }
      }
      catch (InterruptedException e)
      {
        LOG.warn("Error: interrupted while waiting for zookeeper connecting", e);
      }
      finally
      {
        removeStateListener(connectionListener);
      }
    }
  }

  public void shutdown() throws InterruptedException
  {
    ZooKeeper zk = _zkRef.get();
    if (zk == null || !_zkRef.compareAndSet(zk, null))
    {
      throw new IllegalStateException("Already shutdown");
    }
    doShutdown(zk);
  }

  private void doShutdown(final ZooKeeper zk) throws InterruptedException
  {
    if (_shutdownAsynchronously)
    {
      Runnable asyncShutdownRunnable = new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            zk.close();
          }
          catch (InterruptedException e)
          {
            LOG.warn("Failed to shutdown ZooKeeperConnection", e);
          }
        }
      };

      LOG.info("Shutting down ZKConnection asynchronously");
      Thread shutdownThread = new Thread(asyncShutdownRunnable, "Asynchronous ZooKeeperConnection shutdown thread");
      shutdownThread.start();
    }
    else
    {
      LOG.info("Shutting down ZKConnection now");
      zk.close();
    }
  }

  private ZooKeeper zk()
  {
    ZooKeeper zk;
    try
    {
      if (!_zkRefLatch.await(_timeout, TimeUnit.MILLISECONDS))
      {
        throw new RuntimeException("Wait for zkRef timed out.");
      }
      LOG.debug("zkRefLatch complete");
      zk = _zkRef.get();
      if (zk == null)
      {
        throw new IllegalStateException("Null zkRef after countdownlatch.");
      }
    }
    catch (InterruptedException e)
    {
      throw new RuntimeException("Got Interrupt Exception while waiting for zk", e);
    }
    return zk;
  }

  public ZooKeeper getZooKeeper()
  {
    return zk();
  }

  public String getConnectString()
  {
    return _connectString;
  }

  public int getTimeout()
  {
    return _timeout;
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
          throw new TimeoutException("timeout expired without state being reached, current state: " + _currentState.name());
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

  public void removeStateListener(StateListener listener)
  {
    synchronized (_mutex)
    {
      _listeners.remove(listener);
    }
  }

  /**
   * checks if the path in zk exist or not. If it doesn't exist, will create the node.
   *
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
   * sets the data associated with a node in ZooKeeper.
   *
   * "Unsafe" signifies that the node data is set unconditionally, overwriting any concurrent changes to the node data.
   * The method will ignore any "BADVERSION" errors from ZooKeeper, up to MAX_RETRIES (default 10).
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
      // Note that in the case where the path is a symlink, we want to call exists() directly on the
      // symlink znode rather than the znode the symlink points to. To avoid such unnecessary re-routing,
      // we should explicitly call zk.rawExists() if the underlying zookeeper client is SymlinkAwareZooKeeper.
      if (zk instanceof SymlinkAwareZooKeeper)
      {
        ((SymlinkAwareZooKeeper) zk).rawExists(path, false, statCallback, null);
      }
      else
      {
        zk.exists(path, false, statCallback, null);
      }
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * removes a node in zookeeper.
   *
   * "Unsafe" signifies that the node data is set unconditionally, overwriting any concurrent changes to the node data.
   * The method will ignore any "BADVERSION" errors from ZooKeeper, up to MAX_RETRIES (default 10).
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
      // Note that in the case where the path is a symlink, we want to call exists() directly on the
      // symlink znode rather than the znode the symlink points to. To avoid such unnecessary re-routing,
      // we should explicitly call zk.rawExists() if the underlying zookeeper client is SymlinkAwareZooKeeper.
      if (zk instanceof SymlinkAwareZooKeeper)
      {
        ((SymlinkAwareZooKeeper) zk).rawExists(path, false, existsCallback, null);
      }
      else
      {
        zk.exists(path, false, existsCallback, null);
      }
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * see {@link #removeNodeUnsafe} but remove recursively
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
      // Note that in the case where the path is a symlink, we want to call getChildren() directly on the
      // symlink znode rather than the znode the symlink points to. To avoid such unnecessary re-routing,
      // we should explicitly call zk.rawGetChildren() if the underlying zookeeper client is SymlinkAwareZooKeeper.
      if (zk instanceof SymlinkAwareZooKeeper)
      {
        ((SymlinkAwareZooKeeper) zk).rawGetChildren(path, false, childCallback, null);
      }
      else
      {
        zk.getChildren(path, false, childCallback, null);
      }
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * create a symbolic link in zookeeper.
   *
   * @param symlinkPath absolute path of symbolic link in zookeeper.
   * @param realPath    the real path that the symbolic link points to.
   * @param callback
   */
  public void createSymlink(final String symlinkPath, final String realPath, final Callback<None> callback)
  {
    if (!SymlinkUtil.containsSymlink(symlinkPath) ||
        SymlinkUtil.firstSymlinkIndex(symlinkPath) < symlinkPath.length())
    {
      callback.onError(new IllegalArgumentException("Cannot create symbolic link for path " + symlinkPath));
      return;
    }

    final ZooKeeper zk = zk();
    final AsyncCallback.StringCallback createCallback = new AsyncCallback.StringCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, String name)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
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
      zk.create(symlinkPath, _symlinkSerializer.toBytes(realPath), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, createCallback, null);
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * set the realPath data associated with a symbolic link.
   *
   * If the symbolic link doesn't exist, it will create one.
   *
   * @param symlinkPath absolute path of symbolic link in zookeeper.
   * @param realPath    the real path that the symbolic link points to.
   * @param callback
   */
  public void setSymlinkData(String symlinkPath, String realPath, Callback<None> callback)
  {
    if (!SymlinkUtil.containsSymlink(symlinkPath) ||
        SymlinkUtil.firstSymlinkIndex(symlinkPath) < symlinkPath.length())
    {
      callback.onError(new IllegalArgumentException("Cannot set data to symbolic link " + symlinkPath));
      return;
    }
    setSymlinkData(symlinkPath, realPath, callback, 0);
  }

  private void setSymlinkData(final String symlinkPath, final String realPath, final Callback<None> callback, final int count)
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
              setSymlinkData(symlinkPath, realPath, callback, count+1);
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
            zk.setData(symlinkPath, _symlinkSerializer.toBytes(realPath), stat.getVersion(), dataCallback, null);
            break;
          case NONODE:
            createSymlink(symlinkPath, realPath, callback);
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    };
    try
    {
      // Note that we want to call exists() directly on the symlink znode rather than the znode the symlink points to.
      // To avoid such unnecessary re-routing, we should explicitly call zk.rawExists() if the underlying zookeeper client
      // is SymlinkAwareZooKeeper.
      if (zk instanceof SymlinkAwareZooKeeper)
      {
        ((SymlinkAwareZooKeeper) zk).rawExists(symlinkPath, null, existsCallback, null);
      }
      else
      {
        zk.exists(symlinkPath, null, existsCallback, null);
      }
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
      ZooKeeper zk;
      try
      {
        zk = zk();
      }
      catch (IllegalStateException e)
      {
        if (_shutdownAsynchronously) {
          // On asynchronous shutdown, this can be a legitimate race.
          LOG.debug("Watched event received after connection shutdown (type {}, state {}.", watchedEvent.getType(),
              watchedEvent.getState());
          return;
        }
        throw e;
      }
      long sessionID = zk.getSessionId();

      if (watchedEvent.getType() == Event.EventType.None)
      {
        Event.KeeperState state = watchedEvent.getState();
        LOG.info("Received state notification {} for session 0x{}", state, Long.toHexString(sessionID));
        Set<StateListener> listeners = Collections.emptySet();
        synchronized (_mutex)
        {
          if (_currentState != state)
          {
            _currentState = state;
            _mutex.notifyAll();
            listeners = new HashSet<>(_listeners);
          }
        }
        for (StateListener listener : listeners)
        {
          listener.notifyStateChange(state);
        }

      }
      else
      {
        LOG.warn("Received unexpected event of type {} for session 0x{}. " +
            "This event is NOT propagated and NONE of the watchers will receive data for this event",
          watchedEvent.getType(), Long.toHexString(sessionID));
      }
    }
  }
}
