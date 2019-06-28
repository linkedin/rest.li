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

import com.linkedin.data.DataMap;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.common.util.ProjectionMaskApplier;
import com.linkedin.restli.common.util.ProjectionMaskApplier.InvalidProjectionException;
import com.linkedin.restli.common.validation.RestLiDataSchemaDataValidator;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.response.BatchCreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchFinderResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchGetResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchPartialUpdateResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.FinderResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetAllResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetResponseEnvelope;
import com.linkedin.restli.internal.server.response.PartialUpdateResponseEnvelope;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.util.UnstructuredDataUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * Rest.li validation filter that automatically validates incoming and outgoing data,
 * and sends an error response back to the client if the data is invalid.
 *
 * @author Soojung Ha
 */
public class RestLiValidationFilter implements Filter
{
  // The key we'll use to store the validating schema in the filter scratchpad
  private static final String VALIDATING_SCHEMA_KEY = "validatingSchema";

  private static final String TEMPLATE_RUNTIME_EXCEPTION_MESSAGE = "Could not find schema for entity during validation";

  private final Collection<String> _nonSchemaFieldsToAllowInProjectionMask;

  // ValidationErrorHandler interface allows applications to customize the service error code,
  // error message and error details.
  private final ValidationErrorHandler _validationErrorHandler;

  public RestLiValidationFilter()
  {
    this(Collections.emptyList());
  }

  /**
   * Constructs {@link RestLiValidationFilter}
   *
   * @param nonSchemaFieldsToAllowInProjectionMask field names to allow in the projection mask
   *                                               even if the field is not present in the schema.
   */
  public RestLiValidationFilter(Collection<String> nonSchemaFieldsToAllowInProjectionMask)
  {
    this(nonSchemaFieldsToAllowInProjectionMask, null);
  }

  /**
   * Constructs {@link RestLiValidationFilter}
   *
   * @param nonSchemaFieldsToAllowInProjectionMask field names to allow in the projection mask
   *                                               even if the field is not present in the schema.
   * @param errorHandler {@link ValidationErrorHandler} interface allows applications to customize the service error code,
   *                                                   error message and error details.
   */
  public RestLiValidationFilter(Collection<String> nonSchemaFieldsToAllowInProjectionMask,
      ValidationErrorHandler errorHandler)
  {
    _nonSchemaFieldsToAllowInProjectionMask = nonSchemaFieldsToAllowInProjectionMask;
    _validationErrorHandler = errorHandler;
  }

