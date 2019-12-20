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

package com.linkedin.restli.common;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.internal.common.ValueConverter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
 * Represents a compound identifier.
 *
 * @author dellamag
 */
public class CompoundKey
{
  private final Map<String, ValueAndTypeInfoPair> _keys;
  private boolean _isReadOnly;

  public CompoundKey()
  {
    _keys = new HashMap<>(4);
    _isReadOnly = false;
  }

  private CompoundKey(CompoundKey compoundKey)
  {
    _keys = new HashMap<>(compoundKey._keys);
  }

  public static final class TypeInfo
  {
    // binding type could be any type (primitive or custom)
    private final TypeSpec<?> _bindingType;

    // declared type is potentially a typeref to a primitive, otherwise it is a primitive
    private final TypeSpec<?> _declaredType;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public TypeInfo(Class<?> bindingClass, Class<?> declaredClass)
    {
      this(new TypeSpec(bindingClass), new TypeSpec(declaredClass));
    }

    public TypeInfo(TypeSpec<?> bindingType, TypeSpec<?> declaredType)
    {
      _bindingType = bindingType;
      _declaredType = declaredType;
    }

    public Class<?> getBindingType()
    {
      return _bindingType.getType();
    }

    public Class<?> getDeclaredType()
    {
      return _declaredType.getType();
    }

    public TypeSpec<?> getBinding()
    {
      return _bindingType;
    }

    public TypeSpec<?> getDeclared()
    {
      return _declaredType;
    }
  }

  /**
   * Initialize and return a CompoundKey.
   *
   * @param fieldValues DataMap representing the CompoundKey
   * @param fieldTypes Map of key name to it's {@link TypeInfo} (also accepts Class<?> for backward compatibility).
   * @return a CompoundKey
   */
  public static CompoundKey fromValues(DataMap fieldValues, Map<String, CompoundKey.TypeInfo> fieldTypes)
  {
    CompoundKey result = new CompoundKey();
    for (Map.Entry<String, Object> entry : fieldValues.entrySet())
    {
      Object fieldType = fieldTypes.get(entry.getKey());
      TypeInfo typeInfo = (TypeInfo) fieldType;
      DataSchema declaredSchema = typeInfo.getDeclared().getSchema();
      Class<?> declaredType;
      if (declaredSchema.getType() == DataSchema.Type.TYPEREF)
      {
        if (declaredSchema.getDereferencedDataSchema().isPrimitive())
        {
          declaredType = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(declaredSchema.getDereferencedType());
        }
        else if (declaredSchema.getDereferencedType() == DataSchema.Type.ENUM)
        {
          try
          {
            // there is no direct way to access the dereferenced class from TyperefInfo
            declaredType = Class.forName(((EnumDataSchema) declaredSchema.getDereferencedDataSchema()).getFullName());
          }
          catch (ClassNotFoundException e)
          {
            throw new RuntimeException(e);
          }
        }
        else
        {
          throw new IllegalArgumentException("Compound key type must dereference to a primitive type or enum.");
        }
      }
      else
      {
        declaredType = typeInfo.getDeclaredType();
      }

      Object value = entry.getValue();
      if (!value.getClass().equals(declaredType))
      {
        if (value.getClass().equals(String.class))
        {
          // we coerce Strings to the dereferenced class
          value = ValueConverter.coerceString((String) value, declaredType);
        }
        else
        {
          throw new IllegalArgumentException("Value " + value + " is not a String or an object of " +
                                                 declaredType.getSimpleName());
        }
      }
      value = DataTemplateUtil.coerceOutput(value, typeInfo.getBindingType());
      result.append(entry.getKey(), value, typeInfo);
    }
    return result;
  }

  /**
   * Add the key with the given name and value to the CompoundKey.
   *
   * Only primitive values are supported.  The {@link CompoundKey.TypeInfo} will be generated based on the value of the key.
   *
   * @param name name of the key
   * @param value value of the key
   * @return this
   */
  public CompoundKey append(String name, Object value)
  {
    if (value==null)
    {
      throw new IllegalArgumentException("value of CompoundKey part cannot be null");
    }
    TypeInfo typeInfo = new CompoundKey.TypeInfo(value.getClass(), value.getClass());
    append(name, value, typeInfo);
    return this;
  }

