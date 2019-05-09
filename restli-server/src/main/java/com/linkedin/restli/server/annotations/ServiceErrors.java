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

import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.errors.ServiceError;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * <p>List of service error codes that can be returned by a resource or method.</p>
 *
 * <p>The Rest.li framework will use the service error definition in {@link ServiceErrorDef} to convert
 * string service error codes to {@link ServiceError} objects.</p>
 *
 * <p>When validation is enabled, the Rest.li framework will allow only service error codes defined at the resource
 * or method level to be returned. Unrecognized service error codes will result in a 500 error and error details will
 * be pruned. An empty list of service error codes will result in a 500 error for any code whatsoever. An empty list
 * differs semantically from not using the annotation at all, which indicates that validation will not be done.</p>
 *
 * <p>In addition, using service error codes here that aren't included in the corresponding {@link ServiceErrorDef}
 * annotation will result in a {@link ResourceConfigException} during setup.</p>
 *
 * @author Karthik Balasubramanian
 * @author Gevorg Kurghinyan
 * @author Evan Williams
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ServiceErrors
{
  /**
   * List of service error codes that can be returned by a resource or method using this annotation.
   * Using an empty list for this value is equivalent to asserting that no service error codes will be
   * encountered.
   */
  String[] value() default {};
}
