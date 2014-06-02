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
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BatchCreateResponseBuilder implements RestLiResponseBuilder
{
  ErrorResponseBuilder _errorResponseBuilder;

  public BatchCreateResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object object,
                                           final Map<String, String> headers) throws IOException
  {
    /** constrained by the signature of {@link CollectionResource#batchCreate} */
    BatchCreateResult<?, ?> list = (BatchCreateResult<?, ?>) object;

    List<CreateIdStatus<Object>> statuses = new ArrayList<CreateIdStatus<Object>>(list.getResults().size());

    for (CreateResponse e : list.getResults())
    {
      statuses.add(new CreateIdStatus<Object>(e.getStatus().getCode(),
                                              e.getId(),
                                              e.getError() == null ? null : _errorResponseBuilder.buildErrorResponse(e.getError()),
                                              ProtocolVersionUtil.extractProtocolVersion(headers)));
    }

    BatchCreateIdResponse<Object> batchCreateIdResponse = new BatchCreateIdResponse<Object>(statuses);

    return new PartialRestResponse.Builder().entity(batchCreateIdResponse).headers(headers).build();
  }
}
