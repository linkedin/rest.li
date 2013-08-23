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

package com.linkedin.r2.transport.http.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.r2.util.LinkedDeque;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class AsyncPoolImpl<T> implements AsyncPool<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(AsyncPoolImpl.class);

  // Configured
  private final String _poolName;
  private final Lifecycle<T> _lifecycle;
  private final int _maxSize;
  private final long _idleTimeout;
  private final ScheduledExecutorService _timeoutExecutor;
  private final int _minSize;
  private volatile ScheduledFuture<?> _objectTimeoutFuture;

  private enum State { NOT_YET_STARTED, RUNNING, SHUTTING_DOWN, STOPPED }

  public enum Strategy { MRU, LRU };
  private final Strategy _strategy;

  // All members below are protected by this lock
  // Never call user code (callbacks) while holding this lock
  private final Object _lock = new Object();
  // Including idle, checked out, and creations/destructions in progress
  private int _poolSize = 0;
  // Unused objects live here, sorted by age.
  // The first object is the least recently added object.
  private final Deque<TimedObject<T>> _idle = new LinkedList<TimedObject<T>>();
  // When no unused objects are available, callbacks live here while they wait
  // for a new object (either returned by another user, or newly created)
  private final LinkedDeque<Callback<T>> _waiters = new LinkedDeque<Callback<T>>();
  private Throwable _lastCreateError = null;
  private State _state = State.NOT_YET_STARTED;
  private Callback<None> _shutdownCallback = null;

  // Statistics for each pool, retrieved with getStats()
  // See AsyncPoolStats for details
  // These are total counts over the entire lifetime of the pool
  private int _totalCreated = 0;
  private int _totalDestroyed = 0;
  private int _totalCreateErrors = 0;
  private int _totalDestroyErrors = 0;
  private int _totalBadDestroyed = 0;
  private int _totalTimedOut = 0;
  // These counters reset on each call to getStats()
  private int _sampleMaxCheckedOut = 0;
  private int _sampleMaxPoolSize = 0;
  // These are instantaneous values
  private int _checkedOut = 0;

  /**
   * Creates an AsyncPoolImpl using a MRU (most recently used) strategy
   * of reusing pool objects. The minimum number of pool objects is set
   * to zero. This is a sensible configuration for talking to a single host.
   *
   * @param name Pool name, used in logs and statistics.
   * @param lifecycle The lifecycle used to create and destroy pool objects.
   * @param maxSize The maximum number of objects in the pool.
   * @param idleTimeout The number of milliseconds before an idle pool
   *                    object may be destroyed.
   * @param timeoutExecutor A ScheduledExecutorService that will be used to
   *                        periodically timeout objects.
   */
  public AsyncPoolImpl(String name,
                       Lifecycle<T> lifecycle,
                       int maxSize,
                       long idleTimeout,
                       ScheduledExecutorService timeoutExecutor)
  {
    _poolName = name;
    _lifecycle = lifecycle;
    _maxSize = maxSize;
    _idleTimeout = idleTimeout;
    _timeoutExecutor = timeoutExecutor;
    _strategy = Strategy.MRU;
    _minSize = 0;
  }

  /**
   * Creates an AsyncPoolImpl with a specified strategy of
   * returning pool objects and a minimum pool size.
   *
   * Supported strategies are MRU (most recently used) and LRU
   * (least recently used).
   *
   * MRU is sensible for communicating with a single host in order
   * to minimize the number of idle pool objects.
   *
   * LRU, in combination with a minimum pool size, is sensible for
   * communicating with a hardware load balancer that directly maps
   * persistent connections to hosts. In this case, the AsyncPoolImpl
   * balances requests evenly across the pool.
   *
   * @param name Pool name, used in logs and statistics.
   * @param lifecycle The lifecycle used to create and destroy pool objects.
   * @param maxSize The maximum number of objects in the pool.
   * @param idleTimeout The number of milliseconds before an idle pool object
   *                    may be destroyed.
   * @param timeoutExecutor A ScheduledExecutorService that will be used to
   *                        periodically timeout objects.
   * @param strategy The strategy used to return pool objects.
   * @param minSize Minimum number of objects in the pool. Set to zero for
   *                no minimum.
   */
  public AsyncPoolImpl(String name,
                       Lifecycle<T> lifecycle,
                       int maxSize,
                       long idleTimeout,
                       ScheduledExecutorService timeoutExecutor,
                       Strategy strategy,
                       int minSize)
  {
    _poolName = name;
    _lifecycle = lifecycle;
    _maxSize = maxSize;
    _idleTimeout = idleTimeout;
    _timeoutExecutor = timeoutExecutor;
    _strategy = strategy;
    _minSize = minSize;
  }

  @Override
  public String getName()
  {
    return _poolName;
  }

  @Override
  public void start()
  {
    synchronized (_lock)
    {
      if (_state != State.NOT_YET_STARTED)
      {
        throw new IllegalStateException(_poolName + " is " + _state);
      }
      _state = State.RUNNING;
      if (_idleTimeout > 0)
      {
        long freq = Math.min(_idleTimeout / 10, 1000);
        _objectTimeoutFuture = _timeoutExecutor.scheduleAtFixedRate(new Runnable() {
          @Override
          public void run()
          {
            timeoutObjects();
          }
        }, freq, freq, TimeUnit.MILLISECONDS);
      }
    }

    // Make the minimum required number of connections now
    for(int i = 0; i < _minSize; i++)
    {
      if(shouldCreate())
      {
        create();
      }
    }
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
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
      // Retest state outside the synchronized block, since we don't want to invoke this
      // callback inside a synchronized block
      callback.onError(new IllegalStateException(_poolName + " is " + _state));
      return;
    }
    LOG.info("{}: {}", _poolName, "shutdown requested");
    shutdownIfNeeded();
  }

  @Override
  public Collection<Callback<T>> cancelWaiters()
  {
    synchronized (_lock)
    {
      List<Callback<T>> cancelled = new ArrayList<Callback<T>>(_waiters.size());
      for (Callback<T> item; (item = _waiters.poll()) != null;)
      {
         cancelled.add(item);
      }
      return cancelled;
    }
  }

  @Override
  public Cancellable get(final Callback<T> callback)
  {
    // getter needs to add to wait queue atomically with check for empty pool
    // putter needs to add to pool atomically with check for empty wait queue
    boolean create;
    final LinkedDeque.Node<Callback<T>> node;
    for (;;)
    {
      TimedObject<T> obj = null;
      final State state;
      synchronized (_lock)
      {
        state = _state;
        if (state == State.RUNNING)
        {
          if(_strategy == Strategy.LRU)
          {
            obj = _idle.pollFirst();
          }
          else
          {
            obj = _idle.pollLast();
          }
          if (obj == null)
          {
            // No objects available; add to waiter list and break out of loop
            node = _waiters.addLastNode(callback);
            create = shouldCreate();
            break;
          }
        }
      }
      if (state != State.RUNNING)
      {
        // Defer execution of the callback until we are out of the synchronized block
        callback.onError(new IllegalStateException(_poolName + " is " + _state));
        return null;
      }
      T rawObj = obj.get();
      if (_lifecycle.validateGet(rawObj))
      {
        trc("dequeued an idle object");
        // Valid object; done
        synchronized (_lock)
        {
          _checkedOut++;
          _sampleMaxCheckedOut = Math.max(_checkedOut, _sampleMaxCheckedOut);
        }
        callback.onSuccess(rawObj);
        return null;
      }
      // Invalid object, discard it and keep trying
      destroy(rawObj, true);
      trc("dequeued and disposed an invalid idle object");
    }
    trc("enqueued a waiter");
    if (create)
    {
      create();
    }
    return new Cancellable()
    {
      @Override
      public boolean cancel()
      {
        synchronized (_lock)
        {
          return _waiters.removeNode(node) != null;
        }
      }
    };
  }

  @Override
  public void put(T obj)
  {
    synchronized (_lock)
    {
      _checkedOut--;
    }
    if (!_lifecycle.validatePut(obj))
    {
      destroy(obj, true);
      return;
    }
    add(obj);
  }

  private void add(T obj)
  {
    final Callback<None> shutdown;
    Callback<T> waiter;
    synchronized (_lock)
    {
      // If we have waiters, the idle list must already be empty.
      // Therefore, immediately reusing the object is valid with
      // both MRU and LRU strategies.
      waiter = _waiters.poll();
      if (waiter == null)
      {
        _idle.offerLast(new TimedObject<T>(obj));
      }
      else
      {
        _checkedOut++;
        _sampleMaxCheckedOut = Math.max(_checkedOut, _sampleMaxCheckedOut);
      }
      shutdown = checkShutdownComplete();
    }

    if (waiter != null)
    {
      trc("dequeued a waiter");
      // TODO probably shouldn't execute the getter's callback on the putting thread
      // If this callback is moved to another thread, make sure shutdownComplete does not get
      // invoked until after this callback is completed
      waiter.onSuccess(obj);
    }
    else
    {
      trc("enqueued an idle object");
    }
    if (shutdown != null)
    {
      // Now that the final user callback has been executed, pool shutdown is complete
      finishShutdown(shutdown);
    }
  }

  @Override
  public void dispose(T obj)
  {
    synchronized (_lock)
    {
      _checkedOut--;
    }
    destroy(obj, true);
  }

  @Override
  public AsyncPoolStats getStats()
  {
    // get a copy of the stats
    synchronized (_lock)
    {
      AsyncPoolStats stats = new AsyncPoolStats(
        _totalCreated,
        _totalDestroyed,
        _totalCreateErrors,
        _totalDestroyErrors,
        _totalBadDestroyed,
        _totalTimedOut,
        _checkedOut,
        _maxSize,
        _minSize,
        _poolSize,
        _sampleMaxCheckedOut,
        _sampleMaxPoolSize
      );
      _sampleMaxCheckedOut = _checkedOut;
      _sampleMaxPoolSize = _poolSize;
      return stats;
    }
  }

  private void destroy(T obj, boolean bad)
  {
    if(bad)
    {
      synchronized(_lock)
      {
        _totalBadDestroyed++;
      }
    }
    trc("disposing a pooled object");
    _lifecycle.destroy(obj, bad, new Callback<T>() {
      @Override
      public void onSuccess(T t) {
        boolean create;
        synchronized (_lock)
        {
          _totalDestroyed++;
          create = objectDestroyed();
        }
        if (create)
        {
          create();
        }
      }

      @Override
      public void onError(Throwable e) {
        boolean create;
        synchronized (_lock) {
          _totalDestroyErrors++;
          create = objectDestroyed();
        }
        if (create) {
          create();
        }
        // TODO log this error!
      }
    });
  }

  /**
   * This method is safe to call while holding the lock.
   * @return true if another object creation should be initiated
   */
  private boolean objectDestroyed()
  {
    boolean create;
    synchronized (_lock)
    {
      _poolSize--;
      create = shouldCreate();
      shutdownIfNeeded();
    }
    return create;
  }

  /**
   * This method is safe to call while holding the lock.  DO NOT
   * call any callbacks in this method!
   * @return true if another object creation should be initiated.
   */
  private boolean shouldCreate()
  {
    boolean result = false;
    synchronized (_lock)
    {
      if (_state == State.RUNNING)
      {
        if (_poolSize >= _maxSize)
        {
          // If we pass up an opportunity to create an object due to full pool, the next
          // timeout is not necessarily caused by any previous creation failure.  Need to
          // think about this a little more.  What if the pool is full due to pending creations
          // that eventually fail?
          _lastCreateError = null;
        }
        else if (_waiters.size() > 0 || _poolSize < _minSize)
        {
          _poolSize++;
          _sampleMaxPoolSize = Math.max(_poolSize, _sampleMaxPoolSize);
          result = true;
        }
      }
    }
    return result;
  }

  /**
   * DO NOT call this method while holding the lock!  It invokes user code.
   */
  private void create()
  {
    trc("initiating object creation");
    _lifecycle.create(new Callback<T>() {
      @Override
      public void onSuccess(T t)
      {
        synchronized (_lock)
        {
          _totalCreated++;
          _lastCreateError = null;
        }
        add(t);
      }

      @Override
      public void onError(final Throwable e)
      {
        boolean create;
        final Collection<Callback<T>> waitersDenied;
        synchronized (_lock)
        {
          _totalCreateErrors++;
          _lastCreateError = e;
          create = objectDestroyed();
          if (!_waiters.isEmpty())
          {
            waitersDenied = cancelWaiters();
          }
          else
          {
            waitersDenied = Collections.emptyList();
          }
        }
        // Note this callback is invoked by Netty boss thread. We hand the actual callback
        // task to a separate thread since we don't want to block the boss thread
        _timeoutExecutor.submit(new Runnable() {
          @Override
          public void run() {
            // Note we drain all waiters if a create fails.  When a create fails, rate-limiting
            // logic may be applied.  In this case, we may be initiating creations at a lower rate
            // than incoming requests.  While creations are suppressed, it is better to deny all
            // waiters and let them see the real reason (this exception) rather than keep them around
            // to eventually get an unhelpful timeout error
            for (Callback<T> denied : waitersDenied)
            {
              denied.onError(e);
            }
          }
        });
        if (create)
        {
          create();
        }
        LOG.error(_poolName + ": object creation failed", e);
      }
    });
  }

  private void timeoutObjects()
  {
    Collection<T> idle = reap(_idle, _idleTimeout);
    if (idle.size() > 0)
    {
      LOG.debug("{}: disposing {} objects due to idle timeout", _poolName, idle.size());
      for (T obj : idle)
      {
        destroy(obj, false);
      }
    }
  }

  private <U> Collection<U> reap(Queue<TimedObject<U>> queue, long timeout)
  {
    List<U> toReap = new ArrayList<U>();
    long now = System.currentTimeMillis();
    long target = now - timeout;

    synchronized (_lock)
    {
      int excess = _poolSize - _minSize;
      for (TimedObject<U> p; (p = queue.peek()) != null && p.getTime() < target && excess > 0; excess--)
      {
        toReap.add(queue.poll().get());
        _totalTimedOut++;
      }
    }
    return toReap;
  }

  private void shutdownIfNeeded()
  {
    Callback<None> shutdown = checkShutdownComplete();
    if (shutdown != null)
    {
      finishShutdown(shutdown);
    }
  }

  private Callback<None> checkShutdownComplete()
  {
    Callback<None> done = null;
    final State state;
    final int waiters;
    final int idle;
    final int poolSize;
    synchronized (_lock)
    {
      // Save state for logging outside synchronized block
      state = _state;
      waiters = _waiters.size();
      idle = _idle.size();
      poolSize = _poolSize;

      // Now compare against the same state that will be logged
      if (state == State.SHUTTING_DOWN && waiters == 0 && idle == poolSize)
      {
        _state = State.STOPPED;
        done = _shutdownCallback;
        _shutdownCallback = null;
      }
    }
    if (state == State.SHUTTING_DOWN && done == null)
    {
      LOG.info("{}: {} waiters and {} objects outstanding before shutdown", new Object[]{ _poolName, waiters, poolSize - idle });
    }
    return done;
  }

  private void finishShutdown(Callback<None> shutdown)
  {
    ScheduledFuture<?> future = _objectTimeoutFuture;
    if (future != null)
    {
      future.cancel(false);
    }

    LOG.info("{}: {}", _poolName, "shutdown complete");

    shutdown.onSuccess(None.none());
  }

  private static class TimedObject<T>
  {
    private final T _obj;
    private final long _time;

    public TimedObject(T obj)
    {
      _obj = obj;
      _time = System.currentTimeMillis();
    }

    public T get()
    {
      return _obj;
    }

    public long getTime()
    {
      return _time;
    }
  }

  private void trc(Object toLog)
  {
    LOG.trace("{}: {}", _poolName, toLog);
  }

}
