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
  private final Map<String, Class<?>>              _keyParts;
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
         Collections.<String, Class<?>> emptyMap());
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
         Collections.<String, Class<?>> emptyMap());
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
         Collections.<String, Class<?>> emptyMap());
  }

  /**
   * Initialize a ResourceSpecImpl with the given data.
   *
   * @param supportedMethods Set of ResourceMethods supported
   * @param keyClass type of the key of the Resource
   * @param valueClass the type of the RecordTemplate the Resource manages
   * @param keyParts Map of key names to key types, if the keyClass is a ComplexKey
   * @deprecated builder should pass in actionRequestMetadata and actionResponseMetadata
   */
  @Deprecated
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Class<?> keyClass,
                          Class<? extends RecordTemplate> valueClass,
                          Map<String, Class<?>> keyParts)
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
   * @param keyParts Map of key names to key types, if the keyClass is a ComplexKey
   */
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Map<String, DynamicRecordMetadata> actionRequestMetadata,
                          Map<String, DynamicRecordMetadata> actionResponseMetadata,
                          Class<?> keyClass,
                          Class<? extends RecordTemplate> valueClass,
                          Map<String, Class<?>> keyParts)
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
   * @param keyParts Map of the key names to key types, if the keyClass is a ComplexKey
   * @deprecated builder should pass in actionRequestMetadata and actionResponseMetadata
   */
  @Deprecated
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Class<?> keyClass,
                          Class<? extends RecordTemplate> keyKeyClass,
                          Class<? extends RecordTemplate> keyParamsClass,
                          Class<? extends RecordTemplate> valueClass,
                          Map<String, Class<?>> keyParts)
  {
    _supportedMethods = Collections.unmodifiableSet(supportedMethods);
    _actionRequestMetadata = Collections.emptyMap();
    _actionResponseMetadata = Collections.emptyMap();
    _keyClass = keyClass;
    _keyKeyClass = keyKeyClass;
    _keyParamsClass = keyParamsClass;
    _valueClass = valueClass;
    _keyParts = Collections.unmodifiableMap(keyParts);
    Collections.<String, Class<?>>emptyMap();
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
   * @param keyParts Map of the key names to key types, if the keyClass is a ComplexKey
   */
  public ResourceSpecImpl(Set<ResourceMethod> supportedMethods,
                          Map<String, DynamicRecordMetadata> actionRequestMetadata,
                          Map<String, DynamicRecordMetadata> actionResponseMetadata,
                          Class<?> keyClass,
                          Class<? extends RecordTemplate> keyKeyClass,
                          Class<? extends RecordTemplate> keyParamsClass,
                          Class<? extends RecordTemplate> valueClass,
                          Map<String, Class<?>> keyParts)
  {
    _supportedMethods = Collections.unmodifiableSet(supportedMethods);
    _actionRequestMetadata = actionRequestMetadata;
    _actionResponseMetadata = actionResponseMetadata;
    _keyClass = keyClass;
    _keyKeyClass = keyKeyClass;
    _keyParamsClass = keyParamsClass;
    _valueClass = valueClass;
    _keyParts = Collections.unmodifiableMap(keyParts);
    Collections.<String, Class<?>>emptyMap();
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
  public Map<String, Class<?>> getKeyParts()
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

}
