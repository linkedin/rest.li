/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.server.annotations;


import com.linkedin.restli.common.RestConstants;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Method-level annotation for 'finder' methods e.g. /people-search?q=search&name=bob&title=ceo. Note that
 * the annotation just names the finder and implementations @QueryParam to annotate parameters in
 * the method signature
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Finder
{
  /**
   * Name of this Finder
   */
  String value();

  /**
   * The linked batch finder method name on the same resource if any. For this to be valid:
   *
   * <ul>
   *   <li>A batch finder method with the linked batch finder name must exist on the same resource.</li>
   *   <li>If the finder has a metadata type then the linked batch finder must also have the same metadata type.</li>
   *   <li>All the query and assoc key parameters in the finder must have fields with the same name, type and
   *   optionality in the criteria object. The criteria object cannot contain any other fields.</li>
   *   <li>If the finder supports paging, then the linked batch finder must also support paging.</li>
   * </ul>
   *
   * <p>This linkage is useful for clients to optimize parallel finder calls by merging them into a single
   * batch finder.</p>
   */
  String linkedBatchFinderName() default RestAnnotations.DEFAULT;
}
