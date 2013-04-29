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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.RecordTemplate;


/**
 * Runtime representation of resource spec.
 *
 * @author Eran Leshem
 */
public class ResourceSpecImpl implements ResourceSpec
{
  private final Set<ResourceMethod>                _supportedMethods;
  private final Class<?>                           _keyClass;
  private final Class<? extends RecordTemplate>    _keyKeyClass;
  private final Class<? extends RecordTemplate>    _keyParamsClass;
  private final Class<? extends RecordTemplate>    _valueClass;
  private final Map<String, CompoundKey.TypeInfo>  _keyParts;
  private final Map<String, DynamicRecordMetadata> _actionRequestMetadata;
  private final Map<String, DynamicRecordMetadata> _actionResponseMetadata;

  /**
   * Initialize an empty ResourceSpecImpl.
   */
  public ResourceSpecImpl()
  {
    this(Collections.<ResourceMethod> emptySet(),
         Collections.<String, DynamicRecordMetadata> emptyMap(),
         Collections.<String, DynamicRecordMetadata> emptyMap(),
         null,
         null,
         null,
         null,
         Collections.<String, CompoundKey.TypeInfo> emptyMap());
  }

  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @deprecated builder should pass in actionRequestMetadata and actionResponseMetadata
   */
  @Deprecated
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods)
  {
    this(supportedMethods,
         Collections.<String, DynamicRecordMetadata> emptyMap(),
         Collections.<String, DynamicRecordMetadata> emptyMap(),
         null,
         null,
         null,
         null,
         Collections.<String, CompoundKey.TypeInfo> emptyMap());
  }


  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param actionRequestMetadata Map from method name to method {@link RecordDataSchema}
   * @param actionResponseMetadata Map from method name to response RecordDataSchema
   */
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Map<String, DynamicRecordMetadata> actionRequestMetadata,
                          Map<String, DynamicRecordMetadata> actionResponseMetadata)
  {
    this(supportedMethods,
         actionRequestMetadata,
         actionResponseMetadata,
         null,
         null,
         null,
         null,
         Collections.<String, CompoundKey.TypeInfo> emptyMap());
  }

  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param keyClass type of the key of the Resource
   * @param valueClass the type of the RecordTemplate the Resource manages
   * @param keyParts Map of key names to key types (AssocKeyBindingTypes
   *         or, for backward compatibility, Class<?>), if the keyClass is a {@link CompoundKey}.
   * @deprecated builder should pass in actionRequestMetadata and actionResponseMetadata
   */
  @Deprecated
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Class<?> keyClass,
                          Class<? extends RecordTemplate> valueClass,
                          Map<String, ?> keyParts)
  {
    this(supportedMethods,
         Collections.<String, DynamicRecordMetadata> emptyMap(),
         Collections.<String, DynamicRecordMetadata> emptyMap(),
         keyClass,
         null,
         null,
         valueClass,
         keyParts);
  }

  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param actionRequestMetadata Map from method name to method {@link RecordDataSchema}
   * @param actionResponseMetadata Map from method name to response RecordDataSchema
   * @param keyClass type of the key of the Resource
   * @param valueClass the type of the RecordTemplate the Resource manages
   * @param keyParts Map of key names to key types (AssocKeyBindingTypes
   *         or, for backward compatibility, Class<?>), if the keyClass is a {@link CompoundKey}.
   */
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Map<String, DynamicRecordMetadata> actionRequestMetadata,
                          Map<String, DynamicRecordMetadata> actionResponseMetadata,
                          Class<?> keyClass,
                          Class<? extends RecordTemplate> valueClass,
                          Map<String, ?> keyParts)
  {
    this(supportedMethods,
         actionRequestMetadata,
         actionResponseMetadata,
         keyClass,
         null,
         null,
         valueClass,
         keyParts);
  }

  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param keyClass type of the key of the Resource
   * @param keyKeyClass RecordTemplate type of the key, if the keyClass is a ComplexResourceKey
   * @param keyParamsClass RecordTemplate type of parameters of the key, if the keyClass is a ComplexResourceKey
   * @param valueClass the type of the RecordTemplate that the Resource manages
   * @param keyParts Map of key names to key types (AssocKeyBindingTypes
   *         or, for backward compatibility, Class<?>), if the keyClass is a {@link CompoundKey}.
   * @deprecated builder should pass in actionRequestMetadata and actionResponseMetadata
   */
  @Deprecated
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Class<?> keyClass,
                          Class<? extends RecordTemplate> keyKeyClass,
                          Class<? extends RecordTemplate> keyParamsClass,
                          Class<? extends RecordTemplate> valueClass,
                          Map<String, ?> keyParts)
  {
    this(supportedMethods,
         Collections.<String, DynamicRecordMetadata> emptyMap(),
         Collections.<String, DynamicRecordMetadata> emptyMap(),
         keyClass,
         keyKeyClass,
         keyParamsClass,
         valueClass,
         keyParts);
  }

  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param actionRequestMetadata Map from method name to method {@link RecordDataSchema}
   * @param actionResponseMetadata Map from method name to response RecordDataSchema
   * @param keyClass type of the key of the Resource
   * @param keyKeyClass RecordTemplate type of the key, if the keyClass is a ComplexResourceKey
   * @param keyParamsClass RecordTemplate type of parameters of the key, if the keyClass is a ComplexResourceKey
   * @param valueClass the type of the RecordTemplate that the Resource manages
   * @param keyParts Map of key names to key types (AssocKeyBindingTypes
   *         or, for backward compatibility, Class<?>), if the keyClass is a {@link CompoundKey}.
   */
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Map<String, DynamicRecordMetadata> actionRequestMetadata,
                          Map<String, DynamicRecordMetadata> actionResponseMetadata,
                          Class<?> keyClass,
                          Class<? extends RecordTemplate> keyKeyClass,
                          Class<? extends RecordTemplate> keyParamsClass,
                          Class<? extends RecordTemplate> valueClass,
                          Map<String, ?> keyParts)
  {
    _supportedMethods = Collections.unmodifiableSet(supportedMethods);
    _actionRequestMetadata = actionRequestMetadata;
    _actionResponseMetadata = actionResponseMetadata;
    _keyClass = keyClass;
    _keyKeyClass = keyKeyClass;
    _keyParamsClass = keyParamsClass;
    _valueClass = valueClass;

    HashMap<String, CompoundKey.TypeInfo> keyPartTypeInfos = new HashMap<String, CompoundKey.TypeInfo>();
    for(Map.Entry<String, ?> entry : keyParts.entrySet()) {
      if(entry.getValue() instanceof Class<?>)
      {
        Class<?> entryKeyClass = (Class<?>)entry.getValue();
        keyPartTypeInfos.put(entry.getKey(), new CompoundKey.TypeInfo(entryKeyClass, entryKeyClass));
      }
      else if (entry.getValue() instanceof CompoundKey.TypeInfo)
      {
        keyPartTypeInfos.put(entry.getKey(), (CompoundKey.TypeInfo)entry.getValue());
      }
      else
      {
        throw new IllegalArgumentException("keyParts values must be either Class<?> or CompoundKey.TypeInfo, but was: " + entry.getValue().getClass());
      }
    }
    _keyParts = Collections.unmodifiableMap(keyPartTypeInfos);
  }

  @Override
  public Set<ResourceMethod> getSupportedMethods()
  {
    return _supportedMethods;
  }

  @Override
  public Class<?> getKeyClass()
  {
    return _keyClass;
  }

  @Override
  public Class<? extends RecordTemplate> getValueClass()
  {
    return _valueClass;
  }

  @Override
  public Map<String, CompoundKey.TypeInfo> getKeyParts()
  {
    return _keyParts;
  }

  @Override
  public Class<? extends RecordTemplate> getKeyKeyClass()
  {
    return _keyKeyClass;
  }

  @Override
  public Class<? extends RecordTemplate> getKeyParamsClass()
  {
    return _keyParamsClass;
  }

  @Override
  public DynamicRecordMetadata getRequestMetadata(String methodName)
  {
    return _actionRequestMetadata.get(methodName);
  }

  @Override
  public DynamicRecordMetadata getActionResponseMetadata(String methodName)
  {
    return _actionResponseMetadata.get(methodName);
  }

  @Override
  public boolean equals(Object other)
  {
    if (this == other)
      return true;

    if (!(other instanceof ResourceSpecImpl))
      return false;

    ResourceSpecImpl resourceSpec = (ResourceSpecImpl)other;

    return fieldEquals(_supportedMethods, resourceSpec._supportedMethods)
        && fieldEquals(_keyClass, resourceSpec._keyClass)
        && fieldEquals(_keyKeyClass, resourceSpec._keyKeyClass)
        && fieldEquals(_keyParamsClass, resourceSpec._keyParamsClass)
        && fieldEquals(_valueClass, resourceSpec._valueClass)
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
    res = 11 * res + hashOrNull(_keyClass);
    res = 11 * res + hashOrNull(_keyKeyClass);
    res = 11 * res + hashOrNull(_keyParamsClass);
    res = 11 * res + hashOrNull(_valueClass);
    res = 11 * res + hashOrNull(_keyParts);
    return res;
  }

  private int hashOrNull(Object o)
  {
    return o == null ? 0 : o.hashCode();
  }
}
