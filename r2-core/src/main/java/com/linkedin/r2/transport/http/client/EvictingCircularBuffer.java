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
import com.linkedin.util.clock.SystemClock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * TODO: javadocs
 */
@SuppressWarnings("serial")
public class EvictingCircularBuffer implements CallbackStore
{
  private Duration _ttl;
  private final ArrayList<Callback<None>> _callbacks = new ArrayList<>();
  private final ArrayList<Instant> _ttlBuffer = new ArrayList<>();
  private final ArrayList<ReentrantReadWriteLock> _elementLocks = new ArrayList<>();
  private final AtomicInteger _readerPosition = new AtomicInteger();
  private final AtomicInteger _writerPosition = new AtomicInteger();
  private final Clock _clock;


  public EvictingCircularBuffer(int capacity, int ttl, ChronoUnit ttlUnit)
  {
    this(capacity, ttl, ttlUnit, SystemClock.instance());
  }

  EvictingCircularBuffer(int capacity, int ttl, ChronoUnit ttlUnit, Clock clock)
  {
    setCapacity(capacity);
    setTtl(ttl, ttlUnit);
    _clock = clock;
  }

  public void put(Callback<None> toAdd)
  {
    int writerPosition = getAndBumpWriterPosition();
    ReentrantReadWriteLock thisLock = _elementLocks.get(writerPosition);
    thisLock.writeLock().lock();
    try
    {
      _callbacks.set(writerPosition, toAdd);
      _ttlBuffer.set(writerPosition, Instant.ofEpochMilli(_clock.currentTimeMillis()));
    } finally {
      thisLock.writeLock().unlock();
    }
  }

  /**
   * TODO: javadocs
   * @return
   */
  public Callback<None> get()
  {
    int thisReaderPosition = getAndBumpReaderPosition();
    ReentrantReadWriteLock thisLock = _elementLocks.get(thisReaderPosition);

    thisLock.readLock().lock();
    Callback<None> callback;
    Instant ttl;
    try {
      callback = _callbacks.get(thisReaderPosition);
      ttl = _ttlBuffer.get(thisReaderPosition);
    }
    finally
    {
      thisLock.readLock().unlock();
    }

    if (callback == null)
    {
      if (thisReaderPosition == 0)
      {
        throw new NoSuchElementException("Buffer is empty");
      }
      else
      {
        return get();
      }
    }
    else if (Duration.between(ttl, Instant.ofEpochMilli(_clock.currentTimeMillis())).compareTo(_ttl) > 0)
    {
      if (thisReaderPosition != 0 || this.size() == 1)
      {
        thisLock.writeLock().lock();
        try {
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
      return get();
    }
    return callback;
  }

  private int getAndBumpWriterPosition()
  {
    return (_writerPosition.getAndUpdate(x -> (x + 1) % _callbacks.size()));
  }

  private int getAndBumpReaderPosition()
  {
    return (_readerPosition.getAndUpdate(x -> (x + 1) % _callbacks.size()));
  }

  private int size()
  {
    return (int) _callbacks.stream().filter(Objects::nonNull).count();
  }

  int getCapacity()
  {
    return _callbacks.size();
  }

  void setCapacity(int capacity)
  {
    if (capacity < 1) {
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
    try {
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

  public Duration getTtl()
  {
    return _ttl;
  }

  void setTtl(int ttl, ChronoUnit ttlUnit)
  {
    if (ttl < 1) {
      throw new IllegalArgumentException("ttl can't be less than 1");
    }
    if (ttlUnit == null) {
      throw new IllegalArgumentException("ttlUnit can't be null.");
    }
    _ttl = Duration.of(ttl, ttlUnit);
  }
}