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

package com.linkedin.restli.internal.server.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope.BatchResponseEntry;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the base implementation for {@link RestLiResponseBuilder}s for BATCH_UPDATE, BATCH_PARTIAL_UPDATE, and
 * BATCH_DELETE responses.
 *
 * @author Josh Walker
 */
public abstract class BatchResponseBuilder<D extends RestLiResponseData<? extends BatchResponseEnvelope>> implements RestLiResponseBuilder<D>
{
  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  @SuppressWarnings("unchecked")
  public PartialRestResponse buildResponse(RoutingResult routingResult, D responseData)
  {
    Map<Object, UpdateStatus> mergedResults = new HashMap<>();

    final Map<Object, BatchResponseEntry> responses = (Map<Object, BatchResponseEntry>) responseData.getResponseEnvelope().getBatchResponseMap();
    generateResultEntityResponse(routingResult, responses, mergedResults);

    PartialRestResponse.Builder builder = new PartialRestResponse.Builder();
    final ProtocolVersion protocolVersion = routingResult.getContext().getRestliProtocolVersion();

    @SuppressWarnings("unchecked")
    final BatchResponse<AnyRecord> response = toBatchResponse(mergedResults, protocolVersion);
    return builder.entity(response).headers(responseData.getHeaders()).cookies(responseData.getCookies()).build();
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

      UpdateStatus status = entry.getValue().getRecord() instanceof UpdateStatus ?
                              (UpdateStatus) entry.getValue().getRecord() : new UpdateStatus();
      status.setStatus(entry.getValue().getStatus().getCode());
      if (entry.getValue().hasException())
      {
        status.setError(_errorResponseBuilder.buildErrorResponse(entry.getValue().getException()));
      }
      mergedResults.put(entry.getKey(), status);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public D buildRestLiResponseData(RestRequest request,
                                   RoutingResult routingResult,
                                   Object result,
                                   Map<String, String> headers,
                                   List<HttpCookie> cookies)
  {
    @SuppressWarnings({ "unchecked" })
    /* constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchUpdate(java.util.Map)} */
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

    Map<Object, BatchResponseEntry> batchResponseMap = new HashMap<>();
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
        Object finalKey = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(entry.getKey(), routingResult);
        batchResponseMap.put(finalKey, new BatchResponseEntry(entry.getValue().getStatus(), new UpdateStatus()));
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
      Object finalKey = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(entry.getKey(), routingResult);
      batchResponseMap.put(finalKey, new BatchResponseEntry(entry.getValue().getStatus(), entry.getValue()));
    }

    for (Map.Entry<Object, RestLiServiceException> entry : routingResult.getContext().getBatchKeyErrors().entrySet())
    {
      Object finalKey = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(entry.getKey(), routingResult);
      batchResponseMap.put(finalKey, new BatchResponseEntry(entry.getValue().getStatus(), entry.getValue()));
    }

    return buildResponseData(HttpStatus.S_200_OK, batchResponseMap, headers, cookies);
  }

  abstract D buildResponseData(HttpStatus status,
      Map<?, BatchResponseEntry> batchResponseMap,
      Map<String, String> headers, List<HttpCookie> cookies);

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

    return new BatchResponse<>(splitResponseData, AnyRecord.class);
  }
}
