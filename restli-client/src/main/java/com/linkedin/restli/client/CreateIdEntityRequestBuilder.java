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

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.IdEntityResponseDecoder;

import java.util.Map;

/**
 * @author Boyang Chen
 */
public class CreateIdEntityRequestBuilder<K, V extends RecordTemplate> extends SingleEntityRequestBuilder<K, V, CreateIdEntityRequest<K, V>>
{
  protected CreateIdEntityRequestBuilder(String baseURITemplate,
                                         Class<V> valueClass,
                                         ResourceSpec resourceSpec,
                                         RestliRequestOptions requestOptions)
  {
    super(baseURITemplate, valueClass, resourceSpec, requestOptions);
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> input(V entity)
  {
    super.input(entity);
    return this;
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public CreateIdEntityRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public CreateIdEntityRequest<K, V> build()
  {
    @SuppressWarnings("unchecked")
    IdEntityResponseDecoder<K, V> idEntityResponseDecoder = new IdEntityResponseDecoder<K, V>((TypeSpec<K>)_resourceSpec.getKeyType(),
                                                                                              _resourceSpec.getKeyParts(),
                                                                                              _resourceSpec.getComplexKeyType(),
                                                                                              getValueClass());
    return new CreateIdEntityRequest<K, V>(buildReadOnlyInput(),
                                           buildReadOnlyHeaders(),
                                           idEntityResponseDecoder,
                                           _resourceSpec,
                                           buildReadOnlyQueryParameters(),
                                           getBaseUriTemplate(),
                                           buildReadOnlyPathKeys(),
                                           getRequestOptions());
  }
}
