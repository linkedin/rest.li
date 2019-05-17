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

package com.linkedin.restli.server.errors;

import com.linkedin.data.template.RecordTemplate;


/**
 * Interface for defining a service error.
 *
 * Implementations of this interface should be enums that define a set of service errors.
 *
 * @author Karthik Balasubramanian
 * @author Gevorg Kurghinyan
 * @author Evan Williams
 */
public interface ServiceError
{
  /**
   * The HTTP status code.
   *
   * e.g. 400
   */
  int httpStatus();

  /**
   * The canonical error code associated with this service error. The Rest.li framework will validate any service error
   * code returned by a resource to ensure it matches one of the codes defined for the resource/method.
   *
   * Note that this code is not the same as the HTTP status code. This code is a custom code defined for a particular
   * service or for an entire set of services. API consumers should be able to rely on these codes as being consistent
   * and standardized in order to handle service errors effectively for a service or a set of services.
   *
   * e.g. 'INPUT_VALIDATION_FAILED'
   */
  String code();

  /**
   * A human-readable explanation of the error.
   *
   * e.g. 'Validation failed for the input entity.'
   */
  default String message()
  {
    return null;
  }

  /**
   * Error detail type associated with this service error code. The Rest.li framework will validate the type
   * to ensure it matches the service error code returned at runtime.
   *
   * e.g. com.example.api.BadRequest
   */
  default Class<? extends RecordTemplate> errorDetailType()
  {
    return null;
  }
}
