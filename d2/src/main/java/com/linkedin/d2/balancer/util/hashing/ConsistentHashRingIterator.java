/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.hashing;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements an iterator for a consistent hash ring.
 * The iteration starts from a specified position in the objects array.
 *
 * @param <T>
 */
public class ConsistentHashRingIterator<T> implements Iterator<T>
{

  private final T[] _objects;

  private int _iterated;

  private int _index;

  /**
   * Construct the iterator
   * @param from It's guaranteed to be less than the length of objects since it
   *             will be only called in ConsistentHashRing
   */
  public ConsistentHashRingIterator(T[] objects, int from)
  {
    _objects = objects;
    _iterated = 0;
    _index = from;
  }

  @Override
  public boolean hasNext()
  {
    return (_iterated < _objects.length);
  }

  @Override
  public T next()
  {
    if (!hasNext())
    {
      throw new NoSuchElementException();
    }

    T result = _objects[_index];
    _index = (_index + 1) % _objects.length;
    _iterated++;

    return result;
  }

  @Override
  public void remove()
  {
    throw new UnsupportedOperationException();
  }
}
