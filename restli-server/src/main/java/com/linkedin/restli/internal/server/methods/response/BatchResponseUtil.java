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

package com.linkedin.restli.internal.server.methods.response;


import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Keren Jin
 */
class BatchResponseUtil
{
  static <K> Map<K, ErrorResponse> populateErrors(final Map<K, RestLiServiceException> serviceErrors,
                                                  final RoutingResult routingResult,
                                                  final ErrorResponseBuilder builder)
  {
    final Map<K, ErrorResponse> mergedErrors = new HashMap<K, ErrorResponse>();

    for (Map.Entry<K, RestLiServiceException> serviceErrorEntry : serviceErrors.entrySet())
    {
      if (serviceErrorEntry.getKey() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key inside of the errors map returned by resource method: "
                + routingResult.getResourceMethod());
      }

      if (serviceErrorEntry.getValue() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null value inside of the errors map returned by resource method: "
                + routingResult.getResourceMethod());
      }
      mergedErrors.put(serviceErrorEntry.getKey(), builder.buildErrorResponse(serviceErrorEntry.getValue()));
    }

    final ServerResourceContext context = (ServerResourceContext) routingResult.getContext();
    for (Map.Entry<Object, RestLiServiceException> batchErrorEntry : context.getBatchKeyErrors().entrySet())
    {
      @SuppressWarnings("unchecked")
      final K errorKey = (K) batchErrorEntry.getKey();
      mergedErrors.put(errorKey, builder.buildErrorResponse(batchErrorEntry.getValue()));
    }

    return mergedErrors;
  }
}
