/*
   Copyright (c) 2019 LinkedIn Corp.

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
import com.linkedin.restli.server.errors.ServiceError;
import java.util.ArrayList;
import java.util.Arrays;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests for {@link ResourceModel}.
 *
 * @author Evan Williams
 */
public class TestResourceModel
{
  @DataProvider(name = "isAnyServiceErrorListDefinedData")
  public Object[][] providesIsAnyServiceErrorListDefinedData()
  {
    return new Object[][]
    {
        // No service errors or resource methods at all
        {
            null,
            makeMockResourceMethodDescriptors(),
            false
        },
        // Empty resource-level service errors list but not resource methods
        {
            new ServiceError[] {},
            makeMockResourceMethodDescriptors(),
            true
        },
        // One resource-level service error but no resource methods
        {
            new ServiceError[] { SampleResources.SampleServiceError.ERROR_A },
            makeMockResourceMethodDescriptors(),
            true
        },
        // No resource-level service errors but one resource method with a service error
        {
            null,
            makeMockResourceMethodDescriptors(true),
            true
        },
        // No resource-level service errors and two resource methods without service errors
        {
            null,
            makeMockResourceMethodDescriptors(false, false),
            false
        },
        // No resource-level service errors but three resource methods with mixed service errors
        {
            null,
            makeMockResourceMethodDescriptors(false, true, false),
            true
        },
        // Two resource-level service errors and two resource methods with service errors
        {
            new ServiceError[] { SampleResources.SampleServiceError.ERROR_A, SampleResources.SampleServiceError.ERROR_B },
            makeMockResourceMethodDescriptors(true, true),
            true
        }
    };
  }

  /**
   * Creates an array of mock {@link ResourceMethodDescriptor} objects by mocking the result of the method call
   * {@link ResourceMethodDescriptor#getServiceErrors()} for each.
   *
   * @param definesServiceErrorsArray whether each mock method descriptor defines service errors
   * @return array of mocked objects
   */
  private ResourceMethodDescriptor[] makeMockResourceMethodDescriptors(Boolean ... definesServiceErrorsArray)
  {
    final ResourceMethodDescriptor[] resourceMethodDescriptors = new ResourceMethodDescriptor[definesServiceErrorsArray.length];
    int i = 0;
    for (boolean definesServiceErrors : definesServiceErrorsArray)
    {
      final ResourceMethodDescriptor resourceMethodDescriptor = Mockito.mock(ResourceMethodDescriptor.class);
      Mockito.when(resourceMethodDescriptor.getServiceErrors()).thenReturn(definesServiceErrors ? new ArrayList<>() : null);
      resourceMethodDescriptors[i++] = resourceMethodDescriptor;
    }
    return resourceMethodDescriptors;
  }

  /**
   * Ensures that the logic in {@link ResourceModel#isAnyServiceErrorListDefined()} is correct.
   *
   * @param resourceLevelServiceErrors resource-level service errors
   * @param resourceMethodDescriptors resource method descriptors possibly containing method-level service errors
   * @param expected expected result of the method call
   */
  @Test(dataProvider = "isAnyServiceErrorListDefinedData")
  public void testIsAnyServiceErrorListDefined(ServiceError[] resourceLevelServiceErrors,
      ResourceMethodDescriptor[] resourceMethodDescriptors, boolean expected) {
    // Create dummy resource model
    final ResourceModel resourceModel = new ResourceModel(EmptyRecord.class,
                                                          SampleResources.CollectionCollectionResource.class,
                                                          null,
                                                          "collectionCollection",
                                                          ResourceType.COLLECTION,
                                                          "com.linkedin.restli.internal.server.model");

    // Add resource-level service errors
    if (resourceLevelServiceErrors == null)
    {
      resourceModel.setServiceErrors(null);
    }
    else
    {
      resourceModel.setServiceErrors(Arrays.asList(resourceLevelServiceErrors));
    }

    // Add mock resource method descriptors
    for (ResourceMethodDescriptor resourceMethodDescriptor : resourceMethodDescriptors)
    {
      resourceModel.addResourceMethodDescriptor(resourceMethodDescriptor);
    }

    Assert.assertEquals(expected, resourceModel.isAnyServiceErrorListDefined(),
        "Cannot correctly compute whether resource model defines resource-level or method-level service errors.");
  }
}
