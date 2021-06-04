/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.ratelimiter.CallbackStore;
import com.linkedin.util.clock.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A CallbackStore specifically designed to feed an asynchronous event loop with a constant supply of unique Callbacks.
 *
 * EvictingCircularBuffer accomplishes a few key goals:
 * - Must always accept submission of new Callbacks, replacing oldest Callbacks when buffer is exhausted
 * - Must provide a unique Callback between subsequent get() calls
 * - Must be able to honor get requests in excess of put requests, returning previously returned Callbacks is acceptable.
 * - During periods of insufficient write throughput, must prune stale Callbacks, ultimately throwing NoSuchElementException
 *   to upstream callers when inbound write throughput has dropped to zero.
 * - Must do the above with high performance, without adding meaningful latency to reader/writer threads.
 *
 * This class is thread-safe, achieving performance through granular locking of each element of the underlying circular buffer.
 */
public class EvictingCircularBuffer implements CallbackStore
{
  private Duration _ttl;
  private final ArrayList<Callback<None>> _callbacks = new ArrayList<>();
  private final ArrayList<Instant> _ttlBuffer = new ArrayList<>();
  private final ArrayList<ReentrantReadWriteLock> _elementLocks = new ArrayList<>();
  private final AtomicInteger _readerPosition = new AtomicInteger();
  private final AtomicInteger _writerPosition = new AtomicInteger();
  private final Clock _clock;

  /**
   * @param capacity initial value for the maximum number of Callbacks storable by this buffer
   * @param ttl Amount of time a callback is eligible for being returned after being stored
   * @param ttlUnit Unit of time for ttl value
   * @param clock Clock instance used for calculating ttl expiry
   */
  public EvictingCircularBuffer(int capacity, int ttl, ChronoUnit ttlUnit, Clock clock)
  {
    setCapacity(capacity);
    setTtl(ttl, ttlUnit);
    _clock = clock;
  }

  /**
   * Adds the supplied Callback to the internal circular buffer. If the buffer is full, the oldest Callback in the buffer
   * will be overwritten. Calls to put always succeed, and there is no guarantee that Callbacks submitted through the put()
   * method will be subsequently returned by the get() method.
   *
   * @param toAdd Callback that is to be possibly returned by later calls to get()
   */
  public void put(Callback<None> toAdd)
  {
    int writerPosition = getAndBumpWriterPosition();
    ReentrantReadWriteLock thisLock = _elementLocks.get(writerPosition);
    thisLock.writeLock().lock();
    try
    {
      _callbacks.set(writerPosition, toAdd);
      _ttlBuffer.set(writerPosition, Instant.ofEpochMilli(_clock.currentTimeMillis()));
    }
    finally
    {
      thisLock.writeLock().unlock();
    }
  }

  /**
   * Returns a Callback previously stored in the circular buffer through the put() method. Callbacks are generally returned
   * in the order they were received, but in cases of get() throughput in excess of put() throughput, previously returned
   * Callbacks will be sent. Oldest age of returned Callbacks is configurable through the ttl param in the constructor.
   *
   * Calls to get() will always succeed as long as calls to put() continue at a cadence within the ttl duration. When write
   * throughput has dropped to zero, get() will eventually throw NoSuchElementException once the circular buffer has become
   * fully pruned through expired ttl.
   * @return Callback
   * @throws NoSuchElementException if internal circular buffer is empty
   */
  public Callback<None> get() throws NoSuchElementException
  {
    int getAttemptIteration = 0;
    while (getAttemptIteration++ <= getCapacity())
    {
      int thisReaderPosition = getAndBumpReaderPosition();
      ReentrantReadWriteLock thisLock = _elementLocks.get(thisReaderPosition);
      thisLock.readLock().lock();
      Callback<None> callback;
      Instant ttl;
      try
      {
        callback = _callbacks.get(thisReaderPosition);
        ttl = _ttlBuffer.get(thisReaderPosition);
      }
      finally
      {
        thisLock.readLock().unlock();
      }

      if (callback != null)
      {
        // check for expired ttl
        if (Duration.between(ttl, Instant.ofEpochMilli(_clock.currentTimeMillis())).compareTo(_ttl) > 0)
        {
          thisLock.writeLock().lock();
          try
          {
            // after acquiring write lock at reader position, ensure the data at reader position is the same as when we read it
            if (callback == _callbacks.get(thisReaderPosition))
            {
              _callbacks.set(thisReaderPosition, null);
              _ttlBuffer.set(thisReaderPosition, null);
            }
          }
          finally
          {
            thisLock.writeLock().unlock();
          }
        }
        else
        {
          return callback;
        }
      }
    }
    throw new NoSuchElementException("buffer is empty");
  }

  /**
   * @return the number of unique Callbacks this buffer can hold.
   */
  int getCapacity()
  {
    return _callbacks.size();
  }

  /**
   * Resizes the circular buffer, deleting the contents in the process.
   * This method should not be called frequently, ideally only as part of a startup lifecycle, as it does heavy locking
   * to ensure all reads and writes are drained coinciding with the resizing of the buffer.
   * @param capacity
   */
  void setCapacity(int capacity)
  {
    if (capacity < 1)
    {
      throw new IllegalArgumentException("capacity can't be less than 1");
    }
    // acquire write lock for all elements in the buffer to prevent reads while the buffer is re-created,
    // taking care to store them in a temporary location for releasing afterward.
    ArrayList<ReentrantReadWriteLock> tempLocks = new ArrayList<>();
    _elementLocks.forEach(x ->
    {
      x.writeLock().lock();
      tempLocks.add(x);
    });
    try
    {
      _callbacks.clear();
      _ttlBuffer.clear();
      _elementLocks.clear();
      // populate ArrayList with nulls to prevent changes to underlying data structure size during writes,
      // also needed to compute reader and writer position through calls to size()
      _ttlBuffer.addAll(Collections.nCopies(capacity, null));
      _callbacks.addAll(Collections.nCopies(capacity, null));
      for(int i = 0; i <= capacity; i++)
      {
        _elementLocks.add(new ReentrantReadWriteLock());
      }
    }
    finally
    {
      // these locks no longer exist in _elementLocks, but we need to release them in order to unblock
      // pending reads.
      tempLocks.forEach(x -> x.writeLock().unlock());
    }
  }

  /**
   * @return the currently configured TTL.
   */
  Duration getTtl()
  {
    return _ttl;
  }

  /**
   * Sets the amount of time a Callback is eligible to be returned after it has been stored in the buffer.
   * TTL is shared across all stored Callbacks for the sake of simplicity.
   * @param ttl number value of amount of time
   * @param ttlUnit unit of time for number value
   */
  void setTtl(int ttl, ChronoUnit ttlUnit)
  {
    if (ttl < 1)
    {
      throw new IllegalArgumentException("ttl can't be less than 1");
    }
    if (ttlUnit == null)
    {
      throw new IllegalArgumentException("ttlUnit can't be null.");
    }
    _ttl = Duration.of(ttl, ttlUnit);
  }

  private int getAndBumpWriterPosition()
  {
    return (_writerPosition.getAndUpdate(x -> (x + 1) % _callbacks.size()));
  }

  private int getAndBumpReaderPosition()
  {
    return (_readerPosition.getAndUpdate(x -> (x + 1) % _callbacks.size()));
  }
}
