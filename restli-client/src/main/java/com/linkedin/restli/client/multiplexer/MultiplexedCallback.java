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
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiDecodingException;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.common.multiplexer.MultiplexedResponseContent;
import com.linkedin.restli.internal.client.EntityResponseDecoder;

import java.util.List;
import java.util.Map;


/**
 * Converts multiplexed callbacks (aggregated and individual) into a REST response callback.
 *
 * @author Dmitriy Yefremov
 */
public class MultiplexedCallback implements Callback<RestResponse>
{
  private final EntityResponseDecoder<MultiplexedResponseContent> _decoder = new EntityResponseDecoder<MultiplexedResponseContent>(MultiplexedResponseContent.class);

  private final Map<Integer, Callback<RestResponse>> _callbacks;
  private final Callback<MultiplexedResponse> _aggregatedCallback;

  public MultiplexedCallback(Map<Integer, Callback<RestResponse>> callbacks, Callback<MultiplexedResponse> aggregatedCallback)
  {
    _callbacks = callbacks;
    _aggregatedCallback = aggregatedCallback;
  }

  @Override
  public void onError(Throwable e)
  {
    // individual callbacks are notified first
    for (Callback<RestResponse> callback : _callbacks.values())
    {
      callback.onError(e);
    }
    // aggregated callback is guaranteed to be called after all individual callbacks
    _aggregatedCallback.onError(e);
  }

  @Override
  public void onSuccess(RestResponse restResponse)
  {
    Response<MultiplexedResponseContent> response;
    try
    {
      response = _decoder.decodeResponse(restResponse);
    }
    catch (RestLiDecodingException e)
    {
      onError(e);
      return;
    }
    // individual callbacks are notified first
    notifyIndividualCallbacks(response);
    // aggregated callback is guaranteed to be called after all individual callbacks
    notifyAggregatedCallback(response);
  }

  private void notifyIndividualCallbacks(Response<MultiplexedResponseContent> muxResponse)
  {
    List<IndividualResponse> indResponses = muxResponse.getEntity().getResponses();
    for (IndividualResponse individualResponse : indResponses)
    {
      Callback<RestResponse> callback = _callbacks.get(individualResponse.getId());
      RestResponse individualRestResponse = buildIndividualRestResponse(individualResponse);
      if (RestStatus.isOK(individualResponse.getStatus()))
      {
        callback.onSuccess(individualRestResponse);
      }
      else
      {
        RestException exception = new RestException(individualRestResponse, "Received error " + individualRestResponse.getStatus());
        callback.onError(exception);
      }
    }
  }

  private RestResponse buildIndividualRestResponse(IndividualResponse individualResponse)
  {
    return new RestResponseBuilder()
        .setStatus(individualResponse.getStatus())
        .setHeaders(individualResponse.getHeaders())
        .setCookies(individualResponse.getCookies())
        .setEntity(individualResponse.getBody())
        .build();
  }

  private void notifyAggregatedCallback(Response<MultiplexedResponseContent> response)
  {
    MultiplexedResponse muxResponse = new MultiplexedResponse(response.getStatus(), response.getHeaders());
    _aggregatedCallback.onSuccess(muxResponse);
  }
}