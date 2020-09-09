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
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.util.ArgumentUtil;

/**
 * Abstract class for arrays of value types that do not require proxying by a {@link DataTemplate}.
 *
 * @param <E> is the element type of the array.
 */
public abstract class DirectArrayTemplate<E> extends AbstractArrayTemplate<E>
{
  /**
   * Constructor.
   *
   * This constructor is retained for backwards compatibility.
   *
   * @param list is the underlying {@link DataList} that will be proxied by this {@link DirectArrayTemplate}.
   * @param schema is the {@link DataSchema} of the array.
   * @param elementClass is the class of elements returned by this {@link DirectArrayTemplate}.
   */
  protected DirectArrayTemplate(DataList list, ArrayDataSchema schema, Class<E> elementClass)
  {
    this(list, schema, elementClass, elementClass.isEnum() ? String.class : elementClass);
  }

  /**
   * Constructor.
   *
   * @param list is the underlying {@link DataList} that will be proxied by this {@link DirectArrayTemplate}.
   * @param schema is the {@link DataSchema} of the array.
   * @param elementClass is the class of elements returned by this {@link DirectArrayTemplate}.
   * @param dataClass is the class of elements stored in the underlying {@link DataList}.
   */
  protected DirectArrayTemplate(DataList list, ArrayDataSchema schema, Class<E> elementClass, Class<?> dataClass)
  {
    super(list, schema, elementClass, dataClass);
  }

  @Override
  public boolean add(E element) throws ClassCastException
  {
    return CheckedUtil.addWithoutChecking(_list, safeCoerceInput(element));
  }

  @Override
  public void add(int index, E element) throws ClassCastException
  {
    CheckedUtil.addWithoutChecking(_list, index, safeCoerceInput(element));
  }

  @Override
  public E get(int index) throws TemplateOutputCastException
  {
    return coerceOutput(_list.get(index));
  }

  @Override
  public E remove(int index) throws TemplateOutputCastException
  {
    return coerceOutput(_list.remove(index));
  }

  @Override
  public void removeRange(int fromIndex, int toIndex)
  {
    _list.removeRange(fromIndex, toIndex);
  }

  @Override
  public E set(int index, E element) throws ClassCastException, TemplateOutputCastException
  {
    return coerceOutput(CheckedUtil.setWithoutChecking(_list, index, safeCoerceInput(element)));
  }

  @SuppressWarnings("unchecked")
  private Object safeCoerceInput(Object object) throws ClassCastException
  {
    //
    // This UGLY hack is needed because we have code that expects some types to be artificially inter-fungible
    // and even tests for it, for example coercing between number types.
    //
    ArgumentUtil.notNull(object, "object");
    if (object.getClass() != _elementClass)
    {
      return DataTemplateUtil.coerceInput((E) object, _elementClass, _dataClass);
    }
    else
    {
      return coerceInput((E) object);
    }
  }

  protected Object coerceInput(E object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    return DataTemplateUtil.coerceInput(object, _elementClass, _dataClass);
  }

  protected E coerceOutput(Object object) throws TemplateOutputCastException
  {
    assert(object != null);
    return DataTemplateUtil.coerceOutput(object, _elementClass);
  }
}
