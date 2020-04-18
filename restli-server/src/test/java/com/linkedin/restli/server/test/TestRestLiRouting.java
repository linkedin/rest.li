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


import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.RestLiRouter;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.combined.CombinedResources;
import com.linkedin.restli.server.twitter.CustomStatusCollectionResource;
import com.linkedin.restli.server.twitter.DiscoveredItemsResource;
import com.linkedin.restli.server.twitter.FollowsAssociativeResource;
import com.linkedin.restli.server.twitter.LocationResource;
import com.linkedin.restli.server.twitter.RepliesCollectionResource;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TrendRegionsCollectionResource;
import com.linkedin.restli.server.twitter.TrendingResource;
import com.linkedin.restli.server.twitter.TwitterAccountsResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Trending;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModels;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * @author dellamag
 */
public class TestRestLiRouting
{
  private RestLiRouter _router;

  @DataProvider(name =  TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsCollectionEntity")
  public Object[][] routingDetailsCollectionEntity()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsCollectionEntity")
  public void testRoutingDetailsCollectionGet(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(StatusCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    // #1 simple GET
    RestRequest request = createRequest(uri, "GET", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.GET);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), StatusCollectionResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "get");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {Long.class});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "statuses");

    PathKeys keys = context.getPathKeys();
    assertEquals(keys.getAsLong("statusID"), new Long(1));
    assertNull(keys.getAsString("foo"));
  }

  @DataProvider(name =  TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsAssociationEntity")
  public Object[][] routingDetailsAssociationEntity()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/follows/followerID=1&followeeID=2" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/follows/(followerID:1,followeeID:2)" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsAssociationEntity")
  public void testRoutingDetailsAssociationGet(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(FollowsAssociativeResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "GET", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.GET);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), FollowsAssociativeResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "get");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {CompoundKey.class});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "follows");

    PathKeys keys = context.getPathKeys();
    assertEquals(keys.getAsLong("followerID"), new Long(1L));
    assertEquals(keys.getAsLong("followeeID"), new Long(2L));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsCollectionEntity")
  public void testRoutingDetailsCollectionUpdate(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(StatusCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "PUT", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.UPDATE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), StatusCollectionResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "update");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] { Long.class, Status.class });
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "statuses");

    PathKeys keys = context.getPathKeys();
    assertEquals(keys.getAsLong("statusID"), new Long(1));
    assertNull(keys.getAsString("foo"));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsCollectionEntity")
  public void testRoutingDetailsCollectionDelete(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(StatusCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "DELETE", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.DELETE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), StatusCollectionResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "delete");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] { Long.class });
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "statuses");

    PathKeys keys = context.getPathKeys();
    assertEquals(keys.getAsLong("statusID"), new Long(1));
    assertNull(keys.getAsString("foo"));
  }

  @DataProvider(name =  TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsAssociationBatch")
  public Object[][] routingDetailsAssociationBatch()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/follows?ids=followerID%3D1%26followeeID%3D2&ids=followerID%3D3%26followeeID%3D4" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/follows?ids=List((followerID:1,followeeID:2),(followerID:3,followeeID:4))" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsAssociationBatch")
  public void testRoutingDetailsAssociationBatchGet(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(FollowsAssociativeResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "GET", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.BATCH_GET);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), FollowsAssociativeResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "batchGet");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {Set.class});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "follows");

    PathKeys keys = context.getPathKeys();
    assertNull(keys.getAsString("followerID"));
    assertNull(keys.getAsString("followeeID"));

    CompoundKey key1 = new CompoundKey();
    key1.append("followerID", 1L);
    key1.append("followeeID", 2L);
    CompoundKey key2 = new CompoundKey();
    key2.append("followerID", 3L);
    key2.append("followeeID", 4L);
    Set<CompoundKey> expectedBatchKeys = new HashSet<>();
    expectedBatchKeys.add(key1);
    expectedBatchKeys.add(key2);

    assertEquals(keys.getBatchIds().size(), 2);
    for (CompoundKey batchKey : keys.<CompoundKey>getBatchIds())
    {
      assertTrue(expectedBatchKeys.contains(batchKey));
      expectedBatchKeys.remove(batchKey);
    }
    assertEquals(expectedBatchKeys.size(), 0);
  }

  @DataProvider(name =  TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsSimple")
  public Object[][] routingDetailsSimple()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/trending" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsSimple")
  public void testRoutingDetailsSimpleGet(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(TrendingResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "GET", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.GET);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), TrendingResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "get");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "trending");

    PathKeys keys = context.getPathKeys();
    assertNull(keys.getBatchIds());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsSimple")
  public void testRoutingDetailsSimpleUpdate(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(TrendingResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "PUT", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.UPDATE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), TrendingResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "update");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] { Trending.class });
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "trending");

    PathKeys keys = context.getPathKeys();
    assertNull(keys.getBatchIds());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsSimple")
  public void testRoutingDetailsSimplePartialUpdate(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(TrendingResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "POST", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.PARTIAL_UPDATE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), TrendingResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "update");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] { PatchRequest.class });
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "trending");

    PathKeys keys = context.getPathKeys();
    assertNull(keys.getBatchIds());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsSimple")
  public void testRoutingDetailsSimpleDelete(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(TrendingResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "DELETE", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());

    ResourceMethodDescriptor resourceMethodDescriptor = _router.process(context);
    assertNotNull(resourceMethodDescriptor);
    assertEquals(resourceMethodDescriptor.getType(), ResourceMethod.DELETE);
    assertNull(resourceMethodDescriptor.getActionName());
    assertNull(resourceMethodDescriptor.getFinderName());
    assertEquals(resourceMethodDescriptor.getMethod().getDeclaringClass(), TrendingResource.class);
    assertEquals(resourceMethodDescriptor.getMethod().getName(), "delete");
    assertEquals(resourceMethodDescriptor.getMethod().getParameterTypes(), new Class<?>[] {});
    assertEquals(resourceMethodDescriptor.getResourceModel().getName(), "trending");

    PathKeys keys = context.getPathKeys();
    assertNull(keys.getBatchIds());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingCollectionBatch")
  public Object[][] routingCollectionBatch()
  {
    return new Object[][]
      {
        {
          "/statuses?ids=1&ids=2&ids=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses?ids=List(1,2,3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses?ids=1&ids=2&ids=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "BATCH_GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses?ids=List(1,2,3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "BATCH_GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "BATCH_UPDATE",
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "BATCH_UPDATE",
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "BATCH_PARTIAL_UPDATE",
          ResourceMethod.BATCH_PARTIAL_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "BATCH_PARTIAL_UPDATE",
          ResourceMethod.BATCH_PARTIAL_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "BATCH_DELETE",
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "BATCH_DELETE",
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingCollectionBatch")
  public void testRoutingCollectionBatch(String uri,
                                         ProtocolVersion version,
                                         String httpMethod,
                                         String restliMethod,
                                         ResourceMethod method,
                                         String methodName,
                                         Set<Long> keys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, StatusCollectionResource.class, methodName, true);
    checkBatchKeys(uri, version, httpMethod, keys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingAltBatch")
  public Object[][] routingAltBatch()
  {
    return new Object[][]
      {
        {
          "/statuses?ids=Alt1&ids=Alt2&ids=Alt3&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses?ids=Alt1&ids=badAlt2&ids=Alt3&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 3L)) // second key should log an error.
        },
        {
          "/statuses?ids=List(Alt1,Alt2,Alt3)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses?ids=List(Alt1,badAlt2,Alt3)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 3L)) // second key should log an error.
        },
        {
          "/statuses?ids=Alt1&ids=Alt2&ids=Alt3&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "BATCH_GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses?ids=Alt1&ids=badAlt2&ids=Alt3&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "BATCH_GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 3L)) // second key should log an error
        },
        {
          "/statuses?ids=List(Alt1,Alt2,Alt3)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "BATCH_GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses?ids=List(Alt1,badAlt2,Alt3)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "BATCH_GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 3L)) // second key should log an error
        },
        {
          "/statuses?ids=Alt1&ids=Alt2&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(Alt1,Alt2)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=Alt1&ids=Alt2&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "BATCH_UPDATE",
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(Alt1,Alt2)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "BATCH_UPDATE",
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=Alt1&ids=Alt2&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "BATCH_PARTIAL_UPDATE",
          ResourceMethod.BATCH_PARTIAL_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(Alt1,Alt2)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "BATCH_PARTIAL_UPDATE",
          ResourceMethod.BATCH_PARTIAL_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=Alt1&ids=Alt2&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(Alt1,Alt2)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=Alt1&ids=Alt2&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "BATCH_DELETE",
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(1L, 2L))
        },
        {
          "/statuses?ids=List(Alt1,Alt2)&altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "BATCH_DELETE",
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(1L, 2L))
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingAltBatch")
  public void testRoutingAltBatch(String uri,
                                  ProtocolVersion version,
                                  String httpMethod,
                                  String restliMethod,
                                  ResourceMethod method,
                                  String methodName,
                                  Set<Long> expectedKeys) throws Exception
  {

    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(StatusCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, StatusCollectionResource.class, methodName, true);
    checkBatchKeys(uri, version, httpMethod, expectedKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingSubResourceCollectionBatch")
  public Object[][] routingSubResourceCollectionBatch()
  {
    return new Object[][]
      {
        {
          "/statuses/1/replies?ids=1&ids=2&ids=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        },
        {
          "/statuses/1/replies?ids=List(1,2,3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(1L, 2L, 3L))
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingSubResourceCollectionBatch")
  public void testRoutingSubResourceCollectionBatch(String uri,
                                                    ProtocolVersion version,
                                                    String httpMethod, String restliMethod,
                                                    ResourceMethod method,
                                                    String methodName,
                                                    Set<Long> keys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          RepliesCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, RepliesCollectionResource.class, methodName, true);
    checkBatchKeys(uri, version, httpMethod, keys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingSimpleSubResourceBatch")
  public Object[][] routingSimpleSubResourceBatch()
  {
    return new Object[][]
      {
        {
          "/trending/trendRegions?ids=1&ids=2&ids=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList("1", "2", "3"))
        },
        {
          "/trending/trendRegions?ids=List(1,2,3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList("1", "2", "3"))
        },
        {
          "/trending/trendRegions?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "BATCH_UPDATE",
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList("1", "2"))
        },
        {
          "/trending/trendRegions?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "BATCH_UPDATE",
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList("1", "2"))
        },
        {
          "/trending/trendRegions?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "BATCH_PARTIAL_UPDATE",
          ResourceMethod.BATCH_PARTIAL_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList("1", "2"))
        },
        {
          "/trending/trendRegions?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "BATCH_PARTIAL_UPDATE",
          ResourceMethod.BATCH_PARTIAL_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList("1", "2"))
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingSimpleSubResourceBatch")
  public void testRoutingSimbleSubResourceBatch(String uri,
                                                ProtocolVersion version,
                                                String httpMethod, String restliMethod,
                                                ResourceMethod method,
                                                String methodName,
                                                Set<String> keys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(TrendingResource.class,
                          TrendRegionsCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, TrendRegionsCollectionResource.class, methodName, true);
    checkBatchKeys(uri, version, httpMethod, keys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingAssociationResourceBatch")
  public Object[][] routingAssociationResourceBatch()
  {

    CompoundKey key1 = new CompoundKey();
    key1.append("followeeID", 1L);
    key1.append("followerID", 1L);

    CompoundKey key2 = new CompoundKey();
    key2.append("followeeID", 3L);
    key2.append("followerID", 1L);

    CompoundKey key3 = new CompoundKey();
    key3.append("followeeID", 2L);
    key3.append("followerID", 1L);

    // for reference.
    CompoundKey key2Mismatch = new CompoundKey();
    key2Mismatch.append("followeeID", 3L);
    key2Mismatch.append("followerID", 1L);
    key2Mismatch.append("badKey", 5L);

    CompoundKey key2Partial = new CompoundKey();
    key2Partial.append("followeeID", 3L);

    return new Object[][]
      {
        { "/follows?ids=followerID:1;followeeID:1&ids=followerID:1;followeeID:3&ids=followerID:1;followeeID:2", // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(key1, key2, key3))
        },
        { "/follows?ids=followerID%3D1%26followeeID%3D1&ids=followerID%3D1%26followeeID%3D3&ids=followerID%3D1%26followeeID%3D2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(key1, key2, key3))
        },
        { "/follows?ids=List((followerID:1,followeeID:1),(followerID:1,followeeID:3),(followerID:1,followeeID:2))",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(key1, key2, key3))
        },
        { "/follows?ids=followerID:1;followeeID:1&ids=followerID:1;followeeID:3;badKey:5&ids=followerID:1;followeeID:2", // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(key1, key3)) // second key should log an error.
        },
        { "/follows?ids=followerID%3D1%26followeeID%3D1&ids=followerID%3D1%26followeeID%3D3%26badKey%3D5&ids=followerID%3D1%26followeeID%3D2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(key1, key3)) // second key should log an error
        },
        { "/follows?ids=List((followerID:1,followeeID:1),(followerID:1,followeeID:3,badKey:5),(followerID:1,followeeID:2))",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(key1, key3)) // second key should log an error
        },
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingAssociationResourceBatch")
  public void testRoutingAssociationBatch(String uri,
                                          ProtocolVersion version,
                                          String httpMethod,
                                          String restliMethod,
                                          ResourceMethod method,
                                          String methodName,
                                          Set<CompoundKey> compoundKeys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(FollowsAssociativeResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, FollowsAssociativeResource.class, methodName, true);
    checkBatchKeys(uri, version, httpMethod, compoundKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingComplexKeyBatch")
  @SuppressWarnings("unchecked")
  public Object[][] routingComplexKeyBatch()
  {
    TwitterTestDataModels.DiscoveredItemKey keyPart1 = new TwitterTestDataModels.DiscoveredItemKey().setUserId(1L).setType(2).setItemId(3L);
    TwitterTestDataModels.DiscoveredItemKey keyPart2 = new TwitterTestDataModels.DiscoveredItemKey().setUserId(4L).setType(5).setItemId(6L);

    TwitterTestDataModels.DiscoveredItemKeyParams emptyParams = new TwitterTestDataModels.DiscoveredItemKeyParams();

    ComplexResourceKey<TwitterTestDataModels.DiscoveredItemKey, TwitterTestDataModels.DiscoveredItemKeyParams> complexKey1 =
      new ComplexResourceKey<>(keyPart1, emptyParams);
    ComplexResourceKey<TwitterTestDataModels.DiscoveredItemKey, TwitterTestDataModels.DiscoveredItemKeyParams> complexKey2 =
      new ComplexResourceKey<>(keyPart2, emptyParams);

    return new Object[][]
      {
        {
          "/discovereditems?ids%5B0%5D.userId=1&ids%5B0%5D.type=2&ids%5B0%5D.itemId=3&ids%5B1%5D.userId=4&ids%5B1%5D.type=5&ids%5B1%5D.itemId=6",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "BATCH_GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(complexKey1, complexKey2))
        },
        {
          "/discovereditems?ids=List((userId:1,type:2,itemId:3),(userId:4,type:5,itemId:6))",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "BATCH_GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          new HashSet<>(Arrays.asList(complexKey1, complexKey2))
        },
        {
          "/discovereditems?ids%5B0%5D.userId=1&ids%5B0%5D.type=2&ids%5B0%5D.itemId=3&ids%5B1%5D.userId=4&ids%5B1%5D.type=5&ids%5B1%5D.itemId=6",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "BATCH_UPDATE",
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(complexKey1, complexKey2))
        },
        {
          "/discovereditems?ids=List((userId:1,type:2,itemId:3),(userId:4,type:5,itemId:6))",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "BATCH_UPDATE",
          ResourceMethod.BATCH_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(complexKey1, complexKey2))
        },
        {
          "/discovereditems?ids%5B0%5D.userId=1&ids%5B0%5D.type=2&ids%5B0%5D.itemId=3&ids%5B1%5D.userId=4&ids%5B1%5D.type=5&ids%5B1%5D.itemId=6",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "BATCH_PARTIAL_UPDATE",
          ResourceMethod.BATCH_PARTIAL_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(complexKey1, complexKey2))
        },
        {
          "/discovereditems?ids=List((userId:1,type:2,itemId:3),(userId:4,type:5,itemId:6))",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "BATCH_PARTIAL_UPDATE",
          ResourceMethod.BATCH_PARTIAL_UPDATE,
          "batchUpdate",
          new HashSet<>(Arrays.asList(complexKey1, complexKey2))
        },
        {
          "/discovereditems?ids%5B0%5D.userId=1&ids%5B0%5D.type=2&ids%5B0%5D.itemId=3&ids%5B1%5D.userId=4&ids%5B1%5D.type=5&ids%5B1%5D.itemId=6",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "BATCH_DELETE",
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(complexKey1, complexKey2))
        },
        {
          "/discovereditems?ids=List((userId:1,type:2,itemId:3),(userId:4,type:5,itemId:6))",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "BATCH_DELETE",
          ResourceMethod.BATCH_DELETE,
          "batchDelete",
          new HashSet<>(Arrays.asList(complexKey1, complexKey2))
        },
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingComplexKeyBatch")
  public void testRoutingComplexKeyBatch(String uri,
                                         ProtocolVersion version,
                                         String httpMethod,
                                         String restliMethod,
                                         ResourceMethod method,
                                         String methodName,
                                         Set<ComplexResourceKey<TwitterTestDataModels.DiscoveredItemKey,
                                                                TwitterTestDataModels.DiscoveredItemKeyParams>> compoundKeys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(DiscoveredItemsResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, DiscoveredItemsResource.class, methodName, true);
    checkBatchKeys(uri, version, httpMethod, compoundKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingCollection")
  public Object[][] routingCollection()
  {
    String[] statusKey = new String[] { "statusID" };

    return new Object[][]
      {
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "GET",
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "GET",
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/st%61tuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/st%61tuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/%31",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/%31",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/-1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
           null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/-1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
           null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.CREATE,
          "create",
          new String[0]
        },
        { "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.CREATE,
          "create",
          new String[0]
        },
        { "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "CREATE",
          ResourceMethod.CREATE,
          "create",
          new String[0]
        },
        { "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "CREATE",
          ResourceMethod.CREATE,
          "create",
          new String[0]
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.UPDATE,
          "update",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.UPDATE,
          "update",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "UPDATE",
          ResourceMethod.UPDATE,
          "update",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "UPDATE",
          ResourceMethod.UPDATE,
          "update",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "PARTIAL_UPDATE",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "PARTIAL_UPDATE",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.DELETE,
          "delete",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.DELETE,
          "delete",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "DELETE",
          ResourceMethod.DELETE,
          "delete",
          statusKey
        },
        { "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "DELETE",
          ResourceMethod.DELETE,
          "delete",
          statusKey
        },
        { "/statuses?q=search",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "search",
          new String[0]
        },
        { "/statuses?q=search",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "search",
          new String[0]
        },
        { "/statuses?q=search",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "FINDER",
          ResourceMethod.FINDER,
          "search",
          new String[0]
        },
        { "/statuses?q=search",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "FINDER",
          ResourceMethod.FINDER,
          "search",
          new String[0]
        },
        { "/statuses?q=search&keywords=linkedin",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "search",
          new String[0]
        },
        { "/statuses?q=search&keywords=linkedin",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "search",
          new String[0]
        },
        { "/statuses?q=user_timeline",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "getUserTimeline",
          new String[0]
        },
        { "/statuses?q=user_timeline",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "getUserTimeline",
          new String[0]
        },
        { "/statuses?q=public_timeline",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "getPublicTimeline",
          new String[0]
        },
        { "/statuses?q=public_timeline",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "getPublicTimeline",
          new String[0]
        },
        { "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "BATCH_CREATE",
          ResourceMethod.BATCH_CREATE,
          "batchCreate",
          new String[0]
        },
        { "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "BATCH_CREATE",
          ResourceMethod.BATCH_CREATE,
          "batchCreate",
          new String[0]
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingCollection")
  public void testRoutingCollection(String uri, ProtocolVersion version, String httpMethod, String restliMethod, ResourceMethod method, String methodName, String[] pathKeys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, StatusCollectionResource.class, methodName, false, pathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingAltCollection")
  public Object[][] routingAltCollection()
  {
    String[] statusKey = new String[] { "statusID" };

    return new Object[][]
      {
        {
          "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "GET",
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "GET",
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/st%61tuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/st%61tuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/Alt%31?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/Alt%31?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/Alt-1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/Alt-1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.UPDATE,
          "update",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.UPDATE,
          "update",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "UPDATE",
          ResourceMethod.UPDATE,
          "update",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "UPDATE",
          ResourceMethod.UPDATE,
          "update",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "PARTIAL_UPDATE",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "PARTIAL_UPDATE",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.DELETE,
          "delete",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.DELETE,
          "delete",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "DELETE",
          ResourceMethod.DELETE,
          "delete",
          statusKey
        },
        { "/statuses/Alt1?altkey=alt",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "DELETE",
          ResourceMethod.DELETE,
          "delete",
          statusKey
        },
        {
          "/statuses/Alt1?altkey=alt&action=forward&to=5",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.ACTION,
          "forward",
          statusKey
        },
        {
          "/statuses/Alt1?altkey=alt&action=forward&to=5",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.ACTION,
          "forward",
          statusKey
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingAltCollection")
  public void testRoutingAltKeyCollection(String uri,
                                          ProtocolVersion version,
                                          String httpMethod,
                                          String restliMethod,
                                          ResourceMethod method,
                                          String methodName,
                                          String[] pathKeys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, StatusCollectionResource.class, methodName, false, pathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingCollectionSubResource")
  public Object[][] routingCollectionSubResource()
  {
    return new Object[][]
      {
        {
          "/statuses/1/replies",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.CREATE,
          RepliesCollectionResource.class,
          "create"
        },
        {
          "/statuses/1/replies",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.CREATE,
          RepliesCollectionResource.class,
          "create"
        },
        {
          "/statuses/1/location",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          LocationResource.class,
          "get"
        },
        {
          "/statuses/1/location",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          LocationResource.class,
          "get"
        },
        {
          "/statuses/1/location",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          ResourceMethod.UPDATE,
          LocationResource.class,
          "update"
        },
        {
          "/statuses/1/location",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          ResourceMethod.UPDATE,
          LocationResource.class,
          "update"
        },
        {
          "/statuses/1/location",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          ResourceMethod.DELETE,
          LocationResource.class,
          "delete"
        },
        {
          "/statuses/1/location",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          ResourceMethod.DELETE,
          LocationResource.class,
          "delete"
        },
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingCollectionSubResource")
  public void testRoutingCollectionSubResource(String uri,
                                               ProtocolVersion version,
                                               String httpMethod,
                                               ResourceMethod method,
                                               Class<?> resourceClass,
                                               String methodName) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          RepliesCollectionResource.class,
                          LocationResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, method, resourceClass, methodName, false, "statusID");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingAssociation")
  public Object[][] routingAssociation()
  {
    String[] assocPathKeys = new String[] { "followerID", "followeeID" };

    return new Object[][]
      {
        {
          "/follows/followerID:1;followeeID:1", // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          assocPathKeys
        },
        {
          "/follows/followerID=1&followeeID=1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          assocPathKeys
        },
        {
          "/follows/(followerID:1,followeeID:1)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          assocPathKeys
        },
        { "/follows/followerID:1;followeeID:1", // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          ResourceMethod.UPDATE,
          "update",
          assocPathKeys
        },
        { "/follows/followerID=1&followeeID=1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          ResourceMethod.UPDATE,
          "update",
          assocPathKeys
        },
        { "/follows/(followerID:1,followeeID:1)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          ResourceMethod.UPDATE,
          "update",
          assocPathKeys
        },
        { "/follows/followerID:1;followeeID:1", // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          assocPathKeys
        },
        { "/follows/followerID=1&followeeID=1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          assocPathKeys
        },
        { "/follows/(followerID:1,followeeID:1)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          assocPathKeys
        },
        { "/follows?q=friends&userID=1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getFriends",
          new String[0]
        },
        { "/follows?q=friends&userID=1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getFriends",
          new String[0]
        },
        { "/follows?q=followers&userID=1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getFollowers",
          new String[0]
        },
        { "/follows?q=followers&userID=1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getFollowers",
          new String[0]
        },
        { "/follows/followerID:1?q=other&someParam=value", // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getOther",
          new String[] { "followerID" }
        },
        { "/follows/followerID=1?q=other&someParam=value",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getOther",
          new String[] { "followerID" }
        },
        { "/follows/(followerID:1)?q=other&someParam=value",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getOther",
          new String[] { "followerID" }
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingAssociation")
  public void testRoutingAssociation(String uri, ProtocolVersion version, String httpMethod, ResourceMethod method, String methodName, String[] pathKeys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(FollowsAssociativeResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, method, FollowsAssociativeResource.class, methodName, false, pathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingComplexKey")
  public Object[][] routingComplexKey()
  {
    return new Object[][]
      {
        {
          "/discovereditems/userId=1&type=2&itemId=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          true
        },
        {
          "/discovereditems/(userId:1,type:2,itemId:3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get",
          true
        },
        {
          "/discovereditems/",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.CREATE,
          "create",
          false
        },
        {
          "/discovereditems/",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.CREATE,
          "create",
          false
        },
        {
          "/discovereditems/",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "BATCH_CREATE",
          ResourceMethod.BATCH_CREATE,
          "batchCreate",
          false
        },
        {
          "/discovereditems/",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "BATCH_CREATE",
          ResourceMethod.BATCH_CREATE,
          "batchCreate",
          false
        },
        {
          "/discovereditems/userId=1&type=2&itemId=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.UPDATE,
          "update",
          true
        },
        {
          "/discovereditems/(userId:1,type:2,itemId:3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.UPDATE,
          "update",
          true
        },
        {
          "/discovereditems/userId=1&type=2&itemId=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          true
        },
        {
          "/discovereditems/(userId:1,type:2,itemId:3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          true
        },
        {
          "/discovereditems/userId=1&type=2&itemId=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.DELETE,
          "delete",
          true
        },
        {
          "/discovereditems/(userId:1,type:2,itemId:3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.DELETE,
          "delete",
          true
        },
        {
          "/discovereditems?q=user&userId=1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "findByUser",
          false
        },
        {
          "/discovereditems?q=user&userId=1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.FINDER,
          "findByUser",
          false
        },
        {
          "/discovereditems?action=purge&user=1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.ACTION,
          "purge",
          false
        },
        {
          "/discovereditems?action=purge&user=1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          null,
          ResourceMethod.ACTION,
          "purge",
          false
        },
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingComplexKey")
  public void testRoutingComplexKey(String uri,
                                    ProtocolVersion version,
                                    String httpMethod,
                                    String restliMethod,
                                    ResourceMethod method,
                                    String methodName,
                                    boolean hasKeys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(DiscoveredItemsResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, restliMethod, method, DiscoveredItemsResource.class, methodName, false, hasKeys? new String[]{"discoveredItemId"} : new String[0]);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingSimpleResource")
  public Object[][] routingSimpleResource() throws Exception
  {
    return new Object[][]
      {
        {
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get"
        },
        {
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          null,
          ResourceMethod.GET,
          "get"
        },
        {
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.UPDATE,
          "update"
        },
        {
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          null,
          ResourceMethod.UPDATE,
          "update"
        },
        {
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.DELETE,
          "delete"
        },
        {
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          null,
          ResourceMethod.DELETE,
          "delete"
        },
        {
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "PARTIAL_UPDATE",
          ResourceMethod.PARTIAL_UPDATE,
          "update"
        },
        {
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "PARTIAL_UPDATE",
          ResourceMethod.PARTIAL_UPDATE,
          "update"
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingSimpleResource")
  public void testRoutingSimpleResource(ProtocolVersion version, String httpMethod, String restliMethod, ResourceMethod method, String methodName) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(TrendingResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult("/trending", version, httpMethod, restliMethod, method, TrendingResource.class, methodName, false);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingSubSimpleResource")
  public Object[][] routingSubSimpleResource() throws Exception
  {
    return new Object[][]
      {
        {
          "/trending/trendRegions/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          new String[] { "trendRegionId" }
        },
        {
          "/trending/trendRegions/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          new String[] { "trendRegionId" }
        },
        {
          "/trending/trendRegions/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          ResourceMethod.UPDATE,
          "update",
          new String[] { "trendRegionId" }
        },
        {
          "/trending/trendRegions/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          ResourceMethod.UPDATE,
          "update",
          new String[] { "trendRegionId" }
        },
        {
          "/trending/trendRegions/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          new String[] { "trendRegionId" }
        },
        {
          "/trending/trendRegions/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.PARTIAL_UPDATE,
          "update",
          new String[] { "trendRegionId" }
        },
        {
          "/trending/trendRegions",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.CREATE,
          "create",
          new String[0]
        },
        {
          "/trending/trendRegions",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          ResourceMethod.CREATE,
          "create",
          new String[0]
        },
        {
          "/trending/trendRegions/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          ResourceMethod.DELETE,
          "delete",
          new String[] { "trendRegionId" }
        },
        {
          "/trending/trendRegions/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          ResourceMethod.DELETE,
          "delete",
          new String[] { "trendRegionId" }
        },
        {
          "/trending/trendRegions?q=get_trending_by_popularity",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getTrendingByPopularity",
          new String[0]
        },
        {
          "/trending/trendRegions?q=get_trending_by_popularity",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "getTrendingByPopularity",
          new String[0]
        },
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingSubSimpleResource")
  public void testRoutingSubSimpleResource(String uri, ProtocolVersion version, String httpMethod, ResourceMethod method, String methodName, String[] pathKeys) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(TrendingResource.class,
                          TrendRegionsCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri, version, httpMethod, method, TrendRegionsCollectionResource.class, methodName, false, pathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "methodMismatch")
  public Object[][] methodMismatch()
  {
    return new Object[][]
      {
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "CREATE"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "CREATE"
        },
        {
          "/statuses?ids=1&ids=2&ids=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "CREATE"
        },
        {
          "/statuses?ids=List(1,2,3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "CREATE"
        },
        {
          "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "GET"
        },
        {
          "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "GET"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "CREATE"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "CREATE"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "CREATE"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "CREATE"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "CREATE"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "CREATE"
        },
        {
          "/statuses?q=search",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "CREATE"
        },
        {
          "/statuses?q=search",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "CREATE"
        },
        {
          "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "GET"
        },
        {
          "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "GET"
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "CREATE"
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "CREATE"
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "CREATE"
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "CREATE"
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "CREATE"
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "CREATE"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "FOO"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "FOO"
        },
        {
          "/statuses?ids=1,2,3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "FOO"
        },
        {
          "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "FOO"
        },
        {
          "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "FOO"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "FOO"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "FOO"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "FOO"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "FOO"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "FOO"
        },
        {
          "/statuses/1",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "FOO"
        },
        {
          "/statuses?q=search",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          "FOO"
        },
        {
          "/statuses?q=search",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          "FOO"
        },
        {
          "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "FOO"
        },
        {
          "/statuses",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "FOO"
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "PUT",
          "FOO"
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "PUT",
          "FOO"
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "POST",
          "FOO"
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "POST",
          "FOO"
        },
        {
          "/statuses?ids=1&ids=2",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "DELETE",
          "FOO"
        },
        {
          "/statuses?ids=List(1,2)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "DELETE",
          "FOO"
        },
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "methodMismatch")
  public void testRestLiMethodMismatch(String uri, ProtocolVersion version, String httpMethod, String restliMethod) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          FollowsAssociativeResource.class,
                          RepliesCollectionResource.class);

    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    expectRoutingExceptionWithStatus(uri, version, httpMethod, restliMethod, HttpStatus.S_400_BAD_REQUEST);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "invalidList")
  public Object[][] invalidList()
  {
    return new Object[][]
        {
            {
                "/statuses?ids=1&ids=2&ids=3",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "GET",
                "CREATE"
            },
            {
                "/statuses?ids=1",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "GET",
                "CREATE"
            },
            {
                "/statuses?ids=1&ids=2",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "PUT",
                "CREATE"
            },
            {
                "/statuses?ids=1",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "PUT",
                "CREATE"
            },
            {
                "/statuses?ids=1&ids=2",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "POST",
                "CREATE"
            },
            {
                "/statuses?ids=1",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "POST",
                "CREATE"
            },
            {
                "/statuses?ids=1&ids=2",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "DELETE",
                "CREATE"
            },
            {
                "/statuses?ids=1",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "DELETE",
                "CREATE"
            },
            {
                "/statuses?ids=List(NONE)",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "DELETE",
                "CREATE"
            },
            {
                "/statuses?ids=List(1,2,3,NONE)",
                AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
                "DELETE",
                "CREATE"
            },
        };
  }


  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "invalidList")
  public void testRestliInvalidList(String uri, ProtocolVersion version, String httpMethod, String restliMethod) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
        buildResourceModels(StatusCollectionResource.class,
            FollowsAssociativeResource.class,
            RepliesCollectionResource.class);

    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    expectRoutingExceptionWithStatus(uri, version, httpMethod, restliMethod, HttpStatus.S_400_BAD_REQUEST);
  }



    @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "nKeyAssociationRouting")
  public Object[][] nKeyAssociationRouting()
  {
    return new Object[][]
      {
        {
          "/test/foo:1;bar:2;baz:3", // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          new String[] {"foo", "bar", "baz"}
        },
        {
          "/test/foo=1&bar=2&baz=3",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          new String[] {"foo", "bar", "baz"}
        },
        {
          "/test/(foo:1,bar:2,baz:3)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET ,
          "get",
          new String[] {"foo", "bar", "baz"}
        },
        {
          "/test/foo:%3A;bar:%3B;baz:%3D", // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          new String[] {"foo", "bar", "baz"}
        },
        {
          "/test/foo=%3A&bar=%3B&baz=%3D",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          new String[] {"foo", "bar", "baz"}
        },
        {
          "/test/(foo:%3A,bar:%3B,baz:%3D)",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.GET,
          "get",
          new String[] {"foo", "bar", "baz"}
        },
        {
          "/test/foo=1&bar=2?q=find",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "find",
          new String[] {"foo", "bar"}
        },
        {
          "test/foo:1;bar:2?q=find",  // legacy
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "find",
          new String[] {"foo", "bar"}
        },
        {
          "/test/(foo:1,bar:2)?q=find",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.FINDER,
          "find",
          new String[] {"foo", "bar"}
        },
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "nKeyAssociationRouting")
  public void testNKeyAssociationRoutingBasicNonBatch(String uri,
                                                      ProtocolVersion version,
                                                      String httpMethod,
                                                      ResourceMethod method,
                                                      String methodName,
                                                      String[] expectedPathKeys)
    throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(
      CombinedResources.CombinedNKeyAssociationResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri,
                version,
                httpMethod,
                method,
                CombinedResources.CombinedNKeyAssociationResource.class,
                methodName,
                false,
                expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "nKeyAssociationRoutingBatch")
  public Object[][] nKeyAssociationRoutingBatch()
  {

    CompoundKey key1 = new CompoundKey();
    key1.append("foo", "1,1").append("bar", "1:2");
    CompoundKey key2 = new CompoundKey();
    key2.append("foo", "2,1").append("bar", "2;2");

    Set<CompoundKey> keys = new HashSet<>();
    keys.add(key1);
    keys.add(key2);

    return new Object[][]
      {
        {
          "/test?ids=bar%3D1%253A2%26foo%3D1%252C1&ids=bar%3D2%253B2%26foo%3D2%252C1",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          keys
        },
        {
          "/test?ids=List((bar:1%3A2,foo:1%2C1),(bar:2;2,foo:2%2C1))",
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "GET",
          ResourceMethod.BATCH_GET,
          "batchGet",
          keys
        },
      };
  }
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "nKeyAssociationRoutingBatch")
  public void testNKeyAssociationRoutingBasicBatch(String uri,
                                                      ProtocolVersion version,
                                                      String httpMethod,
                                                      ResourceMethod method,
                                                      String methodName,
                                                      Set<CompoundKey> batchKeys)
    throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(
      CombinedResources.CombinedNKeyAssociationResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    checkResult(uri,
                version,
                httpMethod,
                method,
                CombinedResources.CombinedNKeyAssociationResource.class,
                methodName,
                true);

    checkBatchKeys(uri, version, httpMethod, batchKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "actionRootRouting")
  public Object[][] actionRootRouting()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/accounts?action=register" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/accounts?action=register" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "actionRootRouting")
  public void testActionRootRouting(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(TwitterAccountsResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "POST", version);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    ResourceMethodDescriptor method = _router.process(context);

    assertNotNull(method);
    assertEquals(method.getActionName(), "register");
    assertEquals(method.getType(), ResourceMethod.ACTION);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "actionNestedRouting")
  public Object[][] actionNestedRouting()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/replies?action=replyToAll" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/replies?action=replyToAll" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "actionNestedRouting")
  public void testActionNestedRouting(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          RepliesCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "POST", version);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    ResourceMethodDescriptor method = _router.process(context);

    assertNotNull(method);
    assertEquals(method.getActionName(), "replyToAll");
    assertEquals(method.getType(), ResourceMethod.ACTION);
    assertEquals(context.getPathKeys().get("statusID"), Long.valueOf(1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "actionNestedSimpleRouting")
  public Object[][] actionNestedSimpleRouting()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/statuses/1/location?action=new_status_from_location" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/statuses/1/location?action=new_status_from_location" }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "actionNestedSimpleRouting")
  public void testActionNestedSimpleRouting(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(StatusCollectionResource.class,
                          LocationResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request = createRequest(uri, "POST", version);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    ResourceMethodDescriptor method = _router.process(context);

    assertNotNull(method);
    assertEquals(method.getActionName(), "new_status_from_location");
    assertEquals(method.getType(), ResourceMethod.ACTION);
    assertEquals(method.getMethod().getParameterTypes(), new Class<?>[] { String.class });
    assertEquals(context.getPathKeys().get("statusID"), Long.valueOf(1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingErrors")
  public Object[][] routingErrors() throws Exception
  {
    return new Object[][]
      {
        { "/", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_404_NOT_FOUND },
        { "/", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_404_NOT_FOUND },
        { "/", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_404_NOT_FOUND },
        { "/", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_404_NOT_FOUND },
        { "/", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_404_NOT_FOUND },
        { "/", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_404_NOT_FOUND },
        { "/replies", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/replies", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/location", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/location", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/trendRegions", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/trendRegions", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/asdfasf", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/asdfasf", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses%2F1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses%2F1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/asdf", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/asdf", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/replies/2", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies/2", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/replies", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/replies", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/2.3", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/2.3", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?ids=Alt1&ids=Alt2&ids=Alt3&altkey=notAlt", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?ids=List(Alt1,Alt2,Alt3)&altkey=notAlt", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?ids=Alt1&ids=Altfoo&ids=Alt3&altkey=alt", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_500_INTERNAL_SERVER_ERROR},
        { "/statuses?ids=List(Alt1,Altfoo,Alt3)&altkey=alt", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_500_INTERNAL_SERVER_ERROR},
        { "/statuses/1/replies", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/2", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/2", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/badpath", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/badpath", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/badpath/2", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/badpath/2", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies?q=wrong", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies?q=wrong", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/location/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/location/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/location/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/location/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/location/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/location/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/location/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/location/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_404_NOT_FOUND },
        { "/statuses/1/location?q=wrong", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/location?q=wrong", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/location?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/location?q=wrong&keywords=linkedin", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/custom_status/1234", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/custom_status/1234", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/custom_status/ids=1234&ids=12345", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/custom_status/List(1234,12345)", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/follows", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/follows", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/follows", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/follows", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/follows", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows?q=wrong", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows?q=wrong", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/followerID=1/bad_path", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/(followerID:1)/bad_path", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/followerID=1&followerID=2/bad_path", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/(followerID:1,followerID:2)/bad_path", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/followerID=1&wrongID=2", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/(followerID:1,wrongID:2)", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/follows/followerID=1&followerID=2", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST }, // delete not supported
        { "/follows/(followerID:1,followerID:2)", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_404_NOT_FOUND },
        { "/trending?q=abc", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/trending?q=abc", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/trending?q=def&param1=1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/trending?q=def&param1=1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/1/trendRegions/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1/trendRegions/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1/trendRegions/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1/trendRegions/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1/trendRegions/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1/trendRegions/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1/trendRegions/1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_404_NOT_FOUND },
        { "/trending/1/trendRegions/1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_404_NOT_FOUND },
        { "/trending/trendRegions", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions?q=abc", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions?q=abc", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions?q=def&param1=1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/trending/trendRegions?q=def&param1=1", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/bogusResource", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/bogusResource", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_404_NOT_FOUND },
        { "/accounts", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?q=register", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?q=register", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        // associations
        { "/test/foo:1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST }, // legacy
        { "/test/foo=1", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/test/(foo:1)", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/test/foo:1;bar:2;baz:3;qux:4", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST }, // legacy
        { "/test/foo=1&bar=2&baz=3&qux=4", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/test/(foo:1,bar:2,baz:3,qux:4)", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        // actions
        { "/accounts?action=bogusMethod", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=bogusMethod", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=register", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=register", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=register", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=register", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "PUT", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=register", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts?action=register", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts/1?action=register", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/accounts/1?action=register", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies/1,2,3?action=replyToAll", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies/1,2,3?action=replyToAll", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies?action=replyToAll", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies?action=replyToAll", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "DELETE", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies?action=bogusAction", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/replies?action=bogusAction", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1?action=search", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1?action=search", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/location?action=new_status_from_location", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/location?action=new_status_from_location", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "GET", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/location?action=bogusAction", AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST },
        { "/statuses/1/location?action=bogusAction", AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "POST", HttpStatus.S_400_BAD_REQUEST }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingErrors")
  public void testRoutingErrors(String uri, ProtocolVersion version, String httpMethod, HttpStatus status) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(CombinedResources.CombinedNKeyAssociationResource.class,
                          DiscoveredItemsResource.class,
                          FollowsAssociativeResource.class,
                          LocationResource.class,
                          RepliesCollectionResource.class,
                          StatusCollectionResource.class,
                          CustomStatusCollectionResource.class,
                          TrendRegionsCollectionResource.class,
                          TrendingResource.class,
                          TwitterAccountsResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    expectRoutingExceptionWithStatus(uri, version, httpMethod, null, status);
  }

  private void checkResult(String uri,
                           ProtocolVersion version,
                           String httpMethod,
                           String restliMethod,
                           ResourceMethod method,
                           Class<?> resourceClass,
                           String methodName,
                           boolean hasBatchKeys,
                           String... expectedPathKeys)
      throws URISyntaxException, RestLiSyntaxException
  {
    RestRequestBuilder builder = createRequestBuilder(uri, httpMethod, version);
    if (restliMethod != null)
    {
      builder.setHeader("X-RestLi-Method", restliMethod);
    }
    RestRequest request = builder.build();
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    ResourceMethodDescriptor methodDescriptor = _router.process(context);

    assertEquals(methodDescriptor.getType(), method);
    assertEquals(methodDescriptor.getResourceModel().getResourceClass(), resourceClass);
    assertEquals(methodDescriptor.getMethod().getName(), methodName);
    // If hasBatchKeys, there are batch keys in the context, and if not, there are none.
    assertEquals(hasBatchKeys, context.getPathKeys().getBatchIds() != null);

    for (String pathKey : expectedPathKeys)
    {
      assertNotNull(context.getPathKeys().get(pathKey));
    }
    if (method != null)
    {
      String expectedOperationName;
      switch (method)
      {
        case ACTION:
          expectedOperationName = "action:" + methodDescriptor.getActionName();
          break;
        case FINDER:
          expectedOperationName = "finder:" + methodDescriptor.getFinderName();
          break;
        case BATCH_FINDER:
          expectedOperationName = "batch_finder:" + methodDescriptor.getBatchFinderName();
          break;
        default:
          expectedOperationName = method.toString().toLowerCase();
      }
      assertEquals(context.getRawRequestContext().getLocalAttr(R2Constants.OPERATION),
                   expectedOperationName);
    }
  }

  private void checkResult(String uri,
                           ProtocolVersion version,
                           String httpMethod,
                           ResourceMethod method,
                           Class<?> resourceClass,
                           String methodName,
                           boolean hasBatchKeys,
                           String... expectedPathKeys)
      throws URISyntaxException, RestLiSyntaxException
  {
    checkResult(uri,
                version,
                httpMethod,
                null,
                method,
                resourceClass,
                methodName,
                hasBatchKeys,
                expectedPathKeys);
  }

  private void checkBatchKeys(String uri,
                              ProtocolVersion version,
                              String httpMethod,
                              Set<?> expectedBatchKeys)
      throws URISyntaxException, RestLiSyntaxException
  {
    RestRequest request = createRequest(uri, httpMethod, version);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    _router.process(context);
    Set<?> batchKeys = context.getPathKeys().getBatchIds();
    assertEquals(batchKeys, expectedBatchKeys);
  }

  private void expectRoutingExceptionWithStatus(String uri,
                                                ProtocolVersion version,
                                                String httpMethod,
                                                String restliMethod,
                                                HttpStatus status)
      throws URISyntaxException
  {
    RestRequestBuilder builder = createRequestBuilder(uri, httpMethod, version);
    if (restliMethod != null)
    {
      builder.setHeader("X-RestLi-Method", restliMethod);
    }
    RestRequest request = builder.build();
    try
    {
      try
      {
        ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
        ResourceMethodDescriptor method = _router.process(context);
        fail("Expected RoutingException, got: " + method.toString());
      }
      catch (RestLiSyntaxException e)
      {
        throw new RoutingException(e.getMessage(), HttpStatus.S_400_BAD_REQUEST.getCode());
      }
    }
    catch (RoutingException e)
    {
      // expected a certain httpStatus code
      assertEquals(e.getStatus(), status.getCode());
    }
  }

  private RestRequest createRequest(String uri, String method, ProtocolVersion version)
    throws URISyntaxException
  {
    return createRequestBuilder(uri, method, version).build();
  }

  private RestRequestBuilder createRequestBuilder(String uri, String method, ProtocolVersion version)
    throws URISyntaxException
  {
    return new RestRequestBuilder(new URI(uri)).setMethod(method)
                                               .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());
  }

  /**
   * Tests routing on a more complicated resource hierarchy
   */
  @Test
  public void testRoutingComplex() throws Exception
  {
    // TODO Need new domain for testing
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "uniqueness")
  public void testDefaultPathKeyUniqueness(ProtocolVersion version, String uri) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap =
      buildResourceModels(CombinedResources.CombinedCollectionWithSubresources.class,
                          CombinedResources.SubCollectionResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    RestRequest request;

    // #1 simple GET
    request = createRequest(uri, "GET", version);

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    ResourceMethodDescriptor method = _router.process(context);
    assertNotNull(method);
    PathKeys keys = context.getPathKeys();
    assertEquals(keys.getAsString("testId"), "foo");
    assertEquals(keys.getAsString("subId"), "bar");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "uniqueness")
  public Object[][] uniqueness()
  {
    return new Object[][]
      {
        { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/test/foo/sub/bar" },
        { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/test/foo/sub/bar" }
      };
  }

  @DataProvider(name =  TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsSimpleStreaming")
  public Object[][] routingDetailsSimpleStreaming()
  {
    return new Object[][]
        {
            //No response attachments allowed but request attachments are present
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/trending", "application/x-pson;q=1.0,application/json;q=0.9",
                new RestLiAttachmentReader(null) },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/x-pson;q=1.0,*/*;q=0.9",
                new RestLiAttachmentReader(null) },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8",
                new RestLiAttachmentReader(null) },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/x-pson",
                new RestLiAttachmentReader(null) },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending",
                null, new RestLiAttachmentReader(null) },

            //Response attachments allowed with a variety of different headers, but no request attachments present.
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/trending", "multipart/related;q=1.0,application/x-pson;q=0.9,application/json;q=0.8", null },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/x-pson;q=1.0,multipart/related;q=0.9,*/*;q=0.8", null },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8,multipart/related;q=0.7", null },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/x-pson,multipart/related", null },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "multipart/related", null },

            //Response attachments allowed with a variety of different headers as well as request attachments present.
            { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "/trending", "multipart/related;q=1.0,application/x-pson;q=0.9,application/json;q=0.8",
                new RestLiAttachmentReader(null) },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/x-pson;q=1.0,multipart/related;q=0.9,*/*;q=0.8",
                new RestLiAttachmentReader(null) },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8,multipart/related;q=0.7",
                new RestLiAttachmentReader(null) },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "application/x-pson,multipart/related",
                new RestLiAttachmentReader(null) },
            { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "/trending", "multipart/related",
                new RestLiAttachmentReader(null) },
        };
  }

  //This test verifies that the router can create the correct resource context based on attachments being
  //present in the request or an accept type indicating a desire to receive response attachments.
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "routingDetailsSimpleStreaming")
  public void testStreamingResourceContext(ProtocolVersion version, String uri, String acceptHeader,
                                           RestLiAttachmentReader requestAttachments) throws Exception
  {
    Map<String, ResourceModel> pathRootResourceMap = buildResourceModels(TrendingResource.class);
    _router = new RestLiRouter(pathRootResourceMap, new RestLiConfig());

    final RestRequestBuilder requestBuilder = new RestRequestBuilder(new URI(uri)).setMethod("GET")
        .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, version.toString());

    if (acceptHeader != null)
    {
      requestBuilder.setHeader(RestConstants.HEADER_ACCEPT, acceptHeader);
    }

    final RestRequest request = requestBuilder.build();

    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    context.setRequestAttachmentReader(requestAttachments);
    ResourceMethodDescriptor method = _router.process(context);

    assertNotNull(method);

    if (requestAttachments != null)
    {
      Assert.assertEquals(context.getRequestAttachmentReader(), requestAttachments);
    }
    else
    {
      Assert.assertNull(context.getRequestAttachmentReader());
    }

    if (acceptHeader != null && acceptHeader.contains("multipart/related"))
    {
      Assert.assertTrue(context.responseAttachmentsSupported());
    }
    else
    {
      Assert.assertFalse(context.responseAttachmentsSupported());
    }
  }
}