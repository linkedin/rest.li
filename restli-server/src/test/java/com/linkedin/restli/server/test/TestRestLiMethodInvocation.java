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


import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.data.transform.patch.request.PatchOpFactory;
import com.linkedin.data.transform.patch.request.PatchTree;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.PromiseListener;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.parseq.trace.Trace;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.PathSegment;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.MutablePathKeys;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.RestLiCallback;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.filter.FilterChainDispatcher;
import com.linkedin.restli.internal.server.filter.FilterChainDispatcherImpl;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.internal.server.response.ResponseUtils;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.filter.FilterChainCallback;
import com.linkedin.restli.internal.server.filter.FilterChainCallbackImpl;
import com.linkedin.restli.internal.server.filter.FilterRequestContextInternalImpl;
import com.linkedin.restli.internal.server.filter.RestLiFilterChain;
import com.linkedin.restli.internal.server.filter.RestLiFilterResponseContextFactory;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiRequestDataImpl;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.combined.CombinedResources;
import com.linkedin.restli.server.combined.CombinedTestDataModels;
import com.linkedin.restli.server.config.ResourceMethodConfig;
import com.linkedin.restli.server.config.ResourceMethodConfigImpl;
import com.linkedin.restli.server.config.ResourceMethodConfigProvider;
import com.linkedin.restli.server.config.RestLiMethodConfig;
import com.linkedin.restli.server.config.RestLiMethodConfigBuilder;
import com.linkedin.restli.server.custom.types.CustomLong;
import com.linkedin.restli.server.custom.types.CustomString;
import com.linkedin.restli.server.filter.Filter;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.test.EasyMockUtils.Matchers;
import com.linkedin.restli.server.twitter.AsyncDiscoveredItemsResource;
import com.linkedin.restli.server.twitter.AsyncFollowsAssociativeResource;
import com.linkedin.restli.server.twitter.AsyncLocationResource;
import com.linkedin.restli.server.twitter.AsyncRepliesCollectionResource;
import com.linkedin.restli.server.twitter.AsyncStatusCollectionResource;
import com.linkedin.restli.server.twitter.CustomStatusCollectionResource;
import com.linkedin.restli.server.twitter.DiscoveredItemsResource;
import com.linkedin.restli.server.twitter.FollowsAssociativeResource;
import com.linkedin.restli.server.twitter.LocationResource;
import com.linkedin.restli.server.twitter.PromiseDiscoveredItemsResource;
import com.linkedin.restli.server.twitter.PromiseFollowsAssociativeResource;
import com.linkedin.restli.server.twitter.PromiseLocationResource;
import com.linkedin.restli.server.twitter.PromiseRepliesCollectionResource;
import com.linkedin.restli.server.twitter.PromiseStatusCollectionResource;
import com.linkedin.restli.server.twitter.RepliesCollectionResource;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TaskStatusCollectionResource;
import com.linkedin.restli.server.twitter.TwitterAccountsResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItem;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItemKey;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItemKeyParams;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Followed;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Location;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.StatusType;

import com.google.common.collect.Sets;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModel;
import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModels;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;


/**
 * @author dellamag
 */
public class TestRestLiMethodInvocation
{
  private static final ProtocolVersion version = AllProtocolVersions.NEXT_PROTOCOL_VERSION;
  public static final String ATTRIBUTE_PARSEQ_TRACE = "parseqTrace";

  private ScheduledExecutorService _scheduler;
  private Engine _engine;
  private EasyMockResourceFactory _resourceFactory;
  private ErrorResponseBuilder _errorResponseBuilder;
  private MethodAdapterRegistry _methodAdapterRegistry;
  private RestLiMethodInvoker _invoker;

  @BeforeTest
  protected void setUp()
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    _engine = new EngineBuilder()
        .setTaskExecutor(_scheduler)
        .setTimerScheduler(_scheduler)
        .build();

    _resourceFactory  = new EasyMockResourceFactory();

    _errorResponseBuilder = new ErrorResponseBuilder();
    _methodAdapterRegistry = new MethodAdapterRegistry(_errorResponseBuilder);

