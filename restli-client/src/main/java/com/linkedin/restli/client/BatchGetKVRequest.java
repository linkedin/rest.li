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
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.RestResponseDecoder;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A request for reading a resource batch.
 *
 * @param <K> resource key class
 * @param <V> entity template class
 *
 * @author Eran Leshem
 */
public class BatchGetKVRequest<K, V extends RecordTemplate> extends BatchRequest<BatchKVResponse<K, V>>
{
  BatchGetKVRequest(Map<String, String> headers,
                    List<HttpCookie> cookies,
                    RestResponseDecoder<BatchKVResponse<K, V>> decoder,
                    Map<String, Object> queryParams,
                    Map<String, Class<?>> queryParamClasses,
                    ResourceSpec resourceSpec,
                    String baseUriTemplate,
                    Map<String, Object> pathKeys,
                    RestliRequestOptions requestOptions)
  {
    super(ResourceMethod.BATCH_GET,
          null,
          headers,
          cookies,
          decoder,
          resourceSpec,
          queryParams,
          queryParamClasses,
          baseUriTemplate,
          pathKeys,
          requestOptions,
          null);
  }

  @Override
  public Set<PathSpec> getFields()
  {
    return super.getFields();
  }
}
