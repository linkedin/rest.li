/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.server.validation;


import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateCollectionResponseEnvelope;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.NextResponseFilter;
import com.linkedin.restli.server.filter.ResponseFilter;

import java.util.List;
import java.util.Map;


/**
 * Rest.li validation filter that validates outgoing data automatically,
 * and sends an error response back to the client if the data is invalid.
 *
 * @author Soojung Ha
 */
public class RestLiOutputValidationFilter implements ResponseFilter
{
  @Override
  public void onResponse(final FilterRequestContext requestContext, final FilterResponseContext responseContext, final NextResponseFilter nextResponseFilter)
  {
    Class<?> resourceClass = requestContext.getFilterResourceModel().getResourceClass();
    ResourceMethod method = requestContext.getMethodType();
    RestLiDataValidator validator = new RestLiDataValidator(resourceClass.getAnnotations(), requestContext.getFilterResourceModel().getValueClass(), method);
    RestLiResponseData responseData = responseContext.getResponseData();
    if (responseData.isErrorResponse())
    {
      nextResponseFilter.onResponse(requestContext, responseContext);
      return;
    }
    switch (method)
    {
      case GET:
        validateSingleResponse(validator, responseData.getRecordResponseEnvelope().getRecord());
        break;
      case CREATE:
        if (requestContext.getCustomAnnotations().containsKey("returnEntity"))
        {
          validateSingleResponse(validator, responseData.getRecordResponseEnvelope().getRecord());
        }
        break;
      case GET_ALL:
      case FINDER:
        validateCollectionResponse(validator, responseData.getCollectionResponseEnvelope().getCollectionResponse());
        break;
      case BATCH_GET:
        validateBatchResponse(validator, responseData.getBatchResponseEnvelope().getBatchResponseMap());
        break;
      case BATCH_CREATE:
        if (requestContext.getCustomAnnotations().containsKey("returnEntity"))
        {
          validateCreateCollectionResponse(validator, responseData.getCreateCollectionResponseEnvelope().getCreateResponses());
        }
        break;
    }
    nextResponseFilter.onResponse(requestContext, responseContext);
  }

  private void validateSingleResponse(RestLiDataValidator validator, RecordTemplate entity)
  {
    ValidationResult result = validator.validateOutput(entity);
    if (!result.isValid())
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, result.getMessages().toString());
    }
  }

  private void validateCollectionResponse(RestLiDataValidator validator, List<? extends RecordTemplate> entities)
  {
    StringBuilder sb = new StringBuilder();
    for (RecordTemplate entity : entities)
    {
      ValidationResult result = validator.validateOutput(entity);
      if (!result.isValid())
      {
        sb.append(result.getMessages().toString());
      }
    }
    if (sb.length() > 0)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, sb.toString());
    }
  }

  private void validateBatchResponse(RestLiDataValidator validator, Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<?, ? extends BatchResponseEnvelope.BatchResponseEntry> entry : batchResponseMap.entrySet())
    {
      if (entry.getValue().hasException())
      {
        continue;
      }
      ValidationResult result = validator.validateOutput(entry.getValue().getRecord());
      if (!result.isValid())
      {
        sb.append("Key: ");
        sb.append(entry.getKey());
        sb.append(", ");
        sb.append(result.getMessages().toString());
      }
    }
    if (sb.length() > 0)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, sb.toString());
    }
  }

  private void validateCreateCollectionResponse(RestLiDataValidator validator, List<CreateCollectionResponseEnvelope.CollectionCreateResponseItem> responses)
  {
    StringBuilder sb = new StringBuilder();
    for (CreateCollectionResponseEnvelope.CollectionCreateResponseItem item : responses)
    {
      if (item.isErrorResponse())
      {
        continue;
      }
      ValidationResult result = validator.validateOutput(((CreateIdEntityStatus)item.getRecord()).getEntity());
      if (!result.isValid())
      {
        sb.append(result.getMessages().toString());
      }
    }
    if (sb.length() > 0)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, sb.toString());
    }
  }
}
