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

import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModels;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.RestLiRouter;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.combined.CombinedResources;
import com.linkedin.restli.server.twitter.FollowsAssociativeResource;
import com.linkedin.restli.server.twitter.LocationResource;
import com.linkedin.restli.server.twitter.RepliesCollectionResource;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TrendingResource;
import com.linkedin.restli.server.twitter.TrendRegionsCollectionResource;
import com.linkedin.restli.server.twitter.TwitterAccountsResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Trending;

/**
 * @author dellamag
 */
public class TestRestLiRouting
{
  private RestLiRouter _router;

  /**
   * Tests details of the RoutingResponse, including PathKeys and fields of the ResourceMethodDescriptor itself
   */
  @Test
  public void testRoutingDetails() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          FollowsAssociativeResource.class,
                          TrendingResource.class);

    _router = new RestLiRouter(pathRootResourceMap);

    RestRequest request;
    RoutingResult result;
    ResourceMethodDescriptor resourceMethodDescriptor;
    PathKeys keys;

    // #1 simple GET
    request = new RestRequestBuilder(new URI("/statuses/1")).setMethod("GET").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.GET);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), StatusCollectionResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "get");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {Long.class});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "statuses");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertEquals(keys.getAsLong("statusID"), new Long(1));
    assertNull(keys.getAsString("foo"));

    // #2 GET with compound key
    request = new RestRequestBuilder(new URI("/follows/followerID:1;followeeID:2"))
             .setMethod("GET").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.GET);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), FollowsAssociativeResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "get");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {CompoundKey.class});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "follows");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertEquals(keys.getAsLong("followerID"), new Long(1L));
    assertEquals(keys.getAsLong("followeeID"), new Long(2L));

    // #3 simple UPDATE
    request = new RestRequestBuilder(new URI("/statuses/1")).setMethod("PUT").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.UPDATE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), StatusCollectionResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "update");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] { Long.class, Status.class });
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "statuses");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertEquals(keys.getAsLong("statusID"), new Long(1));
    assertNull(keys.getAsString("foo"));

    // #4 simple DELETE
    request = new RestRequestBuilder(new URI("/statuses/1")).setMethod("DELETE").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.DELETE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), StatusCollectionResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "delete");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] { Long.class });
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "statuses");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertEquals(keys.getAsLong("statusID"), new Long(1));
    assertNull(keys.getAsString("foo"));

    // #5 GET association
    // TODO Need a real associative resource

    // #6 Batch GET association
    request = new RestRequestBuilder(new URI("/follows?ids=followerID:1;followeeID:2&ids=followerID:3;followeeID:4"))
              .setMethod("GET").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.BATCH_GET);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), FollowsAssociativeResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "batchGet");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {Set.class});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "follows");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertNull(keys.getAsString("followerID"));
    assertNull(keys.getAsString("followeeID"));

    CompoundKey key1 = new CompoundKey();
    key1.append("followerID", 1L);
    key1.append("followeeID", 2L);
    CompoundKey key2 = new CompoundKey();
    key2.append("followerID", 3L);
    key2.append("followeeID", 4L);
    Set<CompoundKey> expectedBatchKeys = new HashSet<CompoundKey>();
    expectedBatchKeys.add(key1);
    expectedBatchKeys.add(key2);

    assertEquals(keys.getBatchKeys().size(), 2);
    for (CompoundKey batchKey : keys.getBatchKeys(CompoundKey.class))
    {
      assertTrue(expectedBatchKeys.contains(batchKey));
      expectedBatchKeys.remove(batchKey);
    }
    assertEquals(expectedBatchKeys.size(), 0);

    // #7 simple GET on simple resource
    request = new RestRequestBuilder(new URI("/trending")).setMethod("GET").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.GET);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), TrendingResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "get");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "trending");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertEquals(keys.getBatchKeys().size(), 0);

    // #8 simple UPDATE on simple resource
    request = new RestRequestBuilder(new URI("/trending")).setMethod("PUT").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.UPDATE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), TrendingResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "update");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] { Trending.class });
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "trending");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertEquals(keys.getBatchKeys().size(), 0);

    // #9 partial UPDATE on simple resource
    request = new RestRequestBuilder(new URI("/trending")).setMethod("POST").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.PARTIAL_UPDATE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), TrendingResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "update");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] { PatchRequest.class });
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "trending");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertEquals(keys.getBatchKeys().size(), 0);

    // #10 simple DELETE on simple resource
    request = new RestRequestBuilder(new URI("/trending")).setMethod("DELETE").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);

    resourceMethodDescriptor = result.getResourceMethod();
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.DELETE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), TrendingResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "delete");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "trending");

    assertNotNull(result.getContext());
    keys = result.getContext().getPathKeys();
    assertEquals(keys.getBatchKeys().size(), 0);
  }

  /**
   * Covers all the conditional routing logic to ensure the correct method was located.
   */
  @Test
  public void testRoutingBasic() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          FollowsAssociativeResource.class,
                          RepliesCollectionResource.class,
                          LocationResource.class,
                          TrendingResource.class,
                          TrendRegionsCollectionResource.class);

    _router = new RestLiRouter(pathRootResourceMap);

    checkResult("/statuses/1", "GET",
                ResourceMethod.GET, StatusCollectionResource.class, "get", false, "statusID");
    checkResult("/statuses/1", "GET", "GET",
                ResourceMethod.GET, StatusCollectionResource.class, "get", false, "statusID");

    checkResult("/st%61tuses/1", "GET",
                ResourceMethod.GET, StatusCollectionResource.class, "get", false, "statusID");
    checkResult("/statuses/%31", "GET",
                ResourceMethod.GET, StatusCollectionResource.class, "get", false, "statusID");
    expectRoutingException("/statuses%2F1", "GET");
    checkResult("/statuses/-1", "GET",
                ResourceMethod.GET, StatusCollectionResource.class, "get", false, "statusID");
    checkResult("/statuses?ids=1&ids=2&ids3", "GET",
                ResourceMethod.BATCH_GET, StatusCollectionResource.class, "batchGet", true);
    checkResult("/statuses?ids=1&ids=2&ids=3", "GET", "BATCH_GET",
                ResourceMethod.BATCH_GET, StatusCollectionResource.class, "batchGet", true);
    checkResult("/statuses", "POST", ResourceMethod.CREATE, StatusCollectionResource.class, "create", false);
    checkResult("/statuses", "POST", "CREATE", ResourceMethod.CREATE, StatusCollectionResource.class, "create", false);
    checkResult("/statuses/1/replies", "POST", ResourceMethod.CREATE, RepliesCollectionResource.class, "create", false, "statusID");
    checkResult("/statuses/1/replies?ids=1&ids=2&ids=3", "GET",
                ResourceMethod.BATCH_GET, RepliesCollectionResource.class, "batchGet", true, "statusID");
    checkResult("/statuses/1/location", "GET", ResourceMethod.GET, LocationResource.class, "get", false, "statusID");
    checkResult("/statuses/1/location", "PUT", ResourceMethod.UPDATE, LocationResource.class, "update", false, "statusID");
    checkResult("/statuses/1/location", "DELETE", ResourceMethod.DELETE, LocationResource.class, "delete", false, "statusID");
    checkResult("/statuses/1", "PUT", ResourceMethod.UPDATE, StatusCollectionResource.class, "update", false);
    checkResult("/statuses/1", "PUT", "UPDATE", ResourceMethod.UPDATE, StatusCollectionResource.class, "update", false);
    checkResult("/statuses/1", "POST", ResourceMethod.PARTIAL_UPDATE, StatusCollectionResource.class, "update", false);
    checkResult("/statuses/1", "POST", "PARTIAL_UPDATE", ResourceMethod.PARTIAL_UPDATE, StatusCollectionResource.class, "update", false);
    checkResult("/statuses/1", "DELETE", ResourceMethod.DELETE, StatusCollectionResource.class, "delete", false);
    checkResult("/statuses/1", "DELETE", "DELETE", ResourceMethod.DELETE, StatusCollectionResource.class, "delete", false);

    checkResult("/statuses?q=search", "GET",
                ResourceMethod.FINDER, StatusCollectionResource.class, "search", false);
    checkResult("/statuses?q=search", "GET", "FINDER",
                ResourceMethod.FINDER, StatusCollectionResource.class, "search", false);
    checkResult("/statuses?q=search&keywords=linkedin", "GET",
                ResourceMethod.FINDER, StatusCollectionResource.class, "search", false);
    checkResult("/statuses?q=user_timeline", "GET",
                ResourceMethod.FINDER, StatusCollectionResource.class, "getUserTimeline", false);
    checkResult("/statuses?q=public_timeline", "GET",
                ResourceMethod.FINDER, StatusCollectionResource.class, "getPublicTimeline", false);

    checkResult("/follows/followerID:1;followeeID:1", "GET",
                ResourceMethod.GET, FollowsAssociativeResource.class, "get", false, "followerID", "followeeID");
    checkResult("/follows/followerID:1;followeeID:1", "PUT",
                ResourceMethod.UPDATE, FollowsAssociativeResource.class, "update", false, "followerID", "followeeID");
    checkResult("/follows/followerID:1;followeeID:1", "POST",
                ResourceMethod.PARTIAL_UPDATE, FollowsAssociativeResource.class, "update", false, "followerID", "followeeID");
    checkResult("/follows?ids=followerID:1;followeeID:1&ids=followerID:1;followeeID:3&ids=followerID:1;followeeID:2", "GET",
                ResourceMethod.BATCH_GET, FollowsAssociativeResource.class, "batchGet", true);
    checkResult("/follows?q=friends&userID=1", "GET",
                ResourceMethod.FINDER, FollowsAssociativeResource.class, "getFriends", false);
    checkResult("/follows?q=followers&userID=1", "GET",
                ResourceMethod.FINDER, FollowsAssociativeResource.class, "getFollowers", false);
    checkResult("/follows?q=followers&userID=1", "GET",
                ResourceMethod.FINDER, FollowsAssociativeResource.class, "getFollowers", false);
    checkResult("/follows/followerID:1?q=other&someParam=value", "GET",
                ResourceMethod.FINDER, FollowsAssociativeResource.class, "getOther", false);

    checkBatchKeys("/statuses?ids=1&ids=2&ids=3", "GET", new HashSet<Object>(Arrays.asList(1L, 2L, 3L)));
    checkBatchKeys("/statuses?ids=1&ids=%32&ids=3", "GET", new HashSet<Object>(Arrays.asList(1L, 2L, 3L)));

    checkResult("/statuses", "POST", "BATCH_CREATE", ResourceMethod.BATCH_CREATE, StatusCollectionResource.class, "batchCreate", false);
    checkResult("/statuses?ids=1&ids=2", "PUT", ResourceMethod.BATCH_UPDATE, StatusCollectionResource.class, "batchUpdate", true);
    checkResult("/statuses?ids=1&ids=2", "PUT", "BATCH_UPDATE", ResourceMethod.BATCH_UPDATE, StatusCollectionResource.class, "batchUpdate", true);
    checkResult("/statuses?ids=1&ids=2", "POST", "BATCH_PARTIAL_UPDATE", ResourceMethod.BATCH_PARTIAL_UPDATE, StatusCollectionResource.class, "batchUpdate", true);
    checkResult("/statuses?ids=1&ids=2", "DELETE", ResourceMethod.BATCH_DELETE, StatusCollectionResource.class, "batchDelete", true);
    checkResult("/statuses?ids=1&ids=2", "DELETE", "BATCH_DELETE", ResourceMethod.BATCH_DELETE, StatusCollectionResource.class, "batchDelete", true);

    checkResult("/trending", "GET", ResourceMethod.GET, TrendingResource.class, "get", false);
    checkResult("/trending", "PUT", ResourceMethod.UPDATE, TrendingResource.class, "update", false);
    checkResult("/trending", "DELETE", ResourceMethod.DELETE, TrendingResource.class, "delete", false);
    checkResult("/trending", "POST", "PARTIAL_UPDATE", ResourceMethod.PARTIAL_UPDATE, TrendingResource.class, "update", false);

    checkResult("/trending/trendRegions/1", "GET", ResourceMethod.GET, TrendRegionsCollectionResource.class, "get", false, "trendRegionId");
    checkResult("/trending/trendRegions/1", "PUT", ResourceMethod.UPDATE, TrendRegionsCollectionResource.class, "update", false);
    checkResult("/trending/trendRegions/1", "POST", ResourceMethod.PARTIAL_UPDATE, TrendRegionsCollectionResource.class, "update", false);
    checkResult("/trending/trendRegions", "POST", ResourceMethod.CREATE, TrendRegionsCollectionResource.class, "create", false);
    checkResult("/trending/trendRegions/1", "DELETE", ResourceMethod.DELETE, TrendRegionsCollectionResource.class, "delete", false);
    checkResult("/trending/trendRegions?ids=1&ids=2&ids=3", "GET",
                ResourceMethod.BATCH_GET, TrendRegionsCollectionResource.class, "batchGet", true);
    checkResult("/trending/trendRegions?ids=1&ids=2", "PUT", "BATCH_UPDATE", ResourceMethod.BATCH_UPDATE, TrendRegionsCollectionResource.class, "batchUpdate", true);
    checkResult("/trending/trendRegions?ids=1&ids=2", "POST", "BATCH_PARTIAL_UPDATE", ResourceMethod.BATCH_PARTIAL_UPDATE, TrendRegionsCollectionResource.class, "batchUpdate", true);
    checkResult("/trending/trendRegions?q=get_trending_by_popularity", "GET",
                ResourceMethod.FINDER, TrendRegionsCollectionResource.class, "getTrendingByPopularity", false);

    //the following two cases would log warnings
    checkBatchKeys("/statuses?ids=1&ids=&ids=3", "GET", new HashSet<Object>(Arrays.asList(1L, 3L)));
    checkBatchKeys("/statuses?ids=1&ids=abc&ids=3", "GET", new HashSet<Object>(Arrays.asList(1L, 3L)));
  }

  @Test
  public void testRestLiMethodMismatch() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          FollowsAssociativeResource.class,
                          RepliesCollectionResource.class);

    _router = new RestLiRouter(pathRootResourceMap);

    expectRoutingException("/statuses/1", "GET", "CREATE");
    expectRoutingException("/statuses?ids=1&ids=2&ids=3", "GET", "CREATE");
    expectRoutingException("/statuses", "POST", "GET");
    expectRoutingException("/statuses/1", "PUT", "CREATE");
    expectRoutingException("/statuses/1", "POST", "CREATE");
    expectRoutingException("/statuses/1", "DELETE", "CREATE");
    expectRoutingException("/statuses?q=search", "GET", "CREATE");
    expectRoutingException("/statuses", "POST", "GET");
    expectRoutingException("/statuses?ids=1&ids=2", "PUT", "CREATE");
    expectRoutingException("/statuses?ids=1&ids=2", "POST", "CREATE");
    expectRoutingException("/statuses?ids=1&ids=2", "DELETE", "CREATE");

    expectRoutingException("/statuses/1", "GET", "FOO");
    expectRoutingException("/statuses?ids=1,2,3", "GET", "FOO");
    expectRoutingException("/statuses", "POST", "FOO");
    expectRoutingException("/statuses/1", "PUT", "FOO");
    expectRoutingException("/statuses/1", "POST", "FOO");
    expectRoutingException("/statuses/1", "DELETE", "FOO");
    expectRoutingException("/statuses?q=search", "GET", "FOO");
    expectRoutingException("/statuses", "POST", "FOO");
    expectRoutingException("/statuses?ids=1&ids=2", "PUT", "FOO");
    expectRoutingException("/statuses?ids=1&ids=2", "POST", "FOO");
    expectRoutingException("/statuses?ids=1&ids=2", "DELETE", "FOO");
  }


  @Test
  public void testNKeyAssociationRouting() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(
            CombinedResources.CombinedNKeyAssociationResource.class);
    _router = new RestLiRouter(pathRootResourceMap);

    checkResult("/test/foo=1&bar=2&baz=3", "GET",
                ResourceMethod.GET, CombinedResources.CombinedNKeyAssociationResource.class, "get", false, "foo", "bar", "baz");
    checkResult("/test/foo:1;bar:2;baz:3", "GET", // same as above but legacy way to spell compound keys
                ResourceMethod.GET, CombinedResources.CombinedNKeyAssociationResource.class, "get", false, "foo", "bar", "baz");

    checkResult("/test/foo=%3A&bar=%3B&baz=%3D", "GET",
                ResourceMethod.GET, CombinedResources.CombinedNKeyAssociationResource.class, "get", false, "foo", "bar", "baz");
    checkResult("/test/foo:%3A;bar:%3B;baz:%3D", "GET", // same as above but legacy way to spell compound keys
                ResourceMethod.GET, CombinedResources.CombinedNKeyAssociationResource.class, "get", false, "foo", "bar", "baz");

    checkResult("/test/foo=1&bar=2?q=find", "GET",
                ResourceMethod.FINDER, CombinedResources.CombinedNKeyAssociationResource.class, "find", false, "foo", "bar");
    checkResult("/test/foo:1;bar:2?q=find", "GET", // same as above but legacy way to spell compound keys
                ResourceMethod.FINDER, CombinedResources.CombinedNKeyAssociationResource.class, "find", false, "foo", "bar");

    // Test delimiters encoding/decoding in assocKeys values
    CompoundKey key1 = new CompoundKey();
    key1.append("foo", "1,1").append("bar", "1:2");
    CompoundKey key2 = new CompoundKey();
    key2.append("foo", "2,1").append("bar", "2;2");
    // Note double encoding of the separators in the compound keys. One comes from CompoundKey.toString(), another is applied
    // to the entire ids param value.
    String uri =
        "/test?ids=" + UriComponent.encode(key1.toString(), UriComponent.Type.QUERY_PARAM) + "&ids="
            + UriComponent.encode(key2.toString(), UriComponent.Type.QUERY_PARAM);
    assertEquals(uri, "/test?ids=bar%3D1%253A2%26foo%3D1%252C1&ids=bar%3D2%253B2%26foo%3D2%252C1");

    checkResult(uri, "GET",
                ResourceMethod.BATCH_GET, CombinedResources.CombinedNKeyAssociationResource.class, "batchGet", true);

    checkBatchKeys(uri, "GET", new HashSet<Object>(Arrays.asList(key1, key2)));

    expectRoutingException("/test/foo=1", "GET");
    expectRoutingException("/test/foo:1", "GET"); // same as above but legacy way to spell compound keys

    expectRoutingException("/test/foo=1&bar=2&baz=3&qux=4", "GET");
    expectRoutingException("/test/foo:1;bar:2;baz:3;qux:4", "GET"); // same as above but legacy way to spell compound keys

  }

  @Test
  public void testRoutingExceptions() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          FollowsAssociativeResource.class,
                          RepliesCollectionResource.class,
                          LocationResource.class,
                          TrendingResource.class,
                          TrendRegionsCollectionResource.class);

    _router = new RestLiRouter(pathRootResourceMap);

    // TODO Would be nice to check status code as well
    expectRoutingException("/", "GET");
    expectRoutingException("/", "POST");
    expectRoutingException("/", "PUT");
    expectRoutingException("/", "DELETE");

    expectRoutingException("/replies", "GET");
    expectRoutingException("/location", "GET");
    expectRoutingException("/trendRegions", "GET");
    expectRoutingException("/asdfasf", "GET");
    expectRoutingException("/1", "GET");

    expectRoutingException("/statuses", "PUT");
    expectRoutingException("/statuses", "DELETE");
    expectRoutingException("/statuses/1/asdf", "GET");
    expectRoutingException("/statuses/1/replies/2", "GET");
    expectRoutingException("/statuses/replies", "GET");
    expectRoutingException("/statuses/2.3", "GET");
    expectRoutingException("/statuses/1/replies", "DELETE");
    expectRoutingException("/statuses/1/replies", "PUT");
    expectRoutingException("/statuses/1/2", "GET");
    expectRoutingException("/statuses/1/badpath", "GET");
    expectRoutingException("/statuses/1/badpath/2", "GET");

    expectRoutingException("/statuses?q=wrong&keywords=linkedin", "GET");
    expectRoutingException("/statuses?q=wrong&keywords=linkedin", "PUT");
    expectRoutingException("/statuses?q=wrong&keywords=linkedin", "DELETE");
    expectRoutingException("/statuses?q=wrong&keywords=linkedin", "POST");
    expectRoutingException("/statuses/1/replies?q=wrong", "GET");
    expectRoutingException("/statuses/1/location/1", "GET");
    expectRoutingException("/statuses/1/location/1", "PUT");
    expectRoutingException("/statuses/1/location/1", "POST");
    expectRoutingException("/statuses/1/location/1", "DELETE");
    expectRoutingException("/statuses/1/location?q=wrong", "GET");
    expectRoutingException("/statuses/1/location?q=wrong&keywords=linkedin", "POST");

    expectRoutingException("/follows", "POST");
    expectRoutingException("/follows", "PUT");
    expectRoutingException("/follows", "DELETE");
    expectRoutingException("/follows/1", "GET");
    expectRoutingException("/follows?q=wrong", "GET");
    expectRoutingException("/follows/followerID=1/bad_path", "GET");
    expectRoutingException("/follows/followerID:1;followerID:2/bad_path", "GET");
    expectRoutingException("/follows/followerID=1;wrongID=2", "GET");

    // delete not supported
    expectRoutingException("/follows/followerID:1;followeeID:1", "DELETE");

    expectRoutingException("/trending/1", "GET");
    expectRoutingException("/trending/1", "PUT");
    expectRoutingException("/trending/1", "POST");
    expectRoutingException("/trending/1", "DELETE");
    expectRoutingException("/trending?q=abc", "GET");
    expectRoutingException("/trending?q=def&param1=1", "POST");

    expectRoutingException("/trending/1/trendRegions/1", "GET");
    expectRoutingException("/trending/1/trendRegions/1", "PUT");
    expectRoutingException("/trending/1/trendRegions/1", "POST");
    expectRoutingException("/trending/1/trendRegions/1", "DELETE");

    expectRoutingException("/trending/trendRegions", "GET");
    expectRoutingException("/trending/trendRegions", "PUT");
    expectRoutingException("/trending/trendRegions", "DELETE");
    expectRoutingException("/trending/trendRegions?q=abc", "GET");
    expectRoutingException("/trending/trendRegions?q=def&param1=1", "POST");
  }

  @Test
  public void testActionRouting() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          RepliesCollectionResource.class,
                          LocationResource.class,
                          TwitterAccountsResource.class);
    _router = new RestLiRouter(pathRootResourceMap);

    RestRequest request;
    RoutingResult result;

    // #1 route to root action
    request = new RestRequestBuilder(new URI("/accounts?action=register")).setMethod("POST").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);
    assertEquals(result.getResourceMethod().getActionName(), "register");
    assertEquals(result.getResourceMethod().getType(), ResourceMethod.ACTION);

    // #2 route to contextual action on a nested resource
    request = new RestRequestBuilder(new URI("/statuses/1/replies?action=replyToAll")).setMethod("POST").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);
    assertEquals(result.getResourceMethod().getActionName(), "replyToAll");
    assertEquals(result.getResourceMethod().getType(), ResourceMethod.ACTION);
    assertEquals(result.getContext().getPathKeys().get("statusID"), 1L);

    // #3 route to action on a nested simple resource
    request = new RestRequestBuilder(new URI("/statuses/1/location?action=new_status_from_location")).setMethod("POST").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);
    assertEquals(result.getResourceMethod().getActionName(), "new_status_from_location");
    assertEquals(result.getResourceMethod().getType(), ResourceMethod.ACTION);
    assertEquals(result.getResourceMethod().getMethod().getParameterTypes(), new Class<?>[] { String.class });
    assertEquals(result.getContext().getPathKeys().get("statusID"), 1L);
  }

  @Test
  public void testActionRoutingErrors() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          RepliesCollectionResource.class,
                          LocationResource.class,
                          TwitterAccountsResource.class);
    _router = new RestLiRouter(pathRootResourceMap);

    expectRoutingException("/bogusResource", "GET");
    expectRoutingException("/accounts", "POST");
    expectRoutingException("/accounts?q=register", "POST");
    expectRoutingException("/accounts?action=bogusMethod", "POST");
    expectRoutingException("/accounts?action=", "POST");
    expectRoutingException("/accounts?action", "POST");
    expectRoutingException("/accounts?action=register", "GET");
    expectRoutingException("/accounts?action=register", "PUT");
    expectRoutingException("/accounts?action=register", "DELETE");
    expectRoutingException("/accounts/1?action=register", "POST");

    expectRoutingException("/statuses/1/replies/1,2,3?action=replyToAll", "GET");
    expectRoutingException("/statuses/1/replies?action=replyToAll", "DELETE");
    expectRoutingException("/statuses/1/replies?action=bogusAction", "POST");
    expectRoutingException("/statuses/1?action=search", "POST");

    expectRoutingException("/statuses/1/location?action=new_status_from_location", "GET");
    expectRoutingException("/statuses/1/location?action=bogusAction", "POST");
  }

  private void checkResult(String uri,
                           String httpMethod,
                           String restliMethod,
                           ResourceMethod method,
                           Class<?> resourceClass,
                           String methodName,
                           boolean hasBatchKeys,
                           String... expectedPathKeys)
          throws URISyntaxException
  {

    RestRequestBuilder builder = new RestRequestBuilder(new URI(uri)).setMethod(httpMethod);
    if (restliMethod != null)
    {
      builder.setHeader("X-RestLi-Method", restliMethod);
    }
    RestRequest request = builder.build();
    RoutingResult result = _router.process(request, new RequestContext());

    assertEquals(result.getResourceMethod().getType(), method);
    assertEquals(result.getResourceMethod().getResourceModel().getResourceClass(), resourceClass);
    assertEquals(result.getResourceMethod().getMethod().getName(), methodName);
    // If hasBatchKeys, there are batch keys in the context, and if not, there are none.
    assertTrue(!(hasBatchKeys ^ result.getContext().getPathKeys().getBatchKeys().size() > 0));

    for (String pathKey : expectedPathKeys)
    {
      assertNotNull(result.getContext().getPathKeys().get(pathKey));
    }
  }

  private void checkResult(String uri,
                           String httpMethod,
                           ResourceMethod method,
                           Class<?> resourceClass,
                           String methodName,
                           boolean hasBatchKeys,
                           String... expectedPathKeys)
      throws URISyntaxException
  {
    checkResult(uri,
                httpMethod,
                null,
                method,
                resourceClass,
                methodName,
                hasBatchKeys,
                expectedPathKeys
                );
  }

  private void checkBatchKeys(String uri,
                           String httpMethod,
                           Set<?> batchCompoundKeys)
      throws URISyntaxException
  {
    RestRequest request = new RestRequestBuilder(new URI(uri)).setMethod(httpMethod).build();
    RoutingResult result = _router.process(request, new RequestContext());
    assertEquals(result.getContext().getPathKeys().getBatchKeys(), batchCompoundKeys);
  }

  private void expectRoutingException(String uri,
                                      String httpMethod,
                                      String restliMethod) throws URISyntaxException
  {
    RestRequestBuilder builder = new RestRequestBuilder(new URI(uri)).setMethod(httpMethod);
    if (restliMethod != null)
    {
      builder.setHeader("X-RestLi-Method", restliMethod);
    }
    RestRequest request = builder.build();
    try
    {
      RoutingResult r = _router.process(request, new RequestContext());
      fail("Expected RoutingException, got: " + r.toString());
    }
    catch (RoutingException e)
    {
      // expected
//      System.out.println(e.getMessage() + ": " + e.getStatus());
    }
  }

  private void expectRoutingException(String uri,
                                      String httpMethod) throws URISyntaxException
  {
    expectRoutingException(uri, httpMethod, null);
  }

  /**
   * Tests routing on a more complicated resource hierarchy
   */
  @Test
  public void testRoutingComplex() throws Exception
  {
    // TODO Need new domain for testing
  }

  @Test
  public void testDefaultPathKeyUniqueness() throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(CombinedResources.CombinedCollectionWithSubresources.class,
                          CombinedResources.SubCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap);

    RestRequest request;
    RoutingResult result;

    // #1 simple GET
    request = new RestRequestBuilder(new URI("/test/foo/sub/bar")).setMethod("GET").build();

    result = _router.process(request, new RequestContext());
    assertNotNull(result);
    PathKeys keys = result.getContext().getPathKeys();
    assertEquals(keys.getAsString("testId"), "foo");
    assertEquals(keys.getAsString("subId"), "bar");
  }
}
