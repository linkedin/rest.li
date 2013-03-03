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

import java.util.IdentityHashMap;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;

/**
 * Abstract {@link DataTemplate} for records.
 * <p>
 *
 * It provides methods to get and set fields belonging to the record.
 * There are two sets of these methods, the direct and wrapping methods.
 * <p>
 *
 * The direct methods ({@link #putDirect(com.linkedin.data.schema.RecordDataSchema.Field, Class, Object)}
 * and {@link #obtainDirect(com.linkedin.data.schema.RecordDataSchema.Field, Class, GetMode)}) provide access
 * to fields whose values do not require DataTemplate's proxy to access.
 * The direct methods are used typically to access primitive types.
 * Enum field values are also accessed using the direct methods.
 * <p>
 *
 * The wrapping methods ({@link #putWrapped(com.linkedin.data.schema.RecordDataSchema.Field, Class, DataTemplate)}
 * and {@link #obtainWrapped(com.linkedin.data.schema.RecordDataSchema.Field, Class, GetMode)}) provide access
 * to fields whose values require DataTemplate's to proxy access.
 * The arrays, unions, records, and fixed field values are accessed
 * using the wrapping methods.
 */
public abstract class RecordTemplate implements DataTemplate<DataMap>
{
  protected RecordTemplate(DataMap map, RecordDataSchema schema)
  {
    _map = map;
    _schema = schema;
  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

  @Override
  public DataMap data()
  {
    return _map;
  }

  @Override
  @SuppressWarnings("unchecked")
  public RecordTemplate clone() throws CloneNotSupportedException
  {
    RecordTemplate clone = (RecordTemplate) super.clone();
    clone._map = clone._map.clone();
    clone._cache = (IdentityHashMap<Object, DataTemplate<?>>) clone._cache.clone();
    return clone;
  }

  /**
   * Returns a deep copy of the {@link RecordTemplate}.
   *
   * This method copies the underlying {@link DataMap}.
   * The copied {@link RecordTemplate} proxies the new copied Data object.
   *
   * Since copying an underlying {@link DataMap} performs a deep copy, this method has the semantics of a deep copy.
   *
   * @return a deep copy of the RecordTemplate.
   * @throws CloneNotSupportedException if the {@link RecordTemplate} or
   *                                    its underlying {@link DataMap}
   *                                    cannot be copied.
   */
  public RecordTemplate copy() throws CloneNotSupportedException
  {
    RecordTemplate copy = (RecordTemplate) super.clone();
    copy._map = _map.copy();
    copy._cache = new IdentityHashMap<Object, DataTemplate<?>>();
    return copy;
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object instanceof RecordTemplate)
    {
      return ((RecordTemplate) object)._map.equals(_map);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return _map.hashCode();
  }

  @Override
  public String toString()
  {
    return _map.toString();
  }

  /**
   * Returns whether the specified field is present.
   *
   * @param field to check.
   * @return whether the specified field is present.
   */
  protected boolean contains(RecordDataSchema.Field field)
  {
    return _map.containsKey(field.getName());
  }

  /**
   * Remove a field from the record.
   *
   * @param field to remove.
   * @return true if the field was removed.
   */
  protected boolean remove(RecordDataSchema.Field field)
  {
    return _map.remove(field.getName()) != null;
  }

  /**
   * Set the value of field.
   *
   * This is direct method. The value is not a {@link DataTemplate}.
   *
   * @see SetMode
   *
   * @param field provides the field to set.
   * @param valueClass provides the expected class of the input value.
   * @param dataClass provides the class stored in the underlying {@link DataMap}.
   * @param object provides the value to set.
   * @param mode determines how should happen if the value provided is null.
   * @param <T> is the type of the object.
   * @throws ClassCastException if provided object is not the same as the expected class or
   *                            it cannot be coerced to the expected class.
   * @throws NullPointerException if null is not allowed, see {@link SetMode#DISALLOW_NULL}.
   * @throws IllegalArgumentException if attempting to remove a mandatory field by setting it to null,
   *                                  see {@link SetMode#REMOVE_OPTIONAL_IF_NULL}.
   */
  protected <T> void putDirect(RecordDataSchema.Field field, Class<T> valueClass, Class<?> dataClass, T object, SetMode mode)
      throws ClassCastException
  {
    if (checkPutNullValue(field, object, mode))
    {
      _map.put(field.getName(), DataTemplateUtil.coerceInput(object, valueClass, dataClass));
    }
  }

  /**
   * Set the value of field.
   *
   * This is direct method. The value is not a {@link DataTemplate}.
   * This method is used by older clients and cannot be removed without breaking
   * backwards compatibility.
   *
   * @see SetMode
   *
   * @param field provides the field to set.
   * @param valueClass provides the expected class of the input value.
   * @param object provides the value to set.
   * @param mode determines how should happen if the value provided is null.
   * @param <T> is the type of the object.
   * @throws ClassCastException if provided object is not the same as the expected class or
   *                            it cannot be coerced to the expected class.
   * @throws NullPointerException if null is not allowed, see {@link SetMode#DISALLOW_NULL}.
   * @throws IllegalArgumentException if attempting to remove a mandatory field by setting it to null,
   *                                  see {@link SetMode#REMOVE_OPTIONAL_IF_NULL}.
   */
  protected <T> void putDirect(RecordDataSchema.Field field, Class<T> valueClass, T object, SetMode mode)
    throws ClassCastException
  {
    putDirect(field, valueClass, valueClass.isEnum() ? String.class : valueClass, object, mode);
  }

  /**
   * Set the value of field with {@link SetMode#DISALLOW_NULL}.
   *
   * This is direct method. The value is not a {@link DataTemplate}.
   * This method is used by older clients and cannot be removed without breaking
   * backwards compatibility.
   *
   * @param field provides the field to set.
   * @param valueClass provides the expected class of the input value.
   * @param object provides the value to set.
   * @param <T> is the type of the object.
   * @throws ClassCastException if provided object is not the same as the expected class or
   *                            it cannot be coerced to the expected class.
   * @throws NullPointerException if null is not allowed, see {@link SetMode#DISALLOW_NULL}.
   */
  protected <T> void putDirect(RecordDataSchema.Field field, Class<T> valueClass, T object)
      throws ClassCastException
  {
    putDirect(field, valueClass, object, SetMode.DISALLOW_NULL);
  }

  /**
   * Set the value of field.
   *
   * This is wrapping method. The value is a {@link DataTemplate}.
   *
   * @see SetMode
   *
   * @param field provides the field to set.
   * @param valueClass provides the expected class of the input value.
   * @param object provides the value to set.
   * @param mode determines how should happen if the value provided is null.
   * @param <T> is the type of the input object.
   * @throws ClassCastException if class of the provided value is not the same as the expected class.
   * @throws NullPointerException if null is not allowed, see {@link SetMode#DISALLOW_NULL}.
   * @throws IllegalArgumentException if attempting to remove a mandatory field by setting it to null,
   *                                  see {@link SetMode#REMOVE_OPTIONAL_IF_NULL}.
   */
  protected <T extends DataTemplate<?>> void putWrapped(RecordDataSchema.Field field, Class<T> valueClass, T object, SetMode mode)
      throws ClassCastException
  {
    if (checkPutNullValue(field, object, mode))
    {
      if (object.getClass() == valueClass)
      {
        _map.put(field.getName(), object.data());
        _cache.put(object.data(), object);
      }
      else
      {
        throw new ClassCastException("Input " + object + " should be a " + valueClass.getName());
      }
    }
  }

  /**
   * Set the value of field with {@link SetMode#DISALLOW_NULL}.
   *
   * This is wrapping method. The value is a {@link DataTemplate}.
   * This method is used by older clients and cannot be removed without breaking
   * backwards compatibility.
   *
   * @param field provides the field to set.
   * @param valueClass provides the expected class of the input value.
   * @param object provides the value to set.
   * @param <T> is the type of the input object.
   * @throws ClassCastException if class of the provided value is not the same as the expected class.
   * @throws NullPointerException if null is not allowed, see {@link SetMode#DISALLOW_NULL}.
   */
  protected <T extends DataTemplate<?>> void putWrapped(RecordDataSchema.Field field, Class<T> valueClass, T object)
      throws ClassCastException
  {
    putWrapped(field, valueClass, object, SetMode.DISALLOW_NULL);
  }

  /**
   * Get the value of field.
   *
   * This is direct method. The result is not a {@link DataTemplate}.
   *
   * @param field provides the field to get.
   * @param valueClass provides the expected class of the result.
   * @param mode determines what should happen if the field is not present.
   * @param <T> is the type of the result object.
   * @return value of field or null with semantics defined by mode.
   * @throws RequiredFieldNotPresentException if mode is STRICT and the field is required but not present.
   * @throws TemplateOutputCastException if the value of the field is not the expected class or
   *                                     it cannot be coerced to the expected class.
   */
  protected <T> T obtainDirect(RecordDataSchema.Field field, Class<T> valueClass, GetMode mode)
      throws RequiredFieldNotPresentException, TemplateOutputCastException
  {
    Object found = obtainValueOrDefault(field, mode);
    if (found == null)
    {
      return null;
    }
    return DataTemplateUtil.coerceOutput(found, valueClass);
  }

  /**
   * Get the value of field.
   *
   * This is wrapping method. The result is a {@link DataTemplate}.
   *
   * @param field provides the field to get.
   * @param valueClass provides the expected class of the result.
   * @param mode determines what should happen if the field is not present.
   * @param <T> is the type of the result object.
   * @return value of field or null with semantics defined by mode.
   * @throws RequiredFieldNotPresentException if mode is STRICT and the field is required but not present.
   * @throws TemplateOutputCastException if the value of the field cannot be wrapped by the expected class.
   */
  protected <T extends DataTemplate<?>> T obtainWrapped(final RecordDataSchema.Field field, Class<T> valueClass, GetMode mode)
      throws RequiredFieldNotPresentException, TemplateOutputCastException
  {
    T wrapped;
    DataTemplate<?> template;
    Object found = obtainValueOrDefault(field, mode);
    if (found == null)
    {
      wrapped = null;
    }
    else if ((template = _cache.get(found)) != null && template.data() == found)
    {
      wrapped = valueClass.cast(template);
    }
    else
    {
      wrapped = DataTemplateUtil.wrap(found, field.getType(), valueClass);
      _cache.put(found, wrapped);
    }
    return wrapped;
  }

  /**
   * Obtain the value of field from the underlying {@link DataMap}.
   *
   * The mode argument determines what should happen if the field is
   * not present.
   *
   * @see GetMode
   *
   * @param field to access.
   * @param mode determines what should happen if the field is not present.
   * @return the value, the default value or null.
   * @throws RequiredFieldNotPresentException if the field is not present
   *                                          and has no default value
   *                                          and is not optional,
   *                                          and mode is STRICT.
   */
  private Object obtainValueOrDefault(RecordDataSchema.Field field, GetMode mode)
      throws RequiredFieldNotPresentException
  {
    Object defaultValue = field.getDefault();
    String fieldName = field.getName();
    Object found = _map.get(field.getName());
    if (found == null && mode != GetMode.NULL)
    {
      if (defaultValue != null)
      {
        // return default value, which is usually read-only
        found = defaultValue;
      }
      else if (field.getOptional() == false && mode == GetMode.STRICT)
      {
        throw new RequiredFieldNotPresentException(fieldName);
      }
    }
    return found;
  }

  /**
   * Check if the provided value is null, and handle the null value according to {@link SetMode}.
   *
   * @param field provides the field to set.
   * @param object provides the value to set.
   * @param mode determines how should happen if the value provided is null.
   * @return true if the put operation should be performed.
   * @throws NullPointerException if null is not allowed, see {@link SetMode#DISALLOW_NULL}.
   * @throws IllegalArgumentException if attempting to remove a mandatory field by setting it to null,
   *                                  see {@link SetMode#REMOVE_OPTIONAL_IF_NULL}.
   */
  private boolean checkPutNullValue(RecordDataSchema.Field field, Object object, SetMode mode)
  {
    boolean doPut;
    if (object == null)
    {
      doPut = false;
      switch (mode)
      {
        case IGNORE_NULL:
          break;
        case REMOVE_IF_NULL:
          _map.remove(field.getName());
          doPut = false;
          break;
        case REMOVE_OPTIONAL_IF_NULL:
          if (field.getOptional())
          {
            _map.remove(field.getName());
            doPut = false;
          }
          else
          {
            throw new IllegalArgumentException("Cannot remove mandatory field " + field.getName() + " of " + _schema.getFullName());
          }
          break;
        case DISALLOW_NULL:
          throw new NullPointerException("Cannot set field " + field.getName() + " of " + _schema.getFullName() + " to null");
        default:
          throw new IllegalStateException("Unknown mode " + mode);
      }
    }
    else
    {
      doPut = true;
    }
    return doPut;
  }

  private DataMap _map;
  private final RecordDataSchema _schema;
  private IdentityHashMap<Object, DataTemplate<?>> _cache = new IdentityHashMap<Object, DataTemplate<?>>();
}
