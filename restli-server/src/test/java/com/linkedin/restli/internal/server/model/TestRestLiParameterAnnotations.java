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
    // Non-POST/PUT resource methods cannot declare a desire to receive attachment params.
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
  public void parametersAreAnnotated()
  {
    try
    {
      RestLiAnnotationReader.processResource(ParamsNotAnnotatedFailureResource.class);
      Assert.fail("Processing " + ParamsNotAnnotatedFailureResource.class.getName() + " should throw " +
                      ResourceConfigException.class.getName());
    }
    catch (ResourceConfigException e) {
      Assert.assertTrue(e.getMessage().contains("@ValidateParam"));
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

  @DataProvider
  private static Object[][] successResourceProvider()
  {
    return new Object[][]
    {
      { CollectionSuccessResource.class },
      { AssociationAsyncSuccessResource.class},
      { UnstructuredDataParams.class}
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
