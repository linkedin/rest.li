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
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;
import java.io.IOException;
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
    @SuppressWarnings({ "unchecked" })
    /** constrained by the signature of {@link CollectionResource#batchCreate} */
    BatchCreateResult<?, ?> list = (BatchCreateResult<?, ?>) object;

    CollectionResponse<CreateStatus> batchResponse =
        new CollectionResponse<CreateStatus>(CreateStatus.class);

    for (CreateResponse e : list.getResults())
    {
      CreateStatus s = new CreateStatus();
      if (e.hasId())
      {
        s.setId(e.getId().toString());
      }
      s.setStatus(e.getStatus().getCode());
      if(e.hasError())
      {
        ErrorResponse errorResponse = _errorResponseBuilder.buildErrorResponse(e.getError());
        s.setError(errorResponse);
      }
      batchResponse.getElements().add(s);
    }
    headers.put(RestConstants.HEADER_RESTLI_TYPE, CollectionResponse.class.getName());
    headers.put(RestConstants.HEADER_RESTLI_SUB_TYPE, CreateStatus.class.getName());

    return new PartialRestResponse(batchResponse);
  }
}
