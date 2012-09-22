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

package com.linkedin.restli.internal.server.methods.response;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;

public class BatchGetResponseBuilder extends AbstractBatchResponseBuilder<RecordTemplate> implements
    RestLiResponseBuilder
{
  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object object,
                                           final Map<String, String> headers)
      throws IOException
  {
    @SuppressWarnings({ "unchecked" })
    /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
    Map<?, ? extends RecordTemplate> map = (Map<?, ? extends RecordTemplate>)object;
    Map<?, RestLiServiceException> errorMap = Collections.emptyMap();

    if (object instanceof BatchResult)
    {
      @SuppressWarnings({"unchecked"})
      /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
      BatchResult<?, ? extends RecordTemplate> batchResult =
          (BatchResult<?, ? extends RecordTemplate>) object;
      errorMap = batchResult.getErrors();
    }

    int numErrors =
        errorMap.size()
            + ((ServerResourceContext) routingResult.getContext()).getBatchKeyErrors()
                                                                  .size();

    Class<? extends RecordTemplate> valueClass =
        routingResult.getResourceMethod().getResourceModel().getValueClass();

    BatchResponse<AnyRecord> batchResponse =
        createBatchResponse(AnyRecord.class, map.size(), numErrors);

    populateResults(batchResponse,
                    map,
                    headers,
                    valueClass,
                    routingResult.getContext());

    populateErrors(request,
                   routingResult,
                   errorMap,
                   headers,
                   batchResponse);

    return new PartialRestResponse(batchResponse);
  }

  @Override
  protected DataMap buildResultRecord(final RecordTemplate o,
                                      final ResourceContext resourceContext)
  {
    DataMap data = RestUtils.projectFields(o.data(), resourceContext);
    return data;
  }
}
