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
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.KeyValueRecordFactory;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchPartialUpdateRequestBuilder<K, V extends RecordTemplate> extends
    BatchKVRequestBuilder<K, V, BatchPartialUpdateRequest<K, V>>
{
  private final KeyValueRecordFactory<K, PatchRequest<V>> _keyValueRecordFactory;
  private final Map<K, PatchRequest<V>> _partialUpdateInputMap;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public BatchPartialUpdateRequestBuilder(String baseUriTemplate,
                                          Class<V> valueClass,
                                          ResourceSpec resourceSpec,
                                          RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _partialUpdateInputMap = new HashMap<K, PatchRequest<V>>();
    _keyValueRecordFactory = new KeyValueRecordFactory(_resourceSpec.getKeyType(),
                                    _resourceSpec.getComplexKeyType(),
                                    _resourceSpec.getKeyParts(),
                                    new TypeSpec<PatchRequest>(PatchRequest.class));
  }

  public BatchPartialUpdateRequestBuilder<K, V> input(K id, PatchRequest<V> patch)
  {
    _partialUpdateInputMap.put(id, patch);
    addKey(id);
    return this;
  }

  public BatchPartialUpdateRequestBuilder<K, V> inputs(Map<K, PatchRequest<V>> patches)
  {
    addKeys(patches.keySet());
    for (Map.Entry<K, PatchRequest<V>> entry : patches.entrySet())
    {
      K key = entry.getKey();
      PatchRequest<V> value = entry.getValue();
      _partialUpdateInputMap.put(key, value);
    }
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequest<K, V> build()
  {
    ensureBatchKeys();

    return new BatchPartialUpdateRequest<K, V>(buildReadOnlyHeaders(),
                                               buildReadOnlyInput(),
                                               buildReadOnlyQueryParameters(),
                                               _resourceSpec,
                                               getBaseUriTemplate(),
                                               buildReadOnlyPathKeys(),
                                               getRequestOptions());
  }

  private CollectionRequest<KeyValueRecord<K, PatchRequest<V>>> buildReadOnlyInput()
  {
    try
    {
      DataMap map = new DataMap();
      @SuppressWarnings("unchecked")
      CollectionRequest<KeyValueRecord<K, PatchRequest<V>>> input = new CollectionRequest(map, KeyValueRecord.class);

      for (Map.Entry<K, PatchRequest<V>> inputEntityEntry : _partialUpdateInputMap.entrySet())
      {
        K key = getReadOnlyOrCopyKey(inputEntityEntry.getKey());
        PatchRequest<V> entity = getReadOnlyOrCopyDataTemplate(inputEntityEntry.getValue());
        KeyValueRecord<K, PatchRequest<V>> keyValueRecord = _keyValueRecordFactory.create(key, entity);
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
