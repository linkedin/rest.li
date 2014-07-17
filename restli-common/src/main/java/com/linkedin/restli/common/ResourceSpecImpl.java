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


import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.RecordTemplate;

import com.linkedin.restli.internal.common.ResourcePropertiesImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * Runtime representation of resource spec.
 *
 * @author Eran Leshem
 */
public class ResourceSpecImpl implements ResourceSpec
{
  private final ResourcePropertiesImpl _resourceProperties;

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
   * @param actionRequestMetadata Map from method name to method {@link RecordDataSchema}
   * @param actionResponseMetadata Map from method name to response RecordDataSchema
   * @param valueClass the type of the RecordTemplate the Resource manages
   */
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Map<String, DynamicRecordMetadata> actionRequestMetadata,
                          Map<String, DynamicRecordMetadata> actionResponseMetadata,
                          Class<? extends RecordTemplate> valueClass)
  {
    this(supportedMethods,
         actionRequestMetadata,
         actionResponseMetadata,
         null,
         null,
         null,
         valueClass,
         Collections.<String, CompoundKey.TypeInfo> emptyMap());
  }

  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param actionRequestMetadata Map from method name to method {@link RecordDataSchema}
   * @param actionResponseMetadata Map from method name to response RecordDataSchema
   * @param keyClass type of the key of the Resource, may be either a typeref or a primitive
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
    this(supportedMethods,
         actionRequestMetadata,
         actionResponseMetadata,
         TypeSpec.forClassMaybeNull(keyClass),
         ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass),
         TypeSpec.forClassMaybeNull(valueClass),
         keyParts);
  }

  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param actionRequestMetadata Map from method name to method {@link RecordDataSchema}
   * @param actionResponseMetadata Map from method name to response RecordDataSchema
   * @param key type of the key of the Resource, may be either a typeref or a primitive
   * @param complexKeyType the key, if the key is a ComplexResourceKey, otherwise null
   * @param value the type of the RecordTemplate that the Resource manages
   * @param keyParts Map of key names to key types (AssocKeyBindingTypes
   *         or, for backward compatibility, Class<?>), if the keyClass is a {@link CompoundKey}.
   */
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Map<String, DynamicRecordMetadata> actionRequestMetadata,
                          Map<String, DynamicRecordMetadata> actionResponseMetadata,
                          TypeSpec<?> key,
                          ComplexKeySpec<?, ?> complexKeyType,
                          TypeSpec<? extends RecordTemplate> value,
                          Map<String, ?> keyParts)
  {
    _resourceProperties = new ResourcePropertiesImpl(
        supportedMethods,
        key,
        complexKeyType,
        value,
        keyParts);
    _actionRequestMetadata = actionRequestMetadata;
    _actionResponseMetadata = actionResponseMetadata;
  }

  @Override
  public Set<ResourceMethod> getSupportedMethods()
  {
    return _resourceProperties.getSupportedMethods();
  }

  @Override
  public Class<?> getKeyClass()
  {
    return _resourceProperties.getKeyType() == null ? null : _resourceProperties.getKeyType().getType();
  }

  @Override
  public TypeSpec<?> getKeyType()
  {
    return _resourceProperties.getKeyType();
  }

  @Override
  public Class<? extends RecordTemplate> getValueClass()
  {
    return _resourceProperties.getValueType() == null ? null : _resourceProperties.getValueType().getType();
  }

  @Override
  public TypeSpec<? extends RecordTemplate> getValueType()
  {
    return _resourceProperties.getValueType();
  }

  @Override
  public Map<String, CompoundKey.TypeInfo> getKeyParts()
  {
    return _resourceProperties.getKeyParts();
  }

  @Override
  public Class<? extends RecordTemplate> getKeyKeyClass()
  {
    return (_resourceProperties.getComplexKeyType() == null) ?
        null :
        _resourceProperties.getComplexKeyType().getKeyType().getType();
  }

  @Override
  public Class<? extends RecordTemplate> getKeyParamsClass()
  {
    return (_resourceProperties.getComplexKeyType() == null) ?
        null :
        _resourceProperties.getComplexKeyType().getParamsType().getType();
  }

  @Override
  public ComplexKeySpec<? extends RecordTemplate, ? extends RecordTemplate> getComplexKeyType()
  {
    return _resourceProperties.getComplexKeyType();
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
  public boolean isKeylessResource()
  {
    return _resourceProperties.isKeylessResource();
  }

  @Override
  public boolean equals(Object other)
  {
    if (this == other)
      return true;

    if (!(other instanceof ResourceSpecImpl))
      return false;

    ResourceSpecImpl resourceSpec = (ResourceSpecImpl)other;

    return _resourceProperties.equals(resourceSpec._resourceProperties);
  }

  @Override
  public int hashCode()
  {
    return 11 * _resourceProperties.hashCode() + 7;
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append(_resourceProperties.toString());
    builder.append("\n, actionRequestMetadata: ");
    builder.append(_actionRequestMetadata);
    builder.append("\n, actionResponseMetadata: ");
    builder.append(_actionResponseMetadata);
    return builder.toString();
  }
}
