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


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;

import java.util.Map;


/**
 * REST collection finder request builder.
 *
 * @param <V> entity type for resource
 *
 * @author Eran Leshem
 */
public class FindRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, FindRequest<V>>
{
  private final Class<V> _elementClass;
  private String _name;

  public FindRequestBuilder(String baseUriTemplate,
                            Class<V> elementClass,
                            ResourceSpec resourceSpec,
                            RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _elementClass = elementClass;
  }

  public FindRequestBuilder<K, V> name(String name)
  {
    setParam(RestConstants.QUERY_TYPE_PARAM, name);
    _name = name;
    return this;
  }

  public FindRequestBuilder<K, V> assocKey(String key, Object value)
  {
    addAssocKey(key, value);
    return this;
  }

  public FindRequestBuilder<K, V> paginate(int start, int count)
  {
    paginateStart(start);
    paginateCount(count);
    return this;
  }

  public FindRequestBuilder<K, V> paginateStart(int start)
  {
    setParam(RestConstants.START_PARAM, String.valueOf(start));
    return this;
  }

  public FindRequestBuilder<K, V> paginateCount(int count)
  {
    setParam(RestConstants.COUNT_PARAM, String.valueOf(count));
    return this;
  }

  public FindRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

  public FindRequestBuilder<K, V> metadataFields(PathSpec... metadataFieldPaths)
  {
    addMetadataFields(metadataFieldPaths);
    return this;
  }

  public FindRequestBuilder<K, V> pagingFields(PathSpec... pagingFieldPaths)
  {
    addPagingFields(pagingFieldPaths);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public FindRequest<V> build()
  {
    return new FindRequest<V>(buildReadOnlyHeaders(),
                              _elementClass,
                              _resourceSpec,
                              buildReadOnlyQueryParameters(),
                              _name,
                              getBaseUriTemplate(),
                              buildReadOnlyPathKeys(),
                              getRequestOptions(),
                              buildReadOnlyAssocKey());
  }
}
