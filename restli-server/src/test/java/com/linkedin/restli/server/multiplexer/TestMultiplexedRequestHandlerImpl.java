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
import com.linkedin.data.template.StringMap;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
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
import com.linkedin.restli.common.multiplexer.IndividualRequestMap;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.common.multiplexer.IndividualResponseMap;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.common.multiplexer.MultiplexedResponseContent;
import com.linkedin.restli.internal.common.ContentTypeUtil;
import com.linkedin.restli.internal.common.ContentTypeUtil.ContentType;
import com.linkedin.restli.internal.common.CookieUtil;
import com.linkedin.restli.internal.common.DataMapConverter;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.server.RestLiServiceException;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.easymock.EasyMock;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class TestMultiplexedRequestHandlerImpl
{
  @DataProvider(name = "multiplexerConfigurations")
  public Object[][] multiplexerConfigurations()
  {
   return new Object[][]
   {
     { MultiplexerRunMode.MULTIPLE_PLANS },
     { MultiplexerRunMode.SINGLE_PLAN }
   };
  }

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

  @Test(dataProvider = "multiplexerConfigurations")
  public void testIsMultiplexedRequest(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null, multiplexerRunMode);
    RestRequest request = fakeMuxRestRequest();
    assertTrue(multiplexer.isMultiplexedRequest(request));
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testIsNotMultiplexedRequest(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null, multiplexerRunMode);
    RestRequest request = new RestRequestBuilder(new URI("/somethingElse")).setMethod(HttpMethod.POST.name()).build();
    assertFalse(multiplexer.isMultiplexedRequest(request));
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleWrongMethod(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null, multiplexerRunMode);
    RestRequest request = muxRequestBuilder().setMethod(HttpMethod.PUT.name()).build();
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getErrorStatus(callback), HttpStatus.S_405_METHOD_NOT_ALLOWED);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleWrongContentType(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null, multiplexerRunMode);
    RestRequest request = muxRequestBuilder()
        .setMethod(HttpMethod.POST.name())
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_PSON)
        .build();
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getErrorStatus(callback), HttpStatus.S_415_UNSUPPORTED_MEDIA_TYPE);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testGetContentTypeDefault(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    createMultiplexer(null, multiplexerRunMode);
    RestRequest request = muxRequestBuilder().build();
    ContentType contentType = ContentTypeUtil.getContentType(request.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    assertEquals(contentType, ContentType.JSON);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testGetContentTypeWithParameters(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    createMultiplexer(null, multiplexerRunMode);
    RestRequest request = muxRequestBuilder()
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, "application/json; charset=utf-8")
        .build();
    ContentType contentType = ContentTypeUtil.getContentType(request.getHeader(RestConstants.HEADER_CONTENT_TYPE));
    assertEquals(contentType, ContentType.JSON);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleEmptyRequest(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null, multiplexerRunMode);
    RestRequest request = fakeMuxRestRequest();
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getErrorStatus(callback), HttpStatus.S_400_BAD_REQUEST);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleTooManyParallelRequests(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    // MultiplexedRequestHandlerImpl is created with the request limit set to 2
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null, multiplexerRunMode);
    RestRequest request = fakeMuxRestRequest(ImmutableMap.of("0", fakeIndRequest(FOO_URL),
      "1", fakeIndRequest(FOO_URL),
      "2", fakeIndRequest(FOO_URL)));
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getErrorStatus(callback), HttpStatus.S_400_BAD_REQUEST);
  }


  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleTooManySequentialRequests(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    // MultiplexedRequestHandlerImpl is created with the request limit set to 2
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(null, multiplexerRunMode);
    IndividualRequest ir2 = fakeIndRequest(FOO_URL);
    IndividualRequest ir1 = fakeIndRequest(FOO_URL, ImmutableMap.of("2", ir2));
    IndividualRequest ir0 = fakeIndRequest(FOO_URL, ImmutableMap.of("1", ir1));
    RestRequest request = fakeMuxRestRequest(ImmutableMap.of("0", ir0));
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    multiplexer.handleRequest(request, new RequestContext(), callback);
    assertEquals(getErrorStatus(callback), HttpStatus.S_400_BAD_REQUEST);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testCustomMultiplexedSingletonFilter(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexerSingletonFilter mockMuxFilter = EasyMock.createMock(MultiplexerSingletonFilter.class);

    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, mockMuxFilter, multiplexerRunMode);
    RequestContext requestContext = new RequestContext();

    // Create multiplexer request with 1 individual request
    RestRequest request = fakeMuxRestRequest(ImmutableMap.of("0", fakeIndRequest("/urlNeedToBeRemapped")));

    // Set mock for request handler.
    // Mock handler will take a FOO_URL request and return the corresponding FOO_ENTITY in the response
    RestRequest individualRestRequest = fakeIndRestRequest(FOO_URL);
    RestResponse individualRestResponse = fakeIndRestResponse(FOO_ENTITY);
    expect(mockHandler.handleRequestSync(individualRestRequest, requestContext)).andReturn(individualRestResponse);

    // Set mock/expectation for multiplexer filter
    // Map request from /urlNeedToBeRemapped to FOO_URL so that mock handler will be able to handle the request.
    // Map response's body from FOO_ENTITY to BAR_JSON_BODY to simulate filtering on response
    expect(mockMuxFilter.filterIndividualRequest(EasyMock.anyObject(IndividualRequest.class)))
                        .andReturn(fakeIndRequest(FOO_URL))
                        .once();
    expect(mockMuxFilter.filterIndividualResponse(EasyMock.anyObject(IndividualResponse.class)))
                        .andReturn(fakeIndResponse(BAR_JSON_BODY))
                        .once();

    // Switch into replay mode
    replay(mockHandler);
    replay(mockMuxFilter);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(ImmutableMap.of(0, fakeIndResponse(BAR_JSON_BODY)));

    assertEquals(muxRestResponse, expectedMuxRestResponse);
    verify(mockMuxFilter);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testIndividualRequestInheritHeadersAndCookiesFromEnvelopeRequest(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    // When some request headers/cookies are passed in the envelope, we need to ensure
    // they are properly included in each of the individual requests sent to
    // MultiplexerSingletonFilter and request handler. In the high level, the expected behavior should be:

    // 1. IndividualRequest that is passed to MultiplexerSingletonFilter should already have headers inherited from the
    //    envelope request.
    // 2. RestRequest that is passed to the request handler should have both headers and cookies inherited from the
    //    envelope request.

    // Create a mockHandler. Captures all headers and cookies found in the request.
    final Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    final Map<String, String> cookies = new HashMap<String, String>();

    SynchronousRequestHandler mockHandler = new SynchronousRequestHandler() {
      @Override
      public RestResponse handleRequestSync(RestRequest request, RequestContext requestContext)
      {
        try
        {
          headers.putAll(request.getHeaders());
          for(HttpCookie cookie : CookieUtil.decodeCookies(request.getCookies()))
          {
            cookies.put(cookie.getName(), cookie.getValue());
          }
          return fakeIndRestResponse(jsonBodyToByteString(new IndividualBody()));
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    };

    // Create a mock MultiplexerSingletonFilter to put request headers inside another headers so
    // we can do assertion on it later.
    final Map<String, String> headersSeenInMuxFilter = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    MultiplexerSingletonFilter muxFilterWithSimulatedFailures = new MultiplexerSingletonFilter() {
      @Override
      public IndividualRequest filterIndividualRequest(IndividualRequest request)
      {
        headersSeenInMuxFilter.putAll(request.getHeaders());
        return request;
      }

      @Override
      public IndividualResponse filterIndividualResponse(IndividualResponse response)
      {
        return response;
      }
    };

    // Prepare request to mux handler
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    RequestContext requestContext = new RequestContext();
    Map<String, IndividualRequest> individualRequests = ImmutableMap.of(
      "0", fakeIndRequest("/request",
                          ImmutableMap.of("X-IndividualHeader", "individualHeader",
                                          "X-OverridableHeader", "overrideHeader"),
                          Collections.<String, IndividualRequest>emptyMap()));

    Set<String> headerWhiteList = new HashSet<String>();
    headerWhiteList.add("X-IndividualHeader");
    headerWhiteList.add("X-OverridableHeader");

    // Prepare mux request with cookie
    RestRequest muxRequest = new RestRequestBuilder(fakeMuxRestRequest(individualRequests))
      .addCookie("cookie1=cookie1Value; cookie2=cookie2Value")
      .addHeaderValue("X-overridableheader", "originalHeader")
      .addHeaderValue("X-Envelope", "envelopeHeaderValue")
      .build();

    // Create mux handler instance
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, muxFilterWithSimulatedFailures, headerWhiteList, individualRequests.size(), multiplexerRunMode);

    try
    {
      multiplexer.handleRequest(muxRequest, requestContext, callback);
    }
    catch (Exception e)
    {
      fail("Multiplexer should not throw exception", e);
    }

    RestResponse muxRestResponse = callback.get();
    // Assert multiplexed request should return a 200 status code
    assertEquals(muxRestResponse.getStatus(), 200, "Multiplexer should return 200");
    MultiplexedResponseContent muxResponseContent = new MultiplexedResponseContent(DataMapConverter.bytesToDataMap(muxRestResponse.getHeaders(), muxRestResponse.getEntity()));

    IndividualResponse response = muxResponseContent.getResponses().get("0");
    assertEquals(response.getStatus().intValue(), 200, "Individual request should not fail. Response body is: " + response.getBody().toString());

    Map<String, String> expectedHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    expectedHeaders.putAll(ImmutableMap.of(
      RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON,
      "X-IndividualHeader", "individualHeader",
      "X-OverridableHeader", "overrideHeader",
      "X-Envelope", "envelopeHeaderValue"));

    Map<String, String> expectedCookies = ImmutableMap.of(
      "cookie1", "cookie1Value",
      "cookie2", "cookie2Value");

    assertEquals(headers.size(), expectedHeaders.size(), "Incorrect numnber of headers, found:" + headers.toString());
    assertEquals(cookies.size(), expectedCookies.size(), "Incorrect numnber of cookies, found:" + cookies.toString());
    assertEquals(headersSeenInMuxFilter.size(), expectedHeaders.size(), "Incorrect numnber of headers seen by Mux filter, found:" + headers.toString());

    for(Map.Entry<String, String> header : headers.entrySet())
    {
      assertEquals(header.getValue(), expectedHeaders.get(header.getKey()), "Incorrect header value for header: " + header.getKey());
    }

    for(Map.Entry<String, String> cookie : cookies.entrySet())
    {
      assertEquals(cookie.getValue(), expectedCookies.get(cookie.getKey()), "Incorrect cookie value for cookie: " + cookie.getKey());
    }

    for(Map.Entry<String, String> header : headersSeenInMuxFilter.entrySet())
    {
      assertEquals(header.getValue(), expectedHeaders.get(header.getKey()), "Incorrect header value for header seen by Mux Filter: " + header.getKey());
    }
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testRequestHeaderWhiteListing(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    // Validating request header white listing logic

    // Create a mockHandler.  Make it return different cookies based on the request
    SynchronousRequestHandler mockHandler = new SynchronousRequestHandler() {
      @Override
      public RestResponse handleRequestSync(RestRequest request, RequestContext requestContext)
      {
        try
        {
          return fakeIndRestResponse(jsonBodyToByteString(fakeIndividualBody("foobar")));
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    };
    // Prepare request to mux handler
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    RequestContext requestContext = new RequestContext();
    Map<String, IndividualRequest> individualRequests = ImmutableMap.of(
      "0", fakeIndRequest("/request1",
                          ImmutableMap.of("x-I-am-a-good-Header", "headerValue"),
                          Collections.<String, IndividualRequest>emptyMap()),
      "1", fakeIndRequest("/request2",
                          ImmutableMap.of("X-Malicious-Header", "evilHeader"),
                          Collections.<String, IndividualRequest>emptyMap()));

    Set<String> headerWhiteList = new HashSet<String>();
    headerWhiteList.add("X-I-AM-A-GOOD-HEADER");

    // Create mux handler instance
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, null, headerWhiteList, individualRequests.size(), multiplexerRunMode);

    try
    {
      multiplexer.handleRequest(fakeMuxRestRequest(individualRequests), requestContext, callback);
    }
    catch (Exception e)
    {
      fail("Multiplexer should not throw exception", e);
    }

    RestResponse muxRestResponse = callback.get();
    // Assert multiplexed request should return a 200 status code
    assertEquals(muxRestResponse.getStatus(), 200, "Multiplexer should return 200");
    MultiplexedResponseContent muxResponseContent = new MultiplexedResponseContent(DataMapConverter.bytesToDataMap(muxRestResponse.getHeaders(), muxRestResponse.getEntity()));
    assertEquals(muxResponseContent.getResponses().get("0").getStatus().intValue(), 200, "Request with whitelisted request header should complete successfully");
    assertEquals(muxResponseContent.getResponses().get("1").getStatus().intValue(), 400, "Request with non-whitelisted request header should receive a 400 bad request error");
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testResponseCookiesAggregated(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    // Per security review: We should not make cookies for each individual responses visible to the client (especially if the cookie is HttpOnly).
    // Therefore all cookies returned by individual responses will be aggregated at the envelope response level.

    // Create a mockHandler. Make it return different cookies based on the request
    SynchronousRequestHandler mockHandler = new SynchronousRequestHandler() {
      @Override
      public RestResponse handleRequestSync(RestRequest request, RequestContext requestContext)
      {
        try
        {
          URI uri = request.getURI();
          RestResponseBuilder restResponseBuilder = new RestResponseBuilder();
          restResponseBuilder.setStatus(HttpStatus.S_200_OK.getCode());
          restResponseBuilder.setEntity(jsonBodyToByteString(fakeIndividualBody("don't care")));
          List<HttpCookie> cookies = new ArrayList<HttpCookie>();
          if (uri.getPath().contains("req1"))
          {
            HttpCookie cookie = new HttpCookie("cookie1", "cookie1Value");
            cookie.setDomain(".www.linkedin.com");
            cookie.setSecure(false);
            cookies.add(cookie);

            HttpCookie commonCookie = new HttpCookie("commonCookie", "commonCookieValue");
            commonCookie.setDomain(".WWW.linkedin.com");
            commonCookie.setPath("/foo");
            commonCookie.setSecure(false);
            cookies.add(commonCookie);
          }
          else if (uri.getPath().contains("req2"))
          {
            HttpCookie cookie = new HttpCookie("cookie2", "cookie2Value");
            cookie.setDomain("www.linkedin.com");
            cookie.setSecure(false);
            cookies.add(cookie);

            cookie = new HttpCookie("cookie3", "cookie3Value");
            cookies.add(cookie);

            HttpCookie commonCookie = new HttpCookie("commonCookie", "commonCookieValue");
            commonCookie.setDomain(".www.linkedin.com");
            commonCookie.setPath("/foo");
            commonCookie.setSecure(true);
            cookies.add(commonCookie);
          }
          else
          {
            HttpCookie cookie = new HttpCookie("cookie2", "newCookie2Value");
            cookie.setDomain("www.linkedin.com");
            cookie.setSecure(true);
            cookies.add(cookie);
          }
          return restResponseBuilder.setCookies(CookieUtil.encodeSetCookies(cookies)).build();
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    };

    // Prepare request to mux handler
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    RequestContext requestContext = new RequestContext();
    Map<String, IndividualRequest> individualRequests = ImmutableMap.of(
      "0", fakeIndRequest("/req1"),
      "1", fakeIndRequest("/req2", ImmutableMap.of("2", fakeIndRequest("/req3"))));

    // Create mux handler instance
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, null, Collections.<String>emptySet(), 3, multiplexerRunMode);

    try
    {
      multiplexer.handleRequest(fakeMuxRestRequest(individualRequests), requestContext, callback);
    }
    catch (Exception e)
    {
      fail("Multiplexer should not throw exception", e);
    }

    RestResponse muxRestResponse = callback.get();
    // assert multiplexed request should return a 200 status code
    assertEquals(muxRestResponse.getStatus(), 200, "Multiplexer should return 200");
    MultiplexedResponseContent muxResponseContent = new MultiplexedResponseContent(DataMapConverter.bytesToDataMap(muxRestResponse.getHeaders(), muxRestResponse.getEntity()));

    // individual response should not have set-cookie headers
    IndividualResponseMap responses = muxResponseContent.getResponses();
    for(IndividualResponse res : responses.values())
    {
      for(String headerName : res.getHeaders().keySet())
      {
        assertTrue(headerName.equalsIgnoreCase("set-cookie"), "Individual response header should not container set-cookie header: " + responses.toString());
      }
    }

    // Ensure cookies are aggregated at envelope level
    List<HttpCookie> cookies = CookieUtil.decodeSetCookies(muxRestResponse.getCookies());
    assertEquals(cookies.size(), 4);
    for(HttpCookie cookie: cookies)
    {
      if ("cookie1".equals(cookie.getName()))
      {
        assertEquals(cookie.getValue(), "cookie1Value");
        assertEquals(cookie.getDomain(), ".www.linkedin.com");
        assertEquals(cookie.getSecure(), false);
      }
      else if ("cookie2".equals(cookie.getName()))
      {
        assertEquals(cookie.getValue(), "newCookie2Value");
        assertEquals(cookie.getDomain(), "www.linkedin.com");
        assertEquals(cookie.getSecure(), true);
      }
      else if ("cookie3".equals(cookie.getName()))
      {
        assertEquals(cookie.getValue(), "cookie3Value");
      }
      else if ("commonCookie".equals(cookie.getName()))
      {
        assertEquals(cookie.getValue(), "commonCookieValue");
        assertEquals(cookie.getDomain().toLowerCase(), ".www.linkedin.com");
        assertEquals(cookie.getPath(), "/foo");
        // Since request0 and request1 are executed in parallel, depending on which request is completed first,
        // we don't know what will be its final 'secure' attribute value.
      }
      else
      {
        fail("Unknown cookie name: " + cookie.getName());
      }
    }
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testMultiplexedSingletonFilterFailures(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    // This test validates when a failure occurred in MultiplexedSingletonFilter for an individual request, only the individual
    // request should fail. The multiplexed request should still be completed successfully with a 200 status code.

    // Setup mock request handler: make handler return a json that contains the request uri
    // We are using this uri in our mock MultiplexerSingletonFilter.filterIndividualResponse function so that
    // we can simulate different response based on the request.
    SynchronousRequestHandler mockHandler = new SynchronousRequestHandler() {
      @Override
      public RestResponse handleRequestSync(RestRequest request, RequestContext requestContext)
      {
        try
        {
          return fakeIndRestResponse(jsonBodyToByteString(fakeIndividualBody(request.getURI().toString())));
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      }
    };

    // Create a mock MultiplexerSingletonFilter to it simulate different type of failures.
    // Failure is simulated are base on the request uri.
    MultiplexerSingletonFilter muxFilterWithSimulatedFailures = new MultiplexerSingletonFilter() {
      @Override
      public IndividualRequest filterIndividualRequest(IndividualRequest request)
      {
        if (request.getRelativeUrl().contains("bad_request"))
        {
          throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "not found");
        }
        else if (request.getRelativeUrl().contains("error_request"))
        {
          throw new IllegalArgumentException("Something really bad happened in filterIndividualRequest");
        }
        return request;
      }

      @Override
      public IndividualResponse filterIndividualResponse(IndividualResponse response)
      {
        if (response.getStatus() == HttpStatus.S_200_OK.getCode())
        {
          if (response.getBody().data().getString("value").contains("notfound_response"))
          {
            throw new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "not found");
          }
          else if (response.getBody().data().getString("value").contains("error_response"))
          {
            // simulate an unexpected exception
            throw new UnsupportedOperationException("Something really bad happened in filterIndividualResponse");
          }
        }
        return response;
      }
    };

    // Prepare request to mux handler
    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
    RequestContext requestContext = new RequestContext();
    Map<String, IndividualRequest> individualRequests = new HashMap<String, IndividualRequest>();
    individualRequests.put("0", fakeIndRequest("/good_request"));
    individualRequests.put("1", fakeIndRequest("/bad_request"));
    individualRequests.put("2", fakeIndRequest("/error_request"));
    individualRequests.put("3", fakeIndRequest("/notfound_response"));
    individualRequests.put("4", fakeIndRequest("/error_response"));
    individualRequests.put("5", fakeIndRequest("/good_request", ImmutableMap.of("6", fakeIndRequest("/bad_request"))));


    RestRequest request = fakeMuxRestRequest(individualRequests);

    // Create mux handler instance
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, muxFilterWithSimulatedFailures, Collections.<String>emptySet(), 10, multiplexerRunMode);

    try
    {
      multiplexer.handleRequest(request, requestContext, callback);
    }
    catch (Exception e)
    {
      fail("Multiplexer should not blow up because one of the individual requests failed", e);
    }

    RestResponse muxRestResponse = callback.get();
    // Assert multiplexed request should return a 200 status code
    assertEquals(muxRestResponse.getStatus(), 200, "Failure in indivudal request should not cause the entire multliplexed request to fail");
    MultiplexedResponseContent muxResponseContent = new MultiplexedResponseContent(DataMapConverter.bytesToDataMap(muxRestResponse.getHeaders(), muxRestResponse.getEntity()));

    IndividualResponseMap responses = muxResponseContent.getResponses();
    // Validate the status code for each of the response
    assertEquals(responses.get("0").getStatus().intValue(), 200, "Mux response body is: " +  responses.toString());
    assertEquals(responses.get("1").getStatus().intValue(), 400, "Mux response body is: " + responses.toString());
    assertEquals(responses.get("2").getStatus().intValue(), 500, "Mux response body is: " +  responses.toString());
    assertEquals(responses.get("3").getStatus().intValue(), 404, "Mux response body is: " +  responses.toString());
    assertEquals(responses.get("4").getStatus().intValue(), 500, "Mux response body is: " + responses.toString());
    assertEquals(responses.get("5").getStatus().intValue(), 200, "Mux response body is: " + responses.toString());
    assertEquals(responses.get("6").getStatus().intValue(), 400, "Mux response body is: " + responses.toString());
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleSingleRequest(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, multiplexerRunMode);
    RequestContext requestContext = new RequestContext();

    RestRequest request = fakeMuxRestRequest(ImmutableMap.of("0", fakeIndRequest(FOO_URL)));

    // set expectations
    RestRequest individualRestRequest = fakeIndRestRequest(FOO_URL);
    RestResponse individualRestResponse = fakeIndRestResponse(FOO_ENTITY);
    expect(mockHandler.handleRequestSync(individualRestRequest, requestContext)).andReturn(individualRestResponse);

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(ImmutableMap.of(0, fakeIndResponse(FOO_JSON_BODY)));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleParallelRequests(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, multiplexerRunMode);
    RequestContext requestContext = new RequestContext();

    RestRequest request = fakeMuxRestRequest(ImmutableMap.of("0", fakeIndRequest(FOO_URL), "1", fakeIndRequest(BAR_URL)));

    // set expectations
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(FOO_URL), requestContext)).andReturn(fakeIndRestResponse(FOO_ENTITY));
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(BAR_URL), requestContext)).andReturn(fakeIndRestResponse(BAR_ENTITY));

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(ImmutableMap.of(0, fakeIndResponse(FOO_JSON_BODY), 1, fakeIndResponse(BAR_JSON_BODY)));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleSequentialRequests(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, multiplexerRunMode);
    RequestContext requestContext = new RequestContext();

    IndividualRequest indRequest1 = fakeIndRequest(BAR_URL);
    IndividualRequest indRequest0 = fakeIndRequest(FOO_URL, ImmutableMap.of("1", indRequest1));
    RestRequest request = fakeMuxRestRequest(ImmutableMap.of("0", indRequest0));

    // set expectations
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(FOO_URL), requestContext)).andReturn(fakeIndRestResponse(FOO_ENTITY));
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(BAR_URL), requestContext)).andReturn(fakeIndRestResponse(BAR_ENTITY));

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(ImmutableMap.of(0, fakeIndResponse(FOO_JSON_BODY), 1, fakeIndResponse(BAR_JSON_BODY)));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  @Test(dataProvider = "multiplexerConfigurations")
  public void testHandleError(MultiplexerRunMode multiplexerRunMode) throws Exception
  {
    SynchronousRequestHandler mockHandler = createMockHandler();
    MultiplexedRequestHandlerImpl multiplexer = createMultiplexer(mockHandler, multiplexerRunMode);
    RequestContext requestContext = new RequestContext();

    RestRequest request = fakeMuxRestRequest(ImmutableMap.of("0", fakeIndRequest(FOO_URL), "1", fakeIndRequest(BAR_URL)));

    // set expectations
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(FOO_URL), requestContext)).andReturn(fakeIndRestResponse(FOO_ENTITY));
    expect(mockHandler.handleRequestSync(fakeIndRestRequest(BAR_URL), requestContext)).andThrow(new NullPointerException());

    // switch into replay mode
    replay(mockHandler);

    FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();

    multiplexer.handleRequest(request, requestContext, callback);

    RestResponse muxRestResponse = callback.get();
    RestResponse expectedMuxRestResponse = fakeMuxRestResponse(ImmutableMap.of(0, fakeIndResponse(FOO_JSON_BODY), 1, errorIndResponse()));

    assertEquals(muxRestResponse, expectedMuxRestResponse);

    verify(mockHandler);
  }

  private static RestRequestBuilder muxRequestBuilder() throws URISyntaxException
  {
    return new RestRequestBuilder(new URI("/mux"));
  }

  private static HttpStatus getErrorStatus(FutureCallback<RestResponse> future) throws InterruptedException
  {
    try
    {
      future.get();
      fail("An error is expected");
      return null;
    }
    catch (ExecutionException e)
    {
      return HttpStatus.fromCode(((RestException) e.getCause()).getResponse().getStatus());
    }
  }

  private static SynchronousRequestHandler createMockHandler()
  {
    return createMockBuilder(SynchronousRequestHandler.class)
        .withConstructor()
        .addMockedMethod("handleRequestSync")
        .createMock();
  }

  private static MultiplexedRequestHandlerImpl createMultiplexer(RestRequestHandler requestHandler, MultiplexerSingletonFilter multiplexerSingletonFilter,
      MultiplexerRunMode multiplexerRunMode)
  {
    return createMultiplexer(requestHandler, multiplexerSingletonFilter, Collections.<String>emptySet(), MAXIMUM_REQUESTS_NUMBER, multiplexerRunMode);
  }

  private static MultiplexedRequestHandlerImpl createMultiplexer(RestRequestHandler requestHandler,
                                                                 MultiplexerSingletonFilter multiplexerSingletonFilter,
                                                                 Set<String> individualRequestHeaderWhitelist,
                                                                 int maxRequestCount,
                                                                 MultiplexerRunMode multiplexerRunMode)
  {
    ExecutorService taskScheduler = Executors.newFixedThreadPool(1);
    ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    Engine engine = new EngineBuilder()
      .setTaskExecutor(taskScheduler)
      .setTimerScheduler(timerScheduler)
      .build();

    return new MultiplexedRequestHandlerImpl(requestHandler, engine, maxRequestCount, individualRequestHeaderWhitelist, multiplexerSingletonFilter,
        multiplexerRunMode, new ErrorResponseBuilder());
  }

  private static MultiplexedRequestHandlerImpl createMultiplexer(RestRequestHandler requestHandler, MultiplexerRunMode multiplexerRunMode)
  {
    return createMultiplexer(requestHandler, null, multiplexerRunMode);
  }

  private static IndividualRequest fakeIndRequest(String url)
  {
    return fakeIndRequest(url, Collections.<String, IndividualRequest>emptyMap());
  }


  private static IndividualRequest fakeIndRequest(String url, Map<String, IndividualRequest> dependentRequests)
  {
    return fakeIndRequest(url, null, dependentRequests);
  }

  private static IndividualRequest fakeIndRequest(String url, Map<String, String> headers, Map<String, IndividualRequest> dependentRequests)
  {
    IndividualRequest individualRequest = new IndividualRequest();
    individualRequest.setMethod(HttpMethod.GET.name());
    individualRequest.setRelativeUrl(url);
    if (headers != null && headers.size() > 0)
    {
      individualRequest.setHeaders(new StringMap(headers));
    }
    individualRequest.setDependentRequests(new IndividualRequestMap(dependentRequests));
    return individualRequest;
  }

  private static IndividualResponse fakeIndResponse(IndividualBody entity)
  {
    IndividualResponse individualResponse = new IndividualResponse();
    individualResponse.setStatus(HttpStatus.S_200_OK.getCode());
    individualResponse.setHeaders(new StringMap());
    individualResponse.setBody(entity);
    return individualResponse;
  }

  private static IndividualResponse errorIndResponse()
  {
    IndividualResponse individualResponse = new IndividualResponse();
    individualResponse.setStatus(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
    return individualResponse;
  }

  private static RestRequest fakeIndRestRequest(String url) throws URISyntaxException
  {
    return new RestRequestBuilder(new URI(url))
        .setMethod(HttpMethod.GET.name())
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON)
        .build();
  }

  private static RestResponse fakeIndRestResponse(ByteString entity) throws URISyntaxException
  {
    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_200_OK.getCode())
        .setEntity(entity)
        .build();
  }

  private static RestRequest fakeMuxRestRequest() throws URISyntaxException, IOException
  {
    return fakeMuxRestRequest(Collections.<String, IndividualRequest>emptyMap());
  }

  private static RestRequest fakeMuxRestRequest(Map<String, IndividualRequest> requests) throws URISyntaxException, IOException
  {
    MultiplexedRequestContent content = new MultiplexedRequestContent();
    content.setRequests(new IndividualRequestMap(requests));
    return muxRequestBuilder()
        .setMethod(HttpMethod.POST.name())
        .setEntity(CODEC.mapToBytes(content.data()))
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON)
        .build();
  }

  private static RestResponse fakeMuxRestResponse(Map<Integer, IndividualResponse> responses) throws IOException
  {
    IndividualResponseMap individualResponseMap = new IndividualResponseMap();
    for (Map.Entry<Integer, IndividualResponse> responseMapEntry : responses.entrySet())
    {
      individualResponseMap.put(Integer.toString(responseMapEntry.getKey()), responseMapEntry.getValue());
    }
    MultiplexedResponseContent content = new MultiplexedResponseContent();
    content.setResponses(individualResponseMap);
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
