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

package com.linkedin.restli.internal.server.model;

import com.linkedin.restli.server.ResourceConfigException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.linkedin.restli.internal.server.model.SampleResources.*;


/**
 * Tests for {@link RestLiApiBuilder}, which transitively are also tests for {@link RestLiAnnotationReader}.
 *
 * Ensures that interesting resource configurations result in the correct behavior, whether that be successful
 * API generation or an appropriate exception being thrown.
 *
 * @author kparikh
 * @author Evan Williams
 */
public class TestRestLiApiBuilder
{
  @DataProvider(name = "resourcesWithClashingNamesDataProvider")
  public Object[][] provideResourcesWithClashingNames()
  {
    return new Object[][]
        {
            { new Class<?>[] { FooResource1.class, FooResource2.class }},
            { new Class<?>[] { FooResource1.class, FooResource2.class, BarResource.class }},
            { new Class<?>[] { FooResource1.class, FooResource3.class }},
            { new Class<?>[] { FooResource1.class, FooResource4.class }},
            { new Class<?>[] { FooResource3.class, FooResource4.class }}
        };
  }

  @Test(dataProvider = "resourcesWithClashingNamesDataProvider")
  public void testResourceNameClash(Class<?>[] classes)
  {
    Set<Class<?>> resourceClasses = new HashSet<Class<?>>(Arrays.<Class<?>>asList(classes));
    try
    {
      RestLiApiBuilder.buildResourceModels(resourceClasses);
      Assert.fail("API generation should have failed due to name clashes!");
    }
    catch (ResourceConfigException e)
    {
      Assert.assertTrue(e.getMessage().contains("clash on the resource name \"foo\""),
                        "The exception did not contain the expected resource name clashing error message.");
    }
  }

  @DataProvider(name = "resourcesWithNoClashingNamesDataProvider")
  public Object[][] provideResourcesWithNoClashingNames()
  {
    return new Object[][]
        {
            { new Class<?>[] {}},
            { new Class<?>[] { FooResource1.class, FOOResource.class }},
            { new Class<?>[] { FooResource1.class, BarResource.class }},
        };
  }

  @Test(dataProvider = "resourcesWithNoClashingNamesDataProvider")
  public void testResourceNameNoClash(Class<?>[] classes)
  {
    Set<Class<?>> resourceClasses = new HashSet<>(Arrays.asList(classes));
    Map<String, ResourceModel> resourceModels = RestLiApiBuilder.buildResourceModels(resourceClasses);
    Assert.assertEquals(resourceModels.size(),
                        classes.length,
                        "The number of ResourceModels generated does not match the number of resource classes.");
  }

  @Test
  public void testProcessResource()
  {
    Set<Class<?>> set = new HashSet<>();
    set.add(ParentResource.class);
    set.add(TestResource.class);
    Map<String, ResourceModel> models = RestLiApiBuilder.buildResourceModels(set);

    ResourceModel parentResource = models.get("/ParentResource");
    Assert.assertNotNull(parentResource.getSubResource("TestResource"));
  }

  @Test
  public void testBadResource()
  {
    Set<Class<?>> set = new HashSet<>();
    set.add(ParentResource.class);
    set.add(BadResource.class);

    try
    {
      RestLiApiBuilder.buildResourceModels(set);
      Assert.fail("Building api with BadResource should throw " + ResourceConfigException.class);
    }
    catch (ResourceConfigException e)  {
      Assert.assertTrue(e.getMessage().contains("bogusKey not found in path keys"));
    }
  }

  @DataProvider(name = "misconfiguredServiceErrorData")
  public Object[][] provideMisconfiguredServiceErrorData()
  {
    return new Object[][]
        {
            { UnknownServiceErrorCodeResource.class, "Unknown service error code 'MADE_UP_ERROR'" },
            { DuplicateServiceErrorCodesResource.class, "Duplicate service error code 'ERROR_A'" },
            { MissingServiceErrorDefResource.class, "is missing a @ServiceErrorDef annotation" }
        };
  }

  /**
   * Ensures that resources with misconfigured service errors will throw an appropriate {@link ResourceConfigException}
   * when its API is being built.
   *
   * @param resourceClass resource used as an input
   * @param expectedPartialMessage expects this string to be contained in the error message
   */
  @Test(dataProvider = "misconfiguredServiceErrorData")
  public void testMisconfiguredServiceErrors(Class<?> resourceClass, String expectedPartialMessage)
  {
    try
    {
      RestLiApiBuilder.buildResourceModels(Collections.singleton(resourceClass));
      Assert.fail(String.format("Expected %s for misconfigured service errors.", ResourceConfigException.class.getSimpleName()));
    }
    catch (ResourceConfigException e)
    {
      Assert.assertTrue(e.getMessage().contains(expectedPartialMessage),
          String.format("Expected %s with message containing \"%s\" but instead found message \"%s\"",
              ResourceConfigException.class.getSimpleName(), expectedPartialMessage, e.getMessage()));
    }
  }
}


