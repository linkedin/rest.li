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
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.NextRequestFilter;
import com.linkedin.restli.server.filter.RequestFilter;

import java.util.Map;

/**
 * Rest.li validation filter that validates incoming data automatically,
 * and sends an error response back to the client if the data is invalid.
 *
 * @author Soojung Ha
 */
public class RestLiInputValidationFilter implements RequestFilter
{
  @Override
  public void onRequest(final FilterRequestContext requestContext, final NextRequestFilter nextRequestFilter)
  {
    Class<?> resourceClass = requestContext.getFilterResourceModel().getResourceClass();
    ResourceMethod method = requestContext.getMethodType();
    RestLiDataValidator validator = new RestLiDataValidator(resourceClass.getAnnotations(), requestContext.getFilterResourceModel().getValueClass(), method);
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
      ValidationResult result = validator.validateInput((PatchRequest) requestData.getEntity());
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
          result = validator.validateInput((PatchRequest) entry.getValue());
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
    nextRequestFilter.onRequest(requestContext);
  }
}
