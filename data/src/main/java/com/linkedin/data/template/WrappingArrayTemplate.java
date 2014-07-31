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
import com.linkedin.data.template.DataObjectToObjectCache;
import com.linkedin.util.ArgumentUtil;
import java.lang.reflect.Constructor;


/**
 * Abstract class for array of value types that require proxying by a {@link DataTemplate}.
 *
 * @param <E> is the element type of the array.
 */
public class WrappingArrayTemplate<E extends DataTemplate<?>> extends AbstractArrayTemplate<E>
{
  /**
   * Constructor.
   *
   * @param list is the underlying {@link DataList} that will be proxied by this {@link WrappingArrayTemplate}.
   * @param schema is the {@link DataSchema} of the array.
   * @param elementClass is the class of elements returned by this {@link WrappingArrayTemplate}.
   */
  protected WrappingArrayTemplate(DataList list, ArrayDataSchema schema, Class<E> elementClass)
      throws TemplateOutputCastException
  {
    super(list, schema, elementClass, DataList.class);
    _constructor = DataTemplateUtil.templateConstructor(elementClass, schema.getItems());
    _cache = new DataObjectToObjectCache<E>(data().size());
  }

  @Override
  public boolean add(E element) throws ClassCastException
  {
    Object unwrapped;
    boolean result = _list.add(unwrapped = unwrap(element));
    _cache.put(unwrapped, element);
    modCount++;
    return result;
  }

  @Override
  public void add(int index, E element) throws ClassCastException
  {
    Object unwrapped;
    _list.add(index, unwrapped = unwrap(element));
    _cache.put(unwrapped, element);
    modCount++;
  }

  @Override
  public E get(int index) throws TemplateOutputCastException
  {
    return cacheLookup(_list.get(index), index);
  }

  @Override
  public E remove(int index) throws TemplateOutputCastException
  {
    Object removed = _list.remove(index);
    modCount++;
    return cacheLookup(removed, -1);
  }

  @Override
  public void removeRange(int fromIndex, int toIndex)
  {
    _list.removeRange(fromIndex, toIndex);
    modCount++;
  }

  @Override
  public E set(int index, E element) throws ClassCastException, TemplateOutputCastException
  {
    Object replaced = _list.set(index, unwrap(element));
    modCount++;
    return cacheLookup(replaced, -1);
  }

  @Override
  public WrappingArrayTemplate<E> clone() throws CloneNotSupportedException
  {
    WrappingArrayTemplate<E> clone = (WrappingArrayTemplate<E>) super.clone();
    clone._cache = clone._cache.clone();
    return clone;
  }

  @Override
  public WrappingArrayTemplate<E> copy() throws CloneNotSupportedException
  {
    @SuppressWarnings("unchecked")
    WrappingArrayTemplate<E> copy = (WrappingArrayTemplate<E>) super.copy();
    copy._cache = new DataObjectToObjectCache<E>(copy.data().size());
    return copy;
  }

  /**
   * Obtain the underlying Data object of the {@link DataTemplate} object.
   *
   * This method checks that the provided object's class is
   * the element class of the {@link WrappingArrayTemplate}.
   *
   * @param object provides the input {@link DataTemplate} object.
   * @return the underlying Data object.
   * @throws ClassCastException if the object's class is not the
   *                            element class of the {@link WrappingArrayTemplate}.
   */
  protected Object unwrap(E object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    if (object.getClass() == _elementClass)
    {
      return object.data();
    }
    else
    {
      throw new ClassCastException("Input " + object + " should be a " + _elementClass.getName());
    }
  }

  /**
   * Lookup the {@link DataTemplate} for a Data object, if not cached,
   * create a {@link DataTemplate} for the Data object and add it to the cache.
   *
   * @param object is the Data object.
   * @param index of the Data object in the underlying {@link DataList},
   *        if index is -1, then the Data object is being removed
   *        from the underlying {@link DataList}.
   * @return the {@link DataTemplate} that proxies the Data object.
   * @throws TemplateOutputCastException if the object cannot be wrapped.
   */
  protected E cacheLookup(Object object, int index) throws TemplateOutputCastException
  {
    E wrapped;
    assert(object != null);
    if ((wrapped = _cache.get(object)) == null || wrapped.data() != object)
    {
      wrapped = DataTemplateUtil.wrap(object, _constructor);
      if (index != -1)
      {
        _cache.put(object, wrapped);
      }
    }
    return wrapped;
  }

  protected final Constructor<E> _constructor;
  protected DataObjectToObjectCache<E> _cache;
}

