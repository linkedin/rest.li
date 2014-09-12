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

package com.linkedin.restli.internal.server.methods.response;


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class BatchCreateResponseBuilder implements RestLiResponseBuilder
{
  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchCreateResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  public PartialRestResponse buildResponse(RoutingResult routingResult, AugmentedRestLiResponseData responseData)
  {
    PartialRestResponse.Builder builder = new PartialRestResponse.Builder();
    @SuppressWarnings("unchecked")
    List<CreateIdStatus<Object>> elements = (List<CreateIdStatus<Object>>) responseData.getCollectionResponse();
    BatchCreateIdResponse<Object> batchCreateIdResponse = new BatchCreateIdResponse<Object>(elements);
    builder.entity(batchCreateIdResponse);
    return builder.headers(responseData.getHeaders()).build();
  }

  @Override
  public AugmentedRestLiResponseData buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object result, Map<String, String> headers)
  {
    BatchCreateResult<?, ?> list = (BatchCreateResult<?, ?>) result;

    //Verify that a null list was not passed into the BatchCreateResult. If so, this is a developer error.
    if (list.getResults() == null)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. Null List inside of a BatchCreateResult returned by the resource method: " + routingResult
              .getResourceMethod());
    }

    List<CreateIdStatus<Object>> statuses = new ArrayList<CreateIdStatus<Object>>(list.getResults().size());
    for (CreateResponse e : list.getResults())
    {
      //Verify that a null element was not passed into the BatchCreateResult list. If so, this is a developer error.
      if (e == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null element inside of List inside of a BatchCreateResult returned by the resource method: "
                + routingResult.getResourceMethod());
      }
      statuses.add(new CreateIdStatus<Object>(e.getStatus().getCode(), e.getId(), e.getError() == null ? null
          : _errorResponseBuilder.buildErrorResponse(e.getError()), ProtocolVersionUtil.extractProtocolVersion(headers)));
    }
    return new AugmentedRestLiResponseData.Builder(routingResult.getResourceMethod().getMethodType()).headers(headers)
                                                                                                     .collectionEntities(statuses)
                                                                                                     .build();
  }
}
