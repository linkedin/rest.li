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

import com.linkedin.data.message.Message;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.errors.MockBadRequest;
import com.linkedin.restli.server.errors.MockInputError;
import com.linkedin.restli.server.errors.MockInputErrorArray;
import java.util.Collection;
import java.util.Map;


/**
 * Mock implementation of {@link ValidationErrorHandler} interface which allows applications to customize the service error code,
 * error message and error details used for validation errors returned by {@link RestLiValidationFilter} filter.
 *
 * @author Gevorg Kurghinyan
 */
public class MockValidationErrorHandler implements ValidationErrorHandler {
  private static final String ERROR_CODE = "BAD_REQUEST";

  @Override
  public void updateErrorDetails(RestLiServiceException exception, Collection<Message> messages)
  {
    MockBadRequest badRequest = new MockBadRequest();
    MockInputErrorArray inputErrors = new MockInputErrorArray();

    for (Message message : messages)
    {
      if (message.isError() && message.getErrorDetails() instanceof MockInputError)
      {
        inputErrors.add((MockInputError) message.getErrorDetails());
      }
    }

    badRequest.setInputErrors(inputErrors);
    exception.setErrorDetails(badRequest);
    exception.setCode(ERROR_CODE);
  }

  @Override
  public void updateErrorDetails(RestLiServiceException exception, Map<String, Collection<Message>> messages)
  {
    MockBadRequest badRequest = new MockBadRequest();
    MockInputErrorArray inputErrors = new MockInputErrorArray();

    for (Map.Entry<String, Collection<Message>> entry : messages.entrySet())
    {
      for (Message message : entry.getValue())
      {
        if (message.isError() && message.getErrorDetails() instanceof MockInputError)
        {
          MockInputError inputError = (MockInputError) message.getErrorDetails();
          inputError.setKey(entry.getKey());
          inputErrors.add(inputError);
        }
      }
    }

    badRequest.setInputErrors(inputErrors);
    exception.setErrorDetails(badRequest);
    exception.setCode(ERROR_CODE);
  }
}