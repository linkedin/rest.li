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
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.linkedin.restli.internal.server.model.SampleResources.*;


/**
 * @author Keren Jin
 */
public class TestRestLiTemplate
{
  @Test(dataProvider = "successResourceProvider")
  public void testSuccessCheckAnnotation(Class<?> testClass)
  {
    RestLiAnnotationReader.processResource(testClass);
  }

  @Test(dataProvider = "failResourceProvider")
  public void testFailCheckAnnotation(Class<?> testClass)
  {
    try
    {
      RestLiAnnotationReader.processResource(testClass);
      Assert.fail("Processing " + testClass.getName() + " should throw " + ResourceConfigException.class.getName());
    }
    catch (ResourceConfigException e)
    {
      Assert.assertTrue(e.getMessage().contains(testClass.getName()) &&
                          e.getMessage().contains("not annotated with"));
    }
  }

  @DataProvider
  private static Object[][] successResourceProvider()
  {
    return new Object[][] {
      { CollectionCollectionResource.class },
      { CollectionCollectionAsyncResource.class },
      { CollectionCollectionPromise.class },
      { CollectionCollectionTask.class },
      { CollectionComplexKeyResource.class },
      { CollectionComplexKeyAsyncResource.class },
      { CollectionComplexKeyPromise.class },
      { CollectionComplexKeyTask.class },
      { AssociationAssociationResource.class },
      { AssociationAssociationAsyncResource.class },
      { AssociationAssociationPromiseResource.class },
      { AssociationAssociationTaskResource.class },
      { SimpleSimpleResource.class },
      { SimpleSimpleAsyncResource.class },
      { SimpleSimplePromiseResource.class },
      { SimpleSimpleTaskResource.class }
    };
  }

  @DataProvider
  private static Object[][] failResourceProvider()
  {
    return new Object[][] {
      { CollectionAssociationResource.class },
      { CollectionAssociationAsyncResource.class },
      { CollectionAssociationPromiseResource.class },
      { CollectionAssociationTaskResource.class },
      { CollectionSimpleResource.class },
      { CollectionSimpleAsyncResource.class },
      { CollectionSimplePromiseResource.class },
      { CollectionSimpleTaskResource.class },
      { AssociationCollectionResource.class },
      { AssociationCollectionAsyncResource.class },
      { AssociationCollectionPromiseResource.class },
      { AssociationCollectionTaskResource.class },
      { AssociationComplexKeyResource.class },
      { AssociationComplexKeyAsyncResource.class },
      { AssociationComplexKeyPromiseResource.class },
      { AssociationComplexKeyTaskResource.class },
      { AssociationSimpleResource.class },
      { AssociationSimpleAsyncResource.class },
      { AssociationSimplePromiseResource.class },
      { AssociationSimpleTaskResource.class },
      { SimpleCollectionResource.class },
      { SimpleCollectionAsyncResource.class },
      { SimpleCollectionPromiseResource.class },
      { SimpleCollectionTaskResource.class },
      { SimpleComplexKeyResource.class },
      { SimpleComplexKeyAsyncResource.class },
      { SimpleComplexKeyPromiseResource.class },
      { SimpleComplexKeyTaskResource.class },
      { SimpleAssociationResource.class },
      { SimpleAssociationAsyncResource.class },
      { SimpleAssociationPromiseResource.class },
      { SimpleAssociationTaskResource.class }
    };
  }
}