 /**
   * Add the key with the given name and value to the CompoundKey.
   *
   * @param name name of the key
   * @param value value of the key
   * @param typeInfo TypeInfo for the value
   * @return this
   */
  public CompoundKey append(String name, Object value, TypeInfo typeInfo)
  {
    if (_isReadOnly)
    {
      throw new UnsupportedOperationException("Can't append to a read only key!");
    }
    if (name==null)
    {
      throw new IllegalArgumentException("name of CompoundKey part cannot be null");
    }
    if (value==null)
    {
      throw new IllegalArgumentException("value of CompoundKey part cannot be null");
    }
    if (typeInfo==null)
    {
      throw new IllegalArgumentException("typeInfo of CompoundKey part cannot be null");
    }

    _keys.put(name, new ValueAndTypeInfoPair(value, typeInfo));
    return this;
  }

  /**
   * Get the Object associated with the given key name.
   *
   * @param name name of the key
   * @return an Object
   */
  public Object getPart(String name)
  {
    return Optional.ofNullable(_keys.get(name)).map(ValueAndTypeInfoPair::getValue).orElse(null);
  }

  /**
   * Get the Object associated with the given key name as an Integer.
   *
   * @param name name of the key
   * @return an Integer
   */
  public Integer getPartAsInt(String name)
  {
    return (Integer)getPart(name);
  }

  /**
   * Get the Object associated with the given key name as a Long.
   *
   * @param name name of the key
   * @return a Long
   */
  public Long getPartAsLong(String name)
  {
    return (Long)getPart(name);
  }

  /**
   * Get the Object associated with the given key name as a String.
   *
   * @param name name of the key
   * @return a String
   */
  public String getPartAsString(String name)
  {
    return (String)getPart(name);
  }

  /**
   * @return the number of keys in this CompoundKey
   */
  public int getNumParts()
  {
    return _keys.size();
  }

  /**
   * @return a set of all the key names in this CompoundKey
   */
  public Set<String> getPartKeys()
  {
    return _keys.keySet();
  }

  /**
   * Returns whether this key is read only.
   */
  public boolean isReadOnly()
  {
    return _isReadOnly;
  }

  /**
   * Makes this key read only. Subsequent calls to {@link #append(String, Object, TypeInfo)} will throw an
   * {@link UnsupportedOperationException}
   */
  public void makeReadOnly()
  {
    _isReadOnly = true;
  }


  @Override
  public int hashCode()
  {
    return _keys.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }

    Class<?> thatClass = obj.getClass();

    if (!CompoundKey.class.isAssignableFrom(thatClass))
    {
      return false;
    }

    CompoundKey other = (CompoundKey) obj;
    if (!_keys.equals(other._keys))
    {
      return false;
    }

