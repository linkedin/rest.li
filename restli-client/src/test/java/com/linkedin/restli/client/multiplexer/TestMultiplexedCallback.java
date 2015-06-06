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


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.RestLiDecodingException;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.common.multiplexer.IndividualResponseArray;
import com.linkedin.restli.common.multiplexer.MultiplexedResponseContent;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;


public class TestMultiplexedCallback extends MultiplexerTestBase
{
  @Test
  public void testSuccess() throws Exception
  {
    FutureCallback<RestResponse> callback1 = new FutureCallback<RestResponse>();
    FutureCallback<RestResponse> callback2 = new FutureCallback<RestResponse>();

    ImmutableMap<Integer, Callback<RestResponse>> individualCallbacks = ImmutableMap.<Integer, Callback<RestResponse>>of(ID1, callback1, ID2, callback2);
    FutureCallback<MultiplexedResponse> aggregatedCallback = new FutureCallback<MultiplexedResponse>();

    TestRecord entity1 = fakeEntity(ID1);
    IndividualResponse ir1 = fakeIndividualResponse(ID1, entity1);
    TestRecord entity2 = fakeEntity(ID2);
    IndividualResponse ir2 = fakeIndividualResponse(ID2, entity2);

    MultiplexedResponseContent responseContent = new MultiplexedResponseContent()
        .setResponses(new IndividualResponseArray(ImmutableList.of(ir1, ir2)));

    MultiplexedCallback multiplexedCallback = new MultiplexedCallback(individualCallbacks, aggregatedCallback);
    multiplexedCallback.onSuccess(fakeRestResponse(responseContent));

    Assert.assertEquals(callback1.get(), fakeRestResponse(entity1));
    Assert.assertEquals(callback2.get(), fakeRestResponse(entity2));

    MultiplexedResponse multiplexedResponse = aggregatedCallback.get();
    Assert.assertEquals(multiplexedResponse.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals(multiplexedResponse.getHeaders(), HEADERS);
  }

  @Test
  public void testError() throws Exception
  {
    FutureCallback<RestResponse> callback1 = new FutureCallback<RestResponse>();
    FutureCallback<RestResponse> callback2 = new FutureCallback<RestResponse>();

    ImmutableMap<Integer, Callback<RestResponse>> individualCallbacks = ImmutableMap.<Integer, Callback<RestResponse>>of(ID1, callback1, ID2, callback2);
    FutureCallback<MultiplexedResponse> aggregatedCallback = new FutureCallback<MultiplexedResponse>();

    MultiplexedCallback multiplexedCallback = new MultiplexedCallback(individualCallbacks, aggregatedCallback);
    RestLiDecodingException exception = new RestLiDecodingException(null, null);
    multiplexedCallback.onError(exception);

    Assert.assertSame(getError(callback1), exception);
    Assert.assertSame(getError(callback2), exception);
    Assert.assertSame(getError(aggregatedCallback), exception);
  }

  @Test
  public void testMixed() throws Exception
  {
    FutureCallback<RestResponse> callback1 = new FutureCallback<RestResponse>();
    FutureCallback<RestResponse> callback2 = new FutureCallback<RestResponse>();

    ImmutableMap<Integer, Callback<RestResponse>> individualCallbacks = ImmutableMap.<Integer, Callback<RestResponse>>of(ID1, callback1, ID2, callback2);
    FutureCallback<MultiplexedResponse> aggregatedCallback = new FutureCallback<MultiplexedResponse>();

    TestRecord entity1 = fakeEntity(ID1);
    IndividualResponse ir1 = fakeIndividualResponse(ID1, entity1);
    IndividualResponse ir2 = fakeIndividualErrorResponse(ID2);

    MultiplexedResponseContent responseContent = new MultiplexedResponseContent()
        .setResponses(new IndividualResponseArray(ImmutableList.of(ir1, ir2)));

    MultiplexedCallback multiplexedCallback = new MultiplexedCallback(individualCallbacks, aggregatedCallback);
    multiplexedCallback.onSuccess(fakeRestResponse(responseContent));

    Assert.assertEquals(callback1.get(), fakeRestResponse(entity1));

    RestException actualError = (RestException) getError(callback2);
    Assert.assertEquals(actualError.getResponse(), fakeRestErrorResponse());

    MultiplexedResponse multiplexedResponse = aggregatedCallback.get();
    Assert.assertEquals(multiplexedResponse.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertEquals(multiplexedResponse.getHeaders(), HEADERS);
  }

  private Throwable getError(FutureCallback<?> callback) throws InterruptedException
  {
    try
    {
      callback.get();
      throw new IllegalStateException("An error is expected");
    }
    catch (ExecutionException e)
    {
      return e.getCause();
    }
  }

}
