/*
   Copyright (c) 2016 LinkedIn Corp.

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
package com.linkedin.restli.internal.server.response;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.ResourceMethod;

import java.util.List;


/**
 * Contains response data for {@link ResourceMethod#GET_ALL}.
 *
 * @author gye
 */
public class GetAllResponseEnvelope extends CollectionResponseEnvelope
{
  /**
   * Instantiates a get-all response envelope.
   *
   * @param collectionResponse The entities of the request.
   * @param collectionResponsePaging Paging for the collection response.
   * @param collectionResponseCustomMetadata the custom metadata used for this collection response.
   * @param restLiResponseData wrapper response data that is storing this envelope.
   */
  GetAllResponseEnvelope(List<? extends RecordTemplate> collectionResponse,
                         CollectionMetadata collectionResponsePaging,
                         RecordTemplate collectionResponseCustomMetadata,
                         RestLiResponseDataImpl restLiResponseData)
  {
    super(collectionResponse, collectionResponsePaging, collectionResponseCustomMetadata, restLiResponseData);
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.GET_ALL;
  }
}