  @Override
  public CompletableFuture<Void> onRequest(final FilterRequestContext requestContext)
  {
    // If the request requires validation on the response, build the validating schema now so that invalid projections
    // are spotted early
    if (shouldValidateOnResponse(requestContext))
    {
      MaskTree projectionMask = requestContext.getProjectionMask();
      if (projectionMask != null)
      {
        try
        {
          // Value class from resource model is the only source of truth for record schema.
          // Schema from the record template itself should not be used.
          DataSchema originalSchema = DataTemplateUtil.getSchema(requestContext.getFilterResourceModel().getValueClass());

          DataSchema validatingSchema = constructValidatingSchema(requestContext, originalSchema,
              projectionMask.getDataMap(), _nonSchemaFieldsToAllowInProjectionMask);

          // Put validating schema in scratchpad for use in onResponse
          requestContext.getFilterScratchpad().put(VALIDATING_SCHEMA_KEY, validatingSchema);
        }
        catch (InvalidProjectionException e)
        {
          throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, e.getMessage());
        }
        catch (TemplateRuntimeException e)
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, TEMPLATE_RUNTIME_EXCEPTION_MESSAGE);
        }
      }
    }

    Class<?> resourceClass = requestContext.getFilterResourceModel().getResourceClass();
    if (UnstructuredDataUtil.isUnstructuredDataClass(resourceClass))
    {
      return CompletableFuture.completedFuture(null);
    }

    ResourceMethod method = requestContext.getMethodType();
    RestLiDataValidator validator = new RestLiDataValidator(resourceClass.getAnnotations(),
        requestContext.getFilterResourceModel().getValueClass(), method);
    RestLiRequestData requestData = requestContext.getRequestData();

    ValidationResult result;

    switch (method)
    {
      case CREATE:
      case UPDATE:
        result = validator.validateInput(requestData.getEntity());
        if (!result.isValid())
        {
          throw constructRestLiServiceException(result.getMessages(), result.getMessages().toString());
        }
        break;
      case PARTIAL_UPDATE:
        result = validator.validateInput((PatchRequest<?>) requestData.getEntity());
        if (!result.isValid())
        {
          throw constructRestLiServiceException(result.getMessages(), result.getMessages().toString());
        }
        break;
      case BATCH_CREATE:
        StringBuilder errorMessage = new StringBuilder();
        Map<String, Collection<Message>> messages = new HashMap<>();
        int index = 0;
        for (RecordTemplate entity : requestData.getBatchEntities())
        {
          result = validator.validateInput(entity);
          if (!result.isValid())
          {
            errorMessage.append("Index: ").append(index).append(", ").append(result.getMessages().toString());
            messages.put(String.valueOf(index), result.getMessages());
          }

          ++index;
        }

        if (errorMessage.length() > 0)
        {
          throw constructRestLiServiceException(messages, errorMessage.toString());
        }
        break;
      case BATCH_UPDATE:
      case BATCH_PARTIAL_UPDATE:
        ProtocolVersion protocolVersion = requestContext.getRestliProtocolVersion();
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Collection<Message>> errorMessages = new HashMap<>();
        for (Map.Entry<?, ? extends RecordTemplate> entry : requestData.getBatchKeyEntityMap().entrySet())
        {
          if (method == ResourceMethod.BATCH_UPDATE) {
            result = validator.validateInput(entry.getValue());
          }
          else
          {
            result = validator.validateInput((PatchRequest<?>) entry.getValue());
          }

          if (!result.isValid())
          {
            stringBuilder.append("Key: ").append(entry.getKey()).append(", ").append(result.getMessages().toString());
            errorMessages.put(URIParamUtils.encodeKeyForBody(entry.getKey(), false, protocolVersion), result.getMessages());
          }
        }

        if (stringBuilder.length() > 0)
        {
          throw constructRestLiServiceException(errorMessages, stringBuilder.toString());
        }
        break;
      default:
        break;
    }

    return CompletableFuture.completedFuture(null);
  }

  /**
   * Constructs {@link RestLiServiceException} based on given {@link Message}s and given error message.
   *
   * @param messages collection of {@link Message}s which contain error details.
   * @param message error message.
   * @return {@link RestLiServiceException}, which represents a service failure.
   */
  private RestLiServiceException constructRestLiServiceException(final Collection<Message> messages, final String message)
  {
    RestLiServiceException restLiServiceException = new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, message);

    if (_validationErrorHandler != null)
    {
      _validationErrorHandler.updateErrorDetails(restLiServiceException, messages);
    }

    return restLiServiceException;
  }

  /**
   * Constructs {@link RestLiServiceException} based on given {@link Message}s and given error message.
   * @apiNote Should be used for batch operations.
   *
   * @param messages Map of {@link Message}s. Each entry in the map corresponds to one entity in the batch request input.
   * @param message error message.
   * @return {@link RestLiServiceException}, which represents a service failure.
   */
  private RestLiServiceException constructRestLiServiceException(final Map<String, Collection<Message>> messages, final String message)
  {
    RestLiServiceException restLiServiceException = new RestLiServiceException(HttpStatus.S_422_UNPROCESSABLE_ENTITY, message);

    if (_validationErrorHandler != null)
    {
      _validationErrorHandler.updateErrorDetails(restLiServiceException, messages);
    }

    return restLiServiceException;
  }

  @SuppressWarnings("unchecked")
  public CompletableFuture<Void> onResponse(final FilterRequestContext requestContext,
                                            final FilterResponseContext responseContext)
  {
    RestLiResponseData<?> responseData = responseContext.getResponseData();

    if (responseData.getResponseEnvelope().isErrorResponse())
    {
      return CompletableFuture.completedFuture(null);
    }

    if (shouldValidateOnResponse(requestContext))
    {
      // Get validating schema if it was already built in onRequest
      DataSchema validatingSchema = (DataSchema) requestContext.getFilterScratchpad().get(VALIDATING_SCHEMA_KEY);

      // Otherwise, build validating schema from original schema
      if (validatingSchema == null)
      {
        try
        {
          // Value class from resource model is the only source of truth for record schema.
          // Schema from the record template itself should not be used.
          validatingSchema = DataTemplateUtil.getSchema(requestContext.getFilterResourceModel().getValueClass());
        }
        catch (TemplateRuntimeException e)
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, TEMPLATE_RUNTIME_EXCEPTION_MESSAGE);
        }
      }

      Class<?> resourceClass = requestContext.getFilterResourceModel().getResourceClass();
      ResourceMethod method = requestContext.getMethodType();
      RestLiDataValidator validator = new RestLiDataSchemaDataValidator(resourceClass.getAnnotations(),
          method, validatingSchema);

      switch (method)
      {
        case GET:
          validateSingleResponse(validator, ((GetResponseEnvelope) responseData.getResponseEnvelope()).getRecord());
          break;
        case CREATE:
          if (requestContext.isReturnEntityMethod() && requestContext.isReturnEntityRequested())
          {
            validateSingleResponse(validator, ((CreateResponseEnvelope) responseData.getResponseEnvelope()).getRecord());
          }
          break;
        case PARTIAL_UPDATE:
          if (requestContext.isReturnEntityMethod() && requestContext.isReturnEntityRequested())
          {
            validateSingleResponse(validator, ((PartialUpdateResponseEnvelope) responseData.getResponseEnvelope()).getRecord());
          }
          break;
        case GET_ALL:
          validateCollectionResponse(validator, ((GetAllResponseEnvelope) responseData.getResponseEnvelope()).getCollectionResponse());
          break;
        case FINDER:
          validateCollectionResponse(validator, ((FinderResponseEnvelope) responseData.getResponseEnvelope()).getCollectionResponse());
          break;
        case BATCH_FINDER:
          validateBatchCollectionResponse(validator, ((BatchFinderResponseEnvelope) responseData.getResponseEnvelope()).getItems());
          break;
        case BATCH_GET:
          validateBatchResponse(validator, ((BatchGetResponseEnvelope) responseData.getResponseEnvelope()).getBatchResponseMap());
          break;
        case BATCH_CREATE:
          if (requestContext.isReturnEntityMethod() && requestContext.isReturnEntityRequested())
          {
            validateCreateCollectionResponse(validator, ((BatchCreateResponseEnvelope) responseData.getResponseEnvelope()).getCreateResponses());
          }
          break;
        case BATCH_PARTIAL_UPDATE:
          if (requestContext.isReturnEntityMethod() && requestContext.isReturnEntityRequested())
          {
            validateBatchResponse(validator, ((BatchPartialUpdateResponseEnvelope) responseData.getResponseEnvelope()).getBatchResponseMap());
          }
          break;
      }
    }

    return CompletableFuture.completedFuture(null);
  }

  protected DataSchema constructValidatingSchema(FilterRequestContext requestContext,
      DataSchema originalSchema,
      DataMap projectionMask,
      Collection<String> nonSchemaFieldsToAllowInProjectionMask)
  {
    return ProjectionMaskApplier.buildSchemaByProjection(originalSchema, projectionMask, nonSchemaFieldsToAllowInProjectionMask);
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

  private void validateBatchCollectionResponse(RestLiDataValidator validator, List<BatchFinderResponseEnvelope.BatchFinderEntry> responses)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < responses.size(); i++) {
      BatchFinderResponseEnvelope.BatchFinderEntry entry = responses.get(i);
      // on error case
      if (entry.isErrorResponse()) {
        continue;
      }

      // on success case
      for (int j = 0; j < entry.getElements().size(); j++) {
        RecordTemplate entity = entry.getElements().get(j);
        ValidationResult result = validator.validateOutput(entity);
        if (!result.isValid())
        {
          sb.append("BatchCriteria: ").append(i + " ").append("Element: ").append(j + " ").append(result.getMessages().toString());
        }
      }
    }

    if (sb.length() > 0)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, sb.toString());
    }
  }

  private void validateBatchResponse(RestLiDataValidator validator,
                                     Map<?, BatchResponseEnvelope.BatchResponseEntry> batchResponseMap)
  {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<?, ? extends BatchResponseEnvelope.BatchResponseEntry> entry : batchResponseMap.entrySet())
    {
      if (entry.getValue().hasException())
      {
        continue;
      }

      // The "entity" in the results map may be the raw record entity, or a wrapper containing the record entity
      final RecordTemplate entity = entry.getValue().getRecord();
      ValidationResult result;
      if (entity instanceof UpdateEntityStatus)
      {
        result = validator.validateOutput(((UpdateEntityStatus<? extends RecordTemplate>) entity).getEntity());
      }
      else
      {
        result = validator.validateOutput(entity);
      }

      if (!result.isValid())
      {
        sb.append("Key: ").append(entry.getKey()).append(", ").append(result.getMessages().toString());
      }
    }
    if (sb.length() > 0)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, sb.toString());
    }
  }

  private void validateCreateCollectionResponse(RestLiDataValidator validator,
                                                List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> responses)
  {
    StringBuilder sb = new StringBuilder();
    for (BatchCreateResponseEnvelope.CollectionCreateResponseItem item : responses)
    {
      if (item.isErrorResponse())
      {
        continue;
      }
      ValidationResult
          result = validator.validateOutput(((CreateIdEntityStatus<?, ?>) item.getRecord()).getEntity());
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

  private boolean shouldValidateOnResponse(FilterRequestContext requestContext)
  {
    MaskTree projectionMask = requestContext.getProjectionMask();

    // Make sure the request is for one of the methods to be validated and has either null or a non-empty projection
    // mask. For context, null projection mask means everything is projected while an empty projection mask means
    // nothing is projected. So the validation can be skipped when an empty projection mask is specified.
    return RestLiDataValidator.METHODS_VALIDATED_ON_RESPONSE.contains(requestContext.getMethodType()) &&
        (projectionMask == null || !projectionMask.getDataMap().isEmpty());
  }

  public CompletableFuture<Void> onError(Throwable t, final FilterRequestContext requestContext,
                                         final FilterResponseContext responseContext)
  {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return future;
  }
}