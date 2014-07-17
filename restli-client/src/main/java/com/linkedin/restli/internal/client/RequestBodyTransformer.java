/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.client;


import com.linkedin.data.DataMap;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceProperties;
import com.linkedin.restli.common.TypeSpec;


/**
 * Transforms the {@link Request} body for {@link com.linkedin.restli.client.BatchUpdateRequest}s and
 * {@link com.linkedin.restli.client.BatchPartialUpdateRequest}s from the new body encoding format to the old body
 * encoding format. For other {@link Request}s no transformation takes place.
 *
 * @author kparikh
 */
public class RequestBodyTransformer
{
  private RequestBodyTransformer()
  {
  }

  /**
   * For {@link com.linkedin.restli.client.BatchUpdateRequest}s and {@link com.linkedin.restli.client.BatchPartialUpdateRequest}s convert the body to the old style body
   * encoding. For any other {@link Request} simply return the {@link DataMap} of the underlying {@link com.linkedin.data.template.RecordTemplate}
   *
   * @param request the {@link Request} whose body we need to transform
   * @param version the {@link ProtocolVersion} to use
   * @param <T>
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> DataMap transform(Request<T> request, ProtocolVersion version)
  {
    ResourceProperties resourceProperties = null;
    switch (request.getMethod())
    {
      case BATCH_UPDATE:
        resourceProperties = request.getResourceProperties();

        return CollectionRequestUtil.
            convertToBatchRequest((CollectionRequest<KeyValueRecord>) request.getInputRecord(),
                                  resourceProperties.getKeyType(),
                                  resourceProperties.getComplexKeyType(),
                                  resourceProperties.getKeyParts(),
                                  resourceProperties.getValueType(),
                                  version).data();
      case BATCH_PARTIAL_UPDATE:
        resourceProperties = request.getResourceProperties();

        return CollectionRequestUtil.
            convertToBatchRequest((CollectionRequest<KeyValueRecord>) request.getInputRecord(),
                                  resourceProperties.getKeyType(),
                                  resourceProperties.getComplexKeyType(),
                                  resourceProperties.getKeyParts(),
                                  new TypeSpec<PatchRequest>(PatchRequest.class),
                                  version).data();
      default:
        return request.getInputRecord().data();
    }
  }
}
