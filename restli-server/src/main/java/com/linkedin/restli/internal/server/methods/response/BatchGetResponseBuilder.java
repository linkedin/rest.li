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
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.RestLiServiceException;

import java.io.IOException;
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
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object object,
                                           final Map<String, String> headers)
      throws IOException
  {
    @SuppressWarnings({ "unchecked" })
    /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
    final Map<Object, RecordTemplate> entities = (Map<Object, RecordTemplate>) object;
    Map<Object, HttpStatus> statuses = Collections.emptyMap();
    Map<Object, RestLiServiceException> serviceErrors = Collections.emptyMap();

    if (object instanceof BatchResult)
    {
      @SuppressWarnings({"unchecked"})
      /** constrained by signature of {@link com.linkedin.restli.server.resources.CollectionResource#batchGet(java.util.Set)} */
      final BatchResult<Object, RecordTemplate> batchResult = (BatchResult<Object, RecordTemplate>) object;
      statuses = batchResult.getStatuses();
      serviceErrors = batchResult.getErrors();
    }

    final ServerResourceContext context = (ServerResourceContext) routingResult.getContext();
    final ProtocolVersion protocolVersion = context.getRestliProtocolVersion();

    final Map<Object, ErrorResponse> errors = BatchResponseUtil.populateErrors(serviceErrors, context, _errorResponseBuilder);

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
        entityResponse.setEntity(DataTemplateUtil.wrap(projectedData, entityClass));
      }

      entityResponse.setStatus(statuses.get(key), SetMode.IGNORE_NULL);
      entityResponse.setError(errors.get(key), SetMode.IGNORE_NULL);
      results.put(key, entityResponse);
    }

    // filter

    final BatchResponse<AnyRecord> response = toBatchResponse(results, protocolVersion);
    return new PartialRestResponse.Builder().entity(response).headers(headers).build();
  }

  private static <K, V extends RecordTemplate> BatchResponse<AnyRecord> toBatchResponse(Map<K, EntityResponse<V>> entities, ProtocolVersion protocolVersion)
  {
    final DataMap splitResponseData = new DataMap(3);
    final DataMap splitResults = new DataMap();
    final DataMap splitStatuses = new DataMap();
    final DataMap splitErrors = new DataMap();

    for (Map.Entry<K, EntityResponse<V>> resultEntry : entities.entrySet())
    {
      final EntityResponse<V> entityResponse = resultEntry.getValue();
      final String stringKey = URIParamUtils.encodeKeyForBody(resultEntry.getKey(), false, protocolVersion);

      final V entity = entityResponse.getEntity();
      if (entity != null)
      {
        splitResults.put(stringKey, entity.data());
      }

      final HttpStatus status = entityResponse.getStatus();
      if (status != null)
      {
        splitStatuses.put(stringKey, status.getCode());
      }

      final ErrorResponse error = entityResponse.getError();
      if (error != null)
      {
        splitErrors.put(stringKey, error.data());
      }
    }

    splitResponseData.put(BatchResponse.RESULTS, splitResults);
    splitResponseData.put(BatchResponse.STATUSES, splitStatuses);
    splitResponseData.put(BatchResponse.ERRORS, splitErrors);

    return new BatchResponse<AnyRecord>(splitResponseData, AnyRecord.class);
  }
}
