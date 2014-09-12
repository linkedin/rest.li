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


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;

import java.util.Map;


public class UpdateResponseBuilder implements RestLiResponseBuilder
{
  @Override
  public PartialRestResponse buildResponse(RoutingResult routingResult, AugmentedRestLiResponseData responseData)
  {
    return new PartialRestResponse.Builder().headers(responseData.getHeaders()).status(responseData.getStatus())
                                            .build();
  }

  @Override
  public AugmentedRestLiResponseData buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object result, Map<String, String> headers)
  {
    UpdateResponse updateResponse = (UpdateResponse) result;
    //Verify that the status in the UpdateResponse is not null. If so, this is a developer error.
    if (updateResponse.getStatus() == null)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. HttpStatus is null inside of a UpdateResponse returned by the resource method: "
              + routingResult.getResourceMethod());
    }
    return new AugmentedRestLiResponseData.Builder(routingResult.getResourceMethod().getMethodType()).headers(headers)
                                                                                                     .status(updateResponse.getStatus())
                                                                                                     .build();
  }
}
