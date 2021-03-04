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
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class GetAllRequest<T extends RecordTemplate> extends
    Request<CollectionResponse<T>>
{
  private final CompoundKey _assocKey;

  public GetAllRequest(Map<String, String> headers,
                List<HttpCookie> cookies,
                Class<T> templateClass,
                ResourceSpec resourceSpec,
                Map<String, Object> queryParams,
                Map<String, Class<?>> queryParamClasses,
                String baseUriTemplate,
                Map<String, Object> pathKeys,
                RestliRequestOptions requestOptions,
                CompoundKey assocKey)
  {
    super(ResourceMethod.GET_ALL,
          null,
          headers,
          cookies,
          new CollectionResponseDecoder<T>(templateClass),
          resourceSpec,
          queryParams,
          queryParamClasses,
          null,
          baseUriTemplate,
          pathKeys,
          requestOptions,
          null);
    _assocKey = assocKey;
  }

  public CompoundKey getAssocKey()
  {
    return _assocKey;
  }

  @Override
  public Set<PathSpec> getFields()
  {
    return super.getFields();
  }
}
