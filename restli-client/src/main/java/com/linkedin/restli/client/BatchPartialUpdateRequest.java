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
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.client.BatchUpdateResponseDecoder;

import java.net.HttpCookie;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchPartialUpdateRequest<K, V extends RecordTemplate> extends
    com.linkedin.restli.client.BatchRequest<BatchKVResponse<K, UpdateStatus>>
{
  private final Map<K, PatchRequest<V>> _partialUpdateInputMap;

  @SuppressWarnings("unchecked")
  public BatchPartialUpdateRequest(Map<String, String> headers,
                            List<HttpCookie> cookies,
                            CollectionRequest<KeyValueRecord<K, PatchRequest<V>>> entities,
                            Map<String, Object> queryParams,
                            Map<String, Class<?>> queryParamClasses,
                            ResourceSpec resourceSpec,
                            String baseUriTemplate,
                            Map<String, Object> pathKeys,
                            RestliRequestOptions requestOptions,
                            Map<K, PatchRequest<V>> patchInputMap,
                            List<Object> streamingAttachments)
  {
    super(ResourceMethod.BATCH_PARTIAL_UPDATE,
          entities,
          headers,
          cookies,
          new BatchUpdateResponseDecoder<K>((TypeSpec<K>) resourceSpec.getKeyType(),
                                            resourceSpec.getKeyParts(),
                                            resourceSpec.getComplexKeyType()),
          resourceSpec,
          queryParams,
          queryParamClasses,
          baseUriTemplate,
          pathKeys,
          requestOptions,
          streamingAttachments);
    _partialUpdateInputMap = Collections.unmodifiableMap(patchInputMap);
  }

  public Map<K, PatchRequest<V>> getPartialUpdateInputMap()
  {
    return _partialUpdateInputMap;
  }
}
