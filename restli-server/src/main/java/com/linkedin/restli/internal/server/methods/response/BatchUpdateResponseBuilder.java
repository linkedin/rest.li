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


import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.internal.common.util.CollectionUtils;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Josh Walker
 * @version $Revision: $
 */
public final class BatchUpdateResponseBuilder implements RestLiResponseBuilder
{
  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchUpdateResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  public PartialRestResponse buildResponse(RoutingResult routingResult, AugmentedRestLiResponseData responseData)
  {
    PartialRestResponse.Builder builder = new PartialRestResponse.Builder();
    final ProtocolVersion protocolVersion =
        ((ServerResourceContext) routingResult.getContext()).getRestliProtocolVersion();
    @SuppressWarnings("unchecked")
    final BatchResponse<AnyRecord> response =
        toBatchResponse((Map<Object, UpdateStatus>) responseData.getBatchResponseMap(), protocolVersion);
    builder.entity(response);
    return builder.headers(responseData.getHeaders()).build();
  }

  @Override
  public AugmentedRestLiResponseData buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object result, Map<String, String> headers)
  {
    @SuppressWarnings({ "unchecked" })
    /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchUpdate(java.util.Map)} */
    final BatchUpdateResult<Object, ?> updateResult = (BatchUpdateResult<Object, ?>) result;

    final Map<Object, UpdateResponse> results = updateResult.getResults();
    //Verify the map is not null. If so, this is a developer error.
    if (results == null)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. Null Map found inside of the BatchUpdateResult returned by the resource method: "
              + routingResult.getResourceMethod());
    }

    final Map<Object, RestLiServiceException> serviceErrors = updateResult.getErrors();
    //Verify the errors map is not null. If so, this is a developer error.
    //Note that we don't have to check the errors map for nulls, because its taken care
    //of in populateErrors below.
    if (serviceErrors == null)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. Null errors Map found inside of the BatchUpdateResult returned by the resource method: "
              + routingResult.getResourceMethod());
    }

    final Map<Object, ErrorResponse> errors =
        BatchResponseUtil.populateErrors(serviceErrors, routingResult, _errorResponseBuilder);

    final Set<Object> mergedKeys = new HashSet<Object>(results.keySet());
    //Verify that there is no null key in the UpdateResponse map. If so, this is a developer error. Note that we wait
    //until this point to check for the existence of a null key since we can't check directly on the updates map
    //since certain Map implementations, such as java.util.concurrent.ConcurrentHashMap, throw an NPE if containsKey(null) is called.
    if (mergedKeys.contains(null))
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. Null key inside of the Map returned inside of the BatchUpdateResult returned by the resource method: "
              + routingResult.getResourceMethod());
    }

    mergedKeys.addAll(errors.keySet());

    final Map<Object, UpdateStatus> mergedResults =
        new HashMap<Object, UpdateStatus>(
            CollectionUtils.getMapInitialCapacity(mergedKeys.size(), 0.75f), 0.75f);

    for (Object key : mergedKeys)
    {
      final UpdateStatus mergedResult = new UpdateStatus();

      final UpdateResponse update = results.get(key);
      if (update != null)
      {
        mergedResult.setStatus(update.getStatus().getCode());
      }

      final ErrorResponse error = errors.get(key);
      if (error != null)
      {
        // The status from RestLiServiceException/ErrorResponse overwrites the one in UpdateResponse,
        // if both are provided for the same key.
        mergedResult.setStatus(error.getStatus());
        mergedResult.setError(error);
      }

      mergedResults.put(key, mergedResult);
    }

    return new AugmentedRestLiResponseData.Builder(routingResult.getResourceMethod().getMethodType()).batchKeyEntityMap(mergedResults)
                                                                                                     .headers(headers)
                                                                                                     .build();
  }

  private static <K> BatchResponse<AnyRecord> toBatchResponse(Map<K, UpdateStatus> statuses,
                                                              ProtocolVersion protocolVersion)
  {
    final DataMap splitResponseData = new DataMap();
    final DataMap splitStatuses = new DataMap();
    final DataMap splitErrors = new DataMap();

    for (Map.Entry<K, UpdateStatus> statusEntry : statuses.entrySet())
    {
      final DataMap statusData = statusEntry.getValue().data();
      final String stringKey = URIParamUtils.encodeKeyForBody(statusEntry.getKey(), false, protocolVersion);

      final DataMap error = statusData.getDataMap("error");
      if (error == null)
      {
        // status and error should be mutually exclusive for now
        CheckedUtil.putWithoutChecking(splitStatuses, stringKey, statusData);
      }
      else
      {
        CheckedUtil.putWithoutChecking(splitErrors, stringKey, error);
      }
    }

    CheckedUtil.putWithoutChecking(splitResponseData, BatchResponse.RESULTS, splitStatuses);
    CheckedUtil.putWithoutChecking(splitResponseData, BatchResponse.ERRORS, splitErrors);

    return new BatchResponse<AnyRecord>(splitResponseData, AnyRecord.class);
  }
}
