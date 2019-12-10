/*
   Copyright (c) 2016 LinkedIn Corp.

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
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingCallback;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.internal.client.ExceptionUtil;
import com.linkedin.restli.internal.client.RestResponseDecoder;


/**
 * Converts StreamResponse into Response and different exceptions -> RemoteInvocationException.
 * @param <T> response type
 *
 * @author Karim Vidhani
 */
public class RestLiStreamCallbackAdapter<T> implements Callback<StreamResponse>
{
  private final Callback<Response<T>> _wrappedCallback;
  private final RestResponseDecoder<T> _decoder;
  private final RequestContext _requestContext;

  public RestLiStreamCallbackAdapter(RestResponseDecoder<T> decoder, Callback<Response<T>> wrappedCallback)
  {
    this(decoder, wrappedCallback, new RequestContext());
  }

  public RestLiStreamCallbackAdapter(RestResponseDecoder<T> decoder, Callback<Response<T>> wrappedCallback,
      RequestContext requestContext)
  {
    _wrappedCallback = wrappedCallback;
    _decoder = decoder;
    _requestContext = requestContext;
  }

  @Override
  public void onError(Throwable e)
  {
    Callback<Response<T>> callback = new TimingCallback.Builder<>(_wrappedCallback, _requestContext)
        .addEndTimingKey(FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_ERROR_DESERIALIZATION.key())
        .build();
    TimingContextUtil.beginTiming(_requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_ERROR_DESERIALIZATION
        .key());

    //Default behavior as specified by ExceptionUtil.java. Convert the StreamException into a RestException
    //to work with rest.li client exception handling. Eventually when RestException is removed, the complete
    //exception handling system in rest.li client will change to move to StreamException.
    if (e instanceof StreamException)
    {
      Messages.toRestException((StreamException)e, new Callback<RestException>()
      {
        @Override
        public void onError(Throwable e)
        {
          //Should never happen.
          callback.onError(e);
        }

        @Override
        public void onSuccess(RestException result)
        {
          callback.onError(ExceptionUtil.exceptionForThrowable(result, _decoder));
        }
      });
      return;
    }

    if (e instanceof RemoteInvocationException)
    {
      callback.onError(e);
      return;
    }

    callback.onError(new RemoteInvocationException(e));
  }

  @Override
  public void onSuccess(StreamResponse result)
  {
    Callback<Response<T>> callback = new TimingCallback.Builder<>(_wrappedCallback, _requestContext)
        .addEndTimingKey(FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_DESERIALIZATION.key())
        .build();
    TimingContextUtil.beginTiming(_requestContext, FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_DESERIALIZATION.key());
    try
    {
      _decoder.decodeResponse(result, callback);
    }
    catch(Exception exception)
    {
      onError(exception);
    }
  }
}