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

package com.linkedin.r2.util;

import java.util.AbstractQueue;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementations must minimally provide the following:
 * {@link java.util.Collection#size()}
 * {@link java.util.Collection#iterator()}
 * {@link java.util.Deque#offerFirst(Object)}
 * {@link java.util.Deque#offerLast(Object)}
 * {@link java.util.Deque#peekFirst()}
 * {@link java.util.Deque#peekLast()}
 * {@link java.util.Deque#pollFirst()}
 * {@link java.util.Deque#pollLast()}
 * {@link java.util.Deque#descendingIterator()}
 *
 * and may override other methods
 * @author Steven Ihde
 * @version $Revision: $
 */

public abstract class AbstractDeque<E> extends AbstractQueue<E> implements Deque<E>
{
  // java.util.Deque methods
  @Override
  public void addFirst(E e)
  {
    if (!offerFirst(e))
    {
      throw new IllegalStateException("Queue full");
    }
  }

  @Override
  public void addLast(E e)
  {
    if (!offerLast(e))
    {
      throw new IllegalStateException("Queue full");
    }
  }

  @Override
  public E removeFirst()
  {
    E e = pollFirst();
    if (e == null)
    {
      throw new NoSuchElementException();
    }
    return e;
  }

  @Override
  public E removeLast()
  {
    E e = pollLast();
    if (e == null)
    {
      throw new NoSuchElementException();
    }
    return e;
  }

  @Override
  public E getFirst()
  {
    E e = peekFirst();
    if (e == null)
    {
      throw new NoSuchElementException();
    }
    return e;
  }

  @Override
  public E getLast()
  {
    E e = peekLast();
    if (e == null)
    {
      throw new NoSuchElementException();
    }
    return e;
  }

  @Override
  public boolean removeFirstOccurrence(Object o)
  {
    for (Iterator<E> i = iterator(); i.hasNext(); )
    {
      if (i.next().equals(o))
      {
        i.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean removeLastOccurrence(Object o)
  {
    for (Iterator<E> i = descendingIterator(); i.hasNext(); )
    {
      if (i.next().equals(o))
      {
        i.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public void push(E e)
  {
    addFirst(e);
  }

  @Override
  public E pop()
  {
    return removeFirst();
  }

  // java.util.Queue methods
  @Override
  public boolean offer(E e)
  {
    return offerLast(e);
  }

  @Override
  public E poll()
  {
    return pollFirst();
  }

  @Override
  public E peek()
  {
    return peekFirst();
  }
}
