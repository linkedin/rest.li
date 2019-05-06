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
import java.util.Collection;
import java.util.Map;


/**
 * Mock implementation of {@link ValidationErrorHandler} interface which allows applications to customize the service error code,
 * error message and error details used for validation errors returned by {@link RestLiValidationFilter} filter.
 *
 * @author Gevorg Kurghinyan
 */
public class MockValidationErrorHandler implements ValidationErrorHandler {
  @Override
  public void updateErrorDetails(RestLiServiceException exception, Collection<Message> messages) {
    // TODO - update when RestLiServiceException supports the new RecordTemplate type error details.
  }

  @Override
  public void updateErrorDetails(RestLiServiceException exception, Map<String, Collection<Message>> messages) {
    // TODO - update when RestLiServiceException supports the new RecordTemplate type error details.
  }
}