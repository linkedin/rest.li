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
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceAsyncTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceAsyncTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;
import com.linkedin.restli.server.resources.SimpleResourceAsyncTemplate;
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
  {
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

  @RestLiCollection(name="collectionSimple")
  private static class CollectionSimpleResource extends SimpleResourceTemplate<EmptyRecord>
  {
  }

  @RestLiCollection(name="collectionSimpleAsync")
  private static class CollectionSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord>
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

  @RestLiAssociation(name="associationComplexKey", assocKeys = {})
  private static class AssociationComplexKeyResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationComplexKeyAsync", assocKeys = {})
  private static class AssociationComplexKeyAsyncResource extends ComplexKeyResourceAsyncTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
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

  @RestLiAssociation(name="associationSimple", assocKeys = {})
  private static class AssociationSimpleResource extends SimpleResourceTemplate<EmptyRecord>
  {
  }

  @RestLiAssociation(name="associationSimpleAsync", assocKeys = {})
  private static class AssociationSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord>
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

  @RestLiSimpleResource(name="simpleComplexKey")
  private static class SimpleComplexKeyResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleComplexKeyAsync")
  private static class SimpleComplexKeyAsyncResource extends ComplexKeyResourceAsyncTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
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

  @RestLiSimpleResource(name="simpleSimple")
  private static class SimpleSimpleResource extends SimpleResourceTemplate<EmptyRecord>
  {
  }

  @RestLiSimpleResource(name="simpleSimpleAsync")
  private static class SimpleSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord>
  {
  }

  @DataProvider
  private static Object[][] successResourceProvider()
  {
    return new Object[][] {
      { CollectionCollectionResource.class },
      { CollectionCollectionAsyncResource.class },
      { CollectionComplexKeyResource.class },
      { CollectionComplexKeyAsyncResource.class },
      { AssociationAssociationResource.class },
      { AssociationAssociationAsyncResource.class },
      { SimpleSimpleResource.class },
      { SimpleSimpleAsyncResource.class }
    };
  }

  @DataProvider
  private static Object[][] failResourceProvider()
  {
    return new Object[][] {
      { CollectionAssociationResource.class },
      { CollectionAssociationAsyncResource.class },
      { CollectionSimpleResource.class },
      { CollectionSimpleAsyncResource.class },
      { AssociationCollectionResource.class },
      { AssociationCollectionAsyncResource.class },
      { AssociationComplexKeyResource.class },
      { AssociationComplexKeyAsyncResource.class },
      { AssociationSimpleResource.class },
      { AssociationSimpleAsyncResource.class },
      { SimpleCollectionResource.class },
      { SimpleCollectionAsyncResource.class },
      { SimpleComplexKeyResource.class },
      { SimpleComplexKeyAsyncResource.class },
      { SimpleAssociationResource.class },
      { SimpleAssociationAsyncResource.class }
    };
  }
}
