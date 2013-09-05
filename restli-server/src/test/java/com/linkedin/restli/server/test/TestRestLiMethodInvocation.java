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


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.data.transform.patch.request.PatchOpFactory;
import com.linkedin.data.transform.patch.request.PatchTree;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.MutablePathKeys;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;
import com.linkedin.restli.internal.server.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiCallback;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.combined.CombinedResources;
import com.linkedin.restli.server.combined.CombinedTestDataModels;
import com.linkedin.restli.server.custom.types.CustomLong;
import com.linkedin.restli.server.custom.types.CustomString;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.test.EasyMockUtils.Matchers;
import com.linkedin.restli.server.twitter.AsyncDiscoveredItemsResource;
import com.linkedin.restli.server.twitter.AsyncFollowsAssociativeResource;
import com.linkedin.restli.server.twitter.AsyncLocationResource;
import com.linkedin.restli.server.twitter.AsyncRepliesCollectionResource;
import com.linkedin.restli.server.twitter.AsyncStatusCollectionResource;
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
import com.linkedin.restli.server.twitter.TwitterAccountsResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Followed;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Location;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.StatusType;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItem;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItemKey;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItemKeyParams;

import org.easymock.IAnswer;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModel;
import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModels;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.reset;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;


/**
 * @author dellamag
 */
public class TestRestLiMethodInvocation
{
  private ScheduledExecutorService _scheduler;
  private Engine _engine;
  private EasyMockResourceFactory _resourceFactory;
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
    _invoker = new RestLiMethodInvoker(_resourceFactory, _engine, new ErrorResponseBuilder());
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
    RestLiCallback<?> callback = getCallback();

    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.getPublicTimeline((PagingContext)EasyMock.anyObject(), EasyMock.<Callback<List<Status>>> anyObject());
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET", "/asyncstatuses?q=public_timeline", null);

    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.search((PagingContext) EasyMock.anyObject(), eq("linkedin"), eq(1L),
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET", "/asyncstatuses?q=search&keywords=linkedin&since=1&type=REPLY", null);

    // #2.1: search, optional argument
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.search((PagingContext)EasyMock.anyObject(), eq("linkedin"), eq(-1L), eq((StatusType)null),
                          EasyMock.<Callback<List<Status>>> anyObject());
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET",
                         "/asyncstatuses?q=search&keywords=linkedin", null);

    //boolean optional parameter
    methodDescriptor = statusResourceModel.findNamedMethod("user_timeline");
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.getUserTimeline((PagingContext) EasyMock.anyObject(), eq(false),
                                   (Callback<List<Status>>) EasyMock.anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        Callback<List<Status>> callback = (Callback<List<Status>>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(statusResource);
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET", "/asyncstatuses?q=user_timeline&includeReplies=false", null);

    // #3: get
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.get(eq(1L), EasyMock.<Callback<Status>> anyObject());
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET", "/asyncstatuses/1",
                         buildPathKeys(
                                 "statusID", 1L));

    // #4: get on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.GET);
    locationResource = getMockResource(AsyncLocationResource.class);
    locationResource.get(EasyMock.<Callback<Location>> anyObject());
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
    checkAsyncInvocation(locationResource, callback, methodDescriptor, "GET", "/asyncstatuses/1/asynclocation",
                         buildPathKeys("statusID", 1L));

    // #5: get on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.GET);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);

    discoveredItemsResource.get(eq(key), EasyMock.<Callback<DiscoveredItem>>anyObject());
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
                        "/asyncdiscovereditems/itemId=1&type=2&userId=3",
                        buildPathKeys("asyncDiscoveredItemId", key));

