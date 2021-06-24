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
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.restli.common.attachments.RestLiDataSourceIterator;
import com.linkedin.restli.internal.client.BatchCreateDecoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * This class has been deprecated. Please use {@link BatchCreateIdRequestBuilder} instead.
 *
 * @author Josh Walker
 */
public class BatchCreateRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, BatchCreateRequest<V>>
{
  private final List<V> _entities = new ArrayList<>();
  private final Class<V> _valueClass;
  private List<Object> _streamingAttachments; //We initialize only when we need to.

  public BatchCreateRequestBuilder(String baseUriTemplate,
                                   Class<V> valueClass,
                                   ResourceSpec resourceSpec,
                                   RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _valueClass = valueClass;
  }

  public BatchCreateRequestBuilder<K, V> input(V entity)
  {
    _entities.add(entity);
    return this;
  }

  public BatchCreateRequestBuilder<K, V> inputs(List<V> entities)
  {
    _entities.addAll(entities);
    return this;
  }

  public BatchCreateRequestBuilder<K, V> appendSingleAttachment(final RestLiAttachmentDataSourceWriter streamingAttachment)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(streamingAttachment);
    return this;
  }

  public BatchCreateRequestBuilder<K, V> appendMultipleAttachments(final RestLiDataSourceIterator dataSourceIterator)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(dataSourceIterator);
    return this;
  }

  @Override
  public BatchCreateRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchCreateRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchCreateRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchCreateRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public BatchCreateRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchCreateRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchCreateRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchCreateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public BatchCreateRequest<V> build()
  {
    @SuppressWarnings("unchecked")
    BatchCreateDecoder<K> decoder = new BatchCreateDecoder<>((TypeSpec<K>) _resourceSpec.getKeyType(),
        _resourceSpec.getKeyParts(),
        _resourceSpec.getComplexKeyType());

    return new BatchCreateRequest<>(buildReadOnlyHeaders(),
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
      CollectionRequest<V> input = new CollectionRequest<>(map, _valueClass);

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
