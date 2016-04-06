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

package com.linkedin.restli.client;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.restli.common.attachments.RestLiDataSourceIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class UpdateRequestBuilder<K, V extends RecordTemplate> extends
    SingleEntityRequestBuilder<K, V, UpdateRequest<V>>
{
  private List<Object> _streamingAttachments; //We initialize only when we need to.

  public UpdateRequestBuilder(String baseUriTemplate,
                              Class<V> valueClass,
                              ResourceSpec resourceSpec,
                              RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, valueClass, resourceSpec, requestOptions);
  }

  public UpdateRequestBuilder<K, V> appendSingleAttachment(final RestLiAttachmentDataSourceWriter streamingAttachment)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(streamingAttachment);
    return this;
  }

  public UpdateRequestBuilder<K, V> appendMultipleAttachments(final RestLiDataSourceIterator dataSourceIterator)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(dataSourceIterator);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> id(K id)
  {
    super.id(id);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> input(V entity)
  {
    super.input(entity);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public UpdateRequest<V> build()
  {
    return new UpdateRequest<V>(buildReadOnlyInput(),
                                buildReadOnlyHeaders(),
                                buildReadOnlyCookies(),
                                _resourceSpec,
                                buildReadOnlyQueryParameters(),
                                getQueryParamClasses(),
                                getBaseUriTemplate(),
                                buildReadOnlyPathKeys(),
                                getRequestOptions(),
                                buildReadOnlyId(),
                                _streamingAttachments == null ? null : Collections.unmodifiableList(_streamingAttachments));
  }
}
