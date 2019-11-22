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
  public void setValues(DataSchema values)
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
   * Return the {@link DataSchema} for the map's key.
   *
   * @return the {@link DataSchema} for the map's key, which is only {@link StringDataSchema}
   *
   */
  public StringDataSchema getKey()
  {
    return _key;
  }

  /**
   * Use this setter function to set a {@link StringDataSchema} as the key schema for this {@link MapDataSchema}.
   * By default the key dataSchema would be the constant StringDataSchema {@link DataSchemaConstants#STRING_DATA_SCHEMA}
   *
   * @param key the StringDataSchema that should be set as the map key schema, for example, a StringDataSchema object that
   *            have non-empty properties.
   *
   */
  public void setKey(StringDataSchema key)
  {
    if (key == null)
    {
      _key = DataSchemaConstants.STRING_DATA_SCHEMA;
      setHasError();
    }
    else
    {
      _key = key;
    }
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
      return super.equals(other) && _values.equals(other._values) && _key.equals(other._key);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() ^ _values.hashCode() ^ _key.hashCode();
  }

  private DataSchema _values = DataSchemaConstants.NULL_DATA_SCHEMA;
  private StringDataSchema _key = DataSchemaConstants.STRING_DATA_SCHEMA;
  private boolean _valuesDeclaredInline = false;
}