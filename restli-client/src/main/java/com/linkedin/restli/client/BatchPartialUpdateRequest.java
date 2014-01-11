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
import com.linkedin.restli.internal.client.CollectionRequestUtil;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.client.BatchKVResponseDecoder;
import java.net.URI;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchPartialUpdateRequest<K, V extends RecordTemplate> extends
    com.linkedin.restli.client.BatchRequest<BatchKVResponse<K, UpdateStatus>>
{
  @SuppressWarnings("unchecked")
  BatchPartialUpdateRequest(Map<String, String> headers,
                            CollectionRequest<KeyValueRecord<K, PatchRequest>> entities,
                            Map<String, Object> queryParams,
                            ResourceSpec resourceSpec,
                            String baseUriTemplate,
                            Map<String, Object> pathKeys,
                            RestliRequestOptions requestOptions)
  {
    super(ResourceMethod.BATCH_PARTIAL_UPDATE,
          entities,
          headers,
          new BatchKVResponseDecoder<K, UpdateStatus>(new TypeSpec<UpdateStatus>(UpdateStatus.class),
                                                      (TypeSpec<K>) resourceSpec.getKeyType(),
                                                      resourceSpec.getKeyParts(),
                                                      resourceSpec.getComplexKeyType()),
          resourceSpec,
          queryParams,
          baseUriTemplate,
          pathKeys,
          requestOptions);
  }

  /**
   * @deprecated Please use {@link #getInputRecord()} instead
   */
  @SuppressWarnings("unchecked")
  @Deprecated
  @Override
  public RecordTemplate getInput()
  {
    return CollectionRequestUtil.convertToBatchRequest((CollectionRequest<KeyValueRecord>) getInputRecord(),
                                                       getResourceSpec().getKeyType(),
                                                       getResourceSpec().getComplexKeyType(),
                                                       getResourceSpec().getKeyParts(),
                                                       new TypeSpec<PatchRequest>(PatchRequest.class));
  }

  /**
   * @deprecated Please use {@link com.linkedin.restli.client.uribuilders.RestliUriBuilder#buildBaseUri()} instead
   * @return
   */
  @Deprecated
  public URI getBaseURI()
  {
    return RestliUriBuilderUtil.createUriBuilder(this).buildBaseUri();
  }
}
