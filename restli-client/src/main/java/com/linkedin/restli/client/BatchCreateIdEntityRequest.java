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
import com.linkedin.restli.common.BatchCreateIdEntityResponse;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.BatchCreateIdEntityDecoder;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Boyang Chen
 */
public class BatchCreateIdEntityRequest<K, V extends RecordTemplate> extends Request<BatchCreateIdEntityResponse<K, V>>
{
  public BatchCreateIdEntityRequest(Map<String, String> headers,
                             List<HttpCookie> cookies,
                             BatchCreateIdEntityDecoder<K, V> decoder,
                             CollectionRequest<V> input,
                             ResourceSpec resourceSpec,
                             Map<String, Object> queryParams,
                             Map<String, Class<?>> queryParamClasses,
                             String baseUriTemplate,
                             Map<String, Object> pathKeys,
                             RestliRequestOptions requestOptions,
                             List<Object> streamingAttachments)
  {
    super(ResourceMethod.BATCH_CREATE,
          input,
          headers,
          cookies,
          decoder,
          resourceSpec,
          queryParams,
          queryParamClasses,
          null,
          baseUriTemplate,
          pathKeys,
          requestOptions,
          streamingAttachments);
  }
}