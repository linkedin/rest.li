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

package com.linkedin.restli.internal.server.methods.arguments;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiRequestDataImpl;
import com.linkedin.restli.server.util.UnstructuredDataUtil;

import static com.linkedin.restli.internal.server.methods.arguments.ArgumentBuilder.checkEntityNotNull;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class CreateArgumentBuilder implements RestLiArgumentBuilder
{
  @Override
  public Object[] buildArguments(RestLiRequestData requestData, RoutingResult routingResult)
  {
    Object[] positionalArgs = requestData.getEntity() != null ? new Object[]{requestData.getEntity()} : new Object[]{};

    return ArgumentBuilder.buildArgs(positionalArgs,
                                     routingResult.getResourceMethod(),
                                     routingResult.getContext(),
                                     null);
  }

  @Override
  public RestLiRequestData extractRequestData(RoutingResult routingResult, DataMap dataMap)
  {
    // Unstructured data is not available in the Rest.Li filters
    if (UnstructuredDataUtil.isUnstructuredDataRouting(routingResult))
    {
      return new RestLiRequestDataImpl.Builder().build();
    }
    else
    {
      checkEntityNotNull(dataMap, ResourceMethod.CREATE);
      RecordTemplate inputEntity = DataTemplateUtil.wrap(dataMap, ArgumentUtils.getValueClass(routingResult));
      return new RestLiRequestDataImpl.Builder().entity(inputEntity).build();
    }
  }
}
