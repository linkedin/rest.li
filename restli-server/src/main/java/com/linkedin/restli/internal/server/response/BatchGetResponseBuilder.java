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
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope.BatchResponseEntry;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BatchGetResponseBuilder implements RestLiResponseBuilder<RestLiResponseData<BatchGetResponseEnvelope>>
{
  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchGetResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  @SuppressWarnings("unchecked")
  public RestLiResponse buildResponse(RoutingResult routingResult, RestLiResponseData<BatchGetResponseEnvelope> responseData)
  {
    final Map<Object, BatchResponseEntry> responses = (Map<Object, BatchResponseEntry>) responseData.getResponseEnvelope().getBatchResponseMap();

    // Build the EntityResponse for each key from the merged map with mask from routingResult.
    Map<Object, EntityResponse<RecordTemplate>> entityBatchResponse = buildEntityResponse(routingResult, responses);

    RestLiResponse.Builder builder = new RestLiResponse.Builder();
    final ProtocolVersion protocolVersion = routingResult.getContext().getRestliProtocolVersion();

    @SuppressWarnings("unchecked")
    final BatchResponse<AnyRecord> response = toBatchResponse(entityBatchResponse, protocolVersion);
    builder.entity(response);
    return builder.headers(responseData.getHeaders()).cookies(responseData.getCookies()).build();
  }

  // Transforms results into the corresponding
  // typed entity responses with status and ErrorResponse populated.
  private Map<Object, EntityResponse<RecordTemplate>> buildEntityResponse(RoutingResult routingResult,
                                                                          Map<Object, BatchResponseEntry> mergedResponse)
  {
    Map<Object, EntityResponse<RecordTemplate>> entityBatchResponse = new HashMap<>(mergedResponse.size());

    for (Map.Entry<Object, BatchResponseEntry> entry : mergedResponse.entrySet())
    {
      @SuppressWarnings("unchecked")
      final EntityResponse<RecordTemplate> entityResponse = entry.getValue().hasException() ?
                                                              createEntityResponse(null) :
                                                              createEntityResponse(entry.getValue().getRecord());

      if (entry.getKey() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null errors Map found inside of the result returned by the resource method: "
                + routingResult.getResourceMethod());
      }

      entityResponse.setStatus(entry.getValue().getStatus(), SetMode.IGNORE_NULL);
      if (entry.getValue().hasException())
      {
        entityResponse.setError(_errorResponseBuilder.buildErrorResponse(entry.getValue().getException()));
      }
      entityBatchResponse.put(entry.getKey(), entityResponse);
    }

    return entityBatchResponse;
  }

  private static EntityResponse<RecordTemplate> createEntityResponse(RecordTemplate entityTemplate)
  {
    final EntityResponse<RecordTemplate> entityResponse;
    if (entityTemplate == null)
    {
      entityResponse = new EntityResponse<>(null);
    }
    else
    {
      @SuppressWarnings("unchecked")
      final Class<RecordTemplate> entityClass = (Class<RecordTemplate>) entityTemplate.getClass();
      entityResponse = new EntityResponse<>(entityClass);
      CheckedUtil.putWithoutChecking(entityResponse.data(), EntityResponse.ENTITY, entityTemplate.data());
    }

    return entityResponse;
  }

  /**
   * {@inheritDoc}
   *
   * @param result The result of the Rest.li BATCH_GET method. It is <code>Map</code> of the entities to return keyed by
   *               the IDs of the entities. Optionally, it may be a {@link BatchResult} object that contains more
   *               information.
   */
  @Override
  public RestLiResponseData<BatchGetResponseEnvelope> buildRestLiResponseData(Request request,
                                                      RoutingResult routingResult,
                                                      Object result,
                                                      Map<String, String> headers,
                                                      List<HttpCookie> cookies)
  {
    @SuppressWarnings({ "unchecked" })
    /* constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
    final Map<Object, RecordTemplate> entities = (Map<Object, RecordTemplate>) result;
    Map<Object, HttpStatus> statuses = Collections.emptyMap();
    Map<Object, RestLiServiceException> serviceErrors = Collections.emptyMap();

    if (result instanceof BatchResult)
    {
      @SuppressWarnings({ "unchecked" })
      /* constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
      final BatchResult<Object, RecordTemplate> batchResult = (BatchResult<Object, RecordTemplate>) result;
      statuses = batchResult.getStatuses();
      serviceErrors = batchResult.getErrors();
    }

    try
    {
      if (statuses.containsKey(null))
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key inside of a Map returned by the resource method: " + routingResult
                .getResourceMethod());
      }
    }
    catch (NullPointerException e)
    {
      // Some map implementations will throw an NPE if they do not support null keys.
      // In this case it is OK to swallow this exception and proceed.
    }

    TimingContextUtil.beginTiming(routingResult.getContext().getRawRequestContext(),
        FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

    Map<Object, BatchResponseEntry> batchResult = new HashMap<>(entities.size() + serviceErrors.size());
    for (Map.Entry<Object, RecordTemplate> entity : entities.entrySet())
    {
      if (entity.getKey() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key inside of a Map returned by the resource method: " + routingResult
                .getResourceMethod());
      }
      Object finalKey = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(entity.getKey(), routingResult);

      final DataMap projectedData = RestUtils.projectFields(entity.getValue().data(), routingResult.getContext());

      AnyRecord anyRecord = new AnyRecord(projectedData);
      batchResult.put(finalKey, new BatchResponseEntry(statuses.get(entity.getKey()), anyRecord));
    }

    TimingContextUtil.endTiming(routingResult.getContext().getRawRequestContext(),
        FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

    for (Map.Entry<Object, RestLiServiceException> entity : serviceErrors.entrySet())
    {
      if (entity.getKey() == null || entity.getValue() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key inside of a Map returned by the resource method: " + routingResult
                .getResourceMethod());
      }
      Object finalKey = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(entity.getKey(), routingResult);
      batchResult.put(finalKey, new BatchResponseEntry(statuses.get(entity.getKey()), entity.getValue()));
    }

    final Map<Object, RestLiServiceException> contextErrors = routingResult.getContext().getBatchKeyErrors();
    for (Map.Entry<Object, RestLiServiceException> entry : contextErrors.entrySet())
    {
      Object finalKey = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(entry.getKey(), routingResult);
      batchResult.put(finalKey, new BatchResponseEntry(statuses.get(entry.getKey()), entry.getValue()));
    }

    return new RestLiResponseDataImpl<>(new BatchGetResponseEnvelope(HttpStatus.S_200_OK, batchResult), headers, cookies);
  }

  private static <K, V extends RecordTemplate> BatchResponse<AnyRecord> toBatchResponse(Map<K, EntityResponse<V>> entities,
                                                                                        ProtocolVersion protocolVersion)
  {
    final DataMap splitResponseData = new DataMap();
    final DataMap splitResults = new DataMap();
    final DataMap splitStatuses = new DataMap();
    final DataMap splitErrors = new DataMap();

    for (Map.Entry<K, EntityResponse<V>> resultEntry : entities.entrySet())
    {
      final DataMap entityResponseData = resultEntry.getValue().data();
      final String stringKey = URIParamUtils.encodeKeyForBody(resultEntry.getKey(), false, protocolVersion);

      final DataMap entityData = entityResponseData.getDataMap(EntityResponse.ENTITY);
      if (entityData != null)
      {
        CheckedUtil.putWithoutChecking(splitResults, stringKey, entityData);
      }

      final Integer status = entityResponseData.getInteger(EntityResponse.STATUS);
      if (status != null)
      {
        CheckedUtil.putWithoutChecking(splitStatuses, stringKey, status);
      }

      final DataMap error = entityResponseData.getDataMap(EntityResponse.ERROR);
      if (error != null)
      {
        CheckedUtil.putWithoutChecking(splitErrors, stringKey, error);
      }
    }

    CheckedUtil.putWithoutChecking(splitResponseData, BatchResponse.RESULTS, splitResults);
    CheckedUtil.putWithoutChecking(splitResponseData, BatchResponse.STATUSES, splitStatuses);
    CheckedUtil.putWithoutChecking(splitResponseData, BatchResponse.ERRORS, splitErrors);

    return new BatchResponse<>(splitResponseData, AnyRecord.class);
  }
}