    return true;
  }

  public CompoundKey copy()
  {
    return new CompoundKey(this);
  }

  /**
   * Create a DataMap representation of this CompoundKey.  If any of its fields are CustomTypes,
   * they will be coerced down to their base type before being placed into the map.
   *
   * @return a {@link DataMap} representation of this {@link CompoundKey}
   * @see com.linkedin.restli.internal.common.URIParamUtils#compoundKeyToDataMap(CompoundKey)
   */
  public DataMap toDataMap()
  {
    DataMap dataMap = new DataMap(_keys.size());
    for (Map.Entry<String, ValueAndTypeInfoPair> keyParts : _keys.entrySet())
    {
      String key = keyParts.getKey();
      ValueAndTypeInfoPair valueAndTypeInfoPair = keyParts.getValue();
      Object value = valueAndTypeInfoPair.getValue();
      TypeInfo typeInfo = valueAndTypeInfoPair.getTypeInfo();
      DataSchema schema = typeInfo.getDeclared().getSchema();
      Object coercedInput = coerceValueForDataMap(value, schema);
      dataMap.put(key, coercedInput);
    }
    return dataMap;
  }

  /**
   * Create a DataMap representation of this CompoundKey.  If any of its fields are CustomTypes,
   * they will be coerced down to their base type before being placed into the map.
   *
   * @param fieldTypes the fieldTypes of this {@link CompoundKey}
   * @return a {@link DataMap} representation of this {@link CompoundKey}
   *
   * @deprecated Use {@link #toDataMap()}.
   */
  @Deprecated
  public DataMap toDataMap(Map<String, CompoundKey.TypeInfo> fieldTypes)
  {
    DataMap dataMap = new DataMap(_keys.size());
    for (Map.Entry<String, ValueAndTypeInfoPair> keyParts : _keys.entrySet())
    {
      String key = keyParts.getKey();
      ValueAndTypeInfoPair valueAndTypeInfoPair = keyParts.getValue();
      Object value = valueAndTypeInfoPair.getValue();
      DataSchema schema = fieldTypes.get(key).getDeclared().getSchema();
      Object coercedInput = coerceValueForDataMap(value, schema);
      dataMap.put(key, coercedInput);
    }
    return dataMap;
  }

  private Object coerceValueForDataMap(Object value, DataSchema schema) {
    Class<?> dereferencedClass = null;
    if (schema != null) {
      DataSchema dereferencedSchema = schema.getDereferencedDataSchema();
      dereferencedClass = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedSchema.getType());
    }
    @SuppressWarnings("unchecked")
    Class<Object> valueClass = (Class<Object>) value.getClass();
    return DataTemplateUtil.coerceInput(value, valueClass, dereferencedClass);
  }

  /**
   This returns a v1 style serialized key. It should not be used structurally.
   *
   * @return a {@link ProtocolVersion} v1 style serialized version of this {@link CompoundKey}.
   * @deprecated the output of this function may change in the future, but it is still acceptable to use for
   *             logging purposes.
   *             If you need a stringified version of a key to extract information from a batch response,
   *             you should use {@link BatchResponse#keyToString(Object, ProtocolVersion)}.
   *             Internal developers can use {@link com.linkedin.restli.internal.common.URIParamUtils#keyToString(Object, com.linkedin.restli.internal.common.URLEscaper.Escaping, com.linkedin.jersey.api.uri.UriComponent.Type, boolean, ProtocolVersion)},
   *             {@link com.linkedin.restli.internal.common.URIParamUtils#encodeKeyForBody(Object, boolean, ProtocolVersion)}, or {@link com.linkedin.restli.internal.common.URIParamUtils#encodeKeyForUri(Object, com.linkedin.jersey.api.uri.UriComponent.Type, ProtocolVersion)}
   *             as needed.
   */
  @Deprecated
  @Override
  public String toString()
  {
    List<String> keyList = new ArrayList<String>(_keys.keySet());
    Collections.sort(keyList);

    StringBuilder b = new StringBuilder();
    boolean delimit=false;
    for (String keyPart : keyList)
    {
      if (delimit)
      {
        b.append(RestConstants.SIMPLE_KEY_DELIMITER);
      }
      try
      {
        b.append(URLEncoder.encode(keyPart, RestConstants.DEFAULT_CHARSET_NAME));
        b.append(RestConstants.KEY_VALUE_DELIMITER);
        b.append(URLEncoder.encode(DataTemplateUtil.stringify(getPart(keyPart)), RestConstants.DEFAULT_CHARSET_NAME));
      }
      catch (UnsupportedEncodingException e)
      {
        throw new RuntimeException("UnsupportedEncodingException while trying to encode the key", e);
      }
      delimit = true;
    }
    return b.toString();
  }

  private static class ValueAndTypeInfoPair
  {
    private final Object _value;
    private final TypeInfo _typeInfo;

    private ValueAndTypeInfoPair(Object value, TypeInfo typeInfo) {
      _value = value;
      _typeInfo = typeInfo;
    }

    Object getValue() {
      return _value;
    }

    TypeInfo getTypeInfo() {
      return _typeInfo;
    }

    public boolean equals(Object o)
    {
      return (o instanceof ValueAndTypeInfoPair) &&
          ((ValueAndTypeInfoPair)o)._value.equals(this._value);

    }
    public int hashCode() {return _value.hashCode();}
  }
}
