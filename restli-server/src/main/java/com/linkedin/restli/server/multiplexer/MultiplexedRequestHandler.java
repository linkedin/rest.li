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

package com.linkedin.restli.server.multiplexer;


import com.linkedin.r2.message.Request;
import com.linkedin.r2.transport.common.RestRequestHandler;


/**
 * Main multiplexer interface. Responsible for handling of multiplexed requests.
 *
 * @author Dmitriy Yefremov
 */
public interface MultiplexedRequestHandler extends RestRequestHandler
{
  /**
   * Checks if the given request is a multiplexed request.
   *
   * @param request the request to check
   * @return true if it is a multiplexer request, false otherwise
   */
  boolean isMultiplexedRequest(Request request);
}
