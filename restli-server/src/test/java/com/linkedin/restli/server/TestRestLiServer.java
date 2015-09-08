/*
   Copyright (c) 2012 LinkedIn Corp.

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


package com.linkedin.restli.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.filter.RequestFilter;
import com.linkedin.restli.server.filter.ResponseFilter;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.test.EasyMockResourceFactory;
import com.linkedin.restli.server.twitter.AsyncStatusCollectionResource;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.eq;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * "Integration" test that exercises a couple end-to-end use cases
 *
 * @author dellamag
 */
public class TestRestLiServer
{
  private static final String DEBUG_HANDLER_RESPONSE_A = "Response A";
  private static final String DEBUG_HANDLER_RESPONSE_B = "Response B";

  private RestLiServer _server;
  private RestLiServer _serverWithFilters;
  private RestLiServer _serverWithCustomErrorResponseConfig; // configured different than server
  private EasyMockResourceFactory _resourceFactory;
  private RequestFilter _mockRequestFilter;
  private ResponseFilter _mockResponseFilter;

  @BeforeTest
  protected void setUp()
  {
    // silence null engine warning and get EasyMock failure if engine is used
    Engine fakeEngine = EasyMock.createMock(Engine.class);
    _mockRequestFilter = EasyMock.createMock(RequestFilter.class);
    _mockResponseFilter = EasyMock.createMock(ResponseFilter.class);
    setUpServer(fakeEngine);
    setupServerWithFilters(fakeEngine);
    setupServerWithCustomErrorResponseConfig(fakeEngine);
    EasyMock.replay(fakeEngine);
  }

  private void setupServerWithCustomErrorResponseConfig(Engine fakeEngine)
  {
    RestLiConfig customErrorResponseConfig = new RestLiConfig();
    customErrorResponseConfig.addResourcePackageNames("com.linkedin.restli.server.twitter");
    customErrorResponseConfig.setErrorResponseFormat(ErrorResponseFormat.MESSAGE_AND_DETAILS);
    customErrorResponseConfig.setInternalErrorMessage("kthxbye.");
    _serverWithCustomErrorResponseConfig = new RestLiServer(customErrorResponseConfig, _resourceFactory, fakeEngine);
  }

  private void setupServerWithFilters(Engine fakeEngine)
  {
    RestLiConfig config = new RestLiConfig(); // default is to use STRICT checking
    config.addResourcePackageNames("com.linkedin.restli.server.twitter");
    config.addRequestFilter(_mockRequestFilter);
    config.addResponseFilter(_mockResponseFilter);
    _serverWithFilters = new RestLiServer(config, _resourceFactory, fakeEngine);
  }

  private void setUpServer(Engine engine)
  {
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.twitter");
    _resourceFactory  = new EasyMockResourceFactory();

    RestLiDebugRequestHandler debugRequestHandlerA = new RestLiDebugRequestHandler()
      {
        @Override
        public void handleRequest(RestRequest request,
                                  RequestContext context,
                                  ResourceDebugRequestHandler resourceRequestHandler,
                                  Callback<RestResponse> callback)
        {
          handleRequestWithCustomResponse(callback, DEBUG_HANDLER_RESPONSE_A);
        }

        @Override
        public String getHandlerId()
        {
          return "a";
        }
      };

    RestLiDebugRequestHandler debugRequestHandlerB = new RestLiDebugRequestHandler()
    {
      @Override
      @SuppressWarnings("unchecked")
      public void handleRequest(RestRequest request,
                                RequestContext context,
                                ResourceDebugRequestHandler resourceRequestHandler,
                                Callback<RestResponse> callback)
      {
        resourceRequestHandler.handleRequest(request,
                                             context,
                                             EasyMock.createMock(RequestExecutionCallback.class));
        handleRequestWithCustomResponse(callback, DEBUG_HANDLER_RESPONSE_B);
      }

      @Override
      public String getHandlerId()
      {
        return "b";
      }
    };

    config.addDebugRequestHandlers(debugRequestHandlerA, debugRequestHandlerB);
    config.addRequestFilter(_mockRequestFilter);
    config.addResponseFilter(_mockResponseFilter);
    _server = new RestLiServer(config, _resourceFactory, engine);
  }

