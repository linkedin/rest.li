/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.client;

import com.linkedin.r2.message.RequestContext;


/**
 * Class representing a rest.li request along with its request context.
 *
 * @author mnchen
 */
public class RequestInfo
{
  private final Request<?> _request;
  private final RequestContext _requestContext;

  public RequestInfo(Request<?> request, RequestContext requestContext)
  {
    _request = request;
    _requestContext = requestContext;
  }

  public Request<?> getRequest()
  {
    return _request;
  }

  public RequestContext getRequestContext()
  {
    return _requestContext;
  }
}