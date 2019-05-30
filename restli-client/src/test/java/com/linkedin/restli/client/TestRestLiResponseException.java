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

package com.linkedin.restli.client;

import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.ErrorResponse;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests for {@link RestLiResponseException}.
 *
 * @author Evan Williams
 */
public class TestRestLiResponseException
{
  /**
   * Ensures that {@link RestLiResponseException#getErrorDetailsRecord()} functions properly.
   */
  @Test
  public void testGetErrorDetailsRecord()
  {
    RestLiResponseException restLiResponseException;

    // No error detail data
    restLiResponseException = new RestLiResponseException(new ErrorResponse()
        .setStatus(500));
    Assert.assertNull(restLiResponseException.getErrorDetailsRecord());

    // Error detail data without a specified type
    restLiResponseException = new RestLiResponseException(new ErrorResponse()
      .setStatus(500)
      .setErrorDetails(new ErrorDetails()));
    Assert.assertNull(restLiResponseException.getErrorDetailsRecord());

    // Error detail data with an invalid type
    restLiResponseException = new RestLiResponseException(new ErrorResponse()
        .setStatus(500)
        .setErrorDetails(new ErrorDetails())
        .setErrorDetailType("com.fake.stupid.forged.ClassName"));
    Assert.assertNull(restLiResponseException.getErrorDetailsRecord());

    // Error detail data with a specified type
    restLiResponseException = new RestLiResponseException(new ErrorResponse()
        .setStatus(500)
        .setErrorDetails(new ErrorDetails())
        .setErrorDetailType(EmptyRecord.class.getCanonicalName()));
    Assert.assertEquals(restLiResponseException.getErrorDetailsRecord().getClass(), EmptyRecord.class);
  }
}
