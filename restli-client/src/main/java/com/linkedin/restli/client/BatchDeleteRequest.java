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
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.client.BatchKVResponseDecoder;
import java.net.URI;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchDeleteRequest<K, V extends RecordTemplate> extends BatchRequest<BatchKVResponse<K, UpdateStatus>>
{
  @SuppressWarnings("unchecked")
  BatchDeleteRequest(Map<String, String> headers,
                     Map<String, Object> queryParams,
                     ResourceSpec resourceSpec,
                     String baseUriTemplate,
                     Map<String, Object> pathKeys,
                     RestliRequestOptions requestOptions)
  {
    super(ResourceMethod.BATCH_DELETE,
          null,
          headers,
          new BatchKVResponseDecoder<K, UpdateStatus>(UpdateStatus.class,
                                                      (Class<K>) resourceSpec.getKeyClass(),
                                                      resourceSpec.getKeyParts(),
                                                      resourceSpec.getKeyKeyClass(),
                                                      resourceSpec.getKeyParamsClass()),
          resourceSpec,
          queryParams,
          baseUriTemplate,
          pathKeys,
          requestOptions);
  }

  /**
   * @deprecated Please use {@link com.linkedin.restli.client.uribuilders.RestliUriBuilder#buildBaseUri()} instead
   * @return
   */
  @Deprecated
  public URI getBaseUri()
  {
    return RestliUriBuilderUtil.createUriBuilder(this).buildBaseUri();
  }
}
