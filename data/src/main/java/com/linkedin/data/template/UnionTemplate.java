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

import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.UnionDataSchema;

/**
 * Abstract {@link DataTemplate} for unions.
 * <p>
 *
 * A {@link UnionTemplate} may represent either a null union, i.e.
 * it does not have value, or a union with a value.
 * A null union's underlying Data object is {@link Data#NULL}.
 * A union with a value's underlying Data object is a {@link DataMap}.
 * The {@link DataMap} should have exactly one entry. The key of the
 * entry provides the type of the value, and the value of the
 * entry provides the value of the union.
 * <p>
 *
 * Because the underlying Data object of a null union and an union
 * with a value are different. The {@link UnionTemplate} does not
 * permit subsequent conversion of a null union to an union with a value.
 * Methods that get and set a value will throw an
 * {@link NullUnionUnsupportedOperationException} when they are
 * invoked on a null union.
 * <p>
 *
 * This class provides methods to get and set members belonging to the union.
 * There are two sets of these methods, the direct and wrapping methods.
 * <p>
 *
 * The direct methods ({@link #selectDirect(com.linkedin.data.schema.DataSchema, Class, String, Object)}
 * and {@link #obtainDirect(com.linkedin.data.schema.DataSchema, Class, String)}) provide access
 * to members whose values do not require {@link DataTemplate}'s to proxy access.
 * The direct methods are used typically to access primitive types.
 * Enum field values are also accessed using the direct methods.
 * <p>
 *
 * The wrapping methods ({@link #selectWrapped(com.linkedin.data.schema.DataSchema, Class, String, DataTemplate)}
 * and {@link #obtainWrapped(com.linkedin.data.schema.DataSchema, Class, String)}) provide access
 * to members whose values require {@link DataTemplate}'s to proxy access.
 * The arrays, unions, records, and fixed field values are accessed
 * using the wrapping methods.
 */
public class UnionTemplate implements DataTemplate<Object>
{
  /**
   * Constructor.
   *
   * @param object backing the union.
   * @param schema of the union.
   * @throws TemplateOutputCastException if the provided object is not {@code null}, {@link Data#NULL}, or a {@link DataMap}.
   */
  protected UnionTemplate(Object object, UnionDataSchema schema) throws TemplateOutputCastException
  {
    if (object == null || object == Data.NULL)
    {
      _map = null;
    }
    else if (object.getClass() == DataMap.class)
    {
      _map = (DataMap) object;
    }
    else
    {
      throw new TemplateOutputCastException("Union not null or a DataMap");
    }
    _data = object;
    _schema = schema;
    _cache = null;
  }

  @Override
  public DataSchema schema()
  {
    return _schema;
  }

  @Override
  public Object data()
  {
    return _map == null ? Data.NULL : _map;
  }

  /**
   * Returns the {@link DataSchema} of the union's current value.
   *
   * @return the {@link DataSchema} of the union's current value.
   * @throws TemplateOutputCastException if the underlying {@link DataMap}
   *                                      does not have exactly one entry
   *                                      or the key of the entry does
   *                                      identify a member type within
   *                                      the {@link DataSchema} of the union.
   */
  public DataSchema memberType() throws TemplateOutputCastException
  {
    String key = null;
    if (_map == null)
    {
      key = DataSchemaConstants.NULL_TYPE;
    }
    else if (_map.size() != 1)
    {
      throw new TemplateOutputCastException("DataMap of union does not have exactly one field: " + _map);
    }
    else
    {
      key = _map.keySet().iterator().next();
    }
    DataSchema memberType = _schema.getType(key);
    if (memberType == null)
    {
      throw new TemplateOutputCastException(key + " is not a member of " + _schema);
    }
    return memberType;
  }

  /**
   * Returns whether the type of the current value is identified by the specified key.
   *
   * The type of the current value is identified by the specified key if the
   * underlying {@link DataMap} has a single entry and the entry's key equals the
   * specified key.
   *
   * For a null union, this method will always return false.
   *
   * @param key to check.
   * @return true if the current value is identified by the specified key.
   */
  public boolean memberIs(String key)
  {
    return (_map != null && _map.size() == 1 && _map.get(key) != null);
  }

