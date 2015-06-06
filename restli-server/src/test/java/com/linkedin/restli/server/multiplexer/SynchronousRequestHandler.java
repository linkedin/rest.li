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


import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.RestRequestHandler;
import org.apache.commons.lang.NotImplementedException;


/**
 * Converts asynchronous callback based RestRequestHandler interface into a synchronous response based interface for simpler mocking.
 */
public class SynchronousRequestHandler implements RestRequestHandler
{
  @Override
  public final void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    try
    {
      RestResponse response = handleRequestSync(request, requestContext);
      callback.onSuccess(response);
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * Returning a response from this method leads to 'onSuccess' invocation on the callback. Throwing an exception - 'onError'.
   */
  public RestResponse handleRequestSync(RestRequest request, RequestContext requestContext)
  {
    throw new NotImplementedException("Please mock me!");
  }
}
