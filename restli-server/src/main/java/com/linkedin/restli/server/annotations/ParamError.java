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

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * <p>List of method parameters for which a given method-level service error applies.</p>
 *
 * <p>This annotation is used to specify which parameters of a resource method are associated with a particular
 * method-level service error code. This information is used only for API/documentation generation, not for
 * any runtime service error validation.</p>
 *
 * @author Karthik Balasubramanian
 * @author Gevorg Kurghinyan
 * @author Evan Williams
 */
@Repeatable(ParamErrors.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ParamError
{
  /**
   * Method-level service error code that this annotation is associated with. Must match one of the service errors
   * defined for this method using {@link ServiceErrors}.
   *
   * e.g. 'INPUT_VALIDATION_FAILED'
   */
  String code();

  /**
   * Resource method parameters for which this service error applies, if any. If provided, the Rest.li framework will
   * validate the parameter name to ensure it matches one of the method's parameters.
   *
   * e.g. { 'firstName', 'lastName' }
   */
  String[] parameterNames();
}
