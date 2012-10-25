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

package com.linkedin.restli.server.test;

import static org.easymock.EasyMock.eq;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.linkedin.parseq.Engine;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.linkedin.data.DataMap;
import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.twitter.AsyncStatusCollectionResource;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

/**
 * "Integration" test that exercises a couple end-to-end use cases
 *
 * @author dellamag
 */
public class TestRestLiServer
{
  private RestLiServer _server;
  private EasyMockResourceFactory _resourceFactory;

  @BeforeTest
  protected void setUp()
  {
    RestLiConfig config = new RestLiConfig();
    config.addResourcePackageNames("com.linkedin.restli.server.twitter");
    _resourceFactory  = new EasyMockResourceFactory();
    // silence null engine warning and get EasyMock failure if engine is used
    Engine fakeEngine = EasyMock.createMock(Engine.class);
    EasyMock.replay(fakeEngine);
    _server = new RestLiServer(config, _resourceFactory, fakeEngine);
  }

  @AfterTest
  protected void tearDown()
  {
    _resourceFactory = null;
    _server = null;
  }

  @Test
  public void testServer() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1")).build();

    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(buildStatusRecord()).once();
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
      }

      @Override
      public void onError(Throwable e)
      {
        fail();
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);
  }

  @SuppressWarnings({"unchecked"})
  @Test
  public void testAsyncServer() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/asyncstatuses/1")).build();

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
        @SuppressWarnings("unchecked")
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
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1")).build();

    final StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(null).once();
    EasyMock.replay(statusResource);

    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onSuccess(RestResponse restResponse)
      {
        assertEquals(restResponse.getStatus(), 404);

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

  @Test
  public void testPreprocessingError() throws Exception
  {
    //Bad key type will generate a routing error
    RestRequest request = new RestRequestBuilder(new URI("/statuses/abcd")).build();
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
        assertEquals(restResponse.getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE), RestConstants.HEADER_VALUE_ERROR_PREPROCESSING);

        EasyMock.verify(statusResource);
        EasyMock.reset(statusResource);
      }
    };

    _server.handleRequest(request, new RequestContext(), callback);
  }

  @Test
  public void testApplicationException() throws Exception
  {
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1")).build();
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
          assertEquals(restResponse.getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE), RestConstants.HEADER_VALUE_ERROR_APPLICATION);
          ErrorResponse responseBody = DataMapUtils.read(restResponse.getEntity().asInputStream(), ErrorResponse.class);
          assertEquals(responseBody.getMessage(), "Mock Exception");

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
  public void testPostProcessingException() throws Exception
  {
    //request for nested projection within string field will generate error
    RestRequest request = new RestRequestBuilder(new URI("/statuses/1?fields=text:(invalid)")).build();
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
          assertEquals(restResponse.getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE), RestConstants.HEADER_VALUE_ERROR_POSTPROCESSING);

          EasyMock.verify(statusResource);
          EasyMock.reset(statusResource);
        }
        catch (Exception e2)
        {
          fail(e2.toString());
        }
      }
    };

    //This test generates a server exception which is logged within rest.li.
    //Suppress logging for this test case, to avoid noise in build log
    Level originalLevel = Logger.getRootLogger().getLevel();
    Logger.getRootLogger().setLevel(Level.OFF);

    try
    {
      _server.handleRequest(request, new RequestContext(), callback);
    }
    finally
    {
      Logger.getRootLogger().setLevel(originalLevel);
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
  
  @SuppressWarnings("unchecked")
  private <R extends BaseResource> R getMockResource(Class<? extends BaseResource> resourceClass)
  {
    BaseResource resource = _resourceFactory.getMock(resourceClass);
    EasyMock.reset(resource);
    resource.setContext((ResourceContext)EasyMock.anyObject());
    EasyMock.expectLastCall().once();

    @SuppressWarnings("unchecked")
    final R r = (R) resource;
    return r;
  }

  private Status buildStatusRecord()
  {
    DataMap map = new DataMap();
    map.put("text", "test status");
    Status status = new Status(map);
    return status;
  }
}
