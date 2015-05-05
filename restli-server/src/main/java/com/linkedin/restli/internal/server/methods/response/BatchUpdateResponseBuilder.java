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
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope.BatchResponseEntry;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;

import java.util.HashMap;
import java.util.Map;

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
  @SuppressWarnings("unchecked")
  public PartialRestResponse buildResponse(RoutingResult routingResult, RestLiResponseEnvelope responseData)
  {
    Map<Object, UpdateStatus> mergedResults = new HashMap<Object, UpdateStatus>();

    final Map<Object, BatchResponseEntry> responses = (Map<Object, BatchResponseEntry>) responseData.getBatchResponseEnvelope().getBatchResponseMap();
    generateResultEntityResponse(routingResult, responses, mergedResults);

    PartialRestResponse.Builder builder = new PartialRestResponse.Builder();
    final ProtocolVersion protocolVersion =
        ((ServerResourceContext) routingResult.getContext()).getRestliProtocolVersion();

    @SuppressWarnings("unchecked")
    final BatchResponse<AnyRecord> response = toBatchResponse(mergedResults, protocolVersion);
    return builder.entity(response).headers(responseData.getHeaders()).build();
  }

  // Updates the merged results with context errors and build map of UpdateStatus.
  private void generateResultEntityResponse(RoutingResult routingResult, Map<Object, BatchResponseEntry> responses , Map<Object, UpdateStatus> mergedResults)
  {
    for (Map.Entry<?, BatchResponseEntry> entry : responses.entrySet())
    {
      if (entry.getKey() == null || entry.getValue() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null errors Map found inside of the result returned by the resource method: "
                + routingResult.getResourceMethod());
      }

      UpdateStatus status = new UpdateStatus();
      status.setStatus(entry.getValue().getStatus().getCode());
      if (entry.getValue().hasException())
      {
        status.setError(_errorResponseBuilder.buildErrorResponse(entry.getValue().getException()));
      }
      mergedResults.put(entry.getKey(), status);
    }
  }

  @Override
  public RestLiResponseEnvelope buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
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
    if (serviceErrors == null)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. Null errors Map found inside of the BatchUpdateResult returned by the resource method: "
              + routingResult.getResourceMethod());
    }

    Map<Object, BatchResponseEntry> batchResponseMap = new HashMap<Object, BatchResponseEntry>();
    for (Map.Entry<Object, UpdateResponse> entry : results.entrySet())
    {
      if (entry.getKey() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key inside of the Map returned inside of the BatchUpdateResult returned by the resource method: "
                + routingResult.getResourceMethod());
      }

      if (!serviceErrors.containsKey(entry.getKey()))
      {
        batchResponseMap.put(entry.getKey(), new BatchResponseEntry(entry.getValue().getStatus(), (RecordTemplate) null));
      }
    }

    for (Map.Entry<Object, RestLiServiceException> entry : serviceErrors.entrySet())
    {
      if (entry.getKey() == null || entry.getValue() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key or value inside of the Map returned inside of the BatchUpdateResult returned by the resource method: "
          + routingResult.getResourceMethod());
      }
      batchResponseMap.put(entry.getKey(), new BatchResponseEntry(entry.getValue().getStatus(), entry.getValue()));
    }

    for (Map.Entry<Object, RestLiServiceException> entry : ((ServerResourceContext) routingResult.getContext()).getBatchKeyErrors().entrySet())
    {
      batchResponseMap.put(entry.getKey(), new BatchResponseEntry(entry.getValue().getStatus(), entry.getValue()));
    }

    return new BatchResponseEnvelope(batchResponseMap, headers);
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
