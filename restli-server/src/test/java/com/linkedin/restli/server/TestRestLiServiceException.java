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

package com.linkedin.restli.server;

import com.linkedin.data.DataMap;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.errors.ServiceError;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link RestLiServiceException}.
 *
 * @author Evan Williams
 */
public class TestRestLiServiceException
{
  /**
   * Ensures that a service exception can be constructed correctly from a {@link ServiceError}.
   */
  @Test
  public void testConstructFromServiceError()
  {
    final Throwable cause = new RuntimeException("Underlying exception message, should not be seen.");
    final RestLiServiceException restLiServiceException = new RestLiServiceException(TestServiceError.METHOD_LEVEL_ERROR, cause)
        .setErrorDetails(new EmptyRecord());

    Assert.assertTrue(restLiServiceException.hasCode());
    Assert.assertTrue(restLiServiceException.hasErrorDetails());
    Assert.assertFalse(restLiServiceException.hasDocUrl());
    Assert.assertFalse(restLiServiceException.hasRequestId());

    Assert.assertEquals(restLiServiceException.getStatus(), TestServiceError.METHOD_LEVEL_ERROR.httpStatus());
    Assert.assertEquals(restLiServiceException.getCode(), TestServiceError.METHOD_LEVEL_ERROR.code());
    Assert.assertEquals(restLiServiceException.getMessage(), TestServiceError.METHOD_LEVEL_ERROR.message());
    Assert.assertEquals(restLiServiceException.getErrorDetailsRecord(), new EmptyRecord());
    Assert.assertEquals(restLiServiceException.getErrorDetailType(), EmptyRecord.class.getCanonicalName());
    Assert.assertEquals(restLiServiceException.getCause(), cause);
  }

  @Test
  public void testErrorDetails()
  {
    final Throwable cause = new RuntimeException("Underlying exception message, should not be seen.");
    final RestLiServiceException restLiServiceException = new RestLiServiceException(TestServiceError.METHOD_LEVEL_ERROR, cause)
        .setErrorDetails((DataMap)null);

    Assert.assertTrue(restLiServiceException.hasCode());
    Assert.assertFalse(restLiServiceException.hasErrorDetails());
    Assert.assertFalse(restLiServiceException.hasDocUrl());
    Assert.assertFalse(restLiServiceException.hasRequestId());

    Assert.assertEquals(restLiServiceException.getStatus(), TestServiceError.METHOD_LEVEL_ERROR.httpStatus());
    Assert.assertEquals(restLiServiceException.getCode(), TestServiceError.METHOD_LEVEL_ERROR.code());
    Assert.assertEquals(restLiServiceException.getMessage(), TestServiceError.METHOD_LEVEL_ERROR.message());
    Assert.assertNull(restLiServiceException.getErrorDetails());
    Assert.assertNull(restLiServiceException.getErrorDetailsRecord());
    Assert.assertEquals(restLiServiceException.getCause(), cause);
  }

  @Test
  public void testNullStatus()
  {
    final RestLiServiceException restLiServiceException = new RestLiServiceException((HttpStatus) null);
    Assert.assertTrue(restLiServiceException.toString().contains("[HTTP Status:null]"));
  }
}