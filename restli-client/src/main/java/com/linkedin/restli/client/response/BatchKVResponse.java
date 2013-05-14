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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TyperefInfo;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CompoundKey.TypeInfo;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.TyperefUtils;
import com.linkedin.restli.internal.common.ValueConverter;


/**
 * A Batch of records. Used to return a fixed-size, unordered, complete collection of records, keyed on resource ID. Used
 * as a response for a get_batch request.
 */
public class BatchKVResponse<K, V extends RecordTemplate> extends RecordTemplate
{
  public static final String RESULTS = "results";
  public static final String ERRORS = "errors";

  private final Map<String, CompoundKey.TypeInfo> _keyParts;
  private final Class<? extends RecordTemplate> _keyKeyClass;
  private final Class<? extends RecordTemplate> _keyParamsClass;

  private final RecordDataSchema _schema;

  private final Map<K, V> _results;
  private final Map<K, ErrorResponse> _errors;

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
                         Map<String, CompoundKey.TypeInfo> keyParts)
  {
    this(data,
          keyClass,
          valueClass,
          keyParts,
          (Class<? extends RecordTemplate>) null,
          (Class<? extends RecordTemplate>) null);
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
                         Class<? extends RecordTemplate> keyParamsClass)
  {
    super(data, null);
    _keyParts = keyParts;
    _keyKeyClass = keyKeyClass;
    _keyParamsClass = keyParamsClass;

    StringBuilder errorMessageBuilder = new StringBuilder(10);
    Name elementSchemaName = new Name(valueClass.getSimpleName(), errorMessageBuilder);
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
    _results = new HashMap<K, V>((int)Math.ceil(resultsRaw.size() / 0.75f));
    for (Map.Entry<String, Object> entry : resultsRaw.entrySet())
    {
      K key = convertKey(entry.getKey(), keyClass);
      V value = DataTemplateUtil.wrap(entry.getValue(), valueClass);
      _results.put(key, value);
    }

    DataMap errorsRaw = (DataMap) data().get(ERRORS);
    _errors = new HashMap<K, ErrorResponse>((int)Math.ceil(errorsRaw.size()/0.75f));
    for (Map.Entry<String, Object> entry : errorsRaw.entrySet())
    {
      K key = convertKey(entry.getKey(), keyClass);
      ErrorResponse value = DataTemplateUtil.wrap(entry.getValue(), ErrorResponse.class);
      _errors.put(key, value);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T convertKey(String rawKey, Class<? extends T> keyClass)
  {
    Class<? extends T> keyBindingClass = keyClass;
    Object result;

    if (TyperefInfo.class.isAssignableFrom(keyClass))
    {
      TyperefDataSchema schema = (TyperefDataSchema)DataTemplateUtil.getSchema(keyClass);
      DataSchema.Type dereferencedType = schema.getDereferencedType();
      if (!schema.getDereferencedDataSchema().isPrimitive())
      {
        throw new IllegalArgumentException("Compound key type must dereference to a primitive type.");
      }
      keyBindingClass = (Class<? extends T>)TyperefUtils.getJavaClassForSchema(schema);
      if(keyBindingClass == null)
      {
        keyBindingClass = (Class<? extends T>)dereferencedType.getClass();
      }
      result = ValueConverter.coerceString(rawKey, DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedType));
    }
    else if (CompoundKey.class.isAssignableFrom(keyClass))
    {
      DataMap key = parseKey(rawKey);
      result = CompoundKey.fromValues(key, _keyParts);
    }
    else if (ComplexResourceKey.class.isAssignableFrom(keyClass))
    {
      try
      {
        result =
            ComplexResourceKey.parseFromPathSegment(rawKey, _keyKeyClass, _keyParamsClass);
      }
      catch (PathSegmentSyntaxException e)
      {
        throw new IllegalStateException(rawKey
            + " is not a valid value for the resource key", e);
      }
    }
    else
    {
      try
      {
        result = ValueConverter.coerceString(rawKey, keyClass);
      }
      catch (IllegalArgumentException e)
      {
        throw new IllegalStateException(keyClass.getName()
            + " is not supported as a key type for BatchResponseKV", e);
      }
    }

    return DataTemplateUtil.coerceOutput(result, keyBindingClass);
  }

  //TODO: replace with generic QueryParam <=> DataMap codec
  private DataMap parseKey(String rawKey)
  {
    Map<String, List<String>> fields = UriComponent.decodeQuery(rawKey, true);
    DataMap result = new DataMap((int)Math.ceil(fields.size()/0.75f));
    for (Map.Entry<String, List<String>> entry : fields.entrySet())
    {
      if (entry.getValue().size()==1)
      {
        result.put(entry.getKey(), entry.getValue().get(0));
      }
      else
      {
        result.put(entry.getKey(), new DataList(entry.getValue()));
      }
    }
    return result;
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
