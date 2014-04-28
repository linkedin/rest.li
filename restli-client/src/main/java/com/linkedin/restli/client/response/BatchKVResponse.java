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

package com.linkedin.restli.client.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CompoundKey.TypeInfo;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.ResponseUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * A Batch of records. Used to return a fixed-size, unordered, complete collection of records, keyed on resource ID. Used
 * as a response for a get_batch request.
 */
public class BatchKVResponse<K, V extends RecordTemplate> extends RecordTemplate
{
  public static final String RESULTS = "results";
  public static final String ERRORS = "errors";

  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final ComplexKeySpec<?, ?> _complexKeyType;

  private final RecordDataSchema _schema;

  private final Map<K, V> _results;
  private final Map<K, ErrorResponse> _errors;

  private final ProtocolVersion _version;

  /**
   * Constructor for collection and association resource responses.  For complex key resources
   * use the constructor that accepts keyKeyClass and keyParamsClass.
   *
   * @param data provides the batch response data.
   * @param keyClass provides the class identifying the key type:
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources do not use this constructor, use the one that accepts keyKeyClass and keyParamsClass.</li>
   *   </ul>
   * @param valueClass provides the entity type of the collection.
   * @param keyParts provides a map for association keys of each key name to {@link TypeInfo}, for non-association resources must be an empty map.
   */
  public BatchKVResponse(DataMap data,
                         Class<K> keyClass,
                         Class<V> valueClass,
                         Map<String, CompoundKey.TypeInfo> keyParts,
                         ProtocolVersion version)
  {
    this(data,
         keyClass,
         valueClass,
         keyParts,
         null,
         null,
         version);
  }

  @Deprecated
  public BatchKVResponse(DataMap data,
                         Class<K> keyClass,
                         Class<V> valueClass,
                         Map<String, CompoundKey.TypeInfo> keyParts)
  {
    this(data, keyClass, valueClass, keyParts, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion());
  }

  /**
   * Constructor for collection and association resource responses.  For complex key resources
   * use the constructor that accepts keyKeyClass and keyParamsClass.
   *
   * @param data provides the batch response data.
   * @param keyType provides the class identifying the key type:
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources do not use this constructor, use the one that accepts keyKeyClass and keyParamsClass.</li>
   *   </ul>
   * @param valueType provides the entity type of the collection.
   * @param keyParts provides a map for association keys of each key name to {@link TypeInfo}, for non-association resources must be an empty map.
   */
  public BatchKVResponse(DataMap data,
                         TypeSpec<K> keyType,
                         TypeSpec<V> valueType,
                         Map<String, CompoundKey.TypeInfo> keyParts,
                         ProtocolVersion version)
  {
    this(data, keyType, valueType, keyParts, null, version);
  }

  @Deprecated
  public BatchKVResponse(DataMap data,
                         TypeSpec<K> keyType,
                         TypeSpec<V> valueType,
                         Map<String, CompoundKey.TypeInfo> keyParts)
  {
    this(data, keyType, valueType, keyParts, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion());
  }

