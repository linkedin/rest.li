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
import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.MultiPartMIMEStreamRequestFactory;
import com.linkedin.multipart.MultiPartMIMEWriter;
import com.linkedin.multipart.utils.MIMETestUtils.MultiPartMIMEFullReaderCallback;
import com.linkedin.multipart.utils.MIMETestUtils.SinglePartMIMEFullReaderCallback;
import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityReader;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.docgen.DefaultDocumentationRequestHandler;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.AttachmentUtils;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.internal.testutils.RestLiTestAttachmentDataSource;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.test.EasyMockResourceFactory;
import com.linkedin.restli.server.twitter.AsyncStatusCollectionResource;
import com.linkedin.restli.server.twitter.FeedDownloadResource;
import com.linkedin.restli.server.twitter.FeedDownloadResourceReactive;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;


/**
 * "Integration" test that exercises a couple end-to-end use cases
 *
 * @author dellamag
 */
public class TestRestLiServer
{
  private static final String DEBUG_HANDLER_RESPONSE_A = "Response A";
  private static final String DEBUG_HANDLER_RESPONSE_B = "Response B";

  private static final String DOCUMENTATION_RESPONSE = "Documentation Response";

  private RestLiServer _server;
  private RestLiServer _serverWithFilters;
  private RestLiServer _serverWithCustomErrorResponseConfig; // configured different than server
  private EasyMockResourceFactory _resourceFactory;
  private Filter _mockFilter;

  @BeforeTest
  protected void setUp()
  {
    // silence null engine warning and get EasyMock failure if engine is used
    Engine fakeEngine = EasyMock.createMock(Engine.class);
    _mockFilter = EasyMock.createMock(Filter.class);
    setUpServer(fakeEngine);
    setupServerWithFilters(fakeEngine);
    setupServerWithCustomErrorResponseConfig(fakeEngine);
    replay(fakeEngine);
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
    config.addFilter(_mockFilter);
    _serverWithFilters = new RestLiServer(config, _resourceFactory, fakeEngine);
  }

  private void setUpServer(Engine engine)
  {
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.twitter");
    _resourceFactory  = new EasyMockResourceFactory();

    RestLiDebugRequestHandler debugRequestHandlerA = new DebugRequestHandler("a", DEBUG_HANDLER_RESPONSE_A);

    RestLiDebugRequestHandler debugRequestHandlerB = new DebugRequestHandler("b", DEBUG_HANDLER_RESPONSE_B);

    config.addDebugRequestHandlers(debugRequestHandlerA, debugRequestHandlerB);
    _server = new RestLiServer(config, _resourceFactory, engine);
  }

  private enum RestOrStream
  {
    REST,
    STREAM
  }

  @AfterTest
  protected void tearDown()
  {
    _resourceFactory = null;
    _server = null;
    EasyMock.reset(_mockFilter, _mockFilter);
  }

  @AfterMethod
  protected void afterMethod()
  {
    EasyMock.reset(_mockFilter, _mockFilter);
  }

  @DataProvider(name = "validClientProtocolVersionData")
  public Object[][] provideValidClientProtocolVersionData()
  {
    return new Object[][]
        {
            //Rest
            { _server, AllProtocolVersions.BASELINE_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.REST },
            { _server, AllProtocolVersions.LATEST_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.REST },
            { _server, AllProtocolVersions.NEXT_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.REST },
            { _server, AllProtocolVersions.PREVIOUS_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.REST },
            //Stream
            { _server, AllProtocolVersions.BASELINE_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.STREAM },
            { _server, AllProtocolVersions.LATEST_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.STREAM },
            { _server, AllProtocolVersions.NEXT_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.STREAM },
            { _server, AllProtocolVersions.PREVIOUS_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.STREAM }
        };
  }

  @DataProvider(name = "validClientProtocolVersionDataStreamOnly")
  public Object[][] provideValidClientProtocolVersionDataStreamOnly()
  {
    return new Object[][]
        {
            { _server, AllProtocolVersions.BASELINE_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION},
            { _server, AllProtocolVersions.LATEST_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION},
            { _server, AllProtocolVersions.NEXT_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION},
            { _server, AllProtocolVersions.PREVIOUS_PROTOCOL_VERSION, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION}
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
            //Rest
            { _server, greaterThanNext, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.REST },
            { _server, new ProtocolVersion(0, 0, 0), RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.REST },

            //Stream
            { _server, greaterThanNext, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.STREAM },
            { _server, new ProtocolVersion(0, 0, 0), RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, RestOrStream.STREAM }
        };
  }

  @DataProvider(name = "restOrStream")
  public Object[][] restOrStream()
  {
    return new Object[][]
        {
            { RestOrStream.REST },
            { RestOrStream.STREAM }
        };
  }

  @Test
  public void testResourceDefinitionListeners() throws Exception
  {
    int[] counts = new int[2];

    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.twitter");
    config.addResourceDefinitionListener(new ResourceDefinitionListener()
    {
      @Override
      public void onInitialized(Map<String, ResourceDefinition> definitions)
      {
        int resourceCount = 0;
        for (ResourceDefinition definition : definitions.values()) {
          resourceCount = resourceCount + 1 + countSubResources(definition);
        }

        counts[0] = definitions.size();
        counts[1] = resourceCount;
      }
    });

    new RestLiServer(config, new EasyMockResourceFactory(), EasyMock.createMock(Engine.class));

    assertEquals(counts[0], 17);
    assertEquals(counts[1], 24);
  }

  private int countSubResources(ResourceDefinition definition) {
    int count = 0;
    if (definition.hasSubResources()) {
      for (ResourceDefinition subResource : definition.getSubResourceDefinitions().values()) {
        count = count + 1 + countSubResources(subResource);
      }
    }
    return count;
  }

  @Test(dataProvider = "restOrStream")
  public void testServer(final RestOrStream restOrStream) throws Exception
  {
    testValidRequest(_server, null, false, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, restOrStream);
  }

