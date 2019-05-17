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

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.EmptyRecord;
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
   * Dummy service error definition to use in this file.
   * TODO: Refactor other dummy service errors in restli-server so that they can be used here, then remove this
   */
  private enum DummyServiceError implements ServiceError
  {
    ERROR_A(400, "I_BROKE", "I just broke.", EmptyRecord.class);

    int _status;
    String _code;
    String _message;
    Class<? extends RecordTemplate> _errorDetailType;

    DummyServiceError(int status, String code, String message, Class<? extends RecordTemplate> errorDetailType)
    {
      _status = status;
      _code = code;
      _message = message;
      _errorDetailType = errorDetailType;
    }

    @Override
    public int httpStatus() {
      return _status;
    }

    @Override
    public String code() {
      return _code;
    }

    @Override
    public String message() {
      return _message;
    }

    @Override
    public Class<? extends RecordTemplate> errorDetailType() {
      return _errorDetailType;
    }
  }

  /**
   * Ensures that a service exception can be constructed correctly from a {@link ServiceError}.
   */
  @Test
  public void testConstructFromServiceError()
  {
    final Throwable cause = new RuntimeException("Underlying exception message, should not be seen.");
    final RestLiServiceException restLiServiceException = new RestLiServiceException(DummyServiceError.ERROR_A, cause)
        .setErrorDetails(new EmptyRecord());

    Assert.assertTrue(restLiServiceException.hasCode());
    Assert.assertTrue(restLiServiceException.hasErrorDetails());
    Assert.assertFalse(restLiServiceException.hasDocUrl());
    Assert.assertFalse(restLiServiceException.hasRequestId());

    Assert.assertEquals(restLiServiceException.getStatus().getCode(), DummyServiceError.ERROR_A.httpStatus());
    Assert.assertEquals(restLiServiceException.getCode(), DummyServiceError.ERROR_A.code());
    Assert.assertEquals(restLiServiceException.getMessage(), DummyServiceError.ERROR_A.message());
    Assert.assertEquals(restLiServiceException.getErrorDetailsRecord(), new EmptyRecord());
    Assert.assertEquals(restLiServiceException.getErrorDetailType(), EmptyRecord.class.getCanonicalName());
    Assert.assertEquals(restLiServiceException.getCause(), cause);
  }
}
