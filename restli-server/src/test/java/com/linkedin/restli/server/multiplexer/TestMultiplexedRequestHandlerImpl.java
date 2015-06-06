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


import com.google.common.collect.ImmutableMap;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestArray;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.common.multiplexer.IndividualResponseArray;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.common.multiplexer.MultiplexedResponseContent;
import com.linkedin.restli.server.RestLiServiceException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;


public class TestMultiplexedRequestHandlerImpl
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();

  private static final String FOO_URL = "/foo";
  private static final String BAR_URL = "/bar";

  private static final ByteString FOO_ENTITY = fakeEntity("foo");
  private static final ByteString BAR_ENTITY = fakeEntity("bar");

  @Test
  public void testIsMultiplexedRequest() throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);

    RestRequest request = fakeMuxRestRequest();
    assertTrue(multiplexer.isMultiplexedRequest(request));
  }

  @Test
  public void testIsNotMultiplexedRequest() throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);

    RestRequest request = new RestRequestBuilder(new URI("/somethingElse")).setMethod(HttpMethod.POST.name()).build();
    assertFalse(multiplexer.isMultiplexedRequest(request));
  }

  @Test
  public void testHandleWrongMethod() throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);

    RestRequest request = new RestRequestBuilder(new URI("/mux")).setMethod(HttpMethod.PUT.name()).build();

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, new RequestContext(), callback);

    RestLiServiceException methodNotAllowedException = (RestLiServiceException) getError(callback);
    assertEquals(methodNotAllowedException.getStatus(), HttpStatus.S_405_METHOD_NOT_ALLOWED);
  }

  @Test
  public void testHandleEmptyRequest() throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);

    RestRequest request = fakeMuxRestRequest();

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, new RequestContext(), callback);

    RestLiServiceException badRequestException = (RestLiServiceException) getError(callback);

    assertEquals(badRequestException.getStatus(), HttpStatus.S_400_BAD_REQUEST);
  }

  @Test
  public void testHandleSingleRequest() throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler);
    RequestContext requestContext = new RequestContext();

    RestRequest request = fakeMuxRestRequest(fakeIndRequest(0, FOO_URL));

    // set expectations
    RestRequest individualRestRequest = fakeIndRestRequest(FOO_URL);
    RestResponse individualRestResponse = fakeIndRestResponse(FOO_ENTITY);
    expect(mockHandler.handleRequestSync(individualRestRequest, requestContext)).andReturn(individualRestResponse);

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(fakeIndResponse(0, FOO_ENTITY));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  @Test
  public void testHandleParallelRequests() throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler);
    RequestContext requestContext = new RequestContext();

    RestRequest request = fakeMuxRestRequest(fakeIndRequest(0, FOO_URL), fakeIndRequest(1, BAR_URL));

    // set expectations
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(FOO_URL), requestContext)).andReturn(fakeIndRestResponse(FOO_ENTITY));
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(BAR_URL), requestContext)).andReturn(fakeIndRestResponse(BAR_ENTITY));

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(fakeIndResponse(0, FOO_ENTITY), fakeIndResponse(1, BAR_ENTITY));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  @Test
  public void testHandleSequentialRequests() throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler);
    RequestContext requestContext = new RequestContext();

    IndividualRequest indRequest0 = fakeIndRequest(0, FOO_URL);
    IndividualRequest indRequest1 = fakeIndRequest(1, BAR_URL);
    indRequest0.setDependentRequests(new IndividualRequestArray(Collections.singleton(indRequest1)));
    RestRequest request = fakeMuxRestRequest(indRequest0);

    // set expectations
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(FOO_URL), requestContext)).andReturn(fakeIndRestResponse(FOO_ENTITY));
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(BAR_URL), requestContext)).andReturn(fakeIndRestResponse(BAR_ENTITY));

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(fakeIndResponse(0, FOO_ENTITY), fakeIndResponse(1, BAR_ENTITY));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  @Test
  public void testHandleError() throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler);
    RequestContext requestContext = new RequestContext();

    RestRequest request = fakeMuxRestRequest(fakeIndRequest(0, FOO_URL), fakeIndRequest(1, BAR_URL));

    // set expectations
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(FOO_URL), requestContext)).andReturn(fakeIndRestResponse(FOO_ENTITY));
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(BAR_URL), requestContext)).andThrow(new NullPointerException());

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(fakeIndResponse(0, FOO_ENTITY), errorIndResponse(1));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  private Throwable getError(FutureCallback<RestResponse> future) throws InterruptedException
  {
    try
    {
      future.get();
      throw new IllegalStateException("An error is expected");
    }
    catch (ExecutionException e)
    {
      return e.getCause();
    }
  }

  private SynchronousRequestHandler createMockHandler()
  {
    return createMockBuilder(SynchronousRequestHandler.class)
        .withConstructor()
        .addMockedMethod("handleRequestSync")
        .createMock();
  }

  private MultiplexedRequestHandlerImpl createMultiplexer(RestRequestHandler requestHandler)
  {
    ExecutorService taskScheduler = Executors.newFixedThreadPool(1);
    ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    Engine engine = new EngineBuilder()
        .setTaskExecutor(taskScheduler)
        .setTimerScheduler(timerScheduler)
        .build();

    return new MultiplexedRequestHandlerImpl(requestHandler, engine);
  }

  private IndividualRequest fakeIndRequest(int id, String url)
  {
    IndividualRequest individualRequest = new IndividualRequest();
    individualRequest.setId(id);
    individualRequest.setMethod(HttpMethod.GET.name());
    individualRequest.setRelativeUrl(url);
    return individualRequest;
  }

  private IndividualResponse fakeIndResponse(int id, ByteString entity)
  {
    IndividualResponse individualResponse = new IndividualResponse();
    individualResponse.setId(id);
    individualResponse.setStatus(HttpStatus.S_200_OK.getCode());
    individualResponse.setCookies(new StringArray());
    individualResponse.setHeaders(new StringMap());
    individualResponse.setBody(entity);
    return individualResponse;
  }

  private IndividualResponse errorIndResponse(int id)
  {
    IndividualResponse individualResponse = new IndividualResponse();
    individualResponse.setId(id);
    individualResponse.setStatus(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
    individualResponse.setCookies(new StringArray());
    individualResponse.setHeaders(new StringMap());
    return individualResponse;
  }

  private RestRequest fakeIndRestRequest(String url) throws URISyntaxException
  {
    return new RestRequestBuilder(new URI(url))
        .setMethod(HttpMethod.GET.name())
        .build();
  }

  private RestResponse fakeIndRestResponse(ByteString entity) throws URISyntaxException
  {
    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_200_OK.getCode())
        .setEntity(entity)
        .build();
  }

  private RestRequest fakeMuxRestRequest(IndividualRequest... requests) throws URISyntaxException, IOException
  {
    MultiplexedRequestContent content = new MultiplexedRequestContent();
    content.setRequests(new IndividualRequestArray(Arrays.asList(requests)));
    return new RestRequestBuilder(new URI("/mux"))
        .setMethod(HttpMethod.POST.name())
        .setEntity(CODEC.mapToBytes(content.data()))
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON)
        .build();
  }

  private RestResponse fakeMuxRestResponse(IndividualResponse... responses) throws IOException
  {
    MultiplexedResponseContent content = new MultiplexedResponseContent();
    content.setResponses(new IndividualResponseArray(Arrays.asList(responses)));
    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_200_OK.getCode())
        .setEntity(CODEC.mapToBytes(content.data()))
        .build();
  }

  private static ByteString fakeEntity(String value)
  {
    try
    {
      ImmutableMap<String, String> data = ImmutableMap.of("value", value);
      byte[] bytes = CODEC.mapToBytes(new DataMap(data));
      return ByteString.copy(bytes);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
}
