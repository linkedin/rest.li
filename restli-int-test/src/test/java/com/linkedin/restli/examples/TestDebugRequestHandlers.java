/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.examples;


import com.linkedin.parseq.trace.Trace;
import com.linkedin.parseq.trace.codec.json.JsonTraceCodec;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.CreateIdRequestBuilder;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.response.CreateResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsPromiseRequestBuilders;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestDebugRequestHandlers extends RestLiIntegrationTest
{
  private static final String HEADER_VALUE_TEXT_HTML = "text/html";
  private static final String HEADER_VALUE_APPLICATION_JSON = "application/json";

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
  public void testParseqTraceDebugGetRequestHandlerTracevis()
      throws URISyntaxException, ExecutionException, InterruptedException
  {
    RestRequest request =
        new RestRequestBuilder(
            new URI(URI_PREFIX + "greetingsPromise/1/__debug/parseqtrace/tracevis"))
              .setMethod("GET")
              .build();

    sendRequestAndVerifyParseqTracevisResponse(request);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testParseqTraceDebugPutRequestHandlerTracevis(RootBuilderWrapper<Long, Greeting> builders)
      throws URISyntaxException, ExecutionException, InterruptedException, RemoteInvocationException
  {
    Long newId = createNewGreetingOnTheServer(builders);

    RestRequest request =
        new RestRequestBuilder(
            new URI(URI_PREFIX + "greetingsPromise/" + newId + "/__debug/parseqtrace/tracevis"))
            .setMethod("PUT")
            .setEntity(createNewGreetingBytes(newId))
            .build();

    sendRequestAndVerifyParseqTracevisResponse(request);
  }

  @Test
  public void testParseqTraceDebugPostRequestHandlerTracevis()
      throws URISyntaxException, ExecutionException, InterruptedException
  {
    RestRequest request =
        new RestRequestBuilder(
            new URI(URI_PREFIX + "greetingsPromise/__debug/parseqtrace/tracevis"))
            .setMethod("POST")
            .setEntity(createNewGreetingBytes(444L))
            .build();

    sendRequestAndVerifyParseqTracevisResponse(request);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testParseqTraceDebugDeleteRequestHandlerTracevis(RootBuilderWrapper<Long, Greeting> builders)
      throws URISyntaxException, ExecutionException, InterruptedException, RemoteInvocationException
  {
    Long newId = createNewGreetingOnTheServer(builders);

    RestRequest request =
        new RestRequestBuilder(
            new URI(URI_PREFIX + "greetingsPromise/" + newId + "/__debug/parseqtrace/tracevis"))
            .setMethod("DELETE")
            .setEntity(createNewGreetingBytes(newId))
            .build();

    sendRequestAndVerifyParseqTracevisResponse(request);
  }

  @Test
  public void testParseqTraceDebugRequestHandlerRaw()
      throws URISyntaxException, ExecutionException, InterruptedException
  {
    RestRequest request =
        new RestRequestBuilder(
            new URI(URI_PREFIX + "greetingsPromise/1/__debug/parseqtrace/raw"))
              .setMethod("GET")
              .build();

    sendRequestAndVerifyParseqTraceRaw(request);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testParseqTraceDebugPutRequestHandlerRaw(RootBuilderWrapper<Long, Greeting> builders)
      throws URISyntaxException, ExecutionException, InterruptedException, RemoteInvocationException
  {
    Long newId = createNewGreetingOnTheServer(builders);

    RestRequest request =
        new RestRequestBuilder(
            new URI(URI_PREFIX + "greetingsPromise/" + newId + "/__debug/parseqtrace/raw"))
            .setMethod("PUT")
            .setEntity(createNewGreetingBytes(newId))
            .build();

    sendRequestAndVerifyParseqTraceRaw(request);
  }

  @Test
  public void testParseqTraceDebugPostRequestHandlerRaw()
      throws URISyntaxException, ExecutionException, InterruptedException
  {
    RestRequest request =
        new RestRequestBuilder(
            new URI(URI_PREFIX + "greetingsPromise/__debug/parseqtrace/raw"))
            .setMethod("POST")
            .setEntity(createNewGreetingBytes(444L))
            .build();

    sendRequestAndVerifyParseqTraceRaw(request);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testParseqTraceDebugDeleteRequestHandlerRaw(RootBuilderWrapper<Long, Greeting> builders)
      throws URISyntaxException, ExecutionException, InterruptedException, RemoteInvocationException
  {
    Long newId = createNewGreetingOnTheServer(builders);

    RestRequest request =
        new RestRequestBuilder(
            new URI(URI_PREFIX + "greetingsPromise/" + newId + "/__debug/parseqtrace/raw"))
            .setMethod("DELETE")
            .setEntity(createNewGreetingBytes(newId))
            .build();

    sendRequestAndVerifyParseqTraceRaw(request);
  }

  private void sendRequestAndVerifyParseqTraceRaw(RestRequest request)
      throws InterruptedException, ExecutionException
  {
    Future<RestResponse> restResponseFuture = getDefaultTransportClient().restRequest(request);

    RestResponse restResponse = restResponseFuture.get();
    Assert.assertEquals(restResponse.getStatus(), 200);
    List<String> contentTypeValues = restResponse.getHeaderValues(RestConstants.HEADER_CONTENT_TYPE);
    Assert.assertTrue(contentTypeValues.size() == 1);
    Assert.assertEquals(contentTypeValues.get(0), HEADER_VALUE_APPLICATION_JSON);
    InputStream traceRawStream = restResponse.getEntity().asInputStream();

    JsonTraceCodec codec = new JsonTraceCodec();

    try
    {
      Trace trace = codec.decode(traceRawStream);
      Assert.assertNotNull(trace);
      Assert.assertNotNull(trace.getValue());
      Assert.assertNotEquals(trace.getValue(), "");
    }
    catch (IOException exc)
    {
      Assert.fail("Parseq trace cannot be decoded. Details: " + exc.getMessage());
    }
  }

  private byte[] createNewGreetingBytes(Long id)
  {
    Greeting newGreeting = new Greeting().setMessage("New Greeting!").setId(id);
    return DataMapUtils.mapToBytes(newGreeting.data());
  }

  private void sendRequestAndVerifyParseqTracevisResponse(RestRequest request)
      throws InterruptedException, ExecutionException
  {
    Future<RestResponse> restResponseFuture = getDefaultTransportClient().restRequest(request);

    RestResponse restResponse = restResponseFuture.get();
    Assert.assertEquals(restResponse.getStatus(), 200);
    List<String> contentTypeValues = restResponse.getHeaderValues(RestConstants.HEADER_CONTENT_TYPE);
    Assert.assertTrue(contentTypeValues.size() == 1);
    Assert.assertEquals(contentTypeValues.get(0), HEADER_VALUE_TEXT_HTML);
  }

  private Long createNewGreetingOnTheServer(RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    Greeting newGreeting = new Greeting().setMessage("New Greeting!").setTone(Tone.FRIENDLY);
    RootBuilderWrapper.MethodBuilderWrapper<Long, Greeting, EmptyRecord> createBuilderWrapper = builders.create();
    Long createdId;
    if (createBuilderWrapper.isRestLi2Builder())
    {
      Object objBuilder = createBuilderWrapper.getBuilder();
      @SuppressWarnings("unchecked")
      CreateIdRequestBuilder<Long, Greeting> createIdRequestBuilder = (CreateIdRequestBuilder<Long, Greeting>) objBuilder;
      CreateIdRequest<Long, Greeting> request = createIdRequestBuilder.input(newGreeting).build();
      Response<IdResponse<Long>> response = getClient().sendRequest(request).getResponse();
      createdId = response.getEntity().getId();
    }
    else
    {
      Request<EmptyRecord> request = createBuilderWrapper.input(newGreeting).build();
      Response<EmptyRecord> response = getClient().sendRequest(request).getResponse();
      @SuppressWarnings("unchecked")
      CreateResponse<Long> createResponse = (CreateResponse<Long>) response.getEntity();
      createdId= createResponse.getId();
    }
    return createdId;
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders()) },
      { new RootBuilderWrapper<Long, Greeting>(new GreetingsPromiseRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
