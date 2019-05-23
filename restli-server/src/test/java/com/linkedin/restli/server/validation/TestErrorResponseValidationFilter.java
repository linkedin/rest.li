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

package com.linkedin.restli.server.validation;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.TestServiceError;
import com.linkedin.restli.server.errors.ServiceError;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResourceModel;
import com.linkedin.restli.server.filter.FilterResponseContext;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


/**
 * Tests for {@link ErrorResponseValidationFilter}.
 *
 * @author Gevorg Kurghinyan
 */
public class TestErrorResponseValidationFilter
{
  @Mock
  private FilterRequestContext filterRequestContext;

  @Mock
  private FilterResponseContext filterResponseContext;

  @Mock
  private FilterResourceModel resourceModel;

  @BeforeMethod
  public void setUpMocks()
  {
    MockitoAnnotations.initMocks(this);
  }

  @DataProvider(name = "errorResponseValidationDataProvider")
  public Object[][] errorResponseValidationDataProvider()
  {
    return new Object[][]
        {
            // Resource level service errors
            // Method level service errors
            // Http status
            // Service error code
            // Error details
            // Expected Http status
            // Expected error code
            // Expected error details
            {
              // error code is defined through @ServiceErrors annotation on resource level.
              Collections.singletonList(TestServiceError.RESOURCE_LEVEL_ERROR),
              Collections.singletonList(TestServiceError.METHOD_LEVEL_ERROR),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, new ErrorDetails(),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, new ErrorDetails()
            },
            {
              // error code is defined through @ServiceErrors annotation on resource level with error details.
              Collections.singletonList(TestServiceError.RESOURCE_LEVEL_ERROR_WITH_ERROR_DETAILS),
              Collections.singletonList(TestServiceError.METHOD_LEVEL_ERROR),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR_WITH_ERROR_DETAILS, new EmptyRecord(),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR_WITH_ERROR_DETAILS, new EmptyRecord()
            },
            {
              // error code is defined through @ServiceErrors annotation on method level.
              Collections.singletonList(TestServiceError.RESOURCE_LEVEL_ERROR),
              Collections.singletonList(TestServiceError.METHOD_LEVEL_ERROR_WITH_ERROR_DETAILS),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.METHOD_LEVEL_ERROR_WITH_ERROR_DETAILS, new EmptyRecord(),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.METHOD_LEVEL_ERROR_WITH_ERROR_DETAILS, new EmptyRecord()
            },
            {
              // error code is defined through @ServiceErrors annotation on resource level
              // and on method level no service error code has been defined.
              Collections.singletonList(TestServiceError.RESOURCE_LEVEL_ERROR),
              Collections.emptyList(),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, new ErrorDetails(),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, new ErrorDetails()
            },
            {
              // error code is defined through @ServiceErrors annotation on method level
              // and on resource level no service error code has been defined.
              Collections.emptyList(),
              Collections.singletonList(TestServiceError.METHOD_LEVEL_ERROR),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.METHOD_LEVEL_ERROR, new ErrorDetails(),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.METHOD_LEVEL_ERROR, new ErrorDetails()
            },
            {
              // service error code hasn't been defined neither on resource level nor on method level.
              Collections.emptyList(),
              Collections.emptyList(),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, new ErrorDetails(),
              HttpStatus.S_500_INTERNAL_SERVER_ERROR, null, null
            },
            {
              // service error code is null both on resource level and on method level.
              null,
              null,
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, null,
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, null
            },
            {
              // service error code is null both on resource level and on method level
              // and error response has an error details
              null,
              null,
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, new ErrorDetails(),
              HttpStatus.S_500_INTERNAL_SERVER_ERROR, null, null
            },
            {
              // Http status code in error response doesn't match with defined service error code.
              Collections.singletonList(TestServiceError.RESOURCE_LEVEL_ERROR),
              Collections.singletonList(TestServiceError.METHOD_LEVEL_ERROR),
              HttpStatus.S_401_UNAUTHORIZED, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, new ErrorDetails(),
              HttpStatus.S_500_INTERNAL_SERVER_ERROR, null, null
            },
            {
              // Error detail type in error response doesn't match with defined error detail type.
              Collections.singletonList(TestServiceError.RESOURCE_LEVEL_ERROR),
              Collections.singletonList(TestServiceError.METHOD_LEVEL_ERROR),
              HttpStatus.S_400_BAD_REQUEST, TestServiceError.Codes.RESOURCE_LEVEL_ERROR, new EmptyRecord(),
              HttpStatus.S_500_INTERNAL_SERVER_ERROR, null, null
            }
        };
  }

  /**
   * Ensures that the validation filter correctly validates outgoing error response
   * to have predefined http status code, service error code and error details.
   */
  @Test(dataProvider = "errorResponseValidationDataProvider")
  public void testErrorResponseValidation(List<ServiceError> resourceServiceErrors,
      List<ServiceError> methodServiceErrors, HttpStatus httpStatus, String serviceErrorCode,
      RecordTemplate errorDetails, HttpStatus expectedHttpStatus, String expectedErrorCode,
      RecordTemplate expectedErrorDetails)
  {
    try
    {
      when(filterRequestContext.getFilterResourceModel()).thenReturn(resourceModel);
      when(resourceModel.getServiceErrors()).thenReturn(resourceServiceErrors);
      when(filterRequestContext.getMethodServiceErrors()).thenReturn(methodServiceErrors);

      ErrorResponseValidationFilter validationFilter = new ErrorResponseValidationFilter();

      RestLiServiceException restLiServiceException = new RestLiServiceException(httpStatus);
      restLiServiceException.setCode(serviceErrorCode);
      restLiServiceException.setErrorDetails(errorDetails);

      CompletableFuture<Void> future = validationFilter.onError(restLiServiceException,
          filterRequestContext, filterResponseContext);
      Assert.assertTrue(future.isCompletedExceptionally());

      future.get();
    }
    catch (Exception exception)
    {
      if (exception.getCause() != null && exception.getCause() instanceof RestLiServiceException)
      {
        RestLiServiceException restLiServiceException = (RestLiServiceException) exception.getCause();

        Assert.assertEquals(restLiServiceException.getStatus(), expectedHttpStatus);
        Assert.assertEquals(restLiServiceException.getCode(), expectedErrorCode);
        Assert.assertEquals(restLiServiceException.getErrorDetailsRecord(), expectedErrorDetails);
      }
      else
      {
        Assert.fail("Expected to get only RestLiServiceException.");
      }
    }
  }
}