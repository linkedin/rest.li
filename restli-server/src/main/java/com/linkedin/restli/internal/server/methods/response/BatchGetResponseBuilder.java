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
import com.linkedin.data.template.SetMode;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope.BatchResponseEntry;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class BatchGetResponseBuilder implements RestLiResponseBuilder
{
  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchGetResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  @SuppressWarnings("unchecked")
  public PartialRestResponse buildResponse(RoutingResult routingResult, RestLiResponseEnvelope responseData)
  {
    final Map<Object, BatchResponseEntry> responses = (Map<Object, BatchResponseEntry>) responseData.getBatchResponseEnvelope().getBatchResponseMap();

    // Build the EntityResponse for each key from the merged map with mask from routingResult.
    Map<Object, EntityResponse<RecordTemplate>> entityBatchResponse = buildEntityResponse(routingResult, responses);

    PartialRestResponse.Builder builder = new PartialRestResponse.Builder();
    final ProtocolVersion protocolVersion = ((ServerResourceContext) routingResult.getContext()).getRestliProtocolVersion();

    @SuppressWarnings("unchecked")
    final BatchResponse<AnyRecord> response = toBatchResponse(entityBatchResponse, protocolVersion);
    builder.entity(response);
    return builder.headers(responseData.getHeaders()).build();
  }

  // Transforms results into the corresponding
  // typed entity responses with status and ErrorResponse populated.
  private Map<Object, EntityResponse<RecordTemplate>> buildEntityResponse(RoutingResult routingResult,
                                                                          Map<Object, BatchResponseEntry> mergedResponse)
  {
    Map<Object, EntityResponse<RecordTemplate>> entityBatchResponse = new HashMap<Object, EntityResponse<RecordTemplate>>(mergedResponse.size());

    for (Map.Entry<Object, BatchResponseEntry> entry : mergedResponse.entrySet())
    {
      @SuppressWarnings("unchecked")
      final EntityResponse<RecordTemplate> entityResponse = entry.getValue().hasException() ?
                                                              createEntityResponse(null, routingResult) :
                                                              createEntityResponse(entry.getValue().getRecord(), routingResult);

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

  private static EntityResponse<RecordTemplate> createEntityResponse(RecordTemplate entityTemplate, RoutingResult routingResult)
  {
    final EntityResponse<RecordTemplate> entityResponse;
    if (entityTemplate == null)
    {
      entityResponse = new EntityResponse<RecordTemplate>(null);
    }
    else
    {
      @SuppressWarnings("unchecked")
      final Class<RecordTemplate> entityClass = (Class<RecordTemplate>) entityTemplate.getClass();
      entityResponse = new EntityResponse<RecordTemplate>(entityClass);

      final DataMap projectedData = RestUtils.projectFields(entityTemplate.data(),
                                                            routingResult.getContext().getProjectionMode(),
                                                            routingResult.getContext().getProjectionMask());
      CheckedUtil.putWithoutChecking(entityResponse.data(), EntityResponse.ENTITY, projectedData);
    }

    return entityResponse;
  }

  @Override
  public RestLiResponseEnvelope buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object result, Map<String, String> headers)
  {
    @SuppressWarnings({ "unchecked" })
    /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
    final Map<Object, RecordTemplate> entities = (Map<Object, RecordTemplate>) result;
    Map<Object, HttpStatus> statuses = Collections.emptyMap();
    Map<Object, RestLiServiceException> serviceErrors = Collections.emptyMap();

    if (result instanceof BatchResult)
    {
      @SuppressWarnings({ "unchecked" })
      /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
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

    Map<Object, BatchResponseEntry> batchResult = new HashMap<Object, BatchResponseEntry>(entities.size() + serviceErrors.size());
    for (Map.Entry<Object, RecordTemplate> entity : entities.entrySet())
    {
      if (entity.getKey() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key inside of a Map returned by the resource method: " + routingResult
                .getResourceMethod());
      }

      batchResult.put(entity.getKey(), new BatchResponseEntry(statuses.get(entity.getKey()), entity.getValue()));
    }

    for (Map.Entry<Object, RestLiServiceException> entity : serviceErrors.entrySet())
    {
      if (entity.getKey() == null || entity.getValue() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key inside of a Map returned by the resource method: " + routingResult
                .getResourceMethod());
      }
      batchResult.put(entity.getKey(), new BatchResponseEntry(statuses.get(entity.getKey()), entity.getValue()));
    }

    final Map<Object, RestLiServiceException> contextErrors = ((ServerResourceContext) routingResult.getContext()).getBatchKeyErrors();
    for (Map.Entry<Object, RestLiServiceException> entry : contextErrors.entrySet())
    {
      batchResult.put(entry.getKey(), new BatchResponseEntry(statuses.get(entry.getKey()), entry.getValue()));
    }

    return new BatchResponseEnvelope(batchResult, headers);
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

    return new BatchResponse<AnyRecord>(splitResponseData, AnyRecord.class);
  }
}
