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

package com.linkedin.data.element;


import com.linkedin.data.schema.DataSchema;


/**
 * {@link DataElement} with mutation access to change its fields.
 *
 * The mutation methods only change the values of fields in a {@link DataElement}.
 * They do not change affect the actual data objects referenced by this {@link DataElement}.
 */
public class MutableDataElement extends AbstractDataElement
{
  public MutableDataElement(Object value, Object name, DataSchema schema, DataElement parentElement)
  {
    _value = value;
    _name = name;
    _schema = schema;
    _parentElement = parentElement;
    _level = (parentElement == null ? 0 : parentElement.level() + 1);
  }

  public MutableDataElement(DataElement parentElement)
  {
    this(null, null, null, parentElement);
  }

  @Override
  public Object getValue()
  {
    return _value;
  }

  @Override
  public Object getName()
  {
    return _name;
  }

  @Override
  public DataSchema getSchema()
  {
    return _schema;
  }

  @Override
  public DataElement getParent()
  {
    return _parentElement;
  }

  @Override
  public int level()
  {
    return _level;
  }

  /**
   * Sets the reference to the value held by this instance.
   *
   * It does not actually change the actual value referenced by parent.
   *
   * @param value provides the reference to the new value.
   */
  public void setValue(Object value)
  {
    _value = value;
  }

  /**
   * Sets the name held by this instance.
   *
   * It does not actually change the name in the parent.
   *
   * @param name provides the new name.
   */
  public void setName(Object name)
  {
    _name = name;
  }

  /**
   * Sets the {@link DataSchema} held by this instance.
   *
   * It does not actually change the {@link DataSchema} of the value.
   *
   * @param schema provides the new {@link DataSchema}.
   */
  public void setSchema(DataSchema schema)
  {
    _schema = schema;
  }

  /**
   * Sets the value, name, and {@link DataSchema} held by this instance.
   *
   * It does not actually change the underlying data objects referenced by this {@link DataElement}.
   *
   * @param value provides the new value.
   * @param name provides the new name.
   * @param schema provides the new {@link DataSchema}.
   */
  public void setValueNameSchema(Object value, Object name, DataSchema schema)
  {
    _value = value;
    _name = name;
    _schema = schema;
  }

  protected Object _value;
  protected Object _name;
  protected DataSchema _schema;
  protected DataElement _parentElement;
  protected int _level;
}
