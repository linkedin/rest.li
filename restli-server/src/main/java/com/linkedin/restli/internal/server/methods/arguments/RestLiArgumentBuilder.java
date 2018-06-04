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
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.RestLiRequestData;


/**
 * A builder of the arguments used for resource method invocation. The arguments to build depends on the Rest.li
 * method and resource method definition.
 */
public interface RestLiArgumentBuilder
{
  /**
   * Builds arguments used for resource method invocation. The argument definitions are encapsulated in {@link com.linkedin.restli.internal.server.model.ResourceMethodDescriptor}
   * which is available through {@link RoutingResult#getResourceMethod()}.
   *
   * @param requestData The request data built by {@link #buildArguments(RestLiRequestData, RoutingResult)} and processed
   *                    by Rest.li filter.
   */
  Object[] buildArguments(RestLiRequestData requestData, RoutingResult routingResult);

  /**
   * Builds the {@link RestLiRequestData} from the {@link DataMap} parsed from the request body. The <code>RestLiRequestData</code>
   * is processed by Rest.li filters.
   */
  RestLiRequestData extractRequestData(RoutingResult routingResult, DataMap dataMap);
}
