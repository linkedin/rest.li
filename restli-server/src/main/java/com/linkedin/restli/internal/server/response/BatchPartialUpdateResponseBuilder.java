/*
    Copyright (c) 2017 LinkedIn Corp.

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

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.UpdateEntityResponse;
import com.linkedin.restli.server.UpdateResponse;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * Response builder implementation for BATCH_PARTIAL_UPDATE. Uses much of the shared {@link BatchResponseBuilder}
 * logic, but provides support for returning the patched entities.
 *
 * @author Xiao Ma
 * @author Evan Williams
 */
public class BatchPartialUpdateResponseBuilder extends BatchResponseBuilder<RestLiResponseData<BatchPartialUpdateResponseEnvelope>>
{
  public BatchPartialUpdateResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    super(errorResponseBuilder);
  }

  @Override
  RestLiResponseData<BatchPartialUpdateResponseEnvelope> buildResponseData(HttpStatus status,
      Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap,
      Map<String, String> headers,
      List<HttpCookie> cookies)
  {
    return new RestLiResponseDataImpl<>(new BatchPartialUpdateResponseEnvelope(status, batchResponseMap), headers, cookies);
  }

  /**
   * Defines how to build an update status for batch partial update. If the update response is an {@link UpdateEntityResponse}
   * and the client is requesting the entities to be returned, then build an {@link UpdateEntityStatus} containing the
   * entity to be returned for a given update response.
   * @param resourceContext current resource context
   * @param updateResponse update response returned by the resource method
   * @return update status possibly containing the returned entity
   */
  @Override
  protected UpdateStatus buildUpdateStatus(ResourceContext resourceContext, UpdateResponse updateResponse)
  {
    if (updateResponse instanceof UpdateEntityResponse && resourceContext.shouldReturnEntity())
    {
      final RecordTemplate entity = ((UpdateEntityResponse<?>) updateResponse).getEntity();

      final DataMap entityData = entity != null ? entity.data() : null;
      final DataMap projectedData = RestUtils.projectFields(entityData,
          resourceContext.getProjectionMode(),
          resourceContext.getProjectionMask());

      return new UpdateEntityStatus<>(updateResponse.getStatus().getCode(),
                                      new AnyRecord(projectedData));
    }
    else
    {
      return super.buildUpdateStatus(resourceContext, updateResponse);
    }
  }
}
