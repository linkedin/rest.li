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

package com.linkedin.restli.internal.server.methods;

import java.io.IOException;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public final class BatchUpdateResponseBuilder extends
    AbstractBatchResponseBuilder<UpdateResponse> implements RestLiResponseBuilder
{

  private static final BatchUpdateResponseBuilder _INSTANCE = new BatchUpdateResponseBuilder();

  public static BatchUpdateResponseBuilder getInstance()
  {
    return _INSTANCE;
  }

  private BatchUpdateResponseBuilder()
  {
  }

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object object,
                                           final Map<String, String> headers)
      throws IOException
  {
    @SuppressWarnings({ "unchecked" })
    /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchUpdate(java.util.Map)} */
    BatchUpdateResult<?, ?> result = (BatchUpdateResult<?, ?>) object;

    Map<?, UpdateResponse> resultsMap = result.getResults();
    Map<?, RestLiServiceException> errorMap = result.getErrors();

    BatchResponse<UpdateStatus> batchResponse =
        createBatchResponse(UpdateStatus.class, resultsMap.size(), errorMap.size());

    populateErrors(request,
                   routingResult,
                   errorMap,
                   headers,
                   batchResponse);


    populateResults(batchResponse,
                    resultsMap,
                    headers,
                    UpdateStatus.class,
                    routingResult.getContext());


    return new PartialRestResponse(batchResponse);
  }

  @Override
  protected DataMap buildResultRecord(final UpdateResponse o,
                                      final ResourceContext resourceContext)
  {
    UpdateStatus output = new UpdateStatus();
    output.setStatus(o.getStatus().getCode());
    return output.data();
  }


}
