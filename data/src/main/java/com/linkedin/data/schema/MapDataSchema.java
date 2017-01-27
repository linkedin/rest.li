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
 * {@link DataSchema} for map.
 *
 * @author slim
 */
public final class MapDataSchema extends ComplexDataSchema
{
  public MapDataSchema(DataSchema values)
  {
    super(Type.MAP);
    setValues(values);
  }

  /**
   * Set the {@link DataSchema} of values in the map.
   *
   * @param values is the {@link DataSchema} of values in the map.
   */
  void setValues(DataSchema values)
  {
    if (values == null)
    {
      _values = DataSchemaConstants.NULL_DATA_SCHEMA;
      setHasError();
    }
    else
    {
      _values = values;
    }
  }

  /**
   * Return the {@link DataSchema} for the map's values.
   *
   * @return the {@link DataSchema} for the map's values.
   */
  public DataSchema getValues()
  {
    return _values;
  }

  /**
   * Sets if the values type is declared inline in the schema.
   * @param valuesDeclaredInline true if the values type is declared inline, false if it is referenced by name.
   */
  public void setValuesDeclaredInline(boolean valuesDeclaredInline)
  {
    _valuesDeclaredInline = valuesDeclaredInline;
  }

  /**
   * Checks if the values type is declared inline.
   * @return true if the values type is declared inline, false if it is referenced by name.
   */
  public boolean isValuesDeclaredInline()
  {
    return _valuesDeclaredInline;
  }

  @Override
  public String getUnionMemberKey()
  {
    return "map";
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object.getClass() == MapDataSchema.class)
    {
      MapDataSchema other = (MapDataSchema) object;
      return super.equals(other) && _values.equals(other._values);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() ^ _values.hashCode();
  }

  private DataSchema _values = DataSchemaConstants.NULL_DATA_SCHEMA;
  private boolean _valuesDeclaredInline = false;
}