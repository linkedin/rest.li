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

package com.linkedin.restli.internal.server.methods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.Parameter.ParamType;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.RoutingException;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ActionMethodAdapter implements RestLiMethodAdapter
{
  ActionMethodAdapter()
  {
  }

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object result,
                                           final Map<String, String> headers)
          throws IOException
  {
    // note that we always return 200 OK for ActionResponse: all response meaning is
    // encoded in the response entity. The HTTP response code is unused.
    headers.put(RestConstants.HEADER_LINKEDIN_TYPE, ActionResponse.class.getName());
    headers.put(RestConstants.HEADER_LINKEDIN_SUB_TYPE, result.getClass().getName());

    ActionResponse<?> actionResponse = new ActionResponse<Object>(result);
    return new PartialRestResponse(actionResponse);
  }

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
      data = DataMapUtils.readMap(request.getEntity().asInputStream());
    }

    // Just want the posted parameters
    List<Parameter<?>> actionPostParams = new ArrayList<Parameter<?>>();
    List<Parameter<?>> parameters = resourceMethodDescriptor.getParameters();
    for (Parameter<?> p : parameters)
    {
      if (p.getParamType().equals(ParamType.POST))
      {
        actionPostParams.add(p);
      }
    }
    DynamicRecordTemplate template =
        new DynamicRecordTemplate(resourceMethodDescriptor.getActionName(),
                                  actionPostParams,
                                  data);

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

          Object toValidate = value;
          if (value instanceof DataTemplate)
          {
            toValidate = ((DataTemplate) value).data();
          }
          else if (value instanceof Enum)
          {
            toValidate = value.toString();
          }

          // validate with fixup=true to ensure that we recursively coerce primitive
          // fields to their schema-defined type.
          ValidationResult result =
              ValidateDataAgainstSchema.validate(toValidate,
                                                 DataTemplateUtil.getSchema(param.getType()),
                                                 new ValidationOptions(RequiredMode.IGNORE,
                                                                       CoercionMode.NORMAL));
          if (!result.isValid())
          {
            throw new RoutingException("Parameter '" + param.getName() + "' of method '"
                                           + resourceMethodDescriptor.getActionName()
                                           + "' failed validation with error '"
                                           + result.getMessages() + "'",
                                       HttpStatus.S_400_BAD_REQUEST.getCode());
          }
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