  /**
   * Constructor resource responses.
   *
   * @param data provides the batch response data.
   * @param keyClass provides the class identifying the key type.
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources must be {@link ComplexResourceKey}, keyKeyClass must contain the
   *           key's record template class and if the resource has a key params their record template type keyParamsClass must be provided.</li>
   * @param valueClass provides the entity type of the collection.
   * @param keyParts provides a map for association keys of each key name to {@link TypeInfo}, for non-association resources must be an empty map.
   * @param keyKeyClass provides the record template class for the key for complex key resources, otherwise null.
   * @param keyParamsClass provides the record template class for the key params for complex key resources, otherwise null.
   */
  public BatchKVResponse(DataMap data,
                         Class<K> keyClass,
                         Class<V> valueClass,
                         Map<String, CompoundKey.TypeInfo> keyParts,
                         Class<? extends RecordTemplate> keyKeyClass,
                         Class<? extends RecordTemplate> keyParamsClass,
                         ProtocolVersion version)
  {
    this(data,
         TypeSpec.forClassMaybeNull(keyClass),
         TypeSpec.forClassMaybeNull(valueClass),
         keyParts,
         ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass),
         version);
  }

  @Deprecated
  public BatchKVResponse(DataMap data,
                         Class<K> keyClass,
                         Class<V> valueClass,
                         Map<String, CompoundKey.TypeInfo> keyParts,
                         Class<? extends RecordTemplate> keyKeyClass,
                         Class<? extends RecordTemplate> keyParamsClass)
  {
    this(data, keyClass, valueClass, keyParts, keyKeyClass, keyParamsClass, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion());
  }

  /**
   * Constructor resource responses.
   *
   * @param data provides the batch response data.
   * @param keyType provides the class identifying the key type.
   *   <ul>
   *     <li>For collection resources must be a primitive or a typeref to a primitive.</li>
   *     <li>For an association resources must be {@link CompoundKey} and keyParts must contain an entry for each association key field.</li>
   *     <li>For complex resources must be {@link ComplexResourceKey}, keyKeyClass must contain the
   *           key's record template class and if the resource has a key params their record template type keyParamsClass must be provided.</li>
   * @param valueType provides the entity type of the collection.
   * @param keyParts provides a map for association keys of each key name to {@link TypeInfo}, for non-association resources must be an empty map.
   * @param complexKeyType provides the type of the key for complex key resources, otherwise null.
   */
  public BatchKVResponse(DataMap data,
                         TypeSpec<K> keyType,
                         TypeSpec<V> valueType,
                         Map<String, CompoundKey.TypeInfo> keyParts,
                         ComplexKeySpec<?,?> complexKeyType,
                         ProtocolVersion version)
  {
    super(data, null);
    _keyParts = keyParts;
    _complexKeyType = complexKeyType;
    _version = version;

    StringBuilder errorMessageBuilder = new StringBuilder(10);
    Name elementSchemaName = new Name(valueType.getType().getSimpleName(), errorMessageBuilder);
    MapDataSchema resultsSchema = new MapDataSchema(new RecordDataSchema(elementSchemaName, RecordDataSchema.RecordType.RECORD));
    RecordDataSchema.Field resultsField = new RecordDataSchema.Field(resultsSchema);
    resultsField.setName(RESULTS, errorMessageBuilder);

    Name errorSchemaName = new Name(ErrorResponse.class.getSimpleName(), errorMessageBuilder);
    MapDataSchema errorsSchema = new MapDataSchema(new RecordDataSchema(errorSchemaName, RecordDataSchema.RecordType.RECORD));
    RecordDataSchema.Field errorsField = new RecordDataSchema.Field(errorsSchema);
    errorsField.setName(ERRORS, errorMessageBuilder);

    Name name = new Name(BatchKVResponse.class.getSimpleName(), errorMessageBuilder);
    _schema = new RecordDataSchema(name, RecordDataSchema.RecordType.RECORD);
    _schema.setFields(Arrays.asList(resultsField, errorsField), errorMessageBuilder);

    DataMap resultsRaw = (DataMap) data().get(RESULTS);
    _results = new HashMap<K, V>((int)Math.ceil(resultsRaw.size() / 0.75f)){

      private static final long serialVersionUID = 1L;

      @Override
      public V get(Object key)
      {
        if (key instanceof ComplexResourceKey)
        {
          ComplexResourceKey<RecordTemplate, RecordTemplate> paramlessKey =
              getParameterlessComplexKey((ComplexResourceKey) key);
          return super.get(paramlessKey);
        }
        else
        {
          return super.get(key);
        }
      }

      @Override
      public boolean containsKey(Object key)
      {
        if (key instanceof ComplexResourceKey)
        {
          ComplexResourceKey<RecordTemplate, RecordTemplate> paramlessKey =
              getParameterlessComplexKey((ComplexResourceKey) key);

          return super.containsKey(paramlessKey);
        }
        else
        {
          return super.containsKey(key);
        }
      }

      @Override
      @SuppressWarnings("unchecked")
      public V put(K key, V value)
      {
        if (key instanceof ComplexResourceKey)
        {
          ComplexResourceKey<RecordTemplate, RecordTemplate> paramlessKey =
              getParameterlessComplexKey((ComplexResourceKey) key);

          return super.put((K) paramlessKey, value);
        }
        else
        {
          return super.put(key, value);
        }
      }

      @Override
      public V remove(Object key)
      {
        if (key instanceof ComplexResourceKey)
        {
          ComplexResourceKey<RecordTemplate, RecordTemplate> paramlessKey =
              getParameterlessComplexKey((ComplexResourceKey) key);

          return super.remove(paramlessKey);
        }
        else
        {
          return super.remove(key);
        }
      }

      private ComplexResourceKey<RecordTemplate, RecordTemplate> getParameterlessComplexKey(ComplexResourceKey key)
      {
        return new ComplexResourceKey<RecordTemplate, RecordTemplate>(
            key.getKey(),
            DataTemplateUtil.wrap(new DataMap(), _complexKeyType.getParamsType().getType()));
      }
    };
    for (Map.Entry<String, Object> entry : resultsRaw.entrySet())
    {
      K key = ResponseUtils.convertKey(entry.getKey(), keyType, _keyParts, _complexKeyType, _version);
      V value = DataTemplateUtil.wrap(entry.getValue(), valueType.getType());
      _results.put(key, value);
    }

    DataMap errorsRaw = (DataMap) data().get(ERRORS);
    _errors = new HashMap<K, ErrorResponse>((int)Math.ceil(errorsRaw.size()/0.75f));
    for (Map.Entry<String, Object> entry : errorsRaw.entrySet())
    {
      K key = ResponseUtils.convertKey(entry.getKey(), keyType, _keyParts, _complexKeyType, _version);
      ErrorResponse value = DataTemplateUtil.wrap(entry.getValue(), ErrorResponse.class);
      _errors.put(key, value);
    }
  }

  @Deprecated
  public BatchKVResponse(DataMap data,
                         TypeSpec<K> keyType,
                         TypeSpec<V> valueType,
                         Map<String, CompoundKey.TypeInfo> keyParts,
                         ComplexKeySpec<?,?> complexKeyType)
  {
    this(data, keyType, valueType, keyParts, complexKeyType, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion());
  }

  public Map<K, V> getResults()
  {
    return _results;
  }

  public Map<K, ErrorResponse> getErrors()
  {
    return _errors;
  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

}
