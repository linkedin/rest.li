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
import com.linkedin.restli.common.multiplexer.IndividualBody;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestArray;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.common.multiplexer.IndividualResponseArray;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.common.multiplexer.MultiplexedResponseContent;
import com.linkedin.restli.internal.common.ContentTypeUtil;
import com.linkedin.restli.internal.common.ContentTypeUtil.ContentType;
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

  /**
   * The maximum number of requests allowed in the multiplexer under test.
   */
  private static final int MAXIMUM_REQUESTS_NUMBER = 2;

  private static final String FOO_URL = "/foo";
  private static final String BAR_URL = "/bar";

  private static final IndividualBody FOO_JSON_BODY = fakeIndividualBody("foo");
  private static final IndividualBody BAR_JSON_BODY = fakeIndividualBody("bar");
  private static final ByteString FOO_ENTITY = jsonBodyToByteString(FOO_JSON_BODY);
  private static final ByteString BAR_ENTITY = jsonBodyToByteString(BAR_JSON_BODY);

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
    RestRequest request = muxRequestBuilder().setMethod(HttpMethod.PUT.name()).build();
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getError(callback).getStatus(), HttpStatus.S_405_METHOD_NOT_ALLOWED);
  }

  @Test
  public void testHandleWrongContentType() throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);
    RestRequest request = muxRequestBuilder()
        .setMethod(HttpMethod.POST.name())
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_PSON)
        .build();
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getError(callback).getStatus(), HttpStatus.S_415_UNSUPPORTED_MEDIA_TYPE);
  }

  @Test
  public void testGetContentTypeDefault() throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);
    RestRequest request = muxRequestBuilder().build();
    ContentType contentType = ContentTypeUtil.getContentType(request.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    assertEquals(contentType, ContentType.JSON);
  }

  @Test
  public void testGetContentTypeWithParameters() throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);
    RestRequest request = muxRequestBuilder()
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, "application/json; charset=utf-8")
        .build();
    ContentType contentType = ContentTypeUtil.getContentType(request.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    assertEquals(contentType, ContentType.JSON);
  }

  @Test
  public void testHandleEmptyRequest() throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);
    RestRequest request = fakeMuxRestRequest();
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getError(callback).getStatus(), HttpStatus.S_400_BAD_REQUEST);
  }

  @Test
  public void testHandleTooManyParallelRequests() throws Exception
  {
    // MultiplexedRequestHandlerImpl is create with the request limit set to 2
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);
    RestRequest request = fakeMuxRestRequest(fakeIndRequest(0, FOO_URL), fakeIndRequest(1, FOO_URL), fakeIndRequest(2, FOO_URL));
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getError(callback).getStatus(), HttpStatus.S_400_BAD_REQUEST);
  }


  @Test
  public void testHandleTooManySequentialRequests() throws Exception
  {
    // MultiplexedRequestHandlerImpl is create with the request limit set to 2
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null);
    RestRequest request = fakeMuxRestRequest(fakeIndRequest(0, FOO_URL, fakeIndRequest(1, FOO_URL, fakeIndRequest(2, FOO_URL))));
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getError(callback).getStatus(), HttpStatus.S_400_BAD_REQUEST);
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
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(fakeIndResponse(0, FOO_JSON_BODY));

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
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(fakeIndResponse(0, FOO_JSON_BODY), fakeIndResponse(1, BAR_JSON_BODY));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  @Test
  public void testHandleSequentialRequests() throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler);
    RequestContext requestContext = new RequestContext();

    IndividualRequest indRequest1 = fakeIndRequest(1, BAR_URL);
    IndividualRequest indRequest0 = fakeIndRequest(0, FOO_URL, indRequest1);
    RestRequest request = fakeMuxRestRequest(indRequest0);

    // set expectations
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(FOO_URL), requestContext)).andReturn(fakeIndRestResponse(FOO_ENTITY));
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(BAR_URL), requestContext)).andReturn(fakeIndRestResponse(BAR_ENTITY));

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(fakeIndResponse(0, FOO_JSON_BODY), fakeIndResponse(1, BAR_JSON_BODY));

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
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(fakeIndResponse(0, FOO_JSON_BODY), errorIndResponse(1));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  private RestRequestBuilder muxRequestBuilder() throws URISyntaxException
  {
    return new RestRequestBuilder(new URI("/mux"));
  }

  private RestLiServiceException getError(FutureCallback<RestResponse> future) throws InterruptedException
  {
    try
    {
      future.get();
      throw new IllegalStateException("An error is expected");
    }
    catch (ExecutionException e)
    {
      return (RestLiServiceException) e.getCause();
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

    return new MultiplexedRequestHandlerImpl(requestHandler, engine, MAXIMUM_REQUESTS_NUMBER);
  }

  private IndividualRequest fakeIndRequest(int id, String url, IndividualRequest... dependentRequests)
  {
    IndividualRequest individualRequest = new IndividualRequest();
    individualRequest.setId(id);
    individualRequest.setMethod(HttpMethod.GET.name());
    individualRequest.setRelativeUrl(url);
    individualRequest.setDependentRequests(new IndividualRequestArray(Arrays.asList(dependentRequests)));
    return individualRequest;
  }

  private IndividualResponse fakeIndResponse(int id, IndividualBody entity)
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
    return muxRequestBuilder()
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

  private static IndividualBody fakeIndividualBody(String value)
  {
    return new IndividualBody(new DataMap(Collections.singletonMap("value", value)));
  }

  private static ByteString jsonBodyToByteString(IndividualBody jsonBody)
  {
    try
    {
      byte[] bytes = CODEC.mapToBytes(jsonBody.data());
      return ByteString.copy(bytes);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
}
