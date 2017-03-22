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
import com.linkedin.internal.common.util.CollectionUtils;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.ResponseUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * A batch of records. Used to return a fixed-size, unordered, complete collection of records, keyed on resource ID. Used
 * as a response for a get_batch request.
 *
 * @author Keren Jin
 */
public class BatchKVResponse<K, V extends RecordTemplate> extends RecordTemplate
{
  public static final String RESULTS = "results";
  public static final String ERRORS = "errors";

  private RecordDataSchema _schema;
  private Class<V> _valueClass;
  private Map<K, V> _results;
  private Map<K, ErrorResponse> _errors;

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
   * @param keyParts provides a map for association keys of each key name to {@link CompoundKey.TypeInfo}, for non-association resources must be an empty map.
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
   * @param keyParts provides a map for association keys of each key name to {@link CompoundKey.TypeInfo}, for non-association resources must be an empty map.
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
   * @param keyParts provides a map for association keys of each key name to {@link CompoundKey.TypeInfo}, for non-association resources must be an empty map.
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
   * @param keyParts provides a map for association keys of each key name to {@link CompoundKey.TypeInfo}, for non-association resources must be an empty map.
   * @param complexKeyType provides the type of the key for complex key resources, otherwise null.
   */
  public BatchKVResponse(DataMap data,
                         TypeSpec<K> keyType,
                         TypeSpec<V> valueType,
                         Map<String, CompoundKey.TypeInfo> keyParts,
                         ComplexKeySpec<?, ?> complexKeyType,
                         ProtocolVersion version)
  {
    super(data, null);

    createSchema(valueType.getType());
    deserializeData(keyType, keyParts, complexKeyType, version);
  }

  @Deprecated
  public BatchKVResponse(DataMap data,
                         TypeSpec<K> keyType,
                         TypeSpec<V> valueType,
                         Map<String, CompoundKey.TypeInfo> keyParts,
                         ComplexKeySpec<?, ?> complexKeyType)
  {
    this(data, keyType, valueType, keyParts, complexKeyType, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion());
  }


  protected BatchKVResponse(DataMap data)
  {
    super(data, null);
  }

  protected void createSchema(Class<V> valueClass)
  {
    _valueClass = valueClass;

    final StringBuilder errorMessageBuilder = new StringBuilder(10);
    final Name elementSchemaName = new Name(valueClass.getSimpleName(), errorMessageBuilder);
    final MapDataSchema resultsSchema = new MapDataSchema(new RecordDataSchema(elementSchemaName, RecordDataSchema.RecordType.RECORD));
    final RecordDataSchema.Field resultsField = new RecordDataSchema.Field(resultsSchema);
    resultsField.setName(RESULTS, errorMessageBuilder);

    final Name errorSchemaName = new Name(ErrorResponse.class.getSimpleName(), errorMessageBuilder);
    final MapDataSchema errorsSchema = new MapDataSchema(new RecordDataSchema(errorSchemaName, RecordDataSchema.RecordType.RECORD));
    final RecordDataSchema.Field errorsField = new RecordDataSchema.Field(errorsSchema);
    errorsField.setName(ERRORS, errorMessageBuilder);

    final Name name = new Name(BatchKVResponse.class.getSimpleName(), errorMessageBuilder);
    _schema = new RecordDataSchema(name, RecordDataSchema.RecordType.RECORD);
    _schema.setFields(Arrays.asList(resultsField, errorsField), errorMessageBuilder);
  }

  protected void deserializeData(TypeSpec<K> keyType,
      Map<String, CompoundKey.TypeInfo> keyParts,
      ComplexKeySpec<?, ?> complexKeyType,
      ProtocolVersion version)
  {
    deserializeData(data(), keyType, keyParts, complexKeyType, version);
  }

