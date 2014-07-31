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
 * $id$
 */
package com.linkedin.data.transform;

import com.linkedin.util.ArgumentUtil;

/**
 * Immutable list, which allows appending single element and conversion into
 * an array.
 * <p>
 * Append, size and hashCode are O(1). toArray and equals are O(n).
 *
 * @author jodzga
 *
 * @param <T>
 */
public class ImmutableList<T>
{
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static final ImmutableList EMPTY = new ImmutableList(null, null);
  private static final Object[] EMPTY_ARRAY = new Object[0];

  private final T _tail;
  private final ImmutableList<T> _head;
  private final int _size;
  private final int _hashCode;

  private ImmutableList(T tail, ImmutableList<T> head)
  {
    this._tail = tail;
    this._head = head;
    this._size = ((head != null) ? head._size : 0) + ((tail != null) ? 1 : 0);
    this._hashCode = calculateHashCode(tail, head);
  }

  private int calculateHashCode(T tail, ImmutableList<T> head)
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_head == null) ? 0 : _head.hashCode());
    result = prime * result + ((_tail == null) ? 0 : _tail.hashCode());
    return result;
  }

  /**
   * Create list containing single element. Throws NullPointerException if element is null.
   * @param element the content of newly created list
   */
  public ImmutableList(T element)
  {
    if (element == null)
      throw new NullPointerException();
    this._tail = element;
    this._head = empty();
    this._size = 1;
    this._hashCode = calculateHashCode(element, _head);
  }

  /**
   * Creates new list with appended element.
   * <p>O(1) complexity.
   *
   * @param element the element to append
   * @return new instance of list with appended
   */
  public ImmutableList<T> append(T element)
  {
    ArgumentUtil.notNull(element, "element");
    return new ImmutableList<T>(element, this);
  }

  /**
   * Returns an empty list.
   *
   * @param <T> the type
   * @return empty list
   */
  @SuppressWarnings("unchecked")
  public static final <T> ImmutableList<T> empty()
  {
    return (ImmutableList<T>) EMPTY;
  }

  /**
   * Returns an array containing all of the elements in this list
   * in proper sequence (from first to last element).
   *
   * <p>The returned array will be "safe" in that no references to it are
   * maintained by this list.  (In other words, this method must allocate
   * a new array).  The caller is thus free to modify the returned array.
   *
   * <p>This method acts as bridge between array-based and collection-based
   * APIs.
   *
   * <p>
   * O(n) complexity.
   *
   * @return an array containing all of the elements in this list
   *         in proper sequence
   */
  public Object[] toArray()
  {
    if (_size == 0)
      return EMPTY_ARRAY;
    else
    {
      Object[] result = new Object[_size];
      int index = _size - 1;
      ImmutableList<T> current = this;
      //uses while loop instead of recurrence to not use up stack
      while (current._tail != null)
      {
          result[index--] = current._tail;
          current = current._head;
      }
      return result;
    }
  }

  /**
   * Returns size of a list.
   * <p>
   * O(1) complexity.
   * @return size of a list
   */
  public int size()
  {
    return _size;
  }

  @Override
  public int hashCode()
  {
    return _hashCode;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    @SuppressWarnings("rawtypes")
    ImmutableList other = (ImmutableList) obj;
    if (_head == null)
    {
      if (other._head != null)
        return false;
    }
    else if (!_head.equals(other._head))
      return false;
    if (_tail == null)
    {
      if (other._tail != null)
        return false;
    }
    else if (!_tail.equals(other._tail))
      return false;
    return true;
  }
}