    // Add filters to the invoker.
    _invoker = new RestLiMethodInvoker(_resourceFactory, _engine, ErrorResponseBuilder.DEFAULT_INTERNAL_ERROR_MESSAGE);
  }

  @AfterTest
  protected void tearDown()
  {
    _resourceFactory = null;
    _invoker = null;
    _engine.shutdown();
    _engine = null;
    _scheduler.shutdownNow();
    _scheduler = null;
  }

  @DataProvider(name = "provideFilterConfig")
  private Object[][] provideFilterConfig()
  {
    return new Object[][] { { true }, { false } };
  }

  @Test(dataProvider = "provideFilterConfig")
  public void testInvokerWithFilters(final boolean throwExceptionFromFirstFilter) throws Exception
  {
    RestLiArgumentBuilder mockArgumentBuilder = createMock(RestLiArgumentBuilder.class);
    Filter mockFilter = createMock(Filter.class);
    @SuppressWarnings("unchecked")
    Callback<Object> mockCallback = createMock(Callback.class);
    FilterRequestContext mockFilterContext = createMock(FilterRequestContext.class);
    RestLiRequestData requestData = new RestLiRequestDataImpl.Builder().key("Key").build();
    RestLiMethodInvoker invokerWithFilters =
        new RestLiMethodInvoker(_resourceFactory, _engine, ErrorResponseBuilder.DEFAULT_INTERNAL_ERROR_MESSAGE);
    Map<String, ResourceModel> resourceModelMap =
        buildResourceModels(StatusCollectionResource.class, LocationResource.class, DiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/statuses");
    ResourceMethodDescriptor resourceMethodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    final StatusCollectionResource resource = getMockResource(StatusCollectionResource.class);
    RestRequestBuilder builder =
        new RestRequestBuilder(new URI("/statuses/1")).setMethod("GET")
            .addHeaderValue("Accept", "application/json")
            .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
                       AllProtocolVersions.LATEST_PROTOCOL_VERSION.toString());
    RestRequest request = builder.build();
    RoutingResult routingResult =
        new RoutingResult(new ResourceContextImpl(buildPathKeys("statusID", 1L), request, new RequestContext()),
                          resourceMethodDescriptor);

    expectLastCall();

    FilterChainDispatcher filterChainDispatcher = new FilterChainDispatcher()
    {
      @Override
      public void onRequestSuccess(RestLiRequestData requestData, RestLiCallback restLiCallback)
      {
        // only invoke if filter chain's requests were successful
        invokerWithFilters.invoke(requestData, routingResult, mockArgumentBuilder, restLiCallback);
      }
    };
    FilterChainCallback filterChainCallback = new FilterChainCallback()
    {
      @Override
      public void onResponseSuccess(RestLiResponseData<?> responseData)
      {
        // unused
      }
      @Override
      public void onError(Throwable th, RestLiResponseData<?> responseData)
      {
        // unused
      }
    };

    final Exception exFromFilter = new RuntimeException("Exception from filter!");
    if (throwExceptionFromFirstFilter)
    {
      mockFilter.onRequest(eq(mockFilterContext));
      expectLastCall().andThrow(exFromFilter);
      mockCallback.onError(eq(exFromFilter));
    }
    else
    {
      expect(mockFilterContext.getRequestData()).andReturn(requestData).times(3);
      mockFilter.onRequest(eq(mockFilterContext));
      expectLastCall().andAnswer(new IAnswer<Object>()
      {
        @Override
        public Object answer() throws Throwable
        {
          FilterRequestContext filterContext = (FilterRequestContext) getCurrentArguments()[0];
          RestLiRequestData data = filterContext.getRequestData();
          // Verify incoming data.
          assertEquals(data.getKey(), "Key");
          // Update data.
          data.setKey("Key-Filter1");
          return CompletableFuture.completedFuture(null);
        }
      }).andAnswer(new IAnswer<Object>()
      {
        @Override
        public Object answer() throws Throwable
        {
          FilterRequestContext filterContext = (FilterRequestContext) getCurrentArguments()[0];
          RestLiRequestData data = filterContext.getRequestData();
          // Verify incoming data.
          assertEquals(data.getKey(), "Key-Filter1");
          // Update data.
          data.setKey("Key-Filter2");
          return CompletableFuture.completedFuture(null);
        }
      });
      Long[] argsArray = { 1L };
      expect(mockArgumentBuilder.buildArguments(requestData, routingResult)).andReturn(argsArray);
      expect(resource.get(eq(1L))).andReturn(null).once();
      mockCallback.onSuccess(eq(null));
    }
    replay(resource, mockArgumentBuilder, mockFilterContext, mockFilter, mockCallback);

    RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
        Collections.emptySet(),
        routingResult.getContext());

    RestLiFilterChain filterChain = new RestLiFilterChain(Arrays.asList(mockFilter, mockFilter), filterChainDispatcher,
        filterChainCallback);
    filterChain.onRequest(mockFilterContext,
                          new RestLiFilterResponseContextFactory(request, routingResult, new RestLiResponseHandler()));

    verifyRecording(mockArgumentBuilder, mockFilterContext, mockFilter);
    if (throwExceptionFromFirstFilter)
    {
      assertEquals(requestData.getKey(), "Key");
    }
    else
    {
      assertEquals(requestData.getKey(), "Key-Filter2");
      verify(resource);
    }
    EasyMock.reset(resource);
    EasyMock.makeThreadSafe(resource, true);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAsyncGet() throws Exception
  {
    AsyncStatusCollectionResource statusResource;
    AsyncLocationResource locationResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    Map<String, ResourceModel> resourceModelMap = buildResourceModels(AsyncStatusCollectionResource.class,
                                                                      AsyncLocationResource.class,
                                                                      AsyncDiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/asyncstatuses");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("asynclocation");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/asyncdiscovereditems");

    ResourceMethodDescriptor methodDescriptor;
    RestLiCallback callback = getCallback();

    methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.getPublicTimeline(EasyMock.anyObject(), EasyMock.anyObject());
    // the goal of below lines is that to make sure that we are getting callback in the resource
    //an callback is called without any problem
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        Callback<List<Status>> callback = (Callback<List<Status>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncstatuses?q=public_timeline",
                         null);

    // #3: get
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.get(eq(1L), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Status> callback = (Callback<Status>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncstatuses/1",
                         buildPathKeys("statusID", 1L));

    // #4: get on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.GET);
    locationResource = getMockResource(AsyncLocationResource.class);
    locationResource.get(EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Location> callback = (Callback<Location>) EasyMock.getCurrentArguments()[0];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(locationResource);
    checkAsyncInvocation(locationResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncstatuses/1/asynclocation",
                         buildPathKeys("statusID", 1L));

    // #5: get on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.GET);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);

    discoveredItemsResource.get(eq(key), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<List<DiscoveredItem>> callback = (Callback<List<DiscoveredItem>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });

    EasyMock.replay(discoveredItemsResource);
    checkAsyncInvocation(discoveredItemsResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncdiscovereditems/(itemId:1,type:2,userId:3)",
                         buildPathKeys("asyncDiscoveredItemId", key));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinder")
  @SuppressWarnings("unchecked")
  public void testAsyncFinder(ProtocolVersion version, String query) throws Exception
  {
    RestLiCallback callback = getCallback();
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("search");
    AsyncStatusCollectionResource statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.search(EasyMock.anyObject(), eq("linkedin"), eq(1L),
                          eq(StatusType.REPLY), (Callback<List<Status>>) EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<List<Status>> callback = (Callback<List<Status>>) EasyMock.getCurrentArguments()[4];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET", version, "/asyncstatuses" + query, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinderOptionalParam")
  public void testAsyncFinderOptionalParam(ProtocolVersion version, String query) throws Exception
  {
    RestLiCallback callback = getCallback();
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("search");
    AsyncStatusCollectionResource statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.search(EasyMock.anyObject(), eq("linkedin"), eq(-1L), eq(null),
                          EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<List<Status>> callback = (Callback<List<Status>>) EasyMock.getCurrentArguments()[4];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET", version, "/asyncstatuses" + query, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinderOptionalBooleanParam")
  @SuppressWarnings("unchecked")
  public void testAsyncFinderOptionalBooleanParam(ProtocolVersion version, String query) throws Exception
  {
    RestLiCallback callback = getCallback();
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("user_timeline");
    AsyncStatusCollectionResource statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.getUserTimeline(EasyMock.anyObject(), eq(false),
                                   EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        @SuppressWarnings("unchecked")
        Callback<List<Status>> callback = (Callback<List<Status>>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET", version, "/asyncstatuses" + query, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "discoveredItemsFinder")
  public void testAsyncFinderOnComplexKey(ProtocolVersion version, String query) throws Exception
  {
    RestLiCallback callback = getCallback();
    ResourceModel discoveredItemsResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    ResourceMethodDescriptor methodDescriptor = discoveredItemsResourceModel.findFinderMethod("user");
    AsyncDiscoveredItemsResource discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    discoveredItemsResource.getDiscoveredItemsForUser(
        EasyMock.anyObject(), eq(1L), EasyMock.anyObject());

    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        @SuppressWarnings("unchecked")
        Callback<List<DiscoveredItem>> callback = (Callback<List<DiscoveredItem>>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });

    EasyMock.replay(discoveredItemsResource);
    checkAsyncInvocation(discoveredItemsResource, callback, methodDescriptor, "GET", version, "/asyncdiscovereditems" + query, null);
  }

  @Test
  public void testAsyncGetAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    // #1: get
    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.GET);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    resource.get(key, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Followed> callback = (Callback<Followed>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(resource);
    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncfollows/(followerID:1,followeeID:2)",
                         buildPathKeys("followerID", 1L, "followeeID", 2L,
                                       followsResourceModel.getKeyName(), rawKey));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAsyncBatchGet() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    ResourceModel followsAssociationResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);

    RestLiCallback callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;
    AsyncFollowsAssociativeResource followsResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    // #1 Batch get on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_GET);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.batchGet((Set<Long>)Matchers.eqCollectionUnordered(Sets.newHashSet(1L, 2L, 3L)),
                            EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Map<Long, Status>> callback = (Callback<Map<Long, Status>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncstatuses?ids=List(1,2,3)",
                         buildBatchPathKeys(1L, 2L, 3L));

    // #2 Batch get on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.BATCH_GET);
    followsResource = getMockResource(AsyncFollowsAssociativeResource.class);

    Set<CompoundKey> expectedKeys = new HashSet<CompoundKey>();
    CompoundKey key1 = new CompoundKey();
    key1.append("followeeID", 1L);
    key1.append("followerID", 1L);
    expectedKeys.add(key1);
    CompoundKey key2 = new CompoundKey();
    key2.append("followeeID", 2L);
    key2.append("followerID", 2L);
    expectedKeys.add(key2);

    followsResource.batchGet((Set<CompoundKey>) Matchers.eqCollectionUnordered(expectedKeys),
                             (Callback<Map<CompoundKey, Followed>>) EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Map<CompoundKey, Followed>> callback = (Callback<Map<CompoundKey, Followed>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(followsResource);

    checkAsyncInvocation(followsResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncfollows?ids=List((followeeID:1,followerID:1),(followeeID:2,followerID:2))",
                         buildBatchPathKeys(key1, key2));

    // #3 Batch get on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_GET);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    @SuppressWarnings("unchecked")
    Set<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>> set =
        (Set<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>>)
            Matchers.eqCollectionUnordered(Sets.newHashSet(keyA, keyB));

    discoveredItemsResource.batchGet(
        set,
        EasyMock.anyObject());

    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Map<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback =
            (Callback<Map<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });

    EasyMock.replay(discoveredItemsResource);

    checkAsyncInvocation(discoveredItemsResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncdiscovereditems?ids=List((userId:3,type:2,itemId:1),(itemId:4,type:5,userId:6))",
                         buildBatchPathKeys(keyA, keyB));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAsyncPost() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        AsyncStatusCollectionResource.class,
        AsyncRepliesCollectionResource.class,
        AsyncLocationResource.class,
        AsyncDiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/asyncstatuses");
    ResourceModel repliesResourceModel = statusResourceModel.getSubResource("asyncreplies");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("asynclocation");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/asyncdiscovereditems");
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;
    AsyncRepliesCollectionResource repliesResource;
    AsyncLocationResource locationResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    // #1
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.CREATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.create(EasyMock.anyObject(), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<CreateResponse> callback = (Callback<CreateResponse>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         "/asyncstatuses",
                         "{}",
                         null);

    // #1.1: different endpoint
    methodDescriptor = repliesResourceModel.findMethod(ResourceMethod.CREATE);
    repliesResource = getMockResource(AsyncRepliesCollectionResource.class);
    repliesResource.create(EasyMock.anyObject(), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        Callback<CreateResponse> callback = (Callback<CreateResponse>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(repliesResource);
    checkAsyncInvocation(repliesResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         "/asyncstatuses/1/replies",
                         "{}",
                         buildPathKeys("statusID", 1L));

    // #2: Collection Partial Update
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(42));
    PatchRequest<Status> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    statusResource.update(eq(1L), eq(expected), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         "/asyncstatuses/1",
                         "{\"patch\":{\"$set\":{\"foo\":42}}}",
                         buildPathKeys("statusID", 1L));

    // #3: Simple Resource Partial Update
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    locationResource = getMockResource(AsyncLocationResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(51));
    PatchRequest<Location> expectedLocation = PatchRequest.createFromPatchDocument(p.getDataMap());
    locationResource.update(eq(expectedLocation), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(locationResource);
    checkAsyncInvocation(locationResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         "/asyncstatuses/1/asynclocation",
                         "{\"patch\":{\"$set\":{\"foo\":51}}}",
                         buildPathKeys("statusID", 1L));

    // #4 Complex-key resource create
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.CREATE);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    discoveredItemsResource.create(EasyMock.anyObject(),
                                   EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<CreateResponse> callback = (Callback<CreateResponse>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });

    EasyMock.replay(discoveredItemsResource);
    checkAsyncInvocation(discoveredItemsResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         "/asyncdiscovereditems",
                         "{}",
                         null);

    // #5 Partial update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(43));
    PatchRequest<DiscoveredItem> expectedDiscoveredItem =
        PatchRequest.createFromPatchDocument(p.getDataMap());
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);

    discoveredItemsResource.update(eq(key), eq(expectedDiscoveredItem), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<CreateResponse> callback = (Callback<CreateResponse>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });

    EasyMock.replay(discoveredItemsResource);
    checkAsyncInvocation(discoveredItemsResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         "/asyncdiscovereditems/(itemId:1,type:2,userId:3)",
                         "{\"patch\":{\"$set\":{\"foo\":43}}}",
                         buildPathKeys("asyncDiscoveredItemId", key));
  }

  @Test
  public void testAsyncBatchCreate() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    BatchCreateRequest<Long, Status> mockBatchCreateReq = EasyMock.anyObject();
    statusResource.batchCreate(mockBatchCreateReq, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<BatchCreateResult<Long, Status>> callback =
            (Callback<BatchCreateResult<Long, Status>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);

    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         "/asyncstatuses",
                         "{}",
                         null);
  }

  @Test
  public void testAsyncBatchDelete() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    BatchDeleteRequest<Long, Status> mockBatchDeleteReq = EasyMock.anyObject();
    statusResource.batchDelete(mockBatchDeleteReq, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<BatchCreateResult<Long, Status>> callback =
            (Callback<BatchCreateResult<Long, Status>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);

    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "DELETE",
                         version, "/asyncstatuses?ids=List(1,2,3)",
                         buildBatchPathKeys(1L, 2L, 3L));
  }

  @Test
  public void testAsyncBatchUpdate() throws Exception
  {

    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    BatchUpdateRequest<Long, Status> mockBatchUpdateReq = EasyMock.anyObject();
    statusResource.batchUpdate(mockBatchUpdateReq, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<BatchCreateResult<Long, Status>> callback =
            (Callback<BatchCreateResult<Long, Status>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);

    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         version, "/asyncstatuses?ids=List(1,2,3)",
                         "{\"entities\": {\"1\": {}, \"2\": {}, \"3\": {}}}",
                         buildBatchPathKeys(1L, 2L, 3L));
  }

  @Test
  public void testAsyncBatchPatch() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    BatchPatchRequest<Long, Status> mockBatchPatchReq =  EasyMock.anyObject();
    statusResource.batchUpdate(mockBatchPatchReq, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<BatchCreateResult<Long, Status>> callback =
            (Callback<BatchCreateResult<Long, Status>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);

    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version, "/asyncstatuses?ids=List(1,2,3)",
                         "{\"entities\": {\"1\": {}, \"2\": {}, \"3\": {}}}",
                         buildBatchPathKeys(1L, 2L, 3L));
  }

  @Test
  public void testAsyncGetAll() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET_ALL);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    PagingContext mockCtx = EasyMock.anyObject();
    statusResource.getAll(mockCtx, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<List<Status>> callback =
            (Callback<List<Status>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);

    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version, "/asyncstatuses",
                         "{}",
                         null);
  }


  @SuppressWarnings("unchecked")
  @Test
  public void testAsyncPut() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(AsyncStatusCollectionResource.class,
                                                                      AsyncLocationResource.class,
                                                                      AsyncDiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/asyncstatuses");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("asynclocation");
    ResourceModel followsAssociationResourceModel = buildResourceModel(
        AsyncFollowsAssociativeResource.class);
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/asyncdiscovereditems");

    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;
    AsyncFollowsAssociativeResource followsResource;
    AsyncLocationResource locationResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    // #1 Update on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.UPDATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    long id = eq(1L);
    Status status  =EasyMock.anyObject();
    statusResource.update(id, status, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         version,
                         "/asyncstatuses/1",
                         "{}",
                         buildPathKeys("statusID", 1L));

    // #2 Update on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.UPDATE);
    followsResource = getMockResource(AsyncFollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    Followed followed = EasyMock.anyObject();
    followsResource.update(key, followed, (Callback<UpdateResponse>) EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(followsResource);
    checkAsyncInvocation(followsResource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         version,
                         "/asyncfollows/(followerID:1,followeeID:2)",
                         "{}",
                         buildPathKeys("followerID", 1L, "followeeID", 2L, followsAssociationResourceModel.getKeyName(), rawKey));

    // #3 Update on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.UPDATE);
    locationResource = getMockResource(AsyncLocationResource.class);
    Location location  = EasyMock.anyObject();
    locationResource.update(location, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(locationResource);
    checkAsyncInvocation(locationResource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         version,
                         "/asyncstatuses/1/asynclocation",
                         "{}",
                         buildPathKeys("statusID", 1L));

    // #4 Update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.UPDATE);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> complexKey =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    discoveredItemsResource.update(eq(complexKey),
                                   EasyMock.anyObject(DiscoveredItem.class),
                                   EasyMock.anyObject());

    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });

    EasyMock.replay(discoveredItemsResource);
    checkAsyncInvocation(discoveredItemsResource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         version,
                         "/asyncdiscovereditems/(itemId:1,type:2,userId:3)",
                         "{}",
                         buildPathKeys("asyncDiscoveredItemId", complexKey));
  }

  @Test
  public void testAsyncDelete() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(AsyncStatusCollectionResource.class,
                                                                      AsyncLocationResource.class,
                                                                      AsyncDiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/asyncstatuses");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("asynclocation");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/asyncdiscovereditems");

    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;
    AsyncLocationResource locationResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    // #1 Delete on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.DELETE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.delete(eq(1L), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource,
                         callback,
                         methodDescriptor,
                         "DELETE",
                         version,
                         "/asyncstatuses/1",
                         buildPathKeys("statusID", 1L));

    // #2 Delete on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.DELETE);
    locationResource = getMockResource(AsyncLocationResource.class);
    locationResource.delete(EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[0];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(locationResource);
    checkAsyncInvocation(locationResource,
                         callback,
                         methodDescriptor,
                         "DELETE",
                         version,
                         "/asyncstatuses/1/asynclocation",
                         buildPathKeys("statusID", 1L));

    // #3 Delete on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.DELETE);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);

    discoveredItemsResource.delete(eq(key), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<UpdateResponse> callback = (Callback<UpdateResponse>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });

    EasyMock.replay(discoveredItemsResource);
    checkAsyncInvocation(discoveredItemsResource,
                         callback,
                         methodDescriptor,
                         "DELETE",
                         version,
                         "/asyncdiscovereditems/(itemId:1,type:2,userId:3)",
                         buildPathKeys("asyncDiscoveredItemId", key));
  }

  @Test
  public void testPromiseGet() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(PromiseStatusCollectionResource.class,
                                                                      PromiseLocationResource.class,
                                                                      PromiseDiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/promisestatuses");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("promiselocation");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/promisediscovereditems");

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseLocationResource locationResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1: simple filter
    methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(EasyMock.anyObject()))
        .andReturn(Promises.value(null))
        .once();
    checkInvocation(statusResource, methodDescriptor, "GET", version, "/promisestatuses?q=public_timeline");

    // #2: get
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisestatuses/1",
                    buildPathKeys("statusID", 1L));

    // #3: get on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.GET);
    locationResource = getMockResource(PromiseLocationResource.class);
    EasyMock.expect(locationResource.get()).andReturn(Promises.value(null)).once();
    checkInvocation(locationResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisestatuses/1/promiselocation",
                    buildPathKeys("statusID", 1L));

    // #4 get on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.GET);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.get(eq(key))).andReturn(Promises.value(null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisediscovereditems/(itemId:1,type:2,userId:3)",
                    buildPathKeys("promiseDiscoveredItemId", key));

  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "promiseFinderError")
  public Object[][] promiseFinder()
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/promisestatuses?q=search&since=1" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/promisestatuses?q=search&since=1" },
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "promiseFinderError")
  public void testPromiseFinderError(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("search");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET", uri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinder")
  public void testPromiseFinder(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("search");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.search(eq("linkedin"), eq(1L), eq(StatusType.REPLY))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", version, "/promiseStatuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinderOptionalParam")
  public void testPromiseFinderOptionalParam(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("search");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.search(eq("linkedin"), eq(-1L), eq((StatusType) null))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", version, "/promiseStatuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinderOptionalBooleanParam")
  public void testPromiseFinderOptionalBooleanParam(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("user_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(false), EasyMock.anyObject())).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", version, "/promiseStatuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "discoveredItemsFinder")
  public void testPromiseFinderOnComplexKey(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel discoveredItemsResourceModel = buildResourceModel(PromiseDiscoveredItemsResource.class);
    ResourceMethodDescriptor methodDescriptor = discoveredItemsResourceModel.findFinderMethod("user");
    PromiseDiscoveredItemsResource discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    EasyMock.expect(
        discoveredItemsResource.getDiscoveredItemsForUser(eq(1L), EasyMock.anyObject())).andReturn(Promises.value(null)).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "GET", version, "/promiseDiscoveredItems" + query);
  }

  @Test
  public void testPromiseGetAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(
        PromiseFollowsAssociativeResource.class);

    ResourceMethodDescriptor methodDescriptor;
    PromiseFollowsAssociativeResource resource;

    // #1: get
    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.GET);
    resource = getMockResource(PromiseFollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    EasyMock.expect(resource.get(key)).andReturn(Promises.value(new Followed(new DataMap()))).once();
    checkInvocation(resource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisefollows/(followerID:1,followeeID:2)",
                    buildPathKeys("followerID", 1L, "followeeID", 2L, followsResourceModel.getKeyName(), rawKey));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextDefault")
  public void testPromisePagingContextDefault(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(null, null)))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisestatuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextStartOnly")
  public void testPromisePagingContextStartOnly(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(5, null)))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisestatuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextCountOnly")
  public void testPromisePagingContextCountOnly(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(null, 4)))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", version, "/promisestatuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextBadCount")
  public void testPromisePagingContextBadCount(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor,
                           statusResource, "GET",
                           "/promisestatuses" + query,
                           version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextBadStart")
  public void testPromisePagingContextBadStart(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor,
                           statusResource,
                           "GET",
                           "/promisestatuses" + query,
                           version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextNegativeCount")
  public void testPromisePagingContextNegativeCount(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor,
                           statusResource,
                           "GET",
                           "/promisestatuses" + query,
                           version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextNegativeStart")
  public void testPromisePagingContextNegativeStart(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET", "/promisestatuses" + query, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusUserTimelineDefault")
  public void testPromisePagingContextUserTimelineDefault(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("user_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(true), eq(new PagingContext(10, 100, false, false))))
        .andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisestatuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusUserTimelineStartAndCount")
  public void testPromisePagingContextUserTimelineStartAndCount(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("user_timeline");
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(true), eq(new PagingContext(0, 20, true, true))))
        .andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisestatuses" + query);
  }

  @Test
  public void testPromiseBatchGet() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceModel followsAssociationResourceModel = buildResourceModel(
        PromiseFollowsAssociativeResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(PromiseDiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseFollowsAssociativeResource followsResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1 Batch get on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_GET);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.batchGet((Set<Long>)Matchers.eqCollectionUnordered(Sets.newHashSet(1L, 2L, 3L)))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisestatuses?ids=List(1,2,3)",
                    buildBatchPathKeys(1L, 2L, 3L));

    // #2 Batch get on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.BATCH_GET);
    followsResource = getMockResource(PromiseFollowsAssociativeResource.class);

    Set<CompoundKey> expectedKeys = new HashSet<CompoundKey>();
    CompoundKey key1 = new CompoundKey();
    key1.append("followeeID", 1L);
    key1.append("followerID", 1L);
    expectedKeys.add(key1);
    CompoundKey key2 = new CompoundKey();
    key2.append("followeeID", 2L);
    key2.append("followerID", 2L);
    expectedKeys.add(key2);

    EasyMock.expect(followsResource.batchGet((Set<CompoundKey>)Matchers.eqCollectionUnordered(expectedKeys))).andReturn(Promises.value(null)).once();
    checkInvocation(followsResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisefollows?ids=List((followeeID:1,followerID:1),(followeeID:2,followerID:2))",
                    buildBatchPathKeys(key1, key2));

    // #3 Batch get on complex key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_GET);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    @SuppressWarnings("unchecked")
    Set<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>> set =
        (Set<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>>)
            Matchers.eqCollectionUnordered(Sets.newHashSet(keyA, keyB));

    EasyMock.expect(discoveredItemsResource.batchGet(set)).andReturn(Promises.value(
        null)).once();

    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/promisediscovereditems?ids=List((itemId:1,type:2,userId:3),(itemId:4,type:5,userId:6))",
                    buildBatchPathKeys(keyA, keyB));
  }

  @Test
  public void testPromisePost() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        PromiseStatusCollectionResource.class,
        PromiseRepliesCollectionResource.class,
        PromiseLocationResource.class,
        PromiseDiscoveredItemsResource.class);

    ResourceModel statusResourceModel = resourceModelMap.get("/promisestatuses");
    ResourceModel repliesResourceModel = statusResourceModel.getSubResource("promisereplies");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("promiselocation");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/promisediscovereditems");

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseRepliesCollectionResource repliesResource;
    PromiseLocationResource locationResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.CREATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.create(EasyMock.anyObject())).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/promisestatuses",
                    "{}");

    // #1.1: different endpoint
    methodDescriptor = repliesResourceModel.findMethod(ResourceMethod.CREATE);
    repliesResource = getMockResource(PromiseRepliesCollectionResource.class);
    EasyMock.expect(repliesResource.create(EasyMock.anyObject())).andReturn(Promises.value(null)).once();
    checkInvocation(repliesResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/promisestatuses/1/replies",
                    "{}",
                    buildPathKeys("statusID", 1L));

    // #1.2: invalid entity
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.CREATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.create(EasyMock.anyObject())).andReturn(Promises.value(null)).once();
    try
    {
      checkInvocation(statusResource,
                      methodDescriptor,
                      "POST",
                      version,
                      "/promisestatuses",
                      "{");
      fail("Expected exception");
    }
    catch (Exception e)
    {
      // expected
      EasyMock.reset(statusResource);
    }

    // #2: Collection Partial Update
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(42));
    PatchRequest<Status> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(statusResource.update(eq(1L), eq(expected))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/promisestatuses/1",
                    "{\"patch\":{\"$set\":{\"foo\":42}}}",
                    buildPathKeys("statusID", 1L));

    // #3: Simple Resource Partial Update

    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    locationResource = getMockResource(PromiseLocationResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(51));
    PatchRequest<Location> expectedLocation = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(locationResource.update(eq(expectedLocation))).andReturn(Promises.value(null)).once();
    checkInvocation(locationResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/promisestatuses/1/promiselocation",
                    "{\"patch\":{\"$set\":{\"foo\":51}}}",
                    buildPathKeys("statusID", 1L));

    // #4 Complex key resource create
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.CREATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    EasyMock.expect(
        discoveredItemsResource.create(
            EasyMock.anyObject())).andReturn(Promises.value(null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/promisediscovereditems",
                    "{}");

    // #5 Complex key resource partial update
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(43));
    PatchRequest<DiscoveredItem> expectedDiscoveredItem =
        PatchRequest.createFromPatchDocument(p.getDataMap());
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(
        discoveredItemsResource.update(eq(key), eq(expectedDiscoveredItem))).andReturn(
        Promises.value(null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/promisediscovereditems/(itemId:1,type:2,userId:3)",
                    "{\"patch\":{\"$set\":{\"foo\":43}}}",
                    buildPathKeys("promiseDiscoveredItemId", key));

    // TODO would be nice to verify that posting an invalid record type fails
  }

  @Test
  public void testPromisePut() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(PromiseStatusCollectionResource.class,
                                                                      PromiseLocationResource.class,
                                                                      PromiseDiscoveredItemsResource.class);

    ResourceModel statusResourceModel = resourceModelMap.get("/promisestatuses");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("promiselocation");
    ResourceModel followsAssociationResourceModel = buildResourceModel(PromiseFollowsAssociativeResource.class);
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/promisediscovereditems");

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseFollowsAssociativeResource followsResource;
    PromiseLocationResource locationResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1 Update on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.UPDATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    long id = eq(1L);
    Status status  =EasyMock.anyObject();
    EasyMock.expect(statusResource.update(id, status)).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/promisestatuses/1",
                    "{}",
                    buildPathKeys("statusID", 1L));

    // #2 Update on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.UPDATE);
    followsResource = getMockResource(PromiseFollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    Followed followed = EasyMock.anyObject();
    EasyMock.expect(followsResource.update(key, followed)).andReturn(Promises.value(null)).once();
    checkInvocation(followsResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/promisefollows/(followerID:1,followeeID:2)",
                    "{}",
                    buildPathKeys("followerID", 1L, "followeeID", 2L, followsAssociationResourceModel.getKeyName(), rawKey));

    // #3 Update on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.UPDATE);
    locationResource = getMockResource(PromiseLocationResource.class);
    Location location  = EasyMock.anyObject();
    EasyMock.expect(locationResource.update(location)).andReturn(Promises.value(null)).once();
    checkInvocation(locationResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/promisestatuses/1/promiselocation",
                    "{}",
                    buildPathKeys("statusID", 1L));

    // #4 Update on complex key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.UPDATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> complexKey =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.update(
        eq(complexKey),
        EasyMock.anyObject(DiscoveredItem.class))).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/promisediscovereditems/(itemId:1,type:2,userId:3)", "{}",
                    buildPathKeys("promiseDiscoveredItemId", complexKey));

    // TODO would be nice to verify that posting an invalid record type fails
  }

  @Test
  public void testPromiseDelete() throws Exception
  {

    Map<String, ResourceModel> resourceModelMap = buildResourceModels(PromiseStatusCollectionResource.class,
                                                                      PromiseLocationResource.class,
                                                                      PromiseDiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/promisestatuses");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("promiselocation");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/promisediscovereditems");

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseLocationResource locationResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1 Delete on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.DELETE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.delete(eq(1L))).andReturn(Promises.value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "DELETE",
                    version,
                    "/promisestatuses/1",
                    buildPathKeys("statusID", 1L));

    // #2 Delete on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.DELETE);
    locationResource = getMockResource(PromiseLocationResource.class);
    EasyMock.expect(locationResource.delete()).andReturn(Promises.value(null)).once();
    checkInvocation(locationResource,
                    methodDescriptor,
                    "DELETE",
                    version,
                    "/promisestatuses/1/promiselocation",
                    buildPathKeys("statusID", 1L));

    // #3 Delete on complex key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.DELETE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.delete(eq(key))).andReturn(Promises.value(null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "DELETE",
                    version,
                    "/promisediscovereditems/(itemId:1,type:2,userId:3)",
                    buildPathKeys("promiseDiscoveredItemId", key));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testPromiseBatchUpdateCollection() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);

    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchUpdateRequest batchUpdateRequest = EasyMock.anyObject();
    EasyMock.expect(statusResource.batchUpdate(batchUpdateRequest)).andReturn(
        Promises.value(null)).once();
    String body = RestLiTestHelper.doubleQuote("{'entities':{'1':{},'2':{}}}");
    checkInvocation(statusResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/promisestatuses?ids=List(1,2)",
                    body,
                    buildBatchPathKeys(1L, 2L));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchUpdateComplexKey")
  public Object[][] batchUpdateComplexKey()
  {
    return new Object[][]
        {
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids[0].itemId=1&ids[0].type=2&ids[0].userId=3&ids[1].itemId=4&ids[1].type=5&ids[1].userId=6",
                "{\"entities\":{\"itemId=1&type=2&userId=3\":{},\"itemId=4&type=5&userId=6\":{}}}"
            },
            // With entity key fields arranged in random order
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids[0].itemId=1&ids[0].type=2&ids[0].userId=3&ids[1].itemId=4&ids[1].type=5&ids[1].userId=6",
                "{\"entities\":{\"type=2&userId=3&itemId=1\":{},\"userId=6&type=5&itemId=4\":{}}}"
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids=List((itemId:1,type:2,userId:3),(itemId:4,type:5,userId:6))",
                "{\"entities\":{\"(itemId:1,type:2,userId:3)\":{},\"(itemId:4,type:5,userId:6)\":{}}}"
            },
            // With entity key fields arranged in random order
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids=List((itemId:1,type:2,userId:3),(itemId:4,type:5,userId:6))",
                "{\"entities\":{\"(type:2,userId:3,itemId:1)\":{},\"(userId:6,type:5,itemId:4)\":{}}}"
            }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchUpdateComplexKey")
  public void testPromiseBatchUpdateComplexKey(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel discoveredItemsResourceModel = buildResourceModel(PromiseDiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    PromiseDiscoveredItemsResource discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    BatchUpdateRequest<ComplexResourceKey<DiscoveredItemKey,DiscoveredItemKeyParams>,DiscoveredItem> batchUpdateRequest = EasyMock.anyObject();
    @SuppressWarnings("unchecked")
    Promise<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey,DiscoveredItemKeyParams>,DiscoveredItem>> batchUpdateResult =
        discoveredItemsResource.batchUpdate(batchUpdateRequest);
    EasyMock.expect(batchUpdateResult).andReturn(
        Promises.value(
            null)).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "PUT", version, uri, body, buildBatchPathKeys(keyA, keyB));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testPromiseBatchPatch() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    PromiseStatusCollectionResource statusResource = getMockResource(PromiseStatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchPatchRequest batchPatchRequest = EasyMock.anyObject();
    EasyMock.expect(statusResource.batchUpdate(batchPatchRequest)).andReturn(
        Promises.value(null)).once();
    String body = RestLiTestHelper.doubleQuote("{'entities':{'1':{},'2':{}}}");
    checkInvocation(statusResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/promisestatuses?ids=List(1,2)",
                    body,
                    buildBatchPathKeys(1L, 2L));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKeyWithBody")
  public Object[][] batchComplexKeyWithBody()
  {
    return new Object[][]
        {
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids[0].itemId=1&ids[0].type=2&ids[0].userId=3&ids[1].itemId=4&ids[1].type=5&ids[1].userId=6",
                "{\"entities\":{\"itemId=1&type=2&userId=3\":{},\"itemId=4&type=5&userId=6\":{}}}"
            },
            // With entity key fields arranged in random order
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids[0].itemId=1&ids[0].type=2&ids[0].userId=3&ids[1].itemId=4&ids[1].type=5&ids[1].userId=6",
                "{\"entities\":{\"type=2&userId=3&itemId=1\":{},\"userId=6&type=5&itemId=4\":{}}}"
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids=List((itemId:1,type:2,userId:3),(itemId:4,type:5,userId:6))",
                "{\"entities\":{\"(itemId:1,type:2,userId:3)\":{},\"(itemId:4,type:5,userId:6)\":{}}}"
            },
            // With entity key fields arranged in random order
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids=List((itemId:1,type:2,userId:3),(itemId:4,type:5,userId:6))",
                "{\"entities\":{\"(type:2,userId:3,itemId:1)\":{},\"(userId:6,type:5,itemId:4)\":{}}}"
            }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKeyWithBody")
  public void testPromiseBatchPatchComplexKey(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel discoveredItemsResourceModel = buildResourceModel(PromiseDiscoveredItemsResource.class);
    ResourceMethodDescriptor methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    PromiseDiscoveredItemsResource discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    BatchPatchRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> batchPatchRequest = EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchUpdate(batchPatchRequest)).andReturn(
        Promises.value(null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    uri,
                    body,
                    buildBatchPathKeys(keyA, keyB));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testPromiseBatchCreate() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(PromiseDiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1 Batch create on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchCreateRequest batchCreateRequest = EasyMock.anyObject();
    EasyMock.expect(statusResource.batchCreate(batchCreateRequest)).andReturn(
        Promises.value(null)).once();
    String body = RestLiTestHelper.doubleQuote("{'elements':[{},{}]}");
    checkInvocation(statusResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/promisestatuses",
                    body,
                    buildBatchPathKeys());

    // #2 Batch create on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    batchCreateRequest = EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchCreate(batchCreateRequest)).andReturn(
        Promises.value(
            null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    version, "/promisediscovereditems",
                    body,
                    buildBatchPathKeys());
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testPromiseBatchDelete() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(PromiseDiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1 Batch delete on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchDeleteRequest batchDeleteRequest = EasyMock.anyObject();
    EasyMock.expect(statusResource.batchDelete(batchDeleteRequest)).andReturn(
        Promises.<BatchUpdateResult<Long, Status>> value(null)).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "DELETE",
                    version,
                    "/promisestatuses?ids=List(1,2)",
                    "",
                    buildBatchPathKeys(1L, 2L));

    // #2 Batch delete on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    batchDeleteRequest = EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchDelete(batchDeleteRequest)).andReturn(
        Promises.<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> value(null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "DELETE",
                    version,
                    "/promisediscovereditems?ids=List((itemId:1,type:2,userId:3),(itemId:4,type:5,userId:6))",
                    "",
                    buildBatchPathKeys(keyA, keyB));
  }

  @Test
  public void testGet() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        StatusCollectionResource.class,
        LocationResource.class,
        DiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/statuses");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/discovereditems");

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1: simple filter
    methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/statuses?q=public_timeline");

    // #2: get
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET",
                    version, "/statuses/1", buildPathKeys("statusID", 1L));

    // #3: get simple sub-resource
    ResourceModel locationResourceModel = statusResourceModel.getSubResource(
        "location");

    LocationResource locationResource;

    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.GET);
    locationResource = getMockResource(LocationResource.class);
    EasyMock.expect(locationResource.get()).andReturn(null).once();
    checkInvocation(locationResource, methodDescriptor, "GET",
                    version, "/statuses/1/location", buildPathKeys("statusID", 1L));

    // #4: get complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.GET);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.get(eq(key))).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/discovereditems/(itemId:1,type:2,userId:3)",
                    buildPathKeys("discoveredItemId", key));
  }

  // keys are already parsed by this point, so in path keys alternative keys are in canonical format.
  // tests for ensuring keys are parsed into pathKeys correctly can be found in TestRestliRouting.
  // this test is primarily for ensuring that alternate keys to not cause parameter parsing to fail.
  @Test
  public void testAltKeyGet() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);

    ResourceMethodDescriptor  getMethodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(null).once();
    checkInvocation(statusResource, getMethodDescriptor, "GET",
                    version, "/statuses/Alt1?altkey=alt", buildPathKeys("statusID", 1L));

    ResourceMethodDescriptor  batchGetMethodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_GET);
    statusResource = getMockResource(StatusCollectionResource.class);
    Set<Long> batchKeys = new HashSet<Long>(3);
    batchKeys.add(1L);
    batchKeys.add(2L);
    batchKeys.add(3L);
    EasyMock.expect(statusResource.batchGet(eq(batchKeys))).andReturn(null).once();
    checkInvocation(statusResource, batchGetMethodDescriptor, "GET",
                    version, "/statuses?ids=List(Alt1,Alt2,Alt3)&altkey=alt", buildBatchPathKeys(1L, 2L, 3L));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinder")
  public void testFinder(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("search");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.search(eq("linkedin"), eq(1L), eq(StatusType.REPLY))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", version, "/statuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinderOptionalParam")
  public void testFinderOptionalParam(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("search");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.search(eq("linkedin"), eq(-1L), eq((StatusType) null))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", version, "/statuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinderOptionalBooleanParam")
  public void testFinderOptionalBooleanParam(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("user_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(false), EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", version, "/statuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "discoveredItemsFinder")
  public void testFinderOnComplexKey(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel discoveredItemsResourceModel = buildResourceModel(DiscoveredItemsResource.class);
    ResourceMethodDescriptor methodDescriptor = discoveredItemsResourceModel.findFinderMethod("user");
    DiscoveredItemsResource discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    EasyMock.expect(discoveredItemsResource.findByUser(eq(1L))).andReturn(null).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "GET", version, "/discoveredItems" + query);
  }

  @Test
  public void testGetAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(
        FollowsAssociativeResource.class);

    ResourceMethodDescriptor methodDescriptor;
    FollowsAssociativeResource resource;

    // #1: get
    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.GET);
    resource = getMockResource(FollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    EasyMock.expect(resource.get(key)).andReturn(new Followed(new DataMap())).once();
    checkInvocation(resource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/follows/(followerID:1,followeeID:2)",
                    buildPathKeys("followerID", 1L, "followeeID", 2L, followsResourceModel.getKeyName(), rawKey));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextDefault")
  public void testPagingContextDefault(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(null, null)))).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/statuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextStartOnly")
  public void testPagingContextStartOnly(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(5, null)))).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/statuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextCountOnly")
  public void testPagingContextCountOnly(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(null, 4)))).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/statuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextBadCount")
  public void testPagingContextBadCount(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor,
                           statusResource, "GET",
                           "/statuses" + query,
                           version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextBadStart")
  public void testPagingContextBadStart(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor,
                           statusResource,
                           "GET",
                           "/statuses" + query,
                           version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextNegativeCount")
  public void testPagingContextNegativeCount(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor,
                           statusResource,
                           "GET",
                           "/statuses" + query,
                           version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextNegativeStart")
  public void testPagingContextNegativeStart(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("public_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor,
                           statusResource,
                           "GET",
                           "/statuses" + query,
                           version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusUserTimelineDefault")
  public void testPagingContextUserTimelineDefault(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("user_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(true), eq(new PagingContext(10, 100, false, false)))).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/statuses" + query);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusUserTimelineStartAndCount")
  public void testPagingContextUserTimelineStartAndCount(ProtocolVersion version, String query) throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findFinderMethod("user_timeline");
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(true), eq(new PagingContext(0, 20, true, true)))).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/statuses" + query);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBatchGet() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceModel followsAssociationResourceModel = buildResourceModel(
        FollowsAssociativeResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(
        DiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    FollowsAssociativeResource followsResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1 Batch get on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_GET);
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.batchGet((Set<Long>)Matchers.eqCollectionUnordered(Sets.newHashSet(1L, 2L, 3L)))).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "GET",
                    version,
                    "/statuses?ids=List(1,2,3)",
                    buildBatchPathKeys(1L, 2L, 3L));

    // #2 Batch get on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.BATCH_GET);
    followsResource = getMockResource(FollowsAssociativeResource.class);

    Set<CompoundKey> expectedKeys = new HashSet<CompoundKey>();
    CompoundKey key1 = new CompoundKey();
    key1.append("followeeID", 1L);
    key1.append("followerID", 1L);
    expectedKeys.add(key1);
    CompoundKey key2 = new CompoundKey();
    key2.append("followeeID", 2L);
    key2.append("followerID", 2L);
    expectedKeys.add(key2);

    EasyMock.expect(followsResource.batchGet((Set<CompoundKey>)Matchers.eqCollectionUnordered(expectedKeys))).andReturn(null).once();

    String uri = "/follows?ids=List((followeeID:1,followerId:1),(followeeID:2,followerId:2))";

    checkInvocation(followsResource,
                    methodDescriptor,
                    "GET",
                    version, uri,
                    buildBatchPathKeys(key1, key2));

    // #3 Batch get on complex key resource.
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_GET);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    @SuppressWarnings("unchecked")
    Set<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>> set =
        (Set<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>>)
            Matchers.eqCollectionUnordered(Sets.newHashSet(keyA, keyB));

    EasyMock.expect(discoveredItemsResource.batchGet(set)).andReturn(null).once();

    uri = "/discovereditems?ids=List((itemId:1,type:2,userId:3),(itemId:4,type:5,userId:6))";

    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "GET",
                    version, uri,
                    buildBatchPathKeys(keyA, keyB));
  }

  @Test
  public void testPost() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        StatusCollectionResource.class,
        RepliesCollectionResource.class,
        LocationResource.class,
        DiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/statuses");
    ResourceModel repliesResourceModel = statusResourceModel.getSubResource("replies");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("location");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/discovereditems");

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    RepliesCollectionResource repliesResource;
    LocationResource locationResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.CREATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.create(EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/statuses",
                    "{}");

    // #1.1: different endpoint
    methodDescriptor = repliesResourceModel.findMethod(ResourceMethod.CREATE);
    repliesResource = getMockResource(RepliesCollectionResource.class);
    EasyMock.expect(repliesResource.create(EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(repliesResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/statuses/1/replies",
                    "{}",
                    buildPathKeys("statusID", 1L));

    // #1.2: invalid entity
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.CREATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.create(EasyMock.anyObject())).andReturn(null).once();
    try
    {
      checkInvocation(statusResource,
                      methodDescriptor,
                      "POST",
                      version,
                      "/statuses",
                      "{");
      fail("Expected exception");
    }
    catch (Exception e)
    {
      // expected
      EasyMock.reset(statusResource);
    }

    // #2 Collection Partial Update
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(42));
    PatchRequest<Status> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(statusResource.update(eq(1L), eq(expected))).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/statuses/1",
                    "{\"patch\":{\"$set\":{\"foo\":42}}}",
                    buildPathKeys("statusID", 1L));

    // #3 Simple Resource Partial Update

    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    locationResource = getMockResource(LocationResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(51));
    PatchRequest<Location> expectedLocation = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(locationResource.update(eq(expectedLocation))).andReturn(null).once();
    checkInvocation(locationResource,
                    methodDescriptor,
                    "POST",
                    version, "/statuses/1/location",
                    "{\"patch\":{\"$set\":{\"foo\":51}}}",
                    buildPathKeys("statusID", 1L));

    // #4 Complex-key resource create
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.CREATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    EasyMock.expect(
        discoveredItemsResource.create(EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/discovereditems",
                    "{}");

    // #5 Partial update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(43));
    PatchRequest<DiscoveredItem> expectedDiscoveredItem =
        PatchRequest.createFromPatchDocument(p.getDataMap());
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(
        discoveredItemsResource.update(eq(key), eq(expectedDiscoveredItem))).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    version, "/discovereditems/(itemId:1,type:2,userId:3)",
                    "{\"patch\":{\"$set\":{\"foo\":43}}}",
                    buildPathKeys("discoveredItemId", key));

    // TODO would be nice to verify that posting an invalid record type fails
  }

  @Test
  public void testPut() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        StatusCollectionResource.class,
        LocationResource.class,
        DiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/statuses");
    ResourceModel followsAssociationResourceModel = buildResourceModel(
        FollowsAssociativeResource.class);
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("location");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/discovereditems");

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    FollowsAssociativeResource followsResource;
    LocationResource locationResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1 Update on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.UPDATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    long id = eq(1L);
    Status status = EasyMock.anyObject();
    EasyMock.expect(statusResource.update(id, status)).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/statuses/1",
                    "{}",
                    buildPathKeys("statusID", 1L));

    // #2 Update on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.UPDATE);
    followsResource = getMockResource(FollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    Followed followed = EasyMock.anyObject();
    EasyMock.expect(followsResource.update(key, followed)).andReturn(null).once();
    checkInvocation(followsResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/follows/(followerID:1,followeeID:2)", "{}",
                    buildPathKeys("followerID", 1L, "followeeID", 2L, followsAssociationResourceModel.getKeyName(), rawKey));

    // #3 Update on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.UPDATE);
    locationResource = getMockResource(LocationResource.class);
    Location location  = EasyMock.anyObject();
    EasyMock.expect(locationResource.update(location)).andReturn(null).once();
    checkInvocation(locationResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/statuses/1/location",
                    "{}",
                    buildPathKeys("statusID", 1L));

    // #4 Update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.UPDATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> complexKey =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.update(
        eq(complexKey),
        EasyMock.anyObject(DiscoveredItem.class))).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/discovereditems/(itemId:1,type:2,userId:3)",
                    "{}",
                    buildPathKeys("discoveredItemId", complexKey));

    // TODO would be nice to verify that posting an invalid record type fails
  }

  @Test
  public void testDelete() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(StatusCollectionResource.class,
                                                                      LocationResource.class,
                                                                      DiscoveredItemsResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/statuses");
    ResourceModel locationResourceModel = statusResourceModel.getSubResource("location");
    ResourceModel discoveredItemsResourceModel = resourceModelMap.get("/discovereditems");

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    LocationResource locationResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1 Delete on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.DELETE);
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.delete(eq(1L))).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "DELETE",
                    version, "/statuses/1",
                    buildPathKeys("statusID", 1L));

    // #2 Delete on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.DELETE);
    locationResource = getMockResource(LocationResource.class);
    EasyMock.expect(locationResource.delete()).andReturn(null).once();
    checkInvocation(locationResource,
                    methodDescriptor,
                    "DELETE",
                    version, "/statuses/1/location",
                    buildPathKeys("statusID", 1L));

    // #3 Delete on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.DELETE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.delete(eq(key))).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "DELETE",
                    version, "/discovereditems/(itemId:1,type:2,userId:3)",
                    buildPathKeys("discoveredItemId", key));
  }

  @Test
  public void testAction_SimpleParameters() throws Exception
  {
    ResourceModel accountsResourceModel = buildResourceModel(TwitterAccountsResource.class);
    ResourceMethodDescriptor methodDescriptor;
    TwitterAccountsResource accountsResource;

    // #1 no defaults provided
    methodDescriptor = accountsResourceModel.findActionMethod("register", ResourceLevel.COLLECTION);
    accountsResource = getMockResource(TwitterAccountsResource.class);
    accountsResource.register(eq("alfred"),
                              eq("hitchcock"),
                              eq("alfred@test.linkedin.com"),
                              eq("genentech"),
                              eq(false));
    EasyMock.expectLastCall().once();

    String jsonEntityBody = RestLiTestHelper.doubleQuote(
        "{'first': 'alfred', 'last': 'hitchcock', 'email': 'alfred@test.linkedin.com', " +
            "'company': 'genentech', 'openToMarketingEmails': false}");
    checkInvocation(accountsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/accounts?action=register",
                    jsonEntityBody);

    // #2 defaults filled in
    methodDescriptor = accountsResourceModel.findActionMethod("register", ResourceLevel.COLLECTION);
    accountsResource = getMockResource(TwitterAccountsResource.class);
    accountsResource.register(eq("alfred"),
                              eq("hitchcock"),
                              eq("alfred@test.linkedin.com"),
                              eq((String) null),
                              eq(true));
    EasyMock.expectLastCall().once();

    jsonEntityBody = RestLiTestHelper.doubleQuote(
        "{'first': 'alfred', 'last': 'hitchcock', 'email': 'alfred@test.linkedin.com'}");
    checkInvocation(accountsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/accounts?action=register",
                    jsonEntityBody);

    // #3 no-arg method
    methodDescriptor = accountsResourceModel.findActionMethod("noArgMethod", ResourceLevel.COLLECTION);
    accountsResource = getMockResource(TwitterAccountsResource.class);
    accountsResource.noArgMethod();
    EasyMock.expectLastCall().once();

    jsonEntityBody = RestLiTestHelper.doubleQuote("{}");
    checkInvocation(accountsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/accounts?action=noArgMethod",
                    jsonEntityBody);

    // #4 primitive response
    methodDescriptor = accountsResourceModel.findActionMethod("primitiveResponse", ResourceLevel.COLLECTION);
    accountsResource = getMockResource(TwitterAccountsResource.class);
    EasyMock.expect(accountsResource.primitiveResponse()).andReturn(1).once();

    jsonEntityBody = RestLiTestHelper.doubleQuote("{'value': 1}");
    checkInvocation(accountsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/accounts?action=primitiveResponse",
                    jsonEntityBody);
  }

  @Test
  public void testAction_BadParameterTypes() throws Exception
  {
    ResourceModel accountsResourceModel = buildResourceModel(TwitterAccountsResource.class);
    ResourceMethodDescriptor methodDescriptor;

    // #1 no defaults provided
    methodDescriptor = accountsResourceModel.findActionMethod("register", ResourceLevel.COLLECTION);

    String jsonEntityBody = RestLiTestHelper.doubleQuote(
        "{'first': 42, 'last': 42, 'email': 42, " +
            "'company': 42, 'openToMarketingEmails': 'false'}");

    RestRequest request =
        new RestRequestBuilder(new URI("/accounts?action=register"))
            .setMethod("POST").setEntity(jsonEntityBody.getBytes(Data.UTF_8_CHARSET))
            .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString())
            .build();

    RoutingResult routingResult = new RoutingResult(new ResourceContextImpl(null, request,
                                                                            new RequestContext()), methodDescriptor);

    try {
      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
          Collections.emptySet(),
          routingResult.getContext());
      _methodAdapterRegistry.getArgumentBuilder(methodDescriptor.getMethodType())
          .extractRequestData(routingResult, DataMapUtils.readMapWithExceptions(request));
      _invoker.invoke(null, routingResult, _methodAdapterRegistry.getArgumentBuilder(methodDescriptor.getMethodType()),
                      null);
      Assert.fail("expected routing exception");
    }
    catch (RoutingException e)
    {
      Assert.assertEquals(e.getStatus(), 400);
    }
  }

  @Test
  public void testAction_BadArrayElements() throws Exception
  {
    ResourceModel accountsResourceModel = buildResourceModel(TwitterAccountsResource.class);
    ResourceMethodDescriptor methodDescriptor;

    // #1 no defaults provided
    methodDescriptor = accountsResourceModel.findActionMethod("spamTweets", ResourceLevel.COLLECTION);

    String jsonEntityBody = RestLiTestHelper.doubleQuote(
        "{'statuses':[1,2,3]}");

    RestRequest request =
        new RestRequestBuilder(new URI("/accounts?action=spamTweets"))
            .setMethod("POST").setEntity(jsonEntityBody.getBytes(Data.UTF_8_CHARSET))
            .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString())
            .build();

    RoutingResult routingResult = new RoutingResult(new ResourceContextImpl(null, request,
                                                                            new RequestContext()), methodDescriptor);
    RestLiArgumentBuilder argumentBuilder = _methodAdapterRegistry.getArgumentBuilder(methodDescriptor.getType());

    try {
      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
          Collections.emptySet(),
          routingResult.getContext());
      RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult,
          DataMapUtils.readMapWithExceptions(request));
      _invoker.invoke(requestData, routingResult,
                      _methodAdapterRegistry.getArgumentBuilder(methodDescriptor.getMethodType()), null);
      Assert.fail("expected routing exception");
    }
    catch (RoutingException e)
    {
      Assert.assertEquals(e.getStatus(), 400);
    }
  }

  @Test
  public void testInvoke_testComplexParameters() throws Exception
  {
    ResourceModel accountsResourceModel = buildResourceModel(TwitterAccountsResource.class);
    ResourceMethodDescriptor methodDescriptor;
    TwitterAccountsResource accountsResource;

    // #1 no defaults provided
    methodDescriptor = accountsResourceModel.findActionMethod("closeAccounts", ResourceLevel.COLLECTION);
    accountsResource = getMockResource(TwitterAccountsResource.class);
    StringArray emailAddresses = new StringArray("bob@test.linkedin.com", "joe@test.linkedin.com");

    EasyMock.expect(accountsResource.closeAccounts(eq(emailAddresses), eq(true), eq((StringMap)null)))
        .andReturn((new StringMap())).once();

    String jsonEntityBody = RestLiTestHelper.doubleQuote(
        "{'emailAddresses': ['bob@test.linkedin.com', 'joe@test.linkedin.com'], 'someFlag': true}");
    checkInvocation(accountsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/accounts?action=closeAccounts",
                    jsonEntityBody);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customTypeNoCoercer")
  public Object[][] customStringNoCoercer() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/replies?query=noCoercerCustomString&s=foo" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/replies?query=noCoercerCustomString&s=foo" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customTypeNoCoercer")
  public void testCustomTypeParameters_NoCoercer(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findFinderMethod("noCoercerCustomString");
    expectRoutingException(methodDescriptor,
                           getMockResource(RepliesCollectionResource.class),
                           "GET",
                           uri,
                           version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customTypeWrongType")
  public Object[][] customStringWrongType() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLong&l=foo" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLong&l=foo" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customTypeWrongType")
  public void testCustomTypeParameters_WrongType(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findFinderMethod("customLong");
    expectRoutingException(methodDescriptor,
                           getMockResource(RepliesCollectionResource.class),
                           "GET",
                           uri,
                           version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customTypeCoercerError")
  public Object[][] customTypeCoercerError() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/custom_status?q=search&keywords=1234" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/custom_status?q=search&keywords=1234" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customTypeCoercerError")
  public void testCustomTypeParameters_CoercerError(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(CustomStatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findFinderMethod("search");
    expectRoutingException(methodDescriptor,
                           getMockResource(CustomStatusCollectionResource.class),
                           "GET",
                           uri,
                           version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customStringParam")
  public Object[][] customStringParam() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/replies?query=customString&s=foo" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/replies?query=customString&s=foo" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customStringParam")
  public void testCustomTypeParametersCustomString(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findFinderMethod("customString");
    RepliesCollectionResource repliesResource =  getMockResource(RepliesCollectionResource.class);
    repliesResource.customString(new CustomString("foo"));
    EasyMock.expectLastCall().andReturn(null).once();
    checkInvocation(repliesResource, methodDescriptor, "GET", version, uri);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customLongParam")
  public Object[][] customLongParam() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLong&l=100" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLong&l=100" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customLongParam")
  public void testCustomTypeParametersCustomLong(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findFinderMethod("customLong");
    RepliesCollectionResource repliesResource = getMockResource(RepliesCollectionResource.class);
    repliesResource.customLong(new CustomLong(100L));
    EasyMock.expectLastCall().andReturn(null).once();
    checkInvocation(repliesResource,  methodDescriptor, "GET", version, uri);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customLongDefault")
  public Object[][] customLongDefault() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLongDefault&l=100" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLongDefault&l=100" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customLongDefault")
  public void testCustomTypeParametersCustomLongWithDefault(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findFinderMethod("customLongDefault");
    RepliesCollectionResource repliesResource = getMockResource(RepliesCollectionResource.class);
    repliesResource.customLongDefault(new CustomLong(100L), new CustomLong(1235L));
    EasyMock.expectLastCall().andReturn(null).once();
    checkInvocation(repliesResource,  methodDescriptor, "GET", version, uri);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customLongArray")
  public Object[][] customLongArray() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLongArray&longs=100&longs=200" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLongArray&longs=List(100,200)" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customLongArray")
  public void testCustomTypeParametersCustomLongArray(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findFinderMethod("customLongArray");
    RepliesCollectionResource repliesResource = getMockResource(RepliesCollectionResource.class);
    CustomLong[] longs = {new CustomLong(100L), new CustomLong(200L)};
    repliesResource.customLongArray(EasyMock.aryEq(longs));
    EasyMock.expectLastCall().andReturn(null).once();
    checkInvocation(repliesResource, methodDescriptor, "GET", version, uri);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customLongArrayDefault")
  public Object[][] customLongArrayDefault() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLongArrayDefault&longs=100&longs=200" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/replies?query=customLongArrayDefault&longs=List(100,200)" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "customLongArrayDefault")
  public void testCustomTypeParametersCustomLongArrayWithDefault(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findFinderMethod("customLongArrayDefault");
    RepliesCollectionResource repliesResource = getMockResource(RepliesCollectionResource.class);
    CustomLong[] longs = {new CustomLong(100L), new CustomLong(200L)};
    CustomLong[] longsFromDefault = {new CustomLong(1235L), new CustomLong(6789L)};
    repliesResource.customLongArrayDefault(EasyMock.aryEq(longs), EasyMock.aryEq(longsFromDefault));
    EasyMock.expectLastCall().andReturn(null).once();
    checkInvocation(repliesResource, methodDescriptor, "GET", version, uri);
  }

  @Test
  public void testActionsOnResource() throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceModel locationResourceModel = buildResourceModel(LocationResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(DiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    RepliesCollectionResource repliesResource;
    LocationResource locationResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1 Action on collection resource
    methodDescriptor = repliesResourceModel.findActionMethod("replyToAll", ResourceLevel.COLLECTION);
    repliesResource = getMockResource(RepliesCollectionResource.class);
    repliesResource.replyToAll("hello");
    EasyMock.expectLastCall().once();

    String jsonEntityBody = RestLiTestHelper.doubleQuote("{'status': 'hello'}");
    MutablePathKeys pathKeys = new PathKeysImpl();
    pathKeys.append("statusID", 1L);
    checkInvocation(repliesResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/statuses/1/replies?action=replyToAll",
                    jsonEntityBody,
                    pathKeys);

    // #2 Action on simple resource
    methodDescriptor = locationResourceModel.findActionMethod("new_status_from_location", ResourceLevel.ENTITY);
    locationResource = getMockResource(LocationResource.class);
    locationResource.newStatusFromLocation(eq("hello"));
    EasyMock.expectLastCall().once();

    jsonEntityBody = RestLiTestHelper.doubleQuote("{'status': 'hello'}");
    pathKeys = new PathKeysImpl();
    pathKeys.append("statusID", 1L);
    checkInvocation(locationResource, methodDescriptor, "POST",
                    version,
                    "/statuses/1/location?action=new_status_from_location",
                    jsonEntityBody, pathKeys);

    // #3 Action on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findActionMethod("purge", ResourceLevel.COLLECTION);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    discoveredItemsResource.purge(12L);
    EasyMock.expectLastCall().once();

    jsonEntityBody = RestLiTestHelper.doubleQuote("{'user': 12}");
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    version, "/discovereditems/action=purge",
                    jsonEntityBody,
                    buildPathKeys());
  }

  @Test
  public void testActionParameterTypeCoercion() throws Exception
  {
    ResourceModel model;
    ResourceMethodDescriptor methodDescriptor;
    CombinedResources.TestActionsResource resource;
    String jsonEntityBody;

    model = buildResourceModel(CombinedResources.TestActionsResource.class);

    methodDescriptor = model.findActionMethod("intParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    int expectedInt = DataTemplateUtil.coerceOutput(Long.MAX_VALUE, Integer.class);
    resource.intParam(expectedInt);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'intParam':" + String.valueOf(Long.MAX_VALUE) + "}");
    checkInvocation(resource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/test?action=intParam",
                    jsonEntityBody,
                    buildPathKeys());

    methodDescriptor = model.findActionMethod("longParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    long expectedLong = DataTemplateUtil.coerceOutput(Integer.MAX_VALUE, Long.class);
    resource.longParam(expectedLong);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'longParam':" + String.valueOf(Integer.MAX_VALUE) + "}");
    checkInvocation(resource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/test?action=longParam",
                    jsonEntityBody,
                    buildPathKeys());

    methodDescriptor = model.findActionMethod("byteStringParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    String str = "test string";
    ByteString expectedByteString = ByteString.copyString(str, "UTF-8");
    resource.byteStringParam(expectedByteString);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'byteStringParam': '" + str + "'}");
    checkInvocation(resource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/test?action=byteStringParam",
                    jsonEntityBody,
                    buildPathKeys());

    methodDescriptor = model.findActionMethod("floatParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    float expectedFloat = DataTemplateUtil.coerceOutput(Double.MAX_VALUE, Float.class);
    resource.floatParam(expectedFloat);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'floatParam': " + String.valueOf(Double.MAX_VALUE) + "}");
    checkInvocation(resource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/test?action=floatParam",
                    jsonEntityBody,
                    buildPathKeys());

    methodDescriptor = model.findActionMethod("doubleParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    float floatValue = 567.5f;
    double expectedDouble = DataTemplateUtil.coerceOutput(floatValue, Double.class);
    resource.doubleParam(expectedDouble);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'doubleParam': " + String.valueOf(floatValue) + "}");
    checkInvocation(resource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/test?action=doubleParam",
                    jsonEntityBody,
                    buildPathKeys());

    methodDescriptor = model.findActionMethod("recordParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);

    TestRecord expectedRecord = new TestRecord();
    expectedRecord.setIntField(expectedInt);
    expectedRecord.setLongField(expectedLong);
    expectedRecord.setFloatField(expectedFloat);
    expectedRecord.setDoubleField(expectedDouble);
    resource.recordParam(expectedRecord);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'recordParam':{"
                                                      + "'intField':" + String.valueOf(Long.MAX_VALUE) + ","
                                                      + "'longField':" + String.valueOf(Integer.MAX_VALUE) + ","
                                                      + "'floatField':" + String.valueOf(Double.MAX_VALUE) + ","
                                                      + "'doubleField':" + String.valueOf(floatValue)
                                                      + "}}");

    checkInvocation(resource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/test?action=recordParam",
                    jsonEntityBody,
                    buildPathKeys());

  }

  @Test
  public void testHeuristicKeySyntaxDetection() throws PathSegment.PathSegmentSyntaxException
  {
    Set<Key> keys = new HashSet<Key>(2);
    keys.add(new Key("foo", Integer.class));
    keys.add(new Key("bar", String.class));

    // heuristic key syntax detection only occurs in Protocol Version 1.0.0
    ProtocolVersion v1 = AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();

    Set<String> expectedKeys = new HashSet<String>(Arrays.asList("foo", "bar"));
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo:42;bar:abcd", keys, v1).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo:42;bar:abcd=1&efg=2", keys, v1).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo=42&bar=abcd", keys, v1).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo=42&bar=abcd:1;2", keys, v1).getPartKeys());

    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo=42&bar=foo:42", keys, v1).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo=42&bar=foo:42;bar:abcd", keys, v1).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo:42;bar:foo=42&bar=abcd", keys, v1).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo:42;bar:foo=42", keys, v1).getPartKeys());
  }

  @DataProvider
  public Object[][] dataMapToCompoundKey()
  {
    CompoundKey compoundKey1 = new CompoundKey();
    compoundKey1.append("foo", new Integer(1));
    compoundKey1.append("bar", "hello");

    DataMap dataMap1 = new DataMap();
    dataMap1.put("foo", "1");
    dataMap1.put("bar", "hello");

    Set<Key> keys1 = new HashSet<Key>(2);
    keys1.add(new Key("foo", Integer.class));
    keys1.add(new Key("bar", String.class));

    CompoundKey compoundKey2 = new CompoundKey();
    compoundKey2.append("a", new Long(6));
    compoundKey2.append("b", new Double(3.14));

    DataMap dataMap2 = new DataMap();
    dataMap2.put("a", "6");
    dataMap2.put("b", "3.14");

    Set<Key> keys2 = new HashSet<Key>(2);
    keys2.add(new Key("a", Long.class));
    keys2.add(new Key("b", Double.class));

    return new Object[][]
        {
            { compoundKey1, dataMap1, keys1 },
            { compoundKey2, dataMap2, keys2 }
        };
  }


  @Test(dataProvider = "dataMapToCompoundKey")
  public void testDataMapToCompoundKey(CompoundKey expectedCompoundKey, DataMap dataMap, Set<Key> keys)
  {
    CompoundKey compoundKey = ArgumentUtils.dataMapToCompoundKey(dataMap, keys);
    Assert.assertEquals(compoundKey, expectedCompoundKey);
  }

  @Test
  public void testParseqTraceSync() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        StatusCollectionResource.class);
    ResourceModel statusResourceModel = resourceModelMap.get("/statuses");

    // #1: Sync Method Execution
    RequestContext requestContext = new RequestContext();
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(null).once();
    checkInvocation(statusResource,
                    requestContext,
                    methodDescriptor,
                    "GET",
                    version,
                    "/statuses/1",
                    null,
                    buildPathKeys("statusID", 1L),
                    new Callback<RestResponse>()
                    {
                      //A 404 is considered an error by rest.li
                      @Override
                      public void onError(Throwable e)
                      {
                        Assert.assertNull(requestContext.getLocalAttr(ATTRIBUTE_PARSEQ_TRACE), "There should be no parseq trace!");
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                        Assert.fail("Request failed unexpectedly.");
                      }
                    },
                    true, false);

  }

  @Test
  public void testParseqTraceAsync() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        AsyncStatusCollectionResource.class);

    ResourceModel asyncStatusResourceModel = resourceModelMap.get("/asyncstatuses");

    // #2: Callback based Async Method Execution
    RestLiCallback callback = getCallback();
    ResourceMethodDescriptor methodDescriptor = asyncStatusResourceModel.findMethod(ResourceMethod.GET);
    AsyncStatusCollectionResource asyncStatusResource = getMockResource(AsyncStatusCollectionResource.class);
    asyncStatusResource.get(eq(1L), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Status> callback = (Callback<Status>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(asyncStatusResource);
    checkAsyncInvocation(asyncStatusResource,
        callback,
        methodDescriptor,
        "GET",
        version,
        "/asyncstatuses/1",
        null,
        buildPathKeys("statusID", 1L),
        true);
  }

  @Test
  public void testParseqTracePromise() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        PromiseStatusCollectionResource.class);
    ResourceModel promiseStatusResourceModel = resourceModelMap.get("/promisestatuses");

    // #3: Promise based Async Method Execution
    RequestContext promiseRequestContext = new RequestContext();
    ResourceMethodDescriptor methodDescriptor = promiseStatusResourceModel.findMethod(ResourceMethod.GET);
    PromiseStatusCollectionResource promiseStatusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(promiseStatusResource.get(eq(1L))).andReturn(Promises.value(null)).once();
    checkInvocation(promiseStatusResource,
        promiseRequestContext,
        methodDescriptor,
        "GET",
        version,
        "/promisestatuses/1",
        null,
        buildPathKeys("statusID", 1L),
        new Callback<RestResponse>()
        {
          //A 404 is considered an error by rest.li
          @Override
          public void onError(Throwable e)
          {
            Assert.assertNotNull(promiseRequestContext.getLocalAttr(ATTRIBUTE_PARSEQ_TRACE), "There should be a valid parseq trace!");
          }

          @Override
          public void onSuccess(RestResponse result)
          {
            Assert.fail("Request failed unexpectedly.");
          }
        },
        true, false);
  }

  @Test
  public void testParseqTraceTask() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        TaskStatusCollectionResource.class);
    ResourceModel taskStatusResourceModel = resourceModelMap.get("/taskstatuses");

    // #4: Task based Async Method Execution
    RequestContext taskRequestContext = new RequestContext();
    ResourceMethodDescriptor methodDescriptor = taskStatusResourceModel.findMethod(ResourceMethod.GET);
    TaskStatusCollectionResource taskStatusResource = getMockResource(TaskStatusCollectionResource.class);
    EasyMock.expect(taskStatusResource.get(eq(1L))).andReturn(
        Task.callable(
            "myTask",
            new Callable<Status>()
            {
              @Override
              public Status call() throws Exception
              {
                return new Status();
              }
            })).once();

    checkInvocation(taskStatusResource,
        taskRequestContext,
        methodDescriptor,
        "GET",
        version,
        "/taskstatuses/1",
        null,
        buildPathKeys("statusID", 1L),
        new Callback<RestResponse>()
        {
          @Override
          public void onError(Throwable e)
          {
            Assert.fail("Request failed unexpectedly.");
          }

          @Override
          public void onSuccess(RestResponse result)
          {
            Assert.assertNotNull(taskRequestContext.getLocalAttr(ATTRIBUTE_PARSEQ_TRACE));
          }
        }, true, false);
  }



  @DataProvider
  public Object[][] promiseMethodConfigProviders()
  {
    return new Object[][]
        {
          {
            new RestLiMethodConfigBuilder().build(),
            null
          }, // empty map
          {
            new RestLiMethodConfigBuilder().addTimeoutMs("*.*", 1000L).build(),
            "withTimeout 1000ms src: *.*"
          }, // override default
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("promisestatuses.*", 1100L)
                    .addTimeoutMs("greetings.*", 2000L).build(),
            "withTimeout 1100ms src: promisestatuses.*"
          }, // resource name
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("*.GET", 1200L)
                    .addTimeoutMs("*.DELETE", 2000L).build(),
            "withTimeout 1200ms src: *.GET"
          }, // operation name
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("promisestatuses.GET", 1200L)
                    .addTimeoutMs("promisestatuses.FINDER-public_timeline", 2000L).build(),
            "withTimeout 1200ms src: promisestatuses.GET"
          }, // operation type
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("*.*", 500L)
                    .addTimeoutMs("*.GET", 1000L)
                    .addTimeoutMs("promisestatuses.*", 2000L)
                    .addTimeoutMs("promisestatuses.GET", 2500L).build(),
            "withTimeout 2500ms src: promisestatuses.GET"
          } // multiple configuration precedence
        };
  }

  @Test(dataProvider = "promiseMethodConfigProviders")
  public void testTimeoutParseqTracePromise(RestLiMethodConfig restliMethodConfig, String timeoutTaskName) throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
            PromiseStatusCollectionResource.class);
    ResourceModel promiseStatusResourceModel = resourceModelMap.get("/promisestatuses");

    // Promise based Async Method Execution
    RequestContext promiseRequestContext = new RequestContext();
    ResourceMethodDescriptor methodDescriptor = promiseStatusResourceModel.findMethod(ResourceMethod.GET);
    PromiseStatusCollectionResource promiseStatusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(promiseStatusResource.get(eq(1L))).andReturn(Promises.value(new Status())).once();

    // configure method-level timeout
    ResourceMethodConfigProvider methodConfigProvider = ResourceMethodConfigProvider.build(restliMethodConfig);
    ResourceMethodConfig methodConfig = methodConfigProvider.apply(methodDescriptor);
    checkInvocation(promiseStatusResource,
            promiseRequestContext,
            methodDescriptor,
            methodConfig,
            "GET",
            version,
            "/promisestatuses/1",
            null,
            buildPathKeys("statusID", 1L),
            new Callback<RestResponse>()
            {
              @Override
              public void onError(Throwable e)
              {
                Assert.fail("Request failed unexpectedly.");
              }

              @Override
              public void onSuccess(RestResponse result)
              {
                Trace parseqTrace = (Trace)promiseRequestContext.getLocalAttr(ATTRIBUTE_PARSEQ_TRACE);
                Assert.assertNotNull(parseqTrace);
                if (timeoutTaskName != null)
                {
                  Assert.assertTrue(hasTask(timeoutTaskName, parseqTrace));
                }
              }
            },
            true, false, null, null);
  }

  @DataProvider
  public Object[][] taskMethodConfigProviders()
  {
    return new Object[][]
        {
          {
            new RestLiMethodConfigBuilder().build(),
            null
          }, // empty map
          {
            new RestLiMethodConfigBuilder().addTimeoutMs("*.*", 1000L).build(),
            "withTimeout 1000ms src: *.*"
          }, // override default
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("taskstatuses.*", 1100L)
                    .addTimeoutMs("greetings.*", 2000L).build(),
            "withTimeout 1100ms src: taskstatuses.*"
          }, // resource name
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("*.GET", 1200L)
                    .addTimeoutMs("*.DELETE", 2000L).build(),
            "withTimeout 1200ms src: *.GET"
          }, // operation name
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("taskstatuses.GET", 1200L)
                    .addTimeoutMs("taskstatuses.ACTION-streamingAction", 2000L).build(),
            "withTimeout 1200ms src: taskstatuses.GET"
          }, // operation type
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("*.*", 500L)
                    .addTimeoutMs("*.GET", 1000L)
                    .addTimeoutMs("taskstatuses.*", 2000L)
                    .addTimeoutMs("taskstatuses.GET", 2500L).build(),
            "withTimeout 2500ms src: taskstatuses.GET"
          } // multiple configuration precedence
        };
  }

  @Test(dataProvider = "taskMethodConfigProviders")
  public void testTimeoutParseqTraceTask(RestLiMethodConfig restliMethodConfig, String timeoutTaskName) throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
            TaskStatusCollectionResource.class);
    ResourceModel taskStatusResourceModel = resourceModelMap.get("/taskstatuses");

    // #4: Task based Async Method Execution
    RequestContext taskRequestContext = new RequestContext();
    ResourceMethodDescriptor methodDescriptor = taskStatusResourceModel.findMethod(ResourceMethod.GET);
    TaskStatusCollectionResource taskStatusResource = getMockResource(TaskStatusCollectionResource.class);
    EasyMock.expect(taskStatusResource.get(eq(1L))).andReturn(
            Task.callable(
                    "myTask",
                    new Callable<Status>()
                    {
                      @Override
                      public Status call() throws Exception
                      {
                        return new Status();
                      }
                    })).once();

    // configure method-level timeout
    ResourceMethodConfigProvider methodConfigProvider = ResourceMethodConfigProvider.build(restliMethodConfig);
    ResourceMethodConfig methodConfig = methodConfigProvider.apply(methodDescriptor);
    checkInvocation(taskStatusResource,
            taskRequestContext,
            methodDescriptor,
            methodConfig,
            "GET",
            version,
            "/taskstatuses/1",
            null,
            buildPathKeys("statusID", 1L),
            new Callback<RestResponse>()
            {
              @Override
              public void onError(Throwable e)
              {
                Assert.fail("Request failed unexpectedly.");
              }

              @Override
              public void onSuccess(RestResponse result)
              {
                Trace parseqTrace = (Trace)taskRequestContext.getLocalAttr(ATTRIBUTE_PARSEQ_TRACE);
                Assert.assertNotNull(parseqTrace);
                if (timeoutTaskName != null)
                {
                  Assert.assertTrue(hasTask(timeoutTaskName, parseqTrace));
                }
              }
            }, true, false, null, null);
  }

  public static boolean hasTask(final String name, final Trace trace)
  {
    return trace.getTraceMap().values().stream().anyMatch(shallowTrace -> shallowTrace.getName().equals(name));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testBatchUpdateCollection() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchUpdateRequest batchUpdateRequest = EasyMock.anyObject();
    EasyMock.expect(statusResource.batchUpdate(batchUpdateRequest)).andReturn(null).once();
    String body = RestLiTestHelper.doubleQuote("{'entities':{'1':{},'2':{}}}");
    checkInvocation(statusResource,
                    methodDescriptor,
                    "PUT",
                    version,
                    "/statuses?ids=List(1,2)",
                    body,
                    buildBatchPathKeys(1L, 2L));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchUpdateComplexKey")
  public void testBatchUpdateComplexKey(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel discoveredItemsResourceModel = buildResourceModel(DiscoveredItemsResource.class);
    ResourceMethodDescriptor methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    DiscoveredItemsResource discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    BatchUpdateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> batchUpdateRequest = EasyMock.anyObject();
    @SuppressWarnings("unchecked")
    BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> batchUpdateResult =
        discoveredItemsResource.batchUpdate(batchUpdateRequest);
    EasyMock.expect(batchUpdateResult).andReturn(null).once();

    checkInvocation(discoveredItemsResource, methodDescriptor, "PUT", version, uri, body, buildBatchPathKeys(keyA, keyB));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testBatchPatchCollection() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    StatusCollectionResource statusResource = getMockResource(StatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchPatchRequest batchPatchRequest = EasyMock.anyObject();
    EasyMock.expect(statusResource.batchUpdate(batchPatchRequest)).andReturn(null).once();
    String body = RestLiTestHelper.doubleQuote("{'entities':{'1':{},'2':{}}}");
    checkInvocation(statusResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/statuses?ids=List(1,2)",
                    body,
                    buildBatchPathKeys(1L, 2L));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchUpdateComplexKey")
  public void testBatchPatchComplexKey(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel discoveredItemsResourceModel = buildResourceModel(DiscoveredItemsResource.class);
    ResourceMethodDescriptor methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    DiscoveredItemsResource discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    BatchPatchRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> batchPatchRequest = EasyMock.anyObject();
    @SuppressWarnings("unchecked")
    BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> batchUpdateResult =
        discoveredItemsResource.batchUpdate(batchPatchRequest);
    EasyMock.expect(batchUpdateResult).andReturn(null).once();

    checkInvocation(discoveredItemsResource, methodDescriptor, "POST", version, uri, body, buildBatchPathKeys(keyA, keyB));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testBatchCreate() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(DiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1 Batch create on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchCreateRequest batchCreateRequest = EasyMock.anyObject();
    EasyMock.expect(statusResource.batchCreate(batchCreateRequest)).andReturn(null).once();
    String body = RestLiTestHelper.doubleQuote("{'elements':[{},{}]}");
    checkInvocation(statusResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/statuses",
                    body,
                    buildBatchPathKeys());

    // #2 Batch create on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    batchCreateRequest = EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchCreate(batchCreateRequest)).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    version,
                    "/discovereditems",
                    body,
                    buildBatchPathKeys());
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testBatchDelete() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(DiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1 Batch delete on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    statusResource = getMockResource(StatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchDeleteRequest batchDeleteRequest = EasyMock.anyObject();
    EasyMock.expect(statusResource.batchDelete(batchDeleteRequest)).andReturn(null).once();
    checkInvocation(statusResource,
                    methodDescriptor,
                    "DELETE",
                    version,
                    "/statuses?ids=List(1,2)",
                    "",
                    buildBatchPathKeys(1L, 2L));

    // #2 Batch delete on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    batchDeleteRequest = EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchDelete(batchDeleteRequest)).andReturn(null).once();

    String uri = "/discovereditems?ids=List((itemId:1,type:2,userId:3),(itemId:4,type:5,userId:6))";

    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "DELETE",
                    version,
                    uri,
                    buildBatchPathKeys(keyA, keyB));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionGet")
  public Object[][] paramCollectionGet() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/test/foo?intParam=1&stringParam=bar" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/test/foo?intParam=1&stringParam=bar" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionGet")
  public void testCustomCrudParamsCollectionGet(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.GET);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myGet(eq("foo"), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "GET", version, uri, buildPathKeys("testId", "foo"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchGet")
  public Object[][] paramCollectionBatchGet() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/test?ids=foo&ids=bar&ids=baz&intParam=1&stringParam=qux" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/test?ids=List(foo,bar,baz)&intParam=1&stringParam=qux" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchGet")
  public void testCustomCrudParamsCollectionBatchGet(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.BATCH_GET);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myBatchGet((Set<String>)Matchers.eqCollectionUnordered(Sets.newHashSet("foo", "bar", "baz")), eq(1), eq("qux"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "GET", version, uri, buildBatchPathKeys("foo", "bar", "baz"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionCreate")
  public Object[][] paramCollectionCreate() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/test?intParam=1&stringParam=bar" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/test?intParam=1&stringParam=bar" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionCreate")
  public void testCustomCrudParamsCollectionCreate(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.CREATE);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myCreate((CombinedTestDataModels.Foo) EasyMock.anyObject(), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "POST", version, uri, "{}");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchCreate")
  public Object[][] paramCollectionBatchCreate() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/test?intParam=1&stringParam=bar", "{\"elements\":[{},{}]}" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/test?intParam=1&stringParam=bar", "{\"elements\":[{},{}]}" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchCreate")
  public void testCustomCrudParamsCollectionBatchCreate(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.BATCH_CREATE);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    @SuppressWarnings("rawtypes")
    BatchCreateRequest batchCreateRequest = EasyMock.anyObject();
    @SuppressWarnings("unchecked")
    BatchCreateResult<String, CombinedTestDataModels.Foo> batchCreateResult =
        resource.myBatchCreate(batchCreateRequest, eq(1), eq("bar"));
    EasyMock.expect(batchCreateResult).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "POST", version, uri, body, buildBatchPathKeys());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionUpdate")
  public Object[][] paramCollectionUpdate() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/test?intParam=1&stringParam=bar" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/test?intParam=1&stringParam=bar" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionUpdate")
  public void testCustomCrudParamsCollectionUpdate(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.UPDATE);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myUpdate(eq("foo"), EasyMock.anyObject(CombinedTestDataModels.Foo.class), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "PUT", version, uri, "{}", buildPathKeys("testId", "foo"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchUpdate")
  public Object[][] paramCollectionBatchUpdate() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/test?ids=foo&ids=bar&intParam=1&stringParam=baz", "{\"entities\":{\"foo\":{},\"bar\":{}}}" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/test?ids=List(foo,bar)&intParam=1&stringParam=baz", "{\"entities\":{\"foo\":{},\"bar\":{}}}" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchUpdate")
  public void testCustomCrudParamsCollectionBatchUpdate(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.BATCH_UPDATE);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    @SuppressWarnings("rawtypes")
    BatchUpdateRequest batchUpdateRequest = EasyMock.anyObject();
    @SuppressWarnings("unchecked")
    BatchUpdateResult<String, CombinedTestDataModels.Foo> batchUpdateResult =
        resource.myBatchUpdate(batchUpdateRequest, eq(1), eq("baz"));
    EasyMock.expect(batchUpdateResult).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "PUT", version, uri, body, buildBatchPathKeys("foo", "bar"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionPartialUpdate")
  public Object[][] paramCollectionPartialUpdate() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/test/foo?intParam=1&stringParam=bar", "{\"patch\":{\"$set\":{\"foo\":42}}}" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/test/foo?intParam=1&stringParam=bar", "{\"patch\":{\"$set\":{\"foo\":42}}}" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionPartialUpdate")
  public void testCustomCrudParamsCollectionPartialUpdate(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.PARTIAL_UPDATE);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(42));
    PatchRequest<CombinedTestDataModels.Foo> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(resource.myUpdate(eq("foo"), eq(expected), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "POST", version, uri, body, buildPathKeys("testId", "foo"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchPartialUpdate")
  public Object[][] paramCollectionBatchPartialUpdate() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/test?ids=foo&ids=bar&intParam=1&stringParam=baz", "{\"entities\":{\"foo\":{},\"bar\":{}}}" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/test?ids=List(foo,bar)&intParam=1&stringParam=baz", "{\"entities\":{\"foo\":{},\"bar\":{}}}" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchPartialUpdate")
  public void testCustomCrudParamsCollectionBatchPartialUpdate(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    @SuppressWarnings("rawtypes")
    BatchPatchRequest batchPatchRequest = EasyMock.anyObject();
    @SuppressWarnings("unchecked")
    BatchUpdateResult<String, CombinedTestDataModels.Foo> batchUpdateResult =
        resource.myBatchUpdate(batchPatchRequest, eq(1), eq("baz"));
    EasyMock.expect(batchUpdateResult).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "POST", version, uri, body, buildBatchPathKeys("foo", "bar"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionDelete")
  public Object[][] paramCollectionDelete() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/test/foo?intParam=1&stringParam=bar" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/test/foo?intParam=1&stringParam=bar" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionDelete")
  public void testCustomCrudParamCollectionDelete(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.DELETE);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myDelete(eq("foo"), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "DELETE", version, uri, buildPathKeys("testId", "foo"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchDelete")
  public Object[][] paramCollectionBatchDelete() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses?ids=foo&ids=bar&intParam=1&stringParam=baz" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses?ids=List(foo,bar)&intParam=1&stringParam=baz" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramCollectionBatchDelete")
  public void testCustomCrudParamsCollectionBatchDelete(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.BATCH_DELETE);
    CombinedResources.CollectionWithCustomCrudParams resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    @SuppressWarnings("rawtypes")
    BatchDeleteRequest batchDeleteRequest = EasyMock.anyObject();
    @SuppressWarnings("unchecked")
    BatchUpdateResult<String, CombinedTestDataModels.Foo> batchUpdateResult =
        resource.myBatchDelete(batchDeleteRequest, eq(1), eq("baz"));
    EasyMock.expect(batchUpdateResult).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "DELETE", version, uri, "", buildBatchPathKeys("foo", "bar"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramSimple")
  public Object[][] paramSimpleGet() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/test?intParam=1&stringParam=bar" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/test?intParam=1&stringParam=bar" }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramSimple")
  public void testCustomCrudParamsSimpleGet(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.GET);
    CombinedResources.SimpleResourceWithCustomCrudParams resource = getMockResource(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    EasyMock.expect(resource.myGet(eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "GET", version, uri, buildBatchPathKeys());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramSimple")
  public void testCustomCrudParamsSimpleUpdate(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.UPDATE);
    CombinedResources.SimpleResourceWithCustomCrudParams resource = getMockResource(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    EasyMock.expect(resource.myUpdate(EasyMock.anyObject(), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "PUT", version, uri, "{}", buildBatchPathKeys());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramSimple")
  public void testCustomCrudParamsSimplePartialUpdate(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.PARTIAL_UPDATE);
    CombinedResources.SimpleResourceWithCustomCrudParams resource = getMockResource(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(51));
    PatchRequest<CombinedTestDataModels.Foo> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(resource.myPartialUpdate(eq(expected), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "POST", version, uri,"{\"patch\":{\"$set\":{\"foo\":51}}}", buildBatchPathKeys());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "paramSimple")
  public void testCustomCrudParamsSimpleDelete(ProtocolVersion version, String uri) throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor = model.findMethod(ResourceMethod.DELETE);
    CombinedResources.SimpleResourceWithCustomCrudParams resource = getMockResource(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    EasyMock.expect(resource.myDelete(eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "DELETE",version, uri, buildBatchPathKeys());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchUpdateCompoundKey")
  public Object[][] batchUpdateCompoundKey()
  {
    return new Object[][]
        {
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/asyncfollows?ids=followeeID%3D2%26followerID%3D1&ids=followeeID%3D4%26followerID%3D3&ids=followeeID%3D6%26followerID%3D5))",
                "{\"entities\":{\"followeeID=2&followerID=1\": {}, \"followeeID=4&followerID=3\": {}, \"followeeID=6&followerID=5\": {} }}"
            },
            // With entity and query parameter key fields arranged in random order
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/asyncfollows?ids=followeeID%3D2%26followerID%3D1&ids=followerID%3D3%26followeeID%3D4&ids=followeeID%3D6%26followerID%3D5))",
                "{\"entities\":{\"followerID=1&followeeID=2\": {}, \"followeeID=4&followerID=3\": {}, \"followerID=5&followeeID=6\": {} }}"
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/asyncfollows?ids=List((followeeID:2,followerID:1),(followeeID:4,followerID:3),(followeeID:6,followerID:5))",
                "{\"entities\":{\"(followeeID:2,followerID:1)\": {}, \"(followeeID:4,followerID:3)\": {}, \"(followeeID:6,followerID:5)\": {} }}"
            },
            // With entity and query parameter key fields arranged in random order
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/asyncfollows?ids=List((followeeID:2,followerID:1),(followerID:3,followeeID:4),(followeeID:6,followerID:5))",
                "{\"entities\":{\"(followerID:1,followeeID:2)\": {}, \"(followeeID:4,followerID:3)\": {}, \"(followerID:5,followeeID:6)\": {} }}"
            },
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchUpdateCompoundKey")
  public void testAsyncBatchUpdateAssociativeResource(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    @SuppressWarnings("unchecked")
    BatchUpdateRequest<CompoundKey, Followed> mockBatchUpdateReq =
        EasyMock.anyObject();
    resource.batchUpdate(mockBatchUpdateReq, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer()
          throws Throwable
      {
        @SuppressWarnings("unchecked")
        Callback<BatchUpdateResult<CompoundKey, Followed>> callback =
            (Callback<BatchUpdateResult<CompoundKey, Followed>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(resource);

    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         version,
                         uri,
                         body,
                         buildBatchPathKeys(buildFollowsCompoundKey(1L, 2L),
                                            buildFollowsCompoundKey(3L, 4L),
                                            buildFollowsCompoundKey(5L, 6L)));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchUpdateCompoundKey")
  public void testAsyncBatchPatchAssociativeResource(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    @SuppressWarnings("unchecked")
    BatchPatchRequest<CompoundKey, Followed> mockBatchPatchReq =
         EasyMock.anyObject();
    resource.batchUpdate(mockBatchPatchReq, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer()
          throws Throwable
      {
        @SuppressWarnings("unchecked")
        Callback<BatchUpdateResult<CompoundKey, Followed>> callback =
            (Callback<BatchUpdateResult<CompoundKey, Followed>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(resource);
    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         uri,
                         body,
                         buildBatchPathKeys(buildFollowsCompoundKey(1L, 2L),
                                            buildFollowsCompoundKey(3L, 4L),
                                            buildFollowsCompoundKey(5L, 6L)));
  }

  @Test
  public void testAsyncBatchDeleteAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    @SuppressWarnings("unchecked")
    BatchDeleteRequest<CompoundKey, Followed> mockBatchDeleteReq =
        EasyMock.anyObject();
    resource.batchDelete(mockBatchDeleteReq, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        @SuppressWarnings("unchecked") Callback<BatchUpdateResult<CompoundKey, Followed>> callback = (Callback<BatchUpdateResult<CompoundKey, Followed>>) EasyMock
            .getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(resource);

    String uri = "/asyncfollows?ids=List((followeeID:2,followerID:1),(followeeID:4,followerID:3),(followeeID:6,followerID:5))";

    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "DELETE",
                         version,
                         uri,
                         buildBatchPathKeys(buildFollowsCompoundKey(1L, 2L),
                                            buildFollowsCompoundKey(3L, 4L),
                                            buildFollowsCompoundKey(5L, 6L)));
  }

  @Test
  public void testAsyncGetAllAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.GET_ALL);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    resource.getAll(EasyMock.anyObject(), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        @SuppressWarnings("unchecked") Callback<List<Followed>> callback = (Callback<List<Followed>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(resource);

    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncfollows",
                         buildBatchPathKeys());
  }

  @Test
  public void testAsyncBatchCreateComplexKeyResource() throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    BatchCreateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> mockBatchCreateReq =
        EasyMock.anyObject();
    discoveredResource.batchCreate(mockBatchCreateReq,
                                   EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<BatchCreateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback =
            (Callback<BatchCreateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(discoveredResource);

    String uri = "/asyncdiscovereditems";

    String entityBody = "{}";

    checkAsyncInvocation(discoveredResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         uri,
                         entityBody,
                         buildBatchPathKeys());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "asyncBatchUpdateComplexKey")
  public Object[][] asyncBatchUpdateComplexKey2()
  {
    return new Object[][]
        {
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids[0].itemId=1&ids[0].type=1&ids[0].userId=1&ids[1].itemId=2&ids[1].type=2&ids[1].userId=2&ids[2].itemId=3&ids[2].type=3&ids[2].userId=3",
                "{\"entities\":{\"itemId=1&type=1&userId=1\":{}, \"itemId=2&type=2&userId=2\":{}, \"itemId=3&type=3&userId=3\":{} }}"
            },
            // With entity and query parameter key fields arranged in random order
            {
                AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
                "/promisediscovereditems?ids[0].itemId=1&ids[0].type=1&ids[0].userId=1&ids[1].itemId=2&ids[1].type=2&ids[1].userId=2&ids[2].itemId=3&ids[2].type=3&ids[2].userId=3",
                "{\"entities\":{\"type=1&userId=1&itemId=1\":{}, \"userId=2&itemId=2&type=2\":{}, \"userId=3&type=3&itemId=3\":{} }}"
            },
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/asyncdiscovereditems?ids=List((itemId:1,type:1,userId:1),(itemId:2,type:2,userId:2),(itemId:3,type:3,userId:3))",
                "{\"entities\":{\"(itemId:1,type:1,userId:1)\":{}, \"(itemId:2,type:2,userId:2)\":{}, \"(itemId:3,type:3,userId:3)\": {} }}"
            },
            // With entity and query parameter key fields arranged in random order
            {
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "/asyncdiscovereditems?ids=List((itemId:1,type:1,userId:1),(userId:2,type:2,itemId:2),(itemId:3,type:3,userId:3))",
                "{\"entities\":{\"(type:1,userId:1,itemId:1)\":{}, \"(userId:2,itemId:2,type:2)\":{}, \"(userId:3,type:3,itemId:3)\": {} }}"
            }
        };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "asyncBatchUpdateComplexKey")
  public void testAsyncBatchUpdateComplexKeyResource(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    BatchUpdateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> mockBatchUpdateReq =
        EasyMock.anyObject();
    discoveredResource.batchUpdate(mockBatchUpdateReq,
                                   EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback =
            (Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(discoveredResource);
    checkAsyncInvocation(discoveredResource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         version,
                         uri,
                         body,
                         buildBatchPathKeys(getDiscoveredItemComplexKey(1, 1, 1),
                                            getDiscoveredItemComplexKey(2, 2, 2),
                                            getDiscoveredItemComplexKey(3, 3, 3)));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "asyncBatchUpdateComplexKey")
  public void testAsyncBatchPatchComplexKeyResource(ProtocolVersion version, String uri, String body) throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    BatchPatchRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> mockBatchPatchReq =
         EasyMock.anyObject();
    discoveredResource.batchUpdate(mockBatchPatchReq,
                                   EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback =
            (Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(discoveredResource);

    checkAsyncInvocation(discoveredResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         version,
                         uri,
                         body,
                         buildBatchPathKeys(getDiscoveredItemComplexKey(1, 1, 1),
                                            getDiscoveredItemComplexKey(2, 2, 2),
                                            getDiscoveredItemComplexKey(3, 3, 3)));
  }

  @Test
  public void testAsyncBatchDeleteComplexResource() throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    BatchDeleteRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> mockBatchDeleteReq =
        EasyMock.anyObject();
    discoveredResource.batchDelete(mockBatchDeleteReq,
                                   EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> callback =
            (Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(discoveredResource);

    String uri = "/asyncdiscovereditems?ids=List((itemId:1,type:1,userId:1),(itemId:2,type:2,userId:2),(itemId:3,type:3,userId:3))";

    checkAsyncInvocation(discoveredResource,
                         callback,
                         methodDescriptor,
                         "DELETE",
                         version,
                         uri,
                         buildBatchPathKeys(getDiscoveredItemComplexKey(1, 1, 1),
                                            getDiscoveredItemComplexKey(2, 2, 2),
                                            getDiscoveredItemComplexKey(3, 3, 3)));
  }

  @Test
  public void testInvokeWithUnsupportedAcceptMimeType() throws Exception
  {
    RestRequestBuilder builder = new RestRequestBuilder(new URI(""))
        .addHeaderValue("Accept", "text/plain")
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());
    RestRequest request = builder.build();
    final RestLiAttachmentReader attachmentReader = new RestLiAttachmentReader(null);
    final CountDownLatch latch = new CountDownLatch(1);
    RestLiResponseHandler restLiResponseHandler = new RestLiResponseHandler();

    ServerResourceContext resourceContext = new ResourceContextImpl(new PathKeysImpl(),
        new RestRequestBuilder(URI.create(""))
            .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.LATEST_PROTOCOL_VERSION.toString())
            .build(),
        new RequestContext());
    resourceContext.setRequestAttachmentReader(attachmentReader);

    Callback<RestLiResponse> executionCallback = new Callback<RestLiResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        latch.countDown();
        Assert.assertTrue(e instanceof RestException);
        RestException ex = (RestException) e;
        Assert.assertEquals(ex.getResponse().getStatus(),
                            HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
        Assert.assertEquals(resourceContext.getRequestAttachmentReader(), attachmentReader);
        Assert.assertNull(resourceContext.getResponseAttachments());
      }
      @Override
      public void onSuccess(RestLiResponse result)
      {
        Assert.fail();
      }
    };

    try
    {
      RoutingResult routingResult = new RoutingResult(resourceContext, null);
      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
          Collections.emptySet(),
          routingResult.getContext());

      FilterChainDispatcher filterChainDispatcher = new FilterChainDispatcherImpl(routingResult, _invoker, null);
      FilterChainCallback filterChainCallback = new FilterChainCallbackImpl(null,
                                                                            restLiResponseHandler, executionCallback,
                                                                            _errorResponseBuilder);
      final RestLiCallback callback =
          new RestLiCallback(null,
                                     new RestLiFilterResponseContextFactory(request, null, restLiResponseHandler),
                                     new RestLiFilterChain(null, filterChainDispatcher, filterChainCallback));

      _invoker.invoke(null, routingResult, null, callback);
      latch.await();
    }
    catch (Exception e)
    {
      // exception is expected
      Assert.assertTrue(e instanceof RestLiServiceException);
    }
    Assert.assertNull(resourceContext.getResponseMimeType());
  }

  @Test
  public void testInvokeWithInvalidAcceptMimeType() throws Exception
  {
    RestRequestBuilder builder = new RestRequestBuilder(new URI(""))
        .addHeaderValue("Accept", "foo")
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());
    RestRequest request = builder.build();
    final CountDownLatch latch = new CountDownLatch(1);
    RestLiResponseHandler restLiResponseHandler = new RestLiResponseHandler();

    Callback<RestLiResponse> executionCallback = new Callback<RestLiResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        latch.countDown();
        Assert.assertTrue(e instanceof RestException);
        RestException ex = (RestException) e;
        Assert.assertEquals(ex.getResponse().getStatus(),
                            HttpStatus.S_400_BAD_REQUEST.getCode());
      }
      @Override
      public void onSuccess(RestLiResponse result)
      {
      }
    };

    ServerResourceContext context = new ResourceContextImpl();
    try
    {
      RoutingResult routingResult = new RoutingResult(context, null);
      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
          Collections.emptySet(),
          routingResult.getContext());
      FilterChainDispatcher filterChainDispatcher = new FilterChainDispatcherImpl(routingResult, _invoker, null);
      FilterChainCallback filterChainCallback = new FilterChainCallbackImpl(null,
                                                                            restLiResponseHandler, executionCallback,
                                                                            _errorResponseBuilder);
      final RestLiCallback callback = new RestLiCallback(null,
          new RestLiFilterResponseContextFactory(request, null, restLiResponseHandler),
          new RestLiFilterChain(null, filterChainDispatcher, filterChainCallback));
      _invoker.invoke(null, routingResult, null, callback);
      latch.await();
    }
    catch (Exception e)
    {
      // exception is expected
      Assert.assertTrue(e instanceof RestLiServiceException);
    }
    Assert.assertNull(context.getResponseMimeType());
  }

  @Test
  public void testAsyncGetAllComplexKeyResource() throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.GET_ALL);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    PagingContext mockCtx = EasyMock.anyObject();
    discoveredResource.getAll(mockCtx, EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<List<DiscoveredItem>> callback = (Callback<List<DiscoveredItem>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(discoveredResource);

    checkAsyncInvocation(discoveredResource,
                         callback,
                         methodDescriptor,
                         "GET",
                         version,
                         "/asyncdiscovereditems",
                         buildBatchPathKeys());
  }

  @Test
  public void testStreaming() throws Exception
  {
    Map<String, ResourceModel> resourceModelMap = buildResourceModels(
        StatusCollectionResource.class,
        AsyncStatusCollectionResource.class,
        PromiseStatusCollectionResource.class,
        TaskStatusCollectionResource.class);

    final String payload = "{\"metadata\": \"someMetadata\"}";
    ResourceModel statusResourceModel = resourceModelMap.get("/statuses");
    ResourceModel asyncStatusResourceModel = resourceModelMap.get("/asyncstatuses");
    ResourceModel promiseStatusResourceModel = resourceModelMap.get("/promisestatuses");
    ResourceModel taskStatusResourceModel = resourceModelMap.get("/taskstatuses");

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    AsyncStatusCollectionResource asyncStatusResource;
    PromiseStatusCollectionResource promiseStatusResource;
    TaskStatusCollectionResource taskStatusResource;

    //Sync Method Execution - Successful scenario
    methodDescriptor = statusResourceModel.findActionMethod("streamingAction", ResourceLevel.COLLECTION);
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.streamingAction(EasyMock.anyObject(), EasyMock.anyObject()))
        .andReturn(1234l).once();
    checkInvocation(statusResource, new RequestContext(), methodDescriptor,
                    "POST",
                    version,
                    "/statuses/?action=streamingAction",
                    payload,
                    null,
                    new Callback<RestResponse>()
                    {
                      @Override
                      public void onError(Throwable e)
                      {
                        Assert.fail("Request failed unexpectedly.");
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                      }
                    },
                    false,
                    false,
                    new RestLiAttachmentReader(null),
                    new RestLiResponseAttachments.Builder().build());

    //Sync Method Execution - Error scenario
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.streamingAction(EasyMock.anyObject(), EasyMock.anyObject()))
        .andThrow(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR)).once();
    checkInvocation(statusResource, new RequestContext(), methodDescriptor,
                    "POST",
                    version,
                    "/statuses/?action=streamingAction",
                    payload,
                    null,
                    new Callback<RestResponse>()
                    {
                      @Override
                      public void onError(Throwable e)
                      {
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                        Assert.fail("Request passed unexpectedly.");
                      }
                    },
                    false,
                    false,
                    new RestLiAttachmentReader(null),
                    new RestLiResponseAttachments.Builder().build());

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Callback Method Execution - Successful scenario
    methodDescriptor = asyncStatusResourceModel.findMethod(ResourceMethod.ACTION);
    asyncStatusResource = getMockResource(AsyncStatusCollectionResource.class);
    asyncStatusResource.streamingAction(EasyMock.anyObject(), EasyMock.anyObject(),
                                        EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Long> callback = (Callback<Long>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(1234l);
        return null;
      }
    });
    checkInvocation(asyncStatusResource, new RequestContext(), methodDescriptor,
                    "POST",
                    version,
                    "/asyncstatuses/?action=streamingAction",
                    payload,
                    null,
                    new Callback<RestResponse>()
                    {
                      @Override
                      public void onError(Throwable e)
                      {
                        Assert.fail("Request failed unexpectedly.");
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                      }
                    },
                    false,
                    false,
                    new RestLiAttachmentReader(null),
                    new RestLiResponseAttachments.Builder().build());

    //Callback Method Execution - Error scenario
    asyncStatusResource = getMockResource(AsyncStatusCollectionResource.class);
    asyncStatusResource.streamingAction(EasyMock.anyObject(), EasyMock.anyObject(),
                                        EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        @SuppressWarnings("unchecked")
        Callback<Long> callback = (Callback<Long>) EasyMock.getCurrentArguments()[2];
        callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
        return null;
      }
    });
    checkInvocation(asyncStatusResource, new RequestContext(), methodDescriptor,
                    "POST",
                    version,
                    "/asyncstatuses/?action=streamingAction",
                    payload,
                    null,
                    new Callback<RestResponse>()
                    {
                      @Override
                      public void onError(Throwable e)
                      {
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                        Assert.fail("Request passed unexpectedly.");
                      }
                    },
                    false,
                    false,
                    new RestLiAttachmentReader(null),
                    new RestLiResponseAttachments.Builder().build());

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Promise Method Execution - Successful scenario
    methodDescriptor = promiseStatusResourceModel.findActionMethod("streamingAction", ResourceLevel.COLLECTION);
    promiseStatusResource = getMockResource(PromiseStatusCollectionResource.class);
    promiseStatusResource.streamingAction(EasyMock.anyObject(), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        final SettablePromise<Long> result = Promises.settable();
        final Runnable requestHandler = new Runnable()
        {
          public void run ()
          {
            try
            {
              result.done(1234l);
            }
            catch (final Throwable throwable)
            {
              result.fail(throwable);
            }
          }
        };
        _scheduler.schedule(requestHandler, 0, TimeUnit.MILLISECONDS);
        return result;
      }
    });
    checkInvocation(promiseStatusResource, new RequestContext(), methodDescriptor,
                    "POST",
                    version,
                    "/promisestatuses/?action=streamingAction",
                    payload,
                    null,
                    new Callback<RestResponse>()
                    {
                      @Override
                      public void onError(Throwable e)
                      {
                        Assert.fail("Request failed unexpectedly.");
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                      }
                    },
                    false,
                    false,
                    new RestLiAttachmentReader(null),
                    new RestLiResponseAttachments.Builder().build());

    //Promise Method Execution - Error scenario
    promiseStatusResource = getMockResource(PromiseStatusCollectionResource.class);
    promiseStatusResource.streamingAction(EasyMock.anyObject(), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        final SettablePromise<Long> result = Promises.settable();
        final Runnable requestHandler = new Runnable()
        {
          public void run ()
          {
            result.fail(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
          }
        };
        _scheduler.schedule(requestHandler, 0, TimeUnit.MILLISECONDS);
        return result;
      }
    });
    checkInvocation(promiseStatusResource, new RequestContext(), methodDescriptor,
                    "POST",
                    version,
                    "/promisestatuses/?action=streamingAction",
                    payload,
                    null,
                    new Callback<RestResponse>()
                    {
                      @Override
                      public void onError(Throwable e)
                      {
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                        Assert.fail("Request passed unexpectedly.");
                      }
                    },
                    false,
                    false,
                    new RestLiAttachmentReader(null),
                    new RestLiResponseAttachments.Builder().build());

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Task Method Execution - Successful scenario
    methodDescriptor = taskStatusResourceModel.findMethod(ResourceMethod.ACTION);
    taskStatusResource = getMockResource(TaskStatusCollectionResource.class);
    taskStatusResource.streamingAction(EasyMock.anyObject(), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        return new BaseTask<Long>()
        {
          protected Promise<Long> run(final Context context) throws Exception
          {
            return Promises.value(1234l);
          }
        };
      }
    });
    checkInvocation(taskStatusResource, new RequestContext(), methodDescriptor,
                    "POST",
                    version,
                    "/taskstatuses/?action=streamingAction",
                    payload,
                    null,
                    new Callback<RestResponse>()
                    {
                      @Override
                      public void onError(Throwable e)
                      {
                        Assert.fail("Request failed unexpectedly.");
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                      }
                    },
                    false,
                    false,
                    new RestLiAttachmentReader(null),
                    new RestLiResponseAttachments.Builder().build());

    //Task Method Execution - Error scenario
    taskStatusResource = getMockResource(TaskStatusCollectionResource.class);
    taskStatusResource.streamingAction(EasyMock.anyObject(), EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        return new BaseTask<Long>()
        {
          protected Promise<Long> run(final Context context) throws Exception
          {
            return Promises.error(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
          }
        };
      }
    });
    checkInvocation(taskStatusResource, new RequestContext(), methodDescriptor,
                    "POST",
                    version,
                    "/taskstatuses/?action=streamingAction",
                    payload,
                    null,
                    new Callback<RestResponse>()
                    {
                      @Override
                      public void onError(Throwable e)
                      {
                      }

                      @Override
                      public void onSuccess(RestResponse result)
                      {
                        Assert.fail("Request passed unexpectedly.");
                      }
                    },
                    false,
                    false,
                    new RestLiAttachmentReader(null),
                    new RestLiResponseAttachments.Builder().build());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextDefault")
  public Object[][] statusPagingContextDefault() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=public_timeline" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=public_timeline" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextStartOnly")
  public Object[][] statusPagingContextStartOnly() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=public_timeline&start=5" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=public_timeline&start=5" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextCountOnly")
  public Object[][] statusPagingContextCountOnly() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=public_timeline&count=4" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=public_timeline&count=4" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextBadCount")
  public Object[][] statusPagingContextBadCount() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=public_timeline&start=5&count=asdf" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=public_timeline&start=5&count=asdf" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextBadStart")
  public Object[][] statusPagingContextBadStart() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=public_timeline&start=asdf&count=4" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=public_timeline&start=asdf&count=4" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextNegativeCount")
  public Object[][] statusPagingContextNegativeCount() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=public_timeline&start=5&count=-1" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=public_timeline&start=5&count=-1" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusPagingContextNegativeStart")
  public Object[][] statusPagingContextNegativeStart() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=public_timeline&start=-1&count=4" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=public_timeline&start=-1&count=4" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusUserTimelineDefault")
  public Object[][] statusUserTimelineDefault() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=user_timeline" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=user_timeline" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusUserTimelineStartAndCount")
  public Object[][] statusUserTimelineStartAndCount() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=user_timeline&start=0&count=20" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=user_timeline&start=0&count=20" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinder")
  public Object[][] statusFinder() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=search&keywords=linkedin&since=1&type=REPLY" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=search&keywords=linkedin&since=1&type=REPLY" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinderOptionalParam")
  public Object[][] statusFinderOptionalParam() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=search&keywords=linkedin" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=search&keywords=linkedin" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusFinderOptionalBooleanParam")
  public Object[][] statusFinderOptionalBooleanParam() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=user_timeline&includeReplies=false" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=user_timeline&includeReplies=false" }
        };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "discoveredItemsFinder")
  public Object[][] discoveredItemsFinderOnComplexKey() throws Exception
  {
    return new Object[][]
        {
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "?q=user&userId=1" },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "?q=user&userId=1" }
        };
  }

  // *****************
  // Helper methods
  // *****************

  private MutablePathKeys buildPathKeys(Object... pathKeyValues) throws RestLiSyntaxException
  {
    MutablePathKeys result = new PathKeysImpl();
    for (int i = 0; i < pathKeyValues.length; i += 2)
    {
      result.append((String) pathKeyValues[i], pathKeyValues[i + 1]);
    }

    return result;
  }

  public MutablePathKeys buildBatchPathKeys(Object... batchKeys) throws RestLiSyntaxException
  {
    MutablePathKeys result = new PathKeysImpl();
    Set<Object> keys = new HashSet<Object>();

    for (Object batchKey : batchKeys)
    {
      keys.add(batchKey);
    }

    result.setBatchKeys(keys);
    return result;
  }

  private PagingContext buildPagingContext(Integer start, Integer count)
  {
    boolean hasStart = start!=null;
    boolean hasCount = count!=null;
    start = start == null ? RestConstants.DEFAULT_START : start;
    count = count == null ? RestConstants.DEFAULT_COUNT : count;

    return new PagingContext(start, count, hasStart, hasCount);
  }

  private void expectRoutingException(ResourceMethodDescriptor methodDescriptor,
                                      Object statusResource,
                                      String httpMethod,
                                      String uri,
                                      ProtocolVersion version) throws Exception
  {
    checkInvocation(statusResource, methodDescriptor, httpMethod, version, uri, true);
    reset(statusResource);
  }

  private <R> R getMockResource(Class<R> resourceClass)
  {
    R resource = _resourceFactory.getMock(resourceClass);
    EasyMock.reset(resource);
    EasyMock.makeThreadSafe(resource, true);
    if (BaseResource.class.isAssignableFrom(resourceClass))
    {
      BaseResource baseResource = (BaseResource)resource;
      baseResource.setContext(EasyMock.anyObject());
      EasyMock.expectLastCall().once();
    }
    return resource;
  }

  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               ProtocolVersion version,
                               String uri, boolean expectRoutingException)
      throws Exception
  {
    checkInvocation(resource, new RequestContext(), resourceMethodDescriptor, httpMethod,
                    version, uri, null, null, null, false, expectRoutingException);
  }

  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               ProtocolVersion version,
                               String uri)
      throws Exception
  {
    checkInvocation(resource, new RequestContext(), resourceMethodDescriptor, httpMethod,
                    version, uri, null, null, null, false, false);
  }

  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               ProtocolVersion version,
                               String uri,
                               String entityBody)
      throws Exception
  {
    checkInvocation(resource, new RequestContext(), resourceMethodDescriptor, httpMethod,
                    version, uri, entityBody, null, null, false, false);
  }

  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               ProtocolVersion version,
                               String uri,
                               MutablePathKeys pathkeys)
      throws Exception
  {
    checkInvocation(resource, new RequestContext(), resourceMethodDescriptor, httpMethod,
                    version, uri, null, pathkeys, null, false, false);
  }

  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               ProtocolVersion version,
                               String uri,
                               String entityBody,
                               MutablePathKeys pathkeys)
      throws Exception
  {
    checkInvocation(resource, new RequestContext(), resourceMethodDescriptor, httpMethod,
                    version, uri, entityBody, pathkeys, null, false, false);
  }

  private void checkInvocation(Object resource,
      RequestContext requestContext,
      ResourceMethodDescriptor resourceMethodDescriptor,
      String httpMethod,
      ProtocolVersion version,
      String uri,
      String entityBody,
      MutablePathKeys pathkeys,
      final Callback<RestResponse> callback,
      final boolean isDebugMode,
      final boolean expectRoutingException)
      throws Exception
  {
    checkInvocation(resource, requestContext, resourceMethodDescriptor, httpMethod, version,
                    uri, entityBody, pathkeys, callback, isDebugMode, expectRoutingException, null, null);
  }

  private void checkInvocation(Object resource,
                               RequestContext requestContext,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               ProtocolVersion version,
                               String uri,
                               String entityBody,
                               MutablePathKeys pathkeys,
                               final Callback<RestResponse> callback,
                               final boolean isDebugMode,
                               final boolean expectRoutingException,
                               final RestLiAttachmentReader expectedRequestAttachments,
                               final RestLiResponseAttachments expectedResponseAttachments)
          throws Exception
  {
    checkInvocation(resource, requestContext, resourceMethodDescriptor, new ResourceMethodConfigImpl(null , false, false), httpMethod, version,
            uri, entityBody, pathkeys, callback, isDebugMode, expectRoutingException, expectedRequestAttachments, expectedResponseAttachments);
  }

  private void checkInvocation(Object resource,
      RequestContext requestContext,
      ResourceMethodDescriptor resourceMethodDescriptor,
      ResourceMethodConfig resourceMethodConfig,
      String httpMethod,
      ProtocolVersion version,
      String uri,
      String entityBody,
      MutablePathKeys pathkeys,
      final Callback<RestResponse> callback,
      final boolean isDebugMode,
      final boolean expectRoutingException,
      final RestLiAttachmentReader expectedRequestAttachments,
      final RestLiResponseAttachments expectedResponseAttachments)
      throws Exception
  {
    assertNotNull(resource);
    assertNotNull(resourceMethodDescriptor);

    try
    {
      EasyMock.replay(resource);
      RestRequestBuilder builder =
          new RestRequestBuilder(new URI(uri)).setMethod(httpMethod).addHeaderValue("Accept", "application/json")
              .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());
      if (entityBody != null)
      {
        builder.setEntity(entityBody.getBytes(Data.UTF_8_CHARSET));
      }
      if (expectedResponseAttachments != null)
      {
        builder.addHeaderValue(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_MULTIPART_RELATED);
      }
      RestRequest request = builder.build();

      if (isDebugMode)
      {
        requestContext.putLocalAttr(RestLiMethodInvoker.ATTRIBUTE_PROMISE_LISTENER, new PromiseListener<Object>()
        {
          @Override
          public void onResolved(Promise<Object> promise)
          {
            // ParSeq debugging no longer uses this local attribute to pass the trace. This is simply to test the
            // PromiseListener is invoked with a task.
            if (promise instanceof Task)
            {
              requestContext.putLocalAttr(ATTRIBUTE_PARSEQ_TRACE, ((Task<?>) promise).getTrace());
            }
          }
        });
      }

      final ServerResourceContext resourceContext = new ResourceContextImpl(pathkeys, request, requestContext);
      resourceContext.setRequestAttachmentReader(expectedRequestAttachments);
      if (expectedResponseAttachments != null)
      {
        resourceContext.setResponseAttachments(expectedResponseAttachments);
      }
      RoutingResult routingResult = new RoutingResult(resourceContext, resourceMethodDescriptor, resourceMethodConfig);
      RestLiArgumentBuilder argumentBuilder = _methodAdapterRegistry.getArgumentBuilder(resourceMethodDescriptor.getType());
      RestLiRequestData requestData = argumentBuilder.extractRequestData(
          routingResult,
          entityBody != null && !entityBody.isEmpty() ? DataMapUtils.readMapWithExceptions(request) : null);
      FilterRequestContext filterContext = new FilterRequestContextInternalImpl(routingResult.getContext(),
          resourceMethodDescriptor,
          requestData);
      final CountDownLatch latch = new CountDownLatch(1);
      final CountDownLatch expectedRoutingExceptionLatch = new CountDownLatch(1);
      RestLiResponseHandler restLiResponseHandler = new RestLiResponseHandler();

      Callback<RestLiResponse> executionCallback = new Callback<RestLiResponse>()
      {
        @Override
        public void onError(Throwable e)
        {
          if (e.getCause() != null && e.getCause().getCause() instanceof RoutingException)
          {
            expectedRoutingExceptionLatch.countDown();
          }

          if (callback != null)
          {
            callback.onError(e);
          }

          Assert.assertEquals(resourceContext.getRequestAttachmentReader(), expectedRequestAttachments);
          Assert.assertEquals(resourceContext.getResponseAttachments(), expectedResponseAttachments);
          latch.countDown();
        }

        @Override
        public void onSuccess(final RestLiResponse result)
        {
          if (callback != null)
          {
            callback.onSuccess(ResponseUtils.buildResponse(routingResult, result));
          }

          Assert.assertEquals(resourceContext.getResponseAttachments(), expectedResponseAttachments);
          latch.countDown();
        }
      };

      FilterChainDispatcher filterChainDispatcher = new FilterChainDispatcherImpl(routingResult,
          _invoker,
          argumentBuilder);
      FilterChainCallback filterChainCallback = new FilterChainCallbackImpl(routingResult,
          restLiResponseHandler,
          executionCallback,
          _errorResponseBuilder);
      final RestLiCallback outerCallback =
          new RestLiCallback(filterContext,
                                     new RestLiFilterResponseContextFactory(request, routingResult, restLiResponseHandler),
                                     new RestLiFilterChain(null, filterChainDispatcher, filterChainCallback));
      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
          Collections.emptySet(),
          routingResult.getContext());
      _invoker.invoke(requestData, routingResult, argumentBuilder, outerCallback);
      try
      {
        latch.await();
        if (expectRoutingException)
        {
          expectedRoutingExceptionLatch.await();
        }
      }
      catch (InterruptedException e)
      {
        // Ignore
      }
      EasyMock.verify(resource);
      Assert.assertEquals((routingResult.getContext()).getResponseMimeType(),
                          "application/json");
    }
    catch (RestLiSyntaxException e)
    {
      throw new RoutingException("syntax exception", 400);
    }
    finally
    {
      EasyMock.reset(resource);
      EasyMock.makeThreadSafe(resource, true);
    }
  }

  private RestLiCallback getCallback()
  {
    @SuppressWarnings("unchecked")
    RestLiCallback callback = EasyMock.createMock(RestLiCallback.class);
    callback.onSuccess(EasyMock.anyObject());
    EasyMock.expectLastCall().once();
    EasyMock.replay(callback);
    return callback;
  }

  @SuppressWarnings("rawtypes")
  private void checkAsyncInvocation(BaseResource resource,
                                    RestLiCallback callback,
                                    ResourceMethodDescriptor methodDescriptor,
                                    String httpMethod,
                                    ProtocolVersion version,
                                    String uri,
                                    MutablePathKeys pathkeys) throws Exception
  {
    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         httpMethod,
                         version,
                         uri,
                         null,
                         pathkeys,
                         false);

  }

  @SuppressWarnings("rawtypes")
  private void checkAsyncInvocation(BaseResource resource,
                                    RestLiCallback callback,
                                    ResourceMethodDescriptor methodDescriptor,
                                    String httpMethod,
                                    ProtocolVersion version,
                                    String uri,
                                    String entityBody,
                                    MutablePathKeys pathkeys) throws Exception
  {
    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         httpMethod,
                         version,
                         uri,
                         entityBody,
                         pathkeys,
                         false);
  }

  @SuppressWarnings({"unchecked","rawtypes"})
  private void checkAsyncInvocation(BaseResource resource,
                                    RestLiCallback callback,
                                    ResourceMethodDescriptor methodDescriptor,
                                    String httpMethod,
                                    ProtocolVersion version,
                                    String uri,
                                    String entityBody,
                                    MutablePathKeys pathkeys,
                                    boolean isDebugMode) throws Exception
  {
    try
    {

      RestRequestBuilder builder =
          new RestRequestBuilder(new URI(uri)).setMethod(httpMethod).addHeaderValue("Accept", "application/x-pson")
              .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());
      if (entityBody != null)
      {
        builder.setEntity(entityBody.getBytes(Data.UTF_8_CHARSET));
      }
      RestRequest request = builder.build();
      RoutingResult routingResult =
          new RoutingResult(new ResourceContextImpl(pathkeys, request,
                                                    new RequestContext()), methodDescriptor);

      RestLiArgumentBuilder argumentBuilder = _methodAdapterRegistry.getArgumentBuilder(methodDescriptor.getType());
      RestLiRequestData requestData = argumentBuilder.extractRequestData(
          routingResult, entityBody != null ? DataMapUtils.readMapWithExceptions(request) : null);

      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
          Collections.emptySet(),
          routingResult.getContext());
      _invoker.invoke(requestData, routingResult, argumentBuilder, callback);
      EasyMock.verify(resource);
      EasyMock.verify(callback);
      Assert.assertEquals((routingResult.getContext()).getResponseMimeType(),
                          "application/x-pson");

    }
    catch (RestLiSyntaxException e)
    {
      throw new RoutingException("syntax exception", 400);
    }
    finally
    {
      EasyMock.reset(callback, resource);
      callback.onSuccess(EasyMock.anyObject());
      EasyMock.expectLastCall().once();
      EasyMock.replay(callback);
    }
  }

  /**
   * Helper method to return a Compound Key for the FollowsAssociativeResource and AsyncFollowsAssociativeResource
   * Sets the "followerID" to id1
   * Sets the "followeeID" to id2
   *
   * @param id1 the "followerID".
   * @param id2 the "followeeID"
   *
   * @return
   */
  private CompoundKey buildFollowsCompoundKey(Long id1, Long id2)
  {
    CompoundKey key = new CompoundKey();
    key.append("followerID", id1);
    key.append("followeeID", id2);
    return key;
  }

  private ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> getDiscoveredItemComplexKey(
      long itemId, int type, long userId)
  {
    return new ComplexResourceKey<>(
        new DiscoveredItemKey().setItemId(itemId).setType(type).setUserId(userId),
        new DiscoveredItemKeyParams());
  }
}
