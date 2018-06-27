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
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.resources.AssociationResourceAsyncTemplate;
import com.linkedin.restli.server.resources.AssociationResourceTaskTemplate;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceAsyncTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTaskTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceAsyncTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceTaskTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;
import com.linkedin.restli.server.resources.SimpleResourceAsyncTemplate;
import com.linkedin.restli.server.resources.SimpleResourceTaskTemplate;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


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

  @RestLiCollection(name="collectionCollection")
  private static class CollectionCollectionResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionCollectionAsync")
  private static class CollectionCollectionAsyncResource extends CollectionResourceAsyncTemplate<String, EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionComplexKey")
  private static class CollectionComplexKeyResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {// Use full-qualified classname here since we cannot add @SuppressWarnings("deprecation") in import
  }

  @RestLiCollection(name="collectionComplexKeyAsync")
  private static class CollectionComplexKeyAsyncResource extends ComplexKeyResourceAsyncTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionAssociation")
  private static class CollectionAssociationResource extends AssociationResourceTemplate<EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionAssociationAsync")
  private static class CollectionAssociationAsyncResource extends AssociationResourceAsyncTemplate<EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiCollection(name="collectionAssociationPromise")
  private static class CollectionAssociationPromiseResource extends com.linkedin.restli.server.resources.AssociationResourcePromiseTemplate<EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionAssociationTask")
  private static class CollectionAssociationTaskResource extends AssociationResourceTaskTemplate<EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionSimple")
  private static class CollectionSimpleResource extends SimpleResourceTemplate<EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionSimpleAsync")
  private static class CollectionSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionSimpleTask")
  private static class CollectionSimpleTaskResource extends SimpleResourceTaskTemplate<EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiCollection(name="collectionSimplePromise")
  private static class CollectionSimplePromiseResource extends com.linkedin.restli.server.resources.SimpleResourcePromiseTemplate<EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationCollection", assocKeys = {})
  private static class AssociationCollectionResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationCollectionAsync", assocKeys = {})
  private static class AssociationCollectionAsyncResource extends CollectionResourceAsyncTemplate<String, EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationCollectionTask", assocKeys = {})
  private static class AssociationCollectionTaskResource extends CollectionResourceTaskTemplate<String, EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiAssociation(name="associationCollectionPromise", assocKeys = {})
  private static class AssociationCollectionPromiseResource extends com.linkedin.restli.server.resources.CollectionResourcePromiseTemplate<String, EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationComplexKey", assocKeys = {})
  private static class AssociationComplexKeyResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationComplexKeyAsync", assocKeys = {})
  private static class AssociationComplexKeyAsyncResource extends ComplexKeyResourceAsyncTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiAssociation(name="associationComplexKeyPromise", assocKeys = {})
  private static class AssociationComplexKeyPromiseResource extends com.linkedin.restli.server.resources.ComplexKeyResourcePromiseTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationComplexKeyTask", assocKeys = {})
  private static class AssociationComplexKeyTaskResource extends ComplexKeyResourceTaskTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationAssociation", assocKeys = {
    @Key(name="src", type=String.class),
    @Key(name="dest", type=String.class)
  })
  private static class AssociationAssociationResource extends AssociationResourceTemplate<EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationAssociationAsync", assocKeys = {
    @Key(name="src", type=String.class),
    @Key(name="dest", type=String.class)
  })
  private static class AssociationAssociationAsyncResource extends AssociationResourceAsyncTemplate<EmptyRecord>
  {
  }


  @RestLiAssociation(name="associationAssociationTask", assocKeys = {
      @Key(name="src", type=String.class),
      @Key(name="dest", type=String.class)
  })
  private static class AssociationAssociationTaskResource extends AssociationResourceTaskTemplate<EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiAssociation(name="associationAssociationPromise", assocKeys = {
      @Key(name="src", type=String.class),
      @Key(name="dest", type=String.class)
  })
  private static class AssociationAssociationPromiseResource extends com.linkedin.restli.server.resources.AssociationResourcePromiseTemplate<EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationSimple", assocKeys = {})
  private static class AssociationSimpleResource extends SimpleResourceTemplate<EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationSimpleAsync", assocKeys = {})
  private static class AssociationSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationSimpleTask", assocKeys = {})
  private static class AssociationSimpleTaskResource extends SimpleResourceTaskTemplate<EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiAssociation(name="associationSimplePromise", assocKeys = {})
  private static class AssociationSimplePromiseResource extends com.linkedin.restli.server.resources.SimpleResourcePromiseTemplate<EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleCollection")
  private static class SimpleCollectionResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleCollectionAsync")
  private static class SimpleCollectionAsyncResource extends CollectionResourceAsyncTemplate<String, EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleCollectionTask")
  private static class SimpleCollectionTaskResource extends CollectionResourceTaskTemplate<String, EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiSimpleResource(name="simpleCollectionPromise")
  private static class SimpleCollectionPromiseResource extends com.linkedin.restli.server.resources.CollectionResourcePromiseTemplate<String, EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleComplexKey")
  private static class SimpleComplexKeyResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleComplexKeyAsync")
  private static class SimpleComplexKeyAsyncResource extends ComplexKeyResourceAsyncTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiSimpleResource(name="simpleComplexKeyPromise")
  private static class SimpleComplexKeyPromiseResource extends com.linkedin.restli.server.resources.ComplexKeyResourcePromiseTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleComplexKeyTask")
  private static class SimpleComplexKeyTaskResource extends ComplexKeyResourceTaskTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleAssociation")
  private static class SimpleAssociationResource extends AssociationResourceTemplate<EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleAssociationAsync")
  private static class SimpleAssociationAsyncResource extends AssociationResourceAsyncTemplate<EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiSimpleResource(name="simpleAssociationPromise")
  private static class SimpleAssociationPromiseResource extends com.linkedin.restli.server.resources.AssociationResourcePromiseTemplate<EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleAssociationTask")
  private static class SimpleAssociationTaskResource extends AssociationResourceTaskTemplate<EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleSimple")
  private static class SimpleSimpleResource extends SimpleResourceTemplate<EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleSimpleAsync")
  private static class SimpleSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiSimpleResource(name="simpleSimplePromise")
  private static class SimpleSimplePromiseResource extends com.linkedin.restli.server.resources.SimpleResourcePromiseTemplate<EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleSimpleTask")
  private static class SimpleSimpleTaskResource extends SimpleResourceTaskTemplate<EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiCollection(name = "collectionPromise")
  private static class CollectionCollectionPromise extends com.linkedin.restli.server.resources.CollectionResourcePromiseTemplate<String, EmptyRecord>
  {
  }

  @RestLiCollection(name = "collectionTask")
  private static class CollectionCollectionTask extends CollectionResourceTaskTemplate<String, EmptyRecord>
  {
  }

  @SuppressWarnings("deprecation")
  @RestLiCollection(name = "collectionComplexKeyPromise")
  private static class CollectionComplexKeyPromise extends com.linkedin.restli.server.resources.ComplexKeyResourcePromiseTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiCollection(name = "collectionComplexKeyTask")
  private static class CollectionComplexKeyTask extends ComplexKeyResourceTaskTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
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