    // #6: finder on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findNamedMethod("user");
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    discoveredItemsResource.getDiscoveredItemsForUser(
        (PagingContext)EasyMock.anyObject(), eq(1L), EasyMock.<Callback<List<DiscoveredItem>>> anyObject());

    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer() throws Throwable
      {
        Callback<List<DiscoveredItem>> callback = (Callback<List<DiscoveredItem>>) EasyMock.getCurrentArguments()[2];
        callback.onSuccess(null);
        return null;
      }
    });

    EasyMock.replay(discoveredItemsResource);
    checkAsyncInvocation(
        discoveredItemsResource,
        callback,
        methodDescriptor,
        "GET",
        "/asyncdiscovereditems?q=user&userId=1", null);
  }

  @Test
  public void testAsyncGetAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback<?> callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    // #1: get
    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.GET);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    resource.get(key, EasyMock.<Callback<Followed>> anyObject());
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
    checkAsyncInvocation(resource, callback, methodDescriptor, "GET",
                         "/asyncfollows/followerID:1;followeeID:2",
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

    RestLiCallback<?> callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;
    AsyncFollowsAssociativeResource followsResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    // #1 Batch get on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_GET);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.batchGet((Set<Long>)Matchers.eqCollectionUnordered(Sets.newHashSet(1L, 2L, 3L)),
                            EasyMock.<Callback<Map<Long, Status>>> anyObject());
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "GET",
                         "/asyncstatuses?ids=1,2,3",
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

    checkAsyncInvocation(followsResource, callback, methodDescriptor, "GET",
                         "/asyncfollows?ids=followeeID:1;followerID:1,followeeID:2;followerID:2",
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
        EasyMock.<Callback<Map<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>>anyObject());

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

    checkAsyncInvocation(
        discoveredItemsResource,
        callback,
        methodDescriptor,
        "GET",
        "/asyncdiscovereditems?ids=itemId=1&type=2&userId=3&ids=itemId=4&type=5&userId=6",
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
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;
    AsyncRepliesCollectionResource repliesResource;
    AsyncLocationResource locationResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    // #1
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.CREATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.create((Status)EasyMock.anyObject(), EasyMock.<Callback<CreateResponse>> anyObject());
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "POST", "/asyncstatuses", "{}", null);

    // #1.1: different endpoint
    methodDescriptor = repliesResourceModel.findMethod(ResourceMethod.CREATE);
    repliesResource = getMockResource(AsyncRepliesCollectionResource.class);
    repliesResource.create((Status)EasyMock.anyObject(), (Callback<CreateResponse>)EasyMock.anyObject());
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
    checkAsyncInvocation(repliesResource, callback, methodDescriptor, "POST",
                         "/asyncstatuses/1/replies", "{}", buildPathKeys(
            "statusID", 1L));

    // #2: Collection Partial Update
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(42)));
    PatchRequest<Status> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    statusResource.update(eq(1L), eq(expected), EasyMock.<Callback<UpdateResponse>> anyObject());
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "POST", "/asyncstatuses/1",
                         "{\"patch\":{\"$set\":{\"foo\":42}}}", buildPathKeys("statusID", 1L));

    // #3: Simple Resource Partial Update
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    locationResource = getMockResource(AsyncLocationResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(51)));
    PatchRequest<Location> expectedLocation = PatchRequest.createFromPatchDocument(p.getDataMap());
    locationResource.update(eq(expectedLocation), EasyMock.<Callback<UpdateResponse>> anyObject());
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
    checkAsyncInvocation(locationResource, callback, methodDescriptor, "POST", "/asyncstatuses/1/asynclocation",
                         "{\"patch\":{\"$set\":{\"foo\":51}}}", buildPathKeys("statusID", 1L));

    // #4 Complex-key resource create
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.CREATE);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    discoveredItemsResource.create((DiscoveredItem)EasyMock.anyObject(),
                                   EasyMock.<Callback<CreateResponse>>anyObject());
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
                         "/asyncdiscovereditems",
                         "{}",
                         null);

    // #5 Partial update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(43)));
    PatchRequest<DiscoveredItem> expectedDiscoveredItem =
        PatchRequest.createFromPatchDocument(p.getDataMap());
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);

    discoveredItemsResource.update(eq(key), eq(expectedDiscoveredItem), EasyMock.<Callback<UpdateResponse>>anyObject());
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
    checkAsyncInvocation(
        discoveredItemsResource,
        callback,
        methodDescriptor,
        "POST",
        "/asyncdiscovereditems/itemId=1&type=2&userId=3",
        "{\"patch\":{\"$set\":{\"foo\":43}}}",
        buildPathKeys("asyncDiscoveredItemId", key));
  }

  @Test
  public void testAsyncBatchCreate() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    BatchCreateRequest<Long, Status> mockBatchCreateReq = (BatchCreateRequest<Long, Status>)EasyMock.anyObject();
    statusResource.batchCreate(mockBatchCreateReq, EasyMock.<Callback<BatchCreateResult<Long, Status>>> anyObject());
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

    checkAsyncInvocation(statusResource, callback, methodDescriptor, "POST", "/asyncstatuses", "{}", null);
  }

  @Test
  public void testAsyncBatchDelete() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    BatchDeleteRequest<Long, Status> mockBatchDeleteReq = (BatchDeleteRequest<Long, Status>)EasyMock.anyObject();
    statusResource.batchDelete(mockBatchDeleteReq, EasyMock.<Callback<BatchUpdateResult<Long, Status>>> anyObject());
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
                         "/asyncstatuses?ids=1,2,3",
                         buildBatchPathKeys(1L, 2L, 3L));
  }

  @Test
  public void testAsyncBatchUpdate() throws Exception
  {

    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    BatchUpdateRequest<Long, Status> mockBatchUpdateReq = (BatchUpdateRequest<Long, Status>)EasyMock.anyObject();
    statusResource.batchUpdate(mockBatchUpdateReq, EasyMock.<Callback<BatchUpdateResult<Long, Status>>> anyObject());
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
                         "/asyncstatuses?ids=1,2,3",
                         "{\"entities\": {\"1\": {}, \"2\": {}, \"3\": {}}}",
                         buildBatchPathKeys(1L, 2L, 3L));
  }

  @Test
  public void testAsyncBatchPatch() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    BatchPatchRequest<Long, Status> mockBatchPatchReq = (BatchPatchRequest<Long, Status>)EasyMock.anyObject();
    statusResource.batchUpdate(mockBatchPatchReq, EasyMock.<Callback<BatchUpdateResult<Long, Status>>> anyObject());
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
                         "/asyncstatuses?ids=1,2,3",
                         "{\"entities\": {\"1\": {}, \"2\": {}, \"3\": {}}}",
                         buildBatchPathKeys(1L, 2L, 3L));
  }

  @Test
  public void testAsyncGetAll() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(AsyncStatusCollectionResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;

    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET_ALL);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);

    @SuppressWarnings("unchecked")
    PagingContext mockCtx = (PagingContext)EasyMock.anyObject();
    statusResource.getAll(mockCtx, EasyMock.<Callback<List<Status>>> anyObject());
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
                         "/asyncstatuses",
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

    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;
    AsyncFollowsAssociativeResource followsResource;
    AsyncLocationResource locationResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    // #1 Update on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.UPDATE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    long id = eq(1L);
    Status status  =(Status)EasyMock.anyObject();
    statusResource.update(id, status, EasyMock.<Callback<UpdateResponse>> anyObject());
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "PUT", "/asyncstatuses/1",
                         "{}", buildPathKeys(
            "statusID", 1L));

    // #2 Update on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.UPDATE);
    followsResource = getMockResource(AsyncFollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    Followed followed = (Followed)EasyMock.anyObject();
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
    checkAsyncInvocation(followsResource, callback, methodDescriptor, "PUT",
                         "/asyncfollows/followerID:1;followeeID:2", "{}",
                         buildPathKeys("followerID", 1L, "followeeID", 2L,
                                       followsAssociationResourceModel.getKeyName(), rawKey));

    // #3 Update on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.UPDATE);
    locationResource = getMockResource(AsyncLocationResource.class);
    Location location  =(Location)EasyMock.anyObject();
    locationResource.update(location, EasyMock.<Callback<UpdateResponse>> anyObject());
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
    checkAsyncInvocation(locationResource, callback, methodDescriptor, "PUT", "/asyncstatuses/1/asynclocation",
                         "{}", buildPathKeys("statusID", 1L));

    // #4 Update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.UPDATE);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> complexKey =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    discoveredItemsResource.update(eq(complexKey),
                                   (DiscoveredItem)EasyMock.anyObject(),
                                   EasyMock.<Callback<UpdateResponse>>anyObject());

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
    checkAsyncInvocation(
        discoveredItemsResource,
        callback,
        methodDescriptor,
        "PUT",
        "/asyncdiscovereditems/itemId=1&type=2&userId=3", "{}",
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

    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncStatusCollectionResource statusResource;
    AsyncLocationResource locationResource;
    AsyncDiscoveredItemsResource discoveredItemsResource;

    // #1 Delete on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.DELETE);
    statusResource = getMockResource(AsyncStatusCollectionResource.class);
    statusResource.delete(eq(1L), EasyMock.<Callback<UpdateResponse>> anyObject());
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
    checkAsyncInvocation(statusResource, callback, methodDescriptor, "DELETE", "/asyncstatuses/1", buildPathKeys(
                                                                                             "statusID", 1L));

    // #2 Delete on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.DELETE);
    locationResource = getMockResource(AsyncLocationResource.class);
    locationResource.delete(EasyMock.<Callback<UpdateResponse>> anyObject());
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
    checkAsyncInvocation(locationResource, callback, methodDescriptor, "DELETE", "/asyncstatuses/1/asynclocation",
                         buildPathKeys("statusID", 1L));

    // #3 Delete on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.DELETE);
    discoveredItemsResource = getMockResource(AsyncDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);

    discoveredItemsResource.delete(eq(key), EasyMock.<Callback<UpdateResponse>>anyObject());
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
    checkAsyncInvocation(
        discoveredItemsResource,
        callback,
        methodDescriptor,
        "DELETE",
        "/asyncdiscovereditems/itemId=1&type=2&userId=3",
        buildPathKeys("asyncDiscoveredItemId", key));
  }

  /*
  @Test
  public void testAsyncActions() throws Exception
  {
    ResourceModel accountsResourceModel = buildResourceModel(AsyncTwitterAccountsResource.class);
    RestLiCallback callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncTwitterAccountsResource accountsResource;

    // #1 no defaults provided
    methodDescriptor = accountsResourceModel.findActionMethod("closeAccounts");
    accountsResource = getMockResource(AsyncTwitterAccountsResource.class);
    StringArray emailAddresses = new StringArray(Lists.newArrayList("bob@test.linkedin.com", "joe@test.linkedin.com"));

    accountsResource.closeAccounts(eq(emailAddresses), eq(true), eq((StringMap)null), EasyMock.<Callback<StringMap>> anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        Callback<StringMap> callback = (Callback<StringMap>) EasyMock.getCurrentArguments()[3];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(accountsResource);

    String jsonEntityBody = RestLiTestHelper.doubleQuote(
        "{'emailAddresses': ['bob@test.linkedin.com', 'joe@test.linkedin.com'], 'someFlag': true}");
    checkAsyncInvocation(accountsResource, callback, methodDescriptor, "POST", "/accounts?action=closeAccounts", jsonEntityBody);
  }
*/

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
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline((PagingContext) EasyMock.anyObject()))
            .andReturn(Promises.<List<Status>> value(null))
            .once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses?q=public_timeline");

    // #2: search
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.search(eq("linkedin"), eq(1L), eq(StatusType.REPLY))).andReturn(Promises.<List<Status>> value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET",
                    "/promisestatuses?q=search&keywords=linkedin&since=1&type=REPLY");

    // #2.1: search, optional argument
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.search(eq("linkedin"), eq(-1L), eq((StatusType)null))).andReturn(Promises.<List<Status>> value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses?q=search&keywords=linkedin");

    //boolean optional parameter
    methodDescriptor = statusResourceModel.findNamedMethod("user_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(false), (PagingContext)EasyMock.anyObject())).andReturn(Promises.<List<Status>> value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET",
                    "/promisestatuses?q=user_timeline&includeReplies=false");

    // #3: get
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(Promises.<Status> value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses/1", buildPathKeys(
                                                                                          "statusID", 1L));

    // #5: search - required parameter
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET", "/promisestatuses?q=search&since=1");

    // #6: malformed fields syntax
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET",
                           "/promisestatuses?q=search&fields=foo))");

    // #7: get on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.GET);
    locationResource = getMockResource(PromiseLocationResource.class);
    EasyMock.expect(locationResource.get()).andReturn(Promises.<Location> value(null)).once();
    checkInvocation(locationResource, methodDescriptor, "GET", "/promisestatuses/1/promiselocation",
                    buildPathKeys("statusID", 1L));

    // #8 get on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.GET);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.get(eq(key))).andReturn(Promises.<DiscoveredItem> value(null)).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "GET", "/promisediscovereditems/itemId=1&type=2&userId=3",
                    buildPathKeys("promiseDiscoveredItemId", key));

    // #9 finder on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findNamedMethod("user");
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    EasyMock.expect(
        discoveredItemsResource.getDiscoveredItemsForUser(
            eq(1L), (PagingContext)EasyMock.anyObject())).andReturn(Promises.<List<DiscoveredItem>>value(null)).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "GET", "/promisediscoveredItems?q=user&userId=1");
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
    checkInvocation(resource, methodDescriptor, "GET", "/promisefollows/followerID:1;followeeID:2",
                    buildPathKeys("followerID", 1L, "followeeID", 2L,
                                  followsResourceModel.getKeyName(), rawKey));
  }

  @Test
  public void testPromisePagingContext() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;

    // #1: defaults
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(null, null)))).andReturn(Promises.<List<Status>>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses?q=public_timeline");

    // #2
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(5, null)))).andReturn(Promises.<List<Status>>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses?q=public_timeline&start=5");

    // #3
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(null, 4)))).andReturn(Promises.<List<Status>>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses?q=public_timeline&count=4");

    // #5 invalid (non-integer)
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET", "/promisestatuses?q=public_timeline&start=5&count=asdf");

    // #5.1 invalid (non-integer)
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET",
                           "/promisestatuses?q=public_timeline&start=asdf&count=4");

    // invalid (negative)
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET", "/promisestatuses?q=public_timeline&start=-1&count=4");

    // invalid (negative)
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET",
                           "/promisestatuses?q=public_timeline&start=5&count=-1");

    methodDescriptor = statusResourceModel.findNamedMethod("user_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(true), eq(new PagingContext(10, 100, false, false))))
            .andReturn(Promises.<List<Status>>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses?q=user_timeline");

    methodDescriptor = statusResourceModel.findNamedMethod("user_timeline");
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(true), eq(new PagingContext(0, 20, true, true))))
            .andReturn(Promises.<List<Status>>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses?q=user_timeline&start=0&count=20");

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
    EasyMock.expect(statusResource.batchGet((Set<Long>)Matchers.eqCollectionUnordered(Sets.newHashSet(1L, 2L, 3L)))).andReturn(Promises.<Map<Long, Status>>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/promisestatuses?ids=1,2,3", buildBatchPathKeys(1L, 2L, 3L));

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

    EasyMock.expect(followsResource.batchGet((Set<CompoundKey>)Matchers.eqCollectionUnordered(expectedKeys))).andReturn(Promises.<Map<CompoundKey, Followed>>value(null)).once();
    checkInvocation(followsResource, methodDescriptor, "GET",
                    "/promisefollows?ids=followeeID:1;followerID:1,followeeID:2;followerID:2",
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

    EasyMock.expect(discoveredItemsResource.batchGet(set)).andReturn(Promises.<Map<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>value(
        null)).once();

    checkInvocation(discoveredItemsResource, methodDescriptor,
                    "GET",
                    "/promisediscovereditems?ids=itemId=1&type=2&userId=3&ids=itemId=4&type=5&userId=6",
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
    EasyMock.expect(statusResource.create((Status)EasyMock.anyObject())).andReturn(Promises.<CreateResponse>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "POST", "/promisestatuses", "{}");

    // #1.1: different endpoint
    methodDescriptor = repliesResourceModel.findMethod(ResourceMethod.CREATE);
    repliesResource = getMockResource(PromiseRepliesCollectionResource.class);
    EasyMock.expect(repliesResource.create((Status)EasyMock.anyObject())).andReturn(Promises.<CreateResponse>value(null)).once();
    checkInvocation(repliesResource, methodDescriptor, "POST", "/promisestatuses/1/replies", "{}", buildPathKeys(
                                                                                                          "statusID", 1L));

    // #1.2: invalid entity
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.CREATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    EasyMock.expect(statusResource.create((Status)EasyMock.anyObject())).andReturn(Promises.<CreateResponse>value(null)).once();
    try
    {
      checkInvocation(statusResource, methodDescriptor, "POST", "/promisestatuses", "{");
      fail("Expected exception");
    }
    catch (RoutingException e)
    {
      // expected
      EasyMock.reset(statusResource);
    }

    // #2: Collection Partial Update
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(42)));
    PatchRequest<Status> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(statusResource.update(eq(1L), eq(expected))).andReturn(Promises.<UpdateResponse>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "POST", "/promisestatuses/1",
                    "{\"patch\":{\"$set\":{\"foo\":42}}}", buildPathKeys("statusID", 1L));

    // #3: Simple Resource Partial Update

    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    locationResource = getMockResource(PromiseLocationResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(51)));
    PatchRequest<Location> expectedLocation = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(locationResource.update(eq(expectedLocation))).andReturn(Promises.<UpdateResponse>value(null)).once();
    checkInvocation(locationResource, methodDescriptor, "POST", "/promisestatuses/1/promiselocation",
                    "{\"patch\":{\"$set\":{\"foo\":51}}}", buildPathKeys("statusID", 1L));

    // #4 Complex key resource create
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.CREATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    EasyMock.expect(
        discoveredItemsResource.create(
            (DiscoveredItem)EasyMock.anyObject())).andReturn(Promises.<CreateResponse>value(null)).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "POST", "/promisediscovereditems", "{}");

    // #5 Complex key resource partial update
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(43)));
    PatchRequest<DiscoveredItem> expectedDiscoveredItem =
        PatchRequest.createFromPatchDocument(p.getDataMap());
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(
        discoveredItemsResource.update(eq(key), eq(expectedDiscoveredItem))).andReturn(
          Promises.<UpdateResponse>value(null)).once();
    checkInvocation(
        discoveredItemsResource,
        methodDescriptor,
        "POST",
        "/promisediscovereditems/itemId=1&type=2&userId=3",
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
    Status status  =(Status)EasyMock.anyObject();
    EasyMock.expect(statusResource.update(id, status)).andReturn(Promises.<UpdateResponse>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "PUT", "/promisestatuses/1", "{}", buildPathKeys(
                                                                                                "statusID", 1L));

    // #2 Update on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.UPDATE);
    followsResource = getMockResource(PromiseFollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    Followed followed = (Followed)EasyMock.anyObject();
    EasyMock.expect(followsResource.update(key, followed)).andReturn(Promises.<UpdateResponse>value(null)).once();
    checkInvocation(followsResource, methodDescriptor, "PUT", "/promisefollows/followerID:1;followeeID:2", "{}",
                    buildPathKeys("followerID", 1L, "followeeID", 2L,
                                  followsAssociationResourceModel.getKeyName(), rawKey));

    // #3 Update on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.UPDATE);
    locationResource = getMockResource(PromiseLocationResource.class);
    Location location  =(Location)EasyMock.anyObject();
    EasyMock.expect(locationResource.update(location)).andReturn(Promises.<UpdateResponse>value(null)).once();
    checkInvocation(locationResource, methodDescriptor, "PUT", "/promisestatuses/1/promiselocation", "{}",
                    buildPathKeys("statusID", 1L));

    // #4 Update on complex key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.UPDATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> complexKey =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.update(
        eq(complexKey),
        (DiscoveredItem)EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(discoveredItemsResource, methodDescriptor,
                    "PUT",
                    "/promisediscovereditems/itemId=1&type=2&userId=3", "{}",
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
    EasyMock.expect(statusResource.delete(eq(1L))).andReturn(Promises.<UpdateResponse>value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "DELETE", "/promisestatuses/1", buildPathKeys(
                                                                                             "statusID", 1L));

    // #2 Delete on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.DELETE);
    locationResource = getMockResource(PromiseLocationResource.class);
    EasyMock.expect(locationResource.delete()).andReturn(Promises.<UpdateResponse>value(null)).once();
    checkInvocation(locationResource, methodDescriptor, "DELETE", "/promisestatuses/1/promiselocation",
                    buildPathKeys("statusID", 1L));

    // #3 Delete on complex key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.DELETE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.delete(eq(key))).andReturn(Promises.<UpdateResponse>value(null)).once();
    checkInvocation(
        discoveredItemsResource,
        methodDescriptor,
        "DELETE",
        "/promisediscovereditems/itemId=1&type=2&userId=3",
        buildPathKeys("promiseDiscoveredItemId", key));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testPromiseBatchUpdate() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(PromiseDiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1 Batch update on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchUpdateRequest batchUpdateRequest =(BatchUpdateRequest)EasyMock.anyObject();
    EasyMock.expect(statusResource.batchUpdate(batchUpdateRequest)).andReturn(
        Promises.<BatchUpdateResult<Long, Status>>value(null)).once();
    String body = RestLiTestHelper.doubleQuote("{'entities':{'1':{},'2':{}}}");
    checkInvocation(statusResource, methodDescriptor, "PUT", "/promisestatuses?ids=1&ids=2", body, buildBatchPathKeys(1L, 2L));

    // #2 Batch update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    body = RestLiTestHelper.doubleQuote("{'entities':{'itemId=1&type=2&userId=3':{},'itemId=4&type=5&userId=6':{}}}");
    batchUpdateRequest =(BatchUpdateRequest)EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchUpdate(batchUpdateRequest)).andReturn(
        Promises.<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>value(null)).once();
    checkInvocation(
        discoveredItemsResource,
        methodDescriptor,
        "PUT",
        "/promisediscovereditems?ids=itemId=1&type=2&userId=3&ids=itemId=4&type=5&userId=6",
        body,
        buildBatchPathKeys(keyA, keyB));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testPromiseBatchPatch() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(PromiseStatusCollectionResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(PromiseDiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    PromiseStatusCollectionResource statusResource;
    PromiseDiscoveredItemsResource discoveredItemsResource;

    // #1 Batch partial update on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    statusResource = getMockResource(PromiseStatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchPatchRequest batchPatchRequest =(BatchPatchRequest)EasyMock.anyObject();
    EasyMock.expect(statusResource.batchUpdate(batchPatchRequest)).andReturn(
        Promises.<BatchUpdateResult<Long, Status>>value(null)).once();
    String body = RestLiTestHelper.doubleQuote("{'entities':{'1':{},'2':{}}}");
    checkInvocation(statusResource, methodDescriptor, "POST", "/promisestatuses?ids=1&ids=2", body, buildBatchPathKeys(1L, 2L));

    // #2 Batch partial update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    body = RestLiTestHelper.doubleQuote("{'entities':{'itemId=1&type=2&userId=3':{},'itemId=4&type=5&userId=6':{}}}");
    batchPatchRequest =(BatchPatchRequest)EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchUpdate(batchPatchRequest)).andReturn(
        Promises.<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>value(null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    "/promisediscovereditems?ids=itemId=1&type=2&userId=3&ids=itemId=4&type=5&userId=6",
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
    BatchCreateRequest batchCreateRequest =(BatchCreateRequest)EasyMock.anyObject();
    EasyMock.expect(statusResource.batchCreate(batchCreateRequest)).andReturn(
        Promises.<BatchCreateResult<Long, Status>>value(null)).once();
    String body = RestLiTestHelper.doubleQuote("{'elements':[{},{}]}");
    checkInvocation(statusResource, methodDescriptor, "POST", "/promisestatuses", body, buildBatchPathKeys());

    // #2 Batch create on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    batchCreateRequest =(BatchCreateRequest)EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchCreate(batchCreateRequest)).andReturn(
        Promises.<BatchCreateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>value(null)).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    "/promisediscovereditems",
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
    BatchDeleteRequest batchDeleteRequest =(BatchDeleteRequest)EasyMock.anyObject();
    EasyMock.expect(statusResource.batchDelete(batchDeleteRequest)).andReturn(
        Promises.<BatchUpdateResult<Long, Status>> value(null)).once();
    checkInvocation(statusResource, methodDescriptor, "DELETE", "/promisestatuses?ids=1L&ids=2L", "", buildBatchPathKeys(1L, 2L));

    // #2 Batch delete on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    discoveredItemsResource = getMockResource(PromiseDiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    batchDeleteRequest =(BatchDeleteRequest)EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchDelete(batchDeleteRequest)).andReturn(
        Promises.<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>> value(null)).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "DELETE",
                    "/promisediscovereditems?ids=itemId=1&type=2&userId=3&ids=itemId=4&type=5&userId=6",
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
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline((PagingContext)EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses?q=public_timeline");

    // #2: search
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.search(eq("linkedin"), eq(1L), eq(StatusType.REPLY))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET",
                    "/statuses?q=search&keywords=linkedin&since=1&type=REPLY");

    // #2.1: search, optional argument
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.search(eq("linkedin"), eq(-1L), eq((StatusType)null))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses?q=search&keywords=linkedin");

    //boolean optional parameter
    methodDescriptor = statusResourceModel.findNamedMethod("user_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(false), (PagingContext)EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET",
                    "/statuses?q=user_timeline&includeReplies=false");

    // #3: get
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.GET);
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.get(eq(1L))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses/1", buildPathKeys(
                                                                                          "statusID", 1L));

    // #5: search - required parameter
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET", "/statuses?q=search&since=1");

    // #6: malformed fields syntax
    methodDescriptor = statusResourceModel.findNamedMethod("search");
    statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET",
                           "/statuses?q=search&fields=foo))");

    // #7: get simple sub-resource
    ResourceModel locationResourceModel = statusResourceModel.getSubResource(
        "location");

    LocationResource locationResource;

    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.GET);
    locationResource = getMockResource(LocationResource.class);
    EasyMock.expect(locationResource.get()).andReturn(null).once();
    checkInvocation(locationResource, methodDescriptor, "GET", "/statuses/1/location", buildPathKeys("statusID", 1L));

    // #8: get complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.GET);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.get(eq(key))).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "GET",
                    "/discovereditems/itemId=1&type=2&userId=3",
                    buildPathKeys("discoveredItemId", key));

    // #9: finder on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findNamedMethod("user");
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    EasyMock.expect(discoveredItemsResource.findByUser(eq(1L))).andReturn(null).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "GET", "/discovereditems?q=user&userId=1");
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
    checkInvocation(resource, methodDescriptor, "GET", "/follows/followerID:1;followeeID:2",
                    buildPathKeys("followerID", 1L, "followeeID", 2L,
                                  followsResourceModel.getKeyName(), rawKey));
  }

  @Test
  public void testPagingContext() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;

    // #1: defaults
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(null, null)))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses?q=public_timeline");

    // #2
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(5, null)))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses?q=public_timeline&start=5");

    // #3
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getPublicTimeline(eq(buildPagingContext(null, 4)))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses?q=public_timeline&count=4");

    // #5 invalid (non-integer)
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET", "/statuses?q=public_timeline&start=5&count=asdf");

    // #5.1 invalid (non-integer)
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET",
                           "/statuses?q=public_timeline&start=asdf&count=4");

    // invalid (negative)
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET", "/statuses?q=public_timeline&start=-1&count=4");

    // invalid (negative)
    methodDescriptor = statusResourceModel.findNamedMethod("public_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    expectRoutingException(methodDescriptor, statusResource, "GET",
                           "/statuses?q=public_timeline&start=5&count=-1");

    methodDescriptor = statusResourceModel.findNamedMethod("user_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(true), eq(new PagingContext(10, 100, false, false))))
            .andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses?q=user_timeline");

    methodDescriptor = statusResourceModel.findNamedMethod("user_timeline");
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.getUserTimeline(eq(true), eq(new PagingContext(0, 20, true, true))))
            .andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses?q=user_timeline&start=0&count=20");

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
    checkInvocation(statusResource, methodDescriptor, "GET", "/statuses?ids=1,2,3", buildBatchPathKeys(1L, 2L, 3L));

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

    String uri = "/follows?" +
                           "?ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(1L, 1L)) +
                           "&ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(2L, 2L));

    checkInvocation(followsResource,
                    methodDescriptor,
                    "GET",
                    uri,
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

    uri = "/discovereditems?" +
                            "ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(1, 2, 3)) +
                            "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(4, 5, 6));

    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "GET",
                    uri,
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
    EasyMock.expect(statusResource.create((Status)EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "POST", "/statuses", "{}");

    // #1.1: different endpoint
    methodDescriptor = repliesResourceModel.findMethod(ResourceMethod.CREATE);
    repliesResource = getMockResource(RepliesCollectionResource.class);
    EasyMock.expect(repliesResource.create((Status)EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(repliesResource, methodDescriptor, "POST", "/statuses/1/replies", "{}", buildPathKeys(
                                                                                                          "statusID", 1L));

    // #1.2: invalid entity
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.CREATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    EasyMock.expect(statusResource.create((Status)EasyMock.anyObject())).andReturn(null).once();
    try
    {
      checkInvocation(statusResource, methodDescriptor, "POST", "/statuses", "{");
      fail("Expected exception");
    }
    catch (RoutingException e)
    {
      // expected
      EasyMock.reset(statusResource);
    }

    // #2 Collection Partial Update
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(42)));
    PatchRequest<Status> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(statusResource.update(eq(1L), eq(expected))).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "POST", "/statuses/1",
                    "{\"patch\":{\"$set\":{\"foo\":42}}}", buildPathKeys("statusID", 1L));

    // #3 Simple Resource Partial Update

    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    locationResource = getMockResource(LocationResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(51)));
    PatchRequest<Location> expectedLocation = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(locationResource.update(eq(expectedLocation))).andReturn(null).once();
    checkInvocation(locationResource, methodDescriptor, "POST", "/statuses/1/location",
                    "{\"patch\":{\"$set\":{\"foo\":51}}}", buildPathKeys("statusID", 1L));

    // #4 Complex-key resource create
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.CREATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    EasyMock.expect(
        discoveredItemsResource.create((DiscoveredItem)EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(discoveredItemsResource, methodDescriptor, "POST", "/discovereditems", "{}");

    // #5 Partial update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(43)));
    PatchRequest<DiscoveredItem> expectedDiscoveredItem =
        PatchRequest.createFromPatchDocument(p.getDataMap());
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(
        discoveredItemsResource.update(eq(key), eq(expectedDiscoveredItem))).andReturn(null).once();
    checkInvocation(
        discoveredItemsResource,
        methodDescriptor,
        "POST",
        "/discovereditems/itemId=1&type=2&userId=3",
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
    Status status  =(Status)EasyMock.anyObject();
    EasyMock.expect(statusResource.update(id, status)).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "PUT", "/statuses/1", "{}", buildPathKeys(
                                                                                                "statusID", 1L));

    // #2 Update on association resource
    methodDescriptor = followsAssociationResourceModel.findMethod(ResourceMethod.UPDATE);
    followsResource = getMockResource(FollowsAssociativeResource.class);

    CompoundKey rawKey = new CompoundKey();
    rawKey.append("followerID", 1L);
    rawKey.append("followeeID", 2L);
    CompoundKey key = eq(rawKey);

    Followed followed = (Followed)EasyMock.anyObject();
    EasyMock.expect(followsResource.update(key, followed)).andReturn(null).once();
    checkInvocation(followsResource, methodDescriptor, "PUT", "/follows/followerID:1;followeeID:2", "{}",
                    buildPathKeys("followerID", 1L, "followeeID", 2L,
                                  followsAssociationResourceModel.getKeyName(), rawKey));

    // #3 Update on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.UPDATE);
    locationResource = getMockResource(LocationResource.class);
    Location location  =(Location)EasyMock.anyObject();
    EasyMock.expect(locationResource.update(location)).andReturn(null).once();
    checkInvocation(locationResource, methodDescriptor, "PUT", "/statuses/1/location", "{}", buildPathKeys(
        "statusID", 1L));

    // #4 Update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.UPDATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> complexKey =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.update(
        eq(complexKey),
        (DiscoveredItem)EasyMock.anyObject())).andReturn(null).once();
    checkInvocation(discoveredItemsResource, methodDescriptor,
                    "PUT",
                    "/discovereditems/itemId=1&type=2&userId=3", "{}",
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
    checkInvocation(
        statusResource,
        methodDescriptor,
        "DELETE",
        "/statuses/1",
        buildPathKeys("statusID", 1L));

    // #2 Delete on simple resource
    methodDescriptor = locationResourceModel.findMethod(ResourceMethod.DELETE);
    locationResource = getMockResource(LocationResource.class);
    EasyMock.expect(locationResource.delete()).andReturn(null).once();
    checkInvocation(
        locationResource,
        methodDescriptor,
        "DELETE",
        "/statuses/1/location",
        buildPathKeys("statusID", 1L));

    // #3 Delete on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.DELETE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> key =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    EasyMock.expect(discoveredItemsResource.delete(eq(key))).andReturn(null).once();
    checkInvocation(
        discoveredItemsResource,
        methodDescriptor,
        "DELETE",
        "/discovereditems/itemId=1&type=2&userId=3",
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
    checkInvocation(accountsResource, methodDescriptor, "POST", "/accounts?action=register", jsonEntityBody);

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
    checkInvocation(accountsResource, methodDescriptor, "POST", "/accounts?action=register", jsonEntityBody);

    // #3 no-arg method
    methodDescriptor = accountsResourceModel.findActionMethod("noArgMethod", ResourceLevel.COLLECTION);
    accountsResource = getMockResource(TwitterAccountsResource.class);
    accountsResource.noArgMethod();
    EasyMock.expectLastCall().once();

    jsonEntityBody = RestLiTestHelper.doubleQuote("{}");
    checkInvocation(accountsResource, methodDescriptor, "POST", "/accounts?action=noArgMethod",
                    jsonEntityBody);

    // #4 primitive response
    methodDescriptor = accountsResourceModel.findActionMethod("primitiveResponse", ResourceLevel.COLLECTION);
    accountsResource = getMockResource(TwitterAccountsResource.class);
    EasyMock.expect(accountsResource.primitiveResponse()).andReturn(1).once();

    jsonEntityBody = RestLiTestHelper.doubleQuote("{'value': 1}");
    checkInvocation(accountsResource, methodDescriptor, "POST",
                    "/accounts?action=primitiveResponse", jsonEntityBody);
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
                    .build();

    RoutingResult routingResult = new RoutingResult(new ResourceContextImpl(null, request,
                                                                            new RequestContext()), methodDescriptor);

    try {
      _invoker.invoke(routingResult, request, null);
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
                    .build();

    RoutingResult routingResult = new RoutingResult(new ResourceContextImpl(null, request,
                                                                            new RequestContext()), methodDescriptor);

    try {
      _invoker.invoke(routingResult, request, null);
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
    StringArray emailAddresses = new StringArray(Lists.newArrayList("bob@test.linkedin.com", "joe@test.linkedin.com"));

    EasyMock.expect(accountsResource.closeAccounts(eq(emailAddresses), eq(true), eq((StringMap)null)))
    .andReturn((new StringMap())).once();

    String jsonEntityBody = RestLiTestHelper.doubleQuote(
        "{'emailAddresses': ['bob@test.linkedin.com', 'joe@test.linkedin.com'], 'someFlag': true}");
    checkInvocation(accountsResource, methodDescriptor, "POST", "/accounts?action=closeAccounts", jsonEntityBody);
  }

  @Test
  public void testCustomTypeParameters_NoCoercer() throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findNamedMethod("noCoercerCustomString");

    RestRequest request =
            new RestRequestBuilder(new URI("/statuses/1/replies?query=noCoercerCustomString&s=foo"))
                    .setMethod("GET")
                    .build();

    RoutingResult routingResult = new RoutingResult(new ResourceContextImpl(null, request,
                                                                            new RequestContext()), methodDescriptor);

    try
    {
      _invoker.invoke(routingResult, request, null);
      Assert.fail("expected template output cast exception");
    }
    catch (TemplateOutputCastException e)
    {
      Assert.assertTrue(e.getMessage().contains("cannot be coerced"));
    }

  }

  @Test
  public void testCustomTypeParameters_WrongType() throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = repliesResourceModel.findNamedMethod("customLong");

    RestRequest request =
            new RestRequestBuilder(new URI("/statuses/1/replies?query=customLong&l=foo"))
                    .setMethod("GET")
                    .build();

    RoutingResult routingResult = new RoutingResult(new ResourceContextImpl(null, request,
                                                                            new RequestContext()), methodDescriptor);

    try
    {
      _invoker.invoke(routingResult, request, null);
      Assert.fail("expected routing exception");
    }
    catch (RoutingException e)
    {
      Assert.assertEquals(e.getStatus(), 400);
    }
  }

  @Test
  public void testCustomTypeParameters() throws Exception
  {
    ResourceModel repliesResourceModel = buildResourceModel(RepliesCollectionResource.class);
    RepliesCollectionResource repliesResource;
    ResourceMethodDescriptor methodDescriptor;

    // test CustomString
    methodDescriptor = repliesResourceModel.findNamedMethod("customString");
    repliesResource = getMockResource(RepliesCollectionResource.class);
    repliesResource.customString(new CustomString("foo"));
    EasyMock.expectLastCall().andReturn(null).once();
    checkInvocation(repliesResource, methodDescriptor, "GET", "/statuses/1/replies?query=customString&s=foo");

    // test CustomLong
    methodDescriptor = repliesResourceModel.findNamedMethod("customLong");
    repliesResource = getMockResource(RepliesCollectionResource.class);
    repliesResource.customLong(new CustomLong(100L));
    EasyMock.expectLastCall().andReturn(null).once();
    checkInvocation(repliesResource,  methodDescriptor, "GET", "/statuses/1/replies?query=customLong&l=100");

    // test CustomLongArray
    methodDescriptor = repliesResourceModel.findNamedMethod("customLongArray");
    repliesResource = getMockResource(RepliesCollectionResource.class);
    CustomLong[] longs = {new CustomLong(100L), new CustomLong(200L)};
    repliesResource.customLongArray(EasyMock.aryEq(longs));
    EasyMock.expectLastCall().andReturn(null).once();
    checkInvocation(repliesResource, methodDescriptor, "GET", "/statuses/1/replies?query=customLongArray&longs=100&longs=200");

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
    checkInvocation(locationResource, methodDescriptor, "POST", "/statuses/1/location?action=new_status_from_location",
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
                    "/discovereditems/action=purge",
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
    checkInvocation(resource, methodDescriptor, "POST", "/test?action=intParam", jsonEntityBody, buildPathKeys());

    methodDescriptor = model.findActionMethod("longParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    long expectedLong = DataTemplateUtil.coerceOutput(Integer.MAX_VALUE, Long.class);
    resource.longParam(expectedLong);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'longParam':" + String.valueOf(Integer.MAX_VALUE) + "}");
    checkInvocation(resource, methodDescriptor, "POST", "/test?action=longParam", jsonEntityBody, buildPathKeys());

    methodDescriptor = model.findActionMethod("byteStringParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    String str = "test string";
    ByteString expectedByteString = ByteString.copyString(str, "UTF-8");
    resource.byteStringParam(expectedByteString);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'byteStringParam': '" + str + "'}");
    checkInvocation(resource, methodDescriptor, "POST", "/test?action=byteStringParam", jsonEntityBody, buildPathKeys());

    methodDescriptor = model.findActionMethod("floatParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    float expectedFloat = DataTemplateUtil.coerceOutput(Double.MAX_VALUE, Float.class);
    resource.floatParam(expectedFloat);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'floatParam': " + String.valueOf(Double.MAX_VALUE) + "}");
    checkInvocation(resource, methodDescriptor, "POST", "/test?action=floatParam", jsonEntityBody, buildPathKeys());

    methodDescriptor = model.findActionMethod("doubleParam", ResourceLevel.COLLECTION);
    resource = getMockResource(CombinedResources.TestActionsResource.class);
    float floatValue = 567.5f;
    double expectedDouble = DataTemplateUtil.coerceOutput(floatValue, Double.class);
    resource.doubleParam(expectedDouble);
    EasyMock.expectLastCall().once();
    jsonEntityBody = RestLiTestHelper.doubleQuote("{'doubleParam': " + String.valueOf(floatValue) + "}");
    checkInvocation(resource, methodDescriptor, "POST", "/test?action=doubleParam", jsonEntityBody, buildPathKeys());

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

    checkInvocation(resource, methodDescriptor, "POST", "/test?action=recordParam", jsonEntityBody, buildPathKeys());

  }

  @Test
  public void testHeuristicKeySyntaxDetection()
  {
    Set<Key> keys = new HashSet<Key>(2);
    keys.add(new Key("foo", Integer.class));
    keys.add(new Key("bar", String.class));

    Set<String> expectedKeys = new HashSet<String>(Arrays.asList("foo", "bar"));
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo:42;bar:abcd", keys).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo:42;bar:abcd=1&efg=2", keys).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo=42&bar=abcd", keys).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo=42&bar=abcd:1;2", keys).getPartKeys());

    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo=42&bar=foo:42", keys).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo=42&bar=foo:42;bar:abcd", keys).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo:42;bar:foo=42&bar=abcd", keys).getPartKeys());
    Assert.assertEquals(expectedKeys, ArgumentUtils.parseCompoundKey("foo:42;bar:foo=42", keys).getPartKeys());
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testBatchUpdate() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(DiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1 Batch update on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchUpdateRequest batchUpdateRequest =(BatchUpdateRequest)EasyMock.anyObject();
    EasyMock.expect(statusResource.batchUpdate(batchUpdateRequest)).andReturn(null).once();
    String body = RestLiTestHelper.doubleQuote("{'entities':{'1':{},'2':{}}}");
    checkInvocation(statusResource, methodDescriptor, "PUT", "/statuses?ids=1&ids=2", body, buildBatchPathKeys(1L, 2L));

    // #2 Batch update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    batchUpdateRequest =(BatchUpdateRequest)EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchUpdate(batchUpdateRequest)).andReturn(null).once();

    body = RestLiTestHelper.doubleQuote("{'entities':{'itemId=1&type=2&userId=3':{},'itemId=4&type=5&userId=6':{}}}");
    String uri = "/discovereditems?" +
                                   "ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(1, 2, 3)) +
                                   "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(4, 5, 6));

    checkInvocation(
        discoveredItemsResource,
        methodDescriptor,
        "PUT",
        uri,
        body,
        buildBatchPathKeys(keyA, keyB));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testBatchPatch() throws Exception
  {
    ResourceModel statusResourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceModel discoveredItemsResourceModel = buildResourceModel(DiscoveredItemsResource.class);

    ResourceMethodDescriptor methodDescriptor;
    StatusCollectionResource statusResource;
    DiscoveredItemsResource discoveredItemsResource;

    // #1 Batch partial update on collection resource
    methodDescriptor = statusResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    statusResource = getMockResource(StatusCollectionResource.class);
    @SuppressWarnings("rawtypes")
    BatchPatchRequest batchPatchRequest =(BatchPatchRequest)EasyMock.anyObject();
    EasyMock.expect(statusResource.batchUpdate(batchPatchRequest)).andReturn(null).once();
    String body = RestLiTestHelper.doubleQuote("{'entities':{'1':{},'2':{}}}");
    checkInvocation(statusResource, methodDescriptor, "POST", "/statuses?ids=1&ids=2", body, buildBatchPathKeys(1L, 2L));

    // #2 Batch partial update on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    batchPatchRequest =(BatchPatchRequest)EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchUpdate(batchPatchRequest)).andReturn(null).once();

    body = RestLiTestHelper.doubleQuote("{'entities':{'itemId=1&type=2&userId=3':{},'itemId=4&type=5&userId=6':{}}}");
    String uri = "/discovereditems?" +
                                   "ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(1, 2, 3)) +
                                   "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(4, 5, 6));

    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
                    uri,
                    body,
                    buildBatchPathKeys(keyA, keyB));
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
    BatchCreateRequest batchCreateRequest =(BatchCreateRequest)EasyMock.anyObject();
    EasyMock.expect(statusResource.batchCreate(batchCreateRequest)).andReturn(null).once();
    String body = RestLiTestHelper.doubleQuote("{'elements':[{},{}]}");
    checkInvocation(statusResource, methodDescriptor, "POST", "/statuses", body, buildBatchPathKeys());

    // #2 Batch create on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    batchCreateRequest =(BatchCreateRequest)EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchCreate(batchCreateRequest)).andReturn(null).once();
    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "POST",
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
    BatchDeleteRequest batchDeleteRequest =(BatchDeleteRequest)EasyMock.anyObject();
    EasyMock.expect(statusResource.batchDelete(batchDeleteRequest)).andReturn(null).once();
    checkInvocation(statusResource, methodDescriptor, "DELETE", "/statuses?ids=1L&ids=2L", "", buildBatchPathKeys(1L, 2L));

    // #2 Batch delete on complex-key resource
    methodDescriptor = discoveredItemsResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    discoveredItemsResource = getMockResource(DiscoveredItemsResource.class);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyA =
        getDiscoveredItemComplexKey(1L, 2, 3L);
    ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> keyB =
        getDiscoveredItemComplexKey(4L, 5, 6L);

    batchDeleteRequest =(BatchDeleteRequest)EasyMock.anyObject();
    EasyMock.expect(discoveredItemsResource.batchDelete(batchDeleteRequest)).andReturn(null).once();

    String uri = "/discovereditems?" +
                                   "ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(1, 2, 3)) +
                                   "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(4, 5, 6));

    checkInvocation(discoveredItemsResource,
                    methodDescriptor,
                    "DELETE",
                    uri,
                    buildBatchPathKeys(keyA, keyB));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCustomCrudParams() throws Exception
  {
    // #1 Verify collection with custom CRUD parameters
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    ResourceMethodDescriptor methodDescriptor;
    CombinedResources.CollectionWithCustomCrudParams resource;
    String body;

    methodDescriptor = model.findMethod(ResourceMethod.GET);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myGet(eq("foo"), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "GET", "/test/foo?intParam=1&stringParam=bar", buildPathKeys("testId", "foo"));

    methodDescriptor = model.findMethod(ResourceMethod.BATCH_GET);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myBatchGet((Set<String>)Matchers.eqCollectionUnordered(Sets.newHashSet("foo", "bar", "baz")), eq(1), eq("qux"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "GET",
                    "/test?ids=foo&ids=bar&ids=baz&intParam=1&stringParam=qux",
                    buildBatchPathKeys("foo", "bar", "baz"));

    methodDescriptor = model.findMethod(ResourceMethod.CREATE);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myCreate((CombinedTestDataModels.Foo) EasyMock.anyObject(), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "POST", "/test?intParam=1&stringParam=bar", "{}");

    methodDescriptor = model.findMethod(ResourceMethod.BATCH_CREATE);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    @SuppressWarnings("rawtypes")
    BatchCreateRequest batchCreateRequest =(BatchCreateRequest)EasyMock.anyObject();
    EasyMock.expect(resource.myBatchCreate(batchCreateRequest, eq(1), eq("bar"))).andReturn(null).once();
    body = RestLiTestHelper.doubleQuote("{'elements':[{},{}]}");
    checkInvocation(resource, methodDescriptor, "POST", "/test?intParam=1&stringParam=bar", body, buildBatchPathKeys());

    methodDescriptor = model.findMethod(ResourceMethod.UPDATE);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myUpdate(eq("foo"), (CombinedTestDataModels.Foo)EasyMock.anyObject(), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "PUT", "/test/foo?intParam=1&stringParam=bar", "{}", buildPathKeys("testId", "foo"));

    methodDescriptor = model.findMethod(ResourceMethod.BATCH_UPDATE);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    @SuppressWarnings("rawtypes")
    BatchUpdateRequest batchUpdateRequest =(BatchUpdateRequest)EasyMock.anyObject();
    EasyMock.expect(resource.myBatchUpdate(batchUpdateRequest, eq(1), eq("baz"))).andReturn(null).once();
    body = RestLiTestHelper.doubleQuote("{'entities':{'foo':{},'bar':{}}}");
    checkInvocation(resource, methodDescriptor, "PUT", "/test?ids=foo&ids=bar&intParam=1&stringParam=baz", body, buildBatchPathKeys("foo", "bar"));

    methodDescriptor = model.findMethod(ResourceMethod.PARTIAL_UPDATE);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    PatchTree p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(42)));
    PatchRequest<CombinedTestDataModels.Foo> expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(resource.myUpdate(eq("foo"), eq(expected), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "POST", "/test/foo?intParam=1&stringParam=bar",
                    "{\"patch\":{\"$set\":{\"foo\":42}}}", buildPathKeys("testId", "foo"));

    methodDescriptor = model.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    @SuppressWarnings("rawtypes")
    BatchPatchRequest batchPatchRequest =(BatchPatchRequest)EasyMock.anyObject();
    EasyMock.expect(resource.myBatchUpdate(batchPatchRequest, eq(1), eq("baz"))).andReturn(null).once();
    body = RestLiTestHelper.doubleQuote("{'entities':{'foo':{},'bar':{}}}");
    checkInvocation(resource, methodDescriptor, "POST", "/test?ids=foo&ids=bar&intParam=1&stringParam=baz", body, buildBatchPathKeys("foo", "bar"));

    methodDescriptor = model.findMethod(ResourceMethod.DELETE);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    EasyMock.expect(resource.myDelete(eq("foo"), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "DELETE", "/test/foo?intParam=1&stringParam=bar", buildPathKeys(
            "testId", "foo"));

    methodDescriptor = model.findMethod(ResourceMethod.BATCH_DELETE);
    resource = getMockResource(CombinedResources.CollectionWithCustomCrudParams.class);
    @SuppressWarnings("rawtypes")
    BatchDeleteRequest batchDeleteRequest =(BatchDeleteRequest)EasyMock.anyObject();
    EasyMock.expect(resource.myBatchDelete(batchDeleteRequest, eq(1), eq("baz"))).andReturn(null).once();
    checkInvocation(resource, methodDescriptor, "DELETE", "/statuses?ids=foo&ids=bar&intParam=1&stringParam=baz", "", buildBatchPathKeys("foo", "bar"));

    // #2 Verify simple resource with custom CRUD parameters
    model = buildResourceModel(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    CombinedResources.SimpleResourceWithCustomCrudParams resource2;

    methodDescriptor = model.findMethod(ResourceMethod.GET);
    resource2 = getMockResource(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    EasyMock.expect(resource2.myGet(eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource2, methodDescriptor, "GET", "/test?intParam=1&stringParam=bar", buildBatchPathKeys());

    methodDescriptor = model.findMethod(ResourceMethod.UPDATE);
    resource2 = getMockResource(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    EasyMock.expect(resource2.myUpdate((CombinedTestDataModels.Foo)EasyMock.anyObject(), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource2, methodDescriptor, "PUT", "/test?intParam=1&stringParam=bar", "{}", buildBatchPathKeys());

    methodDescriptor = model.findMethod(ResourceMethod.PARTIAL_UPDATE);
    resource2 = getMockResource(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    p = new PatchTree();
    p.addOperation(new PathSpec("foo"), PatchOpFactory.setFieldOp(Integer.valueOf(51)));
    expected = PatchRequest.createFromPatchDocument(p.getDataMap());
    EasyMock.expect(resource2.myPartialUpdate(eq(expected), eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource2, methodDescriptor, "POST", "/test?intParam=1&stringParam=bar",
                    "{\"patch\":{\"$set\":{\"foo\":51}}}", buildBatchPathKeys());

    methodDescriptor = model.findMethod(ResourceMethod.DELETE);
    resource2 = getMockResource(CombinedResources.SimpleResourceWithCustomCrudParams.class);
    EasyMock.expect(resource2.myDelete(eq(1), eq("bar"))).andReturn(null).once();
    checkInvocation(resource2, methodDescriptor, "DELETE", "/test?intParam=1&stringParam=bar", buildBatchPathKeys());
  }

 @Test
  public void testAsyncBatchUpdateAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback<?> callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    @SuppressWarnings("unchecked")
    BatchUpdateRequest<CompoundKey, Followed> mockBatchUpdateReq =
        (BatchUpdateRequest<CompoundKey, Followed>)EasyMock.anyObject();
    resource.batchUpdate(mockBatchUpdateReq, EasyMock.<Callback<BatchUpdateResult<CompoundKey, Followed>>> anyObject());
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

    String uri = "/asyncfollows?" +
                                 "ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(1L, 2L)) +
                                 "&ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(3L, 4L)) +
                                 "&ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(5L, 6L));

    String entityBody = RestLiTestHelper.doubleQuote("{'entities':{" +
                                                                   "'followeeID=2&followerID=1': {}, " +
                                                                   "'followeeID=4&followerID=3': {}, " +
                                                                   "'followeeID=6&followerID=5': {}" +
                                                                   "}}");

    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         uri,
                         entityBody,
                         buildBatchPathKeys(buildFollowsCompoundKey(1L, 2L),
                                            buildFollowsCompoundKey(3L, 4L),
                                            buildFollowsCompoundKey(5L, 6L)));
  }

  @Test
  public void testAsyncBatchPatchAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback<?> callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    @SuppressWarnings("unchecked")
    BatchPatchRequest<CompoundKey, Followed> mockBatchPatchReq =
        (BatchPatchRequest<CompoundKey, Followed>)EasyMock.anyObject();
    resource.batchUpdate(mockBatchPatchReq, EasyMock.<Callback<BatchUpdateResult<CompoundKey, Followed>>>anyObject());
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

    String uri = "/asyncfollows?" +
                                 "ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(1L, 2L)) +
                                 "&ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(3L, 4L)) +
                                 "&ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(5L, 6L));

    String entityBody = RestLiTestHelper.doubleQuote("{'entities':{" +
                                                         "'followeeID=2&followerID=1': {}, " +
                                                         "'followeeID=4&followerID=3': {}, " +
                                                         "'followeeID=6&followerID=5': {}" +
                                                         "}}");

    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "POST",
                         uri,
                         entityBody,
                         buildBatchPathKeys(buildFollowsCompoundKey(1L, 2L),
                                            buildFollowsCompoundKey(3L, 4L),
                                            buildFollowsCompoundKey(5L, 6L)));
  }

  @Test
  public void testAsyncBatchDeleteAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback<?> callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    @SuppressWarnings("unchecked")
    BatchDeleteRequest<CompoundKey, Followed> mockBatchDeleteReq =
        (BatchDeleteRequest<CompoundKey, Followed>)EasyMock.anyObject();
    resource.batchDelete(mockBatchDeleteReq, EasyMock.<Callback<BatchUpdateResult<CompoundKey, Followed>>> anyObject());
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

    String uri = "/asyncfollows?" +
                                 "ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(1L, 2L)) +
                                 "&ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(3L, 4L)) +
                                 "&ids=" + RestLiTestHelper.simpleURLEncode(buildFollowsURLKey(5L, 6L));

    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "DELETE",
                         uri,
                         buildBatchPathKeys(buildFollowsCompoundKey(1L, 2L),
                                            buildFollowsCompoundKey(3L, 4L),
                                            buildFollowsCompoundKey(5L, 6L)));
  }

  @Test
  public void testAsyncGetAllAssociativeResource() throws Exception
  {
    ResourceModel followsResourceModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    RestLiCallback<?> callback = getCallback();
    ResourceMethodDescriptor methodDescriptor;
    AsyncFollowsAssociativeResource resource;

    methodDescriptor = followsResourceModel.findMethod(ResourceMethod.GET_ALL);
    resource = getMockResource(AsyncFollowsAssociativeResource.class);

    resource.getAll((PagingContext)EasyMock.anyObject(), EasyMock.<Callback<List<Followed>>> anyObject());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>()
    {
      @Override
      public Object answer()
          throws Throwable
      {
        @SuppressWarnings("unchecked")
        Callback<List<Followed>> callback =
            (Callback<List<Followed>>) EasyMock.getCurrentArguments()[1];
        callback.onSuccess(null);
        return null;
      }
    });
    EasyMock.replay(resource);

    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         "GET",
                         "/asyncfollows",
                         buildBatchPathKeys());
  }

  @Test
  public void testAsyncBatchCreateComplexKeyResource() throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.BATCH_CREATE);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    BatchCreateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> mockBatchCreateReq =
        (BatchCreateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>)EasyMock.anyObject();
    discoveredResource.batchCreate(mockBatchCreateReq,
                                   EasyMock.<Callback<BatchCreateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>>anyObject());
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
                         uri,
                         entityBody,
                         buildBatchPathKeys());
  }

  @Test
  public void testAsyncBatchUpdateComplexKeyResource() throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.BATCH_UPDATE);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    BatchUpdateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> mockBatchUpdateReq =
        (BatchUpdateRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>)EasyMock.anyObject();
    discoveredResource.batchUpdate(mockBatchUpdateReq,
                                   EasyMock.<Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>>anyObject());
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

    String uri = "/asyncdiscovereditems?" +
                                        "ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(1, 1, 1)) +
                                        "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(2, 2, 2)) +
                                        "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(3, 3, 3));

    String entityBody = RestLiTestHelper.doubleQuote("{'entities':{" +
                                                         "'itemId=1&type=1&userId=1': {}, " +
                                                         "'itemId=2&type=2&userId=2': {}, " +
                                                         "'itemId=3&type=3&userId=3': {}" +
                                                         "}}");

    checkAsyncInvocation(discoveredResource,
                         callback,
                         methodDescriptor,
                         "PUT",
                         uri,
                         entityBody,
                         buildBatchPathKeys(getDiscoveredItemComplexKey(1, 1, 1),
                                            getDiscoveredItemComplexKey(2, 2, 2),
                                            getDiscoveredItemComplexKey(3, 3, 3)));
  }

  @Test
  public void testAsyncBatchPatchComplexKeyResource() throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    BatchPatchRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> mockBatchPatchReq =
        (BatchPatchRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>)EasyMock.anyObject();
    discoveredResource.batchUpdate(mockBatchPatchReq,
                                   EasyMock.<Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>>anyObject());
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

    String uri = "/asyncdiscovereditems?" +
                                        "ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(1, 1, 1)) +
                                        "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(2, 2, 2)) +
                                        "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(3, 3, 3));

    String entityBody = RestLiTestHelper.doubleQuote("{'entities':{" +
                                                         "'itemId=1&type=1&userId=1': {}, " +
                                                         "'itemId=2&type=2&userId=2': {}, " +
                                                         "'itemId=3&type=3&userId=3': {}" +
                                                         "}}");

    checkAsyncInvocation(discoveredResource,
                         callback,
                         methodDescriptor,
                         "POST",
                         uri,
                         entityBody,
                         buildBatchPathKeys(getDiscoveredItemComplexKey(1, 1, 1),
                                            getDiscoveredItemComplexKey(2, 2, 2),
                                            getDiscoveredItemComplexKey(3, 3, 3)));
  }

  @Test
  public void testAsyncBatchDeleteComplexResource() throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.BATCH_DELETE);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    BatchDeleteRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem> mockBatchDeleteReq =
        (BatchDeleteRequest<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>)EasyMock.anyObject();
    discoveredResource.batchDelete(mockBatchDeleteReq,
                                   EasyMock.<Callback<BatchUpdateResult<ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>, DiscoveredItem>>>anyObject());
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

    String uri = "/asyncdiscovereditems?" +
                                        "ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(1, 1, 1)) +
                                        "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(2, 2, 2)) +
                                        "&ids=" + RestLiTestHelper.simpleURLEncode(buildDiscoveredItemURLKey(3, 3, 3));

    checkAsyncInvocation(discoveredResource,
                         callback,
                         methodDescriptor,
                         "DELETE",
                         uri,
                         buildBatchPathKeys(getDiscoveredItemComplexKey(1, 1, 1),
                                            getDiscoveredItemComplexKey(2, 2, 2),
                                            getDiscoveredItemComplexKey(3, 3, 3)));
  }

  @Test
  public void testAsyncGetAllComplexKeyResource() throws Exception
  {
    ResourceModel discoveredResourceModel = buildResourceModel(AsyncDiscoveredItemsResource.class);
    RestLiCallback<?> callback = getCallback();

    ResourceMethodDescriptor methodDescriptor;
    AsyncDiscoveredItemsResource discoveredResource;

    methodDescriptor = discoveredResourceModel.findMethod(ResourceMethod.GET_ALL);
    discoveredResource = getMockResource(AsyncDiscoveredItemsResource.class);

    @SuppressWarnings("unchecked")
    PagingContext mockCtx = (PagingContext)EasyMock.anyObject();
    discoveredResource.getAll(mockCtx, EasyMock.<Callback<List<DiscoveredItem>>>anyObject());
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
                         "/asyncdiscovereditems",
                         buildBatchPathKeys());
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

    for (Object batchKey : batchKeys)
    {
      result.appendBatchValue(batchKey);
    }

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
                                      String uri)
                                          throws URISyntaxException, RestLiSyntaxException
                                          {
    try
    {
      checkInvocation(statusResource, methodDescriptor, httpMethod, uri);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      // expected
    }
    finally
    {
      reset(statusResource);
    }
                                          }

  private <R> R getMockResource(Class<R> resourceClass)
  {
    R resource = _resourceFactory.getMock(resourceClass);
    EasyMock.reset(resource);
    EasyMock.makeThreadSafe(resource, true);
    if (BaseResource.class.isAssignableFrom(resourceClass))
    {
      BaseResource baseResource = (BaseResource)resource;
      baseResource.setContext((ResourceContext)EasyMock.anyObject());
      EasyMock.expectLastCall().once();
    }
    return resource;
  }

  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               String uri)
                                   throws URISyntaxException, RestLiSyntaxException
                                   {
    checkInvocation(resource, resourceMethodDescriptor, httpMethod, uri, null, null);
                                   }

  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               String uri,
                               String entityBody)
                                   throws URISyntaxException, RestLiSyntaxException
                                   {
    checkInvocation(resource, resourceMethodDescriptor, httpMethod, uri, entityBody, null);
  }

  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               String uri,
                               MutablePathKeys pathkeys)
                                   throws URISyntaxException, RestLiSyntaxException
                                   {
    checkInvocation(resource, resourceMethodDescriptor, httpMethod, uri, null, pathkeys);
                                   }


  private void checkInvocation(Object resource,
                               ResourceMethodDescriptor resourceMethodDescriptor,
                               String httpMethod,
                               String uri,
                               String entityBody,
                               MutablePathKeys pathkeys)
          throws URISyntaxException, RestLiSyntaxException
{
    assertNotNull(resource);
    assertNotNull(resourceMethodDescriptor);

    try
    {
      EasyMock.replay(resource);

      RestRequestBuilder builder = new RestRequestBuilder(new URI(uri)).setMethod(httpMethod);
      if (entityBody != null)
      {
        builder.setEntity(entityBody.getBytes(Data.UTF_8_CHARSET));
      }
      RestRequest request = builder.build();
      RoutingResult routingResult = new RoutingResult(new ResourceContextImpl(pathkeys, request,
                                                                              new RequestContext()), resourceMethodDescriptor);
      final CountDownLatch latch = new CountDownLatch(1);
      final RestLiCallback<Object> callback = new RestLiCallback<Object>(request, routingResult, new RestLiResponseHandler(), new Callback<RestResponse>()
      {
        @Override
        public void onError(final Throwable e)
        {
          latch.countDown();
        }

        @Override
        public void onSuccess(final RestResponse result)
        {
          latch.countDown();
        }
      });
      _invoker.invoke(routingResult, request, callback);
      try
      {
        latch.await();
      }
      catch (InterruptedException e)
      {
        // Ignore
      }
      EasyMock.verify(resource);
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

  private RestLiCallback<Object> getCallback()
  {
    @SuppressWarnings("unchecked")
    RestLiCallback<Object> callback = (RestLiCallback<Object>)EasyMock.createMock(RestLiCallback.class);
    callback.onSuccess(EasyMock.anyObject());
    EasyMock.expectLastCall().once();
    EasyMock.replay(callback);
    return callback;
  }

  @SuppressWarnings({"rawtypes"})
  private void checkAsyncInvocation(BaseResource resource,
                                    RestLiCallback callback,
                                    ResourceMethodDescriptor methodDescriptor,
                                    String httpMethod,
                                    String uri,
                                    MutablePathKeys pathkeys) throws URISyntaxException
  {
    checkAsyncInvocation(resource,
                         callback,
                         methodDescriptor,
                         httpMethod,
                         uri,
                         null,
                         pathkeys);

  }

  @SuppressWarnings({"unchecked","rawtypes"})
  private void checkAsyncInvocation(BaseResource resource,
                                    RestLiCallback callback,
                                    ResourceMethodDescriptor methodDescriptor,
                                    String httpMethod,
                                    String uri,
                                    String entityBody,
                                    MutablePathKeys pathkeys) throws URISyntaxException
  {
    try
    {

      RestRequestBuilder builder =
          new RestRequestBuilder(new URI(uri)).setMethod(httpMethod);
      if (entityBody != null)
      {
        builder.setEntity(entityBody.getBytes(Data.UTF_8_CHARSET));
      }
      RestRequest request = builder.build();
      RoutingResult routingResult =
          new RoutingResult(new ResourceContextImpl(pathkeys, request,
                                                    new RequestContext()), methodDescriptor);

      _invoker.invoke(routingResult, request, callback);
      EasyMock.verify(resource);
      EasyMock.verify(callback);
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

  /**
   * Builds the URL  key for a FollowsAssociativeResource and AsyncFollowsAssociativeResource
   * @param followerID
   * @param followeeID
   * @return association key for a URL
   */
  private String buildFollowsURLKey(Long followerID, Long followeeID)
  {
    return "followerID=" + followerID + "&followeeID=" + followeeID;
  }

  private ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams> getDiscoveredItemComplexKey(
      long itemId, int type, long userId)
  {
    return new ComplexResourceKey<DiscoveredItemKey, DiscoveredItemKeyParams>(
        new DiscoveredItemKey().setItemId(itemId).setType(type).setUserId(userId),
        new DiscoveredItemKeyParams());
  }

  /**
   * Builds the URL key for a DiscoveredItemsResource and AsyncDiscoveredItemsResource
   * @param itemId
   * @param type
   * @param userId
   * @return complex key for a URL
   */
  private String buildDiscoveredItemURLKey(long itemId, int type, long userId)
  {
    return "asyncDiscoveredItemId.userId=" + userId +
           "&asyncDiscoveredItemId.type=" + type +
           "&asyncDiscoveredItemId.itemId=" + itemId;
  }

}
