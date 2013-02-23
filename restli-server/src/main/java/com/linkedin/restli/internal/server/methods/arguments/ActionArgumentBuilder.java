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

/**
 * $Id: $
 */

package com.linkedin.restli.internal.server.methods.arguments;

import java.util.List;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.RoutingException;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ActionArgumentBuilder implements RestLiArgumentBuilder
{
  @Override
  public Object[] buildArguments(final RoutingResult routingResult,
                                 final RestRequest request)
  {
    ResourceMethodDescriptor resourceMethodDescriptor = routingResult.getResourceMethod();
    DataMap data;
    if (request.getEntity().length() == 0)
    {
      data = new DataMap();
    }
    else
    {
      data = DataMapUtils.readMap(request);
    }

    DynamicRecordTemplate template =
            new DynamicRecordTemplate(data, resourceMethodDescriptor.getRequestDataSchema());

    ValidationResult result =
            ValidateDataAgainstSchema.validate(data,
                                               template.schema(),
                                               new ValidationOptions(RequiredMode.IGNORE,
                                                                     CoercionMode.NORMAL));

    if (!result.isValid())
    {
      throw new RoutingException("Parameters of method '"
                                         + resourceMethodDescriptor.getActionName()
                                         + "' failed validation with error '"
                                         + result.getMessages() + "'",
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }

    List<Parameter<?>> parameters = resourceMethodDescriptor.getParameters();

    Object[] arguments = new Object[parameters.size()];
    int i = 0;
    for (Parameter<?> param : parameters)
    {
      Object value;
      if (!data.containsKey(param.getName()))
      {
        if (param.isOptional() && param.hasDefaultValue())
        {
          value = param.getDefaultValue();
        }
        else if (param.isOptional())
        {
          value = null;
        }
        else if (param.getParamType() == Parameter.ParamType.CALLBACK)
        {
          value = null;
        }
        else if (param.getParamType() == Parameter.ParamType.PARSEQ_CONTEXT)
        {
          value = null;
        }
        else
        {
          throw new RoutingException("Parameter '" + param.getName() + "' of method '"
                                         + resourceMethodDescriptor.getActionName()
                                         + "' is required",
                                     HttpStatus.S_400_BAD_REQUEST.getCode());
        }
      }
      else
      {
        try
        {
          value = template.getValue(param);
        }
        catch (TemplateOutputCastException e)
        {
          throw new RoutingException("Parameter '" + param.getName() + "' of method '"
              + resourceMethodDescriptor.getActionName() + "' must be of type '"
              + param.getType().getName() + "'", HttpStatus.S_400_BAD_REQUEST.getCode());
        }
      }

      arguments[i++] = value;
    }
    return arguments;
  }
}
