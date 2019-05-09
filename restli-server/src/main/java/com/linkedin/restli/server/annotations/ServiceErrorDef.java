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

package com.linkedin.restli.server.annotations;

import com.linkedin.restli.server.errors.ServiceError;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Defines the set of acceptable service errors for some resource class.
 *
 * @author Karthik Balasubramanian
 * @author Gevorg Kurghinyan
 * @author Evan Williams
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ServiceErrorDef
{
  /**
   * The enum implementing {@link ServiceError} which describes the set of acceptable service errors.
   * Service error codes are mapped to these service errors using the {@link ServiceError#code()} method.
   */
  Class<? extends Enum<? extends ServiceError>> value();
}