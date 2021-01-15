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

import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.server.ResourceConfigException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

  @DataProvider(name = "badResources")
  public Object[][] badResources()
  {
    return new Object[][]
        {
            { BadResource.class, "bogusKey not found in path keys"},
            { SymbolsResource.class, "\"symbolTable\" is reserved for internal use"},
        };
  }

  @Test(dataProvider = "badResources")
  public void testBadResource(Class<?> resourceClass, String errorMsg)
  {
    Set<Class<?>> set = new HashSet<>();
    set.add(ParentResource.class);
    set.add(resourceClass);
    try
    {
      RestLiApiBuilder.buildResourceModels(set);
      Assert.fail("Building api with BadResource should throw " + ResourceConfigException.class);
    }
    catch (ResourceConfigException e)
    {
      Assert.assertTrue(e.getMessage().contains(errorMsg));
    }
  }

  @DataProvider(name = "misconfiguredServiceErrorData")
  public Object[][] provideMisconfiguredServiceErrorData()
  {
    return new Object[][]
        {
            { UnknownServiceErrorCodeResource.class, "Unknown service error code 'MADE_UP_ERROR'" },
            { DuplicateServiceErrorCodesResource.class, "Duplicate service error code 'ERROR_A'" },
            { MissingServiceErrorDefResource.class, "is missing a @ServiceErrorDef annotation" },
            { ForbiddenErrorDetailTypeResource.class, "Class 'com.linkedin.restli.common.ErrorDetails' is not meant to be used as an error detail type" },
            { UnknownServiceErrorParameterResource.class, "Nonexistent parameter 'spacestamp' specified for method-level service error" },
            { EmptyServiceErrorParametersResource.class, "specifies no parameter names for service error code 'ERROR_A'" },
            { DuplicateServiceErrorParametersResource.class, "Duplicate parameter specified for service error code 'ERROR_A'" },
            { DuplicateServiceErrorParamErrorCodesResource.class, "Redundant @ParamError annotations for service error code 'ERROR_A'" },
            { RedundantServiceErrorCodeWithParameterResource.class, "Service error code 'ERROR_A' redundantly specified in both @ServiceErrors and @ParamError annotations" },
            { InvalidSuccessStatusesResource.class, "Invalid success status 'S_500_INTERNAL_SERVER_ERROR' specified" },
            { EmptySuccessStatusesResource.class, "specifies no success statuses" }
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

  @DataProvider(name = "actionReturnTypeData")
  private Object[][] provideActionReturnTypeData()
  {
    return new Object[][]
        {
            { ActionReturnTypeVoidResource.class, Void.TYPE },
            { ActionReturnTypeIntegerResource.class, Integer.class },
            { ActionReturnTypeRecordResource.class, EmptyRecord.class }
        };
  }

  /**
   * Ensures that when action methods are processed, the correct return "logical" return type is identified.
   * For instance, it should recognize that the "logical" return type for a method
   * {@code Task<ActionResult<String>> doFoo();} is {@code String.class}.
   *
   * @param resourceClass resource used as an input
   * @param expectedActionReturnType the expected action return type
   */
  @Test(dataProvider = "actionReturnTypeData")
  public void testActionReturnType(Class<?> resourceClass, Class<?> expectedActionReturnType)
  {
    // Process the resource and collect the resource method descriptors
    Map<String, ResourceModel> models = RestLiApiBuilder.buildResourceModels(Collections.singleton(resourceClass));
    Assert.assertEquals(models.size(), 1);
    ResourceModel model = models.get(models.keySet().iterator().next());
    Assert.assertNotNull(model);
    List<ResourceMethodDescriptor> resourceMethodDescriptors = model.getResourceMethodDescriptors();

    // For each method, check that the action return type was correctly identified
    for (ResourceMethodDescriptor resourceMethodDescriptor : resourceMethodDescriptors)
    {
      Class<?> logicalReturnType = resourceMethodDescriptor.getActionReturnType();
      Assert.assertEquals(resourceMethodDescriptor.getActionReturnType(), expectedActionReturnType);
    }
  }

  @DataProvider(name = "unsupportedFinderResourceTypeData")
  private Object[][] unsupportedFinderResourceTypeData()
  {
    return new Object[][]
        {
            { FinderUnsupportedKeyUnstructuredDataResource.class },
            { FinderUnsupportedSingleUnstructuredDataResource.class }
        };
  }

  /**
   * Ensures that when finder methods are processed, when the resource value class is a SingleUnstructuredDataResource
   * or a KeyUnstructuredDataResource, we will end up with a ResourceConfigException because we don't support that righ tnow.
   * {@code Task<ActionResult<String>> doFoo();} is {@code String.class}.
   *
   * @param resourceClass resource used as an input
   */
  @Test(dataProvider = "unsupportedFinderResourceTypeData",
      expectedExceptions = ResourceConfigException.class,
      expectedExceptionsMessageRegExp = "Class '.*' does not support @Finder methods, because it's an unstructured data resource")
  public void testFinderUnsupportedResourceType(Class<?> resourceClass)
  {
    RestLiApiBuilder.buildResourceModels(Collections.singleton(resourceClass));
  }

  @DataProvider(name = "finderSupportedResourceTypeData")
  private Object[][] finderSupportedResourceTypeData()
  {
    return new Object[][]
        {
            { FinderSupportedAssociationDataResource.class },
            { FinderSupportedComplexKeyDataResource.class }
        };
  }

  @Test(dataProvider = "finderSupportedResourceTypeData")
  public void testFinderSupportedResourceType(Class<?> resourceClass)
  {
    try
    {
      RestLiApiBuilder.buildResourceModels(Collections.singleton(resourceClass));
    }
    catch (Exception exception)
    {
      Assert.fail(String.format("Unexpected exception:  class: %s, message: \"%s\"",
              resourceClass.getSimpleName(), exception.getMessage()));
    }
  }

  /**
   * Tests usage of {@link com.linkedin.restli.server.annotations.PathKeysParam} and
   * {@link com.linkedin.restli.server.annotations.PathKeyParam} when processing the resource implementation.
   */
  @Test
  public void testPathKeyParamAnnotations()
  {
    // Test correct use of both @PathKeyParam and @PathKeysParam
    final Map<String, ResourceModel> resourceModels = new HashMap<>();
    try
    {
      resourceModels.putAll(RestLiApiBuilder.buildResourceModels(Collections.singleton(SampleResources.PathKeyParamAnnotationsResource.class)));
    }
    catch (Exception exception)
    {
      Assert.fail(String.format("Unexpected exception: class: %s, message: \"%s\"",
          SampleResources.PathKeyParamAnnotationsResource.class.getSimpleName(), exception.getMessage()));
    }
    Assert.assertEquals(1, resourceModels.size());
    Assert.assertTrue(resourceModels.containsKey("/pathKeyParamAnnotations"));

    // Test incorrect usage of @PathKeyParam (unrecognized path key name)
    try
    {
      RestLiApiBuilder.buildResourceModels(Collections.singleton(SampleResources.BadPathKeyParamAnnotationsResource.class));
      Assert.fail("Expected a ResourceConfigException due to unrecognized path key names.");
    }
    catch (Exception exception)
    {
      Assert.assertTrue(exception instanceof ResourceConfigException);
      Assert.assertEquals("Parameter unknownId not found in path keys of class class "
          + "com.linkedin.restli.internal.server.model.SampleResources$BadPathKeyParamAnnotationsResource",
          exception.getMessage());
    }
  }
}