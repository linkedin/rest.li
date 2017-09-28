/*
    Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.filter;

import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.server.RestLiRequestData;


/**
 * @author xma
 */
public class FilterChainDispatcherImpl implements FilterChainDispatcher
{
  private RoutingResult _method;
  private RestLiMethodInvoker _methodInvoker;
  private RestLiArgumentBuilder _restLiArgumentBuilder;

  public FilterChainDispatcherImpl(RoutingResult method,
      RestLiMethodInvoker methodInvoker,
      RestLiArgumentBuilder adapter)
  {
    _method = method;
    _methodInvoker = methodInvoker;
    _restLiArgumentBuilder = adapter;
  }

  @Override
  public void onRequestSuccess(final RestLiRequestData requestData, final RestLiCallback restLiCallback)
  {
    _methodInvoker.invoke(requestData, _method, _restLiArgumentBuilder, restLiCallback);
  }

}
