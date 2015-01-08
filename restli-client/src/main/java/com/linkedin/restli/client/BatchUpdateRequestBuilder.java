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

/**
 * $Id: $
 */

package com.linkedin.restli.client;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.internal.common.util.CollectionUtils;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.KeyValueRecordFactory;
import com.linkedin.restli.common.ResourceSpec;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchUpdateRequestBuilder<K, V extends RecordTemplate> extends
    BatchKVRequestBuilder<K, V, BatchUpdateRequest<K, V>>
{
  private final KeyValueRecordFactory<K, V> _keyValueRecordFactory;
  private final Map<K, V> _updateInputMap;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public BatchUpdateRequestBuilder(String baseUriTemplate,
                                   Class<V> valueClass,
                                   ResourceSpec resourceSpec,
                                   RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _keyValueRecordFactory
        = new KeyValueRecordFactory(_resourceSpec.getKeyType(),
                                    _resourceSpec.getComplexKeyType(),
                                    _resourceSpec.getKeyParts(),
                                    _resourceSpec.getValueType());
    _updateInputMap = new HashMap<K, V>();
  }

  public BatchUpdateRequestBuilder<K, V> input(K id, V entity)
  {
    addKey(id);
    _updateInputMap.put(id, entity);
    return this;
  }

  public BatchUpdateRequestBuilder<K, V> inputs(Map<K, V> entities)
  {
    addKeys(entities.keySet());
    for (Map.Entry<K, V> entry: entities.entrySet())
    {
      K key = entry.getKey();
      V value = entry.getValue();
      _updateInputMap.put(key, value);
    }
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public BatchUpdateRequest<K, V> build()
  {
    ensureBatchKeys();
    Map<K, V> readOnlyUpdateInputMap = new HashMap<K, V>(
        CollectionUtils.getMapInitialCapacity(_updateInputMap.size(), 0.75f), 0.75f);
    CollectionRequest<KeyValueRecord<K, V>> readOnlyInput = buildReadOnlyBatchUpdateInput(readOnlyUpdateInputMap);

    return new BatchUpdateRequest<K, V>(buildReadOnlyHeaders(),
                                        readOnlyInput,
                                        buildReadOnlyQueryParameters(),
                                        _resourceSpec,
                                        getBaseUriTemplate(),
                                        buildReadOnlyPathKeys(),
                                        getRequestOptions(),
                                        Collections.unmodifiableMap(readOnlyUpdateInputMap));
  }

  private CollectionRequest<KeyValueRecord<K, V>> buildReadOnlyBatchUpdateInput(Map<K, V> readOnlyInputEntities)
  {
    try
    {
      DataMap map = new DataMap();
      @SuppressWarnings({"unchecked", "rawtypes"})
      CollectionRequest<KeyValueRecord<K, V>> input = new CollectionRequest(map, KeyValueRecord.class);

      for (Map.Entry<K, V> inputEntityEntry : _updateInputMap.entrySet())
      {
        K key = getReadOnlyOrCopyKey(inputEntityEntry.getKey());
        V entity = getReadOnlyOrCopyDataTemplate(inputEntityEntry.getValue());
        readOnlyInputEntities.put(key, entity);
        KeyValueRecord<K, V> keyValueRecord = _keyValueRecordFactory.create(key, entity);
        keyValueRecord.data().setReadOnly();
        input.getElements().add(keyValueRecord);
      }

      map.setReadOnly();
      return input;
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Entity cannot be copied.", cloneException);
    }
  }
}
