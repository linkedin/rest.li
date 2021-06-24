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
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import java.util.Map;


/**
 * Builds a type-bound finder request {@link BatchFindRequest} from a batch of criteria.
 *
 * @param <V> entity type for resource
 *
 * @author Jiaqi Guan
 */
public class BatchFindRequestBuilder<K, V extends RecordTemplate>
    extends RestfulRequestBuilder<K, V, BatchFindRequest<V>>
{
  private final Class<V> _elementClass;
  private String _name;

  public BatchFindRequestBuilder(String baseUriTemplate,
                                Class<V> elementClass,
                                ResourceSpec resourceSpec,
                                RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _elementClass = elementClass;
  }

  public BatchFindRequestBuilder<K, V> name(String name)
  {
    setParam(RestConstants.BATCH_FINDER_QUERY_TYPE_PARAM, name);
    _name = name;
    return this;
  }

  public BatchFindRequestBuilder<K, V> assocKey(String key, Object value)
  {
    addAssocKey(key, value);
    return this;
  }

  public BatchFindRequestBuilder<K, V> paginate(int start, int count)
  {
    paginateStart(start);
    paginateCount(count);
    return this;
  }

  public BatchFindRequestBuilder<K, V> paginateStart(int start)
  {
    setParam(RestConstants.START_PARAM, String.valueOf(start));
    return this;
  }

  public BatchFindRequestBuilder<K, V> paginateCount(int count)
  {
    setParam(RestConstants.COUNT_PARAM, String.valueOf(count));
    return this;
  }

  public BatchFindRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

  public BatchFindRequestBuilder<K, V> metadataFields(PathSpec... metadataFieldPaths)
  {
    addMetadataFields(metadataFieldPaths);
    return this;
  }

  public BatchFindRequestBuilder<K, V> pagingFields(PathSpec... pagingFieldPaths)
  {
    addPagingFields(pagingFieldPaths);
    return this;
  }

  @Override
  public BatchFindRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchFindRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchFindRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchFindRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public BatchFindRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchFindRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchFindRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchFindRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public BatchFindRequest<V> build()
  {
    return new BatchFindRequest<>(buildReadOnlyHeaders(),
        buildReadOnlyCookies(),
        _elementClass,
        _resourceSpec,
        buildReadOnlyQueryParameters(),
        getQueryParamClasses(),
        _name,
        getBaseUriTemplate(),
        buildReadOnlyPathKeys(),
        getRequestOptions(),
        buildReadOnlyAssocKey());
  }
}