  /**
   * Return whether this {@link UnionTemplate} represents a null union.
   *
   * @return true if this {@link UnionTemplate} represents a null union.
   */
  public boolean isNull()
  {
    return _map == null;
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object instanceof UnionTemplate)
    {
      return ((UnionTemplate) object).data().equals(data());
    }
    return false;
  }

  @Override
  public UnionTemplate clone() throws CloneNotSupportedException
  {
    UnionTemplate clone = (UnionTemplate) super.clone();
    if (clone._map != null)
    {
      clone._map = clone._map.clone();
      clone._data = clone._map;
    }
    else
    {
      assert(clone._data == Data.NULL);
    }
    return clone;
  }

  @Override
  public UnionTemplate copy() throws CloneNotSupportedException
  {
    UnionTemplate copy = (UnionTemplate) super.clone();
    if (copy._map != null)
    {
      copy._map = copy._map.copy();
      copy._data = copy._map;
      copy._cache = null;
    }
    else
    {
      assert(copy._data == Data.NULL);
    }
    return copy;
  }

  @Override
  public int hashCode()
  {
    return data().hashCode();
  }

  @Override
  public String toString()
  {
    return data().toString();
  }

  /**
   * Throws {@link NullUnionUnsupportedOperationException} if the union is a null union.
   *
   * @throws NullUnionUnsupportedOperationException if the union is a null union.
   */
  protected void checkNotNull() throws NullUnionUnsupportedOperationException
  {
    if (_map == null)
    {
      throw new NullUnionUnsupportedOperationException("Operation not supported on a null union");
    }
  }

  /**
   * Set a new value into the union.
   *
   * This is method is retained for backwards compatibility with older generated code.
   *
   * @see #selectDirect(com.linkedin.data.schema.DataSchema, Class, Class, String, Object)
   */
  protected <T> void selectDirect(DataSchema memberSchema, Class<T> memberClass, String key, T value)
    throws ClassCastException, NullUnionUnsupportedOperationException
  {
    selectDirect(memberSchema, memberClass, memberClass.isEnum() ? String.class : memberClass, key, value);
  }

  /**
   * Set a new value into the union.
   *
   * This is a direct method. The value is not a {@link DataTemplate}.
   *
   * @param memberSchema provides the {@link DataSchema} of the new value.
   * @param memberClass provides the expected class of the value.
   * @param dataClass provides the class stored in the underlying {@link DataMap}.
   * @param key provides the key that identifies the type of the value.
   * @param value provides the value to set.
   * @param <T> type of the value.
   * @throws ClassCastException if the input value does not match the
   *                            expected class and the value cannot be coerced to the
   *                            expected class.
   * @throws NullUnionUnsupportedOperationException if the union is a null union.
   */
  protected <T> void selectDirect(DataSchema memberSchema, Class<T> memberClass, Class<?> dataClass, String key, T value)
      throws ClassCastException, NullUnionUnsupportedOperationException
  {
    checkNotNull();
    DataSchema memberType = _schema.getType(key);
    assert(memberType != null); // something is wrong with the generated code if this occurs.
    Object object = DataTemplateUtil.coerceInput(value, memberClass, dataClass);
    _map.clear();
    _map.put(key, object);
  }

  /**
   * Set a new value into the union whose type needs to be coerced by {@link DirectCoercer}.
   *
   * @param memberSchema provides the {@link DataSchema} of the new value.
   * @param memberClass provides the expected class of the value.
   * @param dataClass provides the class stored in the underlying {@link DataMap}.
   * @param key provides the key that identifies the type of the value.
   * @param value provides the value to set.
   * @param <T> type of the value.
   * @throws ClassCastException if the input value does not match the
   *                            expected class and the value cannot be coerced to the
   *                            expected class.
   * @throws NullUnionUnsupportedOperationException if the union is a null union.
   */
  protected <T> void selectCustomType(DataSchema memberSchema, Class<T> memberClass, Class<?> dataClass, String key, T value)
      throws ClassCastException, NullUnionUnsupportedOperationException
  {
    checkNotNull();
    DataSchema memberType = _schema.getType(key);
    assert(memberType != null); // something is wrong with the generated code if this occurs.
    Object object = DataTemplateUtil.coerceInput(value, memberClass, dataClass);
    _map.clear();
    _map.put(key, object);
    _customTypeCache = value;
  }

  /**
   * Set a new value into the union.
   *
   * This is a wrapping method. The value is a {@link DataTemplate}.
   *
   * @param memberSchema provides the {@link DataSchema} of the new value.
   * @param memberClass provides the expected class of the value.
   * @param key provides the key that identifies the type of the value.
   * @param value provides the value to set.
   * @param <T> type of the value.
   * @throws ClassCastException if input value does not match the expected class.
   * @throws NullUnionUnsupportedOperationException if the union is a null union.
   */
  protected <T extends DataTemplate<?>> void selectWrapped(DataSchema memberSchema, Class<T> memberClass, String key, T value)
      throws ClassCastException, NullUnionUnsupportedOperationException
  {
    checkNotNull();
    DataSchema memberType = _schema.getType(key);
    assert(memberType != null); // something is wrong with the generated code if this occurs.
    if (value.getClass() != memberClass)
    {
      throw new ClassCastException("Input " + value + " should be a " + memberClass.getName());
    }
    _map.clear();
    _map.put(key, value.data());
    _cache = value;
  }

  /**
   * Get the value of a specified type.
   *
   * This provides a type-safe get for a particular member type
   * of the union. It returns the value if the current value is
   * of the specified type, or null if the current value is
   * of a different type.
   *
   * This is a direct method. The value is not a {@link DataTemplate}.
   *
   * @param memberSchema provides the {@link DataSchema} of the value.
   * @param memberClass provides the expected class of the value.
   * @param key provides the key that identifies the type of the value.
   * @param <T> type of the value.
   * @return the value if the type of the current value is identified
   *         by the specified key, else return null.
   * @throws NullUnionUnsupportedOperationException if the union is a null union.
   * @throws TemplateOutputCastException if the value identified by the
   *                                     key is not the expected class and the value
   *                                     cannot be coerced to the expected class.
   */
  protected <T> T obtainDirect(DataSchema memberSchema, Class<T> memberClass, String key)
      throws NullUnionUnsupportedOperationException, TemplateOutputCastException
  {
    checkNotNull();
    Object found = _map.get(key);
    if (found == null)
    {
      return null;
    }
    return DataTemplateUtil.coerceOutput(found, memberClass);
  }

  /**
   * Get the value of a specified type which needs to be coerced by {@link DirectCoercer}.
   *
   * @param memberSchema provides the {@link DataSchema} of the value.
   * @param memberClass provides the expected class of the value.
   * @param key provides the key that identifies the type of the value.
   * @param <T> type of the value.
   * @return the value if the type of the current value is identified
   *         by the specified key, else return null.
   * @throws NullUnionUnsupportedOperationException if the union is a null union.
   * @throws TemplateOutputCastException if the value identified by the
   *                                     key is not the expected class and the value
   *                                     cannot be coerced to the expected class.
   */
  protected <T> T obtainCustomType(DataSchema memberSchema, Class<T> memberClass, String key)
      throws NullUnionUnsupportedOperationException, TemplateOutputCastException
  {
    checkNotNull();
    T coerced;
    Object found = _map.get(key);
    if (found == null)
    {
      return null;
    }
    // the underlying data type of the custom typed field should be immutable, thus checking class equality suffices
    else if (_customTypeCache != null && _customTypeCache.getClass() == memberClass)
    {
      coerced = memberClass.cast(_customTypeCache);
    }
    else
    {
      coerced = DataTemplateUtil.coerceOutput(found, memberClass);
      _customTypeCache = coerced;
    }
    return coerced;
  }

  /**
   * Get the value of a specified type.
   *
   * This provides a type-safe get for a particular member type
   * of the union. It returns the value if the current value is
   * of the specified type, or null if the current value is
   * of a different type.
   *
   * This is a wrapping method. The value is a {@link DataTemplate}.
   *
   * @param memberSchema provides the {@link DataSchema} of the value.
   * @param memberClass provides the expected class of the value.
   * @param key provides the key that identifies the type of the value.
   * @param <T> type of the value.
   * @return the value if the type of the current value is identified
   *         by the specified key, else return null.
   * @throws NullUnionUnsupportedOperationException if the union is a null union.
   * @throws TemplateOutputCastException if a {@link DataTemplate} cannot be
   *                                     instantiated to wrap the value object
   *                                     identified by the key.
   */
  protected <T extends DataTemplate<?>> T obtainWrapped(DataSchema memberSchema, Class<T> memberClass, String key)
      throws NullUnionUnsupportedOperationException, TemplateOutputCastException
  {
    checkNotNull();
    T wrapped;
    Object found = _map.get(key);
    if (found == null)
    {
      wrapped = null;
    }
    else if (_cache != null && _cache.data() == found)
    {
      wrapped = memberClass.cast(_cache);
    }
    else
    {
      wrapped = DataTemplateUtil.wrap(found, memberSchema, memberClass);
      _cache = wrapped;
    }
    return wrapped;
  }

  protected Object _data;
  protected DataMap _map;
  protected final UnionDataSchema _schema;
  protected DataTemplate<?> _cache;
  protected Object _customTypeCache;
}
