/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.client;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.internal.common.util.CollectionUtils;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.KeyValueRecordFactory;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.restli.common.attachments.RestLiDataSourceIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Request Builder for BATCH_PARTIAL_UPDATE requests where the patched entities can be returned.
 * Builds {@link BatchPartialUpdateEntityRequest} objects.
 *
 * @param <K> key class
 * @param <V> entity class
 *
 * @author Evan Williams
 */
public class BatchPartialUpdateEntityRequestBuilder<K, V extends RecordTemplate> extends
    BatchKVRequestBuilder<K, V, BatchPartialUpdateEntityRequest<K, V>> implements ReturnEntityRequestBuilder
{
  private final KeyValueRecordFactory<K, PatchRequest<V>> _keyValueRecordFactory;
  private final Map<K, PatchRequest<V>> _partialUpdateInputMap;
  private List<Object> _streamingAttachments; //We initialize only when we need to.

  @SuppressWarnings({"unchecked", "rawtypes"})
  public BatchPartialUpdateEntityRequestBuilder(String baseUriTemplate,
                                                Class<V> valueClass,
                                                ResourceSpec resourceSpec,
                                                RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _partialUpdateInputMap = new HashMap<>();
    _keyValueRecordFactory = new KeyValueRecordFactory(_resourceSpec.getKeyType(),
                                    _resourceSpec.getComplexKeyType(),
                                    _resourceSpec.getKeyParts(),
                                    new TypeSpec<>(PatchRequest.class));
  }

  public BatchPartialUpdateEntityRequestBuilder<K, V> input(K id, PatchRequest<V> patch)
  {
    _partialUpdateInputMap.put(id, patch);
    addKey(id);
    return this;
  }

  public BatchPartialUpdateEntityRequestBuilder<K, V> inputs(Map<K, PatchRequest<V>> patches)
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

  public BatchPartialUpdateEntityRequestBuilder<K, V> appendSingleAttachment(final RestLiAttachmentDataSourceWriter streamingAttachment)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(streamingAttachment);
    return this;
  }

  public BatchPartialUpdateEntityRequestBuilder<K, V> appendMultipleAttachments(final RestLiDataSourceIterator dataSourceIterator)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(dataSourceIterator);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  public BatchPartialUpdateEntityRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequestBuilder<K, V> returnEntity(boolean value)
  {
    setReturnEntityParam(value);
    return this;
  }

  @Override
  public BatchPartialUpdateEntityRequest<K, V> build()
  {
    ensureBatchKeys();

    Map<K, PatchRequest<V>> readOnlyPartialUpdateInputMap = new HashMap<>(
            CollectionUtils.getMapInitialCapacity(_partialUpdateInputMap.size(), 0.75f), 0.75f);
    CollectionRequest<KeyValueRecord<K, PatchRequest<V>>> readOnlyInput = buildReadOnlyInput(readOnlyPartialUpdateInputMap, _partialUpdateInputMap, _keyValueRecordFactory);

    return new BatchPartialUpdateEntityRequest<>(buildReadOnlyHeaders(),
                                                 buildReadOnlyCookies(),
                                                 readOnlyInput,
                                                 buildReadOnlyQueryParameters(),
                                                 getQueryParamClasses(),
                                                 _resourceSpec,
                                                 getBaseUriTemplate(),
                                                 buildReadOnlyPathKeys(),
                                                 getRequestOptions(),
                                                 readOnlyPartialUpdateInputMap,
                                                 _streamingAttachments == null ? null : Collections.unmodifiableList(_streamingAttachments));
  }
}
