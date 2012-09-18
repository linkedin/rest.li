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

package com.linkedin.data.schema;

/**
 * {@link DataSchema} for array.
 *
 * @author slim
 */
public final class ArrayDataSchema extends ComplexDataSchema
{
  public ArrayDataSchema(DataSchema items)
  {
    super(Type.ARRAY);
    setItems(items);
  }

  /**
   * Set the {@link DataSchema} of items in the array.
   *
   * @param items is the {@link DataSchema} of items in the array.
   */
  void setItems(DataSchema items)
  {
    if (items == null)
    {
      _items = DataSchemaConstants.NULL_DATA_SCHEMA;
      setHasError();
    }
    else
    {
      _items = items;
    }
  }

  /**
   * Return the {@link DataSchema} for the array's items.
   *
   * @return the {@link DataSchema} for the array's items.
   */
  public DataSchema getItems()
  {
    return _items;
  }

  @Override
  public String getUnionMemberKey()
  {
    return "array";
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object.getClass() == ArrayDataSchema.class)
    {
      ArrayDataSchema other = (ArrayDataSchema) object;
      return super.equals(other) && _items.equals(other._items);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() ^ _items.hashCode();
  }

  private DataSchema _items = DataSchemaConstants.NULL_DATA_SCHEMA;
}