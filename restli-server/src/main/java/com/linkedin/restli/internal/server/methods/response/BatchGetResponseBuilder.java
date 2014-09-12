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
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class BatchGetResponseBuilder implements RestLiResponseBuilder
{
  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchGetResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
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
        toBatchResponse((Map<Object, EntityResponse<RecordTemplate>>) responseData.getBatchResponseMap(),
                        protocolVersion);
    builder.entity(response);
    return builder.headers(responseData.getHeaders()).build();
  }

  @Override
  public AugmentedRestLiResponseData buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object result, Map<String, String> headers)
  {
    @SuppressWarnings({ "unchecked" })
    /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
    final Map<Object, RecordTemplate> entities = (Map<Object, RecordTemplate>) result;
    Map<Object, HttpStatus> statuses = Collections.emptyMap();
    Map<Object, RestLiServiceException> serviceErrors = Collections.emptyMap();

    //Verify that there is no null key inside any of the maps. If so, this is a developer error.
    if (entities.containsKey(null))
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. Null key inside of the Map returned by the resource method: " + routingResult
              .getResourceMethod());
    }

    if (result instanceof BatchResult)
    {
      @SuppressWarnings({ "unchecked" })
      /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
      final BatchResult<Object, RecordTemplate> batchResult = (BatchResult<Object, RecordTemplate>) result;
      statuses = batchResult.getStatuses();
      //We only need to check the statuses map here inside of BatchResult, otherwise it would be an empty map.
      if (statuses.containsKey(null))
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null key inside of the status map returned by the resource method: " + routingResult
                .getResourceMethod());
      }
      //Note that we don't have to check the service errors map for nulls, because its taken care
      //of in populateErrors below.
      serviceErrors = batchResult.getErrors();
    }

    final ServerResourceContext context = (ServerResourceContext) routingResult.getContext();
    final Map<Object, ErrorResponse> errors =
        BatchResponseUtil.populateErrors(serviceErrors, routingResult, _errorResponseBuilder);

    final Set<Object> mergedKeys = new HashSet<Object>(entities.keySet());
    mergedKeys.addAll(statuses.keySet());
    mergedKeys.addAll(errors.keySet());

    final Map<Object, EntityResponse<RecordTemplate>> results =
        new HashMap<Object, EntityResponse<RecordTemplate>>((int) Math.ceil(mergedKeys.size() / 0.75));

    for (Object key : mergedKeys)
    {
      final EntityResponse<RecordTemplate> entityResponse;

      final RecordTemplate entityTemplate = entities.get(key);
      if (entityTemplate == null)
      {
        entityResponse = new EntityResponse<RecordTemplate>(null);
      }
      else
      {
        @SuppressWarnings("unchecked")
        final Class<RecordTemplate> entityClass = (Class<RecordTemplate>) entityTemplate.getClass();
        entityResponse = new EntityResponse<RecordTemplate>(entityClass);

        final DataMap projectedData = RestUtils.projectFields(entityTemplate.data(), context);
        CheckedUtil.putWithoutChecking(entityResponse.data(), EntityResponse.ENTITY, projectedData);
      }

      entityResponse.setStatus(statuses.get(key), SetMode.IGNORE_NULL);
      entityResponse.setError(errors.get(key), SetMode.IGNORE_NULL);
      results.put(key, entityResponse);
    }
    return new AugmentedRestLiResponseData.Builder(routingResult.getResourceMethod().getMethodType()).batchKeyEntityMap(results)
                                                                                                     .headers(headers)
                                                                                                     .build();
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
