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


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.KeyValueRecordFactory;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchPartialUpdateRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, BatchPartialUpdateRequest<K, V>>
{
  private final CollectionRequest<KeyValueRecord<K, PatchRequest<V>>> _entities;
  private final KeyValueRecordFactory<K, PatchRequest<V>> _keyValueRecordFactory;

  @Deprecated
  public BatchPartialUpdateRequestBuilder(String baseUriTemplate,
                                          Class<V> valueClass,
                                          ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, valueClass, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public BatchPartialUpdateRequestBuilder(String baseUriTemplate,
                                          Class<V> valueClass,
                                          ResourceSpec resourceSpec,
                                          RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _entities = new CollectionRequest(KeyValueRecord.class);
    _keyValueRecordFactory
        = new KeyValueRecordFactory(_resourceSpec.getKeyType(),
                                    _resourceSpec.getComplexKeyType(),
                                    _resourceSpec.getKeyParts(),
                                    new TypeSpec<PatchRequest>(PatchRequest.class));
  }

  public BatchPartialUpdateRequestBuilder<K, V> input(K id, PatchRequest<V> patch)
  {

    _entities.getElements().add(_keyValueRecordFactory.create(id, patch));
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
      _entities.getElements().add(_keyValueRecordFactory.create(key, value));
    }
    return this;
  }

  @Override
  @Deprecated
  public BatchPartialUpdateRequestBuilder<K, V> param(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public BatchPartialUpdateRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.setReqParam(key, value);
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
  @Deprecated
  public BatchPartialUpdateRequestBuilder<K, V> header(String key, String value)
  {
    super.setHeader(key, value);
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
    return new BatchPartialUpdateRequest<K, V>(_headers,
                                               _entities,
                                               _queryParams,
                                               _resourceSpec,
                                               _baseURITemplate,
                                               _pathKeys,
                                               _requestOptions);
  }

}
