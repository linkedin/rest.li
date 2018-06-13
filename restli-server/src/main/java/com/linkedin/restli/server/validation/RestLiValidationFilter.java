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
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.internal.server.response.BatchCreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchGetResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.FinderResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetAllResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetResponseEnvelope;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Rest.li validation filter that validates incoming data automatically,
 * and sends an error response back to the client if the data is invalid.
 *
 * @author Soojung Ha
 */
public class RestLiValidationFilter implements Filter
{
  @Override
  public CompletableFuture<Void> onRequest(final FilterRequestContext requestContext)
  {
    Class<?> resourceClass = requestContext.getFilterResourceModel().getResourceClass();
    ResourceMethod method = requestContext.getMethodType();
    RestLiDataValidator validator = new RestLiDataValidator(resourceClass.getAnnotations(),
                                                            requestContext.getFilterResourceModel().getValueClass(),
                                                            method);
    RestLiRequestData requestData = requestContext.getRequestData();
    if (method == ResourceMethod.CREATE || method == ResourceMethod.UPDATE)
    {
      ValidationResult result = validator.validateInput(requestData.getEntity());
      if (!result.isValid())
      {
        throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString());
      }
    }
    else if (method == ResourceMethod.PARTIAL_UPDATE)
    {
      ValidationResult result = validator.validateInput((PatchRequest<?>) requestData.getEntity());
      if (!result.isValid())
      {
        throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, result.getMessages().toString());
      }
    }
    else if (method == ResourceMethod.BATCH_CREATE)
      {
        StringBuilder sb = new StringBuilder();
        for (RecordTemplate entity : requestData.getBatchEntities())
        {
          ValidationResult result = validator.validateInput(entity);
          if (!result.isValid())
          {
            sb.append(result.getMessages().toString());
          }
        }
        if (sb.length() > 0)
        {
          throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, sb.toString());
        }
      }
      else if (method == ResourceMethod.BATCH_UPDATE || method == ResourceMethod.BATCH_PARTIAL_UPDATE)
        {
          StringBuilder sb = new StringBuilder();
          for (Map.Entry<?, ? extends RecordTemplate> entry : requestData.getBatchKeyEntityMap().entrySet())
          {
            ValidationResult result;
            if (method == ResourceMethod.BATCH_UPDATE)
            {
              result = validator.validateInput(entry.getValue());
            }
            else
            {
              result = validator.validateInput((PatchRequest<?>) entry.getValue());
            }
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
            throw new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, sb.toString());
          }
        }
    return CompletableFuture.completedFuture(null);
  }

  @SuppressWarnings("unchecked")
  public CompletableFuture<Void> onResponse(final FilterRequestContext requestContext,
                                            final FilterResponseContext responseContext)
  {
    Class<?> resourceClass = requestContext.getFilterResourceModel().getResourceClass();
    ResourceMethod method = requestContext.getMethodType();
    RestLiDataValidator
        validator = new RestLiDataValidator(resourceClass.getAnnotations(), requestContext.getFilterResourceModel().getValueClass(), method);
    RestLiResponseData<?> responseData = responseContext.getResponseData();
    MaskTree projectionMask = requestContext.getProjectionMask();

    if (responseData.getResponseEnvelope().isErrorResponse())
    {
      return CompletableFuture.completedFuture(null);
    }
    switch (method)
    {
      case GET:
        validateSingleResponse(validator, ((GetResponseEnvelope) responseData.getResponseEnvelope()).getRecord(), projectionMask);
        break;
      case CREATE:
        if (requestContext.getCustomAnnotations().containsKey("returnEntity"))
        {
          validateSingleResponse(validator, ((CreateResponseEnvelope) responseData.getResponseEnvelope()).getRecord(), projectionMask);
        }
        break;
      case GET_ALL:
        validateCollectionResponse(validator, ((GetAllResponseEnvelope) responseData.getResponseEnvelope()).getCollectionResponse(), projectionMask);
        break;
      case FINDER:
        validateCollectionResponse(validator, ((FinderResponseEnvelope) responseData.getResponseEnvelope()).getCollectionResponse(), projectionMask);
        break;
      case BATCH_GET:
        validateBatchResponse(validator, ((BatchGetResponseEnvelope) responseData.getResponseEnvelope()).getBatchResponseMap(), projectionMask);
        break;
      case BATCH_CREATE:
        if (requestContext.getCustomAnnotations().containsKey("returnEntity"))
        {
          validateCreateCollectionResponse(validator, ((BatchCreateResponseEnvelope) responseData.getResponseEnvelope()).getCreateResponses(), projectionMask);
        }
        break;
    }
    return CompletableFuture.completedFuture(null);
  }

  private void validateSingleResponse(RestLiDataValidator validator, RecordTemplate entity, MaskTree projectionMask)
  {
    ValidationResult result = validator.validateOutput(entity, projectionMask);
    if (!result.isValid())
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, result.getMessages().toString());
    }
  }

  private void validateCollectionResponse(RestLiDataValidator validator, List<? extends RecordTemplate> entities, MaskTree projectionMask)
  {
    StringBuilder sb = new StringBuilder();
    for (RecordTemplate entity : entities)
    {
      ValidationResult result = validator.validateOutput(entity, projectionMask);
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

  private void validateBatchResponse(RestLiDataValidator validator,
                                     Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap,
                                     MaskTree projectionMask)
  {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<?, ? extends BatchResponseEnvelope.BatchResponseEntry> entry : batchResponseMap.entrySet())
    {
      if (entry.getValue().hasException())
      {
        continue;
      }
      ValidationResult result = validator.validateOutput(entry.getValue().getRecord(), projectionMask);
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

  private void validateCreateCollectionResponse(RestLiDataValidator validator,
                                                List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> responses,
                                                MaskTree projectionMask)
  {
    StringBuilder sb = new StringBuilder();
    for (BatchCreateResponseEnvelope.CollectionCreateResponseItem item : responses)
    {
      if (item.isErrorResponse())
      {
        continue;
      }
      ValidationResult
          result = validator.validateOutput(((CreateIdEntityStatus<?, ?>) item.getRecord()).getEntity(), projectionMask);
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

  public CompletableFuture<Void> onError(Throwable t, final FilterRequestContext requestContext,
                                         final FilterResponseContext responseContext)
  {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return future;
  }
}
