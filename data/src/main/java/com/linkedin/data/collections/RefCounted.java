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

package com.linkedin.data.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reference counting for copy-on-write functionality.
 * <p>
 *
 * It holds a pointer to the reference counted object and an atomic counter
 * that counts number of "counted" references to the reference counted object.
 * This class provides methods for sharing and un-sharing the reference
 * counted object.
 * <p>
 *
 * The reference counted object is shared if it has more than one "counted"
 * reference, and it is not shared if it has one or less "counted"
 * reference.
 * <p>
 *
 * @see #acquire()
 * @see #release()
 *
 * This is inspired by a common C++ String copy-on-write implementation.
 */
class RefCounted<T extends Cloneable> implements Cloneable
{
  /**
   * Constructor.
   */
  protected RefCounted(T object)
  {
    _object = object;
    assert (object != null);
    assert (isSharable());
  }

  /**
   * For unit testing.
   *
   * @return the current reference count.
   */
  int getRefCount()
  {
    return _refCount.get();
  }

  /**
   * Get the reference counted object.
   *
   * @return the reference counted object.
   */
  T getObject()
  {
    return _object;
  }

  /**
   * Get mutable {@link RefCounted}.
   *
   * If this {@link RefCounted} is shared, create {@link RefCounted} from
   * this {@link RefCounted}, release reference to this {@link RefCounted}
   * and return the new {@link RefCounted}.
   *
   * @return mutable a {@link RefCounted}, returned value may be this or
   *         a new {@link RefCounted} if the this {@link RefCounted} is shared.
   */
  @SuppressWarnings("unchecked")
  RefCounted<T> getMutable() throws UnsupportedOperationException
  {
    RefCounted<T> o;
    if (isShared())
    {
      try
      {
        o = (RefCounted<T>) clone();
        release();
      }
      catch (CloneNotSupportedException e)
      {
        throw new UnsupportedOperationException("Failed to clone", e);
      }
    }
    else
    {
      o = this;
    }
    return o;
  }

  /**
   * Clone this {@link RefCounted} which creates new {@link RefCounted} with a
   * new reference counter and the cloned {@link RefCounted} referenced object
   * is a clone of this {@link RefCounted}'s referenced object.
   *
   * @return the cloned {@link RefCounted}.
   * @throws CloneNotSupportedException if this object cannot be cloned.
   */
  protected Object clone() throws CloneNotSupportedException
  {
    @SuppressWarnings("unchecked")
    RefCounted<T> o = (RefCounted<T>) super.clone();

    o._refCount = new AtomicInteger();
    assert (o.isSharable());
    Throwable exc = null;
    try
    {
      Method method = _object.getClass().getMethod("clone");

      @SuppressWarnings("unchecked")
      T converted = (T) method.invoke(_object);
      o._object = converted;
    }
    catch (InvocationTargetException e)
    {
      exc = e.getCause();
    }
    catch (Exception e)
    {
      exc = e;
    }
    if (exc != null)
    {
      if (exc instanceof CloneNotSupportedException)
      {
        throw (CloneNotSupportedException) exc;
      }
      else
      {
        CloneNotSupportedException cnse = new CloneNotSupportedException();
        cnse.initCause(exc.getCause());
        throw cnse;
      }
    }
    return o;
  }

  protected final void setSharableIfLeaked()
  {
    if (isLeaked())
    {
      setSharable();
    }
  }

  protected final boolean isLeaked()
  {
    return _refCount.get() < 0;
  }

  protected final boolean isShared()
  {
    return _refCount.get() > 0;
  }

  protected final boolean isSharable()
  {
    return _refCount.get() == 0;
  }

  protected final void setLeaked()
  {
    _refCount.set(-1);
  }

  protected final void setSharable()
  {
    _refCount.set(0);
  }

  /**
   * Acquire a reference, which may cause a cloned {@link RefCounted} to be returned.
   *
   * @return this or a cloned {@link RefCounted} depending on reference count.
   * @throws UnsupportedOperationException if this method cannot clone this {@link RefCounted}.
   */
  @SuppressWarnings("unchecked")
  RefCounted<T> acquire() throws UnsupportedOperationException
  {
    RefCounted<T> o;
    if (isLeaked())
    {
      try
      {
        o = (RefCounted<T>) clone();
      }
      catch (CloneNotSupportedException e)
      {
        throw new UnsupportedOperationException("Failed to clone object", e);
      }
      assert (isSharable());
    }
    else
    {
      _refCount.incrementAndGet();
      o = this;
    }
    return o;
  }

  /**
   * Release a reference which decrements the reference count.
   */
  void release()
  {
    _refCount.decrementAndGet();
  }

  /**
   * The reference counter.
   *
   * _refCount has three states:
   * -1  : leaked, one reference, no ref-copies allowed, non-const.
   * 0   : one reference, non-const.
   * n>0 : n + 1 references, operations require a lock, const.
   */
  protected AtomicInteger _refCount = new AtomicInteger();

  /**
   * The reference object.
   */
  protected T _object = null;
}
