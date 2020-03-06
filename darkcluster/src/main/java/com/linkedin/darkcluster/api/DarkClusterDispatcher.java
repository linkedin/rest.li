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

package com.linkedin.darkcluster.api;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

/**
 * DarkClusterDispatcher is responsible for sending the request to the dark cluster. This is where custom dispatching operations can
 * be done before the request is sent off, such as adding tracking information to the requestContext, company specific logic, etc.
 */
public interface DarkClusterDispatcher
{
  void sendRequest(final RestRequest originalRequest, final RestRequest darkRequest,
                   final RequestContext requestContext, Callback<RestResponse> callback);
}
