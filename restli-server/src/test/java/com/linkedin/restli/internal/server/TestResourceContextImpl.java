/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.server;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.LocalRequestProjectionMask;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.test.TestResourceContext;

import java.net.HttpCookie;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 * @author jnchen
 */
public class TestResourceContextImpl
{
  @Test
  public void testAddCustomContextData() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    String bar = "bar";
    context.putCustomContextData("foo", bar);
    Assert.assertTrue(context.getCustomContextData("foo").isPresent());
    Assert.assertSame(context.getCustomContextData("foo").get(), bar);
  }

  @Test
  public void testRemoveCustomContextData() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    String bar = "bar";
    context.putCustomContextData("foo", bar);
    Optional<Object> barRemove = context.removeCustomContextData("foo");
    Optional<Object> barAfterRemove = context.getCustomContextData("foo");
    Assert.assertSame(barRemove.get(), bar);
    Assert.assertFalse(barAfterRemove.isPresent());
  }

  @Test
  public void testGetEmptyCustomContextData() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    Optional<Object> foo = context.getCustomContextData("foo");
    Assert.assertFalse(foo.isPresent());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetIdHeader() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    context.setResponseHeader(RestConstants.HEADER_ID, "foobar");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetRestLiIdHeader() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    context.setResponseHeader(RestConstants.HEADER_RESTLI_ID, "foobar");
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testUnmodifiableHeaders() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    context.getResponseHeaders().put(RestConstants.HEADER_ID, "foobar");
  }

  @Test
  public void testPathKeysImpl() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    MutablePathKeys mutablePathKeys = context.getPathKeys();
    mutablePathKeys.append("aKey", "aValue")
        .append("bKey", "bValue")
        .append("cKey", "cValue");

    Assert.assertEquals(mutablePathKeys.getKeyMap().size(), 3);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testUnmodifiablePathKeysMap() throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl();
    context.getPathKeys().getKeyMap().put("should", "puke");
  }

  @Test
  public void testCookiesLocalAttr() throws Exception
  {
    URI uri = URI.create("resources");

    RequestContext requestContext = new RequestContext();
    List<HttpCookie> localCookies = Collections.singletonList(new HttpCookie("test", "value"));
    requestContext.putLocalAttr(ServerResourceContext.CONTEXT_COOKIES_KEY, localCookies);

    ServerResourceContext resourceContext = new ResourceContextImpl(
        new PathKeysImpl(), new TestResourceContext.MockRequest(uri), requestContext);

    // Assert that request cookies are retrieved from the local attribute.
    Assert.assertSame(resourceContext.getRequestCookies(), localCookies);
  }

  @Test
  public void testQueryParamsLocalAttr() throws Exception
  {
    URI uri = URI.create("resources");

    RequestContext requestContext = new RequestContext();
    DataMap queryParams = new DataMap(Collections.singletonMap("testKey", "testValue"));
    requestContext.putLocalAttr(ServerResourceContext.CONTEXT_QUERY_PARAMS_KEY, queryParams);

    ServerResourceContext resourceContext = new ResourceContextImpl(
        new PathKeysImpl(), new TestResourceContext.MockRequest(uri), requestContext);

    // Assert that query params are retrieved from the local attribute.
    Assert.assertSame(resourceContext.getParameters(), queryParams);
  }

  @DataProvider
  private static Object[][] overrideMaskData()
  {
    return new Object[][]
        {
            { ProjectionType.METADATA, "resources", Collections.emptyList() },
            { ProjectionType.METADATA, "resources/?metadataFields=locale", Collections.singletonList("locale") },
            { ProjectionType.PAGING, "resources", Collections.emptyList() },
            { ProjectionType.PAGING, "resources/?pagingFields=locale", Collections.singletonList("locale") },
            { ProjectionType.RESOURCE, "resources", Collections.emptyList() },
            { ProjectionType.RESOURCE, "resources/?fields=locale", Collections.singletonList("locale") }
        };
  }

  @Test(dataProvider = "overrideMaskData")
  public void testOverrideMask(ProjectionType projectionType, String stringUri, List<String> projectedFields) throws Exception
  {
    URI uri = URI.create(stringUri);
    ServerResourceContext resourceContext = new ResourceContextImpl(
        new PathKeysImpl(), new TestResourceContext.MockRequest(uri), new RequestContext());

    // Assert the current projections before we set the override mask
    MaskTree projectionMask = getProjectionMask(resourceContext, projectionType);
    if (projectedFields.isEmpty())
    {
      Assert.assertNull(projectionMask);
    }
    else
    {
      Assert.assertNotNull(projectionMask);
      Map<PathSpec, MaskOperation> maskOperations = projectionMask.getOperations();
      Assert.assertNotNull(maskOperations);
      Assert.assertEquals(maskOperations.size(), projectedFields.size());
      for (String projectedField: projectedFields)
      {
        Assert.assertTrue(maskOperations.containsKey(new PathSpec(projectedField)));
        Assert.assertEquals(maskOperations.get(new PathSpec(projectedField)), MaskOperation.POSITIVE_MASK_OP);
      }
    }

    final DataMap overrideDataMap = new DataMap();
    overrideDataMap.put("state", 1);

    setProjectionMask(resourceContext, projectionType, new MaskTree(overrideDataMap));

    // Assert the projections after the projection mask is overridden
    projectionMask = getProjectionMask(resourceContext, projectionType);
    Assert.assertNotNull(projectionMask);
    Map<PathSpec, MaskOperation> maskOperations = projectionMask.getOperations();
    Assert.assertNotNull(maskOperations);
    Assert.assertEquals(maskOperations.size(), 1);
    Assert.assertTrue(maskOperations.containsKey(new PathSpec("state")));
    Assert.assertEquals(maskOperations.get(new PathSpec("state")), MaskOperation.POSITIVE_MASK_OP);
  }

  @Test
  public void testProjectionMaskLocalAttr() throws Exception
  {
    URI uri = URI.create("resources");

    RequestContext requestContext = new RequestContext();
    MaskTree projectionMask = new MaskTree();
    MaskTree metadataProjectionMask = new MaskTree();
    MaskTree pagingProjectionMask = new MaskTree();
    LocalRequestProjectionMask localRequestProjectionMask =
        new LocalRequestProjectionMask(projectionMask, metadataProjectionMask, pagingProjectionMask);
    requestContext.putLocalAttr(ServerResourceContext.CONTEXT_PROJECTION_MASKS_KEY, localRequestProjectionMask);

    ServerResourceContext resourceContext = new ResourceContextImpl(
        new PathKeysImpl(), new TestResourceContext.MockRequest(uri), requestContext);

    // Assert that projection mask is retrieved from the local attribute.
    Assert.assertSame(resourceContext.getProjectionMask(), projectionMask);
    Assert.assertSame(resourceContext.getMetadataProjectionMask(), metadataProjectionMask);
    Assert.assertSame(resourceContext.getPagingProjectionMask(), pagingProjectionMask);
  }

  private enum ProjectionType
  {
    METADATA,
    PAGING,
    RESOURCE
  }

  private static void setProjectionMask(ServerResourceContext resourceContext, ProjectionType projectionType, MaskTree projectionMask)
  {
    switch (projectionType)
    {
      case METADATA:
        resourceContext.setMetadataProjectionMask(projectionMask);
        break;
      case PAGING:
        resourceContext.setPagingProjectionMask(projectionMask);
        break;
      case RESOURCE:
        resourceContext.setProjectionMask(projectionMask);
        break;
    }
  }

  private static MaskTree getProjectionMask(ServerResourceContext resourceContext, ProjectionType projectionType)
  {
    switch (projectionType)
    {
      case METADATA:
        return resourceContext.getMetadataProjectionMask();
      case PAGING:
        return resourceContext.getPagingProjectionMask();
      case RESOURCE:
        return resourceContext.getProjectionMask();
      default:
        throw new IllegalArgumentException("Invalid projection type");
    }
  }

  @DataProvider(name = "returnEntityParameterData")
  public Object[][] provideReturnEntityParameterData()
  {
    return new Object[][]
        {
            { "/foo?" + RestConstants.RETURN_ENTITY_PARAM + "=true", true, false },
            { "/foo?" + RestConstants.RETURN_ENTITY_PARAM + "=false", false, false },
            { "/foo", true, false },
            { "/foo?" + RestConstants.RETURN_ENTITY_PARAM + "=bar", false, true }
        };
  }

  @Test(dataProvider = "returnEntityParameterData")
  public void testReturnEntityParameter(String uri, boolean expectReturnEntity, boolean expectException) throws RestLiSyntaxException
  {
    final ResourceContextImpl context = new ResourceContextImpl(new PathKeysImpl(),
        new RestRequestBuilder(URI.create(uri))
            .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.LATEST_PROTOCOL_VERSION.toString())
            .build(),
        new RequestContext());

    try
    {
      final boolean returnEntity = context.isReturnEntityRequested();

      if (expectException)
      {
        Assert.fail("Exception should be thrown for URI: " + uri);
      }
      Assert.assertEquals(returnEntity, expectReturnEntity, "Resource context was wrong about whether the URI \"" + uri + "\" indicates that the entity should be returned.");
    }
    catch (RestLiServiceException e)
    {
      if (!expectException)
      {
        Assert.fail("Exception should not be thrown for URI: " + uri);
      }
    }
  }
}
