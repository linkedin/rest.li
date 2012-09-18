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
 * Checked List.
 * <p>
 *
 * It enables a {@link ListChecker} to be added to check
 * keys and values being stored into the {@link CheckedList}.
 * <p>
 *
 * The underlying list implementation is {@link ArrayList}. It delegates
 * list operations to the underlying [@link ArrayList} associated
 * with this {@link CheckedList}.
 * <p>
 *
 * A {@link CheckedList} may be marked read-only to disable mutations,
 * and to avoid unintentional changes. It may also be invalidated to
 * release its reference and decrease the reference count on the underlying
 * {@link ArrayList}.
 * <P>
 *
 * @author slim
 */
public class CheckedList<E> extends AbstractList<E> implements CommonList<E>, Cloneable
{
  /**
   * Construct an empty list.
   */
  public CheckedList()
  {
    _checker = null;
    _list = new InternalList<E>();
  }

  /**
   * Construct a new list with elements provided by the specified list.
   *
   * @param list provides the elements to be added to the new list.
   */
  public CheckedList(List<? extends E> list)
  {
    _checker = null;
    checkAll(list);
    _list = new InternalList<E>(list);
  }

  /**
   * Construct a new list with the specified initial capacity.
   *
   * @param initialCapacity provides the initial capacity.
   */
  public CheckedList(int initialCapacity)
  {
    _checker = null;
    _list = new InternalList<E>(initialCapacity);
  }

  /**
   * Construct an empty list with the specified {@link ListChecker}.
   *
   * @param checker provides the {@link ListChecker}.
   */
  public CheckedList(ListChecker<E> checker)
  {
    _checker = checker;
    _list = new InternalList<E>();
  }

  /**
   * Construct a new list with elements provided by the specified list and
   * the specified {@link ListChecker}.
   *
   * @param list provides the elements to be added to the new list.
   * @param checker provides the {@link ListChecker}.
   */
  public CheckedList(List<? extends E> list, ListChecker<E> checker)
  {
    _checker = checker;
    checkAll(list);
    _list = new InternalList<E>(list);
  }

  /**
   * Construct a new list with the specified initial capacity and
   * specified {@link ListChecker}.
   *
   * @param initialCapacity provides the initial capacity.
   * @param checker provides the {@link ListChecker}.
   */
  public CheckedList(int initialCapacity, ListChecker<E> checker)
  {
    _checker = checker;
    _list = new InternalList<E>(initialCapacity);
  }

  @Override
  public boolean add(E e)
  {
    check(e);
    checkMutability();
    return _list.add(e);
  }

  @Override
  public void add(int index, E element)
  {
    check(element);
    checkMutability();
    _list.add(index, element);
  }

  @Override
  public boolean addAll(Collection<? extends E> c)
  {
    checkAll(c);
    checkMutability();
    return _list.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c)
  {
    checkAll(c);
    checkMutability();
    return _list.addAll(index, c);
  }

  @Override
  public void clear()
  {
    checkMutability();
    _list.clear();
  }

  @Override
  @SuppressWarnings("unchecked")
  public CheckedList<E> clone() throws CloneNotSupportedException
  {
    CheckedList<E> o = (CheckedList<E>) super.clone();
    o._list = (InternalList<E>) _list.clone();
    o._readOnly = false;
    return o;
  }

  @Override
  public boolean contains(Object o)
  {
    return _list.contains(o);
  }

  @Override
  public boolean containsAll(Collection<?> c)
  {
    return _list.containsAll(c);
  }

  @Override
  public boolean equals(Object object)
  {
    return _list.equals(object);
  }

  @Override
  public E get(int index)
  {
    return _list.get(index);
  }

  @Override
  public int hashCode()
  {
    return _list.hashCode();
  }

  @Override
  public int indexOf(Object o)
  {
    return _list.indexOf(o);
  }

  @Override
  public boolean isEmpty()
  {
    return _list.isEmpty();
  }

  @Override
  public int lastIndexOf(Object o)
  {
    return _list.lastIndexOf(o);
  }

  @Override
  public E remove(int index)
  {
    checkMutability();
    return _list.remove(index);
  }

  @Override
  public boolean remove(Object o)
  {
    checkMutability();
    return _list.remove(o);
  }

  @Override
  public boolean removeAll(Collection<?> c)
  {
    checkMutability();
    return _list.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c)
  {
    checkMutability();
    return _list.retainAll(c);
  }

  @Override
  public void removeRange(int fromIndex, int toIndex)
  {
    checkMutability();
    _list.removeRange(fromIndex, toIndex);
  }

  @Override
  public E set(int index, E element)
  {
    check(element);
    checkMutability();
    return _list.set(index, element);
  }

  @Override
  public int size()
  {
    return _list.size();
  }

  @Override
  public Object[] toArray()
  {
    return _list.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a)
  {
    return _list.toArray(a);
  }

  @Override
  public String toString()
  {
    return _list.toString();
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
    _list = null;
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
    checkMutability();
    return _list.add(element);
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
    checkMutability();
    return _list.set(index, element);
  }

  private final void checkMutability()
  {
    if (_readOnly)
    {
      throw new UnsupportedOperationException("Cannot mutate a read-only list");
    }
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

  /**
   * Unit test use only.
   *
   * @return underlying map.
   */
  protected final List<E> getObject()
  {
    return _list;
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
  private InternalList<E> _list;
}