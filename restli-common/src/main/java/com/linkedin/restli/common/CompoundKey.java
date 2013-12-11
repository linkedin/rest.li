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
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.internal.common.ValueConverter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Represents a compound identifier.
 *
 * @author dellamag
 */
public class CompoundKey
{
  private final Map<String, Object> _keys;
  private boolean _isReadOnly;

  public CompoundKey()
  {
    _keys = new HashMap<String, Object>(4);
    _isReadOnly = false;
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
      TypeInfo typeInfo = (TypeInfo)fieldType;
      DataSchema schema = typeInfo._declaredType.getSchema();
      DataSchema.Type dereferencedType = schema.getDereferencedType();
      if (!schema.getDereferencedDataSchema().isPrimitive())
      {
        throw new IllegalArgumentException("Compound key type must dereference to a primitive type.");
      }

      Object value = entry.getValue();
      Class<?> dereferencedClass = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedType);

      if (!value.getClass().equals(dereferencedClass))
      {
        if (value.getClass().equals(String.class))
        {
          // we coerce Strings to the dereferenced class
          value = ValueConverter.coerceString((String)value,
                                              DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedType));
        }
        else
        {
          throw new IllegalArgumentException("Value " + value + " is not a String or an object of " +
                                                 dereferencedClass.getSimpleName());
        }
      }
      value = DataTemplateUtil.coerceOutput(value, typeInfo._bindingType.getType());
      result.append(entry.getKey(), value);
    }
    return result;
  }

 /**
   * Add the key with the given name and value to the CompoundKey.
   *
   * @param name name of the key
   * @param value value of the key
   * @return this
   */
  public CompoundKey append(String name, Object value)
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

    _keys.put(name, value);
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
    return _keys.get(name);
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
   * Makes this key read only. Subsequent calls to {@link #append(String, Object)} will throw an
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
    if (getClass() != obj.getClass())
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

  /**
   * Create a DataMap representation of this CompoundKey.  If any of its fields are CustomTypes,
   * they will be coerced down to their base type before being placed into the map.
   *
   * @param fieldTypes the fieldTypes of this {@link CompoundKey}
   * @return a {@link DataMap} representation of this {@link CompoundKey}
   * @see {@link com.linkedin.restli.internal.common.URIParamUtils#compoundKeyToDataMap(CompoundKey)}
   */
  public DataMap toDataMap(Map<String, CompoundKey.TypeInfo> fieldTypes)
  {
    DataMap dataMap = new DataMap(_keys.size());
    for (Map.Entry<String, Object> keyParts : _keys.entrySet())
    {
      String key = keyParts.getKey();
      Object value = keyParts.getValue();
      DataSchema schema = fieldTypes.get(key).getDeclared().getSchema();
      DataSchema dereferencedSchema = schema.getDereferencedDataSchema();
      Class<?> dereferencedClass = DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedSchema.getType());

      @SuppressWarnings("unchecked")
      Object coercedInput = DataTemplateUtil.coerceInput(value,
                                                         (Class<Object>) value.getClass(),
                                                         dereferencedClass);
      dataMap.put(key, coercedInput);
    }
    return dataMap;
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
        b.append(URLEncoder.encode(DataTemplateUtil.stringify(_keys.get(keyPart)), RestConstants.DEFAULT_CHARSET_NAME));
      }
      catch (UnsupportedEncodingException e)
      {
        throw new RuntimeException("UnsupportedEncodingException while trying to encode the key", e);
      }
      delimit = true;
    }
    return b.toString();
  }
}
