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

package com.linkedin.darkcluster.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO: javadocs
 * @param <E>
 */
@SuppressWarnings("serial")
public class ExpiringCircularBuffer<E> extends ArrayList<E> implements Queue<E> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExpiringCircularBuffer.class);
  private Duration _ttl;
  private ArrayList<Instant> _ttlBuffer;
  private volatile int _readerPosition;
  private volatile int _writerPosition;

  public ExpiringCircularBuffer(int capacity, int ttl, ChronoUnit ttlUnit) {
    super();
    setCapacity(capacity);
    setTtl(ttl, ttlUnit);
  }

  public synchronized boolean add(E toAdd) {
    this.set(_writerPosition, toAdd);
    _ttlBuffer.set(_writerPosition, Instant.now());
    bumpWriterPosition();
    return true;
  }

  /**
   * TODO: javadocs
   * @return
   */
  public synchronized E element() {
    LOGGER.info("Trying a get. Reader position is {}", _readerPosition);
    E e = this.get(_readerPosition);
    Instant ttl = _ttlBuffer.get(_readerPosition);
    if (e == null) {
      if (_readerPosition == 0) {
        throw new NoSuchElementException("Buffer is empty");
      } else {
        LOGGER.info("Hit a hole in the buffer, bump readerPosition and retry.");
        bumpReaderPosition();
        return element();
      }
    } else if (Duration.between(ttl, Instant.now()).compareTo(_ttl) > 0) {
      if (_readerPosition != 0 || (_readerPosition == 0 && this.size() == 1)) {
        LOGGER.info("purging object from buffer after expiring ttl");
        this.set(_readerPosition, null);
        _ttlBuffer.set(_readerPosition, null);
      }
      bumpReaderPosition();
      return element();
    }
    bumpReaderPosition();
    return e;
  }

  public boolean offer(E e) {
    return add(e);
  }

  public E remove() {
    throw new NotImplementedException("ExpiringCircularBuffer does not support removal operations");
  }

  public E poll() {
    throw new NotImplementedException("ExpiringCircularBuffer does not support removal operations");
  }

  public E peek() {
    try {
      return element();
    } catch (NoSuchElementException ex) {
      return null;
    }
  }

  public int size() {
    return (int) this.stream().filter(Objects::nonNull).count();
  }

  public boolean isEmpty() {
    return this.size() == 0;
  }

  private synchronized void bumpReaderPosition() {
    _readerPosition = (_readerPosition + 1) % super.size();
    LOGGER.info("reader position is now {}", _readerPosition);
  }

  private synchronized void bumpWriterPosition() {
    _writerPosition = (_writerPosition + 1) % super.size();
    LOGGER.info("writer position is now {}", _writerPosition);
  }

  public synchronized void setCapacity(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity can't be less than 1.");
    }
    super.clear();
    super.addAll(Collections.nCopies(capacity, null));
    _ttlBuffer = new ArrayList<>(Collections.nCopies(capacity, null));
  }

  public void setTtl(int ttl, ChronoUnit ttlUnit)
  {
    if (ttl < 1) {
      throw new IllegalArgumentException("ttl can't be less than 1.");
    }
    if (ttlUnit == null) {
      throw new IllegalArgumentException("ttlUnit can't be null.");
    }
    _ttl = Duration.of(ttl, ttlUnit);
  }
}