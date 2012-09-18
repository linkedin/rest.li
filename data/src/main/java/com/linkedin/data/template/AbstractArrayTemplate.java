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

package com.linkedin.data.template;


import com.linkedin.data.DataList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import java.util.AbstractList;


/**
 * Abstract {@link DataTemplate} for arrays.
 *
 * @param <E> the element type of the array.
 */
public abstract class AbstractArrayTemplate<E> extends AbstractList<E> implements DataTemplate<DataList>
{
  /**
   * Constructor.
   *
   * @param list is the underlying {@link DataList} that will be proxied by this {@link AbstractArrayTemplate}.
   * @param schema is the {@link DataSchema} of the array.
   * @param elementClass is the class of elements returned by this {@link AbstractArrayTemplate}.
   * @param dataClass is the class of elements stored in the underlying {@link DataList}.
   */
  protected AbstractArrayTemplate(DataList list, ArrayDataSchema schema, Class<E> elementClass, Class<?> dataClass)
  {
    _list = list;
    _schema = schema;
    _elementClass = elementClass;
    _dataClass = dataClass;
  }

  @Override
  public abstract boolean add(E element);

  @Override
  public abstract void add(int index, E element);

  @Override
  public abstract E get(int index);

  @Override
  public abstract E remove(int index);

  @Override
  public abstract void removeRange(int fromIndex, int toIndex);

  @Override
  public abstract E set(int index, E element);

  @Override
  public ArrayDataSchema schema()
  {
    return _schema;
  }

  @Override
  public DataList data()
  {
    return _list;
  }

  @Override
  public int size()
  {
    return _list.size();
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object instanceof AbstractArrayTemplate)
    {
      return ((AbstractArrayTemplate<?>) object).data().equals(_list);
    }
    return super.equals(object);
  }

  @Override
  public String toString()
  {
    return _list.toString();
  }

  /**
   * Return the underlying {@link DataList}'s hash code.
   *
   * @return the underlying {@link DataList}'s hash code.
   */
  @Override
  public int hashCode()
  {
    return _list.hashCode();
  }

  /**
   * Return a clone that is backed by a shallow copy of the underlying {@link DataList}.
   *
   * @return a clone that is backed by a shallow copy of the underlying {@link DataList}.
   * @throws CloneNotSupportedException if this object cannot be cloned.
   */
  @Override
  public AbstractArrayTemplate<E> clone() throws CloneNotSupportedException
  {
    @SuppressWarnings("unchecked")
    AbstractArrayTemplate<E> clone = (AbstractArrayTemplate<E>) super.clone();
    clone._list = clone._list.clone();
    return clone;
  }

  protected DataList _list;
  protected final ArrayDataSchema _schema;
  /**
   * Class of the elements of the array.
   */
  protected final Class<E> _elementClass;
  /**
   * Class of elements stored in the underlying {@link DataList}.
   */
  protected final Class<?> _dataClass;
}