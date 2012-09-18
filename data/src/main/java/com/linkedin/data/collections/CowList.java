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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copy-on-Write List.
 * <p>
 *
 * The underlying list implementation is {@link ArrayList}. It delegates
 * list operations to the underlying [@link ArrayList} associated
 * with this {@link CowList}.
 * <p>
 *
 * Multiple {@link CowList}'s can reference the same {@link ArrayList}.
 * When a {@link CowList} is cloned, the underlying {@link ArrayList}
 * is not cloned, instead a reference count is incremented. This reference count
 * tracks the number of {@link CowList}'s sharing the same underlying
 * {@link ArrayList}. A shared underlying {@link ArrayList} is read-only.
 * <p>
 *
 * If a method mutates the {@link CowList} and the underlying {@link ArrayList}
 * is shared, then the underlying {@link ArrayList} will be
 * cloned, the cloned {@link ArrayList} will be exclusively "owned"
 * by this {@link CowList}, and mutations will occur on the clone.
 * <p>
 *
 * A {@link CowList} may be marked read-only to disable mutations,
 * and to avoid unintentional changes. It may also be invalidated to
 * release its reference and decrease the reference count on the underlying
 * {@link ArrayList}.
 * <P>
 *
 * It enables a {@link ListChecker} to be added to check
 * keys and values being stored into the {@link CowList}.
 * <p>
 *
 * @author slim
 */
public class CowList<E> extends AbstractList<E> implements CommonList<E>, Cloneable
{
  /**
   * Construct an empty list.
   */
  public CowList()
  {
    _checker = null;
    _refCounted = new RefCounted<InternalList<E>>(new InternalList<E>());
  }

  /**
   * Construct a list initialized with elements provided by the specified list.
   *
   * @param list provides the initial elements of the new list.
   */
  public CowList(List<? extends E> list)
  {
    _checker = null;
    checkAll(list);
    _refCounted = new RefCounted<InternalList<E>>(new InternalList<E>(list));
  }

  /**
   * Construct a list with the specified initial capacity.
   *
   * @param initialCapacity provides the initial capacity.
   */
  public CowList(int initialCapacity)
  {
    _checker = null;
    _refCounted = new RefCounted<InternalList<E>>(new InternalList<E>(initialCapacity));
  }

  /**
   * Construct a list with the specified {@link ListChecker}.
   *
   * @param checker provides the {@link ListChecker}.
   */
  public CowList(ListChecker<E> checker)
  {
    _checker = checker;
    _refCounted = new RefCounted<InternalList<E>>(new InternalList<E>());
  }

  /**
   * Construct a list initialized with elements provided by the specified list
   * and with the specified {@link ListChecker}.
   *
   * @param list provides the initial elements of the new list.
   * @param checker provides the {@link ListChecker}.
   */
  public CowList(List<? extends E> list, ListChecker<E> checker)
  {
    _checker = checker;
    checkAll(list);
    _refCounted = new RefCounted<InternalList<E>>(new InternalList<E>(list));
  }

  /**
   * Construct a list with the specified initial capacity and {@link ListChecker}.
   *
   * @param initialCapacity provides the initial capacity.
   * @param checker provides the {@link ListChecker}.
   */
  public CowList(int initialCapacity, ListChecker<E> checker)
  {
    _checker = checker;
    _refCounted = new RefCounted<InternalList<E>>(new InternalList<E>(initialCapacity));
  }

  @Override
  public boolean add(E e)
  {
    check(e);
    return getMutable().add(e);
  }

  @Override
  public void add(int index, E element)
  {
    check(element);
    getMutable().add(index, element);
  }

  @Override
  public boolean addAll(Collection<? extends E> c)
  {
    checkAll(c);
    return getMutable().addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c)
  {
    checkAll(c);
    return getMutable().addAll(index, c);
  }

  @Override
  public void clear()
  {
    getMutable().clear();
  }

  @Override
  public CowList<E> clone() throws CloneNotSupportedException
  {
    @SuppressWarnings("unchecked")
    CowList<E> o = (CowList<E>) super.clone();
    o._refCounted = _refCounted.acquire();
    o._readOnly = false;
    return o;
  }

