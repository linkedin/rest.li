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


import com.linkedin.common.callback.FutureCallback;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsCallbackBuilders;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Integration tests for multiplexer.
 *
 * @author Dmitriy Yefremov
 */
public class TestMultiplexerIntegration extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void singleCall() throws Exception
  {
    GetRequest<Greeting> request = new GreetingsCallbackBuilders().get().id(1L).build();
    FutureCallback<Response<Greeting>> muxCallback = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback = new FutureCallback<Response<Greeting>>();
    FutureCallback<MultiplexedResponse> aggregatedCallback = new FutureCallback<MultiplexedResponse>();

    MultiplexedRequest multiplexedRequest = MultiplexedRequestBuilder
        .createParallelRequest()
        .addRequest(request, muxCallback)
        .build();

    getClient().sendRequest(multiplexedRequest, aggregatedCallback);
    getClient().sendRequest(request, directCallback);

    assertEqualResponses(muxCallback, directCallback);

    MultiplexedResponse multiplexedResponse = aggregatedCallback.get();
    Assert.assertEquals(multiplexedResponse.getStatus(), HttpStatus.S_200_OK.getCode());
    Assert.assertFalse(multiplexedResponse.getHeaders().isEmpty());
  }

  @Test
  public void singleCallWithError() throws Exception
  {
    GetRequest<Greeting> request = new GreetingsCallbackBuilders().get().id(Long.MAX_VALUE).build();
    FutureCallback<Response<Greeting>> muxCallback = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback = new FutureCallback<Response<Greeting>>();

    MultiplexedRequest multiplexedRequest = MultiplexedRequestBuilder
        .createParallelRequest()
        .addRequest(request, muxCallback)
        .build();

    getClient().sendRequest(multiplexedRequest);
    getClient().sendRequest(request, directCallback);

    assertEqualErrors(muxCallback, directCallback);
  }

  @Test
  public void twoParallelCalls() throws Exception
  {
    GetRequest<Greeting> request1 = new GreetingsCallbackBuilders().get().id(1L).build();
    FutureCallback<Response<Greeting>> muxCallback1 = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback1 = new FutureCallback<Response<Greeting>>();

    GetRequest<Greeting> request2 = new GreetingsCallbackBuilders().get().id(2L).build();
    FutureCallback<Response<Greeting>> muxCallback2 = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback2 = new FutureCallback<Response<Greeting>>();

    MultiplexedRequest multiplexedRequest = MultiplexedRequestBuilder
        .createParallelRequest()
        .addRequest(request1, muxCallback1)
        .addRequest(request2, muxCallback2)
        .build();

    getClient().sendRequest(multiplexedRequest);
    getClient().sendRequest(request1, directCallback1);
    getClient().sendRequest(request2, directCallback2);

    assertEqualResponses(muxCallback1, directCallback1);
    assertEqualResponses(muxCallback2, directCallback2);
  }

  @ Test
  public void twoParallelCallsWithOneError() throws Exception
  {
    GetRequest<Greeting> request1 = new GreetingsCallbackBuilders().get().id(1L).build();
    FutureCallback<Response<Greeting>> muxCallback1 = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback1 = new FutureCallback<Response<Greeting>>();

    GetRequest<Greeting> request2 = new GreetingsCallbackBuilders().get().id(Long.MAX_VALUE).build();
    FutureCallback<Response<Greeting>> muxCallback2 = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback2 = new FutureCallback<Response<Greeting>>();

    MultiplexedRequest multiplexedRequest = MultiplexedRequestBuilder
        .createParallelRequest()
        .addRequest(request1, muxCallback1)
        .addRequest(request2, muxCallback2)
        .build();

    getClient().sendRequest(multiplexedRequest);
    getClient().sendRequest(request1, directCallback1);
    getClient().sendRequest(request2, directCallback2);

    assertEqualResponses(muxCallback1, directCallback1);
    assertEqualErrors(muxCallback2, directCallback2);
  }

  @Test
  public void twoSequentialCalls() throws Exception
  {
    GetRequest<Greeting> request1 = new GreetingsCallbackBuilders().get().id(1L).build();
    FutureCallback<Response<Greeting>> muxCallback1 = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback1 = new FutureCallback<Response<Greeting>>();

    GetRequest<Greeting> request2 = new GreetingsCallbackBuilders().get().id(2L).build();
    FutureCallback<Response<Greeting>> muxCallback2 = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback2 = new FutureCallback<Response<Greeting>>();

    MultiplexedRequest multiplexedRequest = MultiplexedRequestBuilder
        .createSequentialRequest()
        .addRequest(request1, muxCallback1)
        .addRequest(request2, muxCallback2)
        .build();

    getClient().sendRequest(multiplexedRequest);
    getClient().sendRequest(request1, directCallback1);
    getClient().sendRequest(request2, directCallback2);

    assertEqualResponses(muxCallback1, directCallback1);
    assertEqualResponses(muxCallback2, directCallback2);
  }

  @Test
  public void twoSequentialCallsWithOneError() throws Exception
  {
    GetRequest<Greeting> request1 = new GreetingsCallbackBuilders().get().id(1L).build();
    FutureCallback<Response<Greeting>> muxCallback1 = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback1 = new FutureCallback<Response<Greeting>>();

    GetRequest<Greeting> request2 = new GreetingsCallbackBuilders().get().id(Long.MAX_VALUE).build();
    FutureCallback<Response<Greeting>> muxCallback2 = new FutureCallback<Response<Greeting>>();
    FutureCallback<Response<Greeting>> directCallback2 = new FutureCallback<Response<Greeting>>();

    MultiplexedRequest multiplexedRequest = MultiplexedRequestBuilder
        .createSequentialRequest()
        .addRequest(request1, muxCallback1)
        .addRequest(request2, muxCallback2)
        .build();

    getClient().sendRequest(multiplexedRequest);
    getClient().sendRequest(request1, directCallback1);
    getClient().sendRequest(request2, directCallback2);

    assertEqualResponses(muxCallback1, directCallback1);
    assertEqualErrors(muxCallback2, directCallback2);
  }

  private void assertEqualResponses(FutureCallback<Response<Greeting>> muxCallback, FutureCallback<Response<Greeting>> directCallback) throws InterruptedException, ExecutionException, TimeoutException
  {
    Response<Greeting> muxResponse = getResult(muxCallback);
    Response<Greeting> directResponse = getResult(directCallback);
    Assert.assertEquals(muxResponse.getStatus(), directResponse.getStatus());
    Assert.assertEquals(muxResponse.getEntity(), directResponse.getEntity());
    // multiplexed response headers is a subset of direct response headers (direct response has more due to transport level headers)
    Assert.assertTrue(directResponse.getHeaders().entrySet().containsAll(muxResponse.getHeaders().entrySet()));
  }

  private Response<Greeting> getResult(FutureCallback<Response<Greeting>> callback) throws InterruptedException, ExecutionException, TimeoutException
  {
    return callback.get(5, TimeUnit.SECONDS);
  }

  private void assertEqualErrors(FutureCallback<Response<Greeting>> muxCallback, FutureCallback<Response<Greeting>> directCallback) throws InterruptedException, ExecutionException, TimeoutException
  {
    Exception muxException = getError(muxCallback);
    Exception directException = getError(directCallback);

    Assert.assertEquals(muxException.getClass(), directException.getClass());
    Assert.assertEquals(muxException.toString(), directException.toString());
  }

  private Exception getError(FutureCallback<Response<Greeting>> callback) throws TimeoutException, InterruptedException
  {
    try
    {
      getResult(callback);
      throw new IllegalStateException("The future was expected to fail");
    }
    catch (ExecutionException e)
    {
      return (Exception) e.getCause();
    }
  }
}
