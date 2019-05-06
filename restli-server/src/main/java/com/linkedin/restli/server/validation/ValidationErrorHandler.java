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
 * {@link ValidationErrorHandler} interface allows applications to customize the service error code, error message
 * and error details used for validation errors returned by {@link RestLiValidationFilter} filter.
 *
 * @author Gevorg Kurghinyan
 */
public interface ValidationErrorHandler
{

  /**
   * Updates service error code, error message and error details on the exception
   *
   * @param exception A {@link RestLiServiceException} which contains the appropriate HTTP response status and error details.
   * @param messages Collection of {@link Message}s, which provides an error status and formattable error messages.
   */
  void updateErrorDetails(RestLiServiceException exception, final Collection<Message> messages);

  /**
   * Updates service error code, error message and error details on the exception.
   * @apiNote Should be used for batch operations.
   *
   * @param exception A {@link RestLiServiceException} which contains the appropriate HTTP response status and error details.
   * @param messages Map of {@link Message}s. Each entry in the map corresponds to one entity in the batch request input.
   */
  void updateErrorDetails(RestLiServiceException exception, final Map<String, Collection<Message>> messages);
}