  @Override
  public boolean contains(Object o)
  {
    return getObject().contains(o);
  }

  @Override
  public boolean containsAll(Collection<?> c)
  {
    return getObject().containsAll(c);
  }

  @Override
  public boolean equals(Object object)
  {
    return getObject().equals(object);
  }

  @Override
  public E get(int index)
  {
    return getObject().get(index);
  }

  @Override
  public int hashCode()
  {
    return getObject().hashCode();
  }

  @Override
  public int indexOf(Object o)
  {
    return getObject().indexOf(o);
  }

  @Override
  public boolean isEmpty()
  {
    return getObject().isEmpty();
  }

  @Override
  public int lastIndexOf(Object o)
  {
    return getObject().lastIndexOf(o);
  }

  @Override
  public E remove(int index)
  {
    return getMutable().remove(index);
  }

  @Override
  public boolean remove(Object o)
  {
    return getMutable().remove(o);
  }

  @Override
  public boolean removeAll(Collection<?> c)
  {
    return getMutable().removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c)
  {
    return getMutable().retainAll(c);
  }

  @Override
  public void removeRange(int fromIndex, int toIndex)
  {
    getMutable().removeRange(fromIndex, toIndex);
  }

  @Override
  public E set(int index, E element)
  {
    check(element);
    return getMutable().set(index, element);
  }

  @Override
  public int size()
  {
    return getObject().size();
  }

  @Override
  public Object[] toArray()
  {
    return getObject().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a)
  {
    return getObject().toArray(a);
  }

  @Override
  public String toString()
  {
    return getObject().toString();
  }

  @Override
  public boolean isReadOnly()
  {
    return _readOnly;
  }

  @Override
  public void setReadOnly()
  {
    _readOnly = true;
  }

  @Override
  public void invalidate()
  {
    try
    {
      if (_refCounted != null)
      {
        _refCounted.release();
      }
    }
    finally
    {
      _refCounted = null;
    }
  }

  /**
   * Add that does not invoke checker but does check for read-only, use with caution.
   *
   * This method skips all value checks.
   *
   * @param element provides the element to be added to the list.
   * @return true.
   * @throws UnsupportedOperationException if the list is read-only.
   */
  protected boolean addWithoutChecking(E element)
  {
    return getMutable().add(element);
  }

  /**
   * Set without checking, use with caution.
   *
   * This method skips all checks.
   *
   * @param index of the element to replace.
   * @param element to be stored at the specified position.
   * @return the element previously at the specified position.
   */
  protected E setWithoutChecking(int index, E element)
  {
    return getMutable().set(index, element);
  }

  /* Avoid use of finalize, causes GC issues.
  protected void finalize() throws Throwable
  {
    try
    {
      invalidate();
    }
    finally
    {
      super.finalize();
    }
  }
  */

  /**
   * For debugging use only, package scope.
   *
   * @return underlying {@link RefCounted}.
   */
  RefCounted<InternalList<E>> getRefCounted()
  {
    return _refCounted;
  }

  private final void check(E e)
  {
    if (_checker != null)
    {
      _checker.check(this, e);
    }
  }

  private final void checkAll(Collection<? extends E> c)
  {
    if (_checker != null)
    {
      for (E e : c)
      {
        _checker.check(this, e);
      }
    }
  }

  private final InternalList<E> getMutable()
  {
    if (_readOnly)
    {
      throw new UnsupportedOperationException("Cannot mutate a read-only list");
    }
    _refCounted = _refCounted.getMutable();
    return _refCounted.getObject();
  }

  protected final InternalList<E> getObject()
  {
    return _refCounted.getObject();
  }

  @SuppressWarnings("serial")
  private static class InternalList<E> extends ArrayList<E>
  {
    public InternalList()
    {
    }
    public InternalList(List<? extends E> l)
    {
      super(l);
    }
    public InternalList(int initialCapacity)
    {
      super(initialCapacity);
    }
    @Override
    public void removeRange(int fromIndex, int toIndex)
    {
      super.removeRange(fromIndex, toIndex);
    }
  }

  protected ListChecker<E> _checker;
  private boolean _readOnly = false;
  private RefCounted<InternalList<E>> _refCounted;
}