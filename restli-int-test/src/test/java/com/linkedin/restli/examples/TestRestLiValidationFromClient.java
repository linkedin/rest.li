/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.examples;


import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.client.AutoValidationDemosBuilders;
import com.linkedin.restli.examples.greetings.client.AutoValidationDemosRequestBuilders;
import com.linkedin.restli.examples.greetings.client.ValidationDemosBuilders;
import com.linkedin.restli.examples.greetings.client.ValidationDemosRequestBuilders;
import com.linkedin.restli.test.util.PatchBuilder;
import com.linkedin.restli.test.util.RootBuilderWrapper;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests the Rest.li validator from the client side.
 * Has the same set of tests as {@link TestRestLiValidation}, except for output validation.
 *
 * @author Soojung Ha
 */
public class TestRestLiValidationFromClient
{
  private static final Object[] BUILDERS = new Object[] {
      new ValidationDemosBuilders(), new ValidationDemosRequestBuilders(),
      new AutoValidationDemosBuilders(), new AutoValidationDemosRequestBuilders()
  };

  private static Object[][] wrapFailureCases(Object[][] failureCases, Object[] builders)
  {
    Object[][] result = new Object[builders.length * failureCases.length][3];
    for (int i = 0; i < failureCases.length; i++)
    {
      for (int j = 0; j < builders.length; j++)
      {
        result[builders.length * i + j][0] = builders[j];
        result[builders.length * i + j][1] = failureCases[i][0];
        result[builders.length * i + j][2] = failureCases[i][1];
      }
    }
    return result;
  }

  private static Object[][] wrapSuccessCases(Object[] successCases, Object[] builders)
  {
    Object[][] result = new Object[builders.length * successCases.length][2];
    for (int i = 0; i < successCases.length; i++)
    {
      for (int j = 0; j < builders.length; j++)
      {
        result[builders.length * i + j][0] = builders[j];
        result[builders.length * i + j][1] = successCases[i];
      }
    }
    return result;
  }

  @DataProvider
  public Object[][] provideCreateFailureData()
  {
    return wrapFailureCases(TestRestLiValidation.createFailures(), BUILDERS);
  }

  @Test(dataProvider = "provideCreateFailureData")
  public void testCreateFailure(Object builder, ValidationDemo validationDemo, String errorMessage)
  {
    ValidationResult result = new RootBuilderWrapper<Integer, ValidationDemo>(builder, ValidationDemo.class).create().validateInput(validationDemo);
    Assert.assertEquals(result.isValid(), false);
    Assert.assertTrue(result.getMessages().toString().contains(errorMessage));
  }

  @DataProvider
  public Object[][] provideCreateSuccessData()
  {
    return wrapSuccessCases(TestRestLiValidation.createSuccessData(), BUILDERS);
  }

  @Test(dataProvider = "provideCreateSuccessData")
  public void testCreateSuccess(Object builder, ValidationDemo validationDemo)
  {
    ValidationResult result = new RootBuilderWrapper<Integer, ValidationDemo>(builder, ValidationDemo.class).create().validateInput(validationDemo);
    Assert.assertEquals(result.isValid(), true);
  }

  @DataProvider
  public Object[][] provideUpdateFailureData()
  {
    return wrapFailureCases(TestRestLiValidation.updateFailures(), BUILDERS);
  }

  @Test(dataProvider = "provideUpdateFailureData")
  public void testUpdateFailure(Object builder, ValidationDemo validationDemo, String errorMessage)
  {
    ValidationResult result = new RootBuilderWrapper<Integer, ValidationDemo>(builder, ValidationDemo.class).update().validateInput(validationDemo);
    Assert.assertEquals(result.isValid(), false);
    Assert.assertTrue(result.getMessages().toString().contains(errorMessage));
  }

  @DataProvider
  public Object[][] provideUpdateSuccessData()
  {
    return wrapSuccessCases(TestRestLiValidation.updateSuccesses(), BUILDERS);
  }

  @Test(dataProvider = "provideUpdateSuccessData")
  public void testUpdateSuccess(Object builder, ValidationDemo validationDemo)
  {
    ValidationResult result = new RootBuilderWrapper<Integer, ValidationDemo>(builder, ValidationDemo.class).update().validateInput(validationDemo);
    Assert.assertEquals(result.isValid(), true);
  }

  @DataProvider
  public Object[][] providePartialUpdateFailureData()
  {
    return wrapFailureCases(TestRestLiValidation.partialUpdateFailures(), BUILDERS);
  }

  @Test(dataProvider = "providePartialUpdateFailureData")
  public void testPartialUpdateFailure(Object builder, String patch, String errorMessage)
  {
    PatchRequest<ValidationDemo> patchRequest = PatchBuilder.buildPatchFromString(patch);
    ValidationResult result = new RootBuilderWrapper<Integer, ValidationDemo>(builder, ValidationDemo.class).partialUpdate().validateInput(patchRequest);
    Assert.assertEquals(result.isValid(), false);
    Assert.assertEquals(result.getMessages().toString(), errorMessage);
  }

  @DataProvider
  public Object[][] providePartialUpdateSuccessData()
  {
    return wrapSuccessCases(TestRestLiValidation.partialUpdateSuccesses(), BUILDERS);
  }

  @Test(dataProvider = "providePartialUpdateSuccessData")
  public void testPartialUpdateSuccess(Object builder, String patch)
  {
    PatchRequest<ValidationDemo> patchRequest = PatchBuilder.buildPatchFromString(patch);
    ValidationResult result = new RootBuilderWrapper<Integer, ValidationDemo>(builder, ValidationDemo.class).partialUpdate().validateInput(patchRequest);
    Assert.assertEquals(result.isValid(), true);
  }

  @DataProvider
  public Object[][] provideBatchCreateFailureData()
  {
    return wrapFailureCases(TestRestLiValidation.batchCreateFailureData(), BUILDERS);
  }

  @Test(dataProvider = "provideBatchCreateFailureData")
  public void testBatchCreateFailure(Object builder, List<ValidationDemo> validationDemos, List<String> errorMessages)
  {
    int i = 0;
    for (ValidationDemo input : validationDemos)
    {
      ValidationResult result = new RootBuilderWrapper<Integer, ValidationDemo>(builder, ValidationDemo.class).batchCreate().validateInput(input);
      Assert.assertEquals(result.isValid(), false);
      Assert.assertTrue(result.getMessages().toString().contains(errorMessages.get(i++)));
    }
  }

  @DataProvider
  public Object[][] provideBatchPartialUpdateData() throws DataProcessingException
  {
    return wrapFailureCases(TestRestLiValidation.batchPartialUpdateData(), BUILDERS);
  }

  @Test(dataProvider = "provideBatchPartialUpdateData")
  public void testBatchPartialUpdate(Object builder, Map<Integer, PatchRequest<ValidationDemo>> inputs, Map<Integer, String> errorMessages)
  {
    for (Map.Entry<Integer, PatchRequest<ValidationDemo>> entry : inputs.entrySet())
    {
      ValidationResult result = new RootBuilderWrapper<Integer, ValidationDemo>(builder, ValidationDemo.class).batchPartialUpdate().validateInput(entry.getValue());
      String expected = errorMessages.get(entry.getKey());
      if (expected.isEmpty())
      {
        Assert.assertEquals(result.isValid(), true);
      }
      else
      {
        Assert.assertEquals(result.isValid(), false);
        Assert.assertEquals(result.getMessages().toString(), expected);
      }
    }
  }
}
