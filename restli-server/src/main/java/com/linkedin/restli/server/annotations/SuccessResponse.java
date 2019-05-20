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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.linkedin.restli.server.ResourceConfigException;


/**
 * <p>List of successful status codes that a resource method may return.</p>
 *
 * <p>This is used only for API/documentation generation, and no validation is done by the Rest.li framework
 * to ensure that resource methods abide by these success statuses.</p>
 *
 * <p>If a status code that lies outside of the range <code>[200,400)</code> is used, then a
 * {@link ResourceConfigException} will be thrown during setup.</p>
 *
 * @author Karthik Balasubramanian
 * @author Gevorg Kurghinyan
 * @author Evan Williams
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface SuccessResponse
{
  /**
   * List of successful status codes.
   */
  int[] statuses();
}
