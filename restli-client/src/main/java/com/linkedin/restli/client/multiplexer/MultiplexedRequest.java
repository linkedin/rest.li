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

package com.linkedin.restli.client.multiplexer;


import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;

import java.util.Map;


/**
 * Client side abstraction for a multiplexed request. Contains all the individual requests together with the
 * corresponding callbacks.
 *
 * @author Dmitriy Yefremov
 */
public class MultiplexedRequest
{
  private final MultiplexedRequestContent _content;
  private final Map<Integer, Callback<RestResponse>> _callbacks;

  MultiplexedRequest(MultiplexedRequestContent content, Map<Integer, Callback<RestResponse>> callbacks)
  {
    _content = content;
    _callbacks = callbacks;
  }

  public MultiplexedRequestContent getContent()
  {
    return _content;
  }

  public Map<Integer, Callback<RestResponse>> getCallbacks()
  {
    return _callbacks;
  }
}
