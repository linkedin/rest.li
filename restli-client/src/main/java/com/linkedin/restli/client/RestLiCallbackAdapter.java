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
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.internal.client.ExceptionUtil;
import com.linkedin.restli.internal.client.RestResponseDecoder;


/**
 * Converts RestResponse into Response and different exceptions -> RemoteInvocationException.
 * @param <T> response type
 */
public class RestLiCallbackAdapter<T> extends CallbackAdapter<Response<T>, RestResponse>
{
  private final RestResponseDecoder<T> _decoder;
  private final RequestContext _requestContext;

  public RestLiCallbackAdapter(RestResponseDecoder<T> decoder, Callback<Response<T>> callback)
  {
    this(decoder, callback, new RequestContext());
  }

  public RestLiCallbackAdapter(RestResponseDecoder<T> decoder, Callback<Response<T>> callback,
      RequestContext requestContext)
  {
    super(callback);
    _decoder = decoder;
    _requestContext = requestContext;
  }

  @Override
  protected Response<T> convertResponse(RestResponse response) throws Exception
  {
    TimingContextUtil.beginTiming(_requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_DESERIALIZATION.key());
    Response<T> convertedResponse = _decoder.decodeResponse(response);
    TimingContextUtil.endTiming(_requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_DESERIALIZATION.key());
    return convertedResponse;
  }

  @Override
  protected Throwable convertError(Throwable error)
  {
    TimingContextUtil.beginTiming(_requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_ERROR_DESERIALIZATION
        .key());
    Throwable throwable = ExceptionUtil.exceptionForThrowable(error, _decoder);
    TimingContextUtil.endTiming(_requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_ERROR_DESERIALIZATION.key());
    return throwable;
  }
}