  private void handleRequestWithCustomResponse(Callback<RestResponse> callback, String response)
  {
    RestResponseBuilder responseBuilder = new RestResponseBuilder();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try
    {
      IOUtils.write(response, outputStream);
    }
    catch (IOException exc)
    {
      //Test will fail later.
    }

    responseBuilder.setEntity(outputStream.toByteArray());
    callback.onSuccess(responseBuilder.build());
  }

  @AfterTest
  protected void tearDown()
  {
    _resourceFactory = null;
    _server = null;
    EasyMock.reset(_mockRequestFilter, _mockResponseFilter);
  }

  @AfterMethod
  protected void afterMethod()
  {
    EasyMock.reset(_mockRequestFilter, _mockResponseFilter);
  }

  @DataProvider(name = "validClientProtocolVersionData")
  public Object[][] provideValidClientProtocolVersionData()
  {
    return new Object[][]
        {
            { _server, AllProtocolVersions.BASELINE_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION },
            { _server, AllProtocolVersions.LATEST_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION },
            { _server, AllProtocolVersions.NEXT_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION },
        };
  }

  @DataProvider(name = "invalidClientProtocolVersionData")
  public Object[][] provideInvalidClientProtocolVersionData()
  {
    ProtocolVersion greaterThanNext = new ProtocolVersion(AllProtocolVersions.NEXT_PROTOCOL_VERSION.getMajor() + 1,
                                                          0,
                                                          0);

    return new Object[][]
        {
            { _server, greaterThanNext, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION },
            { _server, new ProtocolVersion(0, 0, 0), RestConstants.HEADER_RESTLI_PROTOCOL_VERSION },
        };
  }

  @Test
  public void testServer() throws Exception
  {
    testValidRequest(_server, null, false, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION);
  }

  @Test
  public void testServerWithFilters() throws Exception
  {
    testValidRequest(_serverWithFilters, null, true, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION);
  }

  @Test(dataProvider = "validClientProtocolVersionData")
  public void testValidClientProtocolVersion(RestLiServer server, ProtocolVersion clientProtocolVersion, String headerConstant)
      throws URISyntaxException
  {
    testValidRequest(server, clientProtocolVersion, false, headerConstant);
  }

  private void testValidRequest(RestLiServer restLiServer, final ProtocolVersion clientProtocolVersion, boolean filters, final String headerConstant) throws URISyntaxException
  {
    RestRequest request;
    if (clientProtocolVersion != null)
    {
      request =
          new RestRequestBuilder(new URI("/statuses/1")).setHeader(headerConstant,
                                                                   clientProtocolVersion.toString()).build();
    }
    else
    {
      request = new RestRequestBuilder(new URI("/statuses/1")).build();
    }

    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(buildStatusRecord()).once();
    if (filters)
    {
      _mockRequestFilter.onRequest(EasyMock.anyObject(FilterRequestContext.class));
      EasyMock.expectLastCall().times(1);
      _mockResponseFilter.onResponse(EasyMock.anyObject(FilterRequestContext.class),
                                     EasyMock.anyObject(FilterResponseContext.class));
      EasyMock.expectLastCall().times(1);
      EasyMock.replay(_mockRequestFilter, _mockResponseFilter);
    }
    EasyMock.replay(statusResource);
    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        assertEquals(restResponse.getStatus(), 200);
        assertTrue(restResponse.getEntity().length() > 0);
        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);

