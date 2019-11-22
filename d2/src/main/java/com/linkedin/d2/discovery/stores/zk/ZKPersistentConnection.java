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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final Object _mutex = new Object();
  private final ZKConnectionBuilder _zkConnectionBuilder;
  private ZKConnection _zkConnection;
  private Set<EventListener> _listeners;
  private State _state = State.INIT;

  //the number of users currently having the connection running
  private AtomicInteger _activeUserCount;
  //the number of users who obtained the connection from the SharedZkConnectionProvider during construction.
  private AtomicInteger _registeredUserCount;
  //the flag to indicate that the connection has been forcefully shutdown by framework
  private volatile boolean _hasForcefullyShutdown;

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
     * The session has expired.  New session establishment is underway.
     */
    SESSION_EXPIRED,

    /**
     * The ZooKeeper ensemble is currently unreachable.
     * After this event, the connection could get re-connected and notified with a CONNECTED event
     */
    DISCONNECTED,

    /**
     * The ZooKeeper ensemble is currently reachable again and the session remains valid.
     * This event is received only after a DISCONNECTED and not on the first connection established.
     *
     * If watches were set, there is no need to recreate them since they will receive all the events they missed
     * while the connection was in DISCONNECTED state
     */
    CONNECTED
  }

  public interface EventListener
  {
    void notifyEvent(Event event);
  }

  /**
   * Helper class to listen to the events coming from ZK
   */
  public static class EventListenerNotifiers implements EventListener
  {
    public void notifyEvent(Event event)
    {
      switch (event)
      {
        case SESSION_ESTABLISHED:
        {
          sessionEstablished(event);
          break;
        }
        case SESSION_EXPIRED:
        {
          sessionExpired(event);
          break;
        }
        case CONNECTED:
        {
          connected(event);
          break;
        }
        case DISCONNECTED:
          disconnected(event);
          break;
      }
    }

    public void sessionEstablished(Event event)
    {
    }

    public void sessionExpired(Event event)
    {
    }

    public void disconnected(Event event)
    {
    }

    public void connected(Event event)
    {
    }
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
    this(connect, timeout, listeners, shutdownAsynchronously, isSymlinkAware, false);
  }

  public ZKPersistentConnection(String connect, int timeout, Collection<? extends EventListener> listeners,
                                boolean shutdownAsynchronously, boolean isSymlinkAware, boolean waitForConnected)
  {
    this(new ZKConnectionBuilder(connect).setTimeout(timeout)
      .setShutdownAsynchronously(shutdownAsynchronously).setIsSymlinkAware(isSymlinkAware).setWaitForConnected(waitForConnected));
    addListeners(listeners);
  }

  public ZKPersistentConnection(ZKConnectionBuilder zkConnectionBuilder)
  {
    _zkConnectionBuilder = zkConnectionBuilder;
    _zkConnection = _zkConnectionBuilder.build();
    _zkConnection.addStateListener(new Listener());
    _listeners = new HashSet<>();
    _activeUserCount = new AtomicInteger(0);
    _registeredUserCount = new AtomicInteger(0);
    _hasForcefullyShutdown = false;
  }

  /**
   * Allows to add other listeners ONLY before the connection is started
   */
  public void addListeners(Collection<? extends EventListener> listeners)
  {
    synchronized (_mutex)
    {
      // NB: to support adding EventListeners after the connection is started, must consider the
      // following:
      // 1. At the moment the registration occurs, the session may already be connected.  We will
      // need to deliver a "dummy" SESSION_ESTABLISHED to the listener (otherwise how does it
      // know to start talking to ZooKeeper?)
      // 2. Events that come to us from the ZooKeeper event thread (via the watcher) are always
      // delivered in the correct order.  If we deliver a dummy SESSION_ESTABLISHED event to the
      // listener, it could arrive out of order (e.g. after a SESSION_EXPIRED that really occurred
      // before).
      if (_state != State.INIT)
      {
        throw new IllegalStateException("Listeners can be added only before connection starts, current state: " + _state);
      }
      _listeners.addAll(listeners);
    }
  }

  /**
   * Called when an additional user requested the connection
   */
  public void incrementShareCount()
  {
    _registeredUserCount.incrementAndGet();
  }

  public void start() throws IOException
  {
    synchronized (_mutex)
    {
      _activeUserCount.getAndIncrement();
      if (_state != State.INIT)
      {
        // if it is not the first time we started it, we just increment the active user count and return
        return;
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
      if (_hasForcefullyShutdown)
      {
        LOG.warn("The connection has already been forcefully shutdown");
        return;
      }
      if (_state != State.STARTED)
      {
        throw new IllegalStateException("Can not shutdown ZKConnection when " + _state);
      }
      int remainingActiveUserCount = _activeUserCount.decrementAndGet();
      int remainingRegisteredUserCount = _registeredUserCount.decrementAndGet();
      if (remainingActiveUserCount > 0 || remainingRegisteredUserCount > 0)
      {
        //connection can only be shut down if
        // 1. no one is using it
        // 2. everyone who has shared it has finished using it.
        return;
      }
      _state = State.STOPPED;
      _zkConnection.shutdown();
    }
  }

  /**
   * This method is intended to be called at the end of framework lifecycle to ensure graceful shutdown, normal shutdown operation should
   * be carried out with the method above.
   */
  public void forceShutdown() throws InterruptedException
  {
    synchronized (_mutex)
    {
      if (_state != State.STARTED)
      {
        LOG.warn("Unnecessary to forcefully shutdown a zkPersistentConnection that is either not started or already stopped");
        return;
      }
      _hasForcefullyShutdown = true;
      int remainingActiveUserCount = _activeUserCount.get();
      if (remainingActiveUserCount != 0)
      {
        LOG.warn("Forcefully shutting down ZkPersistentConnection when there still are" + remainingActiveUserCount
            + " active users");
      }
      _state = State.STOPPED;
      try
      {
        _zkConnection.shutdown();
      } catch (IllegalStateException e)
      {
        LOG.warn("trying to forcefully shutdown zk connection but encountered:" + e.getMessage());
      }
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

  public boolean isConnectionStarted()
  {
    synchronized (_mutex)
    {
      return _state == State.STARTED;
    }
  }

  public boolean isConnectionStopped()
  {
    synchronized (_mutex)
    {
      return _state == State.STOPPED;
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
              _zkConnection = _zkConnectionBuilder.build();
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
