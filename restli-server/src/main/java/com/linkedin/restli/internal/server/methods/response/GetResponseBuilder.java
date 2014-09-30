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


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.ResourceContext;

import java.util.Map;


public class GetResponseBuilder implements RestLiResponseBuilder
{
  @Override
  public PartialRestResponse buildResponse(RoutingResult routingResult, AugmentedRestLiResponseData responseData)
  {
    return new PartialRestResponse.Builder().headers(responseData.getHeaders()).status(responseData.getStatus())
                                            .entity(responseData.getEntityResponse()).build();
  }

  @Override
  public AugmentedRestLiResponseData buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object result, Map<String, String> headers)
  {
    final RecordTemplate record;
    final HttpStatus status;
    if (result instanceof GetResult)
    {
      final GetResult<?> getResult = (GetResult<?>) result;
      record = getResult.getValue();
      status = getResult.getStatus();
    }
    else
    {
      record = (RecordTemplate) result;
      status = HttpStatus.S_200_OK;
    }
    final ResourceContext resourceContext = routingResult.getContext();
    final DataMap data = RestUtils.projectFields(record.data(), resourceContext.getProjectionMode(),
        resourceContext.getProjectionMask());
    return new AugmentedRestLiResponseData.Builder(routingResult.getResourceMethod().getMethodType()).headers(headers)
                                                                                                     .status(status)
                                                                                                     .entity(new AnyRecord(data))
                                                                                                     .build();
  }
}