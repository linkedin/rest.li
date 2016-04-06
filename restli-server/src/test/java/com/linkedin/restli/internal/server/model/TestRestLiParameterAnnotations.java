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
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
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
import com.linkedin.restli.server.annotations.RestLiAttachmentsParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  @Test(dataProvider = "parameterTypeMismatchDataProvider")
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

  @Test (dataProvider = "nonPostOrPutAttachmentsParam")
  public void nonPostPutAttachmentParamsInvalid(Class<?> testClass)
  {
    //Non POST/PUT resource methods cannot declare a desire to receive attachment params.
    try
    {
      RestLiAnnotationReader.processResource(testClass);
      Assert.fail("Processing " + CollectionFinderAttachmentParams.class.getName() + " should throw " +
                      ResourceConfigException.class.getName());
    }
    catch (ResourceConfigException e)
    {
      Assert.assertTrue(e.getMessage().contains("is only allowed within the following"));
    }
  }

  @Test
  public void multipleAttachmentParamsInvalid()
  {
    try
    {
      RestLiAnnotationReader.processResource(CollectionMultipleAttachmentParamsFailureResource.class);
      Assert.fail("Processing " + CollectionMultipleAttachmentParamsFailureResource.class.getName() + " should throw " +
                      ResourceConfigException.class.getName());
    }
    catch (ResourceConfigException e)
    {
      Assert.assertTrue(e.getMessage().contains("is specified more than once"));
    }
  }

  @RestLiCollection(name="CollectionFinderAttachmentParams")
  private static class CollectionFinderAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("attachmentsFinder")
    public List<EmptyRecord> AttachmentsFinder(@RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name="CollectionGetAttachmentParams")
  private static class CollectionGetAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.Get
    public EmptyRecord get(String key, @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name="CollectionBatchGetAttachmentParams")
  private static class CollectionBatchGetAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.BatchGet
    public Map<String, EmptyRecord> batchGet(Set<String> keys, @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name="CollectionDeleteAttachmentParams")
  private static class CollectionDeleteAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.Delete
    public UpdateResponse delete(String key, @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name="CollectionBatchDeleteAttachmentParams")
  private static class CollectionBatchDeleteAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.BatchDelete
    public BatchUpdateResult<String, EmptyRecord> batchDelete(BatchDeleteRequest<String, EmptyRecord> ids,
                                                              @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name="CollectionGetAllAttachmentParams")
  private static class CollectionGetAllAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.GetAll
    public List<EmptyRecord> getAll(@PagingContextParam PagingContext pagingContext, @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @DataProvider
  private static Object[][] nonPostOrPutAttachmentsParam()
  {
    return new Object[][]
        {
            { CollectionFinderAttachmentParams.class },
            { CollectionGetAttachmentParams.class },
            { CollectionBatchGetAttachmentParams.class },
            { CollectionDeleteAttachmentParams.class },
            { CollectionBatchDeleteAttachmentParams.class },
            { CollectionGetAllAttachmentParams.class }
        };
  }

  @RestLiCollection(name="collectionMultipleAttachmentParamsFailureResource")
  private static class CollectionMultipleAttachmentParamsFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Action(name = "MultipleAttachmentParams")
    public void MultipleAttachmentParams(@RestLiAttachmentsParam RestLiAttachmentReader attachmentReaderA,
                                         @RestLiAttachmentsParam RestLiAttachmentReader attachmentReaderB)
    {
    }
  }

  @RestLiCollection(name="collectionSuccessResource")
  private static class CollectionSuccessResource extends CollectionResourceTemplate<String, EmptyRecord>
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

  @RestLiCollection(name="collectionPagingContextParamFailureResource")
  private static class CollectionPagingContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("PagingContextParamIncorrectDataTypeFinder")
    public List<EmptyRecord> PagingContextParamIncorrectDataTypeTest(@PagingContextParam String pagingContext)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name="collectionPathKeysFailureResource")
  private static class CollectionPathKeysFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("PathKeysParamIncorrectDataTypeFinder")
    public List<EmptyRecord> PathKeysParamIncorrectDataTypeTest(@PathKeysParam String keys)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name="collectionProjectionParamFailureResource")
  private static class CollectionProjectionParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
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

  @RestLiCollection(name="collectionResourceContextParamFailureResource")
  private static class CollectionResourceContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("ResourceContextParamIncorrectDataTypeFinder")
    public List<EmptyRecord> ResourceContextParamIncorrectDataTypeTest(@ResourceContextParam String resourceContext)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name="collectionParseqContextParamFailureResource")
  private static class CollectionParseqContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    public Promise<? extends String> ParseqContextParamNewTest(@ParSeqContextParam String parseqContext)
    {
      return null;
    }
  }

  @RestLiCollection(name="collectionAttachmentParamsFailureResource")
  private static class CollectionAttachmentParamsFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Action(name = "AttachmentParamsIncorrectDataTypeAction")
    public void AttachmentParamsIncorrectDataTypeAction(@RestLiAttachmentsParam String attachmentReader)
    {
    }
  }

  @RestLiAssociation(name="associationAsyncSuccessResource",
                     assocKeys={@Key(name="AssocKey_Deprecated", type=String.class),
                                @Key(name="AssocKeyParam_New", type=String.class)})
  private static class AssociationAsyncSuccessResource extends AssociationResourceTemplate<EmptyRecord>
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
    return new Object[][]
    {
      { CollectionSuccessResource.class },
      { AssociationAsyncSuccessResource.class}
    };
  }

  @DataProvider
  private static Object[][] parameterTypeMismatchDataProvider()
  {
    return new Object[][]
    {
      { CollectionPagingContextParamFailureResource.class },
      { CollectionPathKeysFailureResource.class },
      { CollectionProjectionParamFailureResource.class },
      { CollectionResourceContextParamFailureResource.class },
      { CollectionAttachmentParamsFailureResource.class }
    };
  }
}