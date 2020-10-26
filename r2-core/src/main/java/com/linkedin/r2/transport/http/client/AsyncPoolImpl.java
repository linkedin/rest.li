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

import com.linkedin.common.stats.LongTracker;
import com.linkedin.common.stats.LongTracking;
import com.linkedin.r2.util.SingleTimeout;
import com.linkedin.util.ArgumentUtil;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.linkedin.r2.SizeLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.SimpleCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.r2.util.LinkedDeque;
import com.linkedin.r2.transport.http.client.RateLimiter.Task;

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
  private final int _maxWaiters;
  private final long _idleTimeout;
  private final long _waiterTimeout;
  private final long _creationTimeout;
  private final ScheduledExecutorService _timeoutExecutor;
  private final int _minSize;
  private volatile ScheduledFuture<?> _objectTimeoutFuture;
  private final RateLimiter _rateLimiter;

  public static final int MIN_WAITER_TIMEOUT = 300;
  public static final int MAX_WAITER_TIMEOUT = 1000;
  public static final int DEFAULT_OBJECT_CREATION_TIMEOUT = 10000;


  private enum State { NOT_YET_STARTED, RUNNING, SHUTTING_DOWN, STOPPED }

  public enum Strategy { MRU, LRU }
  private final Strategy _strategy;

  // All members below are protected by this lock
  // Never call user code (callbacks) while holding this lock
  private final Object _lock = new Object();
  // Including idle, checked out, and creations/destructions in progress
  private int _poolSize = 0;
  private int _checkedOut = 0;
  // Unused objects live here, sorted by age.
  // The first object is the least recently added object.
  private final Deque<TimedObject<T>> _idle = new LinkedList<TimedObject<T>>();
  // When no unused objects are available, callbacks live here while they wait
  // for a new object (either returned by another user, or newly created)
  private final LinkedDeque<Callback<T>> _waiters = new LinkedDeque<>();
  private Throwable _lastCreateError = null;
  private State _state = State.NOT_YET_STARTED;
  private Callback<None> _shutdownCallback = null;
  private final AsyncPoolStatsTracker _statsTracker;
  private final Clock _clock;

  /**
   * Constructs an AsyncPool with maxWaiters equals to ({@code Integer.MAX_VALUE}).
   */
  public AsyncPoolImpl(String name,
                       Lifecycle<T> lifecycle,
                       int maxSize,
                       long idleTimeout,
                       ScheduledExecutorService timeoutExecutor)
  {
    this(name,
        lifecycle,
        maxSize,
        idleTimeout,
        timeoutExecutor,
        timeoutExecutor,
        Integer.MAX_VALUE);
  }

  /**
   * Constructs an AsyncPool with {@link Strategy#MRU} strategy and
   * minSize equals to 0.
   */
  public AsyncPoolImpl(String name,
                       Lifecycle<T> lifecycle,
                       int maxSize,
                       long idleTimeout,
                       ScheduledExecutorService timeoutExecutor,
                       ExecutorService callbackExecutor,
                       int maxWaiters)
  {
    this(name, lifecycle, maxSize, idleTimeout, timeoutExecutor,
        callbackExecutor, maxWaiters, Strategy.MRU, 0);
  }

  /**
   * Constructs an AsyncPool with an {@link NoopRateLimiter}.
   */
  public AsyncPoolImpl(String name,
                       Lifecycle<T> lifecycle,
                       int maxSize,
                       long idleTimeout,
                       ScheduledExecutorService timeoutExecutor,
                       ExecutorService callbackExecutor,
                       int maxWaiters,
                       Strategy strategy,
                       int minSize)
  {
    this(name, lifecycle, maxSize, idleTimeout, timeoutExecutor,
        maxWaiters, strategy, minSize, new NoopRateLimiter());
  }

  public AsyncPoolImpl(String name,
      Lifecycle<T> lifecycle,
      int maxSize,
      long idleTimeout,
      ScheduledExecutorService timeoutExecutor,
      int maxWaiters,
      Strategy strategy,
      int minSize,
      RateLimiter rateLimiter)
  {
    this(name, lifecycle, maxSize, idleTimeout, timeoutExecutor,
        maxWaiters, strategy, minSize, rateLimiter, SystemClock.instance(), new LongTracking());
  }

  @Deprecated
  public AsyncPoolImpl(String name,
      Lifecycle<T> lifecycle,
      int maxSize,
      long idleTimeout,
      ScheduledExecutorService timeoutExecutor,
      int maxWaiters,
      Strategy strategy,
      int minSize,
      RateLimiter rateLimiter,
      Clock clock,
      LongTracker waitTimeTracker)
  {
    this(name, lifecycle, maxSize, idleTimeout, Integer.MAX_VALUE, timeoutExecutor, maxWaiters, strategy, minSize,
        rateLimiter, clock, waitTimeTracker);
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
   * @param rateLimiter an optional {@link RateLimiter} that controls the
   *                    object creation rate.
   * @param clock a clock object used in tracking async pool stats
   * @param waitTimeTracker tracker used to track pool stats such as percentile
   *                        latency, max, min, standard deviation is enabled.
   *
   */
  public AsyncPoolImpl(String name,
      Lifecycle<T> lifecycle,
      int maxSize,
      long idleTimeout,
      long waiterTimeout,
      ScheduledExecutorService timeoutExecutor,
      int maxWaiters,
      Strategy strategy,
      int minSize,
      RateLimiter rateLimiter,
      Clock clock,
      LongTracker waitTimeTracker)
  {
    ArgumentUtil.notNull(lifecycle, "lifecycle");
    ArgumentUtil.notNull(timeoutExecutor, "timeoutExecutor");
    ArgumentUtil.notNull(strategy, "strategy");
    ArgumentUtil.notNull(rateLimiter, "rateLimiter");

    _poolName = name + "/" + Integer.toHexString(hashCode());
    _lifecycle = lifecycle;
    _maxSize = maxSize;
    _idleTimeout = idleTimeout;
    _waiterTimeout = waiterTimeout;
    _creationTimeout = DEFAULT_OBJECT_CREATION_TIMEOUT; // TODO: expose this through cfg2
    _timeoutExecutor = timeoutExecutor;
    _maxWaiters = maxWaiters;
    _strategy = strategy;
    _minSize = minSize;
    _rateLimiter = rateLimiter;
    _clock = clock;
    _statsTracker = new AsyncPoolStatsTracker(
        () -> _lifecycle.getStats(),
        () -> _maxSize,
        () -> _minSize,
        () -> {
          synchronized (_lock) {
            return _poolSize;
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
            return _idle.size();
          }
        },
        clock,
        waitTimeTracker);
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
    boolean create = false;
    boolean reject = false;
    final LinkedDeque.Node<Callback<T>> node;
    Callback<T> callbackWithTracking = new TimeTrackingCallback<T>(callback);
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
            if (_waiters.size() < _maxWaiters)
            {
              if (isWaiterTimeoutEnabled())
              {
                callbackWithTracking = new WaiterTimeoutCallback(callbackWithTracking);
              }
              // No objects available and the waiter list is not full; add to waiter list and break out of loop
              node = _waiters.addLastNode(callbackWithTracking);
              create = shouldCreate();
            }
            else
            {
              reject = true;
              node = null;
            }
            break;
          }
        }
      }
      if (state != State.RUNNING)
      {
        // Defer execution of the callback until we are out of the synchronized block
        callbackWithTracking.onError(new IllegalStateException(_poolName + " is " + _state));
        return () -> false;
      }
      T rawObj = obj.get();
      if (_lifecycle.validateGet(rawObj))
      {
        trc("dequeued an idle object");
        // Valid object; done
        synchronized (_lock)
        {
          _checkedOut++;
          _statsTracker.sampleMaxCheckedOut();
        }
        callbackWithTracking.onSuccess(rawObj);
        return () -> false;
      }
      // Invalid object, discard it and keep trying
      destroy(rawObj, true);
      trc("dequeued and disposed an invalid idle object");
    }
    if (reject)
    {
      // This is a recoverable exception. User can simply retry the failed get() operation.
      callbackWithTracking.onError(
          new SizeLimitExceededException("AsyncPool " + _poolName + " reached maximum waiter size: " + _maxWaiters));
      return () -> false;
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
          boolean cancelled = _waiters.removeNode(node) != null;
          if (cancelled)
          {
            shutdownIfNeeded();
          }
          return cancelled;
        }
      }
    };
  }

  private boolean isWaiterTimeoutEnabled()
  {
    // Do not enable waiter timeout if the configured value is not within the fail fast threshold
    return _waiterTimeout >= MIN_WAITER_TIMEOUT && _waiterTimeout <= MAX_WAITER_TIMEOUT;
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
    // A channel made it through a complete request lifecycle
    _rateLimiter.setPeriod(0);
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
        _statsTracker.sampleMaxCheckedOut();
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
      return _statsTracker.getStats();
    }
  }

  private void destroy(T obj, boolean bad)
  {
    if(bad)
    {
      synchronized(_lock)
      {
        _statsTracker.incrementBadDestroyed();
      }
    }
    trc("disposing a pooled object");
    _lifecycle.destroy(obj, bad, new Callback<T>() {
      @Override
      public void onSuccess(T t) {
        boolean create;
        synchronized (_lock)
        {
          _statsTracker.incrementDestroyed();
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
          _statsTracker.incrementDestroyErrors();
          create = objectDestroyed();
        }
        if (create) {
          create();
        }
        // TODO log this error!
      }
    });
  }

  private boolean objectDestroyed()
  {
    return objectDestroyed(1);
  }

  /**
   * This method is safe to call while holding the lock.
   * @param num number of objects have been destroyed
   * @return true if another object creation should be initiated
   */
  private boolean objectDestroyed(int num)
  {
    boolean create;
    synchronized (_lock)
    {
      if (_poolSize - num > 0)
      {
        _poolSize -= num;
      }
      else
      {
        _poolSize = 0;
      }
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
          _statsTracker.sampleMaxPoolSize();
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
    _rateLimiter.submit(new Task()
    {
      @Override
      public void run(final SimpleCallback callback)
      {
        boolean shouldIgnore;
        synchronized (_lock) {
          // Ignore the object creation if no one is waiting for the object and the pool already has _minSize objects
          int totalObjects = _checkedOut + _idle.size();
          shouldIgnore = _waiters.size() == 0 && totalObjects >= _minSize;
          if (shouldIgnore) {
            _statsTracker.incrementIgnoredCreation();
            if (_poolSize >= 1)
            {
              // _poolSize also include the count of creation requests pending. So we have to make sure the pool size
              // count is updated when we ignore the creation request.
              _poolSize--;
            }
          }
        }

        if (shouldIgnore) {
          callback.onDone();
          return;
        }

        // Lets not trust the _lifecycle to timely return a response here.
        // Embedding the callback inside a timeout callback (ObjectCreationTimeoutCallback)
        // to force a response within creationTimeout deadline to reclaim the object slot in the pool
        _lifecycle.create(new TimeoutCallback<>(_timeoutExecutor, _creationTimeout, TimeUnit.MILLISECONDS, new Callback<T>() {
          @Override
          public void onSuccess(T t)
          {
            synchronized (_lock)
            {
              _statsTracker.incrementCreated();
              _lastCreateError = null;
            }
            add(t);
            callback.onDone();
          }

          @Override
          public void onError(final Throwable e)
          {
            // Note we drain all waiters and cancel all pending creates if a create fails.
            // When a create fails, rate-limiting logic will be applied.  In this case,
            // we may be initiating creations at a lower rate than incoming requests.  While
            // creations are suppressed, it is better to deny all waiters and let them see
            // the real reason (this exception) rather than keep them around to eventually
            // get an unhelpful timeout error
            final Collection<Callback<T>> waitersDenied;
            final Collection<Task> cancelledCreate = _rateLimiter.cancelPendingTasks();
            boolean create;
            synchronized (_lock)
            {
              _statsTracker.incrementCreateErrors();
              _lastCreateError = e;

              // Cancel all waiters in the rate limiter
              if (!_waiters.isEmpty())
              {
                waitersDenied = cancelWaiters();
              }
              else
              {
                waitersDenied = Collections.<Callback<T>>emptyList();
              }

              // reclaim the slot in the pool
              create = objectDestroyed(1 + cancelledCreate.size());
            }

            // lets fail all the waiters with the object creation error
            for (Callback<T> denied : waitersDenied)
            {
              try
              {
                denied.onError(e);
              }
              catch (Exception ex)
              {
                LOG.error("Encountered error while invoking error waiter callback", ex);
              }
            }

            // Now after cancelling all the pending tasks, lets make sure to back off on the creation
            _rateLimiter.incrementPeriod();

            // if we still need to create a new object, lets initiate that now
            // since all waiters are cancelled, the only condition that makes this true is when the pool is below
            // the min poolSize
            if (create)
            {
              create();
            }
            LOG.debug(_poolName + ": object creation failed", e);
            callback.onDone();
          }
        }, () -> new ObjectCreationTimeoutException(
            "Exceeded creation timeout of " + _creationTimeout + "ms: in Pool: "+ _poolName)));
      }
    });
  }

  private void timeoutObjects()
  {
    Collection<T> expiredObjects = getExpiredObjects();
    if (expiredObjects.size() > 0)
    {
      LOG.debug("{}: disposing {} objects due to idle timeout", _poolName, expiredObjects.size());
      for (T obj : expiredObjects)
      {
        destroy(obj, false);
      }
    }
  }

  private Collection<T> getExpiredObjects()
  {
    List<T> expiredObjects = new ArrayList<T>();
    long now = _clock.currentTimeMillis();

    synchronized (_lock)
    {
      long deadline = now - _idleTimeout;
      int excess = _poolSize - _minSize;
      for (TimedObject<T> p; (p = _idle.peek()) != null && p.getTime() < deadline && excess > 0; excess--)
      {
        expiredObjects.add(_idle.poll().get());
        _statsTracker.incrementTimedOut();
      }
    }
    return expiredObjects;
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

  private class TimedObject<T>
  {
    private final T _obj;
    private final long _time;

    public TimedObject(T obj)
    {
      _obj = obj;
      _time = _clock.currentTimeMillis();
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

  private class WaiterTimeoutCallback implements Callback<T>
  {
    private final SingleTimeout<Callback<T>> _timeout;

    private WaiterTimeoutCallback(final Callback<T> callback)
    {
      _timeout = new SingleTimeout<>(_timeoutExecutor, _waiterTimeout, TimeUnit.MILLISECONDS, callback, (callbackIfTimeout) -> {

        synchronized (_lock)
        {
          _waiters.remove(this);
          _statsTracker.incrementWaiterTimedOut();
        }
        LOG.debug("{}: failing waiter due to waiter timeout", _poolName);
        callbackIfTimeout.onError(
            new WaiterTimeoutException(
                "Exceeded waiter timeout of " + _waiterTimeout + "ms: in Pool: "+ _poolName));
      });
    }

    @Override
    public void onError(Throwable e)
    {
      Callback<T> callback = _timeout.getItem();
      if (callback != null)
      {
        callback.onError(e);
      }
    }

    @Override
    public void onSuccess(T result)
    {
      Callback<T> callback = _timeout.getItem();
      if (callback != null)
      {
        callback.onSuccess(result);
      }
    }
  }

  private class TimeTrackingCallback<T> implements Callback<T>
  {
    private final long _startTime;
    private final Callback<T> _callback;

    public TimeTrackingCallback(Callback<T> callback)
    {
      _callback = callback;
      _startTime = _clock.currentTimeMillis();
    }

    @Override
    public void onError(Throwable e)
    {
      long waitTime = _clock.currentTimeMillis() - _startTime;
      synchronized (_lock)
      {
        _statsTracker.trackWaitTime(waitTime);
        _statsTracker.sampleMaxWaitTime(waitTime);
      }
      _callback.onError(e);
    }

    @Override
    public void onSuccess(T result)
    {
      long waitTime = _clock.currentTimeMillis() - _startTime;
      synchronized (_lock)
      {
        _statsTracker.trackWaitTime(waitTime);
        _statsTracker.sampleMaxWaitTime(waitTime);
      }
      _callback.onSuccess(result);
    }

    public long getTime()
    {
      return _startTime;
    }
  }

  private void trc(Object toLog)
  {
    LOG.trace("{}: {}", _poolName, toLog);
  }
}
