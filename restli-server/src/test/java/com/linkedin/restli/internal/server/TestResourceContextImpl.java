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
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;

import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.test.TestResourceContext;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestResourceContextImpl
{
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
}
