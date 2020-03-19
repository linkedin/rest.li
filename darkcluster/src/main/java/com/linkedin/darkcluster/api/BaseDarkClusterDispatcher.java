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

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;

/**
 * BaseDarkClusterDispatcher handles the basic operations of dispatching a dark request, such as sending the request
 * multiple times, handling errors, gathering metrics, and calling the verifier if needed on the dark response. Because
 * metrics need to be separated out, BaseDarkClusterDispatcher maps 1:1 with a dark cluster, and given that new dark clusters can be
 * added at runtime, BaseDarkClusterDispatcher will be instantiated dynamically.
 *
 * This interface handles multiple requests to be dispatched, whereas {@link DarkClusterDispatcher} is one level down and handles just one request.
 * Both levels are provided as interfaces to allow flexibility in user provided implementations. {@link DarkClusterDispatcher} can also be a singleton,
 * whereas BaseDarkClusterDispatcher is meant to be one per dark cluster, for separation of metrics.
 *
 * The lifecycle of a BaseDarkClusterDispatcher is from the time of the first request sent to that dark cluster until jvm shutdown, or strategy
 * change. As such, the {@link DarkClusterStrategyFactory} will control instantiations of the BaseDarkClusterDispatcher, one per dark cluster, which is
 * the same as the lifecycle of {@link DarkClusterStrategy}.
 */
public interface BaseDarkClusterDispatcher
{
  boolean sendRequest(RestRequest originalRequest, RestRequest darkRequest, RequestContext requestContext, int numRequestDuplicates);
}
