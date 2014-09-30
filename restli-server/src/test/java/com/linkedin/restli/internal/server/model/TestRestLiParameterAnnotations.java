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


import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.MetadataProjectionParam;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.PagingProjectionParam;
import com.linkedin.restli.server.annotations.ParSeqContextParam;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.ProjectionParam;
import com.linkedin.restli.server.annotations.ResourceContextParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Sachin Jhunjhunwala
 */
public class TestRestLiParameterAnnotations
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
      Assert.assertTrue(e.getMessage().contains("Incorrect data type for param"));
    }
  }

  @RestLiCollection(name="collectionCollectionSuccessResource")
  private static class CollectionCollectionSuccessResource extends CollectionResourceTemplate<String, EmptyRecord>
  {

    @Finder("PagingContextParamFinder")
    public List<EmptyRecord> PagingContextParamNewTest(@PagingContextParam PagingContext pagingContext)
    {
      return Collections.emptyList();
    }

    @Finder("PathKeysParamFinder")
    public List<EmptyRecord> PathKeysParamNewTest(@PathKeysParam PathKeys keys)
    {
      return Collections.emptyList();
    }

    @Finder("ProjectionParamFinder")
    public List<EmptyRecord> ProjectionParamDeprecatedTest(@ProjectionParam MaskTree projectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("MetadataProjectionParamFinder")
    public List<EmptyRecord> MetadataProjectionParamNewTest(@MetadataProjectionParam MaskTree metadataProjectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("PagingProjectionParamFinder")
    public List<EmptyRecord> PagingProjectionParamNewTest(@PagingProjectionParam MaskTree pagingProjectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("ResourceContextParamFinder")
    public List<EmptyRecord> ResourceContextParamNewTest(@ResourceContextParam ResourceContext resourceContext)
    {
      return Collections.emptyList();
    }

    public Promise<? extends String> ParseqContextParamNewTest(@ParSeqContextParam com.linkedin.parseq.Context parseqContext)
    {
      return null;
    }
  }

  @RestLiCollection(name="collectionCollectionPagingContextParamFailureResource")
  private static class CollectionCollectionPagingContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("PagingContextParamIncorrectDataTypeFinder")
    public List<EmptyRecord> PagingContextParamIncorrectDataTypeTest(@PagingContextParam String pagingContext)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name="collectionCollectionPathKeysFailureResource")
  private static class CollectionCollectionPathKeysFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("PathKeysParamIncorrectDataTypeFinder")
    public List<EmptyRecord> PathKeysParamIncorrectDataTypeTest(@PathKeysParam String keys)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name="collectionCollectionProjectionParamFailureResource")
  private static class CollectionCollectionProjectionParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("ProjectionParamIncorrectDataTypeFinder")
    public List<EmptyRecord> ProjectionParamIncorrectDataTypeTest(@ProjectionParam String projectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("MetadataProjectionParamIncorrectDataTypeFinder")
    public List<EmptyRecord> MetadataProjectionParamIncorrectDataTypeTest(@MetadataProjectionParam String metadataProjectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("PagingProjectionParamIncorrectDataTypeFinder")
    public List<EmptyRecord> PagingProjectionParamIncorrectDataTypeTest(@PagingProjectionParam String pagingProjectionParam)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name="collectionCollectionResourceContextParamFailureResource")
  private static class CollectionCollectionResourceContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("ResourceContextParamIncorrectDataTypeFinder")
    public List<EmptyRecord> ResourceContextParamIncorrectDataTypeTest(@ResourceContextParam String resourceContext)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name="collectionCollectionParseqContextParamFailureResource")
  private static class CollectionCollectionParseqContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    public Promise<? extends String> ParseqContextParamNewTest(@ParSeqContextParam String parseqContext)
    {
      return null;
    }
  }

  @RestLiAssociation(name="associationCollectionAsyncSuccessResource",
                     assocKeys={@Key(name="AssocKey_Deprecated", type=String.class),
                                @Key(name="AssocKeyParam_New", type=String.class)})
  private static class AssociationCollectionAsyncSuccessResource extends AssociationResourceTemplate<EmptyRecord>
  {
    @Finder("assocKeyParamFinder")
    public List<EmptyRecord> assocKeyParamNewTest(@AssocKeyParam("AssocKeyParam_New") long key)
    {
      return Collections.emptyList();
    }
  }

  @DataProvider
  private static Object[][] successResourceProvider()
  {
    return new Object[][] {
      { CollectionCollectionSuccessResource.class },
      { AssociationCollectionAsyncSuccessResource.class}
    };
  }

  @DataProvider
  private static Object[][] failResourceProvider()
  {
    return new Object[][] {
      { CollectionCollectionPagingContextParamFailureResource.class },
      { CollectionCollectionPathKeysFailureResource.class },
      { CollectionCollectionProjectionParamFailureResource.class },
      { CollectionCollectionResourceContextParamFailureResource.class }
    };
  }
}