  @Test(dataProvider = "restOrStream")
  public void testServerWithFilters(final RestOrStream restOrStream) throws Exception
  {
    testValidRequest(_serverWithFilters, null, true, RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, restOrStream);
  }

  @Test(dataProvider = "validClientProtocolVersionData")
  public void testValidClientProtocolVersion(RestLiServer server, ProtocolVersion clientProtocolVersion,
                                             String headerConstant, RestOrStream restOrStream) throws URISyntaxException
  {
    testValidRequest(server, clientProtocolVersion, false, headerConstant, restOrStream);
  }

  @Test(dataProvider = "validClientProtocolVersionDataStreamOnly")
  public void testValidReactiveUnstructuredDataRequest(RestLiServer server,
                                                       ProtocolVersion clientProtocolVersion,
                                                       String headerConstant)
    throws URISyntaxException, IOException
  {
    StreamRequest streamRequest = new StreamRequestBuilder(new URI("/reactiveFeedDownloads/1")).setHeader(headerConstant,
                                                                                                  clientProtocolVersion.toString())
                                                                                       .build(EntityStreams.emptyStream());

    final FeedDownloadResourceReactive resource = getMockResource(FeedDownloadResourceReactive.class);
    resource.get(eq(1L));
    EasyMock.expectLastCall().andDelegateTo(new FeedDownloadResourceReactive()).once();
    replay(resource);

    @SuppressWarnings("unchecked")
    Callback<StreamResponse> r2Callback = createMock(Callback.class);
    final Capture<StreamResponse> streamResponse = new Capture<>();
    r2Callback.onSuccess(capture(streamResponse));
    expectLastCall().once();
    replay(r2Callback);

    RequestContext requestContext = new RequestContext();
    server.handleRequest(streamRequest, requestContext, r2Callback);

    verify(resource);
    verify(r2Callback);
    assertNotNull(streamResponse);
    assertEquals(streamResponse.getValue().getHeader(RestConstants.HEADER_CONTENT_TYPE), FeedDownloadResourceReactive.CONTENT_TYPE);
    FullEntityReader fullEntityReader = new FullEntityReader(new Callback<ByteString>() {
      @Override
      public void onError(Throwable e)
      {
        fail("Error inside callback!! Failed to read response data from stream!", e);
      }

      @Override
      public void onSuccess(ByteString result)
      {
        assertEquals(result, FeedDownloadResourceReactive.CONTENT);
      }
    });
    streamResponse.getValue().getEntityStream().setReader(fullEntityReader);
  }

  @Test(dataProvider = "validClientProtocolVersionDataStreamOnly")
  public void testValidUnstructuredDataRequest(RestLiServer server,
                                               ProtocolVersion clientProtocolVersion,
                                               String headerConstant)
    throws URISyntaxException, IOException
  {
    StreamRequest streamRequest = new StreamRequestBuilder(new URI("/feedDownloads/1")).setHeader(headerConstant,
                                                                                                  clientProtocolVersion.toString())
                                                                                       .build(EntityStreams.emptyStream());

    final FeedDownloadResource resource = getMockResource(FeedDownloadResource.class);
    resource.get(eq(1L), anyObject(UnstructuredDataWriter.class));
    EasyMock.expectLastCall().andDelegateTo(new FeedDownloadResource()).once();
    replay(resource);

    @SuppressWarnings("unchecked")
    Callback<StreamResponse> r2Callback = createMock(Callback.class);
    final Capture<StreamResponse> streamResponse = new Capture<>();
    r2Callback.onSuccess(capture(streamResponse));
    expectLastCall().once();
    replay(r2Callback);

    RequestContext requestContext = new RequestContext();
    server.handleRequest(streamRequest, requestContext, r2Callback);

    verify(resource);
    verify(r2Callback);

    assertNotNull(streamResponse);
    assertEquals(streamResponse.getValue().getHeader(RestConstants.HEADER_CONTENT_TYPE), FeedDownloadResource.CONTENT_TYPE);
    FullEntityReader fullEntityReader = new FullEntityReader(new Callback<ByteString>() {
      @Override
      public void onError(Throwable e)
      {
        fail("Error inside callback!! Failed to read response data from stream!", e);
      }

      @Override
      public void onSuccess(ByteString result)
      {
        assertEquals(result.copyBytes(), FeedDownloadResource.CONTENT);
      }
    });
    streamResponse.getValue().getEntityStream().setReader(fullEntityReader);
  }

  @Test(dataProvider = "validClientProtocolVersionDataStreamOnly")
  public void testValidUnstructuredDataRequestMissingHeader(RestLiServer server,
                                                            ProtocolVersion clientProtocolVersion,
                                                            String headerConstant)
    throws URISyntaxException, IOException
  {
    StreamRequest streamRequest = new StreamRequestBuilder(new URI("/feedDownloads/1")).setHeader(headerConstant,
                                                                                                  clientProtocolVersion.toString())
                                                                                       .build(EntityStreams.emptyStream());

    final FeedDownloadResource resource = getMockResource(FeedDownloadResource.class);
    resource.get(eq(1L), anyObject(UnstructuredDataWriter.class));
    EasyMock.expectLastCall().andDelegateTo(new FeedDownloadResource() {
      @Override
      public void get(Long key, UnstructuredDataWriter writer)
      {
        // do nothing here, this should cause error
      }
    }).once();
    replay(resource);

    @SuppressWarnings("unchecked")
    Callback<StreamResponse> r2Callback = createMock(Callback.class);
    r2Callback.onError(anyObject());
    expectLastCall().once();
    replay(r2Callback);

    RequestContext requestContext = new RequestContext();
    server.handleRequest(streamRequest, requestContext, r2Callback);

    verify(resource);
    verify(r2Callback);
  }

