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
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;


/**
 * Holds a pair of a request and a corresponding response callback.
 *
 * @author Dmitriy Yefremov
 */
/* package private */ class RequestWithCallback<T>
{
  private final Request<T> _request;
  private final Callback<Response<T>> _callback;

  /* package private */ RequestWithCallback(Request<T> request, Callback<Response<T>> callback)
  {
    _request = request;
    _callback = callback;
  }

  public Request<T> getRequest()
  {
    return _request;
  }

  public Callback<Response<T>> getCallback()
  {
    return _callback;
  }
}
