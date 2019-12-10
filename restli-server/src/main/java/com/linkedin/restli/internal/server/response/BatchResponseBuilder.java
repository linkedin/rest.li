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
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope.BatchResponseEntry;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.ResourceContext;
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
  public RestLiResponse buildResponse(RoutingResult routingResult, D responseData)
  {
    // extract BatchResponseEntry objects from the response envelope
    final Map<Object, BatchResponseEntry> responses = (Map<Object, BatchResponseEntry>) responseData.getResponseEnvelope().getBatchResponseMap();

    // map BatchResponseEntry objects to UpdateStatus objects
    Map<Object, UpdateStatus> mergedResults = generateResultEntityResponse(routingResult, responses);

    // split the merged UpdateStatus map to the properly formatted over-the-wire data map
    final ProtocolVersion protocolVersion = routingResult.getContext().getRestliProtocolVersion();
    final BatchResponse<AnyRecord> response = toBatchResponse(mergedResults, protocolVersion);

    RestLiResponse.Builder builder = new RestLiResponse.Builder();
    return builder.entity(response)
                  .headers(responseData.getHeaders())
                  .cookies(responseData.getCookies())
                  .build();
  }

  /**
   * {@inheritDoc}
   *
   * @param result The result of a Rest.li BATCH_UPDATE, BATCH_PARTIAL_UPDATE, or BATCH_DELETE method. It is a
   *               {@link BatchUpdateResult} object.
   */
  @SuppressWarnings("unchecked")
  @Override
  public D buildRestLiResponseData(Request request,
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

    TimingContextUtil.beginTiming(routingResult.getContext().getRawRequestContext(),
        FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

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

        UpdateStatus updateStatus = buildUpdateStatus(routingResult.getContext(), entry.getValue());

        batchResponseMap.put(finalKey, new BatchResponseEntry(entry.getValue().getStatus(), updateStatus));
      }
    }

    TimingContextUtil.endTiming(routingResult.getContext().getRawRequestContext(),
        FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

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

  /**
   * Defines how to build an {@link UpdateStatus} (or subclass) for a given {@link UpdateResponse} (or subclass).
   * Subclass response builders can override this with their own implementation.
   * @param resourceContext current resource context
   * @param updateResponse update response returned by the resource method
   * @return update status for the given update response
   */
  protected UpdateStatus buildUpdateStatus(ResourceContext resourceContext, UpdateResponse updateResponse)
  {
    return new UpdateStatus();
  }

  /**
   * Helper method for {@link #buildResponse} that produces a merged mapping of {@link UpdateStatus} objects given a
   * mapping of {@link BatchResponseEntry} objects.
   * @param routingResult
   * @param responses
   * @return merged update status map
   */
  private Map<Object, UpdateStatus> generateResultEntityResponse(RoutingResult routingResult, Map<Object, BatchResponseEntry> responses)
  {
    Map<Object, UpdateStatus> mergedResults = new HashMap<>();
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
    return mergedResults;
  }

  /**
   * Helper method for {@link #buildResponse} that splits the merged {@link UpdateStatus} map into a "statuses" map and
   * an "errors" map, uses this to construct a data map that properly matches the over-the-wire format, and wraps it
   * in a {@link BatchResponse}.
   * @param statuses map of {@link UpdateStatus} objects
   * @param protocolVersion
   * @param <K> key type
   * @return batch response
   */
  private static <K> BatchResponse<AnyRecord> toBatchResponse(Map<K, UpdateStatus> statuses,
                                                              ProtocolVersion protocolVersion)
  {
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

    final DataMap splitResponseData = new DataMap();
    CheckedUtil.putWithoutChecking(splitResponseData, BatchResponse.RESULTS, splitStatuses);
    CheckedUtil.putWithoutChecking(splitResponseData, BatchResponse.ERRORS, splitErrors);

    return new BatchResponse<>(splitResponseData, AnyRecord.class);
  }
}
