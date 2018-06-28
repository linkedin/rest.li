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

import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiRequestDataImpl;
import com.linkedin.restli.server.util.UnstructuredDataUtil;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class UpdateArgumentBuilder implements RestLiArgumentBuilder
{
  @Override
  public Object[] buildArguments(RestLiRequestData requestData, RoutingResult routingResult)
  {
    List<Object> positionalArgs = new ArrayList<>();
    if (requestData.hasKey())
    {
      positionalArgs.add(requestData.getKey());
    }

    if (requestData.getEntity() != null)
    {
      positionalArgs.add(requestData.getEntity());
    }
    return ArgumentBuilder.buildArgs(positionalArgs.toArray(),
                                     routingResult.getResourceMethod(),
                                     routingResult.getContext(),
                                     null);
  }

  @Override
  public RestLiRequestData extractRequestData(RoutingResult routingResult, DataMap dataMap)
  {
    RestLiRequestDataImpl.Builder builder = new RestLiRequestDataImpl.Builder();
    if (ArgumentUtils.hasResourceKey(routingResult))
    {
      Object keyValue = ArgumentUtils.getResourceKey(routingResult);
      builder.key(keyValue);
    }
    // Unstructured data is not available in the Rest.Li filters
    if (!UnstructuredDataUtil.isUnstructuredDataRouting(routingResult))
    {
      RecordTemplate record = DataTemplateUtil.wrap(dataMap, ArgumentUtils.getValueClass(routingResult));
      builder.entity(record);
    }
    return builder.build();
  }
}
