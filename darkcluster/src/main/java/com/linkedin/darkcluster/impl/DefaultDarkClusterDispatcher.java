/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster.impl;

import javax.annotation.Nonnull;

import com.linkedin.common.callback.Callback;
import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;

/**
 * Default implementation of DarkClusterDispatcher
 * requests should probably be offloaded to an executor.
 */
public class DefaultDarkClusterDispatcher implements DarkClusterDispatcher
{
  private final Client _client;

  public DefaultDarkClusterDispatcher(@Nonnull final Client client)
  {
    _client = client;
  }

  @Override
  public boolean sendRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext originalRequestContext,
                             String darkClusterName, Callback<RestResponse> callback)
  {
    final RequestContext darkContext = new RequestContext();
    Object requestWasTunneled = originalRequestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED);
    if (requestWasTunneled != null && (Boolean) requestWasTunneled)
    {
      darkContext.putLocalAttr(R2Constants.FORCE_QUERY_TUNNEL, true);
    }
    _client.restRequest(darkRequest, darkContext, callback);
    return true;
  }
}