  protected void deserializeData(DataMap data, TypeSpec<K> keyType,
      Map<String, CompoundKey.TypeInfo> keyParts,
      ComplexKeySpec<?, ?> complexKeyType,
      ProtocolVersion version)
  {
    final DataMap resultsRaw = data.getDataMap(RESULTS);
    if (resultsRaw == null)
    {
      _results = new ParamlessKeyHashMap<V>(complexKeyType);
    }
    else
    {
      _results = new ParamlessKeyHashMap<V>(
          CollectionUtils.getMapInitialCapacity(resultsRaw.size(), 0.75f), 0.75f, complexKeyType);
      for (Map.Entry<String, Object> entry : resultsRaw.entrySet())
      {
        @SuppressWarnings("unchecked")
        final K key = (K) ResponseUtils.convertKey(entry.getKey(), keyType, keyParts, complexKeyType, version);
        final V value = deserializeValue(entry.getValue());
        _results.put(key, value);
      }
    }

    final DataMap errorsRaw = data.getDataMap(ERRORS);
    if (errorsRaw == null)
    {
      _errors = new ParamlessKeyHashMap<ErrorResponse>(complexKeyType);
    }
    else
    {
      _errors = new ParamlessKeyHashMap<ErrorResponse>(
          CollectionUtils.getMapInitialCapacity(errorsRaw.size(), 0.75f), 0.75f, complexKeyType);
      for (Map.Entry<String, Object> entry : errorsRaw.entrySet())
      {
        @SuppressWarnings("unchecked")
        final K key = (K) ResponseUtils.convertKey(entry.getKey(), keyType, keyParts, complexKeyType, version);
        final ErrorResponse value = DataTemplateUtil.wrap(entry.getValue(), ErrorResponse.class);
        _errors.put(key, value);
      }
    }
  }

  /**
   * Returns the results of batch operation. Please note differences between Rest.li protocol before and after 2.0
   * @return
   * For Rest.li protocol ver. <  2.0: entries which succeeded
   * For Rest.li protocol ver. >= 2.0: all entries as EntityResponse, including successful and failed ones.
   */
  public Map<K, V> getResults()
  {
    return _results;
  }

  /**
   * Returns the errors of batch operation. Please note differences between Rest.li protocol before and after 2.0
   * @return
   * For Rest.li protocol ver. <  2.0: entries which failed
   * For Rest.li protocol ver. >= 2.0: ignore, please use getResults() instead
   */
  public Map<K, ErrorResponse> getErrors()
  {
    return _errors;
  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

  protected V deserializeValue(Object valueData)
  {
    return DataTemplateUtil.wrap(valueData, _valueClass);
  }

  private class ParamlessKeyHashMap<MV> extends HashMap<K, MV>
  {
    private static final long serialVersionUID = 1L;
    private final ComplexKeySpec<?, ?> _complexKeyType;

    private ParamlessKeyHashMap(ComplexKeySpec<?, ?> complexKeyType)
    {
      _complexKeyType = complexKeyType;
    }

    private ParamlessKeyHashMap(int initialCapacity, float loadFactor, ComplexKeySpec<?, ?> complexKeyType)
    {
      super(initialCapacity, loadFactor);
      _complexKeyType = complexKeyType;
    }

    @Override
    public MV get(Object key)
    {
      return super.get(getProcessedKey(key));
    }

    @Override
    public boolean containsKey(Object key)
    {
      return super.containsKey(getProcessedKey(key));
    }

    @Override
    public MV put(K key, MV value)
    {
      return super.put(getProcessedKey(key), value);
    }

    @Override
    public MV remove(Object key)
    {
      return super.remove(getProcessedKey(key));
    }

    @SuppressWarnings("unchecked")
    private <MK> MK getProcessedKey(MK key)
    {
      if (key instanceof ComplexResourceKey)
      {
        final ComplexResourceKey<RecordTemplate, RecordTemplate> complexKey = (ComplexResourceKey<RecordTemplate, RecordTemplate>) key;
        return (MK) new ComplexResourceKey<RecordTemplate, RecordTemplate>(
          complexKey.getKey(),
          DataTemplateUtil.wrap(new DataMap(), _complexKeyType.getParamsType().getType()));
      }
      else
      {
        return key;
      }
    }
  }
}
