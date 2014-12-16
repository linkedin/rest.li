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

package com.linkedin.restli.client;


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.ResponseImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Keren Jin
 */
// need to be place in same package as BatchRequest, since BatchRequest.getFields() is protected
public class BatchGetRequestUtil
{
  public static <T extends RecordTemplate> Map<String, Object> getBatchQueryParam(List<? extends BatchRequest<T>> requests, boolean batchFields)
  {
    if (requests.size() < 2)
    {
      throw new IllegalArgumentException("Must have at least two requests to batch");
    }

    final BatchRequest<T> firstRequest = requests.get(0);
    final BatchingKey<T,? extends BatchRequest<T>> batchKey = new BatchingKey<T,BatchRequest<T>>(firstRequest, batchFields);
    final Set<Object> ids = new HashSet<Object>();

    // Default to no fields or to first request's fields, depending on batchFields flag
    Set<PathSpec> fields = batchFields ? new HashSet<PathSpec>() : firstRequest.getFields();

    for (BatchRequest<T> request : requests)
    {
      batchKey.validate(request);

      final Set<Object> requestIds = request.getObjectIds();
      final Set<PathSpec> requestFields = request.getFields();

      if (requestIds != null && !requestIds.isEmpty())
      {
        ids.addAll(requestIds);
      }

      if (batchFields)
      {
        if (requestFields == null || requestFields.isEmpty())
        {
          // Need all fields
          fields = null;
        }
        else if (fields != null)
        {
          fields.addAll(requestFields);
        }
      }
    }

    final Map<String, Object> queryParams = getQueryParamsForBatchingKey(firstRequest);

    // add the ids back to the queryParams
    queryParams.put(RestConstants.QUERY_BATCH_IDS_PARAM, ids);

    // add the fields back to the queryParams
    if (fields != null && !fields.isEmpty())
    {
      queryParams.put(RestConstants.FIELDS_PARAM, fields.toArray(new PathSpec[fields.size()]));
    }

    return queryParams;
  }

  /**
   * Creates a map of those queryParams which are required to be the same for all batchable requests.
   *
   * @param request the BatchGetRequest to pull query params from
   * @return Map which contains all query params save for Batch_Ids and Fields
   */
  public static Map<String, Object> getQueryParamsForBatchingKey(BatchRequest<?> request)
  {
    final Map<String, Object> params = new HashMap<String, Object>(request.getQueryParamsObjects());
    params.remove(RestConstants.QUERY_BATCH_IDS_PARAM);
    params.remove(RestConstants.FIELDS_PARAM);
    return params;
  }

  /**
   * Extract the get response for this resource out of an auto-batched batch response.
   * This is pure rest.li logic, and it complements the auto-batching logic in BatchGetRequestBuilder.
   * @throws com.linkedin.r2.RemoteInvocationException if the server returned an error response for this resource,
   * or if it returned neither a result nor an error.
   */
  public static <K, V extends RecordTemplate> Response<V> unbatchKVResponse(Request request,
                                                                            Response<BatchKVResponse<K, V>> batchResponse,
                                                                            K id)
      throws RemoteInvocationException
  {
    final BatchKVResponse<K, V> batchEntity = batchResponse.getEntity();
    final ErrorResponse errorResponse = batchEntity.getErrors().get(id);
    if (errorResponse != null)
    {
      throw new RestLiResponseException(errorResponse);
    }

    final V entityResult = batchEntity.getResults().get(id);
    if (entityResult == null)
    {
      throw new RestLiDecodingException("No result or error for base URI " + request.getBaseUriTemplate() + ", id " + id +
                                             ". Verify that the batchGet endpoint returns response keys that match batchGet request IDs.", null);
    }

    return new ResponseImpl<V>(batchResponse, entityResult);
  }

  /**
   * Extract the get response for this resource out of an auto-batched batch response.
   * This is pure rest.li logic, and it complements the auto-batching logic in BatchGetRequestBuilder.
   * @throws com.linkedin.r2.RemoteInvocationException if the server returned an error response for this resource,
   * or if it returned neither a result nor an error.
   */
  public static <T extends RecordTemplate> Response<T> unbatchResponse(Request request,
                                                                       Response<BatchResponse<T>> batchResponse,
                                                                       Object id)
      throws RemoteInvocationException
  {
    final BatchResponse<T> batchEntity = batchResponse.getEntity();
    final ErrorResponse errorResponse = batchEntity.getErrors().get(id);
    if (errorResponse != null)
    {
      throw new RestLiResponseException(errorResponse);
    }

    final T entityResult = batchEntity.getResults().get(id);
    if (entityResult == null)
    {
      throw new RestLiDecodingException("No result or error for base URI " + request.getBaseUriTemplate() + ", id " + id +
                                             ". Verify that the batchGet endpoint returns response keys that match batchGet request IDs.", null);
    }

    return new ResponseImpl<T>(batchResponse, entityResult);
  }
}
