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
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.restli.common.attachments.RestLiDataSourceIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class PartialUpdateRequestBuilder<K, V extends RecordTemplate> extends
    SingleEntityRequestBuilder<K, PatchRequest<V>, PartialUpdateRequest<V>>
{
  private List<Object> _streamingAttachments; //We initialize only when we need to.

  public PartialUpdateRequestBuilder(String baseUriTemplate,
                                     Class<V> valueClass,
                                     ResourceSpec resourceSpec,
                                     RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, null, resourceSpec, requestOptions);
  }

  public PartialUpdateRequestBuilder<K, V> appendSingleAttachment(final RestLiAttachmentDataSourceWriter streamingAttachment)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(streamingAttachment);
    return this;
  }

  public PartialUpdateRequestBuilder<K, V> appendMultipleAttachments(final RestLiDataSourceIterator dataSourceIterator)
  {
    if (_streamingAttachments == null)
    {
      _streamingAttachments = new ArrayList<>();
    }

    _streamingAttachments.add(dataSourceIterator);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> id(K id)
  {
    super.id(id);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> input(PatchRequest<V> entity)
  {
    super.input(entity);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public PartialUpdateRequest<V> build()
  {
    return new PartialUpdateRequest<V>(buildReadOnlyInput(),
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
