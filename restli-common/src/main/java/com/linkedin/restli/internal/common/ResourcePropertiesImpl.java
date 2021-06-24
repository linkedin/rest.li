/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.common;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceProperties;
import com.linkedin.restli.common.TypeSpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Runtime representation of resource spec.
 */
public class ResourcePropertiesImpl implements ResourceProperties
{
  private final Set<ResourceMethod> _supportedMethods;

  /*
   * _keyType is a multi-purpose field.  If it's null, the resource is either an actionSet or a simple resource.  If it is non-null
   * it contains either a primitive type (for collections with a primitive key type), or it is ComplexResourceKey.class for
   * complex key collect resources or it is CompoundKey.class for associations.
   */
  private final TypeSpec<?> _keyType;
  private final ComplexKeySpec<? extends RecordTemplate, ? extends RecordTemplate> _complexKeyType; // present for complex key collections
  private final Map<String, CompoundKey.TypeInfo>  _keyParts; // present for associations

  private final TypeSpec<? extends RecordTemplate> _valueType;

  /**
   * Initialize a ResourcePropertiesImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param key type of the key of the Resource, may be either a typeref or a primitive
   * @param complexKeyType the key, if the key is a ComplexResourceKey, otherwise null
   * @param value the type of the RecordTemplate that the Resource manages
   * @param keyParts Map of key names to key types (AssocKeyBindingTypes
   *         or, for backward compatibility, Class<?>), if the keyClass is a {@link CompoundKey}.
   */
  public ResourcePropertiesImpl(Set<ResourceMethod> supportedMethods,
                                TypeSpec<?> key,
                                ComplexKeySpec<?, ?> complexKeyType,
                                TypeSpec<? extends RecordTemplate> value,
                                Map<String, ?> keyParts)
  {
    _supportedMethods = Collections.unmodifiableSet(supportedMethods);

    _keyType = key;
    _complexKeyType = complexKeyType;
    _keyParts = Collections.unmodifiableMap(toTypeInfoKeyParts(keyParts));

    _valueType = value;
  }

  @Override
  public Set<ResourceMethod> getSupportedMethods()
  {
    return _supportedMethods;
  }
  @Override
  public TypeSpec<?> getKeyType()
  {
    return _keyType;
  }

  @Override
  public Map<String, CompoundKey.TypeInfo> getKeyParts()
  {
    return _keyParts;
  }

  @Override
  public TypeSpec<? extends RecordTemplate> getValueType()
  {
    return _valueType;
  }

  @Override
  public ComplexKeySpec<? extends RecordTemplate, ? extends RecordTemplate> getComplexKeyType()
  {
    return _complexKeyType;
  }

  @Override
  public boolean isKeylessResource()
  {
    return _keyType == null;
  }

  @Override
  public boolean equals(Object other)
  {
    if (this == other)
      return true;

    if (!(other instanceof ResourcePropertiesImpl))
      return false;

    ResourcePropertiesImpl resourceSpec = (ResourcePropertiesImpl)other;

    return fieldEquals(_supportedMethods, resourceSpec._supportedMethods)
        && fieldEquals(_keyType, resourceSpec._keyType)
        && fieldEquals(_complexKeyType, resourceSpec._complexKeyType)
        && fieldEquals(_valueType, resourceSpec._valueType)
        && fieldEquals(_keyParts, resourceSpec._keyParts);
  }

  private boolean fieldEquals(Object field1, Object field2)
  {
    return field1 == null ? field2 == null : field1.equals(field2);
  }

  @Override
  public int hashCode()
  {
    int res = 7;
    res = 11 * res + hashOrNull(_supportedMethods);
    res = 11 * res + hashOrNull(_keyType);
    res = 11 * res + hashOrNull(_complexKeyType);
    res = 11 * res + hashOrNull(_valueType);
    res = 11 * res + hashOrNull(_keyParts);
    return res;
  }

  private int hashOrNull(Object o)
  {
    return o == null ? 0 : o.hashCode();
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append("supported: ");
    builder.append(_supportedMethods);
    builder.append("\n, key: ");
    builder.append(_keyType);
    builder.append("\n, complexKey: ");
    builder.append(_complexKeyType);
    builder.append("\n, value: ");
    builder.append(_valueType);
    builder.append("\n, keyParts: ");
    builder.append(_keyParts);
    return builder.toString();
  }

  private static HashMap<String, CompoundKey.TypeInfo> toTypeInfoKeyParts(Map<String, ?> keyParts)
  {
    final HashMap<String, CompoundKey.TypeInfo> keyPartTypeInfos = new HashMap<>();
    for(Map.Entry<String, ?> entry : keyParts.entrySet()) {
      if(entry.getValue() instanceof Class<?>)
      {
        final Class<?> entryKeyClass = (Class<?>) entry.getValue();
        keyPartTypeInfos.put(entry.getKey(), new CompoundKey.TypeInfo(entryKeyClass, entryKeyClass));
      }
      else if (entry.getValue() instanceof CompoundKey.TypeInfo)
      {
        keyPartTypeInfos.put(entry.getKey(), (CompoundKey.TypeInfo) entry.getValue());
      }
      else
      {
        throw new IllegalArgumentException("keyParts values must be either Class<?> or CompoundKey.TypeInfo, but was: " + entry.getValue().getClass());
      }
    }
    return keyPartTypeInfos;
  }
}