  private void testValidRequest(RestLiServer restLiServer, final ProtocolVersion clientProtocolVersion, boolean filters,
                                final String headerConstant, final RestOrStream restOrStream) throws URISyntaxException
  {
    RestRequest request = null;
    StreamRequest streamRequest = null;
    if (clientProtocolVersion != null)
    {
      if (restOrStream == RestOrStream.REST)
      {
        request = new RestRequestBuilder(new URI("/statuses/1")).setHeader(headerConstant, clientProtocolVersion.toString()).build();
      }
      else
      {
        streamRequest = new StreamRequestBuilder(new URI("/statuses/1")).setHeader(headerConstant, clientProtocolVersion.toString()).build(EntityStreams.emptyStream());
      }
    }
    else
    {
      if (restOrStream == RestOrStream.REST)
      {
        request = new RestRequestBuilder(new URI("/statuses/1")).build();
      }
      else
      {
        streamRequest = new StreamRequestBuilder(new URI("/statuses/1")).build(EntityStreams.emptyStream());
      }
    }

    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(buildStatusRecord()).once();
    if (filters)
    {
      _mockFilter.onRequest(anyObject(FilterRequestContext.class));
      EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
      {
        @Override
        public Object answer() throws Throwable
        {
          return CompletableFuture.completedFuture(null);
        }
      }).times(1);

      _mockFilter.onResponse(anyObject(FilterRequestContext.class),
                             anyObject(FilterResponseContext.class));
      EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
      {
        @Override
        public Object answer() throws Throwable
        {
          return CompletableFuture.completedFuture(null);
        }
      }).times(1);
      replay(_mockFilter);
    }
    replay(statusResource);

    final Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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

    if (restOrStream == RestOrStream.REST)
    {
      restLiServer.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      Callback<StreamResponse> streamResponseCallback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          Messages.toRestResponse(streamResponse, new Callback<RestResponse>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestResponse result)
            {
              restResponseCallback.onSuccess(result);
            }
          });
        }

        @Override
        public void onError(Throwable e)
        {
          fail();
        }
      };

      restLiServer.handleRequest(streamRequest, new RequestContext(), streamResponseCallback);
    }
    if (filters)
    {
      EasyMock.verify(_mockFilter, _mockFilter);
    }
  }

  @Test(dataProvider = "invalidClientProtocolVersionData")
  public void testInvalidClientProtocolVersion(RestLiServer server, ProtocolVersion clientProtocolVersion,
                                               String headerConstant, RestOrStream restOrStream) throws URISyntaxException
  {
    testBadRequest(server, clientProtocolVersion, headerConstant, restOrStream);
  }

  private void testBadRequest(RestLiServer restLiServer, final ProtocolVersion clientProtocolVersion, String headerConstant,
                              final RestOrStream restOrStream) throws URISyntaxException
  {
    Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        fail("The request should have failed!");
      }

      @Override
      public void onError(Throwable e)
      {
        assertEquals(((RestException) e).getResponse().getStatus(), 400);
        String expectedErrorMessage = "Rest.li protocol version " + clientProtocolVersion + " used by the client is not supported!";
        assertEquals(e.getCause().getMessage(), expectedErrorMessage);
      }
    };

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request =
          new RestRequestBuilder(new URI("/statuses/1")).setHeader(headerConstant, clientProtocolVersion.toString()).build();

      restLiServer.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      StreamRequest streamRequest =
          new StreamRequestBuilder(new URI("/statuses/1")).setHeader(headerConstant, clientProtocolVersion.toString()).build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          fail("The request should have failed!");
        }

        @Override
        public void onError(Throwable e)
        {
          Messages.toRestException((StreamException)e, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestException result)
            {
              restResponseCallback.onError(result);
            }
          });
        }
      };
      restLiServer.handleRequest(streamRequest, new RequestContext(), callback);
    }
  }

  @SuppressWarnings({"unchecked"})
  @Test(dataProvider = "restOrStream")
  public void testAsyncServer(final RestOrStream restOrStream) throws Exception
  {
    final AsyncStatusCollectionResource statusResource = getMockResource(AsyncStatusCollectionResource.class);

    statusResource.get(eq(1L), EasyMock.<Callback<Status>> anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable {
        Callback<Status> callback = (Callback<Status>) EasyMock.getCurrentArguments()[1];
        Status stat = buildStatusRecord();
        callback.onSuccess(stat);
        return null;
      }
    });
    replay(statusResource);

    final Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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

    if (restOrStream == RestOrStream.REST)
    {
      final RestRequest request = new RestRequestBuilder(new URI("/asyncstatuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString()).build();

      _server.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      final StreamRequest streamRequest = new StreamRequestBuilder(new URI("/asyncstatuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString()).build(EntityStreams.emptyStream());

      final Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          Messages.toRestResponse(streamResponse, new Callback<RestResponse>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestResponse result)
            {
              restResponseCallback.onSuccess(result);
            }
          });
        }

        @Override
        public void onError(Throwable e)
        {
          fail();
        }
      };

      _server.handleRequest(streamRequest, new RequestContext(), callback);
    }
  }

  @Test(dataProvider = "restOrStream")
  public void testSyncNullObject404(final RestOrStream restOrStream) throws Exception
  {
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(null).once();
    replay(statusResource);

    Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString()).build();

      _server.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      StreamRequest streamRequest = new StreamRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString())
          .build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          fail("We should not get a success here. The server should have returned a 404!");
        }

        @Override
        public void onError(Throwable e)
        {
          Messages.toRestException((StreamException) e, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestException result)
            {
              restResponseCallback.onError(result);
            }
          });
        }
      };

      _server.handleRequest(streamRequest, new RequestContext(), callback);
    }
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testPreprocessingError(final ProtocolVersion protocolVersion, final String errorResponseHeaderName,
                                     final RestOrStream restOrStream) throws Exception
  {
    //Bad key type will generate a routing error
    final StatusCollectionResource statusResource = _resourceFactory.getMock(StatusCollectionResource.class);
    replay(statusResource);

    Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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
        RestException restException = (RestException) e;
        RestResponse restResponse = restException.getResponse();

        assertEquals(restResponse.getStatus(), 400);
        assertTrue(restResponse.getEntity().length() > 0);
        assertEquals(restResponse.getHeader(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);

        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);
      }
    };

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/abcd"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString()).build();

      _server.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      StreamRequest streamRequest = new StreamRequestBuilder(new URI("/statuses/abcd"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString())
          .build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          fail();
        }

        @Override
        public void onError(Throwable e)
        {
          Messages.toRestException((StreamException) e, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestException result)
            {
              restResponseCallback.onError(result);
            }
          });
        }
      };

      _server.handleRequest(streamRequest, new RequestContext(), callback);
    }
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testApplicationException(final ProtocolVersion protocolVersion, final String errorResponseHeaderName,
                                       final RestOrStream restOrStream) throws Exception
  {
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andThrow(new RestLiServiceException(
        HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Mock Exception")).once();
    replay(statusResource);

    Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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
        RestException restException = (RestException) e;
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

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString()).build();

      _server.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      StreamRequest streamRequest = new StreamRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString())
          .build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          fail();
        }

        @Override
        public void onError(Throwable e)
        {
          Messages.toRestException((StreamException) e, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestException result)
            {
              restResponseCallback.onError(result);
            }
          });
        }
      };

      _server.handleRequest(streamRequest, new RequestContext(), callback);
    }
  }

  @Test(dataProvider = "restOrStream")
  public void testInternalErrorMessage(final RestOrStream restOrStream) throws Exception
  {
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andThrow(new IllegalArgumentException("oops")).once();
    replay(statusResource);

    Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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
        RestException restException = (RestException) e;
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

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString()).build();

      _server.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      StreamRequest streamRequest = new StreamRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString())
          .build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          fail();
        }

        @Override
        public void onError(Throwable e)
        {
          Messages.toRestException((StreamException) e, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestException result)
            {
              restResponseCallback.onError(result);
            }
          });
        }
      };

      _server.handleRequest(streamRequest, new RequestContext(), callback);
    }
  }

  @Test(dataProvider = "restOrStream")
  public void testCustomizedInternalErrorMessage(final RestOrStream restOrStream) throws Exception
  {
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andThrow(new IllegalArgumentException("oops")).once();
    replay(statusResource);

    Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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
        RestException restException = (RestException) e;
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

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString()).build();

      _serverWithCustomErrorResponseConfig.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      StreamRequest streamRequest = new StreamRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString())
          .build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          fail();
        }

        @Override
        public void onError(Throwable e)
        {
          Messages.toRestException((StreamException) e, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestException result)
            {
              restResponseCallback.onError(result);
            }
          });
        }
      };

      _serverWithCustomErrorResponseConfig.handleRequest(streamRequest, new RequestContext(), callback);
    }
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testMessageAndDetailsErrorFormat(final ProtocolVersion protocolVersion, final String errorResponseHeaderName,
                                               final RestOrStream restOrStream) throws Exception
  {
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    final DataMap details = new DataMap();
    details.put("errorKey", "errorDetail");
    EasyMock.expect(statusResource.get(eq(1L))).andThrow(new RestLiServiceException(
        HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Mock Exception").setErrorDetails(details)).once();
    replay(statusResource);

    Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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
        RestException restException = (RestException) e;
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

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString()).build();

      _serverWithCustomErrorResponseConfig.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      StreamRequest streamRequest = new StreamRequestBuilder(new URI("/statuses/1"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString())
          .build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          fail();
        }

        @Override
        public void onError(Throwable e)
        {
          Messages.toRestException((StreamException) e, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestException result)
            {
              restResponseCallback.onError(result);
            }
          });
        }
      };

      _serverWithCustomErrorResponseConfig.handleRequest(streamRequest, new RequestContext(), callback);
    }
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testPostProcessingException(final ProtocolVersion protocolVersion, final String errorResponseHeaderName,
                                          final RestOrStream restOrStream) throws Exception
  {
    //request for nested projection within string field will generate error
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(buildStatusRecord()).once();
    replay(statusResource);

    Callback<RestResponse> restResponseCallback = new Callback<RestResponse>()
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
        RestException restException = (RestException) e;
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

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/1?fields=text:(invalid)"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString()).build();

      _server.handleRequest(request, new RequestContext(), restResponseCallback);
    }
    else
    {
      StreamRequest streamRequest = new StreamRequestBuilder(new URI("/statuses/1?fields=text:(invalid)"))
          .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString())
          .build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          fail();
        }

        @Override
        public void onError(Throwable e)
        {
          Messages.toRestException((StreamException) e, new Callback<RestException>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestException result)
            {
              restResponseCallback.onError(result);
            }
          });
        }
      };

      _server.handleRequest(streamRequest, new RequestContext(), callback);
    }
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

  @Test(dataProvider =  "restOrStream")
  public void testDebugRequestHandlers(final RestOrStream restOrStream) throws URISyntaxException
  {
    //Without a resource
    final Callback<RestResponse> noResourceRestResponseCallback = new Callback<RestResponse>()
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

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/1/__debug/a/s")).build();

      _server.handleRequest(request, new RequestContext(), noResourceRestResponseCallback);
    }
    else
    {
      StreamRequest request = new StreamRequestBuilder(new URI("/statuses/1/__debug/a/s")).build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          Messages.toRestResponse(streamResponse, new Callback<RestResponse>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestResponse result)
            {
              noResourceRestResponseCallback.onSuccess(result);
            }
          });
        }

        @Override
        public void onError(Throwable e)
        {
          fail();
        }
      };

      _server.handleRequest(request, new RequestContext(), callback);
    }


    //With a resource this time
    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(buildStatusRecord()).once();
    replay(statusResource);

    final Callback<RestResponse> resourceRestResponseCallback = new Callback<RestResponse>()
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

    if (restOrStream == RestOrStream.REST)
    {
      RestRequest request = new RestRequestBuilder(new URI("/statuses/1/__debug/b")).build();

      _server.handleRequest(request, new RequestContext(), resourceRestResponseCallback);
    }
    else
    {
      StreamRequest request = new StreamRequestBuilder(new URI("/statuses/1/__debug/b")).build(EntityStreams.emptyStream());

      Callback<StreamResponse> callback = new Callback<StreamResponse>()
      {
        @Override
        public void onSuccess(StreamResponse streamResponse)
        {
          Messages.toRestResponse(streamResponse, new Callback<RestResponse>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail();
            }

            @Override
            public void onSuccess(RestResponse result)
            {
              resourceRestResponseCallback.onSuccess(result);
            }
          });
        }

        @Override
        public void onError(Throwable e)
        {
          fail();
        }
      };

      _server.handleRequest(request, new RequestContext(), callback);
    }
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  private Object[][] protocolVersions1And2DataProvider()
  {
    return new Object[][] {
        //Rest
        {
            AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
            RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE, RestOrStream.REST
        },
        {
            AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
            RestConstants.HEADER_RESTLI_ERROR_RESPONSE, RestOrStream.REST
        },

        //Stream
        {
            AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
            RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE, RestOrStream.STREAM
        },
        {
            AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
            RestConstants.HEADER_RESTLI_ERROR_RESPONSE, RestOrStream.STREAM
        }
    };
  }

  @Test
  public void testRestRequestAttachmentsPresent() throws Exception
  {
    //This test verifies that a RestRequest sent to the RestLiServer throws an exception if the content type is multipart/related
    RestRequest contentTypeMultiPartRelated = new RestRequestBuilder(new URI("/statuses/abcd"))
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_MULTIPART_RELATED).build();

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

        assertEquals(restResponse.getStatus(), 415);
        assertTrue(restResponse.getEntity().length() > 0);
        assertEquals(restResponse.getEntity().asString(Charset.defaultCharset()), "This server cannot handle requests with a content type of multipart/related");
      }
    };

    _server.handleRequest(contentTypeMultiPartRelated, new RequestContext(), callback);
  }

  @Test
  public void testRestRequestResponseAttachmentsDesired() throws Exception
  {
    //This test verifies that a RestRequest sent to the RestLiServer throws an exception if the accept type
    //includes multipart related
    RestRequest acceptTypeMultiPartRelated = new RestRequestBuilder(new URI("/statuses/abcd"))
        .setHeader(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_MULTIPART_RELATED).build();

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

        assertEquals(restResponse.getStatus(), 406);
        assertTrue(restResponse.getEntity().length() > 0);
        assertEquals(restResponse.getEntity().asString(Charset.defaultCharset()), "This server cannot handle requests with an accept type of multipart/related");
      }
    };

    _server.handleRequest(acceptTypeMultiPartRelated, new RequestContext(), callback);
  }

  @Test
  public void testRestRequestAttemptVerifyParseFailed() throws Exception
  {
    //This test verifies that a RestRequest sent to the RestLiServer throws an exception if the content type or accept types
    //fail to parse properly. This occurs when we try to verify that the request's content type or accept types do
    //not include multipart/related.
    RestRequest invalidContentTypeRequest = new RestRequestBuilder(new URI("/statuses/abcd"))
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, "©").build();

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
        assertEquals(restResponse.getEntity().asString(Charset.defaultCharset()), "Unable to parse content or accept types.");
      }
    };

    _server.handleRequest(invalidContentTypeRequest, new RequestContext(), callback);
  }

  @Test
  public void testStreamRequestMultiplexedRequestMultiPartAcceptType() throws Exception
  {
    //This test verifies that a StreamRequest sent to the RestLiServer throws an exception if the accept type contains
    //multipart/related.
    StreamRequest streamRequestMux = new StreamRequestBuilder(new URI("/mux"))
        .setHeader(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_MULTIPART_RELATED).build(EntityStreams.emptyStream());

    Callback<StreamResponse> callback = new Callback<StreamResponse>()
    {
      @Override
      public void onSuccess(StreamResponse restResponse)
      {
        fail();
      }

      @Override
      public void onError(Throwable e)
      {
        assertTrue(e instanceof StreamException);
        StreamException streamException = (StreamException)e;
        StreamResponse streamResponse = streamException.getResponse();

        assertEquals(streamResponse.getStatus(), 406);
        final FullEntityReader fullEntityReader = new FullEntityReader(new Callback<ByteString>()
        {
          @Override
          public void onError(Throwable e)
          {
            Assert.fail();
          }

          @Override
          public void onSuccess(ByteString result)
          {
            //We have the body so assert
            assertTrue(result.length() > 0);
            assertEquals(result.asString(Charset.defaultCharset()),
                         "This server cannot handle multiplexed requests that have an accept type of multipart/related");
          }
        });
        streamResponse.getEntityStream().setReader(fullEntityReader);
      }
    };

    _server.handleRequest(streamRequestMux, new RequestContext(), callback);
  }

  @Test
  public void testRequestAttachmentsAndResponseAttachments() throws Exception
  {
    //This test verifies the server's ability to accept request attachments and send back response attachments. This is the
    //main test to verify the wire protocol for streaming. We send a payload that contains the rest.li payload and some attachments
    //and we send a response back with a rest.li payload and some attachments.

    //Define the server side resource attachments to be sent back.
    final RestLiResponseAttachments.Builder responseAttachmentsBuilder = new RestLiResponseAttachments.Builder();
    responseAttachmentsBuilder.appendSingleAttachment(new RestLiTestAttachmentDataSource("1",
                                                                                         ByteString.copyString("one", Charset.defaultCharset())));

    Capture<ResourceContext> resourceContextCapture = new Capture<ResourceContext>();
    final AsyncStatusCollectionResource statusResource = getMockResource(AsyncStatusCollectionResource.class,
                                                                         capture(resourceContextCapture));

    statusResource.streamingAction(EasyMock.<String>anyObject(), EasyMock.<RestLiAttachmentReader>anyObject(),
                                   EasyMock.<Callback<Long>> anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        //Verify there are still attachments to be read.
        final RestLiAttachmentReader attachmentReader = (RestLiAttachmentReader)EasyMock.getCurrentArguments()[1];
        Assert.assertFalse(attachmentReader.haveAllAttachmentsFinished());

        //Verify the action param.
        Assert.assertEquals((String)EasyMock.getCurrentArguments()[0], "someMetadata");

        //Set the response attachments
        resourceContextCapture.getValue().setResponseAttachments(responseAttachmentsBuilder.build());

        //Now respond back to the request.
        @SuppressWarnings("unchecked")
        Callback<Long> callback = (Callback<Long>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(1234l);
        return null;
      }
    });
    replay(statusResource);

    //Now we create a multipart/related payload.
    final String payload = "{\"metadata\": \"someMetadata\"}";
    final ByteStringWriter byteStringWriter = new ByteStringWriter(ByteString.copyString(payload, Charset.defaultCharset()));
    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();
    AttachmentUtils.appendSingleAttachmentToBuilder(builder,
                                                    new RestLiTestAttachmentDataSource("2", ByteString.copyString("two", Charset.defaultCharset())));
    final MultiPartMIMEWriter writer = AttachmentUtils.createMultiPartMIMEWriter(byteStringWriter, "application/json", builder);

    final StreamRequest streamRequest =
        MultiPartMIMEStreamRequestFactory.generateMultiPartMIMEStreamRequest(new URI("/asyncstatuses/?action=streamingAction"),
                                                                             "related",
                                                                             writer, Collections.<String, String>emptyMap(),
                                                                             "POST",
                                                                             ImmutableMap.of(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_MULTIPART_RELATED),
                                                                             Collections.emptyList());

    final Callback<StreamResponse> callback = new Callback<StreamResponse>()
    {
      @Override
      public void onSuccess(StreamResponse streamResponse)
      {
        //Before reading the data make sure top level type is multipart/related
        Assert.assertEquals(streamResponse.getStatus(), 200);
        try
        {
          ContentType contentType = new ContentType(streamResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE));
          Assert.assertEquals(contentType.getBaseType(), RestConstants.HEADER_VALUE_MULTIPART_RELATED);
        }
        catch (ParseException parseException)
        {
          Assert.fail();
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        MultiPartMIMEFullReaderCallback fullReaderCallback = new MultiPartMIMEFullReaderCallback(countDownLatch);
        final MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamResponse);
        reader.registerReaderCallback(fullReaderCallback);
        try
        {
          countDownLatch.await(3000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException interruptedException)
        {
          Assert.fail();
        }

        final List<SinglePartMIMEFullReaderCallback> singlePartMIMEReaderCallbacks = fullReaderCallback.getSinglePartMIMEReaderCallbacks();
        Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 2);

        //Verify first part is Action response.
        Assert.assertEquals(singlePartMIMEReaderCallbacks.get(0).getHeaders().get(RestConstants.HEADER_CONTENT_TYPE), RestConstants.HEADER_VALUE_APPLICATION_JSON);
        Assert.assertEquals(singlePartMIMEReaderCallbacks.get(0).getFinishedData().asAvroString(), "{\"value\":1234}");
        //Verify the second part matches what the server should have sent back
        Assert.assertEquals(singlePartMIMEReaderCallbacks.get(1).getHeaders().get(RestConstants.HEADER_CONTENT_ID), "1");
        Assert.assertEquals(singlePartMIMEReaderCallbacks.get(1).getFinishedData().asString(Charset.defaultCharset()), "one");

        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);
      }

      @Override
      public void onError(Throwable e)
      {
        fail();
      }
    };

    _server.handleRequest(streamRequest, new RequestContext(), callback);
  }

  @Test
  public void testMultipartRelatedRequestNoUserAttachments() throws Exception
  {
    //This test verifies the server's ability to handle a multipart related request that has only one part which is
    //the rest.li payload; meaning there are no user defined attachments. Technically the client builders shouldn't do
    //this but we allow this to keep the protocol somewhat flexible.

    final AsyncStatusCollectionResource statusResource = getMockResource(AsyncStatusCollectionResource.class);

    statusResource.streamingAction(EasyMock.<String>anyObject(), EasyMock.<RestLiAttachmentReader>anyObject(),
                                   EasyMock.<Callback<Long>> anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable {
        //Verify there are no attachments.
        final RestLiAttachmentReader attachmentReader = (RestLiAttachmentReader)EasyMock.getCurrentArguments()[1];
        Assert.assertNull(attachmentReader);

        //Verify the action param.
        Assert.assertEquals((String)EasyMock.getCurrentArguments()[0], "someMetadata");

        //Now respond back to the request.
        @SuppressWarnings("unchecked")
        Callback<Long> callback = (Callback<Long>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(1234l);
        return null;
      }
    });
    replay(statusResource);

    //Now we create a multipart/related payload.
    final String payload = "{\"metadata\": \"someMetadata\"}";
    final ByteStringWriter byteStringWriter = new ByteStringWriter(ByteString.copyString(payload, Charset.defaultCharset()));
    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();
    final MultiPartMIMEWriter writer = AttachmentUtils.createMultiPartMIMEWriter(byteStringWriter, "application/json", builder);

    final StreamRequest streamRequest =
        MultiPartMIMEStreamRequestFactory.generateMultiPartMIMEStreamRequest(new URI("/asyncstatuses/?action=streamingAction"),
                                                                             "related",
                                                                             writer, Collections.<String, String>emptyMap(),
                                                                             "POST",
                                                                             ImmutableMap.of(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_MULTIPART_RELATED),
                                                                             Collections.emptyList());

    final Callback<StreamResponse> callback = new Callback<StreamResponse>()
    {
      @Override
      public void onSuccess(StreamResponse streamResponse)
      {
        Messages.toRestResponse(streamResponse, new Callback<RestResponse>()
        {
          @Override
          public void onError(Throwable e)
          {
            Assert.fail();
          }

          @Override
          public void onSuccess(RestResponse result)
          {
            Assert.assertEquals(result.getStatus(), 200);

            try
            {
              ContentType contentType = new ContentType(streamResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE));
              Assert.assertEquals(contentType.getBaseType(), RestConstants.HEADER_VALUE_APPLICATION_JSON);
            }
            catch (ParseException parseException)
            {
              Assert.fail();
            }

            //Verify the response body
            Assert.assertEquals(result.getEntity().asAvroString(), "{\"value\":1234}");

            EasyMock.verify(statusResource);
            EasyMock.reset(statusResource);
          }
        });
      }

      @Override
      public void onError(Throwable e)
      {
        fail();
      }
    };

    _server.handleRequest(streamRequest, new RequestContext(), callback);
  }

  @Test
  public void testMultipartRelatedNoAttachmentsAtAll() throws Exception
  {
    //This test verifies the server's ability to throw an exception if there are absolutely no attachments at all
    //in the request. The protocol allows no user attachments to be required, but there must always be at least a rest.li
    //payload in the first part.

    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();

    final StreamRequest streamRequest =
        MultiPartMIMEStreamRequestFactory.generateMultiPartMIMEStreamRequest(new URI("/doesNotMatter"), "related",
                                                                             builder.build(), Collections.<String, String>emptyMap(),
                                                                             "POST",
                                                                             ImmutableMap.of(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_MULTIPART_RELATED),
                                                                             Collections.emptyList());

    final Callback<StreamResponse> callback = new Callback<StreamResponse>()
    {
      @Override
      public void onSuccess(StreamResponse streamResponse)
      {
        Assert.fail();
      }

      @Override
      public void onError(Throwable e)
      {
        //Verify the exception.
        assertTrue(e instanceof StreamException);
        StreamException streamException = (StreamException)e;
        StreamResponse streamResponse = streamException.getResponse();

        assertEquals(streamResponse.getStatus(), 400);
        final FullEntityReader fullEntityReader = new FullEntityReader(new Callback<ByteString>()
        {
          @Override
          public void onError(Throwable e)
          {
            Assert.fail();
          }

          @Override
          public void onSuccess(ByteString result)
          {
            //We have the body so assert that the exception made it.
            assertTrue(result.length() > 0);
            assertTrue(result.asString(Charset.defaultCharset())
                           .contains("Did not receive any parts in the multipart mime request"));
          }
        });
        streamResponse.getEntityStream().setReader(fullEntityReader);
      }
    };

    _server.handleRequest(streamRequest, new RequestContext(), callback);
  }

  @Test
  public void testRequestAttachmentsResponseAttachmentsException() throws Exception
  {
    //This test verifies the server's behavior in the face of an exception. In this case the resource method
    //threw an exception AFTER setting response attachments. Additionally the resource method failed to absorb any
    //incoming request attachments. We verify in this test that StreamResponseCallbackAdaptor in RestLiServer
    //drains and absorbs all bytes from the incoming request and that the response attachments set by the resource method
    //are told to abort.

    //Define the server side resource attachments to be sent back.
    final RestLiResponseAttachments.Builder responseAttachmentsBuilder = new RestLiResponseAttachments.Builder();
    final RestLiTestAttachmentDataSource toBeAbortedDataSource = RestLiTestAttachmentDataSource.createWithRandomPayload("1");

    responseAttachmentsBuilder.appendSingleAttachment(toBeAbortedDataSource);

    Capture<ResourceContext> resourceContextCapture = new Capture<ResourceContext>();
    final AsyncStatusCollectionResource statusResource = getMockResource(AsyncStatusCollectionResource.class,
                                                                         capture(resourceContextCapture));

    statusResource.streamingAction(EasyMock.<String>anyObject(), EasyMock.<RestLiAttachmentReader>anyObject(),
                                   EasyMock.<Callback<Long>> anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable {
        //Verify there are still attachments to be read.
        final RestLiAttachmentReader attachmentReader = (RestLiAttachmentReader)EasyMock.getCurrentArguments()[1];
        Assert.assertFalse(attachmentReader.haveAllAttachmentsFinished());

        //Verify the action param.
        Assert.assertEquals((String)EasyMock.getCurrentArguments()[0], "someMetadata");

        //Set the response attachments
        resourceContextCapture.getValue().setResponseAttachments(responseAttachmentsBuilder.build());

        //Now throw an exception.
        @SuppressWarnings("unchecked")
        Callback<Long> callback = (Callback<Long>) EasyMock.getCurrentArguments()[2];
        callback.onError(new RestLiServiceException(HttpStatus.S_409_CONFLICT, "Some conflict"));
        return null;
      }
    });
    replay(statusResource);

    //Now we create a multipart/related payload.
    final String payload = "{\"metadata\": \"someMetadata\"}";
    final ByteStringWriter byteStringWriter = new ByteStringWriter(ByteString.copyString(payload, Charset.defaultCharset()));
    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();
    final RestLiTestAttachmentDataSource toBeDrainedDataSource = RestLiTestAttachmentDataSource.createWithRandomPayload("2");

    AttachmentUtils.appendSingleAttachmentToBuilder(builder, toBeDrainedDataSource);
    final MultiPartMIMEWriter writer = AttachmentUtils.createMultiPartMIMEWriter(byteStringWriter, "application/json", builder);

    final StreamRequest streamRequest =
        MultiPartMIMEStreamRequestFactory.generateMultiPartMIMEStreamRequest(new URI("/asyncstatuses/?action=streamingAction"),
                                                                             "related",
                                                                             writer, Collections.<String, String>emptyMap(),
                                                                             "POST",
                                                                             ImmutableMap.of(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_MULTIPART_RELATED),
                                                                             Collections.emptyList());

    final Callback<StreamResponse> callback = new Callback<StreamResponse>()
    {
      @Override
      public void onSuccess(StreamResponse streamResponse)
      {
        fail();
      }

      @Override
      public void onError(Throwable e)
      {
        //Verify the exception.
        assertTrue(e instanceof StreamException);
        StreamException streamException = (StreamException)e;
        StreamResponse streamResponse = streamException.getResponse();

        assertEquals(streamResponse.getStatus(), 409);
        final FullEntityReader fullEntityReader = new FullEntityReader(new Callback<ByteString>()
        {
          @Override
          public void onError(Throwable e)
          {
            Assert.fail();
          }

          @Override
          public void onSuccess(ByteString result)
          {
            //We have the body so assert exception made it.
            assertTrue(result.length() > 0);
            assertTrue(result.asString(Charset.defaultCharset()).contains("Some conflict"));
          }
        });
        streamResponse.getEntityStream().setReader(fullEntityReader);

        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);
      }
    };

    _server.handleRequest(streamRequest, new RequestContext(), callback);

    //Verify that the request level attachments were drained.
    Assert.assertTrue(toBeDrainedDataSource.finished());

    //Verify that response attachments were told to abort.
    Assert.assertTrue(toBeAbortedDataSource.dataSourceAborted());
  }

  @Test(dataProvider = "requestHandlersData")
  public void testRequestHandlers(URI uri, String expectedResponse)
  {
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.twitter");
    config.setDocumentationRequestHandler(new DefaultDocumentationRequestHandler() {
      @Override
      public void initialize(RestLiConfig config, Map<String, ResourceModel> rootResources) {/* no-op */}
      @Override
      public void handleRequest(RestRequest req, RequestContext ctx, Callback<RestResponse> cb)
      {
        cb.onSuccess(new RestResponseBuilder().setEntity(toByteString(DOCUMENTATION_RESPONSE)).build());
      }
    });
    config.addDebugRequestHandlers(new DebugRequestHandler("a", DEBUG_HANDLER_RESPONSE_A),
        new DebugRequestHandler("b", DEBUG_HANDLER_RESPONSE_B));

    RestLiServer server = new RestLiServer(config, new EasyMockResourceFactory(), createMock(Engine.class));

    RestRequest restReq = new RestRequestBuilder(uri).build();
    server.handleRequest(restReq, createMock(RequestContext.class), new RestResponseAssertionCallback(expectedResponse));

    StreamRequest streamReq = new StreamRequestBuilder(uri).build(EntityStreams.emptyStream());
    server.handleRequest(streamReq, createMock(RequestContext.class), new StreamResponseAssertionCallback(expectedResponse));
  }

  @DataProvider
  public Object[][] requestHandlersData() throws URISyntaxException
  {
    return new Object[][] {
        new Object[] {new URI("profiles/__debug/a/1"), DEBUG_HANDLER_RESPONSE_A},
        new Object[] {new URI("profiles/__debug/b/1"), DEBUG_HANDLER_RESPONSE_B},
        new Object[] {new URI("profiles/restli/docs"), DOCUMENTATION_RESPONSE},
    };
  }

  private static class RestResponseAssertionCallback implements Callback<RestResponse>
  {
    private final String _expectedResponse;

    RestResponseAssertionCallback(String expectedResponse)
    {
      _expectedResponse = expectedResponse;
    }

    @Override
    public void onError(Throwable e)
    {
      fail();
    }

    @Override
    public void onSuccess(RestResponse result)
    {
      assertEquals(fromByteString(result.getEntity()), _expectedResponse);
    }
  }

  private static class StreamResponseAssertionCallback implements Callback<StreamResponse>
  {
    private final String _expectedResponse;

    StreamResponseAssertionCallback(String expectedResponse)
    {
      _expectedResponse = expectedResponse;
    }

    @Override
    public void onError(Throwable e)
    {
      fail();
    }

    @Override
    public void onSuccess(StreamResponse result)
    {
      Messages.toRestResponse(result, new RestResponseAssertionCallback(_expectedResponse));
    }
  }

  private static class DebugRequestHandler implements RestLiDebugRequestHandler
  {
    private final String _handlerId;
    private final String _debugHandlerResponse;

    private DebugRequestHandler(String handlerId, String debugHandlerResponse)
    {
      _handlerId = handlerId;
      _debugHandlerResponse = debugHandlerResponse;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleRequest(final RestRequest request,
        final RequestContext context,
        final RestLiDebugRequestHandler.ResourceDebugRequestHandler resourceRequestHandler,
        final RestLiAttachmentReader attachmentReader,
        final RequestExecutionCallback<RestResponse> callback)
    {
      resourceRequestHandler.handleRequest(request,
          context,
          EasyMock.createMock(RequestExecutionCallback.class));
      handleRequestWithCustomResponse(callback, _debugHandlerResponse);
    }

    @Override
    public String getHandlerId()
    {
      return _handlerId;
    }

    private void handleRequestWithCustomResponse(final RequestExecutionCallback<RestResponse> callback, final String response)
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
      callback.onSuccess(responseBuilder.build(), null, null);
    }
  }

  private static ByteString toByteString(String s)
  {
    return ByteString.copyString(s, Charsets.UTF_8);
  }

  private static String fromByteString(ByteString bs)
  {
    return bs.asString(Charsets.UTF_8);
  }

  private <R extends BaseResource> R getMockResource(Class<R> resourceClass)
  {
    return getMockResource(resourceClass, anyObject());
  }

  private <R extends BaseResource> R getMockResource(Class<R> resourceClass, ResourceContext resourceContext)
  {
    R resource = _resourceFactory.getMock(resourceClass);
    EasyMock.reset(resource);
    resource.setContext(resourceContext);
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
}
