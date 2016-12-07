/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.SimpleCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.SizeLimitExceededException;
import com.linkedin.r2.util.Cancellable;

import com.linkedin.r2.util.LinkedDeque;
import com.linkedin.util.ArgumentUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of {@link AsyncPool} where the underlying items may be shared by different getters.
 *
 * @author Sean Sheng
 * @version $Revision: $
 */
public class AsyncSharedPoolImpl<T> implements AsyncPool<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(AsyncSharedPoolImpl.class);

  private static final boolean BAD = true;
  private static final boolean NOT_BAD = false;

  private enum State
  {
    NOT_YET_STARTED,
    RUNNING,
    SHUTTING_DOWN,
    STOPPED
  }

  private final String _name;
  private final AsyncPool.Lifecycle<T> _lifecycle;
  private final ScheduledExecutorService _scheduler;
  private final RateLimiter _rateLimiter;
  private final long _timeoutMills;
  private final int _maxWaiters;

  private volatile ScheduledFuture<?> _reaperTaskFuture = null;

  // ===== All members below are protected by this lock =====
  private final Object _lock = new Object();
  // --------------------------------------------------------

  // The active shared item in the pool
  private final TimedObject<T> _item;

  // The total number of checkouts of the current item
  private int _checkedOut = 0;

  // Keeps track the number of checkouts of non-active pool items
  private final HashMap<T, Integer> _disposedItems = new HashMap<>();

  private final AsyncPoolStatsTracker _statsTracker;
  private final LinkedDeque<Callback<T>> _waiters;
  private State _state = State.NOT_YET_STARTED;
  private Callback<None> _shutdownCallback = null;

  // Use to ensure only one thread is performing the create or destroy operation
  private boolean _isCreateInProgress = false;
  private final HashSet<T> _destroyInProgress = new HashSet<>();
  // ========================================================

  public AsyncSharedPoolImpl(String name, AsyncPool.Lifecycle<T> lifecycle, ScheduledExecutorService scheduler,
      RateLimiter rateLimiter, long timeoutMills, int maxWaiters)
  {
    ArgumentUtil.notNull(name, "name");
    ArgumentUtil.notNull(lifecycle, "lifecycle");
    ArgumentUtil.notNull(scheduler, "scheduler");
    ArgumentUtil.notNull(rateLimiter, "rateLimiter");

    _name = name;
    _lifecycle = lifecycle;
    _scheduler = scheduler;
    _rateLimiter = rateLimiter;
    _timeoutMills = timeoutMills;
    _maxWaiters = maxWaiters;

    _item = new TimedObject<>();
    _waiters = new LinkedDeque<>();
    _statsTracker = new AsyncPoolStatsTracker(
        () -> _lifecycle.getStats(),
        () -> 1,
        () -> 0,
        () -> {
          synchronized (_lock)
          {
            return _item.get() == null ? 0 : 1;
          }
        },
        () -> {
          synchronized (_lock)
          {
            return _checkedOut;
          }
        },
        () -> {
          synchronized (_lock)
          {
            if (_checkedOut > 0)
            {
              return 0;
            }
            return _item.get() == null ? 0 : 1;
          }
        },
        () -> {
          synchronized (_lock)
          {
            return _waiters.size();
          }
        });
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public void start()
  {
    LOG.info("{}: start requested", _name);
    synchronized (_lock)
    {
      if (_state != State.NOT_YET_STARTED)
      {
        throw new IllegalStateException(_name + " is " + _state);
      }
      _state = State.RUNNING;
      if (_timeoutMills > 0)
      {
        long freq = Math.min(_timeoutMills / 10, 1000);
        _reaperTaskFuture = _scheduler.scheduleAtFixedRate(() -> reap(), freq, freq, TimeUnit.MILLISECONDS);
      }
    }
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    ArgumentUtil.notNull(callback, "callback");

    LOG.info("{}: shutdown requested", _name);
    final State state;
    synchronized (_lock)
    {
      state = _state;
      if (state == State.RUNNING)
      {
        _state = State.SHUTTING_DOWN;
        _shutdownCallback = callback;
      }
    }
    if (state != State.RUNNING)
    {
      LOG.error("{}: shutdown requested while pool is not running", _name);
      callback.onError(new IllegalStateException(_name + " is " + _state));
      return;
    }
    doAttemptShutdown();
  }

  @Override
  public Collection<Callback<T>> cancelWaiters()
  {
    synchronized (_lock)
    {
      List<Callback<T>> cancelled = new ArrayList<>(_waiters.size());
      for (Callback<T> item; (item = _waiters.poll()) != null;)
      {
        cancelled.add(item);
      }
      return cancelled;
    }
  }

  @Override
  public Cancellable get(Callback<T> callback)
  {
    ArgumentUtil.notNull(callback, "callback");

    final TimeTrackingCallback timeTrackingCallback = new TimeTrackingCallback(callback);
    final LinkedDeque.Node<Callback<T>> node;
    T item = null;
    boolean create = false;
    while (true)
    {
      final State state;
      synchronized (_lock)
      {
        state = _state;
        if (state == State.RUNNING)
        {
          item = _item.get();
          if (item == null)
          {
            node = _waiters.size() < _maxWaiters ? _waiters.addLastNode(timeTrackingCallback) : null;
            if (_isCreateInProgress)
            {
              LOG.debug("{}: item creation is in progress", _name);
            }
            else
            {
              _isCreateInProgress = true;
              create = true;
            }
            break;
          }

          _checkedOut++;
          _statsTracker.sampleMaxCheckedOut();
        }
      }
      if (state != State.RUNNING)
      {
        // Defer execution of the callback until we are out of the synchronized block
        timeTrackingCallback.onError(new IllegalStateException(_name + " is " + _state));
        return () -> false;
      }
      // At this point, we know a connection has been created, validate the connection
      // through the item lifecycle before passing back to user callback
      if (_lifecycle.validateGet(item))
      {
        timeTrackingCallback.onSuccess(item);
        return () -> false;
      }
      boolean disposed;
      synchronized (_lock)
      {
        // The connection has gone bad so we proceed to destroy it
        disposed = doDispose(item);
      }
      if (disposed)
      {
        doDestroy(item, BAD, () -> {});
      }
    }
    if (node == null)
    {
      // This is a recoverable exception. User can simply retry the failed get() operation.
      timeTrackingCallback.onError(
          new SizeLimitExceededException("AsyncPool " + _name + " reached maximum waiter size: " + _maxWaiters));
      return () -> false;
    }
    // The pool is currently empty we need to construct a new item
    if (create)
    {
      doCreate();
    }
    return () -> {
      synchronized (_lock)
      {
        return _waiters.removeNode(node) != null;
      }
    };
  }

  @Override
  public void put(final T item)
  {
    LOG.debug("{}: putting back an item {}", _name, item);
    boolean disposed = false;
    boolean returned = false;
    synchronized (_lock)
    {
      if (_item.get() == null || _item.get() != item)
      {
        LOG.debug("{}: given item {} does not reference match current item {}", new Object[]{_name, item, _item.get()});
        disposed = doDispose(item);
      }
      else
      {
        if (_lifecycle.validatePut(item))
        {
          LOG.debug("{}: returning an item {} that passed validation", _name, item);
          returned = doReturn(item);
        }
        else
        {
          LOG.debug("{}: disposing an item {} that failed validation", _name, item);
          disposed = doDispose(item);
        }
      }
    }
    if (disposed)
    {
      doDestroy(item, BAD, () -> doAttemptShutdown());
    }
    if (returned)
    {
      doAttemptShutdown();
    }
  }

  @Override
  public void dispose(T item)
  {
    LOG.error("{}: disposing an item {}", _name, item);
    boolean disposed;
    synchronized (_lock)
    {
      disposed = doDispose(item);
    }
    if (disposed)
    {
      doDestroy(item, BAD, () -> doAttemptShutdown());
    }
  }

  @Override
  public PoolStats getStats()
  {
    synchronized (_lock)
    {
      return _statsTracker.getStats();
    }
  }

  /**
   * Destroys the underlying object if the item is expired. This method
   * does not invoke user callback.
   */
  private void reap()
  {
    final T item;
    synchronized (_lock)
    {
      item = _item.get();
      if (item == null)
      {
        LOG.debug("{}: nothing to reap", _name);
        return;
      }

      if (_checkedOut > 0)
      {
        LOG.debug("{}: item still has {} outstanding checkouts", _name, _checkedOut);
        _item.renew();
        return;
      }

      if (!_item.expired())
      {
        LOG.debug("{}: item is still valid", _name);
        return;
      }

      // Current active item has timed out
      _statsTracker.incrementTimedOut();
      _item.reset();
    }

    LOG.debug("{}: item timed out, proceed to destroy", _name);
    doDestroy(item, NOT_BAD, () -> doAttemptShutdown());
  }

  /**
   * Returns an item that has completed a full lifecycle. Item returned must be the same as the currently
   * active item. An IllegalArgumentException is thrown if a reference other than the current active item
   * is returned. This method does not invoke user callback.
   *
   * @return {@code true} if the last checkout of the item is return, {@code false} otherwise
   */
  private boolean doReturn(T item)
  {
    // An item made it through a complete request lifecycle
    _rateLimiter.setPeriod(0);
    synchronized (_lock)
    {
      if (_item.get() == null || _item.get() != item)
      {
        LOG.debug("{}: given item {} does not reference match current item {}", new Object[]{_name, item, _item.get()});
        throw new IllegalArgumentException("Returning an item that is not the same as the current active item");
      }
      if (_checkedOut == 0)
      {
        throw new IllegalArgumentException("Decrementing checked out when it's already at 0");
      }
      _checkedOut--;
      return _checkedOut == 0;
    }
  }

  /**
   * Checks if conditions are met for shutdown.
   *
   * 1. Shutdown has been initiated
   * 2. No outstanding checkouts of the active item
   * 3. No outstanding checkouts of pending disposed items
   *
   * If all above conditions are met, performs the actual tasks to shutdown the pool.
   *
   * This method invokes external callback so do not call while holding the lock.
   */
  private void doAttemptShutdown()
  {
    LOG.debug("{}: attempts to shutdown", _name);
    final Callback<None> shutdownCallback;
    final ScheduledFuture<?> reaperTaskFuture;
    synchronized (_lock)
    {
      shutdownCallback = _shutdownCallback;
      reaperTaskFuture = _reaperTaskFuture;

      if (_state != State.SHUTTING_DOWN)
      {
        LOG.debug("{}: current state is {}", _name, _state);
        return;
      }

      if (_checkedOut > 0)
      {
        LOG.info("{}: awaiting {} more outstanding checkouts", _name, _checkedOut);
        return;
      }

      if (_disposedItems.size() > 0)
      {
        LOG.info("{}: awaiting {} more disposed items {}",
            new Object[] { _name, _disposedItems.keySet().size(), _disposedItems });
        return;
      }

      LOG.info("{}: shutdown conditions are met", _name);
      _state = State.STOPPED;
      _shutdownCallback = null;
      _reaperTaskFuture = null;
    }
    if (reaperTaskFuture != null)
    {
      LOG.debug("{}: attempt to cancel reaper task", _name);
      reaperTaskFuture.cancel(false);
    }

    LOG.info("{}: shutdown complete", _name);
    shutdownCallback.onSuccess(None.none());
  }

  /**
   * Asynchronously creates a new item through its life cycle using a
   * {@link com.linkedin.r2.transport.http.client.RateLimiter.Task}. Method guarantees a maximum of
   * one thread is allowed to create the new item and at most one item is created.
   * This method does not invoke user callback.
   */
  private void doCreate()
  {
    LOG.debug("{}: creating a new item", _name);
    _rateLimiter.submit(callback -> _lifecycle.create(new Callback<T>()
    {
      @Override
      public void onSuccess(T item)
      {
        LOG.debug("{}: item creation succeeded", _name);
        final List<Callback<T>> waiters = new ArrayList<>();
        synchronized (_lock)
        {
          _statsTracker.incrementCreated();

          // Takes a snapshot of waiters and clears all waiters
          int size = _waiters.size();
          _checkedOut += size;
          _statsTracker.sampleMaxCheckedOut();
          IntStream.range(0, size).forEach(i -> waiters.add(_waiters.poll()));

          // Sets the singleton item to be the newly created item
          _item.set(item);
          _statsTracker.sampleMaxPoolSize();
          _isCreateInProgress = false;
        }

        // Invokes #onSuccess on each waiter callback
        waiters.stream().forEach(waiter -> {
          try
          {
            waiter.onSuccess(item);
          }
          catch (Exception ex)
          {
            LOG.error("Encountered error while invoking success waiter callback", ex);
          }
        });
        callback.onDone();
      }

      @Override
      public void onError(final Throwable e)
      {
        LOG.error("{}: item creation failed", _name, e);

        // Note we drain all waiters and cancel all pending creates if a create fails.
        // When a create fails, rate-limiting logic will be applied. In this case,
        // we may be initiating creations at a lower rate than incoming requests. While
        // creations are suppressed, it is better to deny all waiters and let them see
        // the real reason (this exception) rather than keep them around to eventually
        // get an unhelpful timeout error
        _rateLimiter.incrementPeriod();

        // Implementation guarantees that there is no pending task at this point because
        // only one thread can call lifecycle create at a time
        Collection<RateLimiter.Task> tasks = _rateLimiter.cancelPendingTasks();

        final Collection<Callback<T>> waiters;
        synchronized (_lock)
        {
          waiters = cancelWaiters();
          _statsTracker.incrementCreateErrors();
          _isCreateInProgress = false;
        }

        // Notifies the waiters with the current exception
        waiters.stream().forEach(waiter -> {
          try
          {
            waiter.onError(e);
          }
          catch (Exception ex)
          {
            LOG.error("Encountered error while invoking error waiter callback", ex);
          }
        });

        callback.onDone();
      }
    }));
  }

  /**
   * Disposes a given item. If the item is the current active item, it is moved to the disposed buffer and
   * the current active item is set to {@code null}. If the item is not currently active but is present in
   * the disposed item buffer, then checked out count is decremented. When checked out count is decremented
   * to zero, the item is destroyed. If the item is neither active nor present in the disposed buffer, an
   * IllegalArgumentException is thrown because the item was not originally checked out from the pool. A
   * maximum of one thread is allowed to destroy the connection.
   *
   * @param item Item to be disposed
   * @return {@code true} if the given item should be destroyed, {@code false} otherwise.
   */
  private boolean doDispose(T item)
  {
    if (item == null)
    {
      LOG.error("{}: item is null so nothing to dispose", _name);
      return false;
    }

    synchronized (_lock)
    {
      if (_item.get() != null && _item.get() == item)
      {
        _disposedItems.put(_item.get(), _checkedOut);
        _item.reset();
        _checkedOut = 0;
      }
      if (!_disposedItems.containsKey(item))
      {
        throw new IllegalArgumentException(
            "Disposing a previously destroyed item or an item that was not checked out from the pool");
      }
      int count = _disposedItems.get(item) - 1;
      if (count == 0)
      {
        _disposedItems.remove(item);
        if (_destroyInProgress.contains(item))
        {
          LOG.debug("{}: item {} destroy is in progress", _name, item);
          return false;
        }
        // Marks this item as currently being destroyed
        _destroyInProgress.add(item);
        return true;
      }
      else
      {
        _disposedItems.put(item, count);
        return false;
      }
    }
  }

  /**
   * Asynchronously destroys an item through its lifecycle using
   * {@link com.linkedin.r2.transport.http.client.RateLimiter.Task}. Invokes the specified callback
   * after the destroy operation is done.
   *
   * @param item item to be destroyed
   * @param bad indicates whether the item is in an error state
   * @param callback invokes after #doDestroy is completed regardless of success status
   */
  private void doDestroy(final T item, final boolean bad, SimpleCallback callback)
  {
    LOG.debug("{}: destroying an item {}", _name, item);
    if (bad)
    {
      _statsTracker.incrementBadDestroyed();
    }
    _lifecycle.destroy(item, bad, new Callback<T>()
    {
      @Override
      public void onSuccess(T item)
      {
        try
        {
          synchronized (_lock)
          {
            _statsTracker.incrementDestroyed();
            _destroyInProgress.remove(item);
          }
        }
        finally
        {
          callback.onDone();
        }
      }

      @Override
      public void onError(Throwable e)
      {
        LOG.error("{}: failed to destroy an item", _name, e);
        try
        {
          synchronized (_lock)
          {
            _statsTracker.incrementDestroyErrors();
            _destroyInProgress.remove(item);
          }
        }
        finally
        {
          callback.onDone();
        }
      }
    });
  }

  /**
   * Associates an object with a create timestamp and provides utility methods for
   * accessing and modifying the item and associated timestamp. Implementation is not thread safe. Use
   * external synchronization if needed.
   */
  private final class TimedObject<T>
  {
    private T _item = null;
    private long _timestamp = 0;

    public final T get()
    {
      return _item;
    }

    public final long timestamp()
    {
      return _timestamp;
    }

    public void set(T item)
    {
      _item = item;
      _timestamp = System.currentTimeMillis();
    }

    public void renew()
    {
      _timestamp = System.currentTimeMillis();
    }

    public final void reset()
    {
      _item = null;
      _timestamp = 0;
    }

    public boolean expired()
    {
      return _timestamp < (System.currentTimeMillis() - _timeoutMills);
    }
  }

  /**
   * Tracks the time in between time of creation and one of callback method is invoked.
   */
  private class TimeTrackingCallback implements Callback<T>
  {
    private final long _startTime;
    private final Callback<T> _callback;

    public TimeTrackingCallback(Callback<T> callback)
    {
      _callback = callback;
      _startTime = System.currentTimeMillis();
    }

    @Override
    public void onError(Throwable e)
    {
      synchronized (_lock)
      {
        _statsTracker.trackWaitTime(System.currentTimeMillis() - _startTime);
      }
      _callback.onError(e);
    }

    @Override
    public void onSuccess(T item)
    {
      synchronized (_lock)
      {
        _statsTracker.trackWaitTime(System.currentTimeMillis() - _startTime);
      }
      _callback.onSuccess(item);
    }
  }
}
