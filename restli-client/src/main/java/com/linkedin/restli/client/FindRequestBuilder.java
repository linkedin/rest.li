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
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;

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

  public FindRequestBuilder(String baseUriTemplate, Class<V> elementClass, ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, resourceSpec);
    _elementClass = elementClass;

  }

  public FindRequestBuilder<K, V> name(String name)
  {
    addParam(RestConstants.QUERY_TYPE_PARAM, name);
    return this;
  }

  public FindRequestBuilder<K, V> assocKey(String key, Object value)
  {
    addAssocKey(key, value);
    return this;
  }

  public FindRequestBuilder<K, V> paginate(int start, int count)
  {
    addParam(RestConstants.START_PARAM, String.valueOf(start));
    addParam(RestConstants.COUNT_PARAM, String.valueOf(count));
    return this;
  }

  public FindRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> param(String key, Object value)
  {
    super.param(key, value);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.reqParam(key, value);
    return this;
  }

  @Override
  public FindRequestBuilder<K, V> header(String key, String value)
  {
    super.header(key, value);
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
    UriBuilder b = UriBuilder.fromUri(bindPathKeys());
    appendAssocKeys(b);
    appendQueryParams(b);

    return new FindRequest<V>(b.build(), _headers, _elementClass, _resourceSpec, getResourcePath());
  }
}
