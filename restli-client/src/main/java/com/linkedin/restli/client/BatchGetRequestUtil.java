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
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Keren Jin
 */
// need to be place in same package as BatchRequest, since BatchRequest.getFields() is protected
class BatchGetRequestUtil
{
  public static <T> Map<String, Object> getBatchQueryParam(List<? extends BatchRequest<T>> requests, boolean batchFields)
  {
    if (requests.size() < 2)
    {
      throw new IllegalArgumentException("Must have at least two requests to batch");
    }

    final BatchRequest<T> firstRequest = requests.get(0);
    final ResourceSpec firstResourceSpec = firstRequest.getResourceSpec();
    final String firstRequestBaseUriTemplate = firstRequest.getBaseUriTemplate();
    final Map<String, Object> firstRequestPathKeys = firstRequest.getPathKeys();
    final Set<PathSpec> firstFields = firstRequest.getFields();
    final Set<Object> ids = new HashSet<Object>();

    // Default to no fields or to first request's fields, depending on batchFields flag
    Set<PathSpec> fields = batchFields ? new HashSet<PathSpec>() : firstFields;

    // Defensive shallow copy
    final Map<String, Object> firstQueryParams = new HashMap<String, Object>(firstRequest.getQueryParamsObjects());

    firstQueryParams.remove(RestConstants.QUERY_BATCH_IDS_PARAM);
    firstQueryParams.remove(RestConstants.FIELDS_PARAM);

    for (BatchRequest<T> request : requests)
    {
      final String currentRequestBaseUriTemplate = request.getBaseUriTemplate();
      final Map<String, Object> currentRequestPathKeys = request.getPathKeys();
      if (!currentRequestBaseUriTemplate.equals(firstRequestBaseUriTemplate) ||
        !currentRequestPathKeys.equals(firstRequestPathKeys))
      {
        throw new IllegalArgumentException("Requests must have same base URI template and path keys to batch");
      }

      if (!request.getResourceSpec().equals(firstResourceSpec))
      {
        throw new IllegalArgumentException("Requests must be for the same resource to batch");
      }

      if (!request.getRequestOptions().equals(firstRequest.getRequestOptions()))
      {
        throw new IllegalArgumentException("Requests must have the same RestliRequestOptions to batch!");
      }

      final Set<Object> requestIds = request.getObjectIds();
      final Set<PathSpec> requestFields = request.getFields();
      // Defensive shallow copy
      final Map<String, Object> queryParams = new HashMap<String, Object>(request.getQueryParamsObjects());

      queryParams.remove(RestConstants.FIELDS_PARAM);
      queryParams.remove(RestConstants.QUERY_BATCH_IDS_PARAM);

      // Enforce uniformity of query params excluding ids and fields
      if (!firstQueryParams.equals(queryParams))
      {
        throw new IllegalArgumentException("Requests must have same parameters to batch");
      }

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
      else if (!requestFields.equals(firstFields))
      {
        throw new IllegalArgumentException("Requests must have same fields to batch");
      }
    }

    firstQueryParams.put(RestConstants.QUERY_BATCH_IDS_PARAM, ids);

    if (fields != null && !fields.isEmpty())
    {
      firstQueryParams.put(RestConstants.FIELDS_PARAM, fields.toArray(new PathSpec[fields.size()]));
    }

    return firstQueryParams;
  }
}
