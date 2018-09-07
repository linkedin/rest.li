/*
   Copyright (c) 2015 LinkedIn Corp.

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


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.restli.common.attachments.RestLiDataSourceIterator;
import com.linkedin.restli.internal.client.BatchCreateIdEntityDecoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Request Builder for Batch Creates that can respond with strongly-typed keys as first-class citizens.
 *
 * @author  Boyang Chen
 */
public class BatchCreateIdEntityRequestBuilder<K, V extends RecordTemplate>
    extends RestfulRequestBuilder<K, V, BatchCreateIdEntityRequest<K, V>> implements ReturnEntityRequestBuilder
{
  private final List<V> _entities = new ArrayList<V>();
  private final Class<V> _valueClass;
  private List<Object> _streamingAttachments; //We initialize only when we need to.

  protected BatchCreateIdEntityRequestBuilder(String baseURITemplate,
                                              Class<V> valueClass,
                                              ResourceSpec resourceSpec,
                                              RestliRequestOptions requestOptions)
  {
    super(baseURITemplate, resourceSpec, requestOptions);
    _valueClass = valueClass;
  }

  public BatchCreateIdEntityRequestBuilder<K, V> input(V entity)
  {
    _entities.add(entity);
    return this;
  }

  public BatchCreateIdEntityRequestBuilder<K, V> inputs(List<V> entities)
  {
    _entities.addAll(entities);
    return this;
  }

  public BatchCreateIdEntityRequestBuilder<K, V> appendSingleAttachment(final RestLiAttachmentDataSourceWriter streamingAttachment)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(streamingAttachment);
    return this;
  }

  public BatchCreateIdEntityRequestBuilder<K, V> appendMultipleAttachments(final RestLiDataSourceIterator dataSourceIterator)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(dataSourceIterator);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  public BatchCreateIdEntityRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequestBuilder<K, V> returnEntity(boolean value)
  {
    setReturnEntityParam(value);
    return this;
  }

  @Override
  public BatchCreateIdEntityRequest<K, V> build()
  {
    @SuppressWarnings("unchecked")
    BatchCreateIdEntityDecoder<K, V> decoder = new BatchCreateIdEntityDecoder<K, V>((TypeSpec<K>)_resourceSpec.getKeyType(),
                                                                                    (TypeSpec<V>)_resourceSpec.getValueType(),
                                                                                    _resourceSpec.getKeyParts(),
                                                                                    _resourceSpec.getComplexKeyType());

    return new BatchCreateIdEntityRequest<K, V>(buildReadOnlyHeaders(),
                                                buildReadOnlyCookies(),
                                                decoder,
                                                buildReadOnlyInput(),
                                                _resourceSpec,
                                                buildReadOnlyQueryParameters(),
                                                getQueryParamClasses(),
                                                getBaseUriTemplate(),
                                                buildReadOnlyPathKeys(),
                                                getRequestOptions(),
                                                _streamingAttachments == null ? null : Collections.unmodifiableList(_streamingAttachments));
  }

  private CollectionRequest<V> buildReadOnlyInput()
  {
    try
    {
      DataMap map = new DataMap();
      CollectionRequest<V> input = new CollectionRequest<V>(map, _valueClass);

      for (V entity : _entities)
      {
        input.getElements().add(getReadOnlyOrCopyDataTemplate(entity));
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