        if (clientProtocolVersion != null)
        {
          assertEquals(
              RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
              headerConstant,
              "Rest.li protocol header name is unexpected.");
        }
      }

      @Override
      public void onError(Throwable e)
      {
        fail();
      }
    };
    restLiServer.handleRequest(request, new RequestContext(), callback);
    if (filters)
    {
      EasyMock.verify(_mockRequestFilter, _mockResponseFilter);
    }
  }

  @Test(dataProvider = "invalidClientProtocolVersionData")
  public void testInvalidClientProtocolVersion(RestLiServer server, ProtocolVersion clientProtocolVersion, String headerConstant)
      throws URISyntaxException
  {
    testBadRequest(server, clientProtocolVersion, headerConstant);
  }

  private void testBadRequest(RestLiServer restLiServer, final ProtocolVersion clientProtocolVersion, String headerConstant)
      throws URISyntaxException
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1")).setHeader(headerConstant, clientProtocolVersion.toString()).build();

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail("The request should have failed!");
      }

      @Override
      public void onError(Throwable e)
      {
        assertEquals(((RestException)e).getResponse().getStatus(), 400);
        String expectedErrorMessage =
            "Rest.li protocol version " + clientProtocolVersion + " used by the client is not supported!";
        assertEquals(e.getCause().getMessage(), expectedErrorMessage);
      }
    };
    restLiServer.handleRequest(request, new RequestContext(), callback);
  }

  @SuppressWarnings({"unchecked"})
  @Test
  public void testAsyncServer() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/asyncstatuses/1"))
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString())
        .build();

    final AsyncStatusCollectionResource statusResource = getMockResource(AsyncStatusCollectionResource.class);

    final Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        assertEquals(restResponse.getStatus(), 200);
        assertTrue(restResponse.getEntity().length() > 0);
        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);
      }

      @Override
      public void onError(Throwable e)
      {
        fail();
      }
    };

    statusResource.get(eq(1L), EasyMock.<Callback<Status>> anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        Callback<Status> callback = (Callback<Status>) EasyMock.getCurrentArguments()[1];
        Status stat = buildStatusRecord();
        callback.onSuccess(stat);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    _server.handleRequest(request, new RequestContext(), callback);
  }

  @Test
  public void testSyncNullObject404() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString())
        .build();

    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(null).once();
    EasyMock.replay(statusResource);

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail("We should not get a success here. The server should have returned a 404!");
      }

      @Override
      public void onError(Throwable e)
      {
        RestException restException = (RestException) e;
        assertEquals(restException.getResponse().getStatus(), 404, "We should get a 404 back here!");
        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testPreprocessingError(final ProtocolVersion protocolVersion, final String errorResponseHeaderName) throws Exception
  {
    //Bad key type will generate a routing error
    RestRequest request = new RestRequestBuilder(new URI("/statuses/abcd"))
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString())
        .build();
    final StatusCollectionResource statusResource = _resourceFactory.getMock(StatusCollectionResource.class);
    EasyMock.replay(statusResource);

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail();
      }

      @Override
      public void onError(Throwable e)
      {
        assertTrue(e instanceof RestException);
        RestException restException = (RestException)e;
        RestResponse restResponse = restException.getResponse();

        assertEquals(restResponse.getStatus(), 400);
        assertTrue(restResponse.getEntity().length() > 0);
        assertEquals(restResponse.getHeader(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);

        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testApplicationException(final ProtocolVersion protocolVersion, final String errorResponseHeaderName) throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString())
        .build();
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andThrow(new RestLiServiceException(
            HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Mock Exception")).once();
    EasyMock.replay(statusResource);

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail();
      }

      @Override
      public void onError(Throwable e)
      {
        assertTrue(e instanceof RestException);
        RestException restException = (RestException)e;
        RestResponse restResponse = restException.getResponse();

        try
        {
          assertEquals(restResponse.getStatus(), 500);
          assertTrue(restResponse.getEntity().length() > 0);
          assertEquals(restResponse.getHeader(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);
          ErrorResponse responseBody = DataMapUtils.read(restResponse.getEntity().asInputStream(), ErrorResponse.class);
          assertEquals(responseBody.getMessage(), "Mock Exception");
          assertEquals(responseBody.getExceptionClass(), "com.linkedin.restli.server.RestLiServiceException");
          assertTrue(responseBody.getStackTrace().startsWith(
              "com.linkedin.restli.server.RestLiServiceException [HTTP Status:500]: Mock Exception"));
          assertEquals(responseBody.getStatus().intValue(), 500);

          EasyMock.verify(statusResource);
          EasyMock.reset(statusResource);
        }
        catch (Exception e2)
        {
          fail(e2.toString());
        }
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);
  }

  @Test
  public void testInternalErrorMessage() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString())
        .build();
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andThrow(new IllegalArgumentException("oops")).once();
    EasyMock.replay(statusResource);

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail();
      }

      @Override
      public void onError(Throwable e)
      {
        assertTrue(e instanceof RestException);
        RestException restException = (RestException)e;
        RestResponse restResponse = restException.getResponse();

        try
        {
          ErrorResponse responseBody = DataMapUtils.read(restResponse.getEntity().asInputStream(), ErrorResponse.class);
          assertEquals(responseBody.getMessage(), ErrorResponseBuilder.DEFAULT_INTERNAL_ERROR_MESSAGE);

          EasyMock.verify(statusResource);
          EasyMock.reset(statusResource);
        }
        catch (Exception e2)
        {
          fail(e2.toString());
        }
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);
  }

  @Test
  public void testCustomizedInternalErrorMessage() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString())
        .build();
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andThrow(new IllegalArgumentException("oops")).once();
    EasyMock.replay(statusResource);

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail();
      }

      @Override
      public void onError(Throwable e)
      {
        assertTrue(e instanceof RestException);
        RestException restException = (RestException)e;
        RestResponse restResponse = restException.getResponse();

        try
        {
          ErrorResponse responseBody = DataMapUtils.read(restResponse.getEntity().asInputStream(), ErrorResponse.class);
          assertEquals(responseBody.getMessage(), "kthxbye.");

          EasyMock.verify(statusResource);
          EasyMock.reset(statusResource);
        }
        catch (Exception e2)
        {
          fail(e2.toString());
        }
      }
    };

    _serverWithCustomErrorResponseConfig.handleRequest(request, new RequestContext(), callback);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testMessageAndDetailsErrorFormat(final ProtocolVersion protocolVersion, final String errorResponseHeaderName) throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString())
        .build();
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    final DataMap details = new DataMap();
    details.put("errorKey", "errorDetail");
    EasyMock.expect(statusResource.get(eq(1L))).andThrow(new RestLiServiceException(
        HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Mock Exception").setErrorDetails(details)).once();
    EasyMock.replay(statusResource);

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail();
      }

      @Override
      public void onError(Throwable e)
      {
        assertTrue(e instanceof RestException);
        RestException restException = (RestException)e;
        RestResponse restResponse = restException.getResponse();

        try
        {
          assertEquals(restResponse.getStatus(), 500);
          assertTrue(restResponse.getEntity().length() > 0);
          assertEquals(restResponse.getHeader(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);
          ErrorResponse responseBody = DataMapUtils.read(restResponse.getEntity().asInputStream(), ErrorResponse.class);

          // in this test, we're using the _serverWithCustomErrorResponseConfig (see below), which has been configure to use the
          // MESSAGE_AND_DETAILS ErrorResponseFormat, so stack trace and other error response parts should be absent
          assertEquals(responseBody.getMessage(), "Mock Exception");
          assertEquals(responseBody.getErrorDetails().data().getString("errorKey"), "errorDetail");
          assertFalse(responseBody.hasExceptionClass());
          assertFalse(responseBody.hasStackTrace());
          assertFalse(responseBody.hasStatus());

          EasyMock.verify(statusResource);
          EasyMock.reset(statusResource);
        }
        catch (Exception e2)
        {
          fail(e2.toString());
        }
      }
    };

    _serverWithCustomErrorResponseConfig.handleRequest(request, new RequestContext(), callback);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testPostProcessingException(final ProtocolVersion protocolVersion, final String errorResponseHeaderName) throws Exception
  {
    //request for nested projection within string field will generate error
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1?fields=text:(invalid)"))
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString())
        .build();
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(buildStatusRecord()).once();
    EasyMock.replay(statusResource);

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail();
      }

      @Override
      public void onError(Throwable e)
      {
        assertTrue(e instanceof RestException);
        RestException restException = (RestException)e;
        RestResponse restResponse = restException.getResponse();

        try
        {
          assertEquals(restResponse.getStatus(), 500);
          assertTrue(restResponse.getEntity().length() > 0);
          assertEquals(restResponse.getHeader(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);

          EasyMock.verify(statusResource);
          EasyMock.reset(statusResource);
        }
        catch (Exception e2)
        {
          fail(e2.toString());
        }
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);
  }

  @Test
  public void testRestLiConfig()
  {
    // #1 test that setters replace entries set
    RestLiConfig config = new RestLiConfig();
    config.setResourcePackageNames("foo,bar,baz");
    assertEquals(3, config.getResourcePackageNamesSet().size());
    config.setResourcePackageNames("foo");
    assertEquals(1, config.getResourcePackageNamesSet().size());
    config.setResourcePackageNames("foo,bar,baz");
    assertEquals(3, config.getResourcePackageNamesSet().size());

    Set<String> packageSet = new HashSet<String>();
    packageSet.add("a");
    packageSet.add("b");
    config.setResourcePackageNamesSet(packageSet);
    assertEquals(2, config.getResourcePackageNamesSet().size());

    // #2 'add' method doesn't replace set, of course
    config.addResourcePackageNames("c", "d");
    assertEquals(4, config.getResourcePackageNamesSet().size());

    // #3 test that 'empty' values are ignored
    config.setResourcePackageNames("");
    assertEquals(4, config.getResourcePackageNamesSet().size());
    config.setResourcePackageNames("   ");
    assertEquals(4, config.getResourcePackageNamesSet().size());
    config.setResourcePackageNames(null);
    assertEquals(4, config.getResourcePackageNamesSet().size());
    config.setResourcePackageNamesSet(Collections.<String>emptySet());
    assertEquals(4, config.getResourcePackageNamesSet().size());
    config.setResourcePackageNamesSet(null);
    assertEquals(4, config.getResourcePackageNamesSet().size());
  }

  @Test
  public void testDebugRequestHandlers() throws URISyntaxException
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1/__debug/a/s")).build();

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        assertEquals(restResponse.getStatus(), 200);
        String responseString = restResponse.getEntity().asString(Charset.defaultCharset());
        Assert.assertEquals(responseString, DEBUG_HANDLER_RESPONSE_A);
      }

      @Override
      public void onError(Throwable e)
      {
        fail();
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);

    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(buildStatusRecord()).once();
    EasyMock.replay(statusResource);

    request = new RestRequestBuilder(new URI("/statuses/1/__debug/b")).build();

    callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        assertEquals(restResponse.getStatus(), 200);
        String responseString = restResponse.getEntity().asString(Charset.defaultCharset());
        Assert.assertEquals(responseString, DEBUG_HANDLER_RESPONSE_B);

        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);
      }

      @Override
      public void onError(Throwable e)
      {
        fail();
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);
  }

  private <R extends BaseResource> R getMockResource(Class<R> resourceClass)
  {
    R resource = _resourceFactory.getMock(resourceClass);
    EasyMock.reset(resource);
    resource.setContext((ResourceContext) EasyMock.anyObject());
    EasyMock.expectLastCall().once();

    return resource;
  }

  private Status buildStatusRecord()
  {
    DataMap map = new DataMap();
    map.put("text", "test status");
    Status status = new Status(map);
    return status;
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  private Object[][] protocolVersions1And2DataProvider()
  {
    return new Object[][] {
        {
            AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
            RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
            AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
            RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        }
    };
  }
}
