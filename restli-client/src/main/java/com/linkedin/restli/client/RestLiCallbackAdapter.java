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

package com.linkedin.restli.client;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.internal.client.ExceptionUtil;
import com.linkedin.restli.internal.client.RestResponseDecoder;


/**
 * Converts RestResponse into Response and different exceptions -> RemoteInvocationException.
 * @param <T> response type
 */
public class RestLiCallbackAdapter<T> extends CallbackAdapter<Response<T>, RestResponse>
{
  private final RestResponseDecoder<T> _decoder;

  public RestLiCallbackAdapter(RestResponseDecoder<T> decoder, Callback<Response<T>> callback)
  {
    super(callback);
    _decoder = decoder;
  }

  @Override
  protected Response<T> convertResponse(RestResponse response) throws Exception
  {
    return _decoder.decodeResponse(response);
  }

  @Override
  protected Throwable convertError(Throwable error)
  {
    return ExceptionUtil.exceptionForThrowable(error, _decoder);
  }
}
