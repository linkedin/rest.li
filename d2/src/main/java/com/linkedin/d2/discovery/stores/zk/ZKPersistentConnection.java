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

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class provides a simple persistent ZooKeeper connection that automatically reconnects
 * to the ensemble after session expiration.
 *
 * Currently implemented in terms of ZKConnection.  Eventually, ZKConnection should probably
 * go away and this class should deal directly with ZooKeeper.
 *
 * Currently it supports only a static set of listeners specified at construction time.  Eventually
 * it may be desirable to allow listeners to be added at runtime after the connection is already
 * established.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKPersistentConnection
{
  private static final Logger LOG = LoggerFactory.getLogger(ZKPersistentConnection.class);

  private final String _connectionString;
  private final int _sessionTimeout;
  private final boolean _shutdownAsynchronously;
  private final boolean _isSymlinkAware;

  private final Object _mutex = new Object();
  private ZKConnection _zkConnection;
  private Set<EventListener> _listeners;
  private State _state = State.INIT;

  private enum State {
    INIT,
    STARTED,
    STOPPED
  }

  public enum Event {
    /**
     * A new ZooKeeper session has been started with the server.  The client must reread all
     * data, reset all watches, and rebuild any local caches, because watch events may have
     * been missed.
     *
     * This event occurs on the first connection, or upon creation of a new connection after
     * the previous one expired.
     */
    SESSION_ESTABLISHED,

    /**
     * The ZooKeeper ensemble is currently unreachable.
     */
    DISCONNECTED,

    /**
     * The ZooKeeper ensemble is currently reachable and the session remains valid.
     */
    CONNECTED,

    /**
     * The session has expired.  New session establishment is underway.
     */
    SESSION_EXPIRED
  }

  public interface EventListener
  {
    void notifyEvent(Event event);
  }

  public ZKPersistentConnection(String connect, int timeout, Collection<? extends EventListener> listeners)
  {
    this(connect, timeout, listeners, false);
  }

  public ZKPersistentConnection(String connect, int timeout, Collection<? extends EventListener> listeners, boolean shutdownAsynchronously)
  {
    this(connect, timeout, listeners, shutdownAsynchronously, false);
  }

  public ZKPersistentConnection(String connect, int timeout, Collection<? extends EventListener> listeners,
                                boolean shutdownAsynchronously, boolean isSymlinkAware)
  {
    _connectionString = connect;
    _sessionTimeout = timeout;
    _shutdownAsynchronously = shutdownAsynchronously;
    _isSymlinkAware = isSymlinkAware;
    _zkConnection = new ZKConnection(connect, timeout, shutdownAsynchronously, isSymlinkAware);
    _zkConnection.addStateListener(new Listener());
    _listeners = new HashSet<EventListener>(listeners);

    // NB: to support adding EventListeners after the connection is started, must consider the
    // following:
    // 1. At the moment the registration occurs, the session may already be connected.  We will
    // need to deliver a "dummy" SESSION_ESTABLISHED to the listener (otherwise how does it
    // know to start talking to ZooKeeper?)
    // 2. Events that come to us from the ZooKeeper event thread (via the watcher) are always
    // delivered in the correct order.  If we deliver a dummy SESSION_ESTABLISHED event to the
    // listener, it could arrive out of order (e.g. after a SESSION_EXPIRED that really occurred
    // before).
  }

  public void start() throws IOException
  {
    synchronized (_mutex)
    {
      if (_state != State.INIT)
      {
        throw new IllegalStateException("Can not start ZKConnection when " + _state);
      }
      _state = State.STARTED;
      _listeners = Collections.unmodifiableSet(_listeners);
      _zkConnection.start();
    }
  }

  public void shutdown() throws InterruptedException
  {
    synchronized (_mutex)
    {
      if (_state != State.STARTED)
      {
        throw new IllegalStateException("Can not shutdown ZKConnection when " + _state);
      }
      _state = State.STOPPED;
      _zkConnection.shutdown();
    }
  }

  public ZooKeeper getZooKeeper()
  {
    synchronized (_mutex)
    {
      return _zkConnection.getZooKeeper();
    }
  }

  public ZKConnection getZKConnection()
  {
    synchronized (_mutex)
    {
      return _zkConnection;
    }
  }

  private class Listener implements ZKConnection.StateListener
  {
    private long _sessionId;

    @Override
    public void notifyStateChange(Watcher.Event.KeeperState state)
    {
      long sessionId = getZooKeeper().getSessionId();
      LOG.info("Got event {} for session 0x{}", state, Long.toHexString(sessionId));

      boolean newSession = false;
      if (state == Watcher.Event.KeeperState.SyncConnected)
      {
        if (sessionId != _sessionId)
        {
          newSession = true;
          _sessionId = sessionId;
        }
      }

      switch (state)
      {
        case SyncConnected:
          deliver(newSession ? Event.SESSION_ESTABLISHED : Event.CONNECTED);
          break;
        case Disconnected:
          deliver(Event.DISCONNECTED);
          break;
        case Expired:
          deliver(Event.SESSION_EXPIRED);
          break;
      }

      if (state == Watcher.Event.KeeperState.Expired)
      {
        try
        {
          synchronized (_mutex)
          {
            if (_state == State.STARTED)
            {
              _zkConnection.shutdown();
              _zkConnection =
                  new ZKConnection(_connectionString, _sessionTimeout, _shutdownAsynchronously, _isSymlinkAware);
              _zkConnection.addStateListener(new Listener());
              _zkConnection.start();
            }
          }
        }
        catch (InterruptedException e)
        {
          LOG.error("Failed to shutdown ZKConnection after expiration", e);
        }
        catch (IOException e)
        {
          LOG.error("Failed to restart ZKConnection after expiration", e);
        }
      }
    }

    private void deliver(Event event)
    {
      for (EventListener listener : _listeners)
      {
        listener.notifyEvent(event);
      }
    }
  }
}
